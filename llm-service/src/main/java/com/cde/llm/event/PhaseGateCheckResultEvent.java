package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseGateCheckResultEvent {
    private String gateCorrelationId;
    private UUID versionId;
    private String targetPhase;
    private String checkerService;
    private boolean passed;
    private String blockReason;
    private Long openCriticalNcrCount;
    private Integer uncertifiedCount;
}
