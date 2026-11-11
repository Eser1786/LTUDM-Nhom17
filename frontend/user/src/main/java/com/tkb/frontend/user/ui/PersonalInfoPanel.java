package com.tkb.frontend.user.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Màn hình Thông tin cá nhân: hiển thị và chỉnh sửa username + mật khẩu. */
public class PersonalInfoPanel extends VBox {
    private final MainFrame frame;
    private TextField txtUsername;
    private PasswordField txtMatKhauCu;
    private PasswordField txtMatKhauMoi;

    public PersonalInfoPanel(MainFrame frame) {
        this.frame = frame;
        setPadding(new Insets(22, 26, 20, 26));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");

        Label td = Functions.chuDe("Thông tin cá nhân");
        Label moTa = Functions.moTa("Xem và chỉnh sửa thông tin tài khoản.");
        VBox header = new VBox(4, td, moTa);

        VBox card = Functions.the(16);
        String username = frame.trangThai().username;
        if (username == null || username.isBlank()) username = "user";

        txtUsername = new TextField(username);
        txtUsername.setFont(Functions.F_CHU);
        txtUsername.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        txtMatKhauCu = new PasswordField();
        txtMatKhauCu.setPromptText("Mật khẩu hiện tại");
        txtMatKhauCu.setFont(Functions.F_CHU);
        txtMatKhauCu.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        txtMatKhauMoi = new PasswordField();
        txtMatKhauMoi.setPromptText("Mật khẩu mới (để trống nếu không đổi)");
        txtMatKhauMoi.setFont(Functions.F_CHU);
        txtMatKhauMoi.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");

        card.getChildren().addAll(
                new VBox(4, Functions.nhanNho("Tên đăng nhập"), txtUsername),
                new VBox(4, Functions.nhanNho("Mật khẩu cũ"), txtMatKhauCu),
                new VBox(4, Functions.nhanNho("Mật khẩu mới"), txtMatKhauMoi));

        Button luu = Functions.nutChinh("Lưu");
        luu.setOnAction(e -> {
            String u = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
            if (u.isBlank()) return;
            String oldPass = txtMatKhauCu.getText() == null ? "" : txtMatKhauCu.getText();
            String newPass = txtMatKhauMoi.getText() == null ? "" : txtMatKhauMoi.getText();
            String stored = frame.trangThai().password;
            if (stored != null && !stored.isBlank() && !oldPass.equals(stored)) {
                return; // mật khẩu cũ không khớp
            }
            if (!newPass.isEmpty() && (newPass.length() < 8 || newPass.length() > 64)) return;
            frame.trangThai().username = u;
            if (!newPass.isEmpty()) frame.trangThai().password = newPass;
            frame.capNhatHeader();
            txtMatKhauCu.clear();
            txtMatKhauMoi.clear();
        });
        HBox nut = new HBox(10, Functions.khoangTrong(), luu);
        nut.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().add(nut);

        Button quayLai = Functions.nutPhu("Quay lại");
        quayLai.setOnAction(e -> frame.denTrang(MainFrame.CARD_OVERVIEW));
        HBox nut2 = new HBox(10, Functions.khoangTrong(), quayLai);
        nut2.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, card, nut2);
    }
}
