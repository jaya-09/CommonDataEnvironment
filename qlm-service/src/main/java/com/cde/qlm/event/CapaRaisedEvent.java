package com.cde.qlm.event;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CapaRaisedEvent {
    private UUID capaId;
    private String capaNumber;
    private UUID ncrId;
    private UUID productVersionId;
}
