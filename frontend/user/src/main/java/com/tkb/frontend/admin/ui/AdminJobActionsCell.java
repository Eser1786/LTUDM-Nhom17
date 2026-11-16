package com.tkb.frontend.admin.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class AdminJobActionsCell extends TableCell<AdminJobOption, Void> {
    private final Button cancelButton = new Button("Hủy việc");
    private final Label completedLabel = new Label("—");
    private final HBox actions = new HBox(cancelButton);
    private final Consumer<AdminJobOption> onCancel;
    private final BooleanSupplier isBusy;

    AdminJobActionsCell(Consumer<AdminJobOption> onCancel, BooleanSupplier isBusy) {
        this.onCancel = onCancel;
        this.isBusy = isBusy;
        actions.setAlignment(Pos.CENTER);
        cancelButton.setStyle(dangerButtonStyle());
        cancelButton.setOnAction(event -> applyToCurrentJob(onCancel));
        completedLabel.setStyle("-fx-text-fill: #94A3B8; -fx-alignment: center;");
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        AdminJobOption job = getJobAtRow();
        if (empty || job == null) {
            setGraphic(null);
            return;
        }

        if (job.isActive()) {
            cancelButton.setDisable(isBusy.getAsBoolean());
            setGraphic(actions);
        } else {
            setGraphic(completedLabel);
        }
    }

    private void applyToCurrentJob(Consumer<AdminJobOption> action) {
        AdminJobOption job = getJobAtRow();
        if (job != null && !isBusy.getAsBoolean()) {
            action.accept(job);
        }
    }

    private AdminJobOption getJobAtRow() {
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
