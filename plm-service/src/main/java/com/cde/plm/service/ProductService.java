package com.cde.plm.service;

import com.cde.plm.audit.AuditAction;
import com.cde.plm.audit.AuditEntityType;
import com.cde.plm.audit.AuditEventSource;
import com.cde.plm.dto.*;
import com.cde.plm.entity.*;
import com.cde.plm.event.*;
import com.cde.plm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductVersionRepository versionRepo;
    private final LifecyclePhaseRepository phaseRepo;
    private final ChangeRequestRepository crRepo;
    private final ApprovalWorkflowRepository workflowRepo;
    private final BomComponentRepository bomRepo;
    private final TechnicalDataPackageRepository tdpRepo;
    private final TraceabilityAuditLogRepository auditRepo;
    private final ProcessedEventRepository processedEventRepo;
    private final UserShadowRepository userShadowRepo;
    private final PhaseGatePendingCheckRepository pendingCheckRepo;
    private final PlmEventPublisher eventPublisher;

    // ── PRODUCTS ─────────────────────────────────────────────

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req, UUID userId) {
        if (productRepo.existsByProductCode(req.getProductCode()))
            throw new IllegalArgumentException("Product code already exists: " + req.getProductCode());

        Product product = productRepo.save(Product.builder()
                .productCode(req.getProductCode())
                .name(req.getName())
                .description(req.getDescription())
                .status(Product.ProductStatus.ACTIVE)
                .createdBy(userId)
                .approverUserId(req.getApproverUserId())
                .build());

        audit(AuditEntityType.PRODUCT, product.getProductId(), AuditAction.CREATED, null, product, userId, AuditEventSource.USER, null);
        return toProductResponse(product, false);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepo.findAll().stream().map(p -> toProductResponse(p, false)).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        Product p = findProduct(productId);
        ProductResponse res = toProductResponse(p, false);
        res.versions = versionRepo.findByProductId(productId).stream().map(v ->
                ProductVersionSummary.builder()
                        .versionId(v.getVersionId())
                        .versionNumber(v.getVersionNumber())
                        .description(v.getDescription())
                        .status(v.getStatus().name())
                        .currentPhase(v.getCurrentPhase() != null ? v.getCurrentPhase().getDisplayName() : null)
                        .build()).toList();
        return res;
    }

    // ── VERSIONS ─────────────────────────────────────────────

    @Transactional
    public ProductVersionResponse createVersion(UUID productId, CreateVersionRequest req, UUID userId) {
        Product product = findProduct(productId);
        if (versionRepo.existsByProductAndVersionNumber(product, req.getVersionNumber()))
            throw new IllegalArgumentException("Version already exists: " + req.getVersionNumber());

        LifecyclePhase firstPhase = phaseRepo.findBySequenceOrder(1)
                .orElseThrow(() -> new IllegalStateException("No lifecycle phases configured"));

        ProductVersion version = versionRepo.save(ProductVersion.builder()
                .product(product)
                .versionNumber(req.getVersionNumber())
                .description(req.getDescription())
                .status(ProductVersion.VersionStatus.DRAFT)
                .currentPhase(firstPhase)
                .createdBy(userId)
                .build());

        audit(AuditEntityType.VERSION, version.getVersionId(), AuditAction.CREATED, null, version, userId, AuditEventSource.USER, null);
        return toVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public ProductVersionResponse getVersion(UUID versionId) {
        return toVersionResponse(findVersion(versionId));
    }

    @Transactional(readOnly = true)
    public List<ProductVersionResponse> listVersions(UUID productId) {
        Product p = findProduct(productId);
        return versionRepo.findByProduct(p).stream().map(this::toVersionResponse).toList();
    }

    /**
     * Phase Gate Saga — now fully async via Pub/Sub.
     *
     * Instead of calling QLM and LLM synchronously over REST, PLM:
     *   1. Persists a PhaseGatePendingCheck row (status=PENDING)
     *   2. Publishes cde.plm.phase_gate.check_requested to Pub/Sub
     *   3. Returns immediately with canAdvance=false, status=PENDING
     *
     * QLM and LLM each receive the event, evaluate their check,
     * and publish cde.plm.phase_gate.check_result back to PLM.
     *
     * PLM's event handler (PlmEventController) collects both results;
     * once both arrive, it calls finalisePhaseGate() to advance or block.
     *
     * The caller should poll GET /versions/{id}/phase-gate-status to track progress.
     */
    @Transactional
    public PhaseGateResult advancePhase(UUID versionId, PhaseTransitionRequest req, UUID userId, String correlationId) {
        ProductVersion version = findVersion(versionId);
        LifecyclePhase targetPhase = phaseRepo.findByPhaseName(req.getTargetPhase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown phase: " + req.getTargetPhase()));

        // Check for a duplicate in-flight check for the same version+phase
        Optional<PhaseGatePendingCheck> existingPending = pendingCheckRepo
                .findByVersionIdAndTargetPhaseAndStatus(versionId, req.getTargetPhase(),
                        PhaseGatePendingCheck.CheckStatus.PENDING);
        if (existingPending.isPresent()) {
            log.info("Phase gate check already in progress for version {} → {}", versionId, req.getTargetPhase());
            return PhaseGateResult.builder()
                    .canAdvance(false)
                    .targetPhase(req.getTargetPhase())
                    .message("Phase gate check already in progress. Poll /versions/" + versionId + "/phase-gate-status for result.")
                    .build();
        }

        // Unique correlation ID for this gate check — used to match results back
        String gateCorrelationId = UUID.randomUUID().toString();

        // Persist pending check record
        pendingCheckRepo.save(PhaseGatePendingCheck.builder()
                .gateCorrelationId(gateCorrelationId)
                .versionId(versionId)
                .targetPhase(req.getTargetPhase())
                .requestedBy(userId)
                .correlationId(correlationId)
                .status(PhaseGatePendingCheck.CheckStatus.PENDING)
                .qlmResultReceived(false)
                .llmResultReceived(false)
                .build());

        // Publish check request — QLM and LLM will both receive this
        PhaseGateCheckRequestedEvent checkEvent = PhaseGateCheckRequestedEvent.builder()
                .versionId(versionId)
                .versionNumber(version.getVersionNumber())
                .productId(version.getProduct().getProductId())
                .productCode(version.getProduct().getProductCode())
                .targetPhase(req.getTargetPhase())
                .requestedBy(userId)
                .gateCorrelationId(gateCorrelationId)
                .build();

        eventPublisher.publishPhaseGateCheckRequested(checkEvent, correlationId);

        audit(AuditEntityType.VERSION, versionId, AuditAction.PHASE_GATE_CHECK_REQUESTED,
                null, Map.of("targetPhase", req.getTargetPhase(), "gateCorrelationId", gateCorrelationId),
                userId, AuditEventSource.USER, correlationId);

        log.info("Phase gate check dispatched via Pub/Sub [versionId={}, targetPhase={}, gateCorrelationId={}]",
                versionId, req.getTargetPhase(), gateCorrelationId);

        return PhaseGateResult.builder()
                .canAdvance(false)
                .targetPhase(req.getTargetPhase())
                .message("Phase gate check dispatched. Poll /versions/" + versionId + "/phase-gate-status for result.")
                .build();
    }

    /**
     * Called by PlmEventController once BOTH QLM and LLM results have arrived.
     * Makes the final advance-or-block decision and updates the version.
     */
    @Transactional
    public void finalisePhaseGate(PhaseGatePendingCheck check) {
        boolean certOk = Boolean.TRUE.equals(check.getLlmPassed());
        boolean ncrOk  = Boolean.TRUE.equals(check.getQlmPassed());
        boolean canAdvance = certOk && ncrOk;

        if (canAdvance) {
            ProductVersion version = findVersion(check.getVersionId());
            LifecyclePhase targetPhase = phaseRepo.findByPhaseName(check.getTargetPhase())
                    .orElseThrow(() -> new IllegalStateException("Unknown phase: " + check.getTargetPhase()));

            String fromPhase = version.getCurrentPhase() != null
                    ? version.getCurrentPhase().getPhaseName() : "NONE";

            version.setCurrentPhase(targetPhase);
            if (targetPhase.getPhaseName().equals("Deployment") || targetPhase.getPhaseName().equals("Maintenance")) {
                version.setStatus(ProductVersion.VersionStatus.RELEASED);
                version.setReleasedAt(OffsetDateTime.now());
                version.setReleasedBy(check.getRequestedBy());
            } else {
                version.setStatus(ProductVersion.VersionStatus.IN_PROGRESS);
            }
            versionRepo.save(version);

            // Publish downstream events
            PhaseTransitionedEvent phaseEvent = PhaseTransitionedEvent.builder()
                    .versionId(check.getVersionId())
                    .versionNumber(version.getVersionNumber())
                    .productId(version.getProduct().getProductId())
                    .productCode(version.getProduct().getProductCode())
                    .productName(version.getProduct().getName())
                    .fromPhase(fromPhase)
                    .toPhase(targetPhase.getPhaseName())
                    .toPhaseSequence(targetPhase.getSequenceOrder())
                    .triggeredBy(check.getRequestedBy())
                    .build();
            eventPublisher.publishPhaseTransitioned(phaseEvent, check.getCorrelationId());

            if (version.getStatus() == ProductVersion.VersionStatus.RELEASED) {
                eventPublisher.publishVersionReleased(VersionReleasedEvent.builder()
                        .versionId(check.getVersionId())
                        .versionNumber(version.getVersionNumber())
                        .productId(version.getProduct().getProductId())
                        .productCode(version.getProduct().getProductCode())
                        .releasedAt(version.getReleasedAt()).build(), check.getCorrelationId());
            }

            check.setStatus(PhaseGatePendingCheck.CheckStatus.PASSED);
            audit(AuditEntityType.VERSION, check.getVersionId(), AuditAction.PHASE_ADVANCED,
                    Map.of("from", fromPhase),
                    Map.of("to", targetPhase.getPhaseName(), "via", "PUBSUB_PHASE_GATE"),
                    check.getRequestedBy(), AuditEventSource.PUBSUB, check.getCorrelationId());

            log.info("Phase gate PASSED — version {} advanced to {} [gateCorrelationId={}]",
                    check.getVersionId(), check.getTargetPhase(), check.getGateCorrelationId());
        } else {
            check.setStatus(PhaseGatePendingCheck.CheckStatus.BLOCKED);
            String reason = !certOk ? check.getLlmBlockReason() : check.getQlmBlockReason();
            log.info("Phase gate BLOCKED for version {} → {} reason: {} [gateCorrelationId={}]",
                    check.getVersionId(), check.getTargetPhase(), reason, check.getGateCorrelationId());
            audit(AuditEntityType.VERSION, check.getVersionId(), AuditAction.PHASE_GATE_BLOCKED,
                    null, Map.of("reason", reason != null ? reason : "check failed"),
                    check.getRequestedBy(), AuditEventSource.PUBSUB, check.getCorrelationId());
        }

        check.setCompletedAt(OffsetDateTime.now());
        pendingCheckRepo.save(check);
    }

    /**
     * Allows callers to poll the result of an async phase gate check.
     */
    @Transactional(readOnly = true)
    public PhaseGateResult getPhaseGateStatus(UUID versionId, String targetPhase) {
        // Return the most recent check for this version+phase
        return pendingCheckRepo
                .findByVersionIdAndTargetPhaseAndStatus(versionId, targetPhase,
                        PhaseGatePendingCheck.CheckStatus.PENDING)
                .map(check -> PhaseGateResult.builder()
                        .canAdvance(false)
                        .targetPhase(targetPhase)
                        .message("Phase gate check still in progress (qlm=" + check.isQlmResultReceived()
                                + ", llm=" + check.isLlmResultReceived() + ")")
                        .build())
                .orElseGet(() -> {
                    // Check for the most recent completed result
                    return pendingCheckRepo
                            .findByVersionIdAndTargetPhaseAndStatusNot(versionId, targetPhase,
                                    PhaseGatePendingCheck.CheckStatus.PENDING)
                            .stream()
                            .max(Comparator.comparing(PhaseGatePendingCheck::getCreatedAt))
                            .map(c -> PhaseGateResult.builder()
                                    .canAdvance(c.getStatus() == PhaseGatePendingCheck.CheckStatus.PASSED)
                                    .targetPhase(targetPhase)
                                    .certCheckPassed(Boolean.TRUE.equals(c.getLlmPassed()))
                                    .ncrCheckPassed(Boolean.TRUE.equals(c.getQlmPassed()))
                                    .uncertifiedCount(c.getUncertifiedCount() != null ? c.getUncertifiedCount() : 0)
                                    .blockReason(!Boolean.TRUE.equals(c.getLlmPassed())
                                            ? c.getLlmBlockReason() : c.getQlmBlockReason())
                                    .message(c.getStatus().name())
                                    .build())
                            .orElse(PhaseGateResult.builder()
                                    .canAdvance(false)
                                    .targetPhase(targetPhase)
                                    .message("No phase gate check found for this version and phase")
                                    .build());
                });
    }

    // ── CHANGE REQUESTS ───────────────────────────────────────

    @Transactional
    public ChangeRequestResponse createChangeRequest(UUID versionId, CreateChangeRequestRequest req, UUID userId) {
        ProductVersion version = findVersion(versionId);
        String crNumber = "CR-" + version.getProduct().getProductCode() + "-" + System.currentTimeMillis()
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ChangeRequest cr = crRepo.save(ChangeRequest.builder()
                .crNumber(crNumber).version(version)
                .crType(req.crType != null ? req.crType : ChangeRequest.CrType.STANDARD)
                .title(req.title).description(req.description)
                .reason(req.reason).impactAnalysis(req.impactAnalysis)
                .status(ChangeRequest.CrStatus.DRAFT).raisedBy(userId).build());

        audit(AuditEntityType.CHANGE_REQUEST, cr.getCrId(), AuditAction.CREATED, null, cr, userId, AuditEventSource.USER, null);
        return toCrResponse(cr);
    }

    @Transactional
    public ChangeRequestResponse submitChangeRequest(UUID crId, UUID userId, String correlationId) {
        ChangeRequest cr = crRepo.findById(crId)
                .orElseThrow(() -> new NoSuchElementException("CR not found: " + crId));
        cr.setStatus(ChangeRequest.CrStatus.SUBMITTED);
        cr.setSubmittedAt(OffsetDateTime.now());
        crRepo.save(cr);

        // Create approval workflow entry for the product's designated approver
        UUID approverUserId = cr.getVersion().getProduct().getApproverUserId();
        if (approverUserId != null) {
            workflowRepo.save(ApprovalWorkflow.builder()
                    .entityType(ApprovalWorkflow.EntityType.CHANGE_REQUEST)
                    .entityId(crId)
                    .stepOrder(1)
                    .approverUserId(approverUserId)
                    .status(ApprovalWorkflow.WorkflowStatus.PENDING)
                    .build());
        }

        audit(AuditEntityType.CHANGE_REQUEST, crId, AuditAction.SUBMITTED, null, null, userId, AuditEventSource.USER, correlationId);
        return toCrResponse(cr);
    }

    @Transactional
    public ChangeRequestResponse approveChangeRequest(UUID crId, UUID approverId, ChangeRequest.CrStatus decision, String comments, String correlationId) {
        ChangeRequest cr = crRepo.findById(crId)
                .orElseThrow(() -> new NoSuchElementException("CR not found: " + crId));

        // Validate that the caller is the designated approver
        List<ApprovalWorkflow> workflows = workflowRepo
                .findByEntityTypeAndEntityIdOrderByStepOrderAsc(ApprovalWorkflow.EntityType.CHANGE_REQUEST, crId);
        if (!workflows.isEmpty()) {
            ApprovalWorkflow step = workflows.get(0);
            if (!step.getApproverUserId().equals(approverId)) {
                throw new IllegalStateException("You are not the designated approver for this change request.");
            }
            // Mark the workflow step as decided
            step.setStatus(decision == ChangeRequest.CrStatus.APPROVED
                    ? ApprovalWorkflow.WorkflowStatus.APPROVED
                    : ApprovalWorkflow.WorkflowStatus.REJECTED);
            step.setComments(comments);
            step.setDecidedAt(OffsetDateTime.now());
            workflowRepo.save(step);
        }

        ChangeRequest.CrStatus newStatus = decision;
        cr.setStatus(newStatus);
        cr.setDecidedAt(OffsetDateTime.now());
        crRepo.save(cr);

        if (newStatus == ChangeRequest.CrStatus.APPROVED) {
            eventPublisher.publishChangeRequestApproved(ChangeRequestApprovedEvent.builder()
                    .crId(crId).crNumber(cr.getCrNumber()).crType(cr.getCrType().name())
                    .versionId(cr.getVersion().getVersionId()).build(), correlationId);
        }

        AuditAction auditAction = newStatus == ChangeRequest.CrStatus.APPROVED ? AuditAction.APPROVED : AuditAction.REJECTED;
        audit(AuditEntityType.CHANGE_REQUEST, crId, auditAction, null, null, approverId, AuditEventSource.USER, correlationId);
        return toCrResponse(cr);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listChangeRequests(UUID versionId) {
        ProductVersion version = findVersion(versionId);
        return crRepo.findByVersion(version).stream().map(this::toCrResponse).toList();
    }

    // ── BOM ───────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "bom-tree", key = "#versionId")
    public BomComponentResponse addBomComponent(UUID versionId, CreateBomComponentRequest req, UUID userId) {
        ProductVersion version = findVersion(versionId);
        BomComponent parent = req.getParentComponentId() != null
                ? bomRepo.findById(req.getParentComponentId()).orElse(null) : null;

        BomComponent comp = bomRepo.save(BomComponent.builder()
                .version(version).parent(parent)
                .componentCode(req.getComponentCode()).name(req.getName())
                .componentType(req.getComponentType())
                .quantity(req.getQuantity() != null ? new java.math.BigDecimal(req.getQuantity()) : java.math.BigDecimal.ONE)
                .unit(req.getUnit()).notes(req.getNotes())
                .createdBy(userId).build());

        return toBomResponse(comp, false);
    }

    @Cacheable(value = "bom-tree", key = "#versionId")
    @Transactional(readOnly = true)
    public List<BomComponentResponse> getBomTree(UUID versionId) {
        ProductVersion version = findVersion(versionId);
        List<BomComponent> roots = bomRepo.findByVersionAndParentIsNull(version);
        return roots.stream().map(r -> toBomWithChildren(r, bomRepo.findByVersion(version))).toList();
    }

    // ── PHASES ────────────────────────────────────────────────

    @Cacheable(value = "phases")
    @Transactional(readOnly = true)
    public List<LifecyclePhaseResponse> listPhases() {
        return phaseRepo.findAllByOrderBySequenceOrderAsc().stream()
                .map(p -> LifecyclePhaseResponse.builder()
                        .phaseId(p.getPhaseId()).phaseName(p.getPhaseName())
                        .displayName(p.getDisplayName()).sequenceOrder(p.getSequenceOrder())
                        .build()).toList();
    }

    // ── EVENT HANDLING ────────────────────────────────────────

    /**
     * QLM auto-created a CAPA stub for a MAJOR/CRITICAL NCR on this product version.
     * PLM logs this against the version so the product team knows a CAPA is waiting
     * for their corrective/preventive action plan.
     */
    @Transactional
    public void handleCapaRaised(CapaRaisedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-CAPA-" + event.getCapaId(), "cde.qlm.capa.raised")) return;

        if (event.getProductVersionId() != null) {
            versionRepo.findById(event.getProductVersionId()).ifPresent(v -> {
                log.info("CAPA {} auto-raised for version {} (NCR {}). Product team must submit a corrective plan.",
                        event.getCapaNumber(), v.getVersionId(), event.getNcrId());
                audit(AuditEntityType.VERSION, v.getVersionId(), AuditAction.UPDATED,
                        null, Map.of(
                                "event",      "capa.raised",
                                "capaId",     event.getCapaId().toString(),
                                "capaNumber", event.getCapaNumber(),
                                "ncrId",      event.getNcrId() != null ? event.getNcrId().toString() : "N/A"
                        ), null, AuditEventSource.PUBSUB, correlationId);
            });
        }
    }

    /**
     * QLM closed an NCR (all linked CAPAs resolved).
     * If the product version was put on HOLD because of that NCR, lift it back to IN_PROGRESS
     * so the product team can continue working.
     *
     * BUG FIX: Without this handler the version remained on HOLD forever —
     * PLmEventController previously ignored cde.qlm.ncr.closed entirely.
     */
    @Transactional
    public void handleNcrClosed(NcrClosedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-NCRCLOSED-" + event.getNcrId(), "cde.qlm.ncr.closed")) return;

        if (event.getProductVersionId() != null) {
            versionRepo.findById(event.getProductVersionId()).ifPresent(v -> {
                if (v.getStatus() == ProductVersion.VersionStatus.HOLD) {
                    String oldStatus = v.getStatus().name();
                    v.setStatus(ProductVersion.VersionStatus.IN_PROGRESS);
                    versionRepo.save(v);
                    log.info("Version {} lifted off HOLD — NCR {} closed", v.getVersionId(), event.getNcrId());
                    eventPublisher.publishVersionStatusChanged(VersionStatusChangedEvent.builder()
                            .versionId(v.getVersionId())
                            .productCode(v.getProduct().getProductCode())
                            .versionNumber(v.getVersionNumber())
                            .oldStatus(oldStatus)
                            .newStatus(ProductVersion.VersionStatus.IN_PROGRESS.name())
                            .build(), correlationId);
                    audit(AuditEntityType.VERSION, v.getVersionId(), AuditAction.UPDATED,
                            Map.of("oldStatus", oldStatus),
                            Map.of("newStatus", "IN_PROGRESS", "reason", "NCR " + event.getNcrNumber() + " closed"),
                            null, AuditEventSource.PUBSUB, correlationId);
                }
            });
        }
    }

    @Transactional
    public void handleNcrRaised(NcrRaisedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-NCR-" + event.getNcrId(), "cde.qlm.ncr.raised")) return;

        if (NcrRaisedEvent.SEVERITY_CRITICAL.equals(event.getSeverity()) && event.getProductVersionId() != null) {
            versionRepo.findById(event.getProductVersionId()).ifPresent(v -> {
                String oldStatus = v.getStatus().name();
                v.setStatus(ProductVersion.VersionStatus.HOLD);
                versionRepo.save(v);
                log.info("Version {} put on HOLD due to critical NCR {}", v.getVersionId(), event.getNcrId());
                eventPublisher.publishVersionStatusChanged(VersionStatusChangedEvent.builder()
                        .versionId(v.getVersionId())
                        .productCode(v.getProduct().getProductCode())
                        .versionNumber(v.getVersionNumber())
                        .oldStatus(oldStatus)
                        .newStatus(ProductVersion.VersionStatus.HOLD.name())
                        .build(), correlationId);
                audit(AuditEntityType.VERSION, v.getVersionId(), AuditAction.PUT_ON_HOLD,
                        null, Map.of("reason", "Critical NCR " + event.getNcrNumber()), null, AuditEventSource.PUBSUB, correlationId);
            });
        }
    }

    @Transactional
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-USR-" + event.getUserId(), "cde.llm.user.profile.updated")) return;

        UserShadow shadow = UserShadow.builder()
                .userId(event.getUserId()).employeeId(event.getEmployeeId())
                .email(event.getEmail()).fullName(event.getFullName())
                .role(event.getRole()).department(event.getDepartment())
                .lastSyncedAt(OffsetDateTime.now()).build();
        userShadowRepo.save(shadow);
    }

    /**
     * Handles a phase gate check result arriving from QLM or LLM via Pub/Sub.
     * Once both results are received, triggers finalisePhaseGate().
     */
    @Transactional
    public void handlePhaseGateCheckResult(PhaseGateCheckResultEvent event, String correlationId) {
        String eventKey = "PGCR-" + event.getGateCorrelationId() + "-" + event.getCheckerService();
        if (!idempotencyCheck(eventKey, "cde.plm.phase.gate.check.result")) return;

        // Use PESSIMISTIC_WRITE lock: if QLM and LLM results arrive at the same time,
        // only one transaction proceeds at a time — preventing the second writer from
        // overwriting the first writer's flag before bothResultsReceived() is evaluated.
        PhaseGatePendingCheck check = pendingCheckRepo.findByIdForUpdate(event.getGateCorrelationId())
                .orElse(null);

        if (check == null) {
            log.warn("Received phase gate result for unknown gateCorrelationId: {}", event.getGateCorrelationId());
            return;
        }
        if (check.getStatus() != PhaseGatePendingCheck.CheckStatus.PENDING) {
            log.info("Phase gate check already finalised, ignoring late result [gateCorrelationId={}]",
                    event.getGateCorrelationId());
            return;
        }

        switch (event.getCheckerService()) {
            case "QLM" -> {
                check.setQlmResultReceived(true);
                check.setQlmPassed(event.isPassed());
                check.setQlmBlockReason(event.getBlockReason());
                check.setOpenCriticalNcrCount(event.getOpenCriticalNcrCount());
                log.info("QLM result received [gateCorrelationId={}, passed={}]",
                        event.getGateCorrelationId(), event.isPassed());
            }
            case "LLM" -> {
                check.setLlmResultReceived(true);
                check.setLlmPassed(event.isPassed());
                check.setLlmBlockReason(event.getBlockReason());
                check.setUncertifiedCount(event.getUncertifiedCount());
                log.info("LLM result received [gateCorrelationId={}, passed={}]",
                        event.getGateCorrelationId(), event.isPassed());
            }
            default -> log.warn("Unknown checker service in phase gate result: {}", event.getCheckerService());
        }

        pendingCheckRepo.save(check);

        // Both results in — make the final decision
        if (check.bothResultsReceived()) {
            finalisePhaseGate(check);
        }
    }

    // ── AUDIT ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditLog(AuditEntityType entityType, UUID entityId) {
        return auditRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType.name(), entityId).stream()
                .map(l -> AuditLogEntry.builder()
                        .logId(l.getLogId()).entityType(l.getEntityType()).entityId(l.getEntityId())
                        .action(l.getAction()).performedBy(l.getPerformedBy())
                        .eventSource(l.getEventSource()).correlationId(l.getCorrelationId())
                        .createdAt(l.getCreatedAt()).build()).toList();
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Product findProduct(UUID id) {
        return productRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    private ProductVersion findVersion(UUID id) {
        return versionRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Version not found: " + id));
    }

    private boolean idempotencyCheck(String eventId, String type) {
        if (processedEventRepo.existsByEventId(eventId)) return false;
        processedEventRepo.save(ProcessedEvent.builder().eventId(eventId).eventType(type).build());
        return true;
    }

    private void audit(AuditEntityType entityType, UUID entityId, AuditAction action,
                       Object oldVal, Object newVal, UUID by, AuditEventSource source, String correlationId) {
        try {
            auditRepo.save(TraceabilityAuditLog.builder()
                    .entityType(entityType.name()).entityId(entityId).action(action.name())
                    .performedBy(by).eventSource(source.name()).correlationId(correlationId).build());
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }

    private ProductResponse toProductResponse(Product p, boolean includeVersions) {
        String approverName = null;
        if (p.getApproverUserId() != null) {
            approverName = userShadowRepo.findById(p.getApproverUserId())
                    .map(u -> u.getFullName()).orElse(null);
        }
        return ProductResponse.builder()
                .productId(p.getProductId()).productCode(p.getProductCode())
                .name(p.getName()).description(p.getDescription())
                .status(p.getStatus().name())
                .approverUserId(p.getApproverUserId())
                .approverName(approverName)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }

    public ProductVersionResponse toVersionResponse(ProductVersion v) {
        return ProductVersionResponse.builder()
                .versionId(v.getVersionId())
                .productId(v.getProduct().getProductId())
                .productCode(v.getProduct().getProductCode())
                .productName(v.getProduct().getName())
                .versionNumber(v.getVersionNumber())
                .description(v.getDescription())
                .status(v.getStatus().name())
                .currentPhase(v.getCurrentPhase() != null ? v.getCurrentPhase().getDisplayName() : null)
                .phaseSequence(v.getCurrentPhase() != null ? v.getCurrentPhase().getSequenceOrder() : null)
                .releasedAt(v.getReleasedAt())
                .releasedBy(v.getReleasedBy())
                .createdAt(v.getCreatedAt()).updatedAt(v.getUpdatedAt()).build();
    }

    private ChangeRequestResponse toCrResponse(ChangeRequest cr) {
        return ChangeRequestResponse.builder()
                .crId(cr.getCrId()).crNumber(cr.getCrNumber())
                .versionId(cr.getVersion().getVersionId())
                .crType(cr.getCrType().name()).title(cr.getTitle())
                .description(cr.getDescription()).reason(cr.getReason())
                .impactAnalysis(cr.getImpactAnalysis()).status(cr.getStatus().name())
                .raisedBy(cr.getRaisedBy())
                .submittedAt(cr.getSubmittedAt()).decidedAt(cr.getDecidedAt())
                .createdAt(cr.getCreatedAt()).updatedAt(cr.getUpdatedAt()).build();
    }

    private BomComponentResponse toBomResponse(BomComponent c, boolean includeChildren) {
        return BomComponentResponse.builder()
                .componentId(c.getComponentId())
                .parentComponentId(c.getParent() != null ? c.getParent().getComponentId() : null)
                .componentCode(c.getComponentCode()).name(c.getName())
                .componentType(c.getComponentType())
                .quantity(c.getQuantity() != null ? c.getQuantity().doubleValue() : 1.0)
                .unit(c.getUnit()).notes(c.getNotes())
                .createdBy(c.getCreatedBy()).createdAt(c.getCreatedAt()).build();
    }

    private BomComponentResponse toBomWithChildren(BomComponent root, List<BomComponent> all) {
        BomComponentResponse res = toBomResponse(root, false);
        List<BomComponentResponse> children = all.stream()
                .filter(c -> c.getParent() != null && c.getParent().getComponentId().equals(root.getComponentId()))
                .map(child -> toBomWithChildren(child, all)).toList();
        res.children = children.isEmpty() ? null : children;
        return res;
    }
}
