package com.cde.plm.repository;

import com.cde.plm.entity.PhaseGatePendingCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhaseGatePendingCheckRepository extends JpaRepository<PhaseGatePendingCheck, String> {

    Optional<PhaseGatePendingCheck> findByVersionIdAndTargetPhaseAndStatus(
            UUID versionId, String targetPhase, PhaseGatePendingCheck.CheckStatus status);

    List<PhaseGatePendingCheck> findByVersionIdAndTargetPhaseAndStatusNot(
            UUID versionId, String targetPhase, PhaseGatePendingCheck.CheckStatus status);

    List<PhaseGatePendingCheck> findByStatusAndCreatedAtBefore(
            PhaseGatePendingCheck.CheckStatus status, OffsetDateTime cutoff);
}
