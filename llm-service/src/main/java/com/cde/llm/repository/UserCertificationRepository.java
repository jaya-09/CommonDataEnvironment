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
     * Phase-readiness check: find all ACTIVE users who are missing AT LEAST ONE
     * active certification for a course mandatory for the given PLM phase.
     *
     * The previous query (NOT EXISTS any cert) was too permissive — a user with 2 out
     * of 3 required certs would not appear because they had at least one cert for the
     * phase, making them look "ready" when they weren't.
     *
     * Correct logic: for each active user, check whether there EXISTS any mandatory
     * course for the phase that the user does NOT have an active cert for. If yes,
     * the user is NOT ready and should appear in the result.
     */
    @Query("""
        SELECT DISTINCT u FROM UserProfile u
        WHERE u.active = true
        AND EXISTS (
            SELECT c FROM TrainingCourse c
            WHERE c.mandatoryForPhase = :phase
            AND c.active = true
            AND NOT EXISTS (
                SELECT cert FROM UserCertification cert
                WHERE cert.user = u
                AND cert.course = c
                AND cert.status = 'ACTIVE'
            )
        )
        """)
    List<UserProfile> findUsersWithoutCertForPhase(@Param("phase") String phase);

    /** Find certs expiring within the given window — for alerting. */
    @Query("SELECT c FROM UserCertification c WHERE c.status = 'ACTIVE' AND c.expiresAt < :cutoff")
    List<UserCertification> findExpiringSoon(@Param("cutoff") OffsetDateTime cutoff);
}
