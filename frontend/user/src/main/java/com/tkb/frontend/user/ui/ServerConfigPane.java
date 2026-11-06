package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.net.TcpNetworkClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Consumer;

public final class ServerConfigPane extends StackPane {
    private final TcpNetworkClient networkClient;
    private final Consumer<TcpNetworkClient> onConnected;
    private final TextField hostField = Functions.oNhap();
    private final TextField portField = Functions.oNhap();
    private final Button connectButton = Functions.nutChinh("Kết nối máy chủ");
    private final Label statusLabel = Functions.moTa("");

    public ServerConfigPane(TcpNetworkClient networkClient, Consumer<TcpNetworkClient> onConnected) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
        this.onConnected = Objects.requireNonNull(onConnected, "onConnected");

        setPadding(new Insets(36));
        setStyle("-fx-background-color: #F6F8FB;");

        Label title = Functions.chuDe("Kết nối máy chủ");
        Label description = Functions.moTa(
                "Nhập địa chỉ IP hoặc tên máy chủ và cổng TCP để kết nối an toàn với hệ thống.");
        hostField.setPromptText("Ví dụ: 192.168.1.10");
        hostField.setText(System.getProperty("tkb.backend.host", "localhost"));
        portField.setPromptText("9000");
        portField.setText(System.getProperty(
                "tkb.backend.port", String.valueOf(TcpNetworkClient.DEFAULT_PORT)));

        VBox hostGroup = new VBox(6, Functions.nhanNho("Địa chỉ IP / tên máy chủ"), hostField);
        VBox portGroup = new VBox(6, Functions.nhanNho("Cổng TCP"), portField);
        HBox.setHgrow(hostGroup, Priority.ALWAYS);
        portField.setPrefColumnCount(6);
        portField.setMaxWidth(110);
        HBox addressRow = new HBox(14, hostGroup, portGroup);
        addressRow.setAlignment(Pos.BOTTOM_LEFT);

        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(event -> connect());
        statusLabel.setWrapText(true);

        VBox card = Functions.the(28);
        card.setSpacing(16);
        card.setMaxWidth(560);
        card.getChildren().addAll(
                title,
                description,
                Functions.vach(),
                addressRow,
                connectButton,
                statusLabel);
        getChildren().add(card);
    }

    private void connect() {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isEmpty()) {
            showError("Vui lòng nhập địa chỉ IP hoặc tên máy chủ.");
            hostField.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText() == null ? "" : portField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Cổng phải là số nguyên từ 1 đến 65535.");
            portField.requestFocus();
            return;
        }
        if (port < 1 || port > 65_535) {
            showError("Cổng phải nằm trong khoảng 1 đến 65535.");
            portField.requestFocus();
            return;
        }

        setBusy(true);
        statusLabel.setTextFill(Functions.MA_CHU_PHU);
        statusLabel.setText("Đang kết nối và xác thực máy chủ...");
        networkClient.connect(host, port).whenComplete((ignored, failure) ->
                Platform.runLater(() -> {
                    if (failure != null) {
                        setBusy(false);
                        showError("Không thể kết nối máy chủ: " + rootCauseMessage(failure));
                        return;
                    }
                    setBusy(false);
                    statusLabel.setTextFill(Color.web("#15803D"));
                    statusLabel.setText("Đã kết nối an toàn tới " + host + ":" + port + ".");
                    onConnected.accept(networkClient);
                }));
    }

    public void showStatus(String message) {
        setBusy(false);
        statusLabel.setTextFill(Functions.MA_CANH_BAO);
        statusLabel.setText(message);
    }

    private void setBusy(boolean busy) {
        connectButton.setDisable(busy);
        hostField.setDisable(busy);
        portField.setDisable(busy);
    }

    private void showError(String message) {
        statusLabel.setTextFill(Functions.MA_LOI);
        statusLabel.setText(message);
    }

    private String rootCauseMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
