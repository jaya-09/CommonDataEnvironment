package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVersionResponse {
    private UUID versionId;
    private UUID productId;
    private String productCode;
    private String productName;
    private String versionNumber;
    private String status;
    private String currentPhase;
    private Integer phaseSequence;
    private OffsetDateTime releasedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
