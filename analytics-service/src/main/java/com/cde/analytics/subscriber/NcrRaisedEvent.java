package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class NcrRaisedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    private String productCode;
    private String severity;
    private String title;
    private String description;
    private UUID reportedBy;
    private String courseCodeRequired;
    private OffsetDateTime raisedAt;
}
