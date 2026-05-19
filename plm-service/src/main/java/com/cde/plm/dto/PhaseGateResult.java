package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseGateResult {
    public boolean canAdvance;
    public String blockReason;
    public String targetPhase;
    public boolean passed;
    public String fromPhase;
    public String toPhase;
    public Map<String, String> blockers;
    public List<String> warnings;
    public String message;
    public boolean certCheckPassed;
    public boolean ncrCheckPassed;
    public int uncertifiedCount;
    public List<String> openCriticalNcrs;
}
