package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NcrChainSummary {
    private UUID ncrId;
    private String ncrNumber;
    private String ncrSeverity;
    private String ncrTitle;
    private String productCode;
    private UUID capaId;
    private String capaNumber;
    private String capaStatus;
    private boolean trainingTriggered;
    private String courseCode;
    private int enrolledUserCount;
    private int certifiedCount;
    private String resolutionStatus;
    private OffsetDateTime ncrRaisedAt;
    private OffsetDateTime fullyResolvedAt;
    private long daysSinceRaised;
}
