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
  const [tagFilter, setTagFilter] = useState("");

  const load = useCallback(async (tag?: string) => {
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
    const res = await fetchServices({
      workspaceId: ws,
      tag: tag?.trim() || undefined,
    });
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
          onRetry={workspaceId ? () => load(tagFilter) : undefined}
        />
      ) : null}

      <div className="mb-4 flex gap-2">
        <input
          className="nimbus-input max-w-xs"
          placeholder="태그 필터 (예: payment)"
          value={tagFilter}
          onChange={(e) => setTagFilter(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load(tagFilter)}
        />
        <button
          type="button"
          onClick={() => load(tagFilter)}
          className="nimbus-btn-ghost"
        >
          필터
        </button>
        {tagFilter ? (
          <button
            type="button"
            onClick={() => {
              setTagFilter("");
              load("");
            }}
            className="text-xs text-[var(--muted)] hover:text-white"
          >
            초기화
          </button>
        ) : null}
      </div>

      <Card padding={false}>
        {services.length === 0 ? (
          <EmptyState
            title="서비스가 없습니다"
            description="Wizard로 생성하거나 태그 필터를 비워 보세요."
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
                    {s.tags && s.tags.length > 0 ? (
                      <div className="mt-1.5 flex flex-wrap gap-1">
                        {s.tags.map((t) => (
                          <span
                            key={t}
                            className="rounded border border-[var(--border)] px-1.5 py-0.5 text-[10px] text-sky-300/90"
                          >
                            {t}
                          </span>
                        ))}
                      </div>
                    ) : null}
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
