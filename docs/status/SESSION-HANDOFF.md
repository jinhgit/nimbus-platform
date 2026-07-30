# Session Handoff — Nimbus Platform

**목적:** 컨텍스트 스위칭 / 새 세션 재개용.  
**최종 갱신:** 2026-07-30  
**HEAD (push 완료):** `4285466` on `main` (핸드오프 문서 포함)  
**데모 freeze 커밋:** `6397c34`  
**Remote:** https://github.com/jinhgit/nimbus-platform (push 시 리다이렉트 메시지 가능; origin URL은 `nimbus-platform-prd.git` 일 수 있음)

> 이 문서는 “어디까지 했고, 다음에 무엇을 보면 되는지”만 담는다.  
> 제품 스펙 상세는 PRD / 구현 매트릭스를 본다.

---

## 1. 한 줄 상태

**시연 MVP + 운영 thin 기능 구현 완료 · 데모 시나리오·스크린샷·핵심 플로우 검증 고정 · main 푸시됨.**  
다음 가치는 **새 기능 대량 추가보다** 면접 dry-run, LIVE 경로 보강, 또는 포트폴리오 문서 다듬기 쪽.

---

## 2. 제품 한 줄

AI-native IDP 포트폴리오 제품.  
**Catalog → Wizard(+AI) → Saga provision → Environment/Promote/Secret → Audit/Incident/Notification** 을 free-only로 시연.

| 축 | 내용 |
|----|------|
| Web | Next.js 15 · EN chrome/titles · KO body · linear SVG icons |
| API | Spring Boot 4 · Java 21 · JWT · RBAC (`CAN_MUTATE` / VIEWER 차단) |
| 로컬 | H2 `application-local` · Dev Login · SIMULATED when SCM/k8s 없음 |
| 비용 | free-only (과금 클라우드 실생성 없음) |

---

## 3. 최근 작업 타임라인 (커밋)

| Commit | 요약 |
|--------|------|
| `3aef50a` | Dashboard widgets · Viewer RBAC · loading/empty/error |
| `8477bb2` | Members UI · Promote GitOps thin · Playwright · OpenAPI CI |
| `ecd36db` | Pipeline GH Actions thin · Incident · ⌘K · Ollama AI |
| `cb5d57c` | Secret rotation · Catalog detail · Project archive/clone · Argo sync thin |
| `1547bea` | Notifications · Dashboard ops · Service tags · E2E ops |
| **`6397c34`** | **Demo freeze: 시나리오 · 스크린샷 11장 · verify script · VERIFICATION** |
| **`4285466`** | **Session handoff 문서 (context switch)** |

그 이전: Auth/Workspace/Project, Catalog/Wizard/AI, Audit P0, YAML Explain, Deployment history 등.

---

## 4. “데모 패키징” 세션에서 한 일 (직전 완료)

순차 요청: **시나리오 고정 → 스크린샷/README → 핵심 플로우 검증**

### 4.1 산출물

| 경로 | 역할 |
|------|------|
| `docs/demo/DEMO-SCENARIO.md` | **8~12분 고정 시연 대본** (클릭 순서·멘트·DoD·fallback) |
| `docs/demo/screenshots/` | 01~11 PNG + README 인덱스 |
| `docs/demo/VERIFICATION.md` | 검증 PASS 기록 · H2 주의사항 |
| `scripts/verify-demo-flow.sh` | API 핵심 플로우 자동 검증 |
| `scripts/capture-demo-screenshots.mjs` | Playwright로 스크린샷 재캡처 |
| `README.md` §1/16/20/21 + 로드맵 | 시연 링크 · 이미지 임베드 · 검증 명령 |

### 4.2 검증 결과 (2026-07-30)

```bash
bash scripts/verify-demo-flow.sh
# → DEMO CORE FLOW VERIFIED SUCCESSFULLY
```

통과 스텝: health → openapi → dev-login → me/permissions/dashboard → project → catalog → wizard COMPLETED → environments · tags · argo · notifications · incidents → audit.

### 4.3 막혔던 이슈 & 해결

| 증상 | 원인 | 해결 |
|------|------|------|
| `PUT .../tags` → 500 `COMMON001` | H2 file DB에 **구 ENUM CHECK** 가 남아 `UPDATE_SERVICE_TAGS` 거부 (`ddl-auto=update` 가 CHECK 를 안 넓힘) | API 중지 후 `rm -f apps/api/build/data/nimbus*.db` · 재기동 |
| 재발 방지 | action 컬럼이 엔진/버전에 따라 enum-like 될 수 있음 | `AuditLog.action` 에 `columnDefinition = "varchar(48)"` |

**재발 시 치트시트:**

```bash
# API 중지
rm -f apps/api/build/data/nimbus.mv.db apps/api/build/data/nimbus.trace.db
cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'
bash scripts/verify-demo-flow.sh
```

---

## 5. 시연 핵심 경로 (고정)

자세한 대본: **[docs/demo/DEMO-SCENARIO.md](../demo/DEMO-SCENARIO.md)**

```text
Login (Dev)
  → Dashboard (ops 카운트 · 벨)
  → Projects (생성)
  → Wizard (템플릿 → AI → Preview → Deploy / Saga)
  → Service Detail (Env · Promote · Secret · Argo · Tags)
  → Audit
  → Incidents + Notification bell
  → (선택) Catalog 상세 · VIEWER RBAC · ⌘K
```

**원칙:** 구현된 것만 시연 · SIMULATED 정직하게 · free-only.

---

## 6. 로컬 기동 · 검증 명령

```bash
# Terminal 1 — API
cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'

# Terminal 2 — Web
cd apps/web && npm run dev

# 핵심 플로우 (API만)
bash scripts/verify-demo-flow.sh

# 단위/통합
cd apps/api && ./gradlew test

# OpenAPI
bash scripts/check-openapi-sync.sh

# E2E (API 기동 필요; 로컬은 channel chrome 쓸 수 있음)
cd apps/web && npm run test:e2e

# 스크린샷 재캡처 (API+Web)
node scripts/capture-demo-screenshots.mjs
```

| URL | |
|-----|--|
| Health | http://localhost:8080/api/v1/health |
| OpenAPI | http://localhost:8080/v3/api-docs |
| Portal | http://localhost:3000/login |
| H2 file | `apps/api/build/data/nimbus.mv.db` |

---

## 7. 도메인 · 기능 맵 (구현됨)

| 영역 | 상태 | 메모 |
|------|:----:|------|
| Auth / Workspace / RBAC | ✅ | Dev Login · OAuth 골격 · VIEWER mutate 차단 |
| Project | ✅ | CRUD · Archive · Clone |
| Catalog | ✅ | 목록 + Blueprint/Helm 상세 |
| Wizard + Saga | ✅ | 7단계 · retry · SCM 없으면 sim |
| AI | ✅ | rule 기본 · Ollama 옵션 · YAML Explain |
| Environment / Promote / Secrets | ✅ | AES · rotate · GH secrets thin |
| Argo / GitOps | 🔶~✅ | Application CR · LIVE/SIMULATED |
| Pipeline | ✅ | sim build · GH Actions thin |
| Incident / Notification | ✅ | scan · ACK · bell |
| Service tags | ✅ | PUT + list filter |
| Audit / Dashboard | ✅ | mutation · ops widgets |
| Members invite UI | ✅ | Settings |
| ⌘K palette | ✅ | |
| OpenAPI + CI + Playwright | ✅ | |
| K8s / Prom 풀스택 LIVE | 🔶 | 로컬 k3d 마감 보류 가능 |

상세 매트릭스: [PRD-vs-Implementation.md](PRD-vs-Implementation.md)

---

## 8. 코드 진입점 (재개 시 자주 여는 곳)

| 관심 | 경로 |
|------|------|
| Audit actions | `apps/api/.../audit/domain/AuditAction.java` |
| Tags API | `AppServiceQueryService` · `ServiceDtos` · `PUT /services/{id}/tags` |
| Argo thin | `apps/api/.../gitops/` |
| Notifications | `apps/api/.../notification/` |
| Dashboard counts | dashboard overview DTO (`openIncidents`, `failedPipelines`, `unreadNotifications`) |
| Web shell | `apps/web` AppShell · NotificationBell · Command palette |
| E2E | `apps/web` `platform-smoke` · `ops-features.spec.ts` |
| OpenAPI snapshot | `docs/api/openapi.yaml` (버전 0.5.x 대역) |

---

## 9. UX · 컨벤션 (깨지 말 것)

- **Chrome/titles:** English  
- **Body copy:** Korean  
- **Icons:** linear SVG (이모지 남발 금지)  
- **시연 멘트:** “구현 있는 것만” · SIMULATED 숨기지 않음  
- **커밋:** 의미 있는 단위 · 사용자 요청 시 push (이 핸드오프 시점 main 반영됨)

---

## 10. 의도적으로 안 한 것 / 보류

- AWS EKS/RDS/ALB 실생성 (free-only 위반)
- Multi-cluster · Vault cloud · FinOps · Mesh (v2)
- k3d/kind 시연 스크립트 **마감 강제** (보류 가능으로 로드맵 표기)
- 기능 스프린트 추가 — 포트폴리오 관점에서 **데모 고정이 우선**이었음

---

## 11. 다음에 할 수 있는 일 (우선순위 제안)

| 우선 | 작업 | 언제 |
|:----:|------|------|
| 1 | 면접 당일 **UI dry-run 1회** (DEMO-SCENARIO §1) | 시연 전 |
| 2 | `./gradlew test` + OpenAPI + Playwright 재확인 | 리그레션 의심 시 |
| 3 | LIVE GitHub/K8s 경로 문서·시연 분기 보강 | SCM 연결 데모 필요 시 |
| 4 | PRD-vs-Implementation 매트릭스 최신 기능 반영 | 문서 정직성 |
| 5 | origin remote URL을 `nimbus-platform.git` 로 정리 | 선택 |
| — | 새 대형 기능 스프린트 | **필요 명확할 때만** |

---

## 12. 새 세션 시작 프롬프트 예시

복붙용:

```text
Nimbus Platform 이어서.
핸드오프: docs/status/SESSION-HANDOFF.md
HEAD 기준으로 데모 시나리오·검증은 고정된 상태.
(여기에 다음 작업 한 줄)
```

또는:

```text
docs/status/SESSION-HANDOFF.md 읽고 상태 확인한 뒤
[작업] 진행해줘.
```

---

## 13. 관련 문서 인덱스

| 문서 | 용도 |
|------|------|
| [SESSION-HANDOFF.md](SESSION-HANDOFF.md) | **이 파일** — 세션 재개 |
| [DEMO-SCENARIO.md](../demo/DEMO-SCENARIO.md) | 면접 8~12분 대본 |
| [VERIFICATION.md](../demo/VERIFICATION.md) | 검증 PASS 기록 |
| [screenshots/](../demo/screenshots/) | 포트폴리오 캡처 |
| [PRD-vs-Implementation.md](PRD-vs-Implementation.md) | 구현 매트릭스 |
| [README.md](../../README.md) | 설치 · 아키텍처 · 시연 링크 |
| [PRD-MASTER.md](../prd/PRD-MASTER.md) | 제품 명세 |
| [05-Free-Only-Constraints.md](../architecture/05-Free-Only-Constraints.md) | 비용 제약 |

---

## 14. Working tree 메모 (이 문서 작성 시점)

- 데모 패키징 `6397c34` + 핸드오프 `4285466` **push 완료**
- 이 문서 HEAD 필드를 갱신한 뒤 로컬 dirty 가 남을 수 있음 → `docs` 만 추가 커밋해도 됨
- 로컬 API가 떠 있을 수 있음 (`:8080`) — 재기동 시 H2 파일 잠금 주의

---

*End of handoff. 업데이트 시 상단 날짜·HEAD·§3 타임라인·§11 다음 작업만 고치면 된다.*
