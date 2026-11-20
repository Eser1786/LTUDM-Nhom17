
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
import javafx.scene.control.ScrollPane;
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
TableColumn<AdminCourseOption, String> activeSectionsColumn =
        new TableColumn<>("Nhóm đang mở");
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

        TableColumn<AdminCourseOption, Void> actionColumn =
                createActionColumn();

        actionColumn.setPrefWidth(140);

        codeColumn.setStyle("-fx-alignment: CENTER;");
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        creditsColumn.setStyle("-fx-alignment: CENTER;");
        sectionsColumn.setStyle("-fx-alignment: CENTER;");

        courseTable.getColumns().setAll(
                codeColumn,
                nameColumn,
                creditsColumn,
                sectionsColumn,
                actionColumn
        );

        courseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        courseTable.setPlaceholder(
                new Label("Không có học phần phù hợp."));

        AdminTableStyle.apply(courseTable);

        courseTable.setPrefHeight(450);
        courseTable.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(courseTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));

        VBox card = new VBox(14, toolbar, courseTable, statusLabel);

        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #F8F7FC;"
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
                setPadding(Insets.EMPTY);
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

        Label courseTitle = new Label(
                course.courseCode() + " - " + course.name());
        courseTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        courseTitle.setTextFill(Color.web("#75405E"));
        courseTitle.setWrapText(true);

        Label courseInfo = new Label(
                "Số tín chỉ: " + course.credits()
                        + "     |     Tổng số nhóm lớp: " + sections.size());

        courseInfo.setFont(Font.font("Segoe UI", 12));
        courseInfo.setTextFill(Color.web("#82747E"));

        VBox courseHeader = new VBox(8, courseTitle, courseInfo);
        courseHeader.setPadding(new Insets(16));
        courseHeader.setStyle(
                "-fx-background-color: #F4EAF0;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #EAD5E0;"
                        + "-fx-border-radius: 10;"
        );

        VBox sectionList = new VBox(14);
        sectionList.setPadding(new Insets(4, 2, 12, 2));
        sectionList.setFillWidth(true);

        if (sections.isEmpty()) {
            Label emptyLabel = new Label(
                    "Học phần chưa có nhóm lớp trong học kỳ này.");

            emptyLabel.setFont(Font.font("Segoe UI", 13));
            emptyLabel.setTextFill(Color.web("#82747E"));

            VBox emptyCard = new VBox(emptyLabel);
            emptyCard.setPadding(new Insets(20));
            emptyCard.setStyle(
                    "-fx-background-color: white;"
                            + "-fx-background-radius: 10;"
                            + "-fx-border-color: #F0E7EC;"
                            + "-fx-border-radius: 10;"
            );

            sectionList.getChildren().add(emptyCard);

        } else {
            for (int i = 0; i < sections.size(); i++) {
                SectionDto section = sections.get(i);
                sectionList.getChildren().add(
                        createSectionCard(section, i + 1));
            }
        }

        VBox content = new VBox(16, courseHeader, sectionList);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #F8F7FC;");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: #F8F7FC;"
                        + "-fx-background-color: #F8F7FC;"
                        + "-fx-border-color: transparent;"
        );

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết học phần");
        dialog.setHeaderText("Thông tin chi tiết học phần");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(760, 650);

        dialog.showAndWait();
    }


    private VBox createSectionCard(SectionDto section, int index) {

        Label title = new Label(
                "NHÓM LỚP " + index + " - " + section.getGroupNo());

        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#8D456B"));

        Label status = new Label(
                section.isActive() ? "Đang mở" : "Đã đóng");

        status.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        status.setPadding(new Insets(5, 12, 5, 12));

        if (section.isActive()) {
            status.setStyle(
                    "-fx-background-color: #E8F5EC;"
                            + "-fx-text-fill: #27834A;"
                            + "-fx-background-radius: 20;"
            );
        } else {
            status.setStyle(
                    "-fx-background-color: #FCE8EC;"
                            + "-fx-text-fill: #B4234D;"
                            + "-fx-background-radius: 20;"
            );
        }

        HBox heading = new HBox(10, title, status);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox information = new VBox(9);

        information.getChildren().addAll(
                createDetailRow("Mã học phần", section.getCourseCode()),
                createDetailRow("Loại nhóm", section.getType()),
                createDetailRow("Giảng viên", valueOrDash(section.getLecturer()))
        );

        Label scheduleTitle = new Label("LỊCH HỌC VÀ PHÒNG");
        scheduleTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        scheduleTitle.setTextFill(Color.web("#75405E"));

        VBox scheduleList = new VBox(8);

        if (section.getSlots() == null || section.getSlots().isEmpty()) {

            Label noSchedule = new Label("Chưa có lịch học");
            noSchedule.setFont(Font.font("Segoe UI", 12));
            noSchedule.setTextFill(Color.web("#82747E"));

            scheduleList.getChildren().add(noSchedule);

        } else {
            for (SlotDto slot : section.getSlots()) {

                Label slotLabel = new Label(
                        dayName(slot.getDayOfWeek())
                                + "  |  Tiết " + slot.getStartPeriod()
                                + " - " + slot.getEndPeriod()
                                + "  |  Phòng " + valueOrDash(slot.getRoom())
                                + "  |  Tuần " + valueOrDash(slot.getWeeks())
                );

                slotLabel.setFont(Font.font("Segoe UI", 12));
                slotLabel.setTextFill(Color.web("#5A4755"));
                slotLabel.setWrapText(true);
                slotLabel.setPadding(new Insets(9, 12, 9, 12));
                slotLabel.setMaxWidth(Double.MAX_VALUE);
                slotLabel.setStyle(
                        "-fx-background-color: #F8F3F6;"
                                + "-fx-background-radius: 7;"
                                + "-fx-border-color: #F0E7EC;"
                                + "-fx-border-radius: 7;"
                );

                scheduleList.getChildren().add(slotLabel);
            }
        }

        VBox card = new VBox(
                14,
                heading,
                information,
                scheduleTitle,
                scheduleList
        );

        card.setPadding(new Insets(16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #EAD5E0;"
                        + "-fx-border-radius: 10;"
        );

        return card;
    }

    private HBox createDetailRow(String title, String value) {

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", 12));
        titleLabel.setTextFill(Color.web("#82747E"));
        titleLabel.setMinWidth(125);

        Label valueLabel = new Label(valueOrDash(value));
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 12));
        valueLabel.setTextFill(Color.web("#493343"));
        valueLabel.setWrapText(true);

        HBox row = new HBox(12, titleLabel, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
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

                        List<AdminCourseOption> filteredCourses = courses.stream()
                                .filter(course -> course.sectionCount() > 0)
                                .toList();

                        courseTable.getItems().setAll(filteredCourses);

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
