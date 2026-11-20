
package com.tkb.frontend.admin.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

public final class AdminTableStyle {

    private AdminTableStyle() {
    }

    public static <T> void apply(TableView<T> table) {

        // Giao dien tong the
        table.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );

        // Chieu cao dong
        table.setFixedCellSize(54);

        // Giao dien dong du lieu
        table.setRowFactory(tv -> new TableRow<T>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setStyle("-fx-background-color: white;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #FBEAF1;");
                } else {
                    setStyle("-fx-background-color: white;");
                }
            }
        });

        // Cap nhat khi gan vao Scene
        table.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateHeader(table);
            }
        });

        // Cap nhat khi JavaFX tao Skin
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                updateHeader(table);
            }
        });

        // Cap nhat khi bang duoc layout
        table.needsLayoutProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                updateHeader(table);
            }
        });

        // Neu Skin da ton tai
        if (table.getSkin() != null) {
            updateHeader(table);
        }
    }

    private static void updateHeader(TableView<?> table) {

        if (table.getScene() == null || table.getSkin() == null) {
            return;
        }

        Platform.runLater(() -> {

            if (table.getScene() == null || table.getSkin() == null) {
                return;
            }

            table.applyCss();
            table.layout();

            // Nen header
            for (Node node : table.lookupAll(
                    ".column-header-background")) {

                node.setStyle(
                    "-fx-background-color: #B96E8A;"
                );
            }

            // Tung o header
            for (Node node : table.lookupAll(".column-header")) {

                node.setStyle(
                    "-fx-background-color: #B96E8A;" +
                    "-fx-size: 58px;"
                );

                for (Node child : node.lookupAll(".label")) {

                    if (child instanceof Label label) {

                        label.setAlignment(Pos.CENTER);
                        label.setMaxWidth(Double.MAX_VALUE);

                        label.setStyle(
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-alignment: center;"
                        );
                    }
                }
            }

            // Vung header ben phai thanh cuon
            for (Node node : table.lookupAll(".filler")) {

                node.setStyle(
                    "-fx-background-color: #B96E8A;"
                );
            }
        });
    }
}
