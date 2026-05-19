-- V1__create_qlm_tables.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE non_conformance (
    ncr_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ncr_number          VARCHAR(50) NOT NULL UNIQUE,
    product_version_id  UUID NOT NULL,    -- references PLM (no FK — cross-service)
    product_code        VARCHAR(50),      -- denormalized for display
    severity            VARCHAR(10) NOT NULL DEFAULT 'MAJOR',
    title               VARCHAR(300) NOT NULL,
    description         TEXT,
    root_cause          TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reported_by         UUID NOT NULL,    -- references LLM user_profile
    assigned_to         UUID,
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ncr_severity CHECK (severity IN ('CRITICAL','MAJOR','MINOR')),
    CONSTRAINT chk_ncr_status CHECK (status IN ('OPEN','UNDER_REVIEW','PENDING_CAPA','CLOSED','CANCELLED'))
);

CREATE TABLE capa (
    capa_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capa_number         VARCHAR(50) NOT NULL UNIQUE,
    ncr_id              UUID REFERENCES non_conformance(ncr_id),
    title               VARCHAR(300) NOT NULL,
    corrective_action   TEXT,
    preventive_action   TEXT,
    owner_user_id       UUID NOT NULL,    -- references LLM user
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_date            DATE,
    effectiveness_check TEXT,
    effectiveness_verified BOOLEAN,
    closed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_capa_status CHECK (status IN ('OPEN','IN_PROGRESS','PENDING_VERIFICATION','CLOSED','CANCELLED'))
);

CREATE TABLE quality_audit (
    audit_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_number    VARCHAR(50) NOT NULL UNIQUE,
    audit_type      VARCHAR(50) NOT NULL,   -- INTERNAL, SUPPLIER, REGULATORY
    scope           TEXT,
    scheduled_date  DATE,
    conducted_date  DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    lead_auditor_id UUID NOT NULL,
    summary         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE TABLE audit_finding (
    finding_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id        UUID NOT NULL REFERENCES quality_audit(audit_id),
    finding_type    VARCHAR(20) NOT NULL DEFAULT 'OBSERVATION',  -- NONCONFORMITY, OBSERVATION, OPPORTUNITY
    description     TEXT NOT NULL,
    severity        VARCHAR(10) DEFAULT 'MINOR',
    course_code_required VARCHAR(50),    -- optional: trigger LLM enrollment
    affected_dept   VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE risk_register (
    risk_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_number         VARCHAR(50) NOT NULL UNIQUE,
    product_version_id  UUID,       -- optional: link to PLM
    title               VARCHAR(300) NOT NULL,
    description         TEXT,
    category            VARCHAR(50),
    likelihood          INT NOT NULL CHECK (likelihood BETWEEN 1 AND 5),
    impact              INT NOT NULL CHECK (impact BETWEEN 1 AND 5),
    risk_score          INT GENERATED ALWAYS AS (likelihood * impact) STORED,
    mitigation_plan     TEXT,
    owner_user_id       UUID,
    status              VARCHAR(20) NOT NULL DEFAULT 'IDENTIFIED',
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_risk_status CHECK (status IN ('IDENTIFIED','MITIGATING','ACCEPTED','CLOSED'))
);

CREATE TABLE document_control (
    doc_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_number      VARCHAR(100) NOT NULL,
    revision        VARCHAR(20) NOT NULL DEFAULT 'A',
    title           VARCHAR(300) NOT NULL,
    tdp_ref_id      UUID,       -- references PLM TDP
    storage_url     TEXT,
    document_hash   VARCHAR(64) NOT NULL,   -- SHA-256 for tamper detection
    approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by     UUID,
    approved_at     TIMESTAMPTZ,
    effective_date  DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (doc_number, revision),
    CONSTRAINT chk_doc_approval CHECK (approval_status IN ('DRAFT','UNDER_REVIEW','APPROVED','OBSOLETE'))
);

CREATE TABLE quality_audit_log (
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

-- User shadow cache
CREATE TABLE user_shadow (
    user_id         UUID PRIMARY KEY,
    employee_id     VARCHAR(50) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    role            VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    last_synced_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_ncr_status ON non_conformance(status);
CREATE INDEX idx_ncr_version ON non_conformance(product_version_id);
CREATE INDEX idx_capa_ncr ON capa(ncr_id);
CREATE INDEX idx_capa_status ON capa(status);
CREATE INDEX idx_risk_score ON risk_register(risk_score DESC);
CREATE INDEX idx_risk_version ON risk_register(product_version_id);
CREATE INDEX idx_audit_status ON quality_audit(status);
CREATE INDEX idx_finding_audit ON audit_finding(audit_id);
CREATE INDEX idx_doc_number ON document_control(doc_number, revision);
CREATE INDEX idx_quality_audit_entity ON quality_audit_log(entity_type, entity_id);
