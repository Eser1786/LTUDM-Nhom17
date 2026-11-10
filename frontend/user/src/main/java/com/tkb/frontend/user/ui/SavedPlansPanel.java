package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Màn hình Thời khóa biểu đã lưu: card lịch, xem nhanh, mở, đổi tên, xóa và so sánh 2 lịch. */
public class SavedPlansPanel extends ScrollPane {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final TextField txtTimKiem = Functions.oNhap();
    private final VBox danhSach = new VBox(12);
    private final VBox khung = new VBox(14);
    private List<SavedPlan> tatCa = List.of();

    public SavedPlansPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        this.st = frame.trangThai();

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(14);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        VBox header = new VBox(4,
                Functions.chuDe("Thời khóa biểu đã lưu"),
                Functions.moTa("Mở lại lịch, đổi tên hoặc xóa lịch đã lưu."));

        VBox thanhCongCu = Functions.the(12);
        txtTimKiem.setPromptText("Tìm theo tên lịch...");
        txtTimKiem.setPrefWidth(260);
        Button btnMoi = Functions.nutPhu("Tạo phương án mới");
        btnMoi.setOnAction(e -> frame.denTrang(MainFrame.CARD_REQ));
        HBox row = new HBox(10, txtTimKiem, Functions.khoangTrong(), btnMoi);
        row.setAlignment(Pos.CENTER_LEFT);
        thanhCongCu.getChildren().add(row);

        khung.getChildren().addAll(header, thanhCongCu, danhSach);
        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
        txtTimKiem.textProperty().addListener((o, a, b) -> veDanhSach());
    }

    public void taiDanhSach() {
        try {
            String ma = st.hocKy == null ? null : st.hocKy.ma();
            tatCa = client.listPlans(ma);
        } catch (ApiException ex) {
            Functions.loi(frame.window(), ex);
            return;
        }
        veDanhSach();
    }

    private List<SavedPlan> loc() {
        String q = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase(Locale.ROOT);
        List<SavedPlan> out = new ArrayList<>();
        for (SavedPlan p : tatCa) {
            if (q.isEmpty() || p.ten().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(p);
            }
        }
        return out;
    }

    private void veDanhSach() {
        danhSach.getChildren().clear();
        List<SavedPlan> ds = loc();
        if (ds.isEmpty()) {
            VBox empty = Functions.the(24);
            empty.setAlignment(Pos.CENTER);
            empty.getChildren().addAll(
                    Functions.tieuDeThe(tatCa.isEmpty() ? "Chưa có lịch nào được lưu" : "Không tìm thấy lịch phù hợp"),
                    Functions.moTa(tatCa.isEmpty()
                            ? "Hãy tối ưu thời khóa biểu rồi lưu một phương án bạn thích."
                            : "Thử từ khóa khác hoặc xóa ô tìm kiếm."));
            Button moi = Functions.nutChinh("Tạo phương án mới");
            moi.setOnAction(e -> frame.denTrang(MainFrame.CARD_REQ));
            empty.getChildren().add(moi);
            danhSach.getChildren().add(empty);
            return;
        }
        for (SavedPlan p : ds) {
            danhSach.getChildren().add(theLich(p));
        }
    }

    private VBox theLich(SavedPlan p) {
        VBox card = Functions.the(16);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label ten = Functions.tieuDeThe(p.ten());
        top.getChildren().addAll(ten, Functions.khoangTrong(),
                Functions.nhanChinh(Math.round(p.diem()) + "/100"));

        String hocKy = st.hocKy == null ? p.maHocKy() : "Học kỳ I · 2026–2027";
        Label meta = Functions.nhanNho(hocKy + " · Lưu ngày " + p.ngayTao());

        FlowPane chiSo = new FlowPane(8, 8);
        chiSo.getChildren().addAll(Functions.nhanXam(p.stats().soNgayHoc() + " ngày học"));

        VBox xemNhanhBox = new VBox(6);
        Label t = Functions.nhanNho("Xem nhanh thời khóa biểu");
        xemNhanhBox.getChildren().add(t);
        TimetableGridPanel grid = null;
        try {
            PlanDetail d = client.getPlan(p.id());
            grid = new TimetableGridPanel();
            grid.setTieuDe(p.ten());
            grid.setSlots(d.slots());
            grid.setPrefHeight(280);
            xemNhanhBox.getChildren().add(grid);
        } catch (ApiException ex) {
            xemNhanhBox.getChildren().add(Functions.moTa("Không tải được chi tiết lịch."));
        }
        final TimetableGridPanel gridRef = grid;

        HBox actions = new HBox(8);
        Button doi = Functions.nutPhu("Đổi tên");
        Button xoa = Functions.nutXoa("Xóa");
        Button xuat = Functions.nutPhu("Xuất PNG");
        doi.setOnAction(e -> doiTen(p));
        xoa.setOnAction(e -> xoa(p));
        xuat.setOnAction(e -> Functions.xuatPng(gridRef, frame.window(), p.ten()));
        actions.getChildren().addAll(doi, xoa, xuat);

        card.getChildren().addAll(top, meta, chiSo, xemNhanhBox, actions);
        return card;
    }

    private void doiTen(SavedPlan p) {
        MoDals.doiTen(frame.window(), client, p.id(), p.ten(), this::taiDanhSach);
    }

    private void xoa(SavedPlan p) {
        MoDals.xacNhanXoa(frame.window(), client, p.id(), p.ten(), this::taiDanhSach);
    }

}
