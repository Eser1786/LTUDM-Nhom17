package com.tkb.frontend.user.ui.admin;

import com.tkb.common.protocol.Command;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

final class AdminDashboardPane extends VBox {
    private final AdminRequestClient requests;
    private final Label statusLabel = new Label();
    private final Label onlineUsersValue = metricValue();
    private final Label activeJobsValue = metricValue();
    private final Label totalUsersValue = metricValue();
    private final Label totalSectionsValue = metricValue();
    private final Button refreshButton = new Button("Làm mới");

    AdminDashboardPane(AdminRequestClient requests) {
        super(20);
        this.requests = requests;
        setPadding(new Insets(36, 42, 42, 42));
        setFillWidth(true);

        Label heading = new Label("Tình hình hệ thống");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        heading.setTextFill(Color.web("#493343"));

        refreshButton.setOnAction(event -> loadStats());
        refreshButton.setStyle("-fx-background-color: #8D456B; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 9 18 9 18; -fx-cursor: hand;");
        Label note = new Label("Các chỉ số hiện tại từ máy chủ");
        note.setTextFill(Color.web("#82747E"));
        note.setFont(Font.font("Segoe UI", 12));
        BorderPane toolbar = new BorderPane();
        toolbar.setLeft(note);
        toolbar.setRight(refreshButton);
        BorderPane.setAlignment(note, Pos.CENTER_LEFT);

        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setWrapText(true);
        statusLabel.setTextFill(Color.web("#82747E"));

        getChildren().setAll(new VBox(toolbar), heading, createMetricsGrid(), statusLabel);
    }

    void onShown() {
        loadStats();
    }

    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);
        for (int column = 0; column < 2; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(50);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }
        grid.add(metricCard("Người dùng online", onlineUsersValue, "Đang kết nối", "#A74F78"), 0, 0);
        grid.add(metricCard("Công việc đang chạy", activeJobsValue, "Đang được xử lý", "#76518A"), 1, 0);
        grid.add(metricCard("Tổng người dùng", totalUsersValue, "Tài khoản trong hệ thống", "#BF7188"), 0, 1);
        grid.add(metricCard("Nhóm lớp học phần", totalSectionsValue, "Nhóm đang hoạt động", "#96705E"), 1, 1);
        return grid;
    }

    private VBox metricCard(String title, Label value, String detail, String accent) {
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#756572"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        Label detailLabel = new Label(detail);
        detailLabel.setTextFill(Color.web("#9B8C98"));
        detailLabel.setFont(Font.font("Segoe UI", 11));
        VBox card = new VBox(12, titleLabel, value, detailLabel);
        card.setMinHeight(150);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14; "
                + "-fx-border-width: 1 1 1 4; -fx-border-insets: 0 0 0 0;");
        value.setTextFill(Color.web(accent));
        GridPane.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Label metricValue() {
        Label value = new Label("—");
        value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
        return value;
    }

    private void loadStats() {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải thống kê.");
            return;
        }
        refreshButton.setDisable(true);
        statusLabel.setTextFill(Color.web("#82747E"));
        statusLabel.setText("Đang tải thống kê...");
        requests.send(Command.ADMIN_GET_STATS, Map.of()).whenComplete((data, failure) ->
                Platform.runLater(() -> {
                    refreshButton.setDisable(false);
                    if (failure != null) {
                        showError("Không thể tải thống kê: " + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        onlineUsersValue.setText(Integer.toString(readCount(data, "onlineUsers")));
                        activeJobsValue.setText(Integer.toString(readCount(data, "activeJobs")));
                        totalUsersValue.setText(Integer.toString(readCount(data, "totalUsers")));
                        totalSectionsValue.setText(Integer.toString(readCount(data, "totalSections")));
                        statusLabel.setTextFill(Color.web("#82747E"));
                        statusLabel.setText("Đã cập nhật thống kê từ máy chủ.");
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private int readCount(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (!(value instanceof Number number)
                || number.longValue() < 0
                || number.longValue() > Integer.MAX_VALUE
                || number.doubleValue() != number.longValue()) {
            throw new IllegalArgumentException("Dữ liệu thống kê '" + field + "' không hợp lệ.");
        }
        return number.intValue();
    }

    private void showError(String message) {
        statusLabel.setTextFill(Color.web("#B4234D"));
        statusLabel.setText(message);
    }
}
