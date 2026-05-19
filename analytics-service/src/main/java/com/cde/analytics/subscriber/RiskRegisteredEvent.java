package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class RiskRegisteredEvent {
    private UUID riskId;
    private UUID productVersionId;
    private String productCode;
    private String title;
    private String category;
    private Integer likelihood;
    private Integer impact;
    private Integer riskScore;
}
