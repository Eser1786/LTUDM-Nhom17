package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.ClassSession;
import com.tkb.frontend.user.model.Types.Criteria;
import com.tkb.frontend.user.model.Types.Section;
import com.tkb.frontend.user.model.Types.SectionSlot;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình Thiết lập ưu tiên.
 * Chỉ giữ bảng mức độ ưu tiên; người dùng đã chọn môn ở màn hình trước.
 */
public class PriorityPanel extends VBox {
    private final MainFrame frame;
    private final MainFrame.AppUiState st;
    private final boolean hienTieuDe;
    private final VBox cotPhai = new VBox(12);
    private final Slider sSang = new Slider(0, 5, 3);
    private final Slider sItNgay = new Slider(0, 5, 3);
    private final Slider sKhoangTrong = new Slider(0, 5, 3);
    private final Slider sNgayYeuThich = new Slider(0, 5, 0);
    private final Map<Integer, CheckBox> ngayChk = new LinkedHashMap<>();
    private final Label lblSang = Functions.nhanChinh("3");
    private final Label lblItNgay = Functions.nhanChinh("3");
    private final Label lblKhoang = Functions.nhanChinh("3");
    private final Label lblNgayYeu = Functions.nhanChinh("0");
    private final VBox khung = new VBox(14);

    public PriorityPanel(MainFrame frame) {
        this(frame, true);
    }

    public PriorityPanel(MainFrame frame, boolean hienTieuDe) {
        this.hienTieuDe = hienTieuDe;
        this.frame = frame;
        this.st = frame.trangThai();

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
                    Functions.moTa("Sau khi chọn các môn muốn đăng ký, bạn chỉ cần điều chỉnh mức độ ưu tiên để hệ thống sắp xếp lịch theo độ phù hợp tốt nhất."));
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
                Functions.moTa("Kéo thanh trượt để tăng hoặc giảm mức độ quan trọng của từng tiêu chí."));

        cotPhai.getChildren().add(tieuChi("Ưu tiên buổi sáng",
                "Xếp nhiều tiết học vào buổi sáng (tiết 1–5) hơn.", sSang, lblSang));
        cotPhai.getChildren().add(tieuChi("Ít ngày đến trường",
                "Giảm số ngày phải lên trường trong tuần.", sItNgay, lblItNgay));
        cotPhai.getChildren().add(tieuChi("Khoảng trống ngắn",
                "Hạn chế các khoảng nghỉ dài giữa những buổi học trong cùng ngày.", sKhoangTrong, lblKhoang));
        cotPhai.getChildren().add(tieuChiNgay());
        cotPhai.getChildren().addAll(Functions.vach(), theNut(), ghiChu());

        sSang.valueProperty().addListener((o, a, b) -> lblSang.setText(String.valueOf((int) sSang.getValue())));
        sItNgay.valueProperty().addListener((o, a, b) -> lblItNgay.setText(String.valueOf((int) sItNgay.getValue())));
        sKhoangTrong.valueProperty().addListener((o, a, b) -> lblKhoang.setText(String.valueOf((int) sKhoangTrong.getValue())));
        sNgayYeuThich.valueProperty().addListener((o, a, b) -> {
            lblNgayYeu.setText(String.valueOf((int) sNgayYeuThich.getValue()));
            ngayChk.values().forEach(c -> c.setDisable(sNgayYeuThich.getValue() == 0));
        });
        ngayChk.values().forEach(c -> c.setDisable(sNgayYeuThich.getValue() == 0));
    }

    private VBox tieuChi(String td, String mt, Slider slider, Label giaTri) {
        VBox card = Functions.the(12);
        HBox top = new HBox(10, Functions.tieuDeThe(td), Functions.khoangTrong(), giaTri);
        top.setAlignment(Pos.CENTER_LEFT);
        Label mo = Functions.moTa(mt);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        HBox nhan = new HBox(Functions.nhanNho("0 · Không ưu tiên"), Functions.khoangTrong(),
                Functions.nhanNho("5 · Cao nhất"));
        card.getChildren().addAll(top, mo, slider, nhan);
        return card;
    }

    private VBox tieuChiNgay() {
        VBox card = Functions.the(12);
        HBox top = new HBox(10, Functions.tieuDeThe("Ngày học yêu thích"), Functions.khoangTrong(), lblNgayYeu);
        top.setAlignment(Pos.CENTER_LEFT);
        Label mo = Functions.moTa("Ưu tiên xếp lịch vào các ngày bạn đã chọn bên dưới.");
        sNgayYeuThich.setMajorTickUnit(1);
        sNgayYeuThich.setMinorTickCount(0);
        sNgayYeuThich.setBlockIncrement(1);
        sNgayYeuThich.setSnapToTicks(true);
        sNgayYeuThich.setShowTickMarks(true);
        sNgayYeuThich.setShowTickLabels(true);

        ngayChk.clear();
        FlowPane ngayRow = new FlowPane(10, 8);
        String[] tenNgay = {"T2", "T3", "T4", "T5", "T6", "T7"};
        for (int i = 0; i < tenNgay.length; i++) {
            int thu = i + 2;
            CheckBox chk = new CheckBox(tenNgay[i]);
            chk.setSelected(st.criteria.ngayChon().contains(thu));
            ngayChk.put(thu, chk);
            ngayRow.getChildren().add(chk);
        }
        HBox nhan = new HBox(Functions.nhanNho("0 · Không ưu tiên"), Functions.khoangTrong(),
                Functions.nhanNho("5 · Cao nhất"));
        card.getChildren().addAll(top, mo, sNgayYeuThich, nhan, Functions.nhanNho("Các ngày ưu tiên"), ngayRow);
        return card;
    }

    private VBox theNut() {
        VBox box = new VBox(8);
        Button macDinh = Functions.nutPhu("Khôi phục mặc định");
        macDinh.setMaxWidth(Double.MAX_VALUE);
        macDinh.setOnAction(e -> {
            Criteria d = Criteria.macDinh();
            sSang.setValue(d.uuTienSang());
            sItNgay.setValue(d.itNgay());
            sKhoangTrong.setValue(d.khoangTrong());
            sNgayYeuThich.setValue(d.ngayYeuThich());
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
                Functions.moTa("Trọng số chỉ dùng để xếp hạng các lịch. Mọi phương án hợp lệ đều phải "
                        + "không trùng giờ và tuân thủ điều kiện ghép nhóm lý thuyết – thực hành."));
        return box;
    }

    public void luuCriteria() {
        List<Integer> ngay = new ArrayList<>();
        ngayChk.forEach((thu, chk) -> {
            if (chk.isSelected()) ngay.add(thu);
        });
        st.criteria = new Criteria((int) sSang.getValue(), (int) sItNgay.getValue(),
                (int) sKhoangTrong.getValue(), (int) sNgayYeuThich.getValue(), ngay, 3);
    }
}
