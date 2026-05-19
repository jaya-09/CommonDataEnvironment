package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class CapaClosedEvent {
    private UUID capaId;
    private String capaNumber;
    private UUID ncrId;
    private UUID productVersionId;
    private String productCode;
    private OffsetDateTime closedAt;
}
