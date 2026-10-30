package com.tkb.common.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

/**
 * B2.2 - CryptoUtil: Ma hoa lai (Hybrid Encryption) theo dac ta Protocol (Muc 3).
 *
 * Quy trinh:
 *   1. Server sinh cap khoa RSA 2048-bit, gui PublicKey (X.509, Base64) trong goi HELLO.
 *   2. Client sinh khoa phien AES-256 ngau nhien (SecureRandom), ma hoa bang RSA-OAEP
 *      (RSA/ECB/OAEPWithSHA-256AndMGF1Padding) va gui trong goi KEY_EXCHANGE.
 *   3. Server giai ma lay khoa phien AES-256, gui goi READY da ma hoa AES-GCM.
 *   4. Moi goi tin sau do co cau truc:
 *      Payload = [12 byte IV ngau nhien] + [ciphertext] + [16 byte GCM auth tag (128-bit)]
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/CryptoUtil.java
 */
public final class CryptoUtil {

    public static final int RSA_KEY_SIZE = 2048;
    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_IV_LENGTH = 12;       // 12 bytes = 96 bits (chuan GCM)
    public static final int GCM_TAG_BITS = 128;       // 16 bytes = 128 bits auth tag
    public static final int GCM_TAG_BYTES = 16;

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Cau hinh tham so OAEP chuan SHA-256 de dam bao tuong thich giua moi phien ban JDK
     */
    private static final OAEPParameterSpec OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private CryptoUtil() {
        // Utility class
    }

    // =========================================================================
    // 1. CAC HAM RSA (DUNG CHO BUOC BAT TAY HELLO & KEY_EXCHANGE)
    // =========================================================================

    /**
     * Sinh cap khoa RSA 2048-bit (Server goi 1 lan khi khoi dong).
     */
    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE, SECURE_RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Khong the sinh cap khoa RSA", e);
        }
    }

    /**
     * Chuyen PublicKey RSA sang chuoi Base64 (dinh dang X.509) de gui trong goi HELLO.
     */
    public static String encodePublicKeyBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Khoi phuc PublicKey RSA tu chuoi Base64 (Client goi khi nhan goi HELLO).
     */
    public static PublicKey decodePublicKeyBase64(String base64PublicKey) throws IOException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IOException("Khoa cong khai RSA khong hop le", e);
        }
    }

    /**
     * Client ma hoa khoa phien AES-256 bang PublicKey RSA cua Server, tra ve Base64
     * de gui trong goi KEY_EXCHANGE.
     */
    public static String encryptSessionKeyRsaBase64(SecretKey aesKey, PublicKey serverPublicKey) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey, OAEP_SPEC);
            byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
            return Base64.getEncoder().encodeToString(encryptedKey);
        } catch (GeneralSecurityException e) {
            throw new IOException("Loi khi ma hoa khoa phien AES bang RSA-OAEP", e);
        }
    }

    /**
     * Server giai ma chuoi Base64 trong goi KEY_EXCHANGE bang PrivateKey RSA
     * de thu lai khoa phien AES-256 (32 bytes).
     */
    public static SecretKey decryptSessionKeyRsaBase64(String encryptedKeyBase64, PrivateKey serverPrivateKey) throws IOException {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedKeyBase64);
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey, OAEP_SPEC);
            byte[] aesKeyBytes = cipher.doFinal(encryptedBytes);
            if (aesKeyBytes.length != AES_KEY_SIZE / 8) {
                throw new IOException("Do dai khoa AES sau giai ma khong dung 256-bit (32 bytes): " + aesKeyBytes.length);
            }
            return new SecretKeySpec(aesKeyBytes, "AES");
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IOException("Giai ma khoa phien RSA that bai", e);
        }
    }

    // =========================================================================
    // 2. CAC HAM AES-256-GCM (DUNG CHO MOI GOI TIN SAU KHI BAT TAY)
    // =========================================================================

    /**
     * Sinh khoa phien AES-256 ngau nhien (Client goi cho moi ket noi moi).
     */
    public static SecretKey generateAesSessionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE, SECURE_RANDOM);
            return keyGen.generateKey();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Khong the sinh khoa AES-256", e);
        }
    }

    /**
     * Ma hoa mang byte plaintext bang AES-256-GCM.
     * Moi lan goi sinh mot IV 12 byte ngau nhien moi.
     *
     * @return mang byte: [12 byte IV] + [ciphertext + 16 byte GCM tag]
     */
    public static byte[] encryptAesGcm(byte[] plaintext, SecretKey aesKey) throws IOException {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

            byte[] cipherTextWithTag = cipher.doFinal(plaintext);

            byte[] output = new byte[GCM_IV_LENGTH + cipherTextWithTag.length];
            System.arraycopy(iv, 0, output, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherTextWithTag, 0, output, GCM_IV_LENGTH, cipherTextWithTag.length);
            return output;
        } catch (GeneralSecurityException e) {
            throw new IOException("Loi ma hoa AES-256-GCM", e);
        }
    }

    /**
     * Giai ma payload AES-256-GCM ([12 byte IV] + [ciphertext + 16 byte GCM tag]).
     * Neu du lieu bi sua doi hoac sai khoa -> nem IOException ngay lap tuc.
     */
    public static byte[] decryptAesGcm(byte[] payload, SecretKey aesKey) throws IOException {
        if (payload == null || payload.length < GCM_IV_LENGTH + GCM_TAG_BYTES) {
            throw new IOException("Goi ma hoa AES-GCM qua ngan (thieu IV hoac the xac thuc)");
        }
        try {
            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

            return cipher.doFinal(payload, GCM_IV_LENGTH, payload.length - GCM_IV_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new IOException("Giai ma AES-256-GCM that bai (sai khoa hoac du lieu bi thay doi)", e);
        }
    }
}