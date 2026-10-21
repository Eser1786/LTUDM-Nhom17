package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.ClassSession;
import com.tkb.frontend.model.Types.Section;
import com.tkb.frontend.model.Types.SectionSlot;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Màn hình Nhóm lớp học phần: bảng danh sách nhóm lớp với bộ lọc, phân trang,
 * và thể hiện cặp tương thích giữa lý thuyết (LT) và thực hành (TH).
 */
public class RequirementPanel extends ScrollPane {
    private static final int MOI_TRANG = 8;

    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final TextField txtTimKiem = Functions.oNhap();
    private final ComboBox<String> cbLoai = new ComboBox<>();
    private final ComboBox<String> cbNgay = new ComboBox<>();
    private final ComboBox<String> cbKhung = new ComboBox<>();
    private final Button btnDatLai = Functions.nutPhu("Đặt lại bộ lọc");
    private final Button btnTiep = Functions.nutChinh("Tiếp tục: Thiết lập ưu tiên");
    private final VBox bang = new VBox(0);
    private final VBox khung = new VBox(14);
    private final Label lblDem = Functions.moTa("");
    private final HBox phanTrang = new HBox(6);
    private List<SectionSlot> locDuLieu = new ArrayList<>();
    private int trangHienTai;

    public RequirementPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        this.st = frame.trangThai();

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(14);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        VBox header = new VBox(4,
                Functions.chuDe("Nhóm lớp học phần"),
                Functions.moTa("Tìm kiếm và chọn các nhóm lớp bạn được phép tham gia. "
                        + "Nhóm lý thuyết cần được chọn cùng nhóm thực hành tương ứng (LT01 ghép TH01.1)."));

        khung.getChildren().addAll(header, theBoLoc(), theBang());

        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
        getStyleClass().add("requirement-scroll");
    }

    private VBox theBoLoc() {
        VBox card = Functions.the(14);
        cbLoai.getItems().addAll("Tất cả loại lớp", "Lý thuyết (LT)", "Thực hành (TH)");
        cbNgay.getItems().addAll("Tất cả các ngày", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7");
        cbKhung.getItems().addAll("Tất cả khung giờ", "Buổi sáng (tiết 1–5)", "Buổi chiều (tiết 6–12)");
        cbLoai.setValue("Tất cả loại lớp");
        cbNgay.setValue("Tất cả các ngày");
        cbKhung.setValue("Tất cả khung giờ");
        txtTimKiem.setPromptText("Tìm theo tên hoặc mã học phần...");
        txtTimKiem.setPrefWidth(280);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.add(Functions.nhanNho("Tìm kiếm"), 0, 0);
        grid.add(txtTimKiem, 0, 1);
        grid.add(Functions.nhanNho("Loại lớp"), 1, 0);
        grid.add(cbLoai, 1, 1);
        grid.add(Functions.nhanNho("Ngày học"), 2, 0);
        grid.add(cbNgay, 2, 1);
        grid.add(Functions.nhanNho("Khung giờ"), 3, 0);
        grid.add(cbKhung, 3, 1);

        HBox dong2 = new HBox(10, Functions.khoangTrong(), btnDatLai);
        dong2.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(grid, dong2);

        btnDatLai.setOnAction(e -> datLaiBoLoc());
        txtTimKiem.textProperty().addListener((o, a, b) -> {
            trangHienTai = 0;
            veBang();
        });
        cbLoai.valueProperty().addListener((o, a, b) -> {
            trangHienTai = 0;
            veBang();
        });
        cbNgay.valueProperty().addListener((o, a, b) -> {
            trangHienTai = 0;
            veBang();
        });
        cbKhung.valueProperty().addListener((o, a, b) -> {
            trangHienTai = 0;
            veBang();
        });
        return card;
    }

    private VBox theBang() {
        VBox card = Functions.the(14);
        HBox tieuDe = new HBox(10, Functions.tieuDeThe("Danh sách nhóm lớp"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);
        tieuDe.getChildren().addAll(Functions.khoangTrong(), lblDem);

        GridPane hang = new GridPane();
        hang.setMaxWidth(Double.MAX_VALUE);
        hang.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 8;");
        String[] cot = {"Chọn", "Học phần", "Nhóm", "Loại", "Ngày", "Thời gian", "Phòng", "Nhóm đi cùng"};
        for (int i = 0; i < cot.length; i++) {
            Label l = new Label(cot[i]);
            l.setFont(Functions.F_NHO);
            l.setPadding(new Insets(9, 8, 9, 8));
            GridPane.setHgrow(l, Priority.ALWAYS);
            l.setMaxWidth(Double.MAX_VALUE);
            hang.add(l, i, 0);
        }
        double[] mau = new double[]{60, 300, 80, 90, 80, 150, 90, 150};
        for (int i = 0; i < mau.length; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setMinWidth(mau[i]);
            cc.setPercentWidth(-1);
            cc.setHgrow(Priority.ALWAYS);
            hang.getColumnConstraints().add(cc);
        }

        bang.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8;");
        bang.setMaxWidth(Double.MAX_VALUE);
        ScrollPane cuon = new ScrollPane(bang);
        cuon.setFitToWidth(true);
        cuon.setMinHeight(260);
        cuon.setPrefHeight(440);
        cuon.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(cuon, Priority.ALWAYS);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().addAll(phanTrang, Functions.khoangTrong(), btnTiep);
        btnTiep.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));

        card.getChildren().addAll(tieuDe, hang, cuon, footer);
        return card;
    }

    public void taiDuLieu() {
        try {
            if (st.hocKy != null) {
                st.tatCaNhom = client.listAllSections(st.hocKy.ma());
            }
        } catch (com.tkb.frontend.net.ApiException ex) {
            Functions.loi(frame.window(), ex);
            return;
        }
        locDuLieu = st.tatCaNhom;
        veBang();
    }

    private List<SectionSlot> loc() {
        return locDuLieu.stream()
                .filter(this::matchSearch)
                .filter(this::matchLoai)
                .filter(this::matchNgay)
                .filter(this::matchKhung)
                .collect(Collectors.toList());
    }

    private void veBang() {
        List<SectionSlot> ds = loc();
        bang.getChildren().clear();

        if (ds.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setPadding(new Insets(30));
            empty.setAlignment(Pos.CENTER);
            Label t = Functions.tieuDeThe("Không có nhóm lớp nào phù hợp");
            Label m = Functions.moTa("Hãy thử nới lỏng bộ lọc hoặc nhấn “Đặt lại bộ lọc” để xem toàn bộ danh sách.");
            Button reset = Functions.nutPhu("Đặt lại bộ lọc");
            reset.setOnAction(e -> datLaiBoLoc());
            empty.getChildren().addAll(t, m, reset);
            bang.getChildren().add(empty);
            lblDem.setText("0 nhóm");
            phanTrang.getChildren().clear();
            return;
        }

        int tong = ds.size();
        int soTrang = (int) Math.ceil(tong / (double) MOI_TRANG);
        if (trangHienTai >= soTrang) trangHienTai = soTrang - 1;
        int tu = trangHienTai * MOI_TRANG;
        int den = Math.min(tu + MOI_TRANG, tong);
        List<SectionSlot> trang = ds.subList(tu, den);

        String tongKet = "Hiển thị " + (tu + 1) + "–" + den + " trong " + tong + " nhóm lớp";
        lblDem.setText(tongKet);

        for (int i = 0; i < trang.size(); i++) {
            SectionSlot slot = trang.get(i);
            bang.getChildren().add(taoDong(slot, i));
        }
        vePhanTrang(soTrang, tong);
    }

    private GridPane taoDong(SectionSlot slot, int index) {
        Section s = slot.section();
        boolean daChon = st.daChon.contains(s.id());

        GridPane row = new GridPane();
        row.setPadding(new Insets(2, 0, 2, 0));
        row.setMaxWidth(Double.MAX_VALUE);
        String bg = daChon ? "#F0FDF4" : (index % 2 == 0 ? "#FFFFFF" : "#FAFBFC");
        row.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
        double[] mau = new double[]{60, 320, 80, 90, 80, 150, 90, 150};
        for (int i = 0; i < mau.length; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setMinWidth(mau[i]);
            cc.setHgrow(Priority.ALWAYS);
            row.getColumnConstraints().add(cc);
        }

        CheckBox chk = new CheckBox();
        chk.setSelected(daChon);
        HBox chonBox = new HBox(chk);
        chonBox.setPadding(new Insets(9, 8, 9, 8));
        chonBox.setAlignment(Pos.CENTER_LEFT);
        chk.setOnAction(e -> {
            if (chk.isSelected()) {
                st.daChon.add(s.id());
            } else {
                st.daChon.remove(s.id());
            }
            veBang();
        });
        row.add(chonBox, 0, 0);

        VBox hocPhan = new VBox(2);
        hocPhan.setPadding(new Insets(7, 8, 7, 8));
        Label ten = new Label(slot.tenHocPhan());
        ten.setWrapText(true);
        Label ma = Functions.nhanNho(s.maMh() + " · " + slot.soTc() + " tín chỉ");
        hocPhan.getChildren().addAll(ten, ma);
        row.add(hocPhan, 1, 0);

        row.add(o(s.nhom(), false), 2, 0);
        row.add(o(s.loai(), true), 3, 0);
        row.add(o(ngayCua(slot), false), 4, 0);
        row.add(o(thoiGianCua(slot), false), 5, 0);
        row.add(o(phongCua(slot), false), 6, 0);
        row.add(o(nhomDiCung(s), true), 7, 0);
        return row;
    }

    private javafx.scene.layout.Region o(String text, boolean badge) {
        javafx.scene.Node noiDung;
        if (badge) {
            if (text.startsWith("LT")) noiDung = Functions.nhanF("LT", "#DBEAFE", "#1D4ED8");
            else if (text.startsWith("TH")) noiDung = Functions.nhanF("TH", "#FEF3C7", "#B45309");
            else if (text.startsWith("Cần")) noiDung = Functions.nhanCanhBao(text);
            else noiDung = Functions.nhanXam(text);
        } else {
            Label l = new Label(text);
            l.setWrapText(true);
            l.setFont(Functions.F_CHU);
            l.setTextFill(Functions.MA_CHU_DAM);
            noiDung = l;
        }
        HBox box = new HBox(noiDung);
        box.setPadding(new Insets(9, 8, 9, 8));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String ngayCua(SectionSlot slot) {
        List<String> ngay = new ArrayList<>();
        for (ClassSession cs : slot.sessions()) {
            String n = "T" + cs.thu();
            if (!ngay.contains(n)) ngay.add(n);
        }
        return String.join(", ", ngay);
    }

    private String thoiGianCua(SectionSlot slot) {
        List<String> gio = new ArrayList<>();
        for (ClassSession cs : slot.sessions()) {
            gio.add("Tiết " + cs.tietBd() + "–" + (cs.tietBd() + cs.soTiet() - 1));
        }
        return String.join(", ", gio);
    }

    private String phongCua(SectionSlot slot) {
        List<String> p = new ArrayList<>();
        for (ClassSession cs : slot.sessions()) {
            if (cs.phong() != null && !cs.phong().isBlank() && !p.contains(cs.phong())) {
                p.add(cs.phong());
            }
        }
        return String.join(", ", p);
    }

    /**
     * Trả về nhóm đi cùng để bảo đảm cặp tương thích: LT01 -> TH01.1, TH01.1 -> LT01.
     */
    private String nhomDiCung(Section s) {
        List<String> cap = new ArrayList<>();
        for (SectionSlot k : st.tatCaNhom) {
            Section y = k.section();
            if (!y.maMh().equals(s.maMh())) continue;
            if ("LT".equals(s.loai()) && "TH".equals(y.loai()) && y.nhom().equals(s.nhom())) {
                cap.add("TH" + y.nhom() + "." + y.toTh());
            } else if ("TH".equals(s.loai()) && "LT".equals(y.loai())
                    && y.nhom().equals(s.nhom())) {
                cap.add("LT" + y.nhom());
            }
        }
        if (cap.isEmpty()) {
            return "—";
        }
        return "Cần kèm " + String.join(", ", cap);
    }

    private void vePhanTrang(int soTrang, int tong) {
        phanTrang.getChildren().clear();
        Button nhau = Functions.nutPhu("‹");
        nhau.setDisable(trangHienTai == 0);
        nhau.setOnAction(e -> {
            trangHienTai--;
            veBang();
        });
        phanTrang.getChildren().add(nhau);
        for (int i = 0; i < soTrang; i++) {
            int tr = i;
            Button b = Functions.nutPhu(String.valueOf(i + 1));
            if (i == trangHienTai) {
                b.setStyle("-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-border-color: #1D4ED8; "
                        + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
            }
            b.setOnAction(e -> {
                trangHienTai = tr;
                veBang();
            });
            phanTrang.getChildren().add(b);
        }
        Button sau = Functions.nutPhu("›");
        sau.setDisable(trangHienTai >= soTrang - 1);
        sau.setOnAction(e -> {
            trangHienTai++;
            veBang();
        });
        phanTrang.getChildren().addAll(sau, Functions.nhanXam(tong + " nhóm"));
    }

    private boolean matchSearch(SectionSlot slot) {
        String q = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        String hay = (slot.section().maMh() + " " + slot.tenHocPhan()).toLowerCase(Locale.ROOT);
        return hay.contains(q);
    }

    private boolean matchLoai(SectionSlot slot) {
        String val = cbLoai.getValue();
        if (val == null || "Tất cả loại lớp".equals(val)) return true;
        return val.contains(slot.section().loai());
    }

    private boolean matchNgay(SectionSlot slot) {
        String val = cbNgay.getValue();
        if (val == null || "Tất cả các ngày".equals(val)) return true;
        for (ClassSession s : slot.sessions()) {
            if (dayLabel(s.thu()).equals(val)) return true;
        }
        return false;
    }

    private boolean matchKhung(SectionSlot slot) {
        String val = cbKhung.getValue();
        if (val == null || "Tất cả khung giờ".equals(val)) return true;
        for (ClassSession s : slot.sessions()) {
            if ("Buổi sáng (tiết 1–5)".equals(val) && s.tietBd() <= 5) return true;
            if ("Buổi chiều (tiết 6–12)".equals(val) && s.tietBd() >= 6) return true;
        }
        return false;
    }

    private String dayLabel(int thu) {
        return switch (thu) {
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            default -> "";
        };
    }

    private void datLaiBoLoc() {
        txtTimKiem.clear();
        cbLoai.setValue("Tất cả loại lớp");
        cbNgay.setValue("Tất cả các ngày");
        cbKhung.setValue("Tất cả khung giờ");
        trangHienTai = 0;
        veBang();
    }
}
