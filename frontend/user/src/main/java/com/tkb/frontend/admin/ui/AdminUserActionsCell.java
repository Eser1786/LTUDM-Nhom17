package com.tkb.frontend.admin.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class AdminUserActionsCell extends TableCell<AdminUserOption, Void> {
    private final Button roleButton = new Button();
    private final Button resetPasswordButton = new Button("Đặt lại mật khẩu");
    private final Button deleteButton = new Button("Xóa");
    private final HBox actions = new HBox(6, roleButton, resetPasswordButton, deleteButton);
    private final Consumer<AdminUserOption> onChangeRole;
    private final Consumer<AdminUserOption> onResetPassword;
    private final Consumer<AdminUserOption> onDelete;
    private final BooleanSupplier isBusy;

    AdminUserActionsCell(
            Consumer<AdminUserOption> onChangeRole,
            Consumer<AdminUserOption> onResetPassword,
            Consumer<AdminUserOption> onDelete,
            BooleanSupplier isBusy) {
        this.onChangeRole = onChangeRole;
        this.onResetPassword = onResetPassword;
        this.onDelete = onDelete;
        this.isBusy = isBusy;
        actions.setStyle("-fx-alignment: center-left;");
        roleButton.setOnAction(event -> applyToCurrentUser(onChangeRole));
        resetPasswordButton.setOnAction(event -> applyToCurrentUser(onResetPassword));
        deleteButton.setOnAction(event -> applyToCurrentUser(onDelete));
        roleButton.setStyle(secondaryButtonStyle());
        resetPasswordButton.setStyle(secondaryButtonStyle());
        deleteButton.setStyle(dangerButtonStyle());
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        AdminUserOption user = getUserAtRow();
        if (empty || user == null) {
            setGraphic(null);
            return;
        }
        roleButton.setText("Đổi thành " + user.oppositeRole());
        roleButton.setDisable(isBusy.getAsBoolean());
        resetPasswordButton.setDisable(isBusy.getAsBoolean());
        deleteButton.setDisable(isBusy.getAsBoolean());
        setGraphic(actions);
    }

    private void applyToCurrentUser(Consumer<AdminUserOption> action) {
        AdminUserOption user = getUserAtRow();
        if (user != null && !isBusy.getAsBoolean()) {
            action.accept(user);
        }
    }

    private AdminUserOption getUserAtRow() {
        int index = getIndex();
        return index >= 0 && index < getTableView().getItems().size()
                ? getTableView().getItems().get(index)
                : null;
    }

    private static String secondaryButtonStyle() {
        return "-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 7; -fx-padding: 6 9 6 9; -fx-cursor: hand;";
    }

    private static String dangerButtonStyle() {
        return "-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 7; -fx-padding: 6 9 6 9; -fx-cursor: hand;";
    }
}
