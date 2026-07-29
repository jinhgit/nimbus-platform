# API-04-03 Provisioning & Orchestration

**Nimbus Platform · Version 2.0**  
**심장부:** Saga Pattern 기반 Orchestration Engine

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

책임:

- GitHub Repository / Actions 생성
- Helm Chart · Terraform Variables · K8s Manifest · ArgoCD App
- Deployment 요청 · Rollback · Retry · Progress Tracking

```text
Wizard → Workflow Engine → Provisioning Engine → Task Queue → Worker
→ GitHub · Terraform · Helm · ArgoCD → Deployment → Monitoring
```

---

# 2. Saga Workflow

```text
Repository → Pipeline → Helm → Terraform → ArgoCD → Deployment
```

실패 시 → Rollback (이전 성공 Step 역순 보상)

---

# 3. Provision Status

```text
QUEUED · VALIDATING · GENERATING · PROVISIONING · DEPLOYING
VERIFYING · COMPLETED · FAILED · ROLLING_BACK · ROLLED_BACK · CANCELLED
```

## Step Status

`WAITING` · `RUNNING` · `SUCCESS` · `FAILED` · `SKIPPED` · `ROLLBACK`

---

# 4. API List (12)

| API | Method |
|-----|--------|
| Start Provision | POST |
| Get Provision Job | GET |
| List Jobs | GET |
| Cancel Job | POST |
| Retry Job | POST |
| Rollback Job | POST |
| Job Progress | GET |
| Job Logs | GET |
| Job Events | GET |
| Resume Job | POST |
| Verify Deployment | POST |
| Cleanup Resources | DELETE |

---

# 5. Start Provision

```http
POST /api/v1/provision
```

```json
{ "wizardId": "uuid" }
```

동기 금지 → Job Queue 등록

```json
{ "jobId": "uuid", "status": "QUEUED" }
```

Event: `provision.started`

---

# 6. Workflow 실행 순서

```text
Validate → Generate Blueprint → Create Repository
→ Generate GitHub Actions → Generate Helm → Generate Terraform Variables
→ Commit → ArgoCD Application → Deploy → Verify → Complete
```

---

# 7. Step Interface

```java
public interface ProvisionStep {
    String name(); // 또는 getName()
    void execute(ProvisionContext context); // 또는 StepResult execute(...)
    void rollback(ProvisionContext context);
}

public interface ProvisionWorkflow {
    void start(UUID wizardId);
}

public record ProvisionContext(
    UUID wizardId,
    UUID projectId,
    UUID serviceId,
    UUID environmentId,
    Map<String, Object> metadata
) {}
```

---

# 8. Job Status / Progress

```http
GET /api/v1/provision/jobs/{jobId}
```

```json
{
  "status": "DEPLOYING",
  "progress": 72,
  "currentStep": "Generate Helm"
}
```

Progress = 완료 Step / 전체 Step (예: 7/10 = 70%)

---

# 9. Cancel / Retry / Rollback

### Cancel

가능: QUEUED · VALIDATING · GENERATING  
불가: DEPLOYING

### Retry

**실패 Step부터** 재시작  
예: Repo OK · Pipeline OK · Terraform FAIL → Terraform부터

### Rollback 순서

```text
ArgoCD 삭제 → Terraform Cleanup → GitHub Branch 삭제 → Audit
```

**Repository는 삭제하지 않음**

---

# 10. Verify / Cleanup

```http
POST   /api/v1/provision/jobs/{jobId}/verify
DELETE /api/v1/provision/jobs/{jobId}
```

Verify: Repository · Pipeline · Helm · ArgoCD · Deployment · Health  
Cleanup: Temporary File · Job Cache · Artifact

---

# 11. Queue / Worker

| 버전 | Queue |
|------|-------|
| MVP | Spring Async (권장 RabbitMQ) |
| v2 | Kafka |

Worker: Queue → Step Execute → Event Publish → Progress Update

---

# 12. Retry Policy / Timeout

| 기능 | Retry | Timeout |
|------|------:|--------:|
| Repository | 3 | 30s |
| Workflow | 2 | — |
| Helm | 2 | 20s |
| Terraform | 2 | 60s |
| AI | 1 | — |
| Deployment | — | 300s |

Backoff: **Exponential**

---

# 13. Rollback Strategy

| 대상 | 전략 |
|------|------|
| Repository | Skip |
| Pipeline | Delete Workflow |
| Helm | Delete Release |
| Terraform | Destroy Generated Resource |
| ArgoCD | Delete Application |

---

# 14. Realtime

WebSocket: `/ws/provision`  
SSE: `/events/provision/{jobId}`

```json
{
  "jobId": "...",
  "progress": 81,
  "step": "Terraform",
  "status": "RUNNING"
}
```

---

# 15. Entity / DTO

```java
// ProvisionJob
UUID id; ProvisionStatus status; Integer progress; String currentStep; UUID wizardId;

// ProvisionStepExecution
UUID id; String name; StepStatus status; Long duration;
```

DTO: ProvisionRequest · ProvisionResponse · ProvisionStatusResponse · JobLogResponse · RollbackResponse

---

# 16. Error Codes

| Code | 설명 |
|------|------|
| PROVISION001 | Job 없음 |
| PROVISION002 | Validation 실패 |
| PROVISION003 | GitHub 실패 |
| PROVISION004 | Helm 생성 실패 |
| PROVISION005 | Terraform 실패 |
| PROVISION006 | ArgoCD 실패 |
| PROVISION007 | Deployment 실패 |
| PROVISION008 | Rollback 실패 |

---

# 17. Events / Audit

Events:

```text
provision.started · repository.created · pipeline.generated
helm.generated · terraform.generated · argocd.created
deployment.started · deployment.completed
rollback.started · rollback.completed
```

Audit: PROVISION_STARTED/COMPLETED/FAILED · ROLLBACK_STARTED/COMPLETED

---

# 18. Sequence

```text
Wizard → Provision API → Queue → Worker
→ Repository → Pipeline → Helm → Terraform → ArgoCD
→ Deploy → Verify → Complete
```

---

# 19. MVP vs v2 (무료 로컬 범위)

### MVP (무료)

- ✅ GitHub Repository / Actions 생성
- ✅ Helm Chart · Terraform 파일(`.tf`, `.tfvars`) 생성
- ✅ ArgoCD Application Manifest 생성
- ✅ k3d/kind 배포
- ✅ Progress · Retry / Rollback 시뮬레이션

### v2

- AWS EKS 실제 Provision · Terraform apply
- Route53 · ACM · ALB · RDS
- External Secrets · Vault

---

# 20. 구현 지침

```text
provision/controller|facade|service|workflow|job|queue|worker|step|dto|entity|repository|event|exception
```

- Step = SRP · 실패 시 역순 Rollback
- 외부 시스템 Adapter
- Event + Audit 동시
- WebSocket + SSE
- Job 재시작 · 실패 Step부터 Resume

---

# 21. Acceptance Criteria

- [ ] Job Queue · Step Progress · Retry · Rollback
- [ ] Verification · WebSocket Progress
- [ ] Saga · 비동기 · Event · Audit
