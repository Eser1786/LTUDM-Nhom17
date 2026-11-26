package com.tkb.backend.dao;

import com.tkb.backend.db.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class OtpDao {

    public record OtpRecord(long id, long userId, String otpCode, LocalDateTime expiresAt, int failedAttempts, boolean used) {}

    /**
     * Vô hiệu hóa tất cả OTP cũ chưa dùng của user và lưu mã OTP mới.
     */
    public void saveNewOtp(long userId, String otpCode, LocalDateTime expiresAt) throws SQLException {
        String invalidateSql = "UPDATE password_reset_otps SET da_su_dung = TRUE WHERE nguoi_dung_id = ? AND da_su_dung = FALSE";
        String insertSql = "INSERT INTO password_reset_otps (nguoi_dung_id, otp_code, het_han_luc) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(invalidateSql);
                 PreparedStatement ps2 = conn.prepareStatement(insertSql)) {
                ps1.setLong(1, userId);
                ps1.executeUpdate();

                ps2.setLong(1, userId);
                ps2.setString(2, otpCode);
                ps2.setTimestamp(3, Timestamp.valueOf(expiresAt));
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
     * Lấy bản ghi OTP mới nhất chưa sử dụng của user.
     */
    public OtpRecord findLatestActiveOtp(long userId) throws SQLException {
        String sql = "SELECT id, nguoi_dung_id, otp_code, het_han_luc, so_lan_sai, da_su_dung "
                   + "FROM password_reset_otps WHERE nguoi_dung_id = ? AND da_su_dung = FALSE "
                   + "ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new OtpRecord(
                            rs.getLong("id"),
                            rs.getLong("nguoi_dung_id"),
                            rs.getString("otp_code"),
                            rs.getTimestamp("het_han_luc").toLocalDateTime(),
                            rs.getInt("so_lan_sai"),
                            rs.getBoolean("da_su_dung")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Tăng bộ đếm nhập sai; nếu đạt ngưỡng tối đa thì tự động vô hiệu hóa OTP.
     */
    public void incrementFailedAttempts(long otpId, boolean invalidate) throws SQLException {
        String sql = invalidate
                ? "UPDATE password_reset_otps SET so_lan_sai = so_lan_sai + 1, da_su_dung = TRUE WHERE id = ?"
                : "UPDATE password_reset_otps SET so_lan_sai = so_lan_sai + 1 WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, otpId);
            ps.executeUpdate();
        }
    }

    /**
     * Đánh dấu mã OTP đã được sử dụng thành công.
     */
    public void markOtpUsed(long otpId) throws SQLException {
        String sql = "UPDATE password_reset_otps SET da_su_dung = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, otpId);
            ps.executeUpdate();
        }
    }
}