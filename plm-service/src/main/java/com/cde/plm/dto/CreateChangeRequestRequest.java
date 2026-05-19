package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateChangeRequestRequest {
    @NotBlank public String title;
    public String description;
    public String reason;
    public String impactAnalysis;
    public String crType;
}
