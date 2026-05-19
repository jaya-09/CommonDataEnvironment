package com.cde.qlm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


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
        return EventEnvelope.builder().eventId(UUID.randomUUID().toString()).type(type)
                .version("1.0").timestamp(OffsetDateTime.now())
                .correlationId(correlationId).payload(payload).build();
    }
}

// ── PUBLISHED ────────────────────────────────────────────────
