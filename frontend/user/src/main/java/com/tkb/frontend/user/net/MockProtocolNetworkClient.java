package com.tkb.frontend.user.net;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.ErrorCode;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class MockProtocolNetworkClient implements NetworkClient {
    private final ObjectMapper objectMapper;
    private final Map<Command, Function<Request, Response>> responders = new ConcurrentHashMap<>();

    public MockProtocolNetworkClient() {
        this("USER");
    }

    public MockProtocolNetworkClient(String loginRole) {
        this(protocolObjectMapper());
        String role = Objects.requireNonNull(loginRole, "loginRole").trim().toUpperCase(Locale.ROOT);
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Mock login role must be USER or ADMIN");
        }
        responders.put(Command.LOGIN, request -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", "ADMIN".equals(role) ? 1L : 2L);
            data.put("username", request.getString("username") == null
                    ? ("ADMIN".equals(role) ? "admin" : "student") : request.getString("username"));
            data.put("role", role);
            return Response.success(request.getId(), data);
        });
    }

    MockProtocolNetworkClient(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        responders.put(Command.PING, request -> Response.successEmpty(request.getId()));
        registerProtocolSampleResponses();
    }

    private static ObjectMapper protocolObjectMapper() {
        return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private void registerProtocolSampleResponses() {
        Map<String, Object> section = sampleSection();
        Map<String, Object> metrics = Map.of("studyDays", 4, "gapPeriods", 2, "morningPeriods", 18);
        Map<String, Object> plan = samplePlan();
        Map<String, Object> job = Map.of(
                "jobId", "mock-job-1",
                "jobType", "OPTIMIZE",
                "username", "student",
                "status", "RUNNING",
                "percent", 50,
                "message", "Đang xử lý yêu cầu",
                "startedAt", "2026-10-31T09:30:00");
        Map<String, Object> user = Map.of(
                "userId", 2L,
                "username", "student",
                "role", "USER",
                "createdAt", "2026-10-01T08:00:00",
                "online", true);
        Map<String, Object> course = Map.of(
                "courseCode", "841109",
                "name", "Cơ sở dữ liệu",
                "credits", 3);
        Map<String, Object> adminCourse = Map.of(
                "courseCode", "841109",
                "name", "Cơ sở dữ liệu",
                "credits", 3,
                "sectionCount", 2,
                "activeSectionCount", 1);
        registerSuccessResponse(Command.REGISTER, Map.of());
        registerSuccessResponse(Command.LOGOUT, Map.of());
        registerSuccessResponse(Command.GET_SECTIONS, Map.of("sections", List.of(section)));
        registerSuccessResponse(Command.LIST_SEMESTERS, Map.of("semesters", List.of(
                Map.of("code", "2026-2027-1", "name", "Học kỳ I - Năm học 2026-2027", "current", true),
                Map.of("code", "2025-2026-2", "name", "Học kỳ II - Năm học 2025-2026", "current", false))));
        registerSuccessResponse(Command.LIST_COURSES, Map.of("courses", List.of(course)));
        registerSuccessResponse(Command.OPTIMIZE, Map.of("jobId", "mock-job-1"));
        registerSuccessResponse(Command.CANCEL_JOB, Map.of());
        registerSuccessResponse(Command.GET_JOB, job);
        registerSuccessResponse(Command.SAVE_PLAN, Map.of("planId", 17L));
        registerSuccessResponse(Command.LIST_PLANS, Map.of("plans", List.of(
                Map.of(
                        "id", 17L,
                        "name", "Phương án ít ngày",
                        "semesterCode", "2026-2027-1",
                        "score", 87.5,
                        "metrics", metrics,
                        "createdAt", "2026-10-31T09:30:00"))));
        registerSuccessResponse(Command.GET_PLAN, plan);
        registerSuccessResponse(Command.DELETE_PLAN, Map.of());

        registerSuccessResponse(Command.ADMIN_LIST_USERS, Map.of(
                "users", List.of(user),
                "total", 1));
        registerSuccessResponse(Command.ADMIN_SET_USER_ROLE, Map.of());
        registerSuccessResponse(Command.ADMIN_RESET_PASSWORD, Map.of());
        registerSuccessResponse(Command.ADMIN_DELETE_USER, Map.of());
        registerSuccessResponse(Command.ADMIN_CREATE_SEMESTER, Map.of());
        registerSuccessResponse(Command.ADMIN_SET_CURRENT_SEMESTER, Map.of());
        registerSuccessResponse(Command.ADMIN_LIST_COURSES, Map.of("courses", List.of(adminCourse)));
        registerSuccessResponse(Command.ADMIN_UPSERT_COURSE, Map.of());
        registerSuccessResponse(Command.ADMIN_DELETE_COURSE, Map.of());
        registerSuccessResponse(Command.ADMIN_UPSERT_SECTION, Map.of("id", 40L));
        registerSuccessResponse(Command.ADMIN_SET_SECTION_ACTIVE, Map.of());
        registerSuccessResponse(Command.ADMIN_DELETE_SECTION, Map.of());
        registerSuccessResponse(Command.IMPORT_BEGIN, Map.of(
                "uploadId", "mock-upload-1",
                "chunkSize", 524288));
        registerSuccessResponse(Command.IMPORT_CHUNK, Map.of("received", 1));
        registerSuccessResponse(Command.IMPORT_COMMIT, Map.of("jobId", "mock-import-job-1"));
        registerSuccessResponse(Command.IMPORT_ABORT, Map.of());
        registerSuccessResponse(Command.ADMIN_LIST_JOBS, Map.of("jobs", List.of(job)));
        registerSuccessResponse(Command.ADMIN_CANCEL_JOB, Map.of());
        registerSuccessResponse(Command.ADMIN_GET_STATS, Map.of(
                "onlineUsers", 1,
                "activeJobs", 1,
                "queuedJobs", 0,
                "totalUsers", 12,
                "totalPlans", 8,
                "totalSections", 42,
                "uptimeSeconds", 3600));
        registerSuccessResponse(Command.ADMIN_LIST_SESSIONS, Map.of("sessions", List.of(
                Map.of(
                        "sessionId", "mock-session-1",
                        "username", "student",
                        "remoteAddress", "127.0.0.1",
                        "connectedAt", "2026-10-31T09:30:00"))));
        registerSuccessResponse(Command.ADMIN_KICK_SESSION, Map.of());
    }

    public void registerResponder(Command command, Function<Request, Response> responder) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(responder, "responder");
        if (command == Command.JOB_PROGRESS || command == Command.JOB_DONE) {
            throw new IllegalArgumentException("Push commands cannot be request responders");
        }
        responders.put(command, responder);
    }

    public void registerSuccessResponse(Command command, Map<String, Object> responseData) {
        Objects.requireNonNull(responseData, "responseData");
        Map<String, Object> sampleData = copyResponseData(responseData);
        registerResponder(command, request -> Response.success(request.getId(),
                copyResponseData(sampleData)));
    }

    public void registerErrorResponse(Command command, ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode");
        registerResponder(command, request -> Response.error(request.getId(), errorCode));
    }

    public void registerSampleResponse(Command command, String responseJson)
            throws NetworkClientException {
        Objects.requireNonNull(command, "command");
        if (responseJson == null || responseJson.isBlank()) {
            throw new NetworkClientException("Sample response JSON must not be blank");
        }
        if (command == Command.JOB_PROGRESS || command == Command.JOB_DONE) {
            throw new IllegalArgumentException("Push commands cannot be request responders");
        }
        try {
            Response sample = objectMapper.readValue(responseJson, Response.class);
            if (sample == null || sample.isPush() || sample.getOk() == null
                    || (sample.getOk() && sample.getData() == null)
                    || (!sample.getOk() && sample.getError() == null)) {
                throw new NetworkClientException("Sample JSON is not a valid protocol response");
            }
            registerResponder(command, request -> {
                Response response = objectMapper.convertValue(sample, Response.class);
                response.setId(request.getId());
                return response;
            });
        } catch (JsonProcessingException ex) {
            throw new NetworkClientException("Sample response JSON is invalid", ex);
        }
    }

    private Map<String, Object> copyResponseData(Map<String, Object> data) {
        return objectMapper.convertValue(data, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    public String samplePushJson(Command pushCommand) throws NetworkClientException {
        return samplePushJson(pushCommand, "OPTIMIZE");
    }

    public String samplePushJson(Command pushCommand, String jobType) throws NetworkClientException {
        Objects.requireNonNull(pushCommand, "pushCommand");
        if (pushCommand != Command.JOB_PROGRESS && pushCommand != Command.JOB_DONE) {
            throw new IllegalArgumentException("Only job push commands are supported");
        }

        String normalizedJobType = Objects.requireNonNull(jobType, "jobType")
                .trim().toUpperCase(Locale.ROOT);
        if (!normalizedJobType.equals("OPTIMIZE") && !normalizedJobType.equals("IMPORT")) {
            throw new IllegalArgumentException("Job type must be OPTIMIZE or IMPORT");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", normalizedJobType.equals("IMPORT") ? "mock-import-job-1" : "mock-job-1");
        data.put("jobType", normalizedJobType);
        if (pushCommand == Command.JOB_PROGRESS) {
            data.put("status", "RUNNING");
            data.put("percent", 50);
            data.put("message", "Đang xử lý yêu cầu");
        } else {
            data.put("status", "DONE");
            if (normalizedJobType.equals("OPTIMIZE")) {
                data.put("result", Map.of("droppedCourses", List.of(), "plans", List.of(samplePlan())));
            } else {
                data.put("result", sampleImportReport());
            }
        }

        try {
            return objectMapper.writeValueAsString(Response.pushMessage(pushCommand, data));
        } catch (JsonProcessingException ex) {
            throw new NetworkClientException("Could not serialize sample push message", ex);
        }
    }

    private Map<String, Object> sampleImportReport() {
        return Map.of(
                "dryRun", true,
                "detectedRows", 1,
                "courses", Map.of("inserted", 0, "updated", 1),
                "sections", Map.of("inserted", 0, "updated", 1, "deactivated", 0),
                "errors", List.of(),
                "warnings", List.of());
    }

    private Map<String, Object> samplePlan() {
        return Map.of(
                "id", 17L,
                "name", "Phương án ít ngày",
                "semesterCode", "2026-2027-1",
                "rank", 1,
                "score", 87.5,
                "metrics", Map.of("studyDays", 4, "gapPeriods", 2, "morningPeriods", 18),
                "sections", List.of(sampleSection()),
                "droppedCourses", List.of(),
                "createdAt", "2026-10-31T09:30:00");
    }

    private Map<String, Object> sampleSection() {
        Map<String, Object> slot = Map.of(
                "dayOfWeek", 2,
                "startPeriod", 1,
                "periodCount", 3,
                "room", "C.A207",
                "weeks", "1234567890--------");
        Map<String, Object> section = Map.ofEntries(
                Map.entry("id", 40L),
                Map.entry("semesterCode", "2026-2027-1"),
                Map.entry("courseCode", "841109"),
                Map.entry("courseName", "Cơ sở dữ liệu"),
                Map.entry("credits", 3),
                Map.entry("groupNo", "01"),
                Map.entry("labGroup", ""),
                Map.entry("type", "LT"),
                Map.entry("lecturer", "Trương Tấn Khoa"),
                Map.entry("lecturerCode", "11384"),
                Map.entry("className", "DCT1251"),
                Map.entry("capacity", 50),
                Map.entry("active", true),
                Map.entry("slots", List.of(slot)));
        return section;
    }

    @Override
    public CompletableFuture<Response> send(Request request) {
        try {
            return CompletableFuture.completedFuture(createResponse(request));
        } catch (NetworkClientException ex) {
            return CompletableFuture.failedFuture(ex);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(
                    new NetworkClientException("Mock responder failed", ex));
        }
    }

    private Response createResponse(Request request) throws NetworkClientException {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new NetworkClientException("Request must have a positive id");
        }

        Command command = request.resolveCommand();
        if (command == null || command == Command.JOB_PROGRESS || command == Command.JOB_DONE) {
            throw new NetworkClientException("Request contains an invalid command");
        }

        Function<Request, Response> responder = responders.get(command);
        if (responder == null) {
            throw new NetworkClientException("No mock response is configured for " + command.name());
        }

        Response response = responder.apply(request);
        if (response == null || response.getId() == null
                || !response.getId().equals(request.getId())
                || response.getPush() != null
                || response.getOk() == null
                || (response.getOk() && response.getData() == null)
                || (!response.getOk() && response.getError() == null)) {
            throw new NetworkClientException("Mock responder returned an invalid response");
        }
        return response;
    }

    public String sendJson(String requestJson) throws NetworkClientException {
        if (requestJson == null || requestJson.isBlank()) {
            throw new NetworkClientException("Request JSON must not be blank");
        }
        try {
            Request request = objectMapper.readValue(requestJson, Request.class);
            return objectMapper.writeValueAsString(createResponse(request));
        } catch (JsonProcessingException ex) {
            throw new NetworkClientException("Request JSON is invalid", ex);
        }
    }
}
