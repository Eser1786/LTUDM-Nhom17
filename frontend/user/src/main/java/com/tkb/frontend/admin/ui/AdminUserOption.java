package com.tkb.frontend.admin.ui;

record AdminUserOption(
        long userId,
        String username,
        String role,
        String createdAt,
        boolean online) {

    String oppositeRole() {
        return "ADMIN".equals(role) ? "USER" : "ADMIN";
    }

    String createdAtDisplay() {
        return createdAt.replace('T', ' ');
    }
}
