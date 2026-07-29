# API-04-01 Service Wizard Core

**Nimbus Platform · Version 2.0**  
**핵심 차별점:** Wizard = **Workflow Engine / Orchestrator** (CRUD 아님)

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

기반 기능(Auth ~ Catalog)은 모두 Service Wizard를 위해 존재한다.

Wizard 자동 수행:

- Template 선택 · AI 분석
- Repository · GitHub Actions 생성
- Helm Values · Terraform Variables 생성
- ArgoCD Application 생성 · Deployment 시작

---

# 2. Architecture

```text
Developer → Service Wizard → Workflow Engine
  → AI · GitHub · Infrastructure (Terraform / Helm / ArgoCD)
  → Deployment
```

**Job 기반 Orchestration** · API Thread에서 장시간 작업 금지

---

# 3. Wizard Steps (UI 7단계)

```text
Step1 Service Info → Step2 Template → Step3 Infrastructure
→ Step4 AI Review → Step5 Preview → Step6 Provision → Step7 Complete
```

## Wizard Status

```text
DRAFT · VALIDATING · PROVISIONING · DEPLOYING
COMPLETED · FAILED · CANCELLED
```

---

# 4. API List (10)

| API | Method |
|-----|--------|
| Create Wizard | POST |
| Get Wizard | GET |
| Update Wizard | PATCH |
| Validate Wizard | POST |
| Preview Blueprint | POST |
| Execute Wizard | POST |
| Wizard Status | GET |
| Cancel Wizard | POST |
| Wizard Logs | GET |
| Wizard History | GET |

---

# 5. Create Wizard

```http
POST /api/v1/service-wizard
```

권한: `SERVICE_CREATE`

```json
{
  "projectId": "uuid",
  "serviceName": "payment-service",
  "templateId": "uuid"
}
```

| 항목 | 조건 |
|------|------|
| serviceName | 3~50자 |
| templateId | 필수 |
| projectId | 필수 |

```json
{ "wizardId": "uuid", "status": "DRAFT" }
```

Session 생성 · Audit

---

# 6. Update Wizard

```http
PATCH /api/v1/service-wizard/{wizardId}
```

수정: Template · Runtime · Database · Cache · Deployment · Environment  
**DRAFT 상태만** 수정 가능

---

# 7. Validate

```http
POST /api/v1/service-wizard/{wizardId}/validate
```

검사: Project · Service Name · Repository · Cluster · Template · Environment

```json
{
  "valid": true,
  "warnings": ["Repository already exists."]
}
```

---

# 8. Preview Blueprint

```http
POST /api/v1/service-wizard/{wizardId}/preview
```

생성 미리보기: Blueprint · Helm Values · Terraform Variables · GitHub Actions · Repo Structure

```json
{
  "repository": "payment-service",
  "runtime": "Spring Boot",
  "helm": "generated",
  "terraform": "generated"
}
```

---

# 9. Execute (핵심)

```http
POST /api/v1/service-wizard/{wizardId}/execute
```

```text
Create Job → Queue → Worker → Provision
```

```json
{ "jobId": "uuid", "status": "PROVISIONING" }
```

동기 처리 절대 금지.

---

# 10. Job Status / Cancel / Logs / History

```http
GET  /api/v1/service-wizard/jobs/{jobId}
POST /api/v1/service-wizard/jobs/{jobId}/cancel
GET  /api/v1/service-wizard/jobs/{jobId}/logs
GET  /api/v1/service-wizard/history
```

Status 예:

```json
{
  "status": "DEPLOYING",
  "progress": 64,
  "currentStep": "Generate Helm"
}
```

Cancel 가능: VALIDATING · PROVISIONING  
Deploy 이후 취소 불가

History Filter: User · Project · Status · Date

---

# 11. Provision Workflow (내부)

```text
Validate → AI Recommendation → Blueprint Generate
→ Repository Create → GitHub Actions → Terraform Variables
→ Helm Values → ArgoCD Application → Deployment Request → Complete
```

## Job State

```text
QUEUED · RUNNING · WAITING · SUCCESS · FAILED · ROLLBACK · CANCELLED
```

---

# 12. Entity / DTO

```java
// Wizard
UUID id; WizardStatus status; Project project; Service service;
Template template; JobId jobId; LocalDateTime createdAt;

// WizardJob
UUID id; Wizard wizard; JobStatus status; Integer progress; String currentStep;
```

DTO: CreateWizardRequest · UpdateWizardRequest · WizardResponse · WizardStatusResponse · WizardLogResponse

---

# 13. Error Codes

| Code | 설명 |
|------|------|
| WIZARD001 | Wizard 없음 |
| WIZARD002 | Template 없음 |
| WIZARD003 | Validation 실패 |
| WIZARD004 | Job 실패 |
| WIZARD005 | 이미 실행됨 |
| WIZARD006 | 취소 불가 |
| WIZARD007 | Blueprint 생성 실패 |

---

# 14. Events / Audit

Events: wizard.created · validated · executed · completed · failed · cancelled  
Audit: WIZARD_CREATED · UPDATED · EXECUTED · CANCELLED · COMPLETED

---

# 15. Sequence

```text
Developer → Wizard API → Validation → Job Queue → Worker
→ Blueprint → GitHub → Terraform → Helm → ArgoCD → Deployment → Completed
```

---

# 16. 구현 지침

```text
wizard/controller|facade|service|repository|dto|entity|workflow|job|validator|event|exception
```

```java
public interface WizardStep {
    String getName();
    void execute(WizardContext context);
    void rollback(WizardContext context);
}
```

- Step 독립 클래스 · 실패 시 rollback
- 장시간 작업 = 비동기 Job
- progress Step 완료마다 갱신
- 단계별 로그 UI 실시간
- 외부 시스템 Adapter (GitHub → GitLab 확장)

### Step 구성 예

```text
ValidateStep · AIRecommendationStep · BlueprintStep
RepositoryStep · PipelineStep · TerraformStep
HelmStep · ArgoCDStep · DeploymentStep
```

---

# 17. Acceptance Criteria

- [ ] Wizard 생성 · Step 저장 · Validation · Preview
- [ ] Job 실행 · Progress · Logs · Cancel
- [ ] Event · Audit
