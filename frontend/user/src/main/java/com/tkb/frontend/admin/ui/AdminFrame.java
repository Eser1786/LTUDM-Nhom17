package com.tkb.frontend.admin.ui;

import com.tkb.frontend.user.net.NetworkClient;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminFrame extends BorderPane {
    private static final Map<String, String> PAGES = createPages();

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
        AdminRequestClient requests = new AdminRequestClient(networkClient);
        dashboardPane = new AdminDashboardPane(requests);
        importPane = new AdminImportPane(requests);
        semestersPane = new AdminSemestersPane(requests, importPane::refreshSemesters);
        pageContents.put("overview", dashboardPane);
        pageContents.put("import", importPane);
        pageContents.put("semesters", semestersPane);
        for (String page : PAGES.keySet()) {
            pageContents.putIfAbsent(page, placeholderMessage);
        }

        setPrefSize(1360, 860);
        setStyle("-fx-background-color: #FBF8FA;");
        workspaceContent.setPadding(new Insets(36, 42, 42, 42));
        workspaceContent.setFillWidth(true);
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

        Label brand = new Label("TKB  /  QUẢN TRỊ");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        brand.setPadding(new Insets(0, 10, 22, 10));
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
        return sidebar;
    }

    private VBox createTopBar() {
        Label breadcrumb = new Label("HỆ THỐNG  /  QUẢN TRỊ");
        breadcrumb.setTextFill(Color.web("#927F8B"));
        breadcrumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        Label user = new Label("Quản trị viên");
        user.setTextFill(Color.web("#503A4A"));
        user.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        BorderPane bar = new BorderPane();
        bar.setLeft(breadcrumb);
        bar.setRight(user);
        BorderPane.setAlignment(breadcrumb, Pos.CENTER_LEFT);
        BorderPane.setAlignment(user, Pos.CENTER_RIGHT);
        VBox top = new VBox(bar);
        top.setPadding(new Insets(20, 38, 20, 38));
        top.setStyle("-fx-background-color: white; "
                + "-fx-border-color: transparent transparent #F0E7EC transparent;");
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
            default -> {
            }
        }
    }

    private String descriptionFor(String pageKey) {
        return switch (pageKey) {
            case "overview" -> "Theo dõi nhanh tình trạng hoạt động của hệ thống.";
            case "import" -> "Tải PDF, kiểm tra dữ liệu trước khi ghi và xem rõ lỗi theo STT.";
            case "semesters" -> "Tạo học kỳ và chọn học kỳ hiện hành cho hệ thống.";
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
