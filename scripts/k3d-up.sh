#!/usr/bin/env bash
# free-only: k3d 로컬 클러스터 기동
set -euo pipefail

CLUSTER_NAME="${NIMBUS_K3D_CLUSTER:-nimbus}"

if ! command -v k3d >/dev/null 2>&1; then
  echo "k3d 가 없습니다. 설치: https://k3d.io/"
  echo "  brew install k3d   # macOS"
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl 이 필요합니다."
  exit 1
fi

if k3d cluster list 2>/dev/null | grep -q "$CLUSTER_NAME"; then
  echo "k3d cluster '$CLUSTER_NAME' already exists"
  k3d cluster start "$CLUSTER_NAME" || true
else
  echo "Creating k3d cluster '$CLUSTER_NAME'..."
  k3d cluster create "$CLUSTER_NAME" --agents 1
fi

kubectl cluster-info --context "k3d-${CLUSTER_NAME}"
kubectl get nodes
echo ""
echo "Nimbus 는 context 'k3d-${CLUSTER_NAME}' 를 사용합니다."
