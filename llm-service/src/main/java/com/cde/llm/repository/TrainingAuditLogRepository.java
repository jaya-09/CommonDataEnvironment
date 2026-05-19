package com.cde.llm.repository;

import com.cde.llm.entity.TrainingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingAuditLogRepository extends JpaRepository<TrainingAuditLog, UUID> {
    List<TrainingAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
