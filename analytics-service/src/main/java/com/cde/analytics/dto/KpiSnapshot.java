package com.cde.analytics.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiSnapshot {
    private LocalDate date;
    private int totalProducts;
    private int versionsReleased;
    private int versionsOnHold;
    private int phaseTransitions;
    private int ncrsOpened;
    private int ncrsClosed;
    private int criticalNcrsOpen;
    private int capacOpen;
    private BigDecimal avgNcrResolutionDays;
    private int certsIssued;
    private int enrollmentsTriggered;
    private int phaseGatesBlocked;
    private int phaseGatesPassed;
}
