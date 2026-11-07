package com.tkb.backend.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.tkb.backend.TkbServer;
import com.tkb.backend.dao.CatalogDao;
import com.tkb.backend.dao.PlanDao;
import com.tkb.backend.dao.UserDao;
import com.tkb.backend.service.AuthService;
import com.tkb.backend.service.AuthService.AuthResult;
import com.tkb.backend.service.CatalogCache;
import com.tkb.backend.service.JobManager;
import com.tkb.backend.service.PdfImportService;
import com.tkb.backend.service.PdfImportService.UploadActionResult;
import com.tkb.backend.solver.TimetableSolver;
import com.tkb.backend.solver.TimetableSolver.Criteria;
import com.tkb.backend.solver.TimetableSolver.SolveOutcome;
import com.tkb.common.crypto.CryptoUtil;
import com.tkb.common.dto.ImportReport;
import com.tkb.common.dto.JobDto;
import com.tkb.common.dto.PlanDto;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import com.tkb.common.dto.UserDto;
import com.tkb.common.net.FrameCodec;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Command.RequiredRole;
import com.tkb.common.protocol.ErrorCode;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;

/**
 * B3.1, B3.2, B3.3, B3.4, B5.2, B8.1, B8.2, B9.1 -> B9.9 - ClientSession:
 * Xu ly toan bo vong doi cua 1 ket noi Client tren 1 luong rieng:
 *   1. Bat tay 3 buoc: HELLO (RSA PublicKey) -> KEY_EXCHANGE (RSA-OAEP) -> READY (AES-256-GCM).
 *   2. Timeout 5 phut khong co goi tin nao thi tu dong dong ket noi.
 *   3. Giai ma Request AES-GCM, kiem tra quyen (-, U, A), dieu phoi (Dispatch) tat ca 32 lenh.
 *   4. Ho tro day du cac tham so mo rong cua 2 Khung keo tha uu tien trong lenh OPTIMIZE va SAVE_PLAN.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/net/ClientSession.java
 */
public class ClientSession implements Runnable {

    /** Timeout 5 phut khong co goi tin nao (B3.3 - Muc 11 Protocol) */
    public static final int SOCKET_TIMEOUT_MS = 5 * 60 * 1000;

    private static final Pattern SEMESTER_CODE_PATTERN = Pattern.compile("^\\d{4}-\\d{4}-\\d$");
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final String sessionId;
    private final Socket socket;
    private final String remoteAddress;
    private final String connectedAt;
    private final TkbServer server;
    private final KeyPair rsaKeyPair;
    private final CatalogDao catalogDao;
    private final UserDao userDao;
    private final PlanDao planDao;
    private final AuthService authService;
    private final CatalogCache catalogCache;
    private final TimetableSolver solver;
    private final JobManager jobManager;
    private final PdfImportService pdfImportService;

    private SecretKey aesSessionKey;
    private OutputStream socketOut;
    private volatile UserDto authenticatedUser;
    private volatile boolean running = true;

    public ClientSession(
            String sessionId,
            Socket socket,
            TkbServer server,
            KeyPair rsaKeyPair,
            CatalogDao catalogDao,
            UserDao userDao,
            PlanDao planDao,
            AuthService authService,
            CatalogCache catalogCache,
            TimetableSolver solver,
            JobManager jobManager,
            PdfImportService pdfImportService) {
        this.sessionId = sessionId;
        this.socket = socket;
        this.remoteAddress = socket.getRemoteSocketAddress() != null
                ? socket.getRemoteSocketAddress().toString()
                : "unknown";
        this.connectedAt = LocalDateTime.now().format(ISO_FORMATTER);
        this.server = server;
        this.rsaKeyPair = rsaKeyPair;
        this.catalogDao = catalogDao;
        this.userDao = userDao;
        this.planDao = planDao;
        this.authService = authService;
        this.catalogCache = catalogCache;
        this.solver = solver;
        this.jobManager = jobManager;
        this.pdfImportService = pdfImportService;
    }

    @Override
    public void run() {
        server.log("Mo ket noi moi [" + sessionId + "] tu " + remoteAddress);
        try (Socket s = this.socket;
             InputStream in = s.getInputStream();
             OutputStream out = s.getOutputStream()) {

            s.setSoTimeout(SOCKET_TIMEOUT_MS);
            this.socketOut = out;

            // 1. Thuc hien bat tay 3 buoc HELLO -> KEY_EXCHANGE -> READY (Muc 3 Protocol)
            if (!performHandshake(in, out)) {
                return;
            }

            // 2. Vong lap doc va xu ly cac goi Request da ma hoa AES-256-GCM
            while (running && !s.isClosed()) {
                String decryptedJson = FrameCodec.readEncryptedJsonFrame(in, aesSessionKey);
                Response response = processEncryptedRequestJson(decryptedJson);
                if (response != null) {
                    sendResponse(response);
                }
            }

        } catch (EOFException e) {
            server.log("Client [" + sessionId + "] da dong ket noi.");
        } catch (SocketTimeoutException e) {
            server.log("Phien [" + sessionId + "] qua han 5 phut khong hoat dong -> Dong ket noi.");
        } catch (IOException e) {
            if (running) {
                server.log("Ket noi [" + sessionId + "] ket thuc: " + e.getMessage());
            }
        } catch (Exception e) {
            server.logError("Loi ngoai le tren phien [" + sessionId + "]", e);
        } finally {
            cleanupSession();
        }
    }

    // =========================================================================
    // 1. BAT TAY BAO MAT 3 BUOC (HELLO -> KEY_EXCHANGE -> READY)
    // =========================================================================

    private boolean performHandshake(InputStream in, OutputStream out) throws IOException {
        Map<String, Object> helloMsg = new LinkedHashMap<>();
        helloMsg.put("type", "HELLO");
        helloMsg.put("protocolVersion", 1);
        helloMsg.put("publicKey", CryptoUtil.encodePublicKeyBase64(rsaKeyPair.getPublic()));
        FrameCodec.writePlainJsonFrame(out, GSON.toJson(helloMsg));

        String keyExchangeRaw = FrameCodec.readPlainJsonFrame(in);
        JsonObject keyObj = JsonParser.parseString(keyExchangeRaw).getAsJsonObject();
        String type = keyObj.has("type") && !keyObj.get("type").isJsonNull()
                ? keyObj.get("type").getAsString()
                : "";
        if (!"KEY_EXCHANGE".equals(type) || !keyObj.has("encryptedKey")) {
            server.log("Phien [" + sessionId + "] gui goi KEY_EXCHANGE khong hop le -> Dong ket noi.");
            return false;
        }

        String encryptedKeyBase64 = keyObj.get("encryptedKey").getAsString();
        this.aesSessionKey = CryptoUtil.decryptSessionKeyRsaBase64(encryptedKeyBase64, rsaKeyPair.getPrivate());

        Map<String, Object> readyMsg = Map.of("type", "READY");
        FrameCodec.writeEncryptedJsonFrame(out, GSON.toJson(readyMsg), aesSessionKey);
        server.log("Bat tay RSA/AES-GCM thanh cong cho phien [" + sessionId + "].");
        return true;
    }

    // =========================================================================
    // 2. DIEU PHOI LENH & KIEM TRA PHAN QUYEN (B3.2, B3.4)
    // =========================================================================

    private Response processEncryptedRequestJson(String json) {
        Request req;
        try {
            req = GSON.fromJson(json, Request.class);
        } catch (JsonSyntaxException e) {
            return Response.error(null, ErrorCode.BAD_REQUEST, "Gói tin JSON không đúng cú pháp.");
        }

        if (req == null || req.getId() == null || req.getCmd() == null || req.getCmd().isBlank()) {
            Integer reqId = (req != null) ? req.getId() : null;
            return Response.error(reqId, ErrorCode.BAD_REQUEST, "Thiếu trường id hoặc cmd trong Request.");
        }

        Integer reqId = req.getId();
        Command command = req.resolveCommand();
        if (command == null) {
            return Response.error(reqId, ErrorCode.BAD_REQUEST, "Lệnh không tồn tại: " + req.getCmd());
        }

        RequiredRole required = command.getRequiredRole();
        UserDto currentUser = this.authenticatedUser;
        if (required == RequiredRole.USER && currentUser == null) {
            return Response.error(reqId, ErrorCode.UNAUTHENTICATED);
        }
        if (required == RequiredRole.ADMIN) {
            if (currentUser == null) {
                return Response.error(reqId, ErrorCode.UNAUTHENTICATED);
            }
            if (!currentUser.isAdmin()) {
                return Response.error(reqId, ErrorCode.FORBIDDEN);
            }
        }

        try {
            return dispatchCommand(command, req);
        } catch (Exception e) {
            server.logError("Loi khi xu ly lenh " + command + " (id=" + reqId + ") tren phien [" + sessionId + "]", e);
            return Response.error(reqId, ErrorCode.INTERNAL_ERROR);
        }
    }

    private Response dispatchCommand(Command command, Request req) throws Exception {
        return switch (command) {
            case PING -> Response.successEmpty(req.getId());
            case REGISTER -> handleRegister(req);
            case LOGIN -> handleLogin(req);
            case LOGOUT -> handleLogout(req);
            case GET_SECTIONS -> handleGetSections(req);

            case LIST_SEMESTERS -> handleListSemesters(req);
            case LIST_COURSES -> handleListCourses(req);
            case OPTIMIZE -> handleOptimize(req);
            case CANCEL_JOB -> handleCancelJob(req);
            case GET_JOB -> handleGetJob(req);
            case SAVE_PLAN -> handleSavePlan(req);
            case LIST_PLANS -> handleListPlans(req);
            case GET_PLAN -> handleGetPlan(req);
            case DELETE_PLAN -> handleDeletePlan(req);

            case ADMIN_LIST_USERS -> handleAdminListUsers(req);
            case ADMIN_SET_USER_ROLE -> handleAdminSetUserRole(req);
            case ADMIN_RESET_PASSWORD -> handleAdminResetPassword(req);
            case ADMIN_DELETE_USER -> handleAdminDeleteUser(req);
            case ADMIN_CREATE_SEMESTER -> handleAdminCreateSemester(req);
            case ADMIN_SET_CURRENT_SEMESTER -> handleAdminSetCurrentSemester(req);
            case ADMIN_LIST_COURSES -> handleAdminListCourses(req);
            case ADMIN_UPSERT_COURSE -> handleAdminUpsertCourse(req);
            case ADMIN_DELETE_COURSE -> handleAdminDeleteCourse(req);
            case ADMIN_UPSERT_SECTION -> handleAdminUpsertSection(req);
            case ADMIN_SET_SECTION_ACTIVE -> handleAdminSetSectionActive(req);
            case ADMIN_DELETE_SECTION -> handleAdminDeleteSection(req);
            case IMPORT_BEGIN -> handleImportBegin(req);
            case IMPORT_CHUNK -> handleImportChunk(req);
            case IMPORT_COMMIT -> handleImportCommit(req);
            case IMPORT_ABORT -> handleImportAbort(req);
            case ADMIN_LIST_JOBS -> handleAdminListJobs(req);
            case ADMIN_CANCEL_JOB -> handleAdminCancelJob(req);
            case ADMIN_GET_STATS -> handleAdminGetStats(req);
            case ADMIN_LIST_SESSIONS -> handleAdminListSessions(req);
            case ADMIN_KICK_SESSION -> handleAdminKickSession(req);

            default -> Response.error(req.getId(), ErrorCode.BAD_REQUEST, "Lệnh không hợp lệ.");
        };
    }

    // =========================================================================
    // 3. XU LY NHOM LENH DUNG CHUNG (PING, REGISTER, LOGIN, LOGOUT, GET_SECTIONS)
    // =========================================================================

    private Response handleRegister(Request req) throws Exception {
        String username = req.getString("username");
        String password = req.getString("password");
        AuthResult res = authService.register(username, password);
        if (!res.ok()) {
            if (res.invalidField() != null) {
                return Response.error(req.getId(), res.errorCode(), res.errorMessage(), Map.of("field", res.invalidField()));
            }
            return Response.error(req.getId(), res.errorCode(), res.errorMessage());
        }
        server.log("Dang ky tai khoan moi thanh cong: username='" + res.user().getUsername() + "'");
        return Response.successEmpty(req.getId());
    }

    private Response handleLogin(Request req) throws Exception {
        String username = req.getString("username");
        String password = req.getString("password");
        AuthResult res = authService.login(username, password);
        if (!res.ok()) {
            return Response.error(req.getId(), res.errorCode(), res.errorMessage());
        }
        this.authenticatedUser = res.user();
        server.log("Nguoi dung '" + res.user().getUsername() + "' (role=" + res.user().getRole() + ") da dang nhap tren [" + sessionId + "]");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", res.user().getUserId());
        data.put("username", res.user().getUsername());
        data.put("role", res.user().getRole());
        return Response.success(req.getId(), data);
    }

    private Response handleLogout(Request req) {
        UserDto prev = this.authenticatedUser;
        if (prev != null) {
            jobManager.cancelJobsBySession(sessionId);
            pdfImportService.abortUploadsBySession(sessionId);
            this.authenticatedUser = null;
            server.log("Nguoi dung '" + prev.getUsername() + "' da LOGOUT tren [" + sessionId + "]");
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleGetSections(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        String courseCode = req.getString("courseCode");
        Boolean reqInactive = req.getBoolean("includeInactive");

        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        if (courseCode == null || courseCode.isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu mã học phần courseCode.", Map.of("field", "courseCode"));
        }

        boolean includeInactive = Boolean.TRUE.equals(reqInactive) && authenticatedUser != null && authenticatedUser.isAdmin();
        List<SectionDto> sections = catalogCache.getSectionsForCourse(semesterCode.trim(), courseCode.trim(), includeInactive);

        if (sections.isEmpty() && !includeInactive) {
            return Response.error(
                    req.getId(),
                    ErrorCode.COURSE_NOT_OPEN,
                    "Học phần " + courseCode + " không mở nhóm lớp nào trong học kỳ " + semesterCode,
                    Map.of("courseCodes", List.of(courseCode.trim()))
            );
        }

        return Response.successSingle(req.getId(), "sections", sections);
    }

    // =========================================================================
    // 4. XU LY NHOM LENH TRANG USER CUA QUY (Muc 6 Protocol)
    // =========================================================================

    private Response handleListSemesters(Request req) throws Exception {
        List<Map<String, Object>> semesters = catalogDao.listSemesters();
        return Response.successSingle(req.getId(), "semesters", semesters);
    }

    private Response handleListCourses(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        String keyword = req.getString("keyword");
        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        List<Map<String, Object>> courses = catalogDao.listOpenCourses(semesterCode.trim(), keyword);
        return Response.successSingle(req.getId(), "courses", courses);
    }

    private Response handleOptimize(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        List<String> courses = req.getStringList("courses");
        Integer maxPlansObj = req.getInt("maxPlans");
        int maxPlans = (maxPlansObj != null) ? maxPlansObj : 5;

        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        if (courses == null || courses.isEmpty() || courses.size() > 15) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Danh sách học phần phải từ 1 đến 15 môn.",
                    Map.of("field", "courses")
            );
        }

        Set<String> uniqueCourses = new LinkedHashSet<>();
        for (String c : courses) {
            if (c == null || c.isBlank() || !uniqueCourses.add(c.trim())) {
                return Response.error(
                        req.getId(),
                        ErrorCode.VALIDATION_FAILED,
                        "Danh sách học phần có mã rỗng hoặc bị trùng lặp.",
                        Map.of("field", "courses")
                );
            }
        }

        if (maxPlans < 1 || maxPlans > 10) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Số lượng phương án tối đa (maxPlans) phải từ 1 đến 10.",
                    Map.of("field", "maxPlans")
            );
        }

        Map<String, List<SectionDto>> allActiveByCourse = catalogCache.getAllActiveSectionsByCourse(semesterCode.trim());
        List<String> notOpenCourses = new ArrayList<>();
        for (String code : uniqueCourses) {
            List<SectionDto> activeSecs = allActiveByCourse.get(code);
            if (activeSecs == null || activeSecs.isEmpty()) {
                notOpenCourses.add(code);
            }
        }
        if (!notOpenCourses.isEmpty()) {
            return Response.error(
                    req.getId(),
                    ErrorCode.COURSE_NOT_OPEN,
                    "Các học phần sau không mở lớp trong học kỳ " + semesterCode + ": " + String.join(", ", notOpenCourses),
                    Map.of("courseCodes", notOpenCourses)
            );
        }

        long userId = authenticatedUser.getUserId();
        if (!jobManager.canUserSubmitJob(userId)) {
            return Response.error(req.getId(), ErrorCode.TOO_MANY_JOBS);
        }

        Criteria criteria = parseCriteria(req.getData().get("criteria"));
        List<String> orderedCourses = new ArrayList<>(uniqueCourses);

        String jobId = jobManager.submitJob(
                userId,
                authenticatedUser.getUsername(),
                sessionId,
                JobDto.TYPE_OPTIMIZE,
                this::sendResponse,
                (isCancelled, onProgress) -> {
                    SolveOutcome outcome = solver.solve(
                            orderedCourses,
                            allActiveByCourse,
                            criteria,
                            maxPlans,
                            isCancelled,
                            onProgress
                    );
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("droppedCourses", outcome.droppedCourses());
                    result.put("plans", outcome.plans());
                    return result;
                }
        );

        return Response.successSingle(req.getId(), "jobId", jobId);
    }

    private Response handleCancelJob(Request req) {
        String jobId = req.getString("jobId");
        if (jobId == null || !jobManager.cancelJobByUser(jobId.trim(), authenticatedUser.getUserId())) {
            return Response.error(req.getId(), ErrorCode.JOB_NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleGetJob(Request req) {
        String jobId = req.getString("jobId");
        if (jobId == null) {
            return Response.error(req.getId(), ErrorCode.JOB_NOT_FOUND);
        }
        JobDto dto = jobManager.getJobForUser(jobId.trim(), authenticatedUser.getUserId());
        if (dto == null) {
            return Response.error(req.getId(), ErrorCode.JOB_NOT_FOUND);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", dto.getJobId());
        data.put("jobType", dto.getJobType());
        data.put("status", dto.getStatus());
        data.put("percent", dto.getPercent());
        if (dto.getResult() != null) {
            data.put("result", dto.getResult());
        }
        if (dto.getError() != null) {
            data.put("error", dto.getError());
        }
        return Response.success(req.getId(), data);
    }

    private Response handleSavePlan(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        String name = req.getString("name");
        List<Integer> rawSectionIds = req.getIntList("sectionIds");
        List<String> droppedCourseCodes = req.getStringList("droppedCourseCodes");

        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Tên phương án phải từ 1 đến 100 ký tự và không được toàn khoảng trắng.",
                    Map.of("field", "name")
            );
        }
        if (rawSectionIds == null || rawSectionIds.isEmpty()) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Phương án phải chọn ít nhất 1 nhóm lớp.",
                    Map.of("field", "sectionIds")
            );
        }

        long userId = authenticatedUser.getUserId();
        String cleanName = name.trim();
        if (planDao.isPlanNameTaken(userId, cleanName)) {
            return Response.error(req.getId(), ErrorCode.NAME_TAKEN);
        }

        List<SectionDto> selectedSections = new ArrayList<>();
        List<Long> sectionIds = new ArrayList<>();
        Set<String> seenCourses = new LinkedHashSet<>();

        for (Integer rawId : rawSectionIds) {
            long secId = rawId.longValue();
            SectionDto sec = catalogCache.getSectionById(semesterCode.trim(), secId);
            if (sec == null) {
                return Response.error(req.getId(), ErrorCode.NOT_FOUND, "Không tìm thấy nhóm lớp id=" + secId + " trong học kỳ " + semesterCode);
            }
            if (!seenCourses.add(sec.getCourseCode())) {
                return Response.error(
                        req.getId(),
                        ErrorCode.VALIDATION_FAILED,
                        "Phương án có nhiều hơn 1 nhóm lớp cho cùng học phần " + sec.getCourseCode(),
                        Map.of("field", "sectionIds")
                );
            }
            selectedSections.add(sec);
            sectionIds.add(secId);
        }

        if (solver.hasAnyConflict(selectedSections)) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Các nhóm lớp đã chọn bị trùng lịch học với nhau.",
                    Map.of("field", "sectionIds")
            );
        }

        Object criteriaRaw = req.getData().get("criteria");
        Criteria criteria = parseCriteria(criteriaRaw);
        PlanDto evaluated = solver.evaluateAndBuildPlan(selectedSections, criteria, List.of());

        String criteriaJson = (criteriaRaw != null) ? GSON.toJson(criteriaRaw) : "{}";
        long planId = planDao.savePlan(
                userId,
                semesterCode.trim(),
                cleanName,
                criteriaJson,
                evaluated.getScore(),
                evaluated.getMetrics(),
                sectionIds,
                droppedCourseCodes
        );

        return Response.successSingle(req.getId(), "planId", planId);
    }

    private Response handleListPlans(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        List<Map<String, Object>> plans = planDao.listPlansByUser(authenticatedUser.getUserId(), semesterCode);
        return Response.successSingle(req.getId(), "plans", plans);
    }

    private Response handleGetPlan(Request req) throws Exception {
        Long planId = req.getLong("planId");
        if (planId == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu planId.", Map.of("field", "planId"));
        }
        PlanDto plan = planDao.getPlanByIdAndUser(planId, authenticatedUser.getUserId());
        if (plan == null) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", plan.getId());
        data.put("name", plan.getName());
        data.put("semesterCode", plan.getSemesterCode());
        data.put("rank", plan.getRank());
        data.put("score", plan.getScore());
        data.put("metrics", plan.getMetrics());
        data.put("sections", plan.getSections());
        data.put("droppedCourses", plan.getDroppedCourses());
        data.put("createdAt", plan.getCreatedAt());
        return Response.success(req.getId(), data);
    }

    private Response handleDeletePlan(Request req) throws Exception {
        Long planId = req.getLong("planId");
        if (planId == null || !planDao.deletePlanByIdAndUser(planId, authenticatedUser.getUserId())) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    // =========================================================================
    // 5. XU LY NHOM LENH TRANG ADMIN CUA TRAN (Muc 7 Protocol - Quyen ADMIN)
    // =========================================================================

    private Response handleAdminListUsers(Request req) throws Exception {
        String keyword = req.getString("keyword");
        Integer pageObj = req.getInt("page");
        Integer sizeObj = req.getInt("size");
        int page = (pageObj != null) ? pageObj : 0;
        int size = (sizeObj != null) ? sizeObj : 20;

        if (page < 0) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "page phải >= 0.", Map.of("field", "page"));
        }
        if (size < 1 || size > 100) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "size phải từ 1 đến 100.", Map.of("field", "size"));
        }

        Set<Long> onlineUserIds = server.getOnlineUserIds();
        List<UserDto> users = userDao.listUsers(keyword, page, size, onlineUserIds);
        int total = userDao.countUsers(keyword);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", users);
        data.put("total", total);
        return Response.success(req.getId(), data);
    }

    private Response handleAdminSetUserRole(Request req) throws Exception {
        Long targetUserId = req.getLong("userId");
        String role = req.getString("role");
        if (targetUserId == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu userId.", Map.of("field", "userId"));
        }
        if (targetUserId == authenticatedUser.getUserId()) {
            return Response.error(req.getId(), ErrorCode.CANNOT_MODIFY_SELF);
        }
        if (role == null || (!"USER".equalsIgnoreCase(role.trim()) && !"ADMIN".equalsIgnoreCase(role.trim()))) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Vai trò phải là USER hoặc ADMIN.", Map.of("field", "role"));
        }
        if (!userDao.updateUserRole(targetUserId, role.trim().toUpperCase())) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminResetPassword(Request req) throws Exception {
        Long targetUserId = req.getLong("userId");
        String newPassword = req.getString("newPassword");
        if (targetUserId == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu userId.", Map.of("field", "userId"));
        }
        AuthResult val = authService.validatePasswordFormat(newPassword, "newPassword");
        if (!val.ok()) {
            return Response.error(req.getId(), val.errorCode(), val.errorMessage(), Map.of("field", val.invalidField()));
        }
        String newHash = authService.hashPassword(newPassword);
        if (!userDao.updatePasswordHash(targetUserId, newHash)) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminDeleteUser(Request req) throws Exception {
        Long targetUserId = req.getLong("userId");
        if (targetUserId == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu userId.", Map.of("field", "userId"));
        }
        if (targetUserId == authenticatedUser.getUserId()) {
            return Response.error(req.getId(), ErrorCode.CANNOT_MODIFY_SELF);
        }
        if (!userDao.deleteUser(targetUserId)) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminCreateSemester(Request req) throws Exception {
        String code = req.getString("code");
        String name = req.getString("name");
        if (code == null || !SEMESTER_CODE_PATTERN.matcher(code.trim()).matches()) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Mã học kỳ phải đúng định dạng NNNN-NNNN-N (ví dụ: 2026-2027-1).",
                    Map.of("field", "code")
            );
        }
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Tên học kỳ phải từ 1 đến 100 ký tự.",
                    Map.of("field", "name")
            );
        }
        if (!catalogDao.createSemester(code.trim(), name.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_EXISTS);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminSetCurrentSemester(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.setCurrentSemester(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminListCourses(Request req) throws Exception {
        String semesterCode = req.getString("semesterCode");
        String keyword = req.getString("keyword");
        if (semesterCode == null || semesterCode.isBlank()) {
            semesterCode = catalogDao.getCurrentSemesterCode();
        }
        if (semesterCode == null || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        List<Map<String, Object>> courses = catalogDao.listCoursesForAdmin(semesterCode.trim(), keyword);
        return Response.successSingle(req.getId(), "courses", courses);
    }

    private Response handleAdminUpsertCourse(Request req) throws Exception {
        String courseCode = req.getString("courseCode");
        String name = req.getString("name");
        Integer credits = req.getInt("credits");

        if (courseCode == null || !COURSE_CODE_PATTERN.matcher(courseCode.trim()).matches()) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Mã học phần phải từ 1 đến 10 ký tự chữ hoặc số.",
                    Map.of("field", "courseCode")
            );
        }
        if (name == null || name.isBlank() || name.trim().length() > 150) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Tên học phần không được rỗng (tối đa 150 ký tự).",
                    Map.of("field", "name")
            );
        }
        if (credits == null || credits < 0 || credits > 20) {
            return Response.error(
                    req.getId(),
                    ErrorCode.VALIDATION_FAILED,
                    "Số tín chỉ phải từ 0 đến 20.",
                    Map.of("field", "credits")
            );
        }

        catalogDao.upsertCourse(courseCode.trim(), name.trim(), credits);
        catalogCache.clearAll();
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminDeleteCourse(Request req) throws Exception {
        String courseCode = req.getString("courseCode");
        if (courseCode == null || courseCode.isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu courseCode.", Map.of("field", "courseCode"));
        }
        if (catalogDao.isCourseInUse(courseCode.trim())) {
            return Response.error(req.getId(), ErrorCode.COURSE_IN_USE);
        }
        if (!catalogDao.deleteCourse(courseCode.trim())) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        catalogCache.clearAll();
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminUpsertSection(Request req) throws Exception {
        String jsonData = GSON.toJson(req.getData());
        SectionDto section = GSON.fromJson(jsonData, SectionDto.class);

        if (section == null || section.getSemesterCode() == null || section.getSemesterCode().isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu mã học kỳ semesterCode.", Map.of("field", "semesterCode"));
        }
        if (!catalogDao.semesterExists(section.getSemesterCode().trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        if (section.getCourseCode() == null || section.getCourseCode().isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu mã học phần courseCode.", Map.of("field", "courseCode"));
        }
        if (section.getGroupNo() == null || section.getGroupNo().isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu số nhóm groupNo.", Map.of("field", "groupNo"));
        }
        if (section.getCapacity() < 0) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Sĩ số không được âm.", Map.of("field", "capacity"));
        }
        if ("TH".equalsIgnoreCase(section.getType()) && section.getLabGroup().isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Nhóm Thực hành (TH) phải có mã tổ thực hành labGroup.", Map.of("field", "labGroup"));
        }
        if (section.getSlots() == null || section.getSlots().isEmpty() || section.getSlots().size() > 10) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Mỗi nhóm lớp phải có từ 1 đến 10 buổi học (slots).", Map.of("field", "slots"));
        }
        for (SlotDto slot : section.getSlots()) {
            if (slot.getDayOfWeek() < 2 || slot.getDayOfWeek() > 8
                    || slot.getStartPeriod() < 1 || slot.getPeriodCount() < 1
                    || slot.getEndPeriod() > 12) {
                return Response.error(
                        req.getId(),
                        ErrorCode.VALIDATION_FAILED,
                        "Buổi học có thứ (2..8) hoặc tiết (1..12) không hợp lệ.",
                        Map.of("field", "slots")
                );
            }
        }

        try {
            long savedId = catalogDao.upsertSection(section);
            catalogCache.refreshSemester(section.getSemesterCode().trim());
            return Response.successSingle(req.getId(), "id", savedId);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("NOT_FOUND")) {
                return Response.error(req.getId(), ErrorCode.NOT_FOUND, msg);
            }
            if (msg.contains("Duplicate entry")) {
                return Response.error(req.getId(), ErrorCode.SECTION_EXISTS);
            }
            throw e;
        }
    }

    private Response handleAdminSetSectionActive(Request req) throws Exception {
        Long sectionId = req.getLong("sectionId");
        Boolean active = req.getBoolean("active");
        if (sectionId == null || active == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu sectionId hoặc active.", Map.of("field", "sectionId"));
        }
        if (!catalogDao.setSectionActive(sectionId, active)) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        catalogCache.clearAll();
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminDeleteSection(Request req) throws Exception {
        Long sectionId = req.getLong("sectionId");
        if (sectionId == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu sectionId.", Map.of("field", "sectionId"));
        }
        if (catalogDao.isSectionInUse(sectionId)) {
            return Response.error(req.getId(), ErrorCode.SECTION_IN_USE);
        }
        if (!catalogDao.deleteSection(sectionId)) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        catalogCache.clearAll();
        return Response.successEmpty(req.getId());
    }

    private Response handleImportBegin(Request req) throws Exception {
        String fileName = req.getString("fileName");
        Long sizeBytes = req.getLong("sizeBytes");
        String semesterCode = req.getString("semesterCode");

        if (semesterCode == null || semesterCode.isBlank() || !catalogDao.semesterExists(semesterCode.trim())) {
            return Response.error(req.getId(), ErrorCode.SEMESTER_NOT_FOUND);
        }
        if (sizeBytes == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu sizeBytes.", Map.of("field", "sizeBytes"));
        }

        UploadActionResult res = pdfImportService.beginUpload(sessionId, fileName, sizeBytes, semesterCode.trim());
        if (!res.ok()) {
            return Response.error(req.getId(), res.errorCode(), res.errorMessage(), res.errorDetails());
        }
        return Response.success(req.getId(), res.data());
    }

    private Response handleImportChunk(Request req) {
        String uploadId = req.getString("uploadId");
        Integer index = req.getInt("index");
        String dataBase64 = req.getString("dataBase64");

        if (uploadId == null || index == null) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu uploadId hoặc index.", Map.of("field", "uploadId"));
        }

        UploadActionResult res = pdfImportService.receiveChunk(uploadId.trim(), index, dataBase64);
        if (!res.ok()) {
            return Response.error(req.getId(), res.errorCode(), res.errorMessage(), res.errorDetails());
        }
        return Response.success(req.getId(), res.data());
    }

    private Response handleImportCommit(Request req) {
        String uploadId = req.getString("uploadId");
        Boolean dryRunObj = req.getBoolean("dryRun");
        boolean dryRun = (dryRunObj == null) || dryRunObj;

        if (uploadId == null || uploadId.isBlank()) {
            return Response.error(req.getId(), ErrorCode.UPLOAD_NOT_FOUND);
        }

        UploadActionResult check = pdfImportService.validateBeforeCommit(uploadId.trim());
        if (!check.ok()) {
            return Response.error(req.getId(), check.errorCode(), check.errorMessage(), check.errorDetails());
        }

        long userId = authenticatedUser.getUserId();
        String jobId = jobManager.submitJob(
                userId,
                authenticatedUser.getUsername(),
                sessionId,
                JobDto.TYPE_IMPORT,
                this::sendResponse,
                (isCancelled, onProgress) -> {
                    ImportReport report = pdfImportService.executeImportJob(
                            uploadId.trim(),
                            dryRun,
                            isCancelled,
                            onProgress
                    );
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reportMap = GSON.fromJson(GSON.toJson(report), Map.class);
                    return reportMap;
                }
        );

        return Response.successSingle(req.getId(), "jobId", jobId);
    }

    private Response handleImportAbort(Request req) {
        String uploadId = req.getString("uploadId");
        if (uploadId == null || !pdfImportService.abortUpload(uploadId.trim())) {
            return Response.error(req.getId(), ErrorCode.UPLOAD_NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminListJobs(Request req) {
        List<JobDto> jobs = jobManager.listActiveJobsForAdmin();
        return Response.successSingle(req.getId(), "jobs", jobs);
    }

    private Response handleAdminCancelJob(Request req) {
        String jobId = req.getString("jobId");
        if (jobId == null || !jobManager.cancelJobByAdmin(jobId.trim())) {
            return Response.error(req.getId(), ErrorCode.JOB_NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    private Response handleAdminGetStats(Request req) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("onlineUsers", server.countOnlineAuthenticatedUsers());
        data.put("activeJobs", jobManager.countRunningJobs());
        data.put("queuedJobs", jobManager.countQueuedJobs());
        data.put("totalUsers", userDao.countUsers(null));
        data.put("totalPlans", planDao.countTotalPlans());
        data.put("totalSections", catalogDao.countTotalActiveSections());
        data.put("uptimeSeconds", server.getUptimeSeconds());
        return Response.success(req.getId(), data);
    }

    private Response handleAdminListSessions(Request req) {
        return Response.successSingle(req.getId(), "sessions", server.listActiveSessionSummaries());
    }

    private Response handleAdminKickSession(Request req) {
        String targetSessionId = req.getString("sessionId");
        if (targetSessionId == null || targetSessionId.isBlank()) {
            return Response.error(req.getId(), ErrorCode.VALIDATION_FAILED, "Thiếu sessionId.", Map.of("field", "sessionId"));
        }
        if (sessionId.equals(targetSessionId.trim())) {
            return Response.error(req.getId(), ErrorCode.CANNOT_MODIFY_SELF);
        }
        if (!server.kickSession(targetSessionId.trim())) {
            return Response.error(req.getId(), ErrorCode.NOT_FOUND);
        }
        return Response.successEmpty(req.getId());
    }

    // =========================================================================
    // CAC HAM TIEN ICH GUI GOI TIN & PARSE CRITERIA MO RONG
    // =========================================================================

    public void sendResponse(Response response) {
        if (!running || socket.isClosed() || socketOut == null || aesSessionKey == null) {
            return;
        }
        try {
            String json = GSON.toJson(response);
            FrameCodec.writeEncryptedJsonFrame(socketOut, json, aesSessionKey);
        } catch (IOException e) {
            server.log("Khong the gui goi tin ve phien [" + sessionId + "]: " + e.getMessage());
            closeSession();
        }
    }

    public void closeSession() {
        this.running = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void cleanupSession() {
        this.running = false;
        jobManager.cancelJobsBySession(sessionId);
        pdfImportService.abortUploadsBySession(sessionId);
        server.unregisterSession(sessionId);
    }

    /**
     * Đọc đầy đủ các trường cấu hình của cả 2 Khung kéo thả (tương thích ngược 100% với JSON cũ).
     */
    private Criteria parseCriteria(Object rawObj) {
        if (rawObj instanceof Map<?, ?> map) {
            boolean minDays = parseBool(map.get("minDays"), true);
            boolean morningFirst = parseBool(map.get("morningFirst"), true);
            boolean avoidGaps = parseBool(map.get("avoidGaps"), false);
            double wDays = parseDouble(map.get("weightMinDays"), 1.0);
            double wMorning = parseDouble(map.get("weightMorning"), 1.0);
            double wGaps = parseDouble(map.get("weightNoGaps"), 1.0);

            String timeMode = map.get("timeMode") != null
                    ? String.valueOf(map.get("timeMode")).trim()
                    : (morningFirst ? "MORNING" : "NONE");

            Set<Integer> preferredDays = new LinkedHashSet<>();
            if (map.get("preferredDays") instanceof List<?> dayList) {
                for (Object item : dayList) {
                    if (item instanceof Number n) {
                        preferredDays.add(n.intValue());
                    }
                }
            }

            int[] preferredSlotsMask = null;
            if (map.get("preferredSlotsMask") instanceof List<?> maskList && !maskList.isEmpty()) {
                preferredSlotsMask = new int[Math.min(7, maskList.size())];
                for (int i = 0; i < preferredSlotsMask.length; i++) {
                    Object m = maskList.get(i);
                    if (m instanceof Number n) {
                        preferredSlotsMask[i] = n.intValue();
                    }
                }
            }

            String preferredCampus = map.get("preferredCampus") != null
                    ? String.valueOf(map.get("preferredCampus")).trim()
                    : "";
            boolean avoidCampusSwitch = parseBool(map.get("avoidCampusSwitch"), true);
            double wCampus = parseDouble(map.get("weightCampus"), 1.0);

            Map<String, String> preferredLecturers = parseStringMap(map.get("preferredLecturers"));
            Map<String, String> lockedGroups = parseStringMap(map.get("lockedGroups"));

            return new Criteria(
                    minDays,
                    morningFirst,
                    avoidGaps,
                    wDays,
                    wMorning,
                    wGaps,
                    timeMode,
                    preferredDays,
                    preferredSlotsMask,
                    preferredCampus,
                    avoidCampusSwitch,
                    wCampus,
                    preferredLecturers,
                    lockedGroups
            );
        }
        return new Criteria(true, true, false);
    }

    private static Map<String, String> parseStringMap(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            Map<String, String> res = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    res.put(String.valueOf(e.getKey()).trim(), String.valueOf(e.getValue()).trim());
                }
            }
            return res;
        }
        return Collections.emptyMap();
    }

    private static boolean parseBool(Object val, boolean def) {
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(String.valueOf(val));
        return def;
    }

    private static double parseDouble(Object val, double def) {
        if (val instanceof Number n) return n.doubleValue();
        if (val != null) {
            try {
                return Double.parseDouble(String.valueOf(val));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UserDto getAuthenticatedUser() {
        return authenticatedUser;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getConnectedAt() {
        return connectedAt;
    }
}