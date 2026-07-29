# Nimbus Platform PRD v1.4

# AI Native Platform

**Version:** 1.4  
**핵심 차별화:** AI를 Chatbot이 아니라 **Platform Engineer** 처럼 동작시킨다.

---

> **정본 우선:** 이 문서와 최신 결정이 다르면 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 와 [`PRD-MASTER.md`](PRD-MASTER.md) 를 따른다.


# 1. 방향 수정

## 흔한 접근

```text
AI → YAML 생성
```

Cursor / Claude / Copilot 과 차별화 어려움.

## Nimbus 접근

AI 역할:

- Platform Engineer
- DevOps Engineer
- SRE
- Cloud Architect

면접 메시지:

> "LLM을 붙였습니다" ❌  
> **"Platform Engineering Workflow에 AI Agent를 녹였습니다"** ✅

---

# 2. 목적

Nimbus AI = **Platform Engineering Assistant**

이해하고 돕는 대상:

- Kubernetes · Terraform · Helm · GitHub · ArgoCD · Prometheus · Loki

---

# 3. AI Architecture

```text
User → AI Gateway → Prompt Builder → Context Engine → LLM
→ Response → Platform Action
```

---

# 4. AI Modules

```text
AI Layer
├── YAML Generator
├── YAML Explain
├── Architecture Review
├── Incident Analysis
├── Cost Advisor
├── Deployment Advisor
├── Golden Path
├── Prompt Optimizer
└── Knowledge Base
```

---

# 5. Agents

## Agent 1 — YAML Generator

입력 예: Spring Boot, Replica 3, Postgres, Redis, Ingress, domain  
출력: deployment/service/ingress/configmap/hpa.yaml  
Best Practice 적용 (예: Production + Replica 1 → 2 권장)

## Agent 2 — YAML Explain

deployment.yaml 선택 → 필드별 설명 (학습용)

## Agent 3 — Architecture Review (핵심)

분석 예:

- 문제: Replica 1 → HA 불가
- 추천: Replica 2+, HPA, Pod Anti Affinity

**Architecture Score** 예:

| 항목 | 점수 |
|------|------|
| Total | 84/100 |
| Reliability | 92 |
| Scalability | 78 |
| Security | 70 |
| Cost | 85 |

Review Categories: Security · Scalability · Availability · Reliability · Performance · Cost · Maintainability

추천 예: Memory 512Mi + Java Heap 700Mi 예상 → OOM 위험 → 1Gi 권장

## Agent 4 — Deployment Advisor

배포 전 검토:

- Readiness Probe 없음 → **Fail / Block**
- Replica 1 → Warning

## Agent 5 — Incident Analysis

```text
Prometheus Alert → Loki Log → AI → Root Cause
```

Workflow:

```text
Alert → Log 수집 → Events → Pod Describe → Metrics
→ Prompt → LLM → Root Cause → Report
```

Incident Report 예: OOMKilled · 영향 3 Pods · Memory/Replica 증가 추천

## Agent 6 — Cost Advisor (v2)

Terraform + Cloud Cost → 예상 비용 · RI/Spot/HPA 추천

## Agent 7 — Golden Path Advisor (핵심)

서비스 종류: Backend API · Frontend · Worker · Cron · Gateway · Batch  
→ 권장 스택 자동 구성 (예: Spring · 2 Replica · ALB · Redis · Postgres · HPA)

---

# 6. Knowledge Base

포함: Kubernetes · Terraform · Helm · Spring · AWS · EKS · ArgoCD · Prometheus  
추후: RAG

---

# 7. Prompt Builder & Context Engine

LLM에 직접 보내지 않음. Prompt Builder가 생성.

Context 자동 수집:

- Deployment · Pod · Events · Logs · Metrics · Cluster Version

Prompt 예:

```text
You are Senior Platform Engineer
Analyze CrashLoopBackOff
Environment Production
Memory 512Mi
Events ... Logs ... Metrics ...
```

## Response Format (JSON)

```json
{
  "score": 95,
  "severity": "High",
  "reason": "OOM",
  "recommendation": []
}
```

→ Portal Rendering

---

# 8. AI Chat & Explain Mode

질문: "왜 Deployment가 실패했나요?"  
→ Readiness Probe 실패 등 설명

Explain Mode: "Ingress가 뭐예요?" → Gateway 개념 설명

## Confidence

모든 답변 Confidence 표시  
80% 이하: "가능성이 낮습니다. 추가 로그가 필요합니다."

---

# 9. AI Cache / Security / Token

| 항목 | 정책 |
|------|------|
| Cache | Redis, 동일 질문 시 LLM 스킵 |
| Security | Prompt 내 PASSWORD/TOKEN 마스킹 |
| Token | 긴 로그 요약, Metrics Top-N |

---

# 10. Failure Handling

```text
LLM 실패 → Fallback Rule Engine → Template → Error
```

---

# 11. AI Audit & Dashboard

기록: Prompt · Response · Latency · Token · Cost · User

Dashboard 예:

- 오늘 AI 호출 182
- 평균 응답 2.4초
- 절감 시간 13시간
- Incident 분석 28건

---

# 12. MVP (v1) vs v2

### v1

- AI YAML Generator / Explain
- Architecture Review
- Incident Analysis
- AI Chat
- Prompt Builder · Context Engine · AI Cache · AI Audit

### v2

- Cost Advisor · Auto Healing · Deployment Planner · Capacity Planning
- Kubernetes Tutor · FinOps
- Multi-Agent Collaboration
- MCP 기반 외부 도구 연동

---

# 13. Multi-Agent 확장 (권장)

| Agent | 역할 |
|-------|------|
| Platform Architect Agent | 권장 아키텍처 설계 |
| YAML Engineer Agent | Manifest 생성/수정 |
| SRE Agent | 장애 분석, MTTR |
| Security Agent | SecurityContext, RBAC, Secret 검토 |
| Cost Agent | 과다 할당·비용 최적화 |
| Reviewer Agent | 배포 전 종합 품질 점수 |
