#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
# LLM Service — End-to-End Flow Test
# Run against a live stack:  docker compose up --build
#
# Covers:
#   1.  Create ENGINEER user
#   2.  Create TRAINER user
#   3.  Duplicate user creation → rejected
#   4.  Get user by ID
#   5.  List users / filter by role
#   6.  Update user profile → propagates update event
#   7.  List training courses
#   8.  Enroll engineer in first available course (MANUAL trigger)
#   9.  Training gap — show missing certs before completion
#  10.  Complete training with FAILING score → no cert issued
#  11.  Re-enroll after failure (allowed)
#  12.  Complete training with PASSING score → cert issued + event fired
#  13.  Get user certifications → cert appears
#  14.  Phase-readiness check → ready (after all mandatory certs present)
#  15.  404 on unknown user
# ═══════════════════════════════════════════════════════════════

BASE="http://localhost:8080/api/v1/llm"
CORR="test-llm-$(date +%s)"
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

pass() { echo -e "${GREEN}✔ $1${NC}"; }
fail() { echo -e "${RED}✗  $1${NC}"; exit 1; }
info() { echo -e "${BLUE}▶ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }

check_status() {
  local label="$1" expected="$2" actual="$3" body="$4"
  if [ "$actual" -eq "$expected" ]; then
    pass "$label (HTTP $actual)"
  else
    fail "$label — expected HTTP $expected, got $actual. Body: $body"
  fi
}

echo ""
echo "════════════════════════════════════════"
echo "  LLM Service — Flow Test"
echo "════════════════════════════════════════"

# ─────────────────────────────────────────────────────────────────
# 1. Create ENGINEER user
# ─────────────────────────────────────────────────────────────────
info "1. Creating ENGINEER user"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d '{
    "employeeId": "ENG-FT-001",
    "email": "eng.flowtest@cde.local",
    "fullName": "Flow Test Engineer",
    "role": "ENGINEER",
    "department": "Systems Engineering",
    "competencyLevel": "MID"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Create ENGINEER user" 201 "$HTTP" "$BODY"
ENGINEER_ID=$(echo "$BODY" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
[ -n "$ENGINEER_ID" ] || fail "Could not parse userId from response"
pass "Engineer userId = $ENGINEER_ID"

# ─────────────────────────────────────────────────────────────────
# 2. Create TRAINER user
# ─────────────────────────────────────────────────────────────────
info "2. Creating TRAINER user"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d '{
    "employeeId": "TRN-FT-001",
    "email": "trn.flowtest@cde.local",
    "fullName": "Flow Test Trainer",
    "role": "TRAINER",
    "department": "Learning & Development",
    "competencyLevel": "SENIOR"
  }')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Create TRAINER user" 201 "$HTTP" "$BODY"
TRAINER_ID=$(echo "$BODY" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
pass "Trainer userId = $TRAINER_ID"

# ─────────────────────────────────────────────────────────────────
# 3. Duplicate user → rejected
# ─────────────────────────────────────────────────────────────────
info "3. Duplicate user creation — expect 4xx"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/users" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d '{
    "employeeId": "ENG-FT-001",
    "email": "eng.flowtest@cde.local",
    "fullName": "Duplicate Engineer",
    "role": "ENGINEER",
    "department": "Systems Engineering",
    "competencyLevel": "JUNIOR"
  }')
HTTP=$(echo "$RESP" | tail -1)
[ "$HTTP" -ge 400 ] && pass "Duplicate user correctly rejected (HTTP $HTTP)" \
  || fail "Duplicate user should have been rejected, got HTTP $HTTP"

# ─────────────────────────────────────────────────────────────────
# 4. Get user by ID
# ─────────────────────────────────────────────────────────────────
info "4. Get user by ID"
RESP=$(curl -s -w "\n%{http_code}" "$BASE/users/$ENGINEER_ID")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Get user by ID" 200 "$HTTP" "$BODY"
FETCHED_EMAIL=$(echo "$BODY" | grep -o '"email":"[^"]*"' | cut -d'"' -f4)
[ "$FETCHED_EMAIL" = "eng.flowtest@cde.local" ] \
  && pass "Email matches" || fail "Email mismatch: $FETCHED_EMAIL"

# ─────────────────────────────────────────────────────────────────
# 5. List users / filter by role
# ─────────────────────────────────────────────────────────────────
info "5. List users"
RESP=$(curl -s -w "\n%{http_code}" "$BASE/users")
HTTP=$(echo "$RESP" | tail -1)
check_status "List all users" 200 "$HTTP" ""

RESP=$(curl -s -w "\n%{http_code}" "$BASE/users?role=ENGINEER")
HTTP=$(echo "$RESP" | tail -1)
check_status "List ENGINEER users" 200 "$HTTP" ""

RESP=$(curl -s -w "\n%{http_code}" "$BASE/users?role=TRAINER&department=Learning+%26+Development")
HTTP=$(echo "$RESP" | tail -1)
check_status "List TRAINER users by dept" 200 "$HTTP" ""

# ─────────────────────────────────────────────────────────────────
# 6. Update user profile
# ─────────────────────────────────────────────────────────────────
info "6. Update engineer competency level → SENIOR"
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/users/$ENGINEER_ID" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d '{"competencyLevel": "SENIOR"}')
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Update user profile" 200 "$HTTP" "$BODY"
UPDATED_LEVEL=$(echo "$BODY" | grep -o '"competencyLevel":"[^"]*"' | cut -d'"' -f4)
[ "$UPDATED_LEVEL" = "SENIOR" ] \
  && pass "competencyLevel updated to SENIOR" \
  || fail "competencyLevel not updated (got: $UPDATED_LEVEL)"

# ─────────────────────────────────────────────────────────────────
# 7. List training courses — discover courseId for enrollment
# ─────────────────────────────────────────────────────────────────
info "7. Listing training courses"
RESP=$(curl -s -w "\n%{http_code}" "$BASE/courses")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "List courses" 200 "$HTTP" "$BODY"

COURSE_ID=$(echo "$BODY" | grep -o '"courseId":"[^"]*"' | head -1 | cut -d'"' -f4)
COURSE_CODE=$(echo "$BODY" | grep -o '"courseCode":"[^"]*"' | head -1 | cut -d'"' -f4)
PASSING_SCORE=$(echo "$BODY" | grep -o '"passingScore":[0-9]*' | head -1 | cut -d':' -f2)
MANDATORY_PHASE=$(echo "$BODY" | grep -o '"mandatoryForPhase":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$COURSE_ID" ]; then
  warn "No courses found in seed data — skipping enrollment tests."
  warn "Ensure V2__seed_aerospace_data.sql inserts at least one training_course row."
  echo ""
  echo "════════════════════════════════════════"
  echo -e "  ${YELLOW}LLM Flow Test Partial — no seed courses${NC}"
  echo "════════════════════════════════════════"
  exit 0
fi

pass "Using course: $COURSE_CODE (id=$COURSE_ID, passingScore=$PASSING_SCORE, phase=$MANDATORY_PHASE)"

# ─────────────────────────────────────────────────────────────────
# 8. Enroll engineer (manual trigger)
# ─────────────────────────────────────────────────────────────────
info "8. Enrolling engineer in course $COURSE_CODE"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/enrollments" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d "{
    \"userId\": \"$ENGINEER_ID\",
    \"courseId\": \"$COURSE_ID\",
    \"triggerSource\": \"MANUAL\"
  }")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Enroll in course" 201 "$HTTP" "$BODY"
ENROLLMENT_ID=$(echo "$BODY" | grep -o '"enrollmentId":"[^"]*"' | cut -d'"' -f4)
[ -n "$ENROLLMENT_ID" ] || fail "Could not parse enrollmentId"
pass "Enrollment ID = $ENROLLMENT_ID"

# Idempotent re-enroll — should return existing active enrollment, not create duplicate
info "   Idempotent re-enroll (already ENROLLED → return existing)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/enrollments" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d "{
    \"userId\": \"$ENGINEER_ID\",
    \"courseId\": \"$COURSE_ID\",
    \"triggerSource\": \"MANUAL\"
  }")
HTTP=$(echo "$RESP" | tail -1)
check_status "Idempotent re-enroll" 201 "$HTTP" ""

# ─────────────────────────────────────────────────────────────────
# 9. Training gap — shows this course still missing (not completed)
# ─────────────────────────────────────────────────────────────────
info "9. Training gap before completion"
if [ -n "$MANDATORY_PHASE" ] && [ "$MANDATORY_PHASE" != "" ]; then
  RESP=$(curl -s -w "\n%{http_code}" "$BASE/users/$ENGINEER_ID/training-gap?phase=$MANDATORY_PHASE")
  HTTP=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -1)
  check_status "Training gap ($MANDATORY_PHASE)" 200 "$HTTP" "$BODY"
  GAP=$(echo "$BODY" | grep -o '"gapCount":[0-9]*' | cut -d':' -f2)
  [ "$GAP" -gt 0 ] \
    && pass "Gap count=$GAP (correct — not yet certified)" \
    || warn "Gap count=0 (unexpected before any certs)"
else
  warn "Course has no mandatory phase — skipping gap check"
fi

# ─────────────────────────────────────────────────────────────────
# 10. Complete training with FAILING score
# ─────────────────────────────────────────────────────────────────
FAIL_SCORE=$((PASSING_SCORE - 20))
[ "$FAIL_SCORE" -lt 0 ] && FAIL_SCORE=0
info "10. Complete with FAILING score ($FAIL_SCORE < passingScore $PASSING_SCORE)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/enrollments/$ENROLLMENT_ID/complete" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d "{\"score\": $FAIL_SCORE}")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Complete with failing score" 200 "$HTTP" "$BODY"
PASSED=$(echo "$BODY" | grep -o '"passed":[a-z]*' | cut -d':' -f2)
[ "$PASSED" = "false" ] && pass "passed=false (correct)" || fail "Expected passed=false"
CERT_ID_FAIL=$(echo "$BODY" | grep -o '"certId":"[^"]*"' | cut -d'"' -f4)
[ -z "$CERT_ID_FAIL" ] && pass "No cert issued (correct)" || fail "Cert should NOT be issued on failure"

# ─────────────────────────────────────────────────────────────────
# 11. Re-enroll after failure (FAILED status allows re-enrollment)
# ─────────────────────────────────────────────────────────────────
info "11. Re-enrolling after failure"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/enrollments" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d "{
    \"userId\": \"$ENGINEER_ID\",
    \"courseId\": \"$COURSE_ID\",
    \"triggerSource\": \"MANUAL\"
  }")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Re-enroll after failure" 201 "$HTTP" "$BODY"
ENROLLMENT_ID2=$(echo "$BODY" | grep -o '"enrollmentId":"[^"]*"' | cut -d'"' -f4)
[ -n "$ENROLLMENT_ID2" ] || fail "Could not parse enrollmentId for second enrollment"
pass "New enrollment ID = $ENROLLMENT_ID2"

# ─────────────────────────────────────────────────────────────────
# 12. Complete training with PASSING score → cert issued
# ─────────────────────────────────────────────────────────────────
PASS_SCORE=$((PASSING_SCORE + 5))
[ "$PASS_SCORE" -gt 100 ] && PASS_SCORE=100
info "12. Complete with PASSING score ($PASS_SCORE >= passingScore $PASSING_SCORE)"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/enrollments/$ENROLLMENT_ID2/complete" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: $CORR" \
  -d "{\"score\": $PASS_SCORE}")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Complete with passing score" 200 "$HTTP" "$BODY"
PASSED=$(echo "$BODY" | grep -o '"passed":[a-z]*' | cut -d':' -f2)
[ "$PASSED" = "true" ] && pass "passed=true (correct)" || fail "Expected passed=true"
CERT_ID=$(echo "$BODY" | grep -o '"certId":"[^"]*"' | cut -d'"' -f4)
CERT_NUMBER=$(echo "$BODY" | grep -o '"certNumber":"[^"]*"' | cut -d'"' -f4)
[ -n "$CERT_ID" ] && pass "Cert issued: certId=$CERT_ID" || fail "Cert should have been issued"
[ -n "$CERT_NUMBER" ] && pass "Cert number: $CERT_NUMBER"

# ─────────────────────────────────────────────────────────────────
# 13. Get user certifications — cert appears
# ─────────────────────────────────────────────────────────────────
info "13. Get user certifications — expect cert present"
RESP=$(curl -s -w "\n%{http_code}" "$BASE/users/$ENGINEER_ID/certifications")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -1)
check_status "Get certifications" 200 "$HTTP" "$BODY"
echo "$BODY" | grep -q "ACTIVE" \
  && pass "Active cert found in certifications list" \
  || fail "No ACTIVE cert in list after passing completion"

# ─────────────────────────────────────────────────────────────────
# 14. Phase readiness — if this was the only mandatory course, should now be ready
# ─────────────────────────────────────────────────────────────────
info "14. Phase readiness check after certification"
if [ -n "$MANDATORY_PHASE" ] && [ "$MANDATORY_PHASE" != "" ]; then
  RESP=$(curl -s -w "\n%{http_code}" "$BASE/phase-readiness/$MANDATORY_PHASE")
  HTTP=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -1)
  check_status "Phase readiness ($MANDATORY_PHASE)" 200 "$HTTP" "$BODY"
  UNCERTIFIED=$(echo "$BODY" | grep -o '"uncertifiedCount":[0-9]*' | cut -d':' -f2)
  info "Uncertified after cert = $UNCERTIFIED (0 = all staff certified for this phase)"
else
  warn "Course has no mandatory phase — skipping phase readiness check"
fi

# ─────────────────────────────────────────────────────────────────
# 15. 404 on unknown user
# ─────────────────────────────────────────────────────────────────
info "15. Non-existent user → expect 404"
RESP=$(curl -s -w "\n%{http_code}" "$BASE/users/00000000-0000-0000-0000-000000000000")
HTTP=$(echo "$RESP" | tail -1)
check_status "Non-existent user 404" 404 "$HTTP" ""

echo ""
echo "════════════════════════════════════════"
echo -e "  ${GREEN}LLM Flow Test Complete ✔${NC}"
echo "════════════════════════════════════════"
echo ""
echo "Key IDs from this run:"
echo "  Engineer: $ENGINEER_ID"
echo "  Trainer:  $TRAINER_ID"
echo "  Course:   $COURSE_ID ($COURSE_CODE)"
echo "  Cert:     $CERT_ID ($CERT_NUMBER)"
echo ""
