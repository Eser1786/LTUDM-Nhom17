package com.tkb.frontend.user.ui;

import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import com.tkb.frontend.user.net.NetworkClient;
import com.tkb.frontend.user.net.ProtocolTkbClient;
import com.tkb.frontend.user.net.TkbClient;
import com.tkb.frontend.admin.ui.AdminFrame;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientApplicationFrame extends BorderPane {
    private final NetworkClient networkClient;
    private final TkbClient userClient;
    private final LoginPanel loginPanel;
    private final AtomicInteger nextRequestId = new AtomicInteger(1);

    public ClientApplicationFrame(NetworkClient networkClient) {
        this(networkClient, new ProtocolTkbClient(networkClient));
    }

    public ClientApplicationFrame(NetworkClient networkClient, TkbClient userClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.userClient = Objects.requireNonNull(userClient, "userClient");
        this.loginPanel = new LoginPanel(this::login, this::register);
        setCenter(loginPanel);
    }

    private void login(String username, String password) {
        int requestId = nextRequestId.getAndIncrement();
        Request request = new Request(requestId, Command.LOGIN,
                Map.of("username", username, "password", password));
        try {
            networkClient.send(request).whenComplete((response, failure) ->
                    Platform.runLater(() -> handleLoginResponse(requestId, response, failure)));
        } catch (RuntimeException ex) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Không thể gửi yêu cầu đăng nhập: " + ex.getMessage(), true);
        }
    }

    private void register(String username, String password) {
        int requestId = nextRequestId.getAndIncrement();
        Request request = new Request(requestId, Command.REGISTER,
                Map.of("username", username, "password", password));
        try {
            networkClient.send(request).whenComplete((response, failure) ->
                    Platform.runLater(() -> handleRegisterResponse(requestId, response, failure)));
        } catch (RuntimeException ex) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Không thể gửi yêu cầu đăng ký: " + ex.getMessage(), true);
        }
    }

    private void handleRegisterResponse(int requestId, Response response, Throwable failure) {
        if (failure != null) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Không thể đăng ký: " + failureMessage(failure), true);
            return;
        }
        if (response == null || !Integer.valueOf(requestId).equals(response.getId())
                || response.isPush() || response.getOk() == null
                || (Boolean.TRUE.equals(response.getOk()) && response.getData() == null)) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Phản hồi đăng ký không hợp lệ.", true);
            return;
        }
        if (!response.getOk()) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus(errorMessage(response, "Đăng ký thất bại."), true);
            return;
        }
        loginPanel.registrationSucceeded();
    }

    public void resetToLogin() {
        // Return to the shared login screen after logout so the next login is routed
        // by role (USER/ADMIN) through RoleRouter instead of reusing the user-only flow.
        loginPanel.setRequestInProgress(false);
        loginPanel.showStatus("", false);
        setCenter(loginPanel);
    }

    private void handleLoginResponse(int requestId, Response response, Throwable failure) {
        if (failure != null) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Không thể đăng nhập: " + failureMessage(failure), true);
            return;
        }
        if (response == null) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Máy chủ không trả về phản hồi đăng nhập.", true);
            return;
        }
        if (!Integer.valueOf(requestId).equals(response.getId())
                || response.isPush() || response.getOk() == null) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Phản hồi đăng nhập không hợp lệ hoặc không khớp yêu cầu.", true);
            return;
        }
        if (!response.getOk()) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus(errorMessage(response, "Đăng nhập thất bại."), true);
            return;
        }

        Map<String, Object> data = response.getData();
        Object roleValue = data == null ? null : data.get("role");
        if (!(roleValue instanceof String role) || role.isBlank()) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus("Phản hồi đăng nhập thiếu role hợp lệ.", true);
            return;
        }

        try {
            if (userClient instanceof ProtocolTkbClient protocolClient) {
                protocolClient.setAuthenticatedUser(
                        data.get("username") instanceof String username ? username : "student",
                        role);
            }
            Parent roleFrame = RoleRouter.frameForRole(role, userClient, networkClient);
            setCenter(roleFrame);
            if (roleFrame instanceof AdminFrame) {
                setPrefSize(1360, 860);
            }
        } catch (IllegalArgumentException ex) {
            loginPanel.setRequestInProgress(false);
            loginPanel.showStatus(ex.getMessage(), true);
        }
    }

    private String errorMessage(Response response, String fallback) {
        Response.ErrorPayload error = response.getError();
        return error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? fallback
                : error.getMessage();
    }

    private String failureMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }
}
