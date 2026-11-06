package com.tkb.frontend.user;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        new Main().start(stage);
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}