"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchMe, fetchServices, type AppService } from "@/lib/api";

export default function ServicesPage() {
  const [services, setServices] = useState<AppService[]>([]);

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      if (!ws) return;
      const res = await fetchServices({ workspaceId: ws });
      if (res.success && res.data) setServices(res.data);
    });
  }, []);

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">서비스</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            Wizard로 생성된 배포 단위 서비스 목록입니다. 카드를 눌러 상세를 확인하세요.
          </p>
        </div>
        <Link
          href="/wizard"
          className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)]"
        >
          서비스 생성
        </Link>
      </div>

      <div className="rounded-xl border border-[var(--border)] bg-[var(--card)]">
        {services.length === 0 ? (
          <p className="p-8 text-center text-sm text-[var(--muted)]">
            서비스가 없습니다.{" "}
            <Link href="/wizard" className="text-[var(--primary)] hover:underline">
              Service Wizard
            </Link>
            로 생성하세요.
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {services.map((s) => (
              <li key={s.id}>
                <Link
                  href={`/services/${s.id}`}
                  className="flex items-start justify-between gap-4 px-5 py-4 transition hover:bg-white/[0.03]"
                >
                  <div>
                    <p className="font-medium">{s.name}</p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      {s.runtime} · {s.environmentType}
                      {s.databaseType ? ` · ${s.databaseType}` : ""}
                      {s.cacheType ? ` · ${s.cacheType}` : ""}
                    </p>
                    {s.githubRepoUrl && (
                      <p className="mt-1 text-xs text-[var(--primary)]">
                        {s.githubOwner}/{s.githubRepoName}
                      </p>
                    )}
                    {s.k8sNamespace && (
                      <p className="mt-1 text-xs text-[var(--muted)]">
                        K8s: {s.k8sNamespace}/{s.k8sDeployment} · {s.k8sStatus}
                        {s.k8sClusterType ? ` (${s.k8sClusterType})` : ""}
                      </p>
                    )}
                  </div>
                  <div className="text-right text-xs text-[var(--muted)]">
                    <p className="font-medium text-emerald-400">{s.status}</p>
                    <p className="mt-1">레플리카 {s.replicaCount ?? 1}</p>
                    {s.hpaEnabled && <p>HPA 활성</p>}
                    <p className="mt-2 text-[var(--primary)]">상세 →</p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
