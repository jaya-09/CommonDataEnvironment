package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductHealthDto {
    private UUID versionId;
    private UUID productId;
    private String productCode;
    private String productName;
    private String versionNumber;
    private String versionStatus;
    private String currentPhase;
    private Integer phaseSequenceOrder;
    private int openNcrCount;
    private int criticalNcrCount;
    private int openCapaCount;
    private int highRiskCount;
    private int uncertifiedUserCount;
    private boolean phaseCertReady;
    private String overallStatus;
    private OffsetDateTime lastPlmEventAt;
    private OffsetDateTime lastQlmEventAt;
    private OffsetDateTime lastLlmEventAt;
    private OffsetDateTime snapshotUpdatedAt;
}
