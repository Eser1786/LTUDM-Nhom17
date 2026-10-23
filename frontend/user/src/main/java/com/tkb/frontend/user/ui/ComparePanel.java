package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.PlanDetail;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** MÃ n hÃ¬nh So sÃ¡nh thá»i khÃ³a biá»ƒu: hai (hoáº·c nhiá»u) phÆ°Æ¡ng Ã¡n cáº¡nh nhau theo tá»«ng hÃ ng tiÃªu chÃ­. */
public class ComparePanel extends ScrollPane {
    private final MainFrame frame;
    private final TkbClient client;
    private final VBox khung = new VBox(16);
    private final VBox noiDung = new VBox(16);

    public ComparePanel(MainFrame frame) {
        this.frame = frame;
        this.client = frame.trangThai().client;

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(16);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
    }

    public void taiDuLieu() {
        if (noiDung.getChildren().isEmpty()) {
            khung.getChildren().setAll(
                    Functions.chuDe("So sÃ¡nh thá»i khÃ³a biá»ƒu"),
                    Functions.moTa("Chá»n 2 lá»‹ch trong má»¥c â€œKáº¿t quáº£ Ä‘á» xuáº¥tâ€ hoáº·c â€œThá»i khÃ³a biá»ƒu Ä‘Ã£ lÆ°uâ€ "
                            + "Ä‘á»ƒ so sÃ¡nh chi tiáº¿t táº¡i Ä‘Ã¢y."),
                    theNutQuayLai());
        }
    }

    public void napPhuongAn(List<ScheduleOption> ds) {
        List<HoSo> hs = new ArrayList<>();
        for (ScheduleOption o : ds) {
            hs.add(new HoSo("PhÆ°Æ¡ng Ã¡n " + o.ten(), o.diem(), o.moTa(), o.stats(), o.slots(),
                    null, null));
        }
        veSoSanh(hs, false);
    }

    public void napDaLuu(List<Long> ids) {
        List<HoSo> hs = new ArrayList<>();
        for (Long id : ids) {
            try {
                PlanDetail d = client.getPlan(id);
                hs.add(new HoSo(d.ten(), d.diem(), "PhÆ°Æ¡ng Ã¡n Ä‘Ã£ lÆ°u " + d.ngayTao(), d.stats(),
                        d.slots(), id, d.maHocKy()));
            } catch (ApiException ex) {
                Functions.loi(frame.window(), ex);
            }
        }
        veSoSanh(hs, true);
    }

    private void veSoSanh(List<HoSo> hs, boolean tuDaLuu) {
        noiDung.getChildren().clear();
        if (hs.size() < 2) {
            khung.getChildren().setAll(Functions.chuDe("ChÆ°a Ä‘á»§ dá»¯ liá»‡u Ä‘á»ƒ so sÃ¡nh"),
                    Functions.moTa("Cáº§n chá»n Ã­t nháº¥t 2 phÆ°Æ¡ng Ã¡n."), theNutQuayLai());
            return;
        }

        VBox header = new VBox(4,
                Functions.chuDe("So sÃ¡nh thá»i khÃ³a biá»ƒu"),
                Functions.moTa("Äá»‘i chiáº¿u " + hs.size() + " phÆ°Æ¡ng Ã¡n theo cÃ¡c tiÃªu chÃ­. "
                        + "Chá»‰ sá»‘ khÃ¡c biá»‡t Ä‘Æ°á»£c Ä‘Ã¡nh dáº¥u Ä‘á»ƒ dá»… nháº­n biáº¿t."));

        VBox theCot = Functions.the(14);
        HBox cotHeader = new HBox(14);
        for (HoSo h : hs) {
            cotHeader.getChildren().add(theTomTat(h));
        }
        cotHeader.setAlignment(Pos.TOP_LEFT);
        theCot.getChildren().addAll(Functions.tieuDeThe("Tá»•ng quan phÆ°Æ¡ng Ã¡n"), cotHeader);

        VBox theHang = Functions.the(14);
        theHang.getChildren().add(Functions.tieuDeThe("So sÃ¡nh chi tiáº¿t"));
        theHang.getChildren().addAll(
                hang("Sá»‘ ngÃ y Ä‘áº¿n trÆ°á»ng", hs, h -> h.stats.soNgayHoc() + " ngÃ y",
                        h -> String.valueOf(h.stats.soNgayHoc())),
                hang("Khoáº£ng trá»‘ng trong tuáº§n", hs, h -> h.stats.phutTrong() + " phÃºt",
                        h -> String.valueOf(h.stats.phutTrong())),
                hang("ÄÃ¡p á»©ng Æ°u tiÃªn buá»•i sÃ¡ng", hs, h -> h.stats.tyLeSang() + "%",
                        h -> String.valueOf(h.stats.tyLeSang())),
                hang("ÄÃ¡p á»©ng â€œÃ­t ngÃ y Ä‘áº¿n trÆ°á»ngâ€", hs,
                        h -> h.stats.soNgayHoc() + " ngÃ y", h -> String.valueOf(h.stats.soNgayHoc())),
                hang("ÄÃ¡p á»©ng â€œkhoáº£ng trá»‘ng ngáº¯nâ€", hs,
                        h -> h.stats.phutTrong() + " phÃºt", h -> String.valueOf(h.stats.phutTrong())),
                hang("ÄÃ¡p á»©ng ngÃ y há»c yÃªu thÃ­ch", hs, this::ngayHoc, h -> ngayHoc(h)),
                hang("Tá»•ng tÃ­n chá»‰ Â· sá»‘ giá» há»c", hs,
                        h -> soTinChi(h) + " tÃ­n chá»‰ Â· " + h.stats.tongTiet() + " tiáº¿t",
                        h -> soTinChi(h) + "-" + h.stats.tongTiet()));

        VBox theBuoi = Functions.the(14);
        theBuoi.getChildren().add(Functions.tieuDeThe("Chi tiáº¿t cÃ¡c buá»•i há»c"));
        HBox cotBuoi = new HBox(14);
        for (HoSo h : hs) {
            cotBuoi.getChildren().add(theBuoiHoc(h));
        }
        theBuoi.getChildren().add(cotBuoi);

        HBox nut = new HBox(14);
        for (HoSo h : hs) {
            if (tuDaLuu) {
                Button xem = Functions.nutPhu("Má»Ÿ â€œ" + h.ten + "â€");
                xem.setOnAction(e -> MoDals.xemLich(frame.window(),
                        new PlanDetail(h.id, h.maHocKy, h.ten, h.diem, h.stats, h.slots, List.of(), "")));
                nut.getChildren().add(xem);
            } else {
                Button luu = Functions.nutChinh("LÆ°u â€œ" + h.ten + "â€");
                luu.setOnAction(e -> {
                    List<Integer> ids = new ArrayList<>();
                    h.slots.forEach(s -> ids.add(s.section().id()));
                    MoDals.luuLich(frame.window(), client,
                            frame.trangThai().hocKy == null ? "2026-2027-1" : frame.trangThai().hocKy.ma(),
                            h.ten, ids, () -> frame.denTrang(MainFrame.CARD_SAVED));
                });
                nut.getChildren().add(luu);
            }
        }
        nut.getChildren().addAll(Functions.khoangTrong(), theNutQuayLai());

        noiDung.getChildren().addAll(header, theCot, theHang, theBuoi, nut);
        khung.getChildren().setAll(noiDung);
    }

    private VBox theTomTat(HoSo h) {
        VBox v = Functions.the(14);
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setPrefWidth(360);
        HBox top = new HBox(8, Functions.tieuDeThe(h.ten), Functions.khoangTrong(),
                Functions.nhanChinh(Math.round(h.diem) + "/100"));
        top.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().addAll(top, Functions.moTa(h.moTa), Functions.vach(),
                Functions.nhanXam(h.stats.soNgayHoc() + " ngÃ y Â· " + h.stats.phutTrong() + " phÃºt trá»‘ng Â· "
                        + h.stats.tyLeSang() + "% sÃ¡ng"));
        return v;
    }

    private HBox hang(String nhan, List<HoSo> hs, java.util.function.Function<HoSo, String> fmt,
                      java.util.function.Function<HoSo, String> key) {
        HBox row = new HBox(14);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");
        Label l = Functions.nhanNho(nhan);
        l.setMinWidth(240);
        l.setPrefWidth(240);
        row.getChildren().add(l);

        Set<String> khac = new LinkedHashSet<>();
        for (HoSo h : hs) {
            khac.add(key.apply(h));
        }
        boolean coKhac = khac.size() > 1;

        for (HoSo h : hs) {
            Label v = new Label(fmt.apply(h));
            v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
            v.setMinWidth(200);
            v.setPrefWidth(320);
            HBox.setHgrow(v, Priority.ALWAYS);
            if (coKhac) {
                v.setTextFill(Functions.MA_CHINH_DAM);
                v.setText("â— " + fmt.apply(h));
            } else {
                v.setTextFill(Functions.MA_CHU_DAM);
            }
            row.getChildren().add(v);
        }
        return row;
    }

    private VBox theBuoiHoc(HoSo h) {
        VBox v = Functions.the(12);
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setPrefWidth(360);
        v.getChildren().add(Functions.tieuDeThe(h.ten));
        for (SectionSlot sk : h.slots) {
            for (ClassSession cs : sk.sessions()) {
                Label l = Functions.moTa("â€¢ " + sk.tenHocPhan() + " â€” Thá»© " + cs.thu() + ", tiáº¿t "
                        + cs.tietBd() + "â€“" + (cs.tietBd() + cs.soTiet() - 1) + ", " + maNhom(sk)
                        + ", " + cs.phong());
                v.getChildren().add(l);
            }
        }
        return v;
    }

    private String ngayHoc(HoSo h) {
        Set<Integer> ngay = new LinkedHashSet<>();
        for (SectionSlot s : h.slots) {
            for (ClassSession cs : s.sessions()) {
                ngay.add(cs.thu());
            }
        }
        List<String> p = new ArrayList<>();
        for (int t : ngay) {
            p.add("T" + t);
        }
        return String.join(", ", p);
    }

    private int soTinChi(HoSo h) {
        Set<String> mon = new LinkedHashSet<>();
        int tc = 0;
        for (SectionSlot s : h.slots) {
            if (mon.add(s.section().maMh())) {
                tc += s.soTc();
            }
        }
        return tc;
    }

    private String maNhom(SectionSlot s) {
        var x = s.section();
        return "TH".equals(x.loai()) ? "TH" + x.nhom() + "." + x.toTh() : "LT" + x.nhom();
    }

    private Button theNutQuayLai() {
        Button b = Functions.nutPhu("Quay láº¡i káº¿t quáº£");
        b.setOnAction(e -> frame.trangSoSanhQuayLai());
        return b;
    }

    /** Dá»¯ liá»‡u gá»™p cho má»—i phÆ°Æ¡ng Ã¡n trong báº£ng so sÃ¡nh. */
    private static final class HoSo {
        final String ten;
        final double diem;
        final String moTa;
        final OptionStats stats;
        final List<SectionSlot> slots;
        final Long id;
        final String maHocKy;
        final Criteria criteria = Criteria.macDinh();

        HoSo(String ten, double diem, String moTa, OptionStats stats, List<SectionSlot> slots,
             Long id, String maHocKy) {
            this.ten = ten;
            this.diem = diem;
            this.moTa = moTa;
            this.stats = stats;
            this.slots = slots;
            this.id = id;
            this.maHocKy = maHocKy;
        }
    }
}
