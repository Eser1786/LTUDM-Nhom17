package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.PlanDetail;
import com.tkb.frontend.model.Types.SavedPlan;
import com.tkb.frontend.net.ApiException;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavedPlansPanel extends VBox {
    // This panel lists previously saved timetable options and allows the user to reopen or compare them.
    private final TkbClient client;
    private final MainFrame frame;
    private final TextField txtTimKiem = Functions.oNhap();
    private final Button btnSoSanh = Functions.nutChinh("So sánh 2 lịch đã chọn");
    private final Map<Long, CheckBox> chkSoSanh = new LinkedHashMap<>();
    private List<SavedPlan> tatCa = List.of();

    public SavedPlansPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        setPadding(new Insets(22, 26, 18, 26));
        setSpacing(12);
        setStyle("-fx-background-color: #F6F8FB;");

        // Header and controls for searching and comparing saved plans.
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = Functions.chuDe("Thời khóa biểu đã lưu");
        Label desc = Functions.moTa("Mở lại lịch, đổi tên hoặc chọn 2 lịch để so sánh chi tiết.");
        header.getChildren().addAll(title, desc);

        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_RIGHT);
        txtTimKiem.setPromptText("Tìm lịch...");
        txtTimKiem.setPrefWidth(220);
        Button btnMoi = Functions.nutPhu("Tạo phương án mới");
        btnMoi.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));
        btnSoSanh.setDisable(true);
        btnSoSanh.setOnAction(e -> moSoSanh());
        controls.getChildren().addAll(txtTimKiem, btnMoi, btnSoSanh);

        getChildren().addAll(header, controls);
        txtTimKiem.textProperty().addListener((obs, old, val) -> veDanhSach());
    }

    public void taiDanhSach() {
        // Fetch the saved plans for the chosen semester before rendering the list.
        try {
            String ma = frame.trangThai().hocKy == null ? null : frame.trangThai().hocKy.ma();
            tatCa = client.listPlans(ma);
        } catch (ApiException ex) {
            Functions.loi(null, ex);
            return;
        }
        veDanhSach();
    }

    private List<SavedPlan> loc() {
        String tu = txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim().toLowerCase(Locale.ROOT);
        List<SavedPlan> out = new ArrayList<>();
        for (SavedPlan p : tatCa) {
            if (tu.isEmpty() || p.ten().toLowerCase(Locale.ROOT).contains(tu)) {
                out.add(p);
            }
        }
        return out;
    }

    private void veDanhSach() {
        // Rebuild the list every time the search text or selection changes.
        getChildren().removeIf(node -> getChildren().indexOf(node) > 1);
        List<SavedPlan> ds = loc();
        if (ds.isEmpty()) {
            Label empty = Functions.chuDe("Chưa có lịch nào được lưu");
            getChildren().add(empty);
            return;
        }
        for (SavedPlan p : ds) {
            // Each saved plan is rendered as a card with quick actions to open, rename, or delete it.
            VBox card = new VBox(6);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10;");
            CheckBox chk = new CheckBox();
            chk.setOnAction(e -> capNhatNutSoSanh());
            chkSoSanh.put(p.id(), chk);
            HBox top = new HBox(10, chk, new Label(p.ten()));
            top.setAlignment(Pos.CENTER_LEFT);
            Label meta = new Label("Tạo ngày " + p.ngayTao() + " · " + p.soHocPhan() + " học phần · " + p.soTinChi() + " tín chỉ · " + p.soBuoi() + " buổi");
            meta.setTextFill(Functions.MA_CHU_PHU);
            HBox actions = new HBox(8);
            Button mo = Functions.nutPhu("Mở lịch");
            Button doi = Functions.nutPhu("Đổi tên");
            Button xoa = Functions.nutXoa("Xóa");
            mo.setOnAction(e -> moLich(p));
            doi.setOnAction(e -> doiTen(p));
            xoa.setOnAction(e -> xoa(p));
            actions.getChildren().addAll(mo, doi, xoa);
            card.getChildren().addAll(top, meta, actions);
            getChildren().add(card);
        }
        capNhatNutSoSanh();
    }

    private void capNhatNutSoSanh() {
        // Compare button only enables when exactly two saved plans are selected.
        long selected = chkSoSanh.values().stream().filter(CheckBox::isSelected).count();
        btnSoSanh.setDisable(selected != 2);
        btnSoSanh.setText(selected == 2 ? "So sánh 2 lịch đã chọn" : "So sánh 2 lịch đã chọn");
    }

    private void moLich(SavedPlan p) {
        // Open a saved plan and show the timetable details in a temporary grid view.
        try {
            PlanDetail d = client.getPlan(p.id());
            TimetableGridPanel g = new TimetableGridPanel();
            g.setTieuDe("Phương án đã lưu: " + d.ten());
            g.setSlots(d.slots());
            VBox box = new VBox(g);
            box.setPadding(new Insets(8));
            Functions.thongTin(null, d.ten() + " · " + d.maHocKy());
        } catch (ApiException ex) {
            Functions.loi(null, ex);
        }
    }

    private void doiTen(SavedPlan p) { }
    private void xoa(SavedPlan p) { }
    private void moSoSanh() { }
}
