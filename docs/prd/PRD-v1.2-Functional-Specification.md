# Nimbus Platform PRD v1.2

# Functional Specification

**Version:** 1.2  
**목표:** Cursor / Claude Code / Copilot에 입력 가능한 구현 수준 기능 명세

---

# 1. Scope

- Developer Portal
- Authentication
- Dashboard
- Service Wizard
- GitHub Integration
- CI/CD Pipeline
- Background Job
- Notification
- Audit Log

---

# 2. Functional Map

```text
Authentication → Dashboard → Create Service → Service Wizard
→ AI Architecture Review → GitHub Integration
→ CI/CD Pipeline Generation → Terraform Job → Helm Generation
→ Deploy → Success
```

---

# 3. Developer Portal

## 목적

Infrastructure Console 이 아니라 **Developer Workspace**.

## Portal Layout

- Sidebar: LOGO, Dashboard, Projects, Infrastructure, CI/CD, Monitoring, AI Assistant, Team, Settings
- Main Content
- Notification, User

## Dashboard Widgets

| Widget | 예시 |
|--------|------|
| Service Summary | Running 15 / Stopped 2 / Deploying 1 |
| Cluster Health | Healthy 98% |
| Pipeline Status | Success 18 / Failed 1 / Running 2 |
| Incident | CrashLoop 2 / OOM 1 / CPU Alert 3 |
| Cost | 이번 달 예상 ₩128,000 **(v2)** |

## Dashboard Actions

Create Service · Deploy History · Rollback · View Logs · Open Grafana · Open GitHub

---

# 4. Authentication

## Login 지원

- GitHub OAuth
- Google OAuth
- Local Account

**MVP: GitHub OAuth만**

## Login Flow

```text
User → GitHub Login → OAuth → Callback → JWT 생성 → Dashboard
```

## User Information

GitHub ID, Name, Email, Avatar, Organization, Role

## Roles

`Admin` · `Platform Engineer` · `Developer` · `Viewer`

### Permission

| Role | 권한 |
|------|------|
| Admin | 모든 기능 |
| Platform Engineer | Terraform, Kubernetes, Secrets |
| Developer | Service 생성, Deploy, Logs |
| Viewer | 조회만 |

---

# 5. Portal Navigation

## Sidebar

Dashboard · Projects · Infrastructure · Pipelines · Monitoring · AI Assistant · Team · Settings

## Top Navigation

Search · Notification · Workspace · User

## Search targets

Repository · Deployment · Namespace · Pod · Pipeline · Member

## Notification (Realtime)

Deploy Success · Pipeline Failed · GitHub Connected · Incident · Alert

---

# 6. Service Wizard

## 목표

Kubernetes를 몰라도 서비스 생성.

## Wizard Steps

```text
STEP1 Project → STEP2 Framework → STEP3 Database
→ STEP4 Infrastructure → STEP5 Review → Deploy
```

### STEP1 Project Information

- Project Name, Description, Team, Visibility, Environment
- Environment: Dev / Stage / Production
- Name validation: 영문, 숫자, `-` 만, 최대 30자

### STEP2 Framework

**Backend:** Spring Boot, Node, NestJS, Go, Python FastAPI  
**Frontend:** React, Next.js, Vue, Angular  
**Runtime:** Java 21, Node 22, Python 3.13  
**Container:** Dockerfile 자동 생성

### STEP3 Database

None · Postgres · MySQL · Mongo · Redis  
(v2: RabbitMQ, Kafka, Elastic, OpenSearch)

### STEP4 Infrastructure

- Replica: 1~20
- CPU: 250m / 500m / 1000m / 2000m
- Memory: 512Mi / 1Gi / 2Gi / 4Gi
- Ingress Enable/Disable
- Domain: api.example.com

### STEP5 AI Architecture Review

예: Production + Replica 1 → 최소 2 Replica 권장

## Deploy Flow

```text
Deploy → Create Repository → Create Secrets → Generate YAML
→ Create Workflow → Terraform → Helm → Deploy
```

---

# 7. GitHub Integration

## 지원

Personal · Organization

## Repository Template (자동)

README · Dockerfile · .gitignore · LICENSE · CODEOWNERS · Issue Template · PR Template

## Branch Protection (자동)

main · Require PR · Require Review · Require Status Check

## GitHub Secret (자동)

AWS_ROLE · AWS_REGION · ECR · KUBE_CONFIG · ARGO_TOKEN

## Webhook (자동)

Push · PR · Release

## Repository 생성 순서

```text
Create Repository → Initialize README → Create Branch
→ Create Secret → Create Workflow → Commit → Push → Success
```

## Error Handling

| 상황 | 처리 |
|------|------|
| Repository 존재 | Retry · 이름 변경 제안 |
| Rate Limit | Queue · Retry |
| Permission 없음 | OAuth 재인증 |

---

# 8. CI/CD

목표: YAML 직접 작성하지 않음.  
자동 생성: `.github/workflows/deploy.yml`

## Pipeline

```text
Push → Test → Build → Docker Build → Push ECR
→ Deploy → Health Check → Notification
```

### Stages

| Stage | 내용 |
|-------|------|
| Test | Gradle Test 등 |
| Build | Jar Build |
| Docker | Buildx Multi Architecture |
| Registry | Amazon ECR (v1) · 향후 GHCR/DockerHub/Harbor |
| Deploy | Helm Upgrade 또는 ArgoCD Sync |
| Verify | Pod Running, Health API, Ingress HTTP 200 |
| Notify | Slack/Discord/Email **(v2)** |

## Pipeline History

Build ID · Commit · Author · Duration · Status · Artifact · Log

## Retry / Rollback

- Pipeline 실패 → Retry 버튼
- Deploy 실패 → 이전 Revision 복원

---

# 9. Background Job System

Deploy는 비동기. Queue 사용.

**Job types:** Deploy · GitHub · Terraform · Helm · Argo  

**Worker states:** RUNNING · SUCCESS · FAILED · RETRY

---

# 10. Audit Log

모든 이벤트 기록.

예:

- 홍길동 · Create Repository · Success · 13:22
- Deploy · Failed · Health Check Timeout

검색: User · Date · Project · Action

---

# 11. 예외 처리

| 상황 | 처리 방식 |
|------|-----------|
| GitHub API 실패 | Exponential Backoff 최대 3회 |
| Repository 이름 중복 | 새 이름 제안 |
| GitHub Rate Limit | Queue 순차 처리 |
| Helm 배포 실패 | 이전 Release Rollback |
| Health Check 실패 | Deployment 중단 + 원인 로그 |
| OAuth Token 만료 | 재인증 요청 |
| Kubernetes API Timeout | 재시도 후 관리자 알림 |

---

# 12. MVP 완료 기준

- [x] GitHub OAuth 로그인
- [x] Dashboard 구현
- [x] Service Wizard (5단계)
- [x] GitHub Repository 자동 생성
- [x] Dockerfile 자동 생성
- [x] GitHub Actions Pipeline 자동 생성
- [x] Kubernetes 배포 요청 생성
- [x] Audit Log 기록
- [x] 비동기 Job Queue 동작
- [x] 기본 오류 처리 및 재시도

---

# 13. Future Platform Engineering Features (권장)

- Template Catalog
- Golden Path
- Environment Promotion (dev → staging → production)
- Self-Service Secret Management (Vault / External Secrets)
- Deployment Approval (Production)
- Developer Scorecard (coverage, 배포 성공률, MTTR)
