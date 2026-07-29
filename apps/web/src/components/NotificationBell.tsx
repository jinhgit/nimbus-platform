"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  fetchMe,
  fetchNotificationUnreadCount,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  syncNotifications,
  type AppNotification,
} from "@/lib/api";

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [items, setItems] = useState<AppNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async (ws: string) => {
    const [list, count] = await Promise.all([
      fetchNotifications(ws, 20),
      fetchNotificationUnreadCount(ws),
    ]);
    if (list.success && list.data) setItems(list.data);
    if (count.success && count.data) setUnread(count.data.unread);
  }, []);

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id ?? null;
      setWorkspaceId(ws);
      if (ws) await refresh(ws);
    });
    const id = setInterval(() => {
      if (workspaceId) refresh(workspaceId);
    }, 20000);
    return () => clearInterval(id);
  }, [refresh, workspaceId]);

  async function onOpen() {
    setOpen((v) => !v);
    if (!workspaceId) return;
    setLoading(true);
    await syncNotifications(workspaceId);
    await refresh(workspaceId);
    setLoading(false);
  }

  async function onRead(n: AppNotification) {
    if (!n.unread) return;
    await markNotificationRead(n.id);
    if (workspaceId) await refresh(workspaceId);
  }

  async function onReadAll() {
    if (!workspaceId) return;
    await markAllNotificationsRead(workspaceId);
    await refresh(workspaceId);
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={onOpen}
        className="relative flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border)] text-[var(--muted)] transition hover:bg-white/[0.04] hover:text-white"
        aria-label="Notifications"
        title="Notifications"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>
        {unread > 0 ? (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-medium text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        ) : null}
      </button>

      {open ? (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 cursor-default"
            aria-label="Close notifications"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 z-50 mt-2 w-80 overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--card)] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--border)] px-3 py-2">
              <p className="text-xs font-medium text-zinc-200">Notifications</p>
              <button
                type="button"
                onClick={onReadAll}
                className="text-[11px] text-[var(--primary)] hover:underline"
              >
                모두 읽음
              </button>
            </div>
            <div className="max-h-80 overflow-auto">
              {loading && items.length === 0 ? (
                <p className="px-3 py-6 text-center text-xs text-[var(--muted)]">
                  동기화 중…
                </p>
              ) : items.length === 0 ? (
                <p className="px-3 py-6 text-center text-xs text-[var(--muted)]">
                  알림이 없습니다. 벨을 열면 Incident/Saga/Pipeline을 스캔합니다.
                </p>
              ) : (
                <ul className="divide-y divide-[var(--border)]">
                  {items.map((n) => (
                    <li key={n.id}>
                      <Link
                        href={n.href || "/dashboard"}
                        onClick={() => {
                          onRead(n);
                          setOpen(false);
                        }}
                        className={`block px-3 py-2.5 text-left hover:bg-white/[0.04] ${
                          n.unread ? "bg-[var(--primary)]/5" : ""
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <p className="text-xs font-medium text-zinc-100">
                            {n.title}
                          </p>
                          {n.unread ? (
                            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-sky-400" />
                          ) : null}
                        </div>
                        {n.body ? (
                          <p className="mt-0.5 line-clamp-2 text-[11px] text-[var(--muted)]">
                            {n.body}
                          </p>
                        ) : null}
                        <p className="mt-1 text-[10px] uppercase tracking-wide text-[var(--muted-soft)]">
                          {n.type}
                          {n.createdAt
                            ? ` · ${new Date(n.createdAt).toLocaleString("ko-KR")}`
                            : ""}
                        </p>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
