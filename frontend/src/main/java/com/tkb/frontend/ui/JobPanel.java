package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.JobStatus;
import com.tkb.frontend.net.TkbClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class JobPanel extends VBox {
    // This panel shows the background scheduler status while the timetable solver is running.
    private final TkbClient client;
    private final MainFrame frame;
    private final ProgressBar progress = new ProgressBar(0);
    private final Label lblTrangThai = new Label("Đang chuẩn bị...");
    private final Label lblLoi = new Label();
    private String jobId;

    public JobPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        setPadding(new Insets(24));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");

        // Header and status text explain the current scheduling step to the user.
        Label title = Functions.chuDe("Đang tối ưu thời khóa biểu");
        Label info = Functions.moTa("Thuật toán đang cân nhắc các tổ hợp nhóm lớp theo ưu tiên của bạn.");
        progress.setPrefWidth(620);
        progress.setProgress(0.4);

        // Users can cancel the running optimization without losing their current selections.
        Button huy = Functions.nutPhu("Hủy xếp lịch");
        huy.setOnAction(e -> frame.jobHuy());

        HBox actionRow = new HBox(huy);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, info, progress, lblTrangThai, actionRow, lblLoi);
    }

    public void batDau(String jobId) {
        // Start the progress display when a backend job begins.
        this.jobId = jobId;
        lblTrangThai.setText("Đang xử lý job " + jobId + "...");
        progress.setProgress(0.55);
    }

    public void dung() { }

    public void thuLai() { }

    public void hienLoi(String message) {
        lblLoi.setText(message == null ? "" : message);
        lblTrangThai.setText("Không thể theo dõi tiến độ");
    }

    public void capNhat(JobStatus jobStatus) {
        // Keep the status label and progress bar synchronized with the latest backend job update.
        if (jobStatus == null) return;
        String thongBao = jobStatus.thongBaoLoi();
        if (thongBao != null && !thongBao.isBlank()) {
            lblTrangThai.setText(thongBao);
            lblLoi.setText(thongBao);
        } else {
            lblTrangThai.setText("Đang xử lý...");
            lblLoi.setText("");
        }
        if (jobStatus.state() == null) return;
        progress.setProgress(Math.min(1.0, Math.max(0.0, jobStatus.phanTram() / 100.0)));
    }
}
