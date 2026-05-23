-- V3__fix_enrollment_unique_constraint.sql
--
-- BUG FIX: The original UNIQUE (user_id, course_id, status) constraint was intended to
-- prevent duplicate active enrollments, but it also prevented a user from failing the same
-- course more than once (two FAILED rows for the same user+course violate the constraint).
--
-- Replace it with a partial unique index that ONLY enforces uniqueness for active rows
-- (ENROLLED or IN_PROGRESS). Completed, failed, and cancelled rows are unrestricted
-- so a user can retry a course multiple times after failing.

-- Drop the broad table-level unique constraint
ALTER TABLE training_enrollment
    DROP CONSTRAINT IF EXISTS training_enrollment_user_id_course_id_status_key;

-- Add a partial unique index — one active enrollment per user per course at any time
CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollment_active
    ON training_enrollment (user_id, course_id)
    WHERE status IN ('ENROLLED', 'IN_PROGRESS');
