package com.cde.plm.controller;

import com.cde.plm.dto.TdpResponse;
import com.cde.plm.entity.ProductVersion;
import com.cde.plm.entity.TechnicalDataPackage;
import com.cde.plm.repository.ProductVersionRepository;
import com.cde.plm.repository.TechnicalDataPackageRepository;
import com.cde.plm.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for Technical Data Package (TDP) document management.
 *
 * POST /plm/documents/upload  — upload a file for a given product version
 * GET  /plm/documents/{versionId} — list all documents for a version
 */
@Slf4j
@RestController
@RequestMapping("/plm/documents")
@RequiredArgsConstructor
public class PlmDocumentController {

    private final StorageService storageService;
    private final ProductVersionRepository versionRepository;
    private final TechnicalDataPackageRepository tdpRepository;

    // ── Upload ───────────────────────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<TdpResponse> uploadDocument(
            @RequestParam("versionId") UUID versionId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        ProductVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        byte[] bytes = file.getBytes();
        String hash = sha256Hex(bytes);
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";

        // Build a deterministic storage key:  versions/{versionId}/{timestamp}_{filename}
        String objectKey = "versions/" + versionId + "/" + System.currentTimeMillis() + "_" + originalFilename;

        String storageUrl = storageService.upload(
                objectKey,
                file.getInputStream(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize()
        );

        UUID uploaderId = extractUserId(jwt);

        TechnicalDataPackage tdp = TechnicalDataPackage.builder()
                .version(version)
                .documentName(originalFilename)
                .documentType(documentType)
                .storageUrl(storageUrl)
                .documentHash(hash)
                .fileSizeBytes(file.getSize())
                .approvalStatus(TechnicalDataPackage.TdpStatus.PENDING)
                .uploadedBy(uploaderId)
                .build();

        TechnicalDataPackage saved = tdpRepository.save(tdp);
        log.info("TDP saved: {} for version {}", saved.getTdpId(), versionId);

        return ResponseEntity.ok(toResponse(saved));
    }

    // ── List by version ──────────────────────────────────────────────────────

    @GetMapping("/{versionId}")
    public ResponseEntity<List<TdpResponse>> listDocuments(@PathVariable UUID versionId) {
        if (!versionRepository.existsById(versionId)) {
            return ResponseEntity.notFound().build();
        }
        List<TdpResponse> docs = tdpRepository.findByVersion_VersionId(versionId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(docs);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TdpResponse toResponse(TechnicalDataPackage tdp) {
        return TdpResponse.builder()
                .tdpId(tdp.getTdpId())
                .versionId(tdp.getVersion().getVersionId())
                .documentName(tdp.getDocumentName())
                .documentType(tdp.getDocumentType())
                .storageUrl(tdp.getStorageUrl())
                .documentHash(tdp.getDocumentHash())
                .fileSizeBytes(tdp.getFileSizeBytes())
                .approvalStatus(tdp.getApprovalStatus() != null ? tdp.getApprovalStatus().name() : null)
                .approvedBy(tdp.getApprovedBy())
                .approvedAt(tdp.getApprovedAt())
                .uploadedBy(tdp.getUploadedBy())
                .createdAt(tdp.getCreatedAt())
                .build();
    }

    private UUID extractUserId(Jwt jwt) {
        if (jwt == null) return null;
        try {
            String sub = jwt.getSubject();
            return sub != null ? UUID.fromString(sub) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }
}
