"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  createWizard,
  executeWizard,
  fetchCatalog,
  fetchProjects,
  fetchMe,
  getWizard,
  previewWizard,
  recommendWizard,
  updateWizard,
  type AiRecommendation,
  type CatalogTemplate,
  type Project,
  type Wizard,
  type WizardPreview,
} from "@/lib/api";

const STEPS = [
  "Service Info",
  "Template",
  "Infrastructure",
  "AI Review",
  "Preview",
  "Provision",
  "Complete",
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
  const [previewTab, setPreviewTab] = useState<
    "structure" | "blueprint" | "helm" | "terraform" | "actions" | "yaml" | "argo"
  >("structure");

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      if (!ws) return;
      const [p, c] = await Promise.all([fetchProjects(ws), fetchCatalog()]);
      if (p.success && p.data) {
        setProjects(p.data);
        if (p.data[0]) setProjectId(p.data[0].id);
      }
      if (c.success && c.data) {
        setTemplates(c.data);
        const spring = c.data.find((t) => t.runtime === "SPRING_BOOT") ?? c.data[0];
        if (spring) setTemplateId(spring.id);
      }
    });
  }, []);

  // poll provision progress
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
      setError(res.error?.message ?? "Wizard 생성 실패");
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
          setError(v.error?.message ?? "Preview 실패");
          return;
        }
        setPreview(v.data);
        setStep(5);
        const exec = await executeWizard(wizard.id);
        if (!exec.success) {
          setError(exec.error?.message ?? "Deploy 실패");
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
        setError(res.error?.message ?? "AI 추천 실패");
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
      setError(res.error?.message ?? "Preview 실패");
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-6 py-10">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold tracking-tight">Service Wizard</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">
          Catalog → AI → Preview → Provision · 시연 메인 플로우
        </p>
      </div>

      {/* steps */}
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
            <h2 className="text-lg font-medium">Service Info</h2>
            {projects.length === 0 ? (
              <p className="text-sm text-[var(--muted)]">
                프로젝트가 없습니다.{" "}
                <Link href="/projects" className="text-[var(--primary)] hover:underline">
                  Project 생성
                </Link>
              </p>
            ) : (
              <label className="block text-sm">
                <span className="mb-1 block text-[var(--muted)]">Project</span>
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
              <span className="mb-1 block text-[var(--muted)]">Service Name</span>
              <input
                className="w-full rounded-lg border border-[var(--border)] bg-black/30 px-3 py-2"
                value={serviceName}
                onChange={(e) => setServiceName(e.target.value)}
                minLength={3}
                maxLength={50}
              />
            </label>
          </div>
        )}

        {step === 1 && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">Template (Catalog)</h2>
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
            <h2 className="text-lg font-medium">Infrastructure / Environment</h2>
            <label className="block text-sm">
              <span className="mb-1 block text-[var(--muted)]">Environment</span>
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
              Runtime: {selectedTemplate?.runtime ?? wizard?.runtime ?? "—"} · Template:{" "}
              {selectedTemplate?.name ?? "—"}
            </p>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-medium">AI Recommendation</h2>
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
                AI Recommendation 버튼을 눌러 Platform Engineer 추천을 받으세요.
              </p>
            ) : (
              <div className="space-y-4">
                <p className="rounded-lg border border-[var(--border)] bg-black/20 p-4 text-sm">
                  {recommendation.summary}
                  <span className="ml-2 text-[var(--muted)]">
                    (score {recommendation.overallScore} · {recommendation.provider})
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
                          Confidence {item.confidence}%
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
                  Preview로 이동 →
                </button>
              </div>
            )}
          </div>
        )}

        {step === 4 && preview && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">Preview · {preview.repository}</h2>
            <div className="flex flex-wrap gap-2">
              {(
                [
                  "structure",
                  "blueprint",
                  "helm",
                  "terraform",
                  "actions",
                  "yaml",
                  "argo",
                ] as const
              ).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setPreviewTab(tab)}
                  className={`rounded-full px-3 py-1 text-xs ${
                    previewTab === tab
                      ? "bg-[var(--primary)] text-white"
                      : "border border-[var(--border)] text-[var(--muted)]"
                  }`}
                >
                  {tab}
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
              Deploy를 누르면 Provision Job이 비동기로 실행됩니다 (시연 Progress).
            </p>
          </div>
        )}

        {step === 5 && wizard && (
          <div className="space-y-4">
            <h2 className="text-lg font-medium">Provision</h2>
            <p className="text-sm text-[var(--muted)]">
              {wizard.progressMessage ?? wizard.status}
            </p>
            <div className="h-3 overflow-hidden rounded-full bg-zinc-800">
              <div
                className="h-full rounded-full bg-[var(--primary)] transition-all duration-500"
                style={{ width: `${wizard.progress ?? 0}%` }}
              />
            </div>
            <p className="text-2xl font-semibold tabular-nums">{wizard.progress ?? 0}%</p>
            <ul className="space-y-2 text-sm text-[var(--muted)]">
              {[
                "Repository 생성",
                "GitHub Actions 생성",
                "Helm 생성",
                "Terraform 생성",
                "ArgoCD 생성",
                "Deploy",
              ].map((label, i) => {
                const threshold = (i + 1) * 15;
                const done = (wizard.progress ?? 0) >= threshold;
                return (
                  <li key={label} className={done ? "text-emerald-400" : ""}>
                    {done ? "✓" : "○"} {label}
                  </li>
                );
              })}
            </ul>
          </div>
        )}

        {step === 6 && (
          <div className="space-y-4 text-center">
            <h2 className="text-lg font-medium text-emerald-400">Complete</h2>
            <p className="text-sm text-[var(--muted)]">
              Service <strong className="text-white">{wizard?.serviceName}</strong> 가 생성되었습니다.
            </p>
            <div className="flex justify-center gap-3">
              <Link
                href="/services"
                className="rounded-lg bg-[var(--primary)] px-4 py-2 text-sm font-medium text-white"
              >
                Services 보기
              </Link>
              <Link
                href="/dashboard"
                className="rounded-lg border border-[var(--border)] px-4 py-2 text-sm"
              >
                Dashboard
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
                    ? "Preview"
                    : "다음 (추천 권장)"
                  : step === 4
                    ? "Deploy"
                    : "다음"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
