package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.ui.MainFrame.AppUiState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/** Màn hình đăng nhập: hai cột, có hiện/ẩn mật khẩu, ghi nhớ đăng nhập và liên kết quên mật khẩu. */
public class LoginPanel extends HBox {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private boolean registrationMode;
    private Label statusLabel;
    private Button nutDangNhap;
    private Button toggleMode;
    private TextField txtEmail;
    private PasswordField txtMatKhau;
    private PasswordField txtXacNhan;

    public LoginPanel(AppUiState st, Runnable onLogin) {
        this(st, ignoreCredentials(onLogin), null, true);
    }

    public LoginPanel(AppUiState st, BiConsumer<String, String> onLogin) {
        this(st, onLogin, null, false);
    }

    public LoginPanel(
            BiConsumer<String, String> onLogin,
            BiConsumer<String, String> onRegister) {
        this(null, onLogin, Objects.requireNonNull(onRegister, "onRegister"), false);
    }

    private LoginPanel(
            AppUiState st,
            BiConsumer<String, String> onLogin,
            BiConsumer<String, String> onRegister,
            boolean authenticate) {
        Objects.requireNonNull(onLogin, "onLogin");
        setStyle("-fx-background-color: #F6F8FB;");
        setFillHeight(true);

        VBox trai = new VBox(18);
        trai.setPadding(new Insets(48, 44, 48, 56));
        trai.setPrefWidth(720);
        trai.setStyle("-fx-background-color: linear-gradient(to bottom right, #0F172A, #1E3A8A);");

        Label nhanDau = new Label("PROJECT 15 · THỜI KHÓA BIỂU");
        nhanDau.setTextFill(Color.web("#93C5FD"));
        nhanDau.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        Label tieuDe = new Label("Lịch học hợp lý.");
        tieuDe.setTextFill(Color.WHITE);
        tieuDe.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));

        Label moTa = new Label("Chọn nhóm lớp học phần, đặt mức ưu tiên cá nhân và để hệ thống "
                + "tìm những phương án thời khóa biểu phù hợp nhất với bạn.");
        moTa.setTextFill(Color.web("#CBD5E1"));
        moTa.setFont(Font.font("Segoe UI", 14));
        moTa.setWrapText(true);
        moTa.setMaxWidth(480);

        StackPane minhHoa = minhHoaLich();
        VBox.setVgrow(minhHoa, Priority.ALWAYS);

        Label thongDiep = new Label("Ít khoảng trống hơn  ·  Không trùng giờ học");
        thongDiep.setTextFill(Color.web("#93C5FD"));
        thongDiep.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        trai.getChildren().addAll(nhanDau, tieuDe, moTa, minhHoa, thongDiep);

        VBox phai = new VBox(14);
        phai.setPadding(new Insets(56, 64, 56, 64));
        phai.setAlignment(Pos.CENTER_LEFT);
        phai.setStyle("-fx-background-color: #F6F8FB;");
        HBox.setHgrow(phai, Priority.ALWAYS);

        VBox theForm = Functions.the(28);
        theForm.setSpacing(14);
        theForm.setMaxWidth(420);

        Label tieuDeForm = Functions.chuDe("Chào mừng trở lại");
        Label moTaForm = Functions.moTa("Đăng nhập để tiếp tục xây dựng thời khóa biểu học kỳ này.");

        TextField txtEmail = Functions.oNhap();
        txtEmail.setPromptText("Tên đăng nhập");
        txtEmail.setText("student");
        VBox nhomEmail = new VBox(4, Functions.nhanNho("Tên đăng nhập"), txtEmail);

        PasswordField txtMatKhau = new PasswordField();
        txtMatKhau.setFont(Functions.F_CHU);
        txtMatKhau.setPromptText("Mật khẩu");
        txtMatKhau.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        TextField txtHien = Functions.oNhap();
        txtHien.setText(txtMatKhau.getText());
        txtHien.setVisible(false);
        txtHien.setManaged(false);
        StackPane khauStack = new StackPane(txtMatKhau, txtHien);
        txtMatKhau.textProperty().addListener((o, a, b) -> txtHien.setText(b));
        txtHien.textProperty().addListener((o, a, b) -> txtMatKhau.setText(b));

        Button nutHien = Functions.nutPhu("Hiện");
        nutHien.setMinWidth(64);
        nutHien.setOnAction(e -> {
            boolean hien = !txtHien.isVisible();
            txtHien.setVisible(hien);
            txtHien.setManaged(hien);
            txtMatKhau.setVisible(!hien);
            txtMatKhau.setManaged(!hien);
            if (hien) {
                txtHien.setText(txtMatKhau.getText());
            } else {
                txtMatKhau.setText(txtHien.getText());
            }
            nutHien.setText(hien ? "Ẩn" : "Hiện");
        });
        HBox khauRow = new HBox(8, khauStack, nutHien);
        HBox.setHgrow(khauStack, Priority.ALWAYS);
        VBox nhomKhau = new VBox(4, Functions.nhanNho("Mật khẩu"), khauRow);

        PasswordField txtXacNhan = new PasswordField();
        txtXacNhan.setFont(Functions.F_CHU);
        txtXacNhan.setPromptText("Nhập lại mật khẩu");
        txtXacNhan.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        VBox nhomXacNhan = new VBox(4, Functions.nhanNho("Xác nhận mật khẩu"), txtXacNhan);
        nhomXacNhan.setManaged(false);
        nhomXacNhan.setVisible(false);

        CheckBox chkGhiNho = new CheckBox("Ghi nhớ đăng nhập");
        chkGhiNho.setSelected(true);
        Button lienKetQuen = new Button("Quên mật khẩu?");
        lienKetQuen.setStyle("-fx-background-color: transparent; -fx-text-fill: #1D4ED8; -fx-cursor: hand;");
        lienKetQuen.setOnAction(e -> Functions.thongTin(st == null ? null : window(),
                "Vui lòng liên hệ phòng Đào tạo để được cấp lại mật khẩu.\nEmail: daotao@univ.edu.vn"));
        HBox rowGhiNho = new HBox(chkGhiNho, Functions.khoangTrong(), lienKetQuen);
        rowGhiNho.setAlignment(Pos.CENTER_LEFT);

        Button nutDangNhap = Functions.nutChinh("Đăng nhập");
        nutDangNhap.setMaxWidth(Double.MAX_VALUE);
        nutDangNhap.setPadding(new Insets(10));
        nutDangNhap.setOnAction(e -> {
            String username = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
            String password = txtMatKhau.getText() == null ? "" : txtMatKhau.getText();
            boolean registering = onRegister != null && registrationMode;
            String validationError = validateCredentials(username, password);
            if (validationError != null) {
                showStatus(validationError, true);
                return;
            }
            if (registering) {
                if (!password.equals(txtXacNhan.getText())) {
                    showStatus("Mật khẩu xác nhận không khớp.", true);
                    return;
                }
            }
            setRequestInProgress(true);
            statusLabel.setText("");
            if (registering) {
                onRegister.accept(username, password);
                return;
            }
            try {
                if (authenticate) {
                    st.client.login(username, password);
                }
                onLogin.accept(username, password);
            } catch (ApiException ex) {
                setRequestInProgress(false);
                showStatus(ex.getMessage(), true);
            } catch (RuntimeException ex) {
                setRequestInProgress(false);
                showStatus("Không thể gửi yêu cầu đăng nhập: " + ex.getMessage(), true);
            }
        });

        Label statusLabel = Functions.moTa("");
        statusLabel.setWrapText(true);
        Button toggleMode = new Button("Chưa có tài khoản? Đăng ký");
        toggleMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #1D4ED8; -fx-cursor: hand;");
        toggleMode.setVisible(onRegister != null);
        toggleMode.setManaged(onRegister != null);
        toggleMode.setOnAction(event -> {
            registrationMode = !registrationMode;
            boolean registering = registrationMode;
            nhomXacNhan.setVisible(registering);
            nhomXacNhan.setManaged(registering);
            rowGhiNho.setVisible(!registering);
            rowGhiNho.setManaged(!registering);
            tieuDeForm.setText(registering ? "Tạo tài khoản" : "Chào mừng trở lại");
            moTaForm.setText(registering
                    ? "Đăng ký tài khoản USER để bắt đầu xây dựng thời khóa biểu."
                    : "Đăng nhập để tiếp tục xây dựng thời khóa biểu học kỳ này.");
            nutDangNhap.setText(registering ? "Đăng ký" : "Đăng nhập");
            toggleMode.setText(registering ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký");
            txtXacNhan.clear();
            statusLabel.setText("");
        });

        Label ghiChu = Functions.moTa("Sử dụng tài khoản sinh viên do nhà trường cấp. "
                + "Cần hỗ trợ? Gọi 1900 0000 hoặc email hotro@univ.edu.vn.");
        ghiChu.setTextAlignment(TextAlignment.LEFT);

        Label moPhong = Functions.nhanCanhBao("Kết nối máy chủ backend qua TCP · cổng 9000");

        theForm.getChildren().addAll(tieuDeForm, moTaForm, Functions.vach(), nhomEmail, nhomKhau,
                nhomXacNhan, rowGhiNho, nutDangNhap, statusLabel, toggleMode, ghiChu, moPhong);

        phai.getChildren().add(theForm);
        getChildren().addAll(trai, phai);

        this.statusLabel = statusLabel;
        this.nutDangNhap = nutDangNhap;
        this.toggleMode = toggleMode;
        this.txtEmail = txtEmail;
        this.txtMatKhau = txtMatKhau;
        this.txtXacNhan = txtXacNhan;
    }

    public void setRequestInProgress(boolean inProgress) {
        nutDangNhap.setDisable(inProgress);
        toggleMode.setDisable(inProgress);
        txtEmail.setDisable(inProgress);
        txtMatKhau.setDisable(inProgress);
        txtXacNhan.setDisable(inProgress);
    }

    public void showStatus(String message, boolean error) {
        statusLabel.setTextFill(error ? Functions.MA_LOI : Color.web("#15803D"));
        statusLabel.setText(message == null ? "" : message);
    }

    public void registrationSucceeded() {
        setRequestInProgress(false);
        if (registrationMode) {
            toggleMode.fire();
        }
        txtMatKhau.clear();
        txtXacNhan.clear();
        showStatus("Đăng ký thành công. Hãy đăng nhập bằng tài khoản vừa tạo.", false);
    }

    private String validateCredentials(String username, String password) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Tên đăng nhập phải từ 3 đến 30 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới (_).";
        }
        if (password.isEmpty()) {
            return "Vui lòng nhập mật khẩu.";
        }
        if (password.length() < 8 || password.length() > 64) {
            return "Mật khẩu phải có độ dài từ 8 đến 64 ký tự.";
        }
        return null;
    }

    private static BiConsumer<String, String> ignoreCredentials(Runnable onLogin) {
        Objects.requireNonNull(onLogin, "onLogin");
        return (username, password) -> onLogin.run();
    }

    private StackPane minhHoaLich() {
        // Card minh họa lịch học tối giản dạng lưới tuần.
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 14;");
        String[] ngay = {"T2", "T3", "T4", "T5", "T6", "T7"};
        for (int c = 0; c < ngay.length; c++) {
            Label h = new Label(ngay[c]);
            h.setTextFill(Color.web("#93C5FD"));
            h.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            h.setAlignment(Pos.CENTER);
            h.setMinWidth(58);
            grid.add(h, c, 0);
        }
        String[][] o = {
            {"#3B82F6", "", "", "#10B981", "", ""},
            {"#3B82F6", "", "", "#10B981", "", ""},
            {"", "#8B5CF6", "", "", "", "#F59E0B"},
            {"", "#8B5CF6", "", "", "", "#F59E0B"},
        };
        for (int r = 0; r < o.length; r++) {
            for (int c = 0; c < o[r].length; c++) {
                Region cell = new Region();
                cell.setMinSize(58, 34);
                if (o[r][c].isEmpty()) {
                    cell.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 8;");
                } else {
                    cell.setStyle("-fx-background-color: " + o[r][c] + "; -fx-background-radius: 8;");
                }
                grid.add(cell, c, r + 1);
            }
        }
        StackPane p = new StackPane(grid);
        p.setAlignment(Pos.CENTER_LEFT);
        return p;
    }

    private javafx.stage.Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }
}
