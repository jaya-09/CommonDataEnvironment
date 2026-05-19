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

@RestController @RequestMapping("/api/v1/qlm/capa")
@RequiredArgsConstructor @Tag(name = "CAPA", description = "Corrective and Preventive Actions")
public class CapaController {
    private final QlmService qlmService;

    @PostMapping @Operation(summary = "Create CAPA")
    public ResponseEntity<CapaResponse> create(@Valid @RequestBody CreateCapaRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createCapa(req, UUID.fromString(userId), cid));
    }

    @GetMapping @Operation(summary = "List CAPAs")
    public ResponseEntity<List<CapaResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(qlmService.listCapas(status));
    }

    @PostMapping("/{capaId}/close") @Operation(summary = "Close CAPA")
    public ResponseEntity<CapaResponse> close(@PathVariable UUID capaId,
            @RequestParam(required = false) String effectivenessCheck,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.closeCapa(capaId, effectivenessCheck, UUID.fromString(userId), cid));
    }
}
