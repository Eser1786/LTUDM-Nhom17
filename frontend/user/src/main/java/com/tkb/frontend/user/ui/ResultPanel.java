package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.JobResult;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MÃ n hÃ¬nh Káº¿t quáº£ Ä‘á» xuáº¥t: ba card phÆ°Æ¡ng Ã¡n A/B/C, lÆ°á»›i thá»i khÃ³a biá»ƒu vÃ  Ä‘iá»ƒm Ä‘Ã¡p á»©ng tiÃªu chÃ­. */
public class ResultPanel extends ScrollPane {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final HBox cardsBox = new HBox(14);
    private final VBox chiTiet = new VBox(14);
    private final VBox khung = new VBox(16);
    private final Map<String, CheckBox> chkSoSanh = new LinkedHashMap<>();
    private final Button btnSoSanh = Functions.nutPhu("So sÃ¡nh phÆ°Æ¡ng Ã¡n Ä‘Ã£ chá»n");
    private JobResult ketQua;
    private int chiSoDangXem = 0;

    public ResultPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        this.st = frame.trangThai();

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(16);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        VBox header = new VBox(4,
                Functions.chuDe("Káº¿t quáº£ Ä‘á» xuáº¥t"),
                Functions.moTa("Há»‡ thá»‘ng Ä‘á» xuáº¥t cÃ¡c phÆ°Æ¡ng Ã¡n há»£p lá»‡ (khÃ´ng trÃ¹ng giá», Ä‘Ãºng cáº·p LTâ€“TH). "
                        + "Chá»n má»™t phÆ°Æ¡ng Ã¡n Ä‘á»ƒ xem lá»‹ch chi tiáº¿t."));

        cardsBox.setAlignment(Pos.TOP_LEFT);

        HBox thanhNut = new HBox(10, Functions.khoangTrong(), btnSoSanh);
        thanhNut.setAlignment(Pos.CENTER_RIGHT);
        btnSoSanh.setOnAction(e -> moSoSanh());

        khung.getChildren().addAll(header, cardsBox, thanhNut, chiTiet);
        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
    }

    public void hienTrangThai() {
        if (ketQua == null) {
            khung.getChildren().setAll(Functions.chuDe("ChÆ°a cÃ³ káº¿t quáº£ Ä‘á» xuáº¥t"),
                    Functions.moTa("HÃ£y thiáº¿t láº­p Æ°u tiÃªn vÃ  cháº¡y â€œTá»‘i Æ°u thá»i khÃ³a biá»ƒuâ€ Ä‘á»ƒ nháº­n cÃ¡c phÆ°Æ¡ng Ã¡n."),
                    theNutChay());
        } else {
            hienKetQua(ketQua);
        }
    }

    private Button theNutChay() {
        Button b = Functions.nutChinh("Äi tá»›i thiáº¿t láº­p Æ°u tiÃªn");
        b.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));
        return b;
    }

    public void hienKetQua(JobResult result) {
        this.ketQua = result;
        this.chiSoDangXem = 0;
        this.chkSoSanh.clear();
        if (result == null || result.options().isEmpty()) {
            hienTrangThai();
            return;
        }
        veCards();
        veChiTiet(result.options().get(0));
        capNhatNutSoSanh();
    }

    private void veCards() {
        cardsBox.getChildren().clear();
        List<ScheduleOption> options = ketQua.options();
        for (int i = 0; i < options.size(); i++) {
            ScheduleOption o = options.get(i);
            boolean deXuat = i == 0;
            VBox card = deXuat ? Functions.theNoiBat() : Functions.the(16);
            card.setPrefWidth(340);
            card.setMinWidth(320);

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            Label ten = Functions.tieuDeThe("PhÆ°Æ¡ng Ã¡n " + o.ten());
            top.getChildren().add(ten);
            if (deXuat) {
                top.getChildren().add(Functions.nhanDeXuat("Äá» xuáº¥t"));
            }
            top.getChildren().addAll(Functions.khoangTrong(), Functions.nhanChinh(Math.round(o.diem()) + "/100"));

            Label mo = Functions.moTa(o.moTa());

            FlowPane chiSo = new FlowPane(8, 8);
            OptionStats s = o.stats();
            chiSo.getChildren().addAll(
                    chiSoMuc("Sá»‘ ngÃ y Ä‘áº¿n trÆ°á»ng", s.soNgayHoc() + " ngÃ y"),
                    chiSoMuc("Tá»•ng phÃºt trá»‘ng", s.phutTrong() + " phÃºt"),
                    chiSoMuc("Tá»· lá»‡ buá»•i sÃ¡ng", s.tyLeSang() + "%"));

            CheckBox chk = new CheckBox("So sÃ¡nh");
            chkSoSanh.put(o.ten(), chk);
            chk.setOnAction(e -> capNhatNutSoSanh());

            Button xem = deXuat ? Functions.nutChinh("Xem lá»‹ch") : Functions.nutPhu("Xem lá»‹ch");
            int idx = i;
            xem.setOnAction(e -> {
                chiSoDangXem = idx;
                veChiTiet(o);
            });

            HBox dongNut = new HBox(10, chk, Functions.khoangTrong(), xem);
            dongNut.setAlignment(Pos.CENTER_LEFT);

            card.getChildren().addAll(top, mo, Functions.vach(), chiSo, dongNut);
            cardsBox.getChildren().add(card);
        }
    }

    private VBox chiSoMuc(String nhan, String giaTri) {
        VBox v = new VBox(2);
        v.setPadding(new Insets(6, 10, 6, 10));
        v.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");
        v.setMinWidth(140);
        Label g = new Label(giaTri);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        g.setTextFill(Functions.MA_CHU_DAM);
        v.getChildren().addAll(Functions.nhanNho(nhan), g);
        return v;
    }

    private void veChiTiet(ScheduleOption o) {
        chiTiet.getChildren().clear();
        HBox tieuDe = new HBox(10,
                Functions.tieuDeThe("Thá»i khÃ³a biá»ƒu phÆ°Æ¡ng Ã¡n " + o.ten()),
                Functions.nhanChinh("Äiá»ƒm " + Math.round(o.diem()) + "/100"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);

        VBox theLuoi = Functions.the(14);
        theLuoi.getChildren().add(tieuDe);
        TimetableGridPanel grid = new TimetableGridPanel();
        grid.setTieuDe("PhÆ°Æ¡ng Ã¡n " + o.ten());
        grid.setSlots(o.slots());
        grid.setPrefHeight(560);
        theLuoi.getChildren().add(grid);

        VBox theDiem = Functions.the(14);
        theDiem.getChildren().addAll(
                Functions.tieuDeThe("Äiá»ƒm Ä‘Ã¡p á»©ng tá»«ng tiÃªu chÃ­"),
                Functions.moTa(dienDatTieuChi(o)));
        if (!o.biLoai().isEmpty()) {
            theDiem.getChildren().add(Functions.nhanCanhBao("KhÃ´ng xáº¿p Ä‘Æ°á»£c há»c pháº§n: "
                    + String.join(", ", o.biLoai())));
        } else {
            theDiem.getChildren().add(Functions.nhanHopLe("ÄÃ£ xáº¿p Ä‘á»§ táº¥t cáº£ há»c pháº§n Ä‘Ã£ chá»n Â· KhÃ´ng trÃ¹ng giá»"));
        }

        HBox nut = new HBox(10, Functions.khoangTrong(),
                nutLuu("LÆ°u thá»i khÃ³a biá»ƒu", o));
        nut.setAlignment(Pos.CENTER_RIGHT);
        theDiem.getChildren().add(nut);

        chiTiet.getChildren().addAll(theLuoi, theDiem);
    }

    private Button nutLuu(String text, ScheduleOption o) {
        Button b = Functions.nutChinh(text);
        b.setOnAction(e -> {
            List<Integer> ids = new ArrayList<>();
            o.slots().forEach(s -> ids.add(s.section().id()));
            MoDals.luuLich(frame.window(), client, st.hocKy == null ? "2026-2027-1" : st.hocKy.ma(),
                    "Lá»‹ch phÆ°Æ¡ng Ã¡n " + o.ten(), ids,
                    () -> frame.denTrang(MainFrame.CARD_SAVED));
        });
        return b;
    }

    private String dienDatTieuChi(ScheduleOption o) {
        Criteria c = st.criteria;
        OptionStats s = o.stats();
        StringBuilder sb = new StringBuilder();
        sb.append("â€¢ Æ¯u tiÃªn buá»•i sÃ¡ng: ").append(s.tyLeSang()).append("% tiáº¿t há»c buá»•i sÃ¡ng");
        sb.append(" (trá»ng sá»‘ ").append(c.uuTienSang()).append("/5).\n");
        sb.append("â€¢ Ãt ngÃ y Ä‘áº¿n trÆ°á»ng: ").append(s.soNgayHoc()).append(" ngÃ y");
        sb.append(" (trá»ng sá»‘ ").append(c.itNgay()).append("/5).\n");
        sb.append("â€¢ Khoáº£ng trá»‘ng ngáº¯n: ").append(s.phutTrong()).append(" phÃºt trá»‘ng");
        sb.append(" (trá»ng sá»‘ ").append(c.khoangTrong()).append("/5).\n");
        sb.append("â€¢ NgÃ y há»c yÃªu thÃ­ch: trá»ng sá»‘ ").append(c.ngayYeuThich()).append("/5");
        if (c.ngayChon() != null && !c.ngayChon().isEmpty()) {
            sb.append(", cÃ¡c ngÃ y T").append(String.join(", T", c.ngayChon().stream().map(String::valueOf).toList()));
        }
        sb.append(".\n");
        sb.append("VÃ¬ váº­y phÆ°Æ¡ng Ã¡n nÃ y Ä‘áº¡t Ä‘iá»ƒm ").append(Math.round(o.diem())).append("/100 vÃ  Ä‘Æ°á»£c xáº¿p háº¡ng tÆ°Æ¡ng á»©ng.");
        return sb.toString();
    }

    private void capNhatNutSoSanh() {
        long n = chkSoSanh.values().stream().filter(CheckBox::isSelected).count();
        btnSoSanh.setDisable(n < 2);
        btnSoSanh.setText(n < 2 ? "So sÃ¡nh phÆ°Æ¡ng Ã¡n Ä‘Ã£ chá»n (chá»n 2â€“3)"
                : "So sÃ¡nh " + n + " phÆ°Æ¡ng Ã¡n Ä‘Ã£ chá»n");
    }

    private void moSoSanh() {
        List<ScheduleOption> ds = new ArrayList<>();
        for (ScheduleOption o : ketQua.options()) {
            CheckBox chk = chkSoSanh.get(o.ten());
            if (chk != null && chk.isSelected()) {
                ds.add(o);
            }
        }
        if (ds.size() < 2) {
            Functions.canhBao(frame.window(), "HÃ£y chá»n Ã­t nháº¥t 2 phÆ°Æ¡ng Ã¡n Ä‘á»ƒ so sÃ¡nh.");
            return;
        }
        frame.moSoSanhPhuongAn(MainFrame.CARD_RES, ds);
    }
}
