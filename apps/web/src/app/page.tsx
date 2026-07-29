import Link from "next/link";

const features = [
  { title: "Service Wizard", desc: "클릭 몇 번으로 서비스 생성부터 배포까지" },
  { title: "GitOps", desc: "Terraform → Git → ArgoCD → Kubernetes" },
  { title: "AI Platform Engineer", desc: "아키텍처 리뷰, YAML, 장애 분석" },
  { title: "Service Catalog", desc: "Golden Path 템플릿과 Blueprint" },
];

export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-5xl flex-col px-6 py-16">
      <header className="mb-16">
        <p className="mb-3 text-sm font-medium tracking-wide text-[var(--primary)]">
          NIMBUS PLATFORM
        </p>
        <h1 className="mb-4 text-4xl font-semibold tracking-tight sm:text-5xl">
          AI Native Internal
          <br />
          Developer Platform
        </h1>
        <p className="max-w-2xl text-lg text-[var(--muted)]">
          Kubernetes, Helm, Terraform을 몰라도 서비스를 만들고 배포할 수 있는
          Platform Engineering Portal. 지금은 monorepo 뼈대 단계입니다.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link
            href="/dashboard"
            className="rounded-lg bg-[var(--primary)] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-[var(--primary-hover)]"
          >
            Dashboard (skeleton)
          </Link>
          <a
            href="http://localhost:8080/api/v1/health"
            className="rounded-lg border border-[var(--border)] bg-[var(--card)] px-4 py-2.5 text-sm font-medium transition hover:border-zinc-500"
            target="_blank"
            rel="noreferrer"
          >
            API Health
          </a>
        </div>
      </header>

      <section className="grid gap-4 sm:grid-cols-2">
        {features.map((item) => (
          <article
            key={item.title}
            className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5"
          >
            <h2 className="mb-2 text-base font-medium">{item.title}</h2>
            <p className="text-sm text-[var(--muted)]">{item.desc}</p>
          </article>
        ))}
      </section>

      <footer className="mt-auto pt-16 text-sm text-[var(--muted)]">
        Phase 0 — Foundation · docs 는 <code className="text-zinc-300">/docs</code> 참고
      </footer>
    </main>
  );
}
