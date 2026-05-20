package com.cde.plm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks an in-flight phase gate check that was dispatched via Pub/Sub.
 * Stores partial results from QLM and LLM until both arrive,
 * then the final advance-or-block decision is made.
 */
@Entity
@Table(name = "phase_gate_pending_checks")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseGatePendingCheck {

    @Id
    @Column(name = "gate_correlation_id")
    private String gateCorrelationId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "target_phase", nullable = false)
    private String targetPhase;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "correlation_id")
    private String correlationId;

    // ── QLM result ─────────────────────────────
    @Column(name = "qlm_result_received")
    private boolean qlmResultReceived;

    @Column(name = "qlm_passed")
    private Boolean qlmPassed;

    @Column(name = "qlm_block_reason")
    private String qlmBlockReason;

    @Column(name = "open_critical_ncr_count")
    private Long openCriticalNcrCount;

    // ── LLM result ─────────────────────────────
    @Column(name = "llm_result_received")
    private boolean llmResultReceived;

    @Column(name = "llm_passed")
    private Boolean llmPassed;

    @Column(name = "llm_block_reason")
    private String llmBlockReason;

    @Column(name = "uncertified_count")
    private Integer uncertifiedCount;

    // ── Final status ───────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CheckStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public enum CheckStatus {
        PENDING,    // waiting for both QLM and LLM results
        PASSED,     // both checks passed — phase was advanced
        BLOCKED,    // one or both checks failed
        TIMEOUT     // results never arrived (future use)
    }

    public boolean bothResultsReceived() {
        return qlmResultReceived && llmResultReceived;
    }
}
