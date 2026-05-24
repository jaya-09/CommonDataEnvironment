package com.cde.llm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTrainingRequest {
    UUID enrollmentId;
    @NotNull @Min(0) @Max(100) Integer score;
}
