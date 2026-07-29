import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function apiJson<T>(
  request: APIRequestContext,
  path: string,
  init?: { method?: string; token?: string; data?: unknown },
): Promise<T> {
  const res = await request.fetch(`${API}${path}`, {
    method: init?.method ?? "GET",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(init?.token ? { Authorization: `Bearer ${init.token}` } : {}),
    },
    data: init?.data ? JSON.stringify(init.data) : undefined,
  });
  const body = await res.json();
  if (!res.ok() || body.success === false) {
    throw new Error(
      `API ${path} failed: ${res.status()} ${JSON.stringify(body?.error ?? body)}`,
    );
  }
  return body.data as T;
}

test.describe("Nimbus platform smoke", () => {
  test("login → project → wizard → service → audit/dashboard", async ({
    page,
    request,
  }) => {
    const stamp = Date.now();
    const email = `e2e-${stamp}@nimbus.local`;
    const projectName = `E2E Project ${stamp}`;
    const serviceName = `e2e-svc-${stamp}`;

    // 1) Login
    await page.goto("/login");
    await page.getByLabel("이름").fill("E2E User");
    await page.getByLabel("이메일").fill(email);
    await page.getByRole("button", { name: /Dev Login으로 시작/i }).click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 20_000 });
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();

    const token = await page.evaluate(() =>
      localStorage.getItem("nimbus_access_token"),
    );
    expect(token).toBeTruthy();

    // 2) Create project (UI)
    await page.goto("/projects");
    await expect(page.getByRole("heading", { name: "Projects" })).toBeVisible();
    await page.getByPlaceholder("Payment Platform").fill(projectName);
    await page.getByRole("button", { name: "프로젝트 만들기" }).click();
    await expect(page.getByText(projectName)).toBeVisible({ timeout: 15_000 });

    // 3) Wizard page (UI presence)
    await page.goto("/wizard");
    await expect(page.getByRole("heading", { name: "서비스 정보" })).toBeVisible({
      timeout: 15_000,
    });
    // service name field — first textbox on step 0
    const serviceInput = page.getByRole("textbox").first();
    await serviceInput.fill(serviceName);

    // API path: create wizard → update → preview → execute → wait (sim provision)
    const me = await apiJson<{
      workspace?: { id: string };
    }>(request, "/api/v1/auth/me", { token: token! });
    const workspaceId = me.workspace?.id;
    expect(workspaceId).toBeTruthy();

    const projects = await apiJson<
      { id: string; name: string }[]
    >(request, `/api/v1/projects?workspaceId=${workspaceId}`, {
      token: token!,
    });
    const project = projects.find((p) => p.name === projectName) ?? projects[0];
    expect(project).toBeTruthy();

    const catalog = await apiJson<
      { id: string; runtime: string }[]
    >(request, "/api/v1/catalog", { token: token! });
    const template =
      catalog.find((t) => t.runtime === "SPRING_BOOT") ?? catalog[0];
    expect(template).toBeTruthy();

    const wizard = await apiJson<{ id: string }>(
      request,
      "/api/v1/service-wizard",
      {
        method: "POST",
        token: token!,
        data: {
          projectId: project!.id,
          serviceName,
          templateId: template!.id,
        },
      },
    );

    await apiJson(request, `/api/v1/service-wizard/${wizard.id}`, {
      method: "PATCH",
      token: token!,
      data: {
        templateId: template!.id,
        runtime: template!.runtime,
        environmentType: "DEV",
        replicaCount: 1,
        currentStep: 4,
      },
    });

    await apiJson(request, `/api/v1/service-wizard/${wizard.id}/preview`, {
      method: "POST",
      token: token!,
    });

    await apiJson(request, `/api/v1/service-wizard/${wizard.id}/execute`, {
      method: "POST",
      token: token!,
    });

    let serviceId: string | undefined;
    for (let i = 0; i < 40; i++) {
      const w = await apiJson<{
        status: string;
        serviceId?: string;
      }>(request, `/api/v1/service-wizard/${wizard.id}`, { token: token! });
      if (w.status === "COMPLETED" && w.serviceId) {
        serviceId = w.serviceId;
        break;
      }
      if (w.status === "FAILED") {
        throw new Error("Wizard provision FAILED");
      }
      await page.waitForTimeout(500);
    }
    expect(serviceId).toBeTruthy();

    // 4) Service detail
    await page.goto(`/services/${serviceId}`);
    await expect(page.getByRole("heading", { name: serviceName })).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByText("Environments")).toBeVisible();

    // 5) Audit
    await page.goto("/audit");
    await expect(page.getByRole("heading", { name: "Audit" })).toBeVisible();
    // filter select always present; table body or empty state
    await expect(page.getByRole("button", { name: /새로고침|조회/ })).toBeVisible();
    await page.getByRole("button", { name: /새로고침|조회/ }).click();
    await expect(
      page
        .locator("table tbody")
        .getByText(/LOGIN|CREATE_PROJECT|EXECUTE_WIZARD|CREATE_WIZARD|INVITE/i)
        .first()
        .or(page.getByText("감사 로그가 없습니다")),
    ).toBeVisible({ timeout: 15_000 });

    // 6) Dashboard widgets
    await page.goto("/dashboard");
    await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
    await expect(page.getByText("Environments").or(page.getByText("Audit"))).toBeVisible();

    // 7) Settings members — invite VIEWER (API + UI verify list)
    const viewerEmail = `viewer-${stamp}@nimbus.local`;
    await apiJson(request, `/api/v1/workspaces/${workspaceId}/members/invite`, {
      method: "POST",
      token: token!,
      data: { email: viewerEmail, role: "VIEWER" },
    });
    await page.goto("/settings");
    await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Members" })).toBeVisible();
    await expect(page.getByText(viewerEmail)).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText("VIEWER").first()).toBeVisible();
  });
});
