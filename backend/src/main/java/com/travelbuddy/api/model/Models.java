package com.travelbuddy.api.model;

import java.util.*;

public class Models {

    public record User(
            String id,
            String phone,
            String name,
            String avatar,
            long createdAt
    ) {}

    public record UserProfile(
            String id,
            String phone,
            String name,
            String email,
            String avatar,
            String emergencyContactName,
            String emergencyContactPhone,
            int totalTrips,
            double totalDistanceKm,
            double totalSpent,
            long createdAt
    ) {}

    public record TripMember(
            String id,
            String name,
            long color,
            boolean isSharingLocation,
            Double latitude,
            Double longitude,
            Double speedKmh,
            Integer batteryPercent,
            Long lastUpdated
    ) {}

    public record Trip(
            String id,
            String name,
            String source,
            String destination,
            String dates,
            String status,
            String tripCode,
            String createdBy,
            String coverImage,
            List<TripMember> members,
            long createdAt,
            String vehicleType
    ) {
        public Trip(
                String id,
                String name,
                String source,
                String destination,
                String dates,
                String status,
                String tripCode,
                String createdBy,
                String coverImage,
                List<TripMember> members,
                long createdAt
        ) {
            this(id, name, source, destination, dates, status, tripCode, createdBy, coverImage, members, createdAt, "CAR");
        }
    }

    public record LocationEvent(
            String tripId,
            String userId,
            double latitude,
            double longitude,
            Double speed,
            Integer battery,
            Long timestamp,
            Double heading
    ) {}

    public record ChatMessage(
            String id,
            String tripId,
            String senderId,
            String senderName,
            String text,
            long timestamp
    ) {}

    public enum DeviceType {
        VEHICLE,
        CHILD_BAG,
        PET_TAG,
        ASSET_TRACKER
    }

    public record TrackingDevice(
            String id,
            String name,
            DeviceType type,
            String imei,
            String assignedTo, // e.g., "Child: Aarav", "Car: KA-01-MJ-5020"
            boolean isOnline,
            double latitude,
            double longitude,
            double speedKmh,
            double heading,
            boolean ignitionOn,
            int batteryPercent,
            String safeZoneName,
            Double safeZoneLat,
            Double safeZoneLng,
            Double safeZoneRadiusMeters,
            boolean isInsideSafeZone,
            long lastUpdated
    ) {}

    public enum GroupType {
        TRIP,
        MANUAL_PERSONAL
    }

    public record ExpenseGroup(
            String id,
            String name,
            GroupType type,
            String tripId,
            List<String> memberIds,
            long createdAt
    ) {}

    public record Expense(
            String id,
            String groupId,
            String tripId,
            String title,
            double amount,
            String paidBy,
            String splitType, // "equal", "exact", "shares"
            Set<String> splitWith,
            Map<String, Double> customAmounts,
            long createdAt
    ) {}

    public record Settlement(
            String id,
            String groupId,
            String from,
            String to,
            double amount,
            boolean isSettled,
            Long settledAt
    ) {}

    public record SosAlert(
            String alertId,
            String tripId,
            String userId,
            double latitude,
            double longitude,
            Double accuracyMeters,
            String message,
            int notifiedContacts,
            String status,
            long createdAt
    ) {}
}
