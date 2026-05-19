package com.cde.llm.controller;

import com.cde.llm.event.EventEnvelope;
import com.cde.llm.service.EventHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

/**
 * Receives push events from Google Pub/Sub.
 * Pub/Sub pushes JSON to this endpoint; we decode and route to the handler.
 *
 * Internal endpoint — not exposed via API Gateway.
 */
@RestController
@RequestMapping("/internal/events")
@Slf4j
@RequiredArgsConstructor
public class EventSubscriberController {

    private final EventHandlerService eventHandlerService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> handleEvent(@RequestBody Map<String, Object> pubsubMessage) {
        try {
            // Pub/Sub push format: { "message": { "data": "<base64>", "attributes": {...} } }
            Map<String, Object> message = (Map<String, Object>) pubsubMessage.get("message");
            String encodedData = (String) message.get("data");
            String jsonData = new String(Base64.getDecoder().decode(encodedData));

            EventEnvelope envelope = objectMapper.readValue(jsonData, EventEnvelope.class);
            log.info("Received event [type={}, eventId={}, correlationId={}]",
                    envelope.getType(), envelope.getEventId(), envelope.getCorrelationId());

            eventHandlerService.handle(envelope);

            // Returning 2xx acknowledges the message to Pub/Sub
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process Pub/Sub event: {}", e.getMessage(), e);
            // Return 5xx to trigger Pub/Sub retry
            return ResponseEntity.internalServerError().build();
        }
    }
}
