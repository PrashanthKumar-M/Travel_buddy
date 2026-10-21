package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.model.Models.*;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final TravelBuddyStateService stateService;

    public LocationController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateRequest request) {
        stateService.updateLocation(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Location updated"));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<List<TripMember>> getTripMembersLocations(@PathVariable String tripId) {
        return ResponseEntity.ok(stateService.getLiveMembers(tripId));
    }
}
