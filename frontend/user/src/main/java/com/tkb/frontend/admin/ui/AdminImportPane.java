package com.tkb.frontend.admin.ui;

import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.Response;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class AdminImportPane extends ScrollPane {
    private static final int MAX_CHUNK_SIZE_BYTES = 512 * 1024;
    private static final long MAX_PDF_SIZE_BYTES = 20L * 1024L * 1024L;

    private final AdminRequestClient requests;
    private final ComboBox<AdminSemesterOption> semesterSelector = new ComboBox<>();
    private final Label fileLabel = new Label("Chưa chọn tệp PDF");
    private final Label statusLabel = new Label();
    private final Label reportSummary = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final Button chooseButton = new Button("Chọn tệp PDF");
    private final Button uploadButton = new Button("Tải PDF lên máy chủ");
    private final Button abortButton = new Button("Hủy phiên tải");
    private final Button previewButton = new Button("Xem thử, chưa ghi dữ liệu");
    private final Button confirmButton = new Button("Xác nhận ghi thật");
    private final TableView<ImportIssue> issueTable = new TableView<>();
    private final Map<String, Response> earlyDonePushes = new LinkedHashMap<>();

    private File selectedPdf;
    private String uploadId;
    private String semesterCode;
    private String stagedFileName;
    private String activeJobId;
    private boolean semestersLoaded;
    private boolean loadingSemesters;
    private boolean uploading;
    private boolean aborting;
    private boolean submittingJob;
    private boolean awaitingJobId;
    private boolean activeDryRun;
    private boolean previewReady;

    AdminImportPane(AdminRequestClient requests) {
        this.requests = requests;
        setFitToWidth(true);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setContent(createContent());
        requests.addPushListener(response -> Platform.runLater(() -> handlePush(response)));
        updateControls();
    }

    void onShown() {
        if (!semestersLoaded) {
            loadSemesters();
        }
    }

    void refreshSemesters() {
        loadSemesters();
    }

    private VBox createContent() {
        Label semesterTitle = new Label("Học kỳ đích");
        styleFieldTitle(semesterTitle);
        semesterSelector.setPromptText("Chọn học kỳ");
        semesterSelector.setMaxWidth(Double.MAX_VALUE);
        semesterSelector.setCellFactory(list -> semesterCell());
        semesterSelector.setButtonCell(semesterCell());
        semesterSelector.valueProperty().addListener((observable, oldValue, newValue) -> updateControls());

        Button reloadSemestersButton = new Button("Tải lại học kỳ");
        styleSecondaryButton(reloadSemestersButton);
        reloadSemestersButton.setOnAction(event -> loadSemesters());
        HBox semesterRow = new HBox(12, semesterSelector, reloadSemestersButton);
        semesterRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(semesterSelector, Priority.ALWAYS);

        Label semesterHint = new Label("Chọn học kỳ mà dữ liệu thời khóa biểu sẽ được nhập vào.");
        styleHint(semesterHint);
        Label fileTitle = new Label("Tệp PDF");
        styleFieldTitle(fileTitle);
        fileLabel.setTextFill(Color.web("#82747E"));
        fileLabel.setWrapText(true);
        styleSecondaryButton(chooseButton);
        chooseButton.setOnAction(event -> choosePdf());
        HBox fileRow = new HBox(12, chooseButton, fileLabel);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        Label fileHint = new Label("PDF tối đa 20 MiB; mỗi chunk tối đa 512 KiB, gửi tuần tự.");
        styleHint(fileHint);
        stylePrimaryButton(uploadButton, "#8D456B");
        uploadButton.setOnAction(event -> uploadPdf());
        abortButton.setOnAction(event -> abortUpload());
        abortButton.setStyle("-fx-background-color: #FBEAEC; -fx-text-fill: #B4234D; "
                + "-fx-background-radius: 8; -fx-padding: 10 18 10 18; -fx-cursor: hand;");
        HBox uploadActions = new HBox(10, uploadButton, abortButton);
        uploadActions.setAlignment(Pos.CENTER_LEFT);

        styleSecondaryButton(previewButton);
        previewButton.setText("Xem thử, chưa ghi dữ liệu");
        previewButton.setOnAction(event -> startImport(true));
        stylePrimaryButton(confirmButton, "#287A55");
        confirmButton.setText("Xác nhận ghi thật");
        confirmButton.setOnAction(event -> confirmCommit());
        HBox reviewActions = new HBox(10, previewButton, confirmButton);
        reviewActions.setAlignment(Pos.CENTER_LEFT);

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setPrefHeight(12);
        statusLabel.setWrapText(true);
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.web("#82747E"));

        VBox uploadCard = new VBox(14,
                semesterTitle,
                semesterRow,
                semesterHint,
                new Separator(),
                fileTitle,
                fileRow,
                fileHint,
                uploadActions,
                progress,
                statusLabel,
                new Separator(),
                new Label("Xem thử và ghi dữ liệu"),
                reviewActions);
        styleCard(uploadCard);
        uploadCard.setMaxWidth(900);

        Label reportTitle = new Label("Báo cáo kiểm tra PDF");
        reportTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        reportTitle.setTextFill(Color.web("#493343"));
        reportSummary.setText("Chưa có báo cáo. Hãy tải PDF lên rồi chọn Xem thử.");
        reportSummary.setWrapText(true);
        reportSummary.setTextFill(Color.web("#82747E"));
        reportSummary.setFont(Font.font("Segoe UI", 12));
        configureIssueTable();
        VBox reportCard = new VBox(12, reportTitle, reportSummary, issueTable);
        styleCard(reportCard);
        VBox.setVgrow(issueTable, Priority.ALWAYS);

        VBox content = new VBox(16, uploadCard, reportCard);
        content.setPadding(new Insets(8, 0, 16, 0));
        content.setFillWidth(true);
        return content;
    }

    private ListCell<AdminSemesterOption> semesterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AdminSemesterOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        };
    }

    private void configureIssueTable() {
        TableColumn<ImportIssue, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().type()));
        typeColumn.setPrefWidth(110);
        TableColumn<ImportIssue, String> rowColumn = new TableColumn<>("STT");
        rowColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Integer.toString(cell.getValue().row())));
        rowColumn.setPrefWidth(80);
        TableColumn<ImportIssue, String> messageColumn = new TableColumn<>("Nội dung");
        messageColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().message()));
        messageColumn.setPrefWidth(650);
        messageColumn.setMinWidth(300);
        issueTable.getColumns().setAll(typeColumn, rowColumn, messageColumn);
        issueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        issueTable.setPlaceholder(new Label("Không có lỗi hoặc cảnh báo."));
        issueTable.setPrefHeight(300);
    }

    private void loadSemesters() {
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải danh sách học kỳ.");
            return;
        }
        if (loadingSemesters || uploading || aborting || submittingJob) {
            return;
        }
        loadingSemesters = true;
        statusLabel.setTextFill(Color.web("#82747E"));
        statusLabel.setText("Đang tải danh sách học kỳ...");
        updateControls();
        requests.send(Command.LIST_SEMESTERS, Map.of()).whenComplete((data, failure) ->
                Platform.runLater(() -> {
                    loadingSemesters = false;
                    if (failure != null) {
                        semestersLoaded = false;
                        showError("Không thể tải danh sách học kỳ: "
                                + AdminRequestClient.messageOf(failure));
                    } else {
                        try {
                            List<AdminSemesterOption> semesters = AdminSemesterOption.fromResponse(data);
                            String selectedCode = semesterSelector.getValue() == null
                                    ? null : semesterSelector.getValue().code();
                            semesterSelector.setItems(FXCollections.observableArrayList(semesters));
                            semestersLoaded = true;
                            AdminSemesterOption selected = semesters.stream()
                                    .filter(item -> Objects.equals(item.code(), selectedCode))
                                    .findFirst()
                                    .orElseGet(() -> semesters.stream()
                                            .filter(AdminSemesterOption::current)
                                            .findFirst()
                                            .orElse(semesters.isEmpty() ? null : semesters.get(0)));
                            semesterSelector.setValue(selected);
                            if (semesters.isEmpty()) {
                                showError("Chưa có học kỳ nào. Hãy tạo học kỳ trước khi nhập dữ liệu.");
                            } else {
                                showStatus("Đã tải " + semesters.size() + " học kỳ.");
                            }
                        } catch (IllegalArgumentException ex) {
                            semestersLoaded = false;
                            showError(ex.getMessage());
                        }
                    }
                    updateControls();
                }));
    }

    private void choosePdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp thời khóa biểu PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp PDF", "*.pdf"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            showError("Chỉ chấp nhận tệp có phần mở rộng .pdf.");
            return;
        }
        try {
            long size = Files.size(file.toPath());
            if (size <= 0 || size > MAX_PDF_SIZE_BYTES) {
                showError("Kích thước PDF phải lớn hơn 0 và không vượt quá 20 MiB.");
                return;
            }
            selectedPdf = file;
            fileLabel.setText(file.getName() + " (" + formatFileSize(size) + ")");
            progress.setProgress(0);
            clearReport();
            showStatus("Đã chọn tệp. Hãy xác nhận học kỳ rồi tải lên.");
            updateControls();
        } catch (IOException ex) {
            showError("Không thể đọc thông tin tệp PDF: " + AdminRequestClient.messageOf(ex));
        }
    }

    private void uploadPdf() {
        AdminSemesterOption semester = semesterSelector.getValue();
        if (!requests.isAvailable()) {
            showError("Không có kết nối máy chủ để tải tệp lên.");
            return;
        }
        if (!semestersLoaded || semester == null || selectedPdf == null) {
            showError("Hãy chọn học kỳ và tệp PDF trước khi tải lên.");
            return;
        }
        if (uploadId != null) {
            showError("Đang có một tệp đã tải lên chờ xử lý. Hãy hủy phiên đó trước.");
            return;
        }

        uploading = true;
        progress.setProgress(0);
        showStatus("Đang chuẩn bị tải PDF...");
        updateControls();
        CompletableFuture.supplyAsync(() -> uploadInBackground(selectedPdf, semester))
                .whenComplete((result, failure) -> Platform.runLater(() -> {
                    uploading = false;
                    if (failure != null) {
                        showError("Tải PDF thất bại: " + AdminRequestClient.messageOf(failure));
                    } else {
                        uploadId = result.uploadId();
                        semesterCode = semester.code();
                        stagedFileName = selectedPdf.getName();
                        previewReady = false;
                        clearReport();
                        progress.setProgress(1);
                        setStatus("Đã tải lên " + result.chunkCount()
                                + " chunk. Có thể chạy xem thử.");
                    }
                    updateControls();
                }));
    }

    private UploadResult uploadInBackground(File file, AdminSemesterOption semester) {
        String currentUploadId = null;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length == 0 || bytes.length > MAX_PDF_SIZE_BYTES) {
                throw new IllegalArgumentException("Kích thước PDF phải lớn hơn 0 và không vượt quá 20 MiB.");
            }
            Map<String, Object> begin = requests.send(Command.IMPORT_BEGIN, Map.of(
                    "fileName", file.getName(),
                    "sizeBytes", bytes.length,
                    "semesterCode", semester.code())).join();
            Object rawId = begin.get("uploadId");
            Object rawChunkSize = begin.get("chunkSize");
            if (!(rawId instanceof String id) || id.isBlank()
                    || !(rawChunkSize instanceof Number chunkSizeNumber)) {
                throw new IllegalStateException("Máy chủ trả về thông tin phiên tải không hợp lệ.");
            }
            int chunkSize = chunkSizeNumber.intValue();
            if (chunkSize <= 0 || chunkSize > MAX_CHUNK_SIZE_BYTES
                    || chunkSizeNumber.doubleValue() != chunkSize) {
                throw new IllegalStateException("Kích thước chunk máy chủ yêu cầu không hợp lệ.");
            }
            currentUploadId = id;
            int chunkCount = (bytes.length + chunkSize - 1) / chunkSize;
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (int index = 0; index < chunkCount; index++) {
                int chunkIndex = index;
                int offset = chunkIndex * chunkSize;
                int end = Math.min(bytes.length, offset + chunkSize);
                String base64 = Base64.getEncoder().encodeToString(
                        java.util.Arrays.copyOfRange(bytes, offset, end));
                chain = chain.thenCompose(ignored -> requests.send(Command.IMPORT_CHUNK, Map.of(
                                "uploadId", id,
                                "index", chunkIndex,
                                "dataBase64", base64))
                        .thenAccept(data -> Platform.runLater(() -> {
                            progress.setProgress((chunkIndex + 1.0) / chunkCount);
                            statusLabel.setText("Đang tải chunk " + (chunkIndex + 1)
                                    + "/" + chunkCount + "...");
                        })));
            }
            chain.join();
            return new UploadResult(id, chunkCount);
        } catch (IOException | RuntimeException ex) {
            if (currentUploadId != null) {
                try {
                    requests.send(Command.IMPORT_ABORT, Map.of("uploadId", currentUploadId)).join();
                } catch (RuntimeException abortFailure) {
                    throw new CompletionException(new IllegalStateException(
                            AdminRequestClient.messageOf(ex) + " (không hủy được phiên tải: "
                                    + AdminRequestClient.messageOf(abortFailure) + ")", ex));
                }
            }
            throw new CompletionException(ex);
        }
    }

    private void startImport(boolean dryRun) {
        if (!requests.isAvailable() || uploadId == null) {
            showError("Hãy tải tệp PDF lên máy chủ trước.");
            return;
        }
        if (submittingJob || uploading) {
            return;
        }
        if (!dryRun && !previewReady) {
            showError("Hãy chạy xem thử thành công, không có lỗi, trước khi ghi dữ liệu.");
            return;
        }
        submittingJob = true;
        activeDryRun = dryRun;
        activeJobId = null;
        awaitingJobId = true;
        earlyDonePushes.clear();
        previewReady = false;
        if (dryRun) {
            clearReport();
        }
        progress.setProgress(0);
        showStatus(dryRun ? "Đang gửi yêu cầu xem thử..." : "Đang gửi yêu cầu ghi dữ liệu...");
        updateControls();
        requests.send(Command.IMPORT_COMMIT, Map.of(
                "uploadId", uploadId,
                "dryRun", dryRun)).whenComplete((data, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                finishJobFailure("Không thể bắt đầu xử lý PDF: "
                        + AdminRequestClient.messageOf(failure));
                return;
            }
            Object rawJobId = data.get("jobId");
            if (!(rawJobId instanceof String jobId) || jobId.isBlank()) {
                finishJobFailure("Máy chủ không trả về mã công việc nhập PDF hợp lệ.");
                return;
            }
            activeJobId = jobId;
            awaitingJobId = false;
            showStatus("Đang xử lý PDF trên máy chủ...");
            updateControls();
            Response earlyDone = earlyDonePushes.remove(jobId);
            earlyDonePushes.clear();
            if (earlyDone != null) {
                handleJobDone(earlyDone);
            }
        }));
    }

    private void confirmCommit() {
        if (uploadId == null || !previewReady) {
            showError("Chỉ có thể ghi dữ liệu sau khi xem thử không có lỗi.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận nhập dữ liệu");
        confirmation.setHeaderText("Ghi dữ liệu thời khóa biểu vào hệ thống?");
        confirmation.setContentText("Tệp: " + stagedFileName + "\nHọc kỳ: " + semesterCode
                + "\nChỉ xác nhận khi báo cáo xem thử đã được kiểm tra.");
        confirmation.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> startImport(false));
    }

    private void handlePush(Response push) {
        if (push == null || !push.isPush()
                || (!Command.JOB_PROGRESS.name().equals(push.getPush())
                && !Command.JOB_DONE.name().equals(push.getPush()))) {
            return;
        }
        Map<String, Object> data = push.getData();
        if (data == null || !"IMPORT".equals(data.get("jobType"))) {
            return;
        }
        Object rawJobId = data.get("jobId");
        if (!(rawJobId instanceof String jobId) || jobId.isBlank()) {
            if (submittingJob || activeJobId != null) {
                showError("Gói tiến độ nhập PDF thiếu jobId hợp lệ.");
            }
            return;
        }
        if (activeJobId == null && awaitingJobId && Command.JOB_DONE.name().equals(push.getPush())) {
            earlyDonePushes.put(jobId, push);
            return;
        }
        if (!Objects.equals(activeJobId, jobId)) {
            return;
        }
        if (Command.JOB_PROGRESS.name().equals(push.getPush())) {
            Object rawPercent = data.get("percent");
            if (rawPercent instanceof Number number
                    && number.doubleValue() == number.intValue()
                    && number.intValue() >= 0 && number.intValue() <= 100) {
                progress.setProgress(number.intValue() / 100.0);
            }
            Object rawMessage = data.get("message");
            setStatus(rawMessage instanceof String message && !message.isBlank()
                    ? message : "Đang xử lý PDF trên máy chủ...");
        } else {
            handleJobDone(push);
        }
    }

    private void handleJobDone(Response push) {
        Map<String, Object> data = push.getData();
        if (data == null || !Objects.equals(activeJobId, data.get("jobId"))) {
            return;
        }
        String status = data.get("status") instanceof String value ? value : "";
        submittingJob = false;
        awaitingJobId = false;
        activeJobId = null;
        if (!"DONE".equals(status)) {
            previewReady = false;
            Object errorObject = data.get("error");
            String message = errorObject instanceof Map<?, ?> error
                    && error.get("message") instanceof String errorMessage
                    && !errorMessage.isBlank()
                    ? errorMessage
                    : ("CANCELLED".equals(status)
                            ? "Công việc nhập PDF đã bị hủy."
                            : "Công việc nhập PDF thất bại với trạng thái " + status + ".");
            finishJobFailure(message);
            return;
        }

        Object rawReport = data.get("result");
        if (!(rawReport instanceof Map<?, ?> reportMap)) {
            previewReady = false;
            finishJobFailure("Máy chủ không trả về ImportReport hợp lệ.");
            return;
        }
        try {
            ImportReport report = ImportReport.parse(reportMap);
            if (report.dryRun() != activeDryRun) {
                throw new IllegalArgumentException("ImportReport không khớp chế độ xem thử/ghi dữ liệu.");
            }
            showReport(report);
            if (report.dryRun()) {
                previewReady = report.errors().isEmpty();
                if (previewReady) {
                    showSuccess("Xem thử hoàn tất. Hãy kiểm tra báo cáo trước khi xác nhận ghi thật.");
                } else {
                    showError("Xem thử phát hiện lỗi. Hãy sửa dữ liệu nguồn trước khi ghi thật.");
                }
            } else if (report.errors().isEmpty()) {
                uploadId = null;
                semesterCode = null;
                previewReady = false;
                showSuccess("Đã ghi dữ liệu thành công vào học kỳ.");
            } else {
                previewReady = false;
                showError("Máy chủ báo lỗi khi ghi dữ liệu; hãy xem chi tiết theo STT.");
            }
        } catch (IllegalArgumentException ex) {
            previewReady = false;
            finishJobFailure("Không thể đọc ImportReport: " + ex.getMessage());
        }
        updateControls();
    }

    private void showReport(ImportReport report) {
        issueTable.setItems(FXCollections.observableArrayList(report.allIssues()));
        reportSummary.setText(String.format(
                java.util.Locale.ROOT,
                "%s — học kỳ %s, %d dòng được phân tích. Học phần: %d thêm, %d cập nhật. "
                        + "Nhóm lớp: %d thêm, %d cập nhật, %d ngừng hoạt động. %d lỗi, %d cảnh báo.",
                report.dryRun() ? "Xem thử" : "Kết quả ghi dữ liệu",
                semesterCode,
                report.detectedRows(),
                report.coursesInserted(),
                report.coursesUpdated(),
                report.sectionsInserted(),
                report.sectionsUpdated(),
                report.sectionsDeactivated(),
                report.errors().size(),
                report.warnings().size()));
    }

    private void abortUpload() {
        if (uploadId == null || !requests.isAvailable() || uploading || aborting || submittingJob) {
            return;
        }
        aborting = true;
        showStatus("Đang hủy phiên tải...");
        updateControls();
        requests.send(Command.IMPORT_ABORT, Map.of("uploadId", uploadId))
                .whenComplete((data, failure) -> Platform.runLater(() -> {
                    aborting = false;
                    if (failure != null) {
                        showError("Không thể hủy phiên tải: "
                                + AdminRequestClient.messageOf(failure));
                    } else {
                        uploadId = null;
                        semesterCode = null;
                        stagedFileName = null;
                        previewReady = false;
                        progress.setProgress(0);
                        clearReport();
                        showStatus("Đã hủy phiên tải. Có thể chọn tệp khác.");
                    }
                    updateControls();
                }));
    }

    private void finishJobFailure(String message) {
        submittingJob = false;
        awaitingJobId = false;
        activeJobId = null;
        earlyDonePushes.clear();
        showError(message);
        updateControls();
    }

    private void clearReport() {
        issueTable.getItems().clear();
        reportSummary.setText("Chưa có báo cáo. Hãy tải PDF lên rồi chọn Xem thử.");
    }

    private void updateControls() {
        boolean staged = uploadId != null;
        boolean busy = uploading || aborting || submittingJob;
        semesterSelector.setDisable(loadingSemesters || busy || staged);
        chooseButton.setDisable(busy || staged);
        uploadButton.setDisable(!requests.isAvailable() || !semestersLoaded
                || semesterSelector.getValue() == null || selectedPdf == null
                || loadingSemesters || busy || staged);
        abortButton.setDisable(!staged || busy);
        previewButton.setDisable(!staged || busy || loadingSemesters);
        confirmButton.setDisable(!staged || busy || !previewReady);
    }

    private void showStatus(String message) {
        setStatus(message);
        statusLabel.setTextFill(Color.web("#82747E"));
    }

    private void showSuccess(String message) {
        setStatus(message);
        statusLabel.setTextFill(Color.web("#287A55"));
    }

    private void showError(String message) {
        setStatus(message);
        statusLabel.setTextFill(Color.web("#B4234D"));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void styleCard(VBox card) {
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #F0E7EC; -fx-border-radius: 14;");
    }

    private void styleFieldTitle(Label label) {
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#503A4A"));
    }

    private void styleHint(Label label) {
        label.setTextFill(Color.web("#82747E"));
        label.setFont(Font.font("Segoe UI", 12));
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: #F4EAF0; -fx-text-fill: #75405E; "
                + "-fx-background-radius: 8; -fx-padding: 9 14 9 14; -fx-cursor: hand;");
    }

    private void stylePrimaryButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-background-radius: 8; -fx-padding: 10 18 10 18; -fx-cursor: hand;");
    }

    private String formatFileSize(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.2f MiB", bytes / (1024.0 * 1024.0));
    }

    private record UploadResult(String uploadId, int chunkCount) {
    }

    private record ImportIssue(String type, int row, String message) {
    }

    private record ImportReport(
            boolean dryRun,
            int detectedRows,
            int coursesInserted,
            int coursesUpdated,
            int sectionsInserted,
            int sectionsUpdated,
            int sectionsDeactivated,
            List<ImportIssue> errors,
            List<ImportIssue> warnings,
            List<ImportIssue> allIssues) {

        static ImportReport parse(Map<?, ?> data) {
            Object rawDryRun = data.get("dryRun");
            if (!(rawDryRun instanceof Boolean dryRun)) {
                throw new IllegalArgumentException("Thiếu trường dryRun.");
            }
            int detectedRows = readCount(data, "detectedRows");
            Map<?, ?> courseStats = requireMap(data.get("courses"), "courses");
            Map<?, ?> sectionStats = requireMap(data.get("sections"), "sections");
            List<ImportIssue> errors = parseIssues(data.get("errors"), "Lỗi");
            List<ImportIssue> warnings = parseIssues(data.get("warnings"), "Cảnh báo");
            List<ImportIssue> allIssues = new ArrayList<>(errors.size() + warnings.size());
            allIssues.addAll(errors);
            allIssues.addAll(warnings);
            allIssues.sort(Comparator.comparingInt(ImportIssue::row).thenComparing(ImportIssue::type));
            return new ImportReport(dryRun, detectedRows,
                    readCount(courseStats, "inserted"),
                    readCount(courseStats, "updated"),
                    readCount(sectionStats, "inserted"),
                    readCount(sectionStats, "updated"),
                    readCount(sectionStats, "deactivated"),
                    errors, warnings, allIssues);
        }

        private static Map<?, ?> requireMap(Object value, String field) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Trường '" + field + "' không hợp lệ.");
            }
            return map;
        }

        private static List<ImportIssue> parseIssues(Object value, String type) {
            if (!(value instanceof List<?> rows)) {
                throw new IllegalArgumentException("Danh sách " + type.toLowerCase(
                        java.util.Locale.ROOT) + " không hợp lệ.");
            }
            List<ImportIssue> issues = new ArrayList<>(rows.size());
            for (Object item : rows) {
                if (!(item instanceof Map<?, ?> rowData)) {
                    throw new IllegalArgumentException("Một mục trong danh sách " + type.toLowerCase(
                            java.util.Locale.ROOT) + " không hợp lệ.");
                }
                int row = readCount(rowData, "row");
                Object rawMessage = rowData.get("message");
                if (!(rawMessage instanceof String message) || message.isBlank()) {
                    throw new IllegalArgumentException("Nội dung " + type.toLowerCase(
                            java.util.Locale.ROOT) + " tại STT " + row + " không hợp lệ.");
                }
                issues.add(new ImportIssue(type, row, message));
            }
            return issues;
        }

        private static int readCount(Map<?, ?> data, String field) {
            Object value = data.get(field);
            if (!(value instanceof Number number)
                    || number.longValue() < 0
                    || number.longValue() > Integer.MAX_VALUE
                    || number.doubleValue() != number.longValue()) {
                throw new IllegalArgumentException("Dữ liệu '" + field + "' không hợp lệ.");
            }
            return number.intValue();
        }
    }
}
