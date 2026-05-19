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

@RestController @RequestMapping("/api/v1/qlm/risks")
@RequiredArgsConstructor @Tag(name = "Risk Register", description = "Risk management")
public class RiskController {
    private final QlmService qlmService;

    @PostMapping @Operation(summary = "Log a risk")
    public ResponseEntity<RiskResponse> create(@Valid @RequestBody CreateRiskRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createRisk(req, UUID.fromString(userId), cid));
    }

    @GetMapping @Operation(summary = "List all risks")
    public ResponseEntity<List<RiskResponse>> list() {
        return ResponseEntity.ok(qlmService.listRisks());
    }
}
