package com.cde.analytics.repository;

import com.cde.analytics.entity.ProductHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductHealthRepository extends JpaRepository<ProductHealthSnapshot, UUID> {
    List<ProductHealthSnapshot> findByOverallStatusOrderBySnapshotUpdatedAtDesc(String status);
    List<ProductHealthSnapshot> findByCurrentPhaseOrderByProductCode(String phase);
    List<ProductHealthSnapshot> findByCriticalNcrCountGreaterThanOrderByCriticalNcrCountDesc(int threshold);
    Optional<ProductHealthSnapshot> findByProductCodeAndVersionNumber(String productCode, String versionNumber);

    @Query("SELECT p FROM ProductHealthSnapshot p ORDER BY " +
           "CASE p.overallStatus WHEN 'BLOCKED' THEN 0 WHEN 'AT_RISK' THEN 1 ELSE 2 END, " +
           "p.criticalNcrCount DESC")
    List<ProductHealthSnapshot> findAllOrderByRisk();
}
