package com.cde.llm.controller;

import com.cde.llm.dto.*;
import com.cde.llm.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@Tag(name = "Training & Enrollment", description = "Enrollment management and certification")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enrollments")
    @Operation(summary = "Enroll a user in a training course (manual or event-triggered)")
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentRequest request,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(request, correlationId));
    }

    @PostMapping("/enrollments/{enrollmentId}/complete")
    @Operation(summary = "Complete training — issues certification if score passes. Publishes certification.granted event.")
    public ResponseEntity<CompletionResponse> completeTraining(
            @PathVariable java.util.UUID enrollmentId,
            @Valid @RequestBody CompleteTrainingRequest request,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        request.setEnrollmentId(enrollmentId);
        return ResponseEntity.ok(enrollmentService.completeTraining(request, correlationId));
    }

    @GetMapping("/phase-readiness/{phase}")
    @Operation(summary = "Check if all staff are certified for a given PLM phase. Called by PLM during phase gate.")
    public ResponseEntity<PhaseReadinessResponse> checkPhaseReadiness(@PathVariable String phase) {
        return ResponseEntity.ok(enrollmentService.checkPhaseReadiness(phase));
    }
}
