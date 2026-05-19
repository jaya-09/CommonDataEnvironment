package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VersionStatusChangedEvent {
    private UUID versionId;
    private String productCode;
    private String versionNumber;
    private String oldStatus;
    private String newStatus;
}
