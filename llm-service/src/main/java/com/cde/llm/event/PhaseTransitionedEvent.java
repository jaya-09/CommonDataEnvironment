package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseTransitionedEvent {
    private UUID versionId;
    private String productCode;
    private String fromPhase;
    private String toPhase;
    private UUID triggeredBy;
}
