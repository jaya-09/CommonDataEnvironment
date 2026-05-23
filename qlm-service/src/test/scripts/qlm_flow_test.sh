#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# QLM End-to-End Flow Test
# Walks through the full QLM journey with real API endpoints.
#
# Run with: bash qlm_flow_test.sh
# Requires: curl, jq
#
# Direct (no gateway): BASE=http://localhost:8082
# Via API gateway:     BASE=http://localhost:8080
#
# Key scenario verified:
#   - CRITICAL NCR blocks phase gate check
#   - CAPA closes → NCR closes
#   - Phase gate check clears once CRITICAL NCR is closed
# ═══════════════════════════════════════════════════════════════

BASE="${QLM_BASE:-http://localhost:8082}"
API="$BASE/api/v1/qlm"
USER_ID="dddddddd-dddd-dddd-dddd-dddddddddddd"     # Evelyn Martinez — Quality Manager (seeded)
ENGINEER_ID="11111111-1111-1111-1111-111111111111"  # Alice Chen — Engineer (seeded)
VERSION_ID="${TEST_VERSION_ID:-aaaabbbb-0000-0000-0000-000000000001}"  # mock PLM version ID

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

# ── 0. Health check ──────────────────────────────────────────────────────────
step "0. Health check"
R=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health")
check_status "QLM service is up" "200" "$R"

# ── 1. Create a CRITICAL NCR ─────────────────────────────────────────────────
step "1. Create a CRITICAL NCR on a product version"
NCR=$(curl -s -w "\n%{http_code}" -X POST "$API/ncr" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"productVersionId\": \"$VERSION_ID\",
    \"productCode\": \"TEST-PART-001\",
    \"severity\": \"CRITICAL\",
    \"title\": \"Fatigue crack found in primary load-bearing bracket\",
    \"description\": \"Visual inspection during manufacturing revealed a 2mm crack in frame bracket FBK-007. Requires immediate disposition.\"
  }")
NCR_BODY=$(echo "$NCR" | head -1)
NCR_STATUS=$(echo "$NCR" | tail -1)
check_status "Create CRITICAL NCR" "201" "$NCR_STATUS" "$NCR_BODY"
NCR_ID=$(echo "$NCR_BODY" | jq -r '.ncrId')
NCR_NUMBER=$(echo "$NCR_BODY" | jq -r '.ncrNumber')
NCR_STAT=$(echo "$NCR_BODY" | jq -r '.status')
echo "   NCR ID:     $NCR_ID"
echo "   NCR Number: $NCR_NUMBER"
echo "   Status:     $NCR_STAT   (should be OPEN)"
[ "$NCR_STAT" = "OPEN" ] && pass "NCR created in OPEN status" || fail "Expected OPEN, got $NCR_STAT"

# ── 2. Phase gate check — should FAIL (CRITICAL NCR open) ────────────────────
step "2. Phase gate NCR check — should be BLOCKED by open CRITICAL NCR"
CHECK=$(curl -s -H "X-User-Id: $USER_ID" \
  "$API/ncr/check?versionId=$VERSION_ID&severity=CRITICAL")
CLEAR=$(echo "$CHECK" | jq -r '.clear')
OPEN_COUNT=$(echo "$CHECK" | jq -r '.openCriticalCount')
echo "   clear:             $CLEAR   (should be false)"
echo "   openCriticalCount: $OPEN_COUNT   (should be >= 1)"
[ "$CLEAR" = "false" ] && pass "Phase gate correctly blocked (CRITICAL NCR open)" \
  || fail "Expected clear=false, got $CLEAR"
[ "$OPEN_COUNT" -ge 1 ] 2>/dev/null && pass "Open critical count >= 1" \
  || fail "Expected openCriticalCount >= 1, got $OPEN_COUNT"

# ── 3. Create a CAPA linked to the NCR ──────────────────────────────────────
step "3. Create a CAPA to address the NCR"
CAPA=$(curl -s -w "\n%{http_code}" -X POST "$API/capa" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"ncrId\": \"$NCR_ID\",
    \"title\": \"Replace FBK-007 bracket and update inspection procedure\",
    \"correctiveAction\": \"Replace cracked bracket with new part. Perform MPI inspection of all brackets in the same batch.\",
    \"preventiveAction\": \"Update incoming inspection checklist to include fluorescent penetrant testing. Increase sampling rate from 5% to 25%.\",
    \"ownerUserId\": \"$ENGINEER_ID\",
    \"dueDate\": \"2026-06-30\"
  }")
CAPA_BODY=$(echo "$CAPA" | head -1)
CAPA_HTTP=$(echo "$CAPA" | tail -1)
check_status "Create CAPA" "201" "$CAPA_HTTP" "$CAPA_BODY"
CAPA_ID=$(echo "$CAPA_BODY" | jq -r '.capaId')
CAPA_NUMBER=$(echo "$CAPA_BODY" | jq -r '.capaNumber')
CAPA_STAT=$(echo "$CAPA_BODY" | jq -r '.status')
echo "   CAPA ID:     $CAPA_ID"
echo "   CAPA Number: $CAPA_NUMBER"
echo "   CAPA Status: $CAPA_STAT   (should be OPEN)"
[ "$CAPA_STAT" = "OPEN" ] && pass "CAPA created in OPEN status" || fail "Expected OPEN, got $CAPA_STAT"

# ── 4. Verify NCR moved to PENDING_CAPA ─────────────────────────────────────
step "4. Verify NCR is now PENDING_CAPA"
NCR_CHECK=$(curl -s -H "X-User-Id: $USER_ID" "$API/ncr/$NCR_ID")
NCR_NEW_STAT=$(echo "$NCR_CHECK" | jq -r '.status')
echo "   NCR status: $NCR_NEW_STAT   (should be PENDING_CAPA)"
[ "$NCR_NEW_STAT" = "PENDING_CAPA" ] && pass "NCR moved to PENDING_CAPA" \
  || fail "Expected PENDING_CAPA, got $NCR_NEW_STAT"

# ── 5. Close the CAPA ────────────────────────────────────────────────────────
step "5. Close the CAPA (effectiveness verified)"
# effectivenessCheck is a @RequestParam (query string), not request body
EFF_CHECK="Post-repair MPI inspection passed on all 40 brackets. Zero defects found. Confirmed effective."
CLOSE=$(curl -s -w "\n%{http_code}" -X POST \
  "$API/capa/$CAPA_ID/close?effectivenessCheck=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$EFF_CHECK" 2>/dev/null || echo "MPI+inspection+passed")" \
  -H "X-User-Id: $USER_ID")
CLOSE_BODY=$(echo "$CLOSE" | head -1)
CLOSE_HTTP=$(echo "$CLOSE" | tail -1)
check_status "Close CAPA" "200" "$CLOSE_HTTP" "$CLOSE_BODY"
CLOSED_CAPA_STAT=$(echo "$CLOSE_BODY" | jq -r '.status')
CAPA_CLOSED_AT=$(echo "$CLOSE_BODY" | jq -r '.closedAt')
EFF_VERIFIED=$(echo "$CLOSE_BODY" | jq -r '.effectivenessVerified')
echo "   CAPA status:           $CLOSED_CAPA_STAT   (should be CLOSED)"
echo "   closedAt:              $CAPA_CLOSED_AT"
echo "   effectivenessVerified: $EFF_VERIFIED   (should be true)"
[ "$CLOSED_CAPA_STAT" = "CLOSED" ] && pass "CAPA is now CLOSED" || fail "Expected CLOSED, got $CLOSED_CAPA_STAT"
[ "$CAPA_CLOSED_AT" != "null" ] && pass "closedAt timestamp was set" || fail "closedAt is null"
[ "$EFF_VERIFIED" = "true" ] && pass "effectivenessVerified is true" || fail "Expected true, got $EFF_VERIFIED"

# ── 6. Close the NCR ─────────────────────────────────────────────────────────
step "6. Close the NCR (disposition complete)"
NCR_CLOSE=$(curl -s -w "\n%{http_code}" -X PUT "$API/ncr/$NCR_ID" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d '{"status": "CLOSED", "rootCause": "Insufficient incoming inspection for primary structural brackets. Drill burrs created stress concentration points."}')
NCR_CLOSE_BODY=$(echo "$NCR_CLOSE" | head -1)
NCR_CLOSE_HTTP=$(echo "$NCR_CLOSE" | tail -1)
check_status "Close NCR" "200" "$NCR_CLOSE_HTTP" "$NCR_CLOSE_BODY"
FINAL_NCR_STAT=$(echo "$NCR_CLOSE_BODY" | jq -r '.status')
FINAL_CLOSED_AT=$(echo "$NCR_CLOSE_BODY" | jq -r '.closedAt')
echo "   NCR status: $FINAL_NCR_STAT   (should be CLOSED)"
echo "   closedAt:   $FINAL_CLOSED_AT"
[ "$FINAL_NCR_STAT" = "CLOSED" ] && pass "NCR is now CLOSED" || fail "Expected CLOSED, got $FINAL_NCR_STAT"
[ "$FINAL_CLOSED_AT" != "null" ] && pass "closedAt timestamp was set" || fail "closedAt is null"

# ── 7. Phase gate check — should PASS now ────────────────────────────────────
step "7. Phase gate NCR check — should PASS (no open CRITICAL NCRs for this version)"
CHECK2=$(curl -s -H "X-User-Id: $USER_ID" \
  "$API/ncr/check?versionId=$VERSION_ID&severity=CRITICAL")
CLEAR2=$(echo "$CHECK2" | jq -r '.clear')
OPEN_COUNT2=$(echo "$CHECK2" | jq -r '.openCriticalCount')
echo "   clear:             $CLEAR2   (should be true)"
echo "   openCriticalCount: $OPEN_COUNT2   (should be 0)"
[ "$CLEAR2" = "true" ] && pass "Phase gate now clear — no open CRITICAL NCRs" \
  || fail "Expected clear=true, got $CLEAR2"

# ── 8. Register a risk ───────────────────────────────────────────────────────
step "8. Register a risk (likelihood=2, impact=5 → score should be 10)"
RISK=$(curl -s -w "\n%{http_code}" -X POST "$API/risks" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"productVersionId\": \"$VERSION_ID\",
    \"title\": \"Supplier single-source risk for titanium forgings\",
    \"description\": \"Only one qualified supplier for titanium forgings. Disruption would halt production.\",
    \"category\": \"SUPPLY_CHAIN\",
    \"likelihood\": 2,
    \"impact\": 5,
    \"mitigationPlan\": \"Qualify a second supplier by Q3 2026. Maintain 8-week buffer stock.\",
    \"ownerUserId\": \"$ENGINEER_ID\"
  }")
RISK_BODY=$(echo "$RISK" | head -1)
RISK_HTTP=$(echo "$RISK" | tail -1)
check_status "Register risk" "200" "$RISK_HTTP" "$RISK_BODY"
RISK_ID=$(echo "$RISK_BODY" | jq -r '.riskId')
RISK_SCORE=$(echo "$RISK_BODY" | jq -r '.riskScore')
echo "   Risk ID:    $RISK_ID"
echo "   Risk score: $RISK_SCORE   (should be 10 = 2 × 5)"
[ "$RISK_SCORE" -eq 10 ] 2>/dev/null && pass "Risk score computed correctly (2 × 5 = 10)" \
  || fail "Expected risk score 10, got $RISK_SCORE"

# ── 9. Create and complete a quality audit ───────────────────────────────────
step "9. Create and complete a quality audit"
AUDIT=$(curl -s -w "\n%{http_code}" -X POST "$API/audits" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER_ID" \
  -d "{
    \"auditType\": \"INTERNAL\",
    \"scope\": \"Manufacturing process compliance for FBK-007 bracket production\",
    \"scheduledDate\": \"2026-05-21\",
    \"leadAuditorId\": \"$USER_ID\"
  }")
AUDIT_BODY=$(echo "$AUDIT" | head -1)
AUDIT_HTTP=$(echo "$AUDIT" | tail -1)
check_status "Create audit" "201" "$AUDIT_HTTP" "$AUDIT_BODY"
AUDIT_ID=$(echo "$AUDIT_BODY" | jq -r '.auditId')
echo "   Audit ID: $AUDIT_ID"

# Complete the audit — summary, courseCode, affectedDept are all @RequestParam
SUMMARY="All manufacturing processes in compliance following corrective actions. No further findings."
COMPLETE=$(curl -s -w "\n%{http_code}" -X POST \
  "$API/audits/$AUDIT_ID/complete?affectedDept=Manufacturing&summary=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$SUMMARY" 2>/dev/null || echo "All+processes+compliant")" \
  -H "X-User-Id: $USER_ID")
COMPLETE_BODY=$(echo "$COMPLETE" | head -1)
COMPLETE_HTTP=$(echo "$COMPLETE" | tail -1)
check_status "Complete audit" "200" "$COMPLETE_HTTP" "$COMPLETE_BODY"
AUDIT_FINAL_STAT=$(echo "$COMPLETE_BODY" | jq -r '.status')
echo "   Audit status: $AUDIT_FINAL_STAT   (should be COMPLETED)"
[ "$AUDIT_FINAL_STAT" = "COMPLETED" ] && pass "Audit marked COMPLETED" \
  || fail "Expected COMPLETED, got $AUDIT_FINAL_STAT"

# ── 10. List all NCRs (includes seeded data) ─────────────────────────────────
step "10. List all NCRs"
NCR_LIST=$(curl -s -H "X-User-Id: $USER_ID" "$API/ncr")
NCR_LIST_COUNT=$(echo "$NCR_LIST" | jq 'length')
echo "   Total NCRs in system: $NCR_LIST_COUNT   (seeded 5 + 1 we created = 6 expected)"
[ "$NCR_LIST_COUNT" -ge 1 ] && pass "NCR list is non-empty" || fail "Expected at least 1 NCR"

# ── 11. Wrong approver cannot close a CR (not applicable to QLM) ─────────────
# Note: QLM does not have approver-restriction on NCR updates. All authenticated
# users can update NCRs (access control will be added in the auth layer).

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  QLM flow test complete${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo ""
echo "Resources created:"
echo "  NCR ID:   $NCR_ID   ($NCR_NUMBER — CLOSED)"
echo "  CAPA ID:  $CAPA_ID  ($CAPA_NUMBER — CLOSED)"
echo "  Risk ID:  $RISK_ID  (score: $RISK_SCORE)"
echo "  Audit ID: $AUDIT_ID (COMPLETED)"
echo ""
echo "Phase gate flow verified: BLOCKED (CRITICAL NCR open) → CLEARED (NCR closed) ✓"
echo ""
echo "To run with Docker: docker compose up"
echo "Use TEST_VERSION_ID=<uuid> to test against a real PLM version"
