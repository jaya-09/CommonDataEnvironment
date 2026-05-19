package com.cde.analytics.controller;

import com.cde.analytics.dto.*;
import com.cde.analytics.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Cross-service insights — read model built from all 3 service events")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    // ─────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Full dashboard — KPIs, top risk products, recent events, trend data from ALL services")
    public ResponseEntity<DashboardSummary> dashboard() {
        return ResponseEntity.ok(queryService.getDashboard());
    }

    // ─────────────────────────────────────────────────────────
    // PRODUCT HEALTH
    // Scenario: "Which products are blocked and why?"
    // ─────────────────────────────────────────────────────────

    @GetMapping("/product-health")
    @Operation(summary = "Cross-service health for all products — combines PLM phase + QLM NCRs + LLM cert readiness")
    public ResponseEntity<List<ProductHealthDto>> productHealth(
            @RequestParam(required = false) String status) {  // HEALTHY | AT_RISK | BLOCKED
        List<ProductHealthDto> result = queryService.getAllProductHealth();
        if (status != null) {
            result = result.stream().filter(p -> status.equals(p.getOverallStatus())).toList();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/product-health/{versionId}")
    @Operation(summary = "Detailed cross-service health for one product version")
    public ResponseEntity<ProductHealthDto> productHealthByVersion(@PathVariable UUID versionId) {
        return ResponseEntity.ok(queryService.getProductHealth(versionId));
    }

    // ─────────────────────────────────────────────────────────
    // NCR → CAPA → TRAINING CHAIN
    // Scenario: "For each quality issue, what's the full resolution progress?"
    // ─────────────────────────────────────────────────────────

    @GetMapping("/ncr-chains")
    @Operation(summary = "NCR→CAPA→Training resolution chains — full lifecycle of each quality issue")
    public ResponseEntity<List<NcrChainSummary>> ncrChains(
            @RequestParam(required = false) String status,      // NCR_OPEN | CAPA_IN_PROGRESS | etc.
            @RequestParam(required = false) String productCode) {
        return ResponseEntity.ok(queryService.getNcrChains(status, productCode));
    }

    // ─────────────────────────────────────────────────────────
    // PHASE GATE HISTORY
    // Scenario: "What has been blocking phase transitions?"
    // ─────────────────────────────────────────────────────────

    @GetMapping("/phase-gate-history")
    @Operation(summary = "Phase gate history — every gate attempt, what passed and what blocked")
    public ResponseEntity<List<PhaseGateDto>> phaseGateHistory(
            @RequestParam(required = false) UUID versionId,
            @RequestParam(required = false) String result) {  // PASS | BLOCK
        return ResponseEntity.ok(queryService.getPhaseGateHistory(versionId, result));
    }

    // ─────────────────────────────────────────────────────────
    // RISK MATRIX
    // Scenario: "Heatmap of all risks across NCRs + risk register + cert gaps"
    // ─────────────────────────────────────────────────────────

    @GetMapping("/risk-matrix")
    @Operation(summary = "Unified risk heatmap — NCRs + registered risks + certification gaps, scored and levelled")
    public ResponseEntity<List<RiskMatrixDto>> riskMatrix(
            @RequestParam(required = false) String productCode) {
        return ResponseEntity.ok(queryService.getRiskMatrix(productCode));
    }

    // ─────────────────────────────────────────────────────────
    // EVENT TIMELINE
    // Scenario: "Give me a chronological log of everything that happened to product X"
    // ─────────────────────────────────────────────────────────

    @GetMapping("/timeline")
    @Operation(summary = "Unified event timeline — chronological stream of events across PLM, QLM, LLM")
    public ResponseEntity<List<TimelineEventDto>> timeline(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) UUID versionId,
            @RequestParam(required = false) String service) {  // PLM | QLM | LLM
        return ResponseEntity.ok(queryService.getTimeline(productCode, versionId, service));
    }
}
