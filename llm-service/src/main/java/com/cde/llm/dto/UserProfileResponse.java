package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    UUID userId;
    String employeeId;
    String email;
    String fullName;
    String role;
    String department;
    String competencyLevel;
    boolean active;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
