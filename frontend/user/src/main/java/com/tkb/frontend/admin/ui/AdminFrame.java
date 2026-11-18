package com.tkb.frontend.admin.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tkb.frontend.user.net.NetworkClient;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.frontend.user.ui.ClientApplicationFrame;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AdminFrame extends BorderPane {
    private static final Map<String, String> PAGES = createPages();
    private static final AtomicInteger LOGOUT_REQUEST_IDS =new AtomicInteger(1000000);

    private final NetworkClient networkClient;
    private final Map<String, Button> navigation = new LinkedHashMap<>();
    private final Map<String, Node> pageContents = new LinkedHashMap<>();
    private final VBox workspaceContent = new VBox(20);
    private final Label pageTitle = new Label();
    private final Label pageDescription = new Label();
    private final Label placeholderMessage = new Label(
            "Khung trang đã sẵn sàng. Chức năng sẽ được bổ sung ở các hạng mục Admin.");

    private final AdminDashboardPane dashboardPane;
    private final AdminImportPane importPane;
    private final AdminSemestersPane semestersPane;
    private final AdminCoursesPane coursesPane;
    private final AdminSectionsPane sectionsPane;
    private final AdminUsersPane usersPane;
    private final AdminJobsPane jobsPane;
    private final AdminSessionsPane sessionsPane;

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
        this(null);
    }

    public AdminFrame(NetworkClient networkClient) {
        this.networkClient = networkClient;

        AdminRequestClient requests = new AdminRequestClient(networkClient);
        dashboardPane = new AdminDashboardPane(requests);
        importPane = new AdminImportPane(requests);
        semestersPane = new AdminSemestersPane(requests, importPane::refreshSemesters);
        coursesPane = new AdminCoursesPane(requests);
        sectionsPane = new AdminSectionsPane(requests);
        usersPane = new AdminUsersPane(requests);
        jobsPane = new AdminJobsPane(requests);
        sessionsPane = new AdminSessionsPane(requests);
        pageContents.put("overview", dashboardPane);
        pageContents.put("import", importPane);
        pageContents.put("semesters", semestersPane);
        pageContents.put("courses", coursesPane);
        pageContents.put("sections", sectionsPane);
        pageContents.put("users", usersPane);
        pageContents.put("jobs", jobsPane);
        pageContents.put("sessions", sessionsPane);
        for (String page : PAGES.keySet()) {
            pageContents.putIfAbsent(page, placeholderMessage);
        }

        setPrefSize(1360, 860);
        setStyle("-fx-background-color: #FBF8FA;");
        workspaceContent.setPadding(new Insets(28, 28, 28, 28));
        workspaceContent.setFillWidth(true);

        pageTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 27));
        pageTitle.setTextFill(Color.BLACK);

        pageDescription.setFont(Font.font("Segoe UI", 14));
        pageDescription.setTextFill(Color.web("#888888"));
        
        placeholderMessage.setWrapText(true);
        placeholderMessage.setFont(Font.font("Segoe UI", 14));
        placeholderMessage.setTextFill(Color.web("#82747E"));

        BorderPane workspace = new BorderPane();
        workspace.setStyle("-fx-background-color: #FBF8FA;");
        workspace.setTop(createTopBar());
        workspace.setCenter(workspaceContent);
        setLeft(createSidebar());
        setCenter(workspace);
        showPage("overview");
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(244);
        sidebar.setPadding(new Insets(24, 14, 20, 14));
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #49243F, #B9688A);");

        Label brand = new Label("THỜI KHÓA BIỂU");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 21));
        brand.setPadding(new Insets(0, 10, 36, 10));
        sidebar.getChildren().add(brand);

        PAGES.forEach((key, title) -> {
            Button button = new Button(title);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setStyle(navigationStyle(false));
            button.setOnAction(event -> showPage(key));
            navigation.put(key, button);
            sidebar.getChildren().add(button);
        });

        Label account = new Label("ADMIN");
        account.setTextFill(Color.web("#FFE1EC"));
        account.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        VBox.setVgrow(account, Priority.ALWAYS);
        sidebar.getChildren().add(account);

        Button logoutButton = new Button("Đăng xuất");

        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setPrefHeight(42);
        logoutButton.setAlignment(Pos.CENTER);

        logoutButton.setStyle(
                "-fx-background-color: #A52F55;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 12px;" +
                "-fx-cursor: hand;"
        );

        logoutButton.setOnAction(event -> dangXuat());

        sidebar.getChildren().add(logoutButton);
        return sidebar;
    }

    private void dangXuat() {
    if (networkClient != null) {
        try {
            int requestId = LOGOUT_REQUEST_IDS.getAndIncrement();

            networkClient.send(
                    new Request(requestId, Command.LOGOUT, Map.of())
            ).whenComplete((response, error) ->
                    Platform.runLater(this::quayVeDangNhap)
            );

            return;
        } catch (RuntimeException ignored) {
        }
    }

    quayVeDangNhap();
}

private void quayVeDangNhap() {
    if (getParent() instanceof ClientApplicationFrame parentFrame) {
        parentFrame.resetToLogin();
    }
}

    private VBox createTopBar() {
        Label title = new Label("ADMIN");
        title.setTextFill(Color.web("#B9688A"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));

        Label subtitle = new Label("Hệ thống quản trị");
        subtitle.setTextFill(Color.web("#888888"));
        subtitle.setFont(Font.font("Segoe UI", 14));

        Label user = new Label("Quản trị viên");
        user.setTextFill(Color.BLACK);
        user.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        user.setAlignment(Pos.CENTER);
        user.setPadding(new Insets(16, 25, 16, 25));
        user.setStyle(
            "-fx-background-color: #EACFE0;" +
            "-fx-background-radius: 12;"
        );

        javafx.scene.layout.HBox left = new javafx.scene.layout.HBox(12, title, subtitle);
        left.setAlignment(Pos.CENTER_LEFT);

        BorderPane bar = new BorderPane();
        bar.setLeft(left);
        bar.setRight(user);

        VBox top = new VBox(bar);
        top.setPadding(new Insets(10, 16, 10, 16));
        top.setStyle("-fx-background-color: #FBF8FA;");

        return top;
    }

    private void showPage(String pageKey) {
        String title = PAGES.get(pageKey);
        Node page = pageContents.get(pageKey);
        if (title == null || page == null) {
            throw new IllegalArgumentException("Unknown admin page: " + pageKey);
        }
        pageTitle.setText(title);
        pageDescription.setText(descriptionFor(pageKey));
        workspaceContent.getChildren().setAll(pageTitle, pageDescription, page);
        navigation.forEach((key, button) ->
                button.setStyle(navigationStyle(key.equals(pageKey))));

        switch (pageKey) {
            case "overview" -> dashboardPane.onShown();
            case "import" -> importPane.onShown();
            case "semesters" -> semestersPane.onShown();
            case "courses" -> coursesPane.onShown();
            case "sections" -> sectionsPane.onShown();
            case "users" -> usersPane.onShown();
            case "jobs" -> jobsPane.onShown();
            case "sessions" -> sessionsPane.onShown();
            default -> {
            }
        }
    }

    private String descriptionFor(String pageKey) {
        return switch (pageKey) {
            case "overview" -> "Theo dõi nhanh tình trạng hoạt động của hệ thống.";
            case "import" -> "Tải PDF, kiểm tra dữ liệu trước khi ghi và xem rõ lỗi theo STT.";
            case "semesters" -> "Tạo học kỳ và chọn học kỳ hiện hành cho hệ thống.";
            case "courses" -> "Tìm kiếm, thêm, chỉnh sửa và xóa học phần trong danh mục.";
            case "sections" -> "Quản lý nhóm lớp, lịch học và trạng thái mở/tắt theo học kỳ.";
            case "users" -> "Tìm kiếm, phân trang và quản lý tài khoản, vai trò, mật khẩu.";
            case "jobs" -> "Theo dõi tiến trình các công việc tính toán và nhập liệu đang xử lý.";
            case "sessions" -> "Theo dõi các kết nối TCP và ngắt kết nối phiên khi cần thiết.";
            default -> "Khung trang đã sẵn sàng. Chức năng sẽ được bổ sung ở các hạng mục Admin.";
        };
    }

    private String navigationStyle(boolean selected) {
        return selected
                ? "-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; "
                    + "-fx-font-size: 13px; -fx-padding: 11 12 11 12; -fx-alignment: center-left;"
                : "-fx-background-color: transparent; -fx-text-fill: #F7EAF0; "
                    + "-fx-font-size: 13px; -fx-padding: 11 12 11 12; -fx-alignment: center-left;";
    }
}
