package com.cde.plm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChangeRequestApprovedEvent {
    private UUID crId;
    private String crNumber;
    private String crType;
    private UUID versionId;
}

// Consumed from QLM
