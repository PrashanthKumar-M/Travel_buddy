package com.travelbuddy.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class SmsGatewayService {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendSmsOtp(String phone, String otp) {
        String cleanPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "";
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        // 1. Send via Fast2SMS (Indian SMS Gateway)
        String fast2smsKey = System.getenv("FAST2SMS_API_KEY");
        if (fast2smsKey != null && !fast2smsKey.isBlank()) {
            try {
                // Primary: Official JSON POST request
                String jsonBody = String.format("{\"route\":\"otp\",\"variables_values\":\"%s\",\"numbers\":\"%s\"}", otp, cleanPhone);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.fast2sms.com/dev/bulkV2"))
                        .header("authorization", fast2smsKey.trim())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Fast2SMS POST response code: {} body: {}", response.statusCode(), response.body());
                if (response.statusCode() == 200) {
                    return true;
                }

                // Fallback: GET request
                String url = "https://www.fast2sms.com/dev/bulkV2?authorization="
                        + URLEncoder.encode(fast2smsKey.trim(), StandardCharsets.UTF_8)
                        + "&route=otp&variables_values="
                        + URLEncoder.encode(otp, StandardCharsets.UTF_8)
                        + "&numbers=" + cleanPhone;

                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                log.info("Fast2SMS GET fallback response code: {} body: {}", getResponse.statusCode(), getResponse.body());
                if (getResponse.statusCode() == 200) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to send SMS via Fast2SMS: {}", e.getMessage());
            }
        }

        // 2. Try Twilio (International Gateway)
        String twilioSid = System.getenv("TWILIO_ACCOUNT_SID");
        String twilioAuth = System.getenv("TWILIO_AUTH_TOKEN");
        String twilioFrom = System.getenv("TWILIO_PHONE_NUMBER");
        if (twilioSid != null && twilioAuth != null && twilioFrom != null) {
            try {
                String to = cleanPhone.length() == 10 ? "+91" + cleanPhone : "+" + cleanPhone;
                String body = "Your Travel Buddy verification code is: " + otp + ". Valid for 10 minutes.";
                String form = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                        + "&From=" + URLEncoder.encode(twilioFrom, StandardCharsets.UTF_8)
                        + "&Body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);

                String authHeader = "Basic " + Base64.getEncoder().encodeToString((twilioSid + ":" + twilioAuth).getBytes(StandardCharsets.UTF_8));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json"))
                        .header("Authorization", authHeader)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Twilio response code: {} body: {}", response.statusCode(), response.body());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to send SMS via Twilio: {}", e.getMessage());
            }
        }

        // Fallback: log to Render console
        log.info("[SIMULATED SMS GATEWAY] Sent OTP '{}' to mobile number: +91 {}", otp, cleanPhone);
        return false;
    }
}