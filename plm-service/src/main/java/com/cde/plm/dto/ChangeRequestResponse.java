package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeRequestResponse {
    private UUID crId;
    private String crNumber;
    private UUID versionId;
    private String crType;
    private String title;
    private String description;
    private String reason;
    private String impactAnalysis;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
