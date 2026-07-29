# Canonical Decisions

초기 PRD를 쓰면서 방향이 여러 번 바뀌었다.  
이 문서는 **지금 기준으로 확정된 것**만 모은다. 다른 문서와 겹치면 여기를 우선한다.

최종 정리: 2026-07-29

---

## 1. 제품 정체성

| 항목 | 결정 |
|------|------|
| 이름 | Nimbus Platform |
| 한 줄 | AI Native Internal Developer Platform |
| 아님 | Backstage 단순 클론 |
| 가까움 | Backstage(Catalog/Portal) + Port(Blueprint) + Humanitec(오케스트레이션) + GitOps + AI Ops |
| 철학 | Developer First · Zero YAML · GitOps First · AI as Platform Engineer |

Mission: **Empower Developers, Automate Infrastructure.**

---

## 2. 도메인 계층 (최종)

```text
Workspace
  └── Project                 # 비즈니스 컨텍스트 (예: Shopping Mall)
        └── Service           # 실제 배포 단위 (예: payment-api)
              └── Environment # DEV | STAGE | PRODUCTION
                    └── Deployment
```

- **Project** = 여러 Service를 담는 컨테이너. 직접 Deploy 대상이 아님.
- **Service** = GitHub repo, Helm, ArgoCD Application 이 붙는 단위.
- **Environment** = 문자열 태그가 아니라 Infrastructure Context  
  (cluster, namespace, domain, helm values, gitops branch, strategy).

MVP에서는 Organization 계층을 두지 않는다.

```text
Workspace → Team → Member → Project → Service → ...
```

---

## 3. 배포 경로 (GitOps)

```text
Terraform (infra 코드/파일)
    → GitOps Repository (manifest / values commit)
    → ArgoCD Sync
    → Kubernetes
```

- Terraform이 `kubectl apply` 를 직접 하지 않는다.
- Promotion(DEV→STAGE→PROD)도 Git PR/merge → ArgoCD 흐름.

---

## 4. Service Wizard

Wizard는 CRUD UI가 아니라 **Workflow Engine + Job Orchestrator**.

### UI Step (최종 7단계)

1. Service Info  
2. Template (Catalog)  
3. Infrastructure  
4. AI Review  
5. Preview (Blueprint)  
6. Provision (비동기 Job)  
7. Complete  

초안 v1.2의 “5단계”, 프론트 v1.5C의 “6단계”는 **이 7단계로 통일**.

### 실행 규칙

- `execute` 는 API 스레드에서 repo/deploy 를 돌리지 않는다.
- Queue → Worker → Provision Steps (Saga).
- 실패 시 Step 역순 rollback (Repo 자체는 기본적으로 삭제하지 않음).

---

## 5. AI

| 아님 | 임 |
|------|----|
| 단순 채팅봇 / YAML 생성기 | Platform Engineer Decision Engine |

```text
Context Builder → AIProvider → Guardrail(JSON Schema) → Recommendation
```

- 모든 추천에 reason + confidence(0~100).
- Secret은 prompt에 넣기 전 마스킹.
- Provider 교체 가능 (MVP: Gemini/Groq/OpenRouter/Ollama 등 어댑터).
- Catalog 추천은 “새 템플릿 생성”이 아니라 **기존 Catalog에서 고름**.

---

## 6. Configuration

```text
Variable  → ConfigMap 경로
Secret    → 암호화 저장 → K8s Secret / GitHub Secret
```

- Secret 평문 DB 저장·API 응답 금지.
- Clone Project 시 Secret 복사 금지.
- Crypto는 `SecretProvider` 인터페이스 (Local AES → KMS/Vault 확장).

---

## 7. SCM

```text
GitProvider (interface)
  └── GitHubAdapter (MVP)
  └── GitLab / Bitbucket (나중)
```

비즈니스 로직은 GitHub REST를 직접 호출하지 않는다.  
권장: GitHub App (OAuth App만으로도 MVP 가능).

---

## 8. 기술 스택 (코드 기준)

| Layer | 확정 |
|-------|------|
| Web | Next.js 15, TypeScript, Tailwind (shadcn 도입 예정) |
| API | **Java 21, Spring Boot 4.0.x** |
| DB | PostgreSQL 16 |
| Cache/Session | Redis 7 |
| Local K8s (MVP) | k3d / kind |
| Cloud K8s (목표) | Amazon EKS |

초기 PRD의 “Spring Boot 3.5” 표기는 **4.0.x 로 갱신**한다.  
(start.spring.io 호환 버전에 맞춤)

---

## 9. MVP vs 이후

### MVP에 넣는다

- GitHub 로그인 (또는 App 연결)
- Workspace / Project / Service 뼈대
- Catalog 선택 + Wizard + Provision Job
- Repo / Actions / Helm·TF **파일** / Argo **manifest** 생성
- k3d·kind 배포 또는 배포 요청까지
- AI Architecture Review · YAML explain (범위 내)
- Audit · Dashboard skeleton
- Prometheus/Grafana/Loki **연동 링크 또는 기본 스택** (깊이 있는 FinOps는 제외)

### MVP에서 뺀다 (v2+)

- Multi-cloud / Multi-cluster 운영
- Terraform apply 로 실제 AWS 풀 프로비저닝
- FinOps 대시보드, AI Auto Healing
- Service Mesh, 고급 Canary 운영 고도화
- OPA / Vault 본격 연동
- Template Marketplace, MCP multi-agent 협업

---

## 10. API / 응답 규칙 (백엔드)

- `ApiResponse<T>` 래퍼 (`success`, `data`, `error`)
- Entity 직접 반환 금지 · record DTO
- Controller → Facade → Service → Repository
- 상태 변경 시 Domain Event + Audit
- Soft delete (`deleted_at`), Optimistic lock (`version`)
- PK: UUID

---

## 11. 문서 우선순위

1. 이 파일 (Canonical)  
2. [PRD-MASTER](../prd/PRD-MASTER.md)  
3. 개별 API Spec  
4. 구버전 PRD 본문 (맥락·히스토리)

구버전 PRD를 수정할 때마다 여기와 어긋나면 Canonical을 먼저 고친다.
