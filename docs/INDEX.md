# Document Index

전체 설계 문서 목록.  
**충돌 시:** [architecture/03-Canonical-Decisions.md](architecture/03-Canonical-Decisions.md) 우선.

시작점: [docs/README.md](README.md) · 한 장 요약: [prd/PRD-MASTER.md](prd/PRD-MASTER.md)

---

## Reading order

1. [Canonical Decisions](architecture/03-Canonical-Decisions.md)  
2. [PRD Master](prd/PRD-MASTER.md)  
3. [Glossary](architecture/04-Glossary.md)  
4. 필요 시 개별 PRD / API Spec  
5. [Monorepo Layout](architecture/02-Monorepo-Layout.md) → 구현  

---

## Architecture

| File | Topic |
|------|-------|
| [00-System-Overview](architecture/00-System-Overview.md) | E2E 구조, 스택, MVP 경계 |
| [01-Domain-Map](architecture/01-Domain-Map.md) | Aggregate, status, events, roles |
| [02-Monorepo-Layout](architecture/02-Monorepo-Layout.md) | 실제 코드 트리 |
| [03-Canonical-Decisions](architecture/03-Canonical-Decisions.md) | **정본 결정** |
| [04-Glossary](architecture/04-Glossary.md) | 용어 |

---

## PRD

| File | Topic |
|------|-------|
| [**PRD-MASTER**](prd/PRD-MASTER.md) | **통합 개요 (권장 진입)** |
| [v1.1 Overview](prd/PRD-v1.1-Project-Overview.md) | Vision, persona, epic, IA |
| [v1.2 Functional](prd/PRD-v1.2-Functional-Specification.md) | Portal, auth, wizard, CI/CD |
| [v1.3 Infrastructure](prd/PRD-v1.3-Infrastructure-Platform.md) | TF, Helm, GitOps, observability |
| [v1.4 AI](prd/PRD-v1.4-AI-Native-Platform.md) | Agents, context, guardrail |
| [v1.5A Database](prd/PRD-v1.5A-Database-Design.md) | ERD, tables, Redis |
| [v1.5C Frontend](prd/PRD-v1.5C-Frontend-Design.md) | DX, screens, AC |
| [v1.5D Roadmap](prd/PRD-v1.5D-Engineering-Roadmap.md) | 16주, KPI, DoD |

> v1.5B 는 API Engineering Spec 묶음으로 분리되어 `docs/api/` 에 있다.

---

## API Specs (구현 단위)

| File | APIs | Topic |
|------|------|-------|
| [API-01 Authentication](api/API-01-Authentication.md) | 9 | OAuth, JWT, RBAC |
| [API-02 Workspace](api/API-02-Workspace.md) | 16 | Team, Member, Invite |
| [API-03-01 Project Core](api/API-03-01-Project-Core.md) | 10 | Project aggregate |
| [API-03-02 Environment](api/API-03-02-Environment.md) | 12 | Env, Promote, Health |
| [API-03-03 Variable & Secret](api/API-03-03-Variable-Secret.md) | 14 | Config, encryption |
| [API-03-04 Metadata](api/API-03-04-Project-Metadata.md) | 14 | Label, Tag, Annotation |
| [API-03-05 Service Catalog](api/API-03-05-Service-Catalog.md) | 14 | Template, Blueprint |
| [API-04-01 Service Wizard](api/API-04-01-Service-Wizard-Core.md) | 10 | Workflow engine |
| [API-04-02 AI Recommendation](api/API-04-02-AI-Recommendation.md) | 14 | Decision engine |
| [API-04-03 Provisioning](api/API-04-03-Provisioning-Orchestration.md) | 12 | Saga, job, rollback |
| [API-05-01 GitHub](api/API-05-01-GitHub-Integration.md) | 16 | SCM provider |

구현 순서 권장: 01 → 02 → 03-01 → 03-05 → 04-01 → 04-03 → 05-01 → 03-02/03 → 04-02

---

## Counts

| 구분 | 개수 |
|------|------|
| Architecture | 5 |
| PRD (+ Master) | 8 |
| API Spec | 11 |
| Docs hub | README + INDEX |

---

## Note on revisions

초안 작성 중 바뀐 것 (정본 반영 완료):

- Project-only → **Project → Service → Environment**
- 직접 apply 뉘앙스 → **GitOps (ArgoCD)**
- Wizard CRUD → **Job + Saga**
- AI 챗 → **Decision Engine**
- Spring Boot 3.5 표기 → **4.0.x (코드)**
- Wizard 5/6 step → **7 step**
