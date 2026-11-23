package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Course;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

/** Màn hình Kết quả đề xuất: ba card phương án A/B/C, lưới thời khóa biểu và điểm đáp ứng tiêu chí. */
public class ResultPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final VBox chiTiet = new VBox(14);
    private final VBox khung = new VBox(16);
    private final VBox pinnedBar = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane();
    private ScheduleOption pinnedOption;
    private TimetableGridPanel pinnedGrid;
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
    }

    private void ghimTkb(ScheduleOption o) {
        if (pinnedOption != null && pinnedOption.ten().equals(o.ten())) {
            // bo ghim
            pinnedOption = null;
            pinnedGrid = null;
            pinnedBar.getChildren().clear();
            pinnedBar.setVisible(false);
            pinnedBar.setManaged(false);
            return;
        }
        pinnedOption = o;
        pinnedGrid = new TimetableGridPanel();
        pinnedGrid.setTieuDe("Ghim: " + o.ten());
        pinnedGrid.setSlots(o.slots());
        pinnedGrid.setMinHeight(420);
        pinnedGrid.setPrefHeight(420);
        pinnedGrid.setMaxHeight(460);
        VBox.setVgrow(pinnedGrid, Priority.NEVER);

        pinnedBar.getChildren().clear();
        HBox tieuDe = new HBox(10,
                Functions.tieuDeThe("📌 Đang ghim: Phương án " + o.ten()),
                Functions.nhanChinh("Điểm " + Math.round(o.diem()) + "/100"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);
        Button boGhim = Functions.nutPhu("Bỏ ghim");
        boGhim.setOnAction(e -> ghimTkb(o));
        HBox nut = new HBox(10, Functions.khoangTrong(), boGhim);
        nut.setAlignment(Pos.CENTER_RIGHT);
        pinnedBar.getChildren().addAll(tieuDe, pinnedGrid, nut);
        pinnedBar.setVisible(true);
        pinnedBar.setManaged(true);
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
            VBox theLuoi = Functions.the(10);
            HBox tieuDe = new HBox(10,
                    Functions.tieuDeThe("Thời khóa biểu phương án " + o.ten()),
                    Functions.nhanChinh("Điểm " + Math.round(o.diem()) + "/100"));
            tieuDe.setAlignment(Pos.CENTER_LEFT);
            theLuoi.getChildren().add(tieuDe);

            TimetableGridPanel grid = new TimetableGridPanel();
            grid.setTieuDe("Phương án " + o.ten());
            grid.setSlots(o.slots());
            grid.setPrefHeight(400);
            theLuoi.getChildren().add(grid);

            List<String> biLoaiHienThi = new ArrayList<>();
            if (o.biLoai() != null) {
                for (String item : o.biLoai()) {
                    String code = item == null ? "" : item.trim();
                    String name = "";
                    if (code.contains(" - ")) {
                        String[] parts = code.split(" - ", 2);
                        code = parts[0].trim();
                        name = parts.length > 1 ? parts[1].trim() : "";
                    }
                    if (name.isBlank()) {
                        for (Course c : st.courses) {
                            if (c.maMh().equals(code)) { name = c.ten(); break; }
                        }
                    }
                    biLoaiHienThi.add(name.isBlank() ? code : code + " - " + name);
                }
            }
            String biLoaiText = biLoaiHienThi.isEmpty() ? "" : String.join(", ", biLoaiHienThi);
            if (!biLoaiText.isEmpty()) {
                theLuoi.getChildren().add(Functions.nhanCanhBao("Không xếp được học phần: " + biLoaiText));
            } else {
                theLuoi.getChildren().add(Functions.nhanHopLe("Đã xếp đủ tất cả học phần đã chọn · Không trùng giờ"));
            }

            Button xuat = Functions.nutPhu("Xuất PNG");
            xuat.setOnAction(e -> Functions.xuatPng(grid, frame.window(), "Phương án " + o.ten()));
            Button ghim = Functions.nutPhu("📌 Ghim");
            ghim.setOnAction(e -> ghimTkb(o));
            HBox nut = new HBox(10, Functions.khoangTrong(),
                    ghim, xuat, nutLuu("Lưu thời khóa biểu", o));
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
