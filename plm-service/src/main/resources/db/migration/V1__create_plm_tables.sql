-- V1__create_plm_tables.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE product (
    product_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code    VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'OBSOLETE', 'DRAFT'))
);

CREATE TABLE lifecycle_phase (
    phase_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_name      VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    sequence_order  INT NOT NULL UNIQUE,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Seed lifecycle phases
INSERT INTO lifecycle_phase (phase_name, display_name, sequence_order) VALUES
    ('Ideation',    'Ideation',    1),
    ('Design',      'Design',      2),
    ('Development', 'Development', 3),
    ('Testing',     'Testing',     4),
    ('Deployment',  'Deployment',  5),
    ('Maintenance', 'Maintenance', 6);

CREATE TABLE product_version (
    version_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES product(product_id),
    version_number      VARCHAR(30) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    current_phase_id    UUID REFERENCES lifecycle_phase(phase_id),
    released_at         TIMESTAMPTZ,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 0,   -- optimistic locking
    UNIQUE (product_id, version_number),
    CONSTRAINT chk_version_status CHECK (status IN ('DRAFT','IN_PROGRESS','RELEASED','DEPRECATED','HOLD'))
);

CREATE TABLE version_phase_instance (
    instance_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id      UUID NOT NULL REFERENCES product_version(version_id),
    phase_id        UUID NOT NULL REFERENCES lifecycle_phase(phase_id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    notes           TEXT,
    CONSTRAINT chk_phase_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','SKIPPED'))
);

CREATE TABLE change_request (
    cr_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cr_number       VARCHAR(50) NOT NULL UNIQUE,
    version_id      UUID NOT NULL REFERENCES product_version(version_id),
    cr_type         VARCHAR(20) NOT NULL DEFAULT 'STANDARD',   -- STANDARD, ENHANCED
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    reason          TEXT,
    impact_analysis TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    raised_by       UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cr_status CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED','WITHDRAWN')),
    CONSTRAINT chk_cr_type CHECK (cr_type IN ('STANDARD','ENHANCED'))
);

CREATE TABLE approval_workflow (
    workflow_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(20) NOT NULL,   -- VERSION, CHANGE_REQUEST
    entity_id       UUID NOT NULL,
    step_order      INT NOT NULL,
    approver_user_id UUID NOT NULL,         -- references LLM USER_PROFILE
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comments        TEXT,
    decided_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_workflow_status CHECK (status IN ('PENDING','APPROVED','REJECTED','DELEGATED'))
);

CREATE TABLE bom_component (
    component_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id          UUID NOT NULL REFERENCES product_version(version_id),
    parent_component_id UUID REFERENCES bom_component(component_id),  -- self-ref for hierarchy
    component_code      VARCHAR(100) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    component_type      VARCHAR(50),
    quantity            DECIMAL(12,4) NOT NULL DEFAULT 1,
    unit                VARCHAR(20),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE technical_data_package (
    tdp_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id      UUID NOT NULL REFERENCES product_version(version_id),
    document_name   VARCHAR(300) NOT NULL,
    document_type   VARCHAR(50),
    storage_url     TEXT NOT NULL,
    document_hash   VARCHAR(64) NOT NULL,   -- SHA-256
    file_size_bytes BIGINT,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by     UUID,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tdp_approval CHECK (approval_status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE TABLE traceability_audit_log (
    log_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(100) NOT NULL,
    old_value       JSONB,
    new_value       JSONB,
    performed_by    UUID,
    event_source    VARCHAR(20) NOT NULL DEFAULT 'USER',
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE processed_events (
    event_id        VARCHAR(255) PRIMARY KEY,
    event_type      VARCHAR(200) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- User shadow cache (synced via Pub/Sub from LLM)
CREATE TABLE user_shadow (
    user_id         UUID PRIMARY KEY,
    employee_id     VARCHAR(50) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    role            VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    last_synced_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_product_version_product ON product_version(product_id);
CREATE INDEX idx_product_version_status ON product_version(status);
CREATE INDEX idx_cr_version ON change_request(version_id);
CREATE INDEX idx_cr_status ON change_request(status);
CREATE INDEX idx_workflow_entity ON approval_workflow(entity_type, entity_id);
CREATE INDEX idx_workflow_approver ON approval_workflow(approver_user_id);
CREATE INDEX idx_bom_version ON bom_component(version_id);
CREATE INDEX idx_bom_parent ON bom_component(parent_component_id);
CREATE INDEX idx_tdp_version ON technical_data_package(version_id);
CREATE INDEX idx_audit_entity ON traceability_audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_created ON traceability_audit_log(created_at DESC);
