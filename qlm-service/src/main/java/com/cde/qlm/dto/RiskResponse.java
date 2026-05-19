package com.cde.qlm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskResponse {
    private UUID riskId;
    private String riskNumber;
    private UUID productVersionId;
    private String title;
    private String description;
    private String category;
    private Integer likelihood;
    private Integer impact;
    private Integer riskScore;
    private String mitigationPlan;
    private UUID ownerUserId;
    private String status;
    private OffsetDateTime createdAt;
}
