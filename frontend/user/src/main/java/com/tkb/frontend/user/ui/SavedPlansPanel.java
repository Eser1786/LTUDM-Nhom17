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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Màn hình Thời khóa biểu đã lưu: card lịch, xem nhanh, mở, đổi tên, xóa và so sánh 2 lịch. */
public class SavedPlansPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final TextField txtTimKiem = Functions.oNhap();
    private final VBox danhSach = new VBox(12);
    private final VBox khung = new VBox(14);
    private final VBox pinnedBar = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane();
    private SavedPlan pinnedPlan;
    private TimetableGridPanel pinnedGrid;
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

        pinnedBar.setVisible(false);
        pinnedBar.setManaged(false);
        pinnedBar.setPadding(new Insets(10, 26, 6, 26));
        pinnedBar.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-width: 0 0 1 0;");

        scrollPane.setContent(khung);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F6F8FB;");

        setSpacing(0);
        setStyle("-fx-background-color: #F6F8FB;");
        getChildren().addAll(pinnedBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
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

        // Hiển thị thẻ lịch đã lưu theo cùng phong cách với thẻ kết quả đề xuất:
        // tiêu đề + điểm, lưới thời khóa biểu lớn, ghi chú xếp đủ/không xếp được.
        HBox tieuDe = new HBox(10,
                Functions.tieuDeThe("Thời khóa biểu " + p.ten()),
                Functions.nhanChinh("Điểm " + Math.round(p.diem()) + "/100"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);

        String hocKy = st.hocKy == null ? p.maHocKy() : "Học kỳ I · 2026–2027";
        Label meta = Functions.nhanNho(hocKy + " · Lưu ngày " + p.ngayTao());

        FlowPane chiSo = new FlowPane(8, 8);
        chiSo.getChildren().addAll(
                Functions.nhanXam(p.stats().soNgayHoc() + " ngày học"),
                Functions.nhanXam(p.soTinChi() + " tín chỉ"),
                Functions.nhanXam(p.soHocPhan() + " học phần"));

        TimetableGridPanel grid = null;
        List<String> biLoai = List.of();
        try {
            PlanDetail d = client.getPlan(p.id());
            grid = new TimetableGridPanel();
            grid.setTieuDe(p.ten());
            grid.setSlots(d.slots());
            grid.setPrefHeight(560);
            biLoai = d.biLoai() == null ? List.of() : d.biLoai();
        } catch (ApiException ex) {
            // keep grid null; báo lỗi bên dưới
        }
        final TimetableGridPanel gridRef = grid;

        card.getChildren().addAll(tieuDe, meta, chiSo);
        if (grid != null) {
            card.getChildren().add(grid);
            if (!biLoai.isEmpty()) {
                card.getChildren().add(Functions.nhanCanhBao("Không xếp được học phần: " + String.join(", ", biLoai)));
            } else {
                card.getChildren().add(Functions.nhanHopLe("Đã xếp đủ tất cả học phần đã chọn · Không trùng giờ"));
            }
        } else {
            card.getChildren().add(Functions.moTa("Không tải được chi tiết lịch."));
        }

        HBox actions = new HBox(8);
        Button doi = Functions.nutPhu("Đổi tên");
        Button xoa = Functions.nutXoa("Xóa");
        Button xuat = Functions.nutPhu("Xuất PNG");
        Button ghim = Functions.nutPhu("📌 Ghim");
        doi.setOnAction(e -> doiTen(p));
        xoa.setOnAction(e -> xoa(p));
        xuat.setOnAction(e -> {
            if (gridRef != null) Functions.xuatPng(gridRef, frame.window(), p.ten());
        });
        ghim.setOnAction(e -> ghimPlan(p, gridRef));
        actions.getChildren().addAll(doi, xoa, ghim, xuat);

        card.getChildren().add(actions);
        return card;
    }

    private void ghimPlan(SavedPlan p, TimetableGridPanel gridRef) {
        if (pinnedPlan != null && pinnedPlan.id() == p.id()) {
            pinnedPlan = null;
            pinnedGrid = null;
            pinnedBar.getChildren().clear();
            pinnedBar.setVisible(false);
            pinnedBar.setManaged(false);
            return;
        }
        try {
            PlanDetail d = client.getPlan(p.id());
            pinnedPlan = p;
            pinnedGrid = new TimetableGridPanel();
            pinnedGrid.setTieuDe(p.ten());
            pinnedGrid.setSlots(d.slots());
            pinnedGrid.setMinHeight(300);
            pinnedGrid.setPrefHeight(360);
            pinnedGrid.setMaxHeight(420);

            pinnedBar.getChildren().clear();
            HBox tieuDe = new HBox(10,
                    Functions.tieuDeThe("📌 Đang ghim: " + p.ten()),
                    Functions.nhanChinh(Math.round(p.diem()) + "/100"));
            tieuDe.setAlignment(Pos.CENTER_LEFT);
            Button boGhim = Functions.nutPhu("Bỏ ghim");
            boGhim.setOnAction(e -> ghimPlan(p, gridRef));
            HBox nut = new HBox(10, Functions.khoangTrong(), boGhim);
            nut.setAlignment(Pos.CENTER_RIGHT);
            pinnedBar.getChildren().addAll(tieuDe, pinnedGrid, nut);
            pinnedBar.setVisible(true);
            pinnedBar.setManaged(true);
        } catch (ApiException ex) {
            Functions.loi(frame.window(), ex);
        }
    }

    private void doiTen(SavedPlan p) {
        MoDals.doiTen(frame.window(), client, p.id(), p.ten(), this::taiDanhSach);
    }

    private void xoa(SavedPlan p) {
        MoDals.xacNhanXoa(frame.window(), client, p.id(), p.ten(), this::taiDanhSach);
    }

}
