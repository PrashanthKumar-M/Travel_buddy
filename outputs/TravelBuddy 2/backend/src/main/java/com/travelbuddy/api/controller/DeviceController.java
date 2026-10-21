package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.model.Models.*;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final TravelBuddyStateService stateService;

    public DeviceController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping
    public ResponseEntity<List<TrackingDevice>> getAllDevices() {
        return ResponseEntity.ok(stateService.getDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDevice(@PathVariable String id) {
        return stateService.getDevice(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TrackingDevice> registerDevice(@RequestBody RegisterDeviceRequest request) {
        TrackingDevice device = stateService.registerDevice(request);
        return ResponseEntity.ok(device);
    }

    @PostMapping("/{id}/telemetry")
    public ResponseEntity<?> updateTelemetry(@PathVariable String id, @RequestBody DeviceTelemetryRequest request) {
        return stateService.updateDeviceTelemetry(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
