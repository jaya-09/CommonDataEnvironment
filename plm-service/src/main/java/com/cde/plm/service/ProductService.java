package com.cde.plm.service;

import com.cde.plm.dto.*;
import com.cde.plm.entity.*;
import com.cde.plm.event.*;
import com.cde.plm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final PlmEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    @Value("${app.services.llm-url}") private String llmServiceUrl;
    @Value("${app.services.qlm-url:http://localhost:8082}") private String qlmServiceUrl;

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
                .build());

        audit("PRODUCT", product.getProductId(), "CREATED", null, product, userId, "USER", null);
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
                .status(ProductVersion.VersionStatus.DRAFT)
                .currentPhase(firstPhase)
                .createdBy(userId)
                .build());

        audit("VERSION", version.getVersionId(), "CREATED", null, version, userId, "USER", null);
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
     * Phase Gate Saga: checks LLM (certs) + QLM (no open critical NCRs) before advancing.
     */
    @Transactional
    public PhaseGateResult advancePhase(UUID versionId, PhaseTransitionRequest req, UUID userId, String correlationId) {
        ProductVersion version = findVersion(versionId);
        LifecyclePhase targetPhase = phaseRepo.findByPhaseName(req.getTargetPhase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown phase: " + req.getTargetPhase()));

        // 1) Check LLM: all staff certified for target phase?
        PhaseGateResult result = checkPhaseGate(version, targetPhase, correlationId);

        if (!result.canAdvance) {
            log.info("Phase gate blocked for version {} → {}: {}", versionId, req.getTargetPhase(), result.blockReason);
            return result;
        }

        // 2) Advance phase
        String fromPhase = version.getCurrentPhase() != null ? version.getCurrentPhase().getPhaseName() : "NONE";
        version.setCurrentPhase(targetPhase);
        if (targetPhase.getPhaseName().equals("Deployment") || targetPhase.getPhaseName().equals("Maintenance")) {
            version.setStatus(ProductVersion.VersionStatus.RELEASED);
            version.setReleasedAt(OffsetDateTime.now());
        } else {
            version.setStatus(ProductVersion.VersionStatus.IN_PROGRESS);
        }
        versionRepo.save(version);

        // 3) Publish events
        PhaseTransitionedEvent phaseEvent = PhaseTransitionedEvent.builder()
                .versionId(versionId).versionNumber(version.getVersionNumber())
                .productId(version.getProduct().getProductId()).productCode(version.getProduct().getProductCode())
                .fromPhase(fromPhase).toPhase(targetPhase.getPhaseName()).triggeredBy(userId).build();
        eventPublisher.publishPhaseTransitioned(phaseEvent, correlationId);

        if (version.getStatus() == ProductVersion.VersionStatus.RELEASED) {
            eventPublisher.publishVersionReleased(VersionReleasedEvent.builder()
                    .versionId(versionId).versionNumber(version.getVersionNumber())
                    .productId(version.getProduct().getProductId())
                    .productCode(version.getProduct().getProductCode())
                    .releasedAt(version.getReleasedAt()).build(), correlationId);
        }

        audit("VERSION", versionId, "PHASE_ADVANCED",
                Map.of("from", fromPhase), Map.of("to", targetPhase.getPhaseName()), userId, "USER", correlationId);

        return result;
    }

    private PhaseGateResult checkPhaseGate(ProductVersion version, LifecyclePhase targetPhase, String correlationId) {
        boolean certOk = true;
        boolean ncrOk = true;
        int uncertifiedCount = 0;
        List<String> openNcrs = new ArrayList<>();

        // Check LLM for cert readiness
        try {
            var response = restTemplate.getForObject(
                    llmServiceUrl + "/api/v1/llm/phase-readiness/" + targetPhase.getPhaseName(),
                    Map.class);
            certOk = Boolean.TRUE.equals(response.get("ready"));
            uncertifiedCount = response.containsKey("uncertifiedCount")
                    ? (Integer) response.get("uncertifiedCount") : 0;
        } catch (Exception e) {
            log.warn("Could not reach LLM service for phase gate check, proceeding: {}", e.getMessage());
        }

        // Check QLM for open critical NCRs on this version
        try {
            var response = restTemplate.getForObject(
                    qlmServiceUrl + "/api/v1/qlm/ncr/check?versionId=" + version.getVersionId() + "&severity=CRITICAL",
                    Map.class);
            ncrOk = Boolean.TRUE.equals(response.get("clear"));
        } catch (Exception e) {
            log.warn("Could not reach QLM service for phase gate check, proceeding: {}", e.getMessage());
        }

        boolean canAdvance = certOk && ncrOk;
        String blockReason = null;
        if (!certOk) blockReason = uncertifiedCount + " staff missing certifications for phase " + targetPhase.getPhaseName();
        else if (!ncrOk) blockReason = "Open critical NCRs must be resolved before advancing";

        return PhaseGateResult.builder()
                .canAdvance(canAdvance).targetPhase(targetPhase.getPhaseName())
                .certCheckPassed(certOk).ncrCheckPassed(ncrOk)
                .uncertifiedCount(uncertifiedCount).openCriticalNcrs(openNcrs)
                .blockReason(blockReason).build();
    }

    // ── CHANGE REQUESTS ───────────────────────────────────────

    @Transactional
    public ChangeRequestResponse createChangeRequest(UUID versionId, CreateChangeRequestRequest req, UUID userId) {
        ProductVersion version = findVersion(versionId);
        String crNumber = "CR-" + version.getProduct().getProductCode() + "-" + System.currentTimeMillis();

        ChangeRequest cr = crRepo.save(ChangeRequest.builder()
                .crNumber(crNumber).version(version)
                .crType(req.crType != null ? ChangeRequest.CrType.valueOf(req.crType) : ChangeRequest.CrType.STANDARD)
                .title(req.title).description(req.description)
                .reason(req.reason).impactAnalysis(req.impactAnalysis)
                .status(ChangeRequest.CrStatus.DRAFT).raisedBy(userId).build());

        audit("CHANGE_REQUEST", cr.getCrId(), "CREATED", null, cr, userId, "USER", null);
        return toCrResponse(cr);
    }

    @Transactional
    public ChangeRequestResponse submitChangeRequest(UUID crId, UUID userId, String correlationId) {
        ChangeRequest cr = crRepo.findById(crId)
                .orElseThrow(() -> new NoSuchElementException("CR not found: " + crId));
        cr.setStatus(ChangeRequest.CrStatus.SUBMITTED);
        crRepo.save(cr);
        audit("CHANGE_REQUEST", crId, "SUBMITTED", null, null, userId, "USER", correlationId);
        return toCrResponse(cr);
    }

    @Transactional
    public ChangeRequestResponse approveChangeRequest(UUID crId, UUID approverId, String decision, String comments, String correlationId) {
        ChangeRequest cr = crRepo.findById(crId)
                .orElseThrow(() -> new NoSuchElementException("CR not found: " + crId));

        ChangeRequest.CrStatus newStatus = "APPROVED".equals(decision)
                ? ChangeRequest.CrStatus.APPROVED : ChangeRequest.CrStatus.REJECTED;
        cr.setStatus(newStatus);
        crRepo.save(cr);

        if (newStatus == ChangeRequest.CrStatus.APPROVED) {
            eventPublisher.publishChangeRequestApproved(ChangeRequestApprovedEvent.builder()
                    .crId(crId).crNumber(cr.getCrNumber()).crType(cr.getCrType().name())
                    .versionId(cr.getVersion().getVersionId()).build(), correlationId);
        }

        audit("CHANGE_REQUEST", crId, decision, null, null, approverId, "USER", correlationId);
        return toCrResponse(cr);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listChangeRequests(UUID versionId) {
        ProductVersion version = findVersion(versionId);
        return crRepo.findByVersion(version).stream().map(this::toCrResponse).toList();
    }

    // ── BOM ───────────────────────────────────────────────────

    @Transactional
    public BomComponentResponse addBomComponent(UUID versionId, CreateBomComponentRequest req, UUID userId) {
        ProductVersion version = findVersion(versionId);
        BomComponent parent = req.getParentComponentId() != null
                ? bomRepo.findById(req.getParentComponentId()).orElse(null) : null;

        BomComponent comp = bomRepo.save(BomComponent.builder()
                .version(version).parent(parent)
                .componentCode(req.getComponentCode()).name(req.getName())
                .componentType(req.getComponentType())
                .quantity(req.getQuantity() != null ? new java.math.BigDecimal(req.getQuantity()) : java.math.BigDecimal.ONE)
                .unit(req.getUnit()).notes(req.getNotes()).build());

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
                        .active(p.isActive()).build()).toList();
    }

    // ── EVENT HANDLING ────────────────────────────────────────

    @Transactional
    public void handleNcrRaised(NcrRaisedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-NCR-" + event.getNcrId(), "cde.qlm.ncr.raised")) return;

        // If critical NCR, put version on HOLD
        if ("CRITICAL".equals(event.getSeverity()) && event.getProductVersionId() != null) {
            versionRepo.findById(event.getProductVersionId()).ifPresent(v -> {
                v.setStatus(ProductVersion.VersionStatus.HOLD);
                versionRepo.save(v);
                log.info("Version {} put on HOLD due to critical NCR {}", v.getVersionId(), event.getNcrId());
                audit("VERSION", v.getVersionId(), "PUT_ON_HOLD",
                        null, Map.of("reason", "Critical NCR " + event.getNcrNumber()), null, "PUBSUB", correlationId);
            });
        }
    }

    @Transactional
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-USR-" + event.getUserId(), "cde.llm.user.profile_updated")) return;

        UserShadow shadow = UserShadow.builder()
                .userId(event.getUserId()).employeeId(event.getEmployeeId())
                .email(event.getEmail()).fullName(event.getFullName())
                .role(event.getRole()).department(event.getDepartment())
                .lastSyncedAt(OffsetDateTime.now()).build();
        userShadowRepo.save(shadow);
    }

    // ── AUDIT ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditLog(String entityType, UUID entityId) {
        return auditRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId).stream()
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

    private void audit(String entityType, UUID entityId, String action,
                       Object oldVal, Object newVal, UUID by, String source, String correlationId) {
        try {
            auditRepo.save(TraceabilityAuditLog.builder()
                    .entityType(entityType).entityId(entityId).action(action)
                    .performedBy(by).eventSource(source).correlationId(correlationId).build());
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }

    private ProductResponse toProductResponse(Product p, boolean includeVersions) {
        return ProductResponse.builder()
                .productId(p.getProductId()).productCode(p.getProductCode())
                .name(p.getName()).description(p.getDescription())
                .status(p.getStatus().name())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }

    public ProductVersionResponse toVersionResponse(ProductVersion v) {
        return ProductVersionResponse.builder()
                .versionId(v.getVersionId())
                .productId(v.getProduct().getProductId())
                .productCode(v.getProduct().getProductCode())
                .productName(v.getProduct().getName())
                .versionNumber(v.getVersionNumber())
                .status(v.getStatus().name())
                .currentPhase(v.getCurrentPhase() != null ? v.getCurrentPhase().getDisplayName() : null)
                .phaseSequence(v.getCurrentPhase() != null ? v.getCurrentPhase().getSequenceOrder() : null)
                .releasedAt(v.getReleasedAt())
                .createdAt(v.getCreatedAt()).updatedAt(v.getUpdatedAt()).build();
    }

    private ChangeRequestResponse toCrResponse(ChangeRequest cr) {
        return ChangeRequestResponse.builder()
                .crId(cr.getCrId()).crNumber(cr.getCrNumber())
                .versionId(cr.getVersion().getVersionId())
                .crType(cr.getCrType().name()).title(cr.getTitle())
                .description(cr.getDescription()).reason(cr.getReason())
                .impactAnalysis(cr.getImpactAnalysis()).status(cr.getStatus().name())
                .createdAt(cr.getCreatedAt()).updatedAt(cr.getUpdatedAt()).build();
    }

    private BomComponentResponse toBomResponse(BomComponent c, boolean includeChildren) {
        return BomComponentResponse.builder()
                .componentId(c.getComponentId())
                .parentComponentId(c.getParent() != null ? c.getParent().getComponentId() : null)
                .componentCode(c.getComponentCode()).name(c.getName())
                .componentType(c.getComponentType())
                .quantity(c.getQuantity() != null ? c.getQuantity().doubleValue() : 1.0)
                .unit(c.getUnit()).notes(c.getNotes()).build();
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
