package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "product_version")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id", updatable = false, nullable = false)
    private UUID versionId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "version_number", nullable = false, length = 30)
    private String versionNumber;
    @Column(name = "description")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VersionStatus status = VersionStatus.DRAFT;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_phase_id")
    private LifecyclePhase currentPhase;
    @Column(name = "released_at")
    private OffsetDateTime releasedAt;
    @Column(name = "released_by")
    private UUID releasedBy;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Version
    @Column(name = "version")
    private Integer optimisticVersion = 0;
    public enum VersionStatus { DRAFT, IN_PROGRESS, RELEASED, DEPRECATED, HOLD }
}
