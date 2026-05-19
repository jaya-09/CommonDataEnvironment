package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope {
    private String eventId;
    private String type;
    private String version;
    private OffsetDateTime timestamp;
    private String correlationId;
    private Object payload;
}
