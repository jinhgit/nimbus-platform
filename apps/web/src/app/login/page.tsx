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
      setError("API에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="relative flex min-h-screen items-center justify-center px-6 py-12">
      <div className="nimbus-mesh pointer-events-none absolute inset-0 opacity-60" />
      <div className="relative w-full max-w-[420px]">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-blue-400 to-blue-600 text-sm font-bold text-white shadow-[0_0_32px_-4px_var(--primary-glow)]">
            N
          </div>
          <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-blue-300/90">
            Nimbus Platform
          </p>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-white">
            Welcome back
          </h1>
          <p className="mt-2 text-sm text-[var(--muted)]">
            로컬 free-only 경로용 Dev Login입니다. GitHub OAuth는 설정 시 사용할 수
            있습니다.
          </p>
        </div>

        <div className="nimbus-card p-7">
          <form onSubmit={onSubmit} className="space-y-4">
            <label className="block text-sm">
              <span className="mb-1.5 block text-xs text-[var(--muted)]">이름</span>
              <input
                className="nimbus-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1.5 block text-xs text-[var(--muted)]">이메일</span>
              <input
                type="email"
                className="nimbus-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>

            {error && (
              <p className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="nimbus-btn-primary w-full py-2.5"
            >
              {loading ? "로그인 중…" : "Dev Login으로 시작"}
            </button>
          </form>

          <div className="mt-6 border-t border-[var(--border)] pt-4 text-center text-xs text-[var(--muted)]">
            <p className="font-mono text-[11px] text-zinc-500">API {API_BASE}</p>
            <Link
              href="/"
              className="mt-3 inline-block text-[var(--primary)] hover:underline"
            >
              ← 랜딩 페이지로
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
