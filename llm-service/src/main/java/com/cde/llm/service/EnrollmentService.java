package com.cde.llm.service;

import com.cde.llm.audit.AuditAction;
import com.cde.llm.audit.AuditEntityType;
import com.cde.llm.dto.*;
import com.cde.llm.entity.*;
import com.cde.llm.event.CertificationGrantedEvent;
import com.cde.llm.event.EnrollmentTriggeredEvent;
import com.cde.llm.event.EventPublisher;
import com.cde.llm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {

    private final TrainingEnrollmentRepository enrollmentRepository;
    private final UserCertificationRepository certificationRepository;
    private final UserProfileRepository userProfileRepository;
    private final TrainingCourseRepository courseRepository;
    private final EventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    /**
     * Enroll a user in a course.
     * Can be called manually (from API) or automatically (from Pub/Sub event).
     *
     * REQUIRES_NEW so that when called from EventHandlerService (which is @Transactional),
     * each enrollment runs in its own independent transaction. A failure on one user
     * won't roll back successful enrollments already committed for other users.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnrollmentResponse enroll(EnrollmentRequest request, String correlationId) {
        UserProfile user = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        TrainingCourse course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));

        // Idempotency: don't enroll if already active
        boolean alreadyActive = enrollmentRepository.existsByUserAndCourseAndStatusIn(
                user, course,
                List.of(TrainingEnrollment.EnrollmentStatus.ENROLLED,
                        TrainingEnrollment.EnrollmentStatus.IN_PROGRESS));

        if (alreadyActive) {
            log.info("User {} already has active enrollment for course {}. Skipping.",
                    user.getUserId(), course.getCourseId());
            // Must check both ENROLLED and IN_PROGRESS — existsByUserAndCourseAndStatusIn
            // matches either status, so findByUserAndCourseAndStatus(ENROLLED) would throw
            // a NoSuchElementException when the enrollment is actually IN_PROGRESS.
            return enrollmentRepository.findFirstByUserAndCourseAndStatusIn(
                            user, course,
                            List.of(TrainingEnrollment.EnrollmentStatus.ENROLLED,
                                    TrainingEnrollment.EnrollmentStatus.IN_PROGRESS))
                    .map(this::mapToEnrollmentResponse)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Active enrollment not found for user " + user.getUserId()
                            + " and course " + course.getCourseId()));
        }

        TrainingEnrollment enrollment = TrainingEnrollment.builder()
                .user(user)
                .course(course)
                .status(TrainingEnrollment.EnrollmentStatus.ENROLLED)
                .triggerSource(request.getTriggerSource() != null
                        ? TrainingEnrollment.TriggerSource.valueOf(request.getTriggerSource())
                        : TrainingEnrollment.TriggerSource.MANUAL)
                .triggerRefId(request.getTriggerRefId())
                .triggerRefType(request.getTriggerRefType())
                .deadline(request.getDeadline())
                .build();

        enrollment = enrollmentRepository.save(enrollment);
        eventPublisher.publishEnrollmentTriggered(EnrollmentTriggeredEvent.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .userId(enrollment.getUser().getUserId())
                .courseId(enrollment.getCourse().getCourseId())
                .courseCode(enrollment.getCourse().getCourseCode())
                .triggerSource(enrollment.getTriggerSource().name())
                .triggerRefId(enrollment.getTriggerRefId())
                .triggerRefType(enrollment.getTriggerRefType())
                .build(), correlationId);
        auditLogService.log(AuditEntityType.ENROLLMENT, enrollment.getEnrollmentId(), AuditAction.ENROLLED,
                null, mapToEnrollmentResponse(enrollment),
                user.getUserId(), "USER", correlationId);

        log.info("User {} enrolled in course {} [trigger={}, refId={}]",
                user.getUserId(), course.getCourseId(),
                enrollment.getTriggerSource(), enrollment.getTriggerRefId());

        return mapToEnrollmentResponse(enrollment);
    }

    /**
     * Complete training and issue certification if score >= passing score.
     * Publishes cde.llm.certification.granted event on success.
     */
    @CacheEvict(value = "cert-status", allEntries = true)
    @Transactional
    public CompletionResponse completeTraining(CompleteTrainingRequest request, String correlationId) {
        TrainingEnrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + request.getEnrollmentId()));

        if (enrollment.getStatus() == TrainingEnrollment.EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Enrollment already completed");
        }

        enrollment.setScore(request.getScore());
        enrollment.setCompletedAt(OffsetDateTime.now());
        enrollment.setUpdatedAt(OffsetDateTime.now());

        boolean passed = request.getScore() >= enrollment.getCourse().getPassingScore();
        enrollment.setStatus(passed
                ? TrainingEnrollment.EnrollmentStatus.COMPLETED
                : TrainingEnrollment.EnrollmentStatus.FAILED);

        enrollmentRepository.save(enrollment);

        AuditAction enrollmentAction = passed ? AuditAction.COMPLETED : AuditAction.FAILED;
        auditLogService.log(AuditEntityType.ENROLLMENT, enrollment.getEnrollmentId(), enrollmentAction,
                null, mapToEnrollmentResponse(enrollment),
                enrollment.getUser().getUserId(), "USER", correlationId);

        if (passed) {
            // Issue certification
            String certNumber = generateCertNumber(enrollment);
            UserCertification cert = UserCertification.builder()
                    .user(enrollment.getUser())
                    .course(enrollment.getCourse())
                    .enrollment(enrollment)
                    .certNumber(certNumber)
                    .score(request.getScore())
                    .status(UserCertification.CertStatus.ACTIVE)
                    .issuedAt(OffsetDateTime.now())
                    .expiresAt(OffsetDateTime.now().plusYears(2))  // 2-year validity
                    .build();

            cert = certificationRepository.save(cert);
            auditLogService.log(AuditEntityType.CERTIFICATION, cert.getCertId(), AuditAction.ISSUED,
                    null, cert, enrollment.getUser().getUserId(), "USER", correlationId);

            // Publish event — PLM will use this to evaluate phase gate
            CertificationGrantedEvent event = CertificationGrantedEvent.builder()
                    .certId(cert.getCertId())
                    .userId(cert.getUser().getUserId())
                    .employeeId(cert.getUser().getEmployeeId())
                    .courseId(cert.getCourse().getCourseId())
                    .courseCode(cert.getCourse().getCourseCode())
                    .mandatoryForPhase(cert.getCourse().getMandatoryForPhase())
                    .score(cert.getScore())
                    .issuedAt(cert.getIssuedAt())
                    .expiresAt(cert.getExpiresAt())
                    .build();

            eventPublisher.publishCertificationGranted(event, correlationId);
            log.info("Certification issued [certId={}, userId={}, courseCode={}]",
                    cert.getCertId(), cert.getUser().getUserId(), cert.getCourse().getCourseCode());

            return CompletionResponse.builder()
                    .enrollmentId(enrollment.getEnrollmentId())
                    .passed(true)
                    .score(request.getScore())
                    .certId(cert.getCertId())
                    .certNumber(certNumber)
                    .build();
        }

        return CompletionResponse.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .passed(false)
                .score(request.getScore())
                .build();
    }

    /**
     * Phase-readiness check — called by PLM (via REST) during phase gate evaluation.
     * Returns list of users who lack required certs for the given phase.
     */
    @Cacheable(value = "cert-status", key = "#phase")
    @Transactional(readOnly = true)
    public PhaseReadinessResponse checkPhaseReadiness(String phase) {
        List<UserProfile> uncertifiedUsers =
                certificationRepository.findUsersWithoutCertForPhase(phase);

        return PhaseReadinessResponse.builder()
                .phase(phase)
                .ready(uncertifiedUsers.isEmpty())
                .uncertifiedCount(uncertifiedUsers.size())
                .uncertifiedUsers(uncertifiedUsers.stream()
                        .map(u -> UserSummary.builder()
                                .userId(u.getUserId())
                                .employeeId(u.getEmployeeId())
                                .fullName(u.getFullName())
                                .build())
                        .toList())
                .build();
    }

    private String generateCertNumber(TrainingEnrollment enrollment) {
        return String.format("CDE-CERT-%s-%s-%d",
                enrollment.getCourse().getCourseCode(),
                enrollment.getUser().getEmployeeId(),
                System.currentTimeMillis());
    }

    private EnrollmentResponse mapToEnrollmentResponse(TrainingEnrollment e) {
        return EnrollmentResponse.builder()
                .enrollmentId(e.getEnrollmentId())
                .userId(e.getUser().getUserId())
                .courseId(e.getCourse().getCourseId())
                .courseTitle(e.getCourse().getTitle())
                .status(e.getStatus().name())
                .triggerSource(e.getTriggerSource().name())
                .deadline(e.getDeadline())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
