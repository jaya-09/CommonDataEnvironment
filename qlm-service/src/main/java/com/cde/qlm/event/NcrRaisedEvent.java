package com.cde.qlm.event;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrRaisedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    /** Denormalised from the NCR entity so consumers (Analytics) don't need to look it up. */
    private String productCode;
    private String title;
    private String severity;
    private String description;
    private UUID reportedBy;
    private String courseCodeRequired;
    private OffsetDateTime raisedAt;
}
