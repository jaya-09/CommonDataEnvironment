package com.cde.qlm.repository;
import com.cde.qlm.entity.QualityAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface QualityAuditRepository extends JpaRepository<QualityAudit, UUID> {
    List<QualityAudit> findByStatus(QualityAudit.AuditStatus status);
    List<QualityAudit> findByLeadAuditorId(UUID auditorId);
}
