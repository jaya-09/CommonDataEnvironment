package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.Map;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class PhaseTransitionedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    private String productName;
    private String fromPhase;
    private String toPhase;
    private Integer toPhaseSequence;
    private UUID triggeredBy;
    private String gateResult;
    private Map<String, String> blockers;
}
