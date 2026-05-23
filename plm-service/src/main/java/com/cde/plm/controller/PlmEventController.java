package com.cde.plm.controller;

import com.cde.plm.event.*;
import com.cde.plm.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/internal/events")
@RequiredArgsConstructor @Slf4j
public class PlmEventController {
    private final ProductService productService;
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
                case "cde.qlm.ncr.raised" -> productService.handleNcrRaised(
                        objectMapper.convertValue(payload, NcrRaisedEvent.class), correlationId);
                case "cde.qlm.capa.raised" -> productService.handleCapaRaised(
                        objectMapper.convertValue(payload, CapaRaisedEvent.class), correlationId);
                case "cde.llm.user.profile.updated" -> productService.handleUserProfileUpdated(
                        objectMapper.convertValue(payload, UserProfileUpdatedEvent.class), correlationId);
                case "cde.plm.phase.gate.check.result" -> productService.handlePhaseGateCheckResult(
                        objectMapper.convertValue(payload, PhaseGateCheckResultEvent.class), correlationId);
                default -> log.debug("PLM ignoring event type: {}", type);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("PLM event handler error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
