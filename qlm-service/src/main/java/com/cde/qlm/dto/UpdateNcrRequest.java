package com.cde.qlm.dto;

import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateNcrRequest {
    private String status;
    private String rootCause;
    private UUID assignedTo;
}
