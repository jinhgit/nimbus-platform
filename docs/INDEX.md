# Document Index

Nimbus 설계 문서 전체 목록.  
모두 유효한 자료이고, Master / Evolution Map 은 **위에 얹은 안내 레이어**다.

허브: [README.md](README.md)  
한 장 요약: [prd/PRD-MASTER.md](prd/PRD-MASTER.md)  
누적 맵: [architecture/03-Canonical-Decisions.md](architecture/03-Canonical-Decisions.md)

---

## 읽는 방법 (예시)

**스토리부터**

1. PRD v1.1 → v1.2 → v1.3 → v1.4 → v1.5*  
2. API-01 부터 구현 스펙  
3. Master 로 한 번 더 조망  

**구현부터**

1. Monorepo Layout  
2. API-01 Auth …  
3. 막히면 해당 PRD 섹션 보강 열람  

**한눈에만**

1. PRD-MASTER  
2. Evolution Map  
3. Glossary  

**시연·포트폴리오**

1. [Demo Scenario](demo/DEMO-SCENARIO.md) — 10~15분 최종 시연 플로우  
2. Free-Only 제약과 함께 읽기  

**구현 현황**

1. [PRD vs 구현 매트릭스](status/PRD-vs-Implementation.md) — 문서 대비 코드 상태 · 실운영 로드맵  

---

## Status

| File | Topic |
|------|-------|
| [PRD-vs-Implementation](status/PRD-vs-Implementation.md) | PRD/API vs 구현, 로컬 보류, 실운영 P0~P2 |

---

## OpenAPI

| File | Topic |
|------|-------|
| [openapi.yaml](api/openapi.yaml) | 핵심 REST 스냅샷 · 런타임 `GET /v3/api-docs` |
| [api/README](api/README.md) | OpenAPI + Engineering Spec 목록 |

---

## Demo / Portfolio

| File | Topic |
|------|-------|
| [DEMO-SCENARIO](demo/DEMO-SCENARIO.md) | 최종 시연 10~15분, MVP 가능/제외, 면접 한 문장, 체크리스트 |

---

## Architecture

| File | Topic |
|------|-------|
| [00-System-Overview](architecture/00-System-Overview.md) | E2E 구조, 스택, MVP |
| [01-Domain-Map](architecture/01-Domain-Map.md) | Aggregate, status, events |
| [02-Monorepo-Layout](architecture/02-Monorepo-Layout.md) | 코드 트리 |
| [03 Design Evolution Map](architecture/03-Canonical-Decisions.md) | 설계 누적 레이어 정리 |
| [04-Glossary](architecture/04-Glossary.md) | 용어 |
| [05-Free-Only-Constraints](architecture/05-Free-Only-Constraints.md) | **완전 무료** 제약·대체 경로 |

---

## PRD (전부 유지)

| File | Topic |
|------|-------|
| [PRD-MASTER](prd/PRD-MASTER.md) | 통합 요약 (+ 레이어) |
| [v1.1 Overview](prd/PRD-v1.1-Project-Overview.md) | Vision, persona, epic, IA |
| [v1.2 Functional](prd/PRD-v1.2-Functional-Specification.md) | Portal, auth, wizard, CI/CD |
| [v1.3 Infrastructure](prd/PRD-v1.3-Infrastructure-Platform.md) | TF, Helm, GitOps, observability |
| [v1.4 AI](prd/PRD-v1.4-AI-Native-Platform.md) | Agents, context, guardrail |
| [v1.5A Database](prd/PRD-v1.5A-Database-Design.md) | ERD, tables, Redis |
| [v1.5C Frontend](prd/PRD-v1.5C-Frontend-Design.md) | DX, screens, AC |
| [v1.5D Roadmap](prd/PRD-v1.5D-Engineering-Roadmap.md) | 16주, KPI, DoD |

API 쪽 Engineering Spec 묶음은 초기에 v1.5B 흐름으로 이어져 `docs/api/` 에 있다.

---

## API Specs (PRD 위에 얹힌 구현 단위)

| File | APIs | Topic |
|------|------|-------|
| [API-01 Authentication](api/API-01-Authentication.md) | 9 | OAuth, JWT, RBAC |
| [API-02 Workspace](api/API-02-Workspace.md) | 16 | Team, Member, Invite |
| [API-03-01 Project Core](api/API-03-01-Project-Core.md) | 10 | Project |
| [API-03-02 Environment](api/API-03-02-Environment.md) | 12 | Env, Promote |
| [API-03-03 Variable & Secret](api/API-03-03-Variable-Secret.md) | 14 | Config |
| [API-03-04 Metadata](api/API-03-04-Project-Metadata.md) | 14 | Label, Tag |
| [API-03-05 Service Catalog](api/API-03-05-Service-Catalog.md) | 14 | Template, Blueprint |
| [API-04-01 Service Wizard](api/API-04-01-Service-Wizard-Core.md) | 10 | Workflow |
| [API-04-02 AI Recommendation](api/API-04-02-AI-Recommendation.md) | 14 | Decision engine |
| [API-04-03 Provisioning](api/API-04-03-Provisioning-Orchestration.md) | 12 | Saga, job |
| [API-05-01 GitHub](api/API-05-01-GitHub-Integration.md) | 16 | SCM |

구현 시 참고 순서 예: 01 → 02 → 03-01 → 03-05 → 04-01 → 04-03 → 05-01 → 나머지

---

## 개수

| 구분 | 개수 |
|------|------|
| Architecture | 5 |
| PRD (+ Master) | 8 |
| API Spec | 11 |

---

## 설계가 쌓인 축 (삭제 아님)

| 축 | 더해진 내용 |
|----|-------------|
| 도메인 | Project + **Service** + Environment |
| 배포 | Helm/TF + **GitOps(ArgoCD)** |
| Wizard | 입력 단계 + **Preview/Provision Job** |
| AI | YAML/Chat + **Decision Engine / Agents** |
| 스택 | 문서 초안 + **레포 실제 Spring Boot 4 / Next 15** |
