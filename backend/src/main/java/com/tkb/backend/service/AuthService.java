package com.tkb.backend.service;

import com.tkb.backend.dao.OtpDao;
import com.tkb.backend.dao.OtpDao.OtpRecord;
import com.tkb.backend.dao.UserDao;
import com.tkb.backend.dao.UserDao.UserAuthRecord;
import com.tkb.common.dto.UserDto;
import com.tkb.common.protocol.ErrorCode;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
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
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LEN = 8;
    private static final int MAX_PASSWORD_LEN = 64;
    private static final int BCRYPT_LOG_ROUNDS = 10;

    /** B4.4 (Diem cong): Gioi han dang nhap sai tối đa 5 lan lien tiep -> khoa tam 60 giay */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 60_000L;

    /** Otp thay đổi mật khẩu */
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final OtpDao otpDao = new OtpDao();
    private final EmailOtpService emailOtpService = new EmailOtpService();

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

        public static AuthResult success(UserDto user, String message) {
            return new AuthResult(true, user, null, message, null);
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

    public void ensureDefaultUserExists() {
        String userUser = getEnvOrDefault("USER_USER", "student");
        String userPass = getEnvOrDefault("USER_PASS", "student12345");
        try {
            UserAuthRecord existing = userDao.findByUsername(userUser);
            if (existing == null) {
                String hash = hashPassword(userPass);
                long id = userDao.createUser(userUser, hash, UserDto.ROLE_USER);
                System.out.printf("[AuthService] Da tao tai khoan USER mac dinh: username='%s' (id=%d)%n", userUser, id);
            } else if (!UserDto.ROLE_USER.equalsIgnoreCase(existing.role())) {
                userDao.updateUserRole(existing.id(), UserDto.ROLE_USER);
                System.out.printf("[AuthService] Da dat lai quyen USER cho tai khoan '%s'%n", userUser);
            }
        } catch (SQLException e) {
            System.err.println("[AuthService] Loi khi kiem tra/tao tai khoan USER mac dinh: " + e.getMessage());
        }
    }

    /**
     * B4.1 & B4.2 - Dang ky tai khoan moi (REGISTER).
     */
    public AuthResult register(String username, String password) throws SQLException {
        return register(username, null, password);
    }

    public AuthResult register(String username, String email, String password) throws SQLException {
        String cleanUser = (username != null) ? username.trim() : "";
        String cleanEmail = (email != null && !email.isBlank()) ? email.trim() : null;

        AuthResult validation = validateCredentialsFormat(cleanUser, password, "password");
        if (!validation.ok()) {
            return validation;
        }
        if (cleanEmail != null && !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            return AuthResult.validationFail("email", "Địa chỉ Email không đúng định dạng.");
        }

        synchronized (this) {
            UserAuthRecord existing = userDao.findByUsername(cleanUser);
            if (existing != null) {
                return AuthResult.fail(ErrorCode.USERNAME_TAKEN, "Tên đăng nhập '" + cleanUser + "' đã được sử dụng.");
            }
            String passwordHash = hashPassword(password);
            long newUserId = userDao.createUser(cleanUser, cleanEmail, passwordHash, UserDto.ROLE_USER);
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

    public AuthResult requestPasswordResetOtp(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return AuthResult.validationFail("username", "Vui lòng nhập tên đăng nhập.");
        }
        String cleanUser = username.trim();
        UserAuthRecord user = userDao.findByUsername(cleanUser);
        if (user == null) {
            return AuthResult.fail(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản '" + cleanUser + "'.");
        }

        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpDao.saveNewOtp(user.id(), otpCode, expiresAt);

        System.out.printf("[OTP-SERVICE] Tai khoan: '%s' | Ma OTP: %s%n", cleanUser, otpCode);

        String targetEmail = user.email();
        if (emailOtpService.hasValidTarget(targetEmail)) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    emailOtpService.sendOtpEmail(cleanUser, targetEmail, otpCode, OTP_EXPIRY_MINUTES);
                } catch (Exception e) {
                    System.err.println("[OTP-SERVICE] Gui Gmail that bai, chuyen sang dung OTP Console: " + e.getMessage());
                }
            });
            return AuthResult.success(
                    user.toDto(false),
                    "Mã OTP 6 số đang được gửi tới Gmail của bạn (hiệu lực 5 phút)."
            );
        }

        return AuthResult.success(
                user.toDto(false),
                "Mã OTP 6 số đã được tạo (xem trên Console Server). Hiệu lực trong 5 phút."
        );
    }

    public AuthResult resetPasswordWithOtp(String username, String otpInput, String newPassword) throws SQLException {
        if (username == null || username.isBlank()) {
            return AuthResult.validationFail("username", "Vui lòng nhập tên đăng nhập.");
        }
        if (otpInput == null || !otpInput.trim().matches("^\\d{6}$")) {
            return AuthResult.validationFail("otp", "Mã OTP phải gồm đúng 6 chữ số.");
        }
        AuthResult passCheck = validatePasswordFormat(newPassword, "newPassword");
        if (!passCheck.ok()) {
            return passCheck;
        }

        String cleanUser = username.trim();
        UserAuthRecord user = userDao.findByUsername(cleanUser);
        if (user == null) {
            return AuthResult.fail(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản '" + cleanUser + "'.");
        }

        OtpRecord record = otpDao.findLatestActiveOtp(user.id());
        if (record == null || LocalDateTime.now().isAfter(record.expiresAt())) {
            if (record != null) {
                otpDao.markOtpUsed(record.id());
            }
            return AuthResult.fail(ErrorCode.OTP_EXPIRED, ErrorCode.OTP_EXPIRED.getDefaultMessage());
        }

        if (record.failedAttempts() >= MAX_OTP_ATTEMPTS) {
            otpDao.markOtpUsed(record.id());
            return AuthResult.fail(ErrorCode.OTP_TOO_MANY_ATTEMPTS, ErrorCode.OTP_TOO_MANY_ATTEMPTS.getDefaultMessage());
        }

        if (!record.otpCode().equals(otpInput.trim())) {
            int nextAttempts = record.failedAttempts() + 1;
            boolean lockedOut = nextAttempts >= MAX_OTP_ATTEMPTS;
            otpDao.incrementFailedAttempts(record.id(), lockedOut);
            if (lockedOut) {
                return AuthResult.fail(ErrorCode.OTP_TOO_MANY_ATTEMPTS, ErrorCode.OTP_TOO_MANY_ATTEMPTS.getDefaultMessage());
            }
            return AuthResult.fail(
                    ErrorCode.OTP_INVALID,
                    "Mã OTP không chính xác (còn " + (MAX_OTP_ATTEMPTS - nextAttempts) + " lần thử)."
            );
        }

        // OTP hợp lệ -> Cập nhật mật khẩu mới và vô hiệu hóa OTP
        String newHash = hashPassword(newPassword);
        userDao.updatePasswordHash(user.id(), newHash);
        otpDao.markOtpUsed(record.id());
        failedTrackers.remove(cleanUser.toLowerCase()); // Mở khóa brute-force login nếu có

        return AuthResult.success(user.toDto(false));
    }
}