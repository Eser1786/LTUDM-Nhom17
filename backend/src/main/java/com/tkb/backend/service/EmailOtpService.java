package com.tkb.backend.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class EmailOtpService {

    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUser;
    private final String smtpAppPassword;
    private final String defaultFallbackRecipient;
    private static final java.util.Map<String, String> DOT_ENV = loadDotEnv();

    public EmailOtpService() {
        this.smtpHost = getConfig("SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = getConfig("SMTP_PORT", "587");
        this.smtpUser = getConfig("SMTP_USER", "");
        this.smtpAppPassword = getConfig("SMTP_APP_PASSWORD", "");
        this.defaultFallbackRecipient = getConfig("SMTP_DEFAULT_TO", "");
    }

    private static java.util.Map<String, String> loadDotEnv() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        java.nio.file.Path[] candidates = {
                java.nio.file.Paths.get(".env"),
                java.nio.file.Paths.get("..", ".env")
        };
        for (java.nio.file.Path p : candidates) {
            if (java.nio.file.Files.exists(p)) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
                        int idx = trimmed.indexOf('=');
                        String k = trimmed.substring(0, idx).trim();
                        String v = trimmed.substring(idx + 1).trim().replaceAll("^\"|\"$", "");
                        map.put(k, v);
                    }
                    break;
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static String getConfig(String key, String def) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        if (v == null || v.isBlank()) v = DOT_ENV.get(key);
        return (v != null && !v.isBlank()) ? v.trim() : def;
    }

    /**
     * Gửi mã OTP qua Gmail SMTP.
     * @return Địa chỉ email thực tế đã nhận thư, hoặc null nếu chưa cấu hình SMTP (chỉ in console).
     */
    public String sendOtpEmail(String username, String userEmail, String otpCode, int expiryMinutes) throws Exception {
        String recipient = (userEmail != null && !userEmail.isBlank())
                ? userEmail.trim()
                : defaultFallbackRecipient;

        if (smtpUser.isBlank() || smtpAppPassword.isBlank() || recipient.isBlank()) {
            return null;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpAppPassword);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(smtpUser, "Hệ thống Quản lý Thời khóa biểu", StandardCharsets.UTF_8.name()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("[TKB] Mã xác thực OTP đổi mật khẩu", StandardCharsets.UTF_8.name());

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;">
                    <h2 style="color: #1d4ed8; margin-top: 0;">Yêu cầu đặt lại mật khẩu</h2>
                    <p>Xin chào <b>%s</b>,</p>
                    <p>Mã xác thực OTP để đổi mật khẩu của bạn là:</p>
                    <div style="font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #0f172a; background: #f1f5f9; padding: 14px; text-align: center; border-radius: 8px; margin: 16px 0;">
                        %s
                    </div>
                    <p style="color: #64748b; font-size: 13px;">Mã có hiệu lực trong <b>%d phút</b>. Tuyệt đối không chia sẻ mã này cho người khác.</p>
                </div>
                """.formatted(username, otpCode, expiryMinutes);

        message.setContent(htmlContent, "text/html; charset=UTF-8");
        Transport.send(message);
        return recipient;
    }
    

    public boolean hasValidTarget(String userEmail) {
        String recipient = (userEmail != null && !userEmail.isBlank())
                ? userEmail.trim()
                : defaultFallbackRecipient;
        return !smtpUser.isBlank() && !smtpAppPassword.isBlank() && !recipient.isBlank();
    }
}