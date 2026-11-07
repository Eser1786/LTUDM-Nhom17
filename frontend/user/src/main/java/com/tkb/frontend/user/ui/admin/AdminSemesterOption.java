package com.tkb.frontend.user.ui.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record AdminSemesterOption(String code, String name, boolean current) {
    static List<AdminSemesterOption> fromResponse(Map<String, Object> responseData) {
        Object rawSemesters = responseData.get("semesters");
        if (!(rawSemesters instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách học kỳ không hợp lệ.");
        }
        List<AdminSemesterOption> semesters = new ArrayList<>();
        for (Object rawItem : rawList) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                throw new IllegalArgumentException("Dữ liệu một học kỳ không hợp lệ.");
            }
            Object code = item.get("code");
            Object name = item.get("name");
            Object current = item.get("current");
            if (!(code instanceof String semesterCode) || semesterCode.isBlank()
                    || !(name instanceof String semesterName) || semesterName.isBlank()
                    || (current != null && !(current instanceof Boolean))) {
                throw new IllegalArgumentException("Dữ liệu một học kỳ bị thiếu hoặc sai định dạng.");
            }
            semesters.add(new AdminSemesterOption(semesterCode, semesterName,
                    Boolean.TRUE.equals(current)));
        }
        return semesters;
    }

    @Override
    public String toString() {
        return name + (current ? " (hiện tại)" : "");
    }
}
