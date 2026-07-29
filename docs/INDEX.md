# Nimbus Platform — Document Index

학습·설계된 전체 문서의 단일 인덱스입니다.  
구현 시 Cursor / Claude Code / Copilot 에 **해당 MD를 직접 입력**할 수 있는 수준으로 작성되어 있습니다.

---

## Reading Order (권장)

1. PRD v1.1 → Vision & Scope  
2. PRD v1.2 → Functional Spec  
3. PRD v1.3 → GitOps Infrastructure  
4. PRD v1.4 → AI Layer  
5. PRD v1.5A → Database  
6. PRD v1.5C → Frontend  
7. PRD v1.5D → Roadmap  
8. API-01 → API-05 순서로 구현  

---

## PRD

| File | Version | Topic |
|------|---------|-------|
| [PRD-v1.1-Project-Overview.md](prd/PRD-v1.1-Project-Overview.md) | 1.1 | Overview, Vision, Persona, Epic, IA, Architecture, MVP |
| [PRD-v1.2-Functional-Specification.md](prd/PRD-v1.2-Functional-Specification.md) | 1.2 | Portal, Auth, Dashboard, Wizard, GitHub, CI/CD, Job, Audit |
| [PRD-v1.3-Infrastructure-Platform.md](prd/PRD-v1.3-Infrastructure-Platform.md) | 1.3 | Terraform, Helm, ArgoCD, GitOps, Monitoring |
| [PRD-v1.4-AI-Native-Platform.md](prd/PRD-v1.4-AI-Native-Platform.md) | 1.4 | AI Agents, Prompt, Context, Cache, Fallback |
| [PRD-v1.5A-Database-Design.md](prd/PRD-v1.5A-Database-Design.md) | 1.5A | ERD, UUID, Tables, Redis, Indexes |
| [PRD-v1.5C-Frontend-Design.md](prd/PRD-v1.5C-Frontend-Design.md) | 1.5C | DX, Stack, Screens, AC |
| [PRD-v1.5D-Engineering-Roadmap.md](prd/PRD-v1.5D-Engineering-Roadmap.md) | 1.5D | 16 weeks, Sprint, KPI, DoD |

## API Specs

| File | APIs (approx) | Topic |
|------|---------------|-------|
| [API-01-Authentication.md](api/API-01-Authentication.md) | 9 | OAuth, JWT, RBAC |
| [API-02-Workspace.md](api/API-02-Workspace.md) | 16 | Workspace, Team, Member |
| [API-03-01-Project-Core.md](api/API-03-01-Project-Core.md) | 10 | Project Aggregate |
| [API-03-02-Environment.md](api/API-03-02-Environment.md) | 12 | Env, Promote, Health |
| [API-03-03-Variable-Secret.md](api/API-03-03-Variable-Secret.md) | 14 | Config, Secret, Rotation |
| [API-03-04-Project-Metadata.md](api/API-03-04-Project-Metadata.md) | 14 | Label, Tag, Annotation |
| [API-03-05-Service-Catalog.md](api/API-03-05-Service-Catalog.md) | 14 | Catalog, Blueprint |
| [API-04-01-Service-Wizard-Core.md](api/API-04-01-Service-Wizard-Core.md) | 10 | Wizard Workflow |
| [API-04-02-AI-Recommendation.md](api/API-04-02-AI-Recommendation.md) | 14 | AI Decision Engine |
| [API-04-03-Provisioning-Orchestration.md](api/API-04-03-Provisioning-Orchestration.md) | 12 | Saga, Job, Rollback |
| [API-05-01-GitHub-Integration.md](api/API-05-01-GitHub-Integration.md) | 16 | SCM Provider |

## Architecture

| File | Topic |
|------|-------|
| [00-System-Overview.md](architecture/00-System-Overview.md) | End-to-end system |
| [01-Domain-Map.md](architecture/01-Domain-Map.md) | Domains & Aggregates |

---

## Document Count

- PRD: **7**
- API Specs: **11**
- Architecture: **2**
- Root README + this INDEX

**Total core design docs: 20+**
