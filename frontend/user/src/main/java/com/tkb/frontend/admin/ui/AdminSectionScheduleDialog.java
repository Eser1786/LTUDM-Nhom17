package com.tkb.frontend.admin.ui;

import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.ui.TimetableGridPanel;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

final class AdminSectionScheduleDialog {
    private AdminSectionScheduleDialog() {
    }

    static void show(SectionDto section) {
        List<ClassSession> visibleSessions = section.getSlots().stream()
                .filter(AdminSectionScheduleDialog::isVisibleInGrid)
                .map(slot -> new ClassSession(slot.getDayOfWeek(), slot.getStartPeriod(),
                        slot.getPeriodCount(), slot.getRoom(), slot.getWeeks()))
                .toList();
        SectionSlot timetableSlot = toTimetableSlot(section, visibleSessions);
        TimetableGridPanel grid = new TimetableGridPanel();
        grid.setTieuDe("Lịch nhóm " + section.getGroupNo());
        grid.setSlots(List.of(timetableSlot));

        VBox content = new VBox(10, grid);
        content.setPadding(new Insets(10));
        List<String> scheduleNotes = unsupportedScheduleNotes(section.getSlots());
        if (!scheduleNotes.isEmpty()) {
            Label scheduleNote = new Label(String.join("\n", scheduleNotes));
            scheduleNote.setWrapText(true);
            scheduleNote.setTextFill(Color.web("#9A3412"));
            content.getChildren().add(scheduleNote);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Lịch nhóm lớp");
        dialog.setHeaderText(section.getCourseCode() + " - nhóm " + section.getGroupNo());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1000, 650);
        dialog.showAndWait();
    }

    private static SectionSlot toTimetableSlot(
            SectionDto section, List<ClassSession> visibleSessions) {
        Long id = section.getId();
        Section timetableSection = new Section(
                id == null ? 0 : id.intValue(),
                section.getSemesterCode(),
                section.getCourseCode(),
                section.getGroupNo(),
                section.getLabGroup(),
                section.getType(),
                section.getCapacity(),
                section.getLecturer());
        return new SectionSlot(
                timetableSection,
                section.getCourseName(),
                section.getCredits() == null ? 0 : section.getCredits(),
                visibleSessions);
    }

    private static boolean isVisibleInGrid(SlotDto slot) {
        return slot.getDayOfWeek() >= 2 && slot.getDayOfWeek() <= 7
                && slot.getEndPeriod() <= 10;
    }

    private static List<String> unsupportedScheduleNotes(List<SlotDto> slots) {
        List<String> notes = new ArrayList<>();
        List<SlotDto> sundaySlots = slots.stream()
                .filter(slot -> slot.getDayOfWeek() == 8)
                .toList();
        if (!sundaySlots.isEmpty()) {
            notes.add("Lưới D1.1 không có cột Chủ nhật: " + slotSummary(sundaySlots));
        }

        List<SlotDto> extendedSlots = slots.stream()
                .filter(slot -> !isVisibleInGrid(slot) && slot.getDayOfWeek() != 8)
                .toList();
        if (!extendedSlots.isEmpty()) {
            notes.add("Lưới D1.1 chỉ hiển thị đến tiết 10: " + slotSummary(extendedSlots));
        }
        return notes;
    }

    private static String slotSummary(List<SlotDto> slots) {
        return slots.stream()
                .map(slot -> dayShort(slot.getDayOfWeek()) + " "
                        + slot.getStartPeriod() + "-" + slot.getEndPeriod()
                        + (slot.getRoom() == null || slot.getRoom().isBlank()
                                ? "" : " " + slot.getRoom()))
                .reduce((first, second) -> first + "; " + second)
                .orElse("—");
    }

    private static String dayShort(int day) {
        return day == 8 ? "CN" : "T" + day;
    }
}
