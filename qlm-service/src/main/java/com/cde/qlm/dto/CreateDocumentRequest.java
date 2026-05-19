package com.cde.qlm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateDocumentRequest {
    @NotBlank private String docNumber;
    @NotBlank private String title;
    private String revision;
    private String documentType;
    private UUID tdpRefId;
    private String storageUrl;
    private String documentHash;
}
