package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "technical_data_package")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicalDataPackage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tdp_id", updatable = false, nullable = false)
    private UUID tdpId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ProductVersion version;
    @Column(name = "document_name", nullable = false, length = 300)
    private String documentName;
    @Column(name = "document_type", length = 50)
    private String documentType;
    @Column(name = "storage_url", nullable = false)
    private String storageUrl;
    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private TdpStatus approvalStatus = TdpStatus.PENDING;
    @Column(name = "approved_by")
    private UUID approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "uploaded_by")
    private UUID uploadedBy;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    public enum TdpStatus { PENDING, APPROVED, REJECTED }
}
