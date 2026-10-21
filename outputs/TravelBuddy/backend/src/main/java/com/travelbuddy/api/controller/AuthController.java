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
        boolean ok = stateService.requestOtp(request.phone());
        if (ok) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP sent successfully to " + request.phone()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid phone number provided"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = stateService.verifyOtp(request.phone(), request.otp());
        return ResponseEntity.ok(response);
    }
}
