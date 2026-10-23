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

/** Tráº¡ng thÃ¡i â€œÄang xá»­ lÃ½â€: thÃ´ng bÃ¡o, thanh tiáº¿n trÃ¬nh, há»§y. KÃ¨m tráº¡ng thÃ¡i â€œLá»—i káº¿t ná»‘iâ€. */
public class JobPanel extends VBox {
    private final TkbClient client;
    private final MainFrame frame;
    private final ProgressBar progress = new ProgressBar(0);
    private final Label lblTrangThai = new Label("Äang chuáº©n bá»‹...");
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

        Label td = Functions.chuDe("Äang tá»‘i Æ°u thá»i khÃ³a biá»ƒu");
        Label mt = Functions.moTa("Há»‡ thá»‘ng Ä‘ang cÃ¢n nháº¯c cÃ¡c tá»• há»£p nhÃ³m lá»›p theo má»©c Æ°u tiÃªn cá»§a báº¡n. "
                + "QuÃ¡ trÃ¬nh nÃ y chá»‰ mÃ´ phá»ng cá»¥c bá»™ trÃªn mÃ¡y cá»§a báº¡n.");

        progress.setPrefWidth(600);
        progress.setPrefHeight(14);
        progress.setProgress(0);

        HBox trangThai = new HBox(10, lblTrangThai, Functions.khoangTrong(), lblPhanTram);
        trangThai.setAlignment(Pos.CENTER_LEFT);

        nutBox.getChildren().addAll(theNutHuy());

        VBox bangTrong = Functions.the(14);
        bangTrong.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10;");
        bangTrong.getChildren().addAll(
                Functions.tieuDeThe("CÃ¡c bÆ°á»›c Ä‘ang thá»±c hiá»‡n"),
                Functions.moTa("1. Kiá»ƒm tra tá»• há»£p LTâ€“TH há»£p lá»‡.\n"
                        + "2. Loáº¡i bá» phÆ°Æ¡ng Ã¡n trÃ¹ng giá».\n"
                        + "3. Cháº¥m Ä‘iá»ƒm theo trá»ng sá»‘ Æ°u tiÃªn.\n"
                        + "4. Xáº¿p háº¡ng vÃ  chuáº©n bá»‹ phÆ°Æ¡ng Ã¡n A/B/C."));

        loiBox.setStyle("-fx-background-color: #FEF2F2; -fx-background-radius: 12; -fx-border-color: #FECACA; "
                + "-fx-border-radius: 12; -fx-border-width: 1;");
        loiBox.setVisible(false);
        loiBox.setManaged(false);

        card.getChildren().addAll(td, mt, Functions.vach(), progress, trangThai, nutBox, bangTrong, loiBox);
        getChildren().add(card);
    }

    private Button theNutHuy() {
        Button huy = Functions.nutPhu("Há»§y xáº¿p lá»‹ch");
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
        lblTrangThai.setText("Äang chuáº©n bá»‹ káº¿t quáº£...");
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
                hienLoi(true, status.thongBaoLoi() == null ? "Xáº¿p lá»‹ch tháº¥t báº¡i." : status.thongBaoLoi());
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
            lblTrangThai.setText("ChÆ°a thá»ƒ nháº­n káº¿t quáº£");
            progress.setProgress(0);
            lblPhanTram.setText("â€”");
            loiBox.getChildren().setAll(
                    Functions.nhanLoi("Lá»—i káº¿t ná»‘i"),
                    Functions.moTa("ChÆ°a thá»ƒ nháº­n káº¿t quáº£ xáº¿p lá»‹ch. Dá»¯ liá»‡u báº¡n Ä‘Ã£ chá»n váº«n Ä‘Æ°á»£c giá»¯ nguyÃªn. "
                            + (message == null ? "" : "\nChi tiáº¿t: " + message)));

            Button thuLai = Functions.nutChinh("Thá»­ láº¡i");
            Button quayLai = Functions.nutPhu("Quay láº¡i thiáº¿t láº­p");
            thuLai.setOnAction(e -> {
                hienLoi(false, null);
                frame.batDauXepLich();
            });
            quayLai.setOnAction(e -> frame.denTrang(MainFrame.CARD_PRIO));
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
            lblTrangThai.setText(pt < 30 ? "Äang kiá»ƒm tra tá»• há»£p há»£p lá»‡..."
                    : pt < 60 ? "Äang loáº¡i bá» phÆ°Æ¡ng Ã¡n trÃ¹ng giá»..."
                    : pt < 90 ? "Äang cháº¥m Ä‘iá»ƒm theo trá»ng sá»‘..."
                    : "Äang xáº¿p háº¡ng phÆ°Æ¡ng Ã¡n...");
        }
    }
}
