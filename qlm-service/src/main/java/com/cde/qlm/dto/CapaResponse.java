package com.cde.qlm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CapaResponse {
    private UUID capaId;
    private String capaNumber;
    private UUID ncrId;
    private String title;
    private String correctiveAction;
    private String preventiveAction;
    private UUID ownerUserId;
    private String status;
    private LocalDate dueDate;
    private String effectivenessCheck;
    private Boolean effectivenessVerified;
    private OffsetDateTime closedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
