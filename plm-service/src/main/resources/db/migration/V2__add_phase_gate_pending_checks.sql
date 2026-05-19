-- Phase gate pending checks table
-- Tracks in-flight async phase gate checks dispatched via Pub/Sub.
-- PLM collects results from QLM and LLM; once both arrive, the final
-- advance-or-block decision is made and this row is marked complete.

CREATE TABLE phase_gate_pending_checks (
    gate_correlation_id     VARCHAR(255)    PRIMARY KEY,
    version_id              UUID            NOT NULL,
    target_phase            VARCHAR(100)    NOT NULL,
    requested_by            UUID,
    correlation_id          VARCHAR(255),

    -- QLM result
    qlm_result_received     BOOLEAN         NOT NULL DEFAULT FALSE,
    qlm_passed              BOOLEAN,
    qlm_block_reason        TEXT,
    open_critical_ncr_count BIGINT,

    -- LLM result
    llm_result_received     BOOLEAN         NOT NULL DEFAULT FALSE,
    llm_passed              BOOLEAN,
    llm_block_reason        TEXT,
    uncertified_count       INTEGER,

    -- Final status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,

    CONSTRAINT fk_pgpc_version FOREIGN KEY (version_id)
        REFERENCES product_versions(version_id) ON DELETE CASCADE
);

CREATE INDEX idx_pgpc_version_phase_status
    ON phase_gate_pending_checks(version_id, target_phase, status);
