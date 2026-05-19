package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseGateDto {
    private UUID gateId;
    private UUID versionId;
    private String productCode;
    private String versionNumber;
    private String fromPhase;
    private String toPhase;
    private String gateResult;
    private Map<String, String> blockedBy;
    private String correlationId;
    private OffsetDateTime occurredAt;
}
