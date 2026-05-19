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

@RestController @RequestMapping("/api/v1/plm/products")
@RequiredArgsConstructor @Slf4j
@Tag(name = "Products", description = "Product lifecycle management")
public class ProductController {
    private final ProductService productService;

    @PostMapping @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId) {
        return ResponseEntity.status(201).body(productService.createProduct(req, UUID.fromString(userId)));
    }

    @GetMapping @Operation(summary = "List all products")
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.listProducts());
    }

    @GetMapping("/{productId}") @Operation(summary = "Get product with versions")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    @GetMapping("/{productId}/versions") @Operation(summary = "List versions of a product")
    public ResponseEntity<List<ProductVersionResponse>> listVersions(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.listVersions(productId));
    }

    @PostMapping("/{productId}/versions") @Operation(summary = "Create a new product version")
    public ResponseEntity<ProductVersionResponse> createVersion(@PathVariable UUID productId,
            @Valid @RequestBody CreateVersionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "00000000-0000-0000-0000-000000000001") String userId) {
        return ResponseEntity.status(201).body(productService.createVersion(productId, req, UUID.fromString(userId)));
    }
}
