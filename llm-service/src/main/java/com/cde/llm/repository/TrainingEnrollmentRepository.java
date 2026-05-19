package com.cde.llm.repository;

import com.cde.llm.entity.TrainingCourse;
import com.cde.llm.entity.TrainingEnrollment;
import com.cde.llm.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, UUID> {

    List<TrainingEnrollment> findByUser(UserProfile user);

    List<TrainingEnrollment> findByUserAndStatus(UserProfile user, TrainingEnrollment.EnrollmentStatus status);

    Optional<TrainingEnrollment> findByUserAndCourseAndStatus(
            UserProfile user, TrainingCourse course, TrainingEnrollment.EnrollmentStatus status);

    boolean existsByUserAndCourseAndStatusIn(
            UserProfile user, TrainingCourse course, List<TrainingEnrollment.EnrollmentStatus> statuses);

    @Query("SELECT e FROM TrainingEnrollment e WHERE e.triggerRefId = :refId AND e.triggerRefType = :refType")
    List<TrainingEnrollment> findByTriggerRef(
            @Param("refId") UUID refId,
            @Param("refType") String refType);
}
