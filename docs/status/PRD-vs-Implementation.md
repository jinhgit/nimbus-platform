# PRD vs 구현 매트릭스

**기준일:** 2026-07-29  
**목적:** PRD v1.1~v1.5 / API 스펙 대비 **실제 코드 구현 상태**를 한곳에 고정한다.  
**전략 전환:** 로컬(k3d/kind 시연 마감)은 **보류**, 이후 우선순위는 **실운영 풀스택** 경로.

범례:

| 기호 | 의미 |
|:----:|------|
| ✅ | 구현·동작 (프로덕션 품질에 가깝거나 시연 충분) |
| 🔶 | 부분 구현 (생성/시뮬/API 일부, 실운영 미완) |
| ❌ | 미구현 |
| ⏸ | 의도적 보류 (v2 또는 로컬 경로) |

---

## 1. 총평

| 구분 | 상태 |
|------|------|
| PRD 문서 (v1.1~1.5 + Master) | **문서 완료** |
| 시연용 Thin Viable Platform (TVP) | **핵심 플로우 대체로 완료** |
| PRD 전체 기능 100% | **아님** |
| API-01~05 전 endpoint | **부분 구현** |
| 실운영 풀스택 (Prod path) | **기초만 — 본 문서 이후 집중 영역** |

> 면접/포트폴리오 표현:  
> “PRD v1.x로 플랫폼을 **설계**했고, free-only 하 **시연 MVP**를 구현한 뒤,  
> 실운영 경로(감사·환경 승격·시크릿·GitOps 실연동)로 **고도화** 중.”

---

## 2. PRD 버전별 매트릭스

### 2.1 PRD v1.1 — Overview / MVP

| 기능 | 상태 | 비고 |
|------|:----:|------|
| GitHub OAuth 로그인 | 🔶 | Dev Login 안정; OAuth App 설정 시 가능 |
| Project 생성 | ✅ | |
| GitHub Repository 자동 생성 | 🔶 | SCM 연결 시 실연동; 미연결 시 sim |
| GitHub Actions 템플릿 생성 | ✅ | 파일 생성 (Actions 실실행은 환경 의존) |
| Kubernetes Deployment | 🔶 | 로컬 k3d/kind 경로 + 데모 이미지 (**로컬 보류**) |
| Helm Chart 생성 | ✅ | 파일 생성 중심 |
| ArgoCD 연동 | 🔶 | Manifest 생성 수준, 실 Sync UI 약함 |
| AI YAML / Explain | 🔶 | Recommend + Architecture Review 위주 |
| Dashboard | ✅ | |
| Monitoring 링크 | 🔶 | demo 메트릭 + 선택적 Prom/Grafana |
| Audit Log | ✅ | 도메인·API·mutation 기록·UI (`/audit`) |
| Multi-cloud / Mesh / FinOps | ⏸ | PRD 제외(v2) |

### 2.2 PRD v1.2 — Functional Spec

| 기능 | 상태 | 비고 |
|------|:----:|------|
| Auth / JWT / Refresh | ✅ | |
| RBAC (역할 매트릭스 전체) | 🔶 | 기본 역할; 세밀 권한 검사 부족 |
| Workspace / Team / Member | 🔶 | 핵심 CRUD; Invite 풀 플로우 얇음 |
| Dashboard widgets | 🔶 | 기본 집계 |
| Service Wizard 7단계 | ✅ | |
| GitHub Integration | 🔶 | OAuth SCM + PAT + Adapter |
| CI/CD Pipeline | 🔶 | Job 시뮬 + workflow 파일 |
| Background Job Queue | 🔶 | `@Async` (Rabbit/Kafka 아님) |
| Notification (Realtime) | ❌ | |
| Audit Log 검색 | ✅ | `GET /api/v1/audit` 필터 (action/resource/actor) |
| Environment Promote | ❌ → **실운영 P1** | |

### 2.3 PRD v1.3 — Infrastructure

| 기능 | 상태 | 비고 |
|------|:----:|------|
| Terraform 파일 생성 | ✅ | |
| Terraform apply (EKS/VPC 실생성) | ⏸ | 과금·실운영 별 트랙 |
| Helm values/chart | ✅ | |
| Helm upgrade 실운영 | 🔶 | |
| ArgoCD Manifest | ✅ | |
| ArgoCD Auto Sync / Rollback UI | ❌ → **실운영 P1** | GitOps 본선 |
| Prometheus / Grafana / Loki | 🔶 | compose profile + 링크 |
| Resource Explorer | 🔶 | 인프라 페이지 수준 |
| Backup / DR | ❌ | |

### 2.4 PRD v1.4 — AI Native

| 기능 | 상태 | 비고 |
|------|:----:|------|
| Architecture Review | ✅ | rule-engine |
| Runtime/DB/Cache 추천 | ✅ | |
| YAML Generator/Explain 풀셋 | 🔶 | |
| Incident Analysis | ❌ → **실운영 P2** | |
| Cost Advisor | ⏸ | v2 |
| Ollama Provider 기본 | 🔶 | 구조 여지, 기본 rule |
| Multi-Agent / MCP | ⏸ | v2 |

### 2.5 PRD v1.5A — Database

| 기능 | 상태 | 비고 |
|------|:----:|------|
| User / Workspace / Project / Service | ✅ | |
| Soft Delete / UUID / version | ✅ | |
| Deployment / Pipeline 테이블 | 🔶 | 일부 엔티티 |
| Secret 암호화 (토큰) | ✅ | AES |
| audit_log 테이블 | ✅ | `audit_logs` 엔티티 (불변, soft-delete 없음) |
| incident / event_log | ❌ | |
| Redis 실사용 | 🔶 | 의존성 있음, 인메모리 폴백 중심 |

### 2.6 PRD v1.5C — Frontend

| 기능 | 상태 | 비고 |
|------|:----:|------|
| Login / Dashboard / Project / Wizard | ✅ | |
| Service Detail | ✅ | |
| Catalog / Pipelines / Monitoring / Logs | ✅ | |
| Settings (SCM OAuth) | ✅ | |
| Audit UI | ✅ | `/audit` 필터·테이블 |
| Command Palette ⌘K | ❌ | |
| shadcn 풀 디자인 시스템 | 🔶 | Tailwind 커스텀 |
| 전 화면 실시간 WS | 🔶 | SSE/폴링 일부 |

### 2.7 PRD v1.5D — Roadmap

| Phase | 상태 |
|-------|:----:|
| Phase 0~1 Foundation | ✅ |
| Phase 2 Platform Core (Wizard/GitHub) | 🔶~✅ |
| Phase 3 Infra 풀 | 🔶 |
| Phase 4 AI 풀 | 🔶 |
| Phase 5 Observability 풀 | 🔶 |
| Phase 6 Coverage/E2E | ❌ |

---

## 3. API 스펙 vs 구현 (요약)

| Spec | 주제 | 상태 |
|------|------|:----:|
| API-01 Auth | OAuth, JWT, RBAC | 🔶~✅ |
| API-02 Workspace | Team, Invite, Member | 🔶 |
| API-03-01 Project | CRUD, Archive, Clone | 🔶 (Clone 등 일부 약함) |
| API-03-02 Environment | Promote, Health | ❌~🔶 |
| API-03-03 Variable/Secret | Config, Rotation | ❌ |
| API-03-04 Metadata | Label, Tag | ❌ |
| API-03-05 Catalog | Template, Blueprint | 🔶 (시드+조회 중심) |
| API-04-01 Wizard | Workflow | ✅ 핵심 |
| API-04-02 AI | Decision Engine | 🔶 rule-engine |
| API-04-03 Provision | Saga, Rollback | 🔶 Async steps, 보상 약함 |
| API-05-01 GitHub | SCM Provider | 🔶 OAuth/PAT/Repo 생성 |

---

## 4. 시연 MVP 체크 (참고)

DEMO-SCENARIO 기준 핵심 플로우는 **구현됨**.  
다만 다수가 **파일 생성 · 시뮬 · 로컬 경로**이다.

로컬 보류 항목 예:

- kind/k3d 설치·시연 스크립트 고도화  
- 데모 메트릭 튜닝  
- 로컬 전용 DX (`make demo` 등)

→ **실운영 작업과 병행하지 않고 백로그 하단 유지.**

---

## 5. 실운영 풀스택 우선순위 (이후 로드맵)

로컬 보류 후, 아래 순서로 진행한다.

### P0 — 플랫폼 운영 기반 (즉시)

1. **Audit Log** — 모든 mutation 기록, 조회 API, 관리 UI  
2. **application-prod 프로필** — Postgres 필수, 시크릿 env, Actuator 제한  
3. **에러·권한 일관화** — Facade 진입 권한, 표준 ErrorCode

### P1 — 배포 운영 본선

4. **Environment 도메인** — DEV/STAGE/PROD 엔티티, Health  
5. **Environment Promote** — GitOps 브랜치/PR 트리거 설계·구현  
6. **Variable / Secret** — AES 저장, 마스킹, GitHub Secret Sync  
7. **Provision Saga 강화** — Step 상태 DB, 실패 시 보상 로그, Retry API  
8. **ArgoCD Application 실연동** (클러스터에 Argo 있는 환경 전제)

### P2 — 품질·관측·AI

9. **실 Docker build → 레지스트리 push** (환경 있을 때)  
10. **Incident + AI Analysis**  
11. **Ollama / 교체 가능 AIProvider 기본 경로**  
12. **테스트 커버리지 · OpenAPI 고정 · E2E**

### 명시적 비범위 (당분간)

- AWS EKS 자동 생성, Route53, ACM, ALB, RDS 과금 리소스  
- Multi-cluster 실운영  
- Vault Cloud / 상용 SaaS 필수 경로  

(필요 시 “클라우드 실운영 트랙” 문서를 별도 추가)

---

## 6. 이번 스프린트에서 완료한 실운영 작업

| 작업 | 상태 |
|------|------|
| 본 매트릭스 문서화 | ✅ |
| Audit Log 도메인·API·핵심 mutation 기록 | ✅ |
| Audit UI (`/audit`) | ✅ |
| application-prod.yml | ✅ |

**다음 (P1):** Environment 도메인 · Promote · Variable/Secret · Saga 강화 · ArgoCD 실연동

---

## 7. 관련 문서

- [PRD-MASTER](../prd/PRD-MASTER.md)  
- [DEMO-SCENARIO](../demo/DEMO-SCENARIO.md)  
- [05-Free-Only-Constraints](../architecture/05-Free-Only-Constraints.md)  
- [03 Evolution Map](../architecture/03-Canonical-Decisions.md)  
- [Engineering Roadmap v1.5D](../prd/PRD-v1.5D-Engineering-Roadmap.md)  

---

## 8. 갱신 규칙

- 기능 완료 시 해당 행 기호를 ✅/🔶로 수정하고 **기준일**을 올린다.  
- 실운영 트랙에서 범위가 바뀌면 §5 우선순위만 먼저 고친다.  
- 로컬 보류를 해제할 때는 §4에 “재개” 메모를 남긴다.
