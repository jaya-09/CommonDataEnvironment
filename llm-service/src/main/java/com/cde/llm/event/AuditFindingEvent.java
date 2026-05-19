package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditFindingEvent {
    private UUID auditId;
    private UUID findingId;
    private String findingType;
    private String description;
    private String courseCodeRequired;
    private String affectedDepartment;
}
