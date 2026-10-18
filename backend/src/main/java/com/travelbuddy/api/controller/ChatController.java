package com.travelbuddy.api.controller;

import com.travelbuddy.api.dto.Dto.SendMessageRequest;
import com.travelbuddy.api.model.Models.ChatMessage;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final TravelBuddyStateService stateService;

    public ChatController(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping(value = {"/{tripId}", "/{tripId}/messages"})
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable String tripId) {
        return ResponseEntity.ok(stateService.getChatMessages(tripId));
    }

    @PostMapping(value = {"/{tripId}", "/{tripId}/messages"})
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String tripId,
            @RequestBody SendMessageRequest request
    ) {
        ChatMessage msg = stateService.sendChatMessage(tripId, request);
        return ResponseEntity.ok(msg);
    }
}
