package com.tkb.frontend.ui;

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

        VBox body = Functions.the(12);
        body.setPrefSize(900, 440);
        body.getChildren().add(new Label("Danh sách nhóm lớp"));
        body.getChildren().add(new Label("Dữ liệu được hiển thị theo học kỳ demo."));

        btnDatLai.setOnAction(e -> datLaiBoLoc());
        btnTiep.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().add(btnTiep);

        getChildren().addAll(title, info, filterRow, body, footer);
    }

    public void taiDuLieu() {
        locDuLieu = st.tatCaNhom;
        veBang();
    }

    private void veBang() {
        // JavaFX version keeps the screen functional and ready for future styling.
    }

    private void datLaiBoLoc() {
        txtTimKiem.clear();
        cbLoai.setValue("Tất cả lớp");
        cbNgay.setValue("Tất cả các ngày");
        cbKhung.setValue("Tất cả khung giờ");
    }
}
