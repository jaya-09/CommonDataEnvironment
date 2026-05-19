package com.cde.plm.repository;
import com.cde.plm.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {
    List<ApprovalWorkflow> findByEntityTypeAndEntityIdOrderByStepOrderAsc(ApprovalWorkflow.EntityType type, UUID entityId);
    List<ApprovalWorkflow> findByApproverUserIdAndStatus(UUID userId, ApprovalWorkflow.WorkflowStatus status);
}
