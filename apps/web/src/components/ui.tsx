import type { ReactNode } from "react";

export function Page({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return <div className={`nimbus-page ${className}`}>{children}</div>;
}

export function PageHeader({
  title,
  description,
  actions,
  eyebrow,
}: {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  eyebrow?: string;
}) {
  return (
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        {eyebrow ? (
          <p className="mb-1.5 text-[11px] font-medium uppercase tracking-[0.14em] text-[var(--primary)]">
            {eyebrow}
          </p>
        ) : null}
        <h1 className="text-2xl font-semibold tracking-tight text-white sm:text-[1.65rem]">
          {title}
        </h1>
        {description ? (
          <div className="mt-1.5 max-w-2xl text-sm leading-relaxed text-[var(--muted)]">
            {description}
          </div>
        ) : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}

export function Card({
  children,
  className = "",
  interactive = false,
  padding = true,
}: {
  children: ReactNode;
  className?: string;
  interactive?: boolean;
  padding?: boolean;
}) {
  return (
    <div
      className={`nimbus-card ${interactive ? "nimbus-card-interactive" : ""} ${
        padding ? "p-5" : ""
      } ${className}`}
    >
      {children}
    </div>
  );
}

export function CardTitle({
  children,
  action,
}: {
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="mb-3 flex items-center justify-between gap-2">
      <h2 className="text-sm font-medium tracking-tight text-zinc-100">{children}</h2>
      {action}
    </div>
  );
}

export function StatusBadge({ value }: { value?: string | null }) {
  if (!value) {
    return <span className="nimbus-badge nimbus-badge-neutral">—</span>;
  }
  const u = value.toUpperCase();
  const ok =
    u === "READY" ||
    u === "RUNNING" ||
    u === "SUCCESS" ||
    u === "COMPLETED" ||
    u === "HEALTHY" ||
    u === "SIMULATED" ||
    u === "CONNECTED" ||
    u === "UP";
  const bad =
    u === "FAILED" ||
    u === "ERROR" ||
    u === "UNHEALTHY" ||
    u === "DOWN" ||
    u === "CANCELLED";
  let cls = "nimbus-badge-info";
  if (ok) cls = "nimbus-badge-ok";
  else if (bad) cls = "nimbus-badge-bad";
  else if (
    u === "PENDING" ||
    u === "PROVISIONING" ||
    u === "DEPLOYING" ||
    u === "DEGRADED" ||
    u === "ARCHIVED" ||
    u === "QUEUED" ||
    u === "WAITING" ||
    u === "UNKNOWN" ||
    u.includes("ING")
  )
    cls = "nimbus-badge-warn";
  else if (u === "DRAFT" || u === "DISCONNECTED") cls = "nimbus-badge-neutral";

  return <span className={`nimbus-badge ${cls}`}>{value}</span>;
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center">
      <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--border)] bg-white/[0.03] text-[var(--muted)]">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M9 9h6M9 13h4" />
        </svg>
      </div>
      <p className="text-sm font-medium text-zinc-200">{title}</p>
      {description ? (
        <p className="mt-1 max-w-sm text-xs leading-relaxed text-[var(--muted)]">
          {description}
        </p>
      ) : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  );
}

export function StatCard({
  label,
  value,
  hint,
  tone = "default",
}: {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: "default" | "ok" | "warn" | "bad" | "info";
}) {
  const accent =
    tone === "ok"
      ? "from-emerald-500/15"
      : tone === "warn"
        ? "from-amber-500/15"
        : tone === "bad"
          ? "from-rose-500/15"
          : tone === "info"
            ? "from-blue-500/15"
            : "from-white/[0.04]";

  return (
    <div className={`nimbus-card relative overflow-hidden p-4`}>
      <div
        className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${accent} via-transparent to-transparent`}
      />
      <div className="relative">
        <p className="text-[11px] font-medium uppercase tracking-wide text-[var(--muted)]">
          {label}
        </p>
        <p className="mt-2 text-2xl font-semibold tracking-tight tabular-nums text-white">
          {value}
        </p>
        {hint ? (
          <p className="mt-1 text-[11px] text-[var(--muted-soft)]">{hint}</p>
        ) : null}
      </div>
    </div>
  );
}
