package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnrollmentTriggeredEvent {
    private UUID enrollmentId;
    private UUID userId;
    private UUID courseId;
    private String courseCode;
    private String triggerSource;
    private UUID triggerRefId;
    private String triggerRefType;
}
