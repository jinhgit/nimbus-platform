# Nimbus Platform — Master PRD

**Version:** Master (v1.x 통합)  
**Status:** Living document — Canonical Decisions 와 함께 유지  
**Author:** Nasuyu Yu

이 문서는 v1.1~v1.5 및 API 설계에서 합의된 내용을 **한 장으로 읽히게** 정리한 것이다.  
세부 endpoint·DTO·AC 는 `docs/api/` 를 본다.

---

## 1. 무엇을 만드는가

**Nimbus** 는 AI Native Internal Developer Platform 이다.

개발자가 클릭 몇 번으로:

1. Catalog 에서 템플릿을 고르고  
2. AI 리뷰를 받고  
3. GitHub 저장소·CI·Helm/TF 파일·Argo 매니페스트가 생기고  
4. 클러스터에 배포 흐름이 시작되는  

셀프서비스 포털을 목표로 한다.

인프라 콘솔이 아니라 **Developer Workspace**.

---

## 2. 왜 만드는가

새 서비스마다 반복되는 일:

Repo · Dockerfile · Actions · Helm · Manifest · Namespace · Ingress · Monitoring …

숙련 DevOps 에게는 익숙하지만, 앱 개발자·신입에게는 비용이 크다.  
Nimbus 는 그 경로를 **Golden Path + 자동화 + AI 보조** 로 줄인다.

### 성공 지표 (목표)

| 지표 | 목표 |
|------|------|
| 서비스 생성 | ≤ 1분 (Wizard 완료~Job 접수 기준은 구현 시 명확화) |
| Repository 생성 | ≤ 30초 |
| 배포 성공률 | ≥ 95% |
| AI Review | ≤ 5초 |
| 사용자 YAML 작성 | 0줄 (플랫폼이 생성) |

---

## 3. 누구를 위한가

| Persona | Needs |
|---------|--------|
| Backend Dev | K8s 몰라도 배포 |
| DevOps / PE | 반복 작업 제거, 표준화 |
| Platform Engineer | 조직 생산성, Catalog/Policy |
| Student / 학습 | YAML 설명, 안전한 실험 |

Role (Workspace): Owner · Admin · Platform Engineer · Developer · Viewer

---

## 4. 핵심 도메인

```text
Workspace
  └── Project
        └── Service
              └── Environment (DEV / STAGE / PRODUCTION)
                    ├── Variable / Secret
                    └── Deployment
```

옆에 붙는 것:

- **Catalog / Template / Blueprint** — 생성의 입력
- **Wizard / Provision Job** — 생성의 오케스트레이션
- **AI** — 추천·리뷰·설명·장애 분석
- **GitHub (SCM)** — 소스·Actions·Secret 동기화
- **ArgoCD / Helm / Terraform** — GitOps·패키징·인프라 코드
- **Audit / Incident / Pipeline** — 운영 가시성

자세한 Aggregate·이벤트: [01-Domain-Map](../architecture/01-Domain-Map.md)

---

## 5. 사용자 여정 (Happy Path)

```text
Login (GitHub)
  → Workspace 선택
  → Project 안 Create Service
  → Wizard 7 step
       Info → Template → Infra → AI Review → Preview → Provision → Done
  → Provision Job 진행률 확인
  → Repo / Pipeline / Deploy 상태 확인
  → Monitoring / Incident (필요 시 AI 분석)
```

---

## 6. 기능 맵 (Epics)

| Epic | 요약 | 명세 |
|------|------|------|
| Portal & Auth | OAuth, JWT, RBAC, Dashboard | API-01, PRD 1.2/1.5C |
| Workspace | Team, Invite, Role | API-02 |
| Project / Service | CRUD, Archive, Clone, Favorite | API-03-01 |
| Environment | Cluster/NS/Domain, Promote | API-03-02 |
| Config | Variable, Secret, Rotation, Sync | API-03-03 |
| Metadata | Label, Tag, Annotation, Search | API-03-04 |
| Catalog | Template, Blueprint, Validate, AI recommend | API-03-05 |
| Wizard | Workflow session, preview, execute | API-04-01 |
| AI | Decision engine, review, yaml, cost(v2) | API-04-02, PRD 1.4 |
| Provision | Saga, retry, rollback, WS progress | API-04-03 |
| GitHub | Repo, Actions, webhook, secrets | API-05-01 |
| Infra/GitOps | TF modules, Helm, ArgoCD | PRD 1.3 |
| Observability | Prom/Grafana/Loki, Incident | PRD 1.3 |

---

## 7. 아키텍처 한 장

```text
Next.js Portal
      │
Spring Boot Platform API
      │
  Auth · Workspace · Project · Catalog · Wizard · AI · Provision · GitHub
      │
  PostgreSQL · Redis · Job Queue
      │
  GitHub · Terraform files · Helm · ArgoCD · Kubernetes
      │
  Prometheus / Grafana / Loki
```

결정 요약: [03-Canonical-Decisions](../architecture/03-Canonical-Decisions.md)

---

## 8. NFR (요약)

| 항목 | 목표 |
|------|------|
| API 응답 | 평균 ≤ 300~500ms (도메인에 따라) |
| Dashboard 초기 로드 | ≤ 2s |
| API 가용성 | 99.9% (운영 단계) |
| 보안 | OAuth2 + JWT + RBAC · Secret 암호화 |
| 테스트 | Backend coverage ≥ 80% (목표) |

---

## 9. MVP 범위

### In

- GitHub 기반 로그인/연결  
- Workspace · Project · Service 기본  
- Catalog + Wizard + 비동기 Provision  
- Repo/Actions/Helm·TF 파일/Argo manifest  
- 로컬 또는 단일 클러스터 배포 경로  
- AI Review / Explain (범위 내)  
- Audit · Health · Dashboard skeleton  

### Out (v2+)

- Multi-cloud 운영, 본격 FinOps  
- AI Auto Healing, multi-agent MCP  
- OPA / Vault 풀 연동  
- Template Marketplace  

로컬 무료 범위와 클라우드 실운영 범위 분리: API-04-03 참고.

---

## 10. 16주 로드맵 (요약)

| Phase | 주 | 초점 |
|-------|----|------|
| 0 | 1 | 설계·monorepo·PoC |
| 1 | 2–4 | Auth, Dashboard, Project CRUD |
| 2 | 5–8 | Wizard, GitHub, Pipeline |
| 3 | 9–11 | Helm, GitOps, Deploy |
| 4 | 12–13 | AI |
| 5 | 14 | Observability |
| 6 | 15 | Test |
| Release | 16 | v1.0 |

상세: [PRD-v1.5D](PRD-v1.5D-Engineering-Roadmap.md)

---

## 11. 데이터

- PostgreSQL: 플랫폼 **메타데이터** (라이브 K8s 오브젝트 SoT 아님)  
- Redis: session, cache, AI cache, job lock  
- Git: 앱 배포 SoT (GitOps)  

테이블 초안: [PRD-v1.5A](PRD-v1.5A-Database-Design.md)  
※ Service 테이블·`cluster_id`·`gitops_manifest` 등은 Canonical/Domain Map 기준으로 확장한다.

---

## 12. 프론트

- Dark-first, Desktop 우선  
- Zero YAML · 화면마다 AI 진입점  
- Wizard 우측 AI 패널  
- ⌘K 검색 (목표)  

[PRD-v1.5C](PRD-v1.5C-Frontend-Design.md) — Wizard 단계 수는 **7** 로 통일.

---

## 13. 관련 문서 맵

| 깊이 | 경로 |
|------|------|
| 정본 결정 | `docs/architecture/03-Canonical-Decisions.md` |
| 용어 | `docs/architecture/04-Glossary.md` |
| 개별 PRD | `docs/prd/PRD-v1.*` |
| 구현 Spec | `docs/api/API-*` |
| 코드 배치 | `docs/architecture/02-Monorepo-Layout.md` |

---

## 14. 변경 이력 (문서)

| 날짜 | 내용 |
|------|------|
| 2026-07-29 | Master 통합본 작성. 도메인 Service 계층, Wizard 7 step, Spring Boot 4, GitOps/Saga/AI Decision 정본 반영 |
