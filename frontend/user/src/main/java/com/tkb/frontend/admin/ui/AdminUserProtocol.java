package com.tkb.frontend.admin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record AdminUserPage(List<AdminUserOption> users, int total) {
    AdminUserPage {
        users = List.copyOf(users);
    }
}

final class AdminUserProtocol {
    private AdminUserProtocol() {
    }

    static AdminUserPage parseListResponse(Map<String, Object> responseData) {
        Object rawUsers = responseData.get("users");
        if (!(rawUsers instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách người dùng không hợp lệ.");
        }

        List<AdminUserOption> users = new ArrayList<>();
        for (Object rawUser : rawList) {
            if (!(rawUser instanceof Map<?, ?> user)) {
                throw new IllegalArgumentException("Dữ liệu một người dùng không hợp lệ.");
            }
            users.add(parseUser(user));
        }

        int total = readNonNegativeInt(responseData, "total");
        if (users.size() > total) {
            throw new IllegalArgumentException("Số người dùng trả về vượt quá tổng số bản ghi.");
        }
        return new AdminUserPage(users, total);
    }

    private static AdminUserOption parseUser(Map<?, ?> user) {
        Object rawId = user.get("userId");
        Object rawUsername = user.get("username");
        Object rawRole = user.get("role");
        Object rawCreatedAt = user.get("createdAt");
        Object rawOnline = user.get("online");

        if (!(rawId instanceof Number idNumber)
                || idNumber.longValue() <= 0
                || idNumber.doubleValue() != idNumber.longValue()
                || !(rawUsername instanceof String username)
                || username.isBlank()
                || !(rawRole instanceof String role)
                || (!"USER".equals(role) && !"ADMIN".equals(role))
                || !(rawCreatedAt instanceof String createdAt)
                || !(rawOnline instanceof Boolean online)) {
            throw new IllegalArgumentException("Dữ liệu người dùng thiếu trường hoặc sai định dạng.");
        }
        return new AdminUserOption(idNumber.longValue(), username, role, createdAt, online);
    }

    private static int readNonNegativeInt(Map<?, ?> data, String field) {
        Object value = data.get(field);
        if (!(value instanceof Number number)
                || number.longValue() < 0
                || number.longValue() > Integer.MAX_VALUE
                || number.doubleValue() != number.longValue()) {
            throw new IllegalArgumentException("Trường '" + field + "' của danh sách người dùng không hợp lệ.");
        }
        return number.intValue();
    }
}
