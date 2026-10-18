package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.UpdateProfileRequest;
import com.travelbuddy.api.model.Models.UserProfile;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final TravelBuddyStateService stateService;

    public ProfileController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(
            @RequestParam(defaultValue = "usr-1") String userId
    ) {
        return ResponseEntity.ok(stateService.getUserProfile(userId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserProfile> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(stateService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(
            @RequestParam(defaultValue = "usr-1") String userId,
            @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(stateService.updateUserProfile(userId, request));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(
            @RequestParam(defaultValue = "usr-1") String userId
    ) {
        stateService.logout(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }
}
