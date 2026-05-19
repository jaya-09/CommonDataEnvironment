package com.cde.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummary {
    private int totalProducts;
    private int blockedVersions;
    private int atRiskVersions;
    private int healthyVersions;
    private int openCriticalNcrs;
    private int openNcrs;
    private int openCapas;
    private int criticalRisks;
    private int highRisks;
    private int phaseGatesPassedToday;
    private int phaseGatesBlockedToday;
    private int certsIssuedToday;
    private List<ProductHealthDto> topRiskProducts;
    private List<TimelineEventDto> recentEvents;
    private List<TrendPoint> phaseGateTrend;
    private List<NcrChainSummary> openNcrChains;
}
