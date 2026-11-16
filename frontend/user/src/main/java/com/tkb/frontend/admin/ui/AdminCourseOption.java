package com.tkb.frontend.admin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record AdminCourseOption(
        String courseCode,
        String name,
        int credits,
        int sectionCount,
        int activeSectionCount) {

    static List<AdminCourseOption> fromResponse(Map<String, Object> responseData) {
        Object rawCourses = responseData.get("courses");
        if (!(rawCourses instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách học phần không hợp lệ.");
        }

        List<AdminCourseOption> courses = new ArrayList<>();
        for (Object rawItem : rawList) {
            if (!(rawItem instanceof Map<?, ?> item)) {
                throw new IllegalArgumentException("Dữ liệu một học phần không hợp lệ.");
            }
            Object courseCode = item.get("courseCode");
            Object name = item.get("name");
            if (!(courseCode instanceof String code) || code.isBlank()
                    || !(name instanceof String courseName) || courseName.isBlank()) {
                throw new IllegalArgumentException("Dữ liệu học phần bị thiếu mã hoặc tên.");
            }
            courses.add(new AdminCourseOption(
                    code,
                    courseName,
                    readNonNegativeInt(item, "credits"),
                    readNonNegativeInt(item, "sectionCount"),
                    readNonNegativeInt(item, "activeSectionCount")));
        }
        return courses;
    }

    private static int readNonNegativeInt(Map<?, ?> item, String field) {
        Object value = item.get(field);
        if (!(value instanceof Number number)
                || number.longValue() < 0
                || number.longValue() > Integer.MAX_VALUE
                || number.doubleValue() != number.longValue()) {
            throw new IllegalArgumentException("Trường '" + field + "' của học phần không hợp lệ.");
        }
        return number.intValue();
    }
}
