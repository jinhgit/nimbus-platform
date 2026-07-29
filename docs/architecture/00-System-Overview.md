# Nimbus Platform — System Overview

## 1. Product Identity

**Nimbus Platform** = AI Native Internal Developer Platform

통합 대상:

- Spotify Backstage (Developer Portal / Catalog)
- Port (Blueprint / Software Catalog UX)
- Humanitec (Platform orchestration / Score 개념)
- GitHub (Source of Truth)
- ArgoCD (GitOps)
- AI Ops (Platform Engineer Agent)

---

## 2. End-to-End Flow

```text
Developer
    │
    ▼
Next.js Portal (Developer Workspace)
    │
    ▼
Spring Boot Platform API
    │
    ├── Auth / Workspace / Project / Service
    ├── Environment / Config / Metadata / Catalog
    ├── Service Wizard (Workflow Engine)
    ├── AI Decision Engine
    └── Provisioning Engine (Saga)
            │
            ├── GitHub (SCM Provider)
            ├── Terraform (modules / tfvars)
            ├── Helm (values / charts)
            ├── ArgoCD (Application / Sync)
            └── Kubernetes (EKS or k3d/kind)
                    │
                    ▼
            Prometheus / Grafana / Loki
```

---

## 3. Critical Architectural Decisions

### 3.1 GitOps First

```text
Terraform  →  GitOps Repository  →  ArgoCD  →  Sync  →  Kubernetes
```

Terraform 이 직접 `kubectl apply` 하지 않는다.

### 3.2 Project vs Service

```text
Project  = Business Context (예: Shopping Mall)
Service  = Deployable Unit (예: Payment API)
```

### 3.3 Environment = Infrastructure Context

```text
Service → Environment → Namespace → Cluster → Helm Values → ArgoCD → Deployment
```

### 3.4 Wizard = Workflow Engine

Service Wizard 는 CRUD 가 아니라 **Job 기반 Orchestrator** 이다.

### 3.5 AI = Decision Engine

LLM 채팅이 아니라 Context Builder + Guardrail + Multi-Advisor 구조.

### 3.6 SCM Abstraction

```text
GitProvider
  ├── GitHubAdapter (MVP)
  ├── GitLabAdapter (v2)
  └── BitbucketAdapter (v2)
```

---

## 4. Technology Stack

| Layer | Stack |
|-------|-------|
| Web | Next.js 15 App Router, TypeScript, shadcn/ui, Tailwind, Zustand, TanStack Query, RHF + Zod |
| API | Java 21, Spring Boot 3.5, Security 6, OAuth2, JWT RS256, JPA, MapStruct, SpringDoc |
| Data | PostgreSQL 16, Redis (session/cache/queue/AI) |
| Jobs | Spring Async (MVP) → RabbitMQ → Kafka (v2) |
| Infra | Terraform modules, Helm, ArgoCD |
| Cluster | MVP: k3d/kind · Prod: Amazon EKS · v2: NCP/AKS/GKE |
| AI | Gemini / Groq / OpenRouter / Ollama · AIProvider interface |
| Observability | Prometheus, Grafana, Loki, Alertmanager |

---

## 5. Security Baseline

- OAuth2 + JWT + RBAC
- Secret: AES-256 (MVP) → KMS/Vault (v2)
- Prompt 내 Secret 마스킹
- GitHub Webhook HMAC 검증
- IRSA / Least Privilege (infra)
- Soft Delete + Audit Log 전 영역

---

## 6. MVP Boundary

### Included

- GitHub OAuth / App 연동
- Workspace · Project · Service · Environment
- Service Wizard + Provision Job
- Repo / Actions / Helm / Argo Manifest 생성
- k3d/kind 배포
- AI Review / YAML / Incident (범위 내)
- Dashboard · Audit · Observability 링크

### Excluded (v2+)

- Multi-cluster / Multi-cloud 실운영
- FinOps full dashboard
- AI Auto Healing
- Service Mesh / Canary/BG 고급 배포 (설계상 전략 enum 은 존재)
- OPA / Vault production integration

---

## 7. Package / Service Boundaries (Backend)

권장 도메인 패키지:

```text
auth · workspace · project · environment · configuration
metadata · catalog · wizard · ai · provision · github
```

공통:

```text
Controller → Facade → Service → Repository
DTO (record) only · ApiResponse<T>
Domain Events + Audit on every mutation
```
