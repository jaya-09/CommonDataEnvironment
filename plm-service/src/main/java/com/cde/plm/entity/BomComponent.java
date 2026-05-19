package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "bom_component")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BomComponent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "component_id", updatable = false, nullable = false)
    private UUID componentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ProductVersion version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_component_id")
    private BomComponent parent;
    @Column(name = "component_code", nullable = false, length = 100)
    private String componentCode;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "component_type", length = 50)
    private String componentType;
    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;
    @Column(name = "unit", length = 20)
    private String unit;
    @Column(name = "notes")
    private String notes;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
