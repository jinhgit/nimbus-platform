# System Overview

> 원본 PRD와 함께 보면 좋은 누적 맵: [Design Evolution Map](03-Canonical-Decisions.md)

## 1. Product

**Nimbus Platform** — AI Native Internal Developer Platform.

참고 제품 조합:

- Backstage — Portal / Catalog 감성
- Port — Blueprint / 소프트웨어 카탈로그 UX
- Humanitec — 플랫폼 오케스트레이션
- GitHub + ArgoCD — Source of Truth + GitOps
- AI Ops — Platform Engineer Agent

---

## 2. End-to-end

```text
Developer
    │
Next.js Portal (Developer Workspace)
    │
Spring Boot Platform API
    │
    ├── Auth / Workspace / Project / Service
    ├── Environment / Config / Metadata / Catalog
    ├── Service Wizard (Workflow)
    ├── AI Decision Engine
    └── Provisioning Engine (Saga)
            │
            ├── GitHub (SCM Provider)
            ├── Terraform (modules / files)
            ├── Helm (charts / values)
            ├── ArgoCD (Application / Sync)
            └── Kubernetes (EKS or k3d/kind)
                    │
            Prometheus / Grafana / Loki
```

---

## 3. Critical decisions (short)

| 주제 | 결정 |
|------|------|
| Deploy path | TF → Git → ArgoCD → K8s (직접 kubectl apply 지양) |
| Deploy unit | **Service** (Project는 컨텍스트) |
| Wizard | 비동기 Job + Step rollback |
| AI | Context + Guardrail + multi-advisor |
| SCM | `GitProvider` 추상화, MVP GitHub |

---

## 4. Stack (as implemented / target)

| Layer | Stack |
|-------|-------|
| Web | Next.js 15, TypeScript, Tailwind |
| API | Java 21, **Spring Boot 4.0.x**, Security, JPA |
| Data | PostgreSQL 16, Redis 7 |
| Jobs | Spring Async (MVP) → RabbitMQ → Kafka (later) |
| Infra code | Terraform modules, Helm, ArgoCD |
| Cluster | MVP: k3d/kind · Target: EKS |
| AI | AIProvider adapters (Gemini/Groq/…), Guardrail |
| Observability | Prometheus, Grafana, Loki |

---

## 5. Security baseline

- OAuth2 + JWT + RBAC
- Secret: AES (MVP) → KMS/Vault (v2)
- Prompt secret masking
- Webhook HMAC
- Soft delete + Audit

---

## 6. MVP boundary

**In:** Auth 연결, Workspace/Project/Service, Wizard+Provision Job, Repo/Actions/파일 생성, 로컬·단일 클러스터 경로, AI Review 일부, Audit, Dashboard skeleton  

**Out:** Multi-cloud 운영, FinOps full, Auto Healing, OPA/Vault 본격, Marketplace

---

## 7. Backend package map

```text
io.nimbus.platform
  common · health
  auth · workspace · project · catalog
  wizard · ai · provision · github
```

패턴: `Controller → Facade → Service → Repository`  
응답: `ApiResponse<T>` · DTO only
