package com.cde.qlm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "risk_register")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RiskRegister {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "risk_id", updatable = false, nullable = false)
    private UUID riskId;
    @Column(name = "risk_number", nullable = false, unique = true, length = 50)
    private String riskNumber;
    @Column(name = "product_version_id")
    private UUID productVersionId;
    @Column(name = "title", nullable = false, length = 300)
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "category", length = 50)
    private String category;
    @Column(name = "likelihood", nullable = false)
    private Integer likelihood;
    @Column(name = "impact", nullable = false)
    private Integer impact;
    @Column(name = "risk_score")
    private Integer riskScore;
    @Column(name = "mitigation_plan")
    private String mitigationPlan;
    @Column(name = "owner_user_id")
    private UUID ownerUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RiskStatus status = RiskStatus.IDENTIFIED;
    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @PrePersist @PreUpdate
    public void calcScore() { this.riskScore = this.likelihood * this.impact; }
    public enum RiskStatus { IDENTIFIED, MITIGATING, ACCEPTED, CLOSED }
}
