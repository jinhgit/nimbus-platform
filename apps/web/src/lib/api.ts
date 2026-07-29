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

export async function logout() {
  try {
    await apiPost<null>("/api/v1/auth/logout", undefined, true);
  } finally {
    clearAuthSession();
  }
}

export { API_BASE };
