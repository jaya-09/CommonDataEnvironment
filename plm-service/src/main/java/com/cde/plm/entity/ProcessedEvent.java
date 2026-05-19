package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name = "processed_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEvent {
    @Id
    @Column(name = "event_id", length = 255)
    private String eventId;
    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now();
}
