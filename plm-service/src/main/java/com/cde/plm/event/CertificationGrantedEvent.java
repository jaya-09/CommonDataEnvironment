package com.cde.plm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificationGrantedEvent {
    private UUID certId;
    private UUID userId;
    private String employeeId;
    private String courseCode;
    private String mandatoryForPhase;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiresAt;
}
