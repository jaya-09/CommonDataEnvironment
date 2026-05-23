package com.cde.plm.dto;

import com.cde.plm.entity.ChangeRequest;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateChangeRequestRequest {
    @NotBlank public String title;
    public String description;
    public String reason;
    public String impactAnalysis;
    public ChangeRequest.CrType crType;
}
