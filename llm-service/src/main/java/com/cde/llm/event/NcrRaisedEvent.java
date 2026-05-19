package com.cde.llm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrRaisedEvent {
    private UUID ncrId;
    private UUID productVersionId;
    private String severity;
    private String description;
    private UUID reportedBy;
    private String courseCodeRequired;
}
