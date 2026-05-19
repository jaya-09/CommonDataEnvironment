package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_matrix_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskMatrixEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", updatable = false, nullable = false)
    private UUID entryId;

    @Column(name = "source_service", nullable = false, length = 15)
    private String sourceService;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "product_version_id")
    private UUID productVersionId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "risk_category", length = 50)
    private String riskCategory;

    @Column(name = "risk_title", nullable = false, length = 300)
    private String riskTitle;

    @Column(name = "likelihood")
    private Integer likelihood;

    @Column(name = "impact")
    private Integer impact;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "linked_ncr_id")
    private UUID linkedNcrId;

    @Column(name = "linked_capa_id")
    private UUID linkedCapaId;

    @Column(name = "linked_phase", length = 50)
    private String linkedPhase;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "entry_updated_at", nullable = false)
    private OffsetDateTime entryUpdatedAt = OffsetDateTime.now();

    public static String computeLevel(int score) {
        if (score >= 20) return "CRITICAL";
        if (score >= 12) return "HIGH";
        if (score >= 6)  return "MEDIUM";
        return "LOW";
    }
}
