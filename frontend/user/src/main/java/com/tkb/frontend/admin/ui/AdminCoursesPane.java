
package com.tkb.frontend.admin.ui;

import java.util.List;
import java.util.Map;

import com.tkb.common.dto.SectionDto;
import com.tkb.common.dto.SlotDto;
import com.tkb.common.protocol.Command;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

final class AdminCoursesPane extends VBox {

    private final AdminRequestClient requests;
    private final TableView<AdminCourseOption> courseTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();

    private final Button searchButton = new Button("Tìm kiếm");
    private final Button refreshButton = new Button("Tải lại");

    private String currentSemesterCode;
    private boolean loading;

    AdminCoursesPane(AdminRequestClient requests) {
        super(16);
        this.requests = requests;

        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);

        getChildren().setAll(createListCard());
    }

    void onShown() {
        loadCurrentSemester();
    }

    private void loadCurrentSemester() {
        if (!requests.isAvailable() || loading) {
            showError("Không có kết nối máy chủ.");
            return;
        }

        setLoading(true);
        setStatus("Đang tải học kỳ...", false);

        requests.send(Command.LIST_SEMESTERS, Map.of())
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);

                    if (failure != null) {
                        showError("Không thể tải học kỳ: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }

                    try {
                        List<AdminSemesterOption> semesters =
                                AdminSemesterOption.fromResponse(data);

                        AdminSemesterOption current = semesters.stream()
                                .filter(AdminSemesterOption::current)
                                .findFirst()
                                .orElse(null);

                        if (current == null) {
                            currentSemesterCode = null;
                            showError("Chưa có học kỳ hiện tại.");
                            return;
                        }

                        currentSemesterCode = current.code();
                        loadCourses();

                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private VBox createListCard() {

        Label title = new Label("Danh sách học phần");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));

        searchField.setPromptText("Tìm theo mã hoặc tên học phần");
        searchField.setOnAction(event -> loadCourses());

        searchButton.setOnAction(event -> loadCourses());
        searchButton.setStyle(primaryButtonStyle());

        refreshButton.setOnAction(event -> {
            searchField.clear();
            loadCourses();
        });
        refreshButton.setStyle(secondaryButtonStyle());

        HBox searchControls = new HBox(
                10, searchField, searchButton, refreshButton);
        searchControls.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(searchField, Priority.ALWAYS);

        BorderPane toolbar = new BorderPane();
        toolbar.setLeft(title);
        toolbar.setRight(searchControls);

        TableColumn<AdminCourseOption, String> codeColumn =
                new TableColumn<>("Mã học phần");

        codeColumn.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        cell.getValue().courseCode()));
        codeColumn.setPrefWidth(140);

        TableColumn<AdminCourseOption, String> nameColumn =
                new TableColumn<>("Tên học phần");

        nameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        cell.getValue().name()));
        nameColumn.setPrefWidth(360);

        TableColumn<AdminCourseOption, String> creditsColumn =
                new TableColumn<>("Tín chỉ");

        creditsColumn.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        Integer.toString(cell.getValue().credits())));
        creditsColumn.setPrefWidth(90);

        TableColumn<AdminCourseOption, String> sectionsColumn =
                new TableColumn<>("Nhóm lớp");

        sectionsColumn.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        Integer.toString(cell.getValue().sectionCount())));
        sectionsColumn.setPrefWidth(120);

        TableColumn<AdminCourseOption, String> activeSectionsColumn =
                new TableColumn<>("Nhóm đang mở");

        activeSectionsColumn.setCellValueFactory(cell ->
                new javafx.beans.property.ReadOnlyStringWrapper(
                        Integer.toString(cell.getValue().activeSectionCount())));
        activeSectionsColumn.setPrefWidth(120);

        TableColumn<AdminCourseOption, Void> actionColumn =
                createActionColumn();

        actionColumn.setPrefWidth(140);

        courseTable.getColumns().setAll(
                codeColumn,
                nameColumn,
                creditsColumn,
                sectionsColumn,
                activeSectionsColumn,
                actionColumn
        );

        courseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        courseTable.setPlaceholder(
                new Label("Không có học phần phù hợp."));

        courseTable.setPrefHeight(420);
        VBox.setVgrow(courseTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));

        VBox card = new VBox(14, toolbar, courseTable, statusLabel);

        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 14; " +
                "-fx-border-color: #F0E7EC; " +
                "-fx-border-radius: 14;"
        );

        return card;
    }

    private TableColumn<AdminCourseOption, Void> createActionColumn() {

        TableColumn<AdminCourseOption, Void> column =
                new TableColumn<>("Thao tác");

        column.setCellFactory(ignored -> new TableCell<>() {

            private final Button detailButton =
                    new Button("Xem chi tiết");

            {
                detailButton.setStyle(primaryButtonStyle());

                detailButton.setOnAction(event -> {
                    AdminCourseOption course = getCourseAtRow();

                    if (course != null) {
                        showCourseDetails(course);
                    }
                });

                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                AdminCourseOption course = getCourseAtRow();

                if (empty || course == null) {
                    setGraphic(null);
                } else {
                    detailButton.setDisable(loading);
                    setGraphic(detailButton);
                }
            }

            private AdminCourseOption getCourseAtRow() {
                return getIndex() >= 0
                        && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex())
                        : null;
            }
        });

        return column;
    }

    private void showCourseDetails(AdminCourseOption course) {

        if (loading || currentSemesterCode == null) {
            return;
        }

        setLoading(true);
        setStatus("Đang tải chi tiết học phần...", false);

        requests.send(Command.GET_SECTIONS, Map.of(
                "semesterCode", currentSemesterCode,
                "courseCode", course.courseCode(),
                "includeInactive", true
        )).whenComplete((data, failure) -> Platform.runLater(() -> {

            setLoading(false);

            if (failure != null) {
                showError("Không thể tải chi tiết: "
                        + AdminRequestClient.messageOf(failure));
                return;
            }

            try {
                List<SectionDto> sections =
                        AdminSectionProtocol.readSections(data);

                showDetailsDialog(course, sections);

                setStatus("Đã tải chi tiết học phần.", false);

            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }));
    }

    private void showDetailsDialog(
            AdminCourseOption course,
            List<SectionDto> sections) {

        StringBuilder content = new StringBuilder();

        content.append("MÃ HỌC PHẦN: ")
                .append(course.courseCode())
                .append("\n");

        content.append("TÊN HỌC PHẦN: ")
                .append(course.name())
                .append("\n");

        content.append("SỐ TÍN CHỈ: ")
                .append(course.credits())
                .append("\n");

        content.append("TỔNG SỐ NHÓM LỚP: ")
                .append(sections.size())
                .append("\n\n");

        if (sections.isEmpty()) {
            content.append("Học phần chưa có nhóm lớp trong học kỳ này.");
        }

        for (int i = 0; i < sections.size(); i++) {

            SectionDto section = sections.get(i);

            content.append("========== NHÓM LỚP ")
                    .append(i + 1)
                    .append(" ==========\n");

            content.append("Mã môn: ")
                    .append(section.getCourseCode())
                    .append("\n");

            content.append("Mã nhóm: ")
                    .append(section.getGroupNo())
                    .append("\n");

            content.append("Loại nhóm: ")
                    .append(section.getType())
                    .append("\n");

            content.append("Lớp: ")
                    .append(valueOrDash(section.getClassName()))
                    .append("\n");

            content.append("Giảng viên: ")
                    .append(valueOrDash(section.getLecturer()))
                    .append("\n");

            content.append("Mã giảng viên: ")
                    .append(valueOrDash(section.getLecturerCode()))
                    .append("\n");

            content.append("Sức chứa: ")
                    .append(section.getCapacity())
                    .append("\n");

            content.append("Trạng thái: ")
                    .append(section.isActive() ? "Đang mở" : "Đã đóng")
                    .append("\n");

            content.append("Lịch học và phòng:\n");

            if (section.getSlots() == null
                    || section.getSlots().isEmpty()) {
                content.append("  Chưa có lịch học\n");
            } else {
                for (SlotDto slot : section.getSlots()) {

                    content.append("  ")
                            .append(dayName(slot.getDayOfWeek()))
                            .append(", tiết ")
                            .append(slot.getStartPeriod())
                            .append("-")
                            .append(slot.getEndPeriod())
                            .append(", phòng ")
                            .append(valueOrDash(slot.getRoom()))
                            .append(", tuần ")
                            .append(valueOrDash(slot.getWeeks()))
                            .append("\n");
                }
            }

            content.append("\n");
        }

        TextArea detailArea = new TextArea(content.toString());
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setFont(Font.font("Segoe UI", 13));
        detailArea.setPrefRowCount(25);
        detailArea.setPrefColumnCount(65);

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Chi tiết học phần");
        dialog.setHeaderText(course.courseCode() + " - " + course.name());

        dialog.getDialogPane().setContent(detailArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(720, 600);

        dialog.showAndWait();
    }

    private String dayName(int day) {
        return switch (day) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            case 8 -> "Chủ nhật";
            default -> "Thứ " + day;
        };
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void loadCourses() {

        if (loading) {
            return;
        }

        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ.");
            return;
        }

        String keyword = searchField.getText() == null
                ? "" : searchField.getText().trim();

        if (keyword.length() > 150) {
            showError("Từ khóa tìm kiếm không được vượt quá 150 ký tự.");
            return;
        }

        setLoading(true);
        setStatus("Đang tải danh sách học phần...", false);

        requests.send(Command.ADMIN_LIST_COURSES,
                Map.of("keyword", keyword))
                .whenComplete((data, failure) -> Platform.runLater(() -> {

                    setLoading(false);

                    if (failure != null) {
                        showError("Không thể tải danh sách học phần: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }

                    try {
                        List<AdminCourseOption> courses =
                                AdminCourseOption.fromResponse(data);

                        courseTable.getItems().setAll(courses);

                        setStatus(
                                "Đã tải " + courses.size() + " học phần.",
                                false
                        );

                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void setLoading(boolean value) {

        loading = value;

        searchField.setDisable(value);
        searchButton.setDisable(value);
        refreshButton.setDisable(value);

        courseTable.refresh();
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(
                Color.web(error ? "#B4234D" : "#82747E"));
    }

    private void showError(String message) {
        setStatus(message, true);
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: #8D456B; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 9 16 9 16; " +
                "-fx-cursor: hand;";
    }

    private static String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; " +
                "-fx-text-fill: #75405E; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 8 12 8 12; " +
                "-fx-cursor: hand;";
    }
}
