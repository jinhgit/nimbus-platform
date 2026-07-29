"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  API_BASE,
  fetchK8sCluster,
  fetchMe,
  fetchProjects,
  fetchServices,
  fetchWorkspaces,
  type AppService,
  type K8sClusterStatus,
  type MeResponse,
  type Project,
  type WorkspaceSummary,
} from "@/lib/api";

function healthLabel(value: string) {
  if (value === "UP" || value === "up") return "정상";
  if (value === "checking") return "확인 중";
  if (value === "down") return "중단";
  return value;
}

export default function DashboardPage() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [services, setServices] = useState<AppService[]>([]);
  const [health, setHealth] = useState<string>("checking");
  const [cluster, setCluster] = useState<K8sClusterStatus | null>(null);

  useEffect(() => {
    fetch(`${API_BASE}/api/v1/health`)
      .then((r) => r.json())
      .then((j) => setHealth(j?.data?.status ?? "unknown"))
      .catch(() => setHealth("down"));

    fetchMe().then((res) => {
      if (res.success && res.data) setMe(res.data);
    });
    fetchWorkspaces().then((res) => {
      if (res.success && res.data) setWorkspaces(res.data);
    });
    fetchK8sCluster().then((res) => {
      if (res.success && res.data) setCluster(res.data);
    });
  }, []);

  useEffect(() => {
    const ws = me?.workspace?.id ?? workspaces[0]?.id;
    if (!ws) return;
    fetchProjects(ws).then((res) => {
      if (res.success && res.data) setProjects(res.data);
    });
    fetchServices({ workspaceId: ws }).then((res) => {
      if (res.success && res.data) setServices(res.data);
    });
  }, [me, workspaces]);

  const readyServices = services.filter((s) => s.status === "READY").length;
  const widgets = [
    { label: "API 상태", value: healthLabel(health) },
    { label: "프로젝트", value: String(projects.length) },
    { label: "서비스", value: String(services.length) },
    { label: "정상 배포", value: String(readyServices) },
  ];

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">대시보드</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            {me
              ? `${me.name} · ${me.workspace?.name ?? "워크스페이스 없음"}`
              : "사용자 정보를 불러오는 중…"}
          </p>
        </div>
        <Link
          href="/wizard"
          className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)]"
        >
          서비스 생성
        </Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {widgets.map((w) => (
          <div
            key={w.label}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
          >
            <p className="text-xs text-[var(--muted)]">{w.label}</p>
            <p className="mt-2 text-2xl font-semibold tabular-nums">{w.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <h2 className="mb-3 text-sm font-medium">최근 서비스</h2>
          {services.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">
              아직 서비스가 없습니다.{" "}
              <Link href="/wizard" className="text-[var(--primary)] hover:underline">
                Wizard 시작하기
              </Link>
            </p>
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {services.slice(0, 5).map((s) => (
                <li key={s.id} className="flex justify-between py-3 text-sm">
                  <span>{s.name}</span>
                  <span className="text-xs text-emerald-400">{s.status}</span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <h2 className="mb-3 text-sm font-medium">플랫폼 현황</h2>
          <ul className="space-y-2 text-sm text-[var(--muted)]">
            <li>
              클러스터:{" "}
              {cluster?.available
                ? `${cluster.clusterType ?? "local"} · ${cluster.context ?? ""} · 연결됨`
                : "미연결 (kind/k3d 시 실배포)"}
            </li>
            <li>GitHub: Settings에서 PAT 연결 · Free tier</li>
            <li>AI: rule-engine (Ollama 확장 가능)</li>
            <li>카탈로그: Golden Path 템플릿 준비됨</li>
          </ul>
          <div className="mt-4 flex flex-wrap gap-3">
            <Link href="/infrastructure" className="text-xs text-[var(--primary)] hover:underline">
              인프라
            </Link>
            <Link href="/catalog" className="text-xs text-[var(--primary)] hover:underline">
              카탈로그
            </Link>
            <Link href="/projects" className="text-xs text-[var(--primary)] hover:underline">
              프로젝트
            </Link>
            <Link href="/services" className="text-xs text-[var(--primary)] hover:underline">
              서비스
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
