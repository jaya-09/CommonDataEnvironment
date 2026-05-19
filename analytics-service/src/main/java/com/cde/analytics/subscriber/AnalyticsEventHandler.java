package com.cde.analytics.subscriber;

import com.cde.analytics.entity.*;
import com.cde.analytics.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Central event handler for the Analytics service.
 *
 * Every event from PLM, QLM, and LLM flows through here.
 * Each handler updates ONE OR MORE of the cross-service read models:
 *   - ProductHealthSnapshot
 *   - NcrResolutionChain
 *   - PhaseGateHistory
 *   - RiskMatrixEntry
 *   - EventTimeline
 *
 * All writes are idempotent — duplicate events are ignored via processed_events table.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsEventHandler {

    private final ProductHealthRepository healthRepo;
    private final NcrResolutionChainRepository chainRepo;
    private final PhaseGateHistoryRepository gateRepo;
    private final RiskMatrixRepository riskRepo;
    private final EventTimelineRepository timelineRepo;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper objectMapper;

    // ════════════════════════════════════════════════════════
    // ENTRY POINT — routes by event type
    // ════════════════════════════════════════════════════════

    @Transactional
    public void handle(EventEnvelope envelope) {
        // Idempotency check
        if (processedRepo.existsByEventId(envelope.getEventId())) {
            log.debug("Already processed event {}, skipping", envelope.getEventId());
            return;
        }

        log.info("Analytics processing [type={}, eventId={}]", envelope.getType(), envelope.getEventId());

        try {
            switch (envelope.getType()) {
                // PLM
                case "cde.plm.version.released"         -> handleVersionReleased(envelope);
                case "cde.plm.phase.transitioned"        -> handlePhaseTransitioned(envelope);
                case "cde.plm.version.status.changed"    -> handleVersionStatusChanged(envelope);

                // QLM
                case "cde.qlm.ncr.raised"               -> handleNcrRaised(envelope);
                case "cde.qlm.ncr.closed"               -> handleNcrClosed(envelope);
                case "cde.qlm.capa.raised"              -> handleCapaRaised(envelope);
                case "cde.qlm.capa.closed"              -> handleCapaClosed(envelope);
                case "cde.qlm.risk.registered"          -> handleRiskRegistered(envelope);

                // LLM
                case "cde.llm.certification.granted"    -> handleCertGranted(envelope);
                case "cde.llm.enrollment.triggered"     -> handleEnrollmentTriggered(envelope);

                default -> log.debug("Analytics: no handler for {}", envelope.getType());
            }

            processedRepo.save(ProcessedEvent.builder()
                    .eventId(envelope.getEventId())
                    .eventType(envelope.getType())
                    .processedAt(OffsetDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Analytics failed processing event [type={}, eventId={}]: {}",
                    envelope.getType(), envelope.getEventId(), e.getMessage(), e);
            throw e; // let Pub/Sub retry
        }
    }

    // ════════════════════════════════════════════════════════
    // PLM HANDLERS
    // ════════════════════════════════════════════════════════

    private void handleVersionReleased(EventEnvelope env) {
        var e = convert(env.getPayload(), VersionReleasedEvent.class);

        // Upsert product health snapshot
        ProductHealthSnapshot snap = healthRepo.findById(e.getVersionId())
                .orElse(ProductHealthSnapshot.builder()
                        .versionId(e.getVersionId())
                        .productId(e.getProductId())
                        .productCode(e.getProductCode())
                        .productName(e.getProductName() != null ? e.getProductName() : e.getProductCode())
                        .versionNumber(e.getVersionNumber())
                        .build());

        snap.setVersionStatus("RELEASED");
        snap.setLastPlmEventAt(OffsetDateTime.now());
        snap.recomputeStatus();
        healthRepo.save(snap);

        addTimeline(env, "PLM", "VERSION", e.getVersionId(), e.getProductCode(),
                "Version v" + e.getVersionNumber() + " of " + e.getProductCode() + " released",
                "INFO", e.getProductCode(), e.getVersionId());
    }

    private void handlePhaseTransitioned(EventEnvelope env) {
        var e = convert(env.getPayload(), PhaseTransitionedEvent.class);

        // Update product health snapshot
        ProductHealthSnapshot snap = healthRepo.findById(e.getVersionId())
                .orElse(ProductHealthSnapshot.builder()
                        .versionId(e.getVersionId())
                        .productId(e.getProductId())
                        .productCode(e.getProductCode())
                        .productName(e.getProductName() != null ? e.getProductName() : e.getProductCode())
                        .versionNumber(e.getVersionNumber())
                        .versionStatus("IN_PROGRESS")
                        .build());

        snap.setCurrentPhase(e.getToPhase());
        if (e.getToPhaseSequence() != null) snap.setPhaseSequenceOrder(e.getToPhaseSequence());
        snap.setLastPlmEventAt(OffsetDateTime.now());
        snap.recomputeStatus();
        healthRepo.save(snap);

        // Record phase gate history
        String result = (e.getBlockers() != null && !e.getBlockers().isEmpty()) ? "BLOCK" : "PASS";
        gateRepo.save(PhaseGateHistory.builder()
                .versionId(e.getVersionId())
                .productCode(e.getProductCode())
                .versionNumber(e.getVersionNumber())
                .fromPhase(e.getFromPhase())
                .toPhase(e.getToPhase())
                .gateResult(result)
                .blockedBy(e.getBlockers())
                .triggeredByUser(e.getTriggeredBy())
                .correlationId(env.getCorrelationId())
                .occurredAt(env.getTimestamp() != null ? env.getTimestamp() : OffsetDateTime.now())
                .build());

        String severity = "BLOCK".equals(result) ? "WARNING" : "INFO";
        String desc = "BLOCK".equals(result)
                ? "Phase gate BLOCKED: " + e.getFromPhase() + " → " + e.getToPhase() + " for " + e.getProductCode()
                : "Phase advanced: " + e.getFromPhase() + " → " + e.getToPhase() + " for " + e.getProductCode();

        addTimeline(env, "PLM", "VERSION", e.getVersionId(), e.getProductCode(),
                desc, severity, e.getProductCode(), e.getVersionId());
    }

    private void handleVersionStatusChanged(EventEnvelope env) {
        var e = convert(env.getPayload(), VersionStatusChangedEvent.class);

        healthRepo.findById(e.getVersionId()).ifPresent(snap -> {
            snap.setVersionStatus(e.getNewStatus());
            snap.setLastPlmEventAt(OffsetDateTime.now());
            snap.recomputeStatus();
            healthRepo.save(snap);
        });

        String severity = "HOLD".equals(e.getNewStatus()) ? "WARNING" : "INFO";
        addTimeline(env, "PLM", "VERSION", e.getVersionId(), e.getProductCode(),
                "Version status changed to " + e.getNewStatus() + " for " + e.getProductCode(),
                severity, e.getProductCode(), e.getVersionId());
    }

    // ════════════════════════════════════════════════════════
    // QLM HANDLERS
    // ════════════════════════════════════════════════════════

    private void handleNcrRaised(EventEnvelope env) {
        var e = convert(env.getPayload(), NcrRaisedEvent.class);

        // Increment NCR counts on product health snapshot
        if (e.getProductVersionId() != null) {
            healthRepo.findById(e.getProductVersionId()).ifPresent(snap -> {
                snap.setOpenNcrCount(snap.getOpenNcrCount() + 1);
                if ("CRITICAL".equals(e.getSeverity())) {
                    snap.setCriticalNcrCount(snap.getCriticalNcrCount() + 1);
                }
                snap.setLastQlmEventAt(OffsetDateTime.now());
                snap.recomputeStatus();
                healthRepo.save(snap);
            });
        }

        // Start NCR resolution chain
        if (!chainRepo.findByNcrId(e.getNcrId()).isPresent()) {
            chainRepo.save(NcrResolutionChain.builder()
                    .ncrId(e.getNcrId())
                    .ncrNumber(e.getNcrNumber())
                    .ncrSeverity(e.getSeverity())
                    .ncrTitle(e.getTitle())
                    .productVersionId(e.getProductVersionId())
                    .productCode(e.getProductCode())
                    .courseCode(e.getCourseCodeRequired())
                    .ncrRaisedAt(e.getRaisedAt() != null ? e.getRaisedAt() : OffsetDateTime.now())
                    .resolutionStatus("NCR_OPEN")
                    .build());
        }

        // Add to risk matrix if CRITICAL or MAJOR
        if ("CRITICAL".equals(e.getSeverity()) || "MAJOR".equals(e.getSeverity())) {
            int likelihood = "CRITICAL".equals(e.getSeverity()) ? 5 : 3;
            int impact = "CRITICAL".equals(e.getSeverity()) ? 5 : 3;
            int score = likelihood * impact;
            riskRepo.save(RiskMatrixEntry.builder()
                    .sourceService("QLM_NCR")
                    .sourceId(e.getNcrId())
                    .productVersionId(e.getProductVersionId())
                    .productCode(e.getProductCode())
                    .riskCategory("Quality")
                    .riskTitle("NCR: " + (e.getTitle() != null ? e.getTitle() : e.getNcrNumber()))
                    .likelihood(likelihood)
                    .impact(impact)
                    .riskScore(score)
                    .riskLevel(RiskMatrixEntry.computeLevel(score))
                    .linkedNcrId(e.getNcrId())
                    .detectedAt(OffsetDateTime.now())
                    .build());
        }

        String severity = "CRITICAL".equals(e.getSeverity()) ? "CRITICAL" : "WARNING";
        addTimeline(env, "QLM", "NCR", e.getNcrId(), e.getNcrNumber(),
                "NCR raised [" + e.getSeverity() + "]: " + e.getNcrNumber()
                        + (e.getProductCode() != null ? " on " + e.getProductCode() : ""),
                severity, e.getProductCode(), e.getProductVersionId());
    }

    private void handleNcrClosed(EventEnvelope env) {
        var e = convert(env.getPayload(), NcrClosedEvent.class);

        // Decrement counts
        if (e.getProductVersionId() != null) {
            healthRepo.findById(e.getProductVersionId()).ifPresent(snap -> {
                snap.setOpenNcrCount(Math.max(0, snap.getOpenNcrCount() - 1));
                snap.setLastQlmEventAt(OffsetDateTime.now());
                snap.recomputeStatus();
                healthRepo.save(snap);
            });
        }

        // Mark risk matrix entry resolved
        riskRepo.findBySourceServiceAndSourceId("QLM_NCR", e.getNcrId()).ifPresent(r -> {
            r.setResolved(true);
            r.setResolvedAt(OffsetDateTime.now());
            riskRepo.save(r);
        });

        addTimeline(env, "QLM", "NCR", e.getNcrId(), e.getNcrNumber(),
                "NCR closed: " + e.getNcrNumber(), "INFO", e.getProductCode(), e.getProductVersionId());
    }

    private void handleCapaRaised(EventEnvelope env) {
        var e = convert(env.getPayload(), CapaRaisedEvent.class);

        chainRepo.findByNcrId(e.getNcrId()).ifPresent(chain -> {
            chain.setCapaId(e.getCapaId());
            chain.setCapaNumber(e.getCapaNumber());
            chain.setCapaStatus("OPEN");
            chain.recomputeResolutionStatus();
            chainRepo.save(chain);
        });

        addTimeline(env, "QLM", "CAPA", e.getCapaId(), e.getCapaNumber(),
                "CAPA raised: " + e.getCapaNumber() + " for NCR", "INFO", null, e.getProductVersionId());
    }

    private void handleCapaClosed(EventEnvelope env) {
        var e = convert(env.getPayload(), CapaClosedEvent.class);

        if (e.getProductVersionId() != null) {
            healthRepo.findById(e.getProductVersionId()).ifPresent(snap -> {
                snap.setOpenCapaCount(Math.max(0, snap.getOpenCapaCount() - 1));
                snap.setLastQlmEventAt(OffsetDateTime.now());
                snap.recomputeStatus();
                healthRepo.save(snap);
            });
        }

        chainRepo.findByNcrId(e.getNcrId()).ifPresent(chain -> {
            chain.setCapaStatus("CLOSED");
            chain.setCapaClosedAt(e.getClosedAt() != null ? e.getClosedAt() : OffsetDateTime.now());

            // If training is also complete, mark fully resolved
            if (chain.getCertifiedCount() >= chain.getEnrolledUserCount()
                    && chain.getEnrolledUserCount() > 0) {
                chain.setFullyResolvedAt(OffsetDateTime.now());
            }
            chain.recomputeResolutionStatus();
            chainRepo.save(chain);
        });

        addTimeline(env, "QLM", "CAPA", e.getCapaId(), e.getCapaNumber(),
                "CAPA closed: " + e.getCapaNumber(), "INFO", e.getProductCode(), e.getProductVersionId());
    }

    private void handleRiskRegistered(EventEnvelope env) {
        var e = convert(env.getPayload(), RiskRegisteredEvent.class);
        int score = e.getRiskScore() != null ? e.getRiskScore()
                : (e.getLikelihood() != null && e.getImpact() != null ? e.getLikelihood() * e.getImpact() : 0);

        riskRepo.save(RiskMatrixEntry.builder()
                .sourceService("QLM_RISK")
                .sourceId(e.getRiskId())
                .productVersionId(e.getProductVersionId())
                .productCode(e.getProductCode())
                .riskCategory(e.getCategory())
                .riskTitle(e.getTitle())
                .likelihood(e.getLikelihood())
                .impact(e.getImpact())
                .riskScore(score)
                .riskLevel(RiskMatrixEntry.computeLevel(score))
                .detectedAt(OffsetDateTime.now())
                .build());

        // Update high risk count on snapshot
        if (score >= 12 && e.getProductVersionId() != null) {
            healthRepo.findById(e.getProductVersionId()).ifPresent(snap -> {
                snap.setHighRiskCount(snap.getHighRiskCount() + 1);
                snap.setLastQlmEventAt(OffsetDateTime.now());
                snap.recomputeStatus();
                healthRepo.save(snap);
            });
        }
    }

    // ════════════════════════════════════════════════════════
    // LLM HANDLERS
    // ════════════════════════════════════════════════════════

    private void handleCertGranted(EventEnvelope env) {
        var e = convert(env.getPayload(), CertificationGrantedEvent.class);

        // If cert is for a phase-mandatory course, update all versions in that phase
        if (e.getMandatoryForPhase() != null) {
            healthRepo.findByCurrentPhaseOrderByProductCode(e.getMandatoryForPhase())
                    .forEach(snap -> {
                        // Decrement uncertified count (min 0)
                        snap.setUncertifiedUserCount(Math.max(0, snap.getUncertifiedUserCount() - 1));
                        snap.setPhaseCertReady(snap.getUncertifiedUserCount() == 0);
                        snap.setLastLlmEventAt(OffsetDateTime.now());
                        snap.recomputeStatus();
                        healthRepo.save(snap);
                    });
        }

        // Progress certification count on NCR chains triggered by this course
        chainRepo.findByCourseCodeAndTrainingTriggeredTrue(e.getCourseCode()).forEach(chain -> {
            chain.setCertifiedCount(chain.getCertifiedCount() + 1);
            if (chain.getCertifiedCount() >= chain.getEnrolledUserCount()
                    && "CLOSED".equals(chain.getCapaStatus())) {
                chain.setFullyResolvedAt(OffsetDateTime.now());
            }
            chain.recomputeResolutionStatus();
            chainRepo.save(chain);
        });

        addTimeline(env, "LLM", "CERTIFICATION", e.getCertId(), e.getEmployeeId(),
                "Certification issued: " + e.getCourseCode() + " for " + e.getEmployeeId()
                        + (e.getMandatoryForPhase() != null ? " (phase: " + e.getMandatoryForPhase() + ")" : ""),
                "INFO", null, null);
    }

    private void handleEnrollmentTriggered(EventEnvelope env) {
        var e = convert(env.getPayload(), EnrollmentTriggeredEvent.class);

        // If triggered by NCR, increment chain enrolled count
        if ("NCR".equals(e.getTriggerRefType()) && e.getTriggerRefId() != null) {
            chainRepo.findByNcrId(e.getTriggerRefId()).ifPresent(chain -> {
                chain.setTrainingTriggered(true);
                chain.setEnrolledUserCount(chain.getEnrolledUserCount() + 1);
                if (e.getCourseCode() != null) chain.setCourseCode(e.getCourseCode());
                chain.recomputeResolutionStatus();
                chainRepo.save(chain);
            });
        }

        addTimeline(env, "LLM", "ENROLLMENT", e.getEnrollmentId(), e.getCourseCode(),
                "Training enrolled: " + e.getCourseCode() + " triggered by " + e.getTriggerRefType(),
                "INFO", null, null);
    }

    // ════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════

    private void addTimeline(EventEnvelope env, String service, String entityType,
                              UUID entityId, String entityRef, String description,
                              String severity, String productCode, UUID versionId) {
        if (timelineRepo.existsByEventId(env.getEventId())) return;

        timelineRepo.save(EventTimeline.builder()
                .eventId(env.getEventId())
                .eventType(env.getType())
                .sourceService(service)
                .entityType(entityType)
                .entityId(entityId)
                .entityRef(entityRef)
                .description(description)
                .severity(severity)
                .productCode(productCode)
                .versionId(versionId)
                .correlationId(env.getCorrelationId())
                .occurredAt(env.getTimestamp() != null ? env.getTimestamp() : OffsetDateTime.now())
                .build());
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
