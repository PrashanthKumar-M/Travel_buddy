package com.travelbuddy.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbuddy.api.dto.Dto.LocationUpdateRequest;
import com.travelbuddy.api.service.TravelBuddyStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final TravelBuddyStateService stateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LiveWebSocketHandler(TravelBuddyStateService stateService) {
        this.stateService = stateService;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.stateService.setWebSocketBroadcastListener(msg -> broadcast(msg, null));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: session id {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            JsonNode root = objectMapper.readTree(payload);

            if (root.has("tripId") && root.has("userId") && root.has("latitude") && root.has("longitude")) {
                String tripId = root.get("tripId").asText();
                String userId = root.get("userId").asText();
                double latitude = root.get("latitude").asDouble();
                double longitude = root.get("longitude").asDouble();
                double speed = root.has("speed") ? root.get("speed").asDouble() : 0.0;
                int battery = root.has("battery") ? root.get("battery").asInt() : 100;
                double heading = root.has("heading") ? root.get("heading").asDouble() : 0.0;
                long timestamp = root.has("timestamp") ? root.get("timestamp").asLong() : System.currentTimeMillis();

                stateService.updateLocation(new LocationUpdateRequest(
                        tripId, userId, latitude, longitude, speed, battery, heading, timestamp
                ));

                // Broadcast location update to all active sessions
                broadcast(payload, session);
            } else if (root.has("type") && "ping".equalsIgnoreCase(root.get("type").asText())) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.error("Failed to parse WebSocket message", e);
        }
    }

    public void broadcast(String message, WebSocketSession excludeSession) {
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && (excludeSession == null || !s.getId().equals(excludeSession.getId()))) {
                try {
                    s.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Error sending WebSocket message to session {}", s.getId(), e);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket disconnected: session id {}", session.getId());
    }
}
