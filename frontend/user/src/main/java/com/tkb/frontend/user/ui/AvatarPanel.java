package com.tkb.frontend.user.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Avatar hÃ¬nh trÃ²n hiá»ƒn thá»‹ chá»¯ viáº¿t táº¯t cá»§a sinh viÃªn. */
public class AvatarPanel extends StackPane {
    private final Circle circle = new Circle();
    private final Label label = new Label("?");
    private int kichThuoc;

    public AvatarPanel(String chu, int kichThuoc) {
        this.kichThuoc = kichThuoc;
        setAlignment(Pos.CENTER);
        circle.setFill(Color.web("#1D4ED8"));
        circle.setStroke(Color.web("#DBEAFE"));
        circle.setStrokeWidth(2);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, kichThuoc * 0.36));
        getChildren().addAll(circle, label);
        setChu(chu);
        render();
    }

    public void setChu(String chu) {
        label.setText(chu == null || chu.isBlank() ? "?" : chu);
    }

    private void render() {
        circle.setRadius(kichThuoc / 2.0);
        setMinSize(kichThuoc, kichThuoc);
        setPrefSize(kichThuoc, kichThuoc);
        setMaxSize(kichThuoc, kichThuoc);
    }

    public void setKichThuoc(int kichThuoc) {
        this.kichThuoc = kichThuoc;
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, kichThuoc * 0.36));
        render();
    }
}
