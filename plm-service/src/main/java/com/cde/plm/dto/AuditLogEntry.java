package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogEntry {
    private UUID logId;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID performedBy;
    private String eventSource;
    private String correlationId;
    private OffsetDateTime createdAt;
}
