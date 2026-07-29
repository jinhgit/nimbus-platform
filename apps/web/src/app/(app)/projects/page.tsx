"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  archiveProject,
  cloneProject,
  createProject,
  fetchMe,
  fetchProjects,
  restoreProject,
  type Project,
} from "@/lib/api";
import {
  Card,
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  ReadOnlyBanner,
  StatusBadge,
} from "@/components/ui";

export default function ProjectsPage() {
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [canMutate, setCanMutate] = useState(true);
  const [role, setRole] = useState<string | null>(null);

  const load = useCallback(async (ws: string) => {
    const res = await fetchProjects(ws);
    if (res.success && res.data) {
      setProjects(res.data);
      setError(null);
    } else {
      setError(res.error?.message ?? "프로젝트 목록을 불러오지 못했습니다.");
      setProjects([]);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    fetchMe().then(async (res) => {
      const ws = res.data?.workspace?.id;
      setCanMutate(res.data?.canMutate !== false);
      setRole(res.data?.workspaceRole ?? null);
      if (!ws) {
        setError(res.error?.message ?? "워크스페이스가 없습니다.");
        setLoading(false);
        return;
      }
      setWorkspaceId(ws);
      await load(ws);
      setLoading(false);
    });
  }, [load]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!workspaceId || !canMutate) return;
    setSaving(true);
    setError(null);
    const res = await createProject({
      name,
      description: description || undefined,
      workspaceId,
    });
    setSaving(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "프로젝트 생성에 실패했습니다.");
      return;
    }
    setName("");
    setDescription("");
    await load(workspaceId);
  }

  async function onArchive(p: Project) {
    if (!workspaceId || !canMutate) return;
    setBusyId(p.id);
    setError(null);
    const res = await archiveProject(p.id);
    setBusyId(null);
    if (!res.success) {
      setError(res.error?.message ?? "보관 실패");
      return;
    }
    await load(workspaceId);
  }

  async function onRestore(p: Project) {
    if (!workspaceId || !canMutate) return;
    setBusyId(p.id);
    setError(null);
    const res = await restoreProject(p.id);
    setBusyId(null);
    if (!res.success) {
      setError(res.error?.message ?? "복원 실패");
      return;
    }
    await load(workspaceId);
  }

  async function onClone(p: Project) {
    if (!workspaceId || !canMutate) return;
    setBusyId(p.id);
    setError(null);
    const res = await cloneProject(p.id, {
      name: `${p.name} Copy`,
      description: p.description,
    });
    setBusyId(null);
    if (!res.success) {
      setError(res.error?.message ?? "복제 실패");
      return;
    }
    await load(workspaceId);
  }

  if (loading) {
    return (
      <Page>
        <PageHeader eyebrow="Workspace" title="Projects" />
        <LoadingBlock label="프로젝트를 불러오는 중…" />
      </Page>
    );
  }

  return (
    <Page>
      <PageHeader
        eyebrow="Workspace"
        title="Projects"
        description={
          <>
            비즈니스 컨텍스트 단위입니다. 서비스는{" "}
            <Link href="/wizard" className="text-[var(--primary)] hover:underline">
              Service Wizard
            </Link>
            에서 생성합니다. 보관·복제·복원을 지원합니다.
          </>
        }
      />

      {error ? (
        <ErrorBanner
          message={error}
          onRetry={() => workspaceId && load(workspaceId)}
        />
      ) : null}
      {!canMutate ? <ReadOnlyBanner role={role} /> : null}

      <div className="grid gap-5 lg:grid-cols-[300px_1fr]">
        {canMutate ? (
          <Card className="h-fit">
            <h2 className="mb-4 text-sm font-medium">새 프로젝트</h2>
            <form onSubmit={onCreate} className="space-y-3">
              <label className="block text-sm">
                <span className="mb-1 block text-xs text-[var(--muted)]">이름</span>
                <input
                  className="nimbus-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  minLength={3}
                  maxLength={50}
                  required
                  placeholder="Payment Platform"
                />
              </label>
              <label className="block text-sm">
                <span className="mb-1 block text-xs text-[var(--muted)]">설명</span>
                <textarea
                  className="nimbus-input min-h-[80px] resize-y"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="선택 사항"
                />
              </label>
              <button
                type="submit"
                disabled={saving || !workspaceId}
                className="nimbus-btn-primary w-full"
              >
                {saving ? "생성 중…" : "프로젝트 만들기"}
              </button>
            </form>
          </Card>
        ) : null}

        <Card padding={false} className={canMutate ? "" : "lg:col-span-2"}>
          {projects.length === 0 ? (
            <EmptyState
              title="프로젝트가 없습니다"
              description="서비스를 묶을 비즈니스 컨텍스트로 프로젝트를 먼저 만들어 보세요."
            />
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {projects.map((p) => {
                const archived = p.status === "ARCHIVED";
                return (
                  <li
                    key={p.id}
                    className="flex flex-wrap items-start justify-between gap-4 px-5 py-4 transition hover:bg-white/[0.02]"
                  >
                    <div className="min-w-0">
                      <p className="font-medium text-zinc-100">{p.name}</p>
                      {p.description ? (
                        <p className="mt-1 text-xs text-[var(--muted)]">
                          {p.description}
                        </p>
                      ) : null}
                    </div>
                    <div className="flex shrink-0 flex-wrap items-center gap-2">
                      <StatusBadge value={p.status} />
                      {canMutate ? (
                        <>
                          <button
                            type="button"
                            disabled={busyId === p.id}
                            onClick={() => onClone(p)}
                            className="text-xs text-[var(--primary)] hover:underline disabled:opacity-50"
                          >
                            복제
                          </button>
                          {archived ? (
                            <button
                              type="button"
                              disabled={busyId === p.id}
                              onClick={() => onRestore(p)}
                              className="text-xs text-emerald-300 hover:underline disabled:opacity-50"
                            >
                              복원
                            </button>
                          ) : (
                            <button
                              type="button"
                              disabled={busyId === p.id}
                              onClick={() => onArchive(p)}
                              className="text-xs text-[var(--muted)] hover:text-amber-200 disabled:opacity-50"
                            >
                              보관
                            </button>
                          )}
                        </>
                      ) : null}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      </div>
    </Page>
  );
}
