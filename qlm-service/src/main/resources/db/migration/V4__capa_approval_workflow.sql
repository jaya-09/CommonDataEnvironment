-- V4__capa_approval_workflow.sql
-- Extends the CAPA table to support a full approval workflow:
--   OPEN → UNDER_REVIEW → APPROVED → CLOSED
--             └──────────→ OPEN (rejected, with reason)
--
-- Changes:
--   1. Add reviewed_by, reviewed_at, rejection_reason columns
--   2. Expand chk_capa_status constraint to include new statuses

-- ── New columns ──────────────────────────────────────────────────
ALTER TABLE capa
    ADD COLUMN IF NOT EXISTS reviewed_by       UUID,
    ADD COLUMN IF NOT EXISTS reviewed_at       TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason  TEXT;

-- ── Expand status constraint ──────────────────────────────────────
-- PostgreSQL does not support ALTER CONSTRAINT, so drop and recreate.
ALTER TABLE capa DROP CONSTRAINT IF EXISTS chk_capa_status;
ALTER TABLE capa ADD CONSTRAINT chk_capa_status
    CHECK (status IN (
        'OPEN',
        'UNDER_REVIEW',
        'APPROVED',
        'REJECTED',
        'IN_PROGRESS',
        'PENDING_VERIFICATION',
        'CLOSED',
        'CANCELLED'
    ));
