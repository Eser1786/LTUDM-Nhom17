package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.Criteria;
import com.tkb.frontend.model.Types.JobResult;
import com.tkb.frontend.model.Types.OptionStats;
import com.tkb.frontend.model.Types.ScheduleOption;
import com.tkb.frontend.net.TkbClient;
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

/** Màn hình Kết quả đề xuất: ba card phương án A/B/C, lưới thời khóa biểu và điểm đáp ứng tiêu chí. */
public class ResultPanel extends ScrollPane {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final HBox cardsBox = new HBox(14);
    private final VBox chiTiet = new VBox(14);
    private final VBox khung = new VBox(16);
    private final Map<String, CheckBox> chkSoSanh = new LinkedHashMap<>();
    private final Button btnSoSanh = Functions.nutPhu("So sánh phương án đã chọn");
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
                Functions.chuDe("Kết quả đề xuất"),
                Functions.moTa("Hệ thống đề xuất các phương án hợp lệ (không trùng giờ, đúng cặp LT–TH). "
                        + "Chọn một phương án để xem lịch chi tiết."));

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
            khung.getChildren().setAll(Functions.chuDe("Chưa có kết quả đề xuất"),
                    Functions.moTa("Hãy thiết lập ưu tiên và chạy “Tối ưu thời khóa biểu” để nhận các phương án."),
                    theNutChay());
        } else {
            hienKetQua(ketQua);
        }
    }

    private Button theNutChay() {
        Button b = Functions.nutChinh("Đi tới thiết lập ưu tiên");
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
            Label ten = Functions.tieuDeThe("Phương án " + o.ten());
            top.getChildren().add(ten);
            if (deXuat) {
                top.getChildren().add(Functions.nhanDeXuat("Đề xuất"));
            }
            top.getChildren().addAll(Functions.khoangTrong(), Functions.nhanChinh(Math.round(o.diem()) + "/100"));

            Label mo = Functions.moTa(o.moTa());

            FlowPane chiSo = new FlowPane(8, 8);
            OptionStats s = o.stats();
            chiSo.getChildren().addAll(
                    chiSoMuc("Số ngày đến trường", s.soNgayHoc() + " ngày"),
                    chiSoMuc("Tổng phút trống", s.phutTrong() + " phút"),
                    chiSoMuc("Tỷ lệ buổi sáng", s.tyLeSang() + "%"));

            CheckBox chk = new CheckBox("So sánh");
            chkSoSanh.put(o.ten(), chk);
            chk.setOnAction(e -> capNhatNutSoSanh());

            Button xem = deXuat ? Functions.nutChinh("Xem lịch") : Functions.nutPhu("Xem lịch");
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
                Functions.tieuDeThe("Thời khóa biểu phương án " + o.ten()),
                Functions.nhanChinh("Điểm " + Math.round(o.diem()) + "/100"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);

        VBox theLuoi = Functions.the(14);
        theLuoi.getChildren().add(tieuDe);
        TimetableGridPanel grid = new TimetableGridPanel();
        grid.setTieuDe("Phương án " + o.ten());
        grid.setSlots(o.slots());
        grid.setPrefHeight(560);
        theLuoi.getChildren().add(grid);

        VBox theDiem = Functions.the(14);
        theDiem.getChildren().addAll(
                Functions.tieuDeThe("Điểm đáp ứng từng tiêu chí"),
                Functions.moTa(dienDatTieuChi(o)));
        if (!o.biLoai().isEmpty()) {
            theDiem.getChildren().add(Functions.nhanCanhBao("Không xếp được học phần: "
                    + String.join(", ", o.biLoai())));
        } else {
            theDiem.getChildren().add(Functions.nhanHopLe("Đã xếp đủ tất cả học phần đã chọn · Không trùng giờ"));
        }

        HBox nut = new HBox(10, Functions.khoangTrong(),
                nutLuu("Lưu thời khóa biểu", o));
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
                    "Lịch phương án " + o.ten(), ids,
                    () -> frame.denTrang(MainFrame.CARD_SAVED));
        });
        return b;
    }

    private String dienDatTieuChi(ScheduleOption o) {
        Criteria c = st.criteria;
        OptionStats s = o.stats();
        StringBuilder sb = new StringBuilder();
        sb.append("• Ưu tiên buổi sáng: ").append(s.tyLeSang()).append("% tiết học buổi sáng");
        sb.append(" (trọng số ").append(c.uuTienSang()).append("/5).\n");
        sb.append("• Ít ngày đến trường: ").append(s.soNgayHoc()).append(" ngày");
        sb.append(" (trọng số ").append(c.itNgay()).append("/5).\n");
        sb.append("• Khoảng trống ngắn: ").append(s.phutTrong()).append(" phút trống");
        sb.append(" (trọng số ").append(c.khoangTrong()).append("/5).\n");
        sb.append("• Ngày học yêu thích: trọng số ").append(c.ngayYeuThich()).append("/5");
        if (c.ngayChon() != null && !c.ngayChon().isEmpty()) {
            sb.append(", các ngày T").append(String.join(", T", c.ngayChon().stream().map(String::valueOf).toList()));
        }
        sb.append(".\n");
        sb.append("Vì vậy phương án này đạt điểm ").append(Math.round(o.diem())).append("/100 và được xếp hạng tương ứng.");
        return sb.toString();
    }

    private void capNhatNutSoSanh() {
        long n = chkSoSanh.values().stream().filter(CheckBox::isSelected).count();
        btnSoSanh.setDisable(n < 2);
        btnSoSanh.setText(n < 2 ? "So sánh phương án đã chọn (chọn 2–3)"
                : "So sánh " + n + " phương án đã chọn");
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
            Functions.canhBao(frame.window(), "Hãy chọn ít nhất 2 phương án để so sánh.");
            return;
        }
        frame.moSoSanhPhuongAn(MainFrame.CARD_RES, ds);
    }
}
