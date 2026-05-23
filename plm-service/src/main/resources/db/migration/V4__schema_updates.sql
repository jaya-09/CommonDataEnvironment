-- V4__schema_updates.sql
-- Applies all entity changes made after V3 seed data.

-- 1. Add approver_user_id to product (nullable — not all products need a designated approver set at creation)
ALTER TABLE product ADD COLUMN IF NOT EXISTS approver_user_id UUID;

-- 2. Add released_by to product_version (nullable — set only when version is released)
ALTER TABLE product_version ADD COLUMN IF NOT EXISTS released_by UUID;

-- 3. Drop is_active from lifecycle_phase (column was never filtered on; phases are always valid)
ALTER TABLE lifecycle_phase DROP COLUMN IF EXISTS is_active;

-- 4. Add uploaded_by to technical_data_package (tracks who uploaded the document)
ALTER TABLE technical_data_package ADD COLUMN IF NOT EXISTS uploaded_by UUID;
