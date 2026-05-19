package com.cde.qlm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrClosedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    private String productCode;
}
