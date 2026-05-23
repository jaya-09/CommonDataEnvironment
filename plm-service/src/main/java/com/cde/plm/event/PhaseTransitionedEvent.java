package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseTransitionedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    /** Denormalised so Analytics can display the product name without a lookup. */
    private String productName;
    private String fromPhase;
    private String toPhase;
    /** Phase sequence number — lets Analytics sort/chart phase progress numerically. */
    private Integer toPhaseSequence;
    private UUID triggeredBy;
}
