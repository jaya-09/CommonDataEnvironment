package com.cde.llm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {
    @NotNull UUID userId;
    @NotNull UUID courseId;
    String triggerSource;
    UUID triggerRefId;
    String triggerRefType;
    LocalDate deadline;
}
