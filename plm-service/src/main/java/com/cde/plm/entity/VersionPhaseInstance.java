package com.cde.plm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records each phase a product version has passed through.
 * One row is created per phase per version as it advances through the lifecycle.
 *
 * e.g.  version "v1.0"  →  Ideation (COMPLETED)  →  Design (IN_PROGRESS)  →  ...
 */
@Entity
@Table(name = "version_phase_instance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionPhaseInstance {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "instance_id", updatable = false, nullable = false)
    private UUID instanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ProductVersion version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "phase_id", nullable = false)
    private LifecyclePhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhaseInstanceStatus status = PhaseInstanceStatus.PENDING;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** Who triggered the phase transition (advance or skip). */
    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "notes")
    private String notes;

    public enum PhaseInstanceStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        SKIPPED
    }
}
