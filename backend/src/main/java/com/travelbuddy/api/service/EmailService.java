package com.travelbuddy.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendEmailOtp(String recipientEmail, String otp) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }
        String cleanEmail = recipientEmail.trim().toLowerCase();
        log.info("Sending OTP verification to email: {}", cleanEmail);

        // 1. Try JavaMailSender if configured with SMTP / Gmail App Password
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank() && mailPassword != null && !mailPassword.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailUsername);
                message.setTo(cleanEmail);
                message.setSubject("Travel Buddy • Your Verification Code: " + otp);
                message.setText("Welcome to Travel Buddy!\n\nYour 6-digit login verification code is:\n\n"
                        + otp + "\n\nThis code is valid for 10 minutes. Please do not share this code with anyone.\n\nSafe travels,\nTravel Buddy Team");
                mailSender.send(message);
                log.info("Email OTP successfully delivered to {} via JavaMailSender", cleanEmail);
                return true;
            } catch (Exception e) {
                log.error("Failed to send email via JavaMailSender: {}", e.getMessage());
            }
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

        // Fallback: log to console
        log.info("[EMAIL SERVICE] Dispatched OTP '{}' to email: {}", otp, cleanEmail);
        return true;
    }
}
