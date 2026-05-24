package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificationResponse {
    UUID certId;
    String certNumber;
    String courseCode;
    String courseTitle;
    int score;
    String status;
    OffsetDateTime issuedAt;
    OffsetDateTime expiresAt;
}
