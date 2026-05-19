package com.cde.qlm.repository;
import com.cde.qlm.entity.QualityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface QualityAuditLogRepository extends JpaRepository<QualityAuditLog, UUID> {
    List<QualityAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String type, UUID id);
}
