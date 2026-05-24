package com.cde.llm.repository;

import com.cde.llm.entity.TrainingCourse;
import com.cde.llm.entity.TrainingEnrollment;
import com.cde.llm.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, UUID> {

    /** Returns the active enrollment regardless of whether it is ENROLLED or IN_PROGRESS. */
    Optional<TrainingEnrollment> findFirstByUserAndCourseAndStatusIn(
            UserProfile user, TrainingCourse course, List<TrainingEnrollment.EnrollmentStatus> statuses);

    boolean existsByUserAndCourseAndStatusIn(
            UserProfile user, TrainingCourse course, List<TrainingEnrollment.EnrollmentStatus> statuses);
}
