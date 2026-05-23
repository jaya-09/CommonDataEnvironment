package com.cde.qlm.service;

import com.cde.qlm.audit.AuditAction;
import com.cde.qlm.audit.AuditEntityType;
import com.cde.qlm.dto.*;
import com.cde.qlm.entity.*;
import com.cde.qlm.event.*;
import com.cde.qlm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class QlmService {

    private final NcrRepository ncrRepo;
    private final CapaRepository capaRepo;
    private final QualityAuditRepository auditRepo;
    private final RiskRegisterRepository riskRepo;
    private final DocumentControlRepository docRepo;
    private final QualityAuditLogRepository logRepo;
    private final ProcessedEventRepository processedRepo;
    private final UserShadowRepository userShadowRepo;
    private final QlmEventPublisher publisher;

    // ── NCR ──────────────────────────────────────────────────

    @Transactional
    public NcrResponse createNcr(CreateNcrRequest req, UUID userId, String correlationId) {
        // Append short UUID fragment to prevent collision when two creates arrive in the same millisecond
        String ncrNumber = "NCR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        NonConformance.Severity severity = req.getSeverity() != null
                ? NonConformance.Severity.valueOf(req.getSeverity())
                : NonConformance.Severity.MAJOR;

        NonConformance ncr = ncrRepo.save(NonConformance.builder()
                .ncrNumber(ncrNumber)
                .productVersionId(req.getProductVersionId())
                .productCode(req.getProductCode())
                .severity(severity)
                .title(req.getTitle()).description(req.getDescription())
                .status(NonConformance.NcrStatus.OPEN)
                .reportedBy(userId).build());

        audit(AuditEntityType.NCR, ncr.getNcrId(), AuditAction.CREATED, userId, correlationId);

        publisher.publishNcrRaised(NcrRaisedEvent.builder()
                .ncrId(ncr.getNcrId()).ncrNumber(ncrNumber)
                .productVersionId(req.getProductVersionId())
                .productCode(req.getProductCode())
                .title(req.getTitle())
                .severity(ncr.getSeverity().name())
                .description(req.getDescription())
                .reportedBy(userId)
                .raisedAt(ncr.getDetectedAt())
                .build(), correlationId);

        // ── Auto-create CAPA stub for MAJOR and CRITICAL NCRs ──────────────
        // The responsible team (product owners in PLM) will receive the CapaRaisedEvent
        // and fill in their corrective + preventive action plan before submitting for review.
        if (severity == NonConformance.Severity.MAJOR || severity == NonConformance.Severity.CRITICAL) {
            String capaNumber = "CAPA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Capa capa = capaRepo.save(Capa.builder()
                    .capaNumber(capaNumber)
                    .ncr(ncr)
                    .title("CAPA for NCR: " + ncr.getTitle())
                    .ownerUserId(userId)   // initially assigned to reporter; reassigned by PLM team
                    .status(Capa.CapaStatus.OPEN)
                    .build());

            ncr.setStatus(NonConformance.NcrStatus.PENDING_CAPA);
            ncrRepo.save(ncr);

            publisher.publishCapaRaised(CapaRaisedEvent.builder()
                    .capaId(capa.getCapaId()).capaNumber(capa.getCapaNumber())
                    .ncrId(ncr.getNcrId())
                    .productVersionId(ncr.getProductVersionId())
                    .build(), correlationId);

            audit(AuditEntityType.CAPA, capa.getCapaId(), AuditAction.CREATED, userId, correlationId);
            log.info("Auto-created CAPA stub [capaId={}, ncrId={}, severity={}]",
                    capa.getCapaId(), ncr.getNcrId(), severity);
        }

        return toNcrResponse(ncr);
    }

    @Transactional(readOnly = true)
    public List<NcrResponse> listNcrs(String status) {
        List<NonConformance> ncrs = status != null
                ? ncrRepo.findByStatus(NonConformance.NcrStatus.valueOf(status))
                : ncrRepo.findAll();
        return ncrs.stream().map(this::toNcrResponse).toList();
    }

    @Transactional(readOnly = true)
    public NcrResponse getNcr(UUID ncrId) {
        return toNcrResponse(findNcr(ncrId));
    }

    /**
     * Update an NCR's status, root cause, or assignee.
     *
     * Guard: an NCR cannot be set to CLOSED if it has a linked CAPA that is not yet
     * CLOSED or CANCELLED. The responsible team must close their CAPA first.
     */
    @Transactional
    public NcrResponse updateNcr(UUID ncrId, UpdateNcrRequest req, UUID userId, String correlationId) {
        NonConformance ncr = findNcr(ncrId);

        if (req.getStatus() != null) {
            NonConformance.NcrStatus newStatus = NonConformance.NcrStatus.valueOf(req.getStatus());

            // ── CLOSE GUARD ──────────────────────────────────────────────────────
            if (newStatus == NonConformance.NcrStatus.CLOSED) {
                List<Capa> linkedCapas = capaRepo.findByNcr(ncr);
                List<Capa> blockers = linkedCapas.stream()
                        .filter(c -> c.getStatus() != Capa.CapaStatus.CLOSED
                                  && c.getStatus() != Capa.CapaStatus.CANCELLED)
                        .toList();
                if (!blockers.isEmpty()) {
                    String blocking = blockers.stream()
                            .map(c -> c.getCapaNumber() + " [" + c.getStatus() + "]")
                            .reduce((a, b) -> a + ", " + b).orElse("");
                    throw new IllegalStateException(
                            "Cannot close NCR: linked CAPA(s) must be CLOSED or CANCELLED first: " + blocking);
                }
            }
            // ─────────────────────────────────────────────────────────────────────

            ncr.setStatus(newStatus);
            if (newStatus == NonConformance.NcrStatus.CLOSED)
                ncr.setClosedAt(OffsetDateTime.now());
        }

        if (req.getRootCause() != null) ncr.setRootCause(req.getRootCause());
        if (req.getAssignedTo() != null) ncr.setAssignedTo(req.getAssignedTo());
        ncrRepo.save(ncr);
        audit(AuditEntityType.NCR, ncrId, AuditAction.UPDATED, userId, correlationId);

        if (ncr.getStatus() == NonConformance.NcrStatus.CLOSED) {
            publisher.publishNcrClosed(NcrClosedEvent.builder()
                    .ncrId(ncr.getNcrId()).ncrNumber(ncr.getNcrNumber())
                    .productVersionId(ncr.getProductVersionId()).productCode(ncr.getProductCode())
                    .build(), correlationId);
        }
        return toNcrResponse(ncr);
    }

    /** Called by PLM phase gate check. */
    @Transactional(readOnly = true)
    public NcrCheckResponse checkNcrsForVersion(UUID versionId, String severity) {
        long count = ncrRepo.countByProductVersionIdAndSeverityAndStatusNot(
                versionId,
                NonConformance.Severity.valueOf(severity),
                NonConformance.NcrStatus.CLOSED);
        return NcrCheckResponse.builder()
                .clear(count == 0).openCriticalCount(count).versionId(versionId).build();
    }

    // ── CAPA ─────────────────────────────────────────────────

    /**
     * Manually create a CAPA (e.g. for MINOR NCRs where auto-creation doesn't trigger,
     * or as a standalone preventive action not linked to any NCR).
     */
    @Transactional
    public CapaResponse createCapa(CreateCapaRequest req, UUID userId, String correlationId) {
        NonConformance ncr = req.getNcrId() != null ? findNcr(req.getNcrId()) : null;
        String capaNumber = "CAPA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Capa capa = capaRepo.save(Capa.builder()
                .capaNumber(capaNumber).ncr(ncr)
                .title(req.getTitle()).correctiveAction(req.getCorrectiveAction())
                .preventiveAction(req.getPreventiveAction())
                .ownerUserId(req.getOwnerUserId() != null ? req.getOwnerUserId() : userId)
                .status(Capa.CapaStatus.OPEN).dueDate(req.getDueDate()).build());

        if (ncr != null) {
            ncr.setStatus(NonConformance.NcrStatus.PENDING_CAPA);
            ncrRepo.save(ncr);
        }
        publisher.publishCapaRaised(CapaRaisedEvent.builder()
                .capaId(capa.getCapaId()).capaNumber(capa.getCapaNumber())
                .ncrId(ncr != null ? ncr.getNcrId() : null)
                .productVersionId(ncr != null ? ncr.getProductVersionId() : null)
                .build(), correlationId);
        audit(AuditEntityType.CAPA, capa.getCapaId(), AuditAction.CREATED, userId, correlationId);
        return toCapaResponse(capa);
    }

    /**
     * Responsible team submits their corrective + preventive action plan for quality review.
     * OPEN → UNDER_REVIEW.
     * Clears any previous rejection reason.
     */
    @Transactional
    public CapaResponse submitCapa(UUID capaId, SubmitCapaRequest req, UUID userId, String correlationId) {
        Capa capa = findCapa(capaId);
        if (capa.getStatus() != Capa.CapaStatus.OPEN) {
            throw new IllegalStateException(
                    "Only an OPEN CAPA can be submitted for review. Current status: " + capa.getStatus());
        }
        capa.setCorrectiveAction(req.getCorrectiveAction());
        capa.setPreventiveAction(req.getPreventiveAction());
        capa.setDueDate(req.getDueDate());
        capa.setRejectionReason(null);   // clear previous rejection if any
        capa.setStatus(Capa.CapaStatus.UNDER_REVIEW);
        capaRepo.save(capa);
        audit(AuditEntityType.CAPA, capaId, AuditAction.UPDATED, userId, correlationId);
        log.info("CAPA submitted for review [capaId={}, submittedBy={}]", capaId, userId);
        return toCapaResponse(capa);
    }

    /**
     * Quality team reviews the submitted CAPA plan.
     *
     * approved=true  → UNDER_REVIEW → APPROVED
     *                  Responsible team can now execute the fix.
     * approved=false → UNDER_REVIEW → OPEN  (with rejectionReason)
     *                  Responsible team revises and resubmits.
     */
    @Transactional
    public CapaResponse reviewCapa(UUID capaId, ReviewCapaRequest req, UUID userId, String correlationId) {
        Capa capa = findCapa(capaId);
        if (capa.getStatus() != Capa.CapaStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only a CAPA UNDER_REVIEW can be reviewed. Current status: " + capa.getStatus());
        }
        if (!req.getApproved() && (req.getRejectionReason() == null || req.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("rejectionReason is required when rejecting a CAPA plan");
        }

        capa.setReviewedBy(userId);
        capa.setReviewedAt(OffsetDateTime.now());

        if (req.getApproved()) {
            capa.setStatus(Capa.CapaStatus.APPROVED);
            capa.setRejectionReason(null);
            log.info("CAPA approved [capaId={}, approvedBy={}]", capaId, userId);
        } else {
            capa.setStatus(Capa.CapaStatus.OPEN);   // back to OPEN for revision
            capa.setRejectionReason(req.getRejectionReason());
            log.info("CAPA rejected [capaId={}, rejectedBy={}, reason={}]",
                    capaId, userId, req.getRejectionReason());
        }
        capaRepo.save(capa);
        audit(AuditEntityType.CAPA, capaId, AuditAction.UPDATED, userId, correlationId);
        return toCapaResponse(capa);
    }

    /**
     * Close a CAPA after executing the fix and verifying its effectiveness.
     * Guard: CAPA must be in APPROVED state — it cannot be closed without quality approval.
     */
    @Transactional
    public CapaResponse closeCapa(UUID capaId, String effectivenessCheck, UUID userId, String correlationId) {
        Capa capa = findCapa(capaId);

        if (capa.getStatus() != Capa.CapaStatus.APPROVED) {
            throw new IllegalStateException(
                    "CAPA must be APPROVED by the quality team before it can be closed. Current status: "
                    + capa.getStatus());
        }

        capa.setStatus(Capa.CapaStatus.CLOSED);
        capa.setEffectivenessCheck(effectivenessCheck);
        capa.setEffectivenessVerified(true);
        capa.setClosedAt(OffsetDateTime.now());
        capaRepo.save(capa);

        UUID versionId = capa.getNcr() != null ? capa.getNcr().getProductVersionId() : null;
        publisher.publishCapaClosed(CapaClosedEvent.builder()
                .capaId(capaId).capaNumber(capa.getCapaNumber())
                .ncrId(capa.getNcr() != null ? capa.getNcr().getNcrId() : null)
                .productVersionId(versionId).build(), correlationId);

        audit(AuditEntityType.CAPA, capaId, AuditAction.CLOSED, userId, correlationId);
        log.info("CAPA closed [capaId={}, closedBy={}]", capaId, userId);
        return toCapaResponse(capa);
    }

    @Transactional(readOnly = true)
    public List<CapaResponse> listCapas(String status) {
        List<Capa> capas = status != null
                ? capaRepo.findByStatus(Capa.CapaStatus.valueOf(status))
                : capaRepo.findAll();
        return capas.stream().map(this::toCapaResponse).toList();
    }

    // ── QUALITY AUDIT ─────────────────────────────────────────

    @Transactional
    public QualityAuditResponse createAudit(CreateAuditRequest req, UUID userId, String correlationId) {
        String auditNumber = "QA-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        QualityAudit audit = auditRepo.save(QualityAudit.builder()
                .auditNumber(auditNumber).auditType(req.getAuditType())
                .scope(req.getScope()).scheduledDate(req.getScheduledDate())
                .status(QualityAudit.AuditStatus.PLANNED)
                .leadAuditorId(req.getLeadAuditorId()).build());
        audit(AuditEntityType.AUDIT, audit.getAuditId(), AuditAction.CREATED, userId, correlationId);
        return toAuditResponse(audit);
    }

    @Transactional
    public QualityAuditResponse completeAudit(UUID auditId, String summary, String courseCode,
                                               String affectedDept, UUID userId, String correlationId) {
        QualityAudit qa = auditRepo.findById(auditId)
                .orElseThrow(() -> new NoSuchElementException("Audit not found: " + auditId));
        qa.setStatus(QualityAudit.AuditStatus.COMPLETED);
        qa.setSummary(summary);
        qa.setConductedDate(java.time.LocalDate.now());
        auditRepo.save(qa);

        if (courseCode != null && !courseCode.isBlank()) {
            publisher.publishAuditFinding(AuditFindingEvent.builder()
                    .auditId(auditId).findingType("NONCONFORMITY")
                    .description(summary).courseCodeRequired(courseCode)
                    .affectedDepartment(affectedDept).build(), correlationId);
        }
        audit(AuditEntityType.AUDIT, auditId, AuditAction.COMPLETED, userId, correlationId);
        return toAuditResponse(qa);
    }

    @Transactional(readOnly = true)
    public List<QualityAuditResponse> listAudits(String status) {
        List<QualityAudit> audits = status != null
                ? auditRepo.findByStatus(QualityAudit.AuditStatus.valueOf(status))
                : auditRepo.findAll();
        return audits.stream().map(this::toAuditResponse).toList();
    }

    // ── RISK ─────────────────────────────────────────────────

    @Transactional
    public RiskResponse createRisk(CreateRiskRequest req, UUID userId, String correlationId) {
        String riskNumber = "RISK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        RiskRegister risk = riskRepo.save(RiskRegister.builder()
                .riskNumber(riskNumber).productVersionId(req.getProductVersionId())
                .title(req.getTitle()).description(req.getDescription())
                .category(req.getCategory()).likelihood(req.getLikelihood())
                .impact(req.getImpact()).mitigationPlan(req.getMitigationPlan())
                .ownerUserId(req.getOwnerUserId())
                .status(RiskRegister.RiskStatus.IDENTIFIED).build());
        risk.calcScore();
        riskRepo.save(risk);
        publisher.publishRiskRegistered(RiskRegisteredEvent.builder()
                .riskId(risk.getRiskId()).productVersionId(risk.getProductVersionId())
                .title(risk.getTitle()).category(risk.getCategory())
                .likelihood(risk.getLikelihood()).impact(risk.getImpact())
                .riskScore(risk.getRiskScore())
                .build(), correlationId);
        audit(AuditEntityType.RISK, risk.getRiskId(), AuditAction.CREATED, userId, correlationId);
        return toRiskResponse(risk);
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> listRisks() {
        return riskRepo.findAllByOrderByRiskScoreDesc().stream().map(this::toRiskResponse).toList();
    }

    // ── DOCUMENT CONTROL ─────────────────────────────────────

    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest req, UUID userId, String correlationId) {
        DocumentControl doc = docRepo.save(DocumentControl.builder()
                .docNumber(req.getDocNumber())
                .revision(req.getRevision() != null ? req.getRevision() : "A")
                .title(req.getTitle()).tdpRefId(req.getTdpRefId())
                .storageUrl(req.getStorageUrl()).documentHash(req.getDocumentHash())
                .approvalStatus(DocumentControl.DocStatus.DRAFT).build());
        audit(AuditEntityType.DOCUMENT, doc.getDocId(), AuditAction.CREATED, userId, correlationId);
        return toDocResponse(doc);
    }

    @Transactional
    public DocumentResponse approveDocument(UUID docId, UUID approverId, String correlationId) {
        DocumentControl doc = docRepo.findById(docId)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + docId));
        doc.setApprovalStatus(DocumentControl.DocStatus.APPROVED);
        doc.setApprovedBy(approverId);
        doc.setApprovedAt(OffsetDateTime.now());
        doc.setEffectiveDate(java.time.LocalDate.now());
        docRepo.save(doc);
        audit(AuditEntityType.DOCUMENT, docId, AuditAction.APPROVED, approverId, correlationId);
        return toDocResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(String status) {
        List<DocumentControl> docs = status != null
                ? docRepo.findByApprovalStatus(DocumentControl.DocStatus.valueOf(status))
                : docRepo.findAll();
        return docs.stream().map(this::toDocResponse).toList();
    }

    // ── EVENT HANDLING ────────────────────────────────────────

    @Transactional
    public void handlePhaseGateCheckRequested(PhaseGateCheckRequestedEvent event, String correlationId) {
        if (!idempotencyCheck("PGCR-QLM-" + event.getGateCorrelationId(), "cde.plm.phase.gate.check.requested")) return;

        NcrCheckResponse check = checkNcrsForVersion(event.getVersionId(), "CRITICAL");
        boolean passed = check.isClear();

        publisher.publishPhaseGateCheckResult(PhaseGateCheckResultEvent.builder()
                .gateCorrelationId(event.getGateCorrelationId())
                .versionId(event.getVersionId())
                .targetPhase(event.getTargetPhase())
                .checkerService("QLM")
                .passed(passed)
                .blockReason(passed ? null : check.getOpenCriticalCount() + " open critical NCR(s) blocking phase transition")
                .openCriticalNcrCount(check.getOpenCriticalCount())
                .build(), correlationId);

        log.info("Phase gate NCR check completed [versionId={}, targetPhase={}, passed={}, openCritical={}]",
                event.getVersionId(), event.getTargetPhase(), passed, check.getOpenCriticalCount());
    }

    @Transactional
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-USR-" + event.getUserId(), "cde.llm.user.profile.updated")) return;
        userShadowRepo.save(UserShadow.builder()
                .userId(event.getUserId()).employeeId(event.getEmployeeId())
                .fullName(event.getFullName()).role(event.getRole())
                .department(event.getDepartment()).lastSyncedAt(OffsetDateTime.now()).build());
    }

    // ── AUDIT HELPERS ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditLog(AuditEntityType entityType, UUID entityId) {
        return logRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType.name(), entityId).stream()
                .map(l -> AuditLogEntry.builder().logId(l.getLogId())
                        .entityType(l.getEntityType()).entityId(l.getEntityId())
                        .action(l.getAction()).performedBy(l.getPerformedBy())
                        .eventSource(l.getEventSource()).createdAt(l.getCreatedAt()).build()).toList();
    }

    private void audit(AuditEntityType type, UUID id, AuditAction action, UUID by, String correlationId) {
        try {
            logRepo.save(QualityAuditLog.builder().entityType(type.name()).entityId(id)
                    .action(action.name()).performedBy(by).eventSource("USER")
                    .correlationId(correlationId).build());
        } catch (Exception e) { log.warn("Audit log failed: {}", e.getMessage()); }
    }

    private boolean idempotencyCheck(String eventId, String type) {
        if (processedRepo.existsByEventId(eventId)) return false;
        processedRepo.save(ProcessedEvent.builder().eventId(eventId).eventType(type).build());
        return true;
    }

    // ── MAPPERS ───────────────────────────────────────────────

    private NonConformance findNcr(UUID id) {
        return ncrRepo.findById(id).orElseThrow(() -> new NoSuchElementException("NCR not found: " + id));
    }

    private Capa findCapa(UUID id) {
        return capaRepo.findById(id).orElseThrow(() -> new NoSuchElementException("CAPA not found: " + id));
    }

    private NcrResponse toNcrResponse(NonConformance n) {
        return NcrResponse.builder().ncrId(n.getNcrId()).ncrNumber(n.getNcrNumber())
                .productVersionId(n.getProductVersionId()).productCode(n.getProductCode())
                .severity(n.getSeverity().name()).title(n.getTitle()).description(n.getDescription())
                .rootCause(n.getRootCause()).status(n.getStatus().name())
                .reportedBy(n.getReportedBy()).assignedTo(n.getAssignedTo())
                .detectedAt(n.getDetectedAt()).closedAt(n.getClosedAt())
                .createdAt(n.getCreatedAt()).updatedAt(n.getUpdatedAt()).build();
    }

    private CapaResponse toCapaResponse(Capa c) {
        return CapaResponse.builder().capaId(c.getCapaId()).capaNumber(c.getCapaNumber())
                .ncrId(c.getNcr() != null ? c.getNcr().getNcrId() : null)
                .title(c.getTitle()).correctiveAction(c.getCorrectiveAction())
                .preventiveAction(c.getPreventiveAction()).ownerUserId(c.getOwnerUserId())
                .status(c.getStatus().name()).dueDate(c.getDueDate())
                .effectivenessCheck(c.getEffectivenessCheck())
                .effectivenessVerified(c.getEffectivenessVerified())
                .reviewedBy(c.getReviewedBy()).reviewedAt(c.getReviewedAt())
                .rejectionReason(c.getRejectionReason())
                .closedAt(c.getClosedAt()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }

    private QualityAuditResponse toAuditResponse(QualityAudit a) {
        return QualityAuditResponse.builder().auditId(a.getAuditId()).auditNumber(a.getAuditNumber())
                .auditType(a.getAuditType()).scope(a.getScope()).scheduledDate(a.getScheduledDate())
                .conductedDate(a.getConductedDate()).status(a.getStatus().name())
                .leadAuditorId(a.getLeadAuditorId()).summary(a.getSummary()).createdAt(a.getCreatedAt()).build();
    }

    private RiskResponse toRiskResponse(RiskRegister r) {
        return RiskResponse.builder().riskId(r.getRiskId()).riskNumber(r.getRiskNumber())
                .productVersionId(r.getProductVersionId()).title(r.getTitle())
                .description(r.getDescription()).category(r.getCategory())
                .likelihood(r.getLikelihood()).impact(r.getImpact()).riskScore(r.getRiskScore())
                .mitigationPlan(r.getMitigationPlan()).ownerUserId(r.getOwnerUserId())
                .status(r.getStatus().name()).createdAt(r.getCreatedAt()).build();
    }

    private DocumentResponse toDocResponse(DocumentControl d) {
        return DocumentResponse.builder().docId(d.getDocId()).docNumber(d.getDocNumber())
                .revision(d.getRevision()).title(d.getTitle()).tdpRefId(d.getTdpRefId())
                .storageUrl(d.getStorageUrl()).documentHash(d.getDocumentHash())
                .approvalStatus(d.getApprovalStatus().name()).approvedBy(d.getApprovedBy())
                .approvedAt(d.getApprovedAt()).effectiveDate(d.getEffectiveDate())
                .createdAt(d.getCreatedAt()).build();
    }
}
