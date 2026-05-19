package com.cde.llm.repository;

import com.cde.llm.entity.TrainingCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, UUID> {
    Optional<TrainingCourse> findByCourseCode(String courseCode);
    List<TrainingCourse> findByMandatoryForPhase(String phase);
    List<TrainingCourse> findByActiveTrue();
}
