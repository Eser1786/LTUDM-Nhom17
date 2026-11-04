package com.tkb.frontend.user.ui.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminFrame extends BorderPane {
    private static final Map<String, String> PAGES = createPages();

    private final Map<String, Button> navigation = new LinkedHashMap<>();
    private final Label pageTitle = new Label();
    private final Label pageDescription = new Label();

    private static Map<String, String> createPages() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("overview", "Tổng quan");
        pages.put("import", "Nhập dữ liệu");
        pages.put("semesters", "Học kỳ");
        pages.put("courses", "Học phần");
        pages.put("sections", "Nhóm lớp");
        pages.put("users", "Người dùng");
        pages.put("jobs", "Giám sát công việc");
        pages.put("sessions", "Phiên online");
        return Collections.unmodifiableMap(pages);
    }

    public AdminFrame() {
        setPrefSize(1360, 860);
        setStyle("-fx-background-color: #F6F8FB;");

        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(244);
        sidebar.setPadding(new Insets(20, 12, 16, 12));
        sidebar.setStyle("-fx-background-color: #0F172A;");

        Label brand = new Label("Quản trị hệ thống");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        brand.setPadding(new Insets(0, 8, 18, 8));
        sidebar.getChildren().add(brand);

        PAGES.forEach((key, title) -> {
            Button button = new Button(title);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; "
                    + "-fx-padding: 10 8 10 8; -fx-alignment: center-left;");
            button.setOnAction(event -> showPlaceholder(key));
            navigation.put(key, button);
            sidebar.getChildren().add(button);
        });

        Label account = new Label("ADMIN");
        account.setTextFill(Color.web("#93C5FD"));
        account.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        VBox.setVgrow(account, Priority.ALWAYS);
        sidebar.getChildren().add(account);

        pageTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        pageTitle.setTextFill(Color.web("#0F172A"));
        pageDescription.setFont(Font.font("Segoe UI", 14));
        pageDescription.setTextFill(Color.web("#64748B"));

        VBox content = new VBox(12, pageTitle, pageDescription);
        content.setPadding(new Insets(36));

        setLeft(sidebar);
        setCenter(content);
        showPlaceholder("overview");
    }

    private void showPlaceholder(String pageKey) {
        String title = PAGES.get(pageKey);
        if (title == null) {
            throw new IllegalArgumentException("Unknown admin page: " + pageKey);
        }
        pageTitle.setText(title);
        pageDescription.setText("Khung trang đã sẵn sàng. Chức năng sẽ được bổ sung ở các hạng mục Admin.");
        navigation.forEach((key, button) -> button.setStyle(key.equals(pageKey)
                ? "-fx-background-color: #1D4ED8; -fx-text-fill: white; "
                    + "-fx-padding: 10 8 10 8; -fx-alignment: center-left;"
                : "-fx-background-color: transparent; -fx-text-fill: #CBD5E1; "
                    + "-fx-padding: 10 8 10 8; -fx-alignment: center-left;"));
    }
}
