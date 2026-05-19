package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateBomComponentRequest {
    @NotBlank private String componentCode;
    @NotBlank private String name;
    private String componentType;
    private UUID parentComponentId;
    private String quantity;
    private String unit;
    private String notes;
}
