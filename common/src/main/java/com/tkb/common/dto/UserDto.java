package com.tkb.common.dto;

/**
 * B2.4 - UserDto: Cau truc du lieu thong tin tai khoan nguoi dung
 * (dung cho ket qua LOGIN va lenh ADMIN_LIST_USERS).
 *
 * Cau truc JSON chuan:
 *   {"userId": 3, "username": "eser", "role": "USER", "createdAt": "2026-10-01T08:00:00", "online": true}
 *
 * Vi tri dat file:
 *   common/src/main/java/com/tkb/common/dto/UserDto.java
 */
public class UserDto {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private long userId;
    private String username;
    private String role;
    private String createdAt;
    private boolean online;

    public UserDto() {
    }

    public UserDto(long userId, String username, String role, String createdAt, boolean online) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.online = online;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}