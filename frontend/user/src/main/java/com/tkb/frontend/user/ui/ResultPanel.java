package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.JobResult;
import com.tkb.frontend.user.model.Types.OptionStats;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

/** Màn hình Kết quả đề xuất: ba card phương án A/B/C, lưới thời khóa biểu và điểm đáp ứng tiêu chí. */
public class ResultPanel extends ScrollPane {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final VBox chiTiet = new VBox(14);
    private final VBox khung = new VBox(16);
    private JobResult ketQua;

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

        khung.getChildren().addAll(header, chiTiet);
        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
    }

    private void resetKhung() {
        khung.getChildren().setAll(
                Functions.chuDe("Kết quả đề xuất"),
                Functions.moTa("Hệ thống đề xuất các phương án hợp lệ (không trùng giờ, đúng cặp LT–TH). "
                        + "Chọn một phương án để xem lịch chi tiết."),
                chiTiet
        );

        chiTiet.getChildren().clear();
    }

    public void hienTrangThai() {
        resetKhung();

        if (ketQua == null || ketQua.options().isEmpty()) {
            chiTiet.getChildren().setAll(
                    Functions.chuDe("Chưa có kết quả đề xuất"),
                    Functions.moTa("Hãy thiết lập ưu tiên và chạy “Tối ưu thời khóa biểu” để nhận các phương án."),
                    theNutChay()
            );
        } else {
            hienKetQua(ketQua);
        }
    }

    private Button theNutChay() {
        Button b = Functions.nutChinh("Đi tới thiết lập ưu tiên");
        b.setOnAction(e -> frame.denTrang(MainFrame.CARD_REQ));
        return b;
    }

    public void hienKetQua(JobResult result) {
        this.ketQua = result;
        resetKhung();
        if (result == null || result.options().isEmpty()) {
            hienTrangThai();
            return;
        }
        List<ScheduleOption> sorted = new ArrayList<>(result.options());
        sorted.sort(Comparator.comparingDouble(ScheduleOption::diem).reversed());
        this.ketQua = new JobResult(result.maHocKy(), sorted);
        veTatCaLich(sorted);
    }

    private void veTatCaLich(List<ScheduleOption> options) {
        chiTiet.getChildren().clear();
        for (ScheduleOption o : options) {
            VBox theLuoi = Functions.the(14);
            HBox tieuDe = new HBox(10,
                    Functions.tieuDeThe("Thời khóa biểu phương án " + o.ten()),
                    Functions.nhanChinh("Điểm " + Math.round(o.diem()) + "/100"));
            tieuDe.setAlignment(Pos.CENTER_LEFT);
            theLuoi.getChildren().add(tieuDe);

            TimetableGridPanel grid = new TimetableGridPanel();
            grid.setTieuDe("Phương án " + o.ten());
            grid.setSlots(o.slots());
            grid.setPrefHeight(560);
            theLuoi.getChildren().add(grid);

            if (!o.biLoai().isEmpty()) {
                theLuoi.getChildren().add(Functions.nhanCanhBao("Không xếp được học phần: "
                        + String.join(", ", o.biLoai())));
            } else {
                theLuoi.getChildren().add(Functions.nhanHopLe("Đã xếp đủ tất cả học phần đã chọn · Không trùng giờ"));
            }

            HBox nut = new HBox(10, Functions.khoangTrong(),
                    nutLuu("Lưu thời khóa biểu", o));
            nut.setAlignment(Pos.CENTER_RIGHT);
            theLuoi.getChildren().add(nut);

            chiTiet.getChildren().add(theLuoi);
        }
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

}
