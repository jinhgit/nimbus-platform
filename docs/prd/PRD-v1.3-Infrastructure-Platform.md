# Nimbus Platform PRD v1.3

# Infrastructure Platform

**Version:** 1.3  
**철학:** Platform Engineering + GitOps + Infrastructure as Code

---

# 1. 목적

Nimbus는 단순 Kubernetes 배포 플랫폼이 아니다.

모든 Infrastructure는:

1. 코드로 관리되고
2. Git으로 관리되고
3. ArgoCD가 배포한다.

## 설계 변경 (중요)

### 기존

```text
Terraform → Helm → Deploy
```

### 실무 (채택)

```text
Terraform → GitOps Repository → ArgoCD → Sync → Kubernetes
```

**Terraform이 직접 kubectl apply 하는 구조는 지양.**

---

# 2. Architecture Overview

```text
Developer → Nimbus Portal → Spring Boot API
→ Terraform Engine → GitOps Repository
→ ArgoCD → Amazon EKS → Application
```

---

# 3. Infrastructure Components

## Cluster

| 버전 | Provider |
|------|----------|
| v1 | Amazon EKS |
| v2 | NCP Kubernetes, AKS, GKE |

## Infrastructure Layer (Terraform)

```text
VPC → Subnet → Security Group → IAM → EKS → ECR → ALB
```

## Platform Layer (Helm)

```text
Deployment · Service · Ingress · ConfigMap · Secret · HPA
```

## GitOps (ArgoCD)

```text
Sync → Health Check → Rollback
```

## Monitoring

Prometheus · Grafana · Loki

---

# 4. Terraform Module Design

```text
terraform/modules/
  vpc/ · eks/ · ecr/ · iam/ · alb/ · route53/ · monitoring/
```

### VPC Module

생성: VPC, CIDR, Public/Private Subnet, NAT, Route Table, IGW  
입력: Region, CIDR, AZ Count, Environment  
출력: VPC ID, Subnet IDs, Route IDs, Security Group

### IAM Module

EKS Role · Node Role · GitHub OIDC · Service Account · IRSA

### EKS Module

Cluster · Managed Node Group · Addon · OIDC · Cluster Autoscaler  
옵션: Node Count, Instance Type, Disk Size, AMI, Version

### ALB Module

Target Group · Listener · ALB · Security Group

### Route53 Module (v2)

A Record · CNAME · Wildcard

## Terraform Flow

```text
Create Project → Generate tfvars → terraform init → plan
→ AI Review → Approval → apply → Output 저장 → DB 저장
```

## Terraform State

- Backend: **S3 + DynamoDB Lock**
- Versioning · Encryption · Lock · Rollback

## Infrastructure Inventory (Portal)

조회: VPC, Subnet, EKS, Node, ALB, IAM, ECR  
상태: Running · Provisioning · Error · Deleting

---

# 5. Helm Platform

## Template 종류

Spring Boot · Next.js · NestJS · FastAPI · Go · CronJob · Worker

```text
helm/spring/ · next/ · node/ · python/
```

## Helm Values

image · replica · cpu · memory · ingress · domain · config · secret

## Helm Rendering

```text
Template + User Input → values.yaml → helm template → Manifest
```

## Kubernetes Resources (자동)

Namespace · Deployment · Service · Ingress · Secret · ConfigMap · HPA · ServiceAccount

## Health Probe (자동)

Liveness · Readiness · Startup  
AI 추천 예: Spring Boot → Readiness 권장

## HPA

입력: Min/Max Replica, CPU, Memory → HorizontalPodAutoscaler

## Secret Management

| 버전 | 방식 |
|------|------|
| v1 | Kubernetes Secret |
| v2 | AWS Secrets Manager · Vault · External Secret |

## ConfigMap

Portal에서 수정: application.yaml · 환경변수 · Feature Flag

## Namespace Strategy

- `dev` / `stage` / `prod`
- 또는 `team-payment` / `team-auth` / `team-search`

---

# 6. GitOps (ArgoCD)

## Repository 구조 예

```text
infra-live/
  apps/
    payment/
      values.yaml
```

## Sync

```text
Git Commit → Sync → Deploy
```

Sync Policy: Manual · Automatic

Auto Sync: Commit → Sync → Deploy → Health Check

## Rollback

Deploy 실패 → Previous Revision 자동 복구

## Drift Detection

Cluster ≠ Git → OutOfSync 표시

## Deployment History (Portal)

Revision · Author · Commit · Time · Status

---

# 7. Monitoring & Observability

## Stack

Prometheus · Grafana · Loki · Alertmanager  
v2: Tempo · Jaeger · OpenTelemetry

## Metrics

CPU · Memory · Disk · Network · Pod · Node · Namespace · Deployment

## Service Dashboard

CPU · RAM · Restart · Latency · Error Rate

## Log (Loki)

Pod · Namespace · Time · Keyword · ERROR/WARN/Exception

## Alert Rules (기본)

- CPU 90% · Memory 90%
- Restart 5회 · CrashLoopBackOff · OOMKilled

## Incident

Alert → Portal Critical/Warning/Info  
AI 분석 (v1.4): CrashLoop 원인 분석

## Resource Explorer

```text
Cluster → Namespace → Deployment → Pod
```

클릭: Log · YAML · Describe · Events · Restart Count

## Cluster / Node Overview

Node Ready, CPU, RAM, Version, Pods  
Node Detail: OS, Kernel, Container Runtime, Storage, Network

## Cost Dashboard (v2)

Namespace / Deployment / Node 별 비용

---

# 8. Backup & DR

## Backup

- Terraform State: S3 Versioning + Daily Snapshot
- Application: PostgreSQL Snapshot

## Disaster Recovery

```text
Cluster Failure → Terraform Recreate → ArgoCD Sync → Recovery
```

---

# 9. Security

IRSA · RBAC · Security Group · OIDC · IAM · Least Privilege

## Audit (Infra)

Terraform Apply · Helm Upgrade · ArgoCD Sync · Rollback · Delete

---

# 10. MVP 완료 기준

- [ ] Terraform Module 기반 인프라 Provisioning
- [ ] S3 + DynamoDB Remote State
- [ ] EKS 클러스터 및 Node Group 자동 생성
- [ ] Helm Chart 기반 애플리케이션 배포
- [ ] GitOps Repository 생성 및 ArgoCD 연동
- [ ] ArgoCD Auto Sync 및 Rollback
- [ ] Namespace/Deployment/Service/Ingress 자동 생성
- [ ] Prometheus + Grafana + Loki 통합
- [ ] Portal에서 리소스 및 배포 이력 조회

> 로컬 MVP 대안: k3d/kind + Manifest/파일 생성 중심 (API-04-03 참고)

---

# 11. Platform API 계층 권장

```text
Portal (Next.js)
        │
Spring Boot Platform API
        │
Catalog · Template · Provisioning · Deployment · Monitoring · AI Service
        │
Terraform / GitHub / ArgoCD / Kubernetes
```

도메인별 서비스 계층으로 모듈화.
