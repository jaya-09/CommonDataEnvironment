package com.cde.qlm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Sent by the quality team when reviewing a submitted CAPA plan.
 * approved=true  → CAPA moves to APPROVED; responsible team may proceed with execution.
 * approved=false → CAPA returns to OPEN with rejectionReason; team must revise and resubmit.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewCapaRequest {

    @NotNull(message = "approved decision is required")
    private Boolean approved;

    /**
     * Required when approved=false. Explains what is wrong with the plan
     * so the responsible team knows what to change.
     */
    private String rejectionReason;
}
