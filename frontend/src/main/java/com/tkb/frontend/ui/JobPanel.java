package com.tkb.frontend.ui;

import com.tkb.frontend.model.Types.JobResult;
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

        Label title = Functions.chuDe("Đang tối ưu thời khóa biểu");
        Label info = Functions.moTa("Thuật toán đang cân nhắc các tổ hợp nhóm lớp theo ưu tiên của bạn.");
        progress.setPrefWidth(620);
        progress.setProgress(0.4);

        Button huy = Functions.nutPhu("Hủy xếp lịch");
        huy.setOnAction(e -> frame.jobHuy());

        HBox actionRow = new HBox(huy);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, info, progress, lblTrangThai, actionRow, lblLoi);
    }

    public void batDau(String jobId) {
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
        if (jobStatus == null) return;
        lblTrangThai.setText(jobStatus.message());
        if (jobStatus.status() == null) return;
        progress.setProgress(Math.min(1.0, jobStatus.progress() / 100.0));
    }
}
