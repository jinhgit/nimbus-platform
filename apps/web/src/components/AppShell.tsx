"use client";

// UI language: sidebar + page titles EN; body copy KO — see apps/web/UI-CONVENTIONS.md

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
import { CommandPalette } from "@/components/CommandPalette";
import {
  IconAudit,
  IconCatalog,
  IconDashboard,
  IconIncidents,
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

type NavSection = {
  title?: string;
  items: NavItem[];
};

const navSections: NavSection[] = [
  {
    items: [
      { href: "/dashboard", label: "Dashboard", Icon: IconDashboard },
      { href: "/projects", label: "Projects", Icon: IconProjects },
      { href: "/services", label: "Services", Icon: IconServices },
      { href: "/catalog", label: "Catalog", Icon: IconCatalog },
      { href: "/wizard", label: "Create Service", Icon: IconWizard },
    ],
  },
  {
    title: "Operations",
    items: [
      { href: "/pipelines", label: "Pipelines", Icon: IconPipelines },
      { href: "/incidents", label: "Incidents", Icon: IconIncidents },
      { href: "/monitoring", label: "Monitoring", Icon: IconMonitoring },
      { href: "/logs", label: "Logs", Icon: IconLogs },
      { href: "/audit", label: "Audit", Icon: IconAudit },
      { href: "/infrastructure", label: "Infrastructure", Icon: IconInfrastructure },
    ],
  },
  {
    title: "Workspace",
    items: [
      { href: "/workspaces", label: "Workspaces", Icon: IconWorkspaces },
      { href: "/settings", label: "Settings", Icon: IconSettings },
    ],
  },
];

function initials(name?: string | null) {
  if (!name) return "N";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

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
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-sm text-[var(--muted)]">
        <div className="h-8 w-8 animate-pulse rounded-xl bg-[var(--primary)]/30" />
        Loading portal…
      </div>
    );
  }

  const workspaceName =
    user && "workspace" in user && user.workspace
      ? user.workspace.name
      : null;

  return (
    <div className="flex min-h-screen">
      <aside className="sticky top-0 flex h-screen w-[15.5rem] shrink-0 flex-col border-r border-[var(--border)] bg-[var(--card)]/90 backdrop-blur-xl">
        <div className="border-b border-[var(--border)] px-4 py-5">
          <Link href="/dashboard" className="group flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-blue-400 to-blue-600 text-xs font-bold text-white shadow-[0_0_20px_-4px_var(--primary-glow)]">
              N
            </span>
            <span>
              <span className="block text-[13px] font-semibold tracking-[0.12em] text-white">
                NIMBUS
              </span>
              <span className="block text-[11px] text-[var(--muted)]">
                Platform Portal
              </span>
            </span>
          </Link>
        </div>

        <nav className="flex flex-1 flex-col gap-4 overflow-y-auto px-2.5 py-4">
          {navSections.map((section, si) => (
            <div key={section.title ?? `s-${si}`}>
              {section.title ? (
                <p className="mb-1.5 px-2.5 text-[10px] font-medium uppercase tracking-[0.14em] text-[var(--muted-soft)]">
                  {section.title}
                </p>
              ) : null}
              <div className="flex flex-col gap-0.5">
                {section.items.map((item) => {
                  const active = pathname.startsWith(item.href);
                  const { Icon } = item;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`group relative flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-[13px] transition ${
                        active
                          ? "bg-[var(--primary-soft)] text-white"
                          : "text-[var(--muted)] hover:bg-white/[0.04] hover:text-white"
                      }`}
                    >
                      {active ? (
                        <span className="absolute left-0 top-1/2 h-4 w-0.5 -translate-y-1/2 rounded-full bg-[var(--primary)]" />
                      ) : null}
                      <Icon
                        size={17}
                        className={`shrink-0 ${
                          active
                            ? "text-blue-300"
                            : "text-zinc-500 group-hover:text-zinc-300"
                        }`}
                      />
                      <span className="truncate font-medium">{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="border-t border-[var(--border)] p-3">
          <div className="flex items-center gap-2.5 rounded-xl border border-[var(--border)] bg-black/20 px-2.5 py-2.5">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-zinc-600 to-zinc-800 text-[11px] font-semibold text-white">
              {initials(user?.name)}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13px] font-medium text-zinc-100">
                {user?.name}
              </p>
              <p className="truncate text-[11px] text-[var(--muted)]">
                {workspaceName ?? user?.email}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onLogout}
            className="mt-2 flex w-full items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-[12px] text-[var(--muted)] transition hover:bg-white/[0.04] hover:text-white"
          >
            <IconLogout size={14} />
            Log out
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 overflow-auto">{children}</main>
      <CommandPalette />
    </div>
  );
}
