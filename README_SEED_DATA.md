# 🎯 Seed Data Setup - Complete Guide

## TL;DR

I've created **3 data loaders + 3 comprehensive guides** to populate realistic aerospace test data automatically on startup. This gives you a fully functional test environment to explore all CDE features.

**What you have now:**
- ✅ 4 aerospace products with realistic names
- ✅ 5 quality issues (2 CRITICAL blocking advancement)
- ✅ 6 staff members at different certification levels
- ✅ Realistic scenarios you can test (phase gates, NCRs, change requests, BOM, etc.)
- ✅ Step-by-step guides for 10 test scenarios
- ✅ Interview preparation materials

---

## 🚀 Quick Start

### 1. Start Application
```bash
cd "C:\Users\DARSHINI\Downloads\working code CDE 18MAY\cde-platform"
docker compose up --build
```

**First run**: ~10-15 minutes (builds images + populates data)  
**Later runs**: ~30-60 seconds (uses cached layers)

### 2. Open Frontend
```
http://localhost:3000
```

You'll see:
- **Dashboard**: 4 products, 5 NCRs, 6 users, 4 risks
- **Products**: Wing, Landing Gear, Cockpit, Hydraulic
- **Quality**: CRITICAL NCRs blocking landing gear advancement
- **Staff**: Mix of certified and uncertified engineers

### 3. Follow Scenarios
Open **`TEST_SCENARIOS.md`** and work through 10 scenarios (45 min total)

### 4. Interview Prep
Use **`SEED_DATA_SUMMARY.md`** for talking points and demo flow

---

## 📁 What Was Created

### Data Loaders (3 files)

```
plm-service/src/main/java/com/cde/plm/seed/
└── PlmDataLoader.java
    ├── 7 lifecycle phases
    ├── 4 products (Wing, Landing Gear, Cockpit, Hydraulic)
    └── BOM hierarchies for each product

qlm-service/src/main/java/com/cde/qlm/seed/
└── QlmDataLoader.java
    ├── 5 NCRs (2 CRITICAL, 2 MAJOR, 1 MINOR)
    ├── 4 Risks (scores 20, 18, 12, 6)
    └── 2 Quality Audits

llm-service/src/main/java/com/cde/llm/seed/
└── LlmDataLoader.java
    ├── 6 Users (SENIOR→JUNIOR competency levels)
    ├── 5 Training courses (tied to phases)
    ├── Certifications (active, expired, missing)
    └── Enrollments (in progress)
```

### Documentation (3 guides)

```
cde-platform/
├── TEST_SCENARIOS.md           ← 10 scenarios with steps
├── SEED_DATA_QUICKSTART.md     ← Quick reference
├── SEED_DATA_SUMMARY.md        ← Deep dive + interview tips
└── README_SEED_DATA.md         ← This file
```

---

## 📊 Seed Data At a Glance

### Products

| Code | Name | Version | Phase | Status |
|------|------|---------|-------|--------|
| AWA-001 | Main Wing Assembly | 1.0.0 | DESIGN | Ready to advance |
| **LGS-002** | **Landing Gear** | **2.1.0** | **CONCEPT** | **❌ BLOCKED by 2 CRITICAL NCRs** |
| CCP-003 | Cockpit Control Panel | 1.0.0 | DRAFT | New product |
| HYD-004 | Hydraulic Power System | 3.2.1 | TESTING | Advanced phase |

### Quality Issues

| NCR | Severity | Product | Status |
|-----|----------|---------|--------|
| NCR-001 | **CRITICAL** | Landing Gear | **BLOCKS** ❌ |
| NCR-002 | **CRITICAL** | Landing Gear | **BLOCKS** ❌ |
| NCR-003 | MAJOR | Wing | Non-blocking |
| NCR-004 | MINOR | Wing | CLOSED |
| NCR-005 | MAJOR | Hydraulic | Under review |

### Staff (Certification Status)

| Name | Role | Competency | Certs | Status |
|------|------|------------|-------|--------|
| Alice Chen | Engineer | SENIOR | 5/5 | ✅ All active |
| Bob Kumar | Engineer | SENIOR | 4/5 | ⚠️ Missing FAR-23 |
| Carol Johnson | Engineer | MID | 2/5 | ⚠️ 1 expired |
| David Patel | Engineer | JUNIOR | 0/5 | ❌ In training |
| Evelyn Martinez | QA Manager | SENIOR | 5/5 | ✅ All active |
| Frank Wu | Trainer | SENIOR | 5/5 | ✅ All active |

### Risks

| Risk | Score | Category | Status |
|------|-------|----------|--------|
| Strut seal degradation | 20 | Material | Mitigating |
| Wing composite delamination | 18 | Structural | Mitigating |
| Hydraulic leakage | 12 | Operational | Identified |
| Documentation gaps | 6 | Documentation | Identified |

---

## 🎓 Test Scenarios Overview

| # | Scenario | Teaches | Time |
|---|----------|---------|------|
| 1 | Phase Gate PASS | Success case, async UI polling | 5 min |
| 2 | **NCR Blocks Gate** | **Quality enforcement, CRITICAL NCRs hard-block** | **5 min** |
| 3 | **Cert Blocks Gate** | **Staff readiness, missing/expired certs block** | **5 min** |
| 4 | Change Requests | Engineering control, approval workflow | 5 min |
| 5 | BOM Hierarchy | Product structure, component tracking | 5 min |
| 6 | NCR→CAPA | Quality response, linking issues to actions | 5 min |
| 7 | Risk Register | Risk visibility, mitigation, heatmap | 5 min |
| 8 | Dashboard | Executive KPIs, recent activity | 5 min |
| 9 | Audit Trail | Traceability, compliance, who/what/when | 5 min |
| 10 | Manual Testing | Create your own data, test edge cases | 10 min |

**Total**: 50 minutes to understand all core functionality

---

## 🎯 Key Scenarios to Test First

### Scenario A: Phase Gate BLOCKED ⚠️ (Must Test)
```
Product: Landing Gear System v2.1.0
Action: Try "Advance to Design Review" phase
Result: ❌ BLOCKED - 2 CRITICAL NCRs prevent advancement

This teaches: How quality discipline is enforced
Timeline: 3 minutes
```

### Scenario B: Phase Gate PASSES ✅ (Easy Win)
```
Product: Main Wing Assembly v1.0.0
Action: Try "Advance to Design Review" phase
Result: ✅ PASSED - Only 1 MAJOR NCR (non-critical), Alice certified

This teaches: Success case, async polling works, gates respect quality threshold
Timeline: 3 minutes
```

### Scenario C: Certification Block ⚠️ (Demonstrate Staff Readiness)
```
Product: Hydraulic Power System v3.2.1
Action: Try "Advance to Certification" phase
Result: ❌ BLOCKED - Bob Kumar missing FAR Part 23 cert

This teaches: How staff competency is enforced per phase
Timeline: 3 minutes
```

---

## 🛠️ How Data Loaders Work

Each loader is a Spring `@Component` implementing `CommandLineRunner`:

```java
@Component @Slf4j @RequiredArgsConstructor
public class PlmDataLoader implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        // Check if data exists
        if (phaseRepo.count() > 0) {
            log.info("PLM data already exists, skipping seed");
            return;  // Don't reload, don't duplicate
        }
        
        // Load fresh data
        log.info("Loading PLM seed data for aerospace...");
        loadPhases();
        loadProducts();
        log.info("PLM seed data loaded successfully");
    }
}
```

**This means:**
- ✅ First startup: Data loads automatically
- ✅ Later startups: Skips loading (no duplicates)
- ✅ Reset with `docker compose down -v` clears data and reloads

---

## 📖 Documentation Files

### 1. **TEST_SCENARIOS.md** (Detailed, ~15 pages)
Best for: Step-by-step walkthroughs
Contains:
- 10 complete scenarios with screenshots descriptions
- Expected behavior explanations
- Key insights for each scenario
- Database query examples
- Interview talking points
- Summary table of all scenarios

**Use this when**: Working through test cases

### 2. **SEED_DATA_QUICKSTART.md** (Quick Reference, ~10 pages)
Best for: Getting started quickly
Contains:
- Quick start commands
- User credentials table
- Seed data overview
- Database reset instructions
- Debugging tips
- Complete 45-minute workflow

**Use this when**: First time setup or quick lookup

### 3. **SEED_DATA_SUMMARY.md** (Comprehensive, ~20 pages)
Best for: Deep understanding & interviews
Contains:
- Overview of what was created
- How to use each component
- Key testing insights
- Interview preparation tips
- 30-minute demo flow
- Data relationship diagrams
- Common troubleshooting
- Interview questions you can now answer

**Use this when**: Preparing for interviews or understanding architecture deeply

---

## 🧪 Realistic Test Scenarios

The seed data is designed to demonstrate **real aerospace engineering workflows**:

### 1. Quality Enforcement
**Real Problem**: A component with critical quality issues should not advance
- **Landing Gear has 2 CRITICAL NCRs** (strut seal, bearing fatigue)
- Attempts to advance are **BLOCKED** by phase gate
- **Solution**: Close NCRs, then gate PASSES
- **Real Impact**: Prevents bad parts from going to production

### 2. Staff Readiness
**Real Problem**: Unqualified staff cannot work on certifications-critical phases
- **Bob missing FAR Part 23 cert** blocks CERTIFICATION phase
- **Carol's cert expired** blocks TESTING phase
- **David has no certs** blocks ALL phases
- **Solution**: Enroll in training, complete, get certified
- **Real Impact**: Ensures FAA compliance requirements are met

### 3. Change Control
**Real Problem**: Engineering changes must be tracked and approved
- **Create CR** for design modification
- **Submit for approval** → status changes
- **Approver reviews** → APPROVED or REJECTED
- **Audit trail** proves who approved what
- **Real Impact**: Full traceability for certification audits

### 4. Product Structure
**Real Problem**: Complex aerospace products have hierarchical BOMs
- **Wing Assembly** has Spar, Skin, Ailerons
- Each component has type, quantity, unit
- **Hierarchy enables** composition tracking
- **Real Impact**: Bill of Materials for manufacturing

---

## ⚡ Expected Build Times

### First Run
```
Total: 10-15 minutes
├─ Docker image builds (Java compile): 8-10 min
├─ Database initialization: 1-2 min
├─ Seed data loading: 30-60 sec
└─ Service startup: 30-60 sec
```

### Subsequent Runs
```
Total: 30-60 seconds
├─ Container startup: 30-50 sec
├─ Database connection: 5-10 sec
└─ Seed data skip (already exists): <1 sec
```

---

## 🔍 Verifying Seed Data Loaded

### Check Logs
```bash
docker compose logs plm-service | grep "seed data"
```

You should see:
```
INFO  Loading PLM seed data for aerospace...
INFO  Loaded 7 lifecycle phases
INFO  Loaded 4 products with versions and BOM
INFO  PLM seed data loaded successfully
```

### Query Database
```bash
# Check products exist
docker exec -it cde-plm-db psql -U postgres -d cde_plm \
  -c "SELECT product_code, name FROM product ORDER BY created_at;"

# Check NCRs exist
docker exec -it cde-qlm-db psql -U postgres -d cde_qlm \
  -c "SELECT ncr_number, severity, status FROM non_conformance;"
```

### Check Frontend
- Navigate to http://localhost:3000
- Dashboard should show 4 products, 5 NCRs, 4 risks
- Products should have versions and BOM

---

## 🚨 Reset Everything & Start Fresh

```bash
# Option 1: Soft reset (keep containers, clear data)
docker compose down
docker compose up

# Option 2: Hard reset (delete everything)
docker compose down -v
docker compose up --build

# Option 3: Ultra clean (remove all images/volumes)
docker compose down -v
docker system prune -a
docker compose up --build
```

After reset, seed data reloads automatically on startup.

---

## 🎤 Interview Demo Script (30 minutes)

1. **Dashboard Overview** (2 min)
   - "We have 4 aerospace products at different lifecycle phases"
   - "The system shows 2 critical blockers preventing landing gear advancement"

2. **Phase Gate Mechanism** (8 min)
   - "Phase gates are async checkpoints enforcing quality and staff readiness"
   - Show landing gear blocked by NCRs
   - Explain how to resolve and retry
   - Show successful wing advancement

3. **Change Control** (5 min)
   - "Every engineering change goes through formal approval"
   - Create CR, submit, approve with comments
   - Show audit trail

4. **Product Structure** (5 min)
   - "Complex products have hierarchical BOMs"
   - Show wing component tree
   - Add new component

5. **Risk Management** (3 min)
   - "Risk register makes hazards visible"
   - Show heatmap with color-coded risks

6. **Architecture** (7 min)
   - "Why use pub/sub instead of REST?"
   - Explain async saga pattern for phase gates
   - Why event-driven is more resilient

---

## ❓ FAQ

**Q: Will seed data be duplicated if I restart?**  
A: No. Loaders check if data exists first. Idempotent by design.

**Q: Can I modify seed data after loading?**  
A: Yes. Create/edit entities normally. Seed data is just the initial state.

**Q: How do I clear everything and start over?**  
A: `docker compose down -v` clears databases and volumes. Restart loads fresh seed data.

**Q: Where are the loaders in the code?**  
A: `plm-service/src/main/java/com/cde/plm/seed/PlmDataLoader.java` (and qlm, llm versions)

**Q: Can I add more seed data?**  
A: Yes! Edit the loader `loadXxx()` methods to add more products, NCRs, users, etc.

**Q: Will this work for production?**  
A: No. Seed data is for dev/test only. Production needs database migrations or proper initialization.

---

## 📚 Reading Order

For **getting started quickly**:
1. This file (README_SEED_DATA.md)
2. SEED_DATA_QUICKSTART.md
3. Start app and try Scenarios 2, 1, 3 from TEST_SCENARIOS.md

For **deep understanding**:
1. SEED_DATA_SUMMARY.md
2. TEST_SCENARIOS.md (all 10 scenarios)
3. Run app and follow demo script above

For **interview prep**:
1. SEED_DATA_SUMMARY.md (talking points + demo flow)
2. Practice 30-minute demo
3. Try responding to these questions:
   - "How does the system enforce quality discipline?"
   - "How does it prevent unqualified staff from working?"
   - "Walk us through a product from concept to release"
   - "Why use async pub/sub instead of REST?"

---

## ✨ Summary

You now have:

✅ **4 realistic aerospace products** with phase gates  
✅ **5 quality issues** demonstrating blocking scenarios  
✅ **6 staff members** at different certification levels  
✅ **Automatic seed data loading** on app startup  
✅ **10 test scenarios** with step-by-step guides  
✅ **3 documentation files** for reference  
✅ **Interview preparation materials** ready to use  

**Next step**: Run `docker compose up --build` and open http://localhost:3000

Happy testing! 🚀
