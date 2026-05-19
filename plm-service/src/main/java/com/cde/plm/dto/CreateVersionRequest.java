package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateVersionRequest {
    @NotBlank private String versionNumber;
    private String description;
}
