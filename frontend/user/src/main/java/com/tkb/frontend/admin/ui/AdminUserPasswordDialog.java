package com.tkb.frontend.admin.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

final class AdminUserPasswordDialog {
    private AdminUserPasswordDialog() {
    }

    static Optional<String> show(Window owner, AdminUserOption user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Đặt lại mật khẩu");
        dialog.setHeaderText("Đặt mật khẩu mới cho " + user.username());
        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType saveButtonType = new ButtonType("Đặt lại", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(saveButtonType, ButtonType.CANCEL);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu mới (8–64 ký tự)");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Nhập lại mật khẩu mới");
        Label validationMessage = new Label();
        validationMessage.setStyle("-fx-text-fill: #B4234D;");

        VBox content = new VBox(10,
                new Label("Mật khẩu"),
                passwordField,
                new Label("Xác nhận mật khẩu"),
                confirmField,
                validationMessage);
        content.setPadding(new Insets(8, 0, 0, 0));
        passwordField.setMaxWidth(Double.MAX_VALUE);
        confirmField.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> button == saveButtonType ? passwordField.getText() : null);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String password = passwordField.getText();
            if (password.length() < 8 || password.length() > 64) {
                validationMessage.setText("Mật khẩu phải có độ dài từ 8 đến 64 ký tự.");
                event.consume();
            } else if (!password.equals(confirmField.getText())) {
                validationMessage.setText("Hai mật khẩu không khớp.");
                event.consume();
            }
        });
        return dialog.showAndWait();
    }
}
