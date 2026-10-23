package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import java.util.function.Consumer;

/** CÃ¡c há»™p thoáº¡i dÃ¹ng chung: lÆ°u, Ä‘á»•i tÃªn, xÃ¡c nháº­n xÃ³a vÃ  xem nhanh thá»i khÃ³a biá»ƒu. */
public final class MoDals {

    private MoDals() {}

    /** Modal ná»n má», ná»™i dung táº­p trung; giá»¯ nguyÃªn dá»¯ liá»‡u khi Ä‘Ã³ng ngoÃ i Ã½ muá»‘n. */
    private static Stage mo(Window owner, String tieuDe, VBox noiDung) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        VBox hop = Functions.the(24);
        hop.setSpacing(14);
        hop.setPrefWidth(520);
        Label td = Functions.chuDe(tieuDe);
        hop.getChildren().addAll(td, Functions.vach(), noiDung);

        StackPane overlay = new StackPane(hop);
        overlay.setPadding(new Insets(40));
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.35);");
        overlay.setAlignment(Pos.CENTER);
        StackPane.setMargin(hop, new Insets(0));

        Scene scene = new Scene(overlay);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("data:text/css,.root{-fx-font-family:'Segoe UI';}");
        stage.setScene(scene);
        return stage;
    }

    /** Modal lÆ°u thá»i khÃ³a biá»ƒu: nháº­p tÃªn lá»‹ch, hiá»ƒn thá»‹ thÃ´ng tin phÆ°Æ¡ng Ã¡n, hai nÃºt há»§y/lÆ°u. */
    public static void luuLich(Window owner, TkbClient client, String maHocKy, String tenGoiY,
                               java.util.List<Integer> sectionIds, Runnable sauKhiLuu) {
        TextField txtTen = Functions.oNhap();
        txtTen.setText(tenGoiY);
        txtTen.setPromptText("VÃ­ dá»¥: Lá»‹ch há»c táº­p trung");
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanNho("TÃªn lá»‹ch"),
                txtTen,
                Functions.moTa("PhÆ°Æ¡ng Ã¡n sáº½ Ä‘Æ°á»£c lÆ°u vÃ o Thá»i khÃ³a biá»ƒu Ä‘Ã£ lÆ°u cá»§a há»c ká»³ hiá»‡n táº¡i."),
                Functions.nhanCanhBao("Dá»¯ liá»‡u mÃ´ phá»ng Â· chÆ°a gá»­i lÃªn mÃ¡y chá»§ tháº­t"));

        Stage stage = mo(owner, "LÆ°u thá»i khÃ³a biá»ƒu", nd);
        Label loi = Functions.moTa("");
        loi.setTextFill(Functions.MA_LOI);

        Button huy = Functions.nutPhu("Há»§y");
        Button luu = Functions.nutChinh("LÆ°u lá»‹ch");
        huy.setOnAction(e -> stage.close());
        luu.setOnAction(e -> {
            String ten = txtTen.getText() == null ? "" : txtTen.getText().trim();
            if (ten.isEmpty()) {
                loi.setText("Vui lÃ²ng nháº­p tÃªn lá»‹ch.");
                return;
            }
            try {
                client.savePlan(maHocKy, ten, sectionIds);
                stage.close();
                if (sauKhiLuu != null) sauKhiLuu.run();
            } catch (ApiException ex) {
                loi.setText(ex.getMessage());
            }
        });
        HBox nut = new HBox(10, Functions.khoangTrong(), huy, luu);
        nut.setAlignment(Pos.CENTER_RIGHT);
        nd.getChildren().addAll(loi, nut);
        stage.showAndWait();
    }

    /** Modal Ä‘á»•i tÃªn: hiá»ƒn thá»‹ tÃªn hiá»‡n táº¡i, nháº­p tÃªn má»›i, nÃºt lÆ°u. */
    public static void doiTen(Window owner, TkbClient client, long planId, String tenHienTai, Runnable sauKhiDoi) {
        TextField txtTen = Functions.oNhap();
        txtTen.setText(tenHienTai);
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanNho("TÃªn hiá»‡n táº¡i: " + tenHienTai),
                Functions.nhanNho("TÃªn má»›i"),
                txtTen);

        Stage stage = mo(owner, "Äá»•i tÃªn lá»‹ch", nd);
        Label loi = Functions.moTa("");
        loi.setTextFill(Functions.MA_LOI);

        Button huy = Functions.nutPhu("Há»§y");
        Button luu = Functions.nutChinh("LÆ°u tÃªn má»›i");
        huy.setOnAction(e -> stage.close());
        luu.setOnAction(e -> {
            String ten = txtTen.getText() == null ? "" : txtTen.getText().trim();
            if (ten.isEmpty()) {
                loi.setText("TÃªn lá»‹ch khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
                return;
            }
            try {
                client.renamePlan(planId, ten);
                stage.close();
                if (sauKhiDoi != null) sauKhiDoi.run();
            } catch (ApiException ex) {
                loi.setText(ex.getMessage());
            }
        });
        HBox nut = new HBox(10, Functions.khoangTrong(), huy, luu);
        nut.setAlignment(Pos.CENTER_RIGHT);
        nd.getChildren().addAll(loi, nut);
        stage.showAndWait();
    }

    /** Modal xÃ¡c nháº­n xÃ³a: cáº£nh bÃ¡o khÃ´ng thá»ƒ hoÃ n tÃ¡c, nÃºt giá»¯ láº¡i vÃ  xÃ³a lá»‹ch. */
    public static void xacNhanXoa(Window owner, TkbClient client, long planId, String ten,
                                  Runnable sauKhiXoa) {
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanLoi("Thao tÃ¡c khÃ´ng thá»ƒ hoÃ n tÃ¡c"),
                Functions.moTa("Báº¡n sáº¯p xÃ³a lá»‹ch â€œ" + ten + "â€. Lá»‹ch Ä‘Ã£ xÃ³a sáº½ khÃ´ng thá»ƒ khÃ´i phá»¥c."));
        Stage stage = mo(owner, "XÃ¡c nháº­n xÃ³a lá»‹ch", nd);
        Button giu = Functions.nutPhu("Giá»¯ láº¡i");
        Button xoa = Functions.nutXoa("XÃ³a lá»‹ch");
        giu.setOnAction(e -> stage.close());
        xoa.setOnAction(e -> {
            try {
                client.deletePlan(planId);
                stage.close();
                if (sauKhiXoa != null) sauKhiXoa.run();
            } catch (ApiException ex) {
                Functions.loi(owner, ex);
            }
        });
        HBox nut = new HBox(10, Functions.khoangTrong(), giu, xoa);
        nut.setAlignment(Pos.CENTER_RIGHT);
        nd.getChildren().add(nut);
        stage.showAndWait();
    }

    /** Modal xem nhanh má»™t phÆ°Æ¡ng Ã¡n Ä‘Ã£ lÆ°u dáº¡ng lÆ°á»›i thá»i khÃ³a biá»ƒu. */
    public static void xemLich(Window owner, PlanDetail d) {
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.moTa(d.maHocKy() + " Â· Äiá»ƒm " + String.format("%.0f/100", d.diem())
                        + " Â· " + d.stats().soNgayHoc() + " ngÃ y Â· " + d.stats().phutTrong() + " phÃºt trá»‘ng"),
                Functions.nhanCanhBao("Xem nhanh (mÃ´ phá»ng)"));
        TimetableGridPanel grid = new TimetableGridPanel();
        grid.setTieuDe(d.ten());
        grid.setSlots(d.slots());
        grid.setPrefHeight(440);
        nd.getChildren().add(grid);

        Stage stage = mo(owner, d.ten(), nd);
        Button dong = Functions.nutChinh("ÄÃ³ng");
        dong.setOnAction(e -> stage.close());
        HBox nut = new HBox(Functions.khoangTrong(), dong);
        nut.setAlignment(Pos.CENTER_RIGHT);
        nd.getChildren().add(nut);
        stage.showAndWait();
    }

    /** Tiá»‡n Ã­ch cho panel khÃ¡c náº¿u cáº§n má»Ÿ modal tÃ¹y biáº¿n. */
    public static Stage moTuyBien(Window owner, String tieuDe, VBox noiDung, Consumer<Stage> ganNut) {
        Stage stage = mo(owner, tieuDe, noiDung);
        if (ganNut != null) ganNut.accept(stage);
        return stage;
    }
}
