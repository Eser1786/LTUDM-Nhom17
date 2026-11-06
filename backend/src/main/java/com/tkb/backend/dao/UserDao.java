package com.tkb.backend.dao;

import com.tkb.backend.db.DatabaseConfig;
import com.tkb.common.dto.UserDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * B1.6 - UserDao: Truy van va cap nhat bang nguoi_dung (id, ten_dang_nhap, mat_khau_hash, vai_tro, ngay_tao).
 * Su dung 100% PreparedStatement chong SQL Injection.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/dao/UserDao.java
 */
public class UserDao {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Ban ghi noi bo chua day du mat_khau_hash de Server kiem tra BCrypt khi LOGIN.
     */
    public record UserAuthRecord(long id, String username, String passwordHash, String role, String createdAt) {
        public UserDto toDto(boolean online) {
            return new UserDto(id, username, role, createdAt, online);
        }
    }

    /**
     * Tim tai khoan theo ten_dang_nhap (phuc vu LOGIN va REGISTER).
     */
    public UserAuthRecord findByUsername(String username) throws SQLException {
        String sql = "SELECT id, ten_dang_nhap, mat_khau_hash, vai_tro, ngay_tao FROM nguoi_dung WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
                return null;
            }
        }
    }

    /**
     * Tim tai khoan theo ID.
     */
    public UserAuthRecord findById(long userId) throws SQLException {
        String sql = "SELECT id, ten_dang_nhap, mat_khau_hash, vai_tro, ngay_tao FROM nguoi_dung WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
                return null;
            }
        }
    }

    /**
     * Tao tai khoan nguoi dung moi (REGISTER hoac khoi tao ADMIN dau tien - B4.3).
     * @return ID cua nguoi dung vua tao
     */
    public long createUser(String username, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO nguoi_dung (ten_dang_nhap, mat_khau_hash, vai_tro) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, (role != null && role.equalsIgnoreCase("ADMIN")) ? "ADMIN" : "USER");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Khong lay duoc ID nguoi dung sau khi INSERT");
            }
        }
    }

    /**
     * Danh sach nguoi dung co tim kiem va phan trang (phuc vu ADMIN_LIST_USERS).
     *
     * @param keyword       Tu khoa tim theo ten_dang_nhap (tuy chon)
     * @param page          Trang bat dau tu 0
     * @param size          Kich thuoc trang (1..100)
     * @param onlineUserIds Tap hop ID cac nguoi dung dang co ket noi socket online
     */
    public List<UserDto> listUsers(String keyword, int page, int size, Set<Long> onlineUserIds) throws SQLException {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        StringBuilder sql = new StringBuilder(
                "SELECT id, ten_dang_nhap, mat_khau_hash, vai_tro, ngay_tao FROM nguoi_dung"
        );
        if (hasKeyword) {
            sql.append(" WHERE ten_dang_nhap LIKE ?");
        }
        sql.append(" ORDER BY id ASC LIMIT ? OFFSET ?");

        List<UserDto> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasKeyword) {
                ps.setString(idx++, "%" + keyword.trim() + "%");
            }
            ps.setInt(idx++, size);
            ps.setInt(idx, page * size);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserAuthRecord rec = mapRecord(rs);
                    boolean isOnline = onlineUserIds != null && onlineUserIds.contains(rec.id());
                    users.add(rec.toDto(isOnline));
                }
            }
        }
        return users;
    }

    /**
     * Dem tong so nguoi dung khop voi tu khoa (phuc vu phan trang ADMIN_LIST_USERS va ADMIN_GET_STATS).
     */
    public int countUsers(String keyword) throws SQLException {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String sql = hasKeyword
                ? "SELECT COUNT(*) FROM nguoi_dung WHERE ten_dang_nhap LIKE ?"
                : "SELECT COUNT(*) FROM nguoi_dung";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasKeyword) {
                ps.setString(1, "%" + keyword.trim() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Doi vai tro USER / ADMIN cua nguoi dung (ADMIN_SET_USER_ROLE).
     */
    public boolean updateUserRole(long userId, String newRole) throws SQLException {
        String sql = "UPDATE nguoi_dung SET vai_tro = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ADMIN".equalsIgnoreCase(newRole) ? "ADMIN" : "USER");
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Dat lai mat khau (da bam BCrypt) cho nguoi dung (ADMIN_RESET_PASSWORD).
     */
    public boolean updatePasswordHash(long userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE nguoi_dung SET mat_khau_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Xoa nguoi dung (ADMIN_DELETE_USER). Cac phuong an da luu tu dong xoa theo ON DELETE CASCADE.
     */
    public boolean deleteUser(long userId) throws SQLException {
        String sql = "DELETE FROM nguoi_dung WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private UserAuthRecord mapRecord(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String username = rs.getString("ten_dang_nhap");
        String passwordHash = rs.getString("mat_khau_hash");
        String role = rs.getString("vai_tro");
        Timestamp ts = rs.getTimestamp("ngay_tao");
        String createdAt = (ts != null) ? ts.toLocalDateTime().format(ISO_FORMATTER) : "";
        return new UserAuthRecord(id, username, passwordHash, role, createdAt);
    }
}