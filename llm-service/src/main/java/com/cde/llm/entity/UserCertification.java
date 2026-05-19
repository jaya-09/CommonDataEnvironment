package com.cde.llm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_certification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cert_id", updatable = false, nullable = false)
    private UUID certId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private TrainingCourse course;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private TrainingEnrollment enrollment;

    @Column(name = "cert_number", nullable = false, unique = true, length = 100)
    private String certNumber;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CertStatus status = CertStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum CertStatus {
        ACTIVE, EXPIRED, REVOKED
    }
}
