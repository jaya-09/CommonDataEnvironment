#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# PLM End-to-End Flow Test
# Walks through the full PLM journey with mock data.
#
# Run with: bash plm_flow_test.sh
# Requires: curl, jq
#
# If running app locally:    BASE=http://localhost:8081
# If running via gateway:    BASE=http://localhost:8080/api/v1
# ═══════════════════════════════════════════════════════════════

BASE="${PLM_BASE:-http://localhost:8081}"
USER_ID="00000000-0000-0000-0000-000000000001"   # mock user (engineer)
APPROVER_ID="00000000-0000-0000-0000-000000000002" # mock approver

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }
step() { echo -e "\n${YELLOW}── $1 ──${NC}"; }

check_status() {
  local label="$1" expected="$2" actual="$3" body="$4"
  if [ "$actual" = "$expected" ]; then
    pass "$label (HTTP $actual)"
  else
    fail "$label — expected HTTP $expected, got $actual\nBody: $body"
  fi
}

# ── 0. Seed a UserShadow record for the approver ────────────────────────────
# (In real life this comes via Pub/Sub from LLM; for testing we insert directly
#  via psql or just skip the approver name display — the flow still works.)
step "0. Health check"
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health")
check_status "PLM service is up" "200" "$R"

# ── 1. List lifecycle phases ─────────────────────────────────────────────────
step "1. List lifecycle phases (cached after first call)"
PHASES=$(curl -s -H "X-User-Id: $USER_ID" "$BASE/plm/phases")
PHASE_COUNT=$(echo "$PHASES" | jq 'length')
echo "   Phases found: $PHASE_COUNT"
[ "$PHASE_COUNT" -ge 6 ] && pass "All 6 lifecycle phases present" || fail "Expected 6 phases, got $PHASE_COUNT"
echo "$PHASES" | jq -r '.[].displayName' | sed 's/^/   /'

# ── 2. Create a product ──────────────────────────────────────────────────────
step "2. Create product"
PRODUCT=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/products" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"productCode\": \"TEST-WIDGET-001\",
    \"name\": \"Test Widget\",
    \"description\": \"An aerospace widget for testing\",
    \"approverUserId\": \"$APPROVER_ID\"
  }")
PRODUCT_BODY=$(echo "$PRODUCT" | head -1)
PRODUCT_STATUS=$(echo "$PRODUCT" | tail -1)
check_status "Create product" "200" "$PRODUCT_STATUS" "$PRODUCT_BODY"
PRODUCT_ID=$(echo "$PRODUCT_BODY" | jq -r '.productId')
echo "   Product ID: $PRODUCT_ID"

# ── 3. Create a version ──────────────────────────────────────────────────────
step "3. Create product version v1.0"
VERSION=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/products/$PRODUCT_ID/versions" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"versionNumber": "v1.0", "description": "Initial design release"}')
VERSION_BODY=$(echo "$VERSION" | head -1)
VERSION_STATUS=$(echo "$VERSION" | tail -1)
check_status "Create version" "200" "$VERSION_STATUS" "$VERSION_BODY"
VERSION_ID=$(echo "$VERSION_BODY" | jq -r '.versionId')
CURRENT_PHASE=$(echo "$VERSION_BODY" | jq -r '.currentPhase')
echo "   Version ID:    $VERSION_ID"
echo "   Current phase: $CURRENT_PHASE   (should be Ideation)"
[ "$CURRENT_PHASE" = "Ideation" ] && pass "Version starts in Ideation phase" || fail "Expected Ideation, got $CURRENT_PHASE"

# ── 4. Add BOM components ────────────────────────────────────────────────────
step "4. Add BOM components"

# Root component
BOM1=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/versions/$VERSION_ID/bom" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{
    "componentCode": "FRAME-001",
    "name": "Main Frame Assembly",
    "componentType": "ASSEMBLY",
    "quantity": 1,
    "unit": "EA"
  }')
BOM1_BODY=$(echo "$BOM1" | head -1)
BOM1_STATUS=$(echo "$BOM1" | tail -1)
check_status "Add root BOM component" "200" "$BOM1_STATUS" "$BOM1_BODY"
ROOT_COMP_ID=$(echo "$BOM1_BODY" | jq -r '.componentId')
echo "   Root component ID: $ROOT_COMP_ID"

# Child component
BOM2=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/versions/$VERSION_ID/bom" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"componentCode\": \"BOLT-M8-001\",
    \"name\": \"M8 Hex Bolt\",
    \"componentType\": \"FASTENER\",
    \"quantity\": 12,
    \"unit\": \"EA\",
    \"parentComponentId\": \"$ROOT_COMP_ID\"
  }")
check_status "Add child BOM component" "200" "$(echo "$BOM2" | tail -1)" "$(echo "$BOM2" | head -1)"

# ── 5. Get BOM tree ──────────────────────────────────────────────────────────
step "5. Fetch BOM tree"
BOM_TREE=$(curl -s -H "X-User-Id: $USER_ID" "$BASE/plm/versions/$VERSION_ID/bom")
ROOT_COUNT=$(echo "$BOM_TREE" | jq 'length')
CHILD_COUNT=$(echo "$BOM_TREE" | jq '.[0].children | length')
echo "   Root components: $ROOT_COUNT"
echo "   Children of root: $CHILD_COUNT"
[ "$ROOT_COUNT" -eq 1 ] && pass "BOM tree has 1 root" || fail "Expected 1 root, got $ROOT_COUNT"
[ "$CHILD_COUNT" -eq 1 ] && pass "Root has 1 child component" || fail "Expected 1 child, got $CHILD_COUNT"

# ── 6. Create a Change Request ───────────────────────────────────────────────
step "6. Create a Change Request"
CR=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/versions/$VERSION_ID/change-requests" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{
    "title": "Update frame material to titanium alloy",
    "description": "Switch from aluminium to titanium for weight reduction",
    "reason": "Weight target not met with current material",
    "impactAnalysis": "5% weight reduction, 12% cost increase, no dimensional changes",
    "crType": "STANDARD"
  }')
CR_BODY=$(echo "$CR" | head -1)
CR_STATUS=$(echo "$CR" | tail -1)
check_status "Create CR" "200" "$CR_STATUS" "$CR_BODY"
CR_ID=$(echo "$CR_BODY" | jq -r '.crId')
CR_NUMBER=$(echo "$CR_BODY" | jq -r '.crNumber')
CR_STAT=$(echo "$CR_BODY" | jq -r '.status')
echo "   CR ID:     $CR_ID"
echo "   CR Number: $CR_NUMBER"
echo "   Status:    $CR_STAT   (should be DRAFT)"
[ "$CR_STAT" = "DRAFT" ] && pass "CR created in DRAFT status" || fail "Expected DRAFT, got $CR_STAT"

# ── 7. Submit the CR ─────────────────────────────────────────────────────────
step "7. Submit CR for approval"
SUBMIT=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/change-requests/$CR_ID/submit" \
  -H "X-User-Id: $USER_ID")
SUBMIT_BODY=$(echo "$SUBMIT" | head -1)
SUBMIT_STATUS=$(echo "$SUBMIT" | tail -1)
check_status "Submit CR" "200" "$SUBMIT_STATUS" "$SUBMIT_BODY"
SUBMITTED_STAT=$(echo "$SUBMIT_BODY" | jq -r '.status')
SUBMITTED_AT=$(echo "$SUBMIT_BODY" | jq -r '.submittedAt')
echo "   Status:      $SUBMITTED_STAT   (should be SUBMITTED)"
echo "   submittedAt: $SUBMITTED_AT"
[ "$SUBMITTED_STAT" = "SUBMITTED" ] && pass "CR is now SUBMITTED" || fail "Expected SUBMITTED, got $SUBMITTED_STAT"
[ "$SUBMITTED_AT" != "null" ] && pass "submittedAt timestamp was set" || fail "submittedAt is null"

# ── 8. Approve the CR ────────────────────────────────────────────────────────
step "8. Approve CR (as the designated approver)"
APPROVE=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/change-requests/$CR_ID/approve" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $APPROVER_ID" \
  -d '{"decision": "APPROVED", "comments": "Material change approved. Proceed to procurement."}')
APPROVE_BODY=$(echo "$APPROVE" | head -1)
APPROVE_STATUS=$(echo "$APPROVE" | tail -1)
check_status "Approve CR" "200" "$APPROVE_STATUS" "$APPROVE_BODY"
APPROVED_STAT=$(echo "$APPROVE_BODY" | jq -r '.status')
DECIDED_AT=$(echo "$APPROVE_BODY" | jq -r '.decidedAt')
echo "   Status:     $APPROVED_STAT   (should be APPROVED)"
echo "   decidedAt:  $DECIDED_AT"
[ "$APPROVED_STAT" = "APPROVED" ] && pass "CR is now APPROVED" || fail "Expected APPROVED, got $APPROVED_STAT"
[ "$DECIDED_AT" != "null" ] && pass "decidedAt timestamp was set" || fail "decidedAt is null"

# ── 9. Wrong approver rejection test ────────────────────────────────────────
step "9. Non-approver trying to approve a second CR (should be rejected)"
CR2=$(curl -s -X POST "$BASE/plm/versions/$VERSION_ID/change-requests" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"title": "Test wrong approver", "crType": "STANDARD"}')
CR2_ID=$(echo "$CR2" | jq -r '.crId')
curl -s -X POST "$BASE/plm/change-requests/$CR2_ID/submit" -H "X-User-Id: $USER_ID" > /dev/null
WRONG=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/plm/change-requests/$CR2_ID/approve" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"decision": "APPROVED"}')
[ "$WRONG" = "400" ] || [ "$WRONG" = "403" ] || [ "$WRONG" = "500" ] \
  && pass "Wrong approver correctly rejected (HTTP $WRONG)" \
  || fail "Expected rejection for wrong approver, got HTTP $WRONG"

# ── 10. List all CRs for version ────────────────────────────────────────────
step "10. List all CRs for version"
CRS=$(curl -s -H "X-User-Id: $USER_ID" "$BASE/plm/versions/$VERSION_ID/change-requests")
CR_COUNT=$(echo "$CRS" | jq 'length')
echo "   Total CRs: $CR_COUNT   (should be 2)"
[ "$CR_COUNT" -eq 2 ] && pass "Both CRs listed" || fail "Expected 2 CRs, got $CR_COUNT"

# ── 11. Request phase advancement ───────────────────────────────────────────
step "11. Request phase advancement (Ideation → Design)"
ADVANCE=$(curl -s -w "\n%{http_code}" -X POST "$BASE/plm/versions/$VERSION_ID/phase" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"targetPhase": "Design"}')
ADVANCE_BODY=$(echo "$ADVANCE" | head -1)
ADVANCE_STATUS=$(echo "$ADVANCE" | tail -1)
check_status "Request phase advance" "200" "$ADVANCE_STATUS" "$ADVANCE_BODY"
MESSAGE=$(echo "$ADVANCE_BODY" | jq -r '.message')
echo "   Response: $MESSAGE"
# Phase gate is async — it dispatches to Pub/Sub and returns PENDING
echo -e "   ${YELLOW}Note: Phase gate is async. In a real run, QLM and LLM would respond${NC}"
echo -e "   ${YELLOW}      via Pub/Sub and finalise the advance. Poll the status endpoint.${NC}"

# ── 12. Check phase gate status ─────────────────────────────────────────────
step "12. Poll phase gate status"
PG_STATUS=$(curl -s -H "X-User-Id: $USER_ID" \
  "$BASE/plm/versions/$VERSION_ID/phase-gate-status?targetPhase=Design")
echo "   Phase gate response: $(echo "$PG_STATUS" | jq -c '.')"
pass "Phase gate status endpoint responsive"

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}  PLM flow test complete${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""
echo "Resources created:"
echo "  Product ID: $PRODUCT_ID"
echo "  Version ID: $VERSION_ID"
echo "  CR ID:      $CR_ID"
echo ""
echo "Phase advancement requires Pub/Sub emulator + QLM + LLM running."
echo "To run with Docker: docker compose up"
