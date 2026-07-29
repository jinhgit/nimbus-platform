"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { API_BASE, devLogin, setAuthSession } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [name, setName] = useState("Nimbus Developer");
  const [email, setEmail] = useState("dev@nimbus.local");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await devLogin(name, email);
      if (!res.success || !res.data) {
        setError(res.error?.message ?? "로그인에 실패했습니다.");
        return;
      }
      setAuthSession(res.data);
      router.replace("/dashboard");
    } catch {
      setError("API 서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6">
      <div className="rounded-2xl border border-[var(--border)] bg-[var(--card)] p-8">
        <p className="mb-2 text-xs font-medium tracking-wide text-[var(--primary)]">
          NIMBUS PLATFORM
        </p>
        <h1 className="mb-2 text-2xl font-semibold">로그인</h1>
        <p className="mb-6 text-sm text-[var(--muted)]">
          로컬 개발용 Dev Login입니다. GitHub OAuth는 Client ID 설정 후 사용할 수
          있습니다. (완전 무료 경로)
        </p>

        <form onSubmit={onSubmit} className="space-y-4">
          <label className="block text-sm">
            <span className="mb-1.5 block text-[var(--muted)]">이름</span>
            <input
              className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2 outline-none focus:border-[var(--primary)]"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </label>
          <label className="block text-sm">
            <span className="mb-1.5 block text-[var(--muted)]">이메일</span>
            <input
              type="email"
              className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2 outline-none focus:border-[var(--primary)]"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>

          {error && (
            <p className="rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-[var(--primary)] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-[var(--primary-hover)] disabled:opacity-60"
          >
            {loading ? "로그인 중…" : "Dev Login으로 시작"}
          </button>
        </form>

        <div className="mt-6 border-t border-[var(--border)] pt-4 text-xs text-[var(--muted)]">
          <p>API: {API_BASE}</p>
          <p className="mt-2">
            <Link href="/" className="text-[var(--primary)] hover:underline">
              랜딩 페이지로
            </Link>
          </p>
        </div>
      </div>
    </main>
  );
}
