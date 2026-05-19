package com.cde.plm.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Table(name = "user_shadow")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserShadow {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    @Column(name = "role", nullable = false, length = 100)
    private String role;
    @Column(name = "department", length = 100)
    private String department;
    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt = OffsetDateTime.now();
}
