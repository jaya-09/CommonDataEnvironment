package com.cde.plm.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateProductRequest {
    private String name;
    private String description;
    private String status;
}
