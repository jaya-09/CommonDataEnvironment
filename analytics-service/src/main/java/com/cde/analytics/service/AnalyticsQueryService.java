package com.cde.analytics.service;

import com.cde.analytics.dto.*;
import com.cde.analytics.entity.*;
import com.cde.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final ProductHealthRepository healthRepo;
    private final NcrResolutionChainRepository chainRepo;
    private final PhaseGateHistoryRepository gateRepo;
    private final RiskMatrixRepository riskRepo;
    private final EventTimelineRepository timelineRepo;
    private final KpiDailySnapshotRepository kpiRepo;

    // ── DASHBOARD ────────────────────────────────────────────

    public DashboardSummary getDashboard() {
        List<ProductHealthSnapshot> allSnaps = healthRepo.findAllOrderByRisk();

        long blocked = allSnaps.stream().filter(s -> "BLOCKED".equals(s.getOverallStatus())).count();
        long atRisk  = allSnaps.stream().filter(s -> "AT_RISK".equals(s.getOverallStatus())).count();
        long healthy = allSnaps.stream().filter(s -> "HEALTHY".equals(s.getOverallStatus())).count();

        int criticalNcrs = allSnaps.stream().mapToInt(ProductHealthSnapshot::getCriticalNcrCount).sum();
        int openNcrs     = allSnaps.stream().mapToInt(ProductHealthSnapshot::getOpenNcrCount).sum();
        int openCapas    = allSnaps.stream().mapToInt(ProductHealthSnapshot::getOpenCapaCount).sum();

        long critRisks = riskRepo.findByRiskLevelAndResolvedFalseOrderByDetectedAtDesc("CRITICAL").size();
        long highRisks = riskRepo.findByRiskLevelAndResolvedFalseOrderByDetectedAtDesc("HIGH").size();

        OffsetDateTime todayStart = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        long gatesPassed  = gateRepo.countByResultSince("PASS", todayStart);
        long gatesBlocked = gateRepo.countByResultSince("BLOCK", todayStart);

        List<NcrResolutionChain> openChains = chainRepo.findAllOpenOrderBySeverity();

        List<TrendPoint> trend = kpiRepo.findTop30ByOrderBySnapshotDateDesc().stream()
                .map(k -> TrendPoint.builder()
                        .date(k.getSnapshotDate())
                        .passCount(k.getPhaseGatesPassed())
                        .blockCount(k.getPhaseGatesBlocked())
                        .ncrsOpened(k.getNcrsOpened())
                        .ncrsClosed(k.getNcrsClosed())
                        .certsIssued(k.getCertsIssued())
                        .build())
                .collect(Collectors.toList());

        return DashboardSummary.builder()
                .totalProducts((int) allSnaps.stream().map(ProductHealthSnapshot::getProductId).distinct().count())
                .blockedVersions((int) blocked)
                .atRiskVersions((int) atRisk)
                .healthyVersions((int) healthy)
                .openCriticalNcrs(criticalNcrs)
                .openNcrs(openNcrs)
                .openCapas(openCapas)
                .criticalRisks((int) critRisks)
                .highRisks((int) highRisks)
                .phaseGatesPassedToday((int) gatesPassed)
                .phaseGatesBlockedToday((int) gatesBlocked)
                .topRiskProducts(allSnaps.stream().limit(5).map(this::toHealthDto).toList())
                .recentEvents(timelineRepo.findTop50ByOrderByOccurredAtDesc().stream()
                        .limit(10).map(this::toTimelineDto).toList())
                .phaseGateTrend(trend)
                .openNcrChains(openChains.stream().map(this::toChainDto).toList())
                .build();
    }

    // ── PRODUCT HEALTH ───────────────────────────────────────

    public List<ProductHealthDto> getAllProductHealth() {
        return healthRepo.findAllOrderByRisk().stream()
                .map(this::toHealthDto).toList();
    }

    public ProductHealthDto getProductHealth(UUID versionId) {
        return healthRepo.findById(versionId)
                .map(this::toHealthDto)
                .orElseThrow(() -> new java.util.NoSuchElementException("No health data for version: " + versionId));
    }

    // ── NCR CHAINS ───────────────────────────────────────────

    public List<NcrChainSummary> getNcrChains(String status, String productCode) {
        List<NcrResolutionChain> chains;
        if (productCode != null) {
            chains = chainRepo.findByProductCodeOrderByNcrRaisedAtDesc(productCode);
        } else if (status != null) {
            chains = chainRepo.findByResolutionStatusOrderByNcrRaisedAtDesc(status);
        } else {
            chains = chainRepo.findAllOpenOrderBySeverity();
        }
        return chains.stream().map(this::toChainDto).toList();
    }

    // ── PHASE GATE HISTORY ───────────────────────────────────

    public List<PhaseGateDto> getPhaseGateHistory(UUID versionId, String result) {
        List<PhaseGateHistory> gates;
        if (versionId != null) {
            gates = gateRepo.findByVersionIdOrderByOccurredAtDesc(versionId);
        } else if (result != null) {
            gates = gateRepo.findByGateResultOrderByOccurredAtDesc(result);
        } else {
            gates = gateRepo.findTop20ByOrderByOccurredAtDesc();
        }
        return gates.stream().map(g -> PhaseGateDto.builder()
                .gateId(g.getGateId())
                .versionId(g.getVersionId())
                .productCode(g.getProductCode())
                .versionNumber(g.getVersionNumber())
                .fromPhase(g.getFromPhase())
                .toPhase(g.getToPhase())
                .gateResult(g.getGateResult())
                .blockedBy(g.getBlockedBy())
                .correlationId(g.getCorrelationId())
                .occurredAt(g.getOccurredAt())
                .build()).toList();
    }

    // ── RISK MATRIX ──────────────────────────────────────────

    public List<RiskMatrixDto> getRiskMatrix(String productCode) {
        List<RiskMatrixEntry> risks = productCode != null
                ? riskRepo.findByProductCodeAndResolvedFalseOrderByRiskScoreDesc(productCode)
                : riskRepo.findByResolvedFalseOrderByRiskScoreDesc();

        return risks.stream().map(r -> RiskMatrixDto.builder()
                .entryId(r.getEntryId())
                .sourceService(r.getSourceService())
                .productCode(r.getProductCode())
                .riskCategory(r.getRiskCategory())
                .riskTitle(r.getRiskTitle())
                .likelihood(r.getLikelihood())
                .impact(r.getImpact())
                .riskScore(r.getRiskScore())
                .riskLevel(r.getRiskLevel())
                .linkedNcrId(r.getLinkedNcrId())
                .linkedPhase(r.getLinkedPhase())
                .resolved(r.isResolved())
                .detectedAt(r.getDetectedAt())
                .heatmapRow(r.getLikelihood() != null ? r.getLikelihood() : 0)
                .heatmapCol(r.getImpact() != null ? r.getImpact() : 0)
                .build()).toList();
    }

    // ── EVENT TIMELINE ───────────────────────────────────────

    public List<TimelineEventDto> getTimeline(String productCode, UUID versionId, String service) {
        List<EventTimeline> events;
        if (productCode != null) {
            events = timelineRepo.findByProductCodeOrderByOccurredAtDesc(productCode);
        } else if (versionId != null) {
            events = timelineRepo.findByVersionIdOrderByOccurredAtDesc(versionId);
        } else if (service != null) {
            events = timelineRepo.findBySourceServiceOrderByOccurredAtDesc(service);
        } else {
            events = timelineRepo.findTop50ByOrderByOccurredAtDesc();
        }
        return events.stream().map(this::toTimelineDto).toList();
    }

    // ── MAPPERS ──────────────────────────────────────────────

    private ProductHealthDto toHealthDto(ProductHealthSnapshot s) {
        return ProductHealthDto.builder()
                .versionId(s.getVersionId())
                .productId(s.getProductId())
                .productCode(s.getProductCode())
                .productName(s.getProductName())
                .versionNumber(s.getVersionNumber())
                .versionStatus(s.getVersionStatus())
                .currentPhase(s.getCurrentPhase())
                .phaseSequenceOrder(s.getPhaseSequenceOrder())
                .openNcrCount(s.getOpenNcrCount())
                .criticalNcrCount(s.getCriticalNcrCount())
                .openCapaCount(s.getOpenCapaCount())
                .highRiskCount(s.getHighRiskCount())
                .uncertifiedUserCount(s.getUncertifiedUserCount())
                .phaseCertReady(s.isPhaseCertReady())
                .overallStatus(s.getOverallStatus())
                .lastPlmEventAt(s.getLastPlmEventAt())
                .lastQlmEventAt(s.getLastQlmEventAt())
                .lastLlmEventAt(s.getLastLlmEventAt())
                .snapshotUpdatedAt(s.getSnapshotUpdatedAt())
                .build();
    }

    private NcrChainSummary toChainDto(NcrResolutionChain c) {
        long days = c.getNcrRaisedAt() != null
                ? ChronoUnit.DAYS.between(c.getNcrRaisedAt().toLocalDate(), LocalDate.now()) : 0;
        return NcrChainSummary.builder()
                .ncrId(c.getNcrId())
                .ncrNumber(c.getNcrNumber())
                .ncrSeverity(c.getNcrSeverity())
                .ncrTitle(c.getNcrTitle())
                .productCode(c.getProductCode())
                .capaId(c.getCapaId())
                .capaNumber(c.getCapaNumber())
                .capaStatus(c.getCapaStatus())
                .trainingTriggered(c.isTrainingTriggered())
                .courseCode(c.getCourseCode())
                .enrolledUserCount(c.getEnrolledUserCount())
                .certifiedCount(c.getCertifiedCount())
                .resolutionStatus(c.getResolutionStatus())
                .ncrRaisedAt(c.getNcrRaisedAt())
                .fullyResolvedAt(c.getFullyResolvedAt())
                .daysSinceRaised(days)
                .build();
    }

    private TimelineEventDto toTimelineDto(EventTimeline e) {
        return TimelineEventDto.builder()
                .timelineId(e.getTimelineId())
                .eventType(e.getEventType())
                .sourceService(e.getSourceService())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .entityRef(e.getEntityRef())
                .description(e.getDescription())
                .severity(e.getSeverity())
                .productCode(e.getProductCode())
                .versionId(e.getVersionId())
                .correlationId(e.getCorrelationId())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
