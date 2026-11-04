package com.tkb.common.net;

import com.tkb.common.crypto.CryptoUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * B2.1 - FrameCodec: Doc va ghi khung goi tin TCP theo dac ta Protocol (Muc 2).
 *
 * Cau truc khung (Frame):
 *   [4 byte do dai (big-endian int)] + [payload (byte[])]
 *
 * Luu y quan trong khi Client dong ket noi:
 *   - Khi Client goi socket.close() (sau khi LOGOUT), ham dataIn.readInt() se nem ra EOFException
 *     (trong Java, EOFException.getMessage() luon la null).
 *   - Day la tin hieu dong ket noi binh thuong, KHONG phai loi! Vi vay readFrame() phai nem thang
 *     EOFException ra ngoai (khong catch roi in "Loi o FrameCodec.readFrame: null" hay tra ve null/mang rong),
 *     de ClientSession bat EOFException va ghi log "Client da dong ket noi." sach se.
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/net/FrameCodec.java
 */
public final class FrameCodec {

    /** Gioi han kich thuoc payload toi da: 1 MiB (1048576 bytes) */
    public static final int MAX_FRAME_SIZE = 1024 * 1024;

    private FrameCodec() {
        // Utility class
    }

    /**
     * Doc mot khung du lieu (byte[]) tu luong TCP.
     *
     * @param in InputStream cua Socket
     * @return mang byte payload
     * @throws EOFException neu phia ben kia da dong ket noi TCP binh thuong
     * @throws IOException  neu kich thuoc khung <= 0, vuot qua 1 MiB, hoac loi mang
     */
    public static byte[] readFrame(InputStream in) throws IOException {
        DataInputStream dataIn = (in instanceof DataInputStream d) ? d : new DataInputStream(in);

        // Neu Client dong socket, readInt() se nem EOFException -> de nem thang ra ngoai cho ClientSession xu ly
        int length = dataIn.readInt();
        if (length <= 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("Kich thuoc khung goi tin khong hop le: " + length + " bytes (toi da " + MAX_FRAME_SIZE + " bytes)");
        }

        byte[] payload = new byte[length];
        dataIn.readFully(payload); // Dam bao doc du 100% so byte cua goi tin
        return payload;
    }

    /**
     * Ghi mot khung du lieu (byte[]) xuong luong TCP.
     * Dong bo (synchronized) tren OutputStream de dam bao khi server vua tra loi Response
     * vua day goi Push (JOB_PROGRESS / JOB_DONE) tu luong khac khong bi tron lan byte.
     */
    public static void writeFrame(OutputStream out, byte[] payload) throws IOException {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload khong duoc rong");
        }
        if (payload.length > MAX_FRAME_SIZE) {
            throw new IOException("Payload vuot qua gioi han 1 MiB: " + payload.length + " bytes");
        }

        synchronized (out) {
            DataOutputStream dataOut = (out instanceof DataOutputStream d) ? d : new DataOutputStream(out);
            dataOut.writeInt(payload.length); // Ghi 4 byte do dai big-endian
            dataOut.write(payload);           // Ghi toan bo payload
            dataOut.flush();
        }
    }

    /**
     * Tien ich: Doc khung goi tin khong ma hoa (dung cho buoc bat tay HELLO va KEY_EXCHANGE)
     * va chuyen thang sang chuoi JSON UTF-8.
     */
    public static String readPlainJsonFrame(InputStream in) throws IOException {
        byte[] bytes = readFrame(in);
        if (bytes == null || bytes.length == 0) {
            throw new EOFException("Client da dong ket noi");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Tien ich: Ghi chuoi JSON UTF-8 khong ma hoa (dung cho buoc bat tay HELLO va KEY_EXCHANGE).
     */
    public static void writePlainJsonFrame(OutputStream out, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        writeFrame(out, bytes);
    }

    /**
     * Tien ich: Ma hoa chuoi JSON UTF-8 bang khoi AES-256-GCM va ghi xuong luong TCP.
     * Dung cho moi goi tin tu buoc READY tro di.
     */
    public static void writeEncryptedJsonFrame(OutputStream out, String json, javax.crypto.SecretKey aesKey) throws IOException {
        byte[] plainBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = CryptoUtil.encryptAesGcm(plainBytes, aesKey);
        writeFrame(out, encryptedPayload);
    }

    /**
     * Tien ich: Doc khung goi tin tu luong TCP va giai ma AES-256-GCM thanh chuoi JSON UTF-8.
     * Neu readFrame() tra ve null hoac mang rong (do EOF), nem EOFException ngay de khong goi decryptAesGcm.
     */
    public static String readEncryptedJsonFrame(InputStream in, javax.crypto.SecretKey aesKey) throws IOException {
        byte[] encryptedPayload = readFrame(in);
        if (encryptedPayload == null || encryptedPayload.length == 0) {
            throw new EOFException("Client da dong ket noi");
        }
        byte[] plainBytes = CryptoUtil.decryptAesGcm(encryptedPayload, aesKey);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}