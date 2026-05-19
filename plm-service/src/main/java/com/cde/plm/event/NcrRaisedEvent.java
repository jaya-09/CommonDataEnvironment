package com.cde.plm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrRaisedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    private String severity;
    private String description;
    private UUID reportedBy;
}

// Consumed from LLM
