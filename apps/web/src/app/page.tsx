import Link from "next/link";

const features = [
  {
    title: "Service Wizard",
    desc: "카탈로그 → AI 추천 → 미리보기 → 프로비저닝까지 한 흐름으로.",
  },
  {
    title: "GitOps path",
    desc: "Helm, Terraform, Actions, Argo 매니페스트를 자동 생성합니다.",
  },
  {
    title: "AI Decision Engine",
    desc: "Runtime / DB / Cache 추천과 Confidence · Reason을 제공합니다.",
  },
  {
    title: "Environments",
    desc: "DEV → STAGE → PRODUCTION, 변수·시크릿·Promote를 지원합니다.",
  },
];

export default function HomePage() {
  return (
    <main className="relative mx-auto flex min-h-screen max-w-5xl flex-col px-6 py-16">
      <div className="nimbus-mesh pointer-events-none absolute inset-0 opacity-40" />

      <header className="relative mb-16">
        <div className="mb-6 flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-blue-400 to-blue-600 text-xs font-bold text-white shadow-[0_0_28px_-4px_var(--primary-glow)]">
            N
          </span>
          <span className="text-[11px] font-medium uppercase tracking-[0.16em] text-blue-300/90">
            Nimbus Platform
          </span>
        </div>
        <h1 className="mb-4 max-w-2xl text-4xl font-semibold tracking-tight text-white sm:text-5xl sm:leading-[1.1]">
          AI-native Internal
          <br />
          Developer Platform
        </h1>
        <p className="max-w-2xl text-base leading-relaxed text-[var(--muted)] sm:text-lg">
          YAML 없이도 서비스를 만들 수 있습니다. Catalog, Wizard, AI 리뷰, GitHub,
          환경 승격, 감사 로그까지 — 기본 경로는 free-only 입니다.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/login" className="nimbus-btn-primary px-5 py-2.5">
            시작하기
          </Link>
          <Link href="/dashboard" className="nimbus-btn-ghost px-5 py-2.5">
            Dashboard
          </Link>
          <a
            href="http://localhost:8080/api/v1/health"
            className="nimbus-btn-ghost px-5 py-2.5"
            target="_blank"
            rel="noreferrer"
          >
            API 상태
          </a>
        </div>
      </header>

      <section className="relative grid gap-3 sm:grid-cols-2">
        {features.map((f) => (
          <div key={f.title} className="nimbus-card nimbus-card-interactive p-5">
            <h2 className="text-sm font-medium text-zinc-100">{f.title}</h2>
            <p className="mt-2 text-sm leading-relaxed text-[var(--muted)]">
              {f.desc}
            </p>
          </div>
        ))}
      </section>

      <footer className="relative mt-auto pt-16 text-center text-xs text-[var(--muted-soft)]">
        앱만 만들지 말고, 플랫폼을 설계하세요.
      </footer>
    </main>
  );
}
