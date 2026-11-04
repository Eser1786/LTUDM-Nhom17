package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.Criteria;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình Thiết lập ưu tiên.
 * Chỉ giữ bảng mức độ ưu tiên; người dùng đã chọn môn ở màn hình trước.
 */
public class PriorityPanel extends VBox {
    private static final String KEY_SANG = "SANG";
    private static final String KEY_IT_NGAY = "IT_NGAY";
    private static final String KEY_KHOANG = "KHOANG";
    private static final String KEY_NGAY = "NGAY";

    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final boolean hienTieuDe;
    private final VBox cotPhai = new VBox(12);
    private final Map<Integer, CheckBox> ngayChk = new LinkedHashMap<>();
    private final List<PriorityItem> priorityItems = new ArrayList<>();
    private final VBox khung = new VBox(14);

    public PriorityPanel(MainFrame frame) {
        this(frame, true);
    }

    public PriorityPanel(MainFrame frame, boolean hienTieuDe) {
        this.hienTieuDe = hienTieuDe;
        this.frame = frame;
        this.st = frame.trangThai();

        priorityItems.addAll(List.of(
                new PriorityItem(KEY_SANG, "Ưu tiên buổi sáng",
                        "Xếp nhiều tiết học vào buổi sáng (tiết 1–5) hơn."),
                new PriorityItem(KEY_IT_NGAY, "Ít ngày đến trường",
                        "Giảm số ngày phải lên trường trong tuần."),
                new PriorityItem(KEY_KHOANG, "Khoảng trống ngắn",
                        "Hạn chế các khoảng nghỉ dài giữa những buổi học trong cùng ngày."),
                new PriorityItem(KEY_NGAY, "Ngày học yêu thích",
                        "Ưu tiên xếp lịch vào các ngày bạn đã chọn bên dưới.")
        ));

        khung.setPadding(new Insets(0, 26, 20, 26));
        khung.setSpacing(14);
        khung.setStyle("-fx-background-color: #F6F8FB;");

        VBox phaiCard = Functions.the(16);
        phaiCard.setPrefWidth(420);
        phaiCard.setMinWidth(360);
        cotPhai.setFillWidth(true);
        phaiCard.getChildren().add(cotPhai);

        if (hienTieuDe) {
            VBox header = new VBox(4,
                    Functions.chuDe("Thiết lập ưu tiên"),
                    Functions.moTa("Sau khi chọn các môn muốn đăng ký, bạn chỉ cần sắp xếp thứ tự ưu tiên để hệ thống tối ưu lịch theo hướng ưu tiên từ trên xuống."));
            khung.getChildren().addAll(header, phaiCard);
        } else {
            khung.getChildren().add(phaiCard);
        }

        setSpacing(0);
        setStyle("-fx-background-color: #F6F8FB;");
        getChildren().add(khung);
    }

    public void taiLai() {
        veCotPhai();
    }

    private void veCotPhai() {
        cotPhai.getChildren().clear();
        cotPhai.getChildren().addAll(
                Functions.tieuDeThe("Mức độ ưu tiên"),
                Functions.moTa("Kéo các điều kiện lên xuống để đặt thứ tự ưu tiên từ trên xuống."));

        VBox list = new VBox(10);
        for (int i = 0; i < priorityItems.size(); i++) {
            list.getChildren().add(tieuChiTheoThuTu(priorityItems.get(i), i));
        }

        cotPhai.getChildren().add(list);
        cotPhai.getChildren().addAll(Functions.vach(), theNut(), ghiChu());
        capNhatNgayYeuThich();
    }

    private VBox tieuChiTheoThuTu(PriorityItem item, int index) {
        VBox card = Functions.the(12);
        card.setUserData(item);
        card.setStyle(card.getStyle() + " -fx-cursor: hand;");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane dragBox = new StackPane();
        dragBox.setPrefSize(28, 28);
        dragBox.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 8;");
        Label dragIcon = new Label("⋮⋮");
        dragIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        dragIcon.setTextFill(Functions.MA_CHU_PHU);
        dragBox.getChildren().add(dragIcon);

        Label title = Functions.tieuDeThe(item.title());
        Label badge = Functions.nhanXam("Ưu tiên " + (index + 1));
        HBox left = new HBox(10, dragBox, title);
        left.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(left, Functions.khoangTrong(), badge);

        Label mo = Functions.moTa(item.description());
        int currentWeight = priorityWeight(index);
        Label current = Functions.nhanNho("Mức ưu tiên hiện tại: " + currentWeight + "/5");
        VBox body = new VBox(10, top, mo, current);

        if (KEY_NGAY.equals(item.key())) {
            FlowPane ngayRow = new FlowPane(10, 8);
            String[] tenNgay = {"T2", "T3", "T4", "T5", "T6", "T7"};
            for (int i = 0; i < tenNgay.length; i++) {
                int thu = i + 2;
                CheckBox chk = new CheckBox(tenNgay[i]);
                chk.setSelected(st.criteria.ngayChon().contains(thu));
                ngayChk.put(thu, chk);
                ngayRow.getChildren().add(chk);
            }
            body.getChildren().addAll(Functions.nhanNho("Các ngày ưu tiên"), ngayRow);
        }

        card.getChildren().add(body);

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
                int from = findItemIndexByKey(draggedKey);
                int to = priorityItems.indexOf(item);
                if (from >= 0 && from != to) {
                    movePriorityItem(from, to);
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

    private int findItemIndexByKey(String key) {
        for (int i = 0; i < priorityItems.size(); i++) {
            if (priorityItems.get(i).key().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void movePriorityItem(int from, int to) {
        if (from < 0 || to < 0 || from == to) {
            return;
        }
        PriorityItem item = priorityItems.remove(from);
        int targetIndex = to;
        if (to > from) {
            targetIndex = to;
        }
        priorityItems.add(Math.max(0, Math.min(targetIndex, priorityItems.size())), item);
    }

    private void capNhatNgayYeuThich() {
        int uuTienNgay = getPriorityWeight(KEY_NGAY);
        for (CheckBox checkBox : ngayChk.values()) {
            checkBox.setDisable(uuTienNgay == 0);
        }
    }

    private int getPriorityWeight(String key) {
        for (int i = 0; i < priorityItems.size(); i++) {
            if (priorityItems.get(i).key().equals(key)) {
                return priorityWeight(i);
            }
        }
        return 0;
    }

    private int priorityWeight(int index) {
        if (index < 0) return 0;
        return Math.max(1, 5 - index);
    }

    private VBox theNut() {
        VBox box = new VBox(8);
        Button macDinh = Functions.nutPhu("Khôi phục mặc định");
        macDinh.setMaxWidth(Double.MAX_VALUE);
        macDinh.setOnAction(e -> {
            Criteria d = Criteria.macDinh();
            priorityItems.clear();
            priorityItems.addAll(List.of(
                    new PriorityItem(KEY_SANG, "Ưu tiên buổi sáng",
                            "Xếp nhiều tiết học vào buổi sáng (tiết 1–5) hơn."),
                    new PriorityItem(KEY_IT_NGAY, "Ít ngày đến trường",
                            "Giảm số ngày phải lên trường trong tuần."),
                    new PriorityItem(KEY_KHOANG, "Khoảng trống ngắn",
                            "Hạn chế các khoảng nghỉ dài giữa những buổi học trong cùng ngày."),
                    new PriorityItem(KEY_NGAY, "Ngày học yêu thích",
                            "Ưu tiên xếp lịch vào các ngày bạn đã chọn bên dưới.")
            ));
            ngayChk.clear();
            veCotPhai();
            ngayChk.forEach((thu, chk) -> chk.setSelected(d.ngayChon().contains(thu)));
        });
        box.getChildren().add(macDinh);
        return box;
    }

    private VBox ghiChu() {
        VBox box = Functions.the(12);
        box.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 12; -fx-border-color: #BFDBFE; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        box.getChildren().addAll(
                Functions.nhanDeXuat("Ghi chú"),
                Functions.moTa("Thứ tự từ trên xuống là mức ưu tiên cao nhất đến thấp nhất. Hệ thống sẽ tự quy đổi thành trọng số 0–5 để tối ưu hóa lịch học."));
        return box;
    }

    public void luuCriteria() {
        List<Integer> ngay = new ArrayList<>();
        ngayChk.forEach((thu, chk) -> {
            if (chk.isSelected()) ngay.add(thu);
        });

        st.criteria = new Criteria(
                getPriorityWeight(KEY_SANG),
                getPriorityWeight(KEY_IT_NGAY),
                getPriorityWeight(KEY_KHOANG),
                getPriorityWeight(KEY_NGAY),
                ngay,
                3
        );
    }

    private record PriorityItem(String key, String title, String description) {
    }
}
