package com.cde.plm.controller;

import com.cde.plm.dto.LifecyclePhaseResponse;
import com.cde.plm.entity.UserShadow;
import com.cde.plm.repository.UserShadowRepository;
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
    private final UserShadowRepository userShadowRepo;

    @GetMapping("/phases") @Operation(summary = "List all lifecycle phases in order")
    public ResponseEntity<List<LifecyclePhaseResponse>> phases() {
        return ResponseEntity.ok(productService.listPhases());
    }

    @GetMapping("/users") @Operation(summary = "List all known users for approver selection")
    public ResponseEntity<List<UserShadow>> users() {
        return ResponseEntity.ok(userShadowRepo.findAll());
    }
}
