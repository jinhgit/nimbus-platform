/**
 * Capture demo screenshots into docs/demo/screenshots/
 * Requires: API :8080, Web :3000, playwright chromium/chrome available
 *
 *   node scripts/capture-demo-screenshots.mjs
 */
import { chromium } from "../apps/web/node_modules/playwright/index.mjs";
import { mkdirSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const OUT = join(ROOT, "docs/demo/screenshots");
const BASE = process.env.PLAYWRIGHT_BASE_URL || "http://localhost:3000";
const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

mkdirSync(OUT, { recursive: true });

async function shot(page, name) {
  const path = join(OUT, name);
  await page.screenshot({ path, fullPage: false });
  console.log("wrote", name);
}

async function main() {
  const health = await fetch(`${API}/api/v1/health`).then((r) => r.json()).catch(() => null);
  if (!health?.success) {
    console.error("API not up at", API);
    process.exit(1);
  }

  const browser = await chromium.launch({
    channel: process.env.CI ? undefined : "chrome",
    headless: true,
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const stamp = Date.now();
  const email = `shot-${stamp}@nimbus.local`;

  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await shot(page, "01-login.png");

  await page.getByLabel("이름").fill("Screenshot Demo");
  await page.getByLabel("이메일").fill(email);
  await page.getByRole("button", { name: /Dev Login으로 시작/i }).click();
  await page.waitForURL(/\/dashboard/, { timeout: 20000 });
  await page.waitForTimeout(800);
  await shot(page, "02-dashboard.png");

  await page.goto(`${BASE}/projects`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  const name = `Shot Project ${stamp}`;
  await page.getByPlaceholder("Payment Platform").fill(name);
  await page.getByRole("button", { name: "프로젝트 만들기" }).click();
  await page.getByText(name).waitFor({ timeout: 15000 });
  await shot(page, "03-projects.png");

  await page.goto(`${BASE}/wizard`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await shot(page, "04-wizard.png");

  await page.goto(`${BASE}/catalog`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await shot(page, "05-catalog.png");

  await page.goto(`${BASE}/services`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  await shot(page, "06-services.png");

  // API create minimal service path via existing project+wizard if services empty
  const token = await page.evaluate(() => localStorage.getItem("nimbus_access_token"));
  const me = await fetch(`${API}/api/v1/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());
  const ws = me.data?.workspace?.id;
  const projects = await fetch(`${API}/api/v1/projects?workspaceId=${ws}`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());
  const projectId = projects.data?.[0]?.id;
  const catalog = await fetch(`${API}/api/v1/catalog`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());
  const templateId = catalog.data?.[0]?.id;
  let serviceId = null;
  if (projectId && templateId) {
    const w = await fetch(`${API}/api/v1/service-wizard`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        projectId,
        serviceName: `shot-svc-${stamp}`,
        templateId,
      }),
    }).then((r) => r.json());
    const wizardId = w.data?.id;
    if (wizardId) {
      await fetch(`${API}/api/v1/service-wizard/${wizardId}`, {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          templateId,
          runtime: catalog.data[0].runtime,
          environmentType: "DEV",
          replicaCount: 1,
          currentStep: 4,
        }),
      });
      await fetch(`${API}/api/v1/service-wizard/${wizardId}/preview`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      await fetch(`${API}/api/v1/service-wizard/${wizardId}/execute`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      for (let i = 0; i < 40; i++) {
        const st = await fetch(`${API}/api/v1/service-wizard/${wizardId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }).then((r) => r.json());
        if (st.data?.status === "COMPLETED" && st.data?.serviceId) {
          serviceId = st.data.serviceId;
          break;
        }
        if (st.data?.status === "FAILED") break;
        await new Promise((r) => setTimeout(r, 400));
      }
    }
  }

  if (serviceId) {
    await page.goto(`${BASE}/services/${serviceId}`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1000);
    await shot(page, "07-service-detail.png");
  } else {
    console.warn("skip 07-service-detail (no service)");
  }

  await page.goto(`${BASE}/audit`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await shot(page, "08-audit.png");

  await page.goto(`${BASE}/incidents`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  await shot(page, "09-incidents.png");

  await page.goto(`${BASE}/pipelines`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  await shot(page, "10-pipelines.png");

  await page.goto(`${BASE}/settings`, { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await shot(page, "11-settings.png");

  await browser.close();
  console.log("done →", OUT);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
