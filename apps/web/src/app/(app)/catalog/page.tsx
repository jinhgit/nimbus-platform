"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchCatalog, type CatalogTemplate } from "@/lib/api";

export default function CatalogPage() {
  const [items, setItems] = useState<CatalogTemplate[]>([]);
  const [q, setQ] = useState("");

  useEffect(() => {
    fetchCatalog().then((res) => {
      if (res.success && res.data) setItems(res.data);
    });
  }, []);

  async function onSearch() {
    const res = await fetchCatalog(q || undefined);
    if (res.success && res.data) setItems(res.data);
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Service Catalog</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            Golden Path 템플릿. Wizard에서 Publish된 템플릿만 선택합니다.
          </p>
        </div>
        <Link
          href="/wizard"
          className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)]"
        >
          Create Service
        </Link>
      </div>

      <div className="mb-6 flex gap-2">
        <input
          className="flex-1 rounded-lg border border-[var(--border)] bg-[var(--card)] px-3 py-2 text-sm outline-none focus:border-[var(--primary)]"
          placeholder="spring, nextjs, fastapi…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && onSearch()}
        />
        <button
          type="button"
          onClick={onSearch}
          className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5"
        >
          검색
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {items.map((t) => (
          <article
            key={t.id}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5"
          >
            <div className="mb-2 flex items-start justify-between gap-2">
              <h2 className="font-medium">{t.name}</h2>
              {t.official && (
                <span className="rounded-full bg-[var(--primary)]/20 px-2 py-0.5 text-[10px] uppercase tracking-wide text-[var(--primary)]">
                  Official
                </span>
              )}
            </div>
            <p className="mb-3 text-sm text-[var(--muted)]">{t.description}</p>
            <div className="flex flex-wrap gap-2 text-xs text-[var(--muted)]">
              <span className="rounded border border-[var(--border)] px-2 py-0.5">{t.runtime}</span>
              <span className="rounded border border-[var(--border)] px-2 py-0.5">{t.type}</span>
              <span className="rounded border border-[var(--border)] px-2 py-0.5">{t.language}</span>
              <span className="rounded border border-[var(--border)] px-2 py-0.5">v{t.latestVersion}</span>
            </div>
          </article>
        ))}
        {items.length === 0 && (
          <p className="text-sm text-[var(--muted)]">템플릿이 없습니다. API를 확인하세요.</p>
        )}
      </div>
    </div>
  );
}
