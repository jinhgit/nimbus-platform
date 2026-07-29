"use client";

import { useEffect, useState } from "react";
import {
  fetchK8sCluster,
  fetchK8sDeployments,
  refreshK8sCluster,
  type K8sClusterStatus,
  type K8sDeployment,
} from "@/lib/api";

export default function InfrastructurePage() {
  const [cluster, setCluster] = useState<K8sClusterStatus | null>(null);
  const [deployments, setDeployments] = useState<K8sDeployment[]>([]);
  const [loading, setLoading] = useState(false);

  async function load() {
    const [c, d] = await Promise.all([fetchK8sCluster(), fetchK8sDeployments()]);
    if (c.success && c.data) setCluster(c.data);
    if (d.success && d.data) setDeployments(d.data);
  }

  useEffect(() => {
    load();
  }, []);

  async function onRefresh() {
    setLoading(true);
    const res = await refreshK8sCluster();
    setLoading(false);
    if (res.success && res.data) setCluster(res.data);
    await load();
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">인프라</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            로컬 Kubernetes (k3d / kind) · free-only · EKS 등 과금 컨텍스트 차단
          </p>
        </div>
        <button
          type="button"
          onClick={onRefresh}
          disabled={loading}
          className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5 disabled:opacity-60"
        >
          {loading ? "새로고침 중…" : "클러스터 새로고침"}
        </button>
      </div>

      <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-lg font-medium">클러스터 상태</h2>
          <span
            className={`rounded-full px-3 py-1 text-xs ${
              cluster?.available
                ? "bg-emerald-500/15 text-emerald-400"
                : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            {cluster?.available ? "연결됨" : "미연결 / 시뮬레이션"}
          </span>
        </div>
        {cluster ? (
          <ul className="space-y-1 text-sm text-[var(--muted)]">
            <li>Context: {cluster.context ?? "—"}</li>
            <li>Type: {cluster.clusterType ?? "—"}</li>
            <li>Version: {cluster.version ?? "—"}</li>
            <li>
              Nodes: {cluster.nodeCount} · Namespaces: {cluster.namespaceCount}
            </li>
            <li>메시지: {cluster.message ?? "—"}</li>
          </ul>
        ) : (
          <p className="text-sm text-[var(--muted)]">상태 로딩 중…</p>
        )}
        {!cluster?.available && (
          <div className="mt-4 rounded-lg border border-dashed border-[var(--border)] p-4 text-xs text-[var(--muted)]">
            <p className="mb-2 font-medium text-zinc-300">로컬 클러스터 준비</p>
            <pre className="overflow-x-auto text-[11px] leading-relaxed">
{`# kind
brew install kind
./scripts/kind-up.sh

# 또는 k3d
brew install k3d
./scripts/k3d-up.sh

# 이후 API 재시작 → 이 페이지에서 새로고침`}
            </pre>
          </div>
        )}
      </section>

      <section className="rounded-xl border border-[var(--border)] bg-[var(--card)]">
        <div className="border-b border-[var(--border)] px-5 py-3 text-sm font-medium">
          배포 목록
        </div>
        {deployments.length === 0 ? (
          <p className="p-8 text-center text-sm text-[var(--muted)]">
            배포 기록이 없습니다. Service Wizard에서 Deploy 하세요.
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {deployments.map((d) => (
              <li key={d.id} className="px-5 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">
                      {d.deploymentName}{" "}
                      <span className="text-xs text-[var(--muted)]">
                        · {d.namespaceName}
                      </span>
                    </p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      image: {d.image} · {d.clusterType ?? "—"} · {d.clusterContext ?? "—"}
                    </p>
                    {d.pods && d.pods.length > 0 && (
                      <ul className="mt-2 space-y-1 text-xs text-[var(--muted)]">
                        {d.pods.map((p) => (
                          <li key={p.name}>
                            {p.ready ? "●" : "○"} {p.name} · {p.phase}
                            {p.restarts > 0 ? ` · restart ${p.restarts}` : ""}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                  <div className="text-right text-xs">
                    <p
                      className={
                        d.status === "RUNNING"
                          ? "font-medium text-emerald-400"
                          : d.status === "SIMULATED"
                            ? "text-[var(--muted)]"
                            : "text-amber-400"
                      }
                    >
                      {d.status}
                    </p>
                    <p className="mt-1 text-[var(--muted)]">
                      Ready {d.readyReplicas ?? 0}/{d.replicas ?? 0}
                    </p>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
