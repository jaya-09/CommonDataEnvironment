package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "phase_gate_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhaseGateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "gate_id", updatable = false, nullable = false)
    private UUID gateId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "version_number", nullable = false, length = 30)
    private String versionNumber;

    @Column(name = "from_phase", length = 50)
    private String fromPhase;

    @Column(name = "to_phase", nullable = false, length = 50)
    private String toPhase;

    @Column(name = "gate_result", nullable = false, length = 10)
    private String gateResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocked_by", columnDefinition = "jsonb")
    private Map<String, String> blockedBy;

    @Column(name = "triggered_by_user")
    private UUID triggeredByUser;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
