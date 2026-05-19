package com.cde.plm.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LifecyclePhaseResponse {
    private UUID phaseId;
    private String phaseName;
    private String displayName;
    private Integer sequenceOrder;
    private boolean active;
}
