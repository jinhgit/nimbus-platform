"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchCatalog, type CatalogTemplate } from "@/lib/api";
import {
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  StatusBadge,
} from "@/components/ui";

export default function CatalogPage() {
  const [items, setItems] = useState<CatalogTemplate[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load(query?: string) {
    setLoading(true);
    setError(null);
    const res = await fetchCatalog(query);
    setLoading(false);
    if (res.success && res.data) setItems(res.data);
    else {
      setError(res.error?.message ?? "카탈로그를 불러오지 못했습니다.");
      setItems([]);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <Page>
      <PageHeader
        eyebrow="Golden Path"
        title="Catalog"
        description="게시된 템플릿 목록입니다. 카드를 열어 Blueprint · Helm · Terraform · Actions 미리보기를 확인하세요."
        actions={
          <Link href="/wizard" className="nimbus-btn-primary">
            서비스 생성
          </Link>
        }
      />

      <div className="mb-6 flex gap-2">
        <input
          className="nimbus-input flex-1"
          placeholder="spring, nextjs, fastapi…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && load(q || undefined)}
        />
        <button
          type="button"
          onClick={() => load(q || undefined)}
          className="nimbus-btn-ghost"
        >
          검색
        </button>
      </div>

      {error ? <ErrorBanner message={error} onRetry={() => load(q || undefined)} /> : null}

      {loading ? (
        <LoadingBlock label="카탈로그를 불러오는 중…" className="min-h-[20vh]" />
      ) : items.length === 0 ? (
        <EmptyState
          title="템플릿이 없습니다"
          description="API 시드 템플릿 또는 검색어를 확인하세요."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {items.map((t) => (
            <Link
              key={t.id}
              href={`/catalog/${t.id}`}
              className="nimbus-card nimbus-card-interactive block p-5"
            >
              <div className="mb-2 flex items-start justify-between gap-2">
                <h2 className="font-medium text-zinc-100">{t.name}</h2>
                <div className="flex shrink-0 flex-col items-end gap-1">
                  {t.official ? (
                    <span className="rounded-full bg-[var(--primary)]/20 px-2 py-0.5 text-[10px] uppercase tracking-wide text-[var(--primary)]">
                      Official
                    </span>
                  ) : null}
                  <StatusBadge value={t.status} />
                </div>
              </div>
              <p className="mb-3 text-sm text-[var(--muted)]">
                {t.description ?? "설명 없음"}
              </p>
              <div className="flex flex-wrap gap-2 text-xs text-[var(--muted)]">
                <span className="rounded border border-[var(--border)] px-2 py-0.5">
                  {t.runtime}
                </span>
                <span className="rounded border border-[var(--border)] px-2 py-0.5">
                  {t.type}
                </span>
                <span className="rounded border border-[var(--border)] px-2 py-0.5">
                  {t.language}
                </span>
                <span className="rounded border border-[var(--border)] px-2 py-0.5">
                  v{t.latestVersion}
                </span>
              </div>
              <p className="mt-3 text-[11px] text-[var(--primary)]">상세 보기 →</p>
            </Link>
          ))}
        </div>
      )}
    </Page>
  );
}
