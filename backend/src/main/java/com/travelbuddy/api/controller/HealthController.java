package com.travelbuddy.api.controller;

import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final TravelBuddyStateService stateService;

    public HealthController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping("/")
    public ResponseEntity<?> root() {
        return ResponseEntity.ok(Map.of(
                "service", "Travel Buddy Realtime Backend API",
                "status", "UP",
                "version", "1.0.0",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "auth", "/api/auth/request-otp",
                        "trips", "/api/trips",
                        "locations", "/api/locations",
                        "expenses", "/api/expenses",
                        "devices", "/api/devices",
                        "websocket", "/ws/live"
                )
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "uptimeSeconds", uptimeMs / 1000,
                "tripsCount", stateService.getAllTrips().size(),
                "devicesCount", stateService.getDevices().size(),
                "groupsCount", stateService.getExpenseGroups().size()
        ));
    }
}
