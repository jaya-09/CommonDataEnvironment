package com.cde.qlm.controller;

import com.cde.qlm.dto.*;
import com.cde.qlm.service.QlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/qlm/capa")
@RequiredArgsConstructor
@Tag(name = "CAPA", description = "Corrective and Preventive Actions")
public class CapaController {

    private final QlmService qlmService;

    @PostMapping
    @Operation(summary = "Manually create a CAPA (auto-created for MAJOR/CRITICAL NCRs)")
    public ResponseEntity<CapaResponse> create(
            @Valid @RequestBody CreateCapaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createCapa(req, UUID.fromString(userId), cid));
    }

    @GetMapping
    @Operation(summary = "List CAPAs, optionally filtered by status")
    public ResponseEntity<List<CapaResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(qlmService.listCapas(status));
    }

    /**
     * Responsible team (PLM / product owners) submits their corrective + preventive plan.
     * Fills in correctiveAction, preventiveAction, dueDate and moves CAPA to UNDER_REVIEW.
     */
    @PostMapping("/{capaId}/submit")
    @Operation(summary = "Submit CAPA plan for quality review (OPEN → UNDER_REVIEW)")
    public ResponseEntity<CapaResponse> submit(
            @PathVariable UUID capaId,
            @Valid @RequestBody SubmitCapaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.submitCapa(capaId, req, UUID.fromString(userId), cid));
    }

    /**
     * Quality team reviews the submitted plan.
     * approved=true  → APPROVED   (team may proceed with execution)
     * approved=false → OPEN       (returned with rejectionReason; team must revise)
     */
    @PostMapping("/{capaId}/review")
    @Operation(summary = "Quality team approves or rejects CAPA plan (UNDER_REVIEW → APPROVED | OPEN)")
    public ResponseEntity<CapaResponse> review(
            @PathVariable UUID capaId,
            @Valid @RequestBody ReviewCapaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.reviewCapa(capaId, req, UUID.fromString(userId), cid));
    }

    /**
     * Close a CAPA after the fix has been executed and effectiveness verified.
     * Requires CAPA to be in APPROVED state (quality team must have signed off first).
     * Once CAPA is CLOSED the linked NCR may be closed.
     */
    @PostMapping("/{capaId}/close")
    @Operation(summary = "Close CAPA after executing fix (APPROVED → CLOSED)")
    public ResponseEntity<CapaResponse> close(
            @PathVariable UUID capaId,
            @RequestParam(required = false) String effectivenessCheck,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.closeCapa(capaId, effectivenessCheck, UUID.fromString(userId), cid));
    }
}
