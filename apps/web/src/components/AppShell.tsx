"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ComponentType } from "react";
import {
  clearAuthSession,
  fetchMe,
  getStoredUser,
  logout,
  type MeResponse,
  type UserSummary,
} from "@/lib/api";
import {
  IconAudit,
  IconCatalog,
  IconDashboard,
  IconInfrastructure,
  IconLogs,
  IconLogout,
  IconMonitoring,
  IconPipelines,
  IconProjects,
  IconServices,
  IconSettings,
  IconWizard,
  IconWorkspaces,
  type IconProps,
} from "@/components/icons";

type NavItem = {
  href: string;
  label: string;
  Icon: ComponentType<IconProps>;
};

const nav: NavItem[] = [
  { href: "/dashboard", label: "대시보드", Icon: IconDashboard },
  { href: "/projects", label: "프로젝트", Icon: IconProjects },
  { href: "/services", label: "서비스", Icon: IconServices },
  { href: "/catalog", label: "카탈로그", Icon: IconCatalog },
  { href: "/wizard", label: "서비스 생성", Icon: IconWizard },
  { href: "/pipelines", label: "파이프라인", Icon: IconPipelines },
  { href: "/monitoring", label: "모니터링", Icon: IconMonitoring },
  { href: "/logs", label: "로그", Icon: IconLogs },
  { href: "/audit", label: "감사 로그", Icon: IconAudit },
  { href: "/workspaces", label: "워크스페이스", Icon: IconWorkspaces },
  { href: "/infrastructure", label: "인프라", Icon: IconInfrastructure },
  { href: "/settings", label: "설정", Icon: IconSettings },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | MeResponse | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const stored = getStoredUser();
    if (!stored) {
      router.replace("/login");
      return;
    }
    setUser(stored);
    setReady(true);
    fetchMe().then((res) => {
      if (!res.success || !res.data) {
        clearAuthSession();
        router.replace("/login");
        return;
      }
      setUser(res.data);
    });
  }, [router]);

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-[var(--muted)]">
        로딩 중…
      </div>
    );
  }

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 shrink-0 flex-col border-r border-[var(--border)] bg-[var(--card)]">
        <div className="border-b border-[var(--border)] px-5 py-5">
          <Link href="/dashboard" className="text-sm font-semibold tracking-wide">
            NIMBUS
          </Link>
          <p className="mt-1 text-xs text-[var(--muted)]">플랫폼 포털</p>
        </div>
        <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-3">
          {nav.map((item) => {
            const active = pathname.startsWith(item.href);
            const { Icon } = item;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition ${
                  active
                    ? "bg-[var(--primary)]/15 text-white"
                    : "text-[var(--muted)] hover:bg-white/5 hover:text-white"
                }`}
              >
                <Icon
                  size={18}
                  className={`shrink-0 ${active ? "opacity-100" : "opacity-80"}`}
                />
                <span className="truncate">{item.label}</span>
              </Link>
            );
          })}
        </nav>
        <div className="border-t border-[var(--border)] p-4">
          <p className="truncate text-sm font-medium">{user?.name}</p>
          <p className="truncate text-xs text-[var(--muted)]">{user?.email}</p>
          <button
            type="button"
            onClick={onLogout}
            className="mt-3 inline-flex items-center gap-1.5 text-xs text-[var(--muted)] hover:text-white"
          >
            <IconLogout size={14} />
            로그아웃
          </button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 overflow-auto">{children}</main>
    </div>
  );
}
