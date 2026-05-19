package com.cde.llm.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificationGrantedEvent {
    private UUID certId;
    private UUID userId;
    private String employeeId;
    private UUID courseId;
    private String courseCode;
    private String mandatoryForPhase;
    private Integer score;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiresAt;
}
