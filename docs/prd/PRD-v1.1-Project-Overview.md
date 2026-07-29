# Nimbus Platform PRD v1.1

# AI Native Internal Developer Platform

**Version:** 1.1  
**Author:** Nasuyu Yu

---

> **이어서 보면 좋은 문서:** [PRD-MASTER](PRD-MASTER.md) · [Design Evolution Map](../architecture/03-Canonical-Decisions.md) · [Glossary](../architecture/04-Glossary.md)


> **이 프로젝트는 "Backstage 클론"을 만드는 것이 아닙니다.**  
> **"AI Native Platform Engineering Portal"** 을 만드는 것이 목표입니다.  
> Spotify Backstage + Port + Humanitec + GitHub + ArgoCD + AI Ops 를 하나의 플랫폼으로 통합합니다.

---

# 1. Project Overview

## 프로젝트명

Nimbus Platform

## 한 줄 소개

AI 기반 Internal Developer Platform(IDP)로, 개발자가 클릭 몇 번만으로 새로운 서비스를 생성하고 GitHub Repository 생성, CI/CD 구성, Kubernetes 배포, 모니터링 연결까지 자동으로 수행하는 Platform Engineering SaaS.

## 프로젝트 목적

현대 개발 조직에서 새 서비스 생성 시 반복되는 작업:

- GitHub Repository 생성
- Dockerfile 작성
- GitHub Actions 작성
- Helm Chart 작성
- Kubernetes Manifest 작성
- Namespace 생성
- Ingress 생성
- Domain 연결
- Monitoring 구성
- Alert 연결

숙련된 DevOps에게는 익숙하지만 일반 백엔드·신입에게는 높은 진입장벽이다.

Nimbus는 이 반복 작업을 자동화하여 개발자가 **비즈니스 로직 개발에 집중**할 수 있는 환경을 제공한다.

---

# 2. Vision

## Mission

> **Empower Developers, Automate Infrastructure.**

개발자가 인프라를 배우기 전에 서비스를 만들 수 있도록 한다.

## Vision

Nimbus는 **"개발자의 생산성을 극대화하는 AI Platform Engineer"** 가 되는 것을 목표로 한다.

장기적으로:

- Platform Engineering
- GitOps
- AI Ops
- FinOps
- Multi Cloud

까지 확장 가능한 플랫폼을 지향한다.

---

# 3. Background

## 현재 문제점

```text
서비스 생성 → Repository → Dockerfile → Workflow
→ Terraform → Helm → Ingress → Deploy
```

전부 사람이 직접 수행.

문제:

- 반복 업무
- YAML 작성 실수
- 환경마다 다른 설정
- Kubernetes 학습 비용
- 신규 개발자 온보딩 시간 증가

## 해결 방법

```text
Service Wizard → AI 분석 → Template 생성
→ Repository 생성 → CI/CD 생성
→ Terraform → Helm → ArgoCD → Deploy
```

모두 자동 수행.

---

# 4. Target Users

### Platform Engineer

- 목표: 조직 전체 생산성 향상
- Pain: 반복 업무
- Needs: 자동화

### DevOps Engineer

- 목표: 운영 자동화
- Pain: CI/CD 관리
- Needs: GitOps

### Backend Developer

- 목표: 서비스 개발
- Pain: Kubernetes 미숙
- Needs: 클릭 몇 번으로 배포

### Student

- 목표: Kubernetes 학습
- Pain: YAML 난이도
- Needs: AI 설명

---

# 5. User Persona

## Persona A — 김개발

- Backend Developer, 경력 2년, Spring Boot
- Kubernetes 경험 부족
- 원하는 것: "Docker는 알지만 Kubernetes는 모르겠다."
- Nimbus: Create Service → Deploy 끝.

## Persona B — 박DevOps

- DevOps Engineer (Terraform, Helm, GitHub Actions 관리)
- 원하는 것: 반복 작업 제거

## Persona C — 학생

- 컴퓨터공학과, Kubernetes 공부 중
- 원하는 것: AI가 YAML을 설명해줬으면 좋겠다.

---

# 6. Core Value Proposition

기존: `Infrastructure First`  
Nimbus: **`Developer First`**

기술보다 **개발 경험(DX)** 을 우선한다.

---

# 7. Product Goals

| Goal | 내용 |
|------|------|
| 1 | Repository 생성 시간 30분 → **30초** |
| 2 | YAML 작성량 **90% 감소** |
| 3 | 배포 성공률 향상 |
| 4 | 신규 개발자 온보딩 시간 감소 |
| 5 | AI 기반 장애 분석 |

---

# 8. Functional Requirements (Epics)

### Epic 1 — Developer Portal

- 로그인, Dashboard, Service Catalog, Team

### Epic 2 — Service Wizard

입력: Project Name, Language, Framework, DB, Cache, Replica, Namespace, Domain → Deploy

### Epic 3 — GitHub Integration

- Repository 생성, Branch Protection, README, Issue/PR Template, Secret 등록

### Epic 4 — CI/CD

- GitHub Actions, Docker Build, Push, Deploy 자동 생성

### Epic 5 — Infrastructure

- Terraform 자동 실행 (모듈 기반)

### Epic 6 — Kubernetes

- Namespace, Deployment, Service, Ingress, ConfigMap, Secret, HPA

### Epic 7 — Monitoring

- Prometheus, Grafana, Loki 연결

### Epic 8 — AI

- YAML Generator, Incident Analysis, Architecture Review, Prompt Assistant

---

# 9. Non Functional Requirements

| 항목 | 목표 |
|------|------|
| 응답속도 | 500ms 이하 |
| Repository 생성 | 30초 이하 |
| Deployment 시작 | 1분 이하 |
| CI 성공률 | 95% 이상 |
| API 가용성 | 99.9% |
| 보안 | OAuth2 + RBAC |
| 확장성 | Multi Cluster 지원 (로드맵) |

---

# 10. Information Architecture (IA)

```text
Nimbus Platform
├── Dashboard
├── Projects
│     ├── All Services
│     ├── Create Service
│     ├── Deployments
│     └── Templates
├── Infrastructure
│     ├── Kubernetes
│     ├── Terraform
│     ├── Helm
│     ├── ArgoCD
│     └── Namespaces
├── CI/CD
│     ├── Pipelines
│     ├── GitHub Actions
│     ├── Build History
│     └── Rollback
├── Monitoring
│     ├── Grafana / Prometheus / Loki
│     ├── Alerts
│     └── Incidents
├── AI Assistant
│     ├── YAML Generator / Explain
│     ├── Architecture Review
│     ├── Incident Analysis
│     └── Cost Optimization
├── Team
│     ├── Members / Roles / Audit Logs
└── Settings
      ├── GitHub / Kubernetes / Domains / Secrets / API Keys
```

---

# 11. User Flow

```text
Login → Dashboard → Create Service → 입력
→ AI Architecture Review → Repository 생성
→ GitHub Action 생성 → Terraform → Helm
→ Namespace 생성 → Deploy → Monitoring 연결 → 완료
```

---

# 12. System Architecture

> **이후 문서에서 더해진 것:** Service 계층, GitOps(ArgoCD) 강조, 레포 스택(Java 21 / Spring Boot 4 / Next 15 / PG 16 / Redis 7).  
> 상세 누적 맵: [Design Evolution Map](../architecture/03-Canonical-Decisions.md)

```text
                         Users
                           │
                    Next.js Web Portal
                           │
────────────────────────REST API──────────────────────
                           │
                    Spring Boot Backend
                           │
 ┌───────────────┬───────────────┬─────────────────┐
GitHub API   Kubernetes API   AI Engine      Auth Server
Terraform     Helm        Prompt Engine      OAuth2
ArgoCD        (GitOps)    YAML Builder       RBAC
                           │
                    PostgreSQL / Redis / Job Queue
                           │
                    Amazon EKS / NCP (MVP: k3d/kind)
                    Prometheus / Grafana / Loki
```

---

# 13. MVP 범위 (v1)

### 포함

- GitHub OAuth 로그인
- 프로젝트 생성
- GitHub Repository 자동 생성
- GitHub Actions 템플릿 생성
- Kubernetes Deployment 생성
- Helm Chart 생성
- ArgoCD 연동
- AI YAML Generator / Explain
- Dashboard
- Monitoring 링크
- Audit Log

### 제외 (향후)

- Multi-Cluster / Multi-Cloud
- Service Mesh (Istio)
- Canary / Blue-Green (고급 운영)
- FinOps Dashboard
- AI Auto Healing
- Terraform Drift Detection
- Policy as Code (OPA)

---

# 14. Next Steps (문서 관점)

v1.2부터 구현 수준의 Functional Spec, GitHub 시퀀스, CI/CD, Terraform/Helm, ArgoCD, AI 프롬프트, 실패 복구를 세분화한다.
