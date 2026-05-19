package com.cde.qlm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateNcrRequest {
    @NotBlank public String title;
    public String description;
    public UUID productVersionId;
    public String productCode;
    @Pattern(regexp = "CRITICAL|MAJOR|MINOR") public String severity;
}
