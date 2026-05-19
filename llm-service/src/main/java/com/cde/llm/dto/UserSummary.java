package com.cde.llm.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSummary {
    UUID userId;
    String employeeId;
    String fullName;
}
