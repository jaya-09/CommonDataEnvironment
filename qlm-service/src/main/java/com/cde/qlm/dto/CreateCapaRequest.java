package com.cde.qlm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCapaRequest {
    @NotNull private UUID ncrId;
    @NotBlank private String title;
    private String correctiveAction;
    private String preventiveAction;
    private UUID ownerUserId;
    private LocalDate dueDate;
}
