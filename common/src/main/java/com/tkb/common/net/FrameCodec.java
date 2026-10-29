package com.tkb.common.net;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.tkb.common.crypto.CryptoUtil;

/**
 * B2.1 - FrameCodec: Doc va ghi khung goi tin TCP theo dac ta Protocol (Muc 2).
 *
 * Cau truc khung (Frame):
 *   [4 byte do dai (big-endian int)] + [payload (byte[])]
 *
 * Quy dinh bat buoc:
 *   - Do dai payload toi da: 1 MiB (1_048_576 bytes). Neu vuot qua -> nem IOException de dong ket noi.
 *   - Bat buoc dung DataInputStream.readInt() va readFully() vi TCP la luong byte (stream),
 *     mot goi tin lon co the bi chia nho thanh nhieu manh khi truyen qua mang.
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/FrameCodec.java
 */

public final class FrameCodec {
    
    public static final int MAX_FRAME_SIZE = 1024 * 1024; // 1MB

    private FrameCodec(){

    }




    /**
     * Doc mot khung du lieu (byte[]) tu luong TCP.
     * Ham nay se block cho den khi doc du 4 byte do dai va toan bo byte cua payload.
     *
     * @param in InputStream cua Socket
     * @return mang byte payload
     * @throws EOFException neu phia ben kia dong ket noi
     * @throws IOException  neu kich thuoc khung <= 0, vuot qua 1 MiB, hoac loi mang
     */
    public static byte[] readFrame(InputStream in){
        DataInputStream dataIn;

        if(in instanceof DataInputStream d){
            dataIn = d;
        } else {
            dataIn = new DataInputStream(in);
        }
        
        try{
            int length = dataIn.readInt();
            if( length < 0 || length > MAX_FRAME_SIZE) {
                throw new IOException("Invalid frame length: " + length);
            }
            
            byte[] payload = new byte[length];
            dataIn.readFully(payload);
            return payload;

        }catch(Exception e){
            System.out.println("Lỗi ở FrameCodec.readFrame: " + e.getMessage());
        }
        
        return null;

    }


    /**
     * Ghi mot khung du lieu (byte[]) xuong luong TCP.
     * Dong bo (synchronized) tren OutputStream de dam bao khi server vua tra loi Response
     * vua day goi Push (JOB_PROGRESS / JOB_DONE) tu luong khac khong bi tron lan byte.
     *
     * @param out     OutputStream cua Socket
     * @param payload mang byte can gui
     * @throws IOException neu payload rong, vuot qua 1 MiB, hoac loi mang
     */
    public static void writeFrame(OutputStream out, byte[] payload){
        if(payload == null || payload.length == 0 || payload.length > MAX_FRAME_SIZE){
            throw new IllegalArgumentException("Invalid payload length: " + (payload == null ? "null" : payload.length));
        }

        DataOutputStream dataOut;

        if(out instanceof DataOutputStream d){
            dataOut = d;
        } else {
            dataOut = new DataOutputStream(out);
        }

        synchronized (dataOut) {
            try {
                dataOut.writeInt(payload.length);
                dataOut.write(payload);
                dataOut.flush();
            } catch (IOException e) {
                System.out.println("Lỗi ở FrameCodec.writeFrame: " + e.getMessage());
            }
        }

    }


    /**
     * Tien ich: Doc khung goi tin khong ma hoa (dung cho buoc bat tay HELLO va KEY_EXCHANGE)
     * va chuyen thang sang chuoi JSON UTF-8.
     */
    public static String readPlainJsonFrame(InputStream in){
        byte[] bytes = readFrame(in);
        return new String(bytes,StandardCharsets.UTF_8);
    }


    /**
     * Tien ich: Ghi chuoi JSON UTF-8 khong ma hoa (dung cho buoc bat tay HELLO va KEY_EXCHANGE).
     */
    public static void writePlainJsonFrame(OutputStream out, String json){
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        writeFrame(out, bytes);
    }


    /**
     * Tien ich: Ma hoa chuoi JSON UTF-8 bang khoi AES-256-GCM va ghi xuong luong TCP.
     * Dung cho moi goi tin tu buoc READY tro di.
     */
    public static void WriteEncryptedJsonFrame(OutputStream out, String json, SecretKey aeskey) throws IOException{
        byte[] plainBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedPayload = CryptoUtil.encryptAesGcm(plainBytes, aeskey);
        writeFrame(out, encryptedPayload);
    } 

    /**
     * Tien ich: Doc khung goi tin tu luong TCP va giai ma AES-256-GCM thanh chuoi JSON UTF-8.
     * Theo Protocol muc 3: Neu giai ma that bai thi nem IOException de dong ket noi ngay.
     */
    public static String readEncryptedJsonFrame(InputStream in, SecretKey aeskey) throws IOException{
        byte[] encryptedPayload = readFrame(in);
        byte[] plainBytes = CryptoUtil.decryptAesGcm(encryptedPayload, aeskey);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
