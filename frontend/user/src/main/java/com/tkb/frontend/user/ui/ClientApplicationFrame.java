package com.tkb.frontend.user.ui;

import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Request;
import com.tkb.common.protocol.Response;
import com.tkb.frontend.user.net.NetworkClient;
import com.tkb.frontend.user.ui.admin.AdminFrame;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientApplicationFrame extends BorderPane {
    private final NetworkClient networkClient;
    private final AtomicInteger nextRequestId = new AtomicInteger(1);

    public ClientApplicationFrame(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        setCenter(new LoginPanel(null, this::login));
    }

    private void login(String username, String password) {
        int requestId = nextRequestId.getAndIncrement();
        Request request = new Request(requestId, Command.LOGIN,
                Map.of("username", username, "password", password));
        try {
            networkClient.send(request).whenComplete((response, failure) ->
                    Platform.runLater(() -> handleLoginResponse(requestId, response, failure)));
        } catch (RuntimeException ex) {
            Functions.loi(window(), "Không thể gửi yêu cầu đăng nhập: " + ex.getMessage());
        }
    }

    private void handleLoginResponse(int requestId, Response response, Throwable failure) {
        if (failure != null) {
            Functions.loi(window(), "Không thể đăng nhập: " + failure.getMessage());
            return;
        }
        if (response == null) {
            Functions.loi(window(), "Máy chủ không trả về phản hồi đăng nhập.");
            return;
        }
        if (!Integer.valueOf(requestId).equals(response.getId())) {
            Functions.loi(window(), "Phản hồi đăng nhập không khớp với yêu cầu.");
            return;
        }
        if (!Boolean.TRUE.equals(response.getOk())) {
            Response.ErrorPayload error = response.getError();
            String message = error == null || error.getMessage() == null
                    ? "Đăng nhập thất bại."
                    : error.getMessage();
            Functions.loi(window(), message);
            return;
        }

        Map<String, Object> data = response.getData();
        Object roleValue = data == null ? null : data.get("role");
        if (!(roleValue instanceof String role) || role.isBlank()) {
            Functions.loi(window(), "Phản hồi đăng nhập thiếu role hợp lệ.");
            return;
        }

        try {
            Parent roleFrame = RoleRouter.frameForRole(role);
            setCenter(roleFrame);
            if (roleFrame instanceof AdminFrame) {
                setPrefSize(1360, 860);
            }
        } catch (IllegalArgumentException ex) {
            Functions.loi(window(), ex.getMessage());
        }
    }

    private Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
