package com.tkb.frontend.admin.ui;

import com.tkb.common.protocol.Command;
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

final class AdminSessionsPane extends VBox {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AdminRequestClient requests;
    private final TableView<AdminSessionOption> sessionTable = new TableView<>();
    private final List<AdminSessionOption> allSessions = new ArrayList<>();

    private final TextField searchField = new TextField();
    private final ComboBox<String> authFilter = new ComboBox<>();
    private final Button refreshButton = new Button("Làm mới");
    private final CheckBox autoRefreshBox = new CheckBox("Tự động làm mới (3s)");

    private final Label totalChip = new Label("Tổng phiên: 0");
    private final Label authChip = new Label("Đã đăng nhập: 0");
    private final Label guestChip = new Label("Chưa đăng nhập: 0");
    private final Label statusLabel = new Label();

    private Timeline autoRefreshTimeline;
    private boolean loading;

    AdminSessionsPane(AdminRequestClient requests) {
        super(16);
        this.requests = Objects.requireNonNull(requests, "requests");
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);

        configureAutoRefresh();
        getChildren().setAll(createToolbarCard(), createTableCard());
    }

    void onShown() {
        loadSessions(true);
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
        Label title = new Label("Quản lý phiên kết nối online");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));

        Label subtitle = new Label("Theo dõi các socket TCP đang kết nối đến hệ thống và ngắt phiên khi cần thiết.");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#82747E"));

        totalChip.setStyle(chipStyle("#F1F5F9", "#475569"));
        authChip.setStyle(chipStyle("#DBEAFE", "#1D4ED8"));
        guestChip.setStyle(chipStyle("#FEF3C7", "#B45309"));
        HBox chips = new HBox(8, totalChip, authChip, guestChip);
        chips.setAlignment(Pos.CENTER_RIGHT);

        BorderPane headerRow = new BorderPane();
        headerRow.setLeft(new VBox(4, title, subtitle));
        headerRow.setRight(chips);
        BorderPane.setAlignment(chips, Pos.CENTER_RIGHT);

        searchField.setPromptText("Tìm theo mã phiên, người dùng hoặc IP...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        authFilter.setItems(FXCollections.observableArrayList(
                "Tất cả phiên",
                "Đã đăng nhập",
                "Chưa đăng nhập (Khách)"));
        authFilter.getSelectionModel().selectFirst();
        authFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        refreshButton.setStyle(primaryButtonStyle());
        refreshButton.setOnAction(event -> loadSessions(true));

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

        HBox controls = new HBox(12, searchField, authFilter, refreshButton, autoRefreshBox);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox card = new VBox(14, headerRow, controls);
        styleCard(card);
        return card;
    }

    private VBox createTableCard() {
        configureTable();
        sessionTable.setPrefHeight(480);
        VBox.setVgrow(sessionTable, Priority.ALWAYS);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));

        VBox card = new VBox(14, sessionTable, statusLabel);
        styleCard(card);
        return card;
    }

    private void configureTable() {
        TableColumn<AdminSessionOption, String> idColumn = new TableColumn<>("Mã phiên (Session ID)");
        idColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().sessionId()));
        idColumn.setPrefWidth(190);

        TableColumn<AdminSessionOption, Void> userColumn = new TableColumn<>("Người dùng");
        userColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    AdminSessionOption session = getTableView().getItems().get(getIndex());
                    badge.setText(session.username());
                    badge.setStyle(session.userBadgeStyle());
                    setGraphic(badge);
                }
            }
        });
        userColumn.setPrefWidth(150);

        TableColumn<AdminSessionOption, String> addressColumn = new TableColumn<>("Địa chỉ kết nối (IP:Port)");
        addressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().cleanRemoteAddress()));
        addressColumn.setPrefWidth(210);

        TableColumn<AdminSessionOption, String> connectedColumn = new TableColumn<>("Thời điểm kết nối");
        connectedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().connectedAtDisplay()));
        connectedColumn.setPrefWidth(200);

        TableColumn<AdminSessionOption, Void> actionColumn = new TableColumn<>("Thao tác");
        actionColumn.setCellFactory(col -> new AdminSessionActionsCell(
                this::confirmKickSession,
                () -> loading || !requests.isAvailable()));
        actionColumn.setPrefWidth(130);

        sessionTable.getColumns().setAll(
                idColumn, userColumn, addressColumn, connectedColumn, actionColumn);
        sessionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        sessionTable.setPlaceholder(new Label("Hiện không có phiên kết nối nào khác."));
    }

    private void configureAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (autoRefreshBox.isSelected() && requests.isAvailable() && !loading) {
                loadSessions(false);
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void loadSessions(boolean showStatusNotice) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách phiên.");
            return;
        }
        if (loading) {
            return;
        }

        loading = true;
        updateControlsState();
        if (showStatusNotice) {
            setStatus("Đang tải danh sách phiên online...", false);
        }

        requests.send(Command.ADMIN_LIST_SESSIONS, Map.of())
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    loading = false;
                    updateControlsState();
                    if (failure != null) {
                        showError("Không thể tải danh sách phiên: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        List<AdminSessionOption> sessions = AdminSessionProtocol.parseListResponse(data);
                        allSessions.clear();
                        allSessions.addAll(sessions);
                        updateChips();
                        applyFilters();
                        String timeStr = LocalTime.now().format(TIME_FORMAT);
                        setStatus("Đã cập nhật lúc " + timeStr + " • " + sessions.size() + " phiên online.", false);
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void confirmKickSession(AdminSessionOption session) {
        if (loading || !requests.isAvailable() || session == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Ngắt kết nối phiên");
        confirmation.setHeaderText("Xác nhận ngắt phiên " + session.sessionId() + "?");
        confirmation.setContentText("Người dùng: " + session.username()
                + "\nĐịa chỉ: " + session.cleanRemoteAddress()
                + "\nThời điểm kết nối: " + session.connectedAtDisplay()
                + "\n\nKết nối TCP của máy khách sẽ bị đóng ngay lập tức.");

        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> kickSession(session));
    }

    private void kickSession(AdminSessionOption session) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để ngắt phiên.");
            return;
        }

        loading = true;
        updateControlsState();
        setStatus("Đang gửi yêu cầu ngắt phiên " + session.sessionId() + "...", false);

        requests.send(Command.ADMIN_KICK_SESSION, Map.of("sessionId", session.sessionId()))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    loading = false;
                    updateControlsState();
                    if (failure != null) {
                        showError("Không thể ngắt phiên: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    setStatus("Đã ngắt thành công phiên kết nối " + session.sessionId() + ".", false);
                    loadSessions(false);
                }));
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedFilter = authFilter.getValue();

        List<AdminSessionOption> filtered = allSessions.stream()
                .filter(s -> {
                    if (!query.isEmpty()) {
                        boolean matchId = s.sessionId().toLowerCase().contains(query);
                        boolean matchUser = s.username().toLowerCase().contains(query);
                        boolean matchIp = s.cleanRemoteAddress().toLowerCase().contains(query);
                        if (!matchId && !matchUser && !matchIp) {
                            return false;
                        }
                    }
                    if (selectedFilter != null && !selectedFilter.equals("Tất cả phiên")) {
                        if (selectedFilter.contains("Đã đăng nhập") && !s.isAuthenticated()) {
                            return false;
                        }
                        if (selectedFilter.contains("Chưa đăng nhập") && s.isAuthenticated()) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();

        sessionTable.getItems().setAll(filtered);
    }

    private void updateChips() {
        long authCount = allSessions.stream().filter(AdminSessionOption::isAuthenticated).count();
        long guestCount = allSessions.size() - authCount;
        totalChip.setText("Tổng phiên: " + allSessions.size());
        authChip.setText("Đã đăng nhập: " + authCount);
        guestChip.setText("Chưa đăng nhập: " + guestCount);
    }

    private void updateControlsState() {
        refreshButton.setDisable(loading || !requests.isAvailable());
        sessionTable.refresh();
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
