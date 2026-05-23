package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TdpResponse {
    private UUID tdpId;
    private UUID versionId;
    private String documentName;
    private String documentType;
    private String storageUrl;
    private String documentHash;
    private Long fileSizeBytes;
    private String approvalStatus;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private UUID uploadedBy;
    private OffsetDateTime createdAt;
}
