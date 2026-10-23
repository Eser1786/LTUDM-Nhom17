package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MÃ n hÃ¬nh Thiáº¿t láº­p Æ°u tiÃªn: hai cá»™t.
 * TrÃ¡i: nhÃ³m lá»›p Ä‘Æ°á»£c phÃ©p chá»n (card há»c pháº§n vá»›i tá»• há»£p LT+TH há»£p lá»‡).
 * Pháº£i: cÃ¡c tiÃªu chÃ­ Æ°u tiÃªn dáº¡ng thanh trÆ°á»£t 0..5 vÃ  ngÃ y há»c yÃªu thÃ­ch T2..T7.
 */
public class PriorityPanel extends ScrollPane {
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final VBox cotTrai = new VBox(12);
    private final VBox cotPhai = new VBox(12);
    private final Slider sSang = new Slider(0, 5, 3);
    private final Slider sItNgay = new Slider(0, 5, 3);
    private final Slider sKhoangTrong = new Slider(0, 5, 3);
    private final Slider sNgayYeuThich = new Slider(0, 5, 0);
    private final Map<Integer, CheckBox> ngayChk = new LinkedHashMap<>();
    private final Label lblSang = Functions.nhanChinh("3");
    private final Label lblItNgay = Functions.nhanChinh("3");
    private final Label lblKhoang = Functions.nhanChinh("3");
    private final Label lblNgayYeu = Functions.nhanChinh("0");
    private final VBox khung = new VBox(14);

    public PriorityPanel(MainFrame frame) {
        this.frame = frame;
        this.st = frame.trangThai();

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(14);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        VBox header = new VBox(4,
                Functions.chuDe("Thiáº¿t láº­p Æ°u tiÃªn"),
                Functions.moTa("Chá»n cÃ¡c tá»• há»£p nhÃ³m lá»›p Ä‘Æ°á»£c phÃ©p vÃ  Ä‘iá»u chá»‰nh trá»ng sá»‘. "
                        + "Trá»ng sá»‘ chá»‰ dÃ¹ng Ä‘á»ƒ xáº¿p háº¡ng cÃ¡c lá»‹ch; má»i phÆ°Æ¡ng Ã¡n há»£p lá»‡ Ä‘á»u khÃ´ng trÃ¹ng giá» "
                        + "vÃ  tuÃ¢n thá»§ Ä‘iá»u kiá»‡n ghÃ©p nhÃ³m LTâ€“TH."));

        HBox haiCot = new HBox(16);
        khung.getChildren().addAll(header, haiCot);

        VBox traiCard = Functions.the(16);
        HBox.setHgrow(traiCard, Priority.ALWAYS);
        traiCard.getChildren().addAll(
                Functions.tieuDeThe("NhÃ³m lá»›p Ä‘Æ°á»£c phÃ©p chá»n"),
                Functions.moTa("Má»—i há»c pháº§n lÃ  má»™t card. ÄÃ¡nh dáº¥u vÃ o tá»• há»£p báº¡n muá»‘n Ä‘Æ°a vÃ o xáº¿p lá»‹ch."),
                cotTrai);

        VBox phaiCard = Functions.the(16);
        phaiCard.setPrefWidth(420);
        phaiCard.setMinWidth(360);
        cotPhai.setFillWidth(true);
        phaiCard.getChildren().add(cotPhai);

        haiCot.getChildren().addAll(traiCard, phaiCard);

        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
    }

    public void taiLai() {
        veCotTrai();
        veCotPhai();
    }

    // ------------------------------------------------------------- cá»™t trÃ¡i

    private void veCotTrai() {
        cotTrai.getChildren().clear();
        if (st.tatCaNhom.isEmpty()) {
            cotTrai.getChildren().add(Functions.moTa("ChÆ°a cÃ³ dá»¯ liá»‡u nhÃ³m lá»›p. "
                    + "HÃ£y vÃ o má»¥c â€œNhÃ³m lá»›p há»c pháº§nâ€ Ä‘á»ƒ táº£i dá»¯ liá»‡u."));
            return;
        }
        Map<String, List<SectionSlot>> theoMon = new LinkedHashMap<>();
        for (SectionSlot s : st.tatCaNhom) {
            theoMon.computeIfAbsent(s.section().maMh(), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<SectionSlot>> e : theoMon.entrySet()) {
            cotTrai.getChildren().add(theHocPhan(e.getKey(), e.getValue()));
        }
    }

    private VBox theHocPhan(String maMh, List<SectionSlot> nhom) {
        VBox card = Functions.the(14);
        String ten = maMh;
        int tc = 0;
        for (Course c : st.courses) {
            if (c.maMh().equals(maMh)) {
                ten = c.ten();
                tc = c.soTc();
                break;
            }
        }
        Label td = Functions.tieuDeThe(ten);
        Label ma = Functions.nhanNho(maMh + " Â· " + tc + " tÃ­n chá»‰");
        HBox top = new HBox(10, td, Functions.khoangTrong(), ma);
        top.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(top, Functions.vach());

        boolean coTh = nhom.stream().anyMatch(s -> "TH".equals(s.section().loai()));
        List<List<SectionSlot>> toHop = toHop(nhom, coTh);

        card.getChildren().add(Functions.nhanXam(toHop.size() + " tá»• há»£p Ä‘á»§ Ä‘iá»u kiá»‡n"));
        for (List<SectionSlot> th : toHop) {
            card.getChildren().add(theToHop(th));
        }
        return card;
    }

    /** Tá»• há»£p há»£p lá»‡: náº¿u cÃ³ TH thÃ¬ ghÃ©p LT+TH cÃ¹ng nhÃ³m; khÃ´ng thÃ¬ tá»«ng LT riÃªng. */
    private List<List<SectionSlot>> toHop(List<SectionSlot> nhom, boolean coTh) {
        List<List<SectionSlot>> out = new ArrayList<>();
        if (!coTh) {
            for (SectionSlot s : nhom) {
                if ("LT".equals(s.section().loai())) {
                    out.add(List.of(s));
                }
            }
            return out;
        }
        List<SectionSlot> lts = new ArrayList<>();
        List<SectionSlot> ths = new ArrayList<>();
        for (SectionSlot s : nhom) {
            if ("TH".equals(s.section().loai())) ths.add(s);
            else lts.add(s);
        }
        for (SectionSlot lt : lts) {
            List<SectionSlot> ghep = new ArrayList<>();
            ghep.add(lt);
            for (SectionSlot th : ths) {
                if (th.section().nhom().equals(lt.section().nhom())) {
                    ghep.add(th);
                }
            }
            out.add(ghep);
        }
        return out;
    }

    private VBox theToHop(List<SectionSlot> th) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (SectionSlot s : th) {
            ids.add(s.section().id());
        }
        boolean daChon = !ids.isEmpty() && st.daChon.containsAll(ids);

        VBox card = Functions.the(10);
        String bg = daChon ? "#F0FDF4" : "#F8FAFC";
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; -fx-border-color: "
                + (daChon ? "#16A34A" : "#E2E8F0") + "; -fx-border-radius: 10; -fx-border-width: 1;");

        CheckBox chk = new CheckBox(nhanToHop(th));
        chk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        chk.setSelected(daChon);
        chk.setOnAction(e -> {
            if (chk.isSelected()) {
                st.daChon.addAll(ids);
            } else {
                st.daChon.removeAll(ids);
            }
            taiLai();
        });
        card.getChildren().add(chk);

        for (SectionSlot s : th) {
            VBox dong = new VBox(1);
            Label loai = Functions.nhanF(s.section().loai(), s.section().loai().equals("LT")
                    ? "#DBEAFE" : "#FEF3C7", s.section().loai().equals("LT") ? "#1D4ED8" : "#B45309");
            Label lich = Functions.moTa(lichCua(s.section()));
            HBox line = new HBox(8, loai, lich);
            line.setAlignment(Pos.CENTER_LEFT);
            dong.getChildren().add(line);
            card.getChildren().add(dong);
        }
        return card;
    }

    private String nhanToHop(List<SectionSlot> th) {
        List<String> p = new ArrayList<>();
        for (SectionSlot s : th) {
            Section x = s.section();
            if ("LT".equals(x.loai())) {
                p.add("LT" + x.nhom());
            } else {
                p.add("TH" + x.nhom() + "." + x.toTh());
            }
        }
        return "Chá»n tá»• há»£p: " + String.join(" + ", p);
    }

    // ------------------------------------------------------------- cá»™t pháº£i

    private void veCotPhai() {
        cotPhai.getChildren().clear();
        cotPhai.getChildren().addAll(
                Functions.tieuDeThe("Má»©c Ä‘á»™ Æ°u tiÃªn"),
                Functions.moTa("KÃ©o thanh trÆ°á»£t Ä‘á»ƒ tÄƒng hoáº·c giáº£m má»©c Ä‘á»™ quan trá»ng cá»§a tá»«ng tiÃªu chÃ­."));

        cotPhai.getChildren().add(tieuChi("Æ¯u tiÃªn buá»•i sÃ¡ng",
                "Xáº¿p nhiá»u tiáº¿t há»c vÃ o buá»•i sÃ¡ng (tiáº¿t 1â€“5) hÆ¡n.", sSang, lblSang));
        cotPhai.getChildren().add(tieuChi("Ãt ngÃ y Ä‘áº¿n trÆ°á»ng",
                "Giáº£m sá»‘ ngÃ y pháº£i lÃªn trÆ°á»ng trong tuáº§n.", sItNgay, lblItNgay));
        cotPhai.getChildren().add(tieuChi("Khoáº£ng trá»‘ng ngáº¯n",
                "Háº¡n cháº¿ cÃ¡c khoáº£ng nghá»‰ dÃ i giá»¯a nhá»¯ng buá»•i há»c trong cÃ¹ng ngÃ y.", sKhoangTrong, lblKhoang));
        cotPhai.getChildren().add(tieuChiNgay());
        cotPhai.getChildren().addAll(Functions.vach(), theNut(), ghiChu());

        sSang.valueProperty().addListener((o, a, b) -> lblSang.setText(String.valueOf((int) sSang.getValue())));
        sItNgay.valueProperty().addListener((o, a, b) -> lblItNgay.setText(String.valueOf((int) sItNgay.getValue())));
        sKhoangTrong.valueProperty().addListener((o, a, b) -> lblKhoang.setText(String.valueOf((int) sKhoangTrong.getValue())));
        sNgayYeuThich.valueProperty().addListener((o, a, b) -> {
            lblNgayYeu.setText(String.valueOf((int) sNgayYeuThich.getValue()));
            ngayChk.values().forEach(c -> c.setDisable(sNgayYeuThich.getValue() == 0));
        });
        ngayChk.values().forEach(c -> c.setDisable(sNgayYeuThich.getValue() == 0));
    }

    private VBox tieuChi(String td, String mt, Slider slider, Label giaTri) {
        VBox card = Functions.the(12);
        HBox top = new HBox(10, Functions.tieuDeThe(td), Functions.khoangTrong(), giaTri);
        top.setAlignment(Pos.CENTER_LEFT);
        Label mo = Functions.moTa(mt);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        HBox nhan = new HBox(Functions.nhanNho("0 Â· KhÃ´ng Æ°u tiÃªn"), Functions.khoangTrong(),
                Functions.nhanNho("5 Â· Cao nháº¥t"));
        card.getChildren().addAll(top, mo, slider, nhan);
        return card;
    }

    private VBox tieuChiNgay() {
        VBox card = Functions.the(12);
        HBox top = new HBox(10, Functions.tieuDeThe("NgÃ y há»c yÃªu thÃ­ch"), Functions.khoangTrong(), lblNgayYeu);
        top.setAlignment(Pos.CENTER_LEFT);
        Label mo = Functions.moTa("Æ¯u tiÃªn xáº¿p lá»‹ch vÃ o cÃ¡c ngÃ y báº¡n Ä‘Ã£ chá»n bÃªn dÆ°á»›i.");
        sNgayYeuThich.setMajorTickUnit(1);
        sNgayYeuThich.setMinorTickCount(0);
        sNgayYeuThich.setBlockIncrement(1);
        sNgayYeuThich.setSnapToTicks(true);
        sNgayYeuThich.setShowTickMarks(true);
        sNgayYeuThich.setShowTickLabels(true);

        ngayChk.clear();
        FlowPane ngayRow = new FlowPane(10, 8);
        String[] tenNgay = {"T2", "T3", "T4", "T5", "T6", "T7"};
        for (int i = 0; i < tenNgay.length; i++) {
            int thu = i + 2;
            CheckBox chk = new CheckBox(tenNgay[i]);
            chk.setSelected(st.criteria.ngayChon().contains(thu));
            ngayChk.put(thu, chk);
            ngayRow.getChildren().add(chk);
        }
        HBox nhan = new HBox(Functions.nhanNho("0 Â· KhÃ´ng Æ°u tiÃªn"), Functions.khoangTrong(),
                Functions.nhanNho("5 Â· Cao nháº¥t"));
        card.getChildren().addAll(top, mo, sNgayYeuThich, nhan, Functions.nhanNho("CÃ¡c ngÃ y Æ°u tiÃªn"), ngayRow);
        return card;
    }

    private VBox theNut() {
        VBox box = new VBox(8);
        Button macDinh = Functions.nutPhu("KhÃ´i phá»¥c máº·c Ä‘á»‹nh");
        Button toiUu = Functions.nutChinh("Tá»‘i Æ°u thá»i khÃ³a biá»ƒu");
        macDinh.setMaxWidth(Double.MAX_VALUE);
        toiUu.setMaxWidth(Double.MAX_VALUE);
        toiUu.setPadding(new Insets(10));
        macDinh.setOnAction(e -> {
            Criteria d = Criteria.macDinh();
            sSang.setValue(d.uuTienSang());
            sItNgay.setValue(d.itNgay());
            sKhoangTrong.setValue(d.khoangTrong());
            sNgayYeuThich.setValue(d.ngayYeuThich());
            ngayChk.forEach((thu, chk) -> chk.setSelected(d.ngayChon().contains(thu)));
        });
        toiUu.setOnAction(e -> {
            luuCriteria();
            frame.batDauXepLich();
        });
        box.getChildren().addAll(macDinh, toiUu);
        return box;
    }

    private VBox ghiChu() {
        VBox box = Functions.the(12);
        box.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 12; -fx-border-color: #BFDBFE; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        box.getChildren().addAll(
                Functions.nhanDeXuat("Ghi chÃº"),
                Functions.moTa("Trá»ng sá»‘ chá»‰ dÃ¹ng Ä‘á»ƒ xáº¿p háº¡ng cÃ¡c lá»‹ch. Má»i phÆ°Æ¡ng Ã¡n há»£p lá»‡ Ä‘á»u pháº£i "
                        + "khÃ´ng trÃ¹ng giá» vÃ  tuÃ¢n thá»§ Ä‘iá»u kiá»‡n ghÃ©p nhÃ³m lÃ½ thuyáº¿t â€“ thá»±c hÃ nh."));
        return box;
    }

    private void luuCriteria() {
        List<Integer> ngay = new ArrayList<>();
        ngayChk.forEach((thu, chk) -> {
            if (chk.isSelected()) ngay.add(thu);
        });
        st.criteria = new Criteria((int) sSang.getValue(), (int) sItNgay.getValue(),
                (int) sKhoangTrong.getValue(), (int) sNgayYeuThich.getValue(), ngay, 3);
    }

    private String lichCua(Section s) {
        // TÃ¬m cÃ¡c buá»•i há»c cá»§a section nÃ y trong dá»¯ liá»‡u nhÃ³m lá»›p.
        for (SectionSlot sl : st.tatCaNhom) {
            if (sl.section().id() == s.id()) {
                List<String> p = new ArrayList<>();
                for (ClassSession cs : sl.sessions()) {
                    p.add("Thá»© " + cs.thu() + " Â· tiáº¿t " + cs.tietBd() + "â€“"
                            + (cs.tietBd() + cs.soTiet() - 1) + " Â· " + cs.phong());
                }
                return String.join("  |  ", p);
            }
        }
        return "";
    }
}
