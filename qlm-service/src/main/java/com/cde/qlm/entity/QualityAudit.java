package com.cde.qlm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "quality_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualityAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;
    @Column(name = "audit_number", nullable = false, unique = true, length = 50)
    private String auditNumber;
    @Column(name = "audit_type", nullable = false, length = 50)
    private String auditType;
    @Column(name = "scope")
    private String scope;
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    @Column(name = "conducted_date")
    private LocalDate conductedDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status = AuditStatus.PLANNED;
    @Column(name = "lead_auditor_id", nullable = false)
    private UUID leadAuditorId;
    @Column(name = "summary")
    private String summary;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    public enum AuditStatus { PLANNED, IN_PROGRESS, COMPLETED, CANCELLED }
}
