package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.SavedPlan;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.net.ApiException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

/** Màn hình Tổng quan: lời chào, 4 thẻ thống kê, thanh tiến trình 3 bước và hai khu vực thông tin. */
public class OverviewPanel extends VBox {
    private final MainFrame frame;
    private final HBox thongKe = new HBox(14);
    private final VBox hocPhanBox = new VBox(8);
    private final VBox lichGanDayBox = new VBox(10);
    private final Label lblChao = new Label();

    public OverviewPanel(MainFrame frame) {
        this.frame = frame;
        setPadding(new Insets(22, 26, 20, 26));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");

        lblChao.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        Label moTa = Functions.moTa("Đây là kế hoạch học tập tổng quan của bạn. "
                + "Hoàn thành lần lượt các bước để có một thời khóa biểu gọn gàng, ít khoảng trống.");
        VBox header = new VBox(4, lblChao, moTa);

        thongKe.setAlignment(Pos.CENTER_LEFT);
        thongKe.getChildren().setAll(Functions.the(14), Functions.the(14), Functions.the(14), Functions.the(14));

        VBox tienTrinh = theTienTrinh();

        VBox trai = Functions.the(16);
        HBox.setHgrow(trai, Priority.ALWAYS);
        trai.getChildren().addAll(Functions.tieuDeThe("Học phần trong kế hoạch"), hocPhanBox);

        VBox phai = Functions.the(16);
        phai.setPrefWidth(420);
        phai.setMinWidth(360);
        phai.getChildren().addAll(Functions.tieuDeThe("Lịch học gần đây"), lichGanDayBox);

        HBox haiCot = new HBox(16, trai, phai);

        VBox ghiChu = Functions.the(12);
        ghiChu.setStyle("-fx-background-color: #FFF7ED; -fx-background-radius: 12; -fx-border-color: #FED7AA; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        Label lg = Functions.nhanCanhBao("Lưu ý");
        Label lgTxt = Functions.moTa("Các lịch học đề xuất chỉ mang tính tham khảo cho việc sắp xếp cá nhân và "
                + "không thay thế đăng ký học phần chính thức. Vui lòng xác nhận với phòng Đào tạo.");
        ghiChu.getChildren().addAll(lg, lgTxt);

        getChildren().addAll(header, thongKe, tienTrinh, haiCot, ghiChu);
    }

    public void taiLai() {
        MainFrame.AppUiState st = frame.trangThai();
        String ten = st.sinhVien == null ? "bạn" : tenNgan(st.sinhVien.hoTen());
        lblChao.setText("Chào " + ten + ", sẵn sàng cho học kỳ mới?");

        List<SectionSlot> tatCa = frame.trangThai().tatCaNhom;
        int soHocPhan = st.monDaChon().size();
        int soTinChi = 0;
        for (SectionSlot s : tatCa) {
            if (st.daChon.contains(s.section().id()) && st.monDaChon().contains(s.section().maMh())) {
                // Đếm tín chỉ mỗi học phần một lần.
            }
        }
        for (String ma : st.monDaChon()) {
            for (Course c : st.courses) {
                if (c.maMh().equals(ma)) {
                    soTinChi += c.soTc();
                    break;
                }
            }
        }
        int soPhuongAn = st.ketQua == null ? 3 : st.ketQua.options().size();
        int soDaLuu = 0;
        try {
            soDaLuu = frame.trangThai().hocKy == null ? 0
                    : frame.trangThai().client.listPlans(frame.trangThai().hocKy.ma()).size();
        } catch (ApiException ignored) {
        }

        thongKe.getChildren().setAll(
                theThongKe("Học phần đã chọn", soHocPhan + "", "học phần", Functions.MA_CHINH_NEN, Functions.MA_CHINH_DAM),
                theThongKe("Khối lượng học tập", soTinChi + "", "tín chỉ", Functions.MA_CHINH_NEN, Functions.MA_CHINH_DAM),
                theThongKe("Phương án mô phỏng", soPhuongAn + "", "lịch hợp lệ", Functions.MA_XANH_NEN, Functions.MA_XANH_DAM),
                theThongKe("Thời khóa biểu đã lưu", soDaLuu + "", "lịch", Functions.MA_XAM_NEN, Functions.MA_CHU_DAM));

        veHocPhan();
        veLichGanDay();
    }

    private VBox theThongKe(String tieuDe, String so, String donVi,
                            javafx.scene.paint.Color nen, javafx.scene.paint.Color mauSo) {
        VBox v = Functions.the(16);
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setPrefWidth(240);
        Label lTd = Functions.nhanNho(tieuDe);
        Label lSo = new Label(so);
        lSo.setFont(Functions.F_SO_LON);
        lSo.setTextFill(mauSo);
        HBox row = new HBox(6, lSo, new Label(donVi));
        row.setAlignment(Pos.BASELINE_LEFT);
        Label lblDv = (Label) row.getChildren().get(1);
        lblDv.setTextFill(Functions.MA_CHU_PHU);
        lblDv.setFont(Font.font("Segoe UI", 12));
        javafx.scene.layout.Region accent = new javafx.scene.layout.Region();
        accent.setMinHeight(5);
        accent.setStyle("-fx-background-color: " + Functions.toWeb(nen) + "; -fx-background-radius: 4;");
        v.getChildren().addAll(accent, lTd, row);
        return v;
    }

    private VBox theTienTrinh() {
        VBox v = Functions.the(16);
        v.getChildren().add(Functions.tieuDeThe("Tiến trình chuẩn bị"));
        GridPane g = new GridPane();
        g.setHgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        String[] nhan = {"1", "2", "3"};
        String[] td = {"Chọn nhóm lớp", "Thiết lập ưu tiên", "Xem và lưu kết quả"};
        String[] mt = {"Ghép nhóm lý thuyết với thực hành tương ứng.",
                "Điều chỉnh mức ưu tiên cho từng tiêu chí.",
                "Chọn phương án phù hợp và lưu lại."};
        MainFrame.AppUiState st = frame.trangThai();
        boolean buoc1 = !st.daChon.isEmpty();
        boolean buoc2 = buoc1;
        boolean buoc3 = st.ketQua != null;
        boolean[] xong = {buoc1, buoc2, buoc3};
        for (int i = 0; i < 3; i++) {
            VBox step = Functions.the(12);
            if (xong[i]) {
                step.setStyle("-fx-background-color: #F0FDF4; -fx-background-radius: 12; -fx-border-color: #16A34A; "
                        + "-fx-border-radius: 12; -fx-border-width: 1;");
            }
            Label badge = xong[i] ? Functions.nhanHopLe("Đã xong") : Functions.nhanXam("Bước " + nhan[i]);
            Label t = Functions.tieuDeThe(td[i]);
            Label m = Functions.moTa(mt[i]);
            step.getChildren().addAll(badge, t, m);
            g.add(step, i, 0);
        }
        v.getChildren().add(g);
        return v;
    }

    private void veHocPhan() {
        hocPhanBox.getChildren().clear();
        MainFrame.AppUiState st = frame.trangThai();
        List<String> mon = st.monDaChon();
        if (mon.isEmpty()) {
            hocPhanBox.getChildren().add(Functions.moTa("Chưa có học phần nào được chọn. "
                    + "Hãy vào mục “Nhóm lớp học phần” để bắt đầu."));
            return;
        }
        for (String ma : mon) {
            String ten = ma;
            int tc = 0;
            for (Course c : st.courses) {
                if (c.maMh().equals(ma)) {
                    ten = c.ten();
                    tc = c.soTc();
                    break;
                }
            }
            int soNhomDuDieuKien = 0;
            for (SectionSlot s : st.tatCaNhom) {
                if (s.section().maMh().equals(ma)) {
                    soNhomDuDieuKien++;
                }
            }
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10;");
            Label maLbl = new Label(ma);
            maLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            maLbl.setTextFill(Functions.MA_CHINH_DAM);
            maLbl.setMinWidth(78);
            VBox info = new VBox(2, new Label(ten), Functions.nhanNho(tc + " tín chỉ"));
            Label nhom = Functions.nhanXam(soNhomDuDieuKien + " nhóm đủ điều kiện");
            row.getChildren().addAll(maLbl, info, Functions.khoangTrong(), nhom);
            hocPhanBox.getChildren().add(row);
        }
    }

    private void veLichGanDay() {
        lichGanDayBox.getChildren().clear();
        List<SavedPlan> plans;
        try {
            plans = frame.trangThai().client.listPlans(frame.trangThai().hocKy == null
                    ? null : frame.trangThai().hocKy.ma());
        } catch (ApiException ex) {
            lichGanDayBox.getChildren().add(Functions.moTa("Không tải được danh sách lịch đã lưu."));
            return;
        }
        if (plans.isEmpty()) {
            lichGanDayBox.getChildren().add(Functions.moTa("Bạn chưa lưu lịch nào."));
            return;
        }
        plans.stream().limit(2).forEach(p -> lichGanDayBox.getChildren().add(theLich(p)));
        Button moTatCa = Functions.nutPhu("Mở danh sách đã lưu");
        moTatCa.setOnAction(e -> frame.denTrang(MainFrame.CARD_SAVED));
        lichGanDayBox.getChildren().add(moTatCa);
    }

    private VBox theLich(SavedPlan p) {
        VBox card = Functions.the(12);
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label ten = Functions.tieuDeThe(p.ten());
        Label diem = Functions.nhanDeXuat(String.format("%.0f/100", p.diem()));
        top.getChildren().addAll(ten, Functions.khoangTrong(), diem);

        FlowPane chiSo = new FlowPane(8, 6);
        chiSo.getChildren().addAll(
                Functions.nhanXam(p.stats().soNgayHoc() + " ngày đến trường"),
                Functions.nhanXam(p.stats().phutTrong() + " phút trống"),
                Functions.nhanXam(p.soBuoi() + " buổi/tuần"));

        Button mo = Functions.nutPhu("Mở lịch");
        mo.setOnAction(e -> {
            try {
                PlanDetail d = frame.trangThai().client.getPlan(p.id());
                MoDals.xemLich(frame.window(), d);
            } catch (ApiException ex) {
                Functions.loi(frame.window(), ex);
            }
        });
        card.getChildren().addAll(top, chiSo, mo);
        return card;
    }

    private String tenNgan(String hoTen) {
        String[] tu = hoTen.trim().split("\\s+");
        return tu.length == 0 ? hoTen : tu[tu.length - 1];
    }
}
