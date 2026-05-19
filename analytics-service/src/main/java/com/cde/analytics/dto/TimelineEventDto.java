package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimelineEventDto {
    private UUID timelineId;
    private String eventType;
    private String sourceService;
    private String entityType;
    private UUID entityId;
    private String entityRef;
    private String description;
    private String severity;
    private String productCode;
    private UUID versionId;
    private String correlationId;
    private OffsetDateTime occurredAt;
}
