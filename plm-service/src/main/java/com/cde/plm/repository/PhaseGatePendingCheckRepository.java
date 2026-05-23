package com.cde.plm.repository;

import com.cde.plm.entity.PhaseGatePendingCheck;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhaseGatePendingCheckRepository extends JpaRepository<PhaseGatePendingCheck, String> {

    /**
     * Loads the check row with a PESSIMISTIC_WRITE lock.
     *
     * Used by handlePhaseGateCheckResult() to prevent a race condition where
     * concurrent QLM and LLM result events both read the record before either
     * commits. Without this lock, the second transaction overwrites the first
     * transaction's flag update, so bothResultsReceived() never returns true
     * and the phase gate stalls until the timeout job marks it TIMEOUT.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PhaseGatePendingCheck p WHERE p.gateCorrelationId = :id")
    Optional<PhaseGatePendingCheck> findByIdForUpdate(@Param("id") String id);

    Optional<PhaseGatePendingCheck> findByVersionIdAndTargetPhaseAndStatus(
            UUID versionId, String targetPhase, PhaseGatePendingCheck.CheckStatus status);

    List<PhaseGatePendingCheck> findByVersionIdAndTargetPhaseAndStatusNot(
            UUID versionId, String targetPhase, PhaseGatePendingCheck.CheckStatus status);

    List<PhaseGatePendingCheck> findByStatusAndCreatedAtBefore(
            PhaseGatePendingCheck.CheckStatus status, OffsetDateTime cutoff);
}
