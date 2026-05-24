package com.cde.llm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "training_enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "enrollment_id", updatable = false, nullable = false)
    private UUID enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private TrainingCourse course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false, length = 20)
    private TriggerSource triggerSource = TriggerSource.MANUAL;

    /** ID of the entity that triggered this enrollment (NCR id, phase id, audit finding id) */
    @Column(name = "trigger_ref_id")
    private UUID triggerRefId;

    /** Type of entity that triggered: NCR, PHASE_TRANSITION, AUDIT_FINDING */
    @Column(name = "trigger_ref_type", length = 50)
    private String triggerRefType;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "score")
    private Integer score;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum EnrollmentStatus {
        ENROLLED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    public enum TriggerSource {
        MANUAL, PUBSUB
    }
}
