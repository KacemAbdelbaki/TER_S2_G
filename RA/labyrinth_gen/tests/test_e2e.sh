#!/usr/bin/env bash
# =============================================================================
# test_e2e.sh - End-to-end (E2E) curl tests for the maze API.
#
# Usage:
#   LOCAL  : bash test_e2e.sh                          (defaults to localhost:5000)
#   REMOTE : bash test_e2e.sh https://your-render-url  (deployed service)
#
# Requires: curl, jq
# =============================================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:5000}"
PASS=0
FAIL=0

green() { printf '\033[0;32m%s\033[0m\n' "$*"; }
red()   { printf '\033[0;31m%s\033[0m\n' "$*"; }

assert_eq() {
    local label="$1" expected="$2" actual="$3"
    if [[ "$expected" == "$actual" ]]; then
        green "  ✔ $label"
        PASS=$((PASS + 1))
    else
        red "  ✘ $label  (expected=$expected, got=$actual)"
        FAIL=$((FAIL + 1))
    fi
}

assert_gte() {
    local label="$1" min="$2" actual="$3"
    if (( actual >= min )); then
        green "  ✔ $label"
        PASS=$((PASS + 1))
    else
        red "  ✘ $label  (expected >= $min, got=$actual)"
        FAIL=$((FAIL + 1))
    fi
}

echo "============================================"
echo "E2E Tests against: $BASE_URL"
echo "============================================"

# ---------- Test 1: Default params → 200 + valid JSON -----------------------
echo ""
echo "Test 1: Default parameters"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' "$BASE_URL/labyrinth")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
HEIGHT=$(jq '.height' /tmp/maze_e2e.json)
assert_eq "Default height=5" "5" "$HEIGHT"

# ---------- Test 2: Explicit DFS 10x12 --------------------------------------
echo ""
echo "Test 2: DFS 10x12"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' \
    "$BASE_URL/labyrinth?algorithm=dfs&height=10&width=12")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
HEIGHT=$(jq '.height' /tmp/maze_e2e.json)
assert_eq "height=10" "10" "$HEIGHT"
ROW_COUNT=$(jq '.rows | length' /tmp/maze_e2e.json)
assert_eq "10 rows" "10" "$ROW_COUNT"

# ---------- Test 3: Kruskal -------------------------------------------------
echo ""
echo "Test 3: Kruskal 8x8"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' \
    "$BASE_URL/labyrinth?algorithm=kruskal&height=8&width=8")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
HEIGHT=$(jq '.height' /tmp/maze_e2e.json)
assert_eq "height=8" "8" "$HEIGHT"

# ---------- Test 4: Prim ----------------------------------------------------
echo ""
echo "Test 4: Prim 6x6"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' \
    "$BASE_URL/labyrinth?algorithm=prim&height=6&width=6")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
assert_eq "height=6" "6" "$(jq '.height' /tmp/maze_e2e.json)"

# ---------- Test 5: Steps enabled -------------------------------------------
echo ""
echo "Test 5: Steps recording"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' \
    "$BASE_URL/labyrinth?steps=true")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
STEPS_LEN=$(jq '.steps | length' /tmp/maze_e2e.json)
assert_gte "steps array non-empty" 1 "$STEPS_LEN"

# ---------- Test 6: Bad algorithm → 400 -------------------------------------
echo ""
echo "Test 6: Unknown algorithm"
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
    "$BASE_URL/labyrinth?algorithm=bogus")
assert_eq "HTTP 400" "400" "$HTTP_CODE"

# ---------- Test 7: Out-of-range height → 400 --------------------------------
echo ""
echo "Test 7: Out-of-range height"
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
    "$BASE_URL/labyrinth?height=999")
assert_eq "HTTP 400" "400" "$HTTP_CODE"

# ---------- Test 8: 30x30 dimension check ------------------------------------
echo ""
echo "Test 8: 30x30 dimension check"
HTTP_CODE=$(curl -s -o /tmp/maze_e2e.json -w '%{http_code}' \
    "$BASE_URL/labyrinth?height=30&width=30")
assert_eq "HTTP 200" "200" "$HTTP_CODE"
assert_eq "height=30" "30" "$(jq '.height' /tmp/maze_e2e.json)"
ROW_COUNT=$(jq '.rows | length' /tmp/maze_e2e.json)
assert_eq "30 rows" "30" "$ROW_COUNT"
COL_COUNT=$(jq '.rows[0] | length' /tmp/maze_e2e.json)
assert_eq "width matches" "$(jq '.width' /tmp/maze_e2e.json)" "$COL_COUNT"

# ---------- Test 9: Cell structure check -------------------------------------
echo ""
echo "Test 9: Cell structure"
HAS_WALLS=$(jq '.rows[0][0] | has("walls")' /tmp/maze_e2e.json)
assert_eq "cell has walls" "true" "$HAS_WALLS"
WALL_LEN=$(jq '.rows[0][0].walls | length' /tmp/maze_e2e.json)
assert_eq "4 walls" "4" "$WALL_LEN"
HAS_GHOST=$(jq '.rows[0][0] | has("isGhost")' /tmp/maze_e2e.json)
assert_eq "cell has isGhost" "true" "$HAS_GHOST"
HAS_TP=$(jq '.rows[0][0] | has("isTP")' /tmp/maze_e2e.json)
assert_eq "cell has isTP" "true" "$HAS_TP"

# ---------- Summary ---------------------------------------------------------
echo ""
echo "============================================"
echo "Results: $PASS passed, $FAIL failed"
echo "============================================"

rm -f /tmp/maze_e2e.json

if (( FAIL > 0 )); then
    exit 1
fi
