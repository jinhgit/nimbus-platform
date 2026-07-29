"use client";

import { useEffect, useState } from "react";
import {
  createPipeline,
  fetchGithubPipelineRuns,
  fetchMe,
  fetchPipelineLogs,
  fetchPipelines,
  fetchServices,
  rerunPipeline,
  type AppService,
  type GithubRunsResponse,
  type Pipeline,
  type PipelineLogs,
} from "@/lib/api";
import { ErrorBanner, Page, PageHeader, StatusBadge } from "@/components/ui";

export default function PipelinesPage() {
  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [services, setServices] = useState<AppService[]>([]);
  const [serviceId, setServiceId] = useState("");
  const [selected, setSelected] = useState<Pipeline | null>(null);
  const [logs, setLogs] = useState<PipelineLogs | null>(null);
  const [ghRuns, setGhRuns] = useState<GithubRunsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function refresh(ws?: string) {
    const res = await fetchPipelines({ workspaceId: ws });
    if (res.success && res.data) setPipelines(res.data);
  }

  async function loadGhRuns(sid: string) {
    if (!sid) {
      setGhRuns(null);
      return;
    }
    const res = await fetchGithubPipelineRuns(sid);
    if (res.success && res.data) setGhRuns(res.data);
    else setGhRuns(null);
  }

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      if (!ws) return;
      const s = await fetchServices({ workspaceId: ws });
      if (s.success && s.data) {
        setServices(s.data);
        if (s.data[0]) {
          setServiceId(s.data[0].id);
          await loadGhRuns(s.data[0].id);
        }
      }
      await refresh(ws);
    });
  }, []);

  useEffect(() => {
    if (serviceId) loadGhRuns(serviceId);
  }, [serviceId]);

  // poll running pipelines
  useEffect(() => {
    const id = setInterval(async () => {
      const me = await fetchMe();
      const ws = me.data?.workspace?.id;
      if (ws) await refresh(ws);
      if (selected) {
        const l = await fetchPipelineLogs(selected.id);
        if (l.success && l.data) setLogs(l.data);
      }
    }, 1500);
    return () => clearInterval(id);
  }, [selected]);

  async function onRun() {
    if (!serviceId) return;
    setLoading(true);
    setError(null);
    const res = await createPipeline(serviceId);
    setLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "파이프라인 시작 실패");
      return;
    }
    setSelected(res.data);
    const me = await fetchMe();
    await refresh(me.data?.workspace?.id);
  }

  async function onSelect(p: Pipeline) {
    setSelected(p);
    const l = await fetchPipelineLogs(p.id);
    if (l.success && l.data) setLogs(l.data);
  }

  async function onRerun(id: string) {
    setLoading(true);
    const res = await rerunPipeline(id);
    setLoading(false);
    if (res.success && res.data) {
      setSelected(res.data);
      const me = await fetchMe();
      await refresh(me.data?.workspace?.id);
    }
  }

  return (
    <Page>
      <PageHeader
        eyebrow="Operations"
        title="Pipelines"
        description="로컬 시뮬 빌드 + GitHub Actions run (SCM 연결 시 LIVE, 아니면 SIMULATED)"
      />

      <div className="mb-6 flex flex-wrap items-end gap-3 rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
        <label className="block min-w-[220px] flex-1 text-sm">
          <span className="mb-1 block text-[var(--muted)]">서비스</span>
          <select
            className="nimbus-input w-full"
            value={serviceId}
            onChange={(e) => setServiceId(e.target.value)}
          >
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          onClick={onRun}
          disabled={loading || !serviceId}
          className="nimbus-btn-primary disabled:opacity-60"
        >
          {loading ? "시작 중…" : "빌드 실행"}
        </button>
      </div>

      {error ? <ErrorBanner message={error} /> : null}

      {ghRuns ? (
        <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <div>
              <h2 className="text-sm font-medium">GitHub Actions</h2>
              <p className="text-[11px] text-[var(--muted)]">
                {ghRuns.repository ?? "repo 미바인딩"} · mode{" "}
                <span className="text-sky-300">{ghRuns.mode}</span>
                {ghRuns.message ? ` · ${ghRuns.message}` : ""}
              </p>
            </div>
            <StatusBadge value={ghRuns.mode} />
          </div>
          {ghRuns.runs.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">
              표시할 Actions run 이 없습니다. SCM 연결 및 repo 바인딩 후 확인하세요.
            </p>
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {ghRuns.runs.map((r) => (
                <li
                  key={r.id}
                  className="flex flex-wrap items-center justify-between gap-2 py-2.5"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-zinc-100">{r.name}</p>
                    <p className="text-[11px] text-[var(--muted)]">
                      {r.headBranch ?? "—"} · {r.event ?? "—"}
                      {r.updatedAt
                        ? ` · ${new Date(r.updatedAt).toLocaleString("ko-KR")}`
                        : ""}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge value={r.conclusion ?? r.status ?? "—"} />
                    {r.htmlUrl ? (
                      <a
                        href={r.htmlUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-[var(--primary)] hover:underline"
                      >
                        열기
                      </a>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)]">
          <div className="border-b border-[var(--border)] px-4 py-3 text-sm font-medium">
            실행 이력
          </div>
          {pipelines.length === 0 ? (
            <p className="p-6 text-sm text-[var(--muted)]">
              파이프라인이 없습니다. Wizard 완료 시 자동 생성되거나 여기서 실행하세요.
            </p>
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {pipelines.map((p) => (
                <li key={p.id}>
                  <button
                    type="button"
                    onClick={() => onSelect(p)}
                    className={`flex w-full items-start justify-between gap-3 px-4 py-3 text-left text-sm hover:bg-white/5 ${
                      selected?.id === p.id ? "bg-[var(--primary)]/10" : ""
                    }`}
                  >
                    <div>
                      <p className="font-medium">{p.serviceName}</p>
                      <p className="mt-1 text-xs text-[var(--muted)]">
                        {p.currentStep ?? p.name} · {p.progress ?? 0}%
                      </p>
                      {p.imageTag && (
                        <p className="mt-1 text-xs text-sky-400">{p.imageTag}</p>
                      )}
                    </div>
                    <span
                      className={`text-xs ${
                        p.status === "SUCCESS"
                          ? "text-emerald-400"
                          : p.status === "FAILED"
                            ? "text-red-400"
                            : "text-amber-400"
                      }`}
                    >
                      {p.status}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium">상세 / 로그</h2>
            {selected && (
              <button
                type="button"
                onClick={() => onRerun(selected.id)}
                className="text-xs text-[var(--primary)] hover:underline"
              >
                재실행
              </button>
            )}
          </div>
          {!selected ? (
            <p className="text-sm text-[var(--muted)]">파이프라인을 선택하세요.</p>
          ) : (
            <>
              <div className="mb-3 h-2 overflow-hidden rounded-full bg-zinc-800">
                <div
                  className="h-full bg-[var(--primary)] transition-all"
                  style={{ width: `${logs?.progress ?? selected.progress ?? 0}%` }}
                />
              </div>
              <p className="mb-2 text-xs text-[var(--muted)]">
                {logs?.currentStep ?? selected.currentStep} ·{" "}
                {logs?.status ?? selected.status}
              </p>
              <pre className="max-h-96 overflow-auto rounded-lg border border-[var(--border)] bg-black/40 p-3 font-mono text-[11px] leading-relaxed text-zinc-300">
                {logs?.logs || "로그 로딩 중…"}
              </pre>
            </>
          )}
        </section>
      </div>
    </Page>
  );
}
