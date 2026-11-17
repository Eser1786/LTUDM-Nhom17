package com.tkb.frontend.admin.ui;

import java.util.List;
import java.util.Map;

import com.tkb.common.protocol.Command;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private final TextField courseCodeField = new TextField();
    private final TextField courseNameField = new TextField();
    private final TextField creditsField = new TextField();
    private final Label formTitle = new Label("Thêm học phần");
    private final Label statusLabel = new Label();
    private final Button searchButton = new Button("Tìm kiếm");
    private final Button refreshButton = new Button("Tải lại");
    private final Button saveButton = new Button("Thêm học phần");
    private final Button clearFormButton = new Button("Làm mới biểu mẫu");
    private AdminCourseOption editingCourse;
    private boolean loading;

    AdminCoursesPane(AdminRequestClient requests) {
        super(16);
        this.requests = requests;
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);
        getChildren().setAll(createFormCard(), createListCard());
    }

    void onShown() {
        loadCourses();
    }

    private VBox createFormCard() {
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        formTitle.setTextFill(Color.web("#493343"));

        courseCodeField.setPromptText("Mã học phần (1-10 chữ hoặc số)");
        courseNameField.setPromptText("Tên học phần (tối đa 150 ký tự)");
        creditsField.setPromptText("Số tín chỉ (0-20)");
        courseCodeField.setMaxWidth(Double.MAX_VALUE);
        courseNameField.setMaxWidth(Double.MAX_VALUE);
        creditsField.setMaxWidth(Double.MAX_VALUE);

        saveButton.setOnAction(event -> saveCourse());
        saveButton.setStyle(primaryButtonStyle());
        clearFormButton.setOnAction(event -> clearForm());
        clearFormButton.setStyle(secondaryButtonStyle());

        HBox fields = new HBox(12, courseCodeField, courseNameField, creditsField);
        fields.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(courseCodeField, Priority.ALWAYS);
        HBox.setHgrow(courseNameField, Priority.ALWAYS);
        courseCodeField.setPrefWidth(200);
        courseNameField.setPrefWidth(420);
        creditsField.setPrefWidth(140);

        HBox actions = new HBox(10, saveButton, clearFormButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("Mã học phần 1-10 ký tự chữ/số; tên tối đa 150 ký tự; tín chỉ từ 0 đến 20.");
        hint.setTextFill(Color.web("#82747E"));
        VBox card = new VBox(14, formTitle, fields, actions, hint);
        styleCard(card);
        return card;
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

        HBox searchControls = new HBox(10, searchField, searchButton, refreshButton);
        searchControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        BorderPane toolbar = new BorderPane();
        toolbar.setLeft(title);
        toolbar.setRight(searchControls);

        TableColumn<AdminCourseOption, String> codeColumn = new TableColumn<>("Mã học phần");
        codeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().courseCode()));
        codeColumn.setPrefWidth(140);

        TableColumn<AdminCourseOption, String> nameColumn = new TableColumn<>("Tên học phần");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().name()));
        nameColumn.setPrefWidth(360);

        TableColumn<AdminCourseOption, String> creditsColumn = new TableColumn<>("Tín chỉ");
        creditsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Integer.toString(cell.getValue().credits())));
        creditsColumn.setPrefWidth(90);

        TableColumn<AdminCourseOption, String> sectionsColumn =
                new TableColumn<>("Nhóm trong học kỳ hiện tại");
        sectionsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Integer.toString(cell.getValue().sectionCount())));
        sectionsColumn.setPrefWidth(180);

        TableColumn<AdminCourseOption, String> activeSectionsColumn =
                new TableColumn<>("Nhóm đang mở");
        activeSectionsColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Integer.toString(cell.getValue().activeSectionCount())));
        activeSectionsColumn.setPrefWidth(125);

        TableColumn<AdminCourseOption, Void> actionColumn = createActionColumn();
        actionColumn.setPrefWidth(165);

        courseTable.getColumns().setAll(
                codeColumn, nameColumn, creditsColumn, sectionsColumn, activeSectionsColumn, actionColumn);
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        courseTable.setPlaceholder(new Label("Không có học phần phù hợp."));
        courseTable.setPrefHeight(420);
        VBox.setVgrow(courseTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));
        VBox card = new VBox(14, toolbar, courseTable, statusLabel);
        styleCard(card);
        return card;
    }

    private TableColumn<AdminCourseOption, Void> createActionColumn() {
        TableColumn<AdminCourseOption, Void> column = new TableColumn<>("Thao tác");
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button editButton = new Button("Sửa");
            private final Button deleteButton = new Button("Xóa");
            private final HBox actions = new HBox(8, editButton, deleteButton);

            {
                editButton.setStyle(secondaryButtonStyle());
                deleteButton.setStyle(dangerButtonStyle());
                editButton.setOnAction(event -> {
                    AdminCourseOption course = getCourseAtRow();
                    if (course != null) {
                        beginEdit(course);
                    }
                });
                deleteButton.setOnAction(event -> {
                    AdminCourseOption course = getCourseAtRow();
                    if (course != null) {
                        confirmDelete(course);
                    }
                });
                actions.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getCourseAtRow() == null) {
                    setGraphic(null);
                } else {
                    editButton.setDisable(loading);
                    deleteButton.setDisable(loading);
                    setGraphic(actions);
                }
            }

            private AdminCourseOption getCourseAtRow() {
                return getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex())
                        : null;
            }
        });
        return column;
    }

    private void saveCourse() {
        if (loading) {
            return;
        }
        AdminCourseOption course = validateCourseForm();
        if (course == null) {
            return;
        }
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để lưu học phần.");
            return;
        }

        setLoading(true);
        setStatus(editingCourse == null ? "Đang thêm học phần..." : "Đang cập nhật học phần...", false);
        Map<String, Object> requestData = Map.of(
                "courseCode", course.courseCode(),
                "name", course.name(),
                "credits", course.credits());
        requests.send(Command.ADMIN_UPSERT_COURSE, requestData)
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể lưu học phần: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    clearForm();
                    loadCourses();
                }));
    }

    private AdminCourseOption validateCourseForm() {
        String courseCode = courseCodeField.getText() == null
                ? "" : courseCodeField.getText().trim();
        String name = courseNameField.getText() == null
                ? "" : courseNameField.getText().trim();
        String rawCredits = creditsField.getText() == null
                ? "" : creditsField.getText().trim();

        if (!courseCode.matches("^[A-Za-z0-9]{1,10}$")) {
            showError("Mã học phần phải có từ 1 đến 10 ký tự chữ hoặc số.");
            courseCodeField.requestFocus();
            return null;
        }
        if (name.isBlank() || name.length() > 150) {
            showError("Tên học phần không được để trống và tối đa 150 ký tự.");
            courseNameField.requestFocus();
            return null;
        }

        int credits;
        try {
            credits = Integer.parseInt(rawCredits);
        } catch (NumberFormatException ex) {
            showError("Số tín chỉ phải là số nguyên từ 0 đến 20.");
            creditsField.requestFocus();
            return null;
        }
        if (credits < 0 || credits > 20) {
            showError("Số tín chỉ phải từ 0 đến 20.");
            creditsField.requestFocus();
            return null;
        }
        return new AdminCourseOption(courseCode, name, credits, 0, 0);
    }

    private void beginEdit(AdminCourseOption course) {
        if (loading) {
            return;
        }
        editingCourse = course;
        courseCodeField.setText(course.courseCode());
        courseNameField.setText(course.name());
        creditsField.setText(Integer.toString(course.credits()));
        formTitle.setText("Chỉnh sửa học phần");
        saveButton.setText("Cập nhật học phần");
        courseCodeField.setDisable(true);
        setStatus("Đang chỉnh sửa " + course.courseCode() + ".", false);
        courseNameField.requestFocus();
    }

    private void clearForm() {
        editingCourse = null;
        courseCodeField.clear();
        courseNameField.clear();
        creditsField.clear();
        formTitle.setText("Thêm học phần");
        saveButton.setText("Thêm học phần");
        courseCodeField.setDisable(loading);
        courseNameField.setDisable(loading);
        creditsField.setDisable(loading);
    }

    private void confirmDelete(AdminCourseOption course) {
        if (loading) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xóa học phần");
        confirmation.setHeaderText("Xóa " + course.courseCode() + " - " + course.name() + "?");
        confirmation.setContentText(
                "Không thể hoàn tác thao tác này. Học phần đang được nhóm lớp hoặc phương án đã lưu "
                        + "sử dụng sẽ không thể xóa.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> deleteCourse(course));
    }

    private void deleteCourse(AdminCourseOption course) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để xóa học phần.");
            return;
        }
        setLoading(true);
        setStatus("Đang xóa học phần " + course.courseCode() + "...", false);
        requests.send(Command.ADMIN_DELETE_COURSE, Map.of("courseCode", course.courseCode()))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể xóa học phần: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    if (editingCourse != null
                            && editingCourse.courseCode().equals(course.courseCode())) {
                        clearForm();
                    }
                    loadCourses();
                }));
    }

    private void loadCourses() {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách học phần.");
            return;
        }
        if (loading) {
            return;
        }
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.length() > 150) {
            showError("Từ khóa tìm kiếm không được vượt quá 150 ký tự.");
            searchField.requestFocus();
            return;
        }
        setLoading(true);
        setStatus("Đang tải danh sách học phần...", false);
        requests.send(Command.ADMIN_LIST_COURSES, Map.of("keyword", keyword))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể tải danh sách học phần: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        List<AdminCourseOption> courses = AdminCourseOption.fromResponse(data);
                        courseTable.getItems().setAll(courses);
                        setStatus("Đã tải " + courses.size() + " học phần.", false);
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void setLoading(boolean value) {
        loading = value;
        searchField.setDisable(value);
        searchButton.setDisable(value || !requests.isAvailable());
        refreshButton.setDisable(value || !requests.isAvailable());
        courseCodeField.setDisable(value || editingCourse != null);
        courseNameField.setDisable(value);
        creditsField.setDisable(value);
        saveButton.setDisable(value || !requests.isAvailable());
        clearFormButton.setDisable(value);
        courseTable.refresh();
    }

    private void styleCard(VBox card) {
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(error ? "#B4234D" : "#82747E"));
    }

    private void showError(String message) {
        setStatus(message, true);
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: #8D456B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 9 16 9 16; -fx-cursor: hand;";
    }

    private static String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 8 12 8 12; -fx-cursor: hand;";
    }

    private static String dangerButtonStyle() {
        return "-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 8; -fx-padding: 8 12 8 12; -fx-cursor: hand;";
    }
}
