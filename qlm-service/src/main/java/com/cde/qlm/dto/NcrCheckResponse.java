package com.cde.qlm.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrCheckResponse {
    private UUID versionId;
    private boolean clear;
    private long openCriticalCount;
}
