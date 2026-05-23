package com.cde.plm.dto;

import com.cde.plm.entity.ChangeRequest;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovalDecisionRequest {
    @NotNull public ChangeRequest.CrStatus decision;
    public String comments;
}
