package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.ui.MainFrame.AppUiState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
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

/** MÃ n hÃ¬nh Ä‘Äƒng nháº­p: hai cá»™t, cÃ³ hiá»‡n/áº©n máº­t kháº©u, ghi nhá»› Ä‘Äƒng nháº­p vÃ  liÃªn káº¿t quÃªn máº­t kháº©u. */
public class LoginPanel extends HBox {

    public LoginPanel(AppUiState st, Runnable onLogin) {
        setStyle("-fx-background-color: #F6F8FB;");
        setFillHeight(true);

        VBox trai = new VBox(18);
        trai.setPadding(new Insets(48, 44, 48, 56));
        trai.setPrefWidth(720);
        trai.setStyle("-fx-background-color: linear-gradient(to bottom right, #0F172A, #1E3A8A);");

        Label nhanDau = new Label("PROJECT 15 Â· SINH VIÃŠN");
        nhanDau.setTextFill(Color.web("#93C5FD"));
        nhanDau.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        Label tieuDe = new Label("Lá»‹ch há»c há»£p lÃ½.");
        tieuDe.setTextFill(Color.WHITE);
        tieuDe.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));

        Label moTa = new Label("Chá»n nhÃ³m lá»›p há»c pháº§n, Ä‘áº·t má»©c Æ°u tiÃªn cÃ¡ nhÃ¢n vÃ  Ä‘á»ƒ há»‡ thá»‘ng "
                + "tÃ¬m nhá»¯ng phÆ°Æ¡ng Ã¡n thá»i khÃ³a biá»ƒu phÃ¹ há»£p nháº¥t vá»›i báº¡n.");
        moTa.setTextFill(Color.web("#CBD5E1"));
        moTa.setFont(Font.font("Segoe UI", 14));
        moTa.setWrapText(true);
        moTa.setMaxWidth(480);

        StackPane minhHoa = minhHoaLich();
        VBox.setVgrow(minhHoa, Priority.ALWAYS);

        Label thongDiep = new Label("Ãt khoáº£ng trá»‘ng hÆ¡n  Â·  KhÃ´ng trÃ¹ng giá» há»c");
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

        Label tieuDeForm = Functions.chuDe("ChÃ o má»«ng trá»Ÿ láº¡i");
        Label moTaForm = Functions.moTa("ÄÄƒng nháº­p Ä‘á»ƒ tiáº¿p tá»¥c xÃ¢y dá»±ng thá»i khÃ³a biá»ƒu há»c ká»³ nÃ y.");

        TextField txtEmail = Functions.oNhap();
        txtEmail.setPromptText("email@sv.univ.edu.vn");
        txtEmail.setText("minh.anh@sv.univ.edu.vn");
        VBox nhomEmail = new VBox(4, Functions.nhanNho("Email sinh viÃªn"), txtEmail);

        PasswordField txtMatKhau = new PasswordField();
        txtMatKhau.setFont(Functions.F_CHU);
        txtMatKhau.setPromptText("Máº­t kháº©u");
        txtMatKhau.setText("12345678");
        txtMatKhau.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        TextField txtHien = Functions.oNhap();
        txtHien.setText(txtMatKhau.getText());
        txtHien.setVisible(false);
        txtHien.setManaged(false);
        StackPane khauStack = new StackPane(txtMatKhau, txtHien);
        txtMatKhau.textProperty().addListener((o, a, b) -> txtHien.setText(b));
        txtHien.textProperty().addListener((o, a, b) -> txtMatKhau.setText(b));

        Button nutHien = Functions.nutPhu("Hiá»‡n");
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
            nutHien.setText(hien ? "áº¨n" : "Hiá»‡n");
        });
        HBox khauRow = new HBox(8, khauStack, nutHien);
        HBox.setHgrow(khauStack, Priority.ALWAYS);
        VBox nhomKhau = new VBox(4, Functions.nhanNho("Máº­t kháº©u"), khauRow);

        CheckBox chkGhiNho = new CheckBox("Ghi nhá»› Ä‘Äƒng nháº­p");
        chkGhiNho.setSelected(true);
        Button lienKetQuen = new Button("QuÃªn máº­t kháº©u?");
        lienKetQuen.setStyle("-fx-background-color: transparent; -fx-text-fill: #1D4ED8; -fx-cursor: hand;");
        lienKetQuen.setOnAction(e -> Functions.thongTin(st == null ? null : window(),
                "Vui lÃ²ng liÃªn há»‡ phÃ²ng ÄÃ o táº¡o Ä‘á»ƒ Ä‘Æ°á»£c cáº¥p láº¡i máº­t kháº©u.\nEmail: daotao@univ.edu.vn"));
        HBox rowGhiNho = new HBox(chkGhiNho, Functions.khoangTrong(), lienKetQuen);
        rowGhiNho.setAlignment(Pos.CENTER_LEFT);

        Button nutDangNhap = Functions.nutChinh("ÄÄƒng nháº­p");
        nutDangNhap.setMaxWidth(Double.MAX_VALUE);
        nutDangNhap.setPadding(new Insets(10));
        nutDangNhap.setOnAction(e -> {
            if (txtEmail.getText() == null || txtEmail.getText().isBlank()) {
                Functions.canhBao(window(), "Vui lÃ²ng nháº­p email sinh viÃªn.");
                return;
            }
            if (txtMatKhau.getText() == null || txtMatKhau.getText().isBlank()) {
                Functions.canhBao(window(), "Vui lÃ²ng nháº­p máº­t kháº©u.");
                return;
            }
            onLogin.run();
        });

        Label ghiChu = Functions.moTa("Sá»­ dá»¥ng tÃ i khoáº£n sinh viÃªn do nhÃ  trÆ°á»ng cáº¥p. "
                + "Cáº§n há»— trá»£? Gá»i 1900 0000 hoáº·c email hotro@univ.edu.vn.");
        ghiChu.setTextAlignment(TextAlignment.LEFT);

        Label moPhong = Functions.nhanCanhBao("Cháº¿ Ä‘á»™ mÃ´ phá»ng Â· chÆ°a káº¿t ná»‘i mÃ¡y chá»§ tháº­t");

        theForm.getChildren().addAll(tieuDeForm, moTaForm, Functions.vach(), nhomEmail, nhomKhau,
                rowGhiNho, nutDangNhap, ghiChu, moPhong);

        phai.getChildren().add(theForm);
        getChildren().addAll(trai, phai);
    }

    private StackPane minhHoaLich() {
        // Card minh há»a lá»‹ch há»c tá»‘i giáº£n dáº¡ng lÆ°á»›i tuáº§n.
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
