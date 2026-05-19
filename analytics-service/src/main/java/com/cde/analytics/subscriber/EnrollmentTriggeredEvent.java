package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class EnrollmentTriggeredEvent {
    private UUID enrollmentId;
    private UUID userId;
    private UUID courseId;
    private String courseCode;
    private String triggerSource;
    private UUID triggerRefId;
    private String triggerRefType;
}
