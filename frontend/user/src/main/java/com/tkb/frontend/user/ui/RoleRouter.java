package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.ui.admin.AdminFrame;
import javafx.scene.Parent;

import java.util.Locale;

public final class RoleRouter {
    private RoleRouter() {
    }

    public static Parent frameForRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("User role must not be blank");
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "USER" -> new UserFrame();
            case "ADMIN" -> new AdminFrame();
            default -> throw new IllegalArgumentException("Unsupported user role: " + role);
        };
    }
}
