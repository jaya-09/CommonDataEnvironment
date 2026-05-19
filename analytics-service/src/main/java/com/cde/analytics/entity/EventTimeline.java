package com.cde.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_timeline")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "timeline_id", updatable = false, nullable = false)
    private UUID timelineId;

    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_service", nullable = false, length = 10)
    private String sourceService;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_ref", length = 100)
    private String entityRef;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "severity", length = 10)
    private String severity;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
}
