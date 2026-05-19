package com.cde.plm.controller;

import com.cde.plm.dto.*;
import com.cde.plm.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/plm/change-requests")
@RequiredArgsConstructor
@Tag(name = "Change Requests", description = "ECR / Change request management")
public class ChangeRequestController {
    private final ProductService productService;

    @PostMapping("/{crId}/submit") @Operation(summary = "Submit change request for review")
    public ResponseEntity<ChangeRequestResponse> submit(@PathVariable UUID crId,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.ok(productService.submitChangeRequest(crId, UUID.fromString(userId), correlationId));
    }

    @PostMapping("/{crId}/approve") @Operation(summary = "Approve or reject a change request")
    public ResponseEntity<ChangeRequestResponse> approve(@PathVariable UUID crId,
            @Valid @RequestBody ApprovalDecisionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String correlationId) {
        return ResponseEntity.ok(productService.approveChangeRequest(crId, UUID.fromString(userId),
                req.decision, req.comments, correlationId));
    }
}
