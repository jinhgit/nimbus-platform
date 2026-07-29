"use client";

import { useCallback, useEffect, useState } from "react";
import {
  fetchAuditLogs,
  fetchMe,
  type AuditLogItem,
} from "@/lib/api";

const ACTION_OPTIONS = [
  "",
  "LOGIN",
  "LOGOUT",
  "SWITCH_WORKSPACE",
  "CREATE_WORKSPACE",
  "CREATE_PROJECT",
  "UPDATE_PROJECT",
  "DELETE_PROJECT",
  "ARCHIVE_PROJECT",
  "RESTORE_PROJECT",
  "CREATE_WIZARD",
  "EXECUTE_WIZARD",
  "CANCEL_WIZARD",
  "CONNECT_GITHUB",
  "DISCONNECT_GITHUB",
  "CREATE_PIPELINE",
  "RERUN_PIPELINE",
  "CREATE_ENVIRONMENT",
  "UPDATE_ENVIRONMENT",
  "DELETE_ENVIRONMENT",
  "ARCHIVE_ENVIRONMENT",
  "RESTORE_ENVIRONMENT",
  "PROMOTE_ENVIRONMENT",
  "CREATE_VARIABLE",
  "UPDATE_VARIABLE",
  "DELETE_VARIABLE",
  "CREATE_SECRET",
  "UPDATE_SECRET",
  "DELETE_SECRET",
  "REVEAL_SECRET",
];

function formatTime(iso?: string) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleString("ko-KR");
  } catch {
    return iso;
  }
}

function resultBadge(result: string) {
  const ok = result === "SUCCESS";
  return (
    <span
      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
        ok
          ? "bg-emerald-500/15 text-emerald-300"
          : "bg-rose-500/15 text-rose-300"
      }`}
    >
      {result}
    </span>
  );
}

export default function AuditPage() {
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [items, setItems] = useState<AuditLogItem[]>([]);
  const [action, setAction] = useState("");
  const [resourceType, setResourceType] = useState("");
  const [limit, setLimit] = useState(50);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!workspaceId) return;
    setLoading(true);
    setError(null);
    const res = await fetchAuditLogs({
      workspaceId,
      action: action || undefined,
      resourceType: resourceType || undefined,
      limit,
    });
    setLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "감사 로그 조회 실패");
      setItems([]);
      return;
    }
    setItems(res.data.items);
  }, [workspaceId, action, resourceType, limit]);

  useEffect(() => {
    fetchMe().then((me) => {
      const ws = me.data?.workspace?.id ?? null;
      setWorkspaceId(ws);
    });
  }, []);

  useEffect(() => {
    if (workspaceId) load();
  }, [workspaceId, load]);

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Audit</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          워크스페이스의 mutation·로그인 등 운영 이벤트를 기록합니다.
        </p>
      </div>

      <div className="mb-5 flex flex-wrap items-end gap-3 rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
        <label className="block text-sm">
          <span className="mb-1 block text-[var(--muted)]">액션</span>
          <select
            className="min-w-[180px] rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2"
            value={action}
            onChange={(e) => setAction(e.target.value)}
          >
            <option value="">전체</option>
            {ACTION_OPTIONS.filter(Boolean).map((a) => (
              <option key={a} value={a}>
                {a}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-[var(--muted)]">리소스 타입</span>
          <input
            className="w-40 rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2"
            placeholder="PROJECT, WIZARD…"
            value={resourceType}
            onChange={(e) => setResourceType(e.target.value)}
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-[var(--muted)]">개수</span>
          <select
            className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2"
            value={limit}
            onChange={(e) => setLimit(Number(e.target.value))}
          >
            {[25, 50, 100, 200].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          onClick={load}
          disabled={loading || !workspaceId}
          className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
        >
          {loading ? "조회 중…" : "새로고침"}
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-lg border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {error}
        </div>
      )}

      {!workspaceId && (
        <p className="text-sm text-[var(--muted)]">워크스페이스를 불러오는 중…</p>
      )}

      <div className="overflow-hidden rounded-xl border border-[var(--border)]">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-[var(--border)] bg-[var(--card)] text-xs uppercase tracking-wide text-[var(--muted)]">
            <tr>
              <th className="px-4 py-3 font-medium">시각</th>
              <th className="px-4 py-3 font-medium">액션</th>
              <th className="px-4 py-3 font-medium">행위자</th>
              <th className="px-4 py-3 font-medium">리소스</th>
              <th className="px-4 py-3 font-medium">결과</th>
              <th className="px-4 py-3 font-medium">메시지</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && !loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-[var(--muted)]">
                  감사 로그가 없습니다. 로그인·프로젝트 생성 후 다시 확인하세요.
                </td>
              </tr>
            ) : (
              items.map((row) => (
                <tr
                  key={row.id}
                  className="border-b border-[var(--border)]/60 last:border-0 hover:bg-white/[0.03]"
                >
                  <td className="whitespace-nowrap px-4 py-3 text-[var(--muted)]">
                    {formatTime(row.createdAt)}
                  </td>
                  <td className="px-4 py-3 font-mono text-xs">{row.action}</td>
                  <td className="px-4 py-3">
                    <div className="font-medium">{row.actorName ?? "—"}</div>
                    <div className="text-xs text-[var(--muted)]">
                      {row.actorEmail ?? ""}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="text-xs text-[var(--muted)]">
                      {row.resourceType ?? "—"}
                    </div>
                    <div className="truncate max-w-[160px]">
                      {row.resourceName ?? row.resourceId ?? "—"}
                    </div>
                  </td>
                  <td className="px-4 py-3">{resultBadge(row.result)}</td>
                  <td className="max-w-[220px] truncate px-4 py-3 text-[var(--muted)]">
                    {row.message ?? "—"}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-xs text-[var(--muted)]">
        표시 {items.length}건 · API <code className="text-[var(--foreground)]">GET /api/v1/audit</code>
      </p>
    </div>
  );
}
