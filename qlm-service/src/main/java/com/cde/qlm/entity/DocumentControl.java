package com.cde.qlm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "document_control")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentControl {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "doc_id", updatable = false, nullable = false)
    private UUID docId;
    @Column(name = "doc_number", nullable = false, length = 100)
    private String docNumber;
    @Column(name = "revision", nullable = false, length = 20)
    private String revision = "A";
    @Column(name = "title", nullable = false, length = 300)
    private String title;
    @Column(name = "tdp_ref_id")
    private UUID tdpRefId;
    @Column(name = "storage_url")
    private String storageUrl;
    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private DocStatus approvalStatus = DocStatus.DRAFT;
    @Column(name = "approved_by")
    private UUID approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    public enum DocStatus { DRAFT, UNDER_REVIEW, APPROVED, OBSOLETE }
}
