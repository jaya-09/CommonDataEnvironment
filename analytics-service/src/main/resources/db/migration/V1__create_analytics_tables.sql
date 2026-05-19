-- V1__create_analytics_tables.sql
-- Analytics Service: Cross-service read model
-- Populated exclusively by consuming Pub/Sub events from PLM, QLM, LLM
-- This DB is NEVER written to directly by any other service.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────────────────────
-- PRODUCT HEALTH SNAPSHOT
-- Cross-service view: PLM phase + QLM quality + LLM readiness
-- Updated by events from all 3 services
-- ─────────────────────────────────────────────────────────────
CREATE TABLE product_health_snapshot (
    version_id              UUID PRIMARY KEY,      -- from PLM
    product_id              UUID NOT NULL,
    product_code            VARCHAR(50) NOT NULL,
    product_name            VARCHAR(200) NOT NULL,
    version_number          VARCHAR(30) NOT NULL,
    version_status          VARCHAR(30) NOT NULL,  -- PLM status
    current_phase           VARCHAR(50),           -- PLM phase
    phase_sequence_order    INT,

    -- Quality metrics (from QLM events)
    open_ncr_count          INT NOT NULL DEFAULT 0,
    critical_ncr_count      INT NOT NULL DEFAULT 0,
    open_capa_count         INT NOT NULL DEFAULT 0,
    high_risk_count         INT NOT NULL DEFAULT 0,

    -- Training readiness (from LLM events)
    uncertified_user_count  INT NOT NULL DEFAULT 0,
    phase_cert_ready        BOOLEAN NOT NULL DEFAULT TRUE,

    -- Computed overall health
    overall_status          VARCHAR(20) NOT NULL DEFAULT 'HEALTHY',
    -- HEALTHY | AT_RISK | BLOCKED

    -- Tracking
    last_plm_event_at       TIMESTAMPTZ,
    last_qlm_event_at       TIMESTAMPTZ,
    last_llm_event_at       TIMESTAMPTZ,
    snapshot_updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- NCR → CAPA → TRAINING CHAIN
-- Full traceability from a quality issue to its resolution
-- ─────────────────────────────────────────────────────────────
CREATE TABLE ncr_resolution_chain (
    chain_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ncr_id              UUID NOT NULL UNIQUE,      -- from QLM
    ncr_number          VARCHAR(50) NOT NULL,
    ncr_severity        VARCHAR(10) NOT NULL,
    ncr_title           VARCHAR(300),
    product_version_id  UUID,                      -- from PLM (cross-reference)
    product_code        VARCHAR(50),

    -- CAPA linkage (from QLM events)
    capa_id             UUID,
    capa_number         VARCHAR(50),
    capa_status         VARCHAR(20),
    capa_closed_at      TIMESTAMPTZ,

    -- Training triggered (from LLM events)
    training_triggered  BOOLEAN NOT NULL DEFAULT FALSE,
    course_code         VARCHAR(50),
    enrolled_user_count INT NOT NULL DEFAULT 0,
    certified_count     INT NOT NULL DEFAULT 0,

    -- Resolution status (computed)
    resolution_status   VARCHAR(30) NOT NULL DEFAULT 'NCR_OPEN',
    -- NCR_OPEN | CAPA_RAISED | CAPA_IN_PROGRESS | TRAINING_TRIGGERED
    -- | TRAINING_COMPLETE | FULLY_RESOLVED

    ncr_raised_at       TIMESTAMPTZ NOT NULL,
    fully_resolved_at   TIMESTAMPTZ,
    chain_updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- PHASE GATE HISTORY
-- Every phase gate attempt — passed, blocked, what the blockers were
-- ─────────────────────────────────────────────────────────────
CREATE TABLE phase_gate_history (
    gate_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id          UUID NOT NULL,             -- from PLM
    product_code        VARCHAR(50) NOT NULL,
    version_number      VARCHAR(30) NOT NULL,
    from_phase          VARCHAR(50),
    to_phase            VARCHAR(50) NOT NULL,
    gate_result         VARCHAR(10) NOT NULL,       -- PASS | BLOCK
    blocked_by          JSONB,
    -- e.g. {"qlm": "2 open critical NCRs", "llm": "3 uncertified users"}
    triggered_by_user   UUID,
    correlation_id      VARCHAR(100),
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- RISK MATRIX VIEW
-- Combined risk picture: NCR severity + risk register + cert gaps
-- ─────────────────────────────────────────────────────────────
CREATE TABLE risk_matrix_entry (
    entry_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_service      VARCHAR(10) NOT NULL,  -- QLM_NCR | QLM_RISK | LLM_CERT_GAP
    source_id           UUID NOT NULL,
    product_version_id  UUID,
    product_code        VARCHAR(50),

    risk_category       VARCHAR(50),
    risk_title          VARCHAR(300) NOT NULL,
    likelihood          INT,           -- 1-5
    impact              INT,           -- 1-5
    risk_score          INT,           -- likelihood * impact
    risk_level          VARCHAR(10),   -- LOW | MEDIUM | HIGH | CRITICAL

    linked_ncr_id       UUID,
    linked_capa_id      UUID,
    linked_phase        VARCHAR(50),

    is_resolved         BOOLEAN NOT NULL DEFAULT FALSE,
    detected_at         TIMESTAMPTZ NOT NULL,
    resolved_at         TIMESTAMPTZ,
    entry_updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- EVENT TIMELINE
-- Unified chronological stream of events across all 3 services
-- ─────────────────────────────────────────────────────────────
CREATE TABLE event_timeline (
    timeline_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255) NOT NULL UNIQUE,  -- idempotency
    event_type      VARCHAR(100) NOT NULL,
    source_service  VARCHAR(10) NOT NULL,           -- PLM | QLM | LLM
    entity_type     VARCHAR(50),                    -- VERSION | NCR | CAPA | CERTIFICATION
    entity_id       UUID,
    entity_ref      VARCHAR(100),                   -- human-readable ref (product code, ncr number…)
    description     TEXT NOT NULL,                  -- human-readable summary
    severity        VARCHAR(10),                    -- INFO | WARNING | CRITICAL
    product_code    VARCHAR(50),                    -- for filtering by product
    version_id      UUID,                           -- for filtering by version
    correlation_id  VARCHAR(100),
    occurred_at     TIMESTAMPTZ NOT NULL
);

-- ─────────────────────────────────────────────────────────────
-- KPI SNAPSHOTS (daily aggregations for trending)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE kpi_daily_snapshot (
    snapshot_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date       DATE NOT NULL UNIQUE,

    -- PLM KPIs
    total_products      INT NOT NULL DEFAULT 0,
    versions_released   INT NOT NULL DEFAULT 0,
    versions_on_hold    INT NOT NULL DEFAULT 0,
    phase_transitions   INT NOT NULL DEFAULT 0,

    -- QLM KPIs
    ncrs_opened         INT NOT NULL DEFAULT 0,
    ncrs_closed         INT NOT NULL DEFAULT 0,
    critical_ncrs_open  INT NOT NULL DEFAULT 0,
    capas_open          INT NOT NULL DEFAULT 0,
    avg_ncr_resolution_days DECIMAL(5,1),

    -- LLM KPIs
    certs_issued        INT NOT NULL DEFAULT 0,
    enrollments_triggered INT NOT NULL DEFAULT 0,
    phase_gates_blocked INT NOT NULL DEFAULT 0,
    phase_gates_passed  INT NOT NULL DEFAULT 0,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- PROCESSED EVENTS (idempotency)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE processed_events (
    event_id        VARCHAR(255) PRIMARY KEY,
    event_type      VARCHAR(200) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────────────────────────
CREATE INDEX idx_product_health_status   ON product_health_snapshot(overall_status);
CREATE INDEX idx_product_health_phase    ON product_health_snapshot(current_phase);
CREATE INDEX idx_product_health_product  ON product_health_snapshot(product_code);
CREATE INDEX idx_ncr_chain_resolution    ON ncr_resolution_chain(resolution_status);
CREATE INDEX idx_ncr_chain_product       ON ncr_resolution_chain(product_code);
CREATE INDEX idx_phase_gate_version      ON phase_gate_history(version_id);
CREATE INDEX idx_phase_gate_result       ON phase_gate_history(gate_result);
CREATE INDEX idx_risk_matrix_level       ON risk_matrix_entry(risk_level);
CREATE INDEX idx_risk_matrix_resolved    ON risk_matrix_entry(is_resolved);
CREATE INDEX idx_timeline_service        ON event_timeline(source_service);
CREATE INDEX idx_timeline_occurred       ON event_timeline(occurred_at DESC);
CREATE INDEX idx_timeline_product        ON event_timeline(product_code);
CREATE INDEX idx_timeline_version        ON event_timeline(version_id);
CREATE INDEX idx_kpi_date                ON kpi_daily_snapshot(snapshot_date DESC);
