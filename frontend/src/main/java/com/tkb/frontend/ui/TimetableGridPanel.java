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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableGridPanel extends VBox {
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    private final List<SectionSlot> slots = new ArrayList<>();
    private String tieuDe = "Thời khóa biểu";

    public TimetableGridPanel() {
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
        grid.getChildren().clear();
        veMacDinh();
        Map<String, List<SectionSlot>> groups = new HashMap<>();
        for (SectionSlot slot : slots) {
            String key = slot.day() + "-" + slot.start();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
        }

        for (Map.Entry<String, List<SectionSlot>> entry : groups.entrySet()) {
            String[] parts = entry.getKey().split("-");
            int day = indexOfDay(parts[0]);
            int hour = indexOfHour(parts[1]);
            if (day < 0 || hour < 0) continue;

            List<SectionSlot> items = entry.getValue();
            for (SectionSlot item : items) {
                VBox cell = new VBox(4);
                cell.setPadding(new Insets(6));
                cell.setStyle("-fx-background-color: " + colorFor(item.courseCode()) + "; -fx-border-color: #DDE7F0; -fx-background-radius: 8; -fx-border-radius: 8;");
                String label = item.courseCode() + "\n" + item.room();
                Label l1 = new Label(item.courseCode());
                l1.setWrapText(true);
                l1.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                Label l2 = new Label(item.room());
                l2.setWrapText(true);
                l2.setStyle("-fx-text-fill: white;");
                cell.getChildren().addAll(l1, l2);
                grid.add(cell, day + 1, hour + 1);
            }
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

    private int indexOfDay(String day) {
        return switch (day.toLowerCase()) {
            case "mon" -> 0;
            case "tue" -> 1;
            case "wed" -> 2;
            case "thu" -> 3;
            case "fri" -> 4;
            case "sat" -> 5;
            case "sun" -> 6;
            default -> -1;
        };
    }

    private int indexOfHour(String hour) {
        return switch (hour) {
            case "7:00" -> 0;
            case "8:00" -> 1;
            case "9:00" -> 2;
            case "10:00" -> 3;
            case "11:00" -> 4;
            case "13:00" -> 5;
            case "14:00" -> 6;
            case "15:00" -> 7;
            case "16:00" -> 8;
            default -> -1;
        };
    }

    private String dayLabel(int index) {
        return switch (index) {
            case 0 -> "Mon";
            case 1 -> "Tue";
            case 2 -> "Wed";
            case 3 -> "Thu";
            case 4 -> "Fri";
            case 5 -> "Sat";
            case 6 -> "Sun";
            default -> "";
        };
    }

    private String timeLabel(int index) {
        return switch (index) {
            case 1 -> "7:00";
            case 2 -> "8:00";
            case 3 -> "9:00";
            case 4 -> "10:00";
            case 5 -> "11:00";
            case 6 -> "13:00";
            case 7 -> "14:00";
            case 8 -> "15:00";
            case 9 -> "16:00";
            default -> "";
        };
    }
}
