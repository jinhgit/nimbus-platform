"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";
import {
  createProject,
  fetchMe,
  fetchProjects,
  type Project,
} from "@/lib/api";

export default function ProjectsPage() {
  const [workspaceId, setWorkspaceId] = useState<string | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function load(ws: string) {
    const res = await fetchProjects(ws);
    if (res.success && res.data) setProjects(res.data);
  }

  useEffect(() => {
    fetchMe().then((res) => {
      const ws = res.data?.workspace?.id;
      if (!ws) return;
      setWorkspaceId(ws);
      load(ws);
    });
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!workspaceId) return;
    setLoading(true);
    setError(null);
    const res = await createProject({
      name,
      description: description || undefined,
      workspaceId,
    });
    setLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "생성에 실패했습니다.");
      return;
    }
    setName("");
    setDescription("");
    await load(workspaceId);
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">프로젝트</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          비즈니스 컨텍스트 단위입니다. 서비스는{" "}
          <Link href="/wizard" className="text-[var(--primary)] hover:underline">
            Service Wizard
          </Link>
          에서 생성합니다.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <form
          onSubmit={onCreate}
          className="h-fit rounded-xl border border-[var(--border)] bg-[var(--card)] p-5"
        >
          <h2 className="mb-4 text-sm font-medium">새 프로젝트</h2>
          <label className="mb-3 block text-sm">
            <span className="mb-1 block text-[var(--muted)]">이름</span>
            <input
              className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2 text-sm outline-none focus:border-[var(--primary)]"
              value={name}
              onChange={(e) => setName(e.target.value)}
              minLength={3}
              maxLength={50}
              required
              placeholder="Payment Platform"
            />
          </label>
          <label className="mb-4 block text-sm">
            <span className="mb-1 block text-[var(--muted)]">설명</span>
            <textarea
              className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2 text-sm outline-none focus:border-[var(--primary)]"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="선택 사항"
            />
          </label>
          {error && <p className="mb-3 text-xs text-red-300">{error}</p>}
          <button
            type="submit"
            disabled={loading || !workspaceId}
            className="w-full rounded-lg bg-[var(--primary)] px-3 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
          >
            {loading ? "생성 중…" : "프로젝트 만들기"}
          </button>
        </form>

        <div className="rounded-xl border border-[var(--border)] bg-[var(--card)]">
          <div className="border-b border-[var(--border)] px-5 py-3 text-sm text-[var(--muted)]">
            {projects.length}개 프로젝트
          </div>
          {projects.length === 0 ? (
            <p className="p-8 text-center text-sm text-[var(--muted)]">
              프로젝트가 없습니다.
            </p>
          ) : (
            <ul className="divide-y divide-[var(--border)]">
              {projects.map((p) => (
                <li
                  key={p.id}
                  className="flex items-start justify-between gap-4 px-5 py-4"
                >
                  <div>
                    <p className="font-medium">{p.name}</p>
                    {p.description && (
                      <p className="mt-1 text-sm text-[var(--muted)]">
                        {p.description}
                      </p>
                    )}
                  </div>
                  <div className="text-right text-xs text-[var(--muted)]">
                    <p>{p.status}</p>
                    <p className="mt-1">{p.visibility}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
