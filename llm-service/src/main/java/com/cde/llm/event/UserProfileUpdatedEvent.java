package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileUpdatedEvent {
    private UUID userId;
    private String employeeId;
    private String email;
    private String fullName;
    private String role;
    private String department;
    private String competencyLevel;
    private String changeType;
}
