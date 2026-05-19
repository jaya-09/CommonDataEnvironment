package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name = "lifecycle_phase")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LifecyclePhase {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "phase_id", updatable = false, nullable = false)
    private UUID phaseId;
    @Column(name = "phase_name", nullable = false, unique = true, length = 50)
    private String phaseName;
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    @Column(name = "sequence_order", nullable = false, unique = true)
    private Integer sequenceOrder;
    @Column(name = "description")
    private String description;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
