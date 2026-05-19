package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseReadinessResponse {
    String phase;
    boolean ready;
    int uncertifiedCount;
    List<UserSummary> uncertifiedUsers;
}
