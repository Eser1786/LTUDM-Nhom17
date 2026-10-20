package com.tkb.frontend;

import com.tkb.frontend.ui.MainFrame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Project 15 – Tối ưu thời khóa biểu sinh viên");
        stage.setScene(new Scene(new StackPane(), 1, 1));
        stage.show();

        Platform.runLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            stage.hide();
        });
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
