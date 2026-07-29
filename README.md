# Nimbus Platform

> **AI Native Internal Developer Platform (IDP)**

Spotify Backstage + Port + Humanitec + GitHub + ArgoCD + AI Ops 를 하나의 플랫폼으로 통합하는 Platform Engineering SaaS 설계 문서 저장소입니다.

**이 프로젝트는 Backstage 클론이 아닙니다.**  
목표는 **AI Native Platform Engineering Portal** 을 만드는 것입니다.

---

## Mission

> **Empower Developers, Automate Infrastructure.**

개발자가 인프라를 배우기 전에 서비스를 만들 수 있도록 한다.

## Vision

> 개발자의 생산성을 극대화하는 **AI Platform Engineer**

장기 확장: Platform Engineering · GitOps · AI Ops · FinOps · Multi Cloud

---

## Core Philosophy

| 원칙 | 설명 |
|------|------|
| **Developer First** | Infrastructure First 가 아닌 DX 우선 |
| **Zero YAML** | 사용자는 YAML을 직접 작성하지 않음 |
| **AI First** | Platform Engineer / SRE / DevOps 역할의 AI Agent |
| **GitOps First** | Terraform → GitOps Repo → ArgoCD → Kubernetes |
| **One Click Deploy** | Service Wizard 기반 오케스트레이션 |

---

## Domain Hierarchy

```text
Workspace
  └── Project          (비즈니스 컨텍스트)
        └── Service    (배포 단위 애플리케이션)
              └── Environment  (DEV / STAGE / PRODUCTION)
                    └── Deployment
```

```text
Service Catalog → Blueprint → Service Wizard → Provisioning (Saga)
  → GitHub · Terraform · Helm · ArgoCD · Kubernetes
```

---

## Document Map

### PRD (Product Requirements)

| 문서 | 설명 |
|------|------|
| [PRD v1.1](docs/prd/PRD-v1.1-Project-Overview.md) | Overview, Vision, Epic, IA, Architecture, MVP |
| [PRD v1.2](docs/prd/PRD-v1.2-Functional-Specification.md) | Portal, Auth, Wizard, GitHub, CI/CD, Job, Audit |
| [PRD v1.3](docs/prd/PRD-v1.3-Infrastructure-Platform.md) | Terraform, Helm, GitOps, ArgoCD, Observability |
| [PRD v1.4](docs/prd/PRD-v1.4-AI-Native-Platform.md) | AI Agents, Context Engine, Guardrails |
| [PRD v1.5A](docs/prd/PRD-v1.5A-Database-Design.md) | PostgreSQL ERD, Redis, Soft Delete |
| [PRD v1.5C](docs/prd/PRD-v1.5C-Frontend-Design.md) | Next.js UI/UX, DX First, Component System |
| [PRD v1.5D](docs/prd/PRD-v1.5D-Engineering-Roadmap.md) | 16-week plan, Sprint, KPI, DoD |

### API Engineering Specs (구현 가능 수준)

| 문서 | 설명 |
|------|------|
| [API-01 Authentication](docs/api/API-01-Authentication.md) | OAuth, JWT, RBAC, Session |
| [API-02 Workspace](docs/api/API-02-Workspace.md) | Team, Member, Invite, Role |
| [API-03-01 Project Core](docs/api/API-03-01-Project-Core.md) | Project CRUD, Archive, Clone, Favorite |
| [API-03-02 Environment](docs/api/API-03-02-Environment.md) | Cluster, Namespace, Promote, GitOps |
| [API-03-03 Variable & Secret](docs/api/API-03-03-Variable-Secret.md) | ConfigMap, Encryption, Rotation, Sync |
| [API-03-04 Project Metadata](docs/api/API-03-04-Project-Metadata.md) | Label, Tag, Annotation, AI Metadata |
| [API-03-05 Service Catalog](docs/api/API-03-05-Service-Catalog.md) | Golden Path, Blueprint, Template |
| [API-04-01 Service Wizard](docs/api/API-04-01-Service-Wizard-Core.md) | Workflow Engine, 7-step Wizard |
| [API-04-02 AI Recommendation](docs/api/API-04-02-AI-Recommendation.md) | Decision Engine, Context Builder |
| [API-04-03 Provisioning](docs/api/API-04-03-Provisioning-Orchestration.md) | Saga, Retry, Rollback, WebSocket |
| [API-05-01 GitHub Integration](docs/api/API-05-01-GitHub-Integration.md) | SCM Provider, Repo, Actions, Webhook |

### Architecture

| 문서 | 설명 |
|------|------|
| [System Overview](docs/architecture/00-System-Overview.md) | 전체 구조, 흐름, 기술 스택 요약 |
| [Domain Map](docs/architecture/01-Domain-Map.md) | 도메인·Aggregate·이벤트 맵 |

---

## Target Stack (요약)

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 15, TypeScript, shadcn/ui, Tailwind, Zustand, TanStack Query |
| Backend | Java 21, Spring Boot 3.5, Spring Security, JPA |
| Data | PostgreSQL 16, Redis |
| Infra | Terraform, Helm, ArgoCD, Amazon EKS (MVP: k3d/kind) |
| SCM | GitHub (GitProvider 추상화) |
| Observability | Prometheus, Grafana, Loki |
| AI | Gemini / Groq / OpenRouter / Ollama (AIProvider 어댑터) |

---

## KPI (v1)

| 항목 | 목표 |
|------|------|
| 서비스 생성 시간 | ≤ 1분 |
| Repository 생성 | ≤ 30초 |
| 배포 성공률 | ≥ 95% |
| AI Architecture Review | ≤ 5초 |
| Wizard Step | ≤ 6~7 |
| YAML 직접 작성 | 0줄 |

---

## Author

Nasuyu Yu

---

## License

Private personal project documentation. All rights reserved unless otherwise noted.
