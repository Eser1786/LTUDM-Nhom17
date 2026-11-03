package com.tkb.backend.dao;

import com.tkb.backend.db.DatabaseConfig;
import com.tkb.common.dto.PlanDto;
import com.tkb.common.dto.PlanDto.DroppedCourseDto;
import com.tkb.common.dto.PlanDto.MetricsDto;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B1.6 - PlanDao: Truy van va luu tru phuong an thoi khoa bieu cua sinh vien tren 3 bang:
 *   - phuong_an (id, nguoi_dung_id, ma_hoc_ky, ten, tieu_chi, diem, so_ngay_hoc, so_tiet_trong, so_tiet_sang, ngay_tao)
 *   - phuong_an_chi_tiet (phuong_an_id, nhom_lop_id, ma_hoc_ky)
 *   - phuong_an_bi_loai (phuong_an_id, ma_mh)
 *
 * Phuc vu cac lenh: SAVE_PLAN, LIST_PLANS, GET_PLAN, DELETE_PLAN va ADMIN_GET_STATS.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/dao/PlanDao.java
 */
public class PlanDao {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Kiem tra ten phuong an da ton tai trong cung mot nguoi dung chua (NAME_TAKEN).
     */
    public boolean isPlanNameTaken(long userId, String planName) throws SQLException {
        String sql = "SELECT 1 FROM phuong_an WHERE nguoi_dung_id = ? AND ten = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, planName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Luu phuong an thoi khoa bieu moi (SAVE_PLAN) trong mot Transaction gom:
     *   1. Ghi bang phuong_an
     *   2. Ghi danh sach nhom lop vao phuong_an_chi_tiet
     *   3. Ghi danh sach ma mon bi loai vao phuong_an_bi_loai
     *
     * @return ID cua phuong an vua luu (planId)
     */
    public long savePlan(
            long userId,
            String semesterCode,
            String name,
            String criteriaJson,
            double score,
            MetricsDto metrics,
            List<Long> sectionIds,
            List<String> droppedCourseCodes) throws SQLException {

        String insertPlanSql = """
                INSERT INTO phuong_an
                    (nguoi_dung_id, ma_hoc_ky, ten, tieu_chi, diem, so_ngay_hoc, so_tiet_trong, so_tiet_sang)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertDetailSql = """
                INSERT INTO phuong_an_chi_tiet (phuong_an_id, nhom_lop_id, ma_hoc_ky)
                VALUES (?, ?, ?)
                """;
        String insertDroppedSql = """
                INSERT INTO phuong_an_bi_loai (phuong_an_id, ma_mh)
                VALUES (?, ?)
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long planId;
                try (PreparedStatement ps = conn.prepareStatement(insertPlanSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, semesterCode);
                    ps.setString(3, name);
                    ps.setString(4, criteriaJson != null ? criteriaJson : "{}");
                    ps.setDouble(5, score);
                    ps.setInt(6, metrics != null ? metrics.getStudyDays() : 0);
                    ps.setInt(7, metrics != null ? metrics.getGapPeriods() : 0);
                    ps.setInt(8, metrics != null ? metrics.getMorningPeriods() : 0);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            planId = keys.getLong(1);
                        } else {
                            throw new SQLException("Khong lay duoc planId sau khi INSERT phuong_an");
                        }
                    }
                }

                if (sectionIds != null && !sectionIds.isEmpty()) {
                    try (PreparedStatement detailPs = conn.prepareStatement(insertDetailSql)) {
                        for (Long secId : sectionIds) {
                            detailPs.setLong(1, planId);
                            detailPs.setLong(2, secId);
                            detailPs.setString(3, semesterCode);
                            detailPs.addBatch();
                        }
                        detailPs.executeBatch();
                    }
                }

                if (droppedCourseCodes != null && !droppedCourseCodes.isEmpty()) {
                    try (PreparedStatement droppedPs = conn.prepareStatement(insertDroppedSql)) {
                        for (String courseCode : droppedCourseCodes) {
                            if (courseCode != null && !courseCode.isBlank()) {
                                droppedPs.setLong(1, planId);
                                droppedPs.setString(2, courseCode.trim());
                                droppedPs.addBatch();
                            }
                        }
                        droppedPs.executeBatch();
                    }
                }

                conn.commit();
                return planId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Lay danh sach tom tat cac phuong an da luu cua nguoi dung (phuc vu LIST_PLANS).
     * Neu semesterCode = null hoac rong -> tra tat ca hoc ky (ho tro xem lai hoc ky cu).
     */
    public List<Map<String, Object>> listPlansByUser(long userId, String semesterCode) throws SQLException {
        boolean filterBySemester = semesterCode != null && !semesterCode.isBlank();
        StringBuilder sql = new StringBuilder("""
                SELECT id, ten, ma_hoc_ky, diem, so_ngay_hoc, so_tiet_trong, so_tiet_sang, ngay_tao
                FROM phuong_an
                WHERE nguoi_dung_id = ?
                """);
        if (filterBySemester) {
            sql.append(" AND ma_hoc_ky = ?");
        }
        sql.append(" ORDER BY ngay_tao DESC, id DESC");

        List<Map<String, Object>> plans = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setLong(1, userId);
            if (filterBySemester) {
                ps.setString(2, semesterCode.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("name", rs.getString("ten"));
                    item.put("semesterCode", rs.getString("ma_hoc_ky"));
                    item.put("score", rs.getDouble("diem"));

                    MetricsDto metrics = new MetricsDto(
                            rs.getInt("so_ngay_hoc"),
                            rs.getInt("so_tiet_trong"),
                            rs.getInt("so_tiet_sang")
                    );
                    item.put("metrics", metrics);

                    Timestamp ts = rs.getTimestamp("ngay_tao");
                    item.put("createdAt", ts != null ? ts.toLocalDateTime().format(ISO_FORMATTER) : "");
                    plans.add(item);
                }
            }
        }
        return plans;
    }

    /**
     * Lay chi tiet mot phuong an da luu (phuc vu GET_PLAN).
     * Chi tra ve ket qua neu phuong an thuoc ve dung userId (neu cua nguoi khac -> tra ve null -> NOT_FOUND).
     */
    public PlanDto getPlanByIdAndUser(long planId, long userId) throws SQLException {
        String planSql = """
                SELECT id, ten, ma_hoc_ky, diem, so_ngay_hoc, so_tiet_trong, so_tiet_sang, ngay_tao
                FROM phuong_an
                WHERE id = ? AND nguoi_dung_id = ?
                """;
        String sectionsSql = """
                SELECT nl.id AS section_id,
                       nl.ma_hoc_ky,
                       nl.ma_mh,
                       nl.ten_mh,
                       hp.so_tc,
                       nl.nhom,
                       nl.si_so,
                       nl.giang_vien,
                       nl.ma_vc,
                       nl.ten_lop,
                       nl.dang_hoat_dong,
                       lh.id AS slot_id,
                       lh.thu,
                       lh.tiet_bd,
                       lh.so_tiet,
                       lh.phong,
                       lh.to_th,
                       lh.loai,
                       lh.tuan_hoc,
                       lh.tuan_mask
                FROM phuong_an_chi_tiet pact
                JOIN nhom_lop nl ON nl.id = pact.nhom_lop_id AND nl.ma_hoc_ky = pact.ma_hoc_ky
                JOIN hoc_phan hp ON hp.ma_mh = nl.ma_mh
                LEFT JOIN lich_hoc lh ON lh.nhom_lop_id = nl.id
                WHERE pact.phuong_an_id = ?
                ORDER BY nl.ma_mh ASC, nl.nhom ASC, lh.thu ASC, lh.tiet_bd ASC
                """;
        String droppedSql = """
                SELECT pabl.ma_mh, hp.ten
                FROM phuong_an_bi_loai pabl
                JOIN hoc_phan hp ON hp.ma_mh = pabl.ma_mh
                WHERE pabl.phuong_an_id = ?
                ORDER BY pabl.ma_mh ASC
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            PlanDto plan = new PlanDto();
            try (PreparedStatement ps = conn.prepareStatement(planSql)) {
                ps.setLong(1, planId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    plan.setId(rs.getLong("id"));
                    plan.setName(rs.getString("ten"));
                    plan.setSemesterCode(rs.getString("ma_hoc_ky"));
                    plan.setRank(1);
                    plan.setScore(rs.getDouble("diem"));
                    plan.setMetrics(new MetricsDto(
                            rs.getInt("so_ngay_hoc"),
                            rs.getInt("so_tiet_trong"),
                            rs.getInt("so_tiet_sang")
                    ));
                    Timestamp ts = rs.getTimestamp("ngay_tao");
                    plan.setCreatedAt(ts != null ? ts.toLocalDateTime().format(ISO_FORMATTER) : "");
                }
            }

            // Nap danh sach SectionDto + SlotDto
            Map<Long, SectionDto> sectionMap = new LinkedHashMap<>();
            try (PreparedStatement secPs = conn.prepareStatement(sectionsSql)) {
                secPs.setLong(1, planId);
                try (ResultSet rs = secPs.executeQuery()) {
                    while (rs.next()) {
                        long secId = rs.getLong("section_id");
                        SectionDto sec = sectionMap.get(secId);
                        if (sec == null) {
                            sec = new SectionDto();
                            sec.setId(secId);
                            sec.setSemesterCode(rs.getString("ma_hoc_ky"));
                            sec.setCourseCode(rs.getString("ma_mh"));
                            sec.setCourseName(rs.getString("ten_mh"));
                            sec.setCredits(rs.getInt("so_tc"));
                            sec.setGroupNo(rs.getString("nhom"));
                            sec.setLabGroup("");
                            sec.setType(SectionDto.TYPE_LT);
                            sec.setCapacity(rs.getInt("si_so"));
                            sec.setLecturer(rs.getString("giang_vien"));
                            sec.setLecturerCode(rs.getString("ma_vc"));
                            sec.setClassName(rs.getString("ten_lop"));
                            sec.setActive(rs.getBoolean("dang_hoat_dong"));
                            sectionMap.put(secId, sec);
                        }

                        long slotId = rs.getLong("slot_id");
                        if (!rs.wasNull() && slotId > 0) {
                            String slotType = rs.getString("loai");
                            String slotLab = rs.getString("to_th");
                            SlotDto slot = new SlotDto(
                                    rs.getInt("thu"),
                                    rs.getInt("tiet_bd"),
                                    rs.getInt("so_tiet"),
                                    rs.getString("phong"),
                                    rs.getString("tuan_hoc"),
                                    slotType,
                                    slotLab,
                                    rs.getLong("tuan_mask")
                            );
                            sec.getSlots().add(slot);
                            if (SectionDto.TYPE_TH.equalsIgnoreCase(slotType)) {
                                sec.setType(SectionDto.TYPE_TH);
                                if (slotLab != null && !slotLab.isBlank() && sec.getLabGroup().isEmpty()) {
                                    sec.setLabGroup(slotLab);
                                }
                            }
                        }
                    }
                }
            }
            plan.setSections(new ArrayList<>(sectionMap.values()));

            // Nap danh sach hoc phan bi loai
            List<DroppedCourseDto> droppedList = new ArrayList<>();
            try (PreparedStatement dropPs = conn.prepareStatement(droppedSql)) {
                dropPs.setLong(1, planId);
                try (ResultSet rs = dropPs.executeQuery()) {
                    while (rs.next()) {
                        droppedList.add(new DroppedCourseDto(rs.getString("ma_mh"), rs.getString("ten")));
                    }
                }
            }
            plan.setDroppedCourses(droppedList);

            return plan;
        }
    }

    /**
     * Xoa phuong an da luu cua chinh nguoi dung do (DELETE_PLAN).
     * Chi tiet va mon bi loai tu dong xoa theo ON DELETE CASCADE.
     */
    public boolean deletePlanByIdAndUser(long planId, long userId) throws SQLException {
        String sql = "DELETE FROM phuong_an WHERE id = ? AND nguoi_dung_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, planId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Dem tong so phuong an da luu tren toan he thong (phuc vu ADMIN_GET_STATS).
     */
    public int countTotalPlans() throws SQLException {
        String sql = "SELECT COUNT(*) FROM phuong_an";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}