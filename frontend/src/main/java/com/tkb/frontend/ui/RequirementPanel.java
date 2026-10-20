package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.ClassSession;
import com.tkb.frontend.model.Types.SectionSlot;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequirementPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final AppUiState st;
    private final TextField txtTimKiem = Functions.oNhap();
    private final ComboBox<String> cbLoai = new ComboBox<>();
    private final ComboBox<String> cbNgay = new ComboBox<>();
    private final ComboBox<String> cbKhung = new ComboBox<>();
    private final Button btnDatLai = Functions.nutPhu("Đặt lại bộ lọc");
    private final Button btnTiep = Functions.nutChinh("Tiếp tục: Thiết lập ưu tiên");
    private List<SectionSlot> locDuLieu = new ArrayList<>();
    private int trangHienTai;
    private VBox danhSachBox = new VBox(8);

    public RequirementPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        this.st = frame.trangThai();
        setPadding(new Insets(22, 26, 18, 26));
        setSpacing(14);
        setStyle("-fx-background-color: #F6F8FB;");

        cbLoai.getItems().addAll("Tất cả lớp", "Lý thuyết (LT)", "Thực hành (TH)");
        cbNgay.getItems().addAll("Tất cả các ngày", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7");
        cbKhung.getItems().addAll("Tất cả khung giờ", "Buổi sáng (tiết 1–5)", "Buổi chiều (tiết 6–12)");
        cbLoai.setValue("Tất cả lớp");
        cbNgay.setValue("Tất cả các ngày");
        cbKhung.setValue("Tất cả khung giờ");

        Label title = Functions.chuDe("Nhóm lớp học phần");
        Label info = Functions.moTa("Chọn các nhóm lý thuyết và thực hành muốn xếp vào thời khóa biểu.");

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        txtTimKiem.setPromptText("Tìm mã HP, tên học phần, giảng viên...");
        txtTimKiem.setPrefWidth(250);
        filterRow.getChildren().addAll(
                new Label("Tìm kiếm"),
                txtTimKiem,
                new Label("Loại lớp"),
                cbLoai,
                new Label("Ngày học"),
                cbNgay,
                new Label("Khung giờ"),
                cbKhung,
                btnDatLai
        );

        danhSachBox = Functions.the(12);
        danhSachBox.setPrefSize(900, 440);
        danhSachBox.getChildren().add(new Label("Danh sách nhóm lớp"));
        danhSachBox.getChildren().add(new Label("Dữ liệu được hiển thị theo học kỳ demo."));

        btnDatLai.setOnAction(e -> datLaiBoLoc());
        btnTiep.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));

        txtTimKiem.textProperty().addListener((obs, oldVal, newVal) -> veBang());
        cbLoai.valueProperty().addListener((obs, oldVal, newVal) -> veBang());
        cbNgay.valueProperty().addListener((obs, oldVal, newVal) -> veBang());
        cbKhung.valueProperty().addListener((obs, oldVal, newVal) -> veBang());

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().add(btnTiep);

        getChildren().addAll(title, info, filterRow, danhSachBox, footer);
    }

    public void taiDuLieu() {
        locDuLieu = st.tatCaNhom;
        veBang();
    }

    private void veBang() {
        // Rebuild the visible list each time the filter state changes.
        // This keeps the UI in sync with the current search and course filters.
        List<SectionSlot> filtered = locDuLieu.stream()
                .filter(slot -> matchSearch(slot))
                .filter(slot -> matchLoai(slot))
                .filter(slot -> matchNgay(slot))
                .filter(slot -> matchKhung(slot))
                .toList();

        danhSachBox.getChildren().clear();
        if (filtered.isEmpty()) {
            // Show a friendly empty-state when no matching classes remain.
            Label empty = new Label("Không có nhóm lớp nào phù hợp với bộ lọc hiện tại.");
            empty.setStyle("-fx-text-fill: #64748B;");
            danhSachBox.getChildren().add(empty);
            return;
        }

        for (SectionSlot slot : filtered) {
            // Render one card per matching section so the user can review details before selecting it.
            VBox card = new VBox(6);
            card.setPadding(new Insets(10, 12, 10, 12));
            card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10;");

            HBox top = new HBox(10);
            top.setAlignment(Pos.CENTER_LEFT);
            Label ma = new Label(slot.section().maMh() + " · " + slot.section().nhom());
            ma.setStyle("-fx-font-weight: bold;");
            Label loai = new Label(slot.section().loai());
            loai.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-padding: 3 8 3 8; -fx-background-radius: 999; -fx-border-radius: 999;");
            top.getChildren().addAll(ma, loai);

            Label title = new Label(slot.tenHocPhan());
            title.setWrapText(true);

            // Summarize each session as a human-readable schedule block: day + time + room.
            StringBuilder days = new StringBuilder();
            for (ClassSession session : slot.sessions()) {
                if (days.length() > 0) days.append(" · ");
                days.append("Thứ ").append(session.thu()).append(" • tiết ").append(session.tietBd()).append("-").append(session.tietBd() + session.soTiet() - 1);
                if (session.phong() != null && !session.phong().isBlank()) {
                    days.append(" • ").append(session.phong());
                }
            }

            Label meta = new Label(days.toString());
            meta.setWrapText(true);
            meta.setStyle("-fx-text-fill: #475569;");

            HBox actions = new HBox(8);
            Button chon = Functions.nutChinh("Chọn");
            chon.setOnAction(e -> {
                // Add the chosen section to the selected set and continue to the priority screen.
                st.daChon.add(slot.section().id());
                btnTiep.fire();
            });
            actions.getChildren().add(chon);
            card.getChildren().addAll(top, title, meta, actions);
            danhSachBox.getChildren().add(card);
        }
    }

    private boolean matchSearch(SectionSlot slot) {
        // Search by course code, course name, or lecturer name.
        String q = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        String haystack = (slot.section().maMh() + " " + slot.tenHocPhan() + " " + slot.section().giangVien()).toLowerCase(Locale.ROOT);
        return haystack.contains(q);
    }

    private boolean matchLoai(SectionSlot slot) {
        // Keep only classes of the selected type: LT, TH, or all.
        String val = cbLoai.getValue();
        if (val == null || "Tất cả lớp".equals(val)) return true;
        return val.contains(slot.section().loai());
    }

    private boolean matchNgay(SectionSlot slot) {
        // Filter by the day(s) in the selected section's session list.
        String val = cbNgay.getValue();
        if (val == null || "Tất cả các ngày".equals(val)) return true;
        for (ClassSession s : slot.sessions()) {
            String label = dayLabel(s.thu());
            if (label.equals(val)) return true;
        }
        return false;
    }

    private boolean matchKhung(SectionSlot slot) {
        // Morning classes are taught in periods 1-5; afternoon starts at 6 onward.
        String val = cbKhung.getValue();
        if (val == null || "Tất cả khung giờ".equals(val)) return true;
        for (ClassSession s : slot.sessions()) {
            int start = s.tietBd();
            if ("Buổi sáng (tiết 1–5)".equals(val) && start <= 5) return true;
            if ("Buổi chiều (tiết 6–12)".equals(val) && start >= 6) return true;
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
        cbLoai.setValue("Tất cả lớp");
        cbNgay.setValue("Tất cả các ngày");
        cbKhung.setValue("Tất cả khung giờ");
    }
}
