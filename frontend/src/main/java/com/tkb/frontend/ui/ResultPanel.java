package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.JobResult;
import com.tkb.frontend.model.Types.ScheduleOption;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.LinkedHashSet;
import java.util.Set;

public class ResultPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final Label lblTieuDe = Functions.chuDe("Kết quả đề xuất");
    private final Set<Integer> soSanhChon = new LinkedHashSet<>();
    private JobResult ketQua;
    private int chiSo;

    public ResultPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        setPadding(new Insets(22, 26, 18, 26));
        setSpacing(12);
        setStyle("-fx-background-color: #F6F8FB;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);
        Label info = Functions.moTa("Chọn một phương án để xem lịch chi tiết.");
        header.getChildren().addAll(lblTieuDe, info);

        Button btnXuat = Functions.nutPhu("Xuất ảnh PNG");
        Button btnSoSanh = Functions.nutPhu("So sánh lịch");
        Button btnLuu = Functions.nutChinh("Lưu thời khóa biểu");
        btnXuat.setOnAction(e -> xuatAnh());
        btnSoSanh.setOnAction(e -> moSoSanh());
        btnLuu.setOnAction(e -> luuPhuongAn());
        HBox actions = new HBox(8, btnXuat, btnSoSanh, btnLuu);
        actions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(header, actions);
    }

    public void hienTrangThai() {
        if (ketQua == null) {
            getChildren().setAll(Functions.chuDe("Chưa có kết quả đề xuất"));
        } else {
            hienKetQua(ketQua);
        }
    }

    public void hienKetQua(JobResult result) {
        this.ketQua = result;
        this.chiSo = 0;
        this.soSanhChon.clear();
        if (result == null || result.options().isEmpty()) {
            getChildren().setAll(Functions.chuDe("Chưa có kết quả đề xuất"));
            return;
        }
        lblTieuDe.setText("Kết quả đề xuất · " + result.options().size() + " phương án · " + result.maHocKy());
        getChildren().setAll(lblTieuDe, new Label("Đã nhận " + result.options().size() + " phương án."));
    }

    private void xuatAnh() { }
    private void moSoSanh() { }
    private void luuPhuongAn() { }

    public void veLai() { }
}
