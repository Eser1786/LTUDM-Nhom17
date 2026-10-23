package com.tkb.frontend.user;

import com.tkb.frontend.user.ui.MainFrame;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    // Äiá»ƒm vÃ o á»©ng dá»¥ng JavaFX: táº¡o cá»­a sá»• vÃ  gáº¯n MainFrame (shell cá»§a toÃ n bá»™ giao diá»‡n).
    @Override
    public void start(Stage stage) {
        MainFrame frame = new MainFrame();
        Scene scene = new Scene(frame, 1360, 880);
        stage.setTitle("Project 15 â€“ Tá»‘i Æ°u thá»i khÃ³a biá»ƒu sinh viÃªn");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
