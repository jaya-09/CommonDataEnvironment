package com.cde.qlm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

/**
 * Sent by the responsible team (PLM / product owners) when they have filled in
 * their corrective and preventive action plan and are ready for quality review.
 * Moves CAPA from OPEN → UNDER_REVIEW.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubmitCapaRequest {

    /** What will be done to fix the specific defect that caused this NCR. */
    @NotBlank(message = "correctiveAction is required")
    private String correctiveAction;

    /** What will be done to prevent this class of defect from recurring. */
    @NotBlank(message = "preventiveAction is required")
    private String preventiveAction;

    /** Target completion date for executing the plan. */
    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;
}
