"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  archiveServiceEnvironment,
  buildServiceDeploymentYaml,
  createEnvSecret,
  createEnvVariable,
  createServiceEnvironment,
  deleteEnvSecret,
  deleteEnvVariable,
  explainYaml,
  fetchArchitectureReview,
  fetchArgoSync,
  fetchEnvironmentHealth,
  fetchEnvSecrets,
  fetchEnvVariables,
  fetchK8sDeploymentByService,
  fetchPipelines,
  fetchService,
  fetchServiceDeployments,
  fetchServiceEnvironments,
  fetchServiceLogs,
  fetchServiceMetrics,
  fetchServicePromotions,
  fetchServiceTimeline,
  fetchMe,
  getWizard,
  promoteEnvironment,
  restoreServiceEnvironment,
  revealEnvSecret,
  rotateEnvSecret,
  syncEnvSecretsToGitHub,
  type AppService,
  type ArchitectureReview,
  type ArgoSyncStatus,
  type EnvSecret,
  type EnvVariable,
  type K8sDeployment,
  type LogLine,
  type Pipeline,
  type PromoteResult,
  type ServiceDeployment,
  type ServiceEnvironment,
  type ServiceMetrics,
  type TimelineItem,
  type YamlExplainResult,
} from "@/lib/api";
import {
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  ReadOnlyBanner,
  StatusBadge,
  SuccessBanner,
} from "@/components/ui";

function nextPromoteTarget(type: string): string | null {
  if (type === "DEV") return "STAGE";
  if (type === "STAGE") return "PRODUCTION";
  return null;
}

export default function ServiceDetailPage() {
  const params = useParams();
  const serviceId = String(params.serviceId ?? "");

  const [service, setService] = useState<AppService | null>(null);
  const [deploy, setDeploy] = useState<K8sDeployment | null>(null);
  const [metrics, setMetrics] = useState<ServiceMetrics | null>(null);
  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [review, setReview] = useState<ArchitectureReview | null>(null);
  const [yamlSource, setYamlSource] = useState("");
  const [yamlKind, setYamlKind] = useState("DEPLOYMENT");
  const [yamlExplain, setYamlExplain] = useState<YamlExplainResult | null>(null);
  const [explainLoading, setExplainLoading] = useState(false);
  const [environments, setEnvironments] = useState<ServiceEnvironment[]>([]);
  const [promotions, setPromotions] = useState<PromoteResult[]>([]);
  const [deployments, setDeployments] = useState<ServiceDeployment[]>([]);
  const [timeline, setTimeline] = useState<TimelineItem[]>([]);
  const [selectedEnvId, setSelectedEnvId] = useState<string | null>(null);
  const [variables, setVariables] = useState<EnvVariable[]>([]);
  const [secrets, setSecrets] = useState<EnvSecret[]>([]);
  const [varKey, setVarKey] = useState("");
  const [varValue, setVarValue] = useState("");
  const [secretKey, setSecretKey] = useState("");
  const [secretValue, setSecretValue] = useState("");
  const [revealMap, setRevealMap] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [reviewLoading, setReviewLoading] = useState(false);
  const [envBusy, setEnvBusy] = useState(false);
  const [newEnvType, setNewEnvType] = useState("STAGE");
  const [canMutate, setCanMutate] = useState(true);
  const [workspaceRole, setWorkspaceRole] = useState<string | null>(null);
  const [argo, setArgo] = useState<ArgoSyncStatus | null>(null);
  const [rotatedOnce, setRotatedOnce] = useState<Record<string, string>>({});

  const loadConfig = useCallback(async (envId: string) => {
    const [v, s] = await Promise.all([
      fetchEnvVariables(envId),
      fetchEnvSecrets(envId),
    ]);
    setVariables(v.success && v.data ? v.data : []);
    setSecrets(s.success && s.data ? s.data : []);
    setRevealMap({});
  }, []);

  const load = useCallback(async () => {
    if (!serviceId) return;
    setLoading(true);
    setError(null);
    try {
      const [me, s, d, m, p, l, envs, promo, deps, tl, ar] = await Promise.all([
        fetchMe(),
        fetchService(serviceId),
        fetchK8sDeploymentByService(serviceId),
        fetchServiceMetrics(serviceId),
        fetchPipelines({ serviceId }),
        fetchServiceLogs(serviceId, 30),
        fetchServiceEnvironments(serviceId),
        fetchServicePromotions(serviceId),
        fetchServiceDeployments(serviceId),
        fetchServiceTimeline(serviceId),
        fetchArgoSync(serviceId),
      ]);
      setCanMutate(me.data?.canMutate !== false);
      setWorkspaceRole(me.data?.workspaceRole ?? null);
      setArgo(ar.success ? (ar.data ?? null) : null);
      if (!s.success || !s.data) {
        setError(s.error?.message ?? "서비스를 찾을 수 없습니다.");
        setService(null);
        return;
      }
      setService(s.data);
      setDeploy(d.success ? (d.data ?? null) : null);
      setMetrics(m.success ? (m.data ?? null) : null);
      setPipelines(p.success && p.data ? p.data : []);
      setLogs(l.success && l.data ? l.data.lines : []);
      const envItems = envs.success && envs.data ? envs.data.items : [];
      setEnvironments(envItems);
      setPromotions(promo.success && promo.data ? promo.data.items : []);
      setDeployments(deps.success && deps.data ? deps.data.items : []);
      setTimeline(tl.success && tl.data ? tl.data.items : []);
      setSelectedEnvId((prev) => {
        if (prev && envItems.some((e) => e.id === prev)) return prev;
        return envItems[0]?.id ?? null;
      });

      // Wizard Preview YAML 우선, 없으면 서비스 메타 기반 스켈레톤
      let yaml = buildServiceDeploymentYaml(s.data);
      let kind = "DEPLOYMENT";
      if (s.data.wizardId) {
        const w = await getWizard(s.data.wizardId);
        if (w.success && w.data?.preview?.deploymentYaml) {
          yaml = w.data.preview.deploymentYaml;
          kind = "DEPLOYMENT";
        } else if (w.success && w.data?.preview?.helmValues) {
          yaml = w.data.preview.helmValues;
          kind = "HELM";
        } else if (w.success && w.data?.preview?.argoApplication) {
          yaml = w.data.preview.argoApplication;
          kind = "ARGO";
        }
      }
      setYamlSource(yaml);
      setYamlKind(kind);
      setYamlExplain(null);
    } catch {
      setError("서비스 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [serviceId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (selectedEnvId) {
      loadConfig(selectedEnvId);
    } else {
      setVariables([]);
      setSecrets([]);
    }
  }, [selectedEnvId, loadConfig]);

  async function runArchitectureReview() {
    if (!service?.wizardId) {
      setError("이 서비스에 연결된 Wizard가 없어 Architecture Review를 실행할 수 없습니다.");
      return;
    }
    setReviewLoading(true);
    setError(null);
    const res = await fetchArchitectureReview(service.wizardId);
    setReviewLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "Architecture Review 실패");
      return;
    }
    setReview(res.data);
  }

  async function runYamlExplain() {
    if (!yamlSource.trim()) {
      setError("Explain 할 YAML이 없습니다.");
      return;
    }
    setExplainLoading(true);
    setError(null);
    const res = await explainYaml({
      content: yamlSource,
      kind: yamlKind,
      serviceName: service?.name,
      environmentType: service?.environmentType,
    });
    setExplainLoading(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "YAML Explain 실패");
      return;
    }
    setYamlExplain(res.data);
  }

  async function addEnvironment() {
    if (!serviceId) return;
    setEnvBusy(true);
    setError(null);
    setMessage(null);
    const res = await createServiceEnvironment(serviceId, { type: envTypeToCreate });
    setEnvBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "환경 생성에 실패했습니다.");
      return;
    }
    await load();
  }

  async function checkEnvHealth(envId: string) {
    setEnvBusy(true);
    setError(null);
    const res = await fetchEnvironmentHealth(envId);
    setEnvBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "헬스 체크에 실패했습니다.");
      return;
    }
    await load();
  }

  async function toggleArchive(env: ServiceEnvironment) {
    setEnvBusy(true);
    setError(null);
    const res =
      env.status === "ARCHIVED"
        ? await restoreServiceEnvironment(env.id)
        : await archiveServiceEnvironment(env.id);
    setEnvBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "상태 변경에 실패했습니다.");
      return;
    }
    await load();
  }

  async function onPromote(env: ServiceEnvironment) {
    const target = nextPromoteTarget(env.type);
    if (!target) return;
    setEnvBusy(true);
    setError(null);
    setMessage(null);
    const res = await promoteEnvironment(env.id, target);
    setEnvBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "환경 승격에 실패했습니다.");
      return;
    }
    setMessage(
      `${res.data.sourceType} → ${res.data.targetType} 승격 완료 (변수 ${res.data.variablesCopied}, 시크릿 ${res.data.secretsCopied})`,
    );
    await load();
  }

  async function addVariable() {
    if (!selectedEnvId || !varKey.trim()) return;
    setEnvBusy(true);
    setError(null);
    const res = await createEnvVariable(selectedEnvId, {
      key: varKey.trim().toUpperCase(),
      value: varValue,
    });
    setEnvBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "변수 생성에 실패했습니다.");
      return;
    }
    setVarKey("");
    setVarValue("");
    await loadConfig(selectedEnvId);
  }

  async function addSecret() {
    if (!selectedEnvId || !secretKey.trim() || !secretValue) return;
    setEnvBusy(true);
    setError(null);
    const res = await createEnvSecret(selectedEnvId, {
      key: secretKey.trim().toUpperCase(),
      value: secretValue,
    });
    setEnvBusy(false);
    if (!res.success) {
      setError(res.error?.message ?? "시크릿 생성에 실패했습니다.");
      return;
    }
    setSecretKey("");
    setSecretValue("");
    await loadConfig(selectedEnvId);
  }

  async function onRevealSecret(id: string) {
    setEnvBusy(true);
    setError(null);
    const res = await revealEnvSecret(id);
    setEnvBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "시크릿 조회에 실패했습니다.");
      return;
    }
    setRevealMap((prev) => ({ ...prev, [id]: res.data!.value }));
  }

  async function onDeleteVariable(id: string) {
    if (!selectedEnvId) return;
    setEnvBusy(true);
    await deleteEnvVariable(id);
    setEnvBusy(false);
    await loadConfig(selectedEnvId);
  }

  async function onDeleteSecret(id: string) {
    if (!selectedEnvId) return;
    setEnvBusy(true);
    await deleteEnvSecret(id);
    setEnvBusy(false);
    await loadConfig(selectedEnvId);
  }

  async function onRotateSecret(id: string) {
    if (!selectedEnvId) return;
    setEnvBusy(true);
    setError(null);
    setMessage(null);
    const res = await rotateEnvSecret(id, { generateRandom: true });
    setEnvBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "시크릿 로테이션 실패");
      return;
    }
    setMessage(
      `시크릿 로테이션 · ${res.data.key} · v${res.data.version}` +
        (res.data.generated ? " (자동 생성)" : ""),
    );
    if (res.data.plainValueOnce) {
      setRotatedOnce((prev) => ({
        ...prev,
        [id]: res.data!.plainValueOnce!,
      }));
      setRevealMap((prev) => ({
        ...prev,
        [id]: res.data!.plainValueOnce!,
      }));
    }
    await loadConfig(selectedEnvId);
  }

  async function onSyncGitHubSecrets() {
    if (!selectedEnvId) return;
    setEnvBusy(true);
    setError(null);
    setMessage(null);
    const res = await syncEnvSecretsToGitHub(selectedEnvId);
    setEnvBusy(false);
    if (!res.success || !res.data) {
      setError(res.error?.message ?? "GitHub 시크릿 동기화 실패");
      return;
    }
    setMessage(
      `GitHub Secret sync · ${res.data.mode} · 성공 ${res.data.succeeded}/${res.data.attempted}` +
        (res.data.repository ? ` · ${res.data.repository}` : ""),
    );
    await loadConfig(selectedEnvId);
  }

  const missingEnvTypes = ["DEV", "STAGE", "PRODUCTION"].filter(
    (t) => !environments.some((e) => e.type === t && e.status !== "ARCHIVED"),
  );
  const envTypeToCreate = missingEnvTypes.includes(newEnvType)
    ? newEnvType
    : (missingEnvTypes[0] ?? "STAGE");
  const selectedEnv =
    environments.find((e) => e.id === selectedEnvId) ?? null;

  if (loading) {
    return <LoadingBlock label="서비스 상세를 불러오는 중…" />;
  }

  if (!service) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        {error ? <ErrorBanner message={error} onRetry={load} /> : null}
        <EmptyState
          title="서비스를 찾을 수 없습니다"
          description="목록으로 돌아가 다시 선택해 주세요."
          action={
            <Link href="/services" className="nimbus-btn-primary">
              서비스 목록
            </Link>
          }
        />
      </div>
    );
  }

  const latestPipeline = pipelines[0];

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      {/* Header */}
      <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2 text-xs text-[var(--muted)]">
            <Link href="/services" className="hover:text-white">
              서비스
            </Link>
            <span>/</span>
            <span>{service.name}</span>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{service.name}</h1>
            <StatusBadge value={service.status} />
          </div>
          <p className="mt-2 text-sm text-[var(--muted)]">
            {service.runtime} · {service.environmentType}
            {service.databaseType ? ` · DB ${service.databaseType}` : ""}
            {service.cacheType ? ` · Cache ${service.cacheType}` : ""}
            {" · "}레플리카 {service.replicaCount ?? 1}
            {service.hpaEnabled ? " · HPA" : ""}
          </p>
          {service.description && (
            <p className="mt-1 text-sm text-[var(--muted)]">{service.description}</p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href={`/logs?serviceId=${service.id}`}
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            로그
          </Link>
          <Link
            href="/pipelines"
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            파이프라인
          </Link>
          <Link
            href="/monitoring"
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            모니터링
          </Link>
          <button
            type="button"
            onClick={load}
            className="rounded-lg border border-[var(--border)] px-3 py-2 text-sm hover:bg-white/5"
          >
            새로고침
          </button>
        </div>
      </div>

      {error ? <ErrorBanner message={error} /> : null}
      {message ? <SuccessBanner message={message} /> : null}
      {!canMutate ? <ReadOnlyBanner role={workspaceRole} /> : null}

      {/* Environments — Sprint A + B */}
      <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-medium">Environments</h2>
            <p className="mt-0.5 text-xs text-[var(--muted)]">
              DEV → STAGE → PRODUCTION · 변수/시크릿 · 승격(Promote)
            </p>
          </div>
          {canMutate && missingEnvTypes.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <select
                className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 text-xs"
                value={envTypeToCreate}
                onChange={(e) => setNewEnvType(e.target.value)}
              >
                {missingEnvTypes.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={envBusy}
                onClick={addEnvironment}
                className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
              >
                환경 추가
              </button>
            </div>
          )}
        </div>
        {environments.length === 0 ? (
          <EmptyState
            title="아직 환경이 없습니다"
            description="프로비저닝 후 다시 열거나, 위에서 환경을 추가하세요."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-sm">
              <thead className="border-b border-[var(--border)] text-xs uppercase tracking-wide text-[var(--muted)]">
                <tr>
                  <th className="px-2 py-2 font-medium">유형</th>
                  <th className="px-2 py-2 font-medium">상태</th>
                  <th className="px-2 py-2 font-medium">네임스페이스</th>
                  <th className="px-2 py-2 font-medium">브랜치</th>
                  <th className="px-2 py-2 font-medium">레플리카</th>
                  <th className="px-2 py-2 font-medium">헬스</th>
                  <th className="px-2 py-2 font-medium">작업</th>
                </tr>
              </thead>
              <tbody>
                {environments.map((env) => {
                  const target = nextPromoteTarget(env.type);
                  const active = env.id === selectedEnvId;
                  return (
                    <tr
                      key={env.id}
                      className={`border-b border-[var(--border)]/50 last:border-0 ${
                        active ? "bg-[var(--primary)]/10" : "hover:bg-white/[0.03]"
                      }`}
                    >
                      <td className="px-2 py-2.5">
                        <button
                          type="button"
                          className="font-medium hover:underline"
                          onClick={() => setSelectedEnvId(env.id)}
                        >
                          {env.type}
                        </button>
                      </td>
                      <td className="px-2 py-2.5">
                        <StatusBadge value={env.status} />
                      </td>
                      <td className="px-2 py-2.5 font-mono text-xs">
                        {env.namespace}
                        {env.clusterLabel ? (
                          <span className="mt-0.5 block text-[var(--muted)]">
                            {env.clusterLabel}
                          </span>
                        ) : null}
                      </td>
                      <td className="px-2 py-2.5 text-xs text-[var(--muted)]">
                        {env.gitOpsBranch ?? "—"}
                      </td>
                      <td className="px-2 py-2.5 tabular-nums">
                        {env.replicaCount ?? 1}
                        {env.hpaEnabled ? (
                          <span className="ml-1 text-xs text-[var(--muted)]">HPA</span>
                        ) : null}
                      </td>
                      <td className="px-2 py-2.5">
                        <StatusBadge value={env.healthStatus} />
                      </td>
                      <td className="px-2 py-2.5">
                        <div className="flex flex-wrap gap-1.5">
                          <button
                            type="button"
                            disabled={envBusy}
                            onClick={() => setSelectedEnvId(env.id)}
                            className="rounded border border-[var(--border)] px-2 py-0.5 text-[11px] hover:bg-white/5"
                          >
                            설정
                          </button>
                          {canMutate && target && env.status !== "ARCHIVED" && (
                            <button
                              type="button"
                              disabled={envBusy}
                              onClick={() => onPromote(env)}
                              className="rounded border border-sky-500/40 px-2 py-0.5 text-[11px] text-sky-300 hover:bg-sky-500/10 disabled:opacity-50"
                            >
                              승격 → {target}
                            </button>
                          )}
                          <button
                            type="button"
                            disabled={envBusy}
                            onClick={() => checkEnvHealth(env.id)}
                            className="rounded border border-[var(--border)] px-2 py-0.5 text-[11px] hover:bg-white/5 disabled:opacity-50"
                          >
                            헬스
                          </button>
                          {canMutate ? (
                            <button
                              type="button"
                              disabled={envBusy}
                              onClick={() => toggleArchive(env)}
                              className="rounded border border-[var(--border)] px-2 py-0.5 text-[11px] hover:bg-white/5 disabled:opacity-50"
                            >
                              {env.status === "ARCHIVED" ? "복원" : "보관"}
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Config panel for selected env */}
        {selectedEnv && (
          <div className="mt-5 grid gap-4 border-t border-[var(--border)] pt-5 lg:grid-cols-2">
            <div>
              <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--muted)]">
                변수 · {selectedEnv.type}
              </h3>
              {canMutate ? (
                <div className="mb-2 flex flex-wrap gap-2">
                  <input
                    className="min-w-[120px] flex-1 rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 font-mono text-xs"
                    placeholder="KEY"
                    value={varKey}
                    onChange={(e) => setVarKey(e.target.value)}
                  />
                  <input
                    className="min-w-[120px] flex-1 rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 text-xs"
                    placeholder="값"
                    value={varValue}
                    onChange={(e) => setVarValue(e.target.value)}
                  />
                  <button
                    type="button"
                    disabled={envBusy}
                    onClick={addVariable}
                    className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs text-white disabled:opacity-60"
                  >
                    추가
                  </button>
                </div>
              ) : null}
              <ul className="divide-y divide-[var(--border)] rounded-lg border border-[var(--border)] text-xs">
                {variables.length === 0 ? (
                  <li className="px-3 py-3 text-[var(--muted)]">변수가 없습니다</li>
                ) : (
                  variables.map((v) => (
                    <li key={v.id} className="flex items-center justify-between gap-2 px-3 py-2">
                      <span>
                        <span className="font-mono text-sky-300">{v.key}</span>
                        <span className="mx-2 text-[var(--muted)]">=</span>
                        <span>{v.value}</span>
                      </span>
                      {canMutate ? (
                        <button
                          type="button"
                          className="text-[var(--muted)] hover:text-red-300"
                          onClick={() => onDeleteVariable(v.id)}
                        >
                          삭제
                        </button>
                      ) : null}
                    </li>
                  ))
                )}
              </ul>
            </div>
            <div>
              <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--muted)]">
                시크릿 · {selectedEnv.type}
              </h3>
              {canMutate ? (
                <div className="mb-2 flex flex-wrap gap-2">
                  <input
                    className="min-w-[120px] flex-1 rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 font-mono text-xs"
                    placeholder="SECRET_KEY"
                    value={secretKey}
                    onChange={(e) => setSecretKey(e.target.value)}
                  />
                  <input
                    type="password"
                    className="min-w-[120px] flex-1 rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 text-xs"
                    placeholder="값"
                    value={secretValue}
                    onChange={(e) => setSecretValue(e.target.value)}
                  />
                  <button
                    type="button"
                    disabled={envBusy}
                    onClick={addSecret}
                    className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs text-white disabled:opacity-60"
                  >
                    추가
                  </button>
                </div>
              ) : null}
              <ul className="divide-y divide-[var(--border)] rounded-lg border border-[var(--border)] text-xs">
                {secrets.length === 0 ? (
                  <li className="px-3 py-3 text-[var(--muted)]">시크릿이 없습니다</li>
                ) : (
                  secrets.map((s) => (
                    <li key={s.id} className="flex items-center justify-between gap-2 px-3 py-2">
                      <span>
                        <span className="font-mono text-amber-300">{s.key}</span>
                        <span className="mx-2 text-[var(--muted)]">=</span>
                        <span className="font-mono">
                          {revealMap[s.id] ?? s.maskedValue}
                        </span>
                      </span>
                      {canMutate ? (
                        <span className="flex flex-wrap gap-2">
                          <span className="text-[10px] text-[var(--muted)]">
                            v{s.version ?? 1}
                          </span>
                          {!revealMap[s.id] && (
                            <button
                              type="button"
                              className="text-[var(--muted)] hover:text-white"
                              onClick={() => onRevealSecret(s.id)}
                            >
                              보기
                            </button>
                          )}
                          <button
                            type="button"
                            className="text-sky-300/90 hover:underline"
                            onClick={() => onRotateSecret(s.id)}
                          >
                            로테이션
                          </button>
                          <button
                            type="button"
                            className="text-[var(--muted)] hover:text-red-300"
                            onClick={() => onDeleteSecret(s.id)}
                          >
                            삭제
                          </button>
                        </span>
                      ) : null}
                    </li>
                  ))
                )}
              </ul>
              {Object.keys(rotatedOnce).length > 0 ? (
                <p className="mt-2 text-[10px] text-amber-200/90">
                  로테이션으로 생성된 값은 이 화면에서 한 번만 표시됩니다. 안전한 곳에
                  복사하세요.
                </p>
              ) : null}
              <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                <p className="text-[10px] text-[var(--muted)]">
                  AES 저장 · 마스킹 · 로테이션(v++) · 보기 시 감사 기록
                </p>
                {canMutate ? (
                  <button
                    type="button"
                    disabled={envBusy || secrets.length === 0}
                    onClick={onSyncGitHubSecrets}
                    className="rounded border border-[var(--border)] px-2 py-0.5 text-[11px] hover:bg-white/5 disabled:opacity-50"
                  >
                    GitHub Secret 동기화
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        )}

        {promotions.length > 0 && (
          <div className="mt-5 border-t border-[var(--border)] pt-4">
            <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--muted)]">
              승격 이력
            </h3>
            <ul className="space-y-1 text-xs text-[var(--muted)]">
              {promotions.slice(0, 5).map((p) => (
                <li key={p.promotionId}>
                  <span className="text-white">
                    {p.sourceType} → {p.targetType}
                  </span>
                  {" · "}
                  {p.status}
                  {" · "}
                  변수 {p.variablesCopied} / 시크릿 {p.secretsCopied}
                  {p.gitOpsMode ? (
                    <>
                      {" · "}
                      <span className="text-sky-300/90">
                        GitOps {p.gitOpsMode}
                      </span>
                      {p.gitOpsHeadBranch && p.gitOpsBaseBranch
                        ? ` (${p.gitOpsHeadBranch} → ${p.gitOpsBaseBranch})`
                        : ""}
                      {p.pullRequestUrl ? (
                        <>
                          {" · "}
                          <a
                            href={p.pullRequestUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-[var(--primary)] hover:underline"
                          >
                            PR
                            {p.pullRequestNumber
                              ? ` #${p.pullRequestNumber}`
                              : ""}
                          </a>
                        </>
                      ) : null}
                    </>
                  ) : null}
                  {p.finishedAt
                    ? ` · ${new Date(p.finishedAt).toLocaleString("ko-KR")}`
                    : ""}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>

      {/* ArgoCD thin */}
      <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-medium">ArgoCD Sync</h2>
            <p className="mt-0.5 text-xs text-[var(--muted)]">
              Application CR 조회(있으면 LIVE) · 없으면 매니페스트 SIMULATED
            </p>
          </div>
          {argo ? <StatusBadge value={argo.mode} /> : null}
        </div>
        {!argo ? (
          <p className="text-sm text-[var(--muted)]">Argo 상태를 불러오지 못했습니다.</p>
        ) : (
          <div className="space-y-3 text-sm">
            <ul className="grid gap-2 sm:grid-cols-2">
              <li className="text-[var(--muted)]">
                Sync ·{" "}
                <span className="text-zinc-200">{argo.syncStatus ?? "—"}</span>
              </li>
              <li className="text-[var(--muted)]">
                Health ·{" "}
                <span className="text-zinc-200">{argo.healthStatus ?? "—"}</span>
              </li>
              <li className="text-[var(--muted)]">
                App ·{" "}
                <span className="text-zinc-200">
                  {argo.applicationName ?? "—"}
                </span>
              </li>
              <li className="text-[var(--muted)]">
                NS ·{" "}
                <span className="text-zinc-200">{argo.namespace ?? "—"}</span>
              </li>
            </ul>
            <p className="text-xs text-[var(--muted)]">{argo.message}</p>
            {argo.applicationManifest ? (
              <pre className="max-h-48 overflow-auto whitespace-pre-wrap rounded-lg border border-[var(--border)] bg-black/30 p-3 font-mono text-[10px] text-zinc-300">
                {argo.applicationManifest}
              </pre>
            ) : null}
          </div>
        )}
      </section>

      {/* Deployment timeline — Sprint D */}
      <section className="mb-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <h2 className="mb-1 text-sm font-medium">배포 이력 · 타임라인</h2>
        <p className="mb-4 text-xs text-[var(--muted)]">
          Wizard 프로비저닝 · 환경 승격 등 플랫폼 이벤트를 시간순으로 표시합니다.
        </p>
        {timeline.length === 0 && deployments.length === 0 ? (
          <p className="text-sm text-[var(--muted)]">
            아직 배포 이력이 없습니다. Wizard 완료 또는 Promote 후 여기에 기록됩니다.
          </p>
        ) : (
          <ul className="space-y-0 border-l border-[var(--border)] pl-4">
            {(timeline.length > 0
              ? timeline
              : deployments.map((d) => ({
                  kind: "DEPLOYMENT",
                  id: d.id,
                  at: d.finishedAt ?? d.createdAt,
                  title: `${d.trigger} · ${d.environmentType ?? "—"}`,
                  detail: d.message ?? d.versionLabel,
                  status: d.status,
                }))
            ).slice(0, 12).map((item) => (
              <li key={`${item.kind}-${item.id}`} className="relative pb-4 last:pb-0">
                <span className="absolute -left-[1.15rem] top-1 h-2 w-2 rounded-full bg-[var(--primary)]" />
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-[10px] uppercase tracking-wide text-[var(--muted)]">
                    {item.kind}
                  </span>
                  {item.status ? <StatusBadge value={item.status} /> : null}
                  <span className="text-[11px] text-[var(--muted)]">
                    {item.at
                      ? new Date(item.at).toLocaleString("ko-KR")
                      : "—"}
                  </span>
                </div>
                <p className="mt-0.5 text-sm text-zinc-100">{item.title}</p>
                {item.detail ? (
                  <p className="text-xs text-[var(--muted)]">{item.detail}</p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Summary cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          title="GitHub"
          status={service.githubRepoUrl ? "CONNECTED" : "—"}
          body={
            service.githubRepoUrl ? (
              <a
                href={service.githubRepoUrl}
                target="_blank"
                rel="noreferrer"
                className="text-[var(--primary)] hover:underline"
              >
                {service.githubOwner}/{service.githubRepoName}
              </a>
            ) : (
              <span className="text-[var(--muted)]">미연결 / 시뮬레이션</span>
            )
          }
        />
        <SummaryCard
          title="Kubernetes"
          status={service.k8sStatus ?? "—"}
          body={
            service.k8sNamespace ? (
              <span>
                {service.k8sNamespace}/{service.k8sDeployment}
                {service.k8sClusterType ? (
                  <span className="block text-[var(--muted)]">
                    {service.k8sClusterType}
                  </span>
                ) : null}
              </span>
            ) : (
              <span className="text-[var(--muted)]">미배포</span>
            )
          }
        />
        <SummaryCard
          title="Pipeline"
          status={latestPipeline?.status ?? "—"}
          body={
            latestPipeline ? (
              <span>
                {latestPipeline.progress ?? 0}% · {latestPipeline.currentStep}
                {latestPipeline.imageTag && (
                  <span className="mt-1 block text-sky-400">{latestPipeline.imageTag}</span>
                )}
              </span>
            ) : (
              <span className="text-[var(--muted)]">실행 이력 없음</span>
            )
          }
        />
        <SummaryCard
          title="Metrics"
          status={metrics?.source ?? "—"}
          body={
            metrics ? (
              <span className="flex flex-wrap gap-2">
                {metrics.metrics.slice(0, 4).map((m) => (
                  <span key={m.name} className="rounded border border-[var(--border)] px-1.5 py-0.5">
                    {m.name} {m.value}
                    {m.unit === "%" ? "%" : ""}
                  </span>
                ))}
              </span>
            ) : (
              <span className="text-[var(--muted)]">—</span>
            )
          }
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* K8s detail */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <h2 className="mb-3 text-sm font-medium">배포 / Pod</h2>
          {!deploy ? (
            <p className="text-sm text-[var(--muted)]">
              K8s 배포 기록이 없습니다. kind 연결 후 Wizard Deploy 시 생성됩니다.
            </p>
          ) : (
            <div className="space-y-3 text-sm">
              <div className="flex flex-wrap gap-2">
                <StatusBadge value={deploy.status} />
                <span className="text-[var(--muted)]">
                  Ready {deploy.readyReplicas ?? 0}/{deploy.replicas ?? 0}
                </span>
              </div>
              <p className="text-xs text-[var(--muted)]">
                image: {deploy.image} · {deploy.clusterType} · {deploy.clusterContext}
              </p>
              {deploy.message && (
                <p className="text-xs text-[var(--muted)]">{deploy.message}</p>
              )}
              {deploy.pods && deploy.pods.length > 0 ? (
                <ul className="space-y-1 text-xs">
                  {deploy.pods.map((pod) => (
                    <li key={pod.name} className="flex justify-between gap-2">
                      <span>
                        {pod.ready ? "●" : "○"} {pod.name}
                      </span>
                      <span className="text-[var(--muted)]">
                        {pod.phase}
                        {pod.restarts > 0 ? ` · rst ${pod.restarts}` : ""}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-xs text-[var(--muted)]">Pod 목록 없음 (sim 또는 미연결)</p>
              )}
              <Link
                href="/infrastructure"
                className="inline-block text-xs text-[var(--primary)] hover:underline"
              >
                인프라 페이지 →
              </Link>
            </div>
          )}
        </section>

        {/* Pipeline */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium">이미지 빌드 파이프라인</h2>
            <Link href="/pipelines" className="text-xs text-[var(--primary)] hover:underline">
              전체 보기
            </Link>
          </div>
          {pipelines.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">파이프라인 이력이 없습니다.</p>
          ) : (
            <ul className="divide-y divide-[var(--border)] text-sm">
              {pipelines.slice(0, 5).map((p) => (
                <li key={p.id} className="flex justify-between gap-3 py-2">
                  <div>
                    <p className="text-xs text-[var(--muted)]">{p.name}</p>
                    <p className="text-xs">{p.currentStep}</p>
                  </div>
                  <div className="text-right">
                    <StatusBadge value={p.status} />
                    <p className="mt-1 text-xs text-[var(--muted)]">{p.progress ?? 0}%</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {/* Logs preview */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium">최근 로그</h2>
            <Link
              href={`/logs?serviceId=${service.id}`}
              className="text-xs text-[var(--primary)] hover:underline"
            >
              전체 / 스트림 →
            </Link>
          </div>
          <div className="max-h-56 overflow-auto rounded-lg border border-[var(--border)] bg-black/40 p-3 font-mono text-[11px] leading-relaxed">
            {logs.length === 0 ? (
              <p className="text-[var(--muted)]">로그 없음</p>
            ) : (
              logs.slice(-20).map((l, i) => (
                <div key={`${l.timestamp}-${i}`} className="text-zinc-300">
                  <span className="text-zinc-500">
                    {new Date(l.timestamp).toLocaleTimeString()}
                  </span>{" "}
                  <span className="text-emerald-500">{l.level}</span> {l.message}
                </div>
              ))
            )}
          </div>
        </section>

        {/* AI Architecture Review */}
        <section className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
          <div className="mb-3 flex items-center justify-between gap-2">
            <h2 className="text-sm font-medium">AI Architecture Review</h2>
            <button
              type="button"
              onClick={runArchitectureReview}
              disabled={reviewLoading || !service.wizardId}
              className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
            >
              {reviewLoading ? "분석 중…" : "분석 실행"}
            </button>
          </div>
          {!review ? (
            <p className="text-sm text-[var(--muted)]">
              분석을 실행하면 Architecture Score와 권장 사항을 확인할 수 있습니다.
            </p>
          ) : (
            <div className="space-y-3 text-sm">
              <p className="text-3xl font-semibold tabular-nums text-emerald-400">
                {review.score}
                <span className="ml-1 text-sm font-normal text-[var(--muted)]">/ 100</span>
              </p>
              <p className="text-xs text-[var(--muted)]">provider: {review.provider}</p>
              {review.strengths?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-emerald-400">강점</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.strengths.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
              {review.risks?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-amber-400">리스크</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.risks.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
              {review.recommendations?.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-medium text-sky-400">추천</p>
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                    {review.recommendations.map((s) => (
                      <li key={s}>{s}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </section>
      </div>

      {/* YAML Explain — Service Detail 재사용 */}
      <section className="mt-6 rounded-xl border border-[var(--border)] bg-[var(--card)] p-5">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-medium">YAML Explain</h2>
            <p className="mt-0.5 text-xs text-[var(--muted)]">
              Wizard Preview YAML이 있으면 우선 사용하고, 없으면 서비스 메타로 Deployment
              스켈레톤을 만듭니다. rule-engine 설명.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <select
              className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1.5 text-xs"
              value={yamlKind}
              onChange={(e) => setYamlKind(e.target.value)}
            >
              {[
                "AUTO",
                "DEPLOYMENT",
                "HELM",
                "ARGO",
                "ACTIONS",
                "SERVICE",
                "GENERIC",
              ].map((k) => (
                <option key={k} value={k}>
                  {k}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={runYamlExplain}
              disabled={explainLoading || !yamlSource.trim()}
              className="rounded-lg bg-[var(--primary)] px-3 py-1.5 text-xs font-medium text-white hover:bg-[var(--primary-hover)] disabled:opacity-60"
            >
              {explainLoading ? "분석 중…" : "Explain 실행"}
            </button>
          </div>
        </div>
        <textarea
          className="nimbus-input min-h-[160px] font-mono text-[11px] leading-relaxed"
          value={yamlSource}
          onChange={(e) => setYamlSource(e.target.value)}
          spellCheck={false}
        />
        {yamlExplain && (
          <div className="mt-4 rounded-xl border border-[var(--border)] bg-black/25 p-4">
            <p className="text-sm text-zinc-100">{yamlExplain.summary}</p>
            <p className="mt-1 text-[11px] text-[var(--muted)]">
              kind: {yamlExplain.detectedKind} · provider: {yamlExplain.provider}
            </p>
            <ul className="mt-3 space-y-2">
              {yamlExplain.highlights.map((h) => (
                <li
                  key={`${h.path}-${h.title}`}
                  className="rounded-lg border border-[var(--border)] px-3 py-2"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={`text-[10px] font-medium uppercase ${
                        h.level === "WARN"
                          ? "text-amber-300"
                          : h.level === "ERROR"
                            ? "text-red-300"
                            : "text-sky-300"
                      }`}
                    >
                      {h.level}
                    </span>
                    <span className="text-sm font-medium text-zinc-100">{h.title}</span>
                    <span className="font-mono text-[10px] text-[var(--muted)]">
                      {h.path}
                    </span>
                  </div>
                  <p className="mt-1 text-xs leading-relaxed text-[var(--muted)]">
                    {h.explanation}
                  </p>
                </li>
              ))}
            </ul>
            {yamlExplain.risks?.length > 0 && (
              <div className="mt-3">
                <p className="text-xs font-medium text-amber-300">리스크</p>
                <ul className="mt-1 list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                  {yamlExplain.risks.map((r) => (
                    <li key={r}>{r}</li>
                  ))}
                </ul>
              </div>
            )}
            {yamlExplain.suggestions?.length > 0 && (
              <div className="mt-3">
                <p className="text-xs font-medium text-emerald-300">제안</p>
                <ul className="mt-1 list-disc space-y-0.5 pl-4 text-xs text-[var(--muted)]">
                  {yamlExplain.suggestions.map((r) => (
                    <li key={r}>{r}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}

function SummaryCard({
  title,
  status,
  body,
}: {
  title: string;
  status: string;
  body: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-4">
      <div className="mb-2 flex items-center justify-between gap-2">
        <p className="text-xs text-[var(--muted)]">{title}</p>
        <StatusBadge value={status} />
      </div>
      <div className="text-sm">{body}</div>
    </div>
  );
}
