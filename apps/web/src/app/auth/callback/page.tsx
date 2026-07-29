"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { fetchMe, setAuthSession } from "@/lib/api";

function CallbackInner() {
  const params = useSearchParams();
  const router = useRouter();
  const [message, setMessage] = useState("GitHub 로그인 처리 중…");

  useEffect(() => {
    const accessToken = params.get("accessToken");
    if (!accessToken) {
      setMessage("accessToken 이 없습니다.");
      return;
    }
    localStorage.setItem("nimbus_access_token", accessToken);
    fetchMe().then((res) => {
      if (!res.success || !res.data) {
        setMessage(res.error?.message ?? "사용자 정보를 가져오지 못했습니다.");
        return;
      }
      setAuthSession({
        accessToken,
        refreshToken: "",
        expiresIn: Number(params.get("expiresIn") ?? 3600),
        user: {
          id: res.data.id,
          name: res.data.name,
          email: res.data.email,
          avatarUrl: res.data.avatarUrl,
          role: res.data.role,
          workspaceId: res.data.workspace?.id,
        },
      });
      router.replace("/dashboard");
    });
  }, [params, router]);

  return (
    <main className="flex min-h-screen items-center justify-center text-sm text-[var(--muted)]">
      {message}
    </main>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <main className="flex min-h-screen items-center justify-center text-sm text-[var(--muted)]">
          로딩 중…
        </main>
      }
    >
      <CallbackInner />
    </Suspense>
  );
}
