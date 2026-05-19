package com.cde.qlm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "non_conformance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NonConformance {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ncr_id", updatable = false, nullable = false)
    private UUID ncrId;
    @Column(name = "ncr_number", nullable = false, unique = true, length = 50)
    private String ncrNumber;
    @Column(name = "product_version_id")
    private UUID productVersionId;
    @Column(name = "product_code", length = 50)
    private String productCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity = Severity.MAJOR;
    @Column(name = "title", nullable = false, length = 300)
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "root_cause")
    private String rootCause;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NcrStatus status = NcrStatus.OPEN;
    @Column(name = "reported_by", nullable = false)
    private UUID reportedBy;
    @Column(name = "assigned_to")
    private UUID assignedTo;
    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt = OffsetDateTime.now();
    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    public enum Severity { CRITICAL, MAJOR, MINOR }
    public enum NcrStatus { OPEN, UNDER_REVIEW, PENDING_CAPA, CLOSED, CANCELLED }
}
