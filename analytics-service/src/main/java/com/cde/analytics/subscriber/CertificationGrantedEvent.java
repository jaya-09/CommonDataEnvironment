package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class CertificationGrantedEvent {
    private UUID certId;
    private UUID userId;
    private String employeeId;
    private UUID courseId;
    private String courseCode;
    private String mandatoryForPhase;
    private Integer score;
    private OffsetDateTime issuedAt;
}
