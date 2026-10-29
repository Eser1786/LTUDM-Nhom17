package com.tkb.common.protocol;

/**
 * B2.3 - Enum Command: Danh sach tat ca cac lenh Request va goi Push theo trang Protocol (Muc 5, 6, 7).
 * Moi lenh dinh kem quyen truy cap toi thieu (RequiredRole) de Dispatcher tren Server
 * kiem tra quyen tu dong (B3.2).
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/Command.java
 */
public enum Command {

    // --- 5. Lenh dung chung ---
    PING(RequiredRole.NONE),
    REGISTER(RequiredRole.NONE),
    LOGIN(RequiredRole.NONE),
    LOGOUT(RequiredRole.USER),
    GET_SECTIONS(RequiredRole.USER),

    // --- 5. Goi Push do Server chu dong day ve Client ---
    JOB_PROGRESS(RequiredRole.USER),
    JOB_DONE(RequiredRole.USER),

    // --- 6. Lenh trang User (Quy) ---
    LIST_SEMESTERS(RequiredRole.USER),
    LIST_COURSES(RequiredRole.USER),
    OPTIMIZE(RequiredRole.USER),
    CANCEL_JOB(RequiredRole.USER),
    GET_JOB(RequiredRole.USER),
    SAVE_PLAN(RequiredRole.USER),
    LIST_PLANS(RequiredRole.USER),
    GET_PLAN(RequiredRole.USER),
    DELETE_PLAN(RequiredRole.USER),

    // --- 7. Lenh trang Admin (Tran) - Quyen ADMIN ---
    ADMIN_LIST_USERS(RequiredRole.ADMIN),
    ADMIN_SET_USER_ROLE(RequiredRole.ADMIN),
    ADMIN_RESET_PASSWORD(RequiredRole.ADMIN),
    ADMIN_DELETE_USER(RequiredRole.ADMIN),
    ADMIN_CREATE_SEMESTER(RequiredRole.ADMIN),
    ADMIN_SET_CURRENT_SEMESTER(RequiredRole.ADMIN),
    ADMIN_LIST_COURSES(RequiredRole.ADMIN),
    ADMIN_UPSERT_COURSE(RequiredRole.ADMIN),
    ADMIN_DELETE_COURSE(RequiredRole.ADMIN),
    ADMIN_UPSERT_SECTION(RequiredRole.ADMIN),
    ADMIN_SET_SECTION_ACTIVE(RequiredRole.ADMIN),
    ADMIN_DELETE_SECTION(RequiredRole.ADMIN),
    IMPORT_BEGIN(RequiredRole.ADMIN),
    IMPORT_CHUNK(RequiredRole.ADMIN),
    IMPORT_COMMIT(RequiredRole.ADMIN),
    IMPORT_ABORT(RequiredRole.ADMIN),
    ADMIN_LIST_JOBS(RequiredRole.ADMIN),
    ADMIN_CANCEL_JOB(RequiredRole.ADMIN),
    ADMIN_GET_STATS(RequiredRole.ADMIN),
    ADMIN_LIST_SESSIONS(RequiredRole.ADMIN),
    ADMIN_KICK_SESSION(RequiredRole.ADMIN);

    /**
     * Quyen toi thieu de goi lenh:
     *   - NONE (-): Chua dang nhap cung goi duoc (PING, REGISTER, LOGIN)
     *   - USER (U): Da dang nhap (USER hoac ADMIN deu goi duoc)
     *   - ADMIN (A): Chi tai khoan ADMIN moi goi duoc
     */
    public enum RequiredRole {
        NONE,
        USER,
        ADMIN
    }

    private final RequiredRole requiredRole;

    Command(RequiredRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    public RequiredRole getRequiredRole() {
        return requiredRole;
    }

    /**
     * Tim Command tu chuoi cmd trong JSON (tra ve null neu lenh khong ton tai -> BAD_REQUEST).
     */
    public static Command fromString(String cmd) {
        if (cmd == null || cmd.isBlank()) {
            return null;
        }
        try {
            return Command.valueOf(cmd.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}