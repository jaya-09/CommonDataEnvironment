-- V2__seed_aerospace_data.sql
-- QLM seed data for aerospace test scenarios

-- ─────────────────────────────────────────
-- NCRs (Non-Conformance Reports)
-- 2 CRITICAL on Landing Gear → will BLOCK phase advancement
-- ─────────────────────────────────────────
INSERT INTO non_conformance (ncr_id, ncr_number, product_version_id, product_code, severity, title, description, status, reported_by, assigned_to, detected_at) VALUES
    ('c1111111-1111-1111-1111-111111111111', 'NCR-2024-001',
     '22222222-2222-2222-2222-222222222222', 'LGS-002',
     'CRITICAL',
     'Strut damper seal degradation under pressure',
     'Hydraulic damper seals showing micro-fractures when tested at 3000 PSI. This affects shock absorption during landing and can lead to catastrophic failure.',
     'OPEN',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '11111111-1111-1111-1111-111111111111',
     NOW() - INTERVAL '5 days'),

    ('c1111111-1111-1111-1111-111111111112', 'NCR-2024-002',
     '22222222-2222-2222-2222-222222222222', 'LGS-002',
     'CRITICAL',
     'Wheel bearing assembly fatigue life below specification',
     'Bearing test shows 40% lower fatigue life than design requirement. Need to source new supplier or modify bearing geometry.',
     'OPEN',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '11111111-1111-1111-1111-111111111111',
     NOW() - INTERVAL '3 days'),

    ('c1111111-1111-1111-1111-111111111113', 'NCR-2024-003',
     '33333333-3333-3333-3333-333333333333', 'AWA-001',
     'MAJOR',
     'Wing skin panel surface finish below standard',
     'Composite panels showing roughness values exceeding tolerance. Requires additional polishing but does not affect structural integrity.',
     'OPEN',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     NULL,
     NOW() - INTERVAL '8 days'),

    ('c1111111-1111-1111-1111-111111111114', 'NCR-2024-004',
     '33333333-3333-3333-3333-333333333333', 'AWA-001',
     'MINOR',
     'Aileron cable routing documentation incomplete',
     'Technical drawing shows cable paths but lacks detailed routing instructions. Fixed with updated documentation.',
     'CLOSED',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     NULL,
     NOW() - INTERVAL '10 days'),

    ('c1111111-1111-1111-1111-111111111115', 'NCR-2024-005',
     '44444444-4444-4444-4444-444444444444', 'HYD-004',
     'MAJOR',
     'Accumulator pressure gauge calibration drift',
     'Pressure transducers showing 5% drift from reference standard. Need recalibration procedure.',
     'UNDER_REVIEW',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '11111111-1111-1111-1111-111111111111',
     NOW() - INTERVAL '4 days');

-- Close one NCR for realism
UPDATE non_conformance SET closed_at = NOW() - INTERVAL '2 days' WHERE ncr_number = 'NCR-2024-004';

-- ─────────────────────────────────────────
-- RISK REGISTER
-- ─────────────────────────────────────────
INSERT INTO risk_register (risk_number, product_version_id, title, description, category, likelihood, impact, mitigation_plan, owner_user_id, status, reviewed_at) VALUES
    ('RISK-2024-001', '22222222-2222-2222-2222-222222222222',
     'Strut seal material degradation under extreme temperatures',
     'Elastomer seals may degrade in aircraft high-altitude environments (-55C to +85C cycles)',
     'MATERIAL', 4, 5,
     'Use aerospace-grade PTFE seals, conduct thermal cycling tests',
     '11111111-1111-1111-1111-111111111111', 'MITIGATING', NOW() - INTERVAL '2 days'),

    ('RISK-2024-002', '33333333-3333-3333-3333-333333333333',
     'Wing skin composite delamination during service',
     'Composite panels under cyclic loading may experience inter-ply separation',
     'STRUCTURAL', 3, 5,
     'Increase ply thickness by 15%, add edge protection',
     '11111111-1111-1111-1111-111111111111', 'MITIGATING', NULL),

    ('RISK-2024-003', '44444444-4444-4444-4444-444444444444',
     'Potential hydraulic fluid leakage at pump outlet',
     'High-pressure connections susceptible to micro-leaks over time',
     'OPERATIONAL', 2, 4,
     'Upgrade to aerospace-spec fittings with redundant sealing',
     '11111111-1111-1111-1111-111111111111', 'IDENTIFIED', NULL),

    ('RISK-2024-004', '33333333-3333-3333-3333-333333333333',
     'Incomplete maintenance manual for control surface rigging',
     'Some rigging procedures documented only in design notes, not in final manual',
     'DOCUMENTATION', 3, 2,
     'Consolidate all procedures into unified technical manual',
     '11111111-1111-1111-1111-111111111111', 'IDENTIFIED', NULL);

-- ─────────────────────────────────────────
-- QUALITY AUDITS
-- ─────────────────────────────────────────
INSERT INTO quality_audit (audit_number, audit_type, scope, scheduled_date, conducted_date, status, lead_auditor_id, summary) VALUES
    ('AUDIT-2024-Q1-001', 'INTERNAL',
     'LGS-002 manufacturing processes',
     CURRENT_DATE - INTERVAL '15 days', CURRENT_DATE - INTERVAL '10 days',
     'COMPLETED',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     '2 Critical findings: seal installation not per procedure, bearing preload inconsistent. 1 Major finding: documentation not updated for recent tooling changes.'),

    ('AUDIT-2024-Q1-002', 'INTERNAL',
     'AWA-001 design compliance to FAR Part 23',
     CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '5 days',
     'COMPLETED',
     'dddddddd-dddd-dddd-dddd-dddddddddddd',
     'Design meets structural requirements. Minor documentation updates needed for load cases.');

-- ─────────────────────────────────────────
-- USER SHADOWS (cached from LLM service)
-- ─────────────────────────────────────────
INSERT INTO user_shadow (user_id, employee_id, full_name, role, department) VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP-001', 'Alice Chen', 'ENGINEER', 'Structures'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EMP-002', 'Bob Kumar', 'ENGINEER', 'Manufacturing'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'EMP-003', 'Carol Johnson', 'ENGINEER', 'Systems'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'EMP-004', 'David Patel', 'ENGINEER', 'Structures'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'EMP-005', 'Evelyn Martinez', 'QUALITY_MANAGER', 'Quality'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'EMP-006', 'Frank Wu', 'TRAINER', 'Learning & Development');
