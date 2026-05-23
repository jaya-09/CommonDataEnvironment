package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

/**
 * Published by QLM when a CAPA stub is auto-created for a MAJOR/CRITICAL NCR.
 * PLM receives this to notify the product team that a CAPA is waiting for their input.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CapaRaisedEvent {
    private UUID capaId;
    private String capaNumber;
    private UUID ncrId;
    private UUID productVersionId;
}
