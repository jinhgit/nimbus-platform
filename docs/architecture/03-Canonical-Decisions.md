# Design Evolution Map

설계를 쌓아가며 보강된 레이어를 한곳에 모아 둔 문서다.  
**기존 PRD/API를 대체하지 않는다.** 옆에 덧붙인 확장 메모에 가깝다.

누적 일자: 2026-07-29  
**(+)** Free-only 제약: [05-Free-Only-Constraints.md](05-Free-Only-Constraints.md)

함께 보면 좋은 원본:

- Vision·Epic → PRD v1.1  
- 기능 흐름 → PRD v1.2  
- Infra/GitOps 상세 → PRD v1.3  
- AI 에이전트 → PRD v1.4  
- DB → PRD v1.5A  
- Frontend → PRD v1.5C  
- 일정 → PRD v1.5D  
- Endpoint 단위 → `docs/api/*`

---

## 1. 제품 정체성 (유지 + 보강)

v1.1에서 잡은 방향 그대로:

| 항목 | 내용 |
|------|------|
| 이름 | Nimbus Platform |
| 한 줄 | AI Native Internal Developer Platform |
| 포지션 | Backstage + Port + Humanitec + GitHub + ArgoCD + AI Ops 를 묶는 포털 |
| 철학 | Developer First · Zero YAML · GitOps First · AI as Platform Engineer |

Mission: **Empower Developers, Automate Infrastructure.**

---

## 2. 도메인 계층 보강

v1.1~1.2의 Project 중심 설명에, 실무 Platform 관점의 **Service** 레이어를 더한다.

```text
Workspace
  └── Project                 # 비즈니스 컨텍스트 (Shopping Mall)
        └── Service           # 배포 단위 (payment-api)  ← 보강
              └── Environment # DEV | STAGE | PRODUCTION
                    └── Deployment
```

- Project 문서(API-03-01)의 비즈니스 컨텍스트 역할은 그대로.
- Service 단위로 Repo / Helm / Argo Application 을 붙이면 MSA 확장에 유리하다.
- Environment 는 문자열 태그뿐 아니라 cluster·namespace·domain·helm·gitops 맥락까지 포함 (API-03-02).

MVP: Organization 계층은 넣지 않고 Workspace 부터.

```text
Workspace → Team → Member → Project → Service → …
```

---

## 3. 배포 경로 보강 (GitOps)

v1.3에서 잡은 흐름을 한 줄로 다시 적으면:

```text
Terraform (infra 코드/파일)
    → GitOps Repository
    → ArgoCD Sync
    → Kubernetes
```

직접 `kubectl apply` 를 메인 경로로 쓰지 않고, Git을 배포 소스로 둔다.  
Promotion(DEV→STAGE→PROD)도 PR/merge → ArgoCD 쪽과 맞물린다.

### Free-only 실행 환경

- 클러스터: **k3d / kind** (EKS 등 과금 클러스터 없음)
- Terraform/Helm/Argo: **파일 생성 + 로컬 적용** (AWS apply 기본 off)
- 상세: [05-Free-Only-Constraints.md](05-Free-Only-Constraints.md)

---

## 4. Service Wizard 보강

v1.2의 Wizard 스케치 + 프론트 단계 UX + API-04 오케스트레이션을 **합쳐** 보면:

### UI 흐름 (누적 7단계)

1. Service Info  
2. Template (Catalog)  
3. Infrastructure  
4. AI Review  
5. Preview (Blueprint)  
6. Provision (비동기 Job)  
7. Complete  

v1.2의 Project→Framework→DB→Infra→Review 스케치와 같은 맥락이고,  
Template / Preview / 비동기 Provision 이 더해진 형태다.

### 실행

- `execute` 는 긴 작업을 API 스레드에서 끝내지 않는다.
- Queue → Worker → Provision Steps (Saga) — API-04-03.
- 실패 시 Step 역순 보상 (Repo 자체 삭제는 기본적으로 하지 않음).

---

## 5. AI 보강

v1.4 + API-04-02 를 한 줄로:

```text
Context Builder → AIProvider → Guardrail → Recommendation → Wizard/Portal
```

- 채팅만이 아니라 Platform Engineer 역할 (리뷰, 추천, 장애 분석).
- reason + confidence.
- Secret 마스킹, Provider 교체, Catalog 안에서의 추천.
- **Free-only:** 기본 Provider = **Ollama (로컬)**. 유료 LLM API 키 필수 경로 없음.

---

## 6. Configuration 보강

v1.5A / API-03-03:

```text
Variable  → ConfigMap 경로
Secret    → 암호화 저장 → K8s Secret / GitHub Secret
```

- Secret 평문 저장·응답 금지.
- Project Clone 시 Secret 미복사.
- `SecretProvider` 로 Local AES → KMS/Vault 확장 여지.

---

## 7. SCM 보강

API-05-01:

```text
GitProvider
  └── GitHubAdapter (MVP)
  └── GitLab / Bitbucket (이후 추가 여지)
```

비즈니스 로직은 GitHub REST 직접 호출 대신 Adapter.  
GitHub App 권장, OAuth App 으로도 MVP 가능.

---

## 8. 기술 스택 (코드 + 문서)

| Layer | 내용 |
|-------|------|
| Web | Next.js 15, TypeScript, Tailwind (shadcn 도입 예정) |
| API | Java 21, Spring Boot **4.0.x** (초기 문서 3.5 표기 → 코드 생성 시 4.x로 맞춤) |
| DB | PostgreSQL 16 (Docker) |
| Cache | Redis 7 (Docker) |
| K8s | **k3d / kind only** (과금 클러스터 없음) |
| AI | **Ollama 로컬** (유료 LLM API 기본 경로 없음) |
| Git | GitHub Free + Actions free 범위 주의 |
| Observability | 로컬 Prom / Grafana / Loki |

문서의 초기 버전 숫자와 코드 버전이 조금 달라도, **의도는 동일**하고 구현 레포 버전이 실제 기준이 된다.  
**완전 무료:** 과금 클라우드·유료 API·결제 모델 없음 → [05-Free-Only-Constraints](05-Free-Only-Constraints.md)

---

## 9. MVP와 이후 (범위 보강)

### MVP에 쌓아 갈 것

- GitHub 로그인/연결  
- Workspace / Project / Service  
- Catalog + Wizard + Provision Job  
- Repo / Actions / Helm·TF 파일 / Argo manifest  
- 로컬·단일 클러스터 경로  
- AI Review · Explain  
- Audit · Dashboard  
- Observability 기본 연동  

### 이후에 더 얹을 것 (여전히 free 전제)

- Multi-cluster **로컬/라벨 수준** 실험  
- AI Auto Healing 규칙 엔진 (로컬)  
- OPA / 로컬 Vault OSS (선택)  
- Marketplace·MCP 는 free tool 범위만  

유료 클라우드 풀 프로비저닝·FinOps 청구 연동·유료 LLM 은 이 프로젝트 기본 범위에 넣지 않는다.  

---

## 10. 백엔드 공통 규칙 (API 문서 공통 +)

- `ApiResponse<T>`
- Entity 미노출 · DTO(record)
- Controller → Facade → Service → Repository
- Event + Audit
- Soft delete · UUID · version lock

각 API-0x 문서의 세부 endpoint·테스트·AC 는 그대로 유효하다.

---

## 11. 이 문서의 위치

```text
[개별 PRD v1.1~1.5]  +  [API Spec 01~05]  +  [이 Evolution Map]  +  [PRD-MASTER 요약]
```

읽는 순서 강제가 아니다.  
깊게 파고 싶을 때 원본 PRD/API를 보고, 한눈에 누적 레이어만 보고 싶을 때 여기를 보면 된다.
