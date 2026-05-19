package com.cde.qlm.controller;

import com.cde.qlm.dto.*;
import com.cde.qlm.service.QlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/qlm/ncr")
@RequiredArgsConstructor @Slf4j
@Tag(name = "Non-Conformance", description = "NCR management")
public class NcrController {
    private final QlmService qlmService;

    @PostMapping @Operation(summary = "Raise an NCR")
    public ResponseEntity<NcrResponse> create(@Valid @RequestBody CreateNcrRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createNcr(req, UUID.fromString(userId), cid));
    }

    @GetMapping @Operation(summary = "List NCRs")
    public ResponseEntity<List<NcrResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(qlmService.listNcrs(status));
    }

    @GetMapping("/{ncrId}") @Operation(summary = "Get NCR by ID")
    public ResponseEntity<NcrResponse> get(@PathVariable UUID ncrId) {
        return ResponseEntity.ok(qlmService.getNcr(ncrId));
    }

    @PutMapping("/{ncrId}") @Operation(summary = "Update NCR")
    public ResponseEntity<NcrResponse> update(@PathVariable UUID ncrId, @RequestBody UpdateNcrRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.updateNcr(ncrId, req, UUID.fromString(userId), cid));
    }

    @GetMapping("/check") @Operation(summary = "Phase gate NCR check")
    public ResponseEntity<NcrCheckResponse> check(@RequestParam UUID versionId,
            @RequestParam(defaultValue = "CRITICAL") String severity) {
        return ResponseEntity.ok(qlmService.checkNcrsForVersion(versionId, severity));
    }
}
