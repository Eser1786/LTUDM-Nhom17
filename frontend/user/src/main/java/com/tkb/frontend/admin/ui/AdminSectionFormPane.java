package com.tkb.frontend.admin.ui;

import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AdminSectionFormPane extends VBox {
    private final Runnable onSave;
    private final Runnable onClear;
    private final Consumer<String> onError;
    private final BiConsumer<String, Boolean> onStatus;
    private final AdminSectionSlotEditor slotEditor;
    private final TextField groupNoField = new TextField();
    private final TextField capacityField = new TextField();
    private final TextField lecturerField = new TextField();
    private final TextField lecturerCodeField = new TextField();
    private final TextField classNameField = new TextField();
    private final Label formTitle = new Label("Thêm nhóm lớp");
    private final Button saveButton = new Button("Thêm nhóm lớp");
    private final Button clearButton = new Button("Làm mới biểu mẫu");

    AdminSectionFormPane(
            Runnable onSave,
            Runnable onClear,
            Consumer<String> onError,
            BiConsumer<String, Boolean> onStatus) {
        super(12);
        this.onSave = onSave;
        this.onClear = onClear;
        this.onError = onError;
        this.onStatus = onStatus;
        slotEditor = new AdminSectionSlotEditor(onError, onStatus);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
        getChildren().setAll(createFormContent());
    }

    SectionDto buildSection(
            AdminSemesterOption semester,
            AdminCourseOption course,
            SectionDto editingSection) {
        String groupNo = text(groupNoField).trim();
        if (semester == null || course == null) {
            onError.accept("Hãy chọn học kỳ và học phần trước khi lưu nhóm lớp.");
            return null;
        }
        if (groupNo.isBlank() || groupNo.length() > 5) {
            onError.accept("Số nhóm không được để trống và tối đa 5 ký tự.");
            groupNoField.requestFocus();
            return null;
        }

        Integer capacity = parseCapacity();
        if (capacity == null) {
            return null;
        }
        String lecturer = text(lecturerField).trim();
        String lecturerCode = text(lecturerCodeField).trim();
        String className = text(classNameField).trim();
        if (lecturer.length() > 100 || lecturerCode.length() > 10 || className.length() > 30) {
            onError.accept("Giảng viên tối đa 100, mã giảng viên 10 và tên lớp 30 ký tự.");
            return null;
        }
        List<SlotDto> slots = slotEditor.getSlots();
        if (slots.isEmpty() || slots.size() > 10) {
            onError.accept("Mỗi nhóm lớp phải có từ 1 đến 10 buổi học.");
            return null;
        }
        for (SlotDto slot : slots) {
            if ("TH".equalsIgnoreCase(slot.getType()) && slot.getLabGroup().isBlank()) {
                onError.accept("Mỗi buổi thực hành phải có mã tổ thực hành.");
                return null;
            }
        }

        SlotDto firstPracticeSlot = slots.stream()
                .filter(slot -> "TH".equalsIgnoreCase(slot.getType()))
                .findFirst()
                .orElse(null);
        SectionDto section = new SectionDto();
        section.setId(editingSection == null ? null : editingSection.getId());
        section.setSemesterCode(semester.code());
        section.setCourseCode(course.courseCode());
        section.setCourseName(course.name());
        section.setCredits(course.credits());
        section.setGroupNo(groupNo);
        section.setType(firstPracticeSlot == null ? SectionDto.TYPE_LT : SectionDto.TYPE_TH);
        section.setLabGroup(firstPracticeSlot == null ? "" : firstPracticeSlot.getLabGroup());
        section.setLecturer(emptyToNull(lecturer));
        section.setLecturerCode(emptyToNull(lecturerCode));
        section.setClassName(emptyToNull(className));
        section.setCapacity(capacity);
        section.setActive(editingSection == null || editingSection.isActive());
        section.setSlots(slots);
        return section;
    }

    void setEditing(SectionDto section) {
        groupNoField.setText(nullToEmpty(section.getGroupNo()));
        capacityField.setText(section.getCapacity() <= 0
                ? "" : Integer.toString(section.getCapacity()));
        lecturerField.setText(nullToEmpty(section.getLecturer()));
        lecturerCodeField.setText(nullToEmpty(section.getLecturerCode()));
        classNameField.setText(nullToEmpty(section.getClassName()));
        slotEditor.setSlots(section.getSlots());
        formTitle.setText("Chỉnh sửa nhóm lớp " + section.getGroupNo());
        saveButton.setText("Cập nhật nhóm lớp");
        groupNoField.requestFocus();
    }

    void clear() {
        groupNoField.clear();
        capacityField.clear();
        lecturerField.clear();
        lecturerCodeField.clear();
        classNameField.clear();
        slotEditor.clear();
        formTitle.setText("Thêm nhóm lớp");
        saveButton.setText("Thêm nhóm lớp");
    }

    void setBusy(boolean busy) {
        for (TextField field : List.of(
                groupNoField, capacityField, lecturerField, lecturerCodeField, classNameField)) {
            field.setDisable(busy);
        }
        slotEditor.setBusy(busy);
        saveButton.setDisable(busy);
        clearButton.setDisable(busy);
    }

    private VBox createFormContent() {
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        formTitle.setTextFill(Color.web("#493343"));
        groupNoField.setPromptText("Số nhóm (tối đa 5 ký tự)");
        capacityField.setPromptText("Sĩ số");
        lecturerField.setPromptText("Giảng viên");
        lecturerCodeField.setPromptText("Mã giảng viên");
        classNameField.setPromptText("Lớp");
        groupNoField.setPrefWidth(190);
        capacityField.setPrefWidth(130);
        lecturerField.setPrefWidth(250);
        lecturerCodeField.setPrefWidth(180);
        classNameField.setPrefWidth(180);
        for (TextField field : List.of(
                groupNoField, capacityField, lecturerField, lecturerCodeField, classNameField)) {
            field.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(field, Priority.ALWAYS);
        }

        HBox sectionFields = new HBox(10,
                groupNoField, capacityField, lecturerField, lecturerCodeField, classNameField);
        sectionFields.setAlignment(Pos.CENTER_LEFT);
        Label slotsTitle = new Label("Danh sách lịch học (từ 1 đến 10 buổi)");
        slotsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        slotsTitle.setTextFill(Color.web("#493343"));
        saveButton.setOnAction(event -> onSave.run());
        saveButton.setStyle(primaryButtonStyle());
        clearButton.setOnAction(event -> {
            onClear.run();
            onStatus.accept("Đã làm mới biểu mẫu.", false);
        });
        clearButton.setStyle(secondaryButtonStyle());
        HBox formActions = new HBox(10, saveButton, clearButton);
        formActions.setAlignment(Pos.CENTER_LEFT);
        return new VBox(12, formTitle, sectionFields, slotsTitle, slotEditor, formActions);
    }

    private Integer parseCapacity() {
        String rawCapacity = text(capacityField).trim();
        try {
            int capacity = rawCapacity.isBlank() ? 0 : Integer.parseInt(rawCapacity);
            if (capacity < 0) {
                throw new NumberFormatException();
            }
            return capacity;
        } catch (NumberFormatException ex) {
            onError.accept("Sĩ số phải là số nguyên không âm.");
            capacityField.requestFocus();
            return null;
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String primaryButtonStyle() {
        return "-fx-background-color: #8D456B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 8 12 8 12; -fx-cursor: hand;";
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 7 9 7 9; -fx-cursor: hand;";
    }
}
