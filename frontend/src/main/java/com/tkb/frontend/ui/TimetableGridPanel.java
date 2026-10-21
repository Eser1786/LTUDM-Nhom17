package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.ClassSession;
import com.tkb.frontend.model.Types.SectionSlot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lưới thời khóa biểu tuần: trục dọc 07:00–21:00, trục ngang T2–T7.
 * Mỗi buổi học là một block đặt đúng ngày và giờ, hiển thị học phần, mã nhóm và phòng.
 */
public class TimetableGridPanel extends VBox {
    private static final int GIO_BAT_DAU = 7;
    private static final int GIO_KET_THUC = 21;
    private static final int TONG_GIO = GIO_KET_THUC - GIO_BAT_DAU; // 14

    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane();
    private final List<SectionSlot> slots = new ArrayList<>();
    private String tieuDe = "Thời khóa biểu";

    public TimetableGridPanel() {
        setSpacing(10);
        setPadding(new Insets(4));
        scroll.setContent(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
        veMacDinh();
        veLich();
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setSlots(List<SectionSlot> slots) {
        this.slots.clear();
        if (slots != null) {
            this.slots.addAll(slots);
        }
        veLich();
    }

    private void veMacDinh() {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        grid.setHgap(6);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 12; "
                + "-fx-background-radius: 12;");

        ColumnConstraints gioCol = new ColumnConstraints();
        gioCol.setMinWidth(56);
        grid.getColumnConstraints().add(gioCol);
        for (int i = 0; i < 6; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(150);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(34);
        grid.getRowConstraints().add(headerRow);
        for (int i = 0; i < TONG_GIO; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(34);
            grid.getRowConstraints().add(rc);
        }

        for (int c = 0; c < 6; c++) {
            Label header = new Label(dayFull(c + 2));
            header.setAlignment(Pos.CENTER);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            header.setStyle("-fx-background-color: #EFF6FF; -fx-padding: 8 12; -fx-text-fill: #1E40AF; "
                    + "-fx-background-radius: 6;");
            grid.add(header, c + 1, 0);
        }
        grid.add(new Label(""), 0, 0);

        for (int h = 0; h < TONG_GIO; h++) {
            Label time = new Label(gioLabel(h));
            time.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 0 6 0 0;");
            time.setAlignment(Pos.TOP_RIGHT);
            time.setMaxWidth(Double.MAX_VALUE);
            grid.add(time, 0, h + 1);

            // Nền lưới trống để thể hiện rõ khoảng trống.
            for (int c = 0; c < 6; c++) {
                Region cell = new Region();
                cell.setMinHeight(30);
                cell.setStyle("-fx-background-color: #FAFBFC; -fx-background-radius: 6;");
                grid.add(cell, c + 1, h + 1);
            }
        }
    }

    private void veLich() {
        veMacDinh();
        if (slots.isEmpty()) {
            return;
        }

        // Gom theo (ngày, giờ bắt đầu) để xếp các block chồng thời gian thành 1 ô.
        Map<String, List<SectionSlot>> groups = new HashMap<>();
        for (SectionSlot slot : slots) {
            for (ClassSession session : slot.sessions()) {
                String key = session.thu() + "-" + session.tietBd();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        // Track occupied cells to avoid stacking empty Region on top of class cells
        boolean[][] occupied = new boolean[6][TONG_GIO];

        for (Map.Entry<String, List<SectionSlot>> entry : groups.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int day = Integer.parseInt(parts[0]);
            int tiet = Integer.parseInt(parts[1]);
            int col = day - 2;
            int row = tiet - 1;
            if (col < 0 || col > 5 || row < 0) continue;

            List<SectionSlot> items = entry.getValue();
            ClassSession cs = items.get(0).sessions().stream()
                    .filter(s -> s.thu() == day && s.tietBd() == tiet).findFirst().orElse(null);
            int soTiet = cs == null ? 1 : cs.soTiet();
            int span = Math.max(1, Math.min(soTiet, TONG_GIO - row));

            // Mark occupied cells
            for (int r = 0; r < span; r++) {
                if (row + r < TONG_GIO) {
                    occupied[col][row + r] = true;
                }
            }

            VBox cell = new VBox(2);
            cell.setPadding(new Insets(6));
            cell.setStyle("-fx-background-color: " + colorFor(items.get(0).section().maMh(), false)
                    + "; -fx-background-radius: 8; -fx-border-color: "
                    + colorFor(items.get(0).section().maMh(), true) + "; -fx-border-radius: 8;");
            for (SectionSlot item : items) {
                Label l1 = new Label(item.tenHocPhan());
                l1.setWrapText(true);
                l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                l1.setTextFill(javafx.scene.paint.Color.WHITE);
                Label l2 = new Label(item.section().maMh() + " · " + maNhom(item)
                        + " · " + phong(item));
                l2.setWrapText(true);
                l2.setFont(Font.font("Segoe UI", 10));
                l2.setTextFill(javafx.scene.paint.Color.web("#EEF2FF"));
                cell.getChildren().addAll(l1, l2);
            }
            grid.add(cell, col + 1, row + 1, 1, span);
        }

        // Remove empty Region cells that are covered by class cells
        List<Node> toRemove = new ArrayList<>();
        for (Node node : grid.getChildren()) {
            Integer colIdx = GridPane.getColumnIndex(node);
            Integer rowIdx = GridPane.getRowIndex(node);
            if (colIdx != null && rowIdx != null && colIdx > 0 && colIdx <= 6 && rowIdx > 0 && rowIdx <= TONG_GIO) {
                int c = colIdx - 1;
                int r = rowIdx - 1;
                if (occupied[c][r] && node instanceof Region && !(node instanceof VBox)) {
                    toRemove.add(node);
                }
            }
        }
        grid.getChildren().removeAll(toRemove);
    }

    private String maNhom(SectionSlot item) {
        var s = item.section();
        return "TH".equals(s.loai()) ? "TH" + s.nhom() + "." + s.toTh() : "LT" + s.nhom();
    }

    private String phong(SectionSlot item) {
        for (ClassSession cs : item.sessions()) {
            if (cs.phong() != null && !cs.phong().isBlank()) {
                return cs.phong();
            }
        }
        return "";
    }

    private String colorFor(String code, boolean dam) {
        int hash = code == null ? 0 : code.hashCode();
        String[] palette = {"#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
                "#14B8A6", "#EA580C", "#DB2777", "#65A30D", "#0891B2"};
        String[] vien = {"#2563EB", "#059669", "#D97706", "#DC2626", "#7C3AED",
                "#0D9488", "#C2410C", "#BE185D", "#4D7C0F", "#0E7490"};
        int i = Math.abs(hash) % palette.length;
        return dam ? vien[i] : palette[i];
    }

    private String dayFull(int thu) {
        return switch (thu) {
            case 2 -> "Thứ Hai";
            case 3 -> "Thứ Ba";
            case 4 -> "Thứ Tư";
            case 5 -> "Thứ Năm";
            case 6 -> "Thứ Sáu";
            case 7 -> "Thứ Bảy";
            default -> "";
        };
    }

    private String gioLabel(int h) {
        int gio = GIO_BAT_DAU + h;
        return String.format("%02d:00", gio);
    }
}
