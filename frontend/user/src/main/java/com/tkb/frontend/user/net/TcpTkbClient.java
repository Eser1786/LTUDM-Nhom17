package com.tkb.frontend.user.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.common.crypto.CryptoUtil;
import com.tkb.common.dto.PlanDto;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import com.tkb.common.net.FrameCodec;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import com.tkb.frontend.user.model.ErrorCode;
import com.tkb.frontend.user.model.Types;
import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.JobResult;
import com.tkb.frontend.user.model.Types.JobStatus;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.PlanRequest;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.model.Types.Semester;
import com.tkb.frontend.user.model.Types.Student;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;

public class TcpTkbClient implements TkbClient {

    private static final int DEFAULT_PORT = 9000;
    private static final String DEFAULT_HOST = "localhost";

    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private SecretKey aesKey;
    private volatile boolean connected = false;
    private Thread readerThread;

    private volatile String username;
    private volatile String role;
    private volatile Student student;

    private final java.util.concurrent.atomic.AtomicLong idSeq = new java.util.concurrent.atomic.AtomicLong(1);
    private final Map<Integer, CompletableFuture<Response>> pending = new ConcurrentHashMap<>();
    private final Map<String, JobResult> jobResults = new ConcurrentHashMap<>();
    private final Map<String, String> jobErrors = new ConcurrentHashMap<>();
    private final Map<String, Integer> jobProgress = new ConcurrentHashMap<>();

    public TcpTkbClient() {
        this(System.getProperty("tkb.backend.host", DEFAULT_HOST),
             Integer.parseInt(System.getProperty("tkb.backend.port", String.valueOf(DEFAULT_PORT))));
    }

    public TcpTkbClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private synchronized void ensureConnected() throws ApiException {
        if (connected) return;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(0);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // Handshake
            String hello = FrameCodec.readPlainJsonFrame(in);
            if (hello == null) throw new IOException("Khong nhan duoc HELLO");
            Map<String, Object> helloMap = mapper.readValue(hello, new TypeReference<Map<String, Object>>() {});
            String pubKeyB64 = String.valueOf(helloMap.get("publicKey"));
            PublicKey pubKey = CryptoUtil.decodePublicKeyBase64(pubKeyB64);
            SecretKey aes = CryptoUtil.generateAesSessionKey();
            String encKey = CryptoUtil.encryptSessionKeyRsaBase64(aes, pubKey);
            Map<String, Object> ke = new HashMap<>();
            ke.put("type", "KEY_EXCHANGE");
            ke.put("encryptedKey", encKey);
            FrameCodec.writePlainJsonFrame(out, mapper.writeValueAsString(ke));

            String ready = FrameCodec.readEncryptedJsonFrame(in, aes);
            if (ready == null) throw new IOException("Khong nhan duoc READY");
            this.aesKey = aes;
            connected = true;

            readerThread = new Thread(this::readerLoop, "tkb-reader");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (Exception e) {
            closeQuietly();
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Khong the ket noi backend: " + e.getMessage());
        }
    }

    private void readerLoop() {
        try {
            while (connected && !socket.isClosed()) {
                String json = FrameCodec.readEncryptedJsonFrame(in, aesKey);
                if (json == null) break;
                Response resp = mapper.readValue(json, Response.class);
                if (resp.isPush()) {
                    handlePush(resp);
                } else {
                    CompletableFuture<Response> f = pending.remove(resp.getId());
                    if (f != null) f.complete(resp);
                }
            }
        } catch (Exception e) {
            // connection lost
        } finally {
            connected = false;
        }
    }

    private void handlePush(Response resp) {
        String push = resp.getPush();
        Map<String, Object> data = resp.getData();
        if (data == null) return;
        if ("JOB_PROGRESS".equals(push)) {
            Object jid = data.get("jobId");
            Object pct = data.get("percent");
            if (jid != null) {
                jobProgress.put(String.valueOf(jid), pct instanceof Number ? ((Number) pct).intValue() : 0);
            }
        } else if ("JOB_DONE".equals(push)) {
            Object jid = data.get("jobId");
            if (jid == null) return;
            String jobId = String.valueOf(jid);
            String status = String.valueOf(data.getOrDefault("status", ""));
            if ("DONE".equals(status) || "COMPLETED".equals(status)) {
                Object resultObj = data.get("result");
                JobResult jr = mapJobResult(resultObj);
                if (jr != null) jobResults.put(jobId, jr);
            } else if ("FAILED".equals(status)) {
                Object err = data.get("error");
                String msg = "Xep lich that bai";
                if (err instanceof Map<?, ?> m && m.get("message") != null) msg = String.valueOf(m.get("message"));
                jobErrors.put(jobId, msg);
            }
        }
    }

    private Response sendRequest(String cmd, Map<String, Object> data) throws ApiException {
        ensureConnected();
        int id = (int) idSeq.getAndIncrement();
        Request req = new Request();
        req.setId(id);
        req.setCmd(cmd);
        if (data != null) req.setData(data);
        CompletableFuture<Response> future = new CompletableFuture<>();
        pending.put(id, future);
        // write
        try {
            FrameCodec.writeEncryptedJsonFrame(out, mapper.writeValueAsString(req), aesKey);
        } catch (Exception e) {
            pending.remove(id);
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Gui yeu cau that bai: " + e.getMessage());
        }
        try {
            Response resp = future.get(60, TimeUnit.SECONDS);
            if (resp == null) throw new ApiException(ErrorCode.NETWORK_ERROR, "Khong nhan duoc phan hoi");
            checkOk(resp);
            return resp;
        } catch (ApiException e) {
            pending.remove(id);
            throw e;
        } catch (Exception e) {
            pending.remove(id);
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Cho phan hoi that bai: " + e.getMessage());
        }
    }

    private void checkOk(Response resp) throws ApiException {
        if (resp.getOk() == null || !resp.getOk()) {
            ErrorCode code = ErrorCode.UNKNOWN;
            String msg = "Yeu cau that bai";
            if (resp.getError() != null) {
                String backendCode = resp.getError().getCode();
                msg = resp.getError().getMessage();
                code = mapErrorCode(backendCode);
            }
            List<String> details = new ArrayList<>();
            if (resp.getError() != null && resp.getError().getDetails() != null) {
                Object cc = resp.getError().getDetails().get("courseCodes");
                if (cc instanceof List<?> l) for (Object o : l) details.add(String.valueOf(o));
            }
            throw new ApiException(code, msg, details);
        }
    }

    private ErrorCode mapErrorCode(String backendCode) {
        if (backendCode == null) return ErrorCode.UNKNOWN;
        return switch (backendCode) {
            case "COURSE_NOT_OPEN" -> ErrorCode.COURSE_NOT_OPEN;
            case "NAME_TAKEN" -> ErrorCode.NAME_TAKEN;
            case "JOB_NOT_FOUND" -> ErrorCode.JOB_NOT_FOUND;
            case "SEMESTER_NOT_FOUND" -> ErrorCode.SEMESTER_NOT_FOUND;
            case "VALIDATION_FAILED", "VALIDATION_ERROR" -> ErrorCode.VALIDATION_ERROR;
            case "NOT_FOUND" -> ErrorCode.PLAN_NOT_FOUND;
            default -> ErrorCode.UNKNOWN;
        };
    }

    private void closeQuietly() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    // ---------- TkbClient impl ----------

    @Override
    public void login(String username, String password) throws ApiException {
        ensureConnected();
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        Response resp = sendRequest("LOGIN", data);
        Map<String, Object> d = resp.getData();
        this.username = username;
        this.role = d != null ? String.valueOf(d.getOrDefault("role", "USER")) : "USER";
        this.student = new Student(username, username, role, username + "@sv.univ.edu.vn");
    }

    @Override
    public void logout() throws ApiException {
        try { sendRequest("LOGOUT", null); } finally { username = null; }
    }

    @Override
    public Student sinhVien() throws ApiException {
        if (student == null) {
            return new Student("Sinh vien", "unknown", "SV", "");
        }
        return student;
    }

    @Override
    public List<Semester> listSemesters() throws ApiException {
        Response resp = sendRequest("LIST_SEMESTERS", null);
        List<Semester> out = new ArrayList<>();
        Object sems = resp.getData() == null ? null : resp.getData().get("semesters");
        if (sems instanceof List<?> list) {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                String code = String.valueOf(m.get("code"));
                String name = String.valueOf(m.get("name"));
                boolean current = Boolean.TRUE.equals(m.get("current"));
                out.add(new Semester(code, name, current, "", ""));
            }
        }
        return out;
    }

    @Override
    public List<Course> listCourses(String maHocKy) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        if (maHocKy != null) data.put("semesterCode", maHocKy);
        Response resp = sendRequest("LIST_COURSES", data);
        List<Course> out = new ArrayList<>();
        Object courses = resp.getData() == null ? null : resp.getData().get("courses");
        if (courses instanceof List<?> list) {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                out.add(new Course(
                        String.valueOf(m.get("courseCode")),
                        String.valueOf(m.get("name")),
                        m.get("credits") instanceof Number ? ((Number) m.get("credits")).intValue() : 0));
            }
        }
        return out;
    }

    private List<SectionSlot> mapSectionsResponse(Object sectionsObj) {
        List<SectionSlot> out = new ArrayList<>();
        if (!(sectionsObj instanceof List<?> list)) return out;
        for (Object o : list) {
            Map<String, Object> m = (Map<String, Object>) o;
            // convert to SectionDto via mapper for safety
            SectionDto dto = mapper.convertValue(m, SectionDto.class);
            out.add(toSectionSlot(dto));
        }
        return out;
    }

    private SectionSlot toSectionSlot(SectionDto dto) {
        Section section = new Section(
                dto.getId() == null ? 0 : dto.getId().intValue(),
                dto.getSemesterCode(),
                dto.getCourseCode(),
                dto.getGroupNo(),
                dto.getLabGroup(),
                dto.getType(),
                dto.getCapacity(),
                dto.getLecturer()
        );
        List<ClassSession> sessions = new ArrayList<>();
        if (dto.getSlots() != null) {
            for (SlotDto s : dto.getSlots()) {
                sessions.add(new ClassSession(s.getDayOfWeek(), s.getStartPeriod(), s.getPeriodCount(), s.getRoom()));
            }
        }
        return new SectionSlot(section, dto.getCourseName(), dto.getCredits(), sessions);
    }

    @Override
    public List<Types.SectionSlot> listSections(String maHocKy, String maMh) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("semesterCode", maHocKy);
        data.put("courseCode", maMh);
        Response resp = sendRequest("GET_SECTIONS", data);
        Object sections = resp.getData() == null ? null : resp.getData().get("sections");
        return mapSectionsResponse(sections);
    }

    @Override
    public List<Types.SectionSlot> listAllSections(String maHocKy) throws ApiException {
        List<Course> courses = listCourses(maHocKy);
        List<SectionSlot> all = new ArrayList<>();
        for (Course c : courses) {
            try {
                all.addAll(listSections(maHocKy, c.maMh()));
            } catch (ApiException ignored) {}
        }
        return all;
    }

    @Override
    public String submit(PlanRequest request) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("semesterCode", request.maHocKy());
        data.put("courses", request.maMhTheoUuTien() == null ? List.of() : request.maMhTheoUuTien());
        data.put("maxPlans", request.criteria() == null ? 3 : Math.max(1, Math.min(10, request.criteria().soPhuongAn())));
        // criteria
        Map<String, Object> crit = new HashMap<>();
        Criteria c = request.criteria() == null ? Criteria.macDinh() : request.criteria();
        crit.put("minDays", c.itNgay() > 0);
        crit.put("morningFirst", c.uuTienSang() > 0);
        crit.put("avoidGaps", c.khoangTrong() > 0);
        crit.put("weightMinDays", (double) c.itNgay());
        crit.put("weightMorning", (double) c.uuTienSang());
        crit.put("weightNoGaps", (double) c.khoangTrong());
        crit.put("timeMode", c.timeMode() == null ? "NONE" : c.timeMode());
        crit.put("preferredDays", c.preferredDays() == null ? List.of() : c.preferredDays());
        crit.put("preferredSlotsMask", c.preferredSlotsMask());
        crit.put("preferredCampus", c.preferredCampus() == null ? "ANY" : c.preferredCampus());
        crit.put("avoidCampusSwitch", c.avoidCampusSwitch());
        crit.put("weightCampus", (double) Math.max(1, c.ngayYeuThich()));
        crit.put("preferredLecturers", c.preferredLecturers() == null ? Map.of() : c.preferredLecturers());
        crit.put("lockedGroups", c.lockedGroups() == null ? Map.of() : c.lockedGroups());
        data.put("criteria", crit);
        Response resp = sendRequest("OPTIMIZE", data);
        Object jobId = resp.getData() == null ? null : resp.getData().get("jobId");
        if (jobId == null) throw new ApiException(ErrorCode.UNKNOWN, "Server khong tra ve jobId");
        return String.valueOf(jobId);
    }

    @Override
    public JobStatus jobStatus(String jobId) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        Response resp = sendRequest("GET_JOB", data);
        Map<String, Object> d = resp.getData();
        String statusStr = d == null ? "RUNNING" : String.valueOf(d.getOrDefault("status", "RUNNING"));
        JobStatus.State state = mapState(statusStr);
        int pct = 0;
        if (d != null && d.get("percent") instanceof Number) pct = ((Number) d.get("percent")).intValue();
        // wait for result if done
        JobResult result = jobResults.get(jobId);
        if (state == JobStatus.State.DONE && result == null) {
            for (int i = 0; i < 50 && result == null; i++) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                result = jobResults.get(jobId);
                if (jobErrors.containsKey(jobId)) break;
            }
        }
        String err = jobErrors.get(jobId);
        return new JobStatus(jobId, state, pct, result, err);
    }

    private JobStatus.State mapState(String s) {
        if (s == null) return JobStatus.State.RUNNING;
        return switch (s.toUpperCase()) {
            case "DONE", "COMPLETED" -> JobStatus.State.DONE;
            case "CANCELLED" -> JobStatus.State.CANCELLED;
            case "FAILED" -> JobStatus.State.FAILED;
            default -> JobStatus.State.RUNNING;
        };
    }

    @Override
    public void cancel(String jobId) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("jobId", jobId);
        sendRequest("CANCEL_JOB", data);
    }

    private JobResult mapJobResult(Object resultObj) {
        if (!(resultObj instanceof Map<?, ?> rmap)) return null;
        Map<String, Object> r = (Map<String, Object>) rmap;
        Object plansObj = r.get("plans");
        List<ScheduleOption> options = new ArrayList<>();
        if (plansObj instanceof List<?> plist) {
            int rank = 1;
            for (Object po : plist) {
                PlanDto pdto = mapper.convertValue(po, PlanDto.class);
                ScheduleOption opt = planDtoToScheduleOption(pdto, rank++);
                options.add(opt);
            }
        }
        // dropped courses appended? JobResult only has options; dropped info in ScheduleOption.biLoai maybe.
        String maHocKy = "";
        if (!options.isEmpty()) maHocKy = options.get(0) == null ? "" : ""; // unknown
        // try get from first plan? PlanDto has semesterCode
        if (plansObj instanceof List<?> plist && !plist.isEmpty()) {
            PlanDto first = mapper.convertValue(plist.get(0), PlanDto.class);
            maHocKy = first.getSemesterCode();
        }
        return new JobResult(maHocKy, options);
    }

    private ScheduleOption planDtoToScheduleOption(PlanDto pdto, int rank) {
        List<SectionSlot> slots = new ArrayList<>();
        if (pdto.getSections() != null) {
            for (SectionDto sdto : pdto.getSections()) {
                slots.add(toSectionSlot(sdto));
            }
        }
        List<String> biLoai = new ArrayList<>();
        if (pdto.getDroppedCourses() != null) {
            for (PlanDto.DroppedCourseDto dc : pdto.getDroppedCourses()) {
                biLoai.add(dc.getCourseCode());
            }
        }
        OptionStats stats = new OptionStats(
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getStudyDays(),
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getGapPeriods(),
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getMorningPeriods(),
                computeTongTiet(slots)
        );
        String moTa = String.format("%d ngày học · %d phút trống · %d%% tiết buổi sáng",
                stats.soNgayHoc(), stats.phutTrong(), stats.tyLeSang());
        String ten = pdto.getName() != null && !pdto.getName().isBlank() ? pdto.getName() : String.valueOf((char) ('A' + Math.min(rank - 1, 25)));
        return new ScheduleOption(rank, ten, pdto.getScore(), stats, moTa, slots, biLoai, List.of());
    }

    private int computeTongTiet(List<SectionSlot> slots) {
        int total = 0;
        for (SectionSlot s : slots) {
            for (ClassSession cs : s.sessions()) {
                total += cs.soTiet();
            }
        }
        return total;
    }

    @Override
    public List<SavedPlan> listPlans(String maHocKy) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        if (maHocKy != null) data.put("semesterCode", maHocKy);
        Response resp = sendRequest("LIST_PLANS", data);
        List<SavedPlan> out = new ArrayList<>();
        Object plans = resp.getData() == null ? null : resp.getData().get("plans");
        if (plans instanceof List<?> list) {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                Object metricsObj = m.get("metrics");
                OptionStats stats = new OptionStats(0,0,0,0);
                if (metricsObj instanceof Map<?, ?> mm) {
                    stats = new OptionStats(
                            mm.get("studyDays") instanceof Number ? ((Number) mm.get("studyDays")).intValue() : 0,
                            mm.get("gapPeriods") instanceof Number ? ((Number) mm.get("gapPeriods")).intValue() : 0,
                            mm.get("morningPeriods") instanceof Number ? ((Number) mm.get("morningPeriods")).intValue() : 0,
                            0);
                }
                // metrics may be PlanDto.MetricsDto object
                if (metricsObj instanceof PlanDto.MetricsDto md) {
                    stats = new OptionStats(md.getStudyDays(), md.getGapPeriods(), md.getMorningPeriods(), 0);
                }
                long pid = m.get("id") instanceof Number ? ((Number) m.get("id")).longValue() : 0;
                int soHp = 0, soTc = 0, soBuoi = 0;
                try {
                    PlanDetail det = getPlan(pid);
                    java.util.Set<String> mon = new java.util.HashSet<>();
                    for (SectionSlot s : det.slots()) {
                        if (mon.add(s.section().maMh())) soTc += s.soTc();
                        soBuoi += s.sessions().size();
                    }
                    soHp = mon.size();
                } catch (Exception ignored) {}
                out.add(new SavedPlan(
                        pid,
                        String.valueOf(m.get("semesterCode")),
                        String.valueOf(m.get("name")),
                        m.get("score") instanceof Number ? ((Number) m.get("score")).doubleValue() : 0,
                        stats,
                        String.valueOf(m.getOrDefault("createdAt", "")),
                        soHp, soTc, soBuoi));
            }
        }
        return out;
    }

    @Override
    public PlanDetail getPlan(long planId) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        Response resp = sendRequest("GET_PLAN", data);
        Map<String, Object> d = resp.getData();
        PlanDto pdto = mapper.convertValue(d, PlanDto.class);
        // map sections
        List<SectionSlot> slots = new ArrayList<>();
        if (pdto.getSections() != null) {
            for (SectionDto sdto : pdto.getSections()) slots.add(toSectionSlot(sdto));
        }
        List<String> biLoai = new ArrayList<>();
        if (pdto.getDroppedCourses() != null) {
            for (PlanDto.DroppedCourseDto dc : pdto.getDroppedCourses()) biLoai.add(dc.getCourseCode());
        }
        OptionStats stats = new OptionStats(
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getStudyDays(),
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getGapPeriods(),
                pdto.getMetrics() == null ? 0 : pdto.getMetrics().getMorningPeriods(),
                computeTongTiet(slots));
        return new PlanDetail(pdto.getId() == null ? 0 : pdto.getId(), pdto.getSemesterCode(), pdto.getName(),
                pdto.getScore(), stats, slots, biLoai, pdto.getCreatedAt());
    }

    @Override
    public long savePlan(String maHocKy, String ten, List<Integer> sectionIds) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("semesterCode", maHocKy);
        data.put("name", ten);
        data.put("sectionIds", sectionIds == null ? List.of() : sectionIds);
        Response resp = sendRequest("SAVE_PLAN", data);
        Object pid = resp.getData() == null ? null : resp.getData().get("planId");
        if (pid == null) throw new ApiException(ErrorCode.UNKNOWN, "Server khong tra ve planId");
        return pid instanceof Number ? ((Number) pid).longValue() : Long.parseLong(String.valueOf(pid));
    }

    @Override
    public void deletePlan(long planId) throws ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("planId", planId);
        sendRequest("DELETE_PLAN", data);
    }

    @Override
    public void renamePlan(long planId, String tenMoi) throws ApiException {
        PlanDetail plan = getPlan(planId);
        List<Integer> ids = new ArrayList<>();
        for (SectionSlot s : plan.slots()) ids.add(s.section().id());
        long newId = savePlan(plan.maHocKy(), tenMoi, ids);
        if (newId != planId) {
            deletePlan(planId);
        }
    }
}
