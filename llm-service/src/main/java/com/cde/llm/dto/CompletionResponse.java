package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionResponse {
    UUID enrollmentId;
    boolean passed;
    int score;
    UUID certId;
    String certNumber;
    String message;
}
