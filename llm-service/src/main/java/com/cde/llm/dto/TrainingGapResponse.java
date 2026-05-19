package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingGapResponse {
    UUID userId;
    String phase;
    List<String> missingCourseCodes;
    int gapCount;
}
