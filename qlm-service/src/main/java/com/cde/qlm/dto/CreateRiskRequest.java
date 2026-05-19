package com.cde.qlm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateRiskRequest {
    @NotBlank private String title;
    private String description;
    private String category;
    private UUID productVersionId;
    private String productCode;
    @Min(1) @Max(5) private Integer likelihood;
    @Min(1) @Max(5) private Integer impact;
    private String mitigationPlan;
    private UUID ownerUserId;
}
