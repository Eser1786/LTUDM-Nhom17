package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.PlanDetail;
import com.tkb.frontend.net.ApiException;
import com.tkb.frontend.net.TkbClient;
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

/** Các hộp thoại dùng chung: lưu, đổi tên, xác nhận xóa và xem nhanh thời khóa biểu. */
public final class MoDals {

    private MoDals() {}

    /** Modal nền mờ, nội dung tập trung; giữ nguyên dữ liệu khi đóng ngoài ý muốn. */
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

    /** Modal lưu thời khóa biểu: nhập tên lịch, hiển thị thông tin phương án, hai nút hủy/lưu. */
    public static void luuLich(Window owner, TkbClient client, String maHocKy, String tenGoiY,
                               java.util.List<Integer> sectionIds, Runnable sauKhiLuu) {
        TextField txtTen = Functions.oNhap();
        txtTen.setText(tenGoiY);
        txtTen.setPromptText("Ví dụ: Lịch học tập trung");
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanNho("Tên lịch"),
                txtTen,
                Functions.moTa("Phương án sẽ được lưu vào Thời khóa biểu đã lưu của học kỳ hiện tại."),
                Functions.nhanCanhBao("Dữ liệu mô phỏng · chưa gửi lên máy chủ thật"));

        Stage stage = mo(owner, "Lưu thời khóa biểu", nd);
        Label loi = Functions.moTa("");
        loi.setTextFill(Functions.MA_LOI);

        Button huy = Functions.nutPhu("Hủy");
        Button luu = Functions.nutChinh("Lưu lịch");
        huy.setOnAction(e -> stage.close());
        luu.setOnAction(e -> {
            String ten = txtTen.getText() == null ? "" : txtTen.getText().trim();
            if (ten.isEmpty()) {
                loi.setText("Vui lòng nhập tên lịch.");
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

    /** Modal đổi tên: hiển thị tên hiện tại, nhập tên mới, nút lưu. */
    public static void doiTen(Window owner, TkbClient client, long planId, String tenHienTai, Runnable sauKhiDoi) {
        TextField txtTen = Functions.oNhap();
        txtTen.setText(tenHienTai);
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanNho("Tên hiện tại: " + tenHienTai),
                Functions.nhanNho("Tên mới"),
                txtTen);

        Stage stage = mo(owner, "Đổi tên lịch", nd);
        Label loi = Functions.moTa("");
        loi.setTextFill(Functions.MA_LOI);

        Button huy = Functions.nutPhu("Hủy");
        Button luu = Functions.nutChinh("Lưu tên mới");
        huy.setOnAction(e -> stage.close());
        luu.setOnAction(e -> {
            String ten = txtTen.getText() == null ? "" : txtTen.getText().trim();
            if (ten.isEmpty()) {
                loi.setText("Tên lịch không được để trống.");
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

    /** Modal xác nhận xóa: cảnh báo không thể hoàn tác, nút giữ lại và xóa lịch. */
    public static void xacNhanXoa(Window owner, TkbClient client, long planId, String ten,
                                  Runnable sauKhiXoa) {
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.nhanLoi("Thao tác không thể hoàn tác"),
                Functions.moTa("Bạn sắp xóa lịch “" + ten + "”. Lịch đã xóa sẽ không thể khôi phục."));
        Stage stage = mo(owner, "Xác nhận xóa lịch", nd);
        Button giu = Functions.nutPhu("Giữ lại");
        Button xoa = Functions.nutXoa("Xóa lịch");
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

    /** Modal xem nhanh một phương án đã lưu dạng lưới thời khóa biểu. */
    public static void xemLich(Window owner, PlanDetail d) {
        VBox nd = new VBox(10);
        nd.getChildren().addAll(
                Functions.moTa(d.maHocKy() + " · Điểm " + String.format("%.0f/100", d.diem())
                        + " · " + d.stats().soNgayHoc() + " ngày · " + d.stats().phutTrong() + " phút trống"),
                Functions.nhanCanhBao("Xem nhanh (mô phỏng)"));
        TimetableGridPanel grid = new TimetableGridPanel();
        grid.setTieuDe(d.ten());
        grid.setSlots(d.slots());
        grid.setPrefHeight(440);
        nd.getChildren().add(grid);

        Stage stage = mo(owner, d.ten(), nd);
        Button dong = Functions.nutChinh("Đóng");
        dong.setOnAction(e -> stage.close());
        HBox nut = new HBox(Functions.khoangTrong(), dong);
        nut.setAlignment(Pos.CENTER_RIGHT);
        nd.getChildren().add(nut);
        stage.showAndWait();
    }

    /** Tiện ích cho panel khác nếu cần mở modal tùy biến. */
    public static Stage moTuyBien(Window owner, String tieuDe, VBox noiDung, Consumer<Stage> ganNut) {
        Stage stage = mo(owner, tieuDe, noiDung);
        if (ganNut != null) ganNut.accept(stage);
        return stage;
    }
}
