package com.cde.qlm.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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
