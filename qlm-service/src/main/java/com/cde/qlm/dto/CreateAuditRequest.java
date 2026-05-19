package com.cde.qlm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateAuditRequest {
    @NotBlank private String auditType;
    @NotBlank private String scope;
    private String description;
    private LocalDate scheduledDate;
    private UUID leadAuditorId;
}
