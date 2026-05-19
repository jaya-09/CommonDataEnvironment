package com.cde.plm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {
    private UUID productId;
    private String productCode;
    private String name;
    private String description;
    private String status;
    public List<ProductVersionSummary> versions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
