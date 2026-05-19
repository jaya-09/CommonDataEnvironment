package com.cde.llm.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Standard envelope for ALL Pub/Sub events across CDE services.
 * Every event published or consumed follows this structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {

    /** Unique event ID — used for idempotency check on consumer side */
    private String eventId;

    /** Dot-notation event type: cde.llm.certification.granted */
    private String type;

    /** Schema version — allows evolving event payload without breaking consumers */
    private String version;

    /** ISO-8601 timestamp of when the event was created */
    private OffsetDateTime timestamp;

    /** Passed across all services for distributed tracing */
    private String correlationId;

    /** The actual event data — serialized as JSON */
    private Object payload;

    public static EventEnvelope of(String type, Object payload, String correlationId) {
        return EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .version("1.0")
                .timestamp(OffsetDateTime.now())
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}
