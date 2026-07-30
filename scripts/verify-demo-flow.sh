#!/usr/bin/env bash
# Core demo flow verification (API). Exit non-zero on failure.
set -euo pipefail
API="${API_URL:-http://localhost:8080}"
STAMP=$(date +%s)
EMAIL="verify-${STAMP}@nimbus.local"

echo "== health =="
curl -sf "$API/api/v1/health" | grep -q '"status":"UP"'
echo OK

echo "== openapi =="
curl -sf "$API/v3/api-docs" | head -c 200 | grep -q openapi
echo OK

echo "== login =="
LOGIN=$(curl -sf -X POST "$API/api/v1/auth/dev-login" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"Verify User\",\"email\":\"$EMAIL\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
WS=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['user']['workspaceId'])")
test -n "$TOKEN" && test -n "$WS"
echo OK workspace=$WS

AUTH="Authorization: Bearer $TOKEN"

echo "== me / permissions / dashboard =="
curl -sf "$API/api/v1/auth/me" -H "$AUTH" | grep -q canMutate
curl -sf "$API/api/v1/auth/permissions" -H "$AUTH" | grep -q WORKSPACE_MUTATE
curl -sf "$API/api/v1/dashboard/overview?workspaceId=$WS" -H "$AUTH" | grep -q openIncidents
echo OK

echo "== project =="
PROJ=$(curl -sf -X POST "$API/api/v1/projects" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Verify Project $STAMP\",\"workspaceId\":\"$WS\",\"description\":\"demo verify\"}")
PID=$(echo "$PROJ" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo OK project=$PID

echo "== catalog =="
CAT=$(curl -sf "$API/api/v1/catalog" -H "$AUTH")
TID=$(echo "$CAT" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(d[0]['id'])")
curl -sf "$API/api/v1/catalog/$TID" -H "$AUTH" | grep -q blueprint
echo OK template=$TID

echo "== wizard provision =="
WIZ=$(curl -sf -X POST "$API/api/v1/service-wizard" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"projectId\":\"$PID\",\"serviceName\":\"verify-api-$STAMP\",\"templateId\":\"$TID\"}")
WID=$(echo "$WIZ" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
RUNTIME=$(echo "$CAT" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'][0]['runtime'])")
curl -sf -X PATCH "$API/api/v1/service-wizard/$WID" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"templateId\":\"$TID\",\"runtime\":\"$RUNTIME\",\"environmentType\":\"DEV\",\"replicaCount\":1,\"currentStep\":4}" >/dev/null
curl -sf -X POST "$API/api/v1/service-wizard/$WID/preview" -H "$AUTH" >/dev/null
curl -sf -X POST "$API/api/v1/service-wizard/$WID/execute" -H "$AUTH" >/dev/null

SID=""
for i in $(seq 1 50); do
  ST=$(curl -sf "$API/api/v1/service-wizard/$WID" -H "$AUTH")
  STATUS=$(echo "$ST" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['status'])")
  if [[ "$STATUS" == "COMPLETED" ]]; then
    SID=$(echo "$ST" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'].get('serviceId') or '')")
    break
  fi
  if [[ "$STATUS" == "FAILED" ]]; then
    echo "FAIL wizard FAILED"; echo "$ST"; exit 1
  fi
  sleep 0.4
done
test -n "$SID"
echo OK service=$SID

echo "== environments / tags / argo / notifications =="
python3 - <<PY
import json, urllib.request
api="$API"; sid="$SID"; ws="$WS"; token="$TOKEN"
def get(path, method="GET", body=None):
    req = urllib.request.Request(api+path, method=method, data=body,
        headers={"Authorization":"Bearer "+token, "Content-Type":"application/json", "Accept":"application/json"})
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read().decode())
env = get(f"/api/v1/services/{sid}/environments")
items = env["data"].get("items") or env["data"]
assert env["success"] and any(i.get("type")=="DEV" for i in items), env
tags = get(f"/api/v1/services/{sid}/tags", "PUT", json.dumps({"tags":["demo","verify"]}).encode())
assert "demo" in (tags["data"].get("tags") or []), tags
filt = get(f"/api/v1/services?workspaceId={ws}&tag=demo")
assert any(x.get("id")==sid for x in filt["data"]), filt
argo = get(f"/api/v1/services/{sid}/argo-sync")
assert argo["data"]["mode"] in ("SIMULATED","LIVE")
assert "Application" in (argo["data"].get("applicationManifest") or "")
notif = get(f"/api/v1/notifications/sync?workspaceId={ws}", "POST")
assert "scanned" in notif["data"]
inc = get(f"/api/v1/incidents/scan?workspaceId={ws}", "POST")
assert "scanned" in inc["data"]
print("OK substeps")
PY
echo OK

echo "== audit =="
python3 - <<PY
import json, urllib.request
req = urllib.request.Request("$API/api/v1/audit?workspaceId=$WS&limit=50",
    headers={"Authorization":"Bearer $TOKEN", "Accept":"application/json"})
with urllib.request.urlopen(req) as r:
    d = json.loads(r.read().decode())
items = d["data"].get("items") if isinstance(d.get("data"), dict) else d.get("data")
assert items and len(items) >= 1
print("OK audit", len(items))
PY
echo OK

echo ""
echo "========================================"
echo " DEMO CORE FLOW VERIFIED SUCCESSFULLY"
echo "========================================"
echo "workspace=$WS project=$PID service=$SID wizard=$WID"
