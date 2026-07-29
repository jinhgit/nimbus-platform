const widgets = [
  { label: "Running Services", value: "—" },
  { label: "Deployments", value: "—" },
  { label: "Pipelines", value: "—" },
  { label: "Incidents", value: "—" },
];

export default function DashboardPage() {
  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            위젯·실시간 데이터는 Auth / Project API 연결 후 붙입니다.
          </p>
        </div>
        <span className="rounded-full border border-[var(--border)] px-3 py-1 text-xs text-[var(--muted)]">
          skeleton
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {widgets.map((w) => (
          <div
            key={w.label}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4"
          >
            <p className="text-xs text-[var(--muted)]">{w.label}</p>
            <p className="mt-2 text-2xl font-semibold tabular-nums">{w.value}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 rounded-xl border border-dashed border-[var(--border)] p-8 text-center text-sm text-[var(--muted)]">
        Sidebar / Create Service Wizard / AI Panel 은 다음 스프린트에서 구성
      </div>
    </div>
  );
}
