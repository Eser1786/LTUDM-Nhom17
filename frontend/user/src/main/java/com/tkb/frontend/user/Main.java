package com.tkb.frontend.user;

import com.tkb.frontend.user.net.MockProtocolNetworkClient;
import com.tkb.frontend.user.ui.ClientApplicationFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    // Server configuration in C1.4 will select and connect the TCP implementation.
    @Override
    public void start(Stage stage) {
        String mockRole = System.getProperty("tkb.mock.role", "USER");
        ClientApplicationFrame frame = new ClientApplicationFrame(
                new MockProtocolNetworkClient(mockRole));
        Scene scene = new Scene(frame, 1360, 880);
        stage.setTitle("Project 15 – Quản lý thời khóa biểu");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
