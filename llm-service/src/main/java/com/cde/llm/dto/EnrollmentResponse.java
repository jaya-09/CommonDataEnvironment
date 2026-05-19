package com.cde.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentResponse {
    UUID enrollmentId;
    UUID userId;
    UUID courseId;
    String courseTitle;
    String status;
    String triggerSource;
    LocalDate deadline;
    OffsetDateTime createdAt;
}
