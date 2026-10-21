package com.travelbuddy.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String getEffectiveUsername() {
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        String env = System.getenv("SPRING_MAIL_USERNAME");
        return (env != null) ? env.trim() : "";
    }

    private String getEffectivePassword() {
        if (mailPassword != null && !mailPassword.isBlank()) {
            return mailPassword.trim();
        }
        String env = System.getenv("SPRING_MAIL_PASSWORD");
        return (env != null) ? env.trim() : "";
    }

    private String getEffectiveHost() {
        String env = System.getenv("SPRING_MAIL_HOST");
        if (env != null && !env.isBlank()) return env.trim();
        if (mailHost != null && !mailHost.isBlank()) return mailHost.trim();
        return "smtp.gmail.com";
    }

    private int getEffectivePort() {
        String env = System.getenv("SPRING_MAIL_PORT");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ignored) {}
        }
        return mailPort > 0 ? mailPort : 587;
    }

    private JavaMailSender getOrCreateMailSender(String username, String password, String host, int port) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");

        return sender;
    }

    public boolean sendEmailOtp(String recipientEmail, String otp) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }
        String cleanEmail = recipientEmail.trim().toLowerCase();
        String username = getEffectiveUsername();
        String password = getEffectivePassword();
        String host = getEffectiveHost();
        int port = getEffectivePort();

        log.info("Sending OTP verification to email: {} (using account: '{}', host: '{}:{}')",
                cleanEmail, username.isBlank() ? "NOT_SET" : username, host, port);

        // 0. Try Brevo HTTPS API first (free 300 emails/day). Render's free tier blocks
        //    outbound SMTP ports 25/465/587, so an HTTPS API is the reliable option there.
        //    Env: BREVO_API_KEY (required), BREVO_SENDER_EMAIL (a verified sender, e.g. your Gmail).
        String brevoKey = System.getenv("BREVO_API_KEY");
        if (brevoKey != null && !brevoKey.isBlank()) {
            String senderEnv = System.getenv("BREVO_SENDER_EMAIL");
            String sender = (senderEnv != null && !senderEnv.isBlank()) ? senderEnv.trim() : username;
            if (sender.isBlank()) {
                log.warn("BREVO_API_KEY is set but BREVO_SENDER_EMAIL / SPRING_MAIL_USERNAME is empty; skipping Brevo");
            } else {
                try {
                    String jsonBody = String.format(
                            "{\"sender\":{\"name\":\"Travel Buddy\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],"
                                    + "\"subject\":\"Travel Buddy Verification Code: %s\","
                                    + "\"htmlContent\":\"<div style='font-family:sans-serif;max-width:500px;margin:auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;'><h2 style='color:#0A5C67;'>Travel Buddy</h2><p>Your verification code is:</p><h1 style='color:#1D64F2;letter-spacing:4px;'>%s</h1><p style='color:#64748B;'>Valid for 10 minutes. Do not share this code.</p></div>\"}",
                            sender, cleanEmail, otp, otp
                    );
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                            .header("api-key", brevoKey.trim())
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .timeout(Duration.ofSeconds(10))
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    log.info("Brevo HTTP response: {} body: {}", response.statusCode(), response.body());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return true;
                    }
                } catch (Exception e) {
                    log.error("Failed to send email via Brevo: {}", e.getMessage());
                }
            }
        }

        // 1. Try JavaMailSender if configured with SMTP / Gmail App Password
        //    (works locally / on paid hosts; times out on Render free tier)
        if (!username.isBlank() && !password.isBlank()) {
            try {
                JavaMailSender sender = getOrCreateMailSender(username, password, host, port);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(username);
                message.setTo(cleanEmail);
                message.setSubject("Travel Buddy • Your Verification Code: " + otp);
                message.setText("Welcome to Travel Buddy!\n\nYour 6-digit login verification code is:\n\n"
                        + otp + "\n\nThis code is valid for 10 minutes. Please do not share this code with anyone.\n\nSafe travels,\nTravel Buddy Team");

                sender.send(message);
                log.info("Email OTP successfully delivered to {} via JavaMailSender", cleanEmail);
                return true;
            } catch (Exception e) {
                log.error("Failed to send email via JavaMailSender: {}", e.getMessage(), e);
            }
        } else {
            log.warn("SMTP credentials not configured. username='{}', passwordSet={}",
                    username, !password.isBlank());
        }

        // 2. Try Resend API (Free 3,000 emails/month) if RESEND_API_KEY is configured
        String resendKey = System.getenv("RESEND_API_KEY");
        if (resendKey != null && !resendKey.isBlank()) {
            try {
                String jsonBody = String.format(
                        "{\"from\":\"Travel Buddy <onboarding@resend.dev>\",\"to\":[\"%s\"],\"subject\":\"Travel Buddy Verification Code: %s\",\"html\":\"<div style='font-family:sans-serif;max-width:500px;margin:auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;'><h2 style='color:#0A5C67;'>Travel Buddy</h2><p>Your verification code is:</p><h1 style='color:#1D64F2;letter-spacing:4px;'>%s</h1><p style='color:#64748B;'>Valid for 10 minutes. Do not share this code.</p></div>\"}",
                        cleanEmail, otp, otp
                );
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.resend.com/emails"))
                        .header("Authorization", "Bearer " + resendKey.trim())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Resend HTTP response: {} body: {}", response.statusCode(), response.body());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to send email via Resend: {}", e.getMessage());
            }
        }

        // Fallback: nothing delivered - log to console so the failure is visible
        log.error("[EMAIL SERVICE] OTP email NOT delivered to {} (all providers failed or unconfigured). OTP='{}'",
                cleanEmail, otp);
        return false;
    }
}
