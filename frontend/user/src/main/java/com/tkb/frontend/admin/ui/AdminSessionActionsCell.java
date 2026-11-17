package com.tkb.frontend.admin.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class AdminSessionActionsCell extends TableCell<AdminSessionOption, Void> {
    private final Button kickButton = new Button("Ngắt kết nối");
    private final HBox actions = new HBox(kickButton);
    private final Consumer<AdminSessionOption> onKick;
    private final BooleanSupplier isBusy;

    AdminSessionActionsCell(Consumer<AdminSessionOption> onKick, BooleanSupplier isBusy) {
        this.onKick = onKick;
        this.isBusy = isBusy;
        actions.setAlignment(Pos.CENTER);
        kickButton.setStyle(dangerButtonStyle());
        kickButton.setOnAction(event -> applyToCurrentSession(onKick));
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        AdminSessionOption session = getSessionAtRow();
        if (empty || session == null) {
            setGraphic(null);
            return;
        }

        kickButton.setDisable(isBusy.getAsBoolean());
        setGraphic(actions);
    }

    private void applyToCurrentSession(Consumer<AdminSessionOption> action) {
        AdminSessionOption session = getSessionAtRow();
        if (session != null && !isBusy.getAsBoolean()) {
            action.accept(session);
        }
    }

    private AdminSessionOption getSessionAtRow() {
        int index = getIndex();
        return index >= 0 && index < getTableView().getItems().size()
                ? getTableView().getItems().get(index)
                : null;
    }

    private static String dangerButtonStyle() {
        return "-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 7; -fx-padding: 5 10 5 10; -fx-cursor: hand; -fx-font-size: 11px;";
    }
}
