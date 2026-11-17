package com.tkb.frontend.admin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AdminSessionProtocol {
    private AdminSessionProtocol() {
    }

    static List<AdminSessionOption> parseListResponse(Map<String, Object> responseData) {
        if (responseData == null) {
            return List.of();
        }
        Object rawSessions = responseData.get("sessions");
        if (!(rawSessions instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách phiên online không hợp lệ.");
        }

        List<AdminSessionOption> sessions = new ArrayList<>();
        for (Object rawSession : rawList) {
            if (!(rawSession instanceof Map<?, ?> sessionMap)) {
                throw new IllegalArgumentException("Dữ liệu một phiên kết nối không hợp lệ.");
            }
            sessions.add(parseSession(sessionMap));
        }
        return sessions;
    }

    static AdminSessionOption parseSession(Map<?, ?> data) {
        String sessionId = stringOrEmpty(data.get("sessionId"));
        String username = stringOrEmpty(data.get("username"));
        String remoteAddress = stringOrEmpty(data.get("remoteAddress"));
        String connectedAt = stringOrEmpty(data.get("connectedAt"));

        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("Phiên kết nối thiếu mã sessionId hợp lệ.");
        }

        return new AdminSessionOption(sessionId, username, remoteAddress, connectedAt);
    }

    private static String stringOrEmpty(Object obj) {
        return obj instanceof String s ? s.trim() : "";
    }
}
