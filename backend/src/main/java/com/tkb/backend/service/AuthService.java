package com.tkb.backend.service;

import com.tkb.backend.dao.UserDao;
import com.tkb.backend.dao.UserDao.UserAuthRecord;
import com.tkb.common.dto.UserDto;
import com.tkb.common.protocol.ErrorCode;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * B4.1, B4.2, B4.3, B4.4 - AuthService: Xu ly dang ky, dang nhap, bam mat khau BCrypt,
 * kiem tra dinh dang dau vao (Muc 9 Protocol), khoi tao tai khoan ADMIN dau tien,
 * va chan dang nhap sai nhieu lan lien tiep (Brute-force protection).
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/service/AuthService.java
 */
public class AuthService {

    /** Quy tac Muc 9 Protocol: username tu 3 den 30 ky tu, chi gom chu cai, chu so va dau gach duoi */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final int MIN_PASSWORD_LEN = 8;
    private static final int MAX_PASSWORD_LEN = 64;
    private static final int BCRYPT_LOG_ROUNDS = 10;

    /** B4.4 (Diem cong): Gioi han dang nhap sai tối đa 5 lan lien tiep -> khoa tam 60 giay */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 60_000L;

    private final UserDao userDao;
    private final Map<String, FailedLoginTracker> failedTrackers = new ConcurrentHashMap<>();

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Ket qua xu ly xac thuc (thanh cong hoac kem ma loi ErrorCode + ten truong vi pham).
     */
    public record AuthResult(boolean ok, UserDto user, ErrorCode errorCode, String errorMessage, String invalidField) {
        public static AuthResult success(UserDto user) {
            return new AuthResult(true, user, null, null, null);
        }

        public static AuthResult fail(ErrorCode code, String message) {
            return new AuthResult(false, null, code, message, null);
        }

        public static AuthResult validationFail(String field, String message) {
            return new AuthResult(false, null, ErrorCode.VALIDATION_FAILED, message, field);
        }
    }

    /**
     * B4.3 - Đảm bảo tài khoản ADMIN đầu tiên luôn tồn tại khi Server khởi động.
     * Đọc từ biến môi trường ADMIN_USER / ADMIN_PASS (hoặc mặc định: admin / admin12345)
     * để Trân có thể dùng đăng nhập vào trang Admin ngay lập tức.
     */
    public void ensureDefaultAdminExists() {
        String adminUser = getEnvOrDefault("ADMIN_USER", "admin");
        String adminPass = getEnvOrDefault("ADMIN_PASS", "admin12345");
        try {
            UserAuthRecord existing = userDao.findByUsername(adminUser);
            if (existing == null) {
                String hash = hashPassword(adminPass);
                long id = userDao.createUser(adminUser, hash, UserDto.ROLE_ADMIN);
                System.out.printf("[AuthService] Da tao tai khoan ADMIN mac dinh: username='%s' (id=%d)%n", adminUser, id);
            } else if (!UserDto.ROLE_ADMIN.equalsIgnoreCase(existing.role())) {
                userDao.updateUserRole(existing.id(), UserDto.ROLE_ADMIN);
                System.out.printf("[AuthService] Da nang quyen ADMIN cho tai khoan '%s'%n", adminUser);
            }
        } catch (SQLException e) {
            System.err.println("[AuthService] Loi khi kiem tra/tao tai khoan ADMIN mac dinh: " + e.getMessage());
        }
    }

    /**
     * B4.1 & B4.2 - Dang ky tai khoan moi (REGISTER).
     */
    public AuthResult register(String username, String password) throws SQLException {
        String cleanUser = (username != null) ? username.trim() : "";
        AuthResult validation = validateCredentialsFormat(cleanUser, password, "password");
        if (!validation.ok()) {
            return validation;
        }

        synchronized (this) {
            UserAuthRecord existing = userDao.findByUsername(cleanUser);
            if (existing != null) {
                return AuthResult.fail(ErrorCode.USERNAME_TAKEN, "Tên đăng nhập '" + cleanUser + "' đã được sử dụng.");
            }
            String passwordHash = hashPassword(password);
            long newUserId = userDao.createUser(cleanUser, passwordHash, UserDto.ROLE_USER);
            UserDto dto = new UserDto(newUserId, cleanUser, UserDto.ROLE_USER, "", false);
            return AuthResult.success(dto);
        }
    }

    /**
     * B4.1 & B4.4 - Dang nhap (LOGIN) voi kiem tra BCrypt va chan Brute-force.
     */
    public AuthResult login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return AuthResult.fail(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getDefaultMessage());
        }
        String cleanUser = username.trim();
        String trackerKey = cleanUser.toLowerCase();

        // Kiem tra xem tai khoan co dang bi tam khoa do nhap sai nhieu lan khong (B4.4)
        FailedLoginTracker tracker = failedTrackers.get(trackerKey);
        if (tracker != null && tracker.isLocked()) {
            long remainSec = Math.max(1L, (tracker.lockedUntilMs - System.currentTimeMillis()) / 1000L);
            return AuthResult.fail(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Tài khoản tạm khóa do đăng nhập sai quá " + MAX_FAILED_ATTEMPTS + " lần. Vui lòng thử lại sau " + remainSec + " giây."
            );
        }

        UserAuthRecord record = userDao.findByUsername(cleanUser);
        if (record == null || !verifyPassword(password, record.passwordHash())) {
            recordFailedAttempt(trackerKey);
            return AuthResult.fail(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getDefaultMessage());
        }

        // Dang nhap thanh cong -> xoa bo dem loi
        failedTrackers.remove(trackerKey);
        return AuthResult.success(record.toDto(true));
    }

    /**
     * Kiem tra dinh dang username va password theo quy tac Muc 9 trang Protocol.
     */
    public AuthResult validateCredentialsFormat(String username, String password, String passwordFieldName) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return AuthResult.validationFail(
                    "username",
                    "Tên đăng nhập phải từ 3 đến 30 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới (_)."
            );
        }
        return validatePasswordFormat(password, passwordFieldName);
    }

    /**
     * Kiem tra dinh dang mat khau (8..64 ky tu) dung cho REGISTER va ADMIN_RESET_PASSWORD.
     */
    public AuthResult validatePasswordFormat(String password, String fieldName) {
        if (password == null || password.length() < MIN_PASSWORD_LEN || password.length() > MAX_PASSWORD_LEN) {
            return AuthResult.validationFail(
                    fieldName != null ? fieldName : "password",
                    "Mật khẩu phải có độ dài từ 8 đến 64 ký tự."
            );
        }
        return AuthResult.success(null);
    }

    /**
     * Bam mat khau bang BCrypt.
     */
    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * Kiem tra mat khau ro voi chuoi hash BCrypt trong CSDL.
     */
    public boolean verifyPassword(String plainPassword, String bcryptHash) {
        if (plainPassword == null || bcryptHash == null || bcryptHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, bcryptHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void recordFailedAttempt(String key) {
        failedTrackers.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || (existing.lockedUntilMs > 0 && now >= existing.lockedUntilMs)) {
                existing = new FailedLoginTracker();
            }
            existing.attempts++;
            if (existing.attempts >= MAX_FAILED_ATTEMPTS) {
                existing.lockedUntilMs = now + LOCKOUT_DURATION_MS;
            }
            return existing;
        });
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        return (val != null && !val.isBlank()) ? val.trim() : defaultValue;
    }

    private static final class FailedLoginTracker {
        int attempts = 0;
        long lockedUntilMs = 0L;

        boolean isLocked() {
            return lockedUntilMs > System.currentTimeMillis();
        }
    }
}