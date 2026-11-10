package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Criteria;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PriorityPanel extends VBox {
    private static final String KEY_TIME = "TIME_PREF";
    private static final String KEY_CAMPUS = "CAMPUS_PREF";
    private static final String KEY_DAYS = "DAYS_PREF";
    private static final String KEY_GAPS = "GAPS_PREF";

    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final boolean hienTieuDe;
    private final VBox cotPhai = new VBox(12);
    private final Map<Integer, CheckBox> ngayChk = new LinkedHashMap<>();
    private final List<PriorityItem> priorityItems = new ArrayList<>();

    // Cau hinh chi tiet
    private String timeMode = "MORNING"; // MORNING | AFTERNOON | CUSTOM_MATRIX
    private final Set<Integer> preferredDays = new LinkedHashSet<>(List.of(2, 3, 4, 6, 7));
    private boolean minDaysCompact = true;
    private String preferredCampus = "C"; // C | 1 | 2 | ANY
    private boolean avoidCampusSwitch = true;

    // Ma tran 7 ngay x 10 tiet
    private final boolean[][] slotMatrix = new boolean[7][10];

    public PriorityPanel(MainFrame frame) {
        this(frame, true);
    }

    public PriorityPanel(MainFrame frame, boolean hienTieuDe) {
        this.hienTieuDe = hienTieuDe;
        this.frame = frame;
        this.st = frame.trangThai();

        // Khoi tao ma tran mac dinh: sang tiet 1-5 cac ngay T2,T3,T4,T6,T7
        for (int d = 0; d < 7; d++) {
            int dayNum = d + 2;
            boolean daySel = preferredDays.contains(dayNum);
            for (int p = 0; p < 10; p++) {
                slotMatrix[d][p] = daySel && p < 5;
            }
        }

        priorityItems.addAll(List.of(
                new PriorityItem(KEY_TIME, "Khung thời gian",
                        "Chọn ưu tiên Sáng, Chiều hoặc tự quét chọn các tiết rảnh trên lưới 7 ngày × 10 tiết."),
                new PriorityItem(KEY_CAMPUS, "Ưu tiên Cơ sở",
                        "Ký tự đầu mã phòng: C (Chính), 1 (Cơ sở 1), 2 (Cơ sở 2). Phạt nặng nếu 2 tiết kế nhau khác cơ sở."),
                new PriorityItem(KEY_DAYS, "Số ngày trong tuần",
                        "Chọn các thứ muốn đến trường và gom lịch vào ít ngày nhất."),
                new PriorityItem(KEY_GAPS, "Tránh tiết trống",
                        "Ưu tiên các ca học trong cùng ngày nối tiếp nhau liên tục.")
        ));

        VBox phaiCard = Functions.the(16);
        phaiCard.setPrefWidth(420);
        phaiCard.setMinWidth(360);
        cotPhai.setFillWidth(true);
        phaiCard.getChildren().add(cotPhai);

        if (hienTieuDe) {
            VBox header = new VBox(4,
                    Functions.chuDe("Thiết lập ưu tiên"),
                    Functions.moTa("Sắp xếp thứ tự ưu tiên từ trên xuống. Hệ thống quy đổi thành trọng số 4.0/3.0/2.0/1.0."));
            getChildren().addAll(header, phaiCard);
        } else {
            getChildren().add(phaiCard);
        }

        setSpacing(0);
        setStyle("-fx-background-color: #F6F8FB;");
        veCotPhai();
    }

    public void taiLai() {
        veCotPhai();
    }

    private void veCotPhai() {
        cotPhai.getChildren().clear();
        cotPhai.getChildren().addAll(
                Functions.tieuDeThe("Mức độ ưu tiên"),
                Functions.moTa("Kéo các tiêu chí lên xuống. Bật/tắt để áp dụng."));

        VBox list = new VBox(10);
        double[] weights = {4.0, 3.0, 2.0, 1.0};
        for (int i = 0; i < priorityItems.size(); i++) {
            PriorityItem item = priorityItems.get(i);
            double w = item.enabled ? weights[i] : 0.0;
            list.getChildren().add(theTieuChi(item, i, w));
        }
        cotPhai.getChildren().add(list);
        cotPhai.getChildren().addAll(Functions.vach(), theNutMacDinh(), ghiChu());
    }

    private VBox theTieuChi(PriorityItem item, int index, double weight) {
        VBox card = Functions.the(12);
        card.setUserData(item);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        CheckBox toggle = new CheckBox();
        toggle.setSelected(item.enabled);
        toggle.setOnAction(e -> { item.enabled = toggle.isSelected(); veCotPhai(); });

        Label dragIcon = new Label("⋮⋮");
        dragIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        dragIcon.setTextFill(Functions.MA_CHU_PHU);

        Label title = Functions.tieuDeThe(item.title());
        Label badge = Functions.nhanXam("Hạng #" + (index + 1) + " (TS: " + String.format("%.1f", weight) + ")");
        top.getChildren().addAll(dragIcon, toggle, title, Functions.khoangTrong(), badge);

        Label mo = Functions.moTa(item.description());
        VBox body = new VBox(8, top, mo);

        // Chi tiet theo tung loai
        if (item.key().equals(KEY_DAYS)) {
            FlowPane ngayRow = new FlowPane(6, 6);
            String[] tenNgay = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (int i = 0; i < tenNgay.length; i++) {
                int thu = i + 2;
                CheckBox chk = new CheckBox(tenNgay[i]);
                chk.setSelected(preferredDays.contains(thu));
                chk.setDisable(!item.enabled);
                chk.setOnAction(e -> {
                    if (chk.isSelected()) preferredDays.add(thu);
                    else if (preferredDays.size() > 1) preferredDays.remove(thu);
                    veCotPhai();
                });
                ngayChk.put(thu, chk);
                ngayRow.getChildren().add(chk);
            }
            CheckBox chkCompact = new CheckBox("Gom lịch vào ít ngày nhất");
            chkCompact.setSelected(minDaysCompact);
            chkCompact.setDisable(!item.enabled);
            chkCompact.setOnAction(e -> { minDaysCompact = chkCompact.isSelected(); });
            body.getChildren().addAll(Functions.nhanNho("Ngày muốn đi học"), ngayRow, chkCompact);
        } else if (item.key().equals(KEY_TIME)) {
            HBox modeRow = new HBox(6);
            Button bSang = modeBtn("☀ Sáng (1–5)", "MORNING", item.enabled);
            Button bChieu = modeBtn("🌤 Chiều (6–10)", "AFTERNOON", item.enabled);
            Button bTuChon = modeBtn("📅 Tự chọn", "CUSTOM_MATRIX", item.enabled);
            modeRow.getChildren().addAll(bSang, bChieu, bTuChon);

            VBox matrixBox = new VBox(6);
            matrixBox.setVisible(timeMode.equals("CUSTOM_MATRIX") && item.enabled);
            matrixBox.setManaged(timeMode.equals("CUSTOM_MATRIX") && item.enabled);
            matrixBox.getChildren().addAll(Functions.nhanNho("Ma trận tiết muốn học (click/kéo)"), taoMaTran(item.enabled));

            HBox nutMatrix = new HBox(6);
            Button btnSang = Functions.nutPhu("Chọn hết sáng");
            btnSang.setDisable(!item.enabled);
            btnSang.setOnAction(e -> { chonSang(); veCotPhai(); });
            Button btnXoa = Functions.nutPhu("Xóa trắng");
            btnXoa.setDisable(!item.enabled);
            btnXoa.setOnAction(e -> { xoaMatrix(); veCotPhai(); });
            nutMatrix.getChildren().addAll(btnSang, btnXoa);
            matrixBox.getChildren().add(nutMatrix);

            body.getChildren().addAll(modeRow, matrixBox);
        } else if (item.key().equals(KEY_CAMPUS)) {
            HBox campusRow = new HBox(8);
            campusRow.setAlignment(Pos.CENTER_LEFT);
            Label lb = Functions.nhanNho("Cơ sở ưu tiên:");
            ComboBox<String> cb = new ComboBox<>();
            cb.getItems().addAll("C — Cơ sở chính", "1 — Cơ sở 1", "2 — Cơ sở 2", "Không phân biệt");
            cb.setValue(campusLabel(preferredCampus));
            cb.setDisable(!item.enabled);
            cb.setOnAction(e -> preferredCampus = campusCode(cb.getValue()));
            campusRow.getChildren().addAll(lb, cb);

            CheckBox chkSwitch = new CheckBox("Chặn nếu 2 tiết kế nhau khác cơ sở");
            chkSwitch.setSelected(avoidCampusSwitch);
            chkSwitch.setDisable(!item.enabled);
            chkSwitch.setOnAction(e -> avoidCampusSwitch = chkSwitch.isSelected());

            body.getChildren().addAll(campusRow, chkSwitch);
        }

        card.getChildren().add(body);

        // Drag & drop
        card.setOnDragDetected(evt -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(item.key());
            db.setContent(content);
            card.setOpacity(0.7);
            evt.consume();
        });
        card.setOnDragOver(evt -> {
            if (evt.getGestureSource() != card && evt.getDragboard().hasString()) {
                evt.acceptTransferModes(TransferMode.MOVE);
                evt.consume();
            }
        });
        card.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (db.hasString()) {
                String draggedKey = db.getString();
                int from = findIndexByKey(draggedKey);
                int to = priorityItems.indexOf(item);
                if (from >= 0 && from != to) {
                    PriorityItem moved = priorityItems.remove(from);
                    priorityItems.add(Math.max(0, Math.min(to, priorityItems.size())), moved);
                    veCotPhai();
                }
                evt.setDropCompleted(true);
            }
            evt.consume();
        });
        card.setOnDragDone(evt -> {
            card.setOpacity(1.0);
            evt.consume();
        });

        return card;
    }

    private Button modeBtn(String text, String mode, boolean enabled) {
        Button b = Functions.nutPhu(text);
        b.setDisable(!enabled);
        if (timeMode.equals(mode) && enabled) {
            b.setStyle("-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-border-color: #1D4ED8; -fx-border-radius: 8; -fx-background-radius: 8;");
        }
        b.setOnAction(e -> { timeMode = mode; veCotPhai(); });
        return b;
    }

    private GridPane taoMaTran(boolean enabled) {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        // header
        Label corner = new Label("Tiết");
        grid.add(corner, 0, 0);
        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (int d = 0; d < 7; d++) {
            Label h = new Label(days[d]);
            h.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
            grid.add(h, d + 1, 0);
        }
        for (int p = 0; p < 10; p++) {
            Label rl = new Label(String.valueOf(p + 1));
            rl.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B;");
            grid.add(rl, 0, p + 1);
            for (int d = 0; d < 7; d++) {
                Button cell = new Button(slotMatrix[d][p] ? "✓" : "·");
                cell.setPrefSize(34, 28);
                cell.setDisable(!enabled);
                if (slotMatrix[d][p]) {
                    cell.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6;");
                } else {
                    cell.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #94A3B8; -fx-background-radius: 6;");
                }
                final int dd = d, pp = p;
                cell.setOnAction(e -> {
                    slotMatrix[dd][pp] = !slotMatrix[dd][pp];
                    veCotPhai();
                });
                grid.add(cell, d + 1, p + 1);
            }
        }
        return grid;
    }

    private void chonSang() {
        for (int d = 0; d < 7; d++) {
            for (int p = 0; p < 10; p++) {
                slotMatrix[d][p] = p < 5;
            }
        }
    }

    private void xoaMatrix() {
        for (int d = 0; d < 7; d++) {
            for (int p = 0; p < 10; p++) {
                slotMatrix[d][p] = false;
            }
        }
    }

    private int findIndexByKey(String key) {
        for (int i = 0; i < priorityItems.size(); i++) {
            if (priorityItems.get(i).key().equals(key)) return i;
        }
        return -1;
    }

    private VBox theNutMacDinh() {
        VBox box = new VBox(8);
        Button macDinh = Functions.nutPhu("Khôi phục mặc định");
        macDinh.setMaxWidth(Double.MAX_VALUE);
        macDinh.setOnAction(e -> {
            timeMode = "MORNING";
            preferredDays.clear();
            preferredDays.addAll(List.of(2, 3, 4, 6, 7));
            minDaysCompact = true;
            preferredCampus = "C";
            avoidCampusSwitch = true;
            for (int d = 0; d < 7; d++) {
                int dayNum = d + 2;
                boolean daySel = preferredDays.contains(dayNum);
                for (int p = 0; p < 10; p++) slotMatrix[d][p] = daySel && p < 5;
            }
            priorityItems.clear();
            priorityItems.addAll(List.of(
                new PriorityItem(KEY_TIME, "Khung thời gian",
                        "Chọn ưu tiên Sáng, Chiều hoặc tự quét chọn các tiết rảnh trên lưới 7 ngày × 10 tiết."),
                new PriorityItem(KEY_CAMPUS, "Ưu tiên Cơ sở",
                        "Ký tự đầu mã phòng: C (Chính), 1 (Cơ sở 1), 2 (Cơ sở 2). Phạt nặng nếu 2 tiết kế nhau khác cơ sở."),
                new PriorityItem(KEY_DAYS, "Số ngày trong tuần",
                        "Chọn các thứ muốn đến trường và gom lịch vào ít ngày nhất."),
                new PriorityItem(KEY_GAPS, "Tránh tiết trống",
                        "Ưu tiên các ca học trong cùng ngày nối tiếp nhau liên tục.")
            ));
            for (PriorityItem pi : priorityItems) pi.enabled = true;
            veCotPhai();
        });
        box.getChildren().add(macDinh);
        return box;
    }

    private VBox ghiChu() {
        VBox box = Functions.the(12);
        box.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 12; -fx-border-color: #BFDBFE; -fx-border-radius: 12; -fx-border-width: 1;");
        box.getChildren().addAll(
                Functions.nhanDeXuat("Ghi chú"),
                Functions.moTa("Thứ tự từ trên xuống là mức ưu tiên cao nhất. Trọng số 4.0/3.0/2.0/1.0. Backend hiện dùng minDays/morningFirst/avoidGaps + weights."));
        return box;
    }

    private String campusLabel(String code) {
        return switch (code) {
            case "1" -> "1 — Cơ sở 1";
            case "2" -> "2 — Cơ sở 2";
            case "ANY" -> "Không phân biệt";
            default -> "C — Cơ sở chính";
        };
    }

    private String campusCode(String label) {
        if (label == null) return "C";
        if (label.startsWith("1")) return "1";
        if (label.startsWith("2")) return "2";
        if (label.startsWith("Không")) return "ANY";
        return "C";
    }

    public void luuCriteria() {
        double wDays = 1.0, wTime = 1.0, wGaps = 1.0, wCampus = 1.0;
        for (int i = 0; i < priorityItems.size(); i++) {
            PriorityItem item = priorityItems.get(i);
            double w = item.enabled ? Math.max(1.0, 4.0 - i) : 0.0;
            if (item.key().equals(KEY_DAYS)) wDays = w;
            if (item.key().equals(KEY_TIME)) wTime = w;
            if (item.key().equals(KEY_GAPS)) wGaps = w;
            if (item.key().equals(KEY_CAMPUS)) wCampus = w;
        }

        boolean daysEnabled = isEnabled(KEY_DAYS);
        boolean timeEnabled = isEnabled(KEY_TIME);
        boolean gapsEnabled = isEnabled(KEY_GAPS);

        List<Integer> ngay = new ArrayList<>();
        if (daysEnabled) {
            ngayChk.forEach((thu, chk) -> { if (chk.isSelected()) ngay.add(thu); });
            if (ngay.isEmpty()) ngay.addAll(preferredDays);
        }

        // preferred slots mask
        List<Integer> masks = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            int mask = 0;
            for (int p = 0; p < 10; p++) {
                if (slotMatrix[d][p]) mask |= (1 << p);
            }
            masks.add(mask);
        }

        java.util.Map<String, String> lecturers = new java.util.LinkedHashMap<>(st.uuTienGiangVien);
        lecturers.remove("_timeMode");
        lecturers.remove("_campus");
        java.util.Map<String, String> groups = new java.util.LinkedHashMap<>(st.khoaNhom);

        st.criteria = new Criteria(
                timeEnabled && "MORNING".equals(timeMode) ? (int) Math.round(wTime) : 0,
                daysEnabled && minDaysCompact ? (int) Math.round(wDays) : 0,
                gapsEnabled ? (int) Math.round(wGaps) : 0,
                daysEnabled ? (int) Math.round(wDays) : 0,
                ngay,
                3,
                timeEnabled ? timeMode : "NONE",
                daysEnabled ? new ArrayList<>(preferredDays) : new ArrayList<>(),
                timeEnabled && "CUSTOM_MATRIX".equals(timeMode) ? masks : null,
                campusEnabled() ? preferredCampus : "ANY",
                campusEnabled() && avoidCampusSwitch,
                lecturers,
                groups
        );
    }

    private boolean campusEnabled() {
        return isEnabled(KEY_CAMPUS);
    }

    private boolean isEnabled(String key) {
        for (PriorityItem item : priorityItems) {
            if (item.key().equals(key)) return item.enabled;
        }
        return false;
    }

    private static class PriorityItem {
        private final String key;
        private final String title;
        private final String description;
        boolean enabled = true;

        PriorityItem(String key, String title, String description) {
            this.key = key;
            this.title = title;
            this.description = description;
        }

        String key() { return key; }
        String title() { return title; }
        String description() { return description; }
    }
}