package com.cde.qlm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RiskRegisteredEvent {
    private UUID riskId;
    private UUID productVersionId;
    private String title;
    private String category;
    private Integer likelihood;
    private Integer impact;
    private Integer riskScore;
}
