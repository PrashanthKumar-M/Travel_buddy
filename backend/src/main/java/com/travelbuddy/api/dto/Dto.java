package com.travelbuddy.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dto {

    public record RequestOtpRequest(String phone, String email, String type) {}

    public record VerifyOtpRequest(String phone, String email, String otp) {}

    public record AuthResponse(
            boolean success,
            String token,
            String userId,
            String name,
            String phone,
            String message
    ) {}

    public record CreateTripRequest(
            String id,
            String tripName,
            String source,
            String destination,
            String dates,
            String createdBy,
            String coverImage,
            List<String> members,
            String vehicleType
    ) {
        public CreateTripRequest(String id, String tripName, String source, String destination, String dates, String createdBy, String coverImage, List<String> members) {
            this(id, tripName, source, destination, dates, createdBy, coverImage, members, "CAR");
        }
    }

    public record JoinTripRequest(
            String tripCode,
            String userId,
            String userName
    ) {}

    public record LocationUpdateRequest(
            String tripId,
            String userId,
            double latitude,
            double longitude,
            double speed,
            int battery,
            double heading,
            long timestamp
    ) {}

    public record SendMessageRequest(
            String senderId,
            String senderName,
            String text
    ) {}

    public record UpdateProfileRequest(
            String name,
            String email,
            String avatar,
            String emergencyContactName,
            String emergencyContactPhone
    ) {}

    public record UploadImageRequest(
            String imageData // base64 or URL
    ) {}

    public record CreateGroupRequest(
            String name,
            String groupType, // "TRIP" or "MANUAL_PERSONAL"
            String tripId,
            List<String> memberNames
    ) {}

    public record AddExpenseRequest(
            String groupId,
            String tripId,
            String title,
            double amount,
            String paidBy,
            String splitType, // "equal", "exact", "shares"
            List<String> members,
            Map<String, Double> customAmounts
    ) {}

    public record SettleRequest(
            String groupId,
            String from,
            String to,
            double amount
    ) {}

    public record RegisterDeviceRequest(
            String name,
            String type, // "VEHICLE", "CHILD_BAG", "PET_TAG", "ASSET_TRACKER"
            String imei,
            String assignedTo,
            String safeZoneName,
            Double safeZoneLat,
            Double safeZoneLng,
            Double safeZoneRadiusMeters
    ) {}

    public record DeviceTelemetryRequest(
            double latitude,
            double longitude,
            double speedKmh,
            double heading,
            boolean ignitionOn,
            int batteryPercent
    ) {}

    public record SosAlertRequest(
            String tripId,
            String userId,
            double latitude,
            double longitude,
            Double accuracyMeters,
            String message,
            java.util.List<String> phoneNumbers
    ) {}
}
