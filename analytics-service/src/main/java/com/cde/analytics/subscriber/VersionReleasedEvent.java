package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class VersionReleasedEvent {
    private UUID versionId;
    private String versionNumber;
    private UUID productId;
    private String productCode;
    private String productName;
    private OffsetDateTime releasedAt;
}
