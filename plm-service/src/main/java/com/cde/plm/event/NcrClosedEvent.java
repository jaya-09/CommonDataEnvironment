package com.cde.plm.event;

import lombok.*;
import java.util.UUID;

/**
 * Published by QLM when an NCR is closed (all linked CAPAs resolved).
 * PLM uses this to lift a version off HOLD status if it was blocked by that NCR.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NcrClosedEvent {
    private UUID ncrId;
    private String ncrNumber;
    private UUID productVersionId;
    private String productCode;
}
