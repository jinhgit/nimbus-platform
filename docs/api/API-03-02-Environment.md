# API-03-02 Environment

**Nimbus Platform · Version 2.0**

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

Environment = Service의 **Infrastructure Context** (문자열이 아님)

```text
Service → Environment → Namespace → Cluster
       → Helm Values → ArgoCD → Deployment
```

포함: Cluster · Namespace · Domain · Helm Values · Resource Policy · GitOps Branch · Deployment Strategy · Secret · ConfigMap

Environment = Deployment의 부모 Entity · **Aggregate Root**

Child: Deployment · Config · Variable · Secret · HelmValues

---

# 2. Type / Status

**Type:** DEV · STAGE · PRODUCTION  

**Status:** CREATING · READY · DEPLOYING · FAILED · ARCHIVED

---

# 3. API List (12)

| API | Method |
|-----|--------|
| Create Environment | POST |
| Get Environment | GET |
| List Environments | GET |
| Update Environment | PATCH |
| Delete Environment | DELETE |
| Promote Environment | POST |
| Archive Environment | POST |
| Restore Environment | POST |
| Environment Health | GET |
| Resource Policy | PATCH |
| Domain Mapping | PATCH |
| Namespace Detail | GET |

---

# 4. Create Environment

```http
POST /api/v1/services/{serviceId}/environments
```

권한: `ENVIRONMENT_CREATE`

```json
{
  "type": "DEV",
  "clusterId": "uuid",
  "namespace": "shopping-dev",
  "domain": "dev.shopping.demo",
  "deploymentStrategy": "ROLLING"
}
```

| 필드 | 조건 |
|------|------|
| type | ENUM |
| clusterId | 필수 |
| namespace | DNS-1123 |
| domain | FQDN |
| deploymentStrategy | ENUM |

자동: Namespace 등록 · Helm Values · Config · Deployment Placeholder · Audit  
실제 K8s Namespace는 **비동기 Infrastructure Job** 으로 처리

Event: `environment.created` · Audit: `ENVIRONMENT_CREATED`

---

# 5. Get / List

```http
GET /api/v1/environments/{environmentId}
GET /api/v1/services/{serviceId}/environments
```

List 정렬: DEV → STAGE → PRODUCTION

---

# 6. Update

```http
PATCH /api/v1/environments/{environmentId}
```

수정 가능: Domain · Strategy · CPU · Memory · Replica · Autoscaling  
수정 불가: Type · Namespace · Cluster  
Production Cluster 변경 금지

---

# 7. Delete

```http
DELETE /api/v1/environments/{environmentId}
```

Soft Delete · Deployment 존재 시 불가 · 204

---

# 8. Promote (핵심)

```http
POST /api/v1/environments/{environmentId}/promote
```

```json
{ "target": "PRODUCTION" }
```

```text
DEV → STAGE → PRODUCTION
```

GitOps 기반:

```text
Git Commit → PR → Review → Merge → ArgoCD Sync → Deploy
```

직접 kubectl 배포 금지

```json
{ "jobId": "uuid", "status": "PROMOTING" }
```

Event: `environment.promoted`

---

# 9. Archive / Restore

Archive → `ARCHIVED` · Deploy 금지  
Restore → `READY`

---

# 10. Health

```http
GET /api/v1/environments/{id}/health
```

```json
{
  "status": "HEALTHY",
  "pods": 5,
  "readyPods": 5,
  "restartCount": 0,
  "cpu": "42%",
  "memory": "61%"
}
```

---

# 11. Resource Policy

```http
PATCH /api/v1/environments/{id}/resources
```

```json
{
  "cpu": "500m",
  "memory": "512Mi",
  "replica": 2,
  "autoscaling": true,
  "minReplica": 2,
  "maxReplica": 10
}
```

CPU: 100m~4000m · Memory: 128Mi~8Gi  
HPA 시 Replica ≥ MinReplica  
Helm Values 동기화 가능

---

# 12. Domain Mapping

```http
PATCH /api/v1/environments/{id}/domain
```

- 중복 Domain 불가 · HTTPS 필수 · Wildcard v2
- 메타데이터만 관리 · 실제 반영은 Pipeline

---

# 13. Namespace Detail

```http
GET /api/v1/environments/{id}/namespace
```

```json
{
  "cluster": "eks-prod",
  "namespace": "payment-prod",
  "resourceQuota": { "cpu": "4", "memory": "8Gi" },
  "networkPolicy": "enabled"
}
```

---

# 14. Deployment Strategy

```java
enum DeploymentStrategy { ROLLING, BLUE_GREEN, CANARY }
```

---

# 15. GitOps Branch Mapping

| Env | Branch |
|-----|--------|
| DEV | `develop` |
| STAGE | `release/*` |
| PRODUCTION | `main` |

---

# 16. Helm Values (자동 예)

```yaml
replicaCount: 2
resources:
  cpu: 500m
  memory: 512Mi
autoscaling:
  enabled: true
domain:
  host: api.demo.com
```

---

# 17. Entity

```java
// Environment
UUID id
EnvironmentType type
Cluster cluster
String namespace
String domain
DeploymentStrategy strategy
EnvironmentStatus status
```

DTO: CreateEnvironmentRequest · EnvironmentResponse · EnvironmentSummary · ResourcePolicyRequest

---

# 18. Error Codes

| Code | 설명 |
|------|------|
| ENV001 | Environment 없음 |
| ENV002 | Namespace 중복 |
| ENV003 | Domain 중복 |
| ENV004 | Cluster 없음 |
| ENV005 | Promotion 실패 |
| ENV006 | Deployment 존재 |
| ENV007 | Resource Limit 초과 |
| ENV008 | Invalid Strategy |

---

# 19. Events / Audit

Events: created · updated · archived · restored · promoted · deleted  
Audit: ENVIRONMENT_CREATED · PROMOTION_STARTED · DOMAIN_UPDATED · RESOURCE_UPDATED · ENVIRONMENT_ARCHIVED

---

# 20. Sequences

### Create

```text
Facade → Service/Cluster Validation → Environment 생성
→ Namespace 등록 → Helm Values → Event → Audit
```

### Promote

```text
DEV → Promotion API → GitOps Commit → PR → Approval
→ Merge → ArgoCD Sync → Deployment
```

---

# 21. Acceptance Criteria

- [ ] DEV/STAGE/PRODUCTION · Cluster/NS 연결
- [ ] Resource Policy · Strategy · Domain
- [ ] GitOps Promote · Event · Audit
- [ ] Production Approval 확장 가능 설계

---

# 22. 구현 지침

- Environment = Service 종속 Aggregate Root
- Create 시 즉시 K8s NS 생성 X → Provisioning Job
- Promote = GitOps 변경 트리거
- Resource Policy ↔ Helm Values
- Domain = 메타 · Pipeline 반영
