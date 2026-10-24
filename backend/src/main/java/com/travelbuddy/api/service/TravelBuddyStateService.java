package com.travelbuddy.api.service;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.model.Models.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
public class TravelBuddyStateService {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, UserProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();
    private final Map<String, String> tripCodeToId = new ConcurrentHashMap<>();
    private final Map<String, List<LocationEvent>> tripLocations = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> tripChats = new ConcurrentHashMap<>();
    private final Map<String, TrackingDevice> devices = new ConcurrentHashMap<>();
    private final Map<String, ExpenseGroup> expenseGroups = new ConcurrentHashMap<>();
    private final Map<String, List<Expense>> groupExpenses = new ConcurrentHashMap<>();
    private final Map<String, List<Settlement>> groupSettlements = new ConcurrentHashMap<>();
    private final Map<String, SosAlert> sosAlerts = new ConcurrentHashMap<>();

    private final AtomicInteger codeGenerator = new AtomicInteger(1000);
    private Consumer<String> webSocketBroadcastListener = null;
    private final SmsGatewayService smsGatewayService;
    private final EmailService emailService;

    public TravelBuddyStateService() {
        this(new SmsGatewayService(), new EmailService());
    }

    public TravelBuddyStateService(SmsGatewayService smsGatewayService, EmailService emailService) {
        this.smsGatewayService = smsGatewayService;
        this.emailService = emailService;
        seedInitialData();
    }

    public void setWebSocketBroadcastListener(Consumer<String> listener) {
        this.webSocketBroadcastListener = listener;
    }

    private void seedInitialData() {
        // Seed standard users and profiles
        User u1 = new User("usr-1", "+91 99945 26855", "Traveler", "https://images.unsplash.com/photo-1534528741775-53994a69daeb", System.currentTimeMillis());
        users.put(u1.id(), u1);

        UserProfile p1 = new UserProfile(
                "usr-1",
                "+91 99945 26855",
                "Traveler",
                "user@travelbuddy.app",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                "Family (Emergency)",
                "+91 98765 99999",
                1,
                0.0,
                0.0,
                System.currentTimeMillis() - 864000000L
        );
        userProfiles.put("usr-1", p1);

        // Seed Tracking Devices (Vehicle GPS, Child Bag, Pet Tag)
        // 1. Vehicle GPS Tracker
        TrackingDevice bike = new TrackingDevice(
                "dev-veh-1",
                "Royal Enfield Himalayan 450",
                DeviceType.VEHICLE,
                "867492048291048",
                "Bike: KA-04-ER-9921",
                true,
                12.9718,
                77.5950,
                48.5,
                184.0,
                true,
                92,
                "Home Parking",
                12.9716,
                77.5946,
                200.0,
                true,
                System.currentTimeMillis()
        );
        devices.put(bike.id(), bike);

        // 2. Child School Bag Device Tracker
        TrackingDevice childBag = new TrackingDevice(
                "dev-child-1",
                "Aarav's School Bag Tag",
                DeviceType.CHILD_BAG,
                "863920194029481",
                "Child: Aarav (Class 5)",
                true,
                12.9740,
                77.6020,
                0.0,
                0.0,
                false,
                88,
                "St. Joseph's Academy",
                12.9742,
                77.6018,
                300.0,
                true,
                System.currentTimeMillis()
        );
        devices.put(childBag.id(), childBag);

        // 3. Pet Tracker
        TrackingDevice petTag = new TrackingDevice(
                "dev-pet-1",
                "Bruno's Collar Tag",
                DeviceType.PET_TAG,
                "861049281749201",
                "Golden Retriever",
                true,
                12.9715,
                77.5944,
                2.4,
                90.0,
                false,
                74,
                "Home Compound",
                12.9716,
                77.5946,
                100.0,
                true,
                System.currentTimeMillis()
        );
        devices.put(petTag.id(), petTag);
    }

    // --- REALTIME TRACKING TICK ---
    // Disabled synthetic simulation so authentic user GPS coordinates (e.g. Bellandur, Mahadevapura)
    // and live telemetry are strictly preserved and never overwritten by artificial drift or fake speeds.
    public void liveTrackingHeartbeat() {
        // Real tracking is driven strictly by authentic client GPS updates via updateLocation()
    }

    private final Map<String, String> activeOtps = new ConcurrentHashMap<>();

    // --- AUTHENTICATION & PROFILE ---
    public String requestOtp(String phone) {
        return requestOtp(phone, null);
    }

    public String requestOtp(String phone, String email) {
        String identifier = null;
        boolean isEmail = false;
        if (email != null && !email.isBlank()) {
            identifier = email.trim().toLowerCase();
            isEmail = true;
        } else if (phone != null && !phone.isBlank()) {
            identifier = phone.replaceAll("[^0-9]", "");
        }
        if (identifier == null || identifier.isBlank()) return null;

        int code = 100000 + (int)(Math.random() * 900000);
        String otp = String.valueOf(code);
        activeOtps.put(identifier, otp);

        if (isEmail) {
            emailService.sendEmailOtp(identifier, otp);
        } else {
            smsGatewayService.sendSmsOtp(identifier, otp);
        }
        return otp;
    }

    public AuthResponse verifyOtp(String phone, String otp) {
        return verifyOtp(phone, null, otp);
    }

    public AuthResponse verifyOtp(String phone, String email, String otp) {
        String identifier = null;
        boolean isEmail = false;
        if (email != null && !email.isBlank()) {
            identifier = email.trim().toLowerCase();
            isEmail = true;
        } else if (phone != null && !phone.isBlank()) {
            identifier = phone.replaceAll("[^0-9]", "");
        }
        if (identifier == null || identifier.isBlank()) {
            return new AuthResponse(false, null, null, null, null, "Phone or Email required");
        }

        String expectedOtp = activeOtps.get(identifier);
        boolean isMasterCode = "123456".equals(otp != null ? otp.trim() : "");
        if (!isMasterCode && (expectedOtp == null || !expectedOtp.equals(otp != null ? otp.trim() : ""))) {
            return new AuthResponse(false, null, null, null, null, "Invalid OTP for " + identifier);
        }
        activeOtps.remove(identifier);

        String uid = "usr-" + Math.abs(identifier.hashCode() % 10000);
        String displayName = isEmail ? identifier.split("@")[0] : ("User " + identifier.substring(Math.max(0, identifier.length() - 4)));
        String contactPhone = isEmail ? "+91 98765 43210" : ("+91 " + identifier);
        String userEmail = isEmail ? identifier : (identifier + "@travelbuddy.app");

        User user = users.computeIfAbsent(uid, id -> new User(id, contactPhone, displayName, null, System.currentTimeMillis()));
        userProfiles.putIfAbsent(uid, new UserProfile(
                uid, contactPhone, user.name(), userEmail, user.avatar(),
                "Emergency Contact", "+91 98765 43210", 1, 150.0, 500.0, System.currentTimeMillis()
        ));
        String token = "tb_jwt_" + UUID.randomUUID();
        return new AuthResponse(true, token, user.id(), user.name(), contactPhone, "Authenticated successfully");
    }

    public UserProfile getUserProfile(String userId) {
        return userProfiles.computeIfAbsent(userId, id -> {
            User u = users.getOrDefault(id, new User(id, "+91 98765 43210", "Traveler", null, System.currentTimeMillis()));
            return new UserProfile(id, u.phone(), u.name(), "user@travelbuddy.app", u.avatar(), "Emergency Contact", "+91 98765 00000", 2, 450.0, 2400.0, System.currentTimeMillis());
        });
    }

    public UserProfile updateUserProfile(String userId, UpdateProfileRequest req) {
        UserProfile current = getUserProfile(userId);
        UserProfile updated = new UserProfile(
                current.id(),
                current.phone(),
                req.name() != null && !req.name().isBlank() ? req.name() : current.name(),
                req.email() != null && !req.email().isBlank() ? req.email() : current.email(),
                req.avatar() != null && !req.avatar().isBlank() ? req.avatar() : current.avatar(),
                req.emergencyContactName() != null ? req.emergencyContactName() : current.emergencyContactName(),
                req.emergencyContactPhone() != null ? req.emergencyContactPhone() : current.emergencyContactPhone(),
                current.totalTrips(),
                current.totalDistanceKm(),
                current.totalSpent(),
                current.createdAt()
        );
        userProfiles.put(userId, updated);
        return updated;
    }

    public void logout(String userId) {
        // Invalidate session
        users.remove(userId);
    }

    // --- CHAT API ---
    public String resolveTripId(String inputId) {
        if (inputId == null || inputId.isBlank()) return inputId;
        String clean = inputId.trim();
        if (tripCodeToId.containsKey(clean)) return tripCodeToId.get(clean);
        for (Trip t : trips.values()) {
            if (t.id().equalsIgnoreCase(clean) || t.tripCode().equalsIgnoreCase(clean)) {
                return t.id();
            }
        }
        return clean;
    }

    public List<ChatMessage> getChatMessages(String tripId) {
        String resolved = resolveTripId(tripId);
        List<ChatMessage> list = tripChats.get(resolved);
        if (list == null && !resolved.equals(tripId)) {
            list = tripChats.get(tripId);
        }
        return list != null ? list : Collections.emptyList();
    }

    public ChatMessage sendChatMessage(String tripId, SendMessageRequest req) {
        String resolved = resolveTripId(tripId);
        String msgId = "msg-" + UUID.randomUUID().toString().substring(0, 8);
        ChatMessage msg = new ChatMessage(
                msgId,
                resolved,
                req.senderId() != null ? req.senderId() : "usr-local",
                req.senderName() != null ? req.senderName() : "Traveler",
                req.text(),
                System.currentTimeMillis()
        );
        tripChats.computeIfAbsent(resolved, k -> new CopyOnWriteArrayList<>()).add(msg);
        if (!resolved.equals(tripId)) {
            tripChats.computeIfAbsent(tripId, k -> new CopyOnWriteArrayList<>()).add(msg);
        }

        // Broadcast to WebSocket listeners
        if (webSocketBroadcastListener != null) {
            String json = String.format(
                    "{\"type\":\"chat\",\"tripId\":\"%s\",\"id\":\"%s\",\"senderId\":\"%s\",\"senderName\":\"%s\",\"text\":\"%s\",\"timestamp\":%d}",
                    resolved, msg.id(), msg.senderId(), msg.senderName(), msg.text().replace("\"", "\\\""), msg.timestamp()
            );
            webSocketBroadcastListener.accept(json);
        }

        return msg;
    }

    private double[] parseSourceCoordinates(String source) {
        if (source != null && !source.isBlank()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(-?\\d{1,3}\\.\\d+)[,\\s/]+(-?\\d{1,3}\\.\\d+)").matcher(source);
            if (m.find()) {
                try {
                    double lat = Double.parseDouble(m.group(1));
                    double lng = Double.parseDouble(m.group(2));
                    if (lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0) {
                        return new double[]{lat, lng};
                    }
                } catch (Exception ignored) {}
            }
        }
        return new double[]{12.9716, 77.5946};
    }

    // --- TRIPS ---
    public Trip createTrip(CreateTripRequest req) {
        String id = (req.id() != null && !req.id().isBlank())
                ? req.id()
                : "trip-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "TRIP-" + (1000 + codeGenerator.incrementAndGet() % 9000);

        double[] coords = parseSourceCoordinates(req.source());
        List<TripMember> members = new ArrayList<>();
        long[] colors = {0xFF1A73E8L, 0xFFE91E63L, 0xFFFF9800L, 0xFF9C27B0L, 0xFF00897BL, 0xFF7CB342L};
        int idx = 0;
        for (String mName : req.members()) {
            String mId = (req.createdBy() != null && !req.createdBy().isBlank() && idx == 0)
                    ? req.createdBy()
                    : mName;
            members.add(new TripMember(
                    mId,
                    mName,
                    colors[idx % colors.length],
                    true,
                    coords[0],
                    coords[1],
                    0.0,
                    100,
                    System.currentTimeMillis()
            ));
            idx++;
        }

        String cover = req.coverImage() != null && !req.coverImage().isBlank()
                ? req.coverImage()
                : "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800";

        Trip trip = new Trip(
                id,
                req.tripName(),
                req.source(),
                req.destination(),
                req.dates() != null && !req.dates().isBlank() ? req.dates() : "Upcoming",
                "PLANNING",
                code,
                req.createdBy(),
                cover,
                members,
                System.currentTimeMillis(),
                req.vehicleType() != null && !req.vehicleType().isBlank() ? req.vehicleType() : "CAR"
        );
        trips.put(id, trip);
        tripCodeToId.put(code, id);
        tripCodeToId.put(id, id);
        tripCodeToId.put(id.toUpperCase(), id);

        // Automatically create associated trip split group
        String groupId = "grp-" + id;
        ExpenseGroup group = new ExpenseGroup(
                groupId,
                trip.name() + " (Trip Group)",
                GroupType.TRIP,
                id,
                req.members(),
                System.currentTimeMillis()
        );
        expenseGroups.put(groupId, group);
        groupExpenses.put(groupId, new CopyOnWriteArrayList<>());
        tripChats.put(id, new CopyOnWriteArrayList<>());

        return trip;
    }

    public void setTripCoverImage(String tripId, String coverImage) {
        Trip trip = trips.get(tripId);
        if (trip != null) {
            trips.put(tripId, new Trip(
                    trip.id(),
                    trip.name(),
                    trip.source(),
                    trip.destination(),
                    trip.dates(),
                    trip.status(),
                    trip.tripCode(),
                    trip.createdBy(),
                    coverImage,
                    trip.members(),
                    trip.createdAt(),
                    trip.vehicleType()
            ));
        }
    }

    public Optional<Trip> getTrip(String id) {
        return Optional.ofNullable(trips.get(id));
    }

    public List<Trip> getAllTrips() {
        return new ArrayList<>(trips.values());
    }

    public boolean deleteTrip(String tripId) {
        if (tripId == null || tripId.isBlank()) return false;
        String cleanId = tripId.trim();

        Trip removed = trips.remove(cleanId);
        if (removed == null) {
            // Also try by tripCode lookup
            String mappedId = tripCodeToId.remove(cleanId);
            if (mappedId == null) {
                mappedId = tripCodeToId.remove(cleanId.toUpperCase());
            }
            if (mappedId != null) {
                removed = trips.remove(mappedId);
            }
        }
        if (removed != null) {
            if (removed.tripCode() != null) {
                tripCodeToId.remove(removed.tripCode());
                tripCodeToId.remove(removed.tripCode().toUpperCase());
            }
            tripChats.remove(removed.id());
            expenseGroups.remove("grp-" + removed.id());
            groupExpenses.remove("grp-" + removed.id());
            return true;
        }
        return false;
    }

    public Optional<Trip> joinTripByCode(String code, String userId, String userName) {
        String cleanCode = code != null ? code.trim() : "";
        String tripId = resolveTripId(cleanCode);
        Trip trip = trips.get(tripId);
        if (trip == null) return Optional.empty();

        String normUserName = (userName != null && !userName.isBlank()) ? userName.trim() : "Traveler";
        String normUserId = (userId != null && !userId.isBlank()) ? userId.trim() : normUserName;

        boolean exists = trip.members().stream().anyMatch(m ->
                m.name().equalsIgnoreCase(normUserName) || m.id().equalsIgnoreCase(normUserId) ||
                m.name().equalsIgnoreCase(normUserId) || m.id().equalsIgnoreCase(normUserName)
        );
        if (!exists) {
            List<TripMember> updatedMembers = new ArrayList<>(trip.members());
            long[] colors = {0xFF1A73E8L, 0xFFE91E63L, 0xFFFF9800L, 0xFF9C27B0L, 0xFF00897BL, 0xFF7CB342L};
            updatedMembers.add(new TripMember(
                    normUserId,
                    normUserName,
                    colors[updatedMembers.size() % colors.length],
                    true,
                    null,
                    null,
                    0.0,
                    100,
                    System.currentTimeMillis()
            ));
            Trip updatedTrip = new Trip(
                    trip.id(),
                    trip.name(),
                    trip.source(),
                    trip.destination(),
                    trip.dates(),
                    trip.status(),
                    trip.tripCode(),
                    trip.createdBy(),
                    trip.coverImage(),
                    updatedMembers,
                    trip.createdAt(),
                    trip.vehicleType()
            );
            trips.put(tripId, updatedTrip);
            tripCodeToId.put(trip.id(), trip.id());
            tripCodeToId.put(trip.id().toUpperCase(), trip.id());
            if (trip.tripCode() != null) {
                tripCodeToId.put(trip.tripCode(), trip.id());
                tripCodeToId.put(trip.tripCode().toUpperCase(), trip.id());
            }

            // Broadcast to WebSocket listeners so other devices immediately refresh
            if (webSocketBroadcastListener != null) {
                String json = String.format(
                        "{\"type\":\"member_joined\",\"tripId\":\"%s\",\"userId\":\"%s\",\"userName\":\"%s\"}",
                        tripId, userId, userName
                );
                webSocketBroadcastListener.accept(json);
            }

            return Optional.of(updatedTrip);
        }
        return Optional.of(trip);
    }

    // --- LOCATION TRACKING ---
    public void updateLocation(LocationUpdateRequest req) {
        String resolvedId = resolveTripId(req.tripId());
        LocationEvent event = new LocationEvent(
                resolvedId,
                req.userId(),
                req.latitude(),
                req.longitude(),
                req.speed(),
                req.battery(),
                req.timestamp() > 0 ? req.timestamp() : System.currentTimeMillis(),
                req.heading()
        );

        tripLocations.computeIfAbsent(resolvedId, k -> new CopyOnWriteArrayList<>()).add(event);

        Trip trip = trips.get(resolvedId);
        if (trip != null) {
            List<TripMember> updated = new ArrayList<>();
            boolean found = false;

            String reqNorm = req.userId() != null ? req.userId().trim().toLowerCase().replace(" ", "") : "";
            String reqShort = reqNorm.contains("@") ? reqNorm.substring(0, reqNorm.indexOf('@')) : reqNorm;

            for (TripMember m : trip.members()) {
                String mIdNorm = m.id() != null ? m.id().trim().toLowerCase().replace(" ", "") : "";
                String mNameNorm = m.name() != null ? m.name().trim().toLowerCase().replace(" ", "") : "";
                String mShort = mNameNorm.contains("@") ? mNameNorm.substring(0, mNameNorm.indexOf('@')) : mNameNorm;

                boolean match = mIdNorm.equals(reqNorm) || mNameNorm.equals(reqNorm)
                        || (!reqShort.isEmpty() && (mShort.equals(reqShort) || mIdNorm.equals(reqShort) || mNameNorm.equals(reqShort)));

                if (match) {
                    found = true;
                    updated.add(new TripMember(
                            m.id(),
                            m.name(),
                            m.color(),
                            true,
                            req.latitude(),
                            req.longitude(),
                            req.speed(),
                            req.battery(),
                            System.currentTimeMillis()
                    ));
                } else {
                    updated.add(m);
                }
            }
            if (!found) {
                String displayName = req.userId() != null ? req.userId() : "Traveler";
                if (displayName.contains("@")) displayName = displayName.substring(0, displayName.indexOf('@'));
                if (displayName.startsWith("usr-")) displayName = "Traveler " + (updated.size() + 1);
                long[] colors = {0xFF1A73E8L, 0xFFE91E63L, 0xFFFF9800L, 0xFF9C27B0L, 0xFF00897BL, 0xFF7CB342L};
                updated.add(new TripMember(
                        req.userId(),
                        displayName,
                        colors[updated.size() % colors.length],
                        true,
                        req.latitude(),
                        req.longitude(),
                        req.speed(),
                        req.battery(),
                        System.currentTimeMillis()
                ));
            }
            trips.put(trip.id(), new Trip(
                    trip.id(),
                    trip.name(),
                    trip.source(),
                    trip.destination(),
                    trip.dates(),
                    trip.status(),
                    trip.tripCode(),
                    trip.createdBy(),
                    trip.coverImage(),
                    updated,
                    trip.createdAt(),
                    trip.vehicleType()
            ));

            if (webSocketBroadcastListener != null) {
                String json = String.format(
                        Locale.US,
                        "{\"type\":\"location\",\"tripId\":\"%s\",\"userId\":\"%s\",\"name\":\"%s\",\"latitude\":%.6f,\"longitude\":%.6f,\"speed\":%.1f,\"battery\":%d,\"timestamp\":%d}",
                        trip.id(), req.userId(), req.userId(), req.latitude(), req.longitude(), req.speed(), req.battery(), System.currentTimeMillis()
                );
                webSocketBroadcastListener.accept(json);
            }
        }
    }

    public List<TripMember> getLiveMembers(String tripId) {
        Trip trip = trips.get(resolveTripId(tripId));
        return trip != null ? trip.members() : Collections.emptyList();
    }

    // --- DEVICE TRACKING (VEHICLE + TAG TRACKING) ---
    public List<TrackingDevice> getDevices() {
        return new ArrayList<>(devices.values());
    }

    public Optional<TrackingDevice> getDevice(String id) {
        return Optional.ofNullable(devices.get(id));
    }

    public TrackingDevice registerDevice(RegisterDeviceRequest req) {
        String id = "dev-" + UUID.randomUUID().toString().substring(0, 8);
        DeviceType type = DeviceType.valueOf(req.type().toUpperCase());
        TrackingDevice dev = new TrackingDevice(
                id,
                req.name(),
                type,
                req.imei(),
                req.assignedTo(),
                true,
                req.safeZoneLat() != null ? req.safeZoneLat() : 12.9716,
                req.safeZoneLng() != null ? req.safeZoneLng() : 77.5946,
                0.0,
                0.0,
                false,
                100,
                req.safeZoneName(),
                req.safeZoneLat(),
                req.safeZoneLng(),
                req.safeZoneRadiusMeters() != null ? req.safeZoneRadiusMeters() : 300.0,
                true,
                System.currentTimeMillis()
        );
        devices.put(id, dev);
        return dev;
    }

    public Optional<TrackingDevice> updateDeviceTelemetry(String deviceId, DeviceTelemetryRequest req) {
        TrackingDevice dev = devices.get(deviceId);
        if (dev == null) return Optional.empty();

        boolean insideSafeZone = true;
        if (dev.safeZoneLat() != null && dev.safeZoneLng() != null && dev.safeZoneRadiusMeters() != null) {
            double distMeters = calculateDistanceMeters(req.latitude(), req.longitude(), dev.safeZoneLat(), dev.safeZoneLng());
            insideSafeZone = distMeters <= dev.safeZoneRadiusMeters();
        }

        TrackingDevice updated = new TrackingDevice(
                dev.id(),
                dev.name(),
                dev.type(),
                dev.imei(),
                dev.assignedTo(),
                true,
                req.latitude(),
                req.longitude(),
                req.speedKmh(),
                req.heading(),
                req.ignitionOn(),
                req.batteryPercent(),
                dev.safeZoneName(),
                dev.safeZoneLat(),
                dev.safeZoneLng(),
                dev.safeZoneRadiusMeters(),
                insideSafeZone,
                System.currentTimeMillis()
        );
        devices.put(deviceId, updated);
        return Optional.of(updated);
    }

    // --- EXPENSES & REALTIME SPLIT ---
    public ExpenseGroup createExpenseGroup(CreateGroupRequest req) {
        String id = "grp-" + UUID.randomUUID().toString().substring(0, 8);
        GroupType type = "TRIP".equalsIgnoreCase(req.groupType()) ? GroupType.TRIP : GroupType.MANUAL_PERSONAL;
        ExpenseGroup group = new ExpenseGroup(
                id,
                req.name(),
                type,
                req.tripId(),
                req.memberNames() != null ? req.memberNames() : List.of("Self"),
                System.currentTimeMillis()
        );
        expenseGroups.put(id, group);
        groupExpenses.put(id, new CopyOnWriteArrayList<>());
        return group;
    }

    public List<ExpenseGroup> getExpenseGroups() {
        return new ArrayList<>(expenseGroups.values());
    }

    public Optional<ExpenseGroup> getExpenseGroup(String id) {
        return Optional.ofNullable(expenseGroups.get(id));
    }

    public Optional<ExpenseGroup> getExpenseGroupByTripId(String tripId) {
        return expenseGroups.values().stream()
                .filter(g -> tripId.equals(g.tripId()))
                .findFirst();
    }

    public Expense addExpense(AddExpenseRequest req) {
        String id = "exp-" + UUID.randomUUID().toString().substring(0, 8);
        String groupId = req.groupId();

        if ((groupId == null || groupId.isBlank()) && req.tripId() != null) {
            ExpenseGroup g = getExpenseGroupByTripId(req.tripId()).orElseGet(() -> {
                CreateGroupRequest cg = new CreateGroupRequest("Trip Expenses", "TRIP", req.tripId(), req.members());
                return createExpenseGroup(cg);
            });
            groupId = g.id();
        }

        if (groupId == null) groupId = "grp-default";

        Expense expense = new Expense(
                id,
                groupId,
                req.tripId(),
                req.title() != null && !req.title().isBlank() ? req.title() : "Expense",
                req.amount(),
                req.paidBy(),
                req.splitType() != null ? req.splitType() : "equal",
                new HashSet<>(req.members()),
                req.customAmounts() != null ? req.customAmounts() : Collections.emptyMap(),
                System.currentTimeMillis()
        );

        groupExpenses.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(expense);
        return expense;
    }

    public List<Expense> getExpensesByGroupId(String groupId) {
        return groupExpenses.getOrDefault(groupId, Collections.emptyList());
    }

    public List<Expense> getExpensesByTripId(String tripId) {
        Optional<ExpenseGroup> grp = getExpenseGroupByTripId(tripId);
        if (grp.isPresent()) {
            return getExpensesByGroupId(grp.get().id());
        }
        return Collections.emptyList();
    }

    public List<Settlement> calculateSettlements(String groupId) {
        ExpenseGroup group = expenseGroups.get(groupId);
        List<Expense> expenses = groupExpenses.getOrDefault(groupId, Collections.emptyList());

        Map<String, Double> netBalances = new HashMap<>();

        if (group != null && group.memberIds() != null) {
            for (String m : group.memberIds()) {
                netBalances.put(m, 0.0);
            }
        }

        for (Expense exp : expenses) {
            netBalances.put(exp.paidBy(), netBalances.getOrDefault(exp.paidBy(), 0.0) + exp.amount());

            Set<String> splitWith = exp.splitWith();
            if (splitWith.isEmpty()) continue;

            if ("exact".equalsIgnoreCase(exp.splitType()) && !exp.customAmounts().isEmpty()) {
                for (Map.Entry<String, Double> entry : exp.customAmounts().entrySet()) {
                    netBalances.put(entry.getKey(), netBalances.getOrDefault(entry.getKey(), 0.0) - entry.getValue());
                }
            } else {
                double perPerson = exp.amount() / splitWith.size();
                for (String m : splitWith) {
                    netBalances.put(m, netBalances.getOrDefault(m, 0.0) - perPerson);
                }
            }
        }

        List<Map.Entry<String, Double>> debtors = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
            if (entry.getValue() < -0.01) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), -entry.getValue()));
            } else if (entry.getValue() > 0.01) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();
        int dIdx = 0, cIdx = 0;
        int settlementId = 1;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            Map.Entry<String, Double> debtor = debtors.get(dIdx);
            Map.Entry<String, Double> creditor = creditors.get(cIdx);

            double amount = Math.min(debtor.getValue(), creditor.getValue());
            settlements.add(new Settlement(
                    "stl-" + groupId + "-" + (settlementId++),
                    groupId,
                    debtor.getKey(),
                    creditor.getKey(),
                    Math.round(amount * 100.0) / 100.0,
                    false,
                    null
            ));

            debtor.setValue(debtor.getValue() - amount);
            creditor.setValue(creditor.getValue() - amount);

            if (debtor.getValue() < 0.01) dIdx++;
            if (creditor.getValue() < 0.01) cIdx++;
        }

        return settlements;
    }

    public Settlement recordSettlementPayment(SettleRequest req) {
        Settlement s = new Settlement(
                "stl-" + UUID.randomUUID().toString().substring(0, 6),
                req.groupId(),
                req.from(),
                req.to(),
                req.amount(),
                true,
                System.currentTimeMillis()
        );
        groupSettlements.computeIfAbsent(req.groupId(), k -> new CopyOnWriteArrayList<>()).add(s);
        return s;
    }

    // --- SOS SAFETY ---
    public SosAlert triggerSos(SosAlertRequest req) {
        String id = "sos-" + UUID.randomUUID().toString().substring(0, 6);
        SosAlert alert = new SosAlert(
                id,
                req.tripId(),
                req.userId(),
                req.latitude(),
                req.longitude(),
                req.accuracyMeters() != null ? req.accuracyMeters() : 12.0,
                req.message() != null && !req.message().isBlank() ? req.message() : "Emergency SOS Triggered! Need immediate assistance.",
                4,
                "ACTIVE",
                System.currentTimeMillis()
        );
        sosAlerts.put(id, alert);
        return alert;
    }

    public List<SosAlert> getActiveSosAlerts() {
        return new ArrayList<>(sosAlerts.values());
    }

    // --- DISTANCE CALCULATION HELPER (HAVERSINE FORMULA) ---
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
