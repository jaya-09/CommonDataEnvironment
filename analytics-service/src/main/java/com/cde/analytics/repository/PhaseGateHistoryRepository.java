package com.cde.analytics.repository;

import com.cde.analytics.entity.PhaseGateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PhaseGateHistoryRepository extends JpaRepository<PhaseGateHistory, UUID> {
    List<PhaseGateHistory> findByVersionIdOrderByOccurredAtDesc(UUID versionId);
    List<PhaseGateHistory> findByGateResultOrderByOccurredAtDesc(String result);
    List<PhaseGateHistory> findTop20ByOrderByOccurredAtDesc();

    @Query("SELECT COUNT(p) FROM PhaseGateHistory p WHERE p.gateResult = :result AND p.occurredAt >= :since")
    long countByResultSince(@Param("result") String result, @Param("since") OffsetDateTime since);
}
