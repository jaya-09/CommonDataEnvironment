package com.cde.plm.controller;

import com.cde.plm.dto.LifecyclePhaseResponse;
import com.cde.plm.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/plm")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "Phases and reference data")
public class PlmReferenceController {
    private final ProductService productService;

    @GetMapping("/phases") @Operation(summary = "List all lifecycle phases in order")
    public ResponseEntity<List<LifecyclePhaseResponse>> phases() {
        return ResponseEntity.ok(productService.listPhases());
    }
}
