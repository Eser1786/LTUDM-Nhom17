package com.tkb.frontend.admin.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AdminSectionProtocol {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AdminSectionProtocol() {
    }

    static List<SectionDto> readSections(Map<String, Object> responseData) {
        Object rawSections = responseData.get("sections");
        if (!(rawSections instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Phản hồi danh sách nhóm lớp không hợp lệ.");
        }
        List<SectionDto> sections = new ArrayList<>();
        for (Object rawSection : rawList) {
            if (!(rawSection instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Dữ liệu một nhóm lớp không hợp lệ.");
            }
            SectionDto section = OBJECT_MAPPER.convertValue(rawSection, SectionDto.class);
            if (section.getId() == null || section.getCourseCode() == null
                    || section.getGroupNo() == null || section.getSlots() == null) {
                throw new IllegalArgumentException("Dữ liệu nhóm lớp bị thiếu trường bắt buộc.");
            }
            sections.add(section);
        }
        return sections;
    }

    static Map<String, Object> toRequestData(SectionDto section) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", section.getId());
        data.put("semesterCode", section.getSemesterCode());
        data.put("courseCode", section.getCourseCode());
        data.put("courseName", section.getCourseName());
        data.put("credits", section.getCredits());
        data.put("groupNo", section.getGroupNo());
        data.put("labGroup", section.getLabGroup());
        data.put("type", section.getType());
        data.put("lecturer", section.getLecturer());
        data.put("lecturerCode", section.getLecturerCode());
        data.put("className", section.getClassName());
        data.put("capacity", section.getCapacity());
        data.put("active", section.isActive());
        data.put("slots", section.getSlots().stream().map(AdminSectionProtocol::slotRequestData).toList());
        return data;
    }

    static SlotDto copySlot(SlotDto slot) {
        return new SlotDto(slot.getDayOfWeek(), slot.getStartPeriod(), slot.getPeriodCount(),
                slot.getRoom(), slot.getWeeks(), slot.getType(), slot.getLabGroup(), slot.getWeekMask());
    }

    private static Map<String, Object> slotRequestData(SlotDto slot) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dayOfWeek", slot.getDayOfWeek());
        data.put("startPeriod", slot.getStartPeriod());
        data.put("periodCount", slot.getPeriodCount());
        data.put("room", slot.getRoom());
        data.put("weeks", slot.getWeeks());
        data.put("type", slot.getType());
        data.put("labGroup", slot.getLabGroup());
        data.put("weekMask", slot.getWeekMask());
        return data;
    }
}
