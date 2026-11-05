package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình chọn môn và đặt ưu tiên trong cùng một trang.
 * Người dùng chọn các môn cần xếp thời khóa biểu rồi điều chỉnh mức độ ưu tiên ngay trên màn hình này.
 */
public class RequirementPanel extends ScrollPane {
    private static final int MOI_TRANG = 10;

    private final TkbClient client;
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final PriorityPanel priorityPanel;
    private final TextField txtTimKiem = Functions.oNhap();
    private final Button btnDatLai = Functions.nutPhu("Đặt lại");
    private final VBox bang = new VBox(0);
    private final VBox daChonBox = new VBox(10);
    private final VBox khung = new VBox(14);
    private final HBox phanTrang = new HBox(10);
    private final Label lblDem = Functions.moTa("");
    private List<Course> locDuLieu = new ArrayList<>();
    private int trangHienTai;

    public RequirementPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        this.st = frame.trangThai();
        this.priorityPanel = new PriorityPanel(frame, false);

        khung.setPadding(new Insets(22, 26, 20, 26));
        khung.setSpacing(14);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        // Chia tieu de thanh 2 phan de dat "Thiet lap uu tien" ngang voi "Mon muon dang ky".
        HBox titleRow = new HBox(14);
        VBox leftTitle = new VBox(4,
                Functions.chuDe("Môn muốn đăng ký"),
                Functions.moTa("Chọn những môn bạn muốn học và điều chỉnh mức độ ưu tiên ngay trên cùng một màn hình để tối ưu thời khóa biểu."));
        HBox.setHgrow(leftTitle, Priority.ALWAYS);
        VBox rightTitle = new VBox(4,
                Functions.chuDe("Thiết lập ưu tiên"));
        rightTitle.setPrefWidth(420);
        rightTitle.setMinWidth(320);
        titleRow.getChildren().addAll(leftTitle, rightTitle);

        khung.getChildren().addAll(titleRow, theLayout());

        setContent(khung);
        setFitToWidth(true);
        setStyle("-fx-background-color: #F6F8FB;");
        getStyleClass().add("requirement-scroll");
    }

    private HBox theLayout() {
        HBox horizontal = new HBox(14);

        VBox left = new VBox(14);
        HBox.setHgrow(left, Priority.ALWAYS);
        VBox listCard = new VBox(12);
        listCard.getChildren().addAll(theBang());
        left.getChildren().addAll(listCard, daChonPanel());

        Button btnToiUu = Functions.nutChinh("Tối ưu thời khóa biểu");
        btnToiUu.setMaxWidth(Double.MAX_VALUE);
        btnToiUu.setPadding(new Insets(10));
        btnToiUu.setOnAction(e -> {
            priorityPanel.luuCriteria();
            frame.batDauXepLich();
        });
        left.getChildren().add(btnToiUu);

        VBox right = new VBox(14);
        right.setPrefWidth(420);
        right.setMinWidth(320);
        right.setPadding(new Insets(0, 0, 0, 16));
        right.setStyle("-fx-border-color: #CBD5E1; -fx-border-width: 0 0 0 1;");
        HBox.setHgrow(right, Priority.NEVER);
        // Thu nho bang thiet lap uu tien de dat vao nua ben phai, can len tren.
        priorityPanel.setPrefWidth(400);
        priorityPanel.setMaxWidth(400);
        right.getChildren().add(priorityPanel);

        horizontal.getChildren().addAll(left, right);
        return horizontal;
    }

    private VBox theBang() {
        VBox card = Functions.the(14);
        HBox tieuDe = new HBox(10, Functions.tieuDeThe("Danh sách môn"));
        tieuDe.setAlignment(Pos.CENTER_LEFT);
        tieuDe.getChildren().addAll(Functions.khoangTrong(), lblDem);

        GridPane hang = new GridPane();
        hang.setMaxWidth(Double.MAX_VALUE);
        hang.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 8;");
        String[] cot = {"Chọn", "Mã môn", "Tên môn", "Tín chỉ"};
        double[] rongCot = {10, 18, 52, 20};
        for (int i = 0; i < cot.length; i++) {
            Label l = new Label(cot[i]);
            l.setFont(Functions.F_NHO);
            l.setPadding(new Insets(9, 8, 9, 8));
            GridPane.setHgrow(l, Priority.ALWAYS);
            hang.add(l, i, 0);
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(rongCot[i]);
            cc.setHgrow(Priority.ALWAYS);
            hang.getColumnConstraints().add(cc);
        }

        bang.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; "
                + "-fx-background-radius: 8;");
        bang.setMaxWidth(Double.MAX_VALUE);
        ScrollPane cuon = new ScrollPane(bang);
        cuon.setFitToWidth(true);
        cuon.setMinHeight(360);
        cuon.setPrefHeight(420);
        cuon.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(cuon, Priority.ALWAYS);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().addAll(phanTrang, Functions.khoangTrong());
        card.getChildren().addAll(tieuDe, hang, cuon, footer);
        return card;
    }

    private VBox daChonPanel() {
        VBox card = Functions.the(14);
        card.setMinHeight(160);
        card.setPrefHeight(200);
        HBox tit = new HBox(8, Functions.tieuDeThe("Môn đã chọn"), Functions.khoangTrong());
        tit.setAlignment(Pos.CENTER_LEFT);
        daChonBox.setFillWidth(true);
        daChonBox.setPrefHeight(110);
        ScrollPane cuon = new ScrollPane(daChonBox);
        cuon.setFitToWidth(true);
        cuon.setPrefHeight(130);
        cuon.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        card.getChildren().addAll(tit, cuon);
        capNhatMonDaChon();
        return card;
    }

    public void taiDuLieu() {
        if (st.hocKy != null) {
            try {
                st.courses = client.listCourses(st.hocKy.ma());
            } catch (com.tkb.frontend.user.net.ApiException ex) {
                Functions.loi(frame.window(), ex);
                st.courses = new ArrayList<>();
            }
            try {
                st.tatCaNhom = client.listAllSections(st.hocKy.ma());
            } catch (com.tkb.frontend.user.net.ApiException ex) {
                Functions.loi(frame.window(), ex);
                st.tatCaNhom = new ArrayList<>();
            }
        }
        locDuLieu = st.courses == null ? new ArrayList<>() : new ArrayList<>(st.courses);
        priorityPanel.taiLai();
        capNhatMonDaChon();
        veBang();
    }

    private List<Course> loc() {
        String q = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return new ArrayList<>(locDuLieu);
        }
        List<Course> ds = new ArrayList<>();
        for (Course c : locDuLieu) {
            String hay = (c.maMh() + " " + c.ten()).toLowerCase(Locale.ROOT);
            if (hay.contains(q)) {
                ds.add(c);
            }
        }
        return ds;
    }

    private void veBang() {
        List<Course> ds = loc();
        bang.getChildren().clear();

        if (ds.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setPadding(new Insets(30));
            empty.setAlignment(Pos.CENTER);
            Label t = Functions.tieuDeThe("Không có môn nào");
            Label m = Functions.moTa("Chưa có dữ liệu học phần hoặc chưa tải được từ backend.");
            empty.getChildren().addAll(t, m);
            bang.getChildren().add(empty);
            lblDem.setText("0 môn");
            phanTrang.getChildren().clear();
            return;
        }

        int tong = ds.size();
        int soTrang = (int) Math.ceil(tong / (double) MOI_TRANG);
        if (trangHienTai >= soTrang) trangHienTai = soTrang - 1;
        int tu = trangHienTai * MOI_TRANG;
        int den = Math.min(tu + MOI_TRANG, tong);
        List<Course> trang = ds.subList(tu, den);

        String tongKet = "Hiển thị " + (tu + 1) + "–" + den + " trong " + tong + " môn";
        lblDem.setText(tongKet);

        for (Course c : trang) {
            bang.getChildren().add(taoDong(c));
        }
        vePhanTrang(soTrang, tong);
        capNhatMonDaChon();
    }

    private void capNhatMonDaChon() {
        daChonBox.getChildren().clear();
        List<String> monChon = st.uuTienMon;
        if (monChon.isEmpty()) {
            daChonBox.getChildren().add(Functions.moTa("Chưa có môn nào được chọn."));
            return;
        }
        for (int i = 0; i < monChon.size(); i++) {
            String ma = monChon.get(i);
            String ten = "";
            int tc = 0;
            for (Course c : st.courses) {
                if (c.maMh().equals(ma)) {
                    ten = c.ten();
                    tc = c.soTc();
                    break;
                }
            }
            VBox card = Functions.the(10);
            card.setStyle(card.getStyle() + " -fx-border-color: #BFDBFE;");

            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            Label badge = Functions.nhanXam("Ưu tiên #" + (i + 1));
            if (i == 0) badge.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 2 6 2 6;");
            else if (i == 1) badge.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 2 6 2 6;");
            Label maLbl = new Label(ma);
            maLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
            Label tenLbl = new Label(ten + " (" + tc + " TC)");
            tenLbl.setStyle("-fx-text-fill: #0F172A;");
            top.getChildren().addAll(badge, maLbl, tenLbl, Functions.khoangTrong());

            Button up = Functions.nutPhu("▲");
            up.setDisable(i == 0);
            int idx = i;
            up.setOnAction(e -> { doiThuTu(idx, idx - 1); });
            Button down = Functions.nutPhu("▼");
            down.setDisable(i == monChon.size() - 1);
            down.setOnAction(e -> { doiThuTu(idx, idx + 1); });
            Button del = Functions.nutPhu("✕");
            del.setOnAction(e -> { boChonMon(ma); });
            top.getChildren().addAll(up, down, del);

            // dropdowns
            javafx.scene.control.ComboBox<String> cbGv = new javafx.scene.control.ComboBox<>();
            cbGv.getItems().add("Tất cả giảng viên");
            for (String gv : danhSachGiangVien(ma)) cbGv.getItems().add(gv);
            String gvHienTai = st.uuTienGiangVien.getOrDefault(ma, "");
            cbGv.setValue(gvHienTai.isBlank() ? "Tất cả giảng viên" : gvHienTai);
            cbGv.setOnAction(e -> st.uuTienGiangVien.put(ma, cbGv.getValue()));

            javafx.scene.control.ComboBox<String> cbNhom = new javafx.scene.control.ComboBox<>();
            cbNhom.getItems().add("Nhóm: Tự động");
            for (String g : danhSachNhom(ma)) cbNhom.getItems().add("Khóa nhóm " + g);
            String nhomHienTai = st.khoaNhom.getOrDefault(ma, "");
            cbNhom.setValue(nhomHienTai.isBlank() ? "Nhóm: Tự động" : "Khóa nhóm " + nhomHienTai);
            cbNhom.setOnAction(e -> {
                String v = cbNhom.getValue();
                st.khoaNhom.put(ma, v.startsWith("Khóa nhóm ") ? v.substring("Khóa nhóm ".length()) : "");
            });

            HBox dropRow = new HBox(10, cbGv, cbNhom);
            card.getChildren().addAll(top, dropRow);
            daChonBox.getChildren().add(card);
        }
    }

    private void doiThuTu(int from, int to) {
        if (from < 0 || to < 0 || from >= st.uuTienMon.size() || to >= st.uuTienMon.size()) return;
        String moved = st.uuTienMon.remove(from);
        st.uuTienMon.add(to, moved);
        capNhatMonDaChon();
    }

    private void boChonMon(String ma) {
        st.uuTienMon.remove(ma);
        // remove selection
        for (SectionSlot slot : st.tatCaNhom) {
            if (slot.section().maMh().equals(ma)) {
                st.daChon.remove(slot.section().id());
            }
        }
        veBang();
        capNhatMonDaChon();
    }

    private java.util.List<String> danhSachGiangVien(String ma) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (SectionSlot slot : st.tatCaNhom) {
            if (slot.section().maMh().equals(ma) && slot.section().giangVien() != null && !slot.section().giangVien().isBlank()) {
                set.add(slot.section().giangVien());
            }
        }
        return new ArrayList<>(set);
    }

    private java.util.List<String> danhSachNhom(String ma) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (SectionSlot slot : st.tatCaNhom) {
            if (slot.section().maMh().equals(ma) && slot.section().nhom() != null && !slot.section().nhom().isBlank()) {
                set.add(slot.section().nhom());
            }
        }
        return new ArrayList<>(set);
    }

    private GridPane taoDong(Course course) {
        GridPane row = new GridPane();
        row.setPadding(new Insets(2, 0, 2, 0));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");

        boolean daChon = monDaChon(course.maMh());
        CheckBox chk = new CheckBox();
        chk.setSelected(daChon);
        chk.setPadding(new Insets(9, 8, 9, 8));
        chk.setAlignment(Pos.CENTER);
        GridPane.setHalignment(chk, javafx.geometry.HPos.CENTER);
        GridPane.setValignment(chk, javafx.geometry.VPos.CENTER);
        chk.setOnAction(e -> {
            chuyenTrangThaiMon(course.maMh(), chk.isSelected());
            veBang();
        });

        VBox maBox = new VBox(2);
        maBox.setPadding(new Insets(9, 8, 9, 8));
        Label ma = new Label(course.maMh());
        ma.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
        maBox.getChildren().add(ma);

        VBox tenBox = new VBox(2);
        tenBox.setPadding(new Insets(9, 8, 9, 8));
        Label ten = new Label(course.ten());
        ten.setWrapText(true);
        ten.setStyle("-fx-text-fill: #0F172A;");
        tenBox.getChildren().add(ten);

        VBox tcBox = new VBox(2);
        tcBox.setPadding(new Insets(9, 8, 9, 8));
        Label tc = new Label(course.soTc() + " tín chỉ");
        tc.setStyle("-fx-text-fill: #1D4ED8; -fx-font-weight: bold;");
        tcBox.getChildren().add(tc);

        row.add(chk, 0, 0);
        row.add(maBox, 1, 0);
        row.add(tenBox, 2, 0);
        row.add(tcBox, 3, 0);

        double[] rongCot = {10, 18, 52, 20};
        for (int i = 0; i < 4; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(rongCot[i]);
            cc.setHgrow(Priority.ALWAYS);
            row.getColumnConstraints().add(cc);
        }

        return row;
    }

    private void chuyenTrangThaiMon(String maMh, boolean chon) {
        for (SectionSlot slot : st.tatCaNhom) {
            if (slot.section().maMh().equals(maMh)) {
                if (chon) {
                    st.daChon.add(slot.section().id());
                } else {
                    st.daChon.remove(slot.section().id());
                }
            }
        }
        if (chon) {
            if (!st.uuTienMon.contains(maMh)) st.uuTienMon.add(maMh);
        } else {
            st.uuTienMon.remove(maMh);
            st.uuTienGiangVien.remove(maMh);
            st.khoaNhom.remove(maMh);
        }
    }

    private boolean monDaChon(String maMh) {
        for (SectionSlot slot : st.tatCaNhom) {
            if (slot.section().maMh().equals(maMh) && st.daChon.contains(slot.section().id())) {
                return true;
            }
        }
        return false;
    }

    private void vePhanTrang(int soTrang, int tong) {
        phanTrang.getChildren().clear();
        Button truoc = Functions.nutPhu("‹");
        truoc.setDisable(trangHienTai == 0);
        truoc.setOnAction(e -> {
            trangHienTai--;
            veBang();
        });
        phanTrang.getChildren().add(truoc);
        for (int i = 0; i < soTrang; i++) {
            int tr = i;
            Button b = Functions.nutPhu(String.valueOf(i + 1));
            if (i == trangHienTai) {
                b.setStyle("-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-border-color: #1D4ED8; -fx-border-radius: 8; -fx-background-radius: 8;");
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
        phanTrang.getChildren().addAll(sau, Functions.nhanXam(tong + " môn"));
    }

    private void datLaiBoLoc() {
        txtTimKiem.clear();
        trangHienTai = 0;
        veBang();
    }
}
