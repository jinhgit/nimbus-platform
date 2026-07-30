# Demo Core Flow — Verification Record

**Date:** 2026-07-30  
**Scope:** 면접 시연 핵심 경로 (API automated + screenshots)

---

## 1. Automated: `scripts/verify-demo-flow.sh`

**Prerequisite:** API on `:8080` with `spring.profiles.active=local`

```bash
bash scripts/verify-demo-flow.sh
```

### Result: **PASS** (2026-07-30)

| Step | Assertion | Status |
|------|-----------|:------:|
| health | `GET /api/v1/health` → UP | ✅ |
| openapi | `GET /v3/api-docs` | ✅ |
| login | Dev Login → JWT + workspaceId | ✅ |
| me / permissions / dashboard | canMutate · WORKSPACE_MUTATE · openIncidents | ✅ |
| project | `POST /projects` | ✅ |
| catalog | list + detail (`blueprint`) | ✅ |
| wizard provision | create → patch → preview → execute → **COMPLETED** + serviceId | ✅ |
| environments | DEV env present | ✅ |
| tags | PUT tags `demo,verify` · filter `?tag=demo` | ✅ |
| argo | sync mode SIMULATED or LIVE · Application manifest | ✅ |
| notifications | `POST /notifications/sync` scanned | ✅ |
| incidents | `POST /incidents/scan` scanned | ✅ |
| audit | recent mutation items ≥ 1 | ✅ |

**Sample IDs from successful run:**

```text
workspace=1320f543-31a3-4eec-8e78-9f6a86629fbf
project=0457bffe-dd1a-4f66-81c2-2c0432b4a27a
service=2bff174c-0a8d-47c7-a5ac-bb97b25584f7
wizard=4230b63c-5021-40c1-951c-7bef902a7ee8
```

### Local H2 note

File DB: `apps/api/build/data/nimbus.mv.db`  
`ddl-auto=update` 는 예전 ENUM CHECK 를 넓히지 못하는 경우가 있다.  
새 `AuditAction` 추가 후 `UPDATE_SERVICE_TAGS` 등이 500 이면:

```bash
# API 중지 후
rm -f apps/api/build/data/nimbus.mv.db apps/api/build/data/nimbus.trace.db
# bootRun 재기동 후 verify 재실행
```

Entity 측: `AuditLog.action` 은 `varchar(48)` columnDefinition 으로 고정.

---

## 2. Screenshots

| File | Captured |
|------|:--------:|
| `docs/demo/screenshots/01-login.png` … `11-settings.png` | ✅ 11장 |
| Index | [screenshots/README.md](screenshots/README.md) |

재생성:

```bash
# API + Web 기동 후
node scripts/capture-demo-screenshots.mjs
```

---

## 3. Related checks (recommended before interview)

```bash
cd apps/api && ./gradlew test
bash scripts/check-openapi-sync.sh
cd apps/web && npm run test:e2e   # API :8080 required
```

| Check | Notes |
|-------|-------|
| Gradle tests | Auth / Wizard / Audit / Tags / Argo / Notification 등 |
| OpenAPI sync | `docs/api/openapi.yaml` vs runtime |
| Playwright | `platform-smoke` · `ops-features` |

---

## 4. Manual demo checklist (from DEMO-SCENARIO §6)

- [x] API health UP  
- [x] Dev Login → Dashboard (screenshot)  
- [x] Project create (API verify)  
- [x] Wizard COMPLETED (API verify)  
- [x] Environments + Tags + Argo (API verify)  
- [x] Audit mutations (API verify)  
- [x] Notifications / Incidents scan (API verify)  
- [ ] (면접 당일) UI 클릭 1회 dry-run 권장  

---

## 5. Fixed demo path

시연 순서·멘트: **[DEMO-SCENARIO.md](DEMO-SCENARIO.md)** (8~12분 고정)
