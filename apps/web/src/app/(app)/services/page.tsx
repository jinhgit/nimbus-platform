"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { fetchMe, fetchServices, type AppService } from "@/lib/api";
import { IconWizard } from "@/components/icons";
import {
  Card,
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  StatusBadge,
} from "@/components/ui";

export default function ServicesPage() {
  const [services, setServices] = useState<AppService[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [canMutate, setCanMutate] = useState(true);
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    const me = await fetchMe();
    setCanMutate(me.data?.canMutate !== false);
    const ws = me.data?.workspace?.id ?? null;
    setWorkspaceId(ws);
    if (!ws) {
      setError(me.error?.message ?? "워크스페이스가 없습니다.");
      setServices([]);
      setLoading(false);
      return;
    }
    const res = await fetchServices({ workspaceId: ws });
    if (res.success && res.data) {
      setServices(res.data);
    } else {
      setError(res.error?.message ?? "서비스 목록을 불러오지 못했습니다.");
      setServices([]);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <Page>
        <PageHeader eyebrow="Deploy" title="Services" />
        <LoadingBlock label="서비스를 불러오는 중…" />
      </Page>
    );
  }

  return (
    <Page>
      <PageHeader
        eyebrow="Deploy"
        title="Services"
        description="Wizard로 생성된 배포 단위입니다. 카드를 열어 환경·파이프라인·상세를 확인하세요."
        actions={
          canMutate ? (
            <Link href="/wizard" className="nimbus-btn-primary">
              <IconWizard size={15} />
              서비스 생성
            </Link>
          ) : null
        }
      />

      {error ? (
        <ErrorBanner
          message={error}
          onRetry={workspaceId ? load : undefined}
        />
      ) : null}

      <Card padding={false}>
        {services.length === 0 ? (
          <EmptyState
            title="서비스가 없습니다"
            description="Service Wizard로 GitHub · K8s · Environment까지 한 번에 프로비저닝할 수 있습니다."
            action={
              canMutate ? (
                <Link href="/wizard" className="nimbus-btn-primary">
                  Wizard 열기
                </Link>
              ) : undefined
            }
          />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {services.map((s) => (
              <li key={s.id}>
                <Link
                  href={`/services/${s.id}`}
                  className="flex items-start justify-between gap-4 px-5 py-4 transition hover:bg-white/[0.03]"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-zinc-100">{s.name}</p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      {s.runtime} · {s.environmentType}
                      {s.databaseType ? ` · ${s.databaseType}` : ""}
                      {s.cacheType ? ` · ${s.cacheType}` : ""}
                    </p>
                    {s.githubRepoUrl ? (
                      <p className="mt-1 truncate text-[11px] text-sky-400/90">
                        {s.githubOwner}/{s.githubRepoName}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-1.5">
                    <StatusBadge value={s.status} />
                    {s.k8sStatus ? <StatusBadge value={s.k8sStatus} /> : null}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </Page>
  );
}
