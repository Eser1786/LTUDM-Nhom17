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

/** MÃ n hÃ¬nh Tá»•ng quan: lá»i chÃ o, 4 tháº» thá»‘ng kÃª, thanh tiáº¿n trÃ¬nh 3 bÆ°á»›c vÃ  hai khu vá»±c thÃ´ng tin. */
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
        Label moTa = Functions.moTa("ÄÃ¢y lÃ  káº¿ hoáº¡ch há»c táº­p tá»•ng quan cá»§a báº¡n. "
                + "HoÃ n thÃ nh láº§n lÆ°á»£t cÃ¡c bÆ°á»›c Ä‘á»ƒ cÃ³ má»™t thá»i khÃ³a biá»ƒu gá»n gÃ ng, Ã­t khoáº£ng trá»‘ng.");
        VBox header = new VBox(4, lblChao, moTa);

        thongKe.setAlignment(Pos.CENTER_LEFT);
        thongKe.getChildren().setAll(Functions.the(14), Functions.the(14), Functions.the(14), Functions.the(14));

        VBox tienTrinh = theTienTrinh();

        VBox trai = Functions.the(16);
        HBox.setHgrow(trai, Priority.ALWAYS);
        trai.getChildren().addAll(Functions.tieuDeThe("Há»c pháº§n trong káº¿ hoáº¡ch"), hocPhanBox);

        VBox phai = Functions.the(16);
        phai.setPrefWidth(420);
        phai.setMinWidth(360);
        phai.getChildren().addAll(Functions.tieuDeThe("Lá»‹ch há»c gáº§n Ä‘Ã¢y"), lichGanDayBox);

        HBox haiCot = new HBox(16, trai, phai);

        VBox ghiChu = Functions.the(12);
        ghiChu.setStyle("-fx-background-color: #FFF7ED; -fx-background-radius: 12; -fx-border-color: #FED7AA; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        Label lg = Functions.nhanCanhBao("LÆ°u Ã½");
        Label lgTxt = Functions.moTa("CÃ¡c lá»‹ch há»c Ä‘á» xuáº¥t chá»‰ mang tÃ­nh tham kháº£o cho viá»‡c sáº¯p xáº¿p cÃ¡ nhÃ¢n vÃ  "
                + "khÃ´ng thay tháº¿ Ä‘Äƒng kÃ½ há»c pháº§n chÃ­nh thá»©c. Vui lÃ²ng xÃ¡c nháº­n vá»›i phÃ²ng ÄÃ o táº¡o.");
        ghiChu.getChildren().addAll(lg, lgTxt);

        getChildren().addAll(header, thongKe, tienTrinh, haiCot, ghiChu);
    }

    public void taiLai() {
        MainFrame.AppUiState st = frame.trangThai();
        String ten = st.sinhVien == null ? "báº¡n" : tenNgan(st.sinhVien.hoTen());
        lblChao.setText("ChÃ o " + ten + ", sáºµn sÃ ng cho há»c ká»³ má»›i?");

        List<SectionSlot> tatCa = frame.trangThai().tatCaNhom;
        int soHocPhan = st.monDaChon().size();
        int soTinChi = 0;
        for (SectionSlot s : tatCa) {
            if (st.daChon.contains(s.section().id()) && st.monDaChon().contains(s.section().maMh())) {
                // Äáº¿m tÃ­n chá»‰ má»—i há»c pháº§n má»™t láº§n.
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
                theThongKe("Há»c pháº§n Ä‘Ã£ chá»n", soHocPhan + "", "há»c pháº§n", Functions.MA_CHINH_NEN, Functions.MA_CHINH_DAM),
                theThongKe("Khá»‘i lÆ°á»£ng há»c táº­p", soTinChi + "", "tÃ­n chá»‰", Functions.MA_CHINH_NEN, Functions.MA_CHINH_DAM),
                theThongKe("PhÆ°Æ¡ng Ã¡n mÃ´ phá»ng", soPhuongAn + "", "lá»‹ch há»£p lá»‡", Functions.MA_XANH_NEN, Functions.MA_XANH_DAM),
                theThongKe("Thá»i khÃ³a biá»ƒu Ä‘Ã£ lÆ°u", soDaLuu + "", "lá»‹ch", Functions.MA_XAM_NEN, Functions.MA_CHU_DAM));

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
        v.getChildren().add(Functions.tieuDeThe("Tiáº¿n trÃ¬nh chuáº©n bá»‹"));
        GridPane g = new GridPane();
        g.setHgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        String[] nhan = {"1", "2", "3"};
        String[] td = {"Chá»n nhÃ³m lá»›p", "Thiáº¿t láº­p Æ°u tiÃªn", "Xem vÃ  lÆ°u káº¿t quáº£"};
        String[] mt = {"GhÃ©p nhÃ³m lÃ½ thuyáº¿t vá»›i thá»±c hÃ nh tÆ°Æ¡ng á»©ng.",
                "Äiá»u chá»‰nh má»©c Æ°u tiÃªn cho tá»«ng tiÃªu chÃ­.",
                "Chá»n phÆ°Æ¡ng Ã¡n phÃ¹ há»£p vÃ  lÆ°u láº¡i."};
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
            Label badge = xong[i] ? Functions.nhanHopLe("ÄÃ£ xong") : Functions.nhanXam("BÆ°á»›c " + nhan[i]);
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
            hocPhanBox.getChildren().add(Functions.moTa("ChÆ°a cÃ³ há»c pháº§n nÃ o Ä‘Æ°á»£c chá»n. "
                    + "HÃ£y vÃ o má»¥c â€œNhÃ³m lá»›p há»c pháº§nâ€ Ä‘á»ƒ báº¯t Ä‘áº§u."));
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
            VBox info = new VBox(2, new Label(ten), Functions.nhanNho(tc + " tÃ­n chá»‰"));
            Label nhom = Functions.nhanXam(soNhomDuDieuKien + " nhÃ³m Ä‘á»§ Ä‘iá»u kiá»‡n");
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
            lichGanDayBox.getChildren().add(Functions.moTa("KhÃ´ng táº£i Ä‘Æ°á»£c danh sÃ¡ch lá»‹ch Ä‘Ã£ lÆ°u."));
            return;
        }
        if (plans.isEmpty()) {
            lichGanDayBox.getChildren().add(Functions.moTa("Báº¡n chÆ°a lÆ°u lá»‹ch nÃ o."));
            return;
        }
        plans.stream().limit(2).forEach(p -> lichGanDayBox.getChildren().add(theLich(p)));
        Button moTatCa = Functions.nutPhu("Má»Ÿ danh sÃ¡ch Ä‘Ã£ lÆ°u");
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
                Functions.nhanXam(p.stats().soNgayHoc() + " ngÃ y Ä‘áº¿n trÆ°á»ng"),
                Functions.nhanXam(p.stats().phutTrong() + " phÃºt trá»‘ng"),
                Functions.nhanXam(p.soBuoi() + " buá»•i/tuáº§n"));

        Button mo = Functions.nutPhu("Má»Ÿ lá»‹ch");
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
