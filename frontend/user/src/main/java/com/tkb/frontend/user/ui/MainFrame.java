package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.Course;
import com.tkb.frontend.user.model.Types.JobResult;
import com.tkb.frontend.user.model.Types.PlanRequest;
import com.tkb.frontend.user.model.Types.ScheduleOption;
import com.tkb.frontend.user.model.Types.SectionSlot;
import com.tkb.frontend.user.model.Types.Semester;
import com.tkb.frontend.user.model.Types.Student;
import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.MockNetworkClient;
import com.tkb.frontend.user.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends BorderPane {
    // Main shell of the app: owns the sidebar, header and the card stack that switches screens.
    static final String CARD_LOGIN = "LOGIN";
    static final String CARD_OVERVIEW = "TONG_QUAN";
    static final String CARD_REQ = "NHOM_LOP";
    static final String CARD_PRIO = "UU_TIEN";
    static final String CARD_JOB = "JOB";
    static final String CARD_RES = "KET_QUA";
    static final String CARD_CMP = "SO_SANH";
    static final String CARD_SAVED = "DA_LUU";

    private static final Map<String, String> TEN_TRANG = Map.of(
            CARD_OVERVIEW, "Tá»•ng quan",
            CARD_REQ, "NhÃ³m lá»›p há»c pháº§n",
            CARD_PRIO, "Thiáº¿t láº­p Æ°u tiÃªn",
            CARD_JOB, "Äang xá»­ lÃ½",
            CARD_RES, "Káº¿t quáº£ Ä‘á» xuáº¥t",
            CARD_CMP, "So sÃ¡nh lá»‹ch",
            CARD_SAVED, "Thá»i khÃ³a biá»ƒu Ä‘Ã£ lÆ°u");

    private final TkbClient client = new MockNetworkClient();
    private final AppUiState trangThai = new AppUiState(client);
    private final Map<String, Node> cards = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final StackPane centerStack = new StackPane();
    private final Label crumb = new Label("Trang chá»§");
    private final Label lblSemSidebar = new Label("â€”");
    private final Label lblSemNgaySidebar = new Label("â€”");
    private final Label lblHeaderSem = new Label();
    private final Label lblHeaderTen = new Label();
    private final Label lblHeaderMssv = new Label();
    private final AvatarPanel avatar = new AvatarPanel("?", 38);
    private final OverviewPanel panelOverview;
    private final RequirementPanel panelReq;
    private final PriorityPanel panelPrio;
    private final JobPanel panelJob;
    private final ResultPanel panelRes;
    private final SavedPlansPanel panelSaved;
    private final ComparePanel panelCmp;
    private final VBox sidebar = new VBox(6);
    private String jobId;
    private String trangSoSanhQuayLai = CARD_SAVED;

    public MainFrame() {
        // Build the shell and register each panel under a card key so navigation can switch scenes quickly.
        setPrefSize(1360, 880);
        setStyle("-fx-background-color: #F6F8FB;");
        panelOverview = new OverviewPanel(this);
        panelReq = new RequirementPanel(client, this);
        panelPrio = new PriorityPanel(this);
        panelJob = new JobPanel(client, this);
        panelRes = new ResultPanel(client, this);
        panelSaved = new SavedPlansPanel(client, this);
        panelCmp = new ComparePanel(this);

        setLeft(taoSidebar());

        HBox header = taoHeader();
        setTop(header);

        cards.put(CARD_OVERVIEW, panelOverview);
        cards.put(CARD_REQ, panelReq);
        cards.put(CARD_PRIO, panelPrio);
        cards.put(CARD_JOB, panelJob);
        cards.put(CARD_RES, panelRes);
        cards.put(CARD_CMP, panelCmp);
        cards.put(CARD_SAVED, panelSaved);

        setCenter(centerStack);
        hienLogin();
    }

    // ------------------------------------------------------------------ sidebar

    private VBox taoSidebar() {
        sidebar.setPrefWidth(252);
        sidebar.setMinWidth(252);
        sidebar.setPadding(new Insets(16, 12, 14, 12));
        sidebar.setStyle("-fx-background-color: #0F172A;");

        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        Label logo = new Label("P15");
        logo.setMinSize(40, 40);
        logo.setAlignment(Pos.CENTER);
        logo.setTextFill(Color.WHITE);
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        logo.setStyle("-fx-background-color: #1D4ED8; -fx-border-radius: 10; -fx-background-radius: 10;");
        VBox brand = new VBox(1);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label l1 = new Label("Tá»‘i Æ°u thá»i khÃ³a biá»ƒu");
        l1.setTextFill(Color.WHITE);
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        Label l2 = new Label("Project 15 Â· Sinh viÃªn");
        l2.setTextFill(Color.web("#94A3B8"));
        l2.setFont(Font.font("Segoe UI", 11));
        brand.getChildren().addAll(l1, l2);
        logoBox.getChildren().addAll(logo, brand);

        Label nhom1 = nhanSidebar("KHÃ”NG GIAN SINH VIÃŠN");
        Button overviewBtn = taoNavItem(CARD_OVERVIEW, "Tá»•ng quan");
        Button reqBtn = taoNavItem(CARD_REQ, "NhÃ³m lá»›p há»c pháº§n");
        Button prioBtn = taoNavItem(CARD_PRIO, "Tá»‘i Æ°u thá»i khÃ³a biá»ƒu");
        Button resBtn = taoNavItem(CARD_RES, "Káº¿t quáº£ Ä‘á» xuáº¥t");
        Button cmpBtn = taoNavItem(CARD_CMP, "So sÃ¡nh lá»‹ch");
        Button savedBtn = taoNavItem(CARD_SAVED, "Thá»i khÃ³a biá»ƒu Ä‘Ã£ lÆ°u");

        Label nhom2 = nhanSidebar("Há»ŒC Ká»² HIá»†N Táº I");
        VBox semBox = new VBox(2);
        semBox.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-border-radius: 10;");
        semBox.setPadding(new Insets(10, 12, 10, 12));
        lblSemSidebar.setTextFill(Color.WHITE);
        lblSemSidebar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblSemSidebar.setWrapText(true);
        lblSemNgaySidebar.setTextFill(Color.web("#94A3B8"));
        lblSemNgaySidebar.setFont(Font.font("Segoe UI", 11));
        lblSemNgaySidebar.setWrapText(true);
        semBox.getChildren().addAll(lblSemSidebar, lblSemNgaySidebar);

        Button infoBtn = nutSidebarPhu("Dá»¯ liá»‡u há»c pháº§n minh há»a");
        infoBtn.setOnAction(e -> Functions.thongTin(window(),
                "Báº£n demo dÃ¹ng dá»¯ liá»‡u há»c pháº§n giáº£ láº­p cho cÃ¡c há»c pháº§n vÃ  nhÃ³m lá»›p.\n"
                        + "ChÆ°a káº¿t ná»‘i mÃ¡y chá»§ tháº­t â€” toÃ n bá»™ káº¿t quáº£ mang tÃ­nh mÃ´ phá»ng."));
        Button helpBtn = nutSidebarPhu("HÆ°á»›ng dáº«n sá»­ dá»¥ng");
        helpBtn.setOnAction(e -> Functions.thongTin(window(),
                "1. Chá»n nhÃ³m lá»›p há»c pháº§n (ghÃ©p LT vá»›i TH tÆ°Æ¡ng á»©ng).\n"
                        + "2. Thiáº¿t láº­p má»©c Æ°u tiÃªn.\n"
                        + "3. Cháº¡y tá»‘i Æ°u vÃ  xem cÃ¡c phÆ°Æ¡ng Ã¡n A/B/C.\n"
                        + "4. LÆ°u hoáº·c so sÃ¡nh cÃ¡c lá»‹ch phÃ¹ há»£p."));

        sidebar.getChildren().addAll(logoBox, nhom1, overviewBtn, reqBtn, prioBtn, resBtn, cmpBtn, savedBtn,
                nhom2, semBox, Functions.khoangTrong(), infoBtn, helpBtn);
        return sidebar;
    }

    private Label nhanSidebar(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setTextFill(Color.web("#64748B"));
        l.setPadding(new Insets(10, 4, 2, 6));
        return l;
    }

    private Button nutSidebarPhu(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", 12));
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-alignment: center-left;");
        return b;
    }

    private Button taoNavItem(String card, String text) {
        // Each navigation button maps to an existing screen card and updates the active styling.
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font("Segoe UI", 12.5));
        b.setPadding(new Insets(9, 10, 9, 10));
        b.setStyle(navStyle(false));
        b.setOnAction(e -> denTrang(card));
        navButtons.put(card, b);
        return b;
    }

    private String navStyle(boolean chon) {
        if (chon) {
            return "-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-background-radius: 8; "
                    + "-fx-alignment: center-left; -fx-cursor: hand;";
        }
        return "-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-background-radius: 8; "
                + "-fx-alignment: center-left; -fx-cursor: hand;";
    }

    // ------------------------------------------------------------------ header

    private HBox taoHeader() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(0, 18, 0, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        header.setPrefHeight(66);
        header.setMinHeight(66);

        Button hamburger = new Button("â˜°");
        hamburger.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; "
                + "-fx-text-fill: #1E293B; -fx-font-size: 16; -fx-cursor: hand;");
        hamburger.setOnAction(e -> {
            if (getLeft() == null) {
                setLeft(sidebar);
            } else {
                setLeft(null);
            }
        });
        hamburger.setTooltip(new javafx.scene.control.Tooltip("Thu gá»n / má»Ÿ rá»™ng menu"));
        crumb.setTextFill(Color.web("#64748B"));
        crumb.setFont(Font.font("Segoe UI", 12));
        header.getChildren().addAll(hamburger, crumb);

        HBox right = new HBox(12);
        right.setAlignment(Pos.CENTER_RIGHT);
        lblHeaderSem.setTextFill(Color.web("#1D4ED8"));
        lblHeaderSem.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblHeaderSem.setStyle("-fx-background-color: #DBEAFE; -fx-padding: 5 10 5 10; -fx-background-radius: 999;");
        VBox identity = new VBox(1);
        identity.setAlignment(Pos.CENTER_LEFT);
        lblHeaderTen.setTextFill(Color.web("#0F172A"));
        lblHeaderTen.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeaderMssv.setTextFill(Color.web("#64748B"));
        lblHeaderMssv.setFont(Font.font("Segoe UI", 11));
        identity.getChildren().addAll(lblHeaderTen, lblHeaderMssv);
        right.getChildren().addAll(lblHeaderSem, identity, avatar);
        header.getChildren().addAll(Functions.khoangTrong(), right);
        return header;
    }

    // ------------------------------------------------------------------ navigation

    void denTrang(String card) {
        // Switch the visible card and refresh any screen-specific data that should be loaded on demand.
        Node node = cards.getOrDefault(card, panelOverview);
        centerStack.getChildren().setAll(node);
        crumb.setText("Trang chá»§ / " + TEN_TRANG.getOrDefault(card, ""));
        for (Map.Entry<String, Button> e : navButtons.entrySet()) {
            e.getValue().setStyle(navStyle(e.getKey().equals(card)));
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

    private void hienLogin() {
        // Show the login screen and disable sidebar navigation until the user signs in.
        crum(true);
        centerStack.getChildren().setAll(new LoginPanel(trangThai, this::sauDangNhap));
        crumb.setText("ÄÄƒng nháº­p");
    }

    private void crum(boolean an) {
        // Sidebar is hidden while on the login screen.
        if (an) {
            setLeft(null);
        } else if (getLeft() == null) {
            setLeft(sidebar);
        }
    }

    private void sauDangNhap() {
        // After login, load student, semester and load the initial requirement data.
        try {
            trangThai.sinhVien = client.sinhVien();
            trangThai.semesters = client.listSemesters();
            trangThai.hocKy = null;
            for (Semester s : trangThai.semesters) {
                if (s.dangDung()) {
                    trangThai.hocKy = s;
                    break;
                }
            }
            if (trangThai.hocKy == null && !trangThai.semesters.isEmpty()) {
                trangThai.hocKy = trangThai.semesters.get(trangThai.semesters.size() - 1);
            }
            trangThai.courses = client.listCourses(trangThai.hocKy.ma());
            panelReq.taiDuLieu();
            trangThai.chonMacDinh();
        } catch (ApiException ex) {
            Functions.loi(window(), ex);
            return;
        }
        crum(false);
        capNhatHeader();
        capNhatSidebarHocKy();
        denTrang(CARD_OVERVIEW);
    }

    private void capNhatHeader() {
        Student sv = trangThai.sinhVien;
        if (sv == null) return;
        lblHeaderTen.setText(sv.hoTen());
        lblHeaderMssv.setText(sv.mssv() + " Â· " + sv.lop());
        avatar.setChu(sv.chuDau());
        lblHeaderSem.setText("Há»c ká»³ I Â· 2026â€“2027");
    }

    private void capNhatSidebarHocKy() {
        Semester s = trangThai.hocKy;
        if (s == null) return;
        lblSemSidebar.setText("Há»c ká»³ I Â· 2026â€“2027");
        lblSemNgaySidebar.setText("Báº¯t Ä‘áº§u " + s.ngayBatDau() + "\nKáº¿t thÃºc " + s.ngayKetThuc());
    }

    static String nganHocKy(Semester s) {
        if (s == null) return "â€”";
        return s.ten().replace(" - NÄƒm há»c ", " Â· ");
    }

    // ------------------------------------------------------------------ flow

    void batDauXepLich() {
        // Submit the selected class set and start the background scheduling process.
        if (trangThai.hocKy == null) {
            Functions.canhBao(window(), "Vui lÃ²ng chá»n há»c ká»³.");
            return;
        }
        if (trangThai.daChon.isEmpty()) {
            Functions.canhBao(window(), "HÃ£y chá»n Ã­t nháº¥t má»™t nhÃ³m lá»›p há»c pháº§n.");
            denTrang(CARD_REQ);
            return;
        }
        try {
            PlanRequest req = new PlanRequest(trangThai.hocKy.ma(), trangThai.monDaChon(),
                    trangThai.criteria, new ArrayList<>(trangThai.daChon));
            jobId = client.submit(req);
            panelJob.batDau(jobId, this::jobHuy);
            denTrang(CARD_JOB);
        } catch (ApiException ex) {
            Functions.loi(window(), ex);
        }
    }

    void jobXong(JobResult result) {
        // Once the scheduling job is done, display the generated result options to the user.
        trangThai.ketQua = result;
        panelRes.hienKetQua(result);
        denTrang(CARD_RES);
    }

    void jobHuy() {
        // The schedule computation can be canceled without resetting the user's selection choices.
        Functions.thongTin(window(), "ÄÃ£ há»§y xáº¿p lá»‹ch. CÃ¡c lá»±a chá»n cá»§a báº¡n váº«n Ä‘Æ°á»£c giá»¯ nguyÃªn.");
        denTrang(CARD_PRIO);
    }

    void jobThatBai(String message) {
        // Show the failure message then route the user back to the priority configuration step.
        if (message != null && !message.isBlank()) {
            Functions.loi(window(), message);
        }
        denTrang(CARD_PRIO);
    }

    void moSoSanhPhuongAn(String quayLaiCard, List<ScheduleOption> ds) {
        // Compare selected result options side by side using a dedicated comparison view.
        trangSoSanhQuayLai = quayLaiCard;
        panelCmp.napPhuongAn(ds);
        denTrang(CARD_CMP);
    }

    void moSoSanhDaLuu(String quayLaiCard, List<Long> ids) {
        // Comparison can also be opened from saved plans using the selected saved-plan IDs.
        trangSoSanhQuayLai = quayLaiCard;
        panelCmp.napDaLuu(ids);
        denTrang(CARD_CMP);
    }

    void trangSoSanhQuayLai() {
        String card = trangSoSanhQuayLai == null ? CARD_SAVED : trangSoSanhQuayLai;
        denTrang(card);
    }

    Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    AppUiState trangThai() {
        return trangThai;
    }

    // ------------------------------------------------------------------ state

    static final class AppUiState {
        final TkbClient client;
        Student sinhVien;
        List<Semester> semesters = List.of();
        Semester hocKy;
        List<Course> courses = List.of();
        Criteria criteria = Criteria.macDinh();
        JobResult ketQua;
        List<SectionSlot> tatCaNhom = new ArrayList<>();
        final Set<Integer> daChon = new LinkedHashSet<>();

        AppUiState(TkbClient client) {
            this.client = client;
        }

        void chonMacDinh() {
            // Pre-select the demo course combination used by the mock data so the flow can be tried immediately.
            try {
                tatCaNhom = hocKy == null ? List.of() : client.listAllSections(hocKy.ma());
            } catch (ApiException ex) {
                tatCaNhom = List.of();
                return;
            }
            daChon.clear();
            daChon.addAll(Bo());
        }

        /** Bá»™ nhÃ³m lá»›p minh há»a: 4 há»c pháº§n 12 tÃ­n chá»‰, tÆ°Æ¡ng á»©ng phÆ°Æ¡ng Ã¡n A. */
        private List<Integer> Bo() {
            List<Integer> ids = new ArrayList<>();
            for (SectionSlot s : tatCaNhom) {
                String ma = s.section().maMh();
                String nhom = s.section().nhom();
                String to = s.section().toTh();
                if (("841109".equals(ma) || "841215".equals(ma)) && "01".equals(nhom)) {
                    ids.add(s.section().id());
                } else if ("841401".equals(ma) && "01".equals(nhom)) {
                    ids.add(s.section().id());
                } else if ("841501".equals(ma) && "01".equals(nhom)) {
                    ids.add(s.section().id());
                }
            }
            return ids;
        }

        List<String> monDaChon() {
            // Unique course codes for the currently selected sections, preserving demo priority order.
            List<String> out = new ArrayList<>();
            for (String ma : new String[]{"841109", "841215", "841401", "841501"}) {
                for (SectionSlot s : tatCaNhom) {
                    if (s.section().maMh().equals(ma) && daChon.contains(s.section().id())) {
                        out.add(ma);
                        break;
                    }
                }
            }
            if (out.isEmpty()) {
                for (SectionSlot s : tatCaNhom) {
                    if (daChon.contains(s.section().id()) && !out.contains(s.section().maMh())) {
                        out.add(s.section().maMh());
                    }
                }
            }
            return out;
        }
    }
}
