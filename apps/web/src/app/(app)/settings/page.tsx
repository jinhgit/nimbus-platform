"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  connectGitHub,
  disconnectGitHub,
  fetchAiStatus,
  fetchGitHubHealth,
  fetchGitHubOauthConfig,
  fetchGitHubRepositories,
  fetchGitHubStatus,
  fetchMe,
  fetchWorkspaceMembers,
  inviteWorkspaceMember,
  removeWorkspaceMember,
  startGitHubOauth,
  updateWorkspaceMemberRole,
  type AiStatus,
  type GitHubHealth,
  type GitHubRepo,
  type WorkspaceMember,
} from "@/lib/api";
import {
  Card,
  EmptyState,
  ErrorBanner,
  LoadingInline,
  Page,
  PageHeader,
  SuccessBanner,
} from "@/components/ui";

const ROLE_OPTIONS = [
  "VIEWER",
  "DEVELOPER",
  "PLATFORM_ENGINEER",
  "ADMIN",
] as const;

export default function SettingsPage() {
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [myUserId, setMyUserId] = useState<string | null>(null);
  const [myRole, setMyRole] = useState<string | null>(null);
  const [canManageMembers, setCanManageMembers] = useState(false);
  const [canInvite, setCanInvite] = useState(false);

  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<string>("VIEWER");
  const [memberBusy, setMemberBusy] = useState(false);

  const [connected, setConnected] = useState(false);
  const [login, setLogin] = useState<string | null>(null);
  const [authMethod, setAuthMethod] = useState<string | null>(null);
  const [oauthConfigured, setOauthConfigured] = useState(false);
  const [token, setToken] = useState("");
  const [showPat, setShowPat] = useState(false);
  const [health, setHealth] = useState<GitHubHealth | null>(null);
  const [repos, setRepos] = useState<GitHubRepo[]>([]);
  const [aiStatus, setAiStatus] = useState<AiStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const loadMembers = useCallback(async (ws: string) => {
    setMembersLoading(true);
    const res = await fetchWorkspaceMembers(ws);
    setMembersLoading(false);
    if (res.success && res.data) {
      setMembers(res.data);
    } else {
      setError(res.error?.message ?? "멤버 목록을 불러오지 못했습니다.");
    }
  }, []);

  async function refreshGithub() {
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

    fetchMe().then((me) => {
      if (me.success && me.data) {
        setMyUserId(me.data.id);
        const role = me.data.workspaceRole ?? null;
        setMyRole(role);
        setCanManageMembers(role === "OWNER" || role === "ADMIN");
        setCanInvite(
          role === "OWNER" ||
            role === "ADMIN" ||
            role === "PLATFORM_ENGINEER",
        );
        const ws = me.data.workspace?.id ?? null;
        setWorkspaceId(ws);
        if (ws) loadMembers(ws);
      }
    });
    refreshGithub();
    fetchAiStatus().then((res) => {
      if (res.success && res.data) setAiStatus(res.data);
    });
  }, [loadMembers]);

  async function onInvite(e: FormEvent) {
    e.preventDefault();
    if (!workspaceId || !canInvite) return;
    setMemberBusy(true);
    setError(null);
    setMessage(null);
    const res = await inviteWorkspaceMember(workspaceId, {
      email: inviteEmail.trim(),
      role: inviteRole,
    });
    setMemberBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "초대에 실패했습니다.");
      return;
    }
    setInviteEmail("");
    setMessage(`${res.data.email} 을(를) ${res.data.role} 로 초대했습니다.`);
    await loadMembers(workspaceId);
  }

  async function onRoleChange(member: WorkspaceMember, role: string) {
    if (!workspaceId || !canManageMembers) return;
    if (member.role === "OWNER") return;
    setMemberBusy(true);
    setError(null);
    const res = await updateWorkspaceMemberRole(workspaceId, member.id, role);
    setMemberBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "역할 변경에 실패했습니다.");
      return;
    }
    setMessage(`${member.email} → ${role}`);
    await loadMembers(workspaceId);
  }

  async function onRemove(member: WorkspaceMember) {
    if (!workspaceId || !canManageMembers) return;
    if (member.role === "OWNER" || member.userId === myUserId) return;
    if (!window.confirm(`${member.email} 멤버를 제거할까요?`)) return;
    setMemberBusy(true);
    setError(null);
    const res = await removeWorkspaceMember(workspaceId, member.id);
    setMemberBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "멤버 제거에 실패했습니다.");
      return;
    }
    setMessage(`${member.email} 을(를) 제거했습니다.`);
    await loadMembers(workspaceId);
  }

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
    await refreshGithub();
  }

  async function onDisconnect() {
    setLoading(true);
    setError(null);
    await disconnectGitHub();
    setLoading(false);
    setMessage("GitHub 연결이 해제되었습니다.");
    setAuthMethod(null);
    await refreshGithub();
  }

  return (
    <Page>
      <PageHeader
        eyebrow="Workspace"
        title="Settings"
        description={
          <>
            멤버 역할(VIEWER 포함) · GitHub SCM 연결
            {myRole ? ` · 내 역할 ${myRole}` : ""}
          </>
        }
      />

      {error ? <ErrorBanner message={error} /> : null}
      {message ? <SuccessBanner message={message} /> : null}

      <Card className="mb-6">
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-sm font-medium text-zinc-100" data-testid="members-heading">
              Members
            </h2>
            <p className="mt-0.5 text-xs text-[var(--muted)]">
              VIEWER는 Promote · Secret · Retry 등 변경 작업이 차단됩니다. 역할을
              VIEWER로 바꿔 RBAC를 바로 확인할 수 있습니다.
            </p>
          </div>
        </div>

        {canInvite ? (
          <form
            onSubmit={onInvite}
            className="mb-5 flex flex-wrap items-end gap-2 border-b border-[var(--border)] pb-5"
          >
            <label className="min-w-[200px] flex-1 text-sm">
              <span className="mb-1 block text-xs text-[var(--muted)]">
                이메일
              </span>
              <input
                type="email"
                className="nimbus-input"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                placeholder="viewer@nimbus.local"
                required
              />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs text-[var(--muted)]">역할</span>
              <select
                className="nimbus-input"
                value={inviteRole}
                onChange={(e) => setInviteRole(e.target.value)}
              >
                {ROLE_OPTIONS.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="submit"
              disabled={memberBusy || !inviteEmail.trim()}
              className="nimbus-btn-primary"
            >
              {memberBusy ? "처리 중…" : "초대"}
            </button>
          </form>
        ) : (
          <p className="mb-4 text-xs text-[var(--muted)]">
            멤버 초대는 OWNER / ADMIN / PLATFORM_ENGINEER 만 가능합니다.
          </p>
        )}

        {membersLoading ? (
          <LoadingInline label="멤버를 불러오는 중…" />
        ) : members.length === 0 ? (
          <EmptyState
            title="멤버가 없습니다"
            description="워크스페이스에 멤버를 초대해 보세요."
          />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {members.map((m) => {
              const isSelf = m.userId === myUserId;
              const isOwner = m.role === "OWNER";
              return (
                <li
                  key={m.id}
                  className="flex flex-wrap items-center justify-between gap-3 py-3"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-zinc-100">
                      {m.name}
                      {isSelf ? (
                        <span className="ml-2 text-[11px] text-[var(--muted)]">
                          (나)
                        </span>
                      ) : null}
                    </p>
                    <p className="text-xs text-[var(--muted)]">{m.email}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    {canManageMembers && !isOwner ? (
                      <select
                        className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 text-xs"
                        value={m.role}
                        disabled={memberBusy}
                        onChange={(e) => onRoleChange(m, e.target.value)}
                      >
                        {ROLE_OPTIONS.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <span className="rounded-full border border-[var(--border)] px-2.5 py-0.5 text-[11px] text-zinc-300">
                        {m.role}
                      </span>
                    )}
                    {canManageMembers && !isOwner && !isSelf ? (
                      <button
                        type="button"
                        disabled={memberBusy}
                        onClick={() => onRemove(m)}
                        className="text-xs text-[var(--muted)] hover:text-rose-300 disabled:opacity-50"
                      >
                        제거
                      </button>
                    ) : null}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <Card>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-medium text-zinc-100">GitHub SCM</h2>
            <p className="mt-1 text-xs text-[var(--muted)]">
              연결 후 Service Wizard Deploy 시 실제 Private Repository가
              생성됩니다. Promote GitOps PR도 토큰이 있을 때 시도합니다.
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
              Rate Limit: {health.rateLimitRemaining ?? "—"} /{" "}
              {health.rateLimitLimit ?? "—"}
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
                className="nimbus-btn-primary disabled:opacity-60"
              >
                {loading ? "이동 중…" : "GitHub OAuth로 연결"}
              </button>
              {!oauthConfigured && (
                <p className="mt-2 text-xs text-amber-400/90">
                  `.env` 에 Client ID/Secret 설정 후 API 재시작이 필요합니다.
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
                      className="nimbus-input"
                      value={token}
                      onChange={(e) => setToken(e.target.value)}
                      placeholder="ghp_…"
                      required
                      autoComplete="off"
                    />
                  </label>
                  <button
                    type="submit"
                    disabled={loading || !token.trim()}
                    className="nimbus-btn-ghost disabled:opacity-60"
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
            className="nimbus-btn-ghost disabled:opacity-60"
          >
            연결 해제
          </button>
        )}
      </Card>

      <Card className="mt-6">
        <h2 className="mb-2 text-sm font-medium">AI Provider</h2>
        <p className="mb-3 text-xs text-[var(--muted)]">
          기본은 rule-engine. <code className="text-zinc-300">AI_PROVIDER=ollama</code> 와
          Ollama 로컬 기동 시 LLM 보조 설명을 붙입니다 (실패 시 자동 fallback).
        </p>
        {aiStatus ? (
          <ul className="space-y-1 text-sm text-[var(--muted)]">
            <li>
              설정:{" "}
              <span className="text-zinc-200">{aiStatus.configuredProvider}</span>
            </li>
            <li>
              활성:{" "}
              <span className="text-zinc-200">{aiStatus.activeProvider}</span>
            </li>
            <li>
              Ollama: {aiStatus.ollamaBaseUrl} · {aiStatus.ollamaModel} ·{" "}
              {aiStatus.ollamaReachable ? "연결됨" : "오프라인/미사용"}
            </li>
            <li className="text-xs">{aiStatus.message}</li>
          </ul>
        ) : (
          <p className="text-sm text-[var(--muted)]">AI 상태 로딩 중…</p>
        )}
      </Card>

      <Card className="mt-6">
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
      </Card>
    </Page>
  );
}
