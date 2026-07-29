# Nimbus Platform PRD v1.5A

# Database Design

**Version:** 1.0 (Engineering PRD)  
**DB Role:** 플랫폼 상태 저장소 (실제 K8s 리소스 SoT가 아닌 **메타데이터**)

---

# 1. 목적

Nimbus는 GitHub · Terraform · Kubernetes · Helm · ArgoCD · AI · Monitoring 을 관리한다.  
DB에는 플랫폼이 관리하는 **메타데이터**를 저장한다.

---

# 2. Database Overview

| 스토어 | 용도 |
|--------|------|
| **PostgreSQL 16** | 영속 도메인 데이터 |
| **Redis** | Queue · Cache · AI Cache · Session |

관리 도메인: User · Workspace · Project · Deployment · Pipeline · Repository · Cluster · Namespace · Audit · AI · Incident

---

# 3. ERD (논리)

```text
User
 └── Workspace
       ├── Team
       ├── Member
       └── Project
             ├── Repository
             ├── Pipeline
             ├── Deployment
             ├── Service
             ├── Environment
             ├── AI Review
             └── Incident
```

---

# 4. UUID Strategy

모든 PK: `UUID`  
이유: URL 안전 · Merge 충돌 감소 · Multi Cluster/Cloud 대응

---

# 5. Common Columns

모든 테이블 공통:

```text
id · created_at · updated_at · created_by · updated_by · deleted_at · version
```

- `version`: Optimistic Lock
- `deleted_at`: Soft Delete (Hard Delete 금지)

---

# 6. users

| 컬럼 | 타입 |
|------|------|
| id | UUID |
| github_id | VARCHAR |
| email | VARCHAR |
| name | VARCHAR |
| avatar_url | TEXT |
| role | ENUM |
| status | ENUM |
| last_login | TIMESTAMP |

**Role:** ADMIN · PLATFORM_ENGINEER · DEVELOPER · VIEWER  
**Status:** ACTIVE · INACTIVE · PENDING  
**Index (Unique):** github_id, email

---

# 7. workspace

```text
id · name · slug · owner_id · description · created_at
```

예: Backend Team · DevOps Team · AI Team

---

# 8. project (핵심 Entity)

```text
id · workspace_id · name · description · framework · language
runtime · visibility · status · created_at
```

Framework 예: Spring Boot · NestJS · FastAPI · Next.js  
**Status:** CREATING · READY · FAILED · ARCHIVED

---

# 9. repository

```text
id · project_id · github_repo_id · owner · repo_name
url · default_branch · visibility · created_at
```

State: CREATING · READY · FAILED

---

# 10. environment

Types: DEV · STAGE · PRODUCTION  

```text
project_id · type · namespace · cluster_id · domain · status
```

---

# 11. cluster

```text
name · provider · region · kubernetes_version · endpoint · status
```

Provider: AWS · NCP · AKS · GKE

---

# 12. namespace

```text
cluster_id · name · quota_cpu · quota_memory · status
```

예: team-payment · team-search · team-auth

---

# 13. deployment (핵심)

```text
project_id · environment · helm_release · revision · image_tag
replica · cpu · memory · deploy_status · started_at · finished_at
```

**Deploy Status:** PENDING · RUNNING · SUCCESS · FAILED · ROLLBACK

---

# 14. pipeline

```text
project_id · github_run_id · workflow_name · commit_sha · branch · status · duration
```

Status: RUNNING · FAILED · SUCCESS · CANCELLED

---

# 15. incident

```text
deployment_id · severity · type · summary · root_cause · ai_report · status · resolved_at
```

Severity: LOW · MEDIUM · HIGH · CRITICAL

---

# 16. ai_review

```text
project_id · architecture_score · security_score · performance_score
availability_score · cost_score · recommendation · llm_provider · latency · token_usage
```

---

# 17. ai_prompt

```text
user_id · project_id · type · prompt · response · latency · input_token · output_token · cost
```

Type: YAML · INCIDENT · REVIEW · CHAT

---

# 18. audit_log

```text
actor · action · resource · resource_id · ip · user_agent · result · created_at
```

Action 예: LOGIN · CREATE_PROJECT · DEPLOY · ROLLBACK · DELETE

---

# 19. job_queue

```text
type · status · payload · retry_count · started_at · finished_at
```

Type: DEPLOY · TERRAFORM · HELM · ARGOCD · GITHUB · AI

---

# 20. Redis 구조

```text
session: · user: · ai: · job: · lock: · pipeline: · deploy:
```

TTL: Session 24h · AI 30m · Pipeline 6h

---

# 21. 인덱스 전략

필수:

- project.name
- deployment.status
- pipeline.status
- incident.severity
- repository.github_repo_id
- user.email

Composite: `(project_id, status)` — Deployment 조회 최적화

---

# 22. Soft Delete

`deleted_at` 사용 · Hard Delete 금지 · Audit 보존

---

# 23. Event Flow (Create Project)

```text
Create Project → Project 저장 → Repository 생성
→ Pipeline 생성 → Deployment 생성 → Audit 생성
```

---

# 24. 데이터 생명주기

```text
Project → Repository → Deployment → Incident → Archive → Soft Delete
```

---

# 25. DB Acceptance Criteria

### User

- GitHub OAuth 사용자 저장
- Role 변경 가능

### Project

- Workspace 종속
- Soft Delete 지원

### Deployment

- Revision 관리
- Rollback 이력 보존

### AI

- Prompt/Response 저장
- 토큰 사용량 집계

### Audit

- 모든 주요 액션 기록

---

# 26. 개선 제안 (대표 포트폴리오 수준)

1. **event_log** — `deployment.created`, `pipeline.failed`, `incident.detected` 등 도메인 이벤트
2. **service_template** — Catalog 버전 관리
3. **gitops_manifest** — Helm Values ↔ Commit SHA ↔ Argo Revision 연결
4. 핵심 테이블에 **cluster_id** 명시 (멀티 클러스터 대비)
