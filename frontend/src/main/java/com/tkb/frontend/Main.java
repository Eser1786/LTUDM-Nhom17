package com.tkb.frontend;

import com.tkb.frontend.ui.MainFrame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    // This is the JavaFX application entry point.
    // The app creates a window and then swaps in the real timetable UI once the JavaFX runtime is ready.
    @Override
    public void start(Stage stage) {
        // Create a tiny hidden root first so the window can initialize cleanly before loading the full UI.
        stage.setTitle("Project 15 – Tối ưu thời khóa biểu sinh viên");
        stage.setScene(new Scene(new StackPane(), 1, 1));
        stage.show();

        // Defer the real UI construction to the JavaFX event queue so the app starts reliably.
        Platform.runLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            stage.hide();
        });
    }

    public static void main(String[] args) {
        // JavaFX launches the app through Application.launch(), which calls start(Stage).
        Application.launch(Main.class, args);
    }
}
