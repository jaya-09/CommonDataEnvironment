-- V1__create_llm_tables.sql
-- LLM Service: Learning Lifecycle Management
-- All UUIDs generated at application layer for portability

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────
-- SKILL_MASTER
-- ─────────────────────────────────────────
CREATE TABLE skill_master (
    skill_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_code      VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- USER_PROFILE (source of truth for all 3 services)
-- ─────────────────────────────────────────
CREATE TABLE user_profile (
    user_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id         VARCHAR(50) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    full_name           VARCHAR(200) NOT NULL,
    role                VARCHAR(100) NOT NULL,   -- ENGINEER, QUALITY_MANAGER, TRAINER, ADMIN
    department          VARCHAR(100),
    competency_level    VARCHAR(20) NOT NULL DEFAULT 'JUNIOR',  -- JUNIOR, MID, SENIOR
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_competency_level CHECK (competency_level IN ('JUNIOR', 'MID', 'SENIOR')),
    CONSTRAINT chk_role CHECK (role IN ('ENGINEER', 'QUALITY_MANAGER', 'TRAINER', 'ADMIN'))
);

-- ─────────────────────────────────────────
-- TRAINING_COURSE
-- ─────────────────────────────────────────
CREATE TABLE training_course (
    course_id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id                UUID NOT NULL REFERENCES skill_master(skill_id),
    course_code             VARCHAR(50) NOT NULL UNIQUE,
    title                   VARCHAR(300) NOT NULL,
    description             TEXT,
    mandatory_for_phase     VARCHAR(50),    -- references PLM lifecycle phase name
    passing_score           INT NOT NULL DEFAULT 70,
    duration_hours          DECIMAL(5,2),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    version                 INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- TRAINING_ENROLLMENT
-- ─────────────────────────────────────────
CREATE TABLE training_enrollment (
    enrollment_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES user_profile(user_id),
    course_id           UUID NOT NULL REFERENCES training_course(course_id),
    status              VARCHAR(30) NOT NULL DEFAULT 'ENROLLED',  -- ENROLLED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    trigger_source      VARCHAR(20) NOT NULL DEFAULT 'MANUAL',    -- MANUAL, PUBSUB
    trigger_ref_id      UUID,           -- the NCR id / phase id that triggered this
    trigger_ref_type    VARCHAR(50),    -- NCR, PHASE_TRANSITION, AUDIT_FINDING
    deadline            DATE,
    score               INT,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_trigger_source CHECK (trigger_source IN ('MANUAL', 'PUBSUB')),
    UNIQUE (user_id, course_id, status)  -- prevent duplicate active enrollments
);

-- ─────────────────────────────────────────
-- USER_CERTIFICATION
-- ─────────────────────────────────────────
CREATE TABLE user_certification (
    cert_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES user_profile(user_id),
    course_id       UUID NOT NULL REFERENCES training_course(course_id),
    enrollment_id   UUID REFERENCES training_enrollment(enrollment_id),
    cert_number     VARCHAR(100) NOT NULL UNIQUE,
    score           INT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRED, REVOKED
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cert_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
);

-- ─────────────────────────────────────────
-- TRAINING_AUDIT_LOG
-- ─────────────────────────────────────────
CREATE TABLE training_audit_log (
    log_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(50) NOT NULL,   -- USER_PROFILE, ENROLLMENT, CERTIFICATION, COURSE
    entity_id       UUID NOT NULL,
    action          VARCHAR(100) NOT NULL,  -- CREATED, UPDATED, ENROLLED, COMPLETED, CERTIFIED, etc.
    old_value       JSONB,
    new_value       JSONB,
    performed_by    UUID,                   -- user_id who performed the action (null if system)
    event_source    VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER, PUBSUB
    correlation_id  VARCHAR(100),           -- for distributed tracing
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- PROCESSED_EVENTS (idempotency table)
-- ─────────────────────────────────────────
CREATE TABLE processed_events (
    event_id        VARCHAR(255) PRIMARY KEY,
    event_type      VARCHAR(200) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────
CREATE INDEX idx_user_profile_email ON user_profile(email);
CREATE INDEX idx_user_profile_employee_id ON user_profile(employee_id);
CREATE INDEX idx_enrollment_user_id ON training_enrollment(user_id);
CREATE INDEX idx_enrollment_course_id ON training_enrollment(course_id);
CREATE INDEX idx_enrollment_status ON training_enrollment(status);
CREATE INDEX idx_cert_user_id ON user_certification(user_id);
CREATE INDEX idx_cert_course_id ON user_certification(course_id);
CREATE INDEX idx_cert_status ON user_certification(status);
CREATE INDEX idx_cert_expires_at ON user_certification(expires_at);
CREATE INDEX idx_audit_entity ON training_audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON training_audit_log(created_at);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
