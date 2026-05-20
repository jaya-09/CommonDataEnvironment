-- V2__seed_aerospace_data.sql
-- LLM seed data: aerospace skills, users, courses, and certifications

-- ─────────────────────────────────────────
-- SKILLS
-- ─────────────────────────────────────────
INSERT INTO skill_master (skill_id, skill_code, name, description, category) VALUES
    ('51111111-1111-1111-1111-111111111111', 'SKIL-001', 'Structural Design & Analysis',
     'Competency in FEA, composite analysis, and aerospace structures', 'ENGINEERING'),
    ('52222222-2222-2222-2222-222222222222', 'SKIL-002', 'Manufacturing & Assembly',
     'Proficiency in aerospace manufacturing processes and quality control', 'MANUFACTURING'),
    ('53333333-3333-3333-3333-333333333333', 'SKIL-003', 'Systems Integration & Testing',
     'Integration testing, hardware-in-loop simulation, flight test', 'TESTING'),
    ('54444444-4444-4444-4444-444444444444', 'SKIL-004', 'Certification & Compliance',
     'Knowledge of FAR/CS-23 regulations and certification procedures', 'COMPLIANCE'),
    ('55555555-5555-5555-5555-555555555555', 'SKIL-005', 'Quality Assurance & Auditing',
     'Quality processes, audit procedures, traceability, compliance', 'QUALITY');

-- ─────────────────────────────────────────
-- USERS (fixed UUIDs match user_shadow in PLM/QLM)
-- ─────────────────────────────────────────
INSERT INTO user_profile (user_id, employee_id, email, full_name, role, department, competency_level) VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMP-001', 'alice.chen@aerospace.local', 'Alice Chen',
     'ENGINEER', 'Structures', 'SENIOR'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EMP-002', 'bob.kumar@aerospace.local', 'Bob Kumar',
     'ENGINEER', 'Manufacturing', 'SENIOR'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'EMP-003', 'carol.johnson@aerospace.local', 'Carol Johnson',
     'ENGINEER', 'Systems', 'MID'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'EMP-004', 'david.patel@aerospace.local', 'David Patel',
     'ENGINEER', 'Structures', 'JUNIOR'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'EMP-005', 'evelyn.martinez@aerospace.local', 'Evelyn Martinez',
     'QUALITY_MANAGER', 'Quality', 'SENIOR'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'EMP-006', 'frank.wu@aerospace.local', 'Frank Wu',
     'TRAINER', 'Learning & Development', 'SENIOR');

-- ─────────────────────────────────────────
-- TRAINING COURSES (tied to PLM lifecycle phases)
-- ─────────────────────────────────────────
INSERT INTO training_course (course_id, skill_id, course_code, title, description, mandatory_for_phase, passing_score, duration_hours) VALUES
    ('61111111-1111-1111-1111-111111111111', '51111111-1111-1111-1111-111111111111',
     'COURSE-001', 'Structural Analysis & FEA Fundamentals',
     'Introduction to finite element analysis for aerospace structures',
     'Design', 70, 24),
    ('62222222-2222-2222-2222-222222222222', '52222222-2222-2222-2222-222222222222',
     'COURSE-002', 'Composite Materials & Manufacturing',
     'Best practices in composite layup, cure, and quality control',
     'Development', 75, 20),
    ('63333333-3333-3333-3333-333333333333', '53333333-3333-3333-3333-333333333333',
     'COURSE-003', 'Systems Integration & Flight Testing',
     'Procedures for aircraft systems integration and flight test execution',
     'Testing', 80, 32),
    ('64444444-4444-4444-4444-444444444444', '54444444-4444-4444-4444-444444444444',
     'COURSE-004', 'FAR Part 23 Certification Essentials',
     'Key regulations and certification procedures for small aircraft',
     'Deployment', 85, 28),
    ('65555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555',
     'COURSE-005', 'Quality Audits & Process Verification',
     'Conducting effective quality audits and compliance verification',
     'Maintenance', 75, 16);

-- ─────────────────────────────────────────
-- CERTIFICATIONS
-- Alice (EMP-001): Fully certified - all 5 courses, active
-- Bob (EMP-002):   Missing FAR Part 23 (COURSE-004)
-- Carol (EMP-003): 2 active certs + 1 EXPIRED for testing
-- David (EMP-004): No certifications (still enrolling)
-- ─────────────────────────────────────────

-- Alice: all 5 active certs
INSERT INTO user_certification (user_id, course_id, cert_number, score, status, issued_at, expires_at) VALUES
    ('11111111-1111-1111-1111-111111111111', '61111111-1111-1111-1111-111111111111',
     'CERT-A1-001', 95, 'ACTIVE', NOW() - INTERVAL '180 days', NOW() + INTERVAL '180 days'),
    ('11111111-1111-1111-1111-111111111111', '62222222-2222-2222-2222-222222222222',
     'CERT-A1-002', 92, 'ACTIVE', NOW() - INTERVAL '180 days', NOW() + INTERVAL '180 days'),
    ('11111111-1111-1111-1111-111111111111', '63333333-3333-3333-3333-333333333333',
     'CERT-A1-003', 88, 'ACTIVE', NOW() - INTERVAL '160 days', NOW() + INTERVAL '200 days'),
    ('11111111-1111-1111-1111-111111111111', '64444444-4444-4444-4444-444444444444',
     'CERT-A1-004', 90, 'ACTIVE', NOW() - INTERVAL '140 days', NOW() + INTERVAL '220 days'),
    ('11111111-1111-1111-1111-111111111111', '65555555-5555-5555-5555-555555555555',
     'CERT-A1-005', 87, 'ACTIVE', NOW() - INTERVAL '120 days', NOW() + INTERVAL '240 days');

-- Bob: 4/5 certs (missing FAR Part 23 / COURSE-004)
INSERT INTO user_certification (user_id, course_id, cert_number, score, status, issued_at, expires_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '61111111-1111-1111-1111-111111111111',
     'CERT-B2-001', 85, 'ACTIVE', NOW() - INTERVAL '150 days', NOW() + INTERVAL '210 days'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '62222222-2222-2222-2222-222222222222',
     'CERT-B2-002', 88, 'ACTIVE', NOW() - INTERVAL '140 days', NOW() + INTERVAL '220 days'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '63333333-3333-3333-3333-333333333333',
     'CERT-B2-003', 82, 'ACTIVE', NOW() - INTERVAL '130 days', NOW() + INTERVAL '230 days'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '65555555-5555-5555-5555-555555555555',
     'CERT-B2-005', 80, 'ACTIVE', NOW() - INTERVAL '100 days', NOW() + INTERVAL '260 days');

-- Carol: 2 active + 1 EXPIRED
INSERT INTO user_certification (user_id, course_id, cert_number, score, status, issued_at, expires_at) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '61111111-1111-1111-1111-111111111111',
     'CERT-C3-001', 82, 'ACTIVE', NOW() - INTERVAL '200 days', NOW() + INTERVAL '160 days'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '62222222-2222-2222-2222-222222222222',
     'CERT-C3-002', 78, 'ACTIVE', NOW() - INTERVAL '180 days', NOW() + INTERVAL '180 days'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '63333333-3333-3333-3333-333333333333',
     'CERT-C3-003', 81, 'EXPIRED', NOW() - INTERVAL '400 days', NOW() - INTERVAL '30 days');

-- Evelyn: 1 QA cert
INSERT INTO user_certification (user_id, course_id, cert_number, score, status, issued_at, expires_at) VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '65555555-5555-5555-5555-555555555555',
     'CERT-E5-005', 94, 'ACTIVE', NOW() - INTERVAL '90 days', NOW() + INTERVAL '270 days');

-- Frank: trainer cert
INSERT INTO user_certification (user_id, course_id, cert_number, score, status, issued_at, expires_at) VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '65555555-5555-5555-5555-555555555555',
     'CERT-F6-005', 90, 'ACTIVE', NOW() - INTERVAL '60 days', NOW() + INTERVAL '300 days');

-- ─────────────────────────────────────────
-- ENROLLMENTS (David is currently in training)
-- ─────────────────────────────────────────
INSERT INTO training_enrollment (user_id, course_id, status, trigger_source, deadline, started_at) VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '61111111-1111-1111-1111-111111111111',
     'IN_PROGRESS', 'MANUAL', CURRENT_DATE + INTERVAL '30 days', NOW() - INTERVAL '5 days'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '62222222-2222-2222-2222-222222222222',
     'ENROLLED', 'MANUAL', CURRENT_DATE + INTERVAL '45 days', NULL);
