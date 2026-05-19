package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "approval_workflow")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalWorkflow {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "workflow_id", updatable = false, nullable = false)
    private UUID workflowId;
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    @Column(name = "approver_user_id", nullable = false)
    private UUID approverUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.PENDING;
    @Column(name = "comments")
    private String comments;
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    public enum EntityType { VERSION, CHANGE_REQUEST }
    public enum WorkflowStatus { PENDING, APPROVED, REJECTED, DELEGATED }
}
