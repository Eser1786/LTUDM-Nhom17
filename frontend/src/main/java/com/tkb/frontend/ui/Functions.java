package com.tkb.frontend.ui;

import com.tkb.frontend.net.ApiException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;

public final class Functions {
    // Shared JavaFX color palette and style constants used across the application UI.
    public static final Color MA_NEN = Color.web("#F6F8FB");
    public static final Color MA_THE = Color.WHITE;
    public static final Color MA_VIEN = Color.web("#E2E8F0");
    public static final Color MA_CHU = Color.web("#0F172A");
    public static final Color MA_CHU_DAM = Color.web("#1E293B");
    public static final Color MA_CHU_PHU = Color.web("#64748B");
    public static final Color MA_CHINH = Color.web("#1D4ED8");
    public static final Color MA_CHINH_DAM = Color.web("#1E40AF");
    public static final Color MA_CHINH_NEN = Color.web("#DBEAFE");
    public static final Color MA_XANH = Color.web("#16A34A");
    public static final Color MA_XANH_NEN = Color.web("#DCFCE7");
    public static final Color MA_XANH_DAM = Color.web("#15803D");
    public static final Color MA_LOI = Color.web("#DC2626");
    public static final Color MA_LOI_NEN = Color.web("#FEF2F2");
    public static final Color MA_CANH_BAO = Color.web("#C2410C");
    public static final Color MA_CANH_BAO_NEN = Color.web("#FFF7ED");
    public static final Color MA_XAM_NEN = Color.web("#F1F5F9");
    public static final Color MA_SIDEBAR = Color.web("#0F172A");
    public static final Color MA_SIDEBAR_VIEN = Color.web("#1E293B");
    public static final Color MA_SIDEBAR_CHU = Color.web("#CBD5E1");
    public static final Color MA_SIDEBAR_DANG_CHON = Color.web("#1D4ED8");

    // Reusable fonts used by labels and cards throughout the JavaFX UI.
    public static final Font F_CHU = Font.font("Segoe UI", 13);
    public static final Font F_CHU_BOLD = Font.font("Segoe UI", FontWeight.BOLD, 13);
    public static final Font F_CHU_DE = Font.font("Segoe UI", FontWeight.BOLD, 22);
    public static final Font F_TIEU_DE_THE = Font.font("Segoe UI", FontWeight.BOLD, 14);
    public static final Font F_MO_TA = Font.font("Segoe UI", 12);
    public static final Font F_NHO = Font.font("Segoe UI", 11);
    public static final Font F_SO_LON = Font.font("Segoe UI", FontWeight.BOLD, 26);

    private Functions() {}

    // ---------------------------------------------------------------- containers

    public static VBox the() {
        return the(16);
    }

    public static VBox the(int padding) {
        // Default card container for most panels. Keeps the visual layout consistent across screens.
        VBox v = new VBox();
        v.setPadding(new Insets(padding));
        v.setSpacing(8);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        return v;
    }

    public static VBox theNoiBat() {
        // Highlighted card used for the recommended option.
        VBox v = the(16);
        v.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #1D4ED8; "
                + "-fx-border-radius: 12; -fx-border-width: 2;");
        return v;
    }

    public static Region khoangTrong() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region vach() {
        Region r = new Region();
        r.setMinHeight(1);
        r.setStyle("-fx-background-color: #E2E8F0;");
        return r;
    }

    // ------------------------------------------------------------------ labels

    public static Label chuDe(String text) {
        Label l = new Label(text);
        l.setFont(F_CHU_DE);
        l.setTextFill(MA_CHU);
        return l;
    }

    public static Label moTa(String text) {
        Label l = new Label(text);
        l.setFont(F_MO_TA);
        l.setTextFill(MA_CHU_PHU);
        l.setWrapText(true);
        return l;
    }

    public static Label nhanNho(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(MA_CHU_PHU);
        return l;
    }

    public static Label tieuDeThe(String text) {
        Label l = new Label(text);
        l.setFont(F_TIEU_DE_THE);
        l.setTextFill(MA_CHU_DAM);
        return l;
    }

    public static Label nhan(String text, Color bg, Color fg) {
        Label l = new Label(text);
        l.setFont(F_NHO);
        l.setTextFill(fg);
        l.setPadding(new Insets(3, 9, 3, 9));
        String hexBg = toWeb(bg);
        l.setStyle("-fx-background-color: " + hexBg + "; -fx-background-radius: 999; -fx-border-radius: 999;");
        return l;
    }

    public static Label nhanHopLe(String text) {
        return nhan(text, MA_XANH_NEN, MA_XANH_DAM);
    }

    public static Label nhanDeXuat(String text) {
        return nhan(text, MA_CHINH_NEN, MA_CHINH_DAM);
    }

    public static Label nhanCanhBao(String text) {
        return nhan(text, MA_CANH_BAO_NEN, MA_CANH_BAO);
    }

    public static Label nhanXam(String text) {
        return nhan(text, MA_XAM_NEN, MA_CHU_PHU);
    }

    public static Label nhanLoi(String text) {
        return nhan(text, MA_LOI_NEN, MA_LOI);
    }

    public static Label nhanChinh(String text) {
        return nhan(text, MA_CHINH, Color.WHITE);
    }

    public static Label nhanF(String text, String bgHex, String fgHex) {
        Label l = new Label(text);
        l.setFont(F_NHO);
        l.setTextFill(Color.web(fgHex));
        l.setPadding(new Insets(3, 9, 3, 9));
        l.setStyle("-fx-background-color: " + bgHex + "; -fx-background-radius: 999; -fx-border-radius: 999;");
        return l;
    }

    // ------------------------------------------------------------------ buttons

    public static Button nut(String text, Color bg, Color border, Color fg) {
        Button b = new Button(text);
        b.setFont(F_CHU_BOLD);
        b.setTextFill(fg);
        b.setPadding(new Insets(8, 16, 8, 16));
        b.setStyle("-fx-background-color: " + toWeb(bg) + "; -fx-border-color: " + toWeb(border)
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }

    public static Button nutChinh(String text) {
        return nut(text, MA_CHINH, MA_CHINH_DAM, Color.WHITE);
    }

    public static Button nutPhu(String text) {
        return nut(text, MA_THE, MA_VIEN, MA_CHU_DAM);
    }

    public static Button nutXoa(String text) {
        return nut(text, MA_LOI_NEN, MA_LOI, MA_LOI);
    }

    public static TextField oNhap() {
        TextField tf = new TextField();
        tf.setFont(F_CHU);
        tf.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        return tf;
    }

    // ------------------------------------------------------------------ header

    public static VBox dauTrang(String tieuDe, String moTa) {
        Label title = chuDe(tieuDe);
        Label desc = moTa(moTa);
        VBox v = new VBox(4, title, desc);
        return v;
    }

    // ------------------------------------------------------------------ dialogs

    public static void thongTin(Window parent, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setTitle("Thông báo");
        a.setHeaderText(null);
        if (parent != null) a.initOwner(parent);
        a.showAndWait();
    }

    public static void canhBao(Window parent, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message);
        a.setTitle("Cảnh báo");
        a.setHeaderText(null);
        if (parent != null) a.initOwner(parent);
        a.showAndWait();
    }

    public static void loi(Window parent, String message) {
        showError(parent, message);
    }

    public static void loi(Window parent, ApiException ex) {
        String msg = ex == null ? "Đã xảy ra lỗi không xác định." : ex.getMessage();
        if (ex != null && ex.details() != null && !ex.details().isEmpty()) {
            msg = msg + "\nChi tiết: " + String.join(", ", ex.details());
        }
        showError(parent, msg);
    }

    private static void showError(Window parent, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setTitle("Lỗi");
        a.setHeaderText(null);
        if (parent != null) a.initOwner(parent);
        a.showAndWait();
    }

    public static String toWeb(javafx.scene.paint.Paint paint) {
        if (paint instanceof Color c) {
            return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                    (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
        }
        return "#FFFFFF";
    }

    public static StackPane stack(Node node) {
        StackPane p = new StackPane(node);
        p.setPadding(new Insets(0));
        return p;
    }
}
