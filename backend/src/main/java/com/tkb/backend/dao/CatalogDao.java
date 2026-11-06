package com.tkb.backend.dao;

import com.tkb.backend.db.DatabaseConfig;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B1.5 - CatalogDao: Truy van va cap nhat 4 bang danh muc & lich hoc:
 *   - hoc_ky (ma, ten, dang_dung)
 *   - hoc_phan (ma_mh, ten, so_tc)
 *   - nhom_lop (id, ma_hoc_ky, ma_mh, ten_mh, nhom, si_so, giang_vien, ma_vc, ten_lop, dang_hoat_dong)
 *   - lich_hoc (id, nhom_lop_id, thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_hoc, tuan_mask)
 *
 * Tat ca truy van deu su dung PreparedStatement de chong SQL Injection.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/dao/CatalogDao.java
 */
public class CatalogDao {

    // =========================================================================
    // 1. BANG hoc_ky (LIST_SEMESTERS, ADMIN_CREATE_SEMESTER, ADMIN_SET_CURRENT_SEMESTER)
    // =========================================================================

    /**
     * Lay danh sach tat ca hoc ky (hoc ky hien tai xep dau, sau do giam dan theo ma).
     * Phuc vu lenh LIST_SEMESTERS.
     */
    public List<Map<String, Object>> listSemesters() throws SQLException {
        String sql = "SELECT ma, ten, dang_dung FROM hoc_ky ORDER BY dang_dung DESC, ma DESC";
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", rs.getString("ma"));
                item.put("name", rs.getString("ten"));
                item.put("current", rs.getBoolean("dang_dung"));
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Lay ma hoc ky hien tai (dang_dung = TRUE). Tra ve null neu chua co.
     */
    public String getCurrentSemesterCode() throws SQLException {
        String sql = "SELECT ma FROM hoc_ky WHERE dang_dung = TRUE LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString("ma") : null;
        }
    }

    /**
     * Kiem tra ma hoc ky co ton tai trong bang hoc_ky hay khong.
     */
    public boolean semesterExists(String semesterCode) throws SQLException {
        String sql = "SELECT 1 FROM hoc_ky WHERE ma = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, semesterCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tao hoc ky moi (ADMIN_CREATE_SEMESTER).
     * @return false neu ma hoc ky da ton tai (SEMESTER_EXISTS)
     */
    public boolean createSemester(String code, String name) throws SQLException {
        if (semesterExists(code)) {
            return false;
        }
        String sql = "INSERT INTO hoc_ky (ma, ten, dang_dung) VALUES (?, ?, FALSE)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Đặt học kỳ hiện tại (ADMIN_SET_CURRENT_SEMESTER) trong một Transaction
     * để đảm bảo luôn chỉ có duy nhất một học kỳ có dang_dung = TRUE (tránh đụng UNIQUE co_hien_tai).
     */
    public boolean setCurrentSemester(String semesterCode) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Kiem tra ton tai
                try (PreparedStatement checkPs = conn.prepareStatement("SELECT 1 FROM hoc_ky WHERE ma = ? FOR UPDATE")) {
                    checkPs.setString(1, semesterCode);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                // Tat hoc ky hien tai cu truoc
                try (PreparedStatement clearPs = conn.prepareStatement("UPDATE hoc_ky SET dang_dung = FALSE WHERE dang_dung = TRUE")) {
                    clearPs.executeUpdate();
                }
                // Bat hoc ky moi
                try (PreparedStatement setPs = conn.prepareStatement("UPDATE hoc_ky SET dang_dung = TRUE WHERE ma = ?")) {
                    setPs.setString(1, semesterCode);
                    setPs.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =========================================================================
    // 2. BANG hoc_phan (LIST_COURSES, ADMIN_LIST_COURSES, ADMIN_UPSERT_COURSE, ADMIN_DELETE_COURSE)
    // =========================================================================

    /**
     * Danh sach hoc phan co it nhat 1 nhom lop dang hoat dong trong hoc ky (phuc vu LIST_COURSES cua User).
     */
    public List<Map<String, Object>> listOpenCourses(String semesterCode, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT hp.ma_mh, hp.ten, hp.so_tc
                FROM hoc_phan hp
                WHERE EXISTS (
                    SELECT 1 FROM nhom_lop nl
                    WHERE nl.ma_mh = hp.ma_mh
                      AND nl.ma_hoc_ky = ?
                      AND nl.dang_hoat_dong = TRUE
                )
                """);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" AND (hp.ma_mh LIKE ? OR hp.ten LIKE ?)");
        }
        sql.append(" ORDER BY hp.ma_mh ASC");

        List<Map<String, Object>> courses = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, semesterCode);
            if (hasKeyword) {
                String pattern = "%" + keyword.trim() + "%";
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("courseCode", rs.getString("ma_mh"));
                    c.put("name", rs.getString("ten"));
                    c.put("credits", rs.getInt("so_tc"));
                    courses.add(c);
                }
            }
        }
        return courses;
    }

    /**
     * Danh sach tat ca hoc phan kem so luong nhom lop trong hoc ky (phuc vu ADMIN_LIST_COURSES).
     */
    public List<Map<String, Object>> listCoursesForAdmin(String semesterCode, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT hp.ma_mh,
                       hp.ten,
                       hp.so_tc,
                       COUNT(nl.id) AS section_count,
                       SUM(CASE WHEN nl.dang_hoat_dong = TRUE THEN 1 ELSE 0 END) AS active_section_count
                FROM hoc_phan hp
                LEFT JOIN nhom_lop nl ON nl.ma_mh = hp.ma_mh AND nl.ma_hoc_ky = ?
                """);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" WHERE (hp.ma_mh LIKE ? OR hp.ten LIKE ?)");
        }
        sql.append(" GROUP BY hp.ma_mh, hp.ten, hp.so_tc ORDER BY hp.ma_mh ASC");

        List<Map<String, Object>> courses = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, semesterCode);
            if (hasKeyword) {
                String pattern = "%" + keyword.trim() + "%";
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("courseCode", rs.getString("ma_mh"));
                    c.put("name", rs.getString("ten"));
                    c.put("credits", rs.getInt("so_tc"));
                    c.put("sectionCount", rs.getInt("section_count"));
                    c.put("activeSectionCount", rs.getInt("active_section_count"));
                    courses.add(c);
                }
            }
        }
        return courses;
    }

    /**
     * Them moi hoac cap nhat hoc phan (ADMIN_UPSERT_COURSE).
     * Dong thoi cap nhat ten_mh trong bang nhom_lop neu ten hoc phan thay doi.
     */
    public void upsertCourse(String courseCode, String name, int credits) throws SQLException {
        String upsertSql = """
                INSERT INTO hoc_phan (ma_mh, ten, so_tc)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE ten = VALUES(ten), so_tc = VALUES(so_tc)
                """;
        String syncSectionNameSql = "UPDATE nhom_lop SET ten_mh = ? WHERE ma_mh = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(upsertSql);
                 PreparedStatement ps2 = conn.prepareStatement(syncSectionNameSql)) {
                ps1.setString(1, courseCode);
                ps1.setString(2, name);
                ps1.setInt(3, credits);
                ps1.executeUpdate();

                ps2.setString(1, name);
                ps2.setString(2, courseCode);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Kiem tra hoc phan co dang duoc su dung trong nhom_lop hoac phuong_an_bi_loai hay khong (COURSE_IN_USE).
     */
    public boolean isCourseInUse(String courseCode) throws SQLException {
        String sql = """
                SELECT 1 FROM nhom_lop WHERE ma_mh = ?
                UNION ALL
                SELECT 1 FROM phuong_an_bi_loai WHERE ma_mh = ?
                LIMIT 1
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Xoa hoc phan (ADMIN_DELETE_COURSE). Tra ve false neu khong tim thay ma hoc phan.
     */
    public boolean deleteCourse(String courseCode) throws SQLException {
        String sql = "DELETE FROM hoc_phan WHERE ma_mh = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // 3. BANG nhom_lop & lich_hoc (B5.1 Cache, GET_SECTIONS, ADMIN_*_SECTION)
    // =========================================================================

    /**
     * Nap toan bo nhom lop + danh sach buoi hoc (lich_hoc) cua mot hoc ky.
     * Su dung JOIN 1 lan duy nhat de nap cuc nhanh len Cache RAM (B5.1).
     *
     * @param semesterCode    Ma hoc ky (vd: "2026-2027-1")
     * @param includeInactive true neu lay ca nhom da tat (Admin), false neu chi lay nhom dang hoat dong
     */
    public List<SectionDto> loadSectionsBySemester(String semesterCode, boolean includeInactive) throws SQLException {
        return loadSectionsInternal(semesterCode, null, includeInactive);
    }

    /**
     * Lay danh sach nhom lop cua 1 hoc phan cu the trong hoc ky (phuc vu GET_SECTIONS).
     */
    public List<SectionDto> getSectionsByCourse(String semesterCode, String courseCode, boolean includeInactive) throws SQLException {
        return loadSectionsInternal(semesterCode, courseCode, includeInactive);
    }

    private List<SectionDto> loadSectionsInternal(String semesterCode, String courseCode, boolean includeInactive) throws SQLException {
        StringBuilder sql = new StringBuilder("""
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
                FROM nhom_lop nl
                JOIN hoc_phan hp ON hp.ma_mh = nl.ma_mh
                LEFT JOIN lich_hoc lh ON lh.nhom_lop_id = nl.id
                WHERE nl.ma_hoc_ky = ?
                """);
        if (courseCode != null && !courseCode.isBlank()) {
            sql.append(" AND nl.ma_mh = ?");
        }
        if (!includeInactive) {
            sql.append(" AND nl.dang_hoat_dong = TRUE");
        }
        sql.append(" ORDER BY nl.ma_mh ASC, nl.nhom ASC, lh.thu ASC, lh.tiet_bd ASC, lh.to_th ASC");

        Map<Long, SectionDto> sectionMap = new LinkedHashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, semesterCode);
            if (courseCode != null && !courseCode.isBlank()) {
                ps.setString(idx, courseCode.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long sectionId = rs.getLong("section_id");
                    SectionDto section = sectionMap.get(sectionId);
                    if (section == null) {
                        section = new SectionDto();
                        section.setId(sectionId);
                        section.setSemesterCode(rs.getString("ma_hoc_ky"));
                        section.setCourseCode(rs.getString("ma_mh"));
                        section.setCourseName(rs.getString("ten_mh"));
                        section.setCredits(rs.getInt("so_tc"));
                        section.setGroupNo(rs.getString("nhom"));
                        section.setLabGroup("");
                        section.setType(SectionDto.TYPE_LT);
                        section.setCapacity(rs.getInt("si_so"));
                        section.setLecturer(rs.getString("giang_vien"));
                        section.setLecturerCode(rs.getString("ma_vc"));
                        section.setClassName(rs.getString("ten_lop"));
                        section.setActive(rs.getBoolean("dang_hoat_dong"));
                        sectionMap.put(sectionId, section);
                    }

                    long slotId = rs.getLong("slot_id");
                    if (!rs.wasNull() && slotId > 0) {
                        String slotType = rs.getString("loai");
                        String slotLabGroup = rs.getString("to_th");
                        String weeks = rs.getString("tuan_hoc");
                        long weekMask = rs.getLong("tuan_mask");

                        SlotDto slot = new SlotDto(
                                rs.getInt("thu"),
                                rs.getInt("tiet_bd"),
                                rs.getInt("so_tiet"),
                                rs.getString("phong"),
                                weeks,
                                slotType,
                                slotLabGroup,
                                weekMask
                        );
                        section.getSlots().add(slot);

                        // Neu buoi hoc co to thuc hanh (TH), cap nhat thong tinTH cho SectionDto
                        if (SectionDto.TYPE_TH.equalsIgnoreCase(slotType)) {
                            section.setType(SectionDto.TYPE_TH);
                            if (slotLabGroup != null && !slotLabGroup.isBlank() && section.getLabGroup().isEmpty()) {
                                section.setLabGroup(slotLabGroup);
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(sectionMap.values());
    }

    /**
     * Them moi hoac cap nhat nhom lop kem danh sach buoi hoc (ADMIN_UPSERT_SECTION) trong 1 Transaction.
     * @return ID cua nhom lop sau khi luu
     */
    public long upsertSection(SectionDto section) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lay ten hoc phan tu bang hoc_phan
                String courseName = section.getCourseName();
                if (courseName == null || courseName.isBlank()) {
                    try (PreparedStatement cp = conn.prepareStatement("SELECT ten FROM hoc_phan WHERE ma_mh = ?")) {
                        cp.setString(1, section.getCourseCode());
                        try (ResultSet rs = cp.executeQuery()) {
                            if (rs.next()) {
                                courseName = rs.getString("ten");
                            } else {
                                throw new SQLException("NOT_FOUND: Khong tim thay hoc phan " + section.getCourseCode());
                            }
                        }
                    }
                }

                long sectionId;
                if (section.getId() != null && section.getId() > 0) {
                    // Cap nhat nhom lop hien co
                    sectionId = section.getId();
                    String updateSql = """
                            UPDATE nhom_lop
                            SET ma_hoc_ky = ?, ma_mh = ?, ten_mh = ?, nhom = ?, si_so = ?,
                                giang_vien = ?, ma_vc = ?, ten_lop = ?
                            WHERE id = ?
                            """;
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, section.getSemesterCode());
                        ps.setString(2, section.getCourseCode());
                        ps.setString(3, courseName);
                        ps.setString(4, section.getGroupNo());
                        ps.setInt(5, section.getCapacity());
                        ps.setString(6, section.getLecturer());
                        ps.setString(7, section.getLecturerCode());
                        ps.setString(8, section.getClassName());
                        ps.setLong(9, sectionId);
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("NOT_FOUND: Khong tim thay nhom lop id=" + sectionId);
                        }
                    }
                    // Xoa cac lich_hoc cu de thay bang danh sach slots moi
                    try (PreparedStatement delSlots = conn.prepareStatement("DELETE FROM lich_hoc WHERE nhom_lop_id = ?")) {
                        delSlots.setLong(1, sectionId);
                        delSlots.executeUpdate();
                    }
                } else {
                    // Them moi nhom lop
                    String insertSql = """
                            INSERT INTO nhom_lop (ma_hoc_ky, ma_mh, ten_mh, nhom, si_so, giang_vien, ma_vc, ten_lop, dang_hoat_dong)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                            """;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, section.getSemesterCode());
                        ps.setString(2, section.getCourseCode());
                        ps.setString(3, courseName);
                        ps.setString(4, section.getGroupNo());
                        if (section.getCapacity() > 0) {
                            ps.setInt(5, section.getCapacity());
                        } else {
                            ps.setNull(5, Types.INTEGER);
                        }
                        ps.setString(6, section.getLecturer());
                        ps.setString(7, section.getLecturerCode());
                        ps.setString(8, section.getClassName());
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                sectionId = keys.getLong(1);
                            } else {
                                throw new SQLException("Khong lay duoc ID nhom lop vua tao");
                            }
                        }
                    }
                }

                // Ghi danh sach buoi hoc (lich_hoc)
                if (section.getSlots() != null && !section.getSlots().isEmpty()) {
                    String slotSql = """
                            INSERT INTO lich_hoc (nhom_lop_id, thu, tiet_bd, so_tiet, phong, to_th, loai, tuan_hoc, tuan_mask)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement slotPs = conn.prepareStatement(slotSql)) {
                        for (SlotDto s : section.getSlots()) {
                            String slotType = (s.getType() != null && !s.getType().isBlank()) ? s.getType() : section.getType();
                            String slotLab = (s.getLabGroup() != null && !s.getLabGroup().isBlank()) ? s.getLabGroup() : section.getLabGroup();
                            if (SectionDto.TYPE_LT.equalsIgnoreCase(slotType)) {
                                slotLab = "";
                            }
                            long mask = s.getWeekMask();

                            slotPs.setLong(1, sectionId);
                            slotPs.setInt(2, s.getDayOfWeek());
                            slotPs.setInt(3, s.getStartPeriod());
                            slotPs.setInt(4, s.getPeriodCount());
                            slotPs.setString(5, s.getRoom());
                            slotPs.setString(6, slotLab != null ? slotLab : "");
                            slotPs.setString(7, SectionDto.TYPE_TH.equalsIgnoreCase(slotType) ? "TH" : "LT");
                            slotPs.setString(8, s.getWeeks());
                            slotPs.setLong(9, mask);
                            slotPs.addBatch();
                        }
                        slotPs.executeBatch();
                    }
                }

                conn.commit();
                return sectionId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Bat / tat trang thai hoat dong cua nhom lop (ADMIN_SET_SECTION_ACTIVE).
     */
    public boolean setSectionActive(long sectionId, boolean active) throws SQLException {
        String sql = "UPDATE nhom_lop SET dang_hoat_dong = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, sectionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Kiem tra nhom lop co dang nam trong phuong an da luu cua sinh vien khong (SECTION_IN_USE).
     */
    public boolean isSectionInUse(long sectionId) throws SQLException {
        String sql = "SELECT 1 FROM phuong_an_chi_tiet WHERE nhom_lop_id = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Xoa nhom lop (ADMIN_DELETE_SECTION). Lich hoc (lich_hoc) se tu dong xoa theo ON DELETE CASCADE.
     */
    public boolean deleteSection(long sectionId) throws SQLException {
        String sql = "DELETE FROM nhom_lop WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sectionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Dem tong so nhom lop dang hoat dong (phuc vu ADMIN_GET_STATS).
     */
    public int countTotalActiveSections() throws SQLException {
        String sql = "SELECT COUNT(*) FROM nhom_lop WHERE dang_hoat_dong = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}