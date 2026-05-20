package com.cde.qlm.controller;

import com.cde.qlm.event.PhaseGateCheckRequestedEvent;
import com.cde.qlm.event.UserProfileUpdatedEvent;
import com.cde.qlm.service.QlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/internal/events")
@RequiredArgsConstructor @Slf4j
public class QlmEventController {
    private final QlmService qlmService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody Map<String, Object> pubsubMessage) {
        try {
            Map<String, Object> msg = (Map<String, Object>) pubsubMessage.get("message");
            String json = new String(Base64.getDecoder().decode((String) msg.get("data")));
            Map<String, Object> envelope = objectMapper.readValue(json, Map.class);
            String type = (String) envelope.get("type");
            String correlationId = (String) envelope.getOrDefault("correlationId", "");
            Object payload = envelope.get("payload");
            switch (type) {
                case "cde.llm.user.profile.updated" -> qlmService.handleUserProfileUpdated(
                        objectMapper.convertValue(payload, UserProfileUpdatedEvent.class), correlationId);
                case "cde.plm.phase.gate.check.requested" -> qlmService.handlePhaseGateCheckRequested(
                        objectMapper.convertValue(payload, PhaseGateCheckRequestedEvent.class), correlationId);
                default -> log.debug("QLM ignoring event type: {}", type);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("QLM event error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
