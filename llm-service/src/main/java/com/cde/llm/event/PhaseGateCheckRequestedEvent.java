package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseGateCheckRequestedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    private String targetPhase;
    private UUID requestedBy;
    private String gateCorrelationId;
}
