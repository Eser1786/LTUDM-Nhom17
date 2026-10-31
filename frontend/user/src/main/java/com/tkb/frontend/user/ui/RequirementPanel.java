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
        listCard.getChildren().addAll(theBoLoc(), theBang());
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

    private VBox theBoLoc() {
        VBox card = Functions.the(14);
        txtTimKiem.setPromptText("Tìm theo mã hoặc tên môn...");
        txtTimKiem.setPrefWidth(320);

        HBox row = new HBox(10, txtTimKiem, Functions.khoangTrong(), btnDatLai);
        row.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(row);

        btnDatLai.setOnAction(e -> datLaiBoLoc());
        txtTimKiem.textProperty().addListener((o, a, b) -> {
            trangHienTai = 0;
            veBang();
        });
        return card;
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
        try {
            if (st.hocKy != null) {
                st.tatCaNhom = client.listAllSections(st.hocKy.ma());
                st.courses = client.listCourses(st.hocKy.ma());
            }
        } catch (com.tkb.frontend.user.net.ApiException ex) {
            Functions.loi(frame.window(), ex);
            return;
        }
        locDuLieu = st.courses;
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
            Label t = Functions.tieuDeThe("Không có môn nào phù hợp");
            Label m = Functions.moTa("Hãy thử tìm kiếm theo mã hoặc tên môn khác.");
            Button reset = Functions.nutPhu("Đặt lại bộ lọc");
            reset.setOnAction(e -> datLaiBoLoc());
            empty.getChildren().addAll(t, m, reset);
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
        List<String> monChon = new ArrayList<>();
        for (SectionSlot slot : st.tatCaNhom) {
            if (st.daChon.contains(slot.section().id()) && !monChon.contains(slot.section().maMh())) {
                monChon.add(slot.section().maMh());
            }
        }
        if (monChon.isEmpty()) {
            daChonBox.getChildren().add(Functions.moTa("Chưa có môn nào được chọn."));
            return;
        }
        for (String ma : monChon) {
            String ten = "";
            for (Course c : st.courses) {
                if (c.maMh().equals(ma)) {
                    ten = c.ten();
                    break;
                }
            }
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle("-fx-background-color: #DBEAFE; -fx-background-radius: 10; -fx-border-radius: 10;");

            Label maLbl = new Label(ma);
            maLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
            Label tenLbl = new Label("• " + ten);
            tenLbl.setWrapText(true);
            tenLbl.setStyle("-fx-text-fill: #1E3A8A;");
            row.getChildren().addAll(maLbl, tenLbl);
            daChonBox.getChildren().add(row);
        }
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
