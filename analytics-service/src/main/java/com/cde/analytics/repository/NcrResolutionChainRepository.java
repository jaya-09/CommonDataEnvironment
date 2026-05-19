package com.cde.analytics.repository;

import com.cde.analytics.entity.NcrResolutionChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NcrResolutionChainRepository extends JpaRepository<NcrResolutionChain, UUID> {
    Optional<NcrResolutionChain> findByNcrId(UUID ncrId);
    List<NcrResolutionChain> findByResolutionStatusOrderByNcrRaisedAtDesc(String status);
    List<NcrResolutionChain> findByProductCodeOrderByNcrRaisedAtDesc(String productCode);
    List<NcrResolutionChain> findByCourseCodeAndTrainingTriggeredTrue(String courseCode);

    @Query("SELECT n FROM NcrResolutionChain n WHERE n.resolutionStatus != 'FULLY_RESOLVED' " +
           "ORDER BY CASE n.ncrSeverity WHEN 'CRITICAL' THEN 0 WHEN 'MAJOR' THEN 1 ELSE 2 END, n.ncrRaisedAt ASC")
    List<NcrResolutionChain> findAllOpenOrderBySeverity();
}
