package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ncr_resolution_chain")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NcrResolutionChain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chain_id", updatable = false, nullable = false)
    private UUID chainId;

    @Column(name = "ncr_id", nullable = false, unique = true)
    private UUID ncrId;

    @Column(name = "ncr_number", nullable = false, length = 50)
    private String ncrNumber;

    @Column(name = "ncr_severity", nullable = false, length = 10)
    private String ncrSeverity;

    @Column(name = "ncr_title", length = 300)
    private String ncrTitle;

    @Column(name = "product_version_id")
    private UUID productVersionId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "capa_id")
    private UUID capaId;

    @Column(name = "capa_number", length = 50)
    private String capaNumber;

    @Column(name = "capa_status", length = 20)
    private String capaStatus;

    @Column(name = "capa_closed_at")
    private OffsetDateTime capaClosedAt;

    @Column(name = "training_triggered", nullable = false)
    private boolean trainingTriggered = false;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "enrolled_user_count", nullable = false)
    private int enrolledUserCount = 0;

    @Column(name = "certified_count", nullable = false)
    private int certifiedCount = 0;

    @Column(name = "resolution_status", nullable = false, length = 30)
    private String resolutionStatus = "NCR_OPEN";

    @Column(name = "ncr_raised_at", nullable = false)
    private OffsetDateTime ncrRaisedAt;

    @Column(name = "fully_resolved_at")
    private OffsetDateTime fullyResolvedAt;

    @Column(name = "chain_updated_at", nullable = false)
    private OffsetDateTime chainUpdatedAt = OffsetDateTime.now();

    public void recomputeResolutionStatus() {
        if (fullyResolvedAt != null) {
            this.resolutionStatus = "FULLY_RESOLVED";
        } else if (certifiedCount > 0 && certifiedCount >= enrolledUserCount && enrolledUserCount > 0) {
            this.resolutionStatus = "TRAINING_COMPLETE";
        } else if (trainingTriggered) {
            this.resolutionStatus = "TRAINING_TRIGGERED";
        } else if ("CLOSED".equals(capaStatus)) {
            this.resolutionStatus = "CAPA_CLOSED";
        } else if (capaId != null) {
            this.resolutionStatus = "CAPA_IN_PROGRESS";
        } else {
            this.resolutionStatus = "NCR_OPEN";
        }
        this.chainUpdatedAt = OffsetDateTime.now();
    }
}
