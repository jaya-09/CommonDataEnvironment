package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class VersionStatusChangedEvent {
    private UUID versionId;
    private String productCode;
    private String versionNumber;
    private String oldStatus;
    private String newStatus;
}
