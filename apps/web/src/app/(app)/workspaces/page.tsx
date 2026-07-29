"use client";

import { useEffect, useState } from "react";
import { fetchWorkspaces, type WorkspaceSummary } from "@/lib/api";

export default function WorkspacesPage() {
  const [items, setItems] = useState<WorkspaceSummary[]>([]);

  useEffect(() => {
    fetchWorkspaces().then((res) => {
      if (res.success && res.data) setItems(res.data);
    });
  }, []);

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">Workspaces</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          로그인 시 개인 워크스페이스가 자동 생성됩니다.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((ws) => (
          <article
            key={ws.id}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5"
          >
            <h2 className="font-medium">{ws.name}</h2>
            <p className="mt-1 text-xs text-[var(--muted)]">/{ws.slug}</p>
            <p className="mt-4 text-xs uppercase tracking-wide text-[var(--primary)]">
              {ws.myRole}
            </p>
          </article>
        ))}
        {items.length === 0 && (
          <p className="text-sm text-[var(--muted)]">워크스페이스가 없습니다.</p>
        )}
      </div>
    </div>
  );
}
