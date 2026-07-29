import { expect, test, type APIRequestContext } from "@playwright/test";

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

test.describe("Ops features smoke", () => {
  test("catalog detail · notifications · tags · incidents path", async ({
    page,
    request,
  }) => {
    const stamp = Date.now();
    const email = `e2e-ops-${stamp}@nimbus.local`;

    await page.goto("/login");
    await page.getByLabel("이름").fill("E2E Ops");
    await page.getByLabel("이메일").fill(email);
    await page.getByRole("button", { name: /Dev Login으로 시작/i }).click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 20_000 });

    const token = await page.evaluate(() =>
      localStorage.getItem("nimbus_access_token"),
    );
    expect(token).toBeTruthy();

    // Dashboard new widgets
    await expect(page.getByText("Open Incidents")).toBeVisible();
    await expect(page.getByText("Failed Pipelines")).toBeVisible();
    await expect(page.getByText("Notifications").first()).toBeVisible();

    // Catalog list → detail
    await page.goto("/catalog");
    await expect(page.getByRole("heading", { name: "Catalog" })).toBeVisible();
    const firstCard = page.locator('a[href^="/catalog/"]').first();
    await expect(firstCard).toBeVisible({ timeout: 15_000 });
    await firstCard.click();
    await expect(page.getByRole("button", { name: "Blueprint" })).toBeVisible({
      timeout: 15_000,
    });
    await page.getByRole("button", { name: "Helm" }).click();

    // Notification sync API + bell UI
    const me = await apiJson<{ workspace?: { id: string } }>(
      request,
      "/api/v1/auth/me",
      { token: token! },
    );
    const ws = me.workspace?.id;
    expect(ws).toBeTruthy();
    await apiJson(request, `/api/v1/notifications/sync?workspaceId=${ws}`, {
      method: "POST",
      token: token!,
    });
    await page.goto("/dashboard");
    await page.getByRole("button", { name: "Notifications" }).click();
    await expect(page.getByText("Notifications").first()).toBeVisible();

    // Incidents page
    await page.goto("/incidents");
    await expect(page.getByRole("heading", { name: "Incidents" })).toBeVisible();
    await page.getByRole("button", { name: /이슈 스캔/ }).click();

    // Projects archive/clone surface
    await page.goto("/projects");
    await expect(page.getByRole("heading", { name: "Projects" })).toBeVisible();
    await page.getByPlaceholder("Payment Platform").fill(`Ops Project ${stamp}`);
    await page.getByRole("button", { name: "프로젝트 만들기" }).click();
    await expect(page.getByText(`Ops Project ${stamp}`)).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByRole("button", { name: "복제" }).first()).toBeVisible();
  });
});
