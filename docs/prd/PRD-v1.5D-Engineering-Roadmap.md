# Nimbus Platform PRD v1.5D

# Engineering Roadmap & Development Plan

**Version:** 1.0  
**성격:** Engineering Execution Plan (Sprint · Epic · Milestone · Release · KPI)  
**접근:** Thin Viable Platform (TVP) 부터 제품처럼 확장

---

# 1. 목표

개발자가 Kubernetes를 몰라도 서비스를 배포할 수 있는 **AI Native Internal Developer Platform** 구축

---

# 2. KPI

| 항목 | 목표 |
|------|------|
| 서비스 생성 시간 | ≤ 1분 |
| Repository 생성 | ≤ 30초 |
| 배포 성공률 | ≥ 95% |
| AI Architecture Review 응답 | ≤ 5초 |
| 신규 서비스 생성 | 6 Step 이하 |
| YAML 직접 작성 | 0줄 |
| GitHub Repository 자동 생성 | 100% |
| GitOps 자동 배포 | 100% |

---

# 3. 전체 일정

```text
Planning → Architecture → Backend → Infrastructure
→ Frontend → AI → Testing → Beta → Release v1
```

총 **16주**

| Phase | 기간 | 초점 |
|-------|------|------|
| Phase 0 | Week 1 | Planning & Architecture |
| Phase 1 | Week 2–4 | Foundation |
| Phase 2 | Week 5–8 | Platform Core |
| Phase 3 | Week 9–11 | Infrastructure |
| Phase 4 | Week 12–13 | AI Platform |
| Phase 5 | Week 14 | Observability |
| Phase 6 | Week 15 | Testing |
| Release | Week 16 | v1.0 |

---

# 4. Phase 0 — Planning (Week 1)

- Repository · Monorepo · PRD · ERD · API · Figma · PoC
- Deliverable: README · PRD · ERD · API · Architecture Diagram
- Exit: PRD 완료 · 스택 확정 · 프로젝트 생성 가능

---

# 5. Phase 1 — Foundation (Week 2–4)

**Backend:** Spring Boot · Security · JWT · OAuth2 · PostgreSQL · Redis  
**Frontend:** Next.js · Layout · Dashboard · Sidebar · Theme  
**Infra:** Docker · Compose · Local K8s (k3d/Kind) · GitHub Actions  

완료: 로그인 · Dashboard · Project CRUD

### Sprint 1 — Authentication

GitHub Login · JWT · Refresh Token · RBAC

### Sprint 2 — Workspace

Workspace · Team · Invite · Member

---

# 6. Phase 2 — Platform Core (Week 5–8)

Epic: Service Wizard · GitHub · Repository · CI/CD

- Sprint 3 Project
- Sprint 4 GitHub (Repo, Branch Protection, Secret, Webhook)
- Sprint 5 Pipeline (Actions, Build, Docker, Push)

Deliverable: Project 생성 · Repo 자동 · Pipeline 생성

---

# 7. Phase 3 — Infrastructure (Week 9–11)

- Sprint 6 Terraform (modules, VPC, IAM, EKS)
- Sprint 7 Helm (Template, Values, Render)
- Sprint 8 GitOps (ArgoCD, Sync, Rollback)

Deliverable: Deploy 성공

---

# 8. Phase 4 — AI (Week 12–13)

- Sprint 9 Architecture Review
- Sprint 10 Incident Analysis
- Sprint 11 YAML Generate / Explain / Optimize

---

# 9. Phase 5 — Observability (Week 14)

Prometheus · Grafana · Loki · Alertmanager  
CPU/Memory/Pod/Node Dashboard · Incident AI · Alert

---

# 10. Phase 6 — Testing (Week 15)

- Backend: Unit · Integration
- Frontend: Component · E2E
- Infra: Deployment · Rollback
- AI: Prompt · Latency · Cache  
Coverage 목표: **≥ 80%**

---

# 11. Release (Week 16)

Checklist: API Freeze · UI Freeze · Bug Fix · Docs · README · Demo Video  
Version: **v1.0**

---

# 12. Git Flow

```text
main · develop · feature/* · release/* · hotfix/*
```

PR: 최소 1 Review · CI 통과 · Squash Merge

Branch 예: `feature/auth` · `feature/project` · `feature/deploy` · `feature/ai` · `feature/monitoring`

---

# 13. GitHub Milestone

1. Foundation  
2. Platform  
3. Deploy  
4. AI  
5. Release  

---

# 14. CI/CD 전략

```text
PR → Build → Test → Docker → Artifact → Merge
→ Deploy → Health Check → Notify
```

---

# 15. 우선순위

| P | 기능 |
|---|------|
| P0 | Login · Project · Deploy · GitHub |
| P1 | AI · Monitoring · Incident |
| P2 | Cost · FinOps · Multi Cloud |

---

# 16. MVP vs 제외

**MVP:** Login · Dashboard · Project · GitHub · Pipeline · Deploy · K8s · Helm · AI Review  

**제외:** Multi Cloud · FinOps · AI Auto Healing · Cost Dashboard

---

# 17. 리스크 관리

| Risk | 대응 |
|------|------|
| GitHub Rate Limit | Queue + Retry |
| Kubernetes 오류 | Kind/k3d 우선 검증 |
| Terraform Apply 실패 | Plan 승인 후 Apply |
| LLM 지연 | Redis Cache + Timeout |
| ArgoCD Sync 실패 | 자동 Rollback |
| API 변경 | OpenAPI 계약 관리 |

---

# 18. Definition of Done

- 기능 구현 · Unit Test · API 문서 · OpenAPI
- Frontend 연결 · 예외 처리 · Audit Log
- AI Review 정상 (해당 시) · PR 승인 · CI 통과 · develop 병합

---

# 19. 품질 목표

| 항목 | 목표 |
|------|------|
| Backend Coverage | ≥ 80% |
| Frontend Lighthouse | ≥ 90 |
| API 평균 응답 | ≤ 300ms |
| Dashboard 초기 로드 | ≤ 2초 |
| AI 평균 응답 | ≤ 5초 |
| Pipeline 성공률 | ≥ 95% |
| 배포 실패 자동 감지 | 100% |

---

# 20. GitHub Project Board

Backlog → Ready → In Progress → Code Review → Testing → Done

---

# 21. 최종 산출물

- 문서: PRD v1.1–v1.5 · OpenAPI · ERD · ADR · Architecture Diagram
- 코드: Next.js · Spring Boot · Terraform · Helm · ArgoCD
- 운영: GitHub Actions · Prometheus · Grafana · Loki
- 포트폴리오: 데모 영상 · README · 기술 블로그 · 발표 자료

---

# 22. v2 로드맵

Multi-Cluster · Multi-Cloud · Template Marketplace · Self-Service DB · OPA · Vault/External Secrets · AI Auto Healing · FinOps · Cost Advisor · OTel+Tempo · Platform Plugin SDK · MCP Tool Integration

---

# 23. 권장 docs/ 실행 문서 구조

```text
docs/
  ADR/
  architecture/
  runbook/
  contributing/
```
