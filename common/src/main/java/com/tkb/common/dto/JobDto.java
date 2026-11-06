package com.tkb.common.dto;

import com.tkb.common.protocol.Response.ErrorPayload;
import java.util.Map;

/**
 * B2.4 - JobDto: Cau truc du lieu trang thai tien trinh tinh toan tren Server
 * (dung cho JOB_PROGRESS, JOB_DONE, GET_JOB va ADMIN_LIST_JOBS).
 *
 * Cac gia tri chuan:
 *   - jobType: "OPTIMIZE" hoac "IMPORT"
 *   - status:  "QUEUED", "RUNNING", "DONE", "CANCELLED", "FAILED"
 *   - percent: 0 den 100
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/JobDto.java
 */
public class JobDto {

    public static final String TYPE_OPTIMIZE = "OPTIMIZE";
    public static final String TYPE_IMPORT = "IMPORT";

    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";

    private String jobId;
    private String jobType;
    private String username;
    private String status;
    private int percent;
    private String message;
    private String startedAt;
    private Map<String, Object> result;
    private ErrorPayload error;

    public JobDto() {
    }

    public JobDto(String jobId, String jobType, String username, String status, int percent, String message, String startedAt) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.username = username;
        this.status = status;
        this.percent = percent;
        this.message = message;
        this.startedAt = startedAt;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public ErrorPayload getError() {
        return error;
    }

    public void setError(ErrorPayload error) {
        this.error = error;
    }
}