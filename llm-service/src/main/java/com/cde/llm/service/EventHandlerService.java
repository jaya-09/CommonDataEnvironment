package com.cde.llm.service;

import com.cde.llm.dto.EnrollmentRequest;
import com.cde.llm.entity.TrainingCourse;
import com.cde.llm.entity.UserProfile;
import com.cde.llm.event.EventEnvelope;
import com.cde.llm.event.PhaseGateCheckRequestedEvent;
import com.cde.llm.event.PhaseGateCheckResultEvent;
import com.cde.llm.event.EventPublisher;

import com.cde.llm.repository.ProcessedEventRepository;
import com.cde.llm.repository.TrainingCourseRepository;
import com.cde.llm.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Routes incoming Pub/Sub events to the correct handler.
 * Implements idempotency: each event is only processed once.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventHandlerService {

    private final ProcessedEventRepository processedEventRepository;
    private final EnrollmentService enrollmentService;
    private final UserProfileRepository userProfileRepository;
    private final TrainingCourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(EventEnvelope envelope) {
        // ── IDEMPOTENCY CHECK ──
        if (processedEventRepository.existsByEventId(envelope.getEventId())) {
            log.info("Event already processed, skipping [eventId={}]", envelope.getEventId());
            return;
        }

        // ── ROUTE ──
        switch (envelope.getType()) {
            case "cde.plm.phase.transitioned"          -> handlePhaseTransitioned(envelope);
            case "cde.qlm.ncr.raised"                  -> handleNcrRaised(envelope);
            case "cde.qlm.audit.finding"               -> handleAuditFinding(envelope);
            case "cde.plm.phase.gate.check.requested"  -> handlePhaseGateCheckRequested(envelope);
            default -> log.warn("No handler for event type: {}", envelope.getType());
        }

        // ── MARK PROCESSED ──
        processedEventRepository.save(com.cde.llm.entity.ProcessedEvent.builder().eventId(envelope.getEventId()).eventType(envelope.getType()).processedAt(java.time.OffsetDateTime.now()).build());
    }

    /**
     * PLM phase transitioned → auto-enroll users who lack the mandatory cert for the new phase.
     */
    private void handlePhaseTransitioned(EventEnvelope envelope) {
        var payload = objectMapper.convertValue(envelope.getPayload(), java.util.Map.class);
        String toPhase = (String) payload.get("toPhase");

        log.info("Handling phase.transitioned → toPhase={}", toPhase);

        // Find all courses mandatory for this phase
        List<TrainingCourse> mandatoryCourses = courseRepository.findByMandatoryForPhase(toPhase);
        if (mandatoryCourses.isEmpty()) {
            log.info("No mandatory courses for phase: {}", toPhase);
            return;
        }

        // Find all active engineers who need these courses
        List<UserProfile> engineers = userProfileRepository
                .findByRoleAndActiveTrue(UserProfile.Role.ENGINEER);

        for (UserProfile engineer : engineers) {
            for (TrainingCourse course : mandatoryCourses) {
                try {
                    EnrollmentRequest req = EnrollmentRequest.builder()
                            .userId(engineer.getUserId())
                            .courseId(course.getCourseId())
                            .triggerSource("PUBSUB")
                            .triggerRefType("PHASE_TRANSITION")
                            .triggerRefId(UUID.fromString(payload.get("versionId").toString()))
                            .build();
                    enrollmentService.enroll(req, envelope.getCorrelationId());
                } catch (Exception e) {
                    log.error("Failed to auto-enroll user {} for course {}: {}",
                            engineer.getUserId(), course.getCourseId(), e.getMessage());
                }
            }
        }
    }

    /**
     * NCR raised → auto-enroll affected team in corrective training.
     */
    private void handleNcrRaised(EventEnvelope envelope) {
        var payload = objectMapper.convertValue(envelope.getPayload(), java.util.Map.class);
        String courseCode = (String) payload.get("courseCodeRequired");

        if (courseCode == null) {
            log.info("NCR event has no courseCodeRequired, skipping auto-enrollment");
            return;
        }

        courseRepository.findByCourseCode(courseCode).ifPresent(course -> {
            List<UserProfile> engineers = userProfileRepository
                    .findByRoleAndActiveTrue(UserProfile.Role.ENGINEER);

            for (UserProfile engineer : engineers) {
                try {
                    EnrollmentRequest req = EnrollmentRequest.builder()
                            .userId(engineer.getUserId())
                            .courseId(course.getCourseId())
                            .triggerSource("PUBSUB")
                            .triggerRefType("NCR")
                            .triggerRefId(UUID.fromString(payload.get("ncrId").toString()))
                            .build();
                    enrollmentService.enroll(req, envelope.getCorrelationId());
                } catch (Exception e) {
                    log.error("Failed to auto-enroll user {} for NCR-triggered course {}: {}",
                            engineer.getUserId(), course.getCourseId(), e.getMessage());
                }
            }
        });
    }

    private void handleAuditFinding(EventEnvelope envelope) {
        var payload = objectMapper.convertValue(envelope.getPayload(), java.util.Map.class);
        String courseCode = (String) payload.get("courseCodeRequired");
        String affectedDept = (String) payload.get("affectedDepartment");

        if (courseCode == null) return;

        courseRepository.findByCourseCode(courseCode).ifPresent(course -> {
            List<UserProfile> targets = affectedDept != null
                    ? userProfileRepository.findByDepartmentAndActiveTrue(affectedDept)
                    : userProfileRepository.findByRoleAndActiveTrue(UserProfile.Role.ENGINEER);

            for (UserProfile user : targets) {
                try {
                    EnrollmentRequest req = EnrollmentRequest.builder()
                            .userId(user.getUserId())
                            .courseId(course.getCourseId())
                            .triggerSource("PUBSUB")
                            .triggerRefType("AUDIT_FINDING")
                            .triggerRefId(UUID.fromString(payload.get("findingId").toString()))
                            .build();
                    enrollmentService.enroll(req, envelope.getCorrelationId());
                } catch (Exception e) {
                    log.error("Failed to auto-enroll user {} for audit-finding-triggered course {}: {}",
                            user.getUserId(), course.getCourseId(), e.getMessage());
                }
            }
        });
    }

    /**
     * PLM phase gate check request → evaluate certification readiness for target phase.
     * Publishes result back on cde.plm.phase.gate.check.result.
     */
    private void handlePhaseGateCheckRequested(EventEnvelope envelope) {
        PhaseGateCheckRequestedEvent event =
                objectMapper.convertValue(envelope.getPayload(), PhaseGateCheckRequestedEvent.class);

        com.cde.llm.dto.PhaseReadinessResponse readiness =
                enrollmentService.checkPhaseReadiness(event.getTargetPhase());

        boolean passed = readiness.isReady();
        String blockReason = passed ? null
                : readiness.getUncertifiedCount() + " staff member(s) missing certifications for phase " + event.getTargetPhase();

        PhaseGateCheckResultEvent result = PhaseGateCheckResultEvent.builder()
                .gateCorrelationId(event.getGateCorrelationId())
                .versionId(event.getVersionId())
                .targetPhase(event.getTargetPhase())
                .checkerService("LLM")
                .passed(passed)
                .blockReason(blockReason)
                .uncertifiedCount(readiness.getUncertifiedCount())
                .build();

        eventPublisher.publishPhaseGateCheckResult(result, envelope.getCorrelationId());

        log.info("Phase gate certification check completed [versionId={}, targetPhase={}, passed={}, uncertified={}]",
                event.getVersionId(), event.getTargetPhase(), passed, readiness.getUncertifiedCount());
    }
}
