package com.tkb.frontend.user.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.frontend.user.model.ErrorCode;
import com.tkb.frontend.user.model.Types;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.JobStatus;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.PlanRequest;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.model.Types.Semester;
import com.tkb.frontend.user.model.Types.Student;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HttpTkbClient implements TkbClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8081/api";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public HttpTkbClient() {
        this(DEFAULT_BASE_URL);
    }

    public HttpTkbClient(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
    }

    private static String normalize(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Override
    public Student sinhVien() throws ApiException {
        return get("/student/current", new TypeReference<Student>() {});
    }

    @Override
    public List<Semester> listSemesters() throws ApiException {
        return getList("/semesters", new TypeReference<List<Semester>>() {});
    }

    @Override
    public List<Course> listCourses(String maHocKy) throws ApiException {
        String url = "/courses" + (maHocKy == null || maHocKy.isBlank() ? "" : "?maHocKy=" + encode(maHocKy));
        return getList(url, new TypeReference<List<Course>>() {});
    }

    @Override
    public List<Types.SectionSlot> listSections(String maHocKy, String maMh) throws ApiException {
        String url = "/sections?maHocKy=" + encode(maHocKy) + "&maMh=" + encode(maMh);
        return getList(url, new TypeReference<List<Types.SectionSlot>>() {});
    }

    @Override
    public List<Types.SectionSlot> listAllSections(String maHocKy) throws ApiException {
        String url = "/sections" + (maHocKy == null || maHocKy.isBlank() ? "" : "?maHocKy=" + encode(maHocKy));
        return getList(url, new TypeReference<List<Types.SectionSlot>>() {});
    }

    @Override
    public String submit(PlanRequest request) throws ApiException {
        Map<String, Object> body = Map.of(
                "maHocKy", request.maHocKy(),
                "maMhTheoUuTien", request.maMhTheoUuTien() == null ? List.of() : request.maMhTheoUuTien(),
                "criteria", request.criteria(),
                "sectionIds", request.sectionIds() == null ? List.of() : request.sectionIds()
        );
        ApiEnvelope envelope = post("/plans/submit", body, ApiEnvelope.class);
        if (envelope == null || envelope.jobId == null || envelope.jobId.isBlank()) {
            throw new ApiException(ErrorCode.UNKNOWN, "Server không trả về jobId hợp lệ");
        }
        return envelope.jobId;
    }

    @Override
    public JobStatus jobStatus(String jobId) throws ApiException {
        return get("/jobs/" + encode(jobId), new TypeReference<JobStatus>() {});
    }

    @Override
    public void cancel(String jobId) throws ApiException {
        delete("/jobs/" + encode(jobId));
    }

    @Override
    public List<SavedPlan> listPlans(String maHocKy) throws ApiException {
        String url = "/plans" + (maHocKy == null || maHocKy.isBlank() ? "" : "?maHocKy=" + encode(maHocKy));
        return getList(url, new TypeReference<List<SavedPlan>>() {});
    }

    @Override
    public PlanDetail getPlan(long planId) throws ApiException {
        return get("/plans/" + planId, new TypeReference<PlanDetail>() {});
    }

    @Override
    public long savePlan(String maHocKy, String ten, List<Integer> sectionIds) throws ApiException {
        Map<String, Object> body = Map.of(
                "maHocKy", maHocKy,
                "ten", ten,
                "sectionIds", sectionIds == null ? List.of() : sectionIds
        );
        ApiEnvelope envelope = post("/plans/save", body, ApiEnvelope.class);
        if (envelope == null || envelope.planId == null) {
            throw new ApiException(ErrorCode.UNKNOWN, "Server không trả về planId hợp lệ");
        }
        return envelope.planId;
    }

    @Override
    public void deletePlan(long planId) throws ApiException {
        delete("/plans/" + planId);
    }

    @Override
    public void renamePlan(long planId, String tenMoi) throws ApiException {
        Map<String, Object> body = Map.of("tenMoi", tenMoi);
        put("/plans/" + planId + "/rename", body);
    }

    private <T> T get(String path, TypeReference<T> type) throws ApiException {
        HttpResponse<String> response = execute(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
        return readJson(response, type);
    }

    private <T> List<T> getList(String path, TypeReference<List<T>> type) throws ApiException {
        HttpResponse<String> response = execute(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
        return readJson(response, type);
    }

    private <T> T post(String path, Map<String, Object> body, Class<T> type) throws ApiException {
        String json = toJson(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = execute(request);
        if (type == ApiEnvelope.class) {
            return readJson(response, type);
        }
        return readJson(response, type);
    }

    private void put(String path, Map<String, Object> body) throws ApiException {
        String json = toJson(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        execute(request);
    }

    private void delete(String path) throws ApiException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .DELETE()
                .build();
        execute(request);
    }

    private HttpResponse<String> execute(HttpRequest request) throws ApiException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response;
            }
            throw parseError(response);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.NETWORK_ERROR, "Không thể kết nối tới backend: " + baseUrl, List.of(ex.getMessage()));
        }
    }

    private ApiException parseError(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        String message = "Yêu cầu tới backend thất bại (HTTP " + response.statusCode() + ")";
        try {
            Map<String, Object> payload = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            if (payload.containsKey("message")) {
                message = String.valueOf(payload.get("message"));
            }
            if (payload.containsKey("errorCode")) {
                String code = String.valueOf(payload.get("errorCode"));
                return new ApiException(ErrorCode.valueOf(code), message);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return new ApiException(ErrorCode.UNKNOWN, message, List.of(body));
    }

    private <T> T readJson(HttpResponse<String> response, TypeReference<T> type) throws ApiException {
        try {
            return mapper.readValue(response.body(), type);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.UNKNOWN, "Backend trả về dữ liệu không hợp lệ", List.of(ex.getMessage()));
        }
    }

    private <T> T readJson(HttpResponse<String> response, Class<T> type) throws ApiException {
        try {
            return mapper.readValue(response.body(), type);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.UNKNOWN, "Backend trả về dữ liệu không hợp lệ", List.of(ex.getMessage()));
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Serialize JSON thất bại", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static final class ApiEnvelope {
        public String jobId;
        public Long planId;
    }
}
