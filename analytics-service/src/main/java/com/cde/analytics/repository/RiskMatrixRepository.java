package com.cde.analytics.repository;

import com.cde.analytics.entity.RiskMatrixEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskMatrixRepository extends JpaRepository<RiskMatrixEntry, UUID> {
    List<RiskMatrixEntry> findByResolvedFalseOrderByRiskScoreDesc();
    List<RiskMatrixEntry> findByProductCodeAndResolvedFalseOrderByRiskScoreDesc(String productCode);
    List<RiskMatrixEntry> findByRiskLevelAndResolvedFalseOrderByDetectedAtDesc(String level);
    Optional<RiskMatrixEntry> findBySourceServiceAndSourceId(String service, UUID sourceId);
}
