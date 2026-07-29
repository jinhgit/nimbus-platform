"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

type PaletteItem = {
  id: string;
  label: string;
  hint?: string;
  href: string;
  keywords?: string;
};

const STATIC_ITEMS: PaletteItem[] = [
  { id: "dashboard", label: "Dashboard", href: "/dashboard", keywords: "home overview" },
  { id: "projects", label: "Projects", href: "/projects", keywords: "프로젝트" },
  { id: "services", label: "Services", href: "/services", keywords: "서비스" },
  { id: "catalog", label: "Catalog", href: "/catalog", keywords: "template blueprint" },
  { id: "wizard", label: "Create Service", href: "/wizard", keywords: "wizard 생성" },
  { id: "pipelines", label: "Pipelines", href: "/pipelines", keywords: "ci build actions" },
  { id: "incidents", label: "Incidents", href: "/incidents", keywords: "이슈 failure saga notification" },
  { id: "monitoring", label: "Monitoring", href: "/monitoring", keywords: "metrics" },
  { id: "logs", label: "Logs", href: "/logs", keywords: "로그" },
  { id: "audit", label: "Audit", href: "/audit", keywords: "감사" },
  { id: "infra", label: "Infrastructure", href: "/infrastructure", keywords: "k8s" },
  { id: "settings", label: "Settings", href: "/settings", keywords: "members github" },
  { id: "workspaces", label: "Workspaces", href: "/workspaces", keywords: "workspace" },
];

export function CommandPalette() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState("");
  const [active, setActive] = useState(0);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      const isPalette =
        (e.key === "k" || e.key === "K") && (e.metaKey || e.ctrlKey);
      if (isPalette) {
        e.preventDefault();
        setOpen((v) => !v);
        setQ("");
        setActive(0);
      }
      if (e.key === "Escape") setOpen(false);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return STATIC_ITEMS;
    return STATIC_ITEMS.filter((item) => {
      const hay = `${item.label} ${item.hint ?? ""} ${item.keywords ?? ""} ${item.href}`.toLowerCase();
      return hay.includes(needle);
    });
  }, [q]);

  useEffect(() => {
    setActive(0);
  }, [q]);

  function go(item: PaletteItem) {
    setOpen(false);
    router.push(item.href);
  }

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[80] flex items-start justify-center bg-black/60 px-4 pt-[12vh] backdrop-blur-sm"
      role="dialog"
      aria-modal
      aria-label="Command palette"
      onClick={() => setOpen(false)}
    >
      <div
        className="w-full max-w-lg overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--card)] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="border-b border-[var(--border)] px-3 py-2">
          <input
            autoFocus
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "ArrowDown") {
                e.preventDefault();
                setActive((i) => Math.min(i + 1, Math.max(filtered.length - 1, 0)));
              } else if (e.key === "ArrowUp") {
                e.preventDefault();
                setActive((i) => Math.max(i - 1, 0));
              } else if (e.key === "Enter" && filtered[active]) {
                e.preventDefault();
                go(filtered[active]);
              }
            }}
            placeholder="페이지 · 작업 검색… (Esc 닫기)"
            className="w-full bg-transparent px-2 py-2 text-sm text-zinc-100 outline-none placeholder:text-[var(--muted)]"
          />
        </div>
        <ul className="max-h-80 overflow-auto py-1">
          {filtered.length === 0 ? (
            <li className="px-4 py-6 text-center text-sm text-[var(--muted)]">
              결과가 없습니다
            </li>
          ) : (
            filtered.map((item, idx) => (
              <li key={item.id}>
                <button
                  type="button"
                  onClick={() => go(item)}
                  onMouseEnter={() => setActive(idx)}
                  className={`flex w-full items-center justify-between gap-3 px-4 py-2.5 text-left text-sm ${
                    idx === active
                      ? "bg-[var(--primary)]/15 text-white"
                      : "text-zinc-200 hover:bg-white/[0.04]"
                  }`}
                >
                  <span className="font-medium">{item.label}</span>
                  <span className="text-[11px] text-[var(--muted)]">{item.href}</span>
                </button>
              </li>
            ))
          )}
        </ul>
        <div className="border-t border-[var(--border)] px-4 py-2 text-[11px] text-[var(--muted)]">
          <kbd className="rounded border border-[var(--border)] px-1">⌘</kbd>
          <kbd className="ml-0.5 rounded border border-[var(--border)] px-1">K</kbd>
          {" / "}
          <kbd className="rounded border border-[var(--border)] px-1">Ctrl</kbd>
          <kbd className="ml-0.5 rounded border border-[var(--border)] px-1">K</kbd>
          {" 열기 · ↑↓ 이동 · Enter 이동"}
        </div>
      </div>
    </div>
  );
}
