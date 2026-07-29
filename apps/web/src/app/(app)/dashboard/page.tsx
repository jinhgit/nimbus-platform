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
import {
  IconCatalog,
  IconInfrastructure,
  IconProjects,
  IconWizard,
} from "@/components/icons";
import {
  Card,
  CardTitle,
  Page,
  PageHeader,
  StatCard,
  StatusBadge,
} from "@/components/ui";

function healthTone(value: string): "ok" | "warn" | "bad" | "default" {
  if (value === "UP" || value === "up") return "ok";
  if (value === "checking") return "warn";
  if (value === "down") return "bad";
  return "default";
}

function healthLabel(value: string) {
  if (value === "UP" || value === "up") return "정상";
  if (value === "checking") return "확인 중…";
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

  return (
    <Page>
      <PageHeader
        eyebrow="Overview"
        title="Dashboard"
        description={
          me
            ? `${me.name} · ${me.workspace?.name ?? "워크스페이스 없음"}`
            : "워크스페이스를 불러오는 중…"
        }
        actions={
          <Link href="/wizard" className="nimbus-btn-primary">
            <IconWizard size={15} />
            서비스 생성
          </Link>
        }
      />

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="API"
          value={healthLabel(health)}
          tone={healthTone(health)}
          hint="플랫폼 헬스"
        />
        <StatCard
          label="Projects"
          value={projects.length}
          tone="info"
          hint="비즈니스 컨텍스트"
        />
        <StatCard
          label="Services"
          value={services.length}
          tone="default"
          hint="배포 단위"
        />
        <StatCard
          label="Ready"
          value={readyServices}
          tone={readyServices > 0 ? "ok" : "default"}
          hint="READY 상태"
        />
      </div>

      <div className="mt-6 grid gap-5 lg:grid-cols-2">
        <Card>
          <CardTitle
            action={
              <Link
                href="/services"
                className="text-xs text-[var(--primary)] hover:underline"
              >
                전체 보기
              </Link>
            }
          >
            최근 서비스
          </CardTitle>
          {services.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">
              아직 서비스가 없습니다.{" "}
              <Link href="/wizard" className="text-[var(--primary)] hover:underline">
                Wizard 시작하기
              </Link>
            </p>
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {services.slice(0, 6).map((s) => (
                <li key={s.id}>
                  <Link
                    href={`/services/${s.id}`}
                    className="-mx-2 flex items-center justify-between rounded-lg px-2 py-2.5 transition hover:bg-white/[0.03]"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-zinc-100">
                        {s.name}
                      </p>
                      <p className="truncate text-[11px] text-[var(--muted)]">
                        {s.runtime} · {s.environmentType}
                      </p>
                    </div>
                    <StatusBadge value={s.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardTitle>플랫폼 현황</CardTitle>
          <ul className="space-y-3 text-sm">
            <li className="flex items-start justify-between gap-3">
              <span className="text-[var(--muted)]">클러스터</span>
              <span className="text-right text-zinc-200">
                {cluster?.available ? (
                  <>
                    <StatusBadge value="CONNECTED" />
                    <span className="mt-1 block text-[11px] text-[var(--muted)]">
                      {cluster.clusterType ?? "local"}
                      {cluster.context ? ` · ${cluster.context}` : ""}
                    </span>
                  </>
                ) : (
                  <StatusBadge value="DISCONNECTED" />
                )}
              </span>
            </li>
            <li className="flex justify-between gap-3 text-[var(--muted)]">
              <span>GitHub SCM</span>
              <span className="text-zinc-300">Settings · OAuth / PAT</span>
            </li>
            <li className="flex justify-between gap-3 text-[var(--muted)]">
              <span>AI</span>
              <span className="text-zinc-300">Rule engine · Ollama 확장 가능</span>
            </li>
            <li className="flex justify-between gap-3 text-[var(--muted)]">
              <span>Catalog</span>
              <span className="text-zinc-300">Golden Path 템플릿</span>
            </li>
          </ul>

          <div className="mt-5 flex flex-wrap gap-2 border-t border-[var(--border)] pt-4">
            <Link href="/infrastructure" className="nimbus-btn-ghost !px-3 !py-1.5 text-xs">
              <IconInfrastructure size={14} />
              인프라
            </Link>
            <Link href="/catalog" className="nimbus-btn-ghost !px-3 !py-1.5 text-xs">
              <IconCatalog size={14} />
              카탈로그
            </Link>
            <Link href="/projects" className="nimbus-btn-ghost !px-3 !py-1.5 text-xs">
              <IconProjects size={14} />
              프로젝트
            </Link>
          </div>
        </Card>
      </div>
    </Page>
  );
}
