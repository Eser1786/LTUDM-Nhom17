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

import java.util.Map;
import java.util.Optional;

final class AdminUsersPane extends VBox {
    private static final int PAGE_SIZE = 20;

    private final AdminRequestClient requests;
    private final TableView<AdminUserOption> userTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Tìm kiếm");
    private final Button refreshButton = new Button("Tải lại");
    private final Button previousPageButton = new Button("Trang trước");
    private final Button nextPageButton = new Button("Trang sau");
    private final Label pageLabel = new Label();
    private final Label statusLabel = new Label();

    private int page;
    private int totalUsers;
    private boolean loading;

    AdminUsersPane(AdminRequestClient requests) {
        super(16);
        this.requests = requests;
        setPadding(new Insets(8, 0, 16, 0));
        setFillWidth(true);
        getChildren().setAll(createListCard());
    }

    void onShown() {
        loadUsers();
    }

    private VBox createListCard() {
        Label title = new Label("Danh sách người dùng");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#493343"));

        searchField.setPromptText("Tìm theo tên đăng nhập");
        searchField.setOnAction(event -> searchUsers());
        searchButton.setOnAction(event -> searchUsers());
        searchButton.setStyle(primaryButtonStyle());
        refreshButton.setOnAction(event -> loadUsers());
        refreshButton.setStyle(secondaryButtonStyle());

        HBox searchControls = new HBox(10, searchField, searchButton, refreshButton);
        searchControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        BorderPane toolbar = new BorderPane();
        toolbar.setLeft(title);
        toolbar.setRight(searchControls);

        configureTable();
        userTable.setPrefHeight(500);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        previousPageButton.setOnAction(event -> changePage(-1));
        nextPageButton.setOnAction(event -> changePage(1));
        previousPageButton.setStyle(secondaryButtonStyle());
        nextPageButton.setStyle(secondaryButtonStyle());
        HBox pagination = new HBox(10, previousPageButton, pageLabel, nextPageButton);
        pagination.setAlignment(Pos.CENTER_RIGHT);

        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));
        VBox card = new VBox(14, toolbar, userTable, pagination, statusLabel);
        styleCard(card);
        return card;
    }

    private void configureTable() {
        TableColumn<AdminUserOption, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Long.toString(cell.getValue().userId())));
        idColumn.setPrefWidth(70);

        TableColumn<AdminUserOption, String> usernameColumn = new TableColumn<>("Tên đăng nhập");
        usernameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().username()));
        usernameColumn.setPrefWidth(180);

        TableColumn<AdminUserOption, String> roleColumn = new TableColumn<>("Vai trò");
        roleColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().role()));
        roleColumn.setPrefWidth(100);

        TableColumn<AdminUserOption, String> onlineColumn = new TableColumn<>("Trạng thái");
        onlineColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().online() ? "Đang online" : "Offline"));
        onlineColumn.setPrefWidth(110);

        TableColumn<AdminUserOption, String> createdColumn = new TableColumn<>("Ngày tạo");
        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().createdAtDisplay()));
        createdColumn.setPrefWidth(180);

        TableColumn<AdminUserOption, Void> actionsColumn = new TableColumn<>("Thao tác");
        actionsColumn.setCellFactory(ignored -> new AdminUserActionsCell(
                this::confirmRoleChange,
                this::resetPassword,
                this::confirmDelete,
                () -> loading || !requests.isAvailable()));
        actionsColumn.setPrefWidth(440);

        userTable.getColumns().setAll(
                idColumn, usernameColumn, roleColumn, onlineColumn, createdColumn, actionsColumn);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        userTable.setPlaceholder(new Label("Không có người dùng phù hợp."));
    }

    private void searchUsers() {
        page = 0;
        loadUsers();
    }

    private void changePage(int offset) {
        int nextPage = page + offset;
        int pageCount = pageCount();
        if (nextPage < 0 || nextPage >= pageCount || loading) {
            return;
        }
        page = nextPage;
        loadUsers();
    }

    private void loadUsers() {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách người dùng.");
            return;
        }
        if (loading) {
            return;
        }

        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.length() > 50) {
            showError("Từ khóa tìm kiếm không được vượt quá 50 ký tự.");
            searchField.requestFocus();
            return;
        }
        setLoading(true);
        setStatus("Đang tải danh sách người dùng...", false);
        requests.send(Command.ADMIN_LIST_USERS, Map.of(
                        "keyword", keyword,
                        "page", page,
                        "size", PAGE_SIZE))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể tải danh sách người dùng: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    try {
                        AdminUserPage result = AdminUserProtocol.parseListResponse(data);
                        int lastPage = result.total() == 0
                                ? 0 : (result.total() - 1) / PAGE_SIZE;
                        if (page > lastPage) {
                            page = lastPage;
                            loadUsers();
                            return;
                        }
                        totalUsers = result.total();
                        userTable.getItems().setAll(result.users());
                        updatePagination();
                        setStatus("Đã tải " + result.users().size()
                                + " người dùng trong trang hiện tại.", false);
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }));
    }

    private void confirmRoleChange(AdminUserOption user) {
        if (loading) {
            return;
        }
        String newRole = user.oppositeRole();
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Đổi vai trò");
        confirmation.setHeaderText("Đổi vai trò của " + user.username() + "?");
        confirmation.setContentText("Vai trò hiện tại: " + user.role()
                + ". Vai trò mới: " + newRole + ".");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> changeRole(user, newRole));
    }

    private void changeRole(AdminUserOption user, String role) {
        runUserAction(
                "Đang đổi vai trò của " + user.username() + "...",
                "Đã đổi vai trò của " + user.username() + ".",
                Command.ADMIN_SET_USER_ROLE,
                Map.of("userId", user.userId(), "role", role),
                true);
    }

    private void resetPassword(AdminUserOption user) {
        if (loading) {
            return;
        }
        Optional<String> password = AdminUserPasswordDialog.show(
                getScene() == null ? null : getScene().getWindow(), user);
        password.ifPresent(value -> runUserAction(
                "Đang đặt lại mật khẩu cho " + user.username() + "...",
                "Đã đặt lại mật khẩu cho " + user.username() + ".",
                Command.ADMIN_RESET_PASSWORD,
                Map.of("userId", user.userId(), "newPassword", value),
                false));
    }

    private void confirmDelete(AdminUserOption user) {
        if (loading) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xóa người dùng");
        confirmation.setHeaderText("Xóa tài khoản " + user.username() + "?");
        confirmation.setContentText(
                "Không thể hoàn tác. Các phương án đã lưu của tài khoản này cũng sẽ bị xóa.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> runUserAction(
                        "Đang xóa tài khoản " + user.username() + "...",
                        "Đã xóa tài khoản " + user.username() + ".",
                        Command.ADMIN_DELETE_USER,
                        Map.of("userId", user.userId()),
                        true));
    }

    private void runUserAction(
            String pendingMessage,
            String successMessage,
            Command command,
            Map<String, Object> requestData,
            boolean refreshAfterSuccess) {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để thực hiện thao tác.");
            return;
        }
        if (loading) {
            return;
        }
        setLoading(true);
        setStatus(pendingMessage, false);
        requests.send(command, requestData).whenComplete((data, failure) ->
                Platform.runLater(() -> {
                    setLoading(false);
                    if (failure != null) {
                        showError("Không thể thực hiện thao tác: "
                                + AdminRequestClient.messageOf(failure));
                        return;
                    }
                    if (refreshAfterSuccess) {
                        setStatus(successMessage, false);
                        loadUsers();
                    } else {
                        setStatus(successMessage, false);
                    }
                }));
    }

    private int pageCount() {
        return totalUsers == 0 ? 0 : (totalUsers + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    private void updatePagination() {
        int pageCount = pageCount();
        pageLabel.setText(pageCount == 0
                ? "0 người dùng"
                : "Trang " + (page + 1) + "/" + pageCount + " • " + totalUsers + " người dùng");
        previousPageButton.setDisable(loading || page == 0);
        nextPageButton.setDisable(loading || page + 1 >= pageCount);
    }

    private void setLoading(boolean value) {
        loading = value;
        searchField.setDisable(value);
        searchButton.setDisable(value || !requests.isAvailable());
        refreshButton.setDisable(value || !requests.isAvailable());
        previousPageButton.setDisable(value || page == 0);
        nextPageButton.setDisable(value || page + 1 >= pageCount());
        userTable.refresh();
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
}
