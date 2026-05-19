package com.cde.llm.repository;

import com.cde.llm.entity.UserCertification;
import com.cde.llm.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCertificationRepository extends JpaRepository<UserCertification, UUID> {

    List<UserCertification> findByUser(UserProfile user);

    List<UserCertification> findByUserAndStatus(UserProfile user, UserCertification.CertStatus status);

    Optional<UserCertification> findByUserAndCourse_CourseIdAndStatus(
            UserProfile user, UUID courseId, UserCertification.CertStatus status);

    /**
     * Phase-readiness check: find all users who do NOT have an active cert
     * for courses mandatory for a given PLM phase.
     */
    @Query("""
        SELECT u FROM UserProfile u
        WHERE u.active = true
        AND NOT EXISTS (
            SELECT 1 FROM UserCertification c
            WHERE c.user = u
            AND c.status = 'ACTIVE'
            AND c.course.mandatoryForPhase = :phase
        )
        """)
    List<UserProfile> findUsersWithoutCertForPhase(@Param("phase") String phase);

    /** Find certs expiring within the given window — for alerting. */
    @Query("SELECT c FROM UserCertification c WHERE c.status = 'ACTIVE' AND c.expiresAt < :cutoff")
    List<UserCertification> findExpiringSoon(@Param("cutoff") OffsetDateTime cutoff);
}
