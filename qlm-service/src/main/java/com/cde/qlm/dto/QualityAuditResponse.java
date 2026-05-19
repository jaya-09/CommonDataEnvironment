package com.cde.qlm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QualityAuditResponse {
    private UUID auditId;
    private String auditNumber;
    private String auditType;
    private String scope;
    private LocalDate scheduledDate;
    private LocalDate conductedDate;
    private String status;
    private UUID leadAuditorId;
    private String summary;
    private OffsetDateTime createdAt;
}
