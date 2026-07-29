#!/usr/bin/env bash
# free-only: kind 로컬 클러스터 기동
set -euo pipefail

CLUSTER_NAME="${NIMBUS_KIND_CLUSTER:-nimbus}"

if ! command -v kind >/dev/null 2>&1; then
  echo "kind 가 없습니다. 설치: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
  echo "  brew install kind   # macOS"
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl 이 필요합니다."
  exit 1
fi

if kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "kind cluster '$CLUSTER_NAME' already exists"
else
  echo "Creating kind cluster '$CLUSTER_NAME'..."
  kind create cluster --name "$CLUSTER_NAME"
fi

kubectl cluster-info --context "kind-${CLUSTER_NAME}"
kubectl get nodes
echo ""
echo "Nimbus 는 context 'kind-${CLUSTER_NAME}' 를 사용합니다."
echo "API 재시작 후 /settings 또는 /infrastructure 에서 클러스터 상태를 확인하세요."
