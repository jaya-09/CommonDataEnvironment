package com.cde.llm.controller;

import com.cde.llm.entity.TrainingCourse;
import com.cde.llm.repository.TrainingCourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only catalogue of training courses.
 * Primarily used by the frontend and flow-test scripts to discover courseId UUIDs
 * before creating enrollments (EnrollmentController requires courseId, not courseCode).
 */
@RestController
@RequestMapping("/api/v1/llm/courses")
@RequiredArgsConstructor
@Tag(name = "Training Courses", description = "Browse the training course catalogue")
public class TrainingCourseController {

    private final TrainingCourseRepository courseRepository;

    @GetMapping
    @Operation(summary = "List all active training courses")
    public ResponseEntity<List<Map<String, Object>>> listCourses(
            @RequestParam(required = false) String phase) {

        List<TrainingCourse> courses = phase != null
                ? courseRepository.findByMandatoryForPhase(phase)
                : courseRepository.findByActiveTrue();

        List<Map<String, Object>> result = courses.stream()
                .map(c -> Map.<String, Object>of(
                        "courseId",          c.getCourseId(),
                        "courseCode",        c.getCourseCode(),
                        "title",             c.getTitle(),
                        "mandatoryForPhase", c.getMandatoryForPhase() != null ? c.getMandatoryForPhase() : "",
                        "passingScore",      c.getPassingScore(),
                        "durationHours",     c.getDurationHours() != null ? c.getDurationHours() : 0
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get a single course by ID")
    public ResponseEntity<TrainingCourse> getCourse(@PathVariable UUID courseId) {
        return courseRepository.findById(courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
