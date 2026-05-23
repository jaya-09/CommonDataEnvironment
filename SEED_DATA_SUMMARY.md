# Seed Data & Testing Setup - Summary

## What I've Created For You

### 1. **Three Data Loader Classes** (Auto-Load on Startup)

#### PlmDataLoader.java (`plm-service/src/main/java/com/cde/plm/seed/`)
- **Purpose**: Populates realistic aerospace product data
- **What it loads**:
  - 7 lifecycle phases (Concept → Maintenance)
  - 4 products with aerospace names:
    - Main Wing Assembly (AWA-001) - Ready for phase advancement
    - Landing Gear System (LGS-002) - BLOCKED by 2 CRITICAL NCRs
    - Cockpit Control Panel (CCP-003) - In early phase
    - Hydraulic Power System (HYD-004) - In testing phase
  - Hierarchical BOM components for each product
  - Component types: ASSEMBLY, STRUCTURAL, COMPOSITE, CONTROL_SURFACE, MECHANICAL, AVIONICS

#### QlmDataLoader.java (`qlm-service/src/main/java/com/cde/qlm/seed/`)
- **Purpose**: Populates quality management data
- **What it loads**:
  - 5 NCRs (Non-Conformances):
    - 2 CRITICAL for landing gear (will block phase advancement)
    - 2 MAJOR (one open, one closed)
    - 1 MINOR (closed)
  - 4 Risks with realistic aerospace hazards:
    - Score 20 (RED): Strut seal material degradation
    - Score 18 (RED): Wing composite delamination
    - Score 12 (AMBER): Hydraulic fluid leakage
    - Score 6 (GRAY): Documentation gaps
  - 2 Quality audits (manufacturing & design review)

#### LlmDataLoader.java (`llm-service/src/main/java/com/cde/llm/seed/`)
- **Purpose**: Populates learning & staff management data
- **What it loads**:
  - 6 users with different competency levels:
    - Alice Chen (SENIOR) - Fully certified for all phases ✅
    - Bob Kumar (SENIOR) - Missing 1 cert (FAR Part 23 Certification) ⚠️
    - Carol Johnson (MID) - 1 cert expired (Systems Integration) ⚠️
    - David Patel (JUNIOR) - No certs, currently enrolling ❌
    - Evelyn Martinez (QUALITY_MANAGER) - Certified
    - Frank Wu (TRAINER) - Certified
  - 5 training courses tied to lifecycle phases
  - Realistic certification scenarios (active, expired, missing)
  - 2 training enrollments in progress

### 2. **Two Comprehensive Test Guides**

#### TEST_SCENARIOS.md
- **10 detailed test scenarios** with step-by-step instructions
- Each scenario demonstrates a specific workflow:
  1. Phase gate PASS (no blockers)
  2. Phase gate BLOCKED by CRITICAL NCR
  3. Phase gate BLOCKED by missing certification
  4. Change request workflow (DRAFT → APPROVED)
  5. BOM management (view/edit hierarchy)
  6. NCR → CAPA workflow (quality tracking)
  7. Risk register & heatmap
  8. Dashboard overview
  9. Audit trail & traceability
  10. Manual data entry for custom testing
- Includes key insights and summary table

#### SEED_DATA_QUICKSTART.md
- Quick reference guide for getting started
- How to use the seed data
- Database reset instructions
- Debugging tips
- Complete workflow walkthrough (45 min)
- Database query examples

### 3. **This Summary Document**
- Overview of what was created
- How to use it
- Key test scenarios to try
- Interview preparation tips

---

## How to Use This

### Step 1: Start the Application
```bash
cd "C:\Users\DARSHINI\Downloads\working code CDE 18MAY\cde-platform"
docker compose up --build
```

**First-time startup**:
- Services compile and start (~10-15 minutes)
- Databases initialize
- Data loaders detect empty DB and populate seed data
- Check logs for: `"XxxDataLoader: ... seed data loaded successfully"`

**Subsequent startups**:
- Loaders detect existing data
- Skip loading (no duplicates)
- App starts faster (~30-60 seconds)

### Step 2: Access the Application
- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Service Swagger UIs**:
  - PLM: http://localhost:8081/swagger-ui.html
  - QLM: http://localhost:8082/swagger-ui.html
  - LLM: http://localhost:8083/swagger-ui.html
  - Analytics: http://localhost:8084/swagger-ui.html

### Step 3: Follow Test Scenarios
Open `TEST_SCENARIOS.md` and work through scenarios in this order:

1. **Scenario 1** (5 min): Phase gate passing → understand success case
2. **Scenario 2** (5 min): Phase gate blocked by NCR → understand quality enforcement
3. **Scenario 3** (5 min): Phase gate blocked by missing cert → understand staff readiness
4. **Scenario 4** (5 min): Change requests → understand engineering workflow
5. **Scenario 5** (5 min): BOM management → understand product structure
6. **Scenario 6** (5 min): NCR→CAPA → understand quality response
7. **Scenario 7** (5 min): Risk register → understand risk visibility
8. **Scenario 8** (5 min): Dashboard → understand executive overview
9. **Scenario 9** (5 min): Audit trail → understand traceability
10. **Scenario 10** (10 min): Manual testing → test your own ideas

### Step 4: Reset and Retry
After completing scenarios, reset the database:
```bash
docker compose down -v
docker compose up --build
```
This clears all data and reloads fresh seed data.

---

## Key Testing Insights

### Phase Gate Behavior (Core Feature to Understand)

**Phase gates are async checkpoints** that prevent unqualified versions from advancing.

```
Version Advancement Request
       ↓
Phase Gate Check (async, ~2 seconds)
       ├─ QLM Check: Count open CRITICAL NCRs
       │  ✓ 0 CRITICAL → PASS
       │  ✗ ≥1 CRITICAL → BLOCK
       │
       └─ LLM Check: Verify staff certifications
          ✓ All staff have active certs for phase → PASS
          ✗ Any staff missing/expired cert → BLOCK

Both checks must PASS → Version advances
Either check fails → Version stays at current phase
```

### Users & Certification Status

| User | Role | Competency | Status |
|------|------|------------|--------|
| Alice Chen | Engineer | SENIOR | ✅ Can pass ALL gates |
| Bob Kumar | Engineer | SENIOR | ⚠️ Blocked on CERTIFICATION phase (missing FAR-23 cert) |
| Carol Johnson | Engineer | MID | ⚠️ Blocked on TESTING phase (cert expired) |
| David Patel | Engineer | JUNIOR | ❌ Blocked on ALL phases (no certs) |
| Evelyn Martinez | Quality Manager | SENIOR | ✅ Can pass all gates |
| Frank Wu | Trainer | SENIOR | ✅ Can pass all gates |

### Blocking Scenarios in Seed Data

| Product | Issue | Type | Trigger |
|---------|-------|------|---------|
| Landing Gear v2.1.0 | 2 CRITICAL NCRs (strut seal, bearing fatigue) | Quality Block | Try advancing from CONCEPT |
| Wing v1.0.0 | 1 MAJOR NCR (surface finish) | Non-blocking | Try advancement - passes |
| Hydraulic v3.2.1 | Bob missing FAR-23 cert | Staff Block | Try CERTIFICATION phase |
| Cockpit v1.0.0 | None, but in DRAFT | Baseline | Use for safe testing |

---

## What Each Test Scenario Teaches You

| # | Scenario | Key Concept | Time |
|---|----------|-------------|------|
| 1 | Phase Gate PASS | Success case, no blockers, async polling UI | 5 min |
| 2 | NCR Block Gate | Quality enforcement, CRITICAL NCRs hard-block, how to resolve | 5 min |
| 3 | Cert Block Gate | Staff readiness, missing/expired cert blocks, enrollment needed | 5 min |
| 4 | Change Requests | Engineering control, approval workflow, audit trail | 5 min |
| 5 | BOM | Product structure, hierarchical components, composition tracking | 5 min |
| 6 | NCR→CAPA | Quality response, linking issues to corrective actions | 5 min |
| 7 | Risk Register | Risk visibility, heatmap, mitigation status tracking | 5 min |
| 8 | Dashboard | Executive overview, KPIs, recent activity, cross-system view | 5 min |
| 9 | Audit Trail | Traceability, compliance proof, who/what/when/source | 5 min |
| 10 | Manual Testing | Create your own products/NCRs/users to test edge cases | 10 min |

---

## For Interview Preparation

### Talking Points

1. **Phase Gate Pattern**
   - "Phase gates enforce quality checkpoints without blocking the system."
   - "Uses async pub/sub to collect results from QLM and LLM in parallel."
   - "Each service independently validates and returns results."

2. **Quality Discipline**
   - "CRITICAL NCRs block advancement — system enforces compliance."
   - "User can see exact reason for block and knows what to fix."
   - "Example: Landing Gear has 2 CRITICAL NCRs, cannot advance until closed."

3. **Staff Readiness**
   - "Phase advancement requires staff to be certified for that phase."
   - "Can have active cert, expired cert, or missing cert — each triggers different UI flow."
   - "Example: Bob missing FAR-23 cert blocks CERTIFICATION phase."

4. **Traceability**
   - "Every action is audited with who/what/when/source."
   - "Enables regulatory compliance verification."
   - "Example: See exact audit trail for a change request approval."

5. **Real-World Complexity**
   - "Product has BOM hierarchy (component composition)."
   - "Change requests go through formal approval."
   - "Risk register tracks hazards and mitigation."
   - "This is how real aerospace engineering works."

### Demo Flow (30 minutes)

1. **Open Dashboard** (2 min)
   - Point out: 4 products, 5 NCRs, 2 CRITICAL, 6 users, 4 risks
   - Explain the KPIs

2. **Attempt Landing Gear Advancement** (3 min)
   - Show BLOCKED result with reason
   - Explain: 2 CRITICAL NCRs prevent progression

3. **Close the NCRs** (3 min)
   - Update NCR status to CLOSED
   - Show audit trail entry

4. **Retry Advancement** (3 min)
   - Now PASSES (only 1 MAJOR NCR, non-critical)
   - Version advances to next phase

5. **Explore Wing BOM** (3 min)
   - Show hierarchical component tree
   - Add a new component
   - Explain: enables traceability for complex products

6. **Create & Approve CR** (5 min)
   - Create change request
   - Submit for approval
   - Approve with comments
   - Show audit trail

7. **View Risk Register** (3 min)
   - Show heatmap with color-coded risks
   - Explain risk scores (likelihood × impact)

8. **Explain Architecture** (5 min)
   - Why pub/sub instead of REST?
   - How async gates handle timeouts
   - Why event-driven is better for resilience

---

## Troubleshooting

### Build Fails
```bash
# Clean everything and rebuild
docker compose down -v
docker system prune -a
docker compose up --build
```

### Services Won't Start
```bash
# Check logs
docker compose logs plm-service
docker compose logs qlm-service
docker compose logs llm-service

# Look for: "seed data loaded successfully"
```

### Seed Data Not Loaded
```bash
# Check database directly
docker exec -it cde-plm-db psql -U postgres -d cde_plm -c "SELECT COUNT(*) FROM product;"

# Should show: count
#             -------
#                 4
```

### Phase Gate Returns 500 Error
- Check if QLM and LLM services are running
- Verify pub/sub topic subscriptions exist
- Check logs for event processing errors

---

## Files You Now Have

```
cde-platform/
├── TEST_SCENARIOS.md                    ← Read this for detailed scenarios
├── SEED_DATA_QUICKSTART.md             ← Quick reference guide
├── SEED_DATA_SUMMARY.md                ← This file
├── plm-service/src/main/java/com/cde/plm/seed/
│   └── PlmDataLoader.java              ← Product data loader
├── qlm-service/src/main/java/com/cde/qlm/seed/
│   └── QlmDataLoader.java              ← Quality data loader
└── llm-service/src/main/java/com/cde/llm/seed/
    └── LlmDataLoader.java              ← Staff & training data loader
```

---

## Next Actions

1. ✅ **Understand the setup** (reading this document)
2. ⏳ **Start the application** (`docker compose up --build`)
3. ⏳ **Wait for seed data to load** (~10-15 min first time)
4. ⏳ **Open http://localhost:3000**
5. ⏳ **Follow TEST_SCENARIOS.md step by step**
6. ⏳ **Practice the 30-minute demo for interviews**
7. ⏳ **Create your own test data to understand edge cases**

---

## Interview Questions You Can Now Answer

After working through these scenarios, you'll be able to explain:

- "How does the system enforce quality discipline?" → Landing Gear NCR block scenario
- "How do you prevent unqualified staff from working on systems?" → Carol's expired cert scenario
- "How does the system track who changed what?" → Audit trail examples
- "How do you manage complex product structures?" → BOM hierarchy examples
- "What happens if a service crashes during a phase gate check?" → Timeout mechanism
- "Why use pub/sub instead of REST?" → Decoupling, resilience, async saga pattern
- "How do you ensure compliance in aerospace?" → Audit trail, phase gates, NCR enforcement

You've now got realistic test data and comprehensive scenarios. This is exactly what you need to understand the system deeply and communicate it effectively in interviews. Good luck! 🚀

---

## One More Thing: Understanding Data Relationships

```
Product (4)
  ├─ AWA-001: Main Wing Assembly
  │   └─ v1.0.0 (DESIGN phase)
  │       ├─ BOM: Main Wing (root) → Spar Box, Skin Panel (2x), Ailerons (2x)
  │       ├─ NCRs: 1 MAJOR (surface finish)
  │       └─ Phase Gate: PASS (ready to advance)
  │
  ├─ LGS-002: Landing Gear System
  │   └─ v2.1.0 (CONCEPT phase)
  │       ├─ BOM: Landing Gear (root) → Strut (3x), Wheel (3x)
  │       ├─ NCRs: 2 CRITICAL (seal, bearing) ❌ BLOCKED
  │       └─ Phase Gate: BLOCKED by NCRs
  │
  ├─ CCP-003: Cockpit Control Panel
  │   └─ v1.0.0 (DRAFT phase)
  │       ├─ BOM: Control Panel (root) → Display Unit
  │       ├─ NCRs: None
  │       └─ Phase Gate: Ready to advance
  │
  └─ HYD-004: Hydraulic Power System
      └─ v3.2.1 (TESTING phase)
          ├─ BOM: Hydraulic Unit (root) → Pump (2x), Accumulator (2x)
          ├─ NCRs: 1 MAJOR (calibration)
          └─ Phase Gate: Blocks on CERTIFICATION (Bob missing cert)

Quality Issues (5 NCRs)
├─ 2 CRITICAL (Landing Gear) ← BLOCKS
├─ 2 MAJOR
└─ 1 MINOR (closed)

Risks (4)
├─ Score 20 (RED): Strut seal
├─ Score 18 (RED): Wing delamination
├─ Score 12 (AMBER): Hydraulic leak
└─ Score 6 (GRAY): Documentation

Users (6)
├─ Alice: Fully certified ✅
├─ Bob: Missing 1 cert
├─ Carol: 1 cert expired
├─ David: No certs
├─ Evelyn: Certified
└─ Frank: Certified

Courses (5, tied to phases)
├─ Structural Analysis → DESIGN
├─ Composite Materials → DESIGN_REVIEW
├─ Systems Integration → TESTING
├─ FAR Part 23 → CERTIFICATION
└─ Quality Audits → DEPLOYMENT
```

This is a complete, interconnected system. Explore it systematically! 🎯
