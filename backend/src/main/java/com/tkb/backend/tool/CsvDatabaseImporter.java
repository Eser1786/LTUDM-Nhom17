package com.tkb.backend.tool;

import java.util.*;
import java.io.*;
import java.sql.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvDatabaseImporter {

    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
    private static final String DB_USER = getEnv("DB_USER", "timetable_user");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "timetable_password");
    private static final String DB_PORT = getEnv("DB_PORT", "3307");
    private static final String DB_NAME = getEnv("DB_NAME", "timetable_db");

    private static final String DB_URL = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&rewriteBatchedStatements=true", DB_HOST, DB_PORT, DB_NAME);

    // Cấu trúc lưu tạm một buổi học (tương ứng với bảng lich_hoc)
    public record SlotRecord(int thu, int tietBd, int soTiet, String phong){}

    // cấu trúc gom nhóm một nhóm lớp (nhom_lop + lich_hoc)
    public static class SectionAggregate{
        final String maMH;
        final String tenMon;
        final String nhom;
        final Set<SlotRecord> slots = new LinkedHashSet<>();
        final Set<String> giangVienSet = new LinkedHashSet<>();

        public SectionAggregate(String maMH, String tenMon, String nhom){
            this.maMH = maMH;
            this.tenMon = tenMon;
            this.nhom = nhom;
        }

        public void AddGiangVien(String gv){
            if(gv.equals(null) || gv.isEmpty()) return;
            String cleaned = gv.trim();
            if(!cleaned.isEmpty() && cleaned.equalsIgnoreCase("Họ và Tên")){
                giangVienSet.add(cleaned);
            }
        }

        public String getMergedGiangVien(){
            if(giangVienSet.isEmpty()) return null;
            String merged = String.join(", ", giangVienSet);
            return merged.length() > 100 ? merged.substring(0, 100) : merged;
        }

    }


    public static void ReadAndAggrevgateCsv(Path csvPath, Map<String, String> courses, Map<String, SectionAggregate> sections) throws IOException {
        
        int totalRows = 0;
        int skippedZeroScheduelRows = 0;
        int duplicateSlotRows = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)){
            String header = reader.readLine();

            if(header == null){
                System.out.println("CSV file is empty");
                return;
            }

            String line;
            int lineNumber = 1;

            while((line = reader.readLine()) != null){

                lineNumber++;
                if(line.isBlank()) continue;
                totalRows++;

                List<String> cols = parseCsvLine(line);
                if(cols.size() < 8){
                    System.out.printf("Line %d: Not enough columns, skipping%n", lineNumber, cols.size(), line);
                    continue;
                }

                String maMh = cols.get(0).trim();
                String tenMon = cols.get(1).trim();
                String hoTenGV = cols.get(2).trim();
                String nhom = cols.get(3).trim();
                int thu = Integer.parseInt(cols.get(4).trim());
                int tietBd = Integer.parseInt(cols.get(5).trim());
                int soTiet = Integer.parseInt(cols.get(6).trim());

                // chuẩn hóa mã phòng, không bị tách chuỗi
                String maPhong = cols.get(7).trim().replaceAll("\\s+", "").replaceAll("\\.+$", "");
                
                // Lưu danh mục học phần
                courses.putIfAbsent(maMh, tenMon);

                





            }




        }


    }








    private static List<String> parseCsvLine(String line) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parseCsvLine'");
    }








    private static String getEnv(String key, String defaultValue){
        String value = System.getenv(key);
        return (value != null) ? value : defaultValue;
    }



    public static void main(String[] args) {
        System.out.println("CSV Database Importer is running");
    }
}