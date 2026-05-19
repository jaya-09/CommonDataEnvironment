package com.cde.plm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileUpdatedEvent {
    private UUID userId;
    private String employeeId;
    private String email;
    private String fullName;
    private String role;
    private String department;
    private String changeType;
}
