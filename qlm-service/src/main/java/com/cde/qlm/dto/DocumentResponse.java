package com.cde.qlm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {
    private UUID docId;
    private String docNumber;
    private String revision;
    private String title;
    private UUID tdpRefId;
    private String storageUrl;
    private String documentHash;
    private String approvalStatus;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private LocalDate effectiveDate;
    private OffsetDateTime createdAt;
}
