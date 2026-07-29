"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  API_BASE,
  fetchDashboardOverview,
  fetchK8sCluster,
  fetchMe,
  fetchServices,
  type AppService,
  type DashboardOverview,
  type K8sClusterStatus,
  type MeResponse,
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
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  ReadOnlyBanner,
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

function formatTime(iso?: string) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("ko-KR");
  } catch {
    return iso;
  }
}

export default function DashboardPage() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [services, setServices] = useState<AppService[]>([]);
  const [health, setHealth] = useState<string>("checking");
  const [cluster, setCluster] = useState<K8sClusterStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const healthRes = await fetch(`${API_BASE}/api/v1/health`)
        .then((r) => r.json())
        .catch(() => null);
      setHealth(healthRes?.data?.status ?? "down");

      const meRes = await fetchMe();
      if (!meRes.success || !meRes.data) {
        setError(meRes.error?.message ?? "사용자 정보를 불러오지 못했습니다.");
        setMe(null);
        setOverview(null);
        return;
      }
      setMe(meRes.data);
      const ws = meRes.data.workspace?.id;
      if (!ws) {
        setError("워크스페이스가 없습니다. 로그인 후 다시 시도하세요.");
        setOverview(null);
        return;
      }

      const [ov, svc, k8s] = await Promise.all([
        fetchDashboardOverview(ws),
        fetchServices({ workspaceId: ws }),
        fetchK8sCluster(),
      ]);

      if (!ov.success || !ov.data) {
        setError(ov.error?.message ?? "대시보드 데이터를 불러오지 못했습니다.");
        setOverview(null);
      } else {
        setOverview(ov.data);
      }
      setServices(svc.success && svc.data ? svc.data : []);
      setCluster(k8s.success ? (k8s.data ?? null) : null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "대시보드 로드 실패");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <Page>
        <PageHeader eyebrow="Overview" title="Dashboard" />
        <LoadingBlock label="대시보드를 불러오는 중…" />
      </Page>
    );
  }

  const counts = overview?.counts;
  const canMutate = overview?.canMutate ?? me?.canMutate ?? true;
  const role = overview?.workspaceRole ?? me?.workspaceRole;

  return (
    <Page>
      <PageHeader
        eyebrow="Overview"
        title="Dashboard"
        description={
          me
            ? `${me.name} · ${me.workspace?.name ?? "워크스페이스 없음"}${
                role ? ` · ${role}` : ""
              }`
            : "워크스페이스 요약"
        }
        actions={
          canMutate ? (
            <Link href="/wizard" className="nimbus-btn-primary">
              <IconWizard size={15} />
              서비스 생성
            </Link>
          ) : null
        }
      />

      {error ? <ErrorBanner message={error} onRetry={load} /> : null}
      {!canMutate ? <ReadOnlyBanner role={role} /> : null}

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-4">
        <StatCard
          label="API"
          value={healthLabel(health)}
          tone={healthTone(health)}
          hint="플랫폼 헬스"
        />
        <StatCard
          label="Projects"
          value={counts?.projects ?? 0}
          tone="info"
          hint="비즈니스 컨텍스트"
        />
        <StatCard
          label="Services"
          value={counts?.services ?? 0}
          tone="default"
          hint="배포 단위"
        />
        <StatCard
          label="Environments"
          value={counts?.environments ?? 0}
          tone="info"
          hint="DEV / STAGE / PROD"
        />
        <StatCard
          label="Ready"
          value={counts?.readyServices ?? 0}
          tone={(counts?.readyServices ?? 0) > 0 ? "ok" : "default"}
          hint="READY 상태"
        />
        <StatCard
          label="Failed Sagas"
          value={counts?.failedSagas ?? 0}
          tone={(counts?.failedSagas ?? 0) > 0 ? "bad" : "ok"}
          hint="FAILED · ROLLED_BACK"
        />
        <StatCard
          label="Audit"
          value={counts?.auditEvents ?? 0}
          tone="default"
          hint="이벤트 누적"
        />
        <StatCard
          label="Cluster"
          value={cluster?.available ? "연결" : "끊김"}
          tone={cluster?.available ? "ok" : "warn"}
          hint={cluster?.clusterType ?? "local k8s"}
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
            <EmptyState
              title="아직 서비스가 없습니다"
              description="Wizard로 GitHub · K8s · Environment까지 한 번에 프로비저닝할 수 있습니다."
              action={
                canMutate ? (
                  <Link href="/wizard" className="nimbus-btn-primary">
                    Wizard 시작하기
                  </Link>
                ) : undefined
              }
            />
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
          <CardTitle>최근 Promote</CardTitle>
          {!overview?.recentPromotes?.length ? (
            <EmptyState
              title="승격 이력이 없습니다"
              description="Service Detail에서 DEV → STAGE → PRODUCTION으로 승격하면 여기에 표시됩니다."
            />
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {overview.recentPromotes.map((p) => (
                <li
                  key={p.id}
                  className="flex items-start justify-between gap-3 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-zinc-100">
                      {p.sourceType} → {p.targetType}
                    </p>
                    <p className="mt-0.5 text-[11px] text-[var(--muted)]">
                      {formatTime(p.at)}
                      {p.message ? ` · ${p.message}` : ""}
                    </p>
                  </div>
                  <StatusBadge value={p.status} />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardTitle
            action={
              <Link
                href="/wizard"
                className="text-xs text-[var(--primary)] hover:underline"
              >
                Wizard
              </Link>
            }
          >
            실패 Saga
          </CardTitle>
          {!overview?.failedSagas?.length ? (
            <EmptyState
              title="실패한 프로비저닝이 없습니다"
              description="FAILED / ROLLED_BACK Saga가 여기에 모입니다."
            />
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {overview.failedSagas.map((s) => (
                <li
                  key={s.id}
                  className="flex items-start justify-between gap-3 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-zinc-100">
                      attempt {s.attempt}
                    </p>
                    <p className="mt-0.5 truncate text-[11px] text-[var(--muted)]">
                      {s.failureReason ?? "사유 없음"} · {formatTime(s.at)}
                    </p>
                  </div>
                  <StatusBadge value={s.status} />
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          <CardTitle
            action={
              <Link
                href="/audit"
                className="text-xs text-[var(--primary)] hover:underline"
              >
                Audit 전체
              </Link>
            }
          >
            Audit 요약
          </CardTitle>
          {!overview?.recentAudits?.length ? (
            <EmptyState
              title="감사 이벤트가 없습니다"
              description="로그인·생성·승격 등 mutation이 기록되면 여기에 표시됩니다."
            />
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {overview.recentAudits.map((a) => (
                <li key={a.id} className="py-2.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs text-sky-300">
                      {a.action}
                    </span>
                    {a.resourceType ? (
                      <span className="text-[11px] text-[var(--muted)]">
                        {a.resourceType}
                        {a.resourceName ? ` · ${a.resourceName}` : ""}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-0.5 text-[11px] text-[var(--muted)]">
                    {a.actorName ?? "—"} · {formatTime(a.at)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <Card className="mt-5">
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
        </ul>
        <div className="mt-5 flex flex-wrap gap-2 border-t border-[var(--border)] pt-4">
          <Link
            href="/infrastructure"
            className="nimbus-btn-ghost !px-3 !py-1.5 text-xs"
          >
            <IconInfrastructure size={14} />
            인프라
          </Link>
          <Link href="/catalog" className="nimbus-btn-ghost !px-3 !py-1.5 text-xs">
            <IconCatalog size={14} />
            카탈로그
          </Link>
          <Link
            href="/projects"
            className="nimbus-btn-ghost !px-3 !py-1.5 text-xs"
          >
            <IconProjects size={14} />
            프로젝트
          </Link>
        </div>
      </Card>
    </Page>
  );
}
