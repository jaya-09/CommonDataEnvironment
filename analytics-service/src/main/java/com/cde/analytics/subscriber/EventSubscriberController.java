package com.cde.analytics.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

/**
 * Receives ALL Pub/Sub push events from every service topic.
 * A single endpoint is registered for all subscriptions.
 *
 * Pub/Sub push format:
 * {
 *   "message": {
 *     "data": "<base64-encoded JSON>",
 *     "attributes": { "eventType": "...", "correlationId": "..." }
 *   }
 * }
 */
@RestController
@RequestMapping("/internal/events")
@Slf4j
@RequiredArgsConstructor
public class EventSubscriberController {

    private final AnalyticsEventHandler handler;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> pubsubMessage) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) pubsubMessage.get("message");
            if (message == null) {
                log.warn("Pub/Sub message missing 'message' field");
                return ResponseEntity.badRequest().build();
            }

            String encodedData = (String) message.get("data");
            String json = new String(Base64.getDecoder().decode(encodedData));
            EventEnvelope envelope = objectMapper.readValue(json, EventEnvelope.class);

            log.debug("Analytics received [type={}, eventId={}, correlationId={}]",
                    envelope.getType(), envelope.getEventId(), envelope.getCorrelationId());

            handler.handle(envelope);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Analytics failed to parse event: {}", e.getMessage(), e);
            // Return 5xx so Pub/Sub retries
            return ResponseEntity.internalServerError().build();
        }
    }
}
