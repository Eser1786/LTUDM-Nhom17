package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.TkbClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.util.concurrent.CompletableFuture;

/** Hộp thoại Quên mật khẩu / Đổi mật khẩu xác thực bằng mã OTP 6 số. */
public final class ChangePasswordOtpDialog {

    private ChangePasswordOtpDialog() {}

    public static void hienThi(Window owner, TkbClient client, String presetUsername, boolean lockUsername) {
        if (client == null) {
            Functions.loi(owner, "Chưa khởi tạo kết nối máy chủ.");
            return;
        }

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox hop = Functions.the(24);
        hop.setSpacing(14);
        hop.setPrefWidth(480);

        Label tieuDe = Functions.chuDe(lockUsername ? "Đổi mật khẩu qua OTP" : "Khôi phục mật khẩu qua OTP");
        Label moTa = Functions.moTa("Bước 1: Nhấn Gửi mã OTP (xem mã 6 số trên Console Server).\n"
                + "Bước 2: Nhập mã OTP cùng mật khẩu mới để hoàn tất.");

        // --- Bước 1: Tài khoản & Nút gửi OTP ---
        TextField txtUser = Functions.oNhap();
        if (presetUsername != null && !presetUsername.isBlank()) {
            txtUser.setText(presetUsername.trim());
        }
        txtUser.setPromptText("Nhập tên đăng nhập (vd: student)");
        txtUser.setDisable(lockUsername);

        Button btnGuiOtp = Functions.nutPhu("Gửi mã OTP");
        btnGuiOtp.setMinWidth(120);

        HBox rowUser = new HBox(8, txtUser, btnGuiOtp);
        HBox.setHgrow(txtUser, Priority.ALWAYS);
        VBox boxUser = new VBox(4, Functions.nhanNho("Tên đăng nhập"), rowUser);

        // --- Bước 2: Nhập OTP & Mật khẩu mới ---
        TextField txtOtp = Functions.oNhap();
        txtOtp.setPromptText("Nhập mã OTP 6 chữ số");

        PasswordField txtNewPass = new PasswordField();
        txtNewPass.setFont(Functions.F_CHU);
        txtNewPass.setPromptText("Mật khẩu mới (8 - 64 ký tự)");
        txtNewPass.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");

        PasswordField txtConfirmPass = new PasswordField();
        txtConfirmPass.setFont(Functions.F_CHU);
        txtConfirmPass.setPromptText("Nhập lại mật khẩu mới");
        txtConfirmPass.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");

        Label lblTrangThai = Functions.moTa("");
        lblTrangThai.setWrapText(true);

        Button btnHuy = Functions.nutPhu("Đóng");
        Button btnXacNhan = Functions.nutChinh("Xác nhận đổi mật khẩu");

        btnHuy.setOnAction(e -> stage.close());

        // Xử lý gửi OTP trên luồng nền (không làm đơ giao diện JavaFX)
        btnGuiOtp.setOnAction(e -> {
            String user = txtUser.getText() == null ? "" : txtUser.getText().trim();
            if (user.isEmpty()) {
                lblTrangThai.setTextFill(Functions.MA_LOI);
                lblTrangThai.setText("Vui lòng nhập tên đăng nhập trước khi lấy OTP.");
                return;
            }
            btnGuiOtp.setDisable(true);
            lblTrangThai.setTextFill(Functions.MA_CHU_PHU);
            lblTrangThai.setText("Đang yêu cầu máy chủ tạo mã OTP...");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return client.requestPasswordOtp(user);
                } catch (ApiException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }).whenComplete((msg, err) -> Platform.runLater(() -> {
                btnGuiOtp.setDisable(false);
                if (err != null) {
                    lblTrangThai.setTextFill(Functions.MA_LOI);
                    lblTrangThai.setText(err.getCause() != null ? err.getCause().getMessage() : err.getMessage());
                } else {
                    lblTrangThai.setTextFill(Color.web("#15803D"));
                    lblTrangThai.setText(msg);
                    txtOtp.requestFocus();
                }
            }));
        });

        // Xử lý xác nhận OTP & Đổi mật khẩu
        btnXacNhan.setOnAction(e -> {
            String user = txtUser.getText() == null ? "" : txtUser.getText().trim();
            String otp = txtOtp.getText() == null ? "" : txtOtp.getText().trim();
            String p1 = txtNewPass.getText() == null ? "" : txtNewPass.getText();
            String p2 = txtConfirmPass.getText() == null ? "" : txtConfirmPass.getText();

            if (user.isEmpty() || !otp.matches("^\\d{6}$")) {
                lblTrangThai.setTextFill(Functions.MA_LOI);
                lblTrangThai.setText("Vui lòng nhập tên đăng nhập và đúng 6 chữ số OTP.");
                return;
            }
            if (p1.length() < 8 || p1.length() > 64) {
                lblTrangThai.setTextFill(Functions.MA_LOI);
                lblTrangThai.setText("Mật khẩu mới phải từ 8 đến 64 ký tự.");
                return;
            }
            if (!p1.equals(p2)) {
                lblTrangThai.setTextFill(Functions.MA_LOI);
                lblTrangThai.setText("Mật khẩu xác nhận không khớp.");
                return;
            }

            btnXacNhan.setDisable(true);
            btnGuiOtp.setDisable(true);
            lblTrangThai.setTextFill(Functions.MA_CHU_PHU);
            lblTrangThai.setText("Đang xác thực OTP và cập nhật mật khẩu...");

            CompletableFuture.runAsync(() -> {
                try {
                    client.resetPasswordWithOtp(user, otp, p1);
                } catch (ApiException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }).whenComplete((v, err) -> Platform.runLater(() -> {
                btnXacNhan.setDisable(false);
                btnGuiOtp.setDisable(false);
                if (err != null) {
                    lblTrangThai.setTextFill(Functions.MA_LOI);
                    lblTrangThai.setText(err.getCause() != null ? err.getCause().getMessage() : err.getMessage());
                } else {
                    stage.close();
                    // Đợi JavaFX đóng xong Stage OTP rồi mới mở Alert thông báo
                    Platform.runLater(() -> Functions.thongTin(
                            owner,
                            "Đổi mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới."
                    ));
                }
            }));
        });

        HBox rowNut = new HBox(10, Functions.khoangTrong(), btnHuy, btnXacNhan);
        rowNut.setAlignment(Pos.CENTER_RIGHT);

        hop.getChildren().addAll(
                tieuDe,
                moTa,
                Functions.vach(),
                boxUser,
                new VBox(4, Functions.nhanNho("Mã OTP (6 số)"), txtOtp),
                new VBox(4, Functions.nhanNho("Mật khẩu mới"), txtNewPass),
                new VBox(4, Functions.nhanNho("Xác nhận mật khẩu mới"), txtConfirmPass),
                lblTrangThai,
                rowNut
        );

        StackPane overlay = new StackPane(hop);
        overlay.setPadding(new Insets(40));
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.35);");
        overlay.setAlignment(Pos.CENTER);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add("data:text/css,.root{-fx-font-family:'Segoe UI';}");
        stage.setScene(scene);
        stage.showAndWait();
    }
}