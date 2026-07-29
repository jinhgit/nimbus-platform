#!/usr/bin/env bash
# Fail if docs/api/openapi.yaml and classpath openapi.yaml drift.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DOC="$ROOT/docs/api/openapi.yaml"
CP="$ROOT/apps/api/src/main/resources/openapi.yaml"

if [[ ! -f "$DOC" || ! -f "$CP" ]]; then
  echo "ERROR: openapi files missing" >&2
  exit 1
fi

if ! cmp -s "$DOC" "$CP"; then
  echo "ERROR: OpenAPI drift detected." >&2
  echo "  docs/api/openapi.yaml" >&2
  echo "  apps/api/src/main/resources/openapi.yaml" >&2
  echo "Sync with: cp docs/api/openapi.yaml apps/api/src/main/resources/openapi.yaml" >&2
  diff -u "$CP" "$DOC" || true
  exit 1
fi

REQUIRED_PATHS=(
  "/api/v1/dashboard/overview"
  "/api/v1/auth/permissions"
  "/api/v1/workspaces/{workspaceId}/members"
  "/api/v1/workspaces/{workspaceId}/members/invite"
  "/api/v1/environments/{environmentId}/promote"
  "/api/v1/services/{serviceId}/promotions"
  "/api/v1/pipelines/github-runs"
  "/api/v1/incidents"
  "/api/v1/incidents/scan"
  "/api/v1/ai/status"
  "/api/v1/ai/yaml/explain"
  "/api/v1/audit"
)

for p in "${REQUIRED_PATHS[@]}"; do
  if ! grep -qF "$p" "$DOC"; then
    echo "ERROR: required OpenAPI path missing: $p" >&2
    exit 1
  fi
done

echo "OpenAPI sync OK · required paths present"
