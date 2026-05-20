# Quick Start: Testing with Seed Data

## What's New

Three data loader classes have been added to automatically populate realistic aerospace test data on app startup:

1. **PlmDataLoader.java** (PLM Service)
   - 4 products with realistic aerospace names
   - BOM hierarchies for each product
   - Products at different lifecycle phases

2. **QlmDataLoader.java** (QLM Service)
   - 5 NCRs (2 CRITICAL, 2 MAJOR, 1 MINOR) with aerospace-specific issues
   - 4 Risks with varying scores for heatmap visualization
   - 2 Quality audits demonstrating manufacturing and design review

3. **LlmDataLoader.java** (LLM Service)
   - 6 users with different roles and competency levels
   - 5 training courses tied to lifecycle phases
   - Realistic certification scenarios (active, expired, missing)
   - Enrollments showing in-progress training

## How to Use

### 1. Start the Application (First Time)
```bash
cd cde-platform
docker compose up --build
```

**On First Startup:**
- Each service initializes its schema
- DataLoaders detect empty database
- Seed data is automatically populated
- Logs show "PLM seed data loaded successfully", etc.
- Application is ready to test

**On Subsequent Startups:**
- Loaders detect existing data
- Skip loading (idempotent)
- No duplicate data

### 2. Access the Application
Once running, navigate to:
- **Frontend**: http://localhost:3000
- **PLM Service Swagger**: http://localhost:8081/swagger-ui.html
- **QLM Service Swagger**: http://localhost:8082/swagger-ui.html
- **LLM Service Swagger**: http://localhost:8083/swagger-ui.html

### 3. Explore the Dashboard
1. Open http://localhost:3000
2. You'll see:
   - **4 Active Products** (Wing, Landing Gear, Cockpit, Hydraulic)
   - **5 Open NCRs** with 2 critical ones blocking landing gear
   - **6 Registered Users** at various competency levels
   - **4 Risks** visible on the heatmap
   - **Recent Activity** showing all seeded entities

### 4. Test Key Scenarios

**Scenario A: Phase Gate Blocked by CRITICAL NCR**
1. Click "Products" → "Landing Gear System" → version 2.1.0
2. Scroll to "Phase Advancement"
3. Try "Advance to Design Review"
4. Gate runs checks...
5. Result: ❌ **BLOCKED** - 2 CRITICAL NCRs prevent advancement
6. Resolve by closing NCRs, then gate will PASS

**Scenario B: Phase Gate Blocked by Missing Certification**
1. Click "Products" → "Hydraulic Power System" → version 3.2.1
2. Try "Advance to Certification" phase
3. Result: ❌ **BLOCKED** - Bob Kumar missing FAR Part 23 certification
4. Bob needs enrollment in training before gate passes

**Scenario C: Phase Gate Passes (No Blockers)**
1. Click "Products" → "Main Wing Assembly" → version 1.0.0
2. Try "Advance to Design Review"
3. Gate checks run...
4. Result: ✅ **PASSED** - Only 1 MAJOR NCR (non-critical), Alice certified
5. Version advances to Design Review phase

**Scenario D: View BOM Hierarchy**
1. Any product version
2. Scroll to "Bill of Materials"
3. Expand/collapse component tree
4. Click component to see details

**Scenario E: Create a Change Request**
1. Click "Products" → "Main Wing Assembly" → v1.0.0
2. Scroll to "Change Requests"
3. Click "+ New Change Request"
4. Fill form: title, type, description, reason
5. Submit → Status = SUBMITTED
6. As approver, APPROVE/REJECT

**Scenario F: View Audit Trail**
1. Any entity (product, version, NCR, etc.)
2. Scroll to "Audit Trail" section
3. See who did what, when, and via what source

**Scenario G: Risk Heatmap**
1. Dashboard or QLM → Risk Register
2. View 4 risks with scores: 20 (RED), 18 (RED), 12 (AMBER), 6 (GRAY)
3. Click to update mitigation status

## User Credentials for Manual Testing

Since there's no auth layer yet, users are identifiable by:

| Name | Employee ID | Role | Status |
|------|-------------|------|--------|
| Alice Chen | EMP-001 | Engineer | ✅ Fully Certified |
| Bob Kumar | EMP-002 | Engineer | ⚠️ Missing 1 cert (FAR Part 23) |
| Carol Johnson | EMP-003 | Engineer | ⚠️ 1 expired cert (Systems Integration) |
| David Patel | EMP-004 | Engineer | ❌ No certs, in training |
| Evelyn Martinez | EMP-005 | Quality Manager | ✅ Fully Certified |
| Frank Wu | EMP-006 | Trainer | ✅ Fully Certified |

**Use these names when:**
- Approving change requests (assume you're representing them)
- Viewing phase readiness (gates check if THEY have current certs)
- Enrolling in training (update their competency)

## Database Reset

To clear seed data and start fresh:

```bash
# Stop containers and remove volumes (deletes databases)
docker compose down -v

# Restart with clean databases
docker compose up --build
```

This triggers re-seeding of all data.

## Debugging

### Check Service Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f plm-service
docker compose logs -f qlm-service
docker compose logs -f llm-service
```

### Look for seed data logs
In the logs, you'll see messages like:
```
plm-service  | INFO  Loading PLM seed data for aerospace...
plm-service  | INFO  Loaded 7 lifecycle phases
plm-service  | INFO  Loaded 4 products with versions and BOM
plm-service  | INFO  PLM seed data loaded successfully

qlm-service  | INFO  Loading QLM seed data for aerospace...
qlm-service  | INFO  Loaded 5 NCRs (2 CRITICAL blocking landing gear, 3 others)
qlm-service  | INFO  Loaded 4 risks with varying risk scores (20, 18, 12, 6)
qlm-service  | INFO  QLM seed data loaded successfully

llm-service  | INFO  Loading LLM seed data for aerospace...
llm-service  | INFO  Loaded 6 users
llm-service  | INFO  Loaded 5 courses tied to phases
llm-service  | INFO  Loaded certifications: Alice(all active), Bob(4/5), Carol(2/5, 1 expired), David(none)
llm-service  | INFO  LLM seed data loaded successfully
```

### Query Databases Directly

```bash
# Connect to PLM DB
docker exec -it cde-plm-db psql -U postgres -d cde_plm

# View products
SELECT product_code, name, status FROM product;

# View versions
SELECT version_number, current_phase, status FROM product_version;

# View BOM
SELECT component_code, name, type FROM bom_component;
```

## Test Workflow: Complete Product Lifecycle

Follow this step-by-step to understand the full system:

### 1. Observe Pre-Seeded State (5 min)
- View dashboard, notice 4 products, 2 critical blockers
- Inspect landing gear NCRs (understand the block)
- Look at wing BOM structure

### 2. Attempt Blocked Advancement (5 min)
- Try to advance landing gear from CONCEPT → DESIGN
- Gate returns BLOCKED with reason
- System shows QLM check failed

### 3. Resolve Quality Issue (5 min)
- Navigate to QLM → Non-Conformances
- Close the 2 CRITICAL landing gear NCRs
- Update status to CLOSED

### 4. Retry Phase Advancement (5 min)
- Return to landing gear product
- Try advancement again
- Gate should now PASS (only 1 MAJOR NCR, which is non-critical)

### 5. Test Successful Advancement (5 min)
- Wing product in DESIGN phase has no critical blockers
- Try advancing to DESIGN_REVIEW
- Gate should PASS immediately
- Version status updates

### 6. Create & Approve Change Request (5 min)
- Open wing product, version 1.0.0
- Create CR for "increase skin thickness"
- Submit for approval
- Approve CR (see how status changes)

### 7. Explore Audit Trail (5 min)
- View audit log for the product
- See each action, who did it, when, source (API/PUBSUB)
- Understand traceability

### 8. View Risk Register (5 min)
- QLM → Risk Register
- See 4 risks with scores
- Understand risk heatmap coloring

**Total Time: ~45 minutes to understand all core functionality**

## What Each Loader Does

### PlmDataLoader
```
Phase Lifecycle:
  1. CONCEPT → 2. DESIGN → 3. DESIGN_REVIEW → 4. TESTING 
  → 5. CERTIFICATION → 6. DEPLOYMENT → 7. MAINTENANCE

Products:
  • AWA-001 v1.0.0: DESIGN phase, 1 MAJOR NCR (non-blocking)
  • LGS-002 v2.1.0: CONCEPT phase, 2 CRITICAL NCRs (BLOCKING!)
  • CCP-003 v1.0.0: DRAFT, no quality issues
  • HYD-004 v3.2.1: TESTING phase, ready to advance

BOM Example (Wing):
  Main Wing Assembly
  ├─ Wing Spar Box (composite, structural)
  ├─ Wing Skin Panel (aluminum-lithium, 2x)
  └─ Ailerons Control Surface (movable, 2x)
```

### QlmDataLoader
```
NCRs (sorted by impact):
  • NCR-001 (CRITICAL): Landing gear strut seal degradation
  • NCR-002 (CRITICAL): Landing gear bearing fatigue (BLOCKS phase)
  • NCR-003 (MAJOR): Wing skin surface finish
  • NCR-004 (MINOR, CLOSED): Wing aileron documentation
  • NCR-005 (MAJOR): Hydraulic accumulator calibration

Risks (by score):
  • RISK-001 (20): Strut seal temperature degradation
  • RISK-002 (18): Wing composite delamination
  • RISK-003 (12): Hydraulic leakage
  • RISK-004 (6): Documentation gaps

Audits:
  • Manufacturing audit: Landing gear strut assembly
  • Design review audit: Wing assembly FAR compliance
```

### LlmDataLoader
```
Users (by competency):
  • Alice (SENIOR): All 5 certs, all active ✅
  • Bob (SENIOR): 4/5 certs (missing FAR-23)
  • Carol (MID): 2/5 certs, 1 expired ⚠️
  • David (JUNIOR): 0 certs, 2 enrollments in progress
  • Evelyn (SENIOR): Quality manager, certified
  • Frank (SENIOR): Trainer, certified

Courses (tied to phases):
  • Structural Analysis → DESIGN phase
  • Composite Materials → DESIGN_REVIEW phase
  • Systems Integration → TESTING phase
  • FAR Part 23 Certification → CERTIFICATION phase
  • Quality Audits → DEPLOYMENT phase

Phase Readiness Example:
  • Alice can pass ALL phase gates (fully certified)
  • Bob blocks CERTIFICATION phase (missing FAR-23)
  • Carol blocks TESTING phase (cert expired)
  • David blocks ALL phases (no certs yet)
```

## Next Steps

1. **Run the app**: `docker compose up --build`
2. **Wait for startup** (~5-10 min first time)
3. **Open frontend**: http://localhost:3000
4. **Follow test scenarios** in `TEST_SCENARIOS.md`
5. **Explore APIs** via Swagger UI
6. **Create your own** products/NCRs/users
7. **Interview practice**: Explain what each scenario demonstrates

Good luck with your interview prep! 🚀
