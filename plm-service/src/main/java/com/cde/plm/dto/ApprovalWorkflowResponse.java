package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalWorkflowResponse {
    private UUID workflowId;
    private String entityType;
    private UUID entityId;
    private Integer stepOrder;
    private UUID approverUserId;
    private String status;
    private String comments;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
}
