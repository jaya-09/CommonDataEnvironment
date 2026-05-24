package com.cde.llm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// ─────────────────────────────────────────
// SKILL MASTER
// ─────────────────────────────────────────
@Entity
@Table(name = "skill_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SkillMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "skill_id", updatable = false, nullable = false)
    private UUID skillId;

    @Column(name = "skill_code", nullable = false, unique = true, length = 50)
    private String skillCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
