package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

/**
 * Published by QLM or LLM after evaluating a phase gate check request.
 * PLM collects results from both and makes the final PASS/BLOCK decision.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseGateCheckResultEvent {
    /** Matches gateCorrelationId from PhaseGateCheckRequestedEvent */
    private String gateCorrelationId;
    private UUID versionId;
    private String targetPhase;
    /** "QLM" or "LLM" — identifies which service produced this result */
    private String checkerService;
    private boolean passed;
    private String blockReason;
    /** QLM specific */
    private Long openCriticalNcrCount;
    /** LLM specific */
    private Integer uncertifiedCount;
}
