package com.tkb.backend.service;

import com.tkb.backend.db.DatabaseConfig;
import com.tkb.common.dto.ImportReport;
import com.tkb.common.dto.ImportReport.CourseStats;
import com.tkb.common.dto.ImportReport.SectionStats;
import com.tkb.common.dto.SlotDto;
import com.tkb.common.protocol.ErrorCode;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * B9.5, B9.6, B9.7 - PdfImportService: Quan ly phien upload file PDF theo tung chunk
 * (IMPORT_BEGIN, IMPORT_CHUNK, IMPORT_ABORT, IMPORT_COMMIT), tu dong don upload do sau 10 phut,
 * phan tich du lieu va ghi vao MySQL trong 1 Transaction (ho tro dryRun).
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/service/PdfImportService.java
 */
public class PdfImportService {

    /** Gioi han kich thuoc file PDF toi da: 20 MiB (Muc 9 Protocol) */
    public static final long MAX_PDF_SIZE_BYTES = 20L * 1024L * 1024L;

    /** Kich thuoc moi chunk tho: 512 KiB (524288 bytes - sau Base64 van nho hon 1 MiB) */
    public static final int CHUNK_SIZE_BYTES = 524_288;

    /** Thoi gian het han cua phien upload do: 10 phut (B9.6) */
    public static final long UPLOAD_TIMEOUT_MS = 10L * 60L * 1000L;

    private final CatalogCache catalogCache;
    private final Map<String, UploadSession> uploads = new ConcurrentHashMap<>();

    public PdfImportService(CatalogCache catalogCache) {
        this.catalogCache = catalogCache;
    }

    public record UploadActionResult(
            boolean ok,
            ErrorCode errorCode,
            String errorMessage,
            Map<String, Object> data,
            Map<String, Object> errorDetails
    ) {
        public static UploadActionResult success(Map<String, Object> data) {
            return new UploadActionResult(true, null, null, data, null);
        }

        public static UploadActionResult fail(ErrorCode code, String message) {
            return new UploadActionResult(false, code, message, null, null);
        }

        public static UploadActionResult failWithDetails(ErrorCode code, String message, Map<String, Object> details) {
            return new UploadActionResult(false, code, message, null, details);
        }
    }

    /**
     * B9.6 - Bat dau mot phien upload file PDF (IMPORT_BEGIN).
     */
    public UploadActionResult beginUpload(String sessionId, String fileName, long sizeBytes, String semesterCode) {
        cleanupExpiredUploads();

        if (fileName == null || !fileName.trim().toLowerCase().endsWith(".pdf")) {
            return UploadActionResult.failWithDetails(
                    ErrorCode.VALIDATION_FAILED,
                    "File tải lên phải có định dạng .pdf",
                    Map.of("field", "fileName")
            );
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_PDF_SIZE_BYTES) {
            return UploadActionResult.failWithDetails(
                    ErrorCode.VALIDATION_FAILED,
                    "Kích thước file PDF phải lớn hơn 0 và không vượt quá 20 MiB.",
                    Map.of("field", "sizeBytes")
            );
        }

        int expectedChunks = (int) ((sizeBytes + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES);
        String uploadId = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        UploadSession session = new UploadSession(uploadId, sessionId, fileName.trim(), sizeBytes, semesterCode.trim(), expectedChunks);
        uploads.put(uploadId, session);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("uploadId", uploadId);
        res.put("chunkSize", CHUNK_SIZE_BYTES);
        return UploadActionResult.success(res);
    }

    /**
     * B9.6 - Nhan mot chunk du lieu Base64 theo thu tu index tu 0 (IMPORT_CHUNK).
     */
    public UploadActionResult receiveChunk(String uploadId, int index, String dataBase64) {
        cleanupExpiredUploads();

        UploadSession session = uploads.get(uploadId);
        if (session == null) {
            return UploadActionResult.fail(ErrorCode.UPLOAD_NOT_FOUND, ErrorCode.UPLOAD_NOT_FOUND.getDefaultMessage());
        }

        synchronized (session) {
            session.touch();
            if (index != session.nextExpectedIndex) {
                return UploadActionResult.fail(
                        ErrorCode.CHUNK_OUT_OF_ORDER,
                        "Thứ tự chunk không hợp lệ: mong đợi chunk " + session.nextExpectedIndex + " nhưng nhận được " + index
                );
            }
            if (dataBase64 == null || dataBase64.isBlank()) {
                return UploadActionResult.failWithDetails(
                        ErrorCode.VALIDATION_FAILED,
                        "Dữ liệu dataBase64 không được rỗng.",
                        Map.of("field", "dataBase64")
                );
            }

            byte[] rawChunk;
            try {
                rawChunk = Base64.getDecoder().decode(dataBase64.trim());
            } catch (IllegalArgumentException e) {
                return UploadActionResult.failWithDetails(
                        ErrorCode.VALIDATION_FAILED,
                        "Chuỗi Base64 của chunk không hợp lệ.",
                        Map.of("field", "dataBase64")
                );
            }

            if (rawChunk.length > CHUNK_SIZE_BYTES) {
                return UploadActionResult.failWithDetails(
                        ErrorCode.VALIDATION_FAILED,
                        "Kích thước chunk vượt quá 512 KiB.",
                        Map.of("field", "dataBase64")
                );
            }

            session.buffer.write(rawChunk, 0, rawChunk.length);
            session.nextExpectedIndex++;

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("received", session.nextExpectedIndex);
            return UploadActionResult.success(res);
        }
    }

    /**
     * B9.6 - Huy phien upload do (IMPORT_ABORT).
     */
    public boolean abortUpload(String uploadId) {
        return uploads.remove(uploadId) != null;
    }

    /**
     * Huy toan bo upload do cua 1 phien khi Admin ngat ket noi (B9.6).
     */
    public void abortUploadsBySession(String sessionId) {
        if (sessionId == null) return;
        uploads.entrySet().removeIf(e -> sessionId.equals(e.getValue().sessionId));
    }

    /**
     * Kiem tra truoc khi chay IMPORT_COMMIT: phien upload co ton tai va da nhan du cac chunk chua.
     */
    public UploadActionResult validateBeforeCommit(String uploadId) {
        cleanupExpiredUploads();
        UploadSession session = uploads.get(uploadId);
        if (session == null) {
            return UploadActionResult.fail(ErrorCode.UPLOAD_NOT_FOUND, ErrorCode.UPLOAD_NOT_FOUND.getDefaultMessage());
        }
        synchronized (session) {
            session.touch();
            if (session.nextExpectedIndex < session.expectedChunks) {
                List<Integer> missing = new ArrayList<>();
                for (int i = session.nextExpectedIndex; i < session.expectedChunks; i++) {
                    missing.add(i);
                }
                return UploadActionResult.failWithDetails(
                        ErrorCode.MISSING_CHUNKS,
                        "File tải lên còn thiếu " + missing.size() + " chunk.",
                        Map.of("missing", missing)
                );
            }
        }
        return UploadActionResult.success(Map.of());
    }

    /**
     * B9.7 - Thực thi phân tích PDF và (nếu dryRun = false) ghi vào CSDL trong 1 Transaction.
     * Được gọi bên trong Worker Thread của JobManager.
     */
    public ImportReport executeImportJob(
            String uploadId,
            boolean dryRun,
            BooleanSupplier isCancelled,
            IntConsumer onProgress) throws Exception {

        UploadSession session = uploads.get(uploadId);
        if (session == null) {
            throw new IllegalStateException("Phiên tải file không tồn tại: " + uploadId);
        }

        byte[] pdfBytes;
        String semesterCode = session.semesterCode;
        synchronized (session) {
            pdfBytes = session.buffer.toByteArray();
        }

        onProgress.accept(10);

        // Luu file PDF tam va goi bo cong cu Python hoac doc CSV cua hoc ky de kiem tra & nap
        Path tempPdf = Files.createTempFile("tkb_import_" + semesterCode + "_", ".pdf");
        Path tempCsv = Files.createTempFile("tkb_parsed_" + semesterCode + "_", ".csv");
        try {
            Files.write(tempPdf, pdfBytes);
            onProgress.accept(25);

            List<ParsedCsvRow> rows = extractRowsFromPdfOrExistingCsv(tempPdf, tempCsv, semesterCode);
            if (isCancelled.getAsBoolean()) {
                return new ImportReport(dryRun, 0, new CourseStats(), new SectionStats(), List.of(), List.of());
            }
            onProgress.accept(60);

            ImportReport report = analyzeAndCommit(semesterCode, rows, dryRun, isCancelled, onProgress);

            // Neu commit that thanh cong (dryRun = false va khong co loi) -> xoa phien upload va lam moi Cache
            if (!dryRun && !report.hasErrors()) {
                uploads.remove(uploadId);
                catalogCache.refreshSemester(semesterCode);
            }
            onProgress.accept(100);
            return report;
        } finally {
            try {
                Files.deleteIfExists(tempPdf);
                Files.deleteIfExists(tempCsv);
            } catch (IOException ignored) {
            }
        }
    }

    // =========================================================================
    // PHAN TICH DU LIEU & GHI TRANSACTION MYSQL (B9.5, B9.7)
    // =========================================================================

    private List<ParsedCsvRow> extractRowsFromPdfOrExistingCsv(Path pdfPath, Path outCsvPath, String semesterCode) throws IOException {
        // Uu tien 1: neu du lieu PDF theo hoc ky da duoc tai ve va xu ly bang script v2,
        // tim file csv/<semester>/thoi_khoa_bieu.csv de lay du lieu goc.
        Path existingCsv = Path.of("csv", semesterCode, "thoi_khoa_bieu.csv");
        Path downloadedPdfDir = Path.of("pdf", semesterCode);
        Path scriptPath = Path.of("tools", "processing_data_v2.py");

        if (Files.exists(scriptPath)) {
            String pythonBin = Files.exists(Path.of(".venv", "bin", "python"))
                    ? Path.of(".venv", "bin", "python").toString()
                    : "python3";
            try {
                List<String> command = new ArrayList<>();
                command.add(pythonBin);
                command.add(scriptPath.toString());
                command.add("--semester");
                command.add(semesterCode);
                if (Files.isDirectory(downloadedPdfDir) && Files.list(downloadedPdfDir).findAny().isPresent()) {
                    command.add("--pdf-dir");
                    command.add(downloadedPdfDir.toString());
                } else {
                    command.add("--pdf-dir");
                    command.add(pdfPath.getParent() != null ? pdfPath.getParent().toString() : ".");
                }
                command.add("--csv-dir");
                command.add(existingCsv.getParent() != null ? existingCsv.getParent().toString() : "csv");
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(Path.of("").toAbsolutePath().normalize().toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Doc het output tien trinh con de tranh treo tien trinh.
                    }
                }
                int code = proc.waitFor();
                if (code == 0 && Files.exists(existingCsv) && Files.size(existingCsv) > 0) {
                    return readCsvRows(existingCsv);
                }
            } catch (Exception ignored) {
                // Neu khong the goi duoc script moi, tiep tuc kiem tra file CSV co san.
            }
        }

        // Uu tien 2: Doc file csv/<semesterCode>/thoi_khoa_bieu.csv da duoc script xuat san
        if (Files.exists(existingCsv)) {
            return readCsvRows(existingCsv);
        }

        return List.of();
    }

    private List<ParsedCsvRow> readCsvRows(Path csvFile) throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        List<ParsedCsvRow> rows = new ArrayList<>();
        int rowNum = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) continue;
            // Bo qua dong tieu de
            if (i == 0 && (line.toLowerCase().contains("ma_mh") || line.toLowerCase().contains("mã mh"))) {
                continue;
            }
            rowNum++;
            String[] cols = line.split(",", -1);
            if (cols.length < 11) {
                rows.add(new ParsedCsvRow(rowNum, "", "", 0, "", "", "", "LT", 0, 0, 0, "", "", "Dòng thiếu cột dữ liệu (" + cols.length + " cột)"));
                continue;
            }
            try {
                String maMh = cols[0].trim();
                String tenMh = cols[1].trim();
                int soTc = parseIntSafe(cols[2], 0);
                String giangVien = cols[3].trim();
                String nhom = cols[4].trim();
                String toTh = cols.length >= 12 ? cols[5].trim() : "";
                String loai = cols.length >= 12 ? cols[6].trim().toUpperCase() : "LT";
                int baseIdx = cols.length >= 12 ? 7 : 5;
                int thu = parseIntSafe(cols[baseIdx], 0);
                int tietBd = parseIntSafe(cols[baseIdx + 1], 0);
                int soTiet = parseIntSafe(cols[baseIdx + 2], 0);
                String phong = cols[baseIdx + 3].trim();
                String tuanHoc = (cols.length > baseIdx + 4) ? cols[baseIdx + 4].replace("\r", "").replace("\n", "") : "***************";

                rows.add(new ParsedCsvRow(rowNum, maMh, tenMh, soTc, giangVien, nhom, toTh, loai, thu, tietBd, soTiet, phong, tuanHoc, null));
            } catch (Exception ex) {
                rows.add(new ParsedCsvRow(rowNum, "", "", 0, "", "", "", "LT", 0, 0, 0, "", "", "Lỗi đọc dữ liệu: " + ex.getMessage()));
            }
        }
        return rows;
    }

    private ImportReport analyzeAndCommit(
            String semesterCode,
            List<ParsedCsvRow> rows,
            boolean dryRun,
            BooleanSupplier isCancelled,
            IntConsumer onProgress) throws SQLException {

        ImportReport report = new ImportReport();
        report.setDryRun(dryRun);
        report.setDetectedRows(rows.size());

        Map<String, ParsedCsvRow> uniqueCourses = new LinkedHashMap<>();
        Map<String, List<ParsedCsvRow>> groupedSections = new LinkedHashMap<>();

        for (ParsedCsvRow r : rows) {
            if (r.parseError != null) {
                report.addError(r.sttRow, r.parseError);
                continue;
            }
            if (r.maMh.isEmpty() || r.nhom.isEmpty()) {
                report.addError(r.sttRow, "Thiếu mã học phần hoặc mã nhóm lớp.");
                continue;
            }
            if (r.thu == 0) {
                report.addWarning(r.sttRow, "Học phần " + r.maMh + " nhóm " + r.nhom + " không có lịch cố định (Thứ = 0), đã bỏ qua buổi này.");
                continue;
            }
            if (r.thu < 2 || r.thu > 8 || r.tietBd < 1 || r.soTiet < 1 || (r.tietBd + r.soTiet - 1) > 12) {
                report.addError(r.sttRow, "Lịch học không hợp lệ (Thứ=" + r.thu + ", Tiết BĐ=" + r.tietBd + ", Số tiết=" + r.soTiet + ").");
                continue;
            }

            uniqueCourses.putIfAbsent(r.maMh, r);
            String secKey = r.maMh + "#" + r.nhom;
            groupedSections.computeIfAbsent(secKey, k -> new ArrayList<>()).add(r);
        }

        onProgress.accept(75);

        // Neu co loi nghiem trong hoac la che do dryRun -> thong ke so luong insert/update du kien ma khong ghi DB
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (Statement ensureSchema = conn.createStatement()) {
                ensureSchema.execute("ALTER TABLE lich_hoc MODIFY COLUMN tuan_hoc VARCHAR(255) NULL");
            } catch (SQLException ignored) {
                // Nếu cột đã đúng kích thước hoặc chưa tồn tại trong schema cũ, bỏ qua.
            }

            Set<String> existingCourses = loadExistingCourseCodes(conn);
            Set<String> existingSectionKeys = loadExistingSectionKeys(conn, semesterCode);

            int courseIns = 0, courseUpd = 0;
            for (String cCode : uniqueCourses.keySet()) {
                if (existingCourses.contains(cCode)) courseUpd++;
                else courseIns++;
            }

            int secIns = 0, secUpd = 0;
            for (String sKey : groupedSections.keySet()) {
                if (existingSectionKeys.contains(sKey)) secUpd++;
                else secIns++;
            }
            int secDeactivated = 0;
            for (String oldKey : existingSectionKeys) {
                if (!groupedSections.containsKey(oldKey)) {
                    secDeactivated++;
                }
            }

            report.setCourses(new CourseStats(courseIns, courseUpd));
            report.setSections(new SectionStats(secIns, secUpd, secDeactivated));

            if (dryRun || report.hasErrors() || isCancelled.getAsBoolean()) {
                return report;
            }

            // dryRun = false va khong co loi -> Ghi toan bo trong 1 Transaction
            conn.setAutoCommit(false);
            try {
                // 1. Upsert hoc_phan
                String upsertCourseSql = """
                        INSERT INTO hoc_phan (ma_mh, ten, so_tc)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE ten = VALUES(ten), so_tc = IF(VALUES(so_tc) > 0, VALUES(so_tc), so_tc)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(upsertCourseSql)) {
                    for (ParsedCsvRow c : uniqueCourses.values()) {
                        ps.setString(1, c.maMh);
                        ps.setString(2, c.tenMh);
                        ps.setInt(3, c.soTc);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 2. Tat tat ca nhom lop cu trong hoc ky truoc, nhom nao co trong file se duoc bat lai TRUE
                try (PreparedStatement deactPs = conn.prepareStatement(
                        "UPDATE nhom_lop SET dang_hoat_dong = FALSE WHERE ma_hoc_ky = ?")) {
                    deactPs.setString(1, semesterCode);
                    deactPs.executeUpdate();
                }

                // 3. Upsert tung nhom_lop va thay the lich_hoc
                String upsertSecSql = """
                        INSERT INTO nhom_lop (ma_hoc_ky, ma_mh, ten_mh, nhom, giang_vien, dang_hoat_dong)
                        VALUES (?, ?, ?, ?, ?, TRUE)
                        ON DUPLICATE KEY UPDATE ten_mh = VALUES(ten_mh), giang_vien = VALUES(giang_vien), dang_hoat_dong = TRUE
                        """;
                String findSecIdSql = "SELECT id FROM nhom_lop WHERE ma_hoc_ky = ? AND ma_mh = ? AND nhom = ?";
                String delSlotsSql = "DELETE FROM lich_hoc WHERE nhom_lop_id = ?";
                String insSlotSql = """
                        INSERT INTO lich_hoc (nhom_lop_id, thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_hoc, tuan_mask)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement upsertSecPs = conn.prepareStatement(upsertSecSql);
                     PreparedStatement findIdPs = conn.prepareStatement(findSecIdSql);
                     PreparedStatement delSlotsPs = conn.prepareStatement(delSlotsSql);
                     PreparedStatement insSlotPs = conn.prepareStatement(insSlotSql)) {

                    for (List<ParsedCsvRow> secRows : groupedSections.values()) {
                        ParsedCsvRow first = secRows.get(0);
                        upsertSecPs.setString(1, semesterCode);
                        upsertSecPs.setString(2, first.maMh);
                        upsertSecPs.setString(3, first.tenMh);
                        upsertSecPs.setString(4, first.nhom);
                        upsertSecPs.setString(5, first.giangVien.isEmpty() ? null : first.giangVien);
                        upsertSecPs.executeUpdate();

                        long secId;
                        findIdPs.setString(1, semesterCode);
                        findIdPs.setString(2, first.maMh);
                        findIdPs.setString(3, first.nhom);
                        try (ResultSet rs = findIdPs.executeQuery()) {
                            if (!rs.next()) continue;
                            secId = rs.getLong(1);
                        }

                        delSlotsPs.setLong(1, secId);
                        delSlotsPs.executeUpdate();

                        for (ParsedCsvRow slotRow : secRows) {
                            String loai = "TH".equalsIgnoreCase(slotRow.loai) ? "TH" : "LT";
                            String toTh = "TH".equals(loai) ? (slotRow.toTh.isEmpty() ? "01" : slotRow.toTh) : "";
                            long mask = SlotDto.computeWeekMask(slotRow.tuanHoc);

                            insSlotPs.setLong(1, secId);
                            insSlotPs.setInt(2, slotRow.thu);
                            insSlotPs.setInt(3, slotRow.tietBd);
                            insSlotPs.setInt(4, slotRow.soTiet);
                            insSlotPs.setString(5, slotRow.phong);
                            insSlotPs.setString(6, toTh);
                            insSlotPs.setString(7, loai);
                            insSlotPs.setString(8, slotRow.tuanHoc);
                            insSlotPs.setLong(9, mask);
                            insSlotPs.addBatch();
                        }
                        insSlotPs.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return report;
    }

    private Set<String> loadExistingCourseCodes(Connection conn) throws SQLException {
        Set<String> set = new LinkedHashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT ma_mh FROM hoc_phan")) {
            while (rs.next()) {
                set.add(rs.getString(1));
            }
        }
        return set;
    }

    private Set<String> loadExistingSectionKeys(Connection conn, String semesterCode) throws SQLException {
        Set<String> set = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT ma_mh, nhom FROM nhom_lop WHERE ma_hoc_ky = ?")) {
            ps.setString(1, semesterCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString(1) + "#" + rs.getString(2));
                }
            }
        }
        return set;
    }

    private static int parseIntSafe(String val, int defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void cleanupExpiredUploads() {
        long now = System.currentTimeMillis();
        uploads.entrySet().removeIf(e -> (now - e.getValue().lastActivityMs) > UPLOAD_TIMEOUT_MS);
    }

    private record ParsedCsvRow(
            int sttRow,
            String maMh,
            String tenMh,
            int soTc,
            String giangVien,
            String nhom,
            String toTh,
            String loai,
            int thu,
            int tietBd,
            int soTiet,
            String phong,
            String tuanHoc,
            String parseError
    ) {
    }

    private static final class UploadSession {
        final String uploadId;
        final String sessionId;
        final String fileName;
        final long sizeBytes;
        final String semesterCode;
        final int expectedChunks;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nextExpectedIndex = 0;
        volatile long lastActivityMs = System.currentTimeMillis();

        UploadSession(String uploadId, String sessionId, String fileName, long sizeBytes, String semesterCode, int expectedChunks) {
            this.uploadId = uploadId;
            this.sessionId = sessionId;
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
            this.semesterCode = semesterCode;
            this.expectedChunks = expectedChunks;
        }

        void touch() {
            this.lastActivityMs = System.currentTimeMillis();
        }
    }
}