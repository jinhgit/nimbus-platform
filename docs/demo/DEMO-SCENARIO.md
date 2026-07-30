# Nimbus Platform — 면접 시연 시나리오 (고정)

**기준일:** 2026-07-30  
**길이:** 약 **8~12분** (핵심 경로) · 확장 시 15분  
**원칙:** free-only · 과금 클라우드 실생성 없음 · **코드가 있는 것만 시연**

> **면접용 한 문장**  
> 개발자가 Catalog·Wizard·AI 추천으로 서비스를 올리고,  
> Environment Promote · Secret · Audit · Incident 까지 **Self-Service 운영 포털**로  
> 이어지는 Platform Engineering 제품을 설계·구현했습니다.

---

## 0. 시연 전 기동 (2분)

```bash
# 터미널 1 — API (H2 local)
cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'

# 터미널 2 — Web
cd apps/web && npm run dev
```

| 확인 | URL |
|------|-----|
| API health | http://localhost:8080/api/v1/health → `UP` |
| OpenAPI | http://localhost:8080/v3/api-docs |
| Portal | http://localhost:3000/login |

**계정:** Dev Login 아무 이름/이메일 (예: `Demo User` / `demo@nimbus.local`)

**선택:** Settings에서 GitHub SCM(OAuth/PAT) 연결 시 Repo/Actions LIVE 경로 가능.  
미연결이어도 **시뮬 프로비저닝**으로 전체 스토리 가능.

스크린샷 참고: [`docs/demo/screenshots/`](screenshots/README.md)

---

## 1. 8분 핵심 스크립트 (고정)

시연은 **아래 순서만** 지킨다. 옆길로 빠지지 않는다.

| # | 화면 | 말할 것 (30초 이내) | 클릭/입력 |
|---|------|---------------------|-----------|
| **1** | Login | “로컬 free-only는 Dev Login, 프로덕션은 GitHub OAuth” | Dev Login |
| **2** | Dashboard | “프로젝트·서비스·Env·Failed Saga·Incident·알림 카운트” | 숫자·위젯 가리키기 |
| **3** | Projects | “비즈니스 컨텍스트 단위, 보관/복제 지원” | 프로젝트 생성 `Payment Platform` |
| **4** | Create Service (Wizard) | “Golden Path Self-Service” | service `payment-api` → 템플릿 → AI 추천 → Preview → Deploy |
| **5** | Provision | “Saga 단계 DB + 보상 로그 + Retry (mutator)” | Progress / Saga 단계 |
| **6** | Service Detail | “Env DEV→STAGE 승격, Secret AES, Argo thin, Tags” | Environments 탭 강조 |
| **7** | Promote (선택 1분) | “config 복사 + GitOps branch meta / PR 시도” | DEV → STAGE 승격 |
| **8** | Audit | “mutation 전부 감사” | PROMOTE / CREATE 필터 |
| **9** | Incidents + Bell | “실패 Saga/Pipeline 스캔 → 알림” | 이슈 스캔 · 벨 아이콘 |
| **10** | 마무리 Dashboard | “플랫폼 한 화면으로 운영 상태 요약” | Open Incidents 등 |

**확장 옵션 (시간 있을 때만 1개)**

- Catalog 상세 (Blueprint/Helm 탭)
- VIEWER 초대 → Promote 버튼 숨김 (RBAC)
- ⌘K Command Palette
- Settings · AI Provider (`rule` / `ollama`)

---

## 2. 화면별 상세 (대본)

### 2.1 Login → Dashboard

```text
Login (Dev) → Dashboard
```

- 헤더 벨: Notifications (읽지 않음 카운트)
- Stat: Projects / Services / Environments / Ready / Failed Sagas / **Open Incidents** / **Failed Pipelines** / Notifications / Cluster

### 2.2 Project

```text
Projects → 이름 Payment Platform → 만들기
```

- 이후 **복제 / 보관 / 복원**은 “메타데이터 운영”으로 한 줄만 언급 가능

### 2.3 Wizard (메인 스토리)

```text
Create Service
  → 서비스 정보 (payment-api, Project 선택)
  → 템플릿 (Spring Boot REST 등)
  → 인프라 (Environment)
  → AI 리뷰 (Recommend)
  → 미리보기 (YAML/Helm/Argo…)
  → 프로비저닝 (Execute)
  → 완료 → 서비스 상세
```

**말할 포인트**

- AI는 Chatbot이 아니라 **Decision Engine** (confidence / reason / rule-engine, Ollama 옵션)
- Preview = Zero-YAML 플랫폼이 대신 만드는 산출물
- Provision = **Saga** (step · compensate · retry)

### 2.4 Service Detail

강조 순서:

1. **Environments** — DEV / STAGE / PRODUCTION  
2. **Variables / Secrets** — AES 마스킹 · 로테이션 · (mutator) Reveal  
3. **Promote** — DEV→STAGE, GitOps mode 표시  
4. **ArgoCD Sync** — LIVE 또는 SIMULATED 매니페스트  
5. **Tags** — `payment, critical` 저장 후 Services 필터  
6. **YAML Explain** — rule-engine 하이라이트  

### 2.5 Audit / Incidents / Pipelines

| 메뉴 | 시연 |
|------|------|
| Audit | `CREATE_PROJECT`, `EXECUTE_WIZARD`, `PROMOTE_ENVIRONMENT` |
| Incidents | 이슈 스캔 → OPEN 목록 · rule 분석 |
| Pipelines | 로컬 시뮬 빌드 + GitHub Actions run (SIMULATED/LIVE) |
| Catalog | 카드 → Blueprint/Helm 상세 |

---

## 3. 실제로 동작하는 것 (DoD)

| 기능 | 시연 | 비고 |
|------|:----:|------|
| Dev Login / me · canMutate | ✅ | |
| Dashboard overview API 위젯 | ✅ | |
| Project CRUD · Archive · Clone | ✅ | |
| Catalog 목록 + 상세 | ✅ | |
| Wizard 7단계 + Saga | ✅ | SCM 없으면 sim |
| AI Recommend / Review / YAML Explain | ✅ | rule 기본 |
| Environment · Variable · Secret · Rotate | ✅ | |
| Promote + GitOps thin | ✅ | |
| Argo sync thin | ✅ | |
| Pipeline sim + GH Actions thin | ✅ | |
| Audit | ✅ | |
| Incident scan/ACK/resolve | ✅ | |
| Notifications bell | ✅ | |
| Service tags | ✅ | |
| RBAC VIEWER 차단 | ✅ | Settings Members |
| ⌘K Palette | ✅ | |
| OpenAPI + CI + Playwright | ✅ | |

---

## 4. 시연에서 빼는 것

| 항목 | 이유 |
|------|------|
| AWS EKS / RDS / ALB 실생성 | 과금 · free-only |
| “항상 LIVE GitHub/K8s” 주장 | 미연결 시 SIMULATED 정직하게 말하기 |
| Multi-cluster / Vault Cloud | v2 |
| 긴 이론 강의 | 8분은 클릭 스토리 중심 |

**Fallback 멘트**

- GitHub rate limit / 미연결 → “Adapter는 있고 시연은 free-only 시뮬 경로”  
- k8s 없음 → “배포 상태 SIMULATED, 매니페스트·Saga는 동일”  
- Ollama 없음 → “기본 rule-engine, Settings에서 provider 상태 확인”

---

## 5. 5분 초압축 버전

시간이 없으면 **이것만**:

1. Login  
2. Dashboard 숫자  
3. Project 생성  
4. Wizard → Deploy 완료  
5. Service Detail Env + Promote 한 번  
6. Audit 한 줄  

---

## 6. 시연 직전 체크리스트

- [ ] API `:8080` health UP  
- [ ] Web `:3000` login 화면  
- [ ] Dev Login 성공 → Dashboard  
- [ ] Project 1개 생성  
- [ ] Wizard 끝까지 (또는 기존 READY 서비스 1개)  
- [ ] Service Detail Environments 표시  
- [ ] Audit에 최근 mutation  
- [ ] 벨 아이콘 열림  
- [ ] (선택) VIEWER 멤버 초대 데모 준비  

자동화 검증:

```bash
# 핵심 데모 플로우 (API :8080 기동 필요) — 최우선
bash scripts/verify-demo-flow.sh

# API 단위·통합
cd apps/api && ./gradlew test

# OpenAPI 동기화
bash scripts/check-openapi-sync.sh

# E2E (API 기동 필요)
cd apps/web && npm run test:e2e
```

검증 결과 기록: [`docs/demo/VERIFICATION.md`](VERIFICATION.md)

---

## 7. 관련 문서

| 문서 | 용도 |
|------|------|
| [README](../../README.md) | 설치 · 아키텍처 · 시연 링크 |
| [PRD-vs-Implementation](../status/PRD-vs-Implementation.md) | 구현 매트릭스 |
| [Screenshots](screenshots/README.md) | 화면 캡처 인덱스 |
| [Free-Only](../architecture/05-Free-Only-Constraints.md) | 비용 제약 |
| [OpenAPI](../api/openapi.yaml) | REST 스냅샷 |

---

## 8. 유지 규칙

1. 이 문서는 **구현이 끝난 경로만** 시연 스텝에 넣는다.  
2. 새 기능 시연에 넣을 때는 **표 §3 DoD** 와 **§1 스크립트 표** 를 같이 수정한다.  
3. SIMULATED 는 숨기지 않는다 — 플랫폼 설계 포인트로 말한다.
