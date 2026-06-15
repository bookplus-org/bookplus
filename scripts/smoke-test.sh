#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════════════════
#  BookPlus — Smoke test de Fase 0
#  Valida, contra el API Gateway (8080), el flujo JWT extremo a extremo,
#  el formato de error unificado (RFC 7807) y la cabecera CORS.
#
#  Uso:   make up   &&   bash scripts/smoke-test.sh
#  Requiere: curl, jq
# ════════════════════════════════════════════════════════════════════════════
set -uo pipefail

GW="${GATEWAY_URL:-http://localhost:8080}"
ORIGIN="${FRONTEND_ORIGIN:-http://localhost:4200}"
EMAIL="smoke_$(date +%s)@bookplus.com"
PASS="Secret123!"
ok=0; fail=0
green(){ printf "\033[32m✔ %s\033[0m\n" "$1"; ok=$((ok+1)); }
red(){   printf "\033[31mx %s\033[0m\n" "$1"; fail=$((fail+1)); }

echo "── 1. Registro ───────────────────────────────────────────────"
code=$(curl -s -o /tmp/reg.json -w '%{http_code}' -X POST "$GW/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"smoke\",\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
[[ "$code" =~ ^20 ]] && green "register → $code" || red "register → $code (esperado 200/201)"

echo "── 2. Login ─────────────────────────────────────────────────"
TOKEN=$(curl -s -X POST "$GW/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | jq -r '.accessToken // .access_token // empty')
[[ -n "$TOKEN" ]] && green "login → token recibido" || red "login → sin token"

echo "── 3. Ruta protegida CON token ──────────────────────────────"
code=$(curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/cart" -H "Authorization: Bearer $TOKEN")
[[ "$code" == "200" ]] && green "GET /cart (auth) → 200" || red "GET /cart (auth) → $code"

echo "── 4. Ruta protegida SIN token (espera 401 + ProblemDetail) ─"
code=$(curl -s -o /tmp/err.json -w '%{http_code}' "$GW/api/v1/orders")
ctype=$(jq -r '.type // empty' /tmp/err.json 2>/dev/null)
[[ "$code" == "401" ]] && green "GET /orders (no auth) → 401" || red "GET /orders (no auth) → $code"

echo "── 5. Formato de error unificado (validación) ───────────────"
curl -s -o /tmp/val.json -X POST "$GW/api/v1/auth/login" \
  -H 'Content-Type: application/json' -d '{"email":"bad","password":""}' >/dev/null
has=$(jq -r 'has("type") and has("title") and has("status") and has("timestamp")' /tmp/val.json 2>/dev/null)
typ=$(jq -r '.type // ""' /tmp/val.json 2>/dev/null)
[[ "$has" == "true" && "$typ" == https://bookplus.com/errors/* ]] \
  && green "error RFC7807 con type/title/status/timestamp y URI bookplus.com" \
  || red "el payload de error no tiene el formato unificado (ver /tmp/val.json)"

echo "── 6. CORS preflight desde el origen Angular ────────────────"
acao=$(curl -s -D - -o /dev/null -X OPTIONS "$GW/api/v1/books" \
  -H "Origin: $ORIGIN" -H 'Access-Control-Request-Method: GET' \
  | grep -i 'access-control-allow-origin' | tr -d '\r')
[[ -n "$acao" ]] && green "CORS → $acao" || red "sin cabecera Access-Control-Allow-Origin para $ORIGIN"

echo
echo "═════════════════════════════════════════════════════════════"
printf "Resultado: %d OK, %d fallos\n" "$ok" "$fail"
[[ "$fail" -eq 0 ]] && echo "Fase 0 verificada ✔" || { echo "Revisa los fallos arriba."; exit 1; }
