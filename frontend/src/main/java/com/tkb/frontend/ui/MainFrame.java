package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.Criteria;
import com.tkb.frontend.model.Types.JobResult;
import com.tkb.frontend.model.Types.PlanRequest;
import com.tkb.frontend.model.Types.ScheduleOption;
import com.tkb.frontend.model.Types.SectionSlot;
import com.tkb.frontend.model.Types.Semester;
import com.tkb.frontend.model.Types.Student;
import com.tkb.frontend.net.ApiException;
import com.tkb.frontend.net.MockNetworkClient;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends BorderPane {
    static final String CARD_LOGIN = "LOGIN";
    static final String CARD_APP = "APP";
    static final String CARD_OVERVIEW = "TONG_QUAN";
    static final String CARD_REQ = "NHOM_LOP";
    static final String CARD_PRIO = "UU_TIEN";
    static final String CARD_JOB = "JOB";
    static final String CARD_RES = "KET_QUA";
    static final String CARD_CMP = "SO_SANH";
    static final String CARD_SAVED = "DA_LUU";

    private static final Map<String, String> TEN_TRANG = Map.of(
            CARD_OVERVIEW, "Tổng quan",
            CARD_REQ, "Nhóm lớp học phần",
            CARD_PRIO, "Thiết lập ưu tiên",
            CARD_JOB, "Đang xử lý",
            CARD_RES, "Kết quả đề xuất",
            CARD_CMP, "So sánh lịch",
            CARD_SAVED, "Thời khóa biểu đã lưu");

    private final TkbClient client = new MockNetworkClient();
    private final AppUiState trangThai = new AppUiState(client);
    private final Map<String, Node> cards = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final StackPane centerStack = new StackPane();
    private final Label crumb = new Label("Trang chủ");
    private final Label lblSemSidebar = new Label("—");
    private final Label lblSemNgaySidebar = new Label("—");
    private final Label lblHeaderSem = new Label();
    private final Label lblHeaderTen = new Label();
    private final Label lblHeaderMssv = new Label();
    private final AvatarPanel avatar = new AvatarPanel("?", 36);
    private final OverviewPanel panelOverview;
    private final RequirementPanel panelReq;
    private final PriorityPanel panelPrio;
    private final JobPanel panelJob;
    private final ResultPanel panelRes;
    private final SavedPlansPanel panelSaved;
    private final ComparePanel panelCmp;
    private String jobId;
    private String trangSoSanhQuayLai = CARD_SAVED;

    public MainFrame() {
        setPrefSize(1360, 860);
        setStyle("-fx-background-color: #F6F8FB;");
        panelOverview = new OverviewPanel(this);
        panelReq = new RequirementPanel(client, this);
        panelPrio = new PriorityPanel(this);
        panelJob = new JobPanel(client, this);
        panelRes = new ResultPanel(client, this);
        panelSaved = new SavedPlansPanel(client, this);
        panelCmp = new ComparePanel(client, this);

        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(244);
        sidebar.setPadding(new Insets(14, 10, 12, 10));
        sidebar.setStyle("-fx-background-color: #0F172A;");

        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        Label logo = new Label("P15");
        logo.setMinSize(38, 38);
        logo.setAlignment(Pos.CENTER);
        logo.setTextFill(Color.WHITE);
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        logo.setStyle("-fx-background-color: #1D4ED8; -fx-border-radius: 9; -fx-background-radius: 9;");
        VBox brand = new VBox(2);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label l1 = new Label("Tối ưu thời khóa biểu");
        l1.setTextFill(Color.WHITE);
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        Label l2 = new Label("Project 15");
        l2.setTextFill(Color.web("#94A3B8"));
        l2.setFont(Font.font("Segoe UI", 11));
        brand.getChildren().addAll(l1, l2);
        logoBox.getChildren().addAll(logo, brand);
        sidebar.getChildren().add(logoBox);

        Button overviewBtn = taoNavItem(CARD_OVERVIEW, "Tổng quan");
        Button reqBtn = taoNavItem(CARD_REQ, "Nhóm lớp học phần");
        Button prioBtn = taoNavItem(CARD_PRIO, "Tối ưu thời khóa biểu");
        Button resBtn = taoNavItem(CARD_RES, "Kết quả đề xuất");
        Button cmpBtn = taoNavItem(CARD_CMP, "So sánh lịch");
        Button savedBtn = taoNavItem(CARD_SAVED, "Thời khóa biểu đã lưu");
        sidebar.getChildren().addAll(overviewBtn, reqBtn, prioBtn, resBtn, cmpBtn, savedBtn);

        VBox semBox = new VBox(0);
        semBox.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #1E293B;");
        semBox.setPadding(new Insets(8, 10, 8, 10));
        lblSemSidebar.setTextFill(Color.WHITE);
        lblSemSidebar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblSemNgaySidebar.setTextFill(Color.web("#94A3B8"));
        lblSemNgaySidebar.setFont(Font.font("Segoe UI", 11));
        semBox.getChildren().addAll(lblSemSidebar, lblSemNgaySidebar);
        sidebar.getChildren().add(semBox);

        Button infoBtn = new Button("Dữ liệu học phần minh họa");
        infoBtn.setMaxWidth(Double.MAX_VALUE);
        infoBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-alignment: center-left;");
        infoBtn.setOnAction(e -> Functions.thongTin(getScene() == null ? null : getScene().getWindow(), "Bản demo dùng dữ liệu học phần giả lập cho các học kỳ-nhóm."));
        Button helpBtn = new Button("Hướng dẫn sử dụng");
        helpBtn.setMaxWidth(Double.MAX_VALUE);
        helpBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-alignment: center-left;");
        helpBtn.setOnAction(e -> Functions.thongTin(getScene() == null ? null : getScene().getWindow(), "1. Chọn nhóm lớp học phần\n2. Thiết lập ưu tiên\n3. Chạy tối ưu và xem các phương án A/B/C."));
        sidebar.getChildren().addAll(infoBtn, helpBtn);

        HBox header = new HBox(12);
        header.setPadding(new Insets(0, 12, 0, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        header.setPrefHeight(64);

        Button hamburger = new Button("☰");
        hamburger.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #1E293B;");
        hamburger.setOnAction(e -> {
            if (getLeft() == null) {
                setLeft(sidebar);
            } else {
                setLeft(null);
            }
        });
        crumb.setTextFill(Color.web("#64748B"));
        crumb.setFont(Font.font("Segoe UI", 12));
        header.getChildren().addAll(hamburger, crumb);

        HBox right = new HBox(10);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPrefWidth(300);
        lblHeaderSem.setTextFill(Color.web("#64748B"));
        lblHeaderSem.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        VBox identity = new VBox(2);
        identity.setAlignment(Pos.CENTER_LEFT);
        lblHeaderTen.setTextFill(Color.web("#0F172A"));
        lblHeaderTen.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeaderMssv.setTextFill(Color.web("#64748B"));
        lblHeaderMssv.setFont(Font.font("Segoe UI", 11));
        identity.getChildren().addAll(lblHeaderTen, lblHeaderMssv);
        right.getChildren().addAll(lblHeaderSem, identity, avatar);
        header.getChildren().add(right);
        setTop(header);
        setLeft(sidebar);

        cards.put(CARD_OVERVIEW, panelOverview);
        cards.put(CARD_REQ, panelReq);
        cards.put(CARD_PRIO, panelPrio);
        cards.put(CARD_JOB, panelJob);
        cards.put(CARD_RES, panelRes);
        cards.put(CARD_CMP, panelCmp);
        cards.put(CARD_SAVED, panelSaved);
        cards.put(CARD_LOGIN, new LoginPanel(trangThai, this::sauDangNhap));

        centerStack.getChildren().add(new LoginPanel(trangThai, this::sauDangNhap));
        setCenter(centerStack);
        denTrang(CARD_LOGIN);
    }

    private Button taoNavItem(String card, String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-padding: 10 8 10 8; -fx-alignment: center-left;");
        b.setOnAction(e -> denTrang(card));
        navButtons.put(card, b);
        return b;
    }

    void denTrang(String card) {
        Node node = cards.getOrDefault(card, panelOverview);
        centerStack.getChildren().setAll(node);
        crumb.setText("Trang chủ / " + TEN_TRANG.getOrDefault(card, ""));
        for (Map.Entry<String, Button> e : navButtons.entrySet()) {
            Button b = e.getValue();
            if (e.getKey().equals(card)) {
                b.setStyle("-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-padding: 10 8 10 8; -fx-alignment: center-left;");
            } else {
                b.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-padding: 10 8 10 8; -fx-alignment: center-left;");
            }
        }
        switch (card) {
            case CARD_OVERVIEW -> panelOverview.taiLai();
            case CARD_REQ -> panelReq.taiDuLieu();
            case CARD_PRIO -> panelPrio.taiLai();
            case CARD_SAVED -> panelSaved.taiDanhSach();
            case CARD_CMP -> panelCmp.taiDuLieu();
            case CARD_RES -> panelRes.hienTrangThai();
            default -> {}
        }
    }

    private void sauDangNhap() {
        try {
            trangThai.sinhVien = client.sinhVien();
            trangThai.semesters = client.listSemesters();
            for (Semester s : trangThai.semesters) {
                if (s.dangDung()) {
                    trangThai.hocKy = s;
                    break;
                }
            }
        } catch (ApiException ex) {
            Functions.loi(getScene() == null ? null : getScene().getWindow(), ex);
            return;
        }
        capNhatHeader();
        capNhatSidebarHocKy();
        panelReq.taiDuLieu();
        trangThai.chonMacDinh();
        denTrang(CARD_OVERVIEW);
    }

    private void capNhatHeader() {
        Student sv = trangThai.sinhVien;
        if (sv == null) return;
        lblHeaderTen.setText(sv.hoTen());
        lblHeaderMssv.setText(sv.mssv() + " · " + sv.lop());
        avatar.setChu(sv.chuDau());
        lblHeaderSem.setText(nganHocKy(trangThai.hocKy));
    }

    private void capNhatSidebarHocKy() {
        Semester s = trangThai.hocKy;
        if (s == null) return;
        lblSemSidebar.setText(nganHocKy(s));
        lblSemNgaySidebar.setText(s.ngayBatDau() + " – " + s.ngayKetThuc());
    }

    static String nganHocKy(Semester s) {
        if (s == null) return "—";
        return s.ten().replace(" - Năm học ", " · ");
    }

    void batDauXepLich() {
        if (trangThai.hocKy == null) {
            Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Vui lòng chọn học kỳ.");
            return;
        }
        if (trangThai.daChon.isEmpty()) {
            Functions.canhBao(getScene() == null ? null : getScene().getWindow(), "Hãy chọn ít nhất một nhóm lớp học phần.");
            denTrang(CARD_REQ);
            return;
        }
        try {
            PlanRequest req = new PlanRequest(trangThai.hocKy.ma(), trangThai.monDaChon(), trangThai.criteria, new ArrayList<>(trangThai.daChon));
            jobId = client.submit(req);
            panelJob.batDau(jobId);
            denTrang(CARD_JOB);
        } catch (ApiException ex) {
            Functions.loi(getScene() == null ? null : getScene().getWindow(), ex);
        }
    }

    void jobXong(JobResult result) {
        trangThai.ketQua = result;
        panelRes.hienKetQua(result);
        denTrang(CARD_RES);
    }

    void jobHuy() {
        Functions.thongTin(getScene() == null ? null : getScene().getWindow(), "Đã hủy xếp lịch. Các lựa chọn của bạn vẫn được giữ nguyên.");
        denTrang(CARD_PRIO);
    }

    void jobThatBai(String message) {
        if (message != null && !message.isBlank()) {
            Functions.loi(getScene() == null ? null : getScene().getWindow(), message);
        }
        denTrang(CARD_PRIO);
    }

    void moSoSanhPhuongAn(String quayLaiCard, List<ScheduleOption> ds) {
        trangSoSanhQuayLai = quayLaiCard;
        panelCmp.napPhuongAn(ds);
        denTrang(CARD_CMP);
    }

    void moSoSanhDaLuu(String quayLaiCard, List<Long> ids) {
        trangSoSanhQuayLai = quayLaiCard;
        panelCmp.napDaLuu(ids);
        denTrang(CARD_CMP);
    }

    String trangSoSanhQuayLai() { return trangSoSanhQuayLai; }

    AppUiState trangThai() { return trangThai; }

    public static void main(String[] args) {
        // no-op; launched by JavaFX entry point
    }
}

class AppUiState {
    static final Set<String> MON_MINH_HOA = Set.of("841109", "841215", "841401", "841501");
    final TkbClient client;
    Student sinhVien;
    List<Semester> semesters = List.of();
    Semester hocKy;
    List<SectionSlot> tatCaNhom = List.of();
    final Set<Integer> daChon = new LinkedHashSet<>();
    Criteria criteria = Criteria.macDinh();
    JobResult ketQua;

    AppUiState(TkbClient client) {
        this.client = client;
    }

    void chonMacDinh() {
        if (!daChon.isEmpty()) return;
        for (SectionSlot s : tatCaNhom) {
            if (MON_MINH_HOA.contains(s.section().maMh())) {
                daChon.add(s.section().id());
            }
        }
    }

    List<String> monDaChon() {
        return new ArrayList<>(new LinkedHashMap<String, List<SectionSlot>>().keySet());
    }
}

class AvatarPanel extends StackPane {
    private String chu;
    private final int kichThuoc;

    AvatarPanel(String chu, int kichThuoc) {
        this.chu = chu;
        this.kichThuoc = kichThuoc;
        setMinSize(kichThuoc, kichThuoc);
        setPrefSize(kichThuoc, kichThuoc);
        setMaxSize(kichThuoc, kichThuoc);
        setAlignment(Pos.CENTER);
        render();
    }

    void setChu(String chu) {
        this.chu = chu;
        render();
    }

    private void render() {
        Circle circle = new Circle(kichThuoc / 2.0, Color.web("#1D4ED8"));
        Label label = new Label(chu == null ? "?" : chu);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, kichThuoc / 3));
        getChildren().setAll(circle, label);
    }
}

class LoginPanel extends VBox {
    private final TextField txtEmail = Functions.oNhap();
    private final PasswordField txtMatKhau = new PasswordField();

    LoginPanel(AppUiState st, Runnable onLogin) {
        setPadding(new Insets(32));
        setSpacing(14);
        setStyle("-fx-background-color: white; -fx-padding: 28 30 28 30;");
        Label title = Functions.chuDe("Đăng nhập");
        Label desc = Functions.moTa("Thông tin demo");
        txtEmail.setText("minh.anh@sv.univ.edu.vn");
        txtMatKhau.setText("demo1234");
        txtMatKhau.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 7 10 7 10;");
        Button login = Functions.nutChinh("Đăng nhập");
        login.setOnAction(e -> onLogin.run());
        getChildren().addAll(title, desc, txtEmail, txtMatKhau, login);
    }
}

class OverviewPanel extends VBox {
    private final MainFrame frame;
    OverviewPanel(MainFrame frame) {
        this.frame = frame;
        setPadding(new Insets(24));
        setSpacing(16);
        getChildren().add(Functions.chuDe("Tổng quan"));
    }
    void taiLai() {}
}

class PriorityPanel extends VBox {
    private final MainFrame frame;
    PriorityPanel(MainFrame frame) { this.frame = frame; setPadding(new Insets(24)); }
    void taiLai() {}
}

class ComparePanel extends VBox {
    private final MainFrame frame;
    ComparePanel(MainFrame frame) { this.frame = frame; setPadding(new Insets(24)); }
    void taiDuLieu() {}
    void napPhuongAn(List<ScheduleOption> ds) {}
    void napDaLuu(List<Long> ids) {}
}
