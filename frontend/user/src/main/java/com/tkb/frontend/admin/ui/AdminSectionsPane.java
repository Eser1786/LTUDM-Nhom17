package com.tkb.frontend.admin.ui;

import com.tkb.common.dto.SectionDto;
import com.tkb.common.protocol.Command;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AdminSectionsPane extends VBox {
    private final AdminRequestClient requests;
    private final ComboBox<AdminSemesterOption> semesterSelector = new ComboBox<>();
    private final ComboBox<AdminCourseOption> courseSelector = new ComboBox<>();
    private final Button refreshButton = new Button("Tải lại");
    private final AdminSectionFormPane formPane;
    private final AdminSectionsTablePane tablePane;

    private SectionDto editingSection;
    private boolean busy;
    private boolean updatingSelections;

    AdminSectionsPane(AdminRequestClient requests) {
        super(14);
        this.requests = requests;
        formPane = new AdminSectionFormPane(
                this::saveSection,
                this::clearForm,
                this::showError,
                this::setStatus);
        tablePane = new AdminSectionsTablePane(new AdminSectionsTablePane.Actions() {
            @Override
            public void viewSchedule(SectionDto section) {
                AdminSectionScheduleDialog.show(section);
            }

            @Override
            public void edit(SectionDto section) {
                beginEdit(section);
            }

            @Override
            public void toggleActive(SectionDto section) {
                confirmToggleActive(section);
            }

            @Override
            public void delete(SectionDto section) {
                confirmDelete(section);
            }
        });
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);
        configureSelectors();
        getChildren().setAll(createScrollableContent());
    }

    void onShown() {
        loadSemesters();
    }

    private void configureSelectors() {
        semesterSelector.setMaxWidth(Double.MAX_VALUE);
        semesterSelector.setPromptText("Chọn học kỳ");
        semesterSelector.setCellFactory(list -> semesterCell());
        semesterSelector.setButtonCell(semesterCell());
        semesterSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingSelections && newValue != null) {
                clearForm();
                loadCourses(newValue);
            }
        });

        courseSelector.setMaxWidth(Double.MAX_VALUE);
        courseSelector.setPromptText("Chọn học phần");
        courseSelector.setCellFactory(list -> courseCell());
        courseSelector.setButtonCell(courseCell());
        courseSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingSelections && newValue != null) {
                clearForm();
                loadSections();
            }
        });
    }

    private ScrollPane createScrollableContent() {
        VBox content = new VBox(14,
                createSelectionCard(),
                formPane,
                tablePane);
        content.setPadding(new Insets(0, 0, 8, 0));
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox createSelectionCard() {
        Label title = new Label("Chọn học kỳ và học phần");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));
        Label semesterLabel = new Label("Học kỳ");
        Label courseLabel = new Label("Học phần");
        semesterLabel.setTextFill(Color.web("#756572"));
        courseLabel.setTextFill(Color.web("#756572"));

        refreshButton.setOnAction(event -> loadSemesters());
        refreshButton.setStyle(secondaryButtonStyle());
        VBox semesterBox = new VBox(6, semesterLabel, semesterSelector);
        VBox courseBox = new VBox(6, courseLabel, courseSelector);
        HBox.setHgrow(semesterBox, Priority.ALWAYS);
        HBox.setHgrow(courseBox, Priority.ALWAYS);
        HBox selectors = new HBox(12, semesterBox, courseBox, refreshButton);
        selectors.setAlignment(Pos.BOTTOM_LEFT);

        VBox card = new VBox(14, title, selectors);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
        return card;
    }

    private void loadSemesters() {
        if (!requireConnection("tải danh sách học kỳ") || busy) {
            return;
        }
        String previouslySelected = semesterSelector.getValue() == null
                ? null : semesterSelector.getValue().code();
        setBusy(true);
        setStatus("Đang tải danh sách học kỳ...", false);
        requests.send(Command.LIST_SEMESTERS, Map.of()).whenComplete((data, failure) ->
                Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể tải danh sách học kỳ: ", failure);
                        return;
                    }
                    try {
                        List<AdminSemesterOption> semesters = AdminSemesterOption.fromResponse(data);
                        AdminSemesterOption selected = semesters.stream()
                                .filter(item -> Objects.equals(item.code(), previouslySelected))
                                .findFirst()
                                .orElseGet(() -> semesters.stream()
                                        .filter(AdminSemesterOption::current)
                                        .findFirst()
                                        .orElse(semesters.isEmpty() ? null : semesters.get(0)));
                        updatingSelections = true;
                        semesterSelector.setItems(FXCollections.observableArrayList(semesters));
                        semesterSelector.setValue(selected);
                        updatingSelections = false;
                        if (selected == null) {
                            courseSelector.getItems().clear();
                            tablePane.setSections(List.of());
                            setStatus("Chưa có học kỳ. Hãy tạo học kỳ trước khi quản lý nhóm lớp.", true);
                        } else {
                            loadCourses(selected);
                        }
                    } catch (IllegalArgumentException ex) {
                        updatingSelections = false;
                        showError(ex.getMessage());
                    }
                }));
    }

    private void loadCourses(AdminSemesterOption semester) {
        if (semester == null || busy) {
            return;
        }
        tablePane.setSections(List.of());
        if (!requireConnection("tải học phần")) {
            return;
        }
        setBusy(true);
        setStatus("Đang tải học phần của " + semester.name() + "...", false);
        requests.send(Command.ADMIN_LIST_COURSES,
                Map.of("semesterCode", semester.code(), "keyword", ""))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể tải học phần: ", failure);
                        return;
                    }
                    try {
                        List<AdminCourseOption> courses = AdminCourseOption.fromResponse(data);
                        String previousCode = courseSelector.getValue() == null
                                ? null : courseSelector.getValue().courseCode();
                        AdminCourseOption selected = courses.stream()
                                .filter(item -> Objects.equals(item.courseCode(), previousCode))
                                .findFirst()
                                .orElse(courses.isEmpty() ? null : courses.get(0));
                        updatingSelections = true;
                        courseSelector.setItems(FXCollections.observableArrayList(courses));
                        courseSelector.setValue(selected);
                        updatingSelections = false;
                        if (selected == null) {
                            tablePane.setSections(List.of());
                            setStatus("Học kỳ này chưa có học phần để mở nhóm lớp.", false);
                        } else {
                            loadSections();
                        }
                    } catch (IllegalArgumentException ex) {
                        updatingSelections = false;
                        showError(ex.getMessage());
                    }
                }));
    }

    private void loadSections() {
        AdminSemesterOption semester = semesterSelector.getValue();
        AdminCourseOption course = courseSelector.getValue();
        if (semester == null || course == null || busy
                || !requireConnection("tải danh sách nhóm lớp")) {
            return;
        }
        setBusy(true);
        setStatus("Đang tải nhóm lớp...", false);
        requests.send(Command.GET_SECTIONS, Map.of(
                        "semesterCode", semester.code(),
                        "courseCode", course.courseCode(),
                        "includeInactive", true))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể tải nhóm lớp: ", failure);
                        return;
                    }
                    try {
                        List<SectionDto> sections = AdminSectionProtocol.readSections(data);
                        tablePane.setSections(sections);
                        setStatus("Đã tải " + sections.size() + " nhóm lớp.", false);
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void saveSection() {
        if (busy) {
            return;
        }
        SectionDto section = formPane.buildSection(
                semesterSelector.getValue(), courseSelector.getValue(), editingSection);
        if (section == null || !requireConnection("lưu nhóm lớp")) {
            return;
        }
        setBusy(true);
        setStatus(editingSection == null ? "Đang thêm nhóm lớp..." : "Đang cập nhật nhóm lớp...", false);
        requests.send(Command.ADMIN_UPSERT_SECTION, AdminSectionProtocol.toRequestData(section))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể lưu nhóm lớp: ", failure);
                        return;
                    }
                    clearForm();
                    setStatus("Đã lưu nhóm lớp. Đang cập nhật danh sách...", false);
                    loadSections();
                }));
    }

    private void confirmToggleActive(SectionDto section) {
        if (busy || section.getId() == null) {
            return;
        }
        boolean active = !section.isActive();
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(active ? "Mở nhóm lớp" : "Tắt nhóm lớp");
        confirmation.setHeaderText((active ? "Mở lại " : "Tắt ") + section.getCourseCode()
                + " - nhóm " + section.getGroupNo() + "?");
        confirmation.setContentText(active
                ? "Nhóm lớp sẽ được đưa vào danh sách để sinh viên đăng ký/xếp lịch."
                : "Nhóm lớp vẫn được giữ lại nhưng sẽ không còn được dùng để xếp lịch.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> setSectionActive(section, active));
    }

    private void setSectionActive(SectionDto section, boolean active) {
        if (!requireConnection(active ? "mở nhóm lớp" : "tắt nhóm lớp")) {
            return;
        }
        setBusy(true);
        setStatus(active ? "Đang mở nhóm lớp..." : "Đang tắt nhóm lớp...", false);
        requests.send(Command.ADMIN_SET_SECTION_ACTIVE, Map.of(
                        "sectionId", section.getId(),
                        "active", active))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể đổi trạng thái nhóm lớp: ", failure);
                        return;
                    }
                    loadSections();
                }));
    }

    private void confirmDelete(SectionDto section) {
        if (busy || section.getId() == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xóa nhóm lớp");
        confirmation.setHeaderText("Xóa " + section.getCourseCode() + " - nhóm "
                + section.getGroupNo() + "?");
        confirmation.setContentText(
                "Thao tác này xóa hẳn nhóm lớp và lịch học. Nếu nhóm đang nằm trong phương án đã lưu, "
                        + "máy chủ sẽ từ chối xóa; hãy dùng nút Tắt nhóm.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> deleteSection(section));
    }

    private void deleteSection(SectionDto section) {
        if (!requireConnection("xóa nhóm lớp")) {
            return;
        }
        setBusy(true);
        setStatus("Đang xóa nhóm lớp...", false);
        requests.send(Command.ADMIN_DELETE_SECTION, Map.of("sectionId", section.getId()))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (failure != null) {
                        showRequestError("Không thể xóa nhóm lớp: ", failure);
                        return;
                    }
                    if (editingSection != null
                            && Objects.equals(editingSection.getId(), section.getId())) {
                        clearForm();
                    }
                    loadSections();
                }));
    }

    private void beginEdit(SectionDto section) {
        if (busy) {
            return;
        }
        editingSection = section;
        formPane.setEditing(section);
        setStatus("Đang chỉnh sửa nhóm lớp " + section.getGroupNo() + ".", false);
    }

    private void clearForm() {
        editingSection = null;
        formPane.clear();
    }

    private boolean requireConnection(String action) {
        if (requests.isAvailable()) {
            return true;
        }
        showError("Không có kết nối máy chủ để " + action + ".");
        return false;
    }

    private void setBusy(boolean value) {
        busy = value;
        semesterSelector.setDisable(value || !requests.isAvailable());
        courseSelector.setDisable(value || !requests.isAvailable());
        refreshButton.setDisable(value || !requests.isAvailable());
        formPane.setBusy(value);
        tablePane.setBusy(value);
    }

    private void showRequestError(String prefix, Throwable failure) {
        showError(prefix + AdminRequestClient.messageOf(failure));
    }

    private void showError(String message) {
        setStatus(message, true);
    }

    private void setStatus(String message, boolean error) {
        tablePane.setStatus(message, error);
    }

    private ListCell<AdminSemesterOption> semesterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AdminSemesterOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        };
    }

    private ListCell<AdminCourseOption> courseCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AdminCourseOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.courseCode() + " - " + item.name());
            }
        };
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 7 9 7 9; -fx-cursor: hand;";
    }
}
