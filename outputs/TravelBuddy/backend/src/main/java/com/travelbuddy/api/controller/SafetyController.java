package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.SosAlertRequest;
import com.travelbuddy.api.model.Models.SosAlert;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
public class SafetyController {

    private final TravelBuddyStateService stateService;

    public SafetyController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping("/sos")
    public ResponseEntity<SosAlert> triggerSos(@RequestBody SosAlertRequest request) {
        SosAlert alert = stateService.triggerSos(request);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/sos")
    public ResponseEntity<List<SosAlert>> getActiveAlerts() {
        return ResponseEntity.ok(stateService.getActiveSosAlerts());
    }
}
