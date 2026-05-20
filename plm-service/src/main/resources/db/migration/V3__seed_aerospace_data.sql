-- V3__seed_aerospace_data.sql
-- Seed realistic aerospace industry test data
-- Fixed UUIDs allow cross-service references to QLM and LLM

-- ─────────────────────────────────────────
-- PRODUCTS
-- ─────────────────────────────────────────
INSERT INTO product (product_id, product_code, name, description, status, created_by) VALUES
    ('a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'AWA-001', 'Main Wing Assembly',
     'Primary structural component for aircraft wing with integrated control surfaces',
     'ACTIVE', '11111111-1111-1111-1111-111111111111'),
    ('a2222222-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'LGS-002', 'Landing Gear System',
     'Retractable landing gear with shock absorption and brake assembly',
     'ACTIVE', '11111111-1111-1111-1111-111111111111'),
    ('a3333333-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CCP-003', 'Cockpit Control Panel',
     'Integrated flight deck control interface with avionics integration',
     'ACTIVE', '11111111-1111-1111-1111-111111111111'),
    ('a4444444-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'HYD-004', 'Hydraulic Power System',
     'Primary hydraulic power distribution and pressure regulation system',
     'ACTIVE', '11111111-1111-1111-1111-111111111111');

-- ─────────────────────────────────────────
-- PRODUCT VERSIONS (using phases from V1 migration)
-- ─────────────────────────────────────────
-- Wing v1.0.0 in Design phase
INSERT INTO product_version (version_id, product_id, version_number, status, current_phase_id, created_by)
SELECT '33333333-3333-3333-3333-333333333333', 'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '1.0.0', 'IN_PROGRESS',
       phase_id, '11111111-1111-1111-1111-111111111111'
FROM lifecycle_phase WHERE phase_name = 'Design';

-- Landing Gear v2.1.0 in Ideation phase (will be blocked by CRITICAL NCRs)
INSERT INTO product_version (version_id, product_id, version_number, status, current_phase_id, created_by)
SELECT '22222222-2222-2222-2222-222222222222', 'a2222222-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2.1.0', 'IN_PROGRESS',
       phase_id, '11111111-1111-1111-1111-111111111111'
FROM lifecycle_phase WHERE phase_name = 'Ideation';

-- Cockpit v1.0.0 in DRAFT
INSERT INTO product_version (version_id, product_id, version_number, status, current_phase_id, created_by)
SELECT '55555555-5555-5555-5555-555555555555', 'a3333333-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '1.0.0', 'DRAFT',
       phase_id, '11111111-1111-1111-1111-111111111111'
FROM lifecycle_phase WHERE phase_name = 'Ideation';

-- Hydraulic v3.2.1 in Testing phase
INSERT INTO product_version (version_id, product_id, version_number, status, current_phase_id, created_by)
SELECT '44444444-4444-4444-4444-444444444444', 'a4444444-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '3.2.1', 'IN_PROGRESS',
       phase_id, '11111111-1111-1111-1111-111111111111'
FROM lifecycle_phase WHERE phase_name = 'Testing';

-- ─────────────────────────────────────────
-- BOM COMPONENTS - Wing Assembly
-- ─────────────────────────────────────────
INSERT INTO bom_component (component_id, version_id, parent_component_id, component_code, name, component_type, quantity, unit, notes) VALUES
    ('b1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', NULL,
     'WING-001', 'Main Wing Assembly', 'ASSEMBLY', 1, 'UNIT', 'Root assembly for main wing'),
    ('b1111111-1111-1111-1111-111111111112', '33333333-3333-3333-3333-333333333333', 'b1111111-1111-1111-1111-111111111111',
     'WING-SPAR-001', 'Wing Spar Box', 'STRUCTURAL', 1, 'UNIT', 'Main structural spar in composite material'),
    ('b1111111-1111-1111-1111-111111111113', '33333333-3333-3333-3333-333333333333', 'b1111111-1111-1111-1111-111111111111',
     'WING-SKIN-001', 'Wing Skin Panel', 'COMPOSITE', 2, 'PANEL', 'Aluminum-lithium alloy skin panels'),
    ('b1111111-1111-1111-1111-111111111114', '33333333-3333-3333-3333-333333333333', 'b1111111-1111-1111-1111-111111111111',
     'AILERONS-001', 'Ailerons Control Surface', 'CONTROL_SURFACE', 2, 'UNIT', 'Primary flight control surfaces');

-- BOM - Landing Gear
INSERT INTO bom_component (component_id, version_id, parent_component_id, component_code, name, component_type, quantity, unit, notes) VALUES
    ('b2222222-2222-2222-2222-222222222221', '22222222-2222-2222-2222-222222222222', NULL,
     'LGS-001', 'Landing Gear Assembly', 'ASSEMBLY', 1, 'UNIT', NULL),
    ('b2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222221',
     'STRUT-001', 'Main Strut with Damper', 'STRUCTURAL', 3, 'UNIT', 'Main landing strut with hydraulic damping'),
    ('b2222222-2222-2222-2222-222222222223', '22222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222221',
     'WHEEL-001', 'Wheel Assembly', 'MECHANICAL', 3, 'UNIT', '20-inch aircraft wheel with tire assembly');

-- BOM - Cockpit
INSERT INTO bom_component (component_id, version_id, parent_component_id, component_code, name, component_type, quantity, unit, notes) VALUES
    ('b3333333-3333-3333-3333-333333333331', '55555555-5555-5555-5555-555555555555', NULL,
     'CP-001', 'Control Panel Assembly', 'ASSEMBLY', 1, 'UNIT', NULL),
    ('b3333333-3333-3333-3333-333333333332', '55555555-5555-5555-5555-555555555555', 'b3333333-3333-3333-3333-333333333331',
     'DISPLAY-001', 'Main Display Unit', 'AVIONICS', 1, 'UNIT', NULL);

-- BOM - Hydraulic System
INSERT INTO bom_component (component_id, version_id, parent_component_id, component_code, name, component_type, quantity, unit, notes) VALUES
    ('b4444444-4444-4444-4444-444444444441', '44444444-4444-4444-4444-444444444444', NULL,
     'HYD-001', 'Hydraulic Power Unit', 'ASSEMBLY', 1, 'UNIT', NULL),
    ('b4444444-4444-4444-4444-444444444442', '44444444-4444-4444-4444-444444444444', 'b4444444-4444-4444-4444-444444444441',
     'PUMP-001', 'Main Hydraulic Pump', 'MECHANICAL', 2, 'UNIT', NULL),
    ('b4444444-4444-4444-4444-444444444443', '44444444-4444-4444-4444-444444444444', 'b4444444-4444-4444-4444-444444444441',
     'ACCUMULATOR-001', 'Pressure Accumulator', 'MECHANICAL', 2, 'UNIT', NULL);

-- ─────────────────────────────────────────
-- USER SHADOWS (cached from LLM service)
-- ─────────────────────────────────────────
INSERT INTO user_shadow (user_id, employee_id, email, full_name, role, department) VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP-001', 'alice.chen@aerospace.local', 'Alice Chen', 'ENGINEER', 'Structures'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EMP-002', 'bob.kumar@aerospace.local', 'Bob Kumar', 'ENGINEER', 'Manufacturing'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'EMP-003', 'carol.johnson@aerospace.local', 'Carol Johnson', 'ENGINEER', 'Systems'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'EMP-004', 'david.patel@aerospace.local', 'David Patel', 'ENGINEER', 'Structures'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'EMP-005', 'evelyn.martinez@aerospace.local', 'Evelyn Martinez', 'QUALITY_MANAGER', 'Quality'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'EMP-006', 'frank.wu@aerospace.local', 'Frank Wu', 'TRAINER', 'Learning & Development');
