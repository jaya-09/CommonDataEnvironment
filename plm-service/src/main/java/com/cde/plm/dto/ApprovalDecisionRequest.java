package com.cde.plm.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalDecisionRequest {
    @NotBlank @Pattern(regexp = "APPROVED|REJECTED") public String decision;
    public String comments;
}
