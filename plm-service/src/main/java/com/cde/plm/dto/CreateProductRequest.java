package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateProductRequest {
    @NotBlank private String productCode;
    @NotBlank private String name;
    private String description;
}
