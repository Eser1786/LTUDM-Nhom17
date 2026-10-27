package com.tkb.frontend.user;

import com.tkb.frontend.user.ui.MainFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    // Điểm vào ứng dụng JavaFX: tạo cửa sổ và gắn MainFrame (shell của toàn bộ giao diện).
    @Override
    public void start(Stage stage) {
        MainFrame frame = new MainFrame();
        Scene scene = new Scene(frame, 1360, 880);
        stage.setTitle("Project 15 – Tối ưu thời khóa biểu sinh viên");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
