package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateProductRequest {
    @NotBlank private String productCode;
    @NotBlank private String name;
    private String description;
    private UUID approverUserId;
}
