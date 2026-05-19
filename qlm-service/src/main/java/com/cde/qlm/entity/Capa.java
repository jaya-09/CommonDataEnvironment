package com.cde.qlm.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "capa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Capa {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "capa_id", updatable = false, nullable = false)
    private UUID capaId;
    @Column(name = "capa_number", nullable = false, unique = true, length = 50)
    private String capaNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ncr_id")
    private NonConformance ncr;
    @Column(name = "title", nullable = false, length = 300)
    private String title;
    @Column(name = "corrective_action")
    private String correctiveAction;
    @Column(name = "preventive_action")
    private String preventiveAction;
    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CapaStatus status = CapaStatus.OPEN;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "effectiveness_check")
    private String effectivenessCheck;
    @Column(name = "effectiveness_verified")
    private Boolean effectivenessVerified;
    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    public enum CapaStatus { OPEN, IN_PROGRESS, PENDING_VERIFICATION, CLOSED, CANCELLED }
}
