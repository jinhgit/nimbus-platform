# Nimbus Platform PRD v1.5C

# Frontend Design Specification (FSD)

**Version:** 1.0  
**원칙:** DX First — 예쁜 화면이 아니라 개발자가 생각 없이 사용할 수 있는 UX

---

# 1. 목표

Frontend는 Terraform · Kubernetes · GitHub · Helm · ArgoCD 복잡성을 **숨긴다**.

Nimbus = AI Native Internal Developer Platform UI  
= **Developer Control Plane** (Service 추상만 노출)

---

# 2. Frontend Tech Stack

| 분야 | 기술 |
|------|------|
| Framework | Next.js 15 (App Router) |
| Language | TypeScript |
| UI | shadcn/ui |
| Style | TailwindCSS |
| Icons | Lucide |
| State | Zustand |
| Server State | TanStack Query |
| Form | React Hook Form |
| Validation | Zod |
| Table | TanStack Table |
| Chart | Recharts |
| Theme | next-themes |

---

# 3. Design Principle

| 원칙 | 설명 |
|------|------|
| Developer First | K8s를 몰라도 됨 |
| Zero YAML | YAML 직접 작성 금지 |
| AI First | 모든 화면에 AI 버튼 |
| One Click Deploy | Deploy 한 번 클릭 |
| Dark Mode First | 기본 Dark Theme |

AI 버튼 예: Review by AI · Generate YAML · Explain · Fix · Optimize

---

# 4. Color System

| Token | Hex |
|-------|-----|
| Primary | `#2563EB` |
| Success | `#16A34A` |
| Warning | `#F59E0B` |
| Danger | `#DC2626` |
| Background | `#09090B` |

## Typography

Inter · Pretendard

## Responsive

Desktop 95% · Tablet 5% · Mobile 조회 전용

---

# 5. Layout

```text
Sidebar | Top Navbar | Content | Status Bar
```

### Sidebar

Dashboard · Projects · Deployments · Infrastructure · Pipelines · Monitoring · Incidents · AI Assistant · Settings

### Top Navbar

Workspace · Search · Notifications · Theme · Profile

---

# 6. Routing

```text
/
/login
/dashboard
/projects
/projects/new
/projects/{id}
/deployments
/pipelines
/monitoring
/incidents
/ai
/settings
```

---

# 7. Screens

## Dashboard

Widget: Running Services · Deployments · Pipeline · Incident · Cluster Health · AI Summary  
AI Summary 예: 오늘 Deployment 12건, 실패 1건, 원인 Health Check

## Project List

Table: Project · Status · Framework · Repository · Cluster · Updated  
Action: Open · Deploy · Delete · AI Review

## Create Project Wizard (6 Step)

```text
Project → Framework → Database → Infrastructure → AI Review → Deploy
```

Progress 1–6 · 우측 상시 AI Panel (Recommendation · Architecture · Cost)

## Project Detail Tabs

Overview · Deployment · Repository · Monitoring · AI · Settings

## Deployment

Timeline: Build → Docker → Push → Helm → Deploy → Healthy  
History: Revision · Commit · Author · Duration · Status · Rollback

## Pipeline

Table: Run · Branch · Commit · Author · Status · Duration → Log

## Monitoring

Metrics: CPU · Memory · Pod · Node · Restart  
Charts: Realtime · 5m · 30m · 1h · 24h

## Incident

Severity: Critical · High · Medium · Low  
Table: Time · Service · Type · AI · Status → Detail

## AI Assistant

Chat + Cards: Architecture Review · Generate YAML · Explain · Cost · Fix

## YAML Viewer

Split: YAML | AI Explain · resources 등 하이라이트 설명

## Infrastructure

Cards: Cluster · Namespace · Node · Terraform · Helm · ArgoCD → Node Detail

## Notification

Deploy · Pipeline · Incident · GitHub · AI (Realtime)

## Search

Global: Project · Deployment · Pod · Namespace · Repository  
Command Palette: **⌘ + K**

---

# 8. Component Structure

```text
components/
  layout/ dashboard/ wizard/ deployment/ pipeline/
  monitoring/ incident/ ai/ common/ ui/
```

## Design System

- Button: Primary · Secondary · Danger · Ghost
- Badge: Running · Failed · Healthy · Pending
- Card: Metric · Project · Incident
- Modal: Delete · Deploy · Rollback

---

# 9. State Management

| Store | 데이터 |
|-------|--------|
| Zustand | User · Workspace · Theme · Notification |
| TanStack Query | Project · Deployment · Pipeline · Incident |

## Loading / Empty / Error UX

- Skeleton: Dashboard · Table · Card · Wizard
- Deploy Progress Animation
- Empty: No Project → Create Service
- Error: Pipeline Failed → Retry · AI Analyze

## Accessibility

Keyboard Navigation · Screen Reader · High Contrast · Focus Ring

## Theme

Default Dark · Light · System

---

# 10. MVP 화면 우선순위

| 화면 | 우선순위 |
|------|----------|
| Login · Dashboard · Project List · Create Wizard · Project Detail · Deployment | P0 |
| Pipeline · Infrastructure · Monitoring · Incident · AI Assistant | P1 |
| Settings | P2 |

## Platform Components 우선

Deployment Timeline · Pipeline Status · Cluster Health Card · Service Catalog Card · AI Recommendation Card · Infrastructure Tree · YAML Diff Viewer · Incident Timeline · Deployment Progress Stepper

---

# 11. Design References

| 프로젝트 | 참고 |
|----------|------|
| Backstage | Catalog, Template UX, Plugin |
| Vercel Dashboard | 프로젝트 중심 UX |
| GitHub | Repo / Action UI |
| Grafana | Monitoring |
| Linear | 빠른 UX, Command Palette |
| shadcn Dashboard | Layout, Card, Table, Dialog |

---

# 12. Frontend Acceptance Criteria

### Dashboard

- 서비스 상태 2초 이내 표시
- Widget 독립 로딩
- WebSocket/SSE 실시간 갱신

### Project Wizard

- 6단계 이내 완료
- 입력 자동 저장 · 이전 단계 복원
- AI 추천 실시간 표시

### Deployment

- 진행률 시각화 · 로그 스트리밍
- 실패 시 AI 분석 버튼

### Monitoring

- CPU/Memory/Pod 실시간 · Grafana 링크 · Incident 연계

### AI

- 주요 화면에서 호출 가능 · 스트리밍 · 대화 컨텍스트 유지
