package com.cde.plm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseTransitionedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    private String fromPhase;
    private String toPhase;
    private UUID triggeredBy;
}
