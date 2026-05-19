package com.cde.plm.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {
    private String eventId;
    private String type;
    private String version;
    private OffsetDateTime timestamp;
    private String correlationId;
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
