package com.tkb.frontend.user;

import com.tkb.frontend.user.net.TcpNetworkClient;
import com.tkb.frontend.user.ui.ClientApplicationFrame;
import com.tkb.frontend.user.ui.ServerConfigPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        TcpNetworkClient networkClient = new TcpNetworkClient();
        BorderPane root = new BorderPane();
        AtomicBoolean connectedSession = new AtomicBoolean();
        ServerConfigPane serverConfigPane = new ServerConfigPane(networkClient,
                connectedClient -> {
                    connectedSession.set(true);
                    root.setCenter(new ClientApplicationFrame(connectedClient));
                });
        root.setCenter(serverConfigPane);
        networkClient.addDisconnectListener(failure ->
                Platform.runLater(() -> {
                    if (!connectedSession.compareAndSet(true, false)) {
                        return;
                    }
                    Throwable cause = failure;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    String reason = cause.getMessage();
                    String status = "Mất kết nối đến máy chủ. Hãy kết nối lại và đăng nhập lần nữa."
                            + (reason == null || reason.isBlank() ? "" : "\nChi tiết: " + reason);
                    serverConfigPane.showStatus(status);
                    root.setCenter(serverConfigPane);
                }));
        Scene scene = new Scene(root, 1360, 880);
        stage.setTitle("Project 15 – Quản lý thời khóa biểu");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setOnCloseRequest(event -> networkClient.close());
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
