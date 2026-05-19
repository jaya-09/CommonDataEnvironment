package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_health_snapshot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductHealthSnapshot {

    @Id
    @Column(name = "version_id", updatable = false, nullable = false)
    private UUID versionId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "version_number", nullable = false, length = 30)
    private String versionNumber;

    @Column(name = "version_status", nullable = false, length = 30)
    private String versionStatus;

    @Column(name = "current_phase", length = 50)
    private String currentPhase;

    @Column(name = "phase_sequence_order")
    private Integer phaseSequenceOrder;

    @Column(name = "open_ncr_count", nullable = false)
    private int openNcrCount = 0;

    @Column(name = "critical_ncr_count", nullable = false)
    private int criticalNcrCount = 0;

    @Column(name = "open_capa_count", nullable = false)
    private int openCapaCount = 0;

    @Column(name = "high_risk_count", nullable = false)
    private int highRiskCount = 0;

    @Column(name = "uncertified_user_count", nullable = false)
    private int uncertifiedUserCount = 0;

    @Column(name = "phase_cert_ready", nullable = false)
    private boolean phaseCertReady = true;

    @Column(name = "overall_status", nullable = false, length = 20)
    private String overallStatus = "HEALTHY";

    @Column(name = "last_plm_event_at")
    private OffsetDateTime lastPlmEventAt;

    @Column(name = "last_qlm_event_at")
    private OffsetDateTime lastQlmEventAt;

    @Column(name = "last_llm_event_at")
    private OffsetDateTime lastLlmEventAt;

    @Column(name = "snapshot_updated_at", nullable = false)
    private OffsetDateTime snapshotUpdatedAt = OffsetDateTime.now();

    public void recomputeStatus() {
        if (criticalNcrCount > 0 || "HOLD".equals(versionStatus)) {
            this.overallStatus = "BLOCKED";
        } else if (openNcrCount > 0 || !phaseCertReady || highRiskCount > 0) {
            this.overallStatus = "AT_RISK";
        } else {
            this.overallStatus = "HEALTHY";
        }
        this.snapshotUpdatedAt = OffsetDateTime.now();
    }
}
