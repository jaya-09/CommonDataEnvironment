package com.cde.llm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateUserRequest {
    @NotBlank String employeeId;
    @Email @NotBlank String email;
    @NotBlank String fullName;
    @NotBlank @Pattern(regexp = "ENGINEER|QUALITY_MANAGER|TRAINER|ADMIN") String role;
    String department;
    @Pattern(regexp = "JUNIOR|MID|SENIOR") String competencyLevel;
}
