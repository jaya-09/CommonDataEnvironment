package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "kpi_daily_snapshot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KpiDailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "snapshot_id", updatable = false, nullable = false)
    private UUID snapshotId;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "total_products") private int totalProducts;
    @Column(name = "versions_released") private int versionsReleased;
    @Column(name = "versions_on_hold") private int versionsOnHold;
    @Column(name = "phase_transitions") private int phaseTransitions;
    @Column(name = "ncrs_opened") private int ncrsOpened;
    @Column(name = "ncrs_closed") private int ncrsClosed;
    @Column(name = "critical_ncrs_open") private int criticalNcrsOpen;
    @Column(name = "capas_open") private int capacOpen;
    @Column(name = "avg_ncr_resolution_days", precision = 5, scale = 1) private BigDecimal avgNcrResolutionDays;
    @Column(name = "certs_issued") private int certsIssued;
    @Column(name = "enrollments_triggered") private int enrollmentsTriggered;
    @Column(name = "phase_gates_blocked") private int phaseGatesBlocked;
    @Column(name = "phase_gates_passed") private int phaseGatesPassed;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
