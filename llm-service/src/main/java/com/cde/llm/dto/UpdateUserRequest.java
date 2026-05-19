package com.cde.llm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateUserRequest {
    String fullName;
    @Pattern(regexp = "ENGINEER|QUALITY_MANAGER|TRAINER|ADMIN") String role;
    String department;
    @Pattern(regexp = "JUNIOR|MID|SENIOR") String competencyLevel;
}
