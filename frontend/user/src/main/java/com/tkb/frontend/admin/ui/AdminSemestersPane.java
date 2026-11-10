package com.tkb.frontend.admin.ui;

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

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AdminSemestersPane extends VBox {
    private final AdminRequestClient requests;
    private final Runnable onSemestersChanged;
    private final TableView<AdminSemesterOption> semesterTable = new TableView<>();
    private final TextField semesterCodeField = new TextField();
    private final TextField semesterNameField = new TextField();
    private final Label statusLabel = new Label();
    private final Button createButton = new Button("Tạo học kỳ");
    private final Button refreshButton = new Button("Tải lại");
    private boolean loading;

    AdminSemestersPane(AdminRequestClient requests, Runnable onSemestersChanged) {
        super(16);
        this.requests = requests;
        this.onSemestersChanged = Objects.requireNonNull(onSemestersChanged, "onSemestersChanged");
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);
        getChildren().setAll(createFormCard(), createListCard());
    }

    void onShown() {
        loadSemesters(onSemestersChanged);
    }

    private VBox createFormCard() {
        Label title = new Label("Tạo học kỳ mới");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));

        semesterCodeField.setPromptText("Mã học kỳ, ví dụ 2026-2027-1");
        semesterCodeField.setMaxWidth(Double.MAX_VALUE);
        semesterNameField.setPromptText("Tên học kỳ");
        semesterNameField.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> createSemester());
        createButton.setStyle("-fx-background-color: #8D456B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 9 18 9 18; -fx-cursor: hand;");
        HBox form = new HBox(12, semesterCodeField, semesterNameField, createButton);
        form.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(semesterCodeField, Priority.ALWAYS);
        HBox.setHgrow(semesterNameField, Priority.ALWAYS);

        Label hint = new Label("Mã theo định dạng NNNN-NNNN-N; tên tối đa 100 ký tự.");
        hint.setTextFill(Color.web("#82747E"));
        VBox card = new VBox(14, title, form, hint);
        styleCard(card);
        return card;
    }

    private VBox createListCard() {
        Label title = new Label("Danh sách học kỳ");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));
        refreshButton.setOnAction(event -> loadSemesters());
        refreshButton.setStyle("-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 9 16 9 16; -fx-cursor: hand;");
        BorderPane toolbar = new BorderPane();
        toolbar.setLeft(title);
        toolbar.setRight(refreshButton);

        TableColumn<AdminSemesterOption, String> codeColumn = new TableColumn<>("Mã học kỳ");
        codeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().code()));
        codeColumn.setPrefWidth(180);

        TableColumn<AdminSemesterOption, String> nameColumn = new TableColumn<>("Tên học kỳ");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().name()));
        nameColumn.setPrefWidth(400);

        TableColumn<AdminSemesterOption, String> currentColumn = new TableColumn<>("Trạng thái");
        currentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().current() ? "Đang áp dụng" : "Chưa áp dụng"));
        currentColumn.setPrefWidth(150);

        TableColumn<AdminSemesterOption, Void> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button setCurrentButton = new Button("Đặt hiện tại");

            {
                setCurrentButton.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        confirmSetCurrent(getTableView().getItems().get(getIndex()));
                    }
                });
                setCurrentButton.setStyle("-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                        + "-fx-background-radius: 7; -fx-padding: 6 10 6 10; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setCurrentButton.setDisable(
                            getTableView().getItems().get(getIndex()).current() || loading);
                    setGraphic(setCurrentButton);
                }
            }
        });
        actionColumn.setPrefWidth(150);

        semesterTable.getColumns().setAll(codeColumn, nameColumn, currentColumn, actionColumn);
        semesterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        semesterTable.setPlaceholder(new Label("Chưa có học kỳ."));
        semesterTable.setPrefHeight(380);
        VBox.setVgrow(semesterTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#82747E"));
        statusLabel.setFont(Font.font("Segoe UI", 12));
        VBox card = new VBox(14, toolbar, semesterTable, statusLabel);
        styleCard(card);
        return card;
    }

    private void styleCard(VBox card) {
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
    }

    private void createSemester() {
        String code = semesterCodeField.getText() == null ? "" : semesterCodeField.getText().trim();
        String name = semesterNameField.getText() == null ? "" : semesterNameField.getText().trim();
        if (!code.matches("^\\d{4}-\\d{4}-\\d$")) {
            showError("Mã học kỳ phải theo định dạng NNNN-NNNN-N, ví dụ 2026-2027-1.");
            semesterCodeField.requestFocus();
            return;
        }
        if (name.isBlank() || name.length() > 100) {
            showError("Tên học kỳ phải có từ 1 đến 100 ký tự.");
            semesterNameField.requestFocus();
            return;
        }
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tạo học kỳ.");
            return;
        }
        if (loading) {
            return;
        }
        setLoading(true);
        setStatus("Đang tạo học kỳ...", false);
        requests.send(Command.ADMIN_CREATE_SEMESTER, Map.of("code", code, "name", name))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể tạo học kỳ: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    semesterCodeField.clear();
                    semesterNameField.clear();
                    setStatus("Đã tạo học kỳ. Đang cập nhật danh sách...", false);
                    loadSemesters(onSemestersChanged);
                }));
    }

    private void confirmSetCurrent(AdminSemesterOption semester) {
        if (semester == null || semester.current() || loading) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Đặt học kỳ hiện tại");
        confirmation.setHeaderText("Đổi học kỳ hiện tại?");
        confirmation.setContentText("Học kỳ " + semester.name() + " (" + semester.code()
                + ") sẽ được dùng làm học kỳ hiện hành.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> setCurrentSemester(semester));
    }

    private void setCurrentSemester(AdminSemesterOption semester) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để đổi học kỳ hiện tại.");
            return;
        }
        setLoading(true);
        setStatus("Đang cập nhật học kỳ hiện tại...", false);
        requests.send(Command.ADMIN_SET_CURRENT_SEMESTER,
                Map.of("semesterCode", semester.code()))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể đặt học kỳ hiện tại: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    setStatus("Đã đổi học kỳ hiện tại. Đang cập nhật danh sách...", false);
                    loadSemesters();
                }));
    }

    private void loadSemesters() {
        loadSemesters(() -> { });
    }

    private void loadSemesters(Runnable onSuccess) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách học kỳ.");
            return;
        }
        if (loading) {
            return;
        }
        setLoading(true);
        setStatus("Đang tải danh sách học kỳ...", false);
        requests.send(Command.LIST_SEMESTERS, Map.of()).whenComplete((data, failure) ->
                Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể tải danh sách học kỳ: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        List<AdminSemesterOption> semesters = AdminSemesterOption.fromResponse(data);
                        semesterTable.getItems().setAll(semesters);
                        setStatus("Đã tải " + semesters.size() + " học kỳ.", false);
                        onSuccess.run();
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void setLoading(boolean value) {
        loading = value;
        createButton.setDisable(value || !requests.isAvailable());
        refreshButton.setDisable(value || !requests.isAvailable());
        semesterCodeField.setDisable(value);
        semesterNameField.setDisable(value);
        semesterTable.refresh();
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(error ? "#B4234D" : "#82747E"));
    }

    private void showError(String message) {
        setStatus(message, true);
    }
}
