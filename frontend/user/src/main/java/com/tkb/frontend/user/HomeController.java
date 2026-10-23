package com.tkb.frontend.user;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class HomeController {

    @FXML
    private void handleViewSchedule() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Thời khóa biểu");
        alert.setHeaderText("Chức năng thời khóa biểu");
        alert.setContentText("Bạn đã nhấn nút xem thời khóa biểu.");

        alert.showAndWait();
    }
}