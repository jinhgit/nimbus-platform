"use client";

import { useCallback, useEffect, useState } from "react";
import {
  acknowledgeIncident,
  fetchIncidentCounts,
  fetchIncidents,
  fetchMe,
  resolveIncident,
  scanIncidents,
  type Incident,
  type IncidentCounts,
} from "@/lib/api";
import {
  Card,
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  StatusBadge,
  SuccessBanner,
} from "@/components/ui";

export default function IncidentsPage() {
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [canMutate, setCanMutate] = useState(true);
  const [items, setItems] = useState<Incident[]>([]);
  const [counts, setCounts] = useState<IncidentCounts | null>(null);
  const [status, setStatus] = useState("");
  const [selected, setSelected] = useState<Incident | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async (ws: string, st?: string) => {
    setLoading(true);
    setError(null);
    const [list, c] = await Promise.all([
      fetchIncidents({ workspaceId: ws, status: st || undefined }),
      fetchIncidentCounts(ws),
    ]);
    setLoading(false);
    if (list.success && list.data) setItems(list.data);
    else {
      setError(list.error?.message ?? "인시던트 목록 조회 실패");
      setItems([]);
    }
    if (c.success && c.data) setCounts(c.data);
  }, []);

  useEffect(() => {
    fetchMe().then((me) => {
      setCanMutate(me.data?.canMutate !== false);
      const ws = me.data?.workspace?.id ?? null;
      setWorkspaceId(ws);
      if (ws) load(ws, status);
      else setLoading(false);
    });
  }, [load, status]);

  async function onScan() {
    if (!workspaceId || !canMutate) return;
    setBusy(true);
    setError(null);
    setMessage(null);
    const res = await scanIncidents(workspaceId);
    setBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "스캔 실패");
      return;
    }
    setMessage(
      `스캔 완료 · 신규 ${res.data.opened}건 (검사 ${res.data.scanned})`,
    );
    await load(workspaceId, status);
  }

  async function onAck(id: string) {
    setBusy(true);
    const res = await acknowledgeIncident(id);
    setBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "ACK 실패");
      return;
    }
    if (workspaceId) await load(workspaceId, status);
    if (res.data) setSelected(res.data);
  }

  async function onResolve(id: string) {
    setBusy(true);
    const res = await resolveIncident(id);
    setBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "해결 처리 실패");
      return;
    }
    if (workspaceId) await load(workspaceId, status);
    if (res.data) setSelected(res.data);
  }

  if (loading && items.length === 0) {
    return (
      <Page>
        <PageHeader eyebrow="Operations" title="Incidents" />
        <LoadingBlock label="인시던트를 불러오는 중…" />
      </Page>
    );
  }

  return (
    <Page>
      <PageHeader
        eyebrow="Operations"
        title="Incidents"
        description="FAILED Saga · Pipeline · Unhealthy Environment 를 스캔해 운영 이슈로 모읍니다. 분석은 rule-engine 기본."
        actions={
          canMutate ? (
            <button
              type="button"
              disabled={busy || !workspaceId}
              onClick={onScan}
              className="nimbus-btn-primary"
            >
              {busy ? "스캔 중…" : "이슈 스캔"}
            </button>
          ) : null
        }
      />

      {error ? <ErrorBanner message={error} /> : null}
      {message ? <SuccessBanner message={message} /> : null}

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <Card className="!p-4">
          <p className="text-[11px] uppercase tracking-wide text-[var(--muted)]">
            Open
          </p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">
            {counts?.open ?? 0}
          </p>
        </Card>
        <Card className="!p-4">
          <p className="text-[11px] uppercase tracking-wide text-[var(--muted)]">
            Acknowledged
          </p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">
            {counts?.acknowledged ?? 0}
          </p>
        </Card>
        <Card className="!p-4">
          <p className="text-[11px] uppercase tracking-wide text-[var(--muted)]">
            Resolved
          </p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">
            {counts?.resolved ?? 0}
          </p>
        </Card>
      </div>

      <div className="mb-4">
        <select
          className="nimbus-input max-w-xs"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">전체 상태</option>
          <option value="OPEN">OPEN</option>
          <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
          <option value="RESOLVED">RESOLVED</option>
        </select>
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        <Card padding={false}>
          {items.length === 0 ? (
            <EmptyState
              title="인시던트가 없습니다"
              description="「이슈 스캔」으로 FAILED Saga / Pipeline / Unhealthy Env 를 수집하세요."
            />
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {items.map((i) => (
                <li key={i.id}>
                  <button
                    type="button"
                    onClick={() => setSelected(i)}
                    className={`flex w-full items-start justify-between gap-3 px-5 py-3.5 text-left hover:bg-white/[0.03] ${
                      selected?.id === i.id ? "bg-[var(--primary)]/10" : ""
                    }`}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-zinc-100">
                        {i.title}
                      </p>
                      <p className="mt-0.5 text-[11px] text-[var(--muted)]">
                        {i.sourceType}
                        {i.serviceName ? ` · ${i.serviceName}` : ""}
                        {i.openedAt
                          ? ` · ${new Date(i.openedAt).toLocaleString("ko-KR")}`
                          : ""}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1">
                      <StatusBadge value={i.severity} />
                      <StatusBadge value={i.status} />
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card>
          {!selected ? (
            <p className="text-sm text-[var(--muted)]">
              인시던트를 선택하면 분석 결과와 조치를 볼 수 있습니다.
            </p>
          ) : (
            <div className="space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-sm font-medium text-zinc-100">
                  {selected.title}
                </h2>
                <StatusBadge value={selected.severity} />
                <StatusBadge value={selected.status} />
              </div>
              {selected.summary ? (
                <p className="text-sm text-[var(--muted)]">{selected.summary}</p>
              ) : null}
              <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-lg border border-[var(--border)] bg-black/30 p-3 text-xs leading-relaxed text-zinc-300">
                {selected.analysisText ?? "분석 없음"}
              </pre>
              <p className="text-[11px] text-[var(--muted)]">
                provider: {selected.provider ?? "rule-engine"}
              </p>
              {canMutate && selected.status !== "RESOLVED" ? (
                <div className="flex flex-wrap gap-2 pt-1">
                  {selected.status === "OPEN" ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => onAck(selected.id)}
                      className="nimbus-btn-ghost"
                    >
                      ACK
                    </button>
                  ) : null}
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => onResolve(selected.id)}
                    className="nimbus-btn-primary"
                  >
                    해결됨
                  </button>
                </div>
              ) : null}
            </div>
          )}
        </Card>
      </div>
    </Page>
  );
}
