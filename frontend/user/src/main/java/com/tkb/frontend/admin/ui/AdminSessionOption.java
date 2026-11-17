package com.tkb.frontend.admin.ui;

public record AdminSessionOption(
        String sessionId,
        String username,
        String remoteAddress,
        String connectedAt) {

    public boolean isAuthenticated() {
        return username != null && !username.isBlank() && !username.equals("(chưa đăng nhập)");
    }

    public String cleanRemoteAddress() {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "—";
        }
        return remoteAddress.startsWith("/") ? remoteAddress.substring(1) : remoteAddress;
    }

    public String connectedAtDisplay() {
        if (connectedAt == null || connectedAt.isBlank()) {
            return "—";
        }
        return connectedAt.replace('T', ' ');
    }

    public String userBadgeStyle() {
        if (isAuthenticated()) {
            boolean isAdmin = "admin".equalsIgnoreCase(username);
            String color = isAdmin ? "#6D28D9" : "#1D4ED8";
            String bg = isAdmin ? "#EDE9FE" : "#DBEAFE";
            return "-fx-background-color: " + bg + "; -fx-text-fill: " + color + "; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 8 3 8; -fx-font-size: 11px;";
        } else {
            return "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; "
                    + "-fx-font-weight: normal; -fx-background-radius: 6; -fx-padding: 3 8 3 8; -fx-font-size: 11px;";
        }
    }
}
