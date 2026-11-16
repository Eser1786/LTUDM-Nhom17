package com.tkb.frontend.admin.ui;

public record AdminJobOption(
        String jobId,
        String jobType,
        String username,
        String status,
        int percent,
        String message,
        String startedAt) {

    public boolean isActive() {
        return "QUEUED".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
    }

    public String jobTypeDisplay() {
        if (jobType == null || jobType.isBlank()) {
            return "Không xác định";
        }
        return switch (jobType.toUpperCase()) {
            case "OPTIMIZE" -> "Tối ưu TKB";
            case "IMPORT" -> "Nhập PDF";
            default -> jobType;
        };
    }

    public String statusDisplay() {
        if (status == null || status.isBlank()) {
            return "Không rõ";
        }
        return switch (status.toUpperCase()) {
            case "QUEUED" -> "Đang chờ";
            case "RUNNING" -> "Đang chạy";
            case "DONE" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "FAILED" -> "Thất bại";
            default -> status;
        };
    }

    public String statusBadgeStyle() {
        String color = switch (status == null ? "" : status.toUpperCase()) {
            case "QUEUED" -> "#B45309";
            case "RUNNING" -> "#1D4ED8";
            case "DONE" -> "#15803D";
            case "CANCELLED" -> "#475569";
            case "FAILED" -> "#B91C1C";
            default -> "#475569";
        };
        String bg = switch (status == null ? "" : status.toUpperCase()) {
            case "QUEUED" -> "#FEF3C7";
            case "RUNNING" -> "#DBEAFE";
            case "DONE" -> "#DCFCE7";
            case "CANCELLED" -> "#F1F5F9";
            case "FAILED" -> "#FEE2E2";
            default -> "#F1F5F9";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; "
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 8 3 8; -fx-font-size: 11px;";
    }

    public String jobTypeBadgeStyle() {
        String color = "IMPORT".equalsIgnoreCase(jobType) ? "#6D28D9" : "#0F766E";
        String bg = "IMPORT".equalsIgnoreCase(jobType) ? "#EDE9FE" : "#CCFBF1";
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; "
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 8 3 8; -fx-font-size: 11px;";
    }

    public String startedAtDisplay() {
        if (startedAt == null || startedAt.isBlank()) {
            return "—";
        }
        return startedAt.replace('T', ' ');
    }
}
