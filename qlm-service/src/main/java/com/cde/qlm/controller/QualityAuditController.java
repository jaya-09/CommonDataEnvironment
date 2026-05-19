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

@RestController @RequestMapping("/api/v1/qlm/audits")
@RequiredArgsConstructor @Tag(name = "Quality Audits", description = "Audit management")
public class QualityAuditController {
    private final QlmService qlmService;

    @PostMapping @Operation(summary = "Create quality audit")
    public ResponseEntity<QualityAuditResponse> create(@Valid @RequestBody CreateAuditRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createAudit(req, UUID.fromString(userId), cid));
    }

    @GetMapping @Operation(summary = "List quality audits")
    public ResponseEntity<List<QualityAuditResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(qlmService.listAudits(status));
    }

    @PostMapping("/{auditId}/complete") @Operation(summary = "Complete audit")
    public ResponseEntity<QualityAuditResponse> complete(@PathVariable UUID auditId,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String affectedDept,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.completeAudit(auditId, summary, courseCode, affectedDept, UUID.fromString(userId), cid));
    }
}
