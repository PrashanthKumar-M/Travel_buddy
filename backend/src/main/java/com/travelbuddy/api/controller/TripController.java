package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.*;
import com.travelbuddy.api.model.Models.*;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TravelBuddyStateService stateService;

    public TripController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody CreateTripRequest request) {
        Trip trip = stateService.createTrip(request);
        return ResponseEntity.ok(trip);
    }

    @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
        return ResponseEntity.ok(stateService.getAllTrips());
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<?> getTrip(@PathVariable String tripId) {
        return stateService.getTrip(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{tripId}/live")
    public ResponseEntity<?> getLiveSnapshot(@PathVariable String tripId) {
        return stateService.getTrip(tripId)
                .map(trip -> ResponseEntity.ok(Map.of(
                        "id", trip.id(),
                        "tripName", trip.name(),
                        "source", trip.source(),
                        "destination", trip.destination(),
                        "tripCode", trip.tripCode(),
                        "status", trip.status(),
                        "members", trip.members()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinTripByCode(@RequestBody JoinTripRequest request) {
        return stateService.joinTripByCode(request.tripCode(), request.userId(), request.userName())
                .map(trip -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Successfully joined trip: " + trip.name(),
                        "trip", trip
                )))
                .orElse(ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid trip code: " + request.tripCode()
                )));
    }

    @PostMapping("/{tripId}/image")
    public ResponseEntity<?> setTripImage(
            @PathVariable String tripId,
            @RequestBody UploadImageRequest request
    ) {
        stateService.setTripCoverImage(tripId, request.imageData());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Trip image updated successfully",
                "coverImage", request.imageData()
        ));
    }
}
