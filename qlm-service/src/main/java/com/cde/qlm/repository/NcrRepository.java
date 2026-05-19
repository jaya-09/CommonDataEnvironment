package com.cde.qlm.repository;
import com.cde.qlm.entity.NonConformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface NcrRepository extends JpaRepository<NonConformance, UUID> {
    List<NonConformance> findByStatus(NonConformance.NcrStatus status);
    List<NonConformance> findByProductVersionId(UUID productVersionId);
    List<NonConformance> findBySeverity(NonConformance.Severity severity);
    long countByProductVersionIdAndSeverityAndStatusNot(UUID versionId, NonConformance.Severity severity, NonConformance.NcrStatus status);
}
