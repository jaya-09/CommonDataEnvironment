package com.cde.plm.repository;
import com.cde.plm.entity.TraceabilityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface TraceabilityAuditLogRepository extends JpaRepository<TraceabilityAuditLog, UUID> {
    List<TraceabilityAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String type, UUID id);
}
