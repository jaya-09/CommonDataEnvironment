package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskMatrixDto {
    private UUID entryId;
    private String sourceService;
    private String productCode;
    private String riskCategory;
    private String riskTitle;
    private Integer likelihood;
    private Integer impact;
    private Integer riskScore;
    private String riskLevel;
    private UUID linkedNcrId;
    private UUID linkedCapaId;
    private String linkedPhase;
    private boolean resolved;
    private OffsetDateTime detectedAt;
    private int heatmapRow;
    private int heatmapCol;
}
