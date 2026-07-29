# API-04-02 AI Recommendation

**Nimbus Platform · Version 2.0**  
**AI = Decision Engine / Platform Engineer Agent** (단순 Chat ❌)

---

# 1. Domain Overview

```text
User Request → AI Context Builder → Knowledge Base → Template Catalog
→ LLM → Decision Engine → Recommendation → Wizard
```

수행 기능:

- Tech/Runtime/DB/Cache/Infra 추천
- Helm Values · Terraform Variables 추천
- Cost · Security · Architecture Review
- YAML Generation · Deployment Recommendation

---

# 2. Architecture

```text
Wizard → Context Builder
  (Project Metadata · Catalog · Workspace Policy · Template
   · Previous Deployments · Knowledge Base)
       → AI Decision Engine
            ├── Recommendation
            └── Explanation
                 → Wizard UI
```

## Provider

**MVP:** Gemini · Groq · OpenRouter · Ollama(Local)  
**v2:** OpenAI · Claude · Azure OpenAI

## Category

Runtime · Framework · Database · Cache · Messaging · Monitoring · Deployment · Infrastructure · Security · Cost · Architecture · YAML

---

# 3. API List (14)

| API | Method |
|-----|--------|
| Runtime Recommendation | POST |
| Architecture Review | POST |
| Database Recommendation | POST |
| Cache Recommendation | POST |
| Helm Recommendation | POST |
| Terraform Recommendation | POST |
| Cost Estimation | POST |
| Security Review | POST |
| YAML Generation | POST |
| Explain Recommendation | POST |
| AI Health | GET |
| Prompt History | GET |
| Feedback | POST |
| Regenerate | POST |

---

# 4. Runtime Recommendation

```http
POST /api/v1/ai/runtime
```

```json
{
  "description": "쇼핑몰 백엔드 API",
  "expectedTraffic": "MEDIUM",
  "language": "JAVA"
}
```

```json
{
  "runtime": "SPRING_BOOT",
  "score": 98,
  "reason": "Java 기반 REST API에 적합"
}
```

---

# 5. Architecture Review (핵심)

```http
POST /api/v1/ai/architecture-review
```

```json
{ "projectId": "uuid" }
```

분석 대상: Repository + Metadata + Environment + Deployment History + Monitoring

```json
{
  "score": 91,
  "strengths": ["모듈 분리가 적절합니다."],
  "risks": ["Replica가 부족합니다."],
  "recommendations": ["Redis Cache 추가 권장"]
}
```

---

# 6. Database / Cache

```http
POST /api/v1/ai/database
POST /api/v1/ai/cache
```

DB 예: PostgreSQL · Redis · ElasticSearch  
Cache 예: `{ "cache": "REDIS", "reason": "조회량이 높음" }`

---

# 7. Helm / Terraform Recommendation

```http
POST /api/v1/ai/helm
POST /api/v1/ai/terraform
```

생성: `values.yaml` · `terraform.tfvars`  
예: replicaCount 3 · autoscaling · cpu/memory/nodeGroup/disk

---

# 8. Cost Estimation

```http
POST /api/v1/ai/cost
```

MVP: Resource Score · v2: Cloud Cost

```json
{ "estimatedCost": "LOW", "resourceScore": 83 }
```

---

# 9. Security Review

```http
POST /api/v1/ai/security
```

검사: Secret · Image · Ingress · Container

```json
{
  "score": 94,
  "issues": ["latest 태그 사용 금지"]
}
```

---

# 10. YAML Generation

```http
POST /api/v1/ai/yaml
```

Deployment · Service · Ingress · HPA

---

# 11. Explain / Feedback / Regenerate

```http
POST /api/v1/ai/explain
POST /api/v1/ai/feedback
POST /api/v1/ai/regenerate
```

Explain: recommendationId → reason  
Feedback: rating · comment (품질 개선)  
Regenerate: 동일 Context 새 추천

---

# 12. Health / Prompt History

```http
GET /api/v1/ai/health
GET /api/v1/ai/prompts
```

```json
{ "provider": "Gemini", "status": "UP", "latency": 1200 }
```

---

# 13. AI Context

Workspace · Project · Service · Template · Metadata · Repository · Environment · Monitoring

### Context Builder (필수)

```text
ContextBuilder
  ├── ProjectContext · RepositoryContext · ServiceContext
  ├── DeploymentContext · MonitoringContext · SecurityContext
```

LLM에는 ContextBuilder가 만든 **JSON만** 전달.

---

# 14. Prompt Template 예

```text
당신은 Platform Engineer입니다.
아래 프로젝트를 분석하여 Runtime, Database, Cache,
Infrastructure, Security를 추천하세요.
```

Prompt Versioning: v1 → v2 → v3 (하드코딩 금지)

---

# 15. Entity / DTO

```java
// Recommendation
UUID id; RecommendationType type; Integer score;
String explanation; String provider; LocalDateTime createdAt;

// PromptHistory
UUID id; String prompt; String provider; Integer tokenUsage;
```

DTO: RecommendationRequest/Response · ArchitectureReviewResponse · SecurityReviewResponse · CostResponse

---

# 16. Error Codes

| Code | 설명 |
|------|------|
| AI001 | Provider 연결 실패 |
| AI002 | Timeout |
| AI003 | Prompt 오류 |
| AI004 | 추천 생성 실패 |
| AI005 | Context 부족 |
| AI006 | Token Limit |
| AI007 | YAML 생성 실패 |

---

# 17. Events / Audit

Events: recommendation.generated · architecture.reviewed · yaml.generated · security.reviewed · cost.estimated  
Audit: AI_RUNTIME · AI_ARCHITECTURE · AI_SECURITY · AI_COST · AI_YAML

---

# 18. Confidence & Guardrail

모든 추천 **Confidence 0~100**

```text
LLM Response → JSON Schema → Validation → Business Rule → Response
```

- 미지원 Runtime 거부
- 없는 Helm Chart 재생성
- 허용되지 않은 K8s 리소스 차단

---

# 19. Provider Adapter

```text
AIProvider
  ├── GeminiAdapter · GroqAdapter · OllamaAdapter
  └── ClaudeAdapter(v2) · OpenAIAdapter(v2)
```

AI 교체 시 Business Logic 수정 금지 · Fallback 지원

---

# 20. 핵심 인터페이스

```java
public interface AIProvider {
    Recommendation generate(AIContext context);
    HealthStatus health();
}

public interface ContextBuilder {
    AIContext build(UUID projectId);
}
```

```text
ai/controller|facade|service|provider|adapter|context|prompt|validator|dto|entity|repository|event|exception
```

### Advisors

Runtime · Architecture · Security · Cost · YAML · Deployment

---

# 21. Acceptance Criteria

- [ ] Runtime/DB/Cache/Helm/TF/YAML 추천
- [ ] Architecture · Security · Cost
- [ ] Context · Confidence · Provider 교체 · Prompt Version
- [ ] Audit · Event
