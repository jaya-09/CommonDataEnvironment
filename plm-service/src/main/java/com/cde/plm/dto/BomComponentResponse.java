package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BomComponentResponse {
    private UUID componentId;
    private UUID parentComponentId;
    private String componentCode;
    private String name;
    private String componentType;
    private Double quantity;
    private String unit;
    private String notes;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    public List<BomComponentResponse> children;
}
