package com.tkb.frontend.admin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AdminJobProtocol {
    private AdminJobProtocol() {
    }

    static List<AdminJobOption> parseListResponse(Map<String, Object> responseData) {
        if (responseData == null) {
            return List.of();
        }
        Object rawJobs = responseData.get("jobs");
        if (!(rawJobs instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách công việc không hợp lệ.");
        }

        List<AdminJobOption> jobs = new ArrayList<>();
        for (Object rawJob : rawList) {
            if (!(rawJob instanceof Map<?, ?> jobMap)) {
                throw new IllegalArgumentException("Dữ liệu một công việc không hợp lệ.");
            }
            jobs.add(parseJob(jobMap));
        }
        return jobs;
    }

    static AdminJobOption parseJob(Map<?, ?> data) {
        String jobId = stringOrEmpty(data.get("jobId"));
        String jobType = stringOrEmpty(data.get("jobType"));
        String username = stringOrEmpty(data.get("username"));
        String status = stringOrEmpty(data.get("status"));
        String message = stringOrEmpty(data.get("message"));
        String startedAt = stringOrEmpty(data.get("startedAt"));
        int percent = intValue(data.get("percent"), 0);

        if (jobId.isBlank()) {
            throw new IllegalArgumentException("Công việc thiếu mã jobId hợp lệ.");
        }

        return new AdminJobOption(jobId, jobType, username, status, percent, message, startedAt);
    }

    private static String stringOrEmpty(Object obj) {
        return obj instanceof String s ? s.trim() : "";
    }

    private static int intValue(Object obj, int defaultValue) {
        if (obj instanceof Number num) {
            return Math.max(0, Math.min(100, num.intValue()));
        }
        return defaultValue;
    }
}
