package com.tkb.frontend.user.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Màn hình Thông tin cá nhân: hiển thị thông tin tài khoản. */
public class PersonalInfoPanel extends VBox {
    private final MainFrame frame;

    public PersonalInfoPanel(MainFrame frame) {
        this.frame = frame;
        setPadding(new Insets(22, 26, 20, 26));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");

        Label td = Functions.chuDe("Thông tin cá nhân");
        Label moTa = Functions.moTa("Thông tin tài khoản đang đăng nhập.");
        VBox header = new VBox(4, td, moTa);

        VBox card = Functions.the(16);
        String username = frame.trangThai().username;
        if (username == null || username.isBlank()) username = "user";
        String role = frame.trangThai().role;
        if (role == null || role.isBlank()) role = "USER";

        card.getChildren().addAll(
                row("Tên đăng nhập", username),
                row("Vai trò", role));

        Button quayLai = Functions.nutPhu("Quay lại");
        quayLai.setOnAction(e -> frame.denTrang(MainFrame.CARD_OVERVIEW));
        HBox nut = new HBox(10, Functions.khoangTrong(), quayLai);
        nut.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, card, nut);
    }

    private HBox row(String nhan, String giaTri) {
        HBox h = new HBox(12);
        Label l = Functions.nhanNho(nhan);
        l.setMinWidth(140);
        Label v = Functions.moTa(giaTri == null || giaTri.isBlank() ? "—" : giaTri);
        v.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13px;");
        h.getChildren().addAll(l, v);
        return h;
    }
}
