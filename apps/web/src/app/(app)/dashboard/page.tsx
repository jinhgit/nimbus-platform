"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  API_BASE,
  fetchMe,
  fetchProjects,
  fetchWorkspaces,
  type MeResponse,
  type Project,
  type WorkspaceSummary,
} from "@/lib/api";

export default function DashboardPage() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [health, setHealth] = useState<string>("checking");

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
  }, []);

  useEffect(() => {
    const ws = me?.workspace?.id ?? workspaces[0]?.id;
    if (!ws) return;
    fetchProjects(ws).then((res) => {
      if (res.success && res.data) setProjects(res.data);
    });
  }, [me, workspaces]);

  const widgets = [
    { label: "API Health", value: health },
    { label: "Workspaces", value: String(workspaces.length) },
    { label: "Projects", value: String(projects.length) },
    { label: "Role", value: me?.role ?? "—" },
  ];

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            {me
              ? `${me.name} · ${me.workspace?.name ?? "워크스페이스 없음"}`
              : "사용자 정보 로딩…"}
          </p>
        </div>
        <Link
          href="/projects"
          className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)]"
        >
          프로젝트 보기
        </Link>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {widgets.map((w) => (
          <div
            key={w.label}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
          >
            <p className="text-xs text-[var(--muted)]">{w.label}</p>
            <p className="mt-2 text-2xl font-semibold tabular-nums capitalize">
              {w.value}
            </p>
          </div>
        ))}
      </div>

      <section className="mt-8 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <h2 className="mb-3 text-sm font-medium">최근 프로젝트</h2>
        {projects.length === 0 ? (
          <p className="text-sm text-[var(--muted)]">
            아직 프로젝트가 없습니다.{" "}
            <Link href="/projects" className="text-[var(--primary)] hover:underline">
              생성하기
            </Link>
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {projects.slice(0, 5).map((p) => (
              <li key={p.id} className="flex items-center justify-between py-3 text-sm">
                <span>{p.name}</span>
                <span className="text-xs text-[var(--muted)]">{p.status}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
