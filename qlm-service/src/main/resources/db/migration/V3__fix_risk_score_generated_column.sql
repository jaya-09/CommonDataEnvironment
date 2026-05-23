-- V3__fix_risk_score_generated_column.sql
--
-- Problem: risk_score was defined as GENERATED ALWAYS AS (likelihood * impact) STORED.
-- Hibernate tries to INSERT/UPDATE the column directly (via @PrePersist/@PreUpdate),
-- which PostgreSQL rejects for GENERATED ALWAYS columns with:
--   "ERROR: column "risk_score" can only be updated to DEFAULT"
--
-- Fix: Convert risk_score to a plain integer column.
-- The Java entity's @PrePersist / @PreUpdate already computes likelihood * impact,
-- so the value is maintained correctly in application code.

ALTER TABLE risk_register
    ALTER COLUMN risk_score DROP EXPRESSION,
    ALTER COLUMN risk_score TYPE INT;
