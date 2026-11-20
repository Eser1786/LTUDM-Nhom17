package com.tkb.frontend.admin.ui;

import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Response;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AdminJobsPane extends VBox {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AdminRequestClient requests;
    private final TableView<AdminJobOption> jobTable = new TableView<>();
    private final List<AdminJobOption> allJobs = new ArrayList<>();

    private final TextField searchField = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final Button refreshButton = new Button("Làm mới");
    private final CheckBox autoRefreshBox = new CheckBox("Tự động làm mới (3s)");

    private final Label runningChip = new Label("Đang chạy: 0");
    private final Label queuedChip = new Label("Đang chờ: 0");
    private final Label totalChip = new Label("Tổng việc: 0");
    private final Label statusLabel = new Label();

    private Timeline autoRefreshTimeline;
    private boolean loading;

    AdminJobsPane(AdminRequestClient requests) {
        super(16);
        this.requests = Objects.requireNonNull(requests, "requests");
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);

        configureAutoRefresh();
        getChildren().setAll(createToolbarCard(), createTableCard());

        requests.addPushListener(response -> Platform.runLater(() -> handlePush(response)));
    }

    void onShown() {
        loadJobs(true);
        if (autoRefreshBox.isSelected() && autoRefreshTimeline != null) {
            autoRefreshTimeline.play();
        }
    }

    void onHidden() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private VBox createToolbarCard() {
        Label title = new Label("Giám sát tiến trình công việc");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));

        Label subtitle = new Label("Theo dõi và hủy các tiến trình tính toán hoặc nhập liệu đang xử lý trên máy chủ.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#82747E"));

        runningChip.setStyle(chipStyle("#DBEAFE", "#1D4ED8"));
        queuedChip.setStyle(chipStyle("#FEF3C7", "#B45309"));
        totalChip.setStyle(chipStyle("#F1F5F9", "#475569"));
        HBox chips = new HBox(8, runningChip, queuedChip, totalChip);
        chips.setAlignment(Pos.CENTER_RIGHT);

        BorderPane headerRow = new BorderPane();
        headerRow.setLeft(new VBox(4, title, subtitle));
        headerRow.setRight(chips);
        BorderPane.setAlignment(chips, Pos.CENTER_RIGHT);

        searchField.setPromptText("Tìm theo mã công việc hoặc người dùng...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        typeFilter.setItems(FXCollections.observableArrayList(
                "Tất cả loại việc",
                "Tối ưu TKB (OPTIMIZE)",
                "Nhập dữ liệu (IMPORT)"));
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        refreshButton.setStyle(primaryButtonStyle());
        refreshButton.setOnAction(event -> loadJobs(true));

        autoRefreshBox.setStyle("-fx-text-fill: #5A4755; -fx-font-size: 12px; -fx-cursor: hand;");
        autoRefreshBox.selectedProperty().addListener((obs, oldVal, selected) -> {
            if (selected) {
                if (autoRefreshTimeline != null) {
                    autoRefreshTimeline.playFromStart();
                }
            } else {
                if (autoRefreshTimeline != null) {
                    autoRefreshTimeline.stop();
                }
            }
        });

        HBox controls = new HBox(12, searchField, typeFilter, refreshButton, autoRefreshBox);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox card = new VBox(14, headerRow, controls);
        styleCard(card);
        return card;
    }

    private VBox createTableCard() {
        configureTable();
        jobTable.setPrefHeight(480);
        VBox.setVgrow(jobTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));

        VBox card = new VBox(14, jobTable, statusLabel);
        styleCard(card);
        return card;
    }

    private void configureTable() {
        TableColumn<AdminJobOption, String> idColumn = new TableColumn<>("Mã công việc");
        idColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().jobId()));
        idColumn.setPrefWidth(160);

        TableColumn<AdminJobOption, Void> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                setAlignment(Pos.CENTER_LEFT);
                setPadding(Insets.EMPTY);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    AdminJobOption job = getTableView().getItems().get(getIndex());
                    badge.setText(job.jobTypeDisplay());
                    badge.setStyle(job.jobTypeBadgeStyle());
                    setGraphic(badge);
                }
            }
        });
        typeColumn.setPrefWidth(130);

        TableColumn<AdminJobOption, String> userColumn = new TableColumn<>("Người tạo");
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username()));
        userColumn.setPrefWidth(120);

        TableColumn<AdminJobOption, Void> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                setAlignment(Pos.CENTER_LEFT);
                setPadding(Insets.EMPTY);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    AdminJobOption job = getTableView().getItems().get(getIndex());
                    badge.setText(job.statusDisplay());
                    badge.setStyle(job.statusBadgeStyle());
                    setGraphic(badge);
                }
            }
        });
        statusColumn.setPrefWidth(120);

        TableColumn<AdminJobOption, Void> progressColumn = new TableColumn<>("Tiến độ");
        progressColumn.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            private final Label pctLabel = new Label("0%");
            private final HBox box = new HBox(8, bar, pctLabel);
            {
                bar.setPrefWidth(90);
                bar.setMaxHeight(12);
                pctLabel.setFont(Font.font("Segoe UI", 11));
                pctLabel.setTextFill(Color.web("#5A4755"));
                box.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    AdminJobOption job = getTableView().getItems().get(getIndex());
                    bar.setProgress(job.percent() / 100.0);
                    pctLabel.setText(job.percent() + "%");
                    setGraphic(box);
                }
            }
        });
        progressColumn.setPrefWidth(160);

        TableColumn<AdminJobOption, String> startedColumn = new TableColumn<>("Bắt đầu lúc");
        startedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startedAtDisplay()));
        startedColumn.setPrefWidth(160);

        TableColumn<AdminJobOption, String> messageColumn = new TableColumn<>("Nội dung / Thông báo");
        messageColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().message()));
        messageColumn.setPrefWidth(240);

        TableColumn<AdminJobOption, Void> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellFactory(col -> new AdminJobActionsCell(
                this::confirmCancelJob,
                () -> loading || !requests.isAvailable()));
        actionColumn.setPrefWidth(100);

        // Can chinh noi dung cac cot
        idColumn.setStyle("-fx-alignment: CENTER;");
        userColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        startedColumn.setStyle("-fx-alignment: CENTER;");
        messageColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        jobTable.getColumns().setAll(
                idColumn,
                typeColumn,
                userColumn,
                statusColumn,
                progressColumn,
                startedColumn,
                messageColumn,
                actionColumn
        );
        jobTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        jobTable.setPlaceholder(new Label("Hiện không có tiến trình nào đang chạy hoặc chờ xử lý."));
        AdminTableStyle.apply(jobTable);
    }

    private void configureAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (autoRefreshBox.isSelected() && requests.isAvailable() && !loading) {
                loadJobs(false);
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void loadJobs(boolean showStatusNotice) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách công việc.");
            return;
        }
        if (loading) {
            return;
        }

        loading = true;
        updateControlsState();
        if (showStatusNotice) {
            setStatus("Đang tải danh sách công việc...", false);
        }

        requests.send(Command.ADMIN_LIST_JOBS, Map.of())
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    loading = false;
                    updateControlsState();
                    if (failure != null) {
                        showError("Không thể tải danh sách công việc: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        List<AdminJobOption> jobs = AdminJobProtocol.parseListResponse(data);
                        allJobs.clear();
                        allJobs.addAll(jobs);
                        updateChips();
                        applyFilters();
                        String timeStr = LocalTime.now().format(TIME_FORMAT);
                        setStatus("Đã cập nhật lúc " + timeStr + " • " + jobs.size() + " công việc.", false);
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void confirmCancelJob(AdminJobOption job) {
        if (loading || !requests.isAvailable() || job == null || !job.isActive()) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Hủy công việc");
        confirmation.setHeaderText("Xác nhận hủy công việc " + job.jobId() + "?");
        confirmation.setContentText("Người thực hiện: " + job.username()
                + "\nLoại công việc: " + job.jobTypeDisplay()
                + "\nTrạng thái: " + job.statusDisplay()
                + "\n\nTiến trình tính toán hoặc nhập liệu trên máy chủ sẽ dừng lại ngay lập tức.");

        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> cancelJob(job));
    }

    private void cancelJob(AdminJobOption job) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để hủy công việc.");
            return;
        }

        loading = true;
        updateControlsState();
        setStatus("Đang gửi yêu cầu hủy công việc " + job.jobId() + "...", false);

        requests.send(Command.ADMIN_CANCEL_JOB, Map.of("jobId", job.jobId()))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    loading = false;
                    updateControlsState();
                    if (failure != null) {
                        showError("Không thể hủy công việc: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    setStatus("Đã hủy thành công công việc " + job.jobId() + ".", false);
                    loadJobs(false);
                }));
    }

    private void handlePush(Response push) {
        if (push == null || !push.isPush() || push.getData() == null) {
            return;
        }
        String event = push.getPush();
        if (Command.JOB_PROGRESS.name().equals(event) || Command.JOB_DONE.name().equals(event)) {
            // Khi có gói push tiến trình trên hệ thống, cập nhật lại danh sách
            loadJobs(false);
        }
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedType = typeFilter.getValue();

        List<AdminJobOption> filtered = allJobs.stream()
                .filter(job -> {
                    if (!query.isEmpty()) {
                        boolean matchId = job.jobId().toLowerCase().contains(query);
                        boolean matchUser = job.username().toLowerCase().contains(query);
                        if (!matchId && !matchUser) {
                            return false;
                        }
                    }
                    if (selectedType != null && !selectedType.equals("Tất cả loại việc")) {
                        if (selectedType.contains("OPTIMIZE") && !"OPTIMIZE".equalsIgnoreCase(job.jobType())) {
                            return false;
                        }
                        if (selectedType.contains("IMPORT") && !"IMPORT".equalsIgnoreCase(job.jobType())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();

        jobTable.getItems().setAll(filtered);
    }

    private void updateChips() {
        long running = allJobs.stream().filter(j -> "RUNNING".equalsIgnoreCase(j.status())).count();
        long queued = allJobs.stream().filter(j -> "QUEUED".equalsIgnoreCase(j.status())).count();
        runningChip.setText("Đang chạy: " + running);
        queuedChip.setText("Đang chờ: " + queued);
        totalChip.setText("Tổng việc: " + allJobs.size());
    }

    private void updateControlsState() {
        refreshButton.setDisable(loading || !requests.isAvailable());
        jobTable.refresh();
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web(error ? "#B4234D" : "#82747E"));
    }

    private void showError(String message) {
        setStatus(message, true);
    }

    private void styleCard(VBox card) {
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: #8D456B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 8 16 8 16; -fx-cursor: hand;";
    }

    private static String chipStyle(String bg, String text) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + text + "; "
                + "-fx-background-radius: 14; -fx-padding: 5 12 5 12; -fx-font-size: 11px; -fx-font-weight: bold;";
    }
}
