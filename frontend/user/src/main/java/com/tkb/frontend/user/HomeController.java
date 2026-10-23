package com.tkb.frontend.user;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class HomeController {

    @FXML
    private void handleViewSchedule() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Thá»i khÃ³a biá»ƒu");
        alert.setHeaderText("Chá»©c nÄƒng thá»i khÃ³a biá»ƒu");
        alert.setContentText("Báº¡n Ä‘Ã£ nháº¥n nÃºt xem thá»i khÃ³a biá»ƒu.");

        alert.showAndWait();
    }
}