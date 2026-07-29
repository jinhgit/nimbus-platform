"use client";

import { useEffect, useState, type FormEvent } from "react";
import {
  connectGitHub,
  disconnectGitHub,
  fetchGitHubHealth,
  fetchGitHubOauthConfig,
  fetchGitHubRepositories,
  fetchGitHubStatus,
  startGitHubOauth,
  type GitHubHealth,
  type GitHubRepo,
} from "@/lib/api";

export default function SettingsPage() {
  const [connected, setConnected] = useState(false);
  const [login, setLogin] = useState<string | null>(null);
  const [authMethod, setAuthMethod] = useState<string | null>(null);
  const [oauthConfigured, setOauthConfigured] = useState(false);
  const [token, setToken] = useState("");
  const [showPat, setShowPat] = useState(false);
  const [health, setHealth] = useState<GitHubHealth | null>(null);
  const [repos, setRepos] = useState<GitHubRepo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function refresh() {
    const [status, cfg] = await Promise.all([
      fetchGitHubStatus(),
      fetchGitHubOauthConfig(),
    ]);
    if (cfg.success && cfg.data) {
      setOauthConfigured(cfg.data.oauthConfigured);
    }
    if (status.success && status.data) {
      setConnected(status.data.connected);
      setAuthMethod(status.data.authMethod ?? null);
      setLogin(status.data.login ?? null);
    }
    if (status.data?.connected) {
      const h = await fetchGitHubHealth();
      if (h.success && h.data) {
        setHealth(h.data);
        setLogin(h.data.login ?? null);
        setAuthMethod(h.data.authMethod ?? null);
      }
      const r = await fetchGitHubRepositories();
      if (r.success && r.data) setRepos(r.data);
    } else {
      setHealth(null);
      setRepos([]);
    }
  }

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const scm = params.get("scm");
    if (scm === "connected") {
      setMessage("GitHub OAuth SCM 연결이 완료되었습니다.");
      window.history.replaceState({}, "", "/settings");
    } else if (scm === "error") {
      const reason = params.get("reason") ?? "unknown";
      setError(`OAuth 연결 실패: ${reason}`);
      window.history.replaceState({}, "", "/settings");
    }
    refresh();
  }, []);

  async function onOauthConnect() {
    setLoading(true);
    setError(null);
    setMessage(null);
    const res = await startGitHubOauth();
    setLoading(false);
    if (!res.success || !res.data?.authorizeUrl) {
      setError(
        res.error?.message ??
          "OAuth를 시작할 수 없습니다. GITHUB_CLIENT_ID/SECRET 설정을 확인하세요.",
      );
      return;
    }
    window.location.href = res.data.authorizeUrl;
  }

  async function onConnectPat(e: FormEvent) {
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
    setMessage(`@${res.data.login} 계정에 PAT 로 연결되었습니다.`);
    await refresh();
  }

  async function onDisconnect() {
    setLoading(true);
    setError(null);
    await disconnectGitHub();
    setLoading(false);
    setMessage("GitHub 연결이 해제되었습니다.");
    setAuthMethod(null);
    await refresh();
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          GitHub SCM 연결 — OAuth 정식 연결(권장) 또는 PAT 보조 연결
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
            <h2 className="text-lg font-medium">GitHub SCM</h2>
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
            {connected
              ? `연결됨 · @${login ?? "…"} · ${authMethod ?? "?"}`
              : "미연결 (시뮬레이션)"}
          </span>
        </div>

        {health && (
          <ul className="mb-4 space-y-1 text-sm text-[var(--muted)]">
            <li>상태: {health.status}</li>
            <li>방식: {health.authMethod ?? authMethod ?? "—"}</li>
            <li>
              Rate Limit: {health.rateLimitRemaining ?? "—"} / {health.rateLimitLimit ?? "—"}
            </li>
          </ul>
        )}

        {!connected ? (
          <div className="space-y-4">
            <div className="rounded-lg border border-[var(--border)] bg-black/20 p-4">
              <p className="mb-2 text-sm font-medium">1. OAuth 정식 연결 (권장)</p>
              <p className="mb-3 text-xs text-[var(--muted)]">
                GitHub OAuth App 으로 repo · workflow 권한을 받아 연결합니다.
                {oauthConfigured
                  ? " OAuth App 설정이 감지되었습니다."
                  : " 서버에 GITHUB_CLIENT_ID / SECRET 이 없습니다."}
              </p>
              <button
                type="button"
                onClick={onOauthConnect}
                disabled={loading || !oauthConfigured}
                className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
              >
                {loading ? "이동 중…" : "GitHub OAuth로 연결"}
              </button>
              {!oauthConfigured && (
                <p className="mt-2 text-xs text-amber-400/90">
                  `.env` 에 Client ID/Secret 설정 후 API 재시작이 필요합니다. 자세한 내용은
                  README · .env.example 참고.
                </p>
              )}
            </div>

            <div>
              <button
                type="button"
                onClick={() => setShowPat((v) => !v)}
                className="text-xs text-[var(--muted)] hover:text-white"
              >
                {showPat ? "▼" : "▶"} PAT 수동 연결 (보조)
              </button>
              {showPat && (
                <form onSubmit={onConnectPat} className="mt-3 space-y-3">
                  <label className="block text-sm">
                    <span className="mb-1 block text-[var(--muted)]">
                      Personal Access Token (classic: repo)
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
                    토큰은 AES로 암호화 저장되며 API 응답에 평문이 노출되지 않습니다.
                  </p>
                  <button
                    type="submit"
                    disabled={loading || !token.trim()}
                    className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5 disabled:opacity-60"
                  >
                    {loading ? "연결 중…" : "PAT로 연결"}
                  </button>
                </form>
              )}
            </div>
          </div>
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
            아직 생성된 저장소가 없습니다. SCM 연결 후 Wizard에서 Deploy 하세요.
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {repos.map((r) => (
              <li
                key={r.id}
                className="flex items-center justify-between gap-3 py-3 text-sm"
              >
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

      <section className="mt-6 rounded-xl border border-dashed border-[var(--border)] p-4 text-xs text-[var(--muted)]">
        <p className="mb-2 font-medium text-zinc-300">OAuth App 등록 요약</p>
        <ol className="list-decimal space-y-1 pl-4">
          <li>GitHub → Settings → Developer settings → OAuth Apps → New</li>
          <li>Homepage URL: http://localhost:3000</li>
          <li>
            Authorization callback URL:{" "}
            <code className="text-zinc-300">
              http://localhost:8080/api/v1/github/oauth/callback
            </code>
          </li>
          <li>
            Client ID/Secret 을 <code className="text-zinc-300">.env</code> 에 넣고 API 재시작
          </li>
        </ol>
      </section>
    </div>
  );
}
