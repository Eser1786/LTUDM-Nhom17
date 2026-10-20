package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.ClassSession;
import com.tkb.frontend.model.Types.SectionSlot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableGridPanel extends VBox {
    // This grid visualizes a student's weekly timetable by day and period.
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    private final List<SectionSlot> slots = new ArrayList<>();
    private String tieuDe = "Thời khóa biểu";

    public TimetableGridPanel() {
        // The timetable always starts with the standard 7-day by 9-period layout.
        setSpacing(12);
        setPadding(new Insets(12));
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        getChildren().add(scroll);
        veMacDinh();
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public void setSlots(List<SectionSlot> slots) {
        // Replace the current schedule with the selected slot list and redraw the grid.
        this.slots.clear();
        if (slots != null) {
            this.slots.addAll(slots);
        }
        veLich();
    }

    private void veMacDinh() {
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: white; -fx-border-color: #DDE7F0; -fx-border-radius: 10; -fx-background-radius: 10;");
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 9; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 9);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }
        for (int c = 0; c < 7; c++) {
            Label header = new Label(dayLabel(c));
            header.setAlignment(Pos.CENTER);
            header.setMaxWidth(Double.MAX_VALUE);
            header.setStyle("-fx-background-color: #EFF6FF; -fx-padding: 8 12; -fx-font-weight: bold; -fx-text-fill: #1F2937");
            grid.add(header, c + 1, 0);
        }
        for (int r = 1; r <= 8; r++) {
            Label time = new Label(timeLabel(r));
            time.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-padding: 6 8;");
            grid.add(time, 0, r);
        }
        grid.add(new Label(""), 0, 0);
    }

    private void veLich() {
        // Clear and rebuild the calendar grid using the current sections.
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        veMacDinh();

        // Group sessions by (day, start period) so classes on the same time cell are shown together.
        Map<String, List<SectionSlot>> groups = new HashMap<>();
        for (SectionSlot slot : slots) {
            for (ClassSession session : slot.sessions()) {
                String key = session.thu() + "-" + session.tietBd();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        for (Map.Entry<String, List<SectionSlot>> entry : groups.entrySet()) {
            // Convert the day/period keys into the correct grid coordinates.
            String[] parts = entry.getKey().split("-");
            int day = Integer.parseInt(parts[0]);
            int start = Integer.parseInt(parts[1]);
            int col = dayToColumn(day);
            int row = hourToRow(start);
            if (col < 0 || row < 0) continue;

            List<SectionSlot> items = entry.getValue();
            VBox cell = new VBox(4);
            cell.setPadding(new Insets(6));
            String bg = colorFor(items.get(0).section().maMh());
            cell.setStyle("-fx-background-color: " + bg + "; -fx-border-color: #DDE7F0; -fx-background-radius: 8; -fx-border-radius: 8;");

            for (SectionSlot item : items) {
                // Each class cell displays the course code and room label for quick scanning.
                String room = item.sessions().isEmpty() ? "" : item.sessions().get(0).phong();
                Label l1 = new Label(item.section().maMh());
                l1.setWrapText(true);
                l1.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                Label l2 = new Label(room == null ? "" : room);
                l2.setWrapText(true);
                l2.setStyle("-fx-text-fill: white;");
                cell.getChildren().addAll(l1, l2);
            }
            grid.add(cell, col + 1, row + 1);
        }
    }

    private String colorFor(String code) {
        int hash = code == null ? 0 : code.hashCode();
        String[] palette = {
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
            "#14B8A6", "#F97316", "#EC4899", "#84CC16", "#06B6D4"
        };
        return palette[Math.abs(hash) % palette.length];
    }

    private int dayToColumn(int day) {
        // Map the numeric weekday value used in the model to the grid's column index.
        return switch (day) {
            case 2 -> 0;
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            case 7 -> 5;
            default -> -1;
        };
    }

    private int hourToRow(int hour) {
        // The schedule uses period numbers as row indices. This converts them to visible grid rows.
        return switch (hour) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 4;
            case 6 -> 5;
            case 7 -> 6;
            case 8 -> 7;
            case 9 -> 8;
            default -> -1;
        };
    }

    private String dayLabel(int index) {
        // These are the visible weekday headings for the timetable grid.
        return switch (index) {
            case 0 -> "Thứ 2";
            case 1 -> "Thứ 3";
            case 2 -> "Thứ 4";
            case 3 -> "Thứ 5";
            case 4 -> "Thứ 6";
            case 5 -> "Thứ 7";
            default -> "";
        };
    }

    private String timeLabel(int index) {
        return switch (index) {
            case 1 -> "1";
            case 2 -> "2";
            case 3 -> "3";
            case 4 -> "4";
            case 5 -> "5";
            case 6 -> "6";
            case 7 -> "7";
            case 8 -> "8";
            case 9 -> "9";
            default -> "";
        };
    }
}
