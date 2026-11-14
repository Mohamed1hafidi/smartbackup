#!/usr/bin/env bash
# ==============================================================
# SmartBackup — Script de vérification post-déploiement (Bash)
# Usage : bash scripts/verify-deployment.sh BACKEND_URL [FRONTEND_URL] [SCHEDULER_SECRET]
# ==============================================================

BACKEND_URL="${1:-}"
FRONTEND_URL="${2:-}"
SCHEDULER_SECRET="${3:-}"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
PASS=0; FAIL=0

check() {
    local name="$1" url="$2" method="${3:-GET}" expected="${4:-200}" extra_args="${5:-}"
    code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" $extra_args --max-time 15 "$url" 2>/dev/null)
    if [ "$code" = "$expected" ]; then
        echo -e "${GREEN}[PASS]${NC} $name (HTTP $code)"
        ((PASS++))
    else
        echo -e "${RED}[FAIL]${NC} $name (HTTP $code, attendu $expected)"
        ((FAIL++))
    fi
}

echo ""
echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN} SmartBackup — Vérification de déploiement${NC}"
echo -e "${CYAN}============================================${NC}"
echo " Backend  : $BACKEND_URL"
[ -n "$FRONTEND_URL" ] && echo " Frontend : $FRONTEND_URL"
echo ""

# 1. Health
echo -e "${YELLOW}--- 1. Backend Health ---${NC}"
check "Actuator Health" "$BACKEND_URL/actuator/health"

# 2. Auth
echo ""
echo -e "${YELLOW}--- 2. API Authentification ---${NC}"
EMAIL="test-$(date +%s)@smartbackup.test"
PASS_VAL="TestPass123!@"

check "POST /api/auth/register" "$BACKEND_URL/api/auth/register" "POST" "200" \
    "-H 'Content-Type: application/json' -d '{\"email\":\"$EMAIL\",\"password\":\"$PASS_VAL\",\"fullName\":\"Test\"}'"

TOKEN=$(curl -s -X POST "$BACKEND_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS_VAL\"}" | \
    python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null)

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}[PASS]${NC} POST /api/auth/login (Token obtenu)"
    ((PASS++))

    echo ""
    echo -e "${YELLOW}--- 3. API Authentifiée ---${NC}"
    for endpoint in users/me files backups schedules notifications "dashboard/stats"; do
        check "GET /api/$endpoint" "$BACKEND_URL/api/$endpoint" "GET" "200" \
            "-H 'Authorization: Bearer $TOKEN'"
    done
else
    echo -e "${RED}[FAIL]${NC} POST /api/auth/login (Pas de token)"
    ((FAIL++))
fi

# 4. Scheduler
if [ -n "$SCHEDULER_SECRET" ]; then
    echo ""
    echo -e "${YELLOW}--- 4. Trigger Scheduler ---${NC}"
    check "POST /api/internal/scheduler/trigger" \
        "$BACKEND_URL/api/internal/scheduler/trigger" "POST" "200" \
        "-H 'X-Scheduler-Secret: $SCHEDULER_SECRET' -H 'Content-Type: application/json'"
fi

# 5. Frontend
if [ -n "$FRONTEND_URL" ]; then
    echo ""
    echo -e "${YELLOW}--- 5. Frontend Vercel ---${NC}"
    check "Frontend index.html" "$FRONTEND_URL"
fi

# Résumé
echo ""
echo -e "${CYAN}============================================${NC}"
TOTAL=$((PASS + FAIL))
if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN} Résultats : $PASS/$TOTAL réussis ✓${NC}"
else
    echo -e "${YELLOW} Résultats : $PASS/$TOTAL réussis ($FAIL échecs)${NC}"
fi
echo -e "${CYAN}============================================${NC}"
echo ""
[ $FAIL -gt 0 ] && exit 1 || exit 0
