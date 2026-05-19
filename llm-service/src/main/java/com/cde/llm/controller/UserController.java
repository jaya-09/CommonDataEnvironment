package com.cde.llm.controller;

import com.cde.llm.dto.*;
import com.cde.llm.service.EnrollmentService;
import com.cde.llm.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/llm/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User management — LLM is the source of truth for all users")
public class UserController {

    private final UserProfileService userProfileService;
    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "Create a new user profile")
    public ResponseEntity<UserProfileResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileService.createUser(request, correlationId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserProfileResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile (propagates to PLM and QLM via event)")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.ok(userProfileService.updateUser(userId, request, correlationId));
    }

    @GetMapping
    @Operation(summary = "List users, optionally filtered by role and/or department")
    public ResponseEntity<List<UserProfileResponse>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(userProfileService.listUsers(role, department));
    }

    @GetMapping("/{userId}/certifications")
    @Operation(summary = "Get all certifications for a user")
    public ResponseEntity<List<CertificationResponse>> getCertifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getUserCertifications(userId));
    }

    @GetMapping("/{userId}/training-gap")
    @Operation(summary = "Training gap analysis — which certs is this user missing for a given phase?")
    public ResponseEntity<TrainingGapResponse> getTrainingGap(
            @PathVariable UUID userId,
            @RequestParam String phase) {
        return ResponseEntity.ok(userProfileService.getTrainingGap(userId, phase));
    }
}
