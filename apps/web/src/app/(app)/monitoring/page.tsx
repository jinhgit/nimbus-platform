"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  fetchMe,
  fetchMonitoringOverview,
  type MonitoringOverview,
} from "@/lib/api";

export default function MonitoringPage() {
  const [overview, setOverview] = useState<MonitoringOverview | null>(null);

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      const res = await fetchMonitoringOverview(ws);
      if (res.success && res.data) setOverview(res.data);
    });
  }, []);

  const links = overview?.links;

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">Monitoring</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Prometheus · Grafana (free-only 로컬). 스택 미기동 시 demo 메트릭을 표시합니다.
        </p>
      </div>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: "서비스", value: overview?.serviceCount ?? "—" },
          { label: "실행 중 배포", value: overview?.runningDeployments ?? "—" },
          { label: "평균 CPU", value: overview ? `${overview.avgCpu}%` : "—" },
          { label: "평균 메모리", value: overview ? `${overview.avgMemory}%` : "—" },
        ].map((w) => (
          <div
            key={w.label}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
          >
            <p className="text-xs text-[var(--muted)]">{w.label}</p>
            <p className="mt-2 text-2xl font-semibold tabular-nums">{w.value}</p>
          </div>
        ))}
      </div>

      <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <h2 className="mb-3 text-sm font-medium">Observability Stack</h2>
        <div className="flex flex-wrap gap-3 text-sm">
          <span
            className={`rounded-full px-3 py-1 text-xs ${
              links?.mode === "live"
                ? "bg-emerald-500/15 text-emerald-400"
                : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            mode: {links?.mode ?? "—"}
          </span>
          {links?.prometheusUrl && (
            <a
              href={links.prometheusUrl}
              target="_blank"
              rel="noreferrer"
              className="text-[var(--primary)] hover:underline"
            >
              Prometheus {links.prometheusUp ? "●" : "○"}
            </a>
          )}
          {links?.grafanaUrl && (
            <a
              href={links.grafanaUrl}
              target="_blank"
              rel="noreferrer"
              className="text-[var(--primary)] hover:underline"
            >
              Grafana {links.grafanaUp ? "●" : "○"}
            </a>
          )}
        </div>
        <p className="mt-3 text-xs text-[var(--muted)]">
          기동: <code className="text-zinc-300">make obs-up</code> (Docker 필요) ·
          Grafana admin / nimbus · 포트 3001
        </p>
      </section>

      <section className="rounded-xl border border-[var(--border)] bg-[var(--card)]">
        <div className="border-b border-[var(--border)] px-5 py-3 text-sm font-medium">
          서비스 메트릭
        </div>
        {!overview?.topServices?.length ? (
          <p className="p-8 text-center text-sm text-[var(--muted)]">
            서비스가 없습니다.{" "}
            <Link href="/wizard" className="text-[var(--primary)] hover:underline">
              Wizard
            </Link>
            로 생성하세요.
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {overview.topServices.map((s) => (
              <li key={s.serviceId} className="px-5 py-4">
                <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                  <p className="font-medium">{s.serviceName}</p>
                  <span className="text-xs text-[var(--muted)]">source: {s.source}</span>
                </div>
                <div className="flex flex-wrap gap-3 text-xs text-[var(--muted)]">
                  {s.metrics.map((m) => (
                    <span
                      key={m.name}
                      className="rounded border border-[var(--border)] px-2 py-1"
                    >
                      {m.name}:{" "}
                      <strong className="text-zinc-200">
                        {m.value}
                        {m.unit === "%" ? "%" : m.unit === "count" ? "" : ` ${m.unit}`}
                      </strong>
                    </span>
                  ))}
                </div>
                <div className="mt-2 flex gap-3 text-xs">
                  <Link
                    href={`/logs?serviceId=${s.serviceId}`}
                    className="text-[var(--primary)] hover:underline"
                  >
                    로그
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
