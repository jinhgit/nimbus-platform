"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  createWizard,
  executeWizard,
  fetchCatalog,
  fetchGitHubStatus,
  fetchK8sCluster,
  fetchProjects,
  fetchMe,
  fetchService,
  getWizard,
  previewWizard,
  recommendWizard,
  updateWizard,
  type AiRecommendation,
  type AppService,
  type CatalogTemplate,
  type Project,
  type Wizard,
  type WizardPreview,
} from "@/lib/api";

const STEPS = [
  "서비스 정보",
  "템플릿",
  "인프라",
  "AI 리뷰",
  "미리보기",
  "프로비저닝",
  "완료",
];

const PREVIEW_TABS = [
  { id: "structure" as const, label: "구조" },
  { id: "blueprint" as const, label: "Blueprint" },
  { id: "helm" as const, label: "Helm" },
  { id: "terraform" as const, label: "Terraform" },
  { id: "actions" as const, label: "Actions" },
  { id: "yaml" as const, label: "YAML" },
  { id: "argo" as const, label: "ArgoCD" },
];

export default function WizardPage() {
  const [step, setStep] = useState(0);
  const [projects, setProjects] = useState<Project[]>([]);
  const [templates, setTemplates] = useState<CatalogTemplate[]>([]);
  const [projectId, setProjectId] = useState("");
  const [serviceName, setServiceName] = useState("payment-api");
  const [templateId, setTemplateId] = useState("");
  const [environmentType, setEnvironmentType] = useState("PRODUCTION");
  const [wizard, setWizard] = useState<Wizard | null>(null);
  const [recommendation, setRecommendation] = useState<AiRecommendation | null>(null);
  const [preview, setPreview] = useState<WizardPreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [previewTab, setPreviewTab] = useState<(typeof PREVIEW_TABS)[number]["id"]>("structure");
  const [githubConnected, setGithubConnected] = useState(false);
  const [k8sAvailable, setK8sAvailable] = useState(false);
  const [createdService, setCreatedService] = useState<AppService | null>(null);

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      if (!ws) return;
      const [p, c, g, k] = await Promise.all([
        fetchProjects(ws),
        fetchCatalog(),
        fetchGitHubStatus(),
        fetchK8sCluster(),
      ]);
      if (p.success && p.data) {
        setProjects(p.data);
        if (p.data[0]) setProjectId(p.data[0].id);
      }
      if (c.success && c.data) {
        setTemplates(c.data);
        const spring = c.data.find((t) => t.runtime === "SPRING_BOOT") ?? c.data[0];
        if (spring) setTemplateId(spring.id);
      }
      if (g.success && g.data) setGithubConnected(g.data.connected);
      if (k.success && k.data) setK8sAvailable(k.data.available);
    });
  }, []);

  useEffect(() => {
    if (!wizard || step !== 5) return;
    if (wizard.status === "COMPLETED" || wizard.status === "FAILED") return;
    const id = setInterval(async () => {
      const res = await getWizard(wizard.id);
      if (res.success && res.data) {
        setWizard(res.data);
        if (res.data.status === "COMPLETED") {
          setStep(6);
        }
      }
    }, 500);
    return () => clearInterval(id);
  }, [wizard, step]);

  // Complete 단계에서 생성된 서비스 상세 로드
  useEffect(() => {
    if (step !== 6 || !wizard?.serviceId) return;
    fetchService(wizard.serviceId).then((res) => {
      if (res.success && res.data) setCreatedService(res.data);
    });
  }, [step, wizard?.serviceId]);

  const selectedTemplate = useMemo(
    () => templates.find((t) => t.id === templateId),
    [templates, templateId],
  );

  async function ensureWizard() {
    if (wizard) return wizard;
    if (!projectId) {
      setError("프로젝트를 먼저 생성하세요.");
      return null;
    }
    const res = await createWizard({
      projectId,
      serviceName,
      templateId: templateId || undefined,
    });
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "Wizard 생성에 실패했습니다.");
      return null;
    }
    setWizard(res.data);
    return res.data;
  }

  async function goNext() {
    setError(null);
    setLoading(true);
    try {
      if (step === 0) {
        const w = await ensureWizard();
        if (!w) return;
        await updateWizard(w.id, { serviceName, currentStep: 2 });
        setStep(1);
      } else if (step === 1) {
        if (!wizard) return;
        await updateWizard(wizard.id, {
          templateId,
          runtime: selectedTemplate?.runtime,
          currentStep: 3,
        });
        const refreshed = await getWizard(wizard.id);
        if (refreshed.data) setWizard(refreshed.data);
        setStep(2);
      } else if (step === 2) {
        if (!wizard) return;
        await updateWizard(wizard.id, {
          environmentType,
          currentStep: 4,
        });
        setStep(3);
      } else if (step === 3) {
        setStep(4);
      } else if (step === 4) {
        if (!wizard) return;
        const v = await previewWizard(wizard.id);
        if (!v.success || !v.data) {
          setError(v.error?.message ?? "미리보기에 실패했습니다.");
          return;
        }
        setPreview(v.data);
        setStep(5);
        const exec = await executeWizard(wizard.id);
        if (!exec.success) {
          setError(exec.error?.message ?? "배포 시작에 실패했습니다.");
          return;
        }
        const refreshed = await getWizard(wizard.id);
        if (refreshed.data) setWizard(refreshed.data);
      }
    } finally {
      setLoading(false);
    }
  }

  async function runRecommend() {
    setLoading(true);
    setError(null);
    try {
      let w = wizard;
      if (!w) {
        w = await ensureWizard();
        if (!w) return;
        await updateWizard(w.id, {
          templateId,
          runtime: selectedTemplate?.runtime,
          environmentType,
        });
      } else {
        await updateWizard(w.id, { environmentType });
      }
      const res = await recommendWizard(w.id);
      if (!res.success || !res.data) {
        setError(res.error?.message ?? "AI 추천에 실패했습니다.");
        return;
      }
      setRecommendation(res.data);
      const refreshed = await getWizard(w.id);
      if (refreshed.data) setWizard(refreshed.data);
    } finally {
      setLoading(false);
    }
  }

  async function runPreviewOnly() {
    if (!wizard) return;
    setLoading(true);
    const res = await previewWizard(wizard.id);
    setLoading(false);
    if (res.success && res.data) {
      setPreview(res.data);
      setStep(4);
    } else {
      setError(res.error?.message ?? "미리보기에 실패했습니다.");
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Service Wizard</h1>
          <p className="mt-1 text-sm text-[var(--muted)]">
            카탈로그 → AI 추천 → 미리보기 → 프로비저닝 · 시연 메인 플로우
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href="/settings"
            className={`rounded-full px-3 py-1 text-xs ${
              githubConnected
                ? "bg-emerald-500/15 text-emerald-400"
                : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            {githubConnected ? "GitHub 연결됨" : "GitHub 미연결"}
          </Link>
          <Link
            href="/infrastructure"
            className={`rounded-full px-3 py-1 text-xs ${
              k8sAvailable
                ? "bg-emerald-500/15 text-emerald-400"
                : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            {k8sAvailable ? "K8s 연결됨" : "K8s 미연결 · sim"}
          </Link>
        </div>
      </div>

      <ol className="mb-8 flex flex-wrap gap-2">
        {STEPS.map((label, i) => (
          <li
            key={label}
            className={`rounded-full px-3 py-1 text-xs ${
              i === step
                ? "bg-[var(--primary)] text-white"
                : i < step
                  ? "bg-[var(--primary)]/20 text-white"
                  : "border border-[var(--border)] text-[var(--muted)]"
            }`}
          >
            {i + 1}. {label}
          </li>
        ))}
      </ol>

      {error && (
        <p className="mb-4 rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {error}
        </p>
      )}

      <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-6">
        {step === 0 && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">서비스 정보</h2>
            {projects.length === 0 ? (
              <p className="text-sm text-[var(--muted)]">
                프로젝트가 없습니다.{" "}
                <Link href="/projects" className="text-[var(--primary)] hover:underline">
                  프로젝트 만들기
                </Link>
              </p>
            ) : (
              <label className="block text-sm">
                <span className="mb-1 block text-[var(--muted)]">프로젝트</span>
                <select
                  className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2"
                  value={projectId}
                  onChange={(e) => setProjectId(e.target.value)}
                >
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label className="block text-sm">
              <span className="mb-1 block text-[var(--muted)]">서비스 이름</span>
              <input
                className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2"
                value={serviceName}
                onChange={(e) => setServiceName(e.target.value)}
                minLength={3}
                maxLength={50}
                placeholder="payment-api"
              />
            </label>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">템플릿 (Catalog)</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              {templates.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => setTemplateId(t.id)}
                  className={`rounded-xl border p-4 text-left transition ${
                    templateId === t.id
                      ? "border-[var(--primary)] bg-[var(--primary)]/10"
                      : "border-[var(--border)] hover:border-zinc-500"
                  }`}
                >
                  <p className="font-medium">{t.name}</p>
                  <p className="mt-1 text-xs text-[var(--muted)]">
                    {t.runtime} · {t.type}
                  </p>
                </button>
              ))}
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">인프라 / 환경</h2>
            <label className="block text-sm">
              <span className="mb-1 block text-[var(--muted)]">환경</span>
              <select
                className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2"
                value={environmentType}
                onChange={(e) => setEnvironmentType(e.target.value)}
              >
                <option value="DEV">DEV</option>
                <option value="STAGE">STAGE</option>
                <option value="PRODUCTION">PRODUCTION</option>
              </select>
            </label>
            <p className="text-sm text-[var(--muted)]">
              Runtime: {selectedTemplate?.runtime ?? wizard?.runtime ?? "—"} ·
              템플릿: {selectedTemplate?.name ?? "—"}
            </p>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-medium">AI 추천</h2>
              <button
                type="button"
                onClick={runRecommend}
                disabled={loading}
                className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
              >
                {loading ? "분석 중…" : "AI Recommendation"}
              </button>
            </div>
            {!recommendation ? (
              <p className="text-sm text-[var(--muted)]">
                AI Recommendation을 눌러 Platform Engineer 역할의 추천을 받으세요.
              </p>
            ) : (
              <div className="space-y-4">
                <p className="rounded-lg border border-[var(--border)] bg-black/20 p-4 text-sm">
                  {recommendation.summary}
                  <span className="ml-2 text-[var(--muted)]">
                    (점수 {recommendation.overallScore} · {recommendation.provider})
                  </span>
                </p>
                <div className="grid gap-3 sm:grid-cols-2">
                  {recommendation.items.map((item) => (
                    <div
                      key={item.key}
                      className="rounded-lg border border-[var(--border)] p-4"
                    >
                      <div className="flex justify-between text-sm">
                        <span className="uppercase tracking-wide text-[var(--muted)]">
                          {item.key}
                        </span>
                        <span className="text-emerald-400">
                          신뢰도 {item.confidence}%
                        </span>
                      </div>
                      <p className="mt-2 text-lg font-semibold">{item.value}</p>
                      <p className="mt-1 text-xs text-[var(--muted)]">{item.reason}</p>
                    </div>
                  ))}
                </div>
                <button
                  type="button"
                  onClick={runPreviewOnly}
                  className="text-sm text-[var(--primary)] hover:underline"
                >
                  미리보기로 이동 →
                </button>
              </div>
            )}
          </div>
        )}

        {step === 4 && preview && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">
              미리보기 · {preview.repository}
            </h2>
            <div className="flex flex-wrap gap-2">
              {PREVIEW_TABS.map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setPreviewTab(tab.id)}
                  className={`rounded-full px-3 py-1 text-xs ${
                    previewTab === tab.id
                      ? "bg-[var(--primary)] text-white"
                      : "border border-[var(--border)] text-[var(--muted)]"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <pre className="max-h-96 overflow-auto rounded-lg border border-[var(--border)] bg-black/40 p-4 text-xs leading-relaxed text-zinc-300">
              {previewTab === "structure" &&
                Object.entries(preview.repositoryStructure)
                  .map(([k, v]) => `${k}  # ${v}`)
                  .join("\n")}
              {previewTab === "blueprint" && preview.blueprint}
              {previewTab === "helm" && preview.helmValues}
              {previewTab === "terraform" && preview.terraformVars}
              {previewTab === "actions" && preview.githubActions}
              {previewTab === "yaml" && preview.deploymentYaml}
              {previewTab === "argo" && preview.argoApplication}
            </pre>
            <p className="text-xs text-[var(--muted)]">
              배포 시작을 누르면 Provision Job이 비동기로 실행됩니다.
              {githubConnected
                ? " GitHub에 Private Repository가 실제로 생성됩니다."
                : " GitHub 미연결 시 시뮬레이션만 수행됩니다. (설정에서 PAT 연결)"}
            </p>
          </div>
        )}

        {step === 5 && wizard && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">프로비저닝</h2>
            <p className="text-sm text-[var(--muted)]">
              {wizard.progressMessage ?? wizard.status}
            </p>
            <div className="h-3 overflow-hidden rounded-full bg-zinc-800">
              <div
                className="h-full rounded-full bg-[var(--primary)] transition-all duration-500"
                style={{ width: `${wizard.progress ?? 0}%` }}
              />
            </div>
            <p className="text-2xl font-semibold tabular-nums">
              {wizard.progress ?? 0}%
            </p>
            <ul className="space-y-2 text-sm text-[var(--muted)]">
              {[
                { label: "Repository / SCM", at: 20 },
                { label: "Helm · Terraform · Actions", at: 48 },
                { label: "로컬 Kubernetes 연결", at: 62 },
                { label: "Namespace · Deployment", at: 75 },
                { label: "Pod Ready / Health", at: 100 },
              ].map((item) => {
                const done = (wizard.progress ?? 0) >= item.at;
                return (
                  <li key={item.label} className={done ? "text-emerald-400" : ""}>
                    {done ? "✓" : "○"} {item.label}
                  </li>
                );
              })}
            </ul>
          </div>
        )}

        {step === 6 && (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="text-lg font-medium text-emerald-400">완료</h2>
              <p className="mt-2 text-sm text-[var(--muted)]">
                서비스{" "}
                <strong className="text-white">
                  {createdService?.name ?? wizard?.serviceName}
                </strong>
                가 생성되었습니다.
              </p>
              <p className="mt-1 text-xs text-[var(--muted)]">
                GitHub · Kubernetes · Pipeline · AI Review 를 서비스 상세에서 이어서 확인하세요.
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-3">
              <CompleteCard
                title="GitHub"
                ok={Boolean(createdService?.githubRepoUrl)}
                body={
                  createdService?.githubRepoUrl ? (
                    <a
                      href={createdService.githubRepoUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-[var(--primary)] hover:underline"
                    >
                      {createdService.githubOwner}/{createdService.githubRepoName}
                    </a>
                  ) : (
                    "미연결 / 시뮬레이션"
                  )
                }
              />
              <CompleteCard
                title="Kubernetes"
                ok={
                  createdService?.k8sStatus === "RUNNING" ||
                  createdService?.k8sStatus === "SIMULATED"
                }
                body={
                  createdService?.k8sNamespace
                    ? `${createdService.k8sNamespace} · ${createdService.k8sStatus}`
                    : "미배포 / 시뮬레이션"
                }
              />
              <CompleteCard
                title="환경"
                ok
                body={`${wizard?.environmentType ?? createdService?.environmentType ?? "—"} · replica ${
                  createdService?.replicaCount ?? wizard?.replicaCount ?? 1
                }`}
              />
            </div>

            <div className="flex flex-wrap justify-center gap-3">
              {wizard?.serviceId || createdService?.id ? (
                <Link
                  href={`/services/${createdService?.id ?? wizard?.serviceId}`}
                  className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)]"
                >
                  서비스 상세 보기
                </Link>
              ) : (
                <Link
                  href="/services"
                  className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white"
                >
                  서비스 목록
                </Link>
              )}
              <Link
                href="/pipelines"
                className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5"
              >
                파이프라인
              </Link>
              <Link
                href="/monitoring"
                className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5"
              >
                모니터링
              </Link>
              <Link
                href="/dashboard"
                className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5"
              >
                대시보드
              </Link>
            </div>
          </div>
        )}

        {step < 6 && step !== 5 && (
          <div className="mt-8 flex justify-between">
            <button
              type="button"
              disabled={step === 0 || loading}
              onClick={() => setStep((s) => Math.max(0, s - 1))}
              className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm disabled:opacity-40"
            >
              이전
            </button>
            <button
              type="button"
              disabled={loading || (step === 0 && !projectId)}
              onClick={goNext}
              className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
            >
              {loading
                ? "처리 중…"
                : step === 3
                  ? recommendation
                    ? "미리보기"
                    : "다음 (추천 권장)"
                  : step === 4
                    ? "배포 시작"
                    : "다음"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function CompleteCard({
  title,
  ok,
  body,
}: {
  title: string;
  ok: boolean;
  body: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-[var(--border)] bg-black/20 p-4 text-left">
      <div className="mb-2 flex items-center justify-between gap-2">
        <p className="text-xs text-[var(--muted)]">{title}</p>
        <span
          className={`text-xs ${ok ? "text-emerald-400" : "text-[var(--muted)]"}`}
        >
          {ok ? "OK" : "—"}
        </span>
      </div>
      <div className="text-sm">{body}</div>
    </div>
  );
}
