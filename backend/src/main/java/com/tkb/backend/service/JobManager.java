package com.tkb.backend.service;

import com.tkb.common.dto.JobDto;
import com.tkb.common.protocol.Command;
import com.tkb.common.protocol.ErrorCode;
import com.tkb.common.protocol.Response;
import com.tkb.common.protocol.Response.ErrorPayload;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * B7.1, B7.2, B7.3, B7.4 - JobManager: Quan ly Thread Pool (ExecutorService) xu ly dong thoi
 * nhieu yeu cau tinh toan OPTIMIZE va IMPORT, bao tien do (JOB_PROGRESS), tra ket qua (JOB_DONE),
 * gioi han 3 job dong thoi moi nguoi dung, va cho phep huy job bang co volatile.
 *
 * Vi tri dat file:
 *   backend/src/main/java/com/tkb/backend/service/JobManager.java
 */
public class JobManager {

    /** Gioi han toi da 3 job dang cho/chay dong thoi tren moi nguoi dung (B7.2) */
    public static final int MAX_CONCURRENT_JOBS_PER_USER = 3;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ExecutorService workerPool;
    private final Map<String, ManagedJob> jobs = new ConcurrentHashMap<>();

    public JobManager(int poolSize) {
        this.workerPool = Executors.newFixedThreadPool(Math.max(2, poolSize), r -> {
            Thread t = new Thread(r);
            t.setName("TKB-JobWorker-" + t.threadId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Giao dien bai toan thuc thi ben trong Worker Thread.
     * Nhan vao ham kiem tra co huy (isCancelled) va ham bao phan tram tien do (onProgress),
     * tra ve Map result de dua vao goi JOB_DONE.
     */
    @FunctionalInterface
    public interface JobTask {
        Map<String, Object> execute(BooleanSupplier isCancelled, IntConsumer onProgress) throws Exception;
    }

    /**
     * Kiem tra nguoi dung hien co vuot qua 3 job dang QUEUED/RUNNING hay khong.
     */
    public boolean canUserSubmitJob(long userId) {
        long activeCount = jobs.values().stream()
                .filter(j -> j.userId == userId && j.isActive())
                .count();
        return activeCount < MAX_CONCURRENT_JOBS_PER_USER;
    }

    /**
     * Đưa một Job mới (OPTIMIZE hoặc IMPORT) vào hàng đợi Thread Pool.
     * Trả về ngay mã jobId để Server phản hồi lập tức cho Client (không chờ tính xong).
     *
     * @param userId     ID người dùng gọi lệnh
     * @param username   Tên đăng nhập (phục vụ ADMIN_LIST_JOBS)
     * @param sessionId  ID phiên kết nối hiện tại (để hủy job nếu đứt kết nối)
     * @param jobType    "OPTIMIZE" hoặc "IMPORT"
     * @param pushSender Hàm gửi gói Push (JOB_PROGRESS / JOB_DONE) về đúng socket của Client
     * @param task       Logic tính toán thực tế
     * @return jobId vừa tạo (vd: "a1b2c3d4")
     */
    public String submitJob(
            long userId,
            String username,
            String sessionId,
            String jobType,
            Consumer<Response> pushSender,
            JobTask task) {

        String jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String startedAt = LocalDateTime.now().format(ISO_FORMATTER);

        ManagedJob managed = new ManagedJob(jobId, userId, username, sessionId, jobType, startedAt, pushSender);
        jobs.put(jobId, managed);

        workerPool.submit(() -> runJobInternal(managed, task));
        return jobId;
    }

    /**
     * Hủy job theo yêu cầu của chính người dùng đó (CANCEL_JOB - B7.2).
     * @return true nếu tìm thấy và thuộc về đúng userId, false nếu không tìm thấy (JOB_NOT_FOUND)
     */
    public boolean cancelJobByUser(String jobId, long userId) {
        ManagedJob job = jobs.get(jobId);
        if (job == null || job.userId != userId) {
            return false;
        }
        job.requestCancel();
        return true;
    }

    /**
     * Hủy job bất kỳ dành cho Admin (ADMIN_CANCEL_JOB - B9.8).
     */
    public boolean cancelJobByAdmin(String jobId) {
        ManagedJob job = jobs.get(jobId);
        if (job == null) {
            return false;
        }
        job.requestCancel();
        return true;
    }

    /**
     * B7.4 - Hủy toàn bộ job đang chờ hoặc đang chạy của một phiên khi Client mất kết nối hoặc LOGOUT.
     */
    public void cancelJobsBySession(String sessionId) {
        if (sessionId == null) return;
        for (ManagedJob job : jobs.values()) {
            if (sessionId.equals(job.sessionId) && job.isActive()) {
                job.requestCancel();
            }
        }
    }

    /**
     * Lay trang thai Job cho nguoi dung (phuc vu GET_JOB - B7.2).
     */
    public JobDto getJobForUser(String jobId, long userId) {
        ManagedJob job = jobs.get(jobId);
        if (job == null || job.userId != userId) {
            return null;
        }
        return job.toDto();
    }

    /**
     * Lay danh sach tat ca job dang QUEUED hoac RUNNING cua moi nguoi dung (phuc vu ADMIN_LIST_JOBS - B9.8).
     */
    public List<JobDto> listActiveJobsForAdmin() {
        List<JobDto> list = new ArrayList<>();
        for (ManagedJob job : jobs.values()) {
            if (job.isActive()) {
                list.add(job.toDto());
            }
        }
        return list;
    }

    public int countRunningJobs() {
        return (int) jobs.values().stream()
                .filter(j -> JobDto.STATUS_RUNNING.equals(j.status))
                .count();
    }

    public int countQueuedJobs() {
        return (int) jobs.values().stream()
                .filter(j -> JobDto.STATUS_QUEUED.equals(j.status))
                .count();
    }

    public void shutdown() {
        workerPool.shutdownNow();
    }

    // =========================================================================
    // LUONG THUC THI JOB & DAY GOI PUSH (JOB_PROGRESS / JOB_DONE)
    // =========================================================================

    private void runJobInternal(ManagedJob job, JobTask task) {
        if (job.cancelled) {
            finishAsCancelled(job);
            return;
        }

        job.status = JobDto.STATUS_RUNNING;
        sendProgressPush(job, 0, "Đang bắt đầu xử lý...");

        try {
            IntConsumer progressCallback = pct -> {
                int clamped = Math.max(0, Math.min(100, pct));
                if (!job.cancelled && clamped != job.percent) {
                    job.percent = clamped;
                    sendProgressPush(job, clamped, "Đang xử lý (" + clamped + "%)...");
                }
            };

            Map<String, Object> resultData = task.execute(() -> job.cancelled, progressCallback);

            if (job.cancelled) {
                finishAsCancelled(job);
                return;
            }

            job.status = JobDto.STATUS_DONE;
            job.percent = 100;
            job.result = resultData;
            sendDonePush(job);

        } catch (Exception e) {
            if (job.cancelled) {
                finishAsCancelled(job);
            } else {
                System.err.println("[JobManager] Loi khi chay job " + job.jobId + ": " + e.getMessage());
                job.status = JobDto.STATUS_FAILED;
                job.error = new ErrorPayload(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "Lỗi trong quá trình xử lý yêu cầu: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"),
                        Map.of()
                );
                sendDonePush(job);
            }
        }
    }

    private void finishAsCancelled(ManagedJob job) {
        job.status = JobDto.STATUS_CANCELLED;
        sendDonePush(job);
    }

    private void sendProgressPush(ManagedJob job, int percent, String message) {
        if (job.pushSender == null) return;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", job.jobId);
        data.put("jobType", job.jobType);
        data.put("status", job.status);
        data.put("percent", percent);
        data.put("message", message);
        job.pushSender.accept(Response.pushMessage(Command.JOB_PROGRESS, data));
    }

    private void sendDonePush(ManagedJob job) {
        if (job.pushSender == null || !job.markDoneSent()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", job.jobId);
        data.put("jobType", job.jobType);
        data.put("status", job.status);
        if (JobDto.STATUS_DONE.equals(job.status) && job.result != null) {
            data.put("result", job.result);
        } else if (JobDto.STATUS_FAILED.equals(job.status) && job.error != null) {
            data.put("error", job.error);
        }
        job.pushSender.accept(Response.pushMessage(Command.JOB_DONE, data));
    }

    /**
     * Doi tuong luu trang thai mot Job trong bo nho.
     * Su dung bien volatile cho cancelled, status va percent de an toan giua nhieu luong.
     */
    private static final class ManagedJob {
        final String jobId;
        final long userId;
        final String username;
        final String sessionId;
        final String jobType;
        final String startedAt;
        final Consumer<Response> pushSender;

        volatile String status = JobDto.STATUS_QUEUED;
        volatile int percent = 0;
        volatile boolean cancelled = false;
        volatile Map<String, Object> result;
        volatile ErrorPayload error;
        private boolean donePushSent = false;

        ManagedJob(
                String jobId,
                long userId,
                String username,
                String sessionId,
                String jobType,
                String startedAt,
                Consumer<Response> pushSender) {
            this.jobId = jobId;
            this.userId = userId;
            this.username = username;
            this.sessionId = sessionId;
            this.jobType = jobType;
            this.startedAt = startedAt;
            this.pushSender = pushSender;
        }

        void requestCancel() {
            this.cancelled = true;
            if (JobDto.STATUS_QUEUED.equals(this.status)) {
                this.status = JobDto.STATUS_CANCELLED;
            }
        }

        boolean isActive() {
            return JobDto.STATUS_QUEUED.equals(status) || JobDto.STATUS_RUNNING.equals(status);
        }

        synchronized boolean markDoneSent() {
            if (donePushSent) {
                return false;
            }
            donePushSent = true;
            return true;
        }

        JobDto toDto() {
            JobDto dto = new JobDto(jobId, jobType, username, status, percent, null, startedAt);
            dto.setResult(result);
            dto.setError(error);
            return dto;
        }
    }
}