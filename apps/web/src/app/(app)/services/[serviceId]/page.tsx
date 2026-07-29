"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  fetchArchitectureReview,
  fetchK8sDeploymentByService,
  fetchPipelines,
  fetchService,
  fetchServiceLogs,
  fetchServiceMetrics,
  type AppService,
  type ArchitectureReview,
  type K8sDeployment,
  type LogLine,
  type Pipeline,
  type ServiceMetrics,
} from "@/lib/api";

function StatusBadge({ value }: { value?: string | null }) {
  if (!value) {
    return (
      <span className="rounded-full border border-[var(--border)] px-2 py-0.5 text-xs text-[var(--muted)]">
        —
      </span>
    );
  }
  const ok =
    value === "READY" ||
    value === "RUNNING" ||
    value === "SUCCESS" ||
    value === "SIMULATED";
  const bad = value === "FAILED" || value === "ERROR";
  return (
    <span
      className={`rounded-full px-2 py-0.5 text-xs ${
        ok
          ? "bg-emerald-500/15 text-emerald-400"
          : bad
            ? "bg-red-500/15 text-red-300"
            : "bg-amber-500/15 text-amber-300"
      }`}
    >
      {value}
    </span>
  );
}

export default function ServiceDetailPage() {
  const params = useParams();
  const serviceId = String(params.serviceId ?? "");

  const [service, setService] = useState<AppService | null>(null);
  const [deploy, setDeploy] = useState<K8sDeployment | null>(null);
  const [metrics, setMetrics] = useState<ServiceMetrics | null>(null);
  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [review, setReview] = useState<ArchitectureReview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [reviewLoading, setReviewLoading] = useState(false);

  const load = useCallback(async () => {
    if (!serviceId) return;
    setLoading(true);
    setError(null);
    try {
      const [s, d, m, p, l] = await Promise.all([
        fetchService(serviceId),
        fetchK8sDeploymentByService(serviceId),
        fetchServiceMetrics(serviceId),
        fetchPipelines({ serviceId }),
        fetchServiceLogs(serviceId, 30),
      ]);
      if (!s.success || !s.data) {
        setError(s.error?.message ?? "서비스를 찾을 수 없습니다.");
        setService(null);
        return;
      }
      setService(s.data);
      setDeploy(d.success ? (d.data ?? null) : null);
      setMetrics(m.success ? (m.data ?? null) : null);
      setPipelines(p.success && p.data ? p.data : []);
      setLogs(l.success && l.data ? l.data.lines : []);
    } catch {
      setError("서비스 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [serviceId]);

  useEffect(() => {
    load();
  }, [load]);

  async function runArchitectureReview() {
    if (!service?.wizardId) {
      setError("이 서비스에 연결된 Wizard가 없어 Architecture Review를 실행할 수 없습니다.");
      return;
    }
    setReviewLoading(true);
    setError(null);
    const res = await fetchArchitectureReview(service.wizardId);
    setReviewLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "Architecture Review 실패");
      return;
    }
    setReview(res.data);
  }

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-sm text-[var(--muted)]">
        로딩 중…
      </div>
    );
  }

  if (!service) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <p className="text-[var(--muted)]">{error ?? "서비스 없음"}</p>
        <Link href="/services" className="mt-4 inline-block text-[var(--primary)] hover:underline">
          서비스 목록으로
        </Link>
      </div>
    );
  }

  const latestPipeline = pipelines[0];

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      {/* Header */}
      <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2 text-xs text-[var(--muted)]">
            <Link href="/services" className="hover:text-white">
              서비스
            </Link>
            <span>/</span>
            <span>{service.name}</span>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{service.name}</h1>
            <StatusBadge value={service.status} />
          </div>
          <p className="mt-2 text-sm text-[var(--muted)]">
            {service.runtime} · {service.environmentType}
            {service.databaseType ? ` · DB ${service.databaseType}` : ""}
            {service.cacheType ? ` · Cache ${service.cacheType}` : ""}
            {" · "}레플리카 {service.replicaCount ?? 1}
            {service.hpaEnabled ? " · HPA" : ""}
          </p>
          {service.description && (
            <p className="mt-1 text-sm text-[var(--muted)]">{service.description}</p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href={`/logs?serviceId=${service.id}`}
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            로그
          </Link>
          <Link
            href="/pipelines"
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            파이프라인
          </Link>
          <Link
            href="/monitoring"
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            모니터링
          </Link>
          <button
            type="button"
            onClick={load}
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            새로고침
          </button>
        </div>
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      {/* Summary cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          title="GitHub"
          status={service.githubRepoUrl ? "CONNECTED" : "—"}
          body={
            service.githubRepoUrl ? (
              <a
                href={service.githubRepoUrl}
                target="_blank"
                rel="noreferrer"
                className="text-[var(--primary)] hover:underline"
              >
                {service.githubOwner}/{service.githubRepoName}
              </a>
            ) : (
              <span className="text-[var(--muted)]">미연결 / 시뮬레이션</span>
            )
          }
        />
        <SummaryCard
          title="Kubernetes"
          status={service.k8sStatus ?? "—"}
          body={
            service.k8sNamespace ? (
              <span>
                {service.k8sNamespace}/{service.k8sDeployment}
                {service.k8sClusterType ? (
                  <span className="block text-[var(--muted)]">
                    {service.k8sClusterType}
                  </span>
                ) : null}
              </span>
            ) : (
              <span className="text-[var(--muted)]">미배포</span>
            )
          }
        />
        <SummaryCard
          title="Pipeline"
          status={latestPipeline?.status ?? "—"}
          body={
            latestPipeline ? (
              <span>
                {latestPipeline.progress ?? 0}% · {latestPipeline.currentStep}
                {latestPipeline.imageTag && (
                  <span className="mt-1 block text-sky-400">{latestPipeline.imageTag}</span>
                )}
              </span>
            ) : (
              <span className="text-[var(--muted)]">실행 이력 없음</span>
            )
          }
        />
        <SummaryCard
          title="Metrics"
          status={metrics?.source ?? "—"}
          body={
            metrics ? (
              <span className="flex flex-wrap gap-2">
                {metrics.metrics.slice(0, 4).map((m) => (
                  <span key={m.name} className="rounded border border-[var(--border)] px-1.5 py-0.5">
                    {m.name} {m.value}
                    {m.unit === "%" ? "%" : ""}
                  </span>
                ))}
              </span>
            ) : (
              <span className="text-[var(--muted)]">—</span>
            )
          }
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* K8s detail */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <h2 className="mb-3 text-sm font-medium">배포 / Pod</h2>
          {!deploy ? (
            <p className="text-sm text-[var(--muted)]">
              K8s 배포 기록이 없습니다. kind 연결 후 Wizard Deploy 시 생성됩니다.
            </p>
          ) : (
            <div className="space-y-3 text-sm">
              <div className="flex flex-wrap gap-2">
                <StatusBadge value={deploy.status} />
                <span className="text-[var(--muted)]">
                  Ready {deploy.readyReplicas ?? 0}/{deploy.replicas ?? 0}
                </span>
              </div>
              <p className="text-xs text-[var(--muted)]">
                image: {deploy.image} · {deploy.clusterType} · {deploy.clusterContext}
              </p>
              {deploy.message && (
                <p className="text-xs text-[var(--muted)]">{deploy.message}</p>
              )}
              {deploy.pods && deploy.pods.length > 0 ? (
                <ul className="space-y-1 text-xs">
                  {deploy.pods.map((pod) => (
                    <li key={pod.name} className="flex justify-between gap-2">
                      <span>
                        {pod.ready ? "●" : "○"} {pod.name}
                      </span>
                      <span className="text-[var(--muted)]">
                        {pod.phase}
                        {pod.restarts > 0 ? ` · rst ${pod.restarts}` : ""}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-xs text-[var(--muted)]">Pod 목록 없음 (sim 또는 미연결)</p>
              )}
              <Link
                href="/infrastructure"
                className="inline-block text-xs text-[var(--primary)] hover:underline"
              >
                인프라 페이지 →
              </Link>
            </div>
          )}
        </section>

        {/* Pipeline */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium">이미지 빌드 파이프라인</h2>
            <Link href="/pipelines" className="text-xs text-[var(--primary)] hover:underline">
              전체 보기
            </Link>
          </div>
          {pipelines.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">파이프라인 이력이 없습니다.</p>
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {pipelines.slice(0, 5).map((p) => (
                <li key={p.id} className="flex justify-between gap-3 py-2">
                  <div>
                    <p className="text-xs text-[var(--muted)]">{p.name}</p>
                    <p className="text-xs">{p.currentStep}</p>
                  </div>
                  <div className="text-right">
                    <StatusBadge value={p.status} />
                    <p className="mt-1 text-xs text-[var(--muted)]">{p.progress ?? 0}%</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Logs preview */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium">최근 로그</h2>
            <Link
              href={`/logs?serviceId=${service.id}`}
              className="text-xs text-[var(--primary)] hover:underline"
            >
              전체 / 스트림 →
            </Link>
          </div>
          <div className="max-h-56 overflow-auto rounded-lg border border-[var(--border)] bg-black/40 p-3 font-mono text-[11px] leading-relaxed">
            {logs.length === 0 ? (
              <p className="text-[var(--muted)]">로그 없음</p>
            ) : (
              logs.slice(-20).map((l, i) => (
                <div key={`${l.timestamp}-${i}`} className="text-zinc-300">
                  <span className="text-zinc-500">
                    {new Date(l.timestamp).toLocaleTimeString()}
                  </span>{" "}
                  <span className="text-emerald-500">{l.level}</span> {l.message}
                </div>
              ))
            )}
          </div>
        </section>

        {/* AI Architecture Review */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between gap-2">
            <h2 className="text-sm font-medium">AI Architecture Review</h2>
            <button
              type="button"
              onClick={runArchitectureReview}
              disabled={reviewLoading || !service.wizardId}
              className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
            >
              {reviewLoading ? "분석 중…" : "Analyze"}
            </button>
          </div>
          {!review ? (
            <p className="text-sm text-[var(--muted)]">
              Analyze를 눌러 Architecture Score와 권장 사항을 확인하세요.
            </p>
          ) : (
            <div className="space-y-3 text-sm">
              <p className="text-3xl font-semibold tabular-nums text-emerald-400">
                {review.score}
                <span className="ml-1 text-sm font-normal text-[var(--muted)]">/ 100</span>
              </p>
              <p className="text-xs text-[var(--muted)]">provider: {review.provider}</p>
              {review.strengths?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-emerald-400">강점</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.strengths.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
              {review.risks?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-amber-400">리스크</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.risks.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
              {review.recommendations?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-sky-400">추천</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.recommendations.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function SummaryCard({
  title,
  status,
  body,
}: {
  title: string;
  status: string;
  body: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
      <div className="mb-2 flex items-center justify-between gap-2">
        <p className="text-xs text-[var(--muted)]">{title}</p>
        <StatusBadge value={status} />
      </div>
      <div className="text-sm">{body}</div>
    </div>
  );
}
