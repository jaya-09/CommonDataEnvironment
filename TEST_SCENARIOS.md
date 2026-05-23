# CDE Platform - Aerospace Test Scenarios

This guide walks you through all the major functionality using the seeded aerospace industry data. Each scenario demonstrates a specific workflow.

---

## Seed Data Overview

### Products (PLM)
1. **Main Wing Assembly (AWA-001)** - v1.0.0 in DESIGN phase, ready for phase advancement
2. **Landing Gear System (LGS-002)** - v2.1.0 in CONCEPT phase, **BLOCKED** by 2 CRITICAL NCRs
3. **Cockpit Control Panel (CCP-003)** - v1.0.0 in DRAFT status, new product
4. **Hydraulic Power System (HYD-004)** - v3.2.1 in TESTING phase

### Users (LLM)
- **Alice Chen** (EMP-001) - SENIOR structures engineer, all certs active ✓
- **Bob Kumar** (EMP-002) - SENIOR manufacturing engineer, missing 1 cert (FAR Part 23)
- **Carol Johnson** (EMP-003) - MID systems engineer, 1 expired cert (Systems Integration)
- **David Patel** (EMP-004) - JUNIOR structures engineer, no certs, currently enrolling
- **Evelyn Martinez** (EMP-005) - Quality Manager
- **Frank Wu** (EMP-006) - Trainer

### NCRs & Risks (QLM)
- **Landing Gear**: 2 CRITICAL (open), 1 MAJOR (open) — will BLOCK phase advancement
- **Wing**: 1 MAJOR (open), 1 MINOR (closed)
- **Hydraulic**: 1 MAJOR (under review)
- **4 Risks** registered with varying scores (6, 12, 18, 20)

---

## Test Scenario 1: Phase Gate PASS (No Blocking Issues)

**Goal**: Successfully advance a product version through a phase gate when no quality or certification issues exist.

**Product**: Main Wing Assembly (AWA-001) v1.0.0

**Setup**:
- Version is in DESIGN phase
- Only 1 MAJOR open NCR (non-critical)
- Wing product has no CRITICAL NCRs
- Alice Chen is certified for all phases

**Steps**:
1. Navigate to Dashboard → PLM → Products → "Main Wing Assembly"
2. Click version "1.0.0"
3. Scroll to "Phase Advancement" section
4. Click "Advance to Design Review"
5. UI shows blue spinner: "Running gate checks..."
6. After ~2 seconds:
   - ✓ QLM check PASSES (only 1 MAJOR NCR, no CRITICAL)
   - ✓ LLM check PASSES (Alice is certified)
   - Status updates: **"PASSED"** with green checkmark
7. Version now in DESIGN_REVIEW phase

**Expected Behavior**:
- Phase gate is polled every 2 seconds until result
- Once both QLM and LLM respond, version advances automatically
- Dashboard shows updated phase in product card

**Key Insight**: A MAJOR NCR doesn't block advancement — only CRITICAL ones do.

---

## Test Scenario 2: Phase Gate BLOCKED by Critical NCR

**Goal**: Witness phase gate blocking due to unresolved critical quality issues.

**Product**: Landing Gear System (LGS-002) v2.1.0

**Setup**:
- Version in CONCEPT phase
- 2 CRITICAL open NCRs (strut seal degradation, bearing fatigue)
- Cannot advance until both closed

**Steps**:
1. Navigate to Dashboard → PLM → Products → "Landing Gear System"
2. Click version "2.1.0"
3. Scroll to "Phase Advancement" section
4. Click "Advance to Design Review"
5. UI shows spinner: "Running gate checks..."
6. After ~2 seconds:
   - ✗ QLM check FAILS: "2 open CRITICAL NCRs found"
   - UI shows: **"BLOCKED: 2 open critical non-conformances"**
   - Version stays in CONCEPT phase

**Steps to Unblock**:
1. Navigate to QLM → Non-Conformances
2. Find "NCR-2024-001: Strut damper seal degradation"
3. Click to edit → update Status to "CLOSED"
4. Repeat for "NCR-2024-002: Wheel bearing assembly fatigue"
5. Try phase advancement again
6. Now with 0 CRITICAL NCRs, gate should PASS

**Expected Behavior**:
- Phase gate status shows real-time QLM result
- Blocking reason is clearly stated
- User knows exactly what to fix

**Key Insight**: CRITICAL NCRs are hard blocks. System forces quality compliance.

---

## Test Scenario 3: Phase Gate BLOCKED by Missing Certification

**Goal**: Phase advancement fails because a required staff member lacks certification.

**Product**: Hydraulic Power System (HYD-004) v3.2.1

**Scenario A: Bob Kumar (missing one cert)**:
1. Navigate to PLM → Products → "Hydraulic Power System" → v3.2.1
2. Scroll to "Phase Advancement"
3. Click "Advance to Certification"
4. Gate checks run...
5. Result: **"BLOCKED: 1 staff member uncertified for this phase"**
   - Bob Kumar is missing FAR Part 23 Certification course
   - Version stays in TESTING phase

**Steps to Resolve**:
1. Navigate to LLM → Trainings & Certifications
2. Find Bob Kumar in user list → "Enroll in Course"
3. Select "FAR Part 23 Certification Essentials" → Enroll
4. (In real scenario, Bob completes training and exam)
5. Training system issues certificate
6. Cache clears, phase readiness updates
7. Try phase advancement again → now PASSES

**Scenario B: Carol Johnson (expired cert)**:
1. Try to advance same version with Carol as required staff
2. Result: **"BLOCKED: 1 staff member has expired certification"**
   - Carol's "Systems Integration & Flight Testing" cert expired 30 days ago
   - Requires renewal before proceeding

**Key Insight**: LLM service validates not just presence of cert, but also validity (not expired).

---

## Test Scenario 4: Creating & Approving Change Requests

**Goal**: Understand the complete change request workflow.

**Product**: Main Wing Assembly (AWA-001) v1.0.0

**Steps - Create CR**:
1. Navigate to PLM → Products → "Main Wing Assembly" → v1.0.0
2. Scroll to "Change Requests" section
3. Click "+ New Change Request"
4. Fill form:
   - **Title**: "Increase aileron skin thickness for fatigue life"
   - **Type**: ENHANCED (new capability, not just fixing something)
   - **Description**: "Current thickness adequate for +2.5g loads. Enhanced design for +3.5g military spec."
   - **Reason**: Design improvement
   - **Impact Analysis**: "Adds 2 kg weight, requires new fastener holes in center section"
5. Click "Create" → Status = DRAFT
6. Click "Submit for Approval" → Status = SUBMITTED

**Steps - Approve CR**:
1. As Admin/Approver, navigate to QLM → Change Requests
2. Find "CR-001-..." with status SUBMITTED
3. Click to review
4. Approver can:
   - **Approve**: Comments box → "Approved. New fatigue analysis confirms 3.5g capability" → Status = APPROVED
     - System publishes ChangeRequestApprovedEvent
     - LLM might auto-enroll staff in affected training
   - **Reject**: Comments → Status = REJECTED
     - Version cannot proceed until CR is resolved

**Expected Behavior**:
- CR follows rigid state machine (DRAFT → SUBMITTED → APPROVED/REJECTED)
- Audit trail logs who approved and when
- Published event triggers downstream actions

**Key Insight**: Change requests are the mechanism to track engineering changes with full traceability.

---

## Test Scenario 5: Bill of Materials (BOM) Management

**Goal**: View hierarchical component structure and understand product composition.

**Product**: Main Wing Assembly (AWA-001) v1.0.0

**Steps**:
1. Navigate to PLM → Products → "Main Wing Assembly" → v1.0.0
2. Scroll to "Bill of Materials" section
3. View tree structure:
   ```
   └─ Main Wing Assembly (WING-001) [1 UNIT]
      ├─ Wing Spar Box (WING-SPAR-001) [1 UNIT]
      ├─ Wing Skin Panel (WING-SKIN-001) [2 PANELs]
      └─ Ailerons Control Surface (AILERONS-001) [2 UNITs]
   ```
4. Click to expand/collapse any component
5. Click on component to see details:
   - Type (ASSEMBLY, STRUCTURAL, COMPOSITE, CONTROL_SURFACE)
   - Quantity and unit
   - Description/notes

**Add Component**:
1. Click "+ Add Component"
2. Select parent: "Main Wing Assembly"
3. Fill:
   - Component Code: "WING-STRUT-001"
   - Name: "Internal Wing Strut"
   - Type: STRUCTURAL
   - Quantity: 2
   - Unit: UNIT
   - Description: "Vertical strut resisting shear loads in wing spar box"
4. Click "Add" → Component added to BOM
5. Refresh view → new strut visible under Main Wing Assembly

**Expected Behavior**:
- BOM is cached in Redis for fast retrieval
- Component hierarchy persists across version changes
- Adding component updates cache and audit trail

**Key Insight**: BOM enables detailed product composition tracking for complex aerospace products.

---

## Test Scenario 6: NCR → CAPA Workflow

**Goal**: Report a quality issue (NCR) and track corrective action (CAPA).

**Product**: Any product with open NCRs

**Steps - Create NCR**:
1. Navigate to QLM → Quality Issues → "Non-Conformances"
2. Click "+ New NCR"
3. Fill:
   - **Product Version**: Select version experiencing issue
   - **Title**: "Fastener corrosion in wing root area"
   - **Severity**: MAJOR
   - **Description**: "During pre-delivery inspection, surface corrosion found on aluminum fasteners in wing root due to insufficient sealant."
4. Click "Create" → NCR-202X-00N created, Status = OPEN

**Steps - Create CAPA**:
1. In same NCR detail view, click "Link CAPA"
2. Click "+ New CAPA"
3. Fill:
   - **Title**: "Improve sealant application procedure and fastener coating"
   - **Root Cause**: "Assembly procedure doesn't specify sealant coverage verification"
   - **Action**: "Update assembly work instruction with photo guide for sealant application; switch to stainless fasteners"
   - **Owner**: Assign to quality manager
   - **Target Closure Date**: 30 days from now
4. Click "Create" → CAPA linked to NCR

**Steps - Close NCR**:
1. Once CAPA is implemented, update NCR Status to "CLOSED"
2. System:
   - Records closedAt timestamp
   - Publishes NcrClosedEvent
   - Analytics service receives event and updates dashboard
3. NCR no longer blocks phase gates

**Expected Behavior**:
- NCRs and CAPAs are linked for traceability
- Closing NCR updates phase readiness for relevant versions
- Audit log shows who closed and when

**Key Insight**: QLM enforces quality discipline — issues must be documented and resolved.

---

## Test Scenario 7: Risk Register & Heatmap

**Goal**: Understand risk management and track mitigation.

**View Risk Register**:
1. Navigate to Dashboard or QLM → Risk Register
2. View all 4 risks with scores:
   - **RISK-001** (Score 20): Strut seal material degradation — RED (high)
   - **RISK-002** (Score 18): Wing composite delamination — RED (high)
   - **RISK-003** (Score 12): Hydraulic fluid leakage — AMBER (medium)
   - **RISK-004** (Score 6): Documentation gaps — GRAY (low)

**Update Risk Status**:
1. Click RISK-001 (score 20)
2. Update:
   - **Status**: MITIGATING (design action in progress)
   - **Mitigation Plan**: "Use PTFE seals, conduct -40 to +85°C thermal cycling tests per AMS 2301"
   - **Owner**: Assign to structures engineer
3. Click "Save" → Status updated, audit logged

**Expected Behavior**:
- Risk score = Likelihood × Impact
- Color coding: Red (20+), Amber (12-19), Gray (<12)
- Score and status visible on dashboard heatmap
- Analytics aggregates risks across all products

**Key Insight**: Risk register makes hazards visible and tracks mitigation progress.

---

## Test Scenario 8: Dashboard Overview

**Goal**: See real-time cross-system status at a glance.

**Navigate to**: Dashboard (home)

**Visible Stats**:
- **Products**: 4 active products
- **Open NCRs**: 5 total (2 CRITICAL blocking landing gear)
- **Open CAPAs**: N with corresponding NCRs
- **Users**: 6 registered
- **Risks**: 4 identified, 2 being mitigated

**Recent Activity Card**:
- Shows last 8 changes: NCR raised, CAPA closed, Phase advanced, User certified
- Sorted by timestamp

**Product Lifecycle Card**:
- Lists products with their current phase
- Quick links to each product

**Quality Status Card**:
- Grid showing: Open NCRs, Critical count, Open CAPAs, High Risks

**Risk Heatmap** (if risks exist):
- Visual grid showing risk score bubbles
- Larger bubbles = higher risk score
- Color coded
- Clicking opens risk details

**Expected Behavior**:
- Dashboard updates as events flow through system
- All data comes from latest completed queries
- No real-time updates; user refreshes to see new data

**Key Insight**: Dashboard provides executive visibility across all services.

---

## Test Scenario 9: Audit Trail & Traceability

**Goal**: Verify that all actions are logged and traceable.

**View Audit Log**:
1. Navigate to PLM → Products → "Main Wing Assembly" → v1.0.0
2. Scroll to bottom → "Audit Trail"
3. View entries (newest first):
   ```
   2024-XX-XX 14:32:15  Phase Advancement  SUBMITTED    Alice Chen  via API
   2024-XX-XX 14:30:00  Status Changed     DRAFT→IN_PROGRESS  System  via PUBSUB
   2024-XX-XX 14:25:00  Created            -→IN_PROGRESS      Alice Chen  via API
   ```

**Trace an NCR**:
1. Navigate to QLM → NCRs → "NCR-2024-001"
2. Scroll to audit section:
   ```
   Created         Bob (QA Inspector)      2024-XX-XX 10:00:00
   Updated         Alice (Eng Manager)     2024-XX-XX 11:30:00  (assigned to)
   Status Changed  Alice (Eng Manager)     2024-XX-XX 16:45:00  (OPEN→UNDER_REVIEW)
   ```
3. Each entry shows exactly who did what, when, and via what source (API, PUBSUB, etc.)

**Expected Behavior**:
- Every state change creates audit entry
- Audit immutable (stored in `traceability_audit_log` table)
- Source field shows if user action or event-driven
- Enables compliance investigations

**Key Insight**: Audit trail ensures regulatory traceability for aerospace certification.

---

## Test Scenario 10: Data Entry & Manual Testing

**After exploring pre-seeded data, try creating your own:**

### Create a New Product
1. PLM → Products → "+ New Product"
2. Fill: Code, Name, Description
3. Create version
4. Add BOM components
5. Advance through phases manually

### Create Custom NCR
1. QLM → NCRs → "+ New"
2. Assign to product version
3. Set severity
4. Watch phase gates respond to CRITICAL NCRs

### Enroll User in Training
1. LLM → Users → Select user
2. "Enroll in Course"
3. Watch phase readiness cache invalidate
4. Attempt phase advancement and observe gate behavior

### Simulate Phase Advancement Sequence
1. Pick a product (e.g., Cockpit Control Panel, currently in DRAFT)
2. Create a BOM for it
3. Try advancing from DRAFT → CONCEPT
4. Resolve any blocking issues (NCRs or certs)
5. Advance to DESIGN
6. Repeat until DEPLOYED

---

## Key Testing Insights

### Phase Gate Behavior
- **QLM Check**: Counts open CRITICAL NCRs for version
  - 0 CRITICAL → ✓ PASS
  - ≥1 CRITICAL → ✗ BLOCK
  
- **LLM Check**: Verifies staff certification
  - All staff have active certs for phase → ✓ PASS
  - Any staff missing/expired cert → ✗ BLOCK

- **Both must PASS** for version to advance

### User Permission Scenarios
- **Alice** can advance most versions (all certs active)
- **Bob** blocks on Certification phase (missing FAR Part 23 cert)
- **Carol** blocks on Testing/Deployment phases (expired Systems Integration cert)
- **David** blocks on all phases (no certifications yet)

### Blocking Scenarios to Trigger
1. **Landing Gear** → Try advancement → 2 CRITICAL NCRs block
2. **Wing** → Attempt deployment → Only 1 MAJOR NCR, should eventually pass
3. **Bob Kumar** → Try Certification phase → Missing 1 cert blocks
4. **Carol Johnson** → Try Testing phase → Expired cert blocks

---

## Monitoring & Debugging

### Check service logs:
```bash
docker compose logs -f plm-service
docker compose logs -f qlm-service
docker compose logs -f llm-service
```

### Check database directly:
```sql
-- View all products
SELECT product_code, name, status FROM product ORDER BY created_at DESC;

-- View pending phase gates
SELECT version_id, target_phase, status, qlm_result_received, llm_result_received 
FROM phase_gate_pending_check WHERE status = 'PENDING';

-- View NCRs by product
SELECT ncr_number, product_code, severity, status 
FROM non_conformance WHERE severity = 'CRITICAL' AND status = 'OPEN';

-- View user certifications
SELECT u.full_name, c.cert_number, tc.title, c.status, c.expires_at 
FROM user_certification c 
JOIN user_profile u ON c.user_id = u.user_id 
JOIN training_course tc ON c.course_id = tc.course_id 
ORDER BY u.full_name, c.expires_at;
```

---

## Summary Table: Which Seed Data Tests What

| Scenario | Product | Key Feature | Expected Outcome |
|----------|---------|-------------|------------------|
| 1. Pass Gate | Wing (v1.0.0) | Phase advancement no blockers | Advances to Design Review |
| 2. NCR Block | Landing Gear (v2.1.0) | Critical NCR blocks | Cannot advance, shows blocking reason |
| 3. Cert Block | Hydraulic (v3.2.1) | Missing certification | Cannot advance, shows uncertified staff |
| 4. Change Request | Wing (v1.0.0) | CR workflow | DRAFT→SUBMITTED→APPROVED |
| 5. BOM | Wing (v1.0.0) | Hierarchical components | View/edit component tree |
| 6. NCR→CAPA | Any | Quality issue tracking | NCR linked to remedial action |
| 7. Risk Register | All | Risk heatmap | View/update risk status & score |
| 8. Dashboard | System-wide | Executive overview | Real-time cross-system KPIs |
| 9. Audit Trail | All entities | Traceability | Immutable action log |
| 10. Manual Entry | New products | Custom testing | Create and test your own scenarios |

---

## Next Steps for Interview Prep

1. **Understand Phase Gates**: Try Scenario 1 (pass) and 2 (block) back-to-back
2. **Master Terminology**: NCR (issue), CAPA (fix), Phase Gate (checkpoint), Certification (competency requirement)
3. **Trace a Workflow**: Pick one product, advance it through 2-3 phases, resolve blockers
4. **Explain Traceability**: Show how audit trail proves compliance (for regulatory interviews)
5. **Discuss Architecture**: Why pub/sub instead of REST? (Answer: async gates, resilience)

Good luck! 🚀
