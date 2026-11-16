package com.tkb.frontend.admin.ui;

import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

final class AdminSectionsTablePane extends VBox {
    interface Actions {
        void viewSchedule(SectionDto section);

        void edit(SectionDto section);

        void toggleActive(SectionDto section);

        void delete(SectionDto section);
    }

    private final TableView<SectionDto> sectionTable = new TableView<>();
    private final Label statusLabel = new Label();
    private boolean busy;

    AdminSectionsTablePane(Actions actions) {
        super(12);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
        getChildren().setAll(createTitle(), createTable(actions), statusLabel);
        VBox.setVgrow(sectionTable, Priority.ALWAYS);
        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        setStatus("Chọn học kỳ và học phần để xem nhóm lớp.", false);
    }

    void setSections(List<SectionDto> sections) {
        sectionTable.getItems().setAll(sections);
    }

    void setBusy(boolean value) {
        busy = value;
        sectionTable.refresh();
    }

    void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(error ? "#B4234D" : "#82747E"));
    }

    private Label createTitle() {
        Label title = new Label("Danh sách nhóm lớp");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));
        return title;
    }

    private TableView<SectionDto> createTable(Actions actions) {
        TableColumn<SectionDto, String> groupColumn = new TableColumn<>("Nhóm");
        groupColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getGroupNo())));
        groupColumn.setPrefWidth(70);

        TableColumn<SectionDto, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(sectionTypeLabel(cell.getValue())));
        typeColumn.setPrefWidth(70);

        TableColumn<SectionDto, String> lecturerColumn = new TableColumn<>("Giảng viên");
        lecturerColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getLecturer())));
        lecturerColumn.setPrefWidth(170);

        TableColumn<SectionDto, String> classColumn = new TableColumn<>("Lớp");
        classColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().getClassName())));
        classColumn.setPrefWidth(100);

        TableColumn<SectionDto, String> capacityColumn = new TableColumn<>("Sĩ số");
        capacityColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getCapacity() <= 0
                        ? "—" : Integer.toString(cell.getValue().getCapacity())));
        capacityColumn.setPrefWidth(70);

        TableColumn<SectionDto, String> activeColumn = new TableColumn<>("Trạng thái");
        activeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().isActive() ? "Đang mở" : "Đã tắt"));
        activeColumn.setPrefWidth(90);

        TableColumn<SectionDto, String> slotsColumn = new TableColumn<>("Lịch học");
        slotsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(slotSummary(cell.getValue().getSlots())));
        slotsColumn.setPrefWidth(240);

        TableColumn<SectionDto, Void> actionsColumn = createActionsColumn(actions);
        actionsColumn.setPrefWidth(245);

        sectionTable.getColumns().setAll(List.of(
                groupColumn, typeColumn, lecturerColumn, classColumn,
                capacityColumn, activeColumn, slotsColumn, actionsColumn));
        sectionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        sectionTable.setPlaceholder(new Label("Chọn học kỳ và học phần để xem nhóm lớp."));
        sectionTable.setPrefHeight(320);
        return sectionTable;
    }

    private TableColumn<SectionDto, Void> createActionsColumn(Actions callbacks) {
        TableColumn<SectionDto, Void> column = new TableColumn<>("Thao tác");
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button scheduleButton = new Button("Lịch");
            private final Button editButton = new Button("Sửa");
            private final Button toggleButton = new Button();
            private final Button deleteButton = new Button("Xóa");
            private final HBox actions = new HBox(5,
                    scheduleButton, editButton, toggleButton, deleteButton);

            {
                scheduleButton.setStyle(secondaryButtonStyle());
                editButton.setStyle(secondaryButtonStyle());
                toggleButton.setStyle(secondaryButtonStyle());
                deleteButton.setStyle(dangerButtonStyle());
                scheduleButton.setOnAction(event -> applyToRow(callbacks::viewSchedule));
                editButton.setOnAction(event -> applyToRow(callbacks::edit));
                toggleButton.setOnAction(event -> applyToRow(callbacks::toggleActive));
                deleteButton.setOnAction(event -> applyToRow(callbacks::delete));
                actions.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                SectionDto section = getSectionAtRow();
                if (empty || section == null) {
                    setGraphic(null);
                    return;
                }
                toggleButton.setText(section.isActive() ? "Tắt" : "Mở");
                scheduleButton.setDisable(busy);
                editButton.setDisable(busy);
                toggleButton.setDisable(busy);
                deleteButton.setDisable(busy);
                setGraphic(actions);
            }

            private void applyToRow(java.util.function.Consumer<SectionDto> action) {
                SectionDto section = getSectionAtRow();
                if (!busy && section != null) {
                    action.accept(section);
                }
            }

            private SectionDto getSectionAtRow() {
                return getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex())
                        : null;
            }
        });
        return column;
    }

    private String sectionTypeLabel(SectionDto section) {
        return SectionDto.TYPE_TH.equalsIgnoreCase(section.getType())
                ? "TH " + nullToEmpty(section.getLabGroup())
                : "LT";
    }

    private String slotSummary(List<SlotDto> slots) {
        if (slots == null || slots.isEmpty()) {
            return "—";
        }
        return slots.stream()
                .map(slot -> dayShort(slot.getDayOfWeek()) + " "
                        + slot.getStartPeriod() + "-" + slot.getEndPeriod()
                        + (slot.getRoom() == null || slot.getRoom().isBlank()
                                ? "" : " " + slot.getRoom()))
                .reduce((first, second) -> first + "; " + second)
                .orElse("—");
    }

    private String dayShort(int day) {
        return day == 8 ? "CN" : "T" + day;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 7 9 7 9; -fx-cursor: hand;";
    }

    private String dangerButtonStyle() {
        return "-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 8; -fx-padding: 7 9 7 9; -fx-cursor: hand;";
    }
}
