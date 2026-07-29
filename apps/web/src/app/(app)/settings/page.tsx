"use client";

import { useEffect, useState, type FormEvent } from "react";
import {
  connectGitHub,
  disconnectGitHub,
  fetchGitHubHealth,
  fetchGitHubRepositories,
  fetchGitHubStatus,
  type GitHubHealth,
  type GitHubRepo,
} from "@/lib/api";

export default function SettingsPage() {
  const [connected, setConnected] = useState(false);
  const [login, setLogin] = useState<string | null>(null);
  const [token, setToken] = useState("");
  const [health, setHealth] = useState<GitHubHealth | null>(null);
  const [repos, setRepos] = useState<GitHubRepo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function refresh() {
    const status = await fetchGitHubStatus();
    if (status.success && status.data) {
      setConnected(status.data.connected);
    }
    if (status.data?.connected) {
      const h = await fetchGitHubHealth();
      if (h.success && h.data) {
        setHealth(h.data);
        setLogin(h.data.login ?? null);
      }
      const r = await fetchGitHubRepositories();
      if (r.success && r.data) setRepos(r.data);
    } else {
      setHealth(null);
      setLogin(null);
      setRepos([]);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function onConnect(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setMessage(null);
    const res = await connectGitHub(token.trim());
    setLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "GitHub 연결에 실패했습니다.");
      return;
    }
    setToken("");
    setMessage(`@${res.data.login} 계정에 연결되었습니다.`);
    await refresh();
  }

  async function onDisconnect() {
    setLoading(true);
    setError(null);
    await disconnectGitHub();
    setLoading(false);
    setMessage("GitHub 연결이 해제되었습니다.");
    await refresh();
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">설정</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          GitHub Adapter 연결 · free-only PAT (Personal Access Token)
        </p>
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}
      {message && (
        <p className="mb-4 rounded-lg border border-emerald-900/40 bg-emerald-950/30 px-3 py-2 text-sm text-emerald-300">
          {message}
        </p>
      )}

      <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-medium">GitHub 연결</h2>
            <p className="mt-1 text-sm text-[var(--muted)]">
              연결 후 Service Wizard Deploy 시 실제 Private Repository가 생성됩니다.
            </p>
          </div>
          <span
            className={`rounded-full px-3 py-1 text-xs ${
              connected
                ? "bg-emerald-500/15 text-emerald-400"
                : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            {connected ? `연결됨 · @${login ?? "…"}` : "미연결 (시뮬레이션)"}
          </span>
        </div>

        {health && (
          <ul className="mb-4 space-y-1 text-sm text-[var(--muted)]">
            <li>상태: {health.status}</li>
            <li>
              Rate Limit: {health.rateLimitRemaining ?? "—"} / {health.rateLimitLimit ?? "—"}
            </li>
          </ul>
        )}

        {!connected ? (
          <form onSubmit={onConnect} className="space-y-3">
            <label className="block text-sm">
              <span className="mb-1 block text-[var(--muted)]">
                Personal Access Token (classic: repo 권한)
              </span>
              <input
                type="password"
                className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2 text-sm outline-none focus:border-[var(--primary)]"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                placeholder="ghp_…"
                required
                autoComplete="off"
              />
            </label>
            <p className="text-xs text-[var(--muted)]">
              GitHub → Settings → Developer settings → Personal access tokens.
              토큰은 AES로 암호화 저장되며 응답에 평문이 노출되지 않습니다.
            </p>
            <button
              type="submit"
              disabled={loading || !token.trim()}
              className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
            >
              {loading ? "연결 중…" : "GitHub 연결"}
            </button>
          </form>
        ) : (
          <button
            type="button"
            onClick={onDisconnect}
            disabled={loading}
            className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5 disabled:opacity-60"
          >
            연결 해제
          </button>
        )}
      </section>

      <section className="mt-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
        <h2 className="mb-3 text-sm font-medium">Nimbus가 생성한 Repository</h2>
        {repos.length === 0 ? (
          <p className="text-sm text-[var(--muted)]">
            아직 생성된 저장소가 없습니다. GitHub 연결 후 Wizard에서 Deploy 하세요.
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {repos.map((r) => (
              <li key={r.id} className="flex items-center justify-between gap-3 py-3 text-sm">
                <div>
                  <p className="font-medium">
                    {r.owner}/{r.repoName}
                  </p>
                  <p className="text-xs text-[var(--muted)]">{r.status}</p>
                </div>
                {r.htmlUrl && (
                  <a
                    href={r.htmlUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-[var(--primary)] hover:underline"
                  >
                    GitHub에서 열기
                  </a>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
