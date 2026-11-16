package com.tkb.frontend.admin.ui;

import com.tkb.common.dto.SlotDto;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class AdminSectionSlotEditor extends VBox {
    private static final List<Integer> DAYS_OF_WEEK = List.of(2, 3, 4, 5, 6, 7, 8);
    private static final List<String> SLOT_TYPES = List.of("LT", "TH");

    private final Consumer<String> onError;
    private final BiConsumer<String, Boolean> onStatus;
    private final TableView<SlotDto> slotTable = new TableView<>();
    private final ComboBox<Integer> daySelector = new ComboBox<>();
    private final TextField startPeriodField = new TextField();
    private final TextField periodCountField = new TextField();
    private final TextField roomField = new TextField();
    private final TextField weeksField = new TextField();
    private final ComboBox<String> slotTypeSelector = new ComboBox<>();
    private final TextField slotLabGroupField = new TextField();
    private final Button saveSlotButton = new Button("Thêm buổi");
    private final Button removeSlotButton = new Button("Xóa buổi chọn");

    private int editingSlotIndex = -1;

    AdminSectionSlotEditor(
            Consumer<String> onError,
            BiConsumer<String, Boolean> onStatus) {
        super(8);
        this.onError = onError;
        this.onStatus = onStatus;
        configureInputs();
        getChildren().setAll(createContent());
    }

    List<SlotDto> getSlots() {
        return slotTable.getItems().stream()
                .map(AdminSectionProtocol::copySlot)
                .toList();
    }

    void setSlots(List<SlotDto> slots) {
        slotTable.getItems().setAll(slots.stream()
                .map(AdminSectionProtocol::copySlot)
                .toList());
        clearSlotEditor();
    }

    void clear() {
        slotTable.getItems().clear();
        clearSlotEditor();
    }

    void setBusy(boolean busy) {
        for (TextField field : List.of(
                startPeriodField, periodCountField, roomField, weeksField, slotLabGroupField)) {
            field.setDisable(busy);
        }
        daySelector.setDisable(busy);
        slotTypeSelector.setDisable(busy);
        slotTable.setDisable(busy);
        saveSlotButton.setDisable(busy);
        removeSlotButton.setDisable(busy);
    }

    private VBox createContent() {
        TableColumn<SlotDto, String> dayColumn = new TableColumn<>("Thứ");
        dayColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(dayName(cell.getValue().getDayOfWeek())));
        dayColumn.setPrefWidth(95);

        TableColumn<SlotDto, String> periodColumn = new TableColumn<>("Tiết");
        periodColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getStartPeriod() + " - " + cell.getValue().getEndPeriod()));
        periodColumn.setPrefWidth(95);

        TableColumn<SlotDto, String> roomColumn = new TableColumn<>("Phòng");
        roomColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getRoom())));
        roomColumn.setPrefWidth(110);

        TableColumn<SlotDto, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getType()));
        typeColumn.setPrefWidth(70);

        TableColumn<SlotDto, String> labGroupColumn = new TableColumn<>("Tổ TH");
        labGroupColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getLabGroup())));
        labGroupColumn.setPrefWidth(70);

        TableColumn<SlotDto, String> weeksColumn = new TableColumn<>("Tuần học");
        weeksColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getWeeks())));
        weeksColumn.setPrefWidth(180);
        slotTable.getColumns().setAll(List.of(
                dayColumn, periodColumn, roomColumn, typeColumn, labGroupColumn, weeksColumn));
        slotTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        slotTable.setPlaceholder(new Label("Chưa thêm buổi học."));
        slotTable.setPrefHeight(150);
        slotTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        beginEditSlot(newValue);
                    }
                });

        HBox timeFields = new HBox(8,
                daySelector, startPeriodField, periodCountField, roomField, weeksField);
        timeFields.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(roomField, Priority.ALWAYS);
        HBox.setHgrow(weeksField, Priority.ALWAYS);
        HBox slotTypeFields = new HBox(8, slotTypeSelector, slotLabGroupField);
        slotTypeFields.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slotLabGroupField, Priority.ALWAYS);
        HBox slotActions = new HBox(8, saveSlotButton, removeSlotButton);
        slotActions.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, slotTable, timeFields, slotTypeFields, slotActions);
    }

    private void configureInputs() {
        daySelector.setItems(FXCollections.observableArrayList(DAYS_OF_WEEK));
        daySelector.setValue(2);
        daySelector.setCellFactory(list -> dayCell());
        daySelector.setButtonCell(dayCell());
        daySelector.setPrefWidth(135);
        slotTypeSelector.setItems(FXCollections.observableArrayList(SLOT_TYPES));
        slotTypeSelector.setValue("LT");
        slotTypeSelector.setPrefWidth(90);
        slotTypeSelector.setMaxWidth(Double.MAX_VALUE);
        startPeriodField.setPromptText("Tiết bắt đầu");
        periodCountField.setPromptText("Số tiết");
        roomField.setPromptText("Phòng");
        weeksField.setPromptText("Tuần học (tùy chọn)");
        slotLabGroupField.setPromptText("Tổ TH (nếu có)");
        startPeriodField.setPrefWidth(115);
        periodCountField.setPrefWidth(95);

        saveSlotButton.setOnAction(event -> addOrUpdateSlot());
        saveSlotButton.setStyle(primaryButtonStyle());
        removeSlotButton.setOnAction(event -> removeSelectedSlot());
        removeSlotButton.setStyle(dangerButtonStyle());
    }

    private void addOrUpdateSlot() {
        SlotDto slot = buildSlotFromEditor();
        if (slot == null) {
            return;
        }
        if (editingSlotIndex >= 0 && editingSlotIndex < slotTable.getItems().size()) {
            slotTable.getItems().set(editingSlotIndex, slot);
        } else {
            if (slotTable.getItems().size() >= 10) {
                onError.accept("Mỗi nhóm lớp chỉ được có tối đa 10 buổi học.");
                return;
            }
            slotTable.getItems().add(slot);
        }
        clearSlotEditor();
        onStatus.accept("Đã cập nhật danh sách buổi học.", false);
    }

    private SlotDto buildSlotFromEditor() {
        Integer day = daySelector.getValue();
        int startPeriod;
        int periodCount;
        try {
            startPeriod = Integer.parseInt(text(startPeriodField).trim());
            periodCount = Integer.parseInt(text(periodCountField).trim());
        } catch (NumberFormatException ex) {
            onError.accept("Tiết bắt đầu và số tiết phải là số nguyên.");
            return null;
        }
        if (day == null || day < 2 || day > 8 || startPeriod < 1
                || periodCount < 1 || startPeriod + periodCount - 1 > 12) {
            onError.accept("Lịch học phải có thứ 2-8, tiết bắt đầu từ 1 và kết thúc không quá tiết 12.");
            return null;
        }
        String room = text(roomField).trim();
        String weeks = text(weeksField).trim();
        String type = slotTypeSelector.getValue();
        String labGroup = text(slotLabGroupField).trim();
        if (room.length() > 20 || weeks.length() > 512 || labGroup.length() > 5) {
            onError.accept("Phòng tối đa 20 ký tự, tuần học 512 ký tự và tổ thực hành 5 ký tự.");
            return null;
        }
        if (type == null || !SLOT_TYPES.contains(type)) {
            onError.accept("Hãy chọn loại buổi học LT hoặc TH.");
            return null;
        }
        if ("TH".equals(type) && labGroup.isBlank()) {
            onError.accept("Buổi thực hành phải có mã tổ thực hành.");
            slotLabGroupField.requestFocus();
            return null;
        }
        return new SlotDto(day, startPeriod, periodCount, emptyToNull(room), weeks,
                type, labGroup, 0);
    }

    private void beginEditSlot(SlotDto slot) {
        editingSlotIndex = slotTable.getSelectionModel().getSelectedIndex();
        daySelector.setValue(slot.getDayOfWeek());
        startPeriodField.setText(Integer.toString(slot.getStartPeriod()));
        periodCountField.setText(Integer.toString(slot.getPeriodCount()));
        roomField.setText(nullToEmpty(slot.getRoom()));
        weeksField.setText(nullToEmpty(slot.getWeeks()));
        slotTypeSelector.setValue(slot.getType());
        slotLabGroupField.setText(nullToEmpty(slot.getLabGroup()));
        saveSlotButton.setText("Cập nhật buổi");
    }

    private void removeSelectedSlot() {
        int selectedIndex = slotTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            onError.accept("Hãy chọn buổi học cần xóa.");
            return;
        }
        slotTable.getItems().remove(selectedIndex);
        clearSlotEditor();
        onStatus.accept("Đã xóa buổi học khỏi biểu mẫu.", false);
    }

    private void clearSlotEditor() {
        editingSlotIndex = -1;
        slotTable.getSelectionModel().clearSelection();
        daySelector.setValue(2);
        startPeriodField.clear();
        periodCountField.clear();
        roomField.clear();
        weeksField.clear();
        slotTypeSelector.setValue("LT");
        slotLabGroupField.clear();
        saveSlotButton.setText("Thêm buổi");
    }

    private ListCell<Integer> dayCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : dayName(item));
            }
        };
    }

    private String dayName(int day) {
        return switch (day) {
            case 2 -> "Thứ Hai";
            case 3 -> "Thứ Ba";
            case 4 -> "Thứ Tư";
            case 5 -> "Thứ Năm";
            case 6 -> "Thứ Sáu";
            case 7 -> "Thứ Bảy";
            case 8 -> "Chủ Nhật";
            default -> "Thứ " + day;
        };
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

    private String dangerButtonStyle() {
        return "-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 8; -fx-padding: 7 9 7 9; -fx-cursor: hand;";
    }
}
