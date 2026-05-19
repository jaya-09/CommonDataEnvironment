package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseTransitionRequest {
    @NotBlank private String targetPhase;
    private String notes;
}
