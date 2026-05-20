package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

/**
 * Published by PLM when a phase gate check is needed.
 * Both QLM and LLM subscribe and respond with PhaseGateCheckResultEvent.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseGateCheckRequestedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    private String targetPhase;
    private UUID requestedBy;
    /** Same correlationId used to match result events back to this request */
    private String gateCorrelationId;
}
