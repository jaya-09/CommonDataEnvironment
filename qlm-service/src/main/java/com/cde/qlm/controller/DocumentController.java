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

@RestController @RequestMapping("/api/v1/qlm/documents")
@RequiredArgsConstructor @Tag(name = "Document Control", description = "Controlled documents")
public class DocumentController {
    private final QlmService qlmService;

    @PostMapping @Operation(summary = "Register a controlled document")
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.status(201).body(qlmService.createDocument(req, UUID.fromString(userId), cid));
    }

    @GetMapping @Operation(summary = "List documents")
    public ResponseEntity<List<DocumentResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(qlmService.listDocuments(status));
    }

    @PostMapping("/{docId}/approve") @Operation(summary = "Approve a document")
    public ResponseEntity<DocumentResponse> approve(@PathVariable UUID docId,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId,
            @RequestHeader(value = "X-Correlation-Id", defaultValue = "") String cid) {
        return ResponseEntity.ok(qlmService.approveDocument(docId, UUID.fromString(userId), cid));
    }
}
