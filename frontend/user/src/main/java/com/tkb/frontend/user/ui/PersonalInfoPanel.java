package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Student;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Màn hình Thông tin cá nhân: hiển thị và chỉnh sửa trực tiếp thông tin user + sinh viên. */
public class PersonalInfoPanel extends VBox {
    private final MainFrame frame;
    private final VBox noiDung = new VBox(14);

    private TextField txtHoTen;
    private TextField txtMssv;
    private TextField txtLop;
    private TextField txtEmail;
    private PasswordField txtMatKhauCu;
    private PasswordField txtMatKhau;
    private GridPane grid;
    private boolean dangSua = false;
    private Button btnSua;
    private Button btnLuu;
    private Button btnHuy;

    public PersonalInfoPanel(MainFrame frame) {
        this.frame = frame;
        setPadding(new Insets(22, 26, 20, 26));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");

        Label td = Functions.chuDe("Thông tin cá nhân");
        Label moTa = Functions.moTa("Xem và chỉnh sửa thông tin tài khoản, thông tin sinh viên.");
        VBox header = new VBox(4, td, moTa);

        noiDung.getChildren().add(veThongTin());

        Button quayLai = Functions.nutPhu("Quay lại");
        quayLai.setOnAction(e -> frame.denTrang(MainFrame.CARD_OVERVIEW));
        HBox nut = new HBox(10, Functions.khoangTrong(), quayLai);
        nut.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, noiDung, nut);
    }

    private VBox veThongTin() {
        VBox card = Functions.the(16);
        Student sv = frame.trangThai().sinhVien;
        if (sv == null) sv = new Student("Sinh viên", "student", "USER", "student@sv.univ.edu.vn");
        String username = frame.trangThai().username;
        if (username == null || username.isBlank()) username = sv.mssv();
        if (username == null || username.isBlank()) username = "student";

        grid = new GridPane();
        GridPane g = grid;
        g.setHgap(12);
        g.setVgap(8);
        g.setPadding(new Insets(8));

        // Username (read-only)
        themDong(g, 0, "Tên đăng nhập", username, false);
        // Role
        themDong(g, 1, "Vai trò", sv.lop(), false);

        txtHoTen = new TextField(sv.hoTen());
        txtMssv = new TextField(sv.mssv());
        txtLop = new TextField(sv.lop());
        txtEmail = new TextField(sv.email());
        txtMatKhauCu = new PasswordField();
        txtMatKhauCu.setPromptText("Mật khẩu hiện tại");
        txtMatKhau = new PasswordField();
        txtMatKhau.setPromptText("Mật khẩu mới (để trống nếu không đổi)");

        themField(g, 2, "Họ tên", txtHoTen);
        themField(g, 3, "Mã số sinh viên", txtMssv);
        themField(g, 4, "Lớp", txtLop);
        themField(g, 5, "Email", txtEmail);
        themField(g, 6, "Mật khẩu cũ", txtMatKhauCu);
        themField(g, 7, "Mật khẩu mới", txtMatKhau);

        // An hang mat khau mac dinh.
        setRowVisible(g, 6, false);
        setRowVisible(g, 7, false);
        setEditable(false);

        btnSua = Functions.nutChinh("Sửa thông tin");
        btnLuu = Functions.nutPhu("Lưu");
        btnHuy = Functions.nutPhu("Hủy");
        btnLuu.setVisible(false);
        btnLuu.setManaged(false);
        btnHuy.setVisible(false);
        btnHuy.setManaged(false);

        btnSua.setOnAction(e -> batDauSua());
        btnLuu.setOnAction(e -> luu());
        btnHuy.setOnAction(e -> huySua());

        HBox nut = new HBox(10, Functions.khoangTrong(), btnSua, btnLuu, btnHuy);
        nut.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(g, nut);
        return card;
    }

    private void themDong(GridPane g, int row, String nhan, String giaTri, boolean editable) {
        Label l = Functions.nhanNho(nhan);
        Label v = Functions.moTa(giaTri == null || giaTri.isBlank() ? "—" : giaTri);
        v.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13px;");
        g.add(l, 0, row);
        g.add(v, 1, row);
    }

    private void themField(GridPane g, int row, String nhan, javafx.scene.control.Control field) {
        Label l = Functions.nhanNho(nhan);
        if (field instanceof TextField tf) {
            tf.setFont(Functions.F_CHU);
            tf.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        } else if (field instanceof PasswordField pf) {
            pf.setFont(Functions.F_CHU);
            pf.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        }
        g.add(l, 0, row);
        g.add(field, 1, row);
    }

    private void setEditable(boolean editable) {
        txtHoTen.setEditable(editable);
        txtMssv.setEditable(editable);
        txtLop.setEditable(editable);
        txtEmail.setEditable(editable);
        txtMatKhauCu.setEditable(editable);
        txtMatKhau.setEditable(editable);
        txtHoTen.setDisable(!editable);
        txtMssv.setDisable(!editable);
        txtLop.setDisable(!editable);
        txtEmail.setDisable(!editable);
        txtMatKhauCu.setDisable(!editable);
        txtMatKhau.setDisable(!editable);
    }

    private void batDauSua() {
        dangSua = true;
        setEditable(true);
        setRowVisible(grid, 6, true);
        setRowVisible(grid, 7, true);
        btnSua.setVisible(false);
        btnSua.setManaged(false);
        btnLuu.setVisible(true);
        btnLuu.setManaged(true);
        btnHuy.setVisible(true);
        btnHuy.setManaged(true);
    }

    private void huySua() {
        Student sv = frame.trangThai().sinhVien;
        if (sv != null) {
            txtHoTen.setText(sv.hoTen());
            txtMssv.setText(sv.mssv());
            txtLop.setText(sv.lop());
            txtEmail.setText(sv.email());
        }
        txtMatKhau.clear();
        dangSua = false;
        setEditable(false);
        setRowVisible(grid, 6, false);
        setRowVisible(grid, 7, false);
        txtMatKhauCu.clear();
        txtMatKhau.clear();
        btnSua.setVisible(true);
        btnSua.setManaged(true);
        btnLuu.setVisible(false);
        btnLuu.setManaged(false);
        btnHuy.setVisible(false);
        btnHuy.setManaged(false);
    }

    private void luu() {
        String oldPass = txtMatKhauCu.getText() == null ? "" : txtMatKhauCu.getText();
        String newPass = txtMatKhau.getText() == null ? "" : txtMatKhau.getText();

        if (!newPass.isEmpty()) {
            String savedPassword = frame.trangThai().password == null ? "" : frame.trangThai().password;
            if (oldPass.isEmpty()) {
                Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Vui lòng nhập mật khẩu cũ trước khi đổi mật khẩu.");
                return;
            }
            if (!savedPassword.equals(oldPass)) {
                Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Mật khẩu cũ không đúng.");
                return;
            }
            if (oldPass.equals(newPass)) {
                Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Mật khẩu mới phải khác mật khẩu cũ.");
                return;
            }
            if (newPass.length() < 8 || newPass.length() > 64) {
                Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Mật khẩu mới phải có độ dài từ 8 đến 64 ký tự.");
                return;
            }
        } else if (!oldPass.isEmpty()) {
            Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Vui lòng nhập mật khẩu mới nếu muốn đổi mật khẩu.");
            return;
        }

        String mssvMoi = txtMssv.getText() == null ? "" : txtMssv.getText().trim();
        if (!mssvMoi.matches("\\d{10}")) {
            Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Mã số sinh viên phải là 10 chữ số.");
            return;
        }
        Student sv = frame.trangThai().sinhVien;
        String mssv = sv == null ? "unknown" : sv.mssv();
        Student moi = new Student(
                txtHoTen.getText().trim(),
                txtMssv.getText().trim().isEmpty() ? mssv : txtMssv.getText().trim(),
                txtLop.getText().trim(),
                txtEmail.getText().trim());
        frame.trangThai().sinhVien = moi;
        try { frame.trangThai().client.setSinhVien(moi); } catch (Exception ignored) {}
        if (!newPass.isEmpty()) {
            frame.trangThai().password = newPass;
        }
        frame.capNhatHeader();
        huySua();
    }

    private void setRowVisible(GridPane g, int row, boolean visible) {
        for (javafx.scene.Node n : g.getChildren()) {
            if (GridPane.getRowIndex(n) != null && GridPane.getRowIndex(n) == row) {
                n.setVisible(visible);
                n.setManaged(visible);
            }
        }
    }

    private void lamMoi() {
        noiDung.getChildren().setAll(veThongTin());
    }
}
