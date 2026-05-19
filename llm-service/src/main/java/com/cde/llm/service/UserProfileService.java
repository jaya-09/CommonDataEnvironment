package com.cde.llm.service;

import com.cde.llm.audit.AuditAction;
import com.cde.llm.audit.AuditEntityType;
import com.cde.llm.dto.*;
import com.cde.llm.entity.UserCertification;
import com.cde.llm.entity.UserProfile;
import com.cde.llm.event.EventPublisher;
import com.cde.llm.event.UserProfileUpdatedEvent;
import com.cde.llm.repository.TrainingCourseRepository;
import com.cde.llm.repository.UserCertificationRepository;
import com.cde.llm.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserCertificationRepository certificationRepository;
    private final TrainingCourseRepository courseRepository;
    private final EventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request, String correlationId) {
        log.info("Creating user [employeeId={}, correlationId={}]", request.getEmployeeId(), correlationId);

        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (userProfileRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Employee ID already registered: " + request.getEmployeeId());
        }

        UserProfile user = UserProfile.builder()
                .employeeId(request.getEmployeeId())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(UserProfile.Role.valueOf(request.getRole()))
                .department(request.getDepartment())
                .competencyLevel(UserProfile.CompetencyLevel.valueOf(
                        request.getCompetencyLevel() != null ? request.getCompetencyLevel() : "JUNIOR"))
                .active(true)
                .build();

        user = userProfileRepository.save(user);
        auditLogService.log(AuditEntityType.USER_PROFILE, user.getUserId(), AuditAction.CREATED, null,
                mapToResponse(user), null, "USER", correlationId);

        // Publish to PLM and QLM so they can cache the user shadow copy
        eventPublisher.publishUserProfileUpdated(
                buildUpdateEvent(user, "CREATED"), correlationId);

        log.info("User created [userId={}, correlationId={}]", user.getUserId(), correlationId);
        return mapToResponse(user);
    }

    @Cacheable(value = "user-profiles", key = "#userId")
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(UUID userId) {
        return userProfileRepository.findById(userId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @CacheEvict(value = "user-profiles", key = "#userId")
    @Transactional
    public UserProfileResponse updateUser(UUID userId, UpdateUserRequest request, String correlationId) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Object oldValue = mapToResponse(user);

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getRole() != null) user.setRole(UserProfile.Role.valueOf(request.getRole()));
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getCompetencyLevel() != null)
            user.setCompetencyLevel(UserProfile.CompetencyLevel.valueOf(request.getCompetencyLevel()));
        user.setUpdatedAt(OffsetDateTime.now());

        user = userProfileRepository.save(user);
        auditLogService.log(AuditEntityType.USER_PROFILE, user.getUserId(), AuditAction.UPDATED,
                oldValue, mapToResponse(user), null, "USER", correlationId);

        // Propagate changes to PLM and QLM via event
        eventPublisher.publishUserProfileUpdated(buildUpdateEvent(user, "UPDATED"), correlationId);

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listUsers(String role, String department) {
        if (role != null && department != null) {
            return userProfileRepository
                    .findActiveByRoleAndDepartment(UserProfile.Role.valueOf(role), department)
                    .stream().map(this::mapToResponse).toList();
        }
        if (role != null) {
            return userProfileRepository
                    .findByRoleAndActiveTrue(UserProfile.Role.valueOf(role))
                    .stream().map(this::mapToResponse).toList();
        }
        return userProfileRepository.findAll()
                .stream().map(this::mapToResponse).toList();
    }

    private UserProfileUpdatedEvent buildUpdateEvent(UserProfile user, String changeType) {
        return UserProfileUpdatedEvent.builder()
                .userId(user.getUserId())
                .employeeId(user.getEmployeeId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .competencyLevel(user.getCompetencyLevel().name())
                .changeType(changeType)
                .build();
    }

    public UserProfileResponse mapToResponse(UserProfile user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .employeeId(user.getEmployeeId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .competencyLevel(user.getCompetencyLevel().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CertificationResponse> getUserCertifications(UUID userId) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return certificationRepository.findByUser(user).stream()
                .map(c -> CertificationResponse.builder()
                        .certId(c.getCertId())
                        .certNumber(c.getCertNumber())
                        .courseCode(c.getCourse().getCourseCode())
                        .courseTitle(c.getCourse().getTitle())
                        .score(c.getScore())
                        .status(c.getStatus().name())
                        .issuedAt(c.getIssuedAt())
                        .expiresAt(c.getExpiresAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingGapResponse getTrainingGap(UUID userId, String phase) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        var mandatoryCourses = courseRepository.findByMandatoryForPhase(phase);
        var activeCerts = certificationRepository.findByUserAndStatus(user, UserCertification.CertStatus.ACTIVE);
        var certifiedCourseIds = activeCerts.stream()
                .map(c -> c.getCourse().getCourseId()).collect(java.util.stream.Collectors.toSet());
        var missing = mandatoryCourses.stream()
                .filter(c -> !certifiedCourseIds.contains(c.getCourseId()))
                .map(com.cde.llm.entity.TrainingCourse::getCourseCode)
                .toList();
        return TrainingGapResponse.builder()
                .userId(userId)
                .phase(phase)
                .missingCourseCodes(missing)
                .gapCount(missing.size())
                .build();
    }
}
