package com.cde.qlm.service;

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
        String ncrNumber = "NCR-" + System.currentTimeMillis();
        NonConformance ncr = ncrRepo.save(NonConformance.builder()
                .ncrNumber(ncrNumber)
                .productVersionId(req.getProductVersionId())
                .productCode(req.getProductCode())
                .severity(req.getSeverity() != null
                        ? NonConformance.Severity.valueOf(req.getSeverity())
                        : NonConformance.Severity.MAJOR)
                .title(req.getTitle()).description(req.getDescription())
                .status(NonConformance.NcrStatus.OPEN)
                .reportedBy(userId).build());

        audit("NCR", ncr.getNcrId(), "CREATED", userId, "USER", correlationId);

        publisher.publishNcrRaised(NcrRaisedEvent.builder()
                .ncrId(ncr.getNcrId()).ncrNumber(ncrNumber)
                .productVersionId(req.getProductVersionId())
                .severity(ncr.getSeverity().name())
                .description(req.getDescription()).reportedBy(userId).build(), correlationId);

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

    @Transactional
    public NcrResponse updateNcr(UUID ncrId, UpdateNcrRequest req, UUID userId, String correlationId) {
        NonConformance ncr = findNcr(ncrId);
        if (req.getRootCause() != null) ncr.setRootCause(req.getRootCause());
        if (req.getStatus() != null) {
            ncr.setStatus(NonConformance.NcrStatus.valueOf(req.getStatus()));
            if (ncr.getStatus() == NonConformance.NcrStatus.CLOSED)
                ncr.setClosedAt(OffsetDateTime.now());
        }
        if (req.getAssignedTo() != null) ncr.setAssignedTo(req.getAssignedTo());
        ncrRepo.save(ncr);
        audit("NCR", ncrId, "UPDATED", userId, "USER", correlationId);
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

    @Transactional
    public CapaResponse createCapa(CreateCapaRequest req, UUID userId, String correlationId) {
        NonConformance ncr = req.getNcrId() != null ? findNcr(req.getNcrId()) : null;
        String capaNumber = "CAPA-" + System.currentTimeMillis();

        Capa capa = capaRepo.save(Capa.builder()
                .capaNumber(capaNumber).ncr(ncr)
                .title(req.getTitle()).correctiveAction(req.getCorrectiveAction())
                .preventiveAction(req.getPreventiveAction())
                .ownerUserId(req.getOwnerUserId())
                .status(Capa.CapaStatus.OPEN).dueDate(req.getDueDate()).build());

        if (ncr != null) {
            ncr.setStatus(NonConformance.NcrStatus.PENDING_CAPA);
            ncrRepo.save(ncr);
        }
        audit("CAPA", capa.getCapaId(), "CREATED", userId, "USER", correlationId);
        return toCapaResponse(capa);
    }

    @Transactional
    public CapaResponse closeCapa(UUID capaId, String effectivenessCheck, UUID userId, String correlationId) {
        Capa capa = capaRepo.findById(capaId)
                .orElseThrow(() -> new NoSuchElementException("CAPA not found: " + capaId));
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

        audit("CAPA", capaId, "CLOSED", userId, "USER", correlationId);
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
        String auditNumber = "QA-" + System.currentTimeMillis();
        QualityAudit audit = auditRepo.save(QualityAudit.builder()
                .auditNumber(auditNumber).auditType(req.getAuditType())
                .scope(req.getScope()).scheduledDate(req.getScheduledDate())
                .status(QualityAudit.AuditStatus.PLANNED)
                .leadAuditorId(req.getLeadAuditorId()).build());
        audit("AUDIT", audit.getAuditId(), "CREATED", userId, "USER", correlationId);
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
        audit("AUDIT", auditId, "COMPLETED", userId, "USER", correlationId);
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
        String riskNumber = "RISK-" + System.currentTimeMillis();
        RiskRegister risk = riskRepo.save(RiskRegister.builder()
                .riskNumber(riskNumber).productVersionId(req.getProductVersionId())
                .title(req.getTitle()).description(req.getDescription())
                .category(req.getCategory()).likelihood(req.getLikelihood())
                .impact(req.getImpact()).mitigationPlan(req.getMitigationPlan())
                .ownerUserId(req.getOwnerUserId())
                .status(RiskRegister.RiskStatus.IDENTIFIED).build());
        audit("RISK", risk.getRiskId(), "CREATED", userId, "USER", correlationId);
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
        audit("DOCUMENT", doc.getDocId(), "CREATED", userId, "USER", correlationId);
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
        audit("DOCUMENT", docId, "APPROVED", approverId, "USER", correlationId);
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
    public void handleUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        if (!idempotencyCheck(correlationId + "-USR-" + event.getUserId(), "cde.llm.user.profile_updated")) return;
        userShadowRepo.save(UserShadow.builder()
                .userId(event.getUserId()).employeeId(event.getEmployeeId())
                .fullName(event.getFullName()).role(event.getRole())
                .department(event.getDepartment()).lastSyncedAt(OffsetDateTime.now()).build());
    }

    // ── AUDIT HELPERS ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditLog(String entityType, UUID entityId) {
        return logRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId).stream()
                .map(l -> AuditLogEntry.builder().logId(l.getLogId())
                        .entityType(l.getEntityType()).entityId(l.getEntityId())
                        .action(l.getAction()).performedBy(l.getPerformedBy())
                        .eventSource(l.getEventSource()).createdAt(l.getCreatedAt()).build()).toList();
    }

    private void audit(String type, UUID id, String action, UUID by, String source, String correlationId) {
        try {
            logRepo.save(QualityAuditLog.builder().entityType(type).entityId(id)
                    .action(action).performedBy(by).eventSource(source)
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
