package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.model.Types.JobStatus;
import com.tkb.frontend.user.net.ApiException;
import com.tkb.frontend.user.net.TkbClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Trạng thái “Đang xử lý”: thông báo, thanh tiến trình, hủy. Kèm trạng thái “Lỗi kết nối”. */
public class JobPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final ProgressBar progress = new ProgressBar(0);
    private final Label lblTrangThai = new Label("Đang chuẩn bị...");
    private final Label lblPhanTram = Functions.nhanChinh("0%");
    private final VBox nutBox = new VBox(8);
    private final VBox loiBox = Functions.the(14);
    private String jobId;
    private Timeline poller;
    private Runnable onHuy;

    public JobPanel(TkbClient client, MainFrame frame) {
        this.client = client;
        this.frame = frame;
        setPadding(new Insets(40, 60, 40, 60));
        setSpacing(16);
        setStyle("-fx-background-color: #F6F8FB;");
        setAlignment(Pos.CENTER);

        VBox card = Functions.the(28);
        card.setMaxWidth(680);
        card.setSpacing(14);

        Label td = Functions.chuDe("Đang tối ưu thời khóa biểu");
        Label mt = Functions.moTa("Hệ thống đang cân nhắc các tổ hợp nhóm lớp theo mức ưu tiên của bạn. "
                + "Quá trình này chỉ mô phỏng cục bộ trên máy của bạn.");

        progress.setPrefWidth(600);
        progress.setPrefHeight(14);
        progress.setProgress(0);

        HBox trangThai = new HBox(10, lblTrangThai, Functions.khoangTrong(), lblPhanTram);
        trangThai.setAlignment(Pos.CENTER_LEFT);

        nutBox.getChildren().addAll(theNutHuy());

        VBox bangTrong = Functions.the(14);
        bangTrong.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10;");
        bangTrong.getChildren().addAll(
                Functions.tieuDeThe("Các bước đang thực hiện"),
                Functions.moTa("1. Kiểm tra tổ hợp LT–TH hợp lệ.\n"
                        + "2. Loại bỏ phương án trùng giờ.\n"
                        + "3. Chấm điểm theo trọng số ưu tiên.\n"
                        + "4. Xếp hạng và chuẩn bị phương án A/B/C."));

        loiBox.setStyle("-fx-background-color: #FEF2F2; -fx-background-radius: 12; -fx-border-color: #FECACA; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        loiBox.setVisible(false);
        loiBox.setManaged(false);

        card.getChildren().addAll(td, mt, Functions.vach(), progress, trangThai, nutBox, bangTrong, loiBox);
        getChildren().add(card);
    }

    private Button theNutHuy() {
        Button huy = Functions.nutPhu("Hủy xếp lịch");
        huy.setOnAction(e -> {
            dungPoll();
            try {
                if (jobId != null) {
                    client.cancel(jobId);
                }
            } catch (ApiException ignored) {
            }
            if (onHuy != null) {
                onHuy.run();
            }
        });
        return huy;
    }

    public void batDau(String jobId, Runnable onHuy) {
        this.jobId = jobId;
        this.onHuy = onHuy;
        hienLoi(false, null);
        lblTrangThai.setText("Đang chuẩn bị kết quả...");
        progress.setProgress(0.02);
        lblPhanTram.setText("0%");
        dungPoll();
        poller = new Timeline(new KeyFrame(Duration.millis(160), e -> poll()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    private void poll() {
        try {
            JobStatus status = client.jobStatus(jobId);
            capNhat(status);
            if (status.state() == JobStatus.State.DONE) {
                dungPoll();
                frame.jobXong(status.ketQua());
            } else if (status.state() == JobStatus.State.CANCELLED) {
                dungPoll();
                frame.jobHuy();
            } else if (status.state() == JobStatus.State.FAILED) {
                dungPoll();
                hienLoi(true, status.thongBaoLoi() == null ? "Xếp lịch thất bại." : status.thongBaoLoi());
            }
        } catch (ApiException ex) {
            dungPoll();
            hienLoi(true, ex.getMessage());
        }
    }

    public void dung() {
        dungPoll();
    }

    private void dungPoll() {
        if (poller != null) {
            poller.stop();
            poller = null;
        }
    }

    public void hienLoi(String message) {
        hienLoi(true, message);
    }

    private void hienLoi(boolean hien, String message) {
        Platform.runLater(() -> {
            loiBox.setVisible(hien);
            loiBox.setManaged(hien);
            if (!hien) {
                loiBox.getChildren().clear();
                nutBox.getChildren().setAll(theNutHuy());
                return;
            }
            lblTrangThai.setText("Chưa thể nhận kết quả");
            progress.setProgress(0);
            lblPhanTram.setText("—");
            loiBox.getChildren().setAll(
                    Functions.nhanLoi("Lỗi kết nối"),
                    Functions.moTa("Chưa thể nhận kết quả xếp lịch. Dữ liệu bạn đã chọn vẫn được giữ nguyên. "
                            + (message == null ? "" : "\nChi tiết: " + message)));

            Button thuLai = Functions.nutChinh("Thử lại");
            Button quayLai = Functions.nutPhu("Quay lại thiết lập");
            thuLai.setOnAction(e -> {
                hienLoi(false, null);
                frame.batDauXepLich();
            });
            quayLai.setOnAction(e -> frame.denTrang(MainFrame.CARD_REQ));
            HBox nut = new HBox(10, quayLai, thuLai);
            nut.setAlignment(Pos.CENTER_LEFT);
            nutBox.getChildren().setAll(nut);
        });
    }

    public void capNhat(JobStatus jobStatus) {
        // Keep the status label and progress bar synchronized with the latest backend job update.
        if (jobStatus == null) return;
        int pt = Math.max(0, Math.min(100, jobStatus.phanTram()));
        progress.setProgress(pt / 100.0);
        lblPhanTram.setText(pt + "%");
        if (jobStatus.state() == JobStatus.State.RUNNING) {
            lblTrangThai.setText(pt < 30 ? "Đang kiểm tra tổ hợp hợp lệ..."
                    : pt < 60 ? "Đang loại bỏ phương án trùng giờ..."
                    : pt < 90 ? "Đang chấm điểm theo trọng số..."
                    : "Đang xếp hạng phương án...");
        }
    }
}
