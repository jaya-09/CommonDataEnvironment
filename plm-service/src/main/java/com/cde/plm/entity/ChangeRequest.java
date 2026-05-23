package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "change_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cr_id", updatable = false, nullable = false)
    private UUID crId;
    @Column(name = "cr_number", nullable = false, unique = true, length = 50)
    private String crNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ProductVersion version;
    @Enumerated(EnumType.STRING)
    @Column(name = "cr_type", nullable = false, length = 20)
    private CrType crType = CrType.STANDARD;
    @Column(name = "title", nullable = false, length = 300)
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "reason")
    private String reason;
    @Column(name = "impact_analysis")
    private String impactAnalysis;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CrStatus status = CrStatus.DRAFT;
    @Column(name = "raised_by", nullable = false)
    private UUID raisedBy;
    /** Set when the CR is submitted for review. */
    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;
    /** Set when the CR is approved or rejected. */
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    public enum CrType { STANDARD, ENHANCED }
    public enum CrStatus { DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, WITHDRAWN }
}
