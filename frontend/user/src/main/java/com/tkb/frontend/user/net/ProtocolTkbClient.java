package com.tkb.frontend.user.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.common.dto.PlanDto;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import com.tkb.frontend.user.model.ErrorCode;
import com.tkb.frontend.user.model.Types;
import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.JobStatus;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.PlanRequest;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.model.Types.Semester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapts the shared JSON protocol to the domain interface used by the user screens.
 */
public final class ProtocolTkbClient implements TkbClient {
    private static final long REQUEST_TIMEOUT_SECONDS = 60;

    private final NetworkClient networkClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger nextRequestId = new AtomicInteger(1);
    private volatile String username;
    private volatile String role;

    public ProtocolTkbClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
    }

    public void setAuthenticatedUser(String username, String role) {
        this.username = (username == null || username.isBlank()) ? "user" : username;
        this.role = (role == null || role.isBlank()) ? "USER" : role;
    }

    @Override
    public void login(String username, String password) throws ApiException {
        Map<String, Object> data = Map.of("username", username, "password", password);
        Response response = send(Command.LOGIN, data);
        String role = value(response.getData(), "role", "USER");
        setAuthenticatedUser(username, role);
    }

    @Override
    public void logout() throws ApiException {
        send(Command.LOGOUT, Map.of());
        username = null;
        role = null;
    }

    @Override
    public String getUsername() {
        return username == null ? "" : username;
    }

    @Override
    public String getRole() {
        return role == null ? "USER" : role;
    }

    @Override
    public List<Semester> listSemesters() throws ApiException {
        List<Semester> semesters = new ArrayList<>();
        for (Object item : listData(send(Command.LIST_SEMESTERS, Map.of()), "semesters")) {
            Map<String, Object> data = asMap(item);
            semesters.add(new Semester(
                    value(data, "code", ""),
                    value(data, "name", ""),
                    Boolean.TRUE.equals(data.get("current")),
                    value(data, "startDate", ""),
                    value(data, "endDate", "")));
        }
        return semesters;
    }

    @Override
    public List<Course> listCourses(String semesterCode) throws ApiException {
        Map<String, Object> requestData = new HashMap<>();
        if (semesterCode != null) {
            requestData.put("semesterCode", semesterCode);
        }
        List<Course> courses = new ArrayList<>();
        for (Object item : listData(send(Command.LIST_COURSES, requestData), "courses")) {
            Map<String, Object> data = asMap(item);
            courses.add(new Course(
                    value(data, "courseCode", ""),
                    value(data, "name", ""),
                    number(data.get("credits")).intValue()));
        }
        return courses;
    }

    @Override
    public List<SectionSlot> listSections(String semesterCode, String courseCode) throws ApiException {
        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("semesterCode", semesterCode);
        requestData.put("courseCode", courseCode);
        List<SectionSlot> sections = new ArrayList<>();
        for (Object item : listData(send(Command.GET_SECTIONS, requestData), "sections")) {
            sections.add(toSectionSlot(mapper.convertValue(item, SectionDto.class)));
        }
        return sections;
    }

    @Override
    public List<SectionSlot> listAllSections(String semesterCode) throws ApiException {
        List<SectionSlot> sections = new ArrayList<>();
        for (Course course : listCourses(semesterCode)) {
            sections.addAll(listSections(semesterCode, course.maMh()));
        }
        return sections;
    }

    @Override
    public String submit(PlanRequest request) throws ApiException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semesterCode", request.maHocKy());
        data.put("courses", request.maMhTheoUuTien() == null ? List.of() : request.maMhTheoUuTien());
        data.put("sectionIds", request.sectionIds() == null ? List.of() : request.sectionIds());
        data.put("maxPlans", request.criteria() == null ? 3 : request.criteria().soPhuongAn());
        data.put("criteria", criteriaData(request.criteria()));
        Object jobId = send(Command.OPTIMIZE, data).getData().get("jobId");
        if (jobId == null) {
            throw new ApiException(ErrorCode.UNKNOWN, "Máy chủ không trả về mã công việc.");
        }
        return String.valueOf(jobId);
    }

    @Override
    public JobStatus jobStatus(String jobId) throws ApiException {
        Response response = send(Command.GET_JOB, Map.of("jobId", jobId));
        Map<String, Object> data = response.getData();
        String status = value(data, "status", "RUNNING");
        JobStatus.State state = switch (status.toUpperCase()) {
            case "DONE", "COMPLETED" -> JobStatus.State.DONE;
            case "CANCELLED" -> JobStatus.State.CANCELLED;
            case "FAILED" -> JobStatus.State.FAILED;
            default -> JobStatus.State.RUNNING;
        };
        int percent = number(data.get("percent")).intValue();
        Types.JobResult result = mapJobResult(data.get("result"));
        return new JobStatus(jobId, state, percent, result, value(data, "message", null));
    }

    @Override
    public void cancel(String jobId) throws ApiException {
        send(Command.CANCEL_JOB, Map.of("jobId", jobId));
    }

    @Override
    public List<SavedPlan> listPlans(String semesterCode) throws ApiException {
        Map<String, Object> requestData = new HashMap<>();
        if (semesterCode != null) {
            requestData.put("semesterCode", semesterCode);
        }
        List<SavedPlan> plans = new ArrayList<>();
        for (Object item : listData(send(Command.LIST_PLANS, requestData), "plans")) {
            Map<String, Object> data = asMap(item);
            PlanDto plan = mapper.convertValue(data, PlanDto.class);
            Long planId = plan.getId();
            List<SectionSlot> sections = mapSections(plan.getSections());
            OptionStats stats = stats(plan.getMetrics(), sections);
            plans.add(new SavedPlan(
                    planId == null ? 0 : planId,
                    plan.getSemesterCode(),
                    plan.getName(),
                    plan.getScore(),
                    stats,
                    plan.getCreatedAt(),
                    (int) sections.stream().map(s -> s.section().maMh()).distinct().count(),
                    totalCredits(sections),
                    sections.stream().mapToInt(s -> s.sessions().size()).sum()));
        }
        return plans;
    }

    @Override
    public PlanDetail getPlan(long planId) throws ApiException {
        PlanDto plan = mapper.convertValue(
                send(Command.GET_PLAN, Map.of("planId", planId)).getData(), PlanDto.class);
        Long resolvedPlanId = plan.getId();
        List<SectionSlot> sections = mapSections(plan.getSections());
        List<String> dropped = plan.getDroppedCourses().stream()
                .map(PlanDto.DroppedCourseDto::getCourseCode)
                .toList();
        return new PlanDetail(
                resolvedPlanId == null ? planId : resolvedPlanId,
                plan.getSemesterCode(),
                plan.getName(),
                plan.getScore(),
                stats(plan.getMetrics(), sections),
                sections,
                dropped,
                plan.getCreatedAt());
    }

    @Override
    public long savePlan(String semesterCode, String name, List<Integer> sectionIds) throws ApiException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semesterCode", semesterCode);
        data.put("name", name);
        data.put("sectionIds", sectionIds == null ? List.of() : sectionIds);
        Object planId = send(Command.SAVE_PLAN, data).getData().get("planId");
        if (planId instanceof Number number) {
            return number.longValue();
        }
        if (planId != null) {
            try {
                return Long.parseLong(String.valueOf(planId));
            } catch (NumberFormatException ex) {
                throw new ApiException(ErrorCode.UNKNOWN, "Máy chủ trả về mã thời khóa biểu không hợp lệ.");
            }
        }
        throw new ApiException(ErrorCode.UNKNOWN, "Máy chủ không trả về mã thời khóa biểu.");
    }

    @Override
    public void deletePlan(long planId) throws ApiException {
        send(Command.DELETE_PLAN, Map.of("planId", planId));
    }

    @Override
    public void renamePlan(long planId, String name) throws ApiException {
        PlanDetail plan = getPlan(planId);
        List<Integer> sectionIds = plan.slots().stream()
                .map(slot -> slot.section().id())
                .toList();
        long newId = savePlan(plan.maHocKy(), name, sectionIds);
        if (newId != planId) {
            deletePlan(planId);
        }
    }

    private Response send(Command command, Map<String, Object> data) throws ApiException {
        int requestId = nextRequestId.getAndIncrement();
        Request request = new Request(requestId, command, data);
        try {
            Response response = networkClient.send(request)
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (response == null || !Integer.valueOf(requestId).equals(response.getId())) {
                throw new ApiException(ErrorCode.NETWORK_ERROR, "Phản hồi không khớp với yêu cầu.");
            }
            if (!Boolean.TRUE.equals(response.getOk())) {
                Response.ErrorPayload error = response.getError();
                String message = error == null || error.getMessage() == null
                        ? "Yêu cầu thất bại."
                        : error.getMessage();
                throw new ApiException(mapError(error == null ? null : error.getCode()), message);
            }
            if (response.getData() == null) {
                throw new ApiException(ErrorCode.NETWORK_ERROR, "Phản hồi máy chủ thiếu dữ liệu.");
            }
            return response;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Đã gián đoạn khi chờ máy chủ phản hồi.");
        } catch (ExecutionException | CompletionException | TimeoutException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Không thể gửi yêu cầu: " + cause.getMessage());
        } catch (RuntimeException ex) {
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Không thể gửi yêu cầu: " + ex.getMessage());
        }
    }

    private ErrorCode mapError(String code) {
        if (code == null) {
            return ErrorCode.UNKNOWN;
        }
        return switch (code) {
            case "COURSE_NOT_OPEN" -> ErrorCode.COURSE_NOT_OPEN;
            case "NAME_TAKEN" -> ErrorCode.NAME_TAKEN;
            case "JOB_NOT_FOUND" -> ErrorCode.JOB_NOT_FOUND;
            case "SEMESTER_NOT_FOUND" -> ErrorCode.SEMESTER_NOT_FOUND;
            case "VALIDATION_FAILED", "VALIDATION_ERROR" -> ErrorCode.VALIDATION_ERROR;
            case "NOT_FOUND", "PLAN_NOT_FOUND" -> ErrorCode.PLAN_NOT_FOUND;
            case "JOB_CANCELLED" -> ErrorCode.JOB_CANCELLED;
            case "JOB_FAILED" -> ErrorCode.JOB_FAILED;
            default -> ErrorCode.UNKNOWN;
        };
    }

    private Map<String, Object> criteriaData(Types.Criteria criteria) {
        Types.Criteria value = criteria == null ? Types.Criteria.macDinh() : criteria;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minDays", value.itNgay() > 0);
        data.put("morningFirst", value.uuTienSang() > 0);
        data.put("avoidGaps", value.khoangTrong() > 0);
        data.put("weightMinDays", (double) value.itNgay());
        data.put("weightMorning", (double) value.uuTienSang());
        data.put("weightNoGaps", (double) value.khoangTrong());
        data.put("timeMode", value.timeMode() == null ? "NONE" : value.timeMode());
        data.put("preferredDays", value.preferredDays() == null ? List.of() : value.preferredDays());
        data.put("preferredSlotsMask", value.preferredSlotsMask());
        data.put("preferredCampus", value.preferredCampus() == null ? "ANY" : value.preferredCampus());
        data.put("avoidCampusSwitch", value.avoidCampusSwitch());
        data.put("weightCampus", (double) Math.max(1, value.ngayYeuThich()));
        data.put("preferredLecturers", value.preferredLecturers() == null ? Map.of() : value.preferredLecturers());
        data.put("lockedGroups", value.lockedGroups() == null ? Map.of() : value.lockedGroups());
        return data;
    }

    private SectionSlot toSectionSlot(SectionDto dto) {
        Long sectionId = dto.getId();
        Integer credits = dto.getCredits();
        Section section = new Section(
                sectionId == null ? 0 : sectionId.intValue(),
                dto.getSemesterCode(),
                dto.getCourseCode(),
                dto.getGroupNo(),
                dto.getLabGroup(),
                dto.getType(),
                dto.getCapacity(),
                dto.getLecturer());
        List<ClassSession> sessions = dto.getSlots().stream()
                .map(slot -> new ClassSession(
                        slot.getDayOfWeek(),
                        slot.getStartPeriod(),
                        slot.getPeriodCount(),
                        slot.getRoom(),
                        slot.getWeeks()))
                .toList();
        return new SectionSlot(section, dto.getCourseName(),
                credits == null ? 0 : credits, sessions);
    }

    private List<SectionSlot> mapSections(List<SectionDto> sections) {
        return sections == null ? List.of() : sections.stream().map(this::toSectionSlot).toList();
    }

    private OptionStats stats(PlanDto.MetricsDto metrics, List<SectionSlot> sections) {
        int totalPeriods = sections.stream()
                .flatMap(section -> section.sessions().stream())
                .mapToInt(ClassSession::soTiet)
                .sum();
        return new OptionStats(
                metrics == null ? 0 : metrics.getStudyDays(),
                metrics == null ? 0 : metrics.getGapPeriods(),
                metrics == null ? 0 : metrics.getMorningPeriods(),
                totalPeriods);
    }

    private Types.JobResult mapJobResult(Object result) throws ApiException {
        if (result == null) {
            return null;
        }
        Map<String, Object> resultData = asMap(result);
        List<ScheduleOption> options = new ArrayList<>();
        List<?> plans = listData(resultData, "plans");
        for (Object item : plans) {
            PlanDto plan = mapper.convertValue(item, PlanDto.class);
            List<SectionSlot> sections = mapSections(plan.getSections());
            OptionStats stats = stats(plan.getMetrics(), sections);
            List<String> dropped = plan.getDroppedCourses().stream()
                    .map(PlanDto.DroppedCourseDto::getCourseCode)
                    .toList();
            int rank = plan.getRank() > 0 ? plan.getRank() : options.size() + 1;
            String name = plan.getName() == null || plan.getName().isBlank()
                    ? String.valueOf((char) ('A' + Math.min(rank - 1, 25)))
                    : plan.getName();
            String description = String.format("%d ngày học · %d phút trống · %d%% tiết buổi sáng",
                    stats.soNgayHoc(), stats.phutTrong(), stats.tyLeSang());
            options.add(new ScheduleOption(rank, name, plan.getScore(), stats,
                    description, sections, dropped, List.of()));
        }
        String semesterCode = plans.isEmpty() ? "" : value(asMap(plans.get(0)), "semesterCode", "");
        return new Types.JobResult(semesterCode, options);
    }

    private int totalCredits(List<SectionSlot> sections) {
        Map<String, Integer> creditsByCourse = new HashMap<>();
        for (SectionSlot section : sections) {
            creditsByCourse.putIfAbsent(section.section().maMh(), section.soTc());
        }
        return creditsByCourse.values().stream().mapToInt(Integer::intValue).sum();
    }

    private List<?> listData(Response response, String key) throws ApiException {
        return listData(response.getData(), key);
    }

    private List<?> listData(Map<String, Object> data, String key) throws ApiException {
        Object value = data == null ? null : data.get(key);
        if (value instanceof List<?> list) {
            return list;
        }
        throw new ApiException(ErrorCode.NETWORK_ERROR,
                "Phản hồi máy chủ thiếu danh sách dữ liệu hợp lệ: " + key);
    }

    private Map<String, Object> asMap(Object value) throws ApiException {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        throw new ApiException(ErrorCode.NETWORK_ERROR, "Phản hồi máy chủ chứa đối tượng không hợp lệ.");
    }

    private String value(Map<String, Object> data, String key, String fallback) {
        Object value = data == null ? null : data.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }
}
