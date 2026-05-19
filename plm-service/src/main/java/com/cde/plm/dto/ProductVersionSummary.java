package com.cde.plm.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductVersionSummary {
    private UUID versionId;
    private String versionNumber;
    private String status;
    private String currentPhase;
}
