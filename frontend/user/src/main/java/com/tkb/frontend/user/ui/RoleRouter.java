package com.tkb.frontend.user.ui;

import com.tkb.frontend.admin.ui.AdminFrame;
import com.tkb.frontend.user.net.NetworkClient;
import com.tkb.frontend.user.net.TcpTkbClient;
import com.tkb.frontend.user.net.TkbClient;
import javafx.scene.Parent;

import java.util.Locale;

public final class RoleRouter {
    private RoleRouter() {
    }

    public static Parent frameForRole(String role) {
        return frameForRole(role, new TcpTkbClient());
    }

    public static Parent frameForRole(String role, TkbClient userClient) {
        return frameForRole(role, userClient, null);
    }

    public static Parent frameForRole(String role, TkbClient userClient, NetworkClient networkClient) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("User role must not be blank");
        }
        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "USER" -> new UserFrame(userClient);
            case "ADMIN" -> new AdminFrame(networkClient);
            default -> throw new IllegalArgumentException("Unsupported user role: " + role);
        };
    }
}
