package com.cde.qlm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NcrResponse {
    public UUID ncrId;
    public String ncrNumber;
    public UUID productVersionId;
    public String productCode;
    public String severity;
    public String title;
    public String description;
    public String rootCause;
    public String status;
    public UUID reportedBy;
    public UUID assignedTo;
    public OffsetDateTime detectedAt;
    public OffsetDateTime closedAt;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
