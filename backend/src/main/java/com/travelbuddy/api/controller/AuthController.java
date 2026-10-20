package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TravelBuddyStateService stateService;

    public AuthController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody RequestOtpRequest request) {
        String otp = stateService.requestOtp(request.phone(), request.email());
        String target = request.email() != null && !request.email().isBlank() ? request.email() : request.phone();
        if (otp != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "otp", otp,
                    "message", "Dynamic OTP sent successfully to " + target
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid phone number or email provided"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = stateService.verifyOtp(request.phone(), request.email(), request.otp());
        if (!response.success()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
