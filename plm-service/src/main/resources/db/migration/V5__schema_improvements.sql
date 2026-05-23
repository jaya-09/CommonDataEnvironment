-- V5__schema_improvements.sql
-- Fills gaps identified during PLM table review.

-- ── 1. product_version ────────────────────────────────────────────────────
-- A version should describe what changed (e.g. "Initial release", "Hotfix for #42")
ALTER TABLE product_version ADD COLUMN IF NOT EXISTS description TEXT;

-- ── 2. bom_component ─────────────────────────────────────────────────────
-- Track who added each component and when it was last modified
ALTER TABLE bom_component ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE bom_component ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- ── 3. change_request ────────────────────────────────────────────────────
-- Timestamps for each key CR milestone — useful for SLA and audit reporting
ALTER TABLE change_request ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;
ALTER TABLE change_request ADD COLUMN IF NOT EXISTS decided_at   TIMESTAMPTZ;

-- ── 4. approval_workflow ─────────────────────────────────────────────────
-- DELEGATED status exists but there was no column to record who it was delegated to
ALTER TABLE approval_workflow ADD COLUMN IF NOT EXISTS delegated_to UUID;

-- ── 5. version_phase_instance ────────────────────────────────────────────
-- Record who performed the phase transition (phase advances are user actions)
ALTER TABLE version_phase_instance ADD COLUMN IF NOT EXISTS performed_by UUID;
