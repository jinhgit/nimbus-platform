# Nimbus Platform — 최종 시연 시나리오 (Demo)

**성격:** 포트폴리오·면접 시연 가이드  
**길이:** 약 10~15분  
**원칙:** 시연 가치가 높은 MVP 경로만 실제 동작. 과금 클라우드 실생성은 v2.

> **핵심 한 문장 (면접용)**  
> 개발자가 버튼 한 번만 누르면 GitHub 저장소 생성부터 Kubernetes 배포까지  
> 자동으로 이루어지는 **Platform Engineering Portal** 을 직접 설계·구현했습니다.

---

## 1. 왜 시연이 중요한가

이 프로젝트는 문서·아키텍처만으로도 가치가 있지만,  
**로그인 → Wizard → AI 추천 → Provision Progress → GitHub/K8s 확인** 을  
실제로 보여줬을 때 임팩트가 훨씬 커진다.

| 기존 포트폴리오 | Nimbus로 더 보여주는 것 |
|----------------|-------------------------|
| Terraform 3-Tier EKS 등 **인프라 구축 능력** | 그 위에 **플랫폼을 만드는 엔지니어** 역량 |
| “Kubernetes를 사용했습니다” | “개발자 Self-Service Platform을 설계·구현했습니다” |

---

## 2. 최종 시연 플로우 (10~15분)

### 2.1 로그인

```text
GitHub 로그인
    ↓
Workspace 진입
```

- MVP: GitHub OAuth (실연 권장) · 로컬 Dev Login 폴백 가능
- 로그인 직후 개인/팀 Workspace 컨텍스트 표시

### 2.2 Dashboard

**Platform Portal** 느낌이 바로 나도록:

| 위젯 | 예시 |
|------|------|
| Project 개수 | 1 |
| Service 개수 | 1 |
| Kubernetes Cluster 상태 | Healthy |
| 최근 배포 | payment-api · SUCCESS |
| 최근 AI 추천 | Architecture / Runtime |
| CPU / Memory | 클러스터·서비스 요약 |
| Deployment 상태 | Running / Healthy |

### 2.3 Project 생성

```text
Create Project
    ↓
Payment Platform
    ↓
생성 완료
```

- Project = 비즈니스 컨텍스트 (여러 Service를 담는 단위)

### 2.4 Service Wizard (메인)

```text
Create Service
    ↓
Service Name: payment-api
    ↓
Runtime: Spring Boot
    ↓
Template: REST API
    ↓
Environment: Production
    ↓
AI Recommendation 클릭
```

### 2.5 AI Recommendation

면접관 반응이 좋은 핵심 화면.

```text
AI Recommendation
──────────────────────
Runtime        Spring Boot        Confidence 97%
Database       PostgreSQL
Cache          Redis
Deployment     Replica 3 · HPA Enabled

Reason
  트래픽이 증가할 가능성이 높습니다.
```

- Chatbot이 아니라 **Platform Engineer Decision Engine** 톤
- Confidence · Reason · 적용 가능한 추천 값

### 2.6 Preview

AI/Blueprint 기준으로 생성될 산출물 전부 미리보기:

```text
Blueprint
    ↓
Repository Structure
    ↓
Helm
    ↓
Terraform Variables
    ↓
GitHub Actions
    ↓
YAML (Deployment 등)
```

### 2.7 Provision 시작 (실시간 Progress)

```text
Deploy 클릭
```

진행 표시 예:

```text
Repository 생성      ███████
GitHub Actions 생성  █████
Helm 생성            ███
Terraform 생성       ████
ArgoCD 생성          ██
Deploy               █
```

- 비동기 Job + Step Progress (WebSocket/SSE 또는 폴링)
- Saga 개념: 실패 시 보상 가능 설계

### 2.8 GitHub 확인

자동 생성된 저장소 `payment-api`:

```text
README
src/
helm/
terraform/
.github/
```

### 2.9 GitHub Actions

Actions 탭에서 파이프라인:

```text
Build → Test → Docker → Success
```

(MVP: free tier / 로컬 생성 후 실행 범위 내)

### 2.10 Kubernetes (Portal)

```text
Running · Replica · Pod · CPU · Memory
```

- 클러스터: **k3d / kind** (free-only)

### 2.11 Monitoring

```text
Grafana · Prometheus 연결
    ↓
CPU · Memory · Request Dashboard
```

### 2.12 Logs

```text
payment-api
INFO  Started ...
```

실시간 로그 스트림 (또는 최근 로그 뷰)

### 2.13 AI Architecture Review

```text
Analyze
    ↓
Architecture Score  92
추천
  · Redis 추가
  · HPA 활성화
  · latest 태그 금지
```

### 2.14 완료 — Dashboard 재확인

```text
Project      1
Service      1
Deployment   Healthy
GitHub       Connected
Cluster      Healthy
```

---

## 3. MVP에서 **실제로 동작**해야 하는 기능

| 기능 | MVP |
|------|:---:|
| 로그인 (GitHub / Dev Login) | ✅ |
| Dashboard | ✅ |
| Project 생성 | ✅ |
| Service 생성 (Wizard) | ✅ |
| AI 추천 | ✅ |
| GitHub Repository 생성 | ✅ |
| GitHub Actions 생성 | ✅ |
| Helm Chart 생성 | ✅ |
| Terraform 파일 생성 | ✅ |
| ArgoCD Manifest 생성 | ✅ |
| Kubernetes (k3d/kind) 배포 | ✅ |
| 진행률 표시 | ✅ |
| 로그 보기 | ✅ |
| AI Architecture Review | ✅ |
| Monitoring (로컬 Prom/Grafana 링크·요약) | ✅ |

---

## 4. MVP에서 **제외** (v2)

| 제외 항목 | 이유 |
|-----------|------|
| AWS EKS 실제 생성 | 과금 · free-only 위반 |
| Route53 자동 연결 | 과금 |
| ACM 인증서 발급 | 과금 |
| ALB 생성 | 과금 |
| RDS 생성 | 과금 |
| Multi Cluster / Multi Cloud | 범위 과다 |
| Vault 연동 (프로덕션) | v2 |
| Kafka 기반 대규모 Workflow | v2 (MVP: Async/Queue 수준) |

구현 구조(Adapter, Provider 인터페이스)는 남겨 두고 **기본 경로는 로컬·OSS·무료 티어**만 탄다.  
→ [05-Free-Only-Constraints](../architecture/05-Free-Only-Constraints.md)

---

## 5. 시연 인프라 추천 구성

| 레이어 | 추천 |
|--------|------|
| 코드 공개 | GitHub public repo |
| Frontend | Vercel (또는 로컬) |
| Backend | Render / 개인 서버 / 로컬 Spring Boot |
| K8s 시연 | **로컬 k3d 또는 kind** (면접장 노트북) |
| AI | **Ollama 로컬** (유료 API 불필요) |
| GitHub | Free OAuth App + Free Actions 한도 주의 |

이 조합이면 면접장에서:

```text
로그인 → 서비스 생성 → AI 추천 → GitHub 저장소 생성
→ Kubernetes 배포 진행 상황 확인
```

까지 자연스럽게 이어진다.

---

## 6. 시연 체크리스트 (DoD)

시연 직전 확인:

- [ ] GitHub OAuth (또는 Dev Login) 동작
- [ ] Create Project → Create Service Wizard 끝까지
- [ ] AI Recommendation UI (score/confidence/reason)
- [ ] Preview (Blueprint / Helm / TF / Actions / YAML)
- [ ] Provision Progress 실시간 갱신
- [ ] GitHub에 `payment-api` 구조 확인 가능
- [ ] k3d/kind에 Pod Running (또는 명확한 상태 표시)
- [ ] Dashboard 요약 숫자 갱신
- [ ] Architecture Review 한 번 실행
- [ ] 장애 시 Fallback 문구 준비 (AI timeout, GitHub rate limit 등)

---

## 7. 구현 우선순위 (이 시연을 기준으로)

시연 시나리오를 **역산**한 구현 순서:

1. Auth (GitHub) + Workspace + Project ✅ (Phase 1 진행 중/완료 영역)
2. Service 엔티티 + Catalog Template
3. Service Wizard (UI + 상태 저장 + Validate/Preview)
4. AI Recommendation (Ollama + Context + Confidence)
5. Provision Job (Saga Step + Progress API/WS)
6. GitHub Adapter (Repo · Actions · 구조 커밋)
7. Helm / TF / Argo Manifest 생성
8. k3d/kind 배포 + 상태 조회
9. Dashboard 위젯 · Logs · Monitoring 링크
10. AI Architecture Review

---

## 8. 관련 문서

| 문서 | 용도 |
|------|------|
| [PRD-MASTER](../prd/PRD-MASTER.md) | 제품 한 장 요약 |
| [Evolution Map](../architecture/03-Canonical-Decisions.md) | 설계 누적 |
| [Free-Only](../architecture/05-Free-Only-Constraints.md) | 과금 없는 실행 경로 |
| [API-04-01 Wizard](../api/API-04-01-Service-Wizard-Core.md) | Wizard 스펙 |
| [API-04-02 AI](../api/API-04-02-AI-Recommendation.md) | AI Decision Engine |
| [API-04-03 Provision](../api/API-04-03-Provisioning-Orchestration.md) | Saga / Progress |
| [PRD v1.5D Roadmap](../prd/PRD-v1.5D-Engineering-Roadmap.md) | 일정·KPI |

---

## 9. 메모

- 시연 스크립트는 **삭제하지 않고 유지**한다. 구현 범위의 나침반이다.
- UI 카피·더미 데이터보다 **Wizard → AI → Progress → GitHub** 한 줄 연결이 최우선이다.
- free-only 원칙과 충돌하는 시연 스텝(EKS 실생성 등)은 넣지 않는다.
