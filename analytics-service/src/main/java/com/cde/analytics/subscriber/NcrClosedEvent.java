package com.cde.analytics.subscriber;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class NcrClosedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    private String productCode;
}
