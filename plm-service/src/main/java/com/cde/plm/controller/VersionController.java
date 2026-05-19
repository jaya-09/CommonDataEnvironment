package com.cde.plm.controller;

import com.cde.plm.dto.*;
import com.cde.plm.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/plm/versions")
@RequiredArgsConstructor @Slf4j
@Tag(name = "Versions", description = "Version and phase management")
public class VersionController {
    private final ProductService productService;

    @GetMapping("/{versionId}") @Operation(summary = "Get version details")
    public ResponseEntity<ProductVersionResponse> get(@PathVariable UUID versionId) {
        return ResponseEntity.ok(productService.getVersion(versionId));
    }

    @PostMapping("/{versionId}/phase") @Operation(summary = "Advance lifecycle phase")
    public ResponseEntity<PhaseGateResult> advancePhase(@PathVariable UUID versionId,
            @Valid @RequestBody PhaseTransitionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.ok(productService.advancePhase(versionId, req, UUID.fromString(userId), correlationId));
    }

    @GetMapping("/{versionId}/change-requests") @Operation(summary = "List change requests")
    public ResponseEntity<List<ChangeRequestResponse>> listCRs(@PathVariable UUID versionId) {
        return ResponseEntity.ok(productService.listChangeRequests(versionId));
    }

    @PostMapping("/{versionId}/change-requests") @Operation(summary = "Create a change request")
    public ResponseEntity<ChangeRequestResponse> createCR(@PathVariable UUID versionId,
            @Valid @RequestBody CreateChangeRequestRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId) {
        return ResponseEntity.status(201).body(productService.createChangeRequest(versionId, req, UUID.fromString(userId)));
    }

    @PostMapping("/{versionId}/bom") @Operation(summary = "Add BOM component")
    public ResponseEntity<BomComponentResponse> addBom(@PathVariable UUID versionId,
            @Valid @RequestBody CreateBomComponentRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId) {
        return ResponseEntity.status(201).body(productService.addBomComponent(versionId, req, UUID.fromString(userId)));
    }

    @GetMapping("/{versionId}/bom") @Operation(summary = "Get full BOM tree")
    public ResponseEntity<List<BomComponentResponse>> getBom(@PathVariable UUID versionId) {
        return ResponseEntity.ok(productService.getBomTree(versionId));
    }

    @GetMapping("/{versionId}/audit") @Operation(summary = "Get audit trail")
    public ResponseEntity<List<AuditLogEntry>> getAudit(@PathVariable UUID versionId) {
        return ResponseEntity.ok(productService.getAuditLog("VERSION", versionId));
    }
}
