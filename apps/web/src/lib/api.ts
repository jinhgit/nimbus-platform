const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: { code: string; message: string };
};

export type UserSummary = {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  role: string;
  workspaceId?: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserSummary;
};

export type MeResponse = {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  role: string;
  workspace?: { id: string; name: string; slug: string };
};

export type WorkspaceSummary = {
  id: string;
  name: string;
  slug: string;
  myRole: string;
};

export type Project = {
  id: string;
  name: string;
  description?: string;
  status: string;
  visibility: string;
  workspaceId: string;
  teamId?: string;
  ownerId: string;
  createdAt?: string;
  updatedAt?: string;
};

export type CatalogTemplate = {
  id: string;
  name: string;
  description?: string;
  type: string;
  runtime: string;
  language: string;
  latestVersion: string;
  official: boolean;
  status: string;
  tags?: string;
};

export type AppService = {
  id: string;
  name: string;
  description?: string;
  runtime: string;
  status: string;
  environmentType: string;
  replicaCount?: number;
  databaseType?: string;
  cacheType?: string;
  hpaEnabled?: boolean;
  projectId: string;
  workspaceId: string;
  templateId?: string;
  wizardId?: string;
  githubRepoUrl?: string;
  githubOwner?: string;
  githubRepoName?: string;
  k8sNamespace?: string;
  k8sDeployment?: string;
  k8sStatus?: string;
  k8sClusterType?: string;
};

export type K8sClusterStatus = {
  available: boolean;
  enabled: boolean;
  context?: string;
  clusterType?: string;
  version?: string;
  message?: string;
  nodeCount: number;
  namespaceCount: number;
};

export type K8sPod = {
  name: string;
  phase: string;
  node?: string;
  restarts: number;
  ready: boolean;
};

export type K8sDeployment = {
  id: string;
  serviceId?: string;
  wizardId?: string;
  namespaceName: string;
  deploymentName: string;
  image?: string;
  replicas?: number;
  readyReplicas?: number;
  clusterContext?: string;
  clusterType?: string;
  status: string;
  message?: string;
  pods?: K8sPod[];
};

export type GitHubConnection = {
  id: string;
  login: string;
  avatarUrl?: string;
  status: string;
  scopes?: string;
  lastValidatedAt?: string;
  connectedAt?: string;
};

export type GitHubHealth = {
  provider: string;
  status: string;
  login?: string;
  rateLimitRemaining?: number;
  rateLimitLimit?: number;
  connected: boolean;
};

export type GitHubRepo = {
  id: string;
  owner: string;
  repoName: string;
  htmlUrl?: string;
  cloneUrl?: string;
  defaultBranch?: string;
  visibility?: string;
  status: string;
  serviceId?: string;
  wizardId?: string;
};

export type AiRecommendation = {
  runtime: string;
  runtimeConfidence: number;
  database: string;
  databaseConfidence: number;
  cache: string;
  cacheConfidence: number;
  replicaCount: number;
  hpaEnabled: boolean;
  cpu: string;
  memory: string;
  overallScore: number;
  summary: string;
  items: { key: string; value: string; confidence: number; reason: string }[];
  provider: string;
};

export type WizardPreview = {
  repository: string;
  runtime: string;
  environment: string;
  repositoryStructure: Record<string, string>;
  blueprint: string;
  helmValues: string;
  terraformVars: string;
  githubActions: string;
  deploymentYaml: string;
  argoApplication: string;
};

export type Wizard = {
  id: string;
  projectId: string;
  workspaceId: string;
  serviceName: string;
  templateId?: string;
  runtime?: string;
  environmentType?: string;
  databaseType?: string;
  cacheType?: string;
  replicaCount?: number;
  hpaEnabled?: boolean;
  cpu?: string;
  memory?: string;
  domain?: string;
  status: string;
  currentStep?: number;
  progress?: number;
  progressMessage?: string;
  serviceId?: string;
  recommendation?: AiRecommendation | null;
  preview?: WizardPreview | null;
};

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("nimbus_access_token");
}

export function setAuthSession(login: LoginResponse) {
  localStorage.setItem("nimbus_access_token", login.accessToken);
  localStorage.setItem("nimbus_refresh_token", login.refreshToken);
  localStorage.setItem("nimbus_user", JSON.stringify(login.user));
}

export function clearAuthSession() {
  localStorage.removeItem("nimbus_access_token");
  localStorage.removeItem("nimbus_refresh_token");
  localStorage.removeItem("nimbus_user");
}

export function getStoredUser(): UserSummary | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem("nimbus_user");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  auth = true,
): Promise<ApiResponse<T>> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (auth) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: "include",
    cache: "no-store",
  });

  const json = (await res.json()) as ApiResponse<T>;
  if (!res.ok && !json.error) {
    return {
      success: false,
      error: { code: `HTTP_${res.status}`, message: res.statusText },
    };
  }
  return json;
}

export async function apiGet<T>(path: string, auth = true) {
  return request<T>(path, { method: "GET" }, auth);
}

export async function apiPost<T>(path: string, body?: unknown, auth = true) {
  return request<T>(
    path,
    {
      method: "POST",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    },
    auth,
  );
}

export async function apiPatch<T>(path: string, body?: unknown, auth = true) {
  return request<T>(
    path,
    {
      method: "PATCH",
      body: body !== undefined ? JSON.stringify(body) : undefined,
    },
    auth,
  );
}

export async function apiDelete<T>(path: string, auth = true) {
  return request<T>(path, { method: "DELETE" }, auth);
}

export async function devLogin(name: string, email: string) {
  return apiPost<LoginResponse>(
    "/api/v1/auth/dev-login",
    { name, email },
    false,
  );
}

export async function fetchMe() {
  return apiGet<MeResponse>("/api/v1/auth/me");
}

export async function fetchWorkspaces() {
  return apiGet<WorkspaceSummary[]>("/api/v1/workspaces");
}

export async function fetchProjects(workspaceId: string) {
  return apiGet<Project[]>(`/api/v1/projects?workspaceId=${workspaceId}`);
}

export async function createProject(input: {
  name: string;
  workspaceId: string;
  description?: string;
}) {
  return apiPost<Project>("/api/v1/projects", input);
}

export async function fetchCatalog(q?: string) {
  const qs = q ? `?q=${encodeURIComponent(q)}` : "";
  return apiGet<CatalogTemplate[]>(`/api/v1/catalog${qs}`);
}

export async function fetchServices(params: {
  workspaceId?: string;
  projectId?: string;
}) {
  const sp = new URLSearchParams();
  if (params.workspaceId) sp.set("workspaceId", params.workspaceId);
  if (params.projectId) sp.set("projectId", params.projectId);
  const q = sp.toString();
  return apiGet<AppService[]>(`/api/v1/services${q ? `?${q}` : ""}`);
}

export async function createWizard(input: {
  projectId: string;
  serviceName: string;
  templateId?: string;
}) {
  return apiPost<Wizard>("/api/v1/service-wizard", input);
}

export async function getWizard(wizardId: string) {
  return apiGet<Wizard>(`/api/v1/service-wizard/${wizardId}`);
}

export async function updateWizard(
  wizardId: string,
  body: Record<string, unknown>,
) {
  return apiPatch<Wizard>(`/api/v1/service-wizard/${wizardId}`, body);
}

export async function recommendWizard(wizardId: string) {
  return apiPost<AiRecommendation>(
    `/api/v1/service-wizard/${wizardId}/recommend`,
  );
}

export async function previewWizard(wizardId: string) {
  return apiPost<WizardPreview>(`/api/v1/service-wizard/${wizardId}/preview`);
}

export async function validateWizard(wizardId: string) {
  return apiPost<{ valid: boolean; warnings: string[]; errors: string[] }>(
    `/api/v1/service-wizard/${wizardId}/validate`,
  );
}

export async function executeWizard(wizardId: string) {
  return apiPost<{
    wizardId: string;
    status: string;
    progress: number;
  }>(`/api/v1/service-wizard/${wizardId}/execute`);
}

export async function wizardLogs(wizardId: string) {
  return apiGet<{
    wizardId: string;
    status: string;
    progress: number;
    progressMessage?: string;
    logs?: string;
  }>(`/api/v1/service-wizard/${wizardId}/logs`);
}

export async function logout() {
  try {
    await apiPost<null>("/api/v1/auth/logout", undefined, true);
  } finally {
    clearAuthSession();
  }
}

export async function connectGitHub(personalAccessToken: string) {
  return apiPost<GitHubConnection>("/api/v1/github/connect", {
    personalAccessToken,
  });
}

export async function fetchGitHubStatus() {
  return apiGet<{ connected: boolean; provider: string }>("/api/v1/github/status");
}

export async function fetchGitHubConnection() {
  return apiGet<GitHubConnection>("/api/v1/github/connection");
}

export async function disconnectGitHub() {
  return apiDelete<null>("/api/v1/github/connection");
}

export async function fetchGitHubHealth() {
  return apiGet<GitHubHealth>("/api/v1/github/health");
}

export async function fetchGitHubRepositories() {
  return apiGet<GitHubRepo[]>("/api/v1/github/repositories");
}

export async function fetchK8sCluster() {
  return apiGet<K8sClusterStatus>("/api/v1/k8s/cluster");
}

export async function refreshK8sCluster() {
  return apiPost<K8sClusterStatus>("/api/v1/k8s/cluster/refresh");
}

export async function fetchK8sDeployments(workspaceId?: string) {
  const q = workspaceId ? `?workspaceId=${workspaceId}` : "";
  return apiGet<K8sDeployment[]>(`/api/v1/k8s/deployments${q}`);
}

export type MonitoringLinks = {
  prometheusUrl: string;
  grafanaUrl: string;
  lokiUrl: string;
  prometheusUp: boolean;
  grafanaUp: boolean;
  mode: string;
};

export type MetricPoint = { name: string; value: number; unit: string };

export type ServiceMetrics = {
  serviceId: string;
  serviceName: string;
  source: string;
  metrics: MetricPoint[];
  collectedAt: string;
};

export type MonitoringOverview = {
  links: MonitoringLinks;
  serviceCount: number;
  runningDeployments: number;
  avgCpu: number;
  avgMemory: number;
  topServices: ServiceMetrics[];
};

export type LogLine = {
  timestamp: string;
  level: string;
  pod: string;
  message: string;
};

export type LogSnapshot = {
  serviceId: string;
  serviceName: string;
  source: string;
  lines: LogLine[];
};

export type Pipeline = {
  id: string;
  serviceId?: string;
  projectId?: string;
  workspaceId?: string;
  serviceName: string;
  name: string;
  status: string;
  progress?: number;
  currentStep?: string;
  imageTag?: string;
  dockerfilePath?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
};

export type PipelineLogs = {
  id: string;
  status: string;
  progress?: number;
  currentStep?: string;
  logs?: string;
};

export async function fetchMonitoringOverview(workspaceId?: string) {
  const q = workspaceId ? `?workspaceId=${workspaceId}` : "";
  return apiGet<MonitoringOverview>(`/api/v1/monitoring/overview${q}`);
}

export async function fetchMonitoringLinks() {
  return apiGet<MonitoringLinks>("/api/v1/monitoring/links");
}

export async function fetchServiceMetrics(serviceId: string) {
  return apiGet<ServiceMetrics>(`/api/v1/monitoring/services/${serviceId}/metrics`);
}

export async function fetchServiceLogs(serviceId: string, limit = 100) {
  return apiGet<LogSnapshot>(`/api/v1/logs/services/${serviceId}?limit=${limit}`);
}

export function openLogStream(serviceId: string): EventSource {
  const token = getToken();
  // EventSource cannot set Authorization header — use query token fallback not implemented.
  // Snapshot + polling is primary; for SSE we rely on cookie session if any.
  // JWT is bearer-only: stream via fetch + ReadableStream on UI instead when needed.
  const url = `${API_BASE}/api/v1/logs/services/${serviceId}/stream`;
  // Note: browser EventSource without auth will 401. UI uses fetchLogStream.
  return new EventSource(url + (token ? `?access_token=${encodeURIComponent(token)}` : ""));
}

/** Auth-aware log stream using fetch + SSE parse */
export async function* iterateLogStream(
  serviceId: string,
  signal?: AbortSignal,
): AsyncGenerator<LogLine> {
  const token = getToken();
  const res = await fetch(`${API_BASE}/api/v1/logs/services/${serviceId}/stream`, {
    headers: {
      Accept: "text/event-stream",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    signal,
    credentials: "include",
  });
  if (!res.ok || !res.body) {
    throw new Error(`log stream failed: ${res.status}`);
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split("\n\n");
    buffer = chunks.pop() ?? "";
    for (const chunk of chunks) {
      const dataLine = chunk
        .split("\n")
        .find((l) => l.startsWith("data:"));
      if (!dataLine) continue;
      const json = dataLine.replace(/^data:\s?/, "");
      try {
        yield JSON.parse(json) as LogLine;
      } catch {
        // ignore
      }
    }
  }
}

export async function fetchPipelines(params?: {
  workspaceId?: string;
  serviceId?: string;
}) {
  const sp = new URLSearchParams();
  if (params?.workspaceId) sp.set("workspaceId", params.workspaceId);
  if (params?.serviceId) sp.set("serviceId", params.serviceId);
  const q = sp.toString();
  return apiGet<Pipeline[]>(`/api/v1/pipelines${q ? `?${q}` : ""}`);
}

export async function createPipeline(serviceId: string) {
  return apiPost<Pipeline>("/api/v1/pipelines", { serviceId });
}

export async function fetchPipeline(pipelineId: string) {
  return apiGet<Pipeline>(`/api/v1/pipelines/${pipelineId}`);
}

export async function fetchPipelineLogs(pipelineId: string) {
  return apiGet<PipelineLogs>(`/api/v1/pipelines/${pipelineId}/logs`);
}

export async function rerunPipeline(pipelineId: string) {
  return apiPost<Pipeline>(`/api/v1/pipelines/${pipelineId}/rerun`);
}

export { API_BASE };
