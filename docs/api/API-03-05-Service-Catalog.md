# API-03-05 Service Catalog

**Nimbus Platform · Version 2.0**  
**Catalog = Backstage Software Catalog + Software Template + Humanitec Score + Port Blueprint**

Self-Service Platform의 핵심 자산 관리 시스템. 단순 템플릿 폴더가 아님.

---

> **이어서 보면 좋은 문서:** [PRD-MASTER](../prd/PRD-MASTER.md) · [Design Evolution Map](../architecture/03-Canonical-Decisions.md) · 연관 PRD (`docs/prd/`)


# 1. Domain Overview

관리 대상:

- Golden Path Template
- Framework / Infrastructure Template
- AI Recommendation
- Blueprint · Version · Marketplace

```text
Developer → Catalog Template → AI 설정 생성 → Service Wizard → 배포 가능 서비스
```

---

# 2. Domain Model

```text
Catalog → Template
  ├── Version · Blueprint · Runtime · Framework
  ├── Helm Chart · Terraform Module
  └── AI Recommendation
```

## Template Type

```text
BACKEND · FRONTEND · FULLSTACK · WORKER · CRONJOB
BATCH · DATABASE · CACHE · MESSAGE_QUEUE · AI_SERVICE
```

## Runtime

Spring Boot · Next.js · NestJS · FastAPI · Go · Node.js · Python

## Status

`DRAFT` → `PUBLISHED` (Wizard에서 선택 가능 = **Publish된 것만**)

---

# 3. API List (14)

| API | Method |
|-----|--------|
| List Catalog | GET |
| Get Template | GET |
| Create/Update/Delete Template | POST/PATCH/DELETE |
| Publish Template | POST |
| Clone Template | POST |
| Template Versions | GET |
| Restore Version | POST |
| Validate Template | POST |
| Recommend Template | POST |
| Search Catalog | GET |
| Import/Export Template | POST/GET |

---

# 4. List / Get / Search

```http
GET /api/v1/catalog
GET /api/v1/catalog/{templateId}
GET /api/v1/catalog/search
```

Filter: Runtime · Framework · Type · Language · AI · Official · Favorite  
Search: `?q=spring`

```json
{
  "content": [{
    "id": "uuid",
    "name": "Spring Boot REST API",
    "type": "BACKEND",
    "runtime": "SPRING_BOOT",
    "latestVersion": "1.4.2",
    "official": true
  }]
}
```

---

# 5. Create Template

```http
POST /api/v1/catalog
```

```json
{
  "name": "Spring API",
  "runtime": "SPRING_BOOT",
  "type": "BACKEND",
  "description": "..."
}
```

자동: Template → Blueprint → Version 1.0 → Audit  
Event: `template.created`

---

# 6. Update / Delete / Publish / Clone

- Update: Description · Blueprint · Helm · Terraform · README · **Version 자동 증가**
- Delete: Soft Delete · 사용 중이면 불가
- Publish: README 필수 · DRAFT → PUBLISHED
- Clone: Blueprint · Helm · TF · README 복사

---

# 7. Version / Restore

```http
GET  /api/v1/catalog/{id}/versions
POST /api/v1/catalog/{id}/versions/{version}/restore
```

Restore: 현재 Version History 저장 후 Rollback

---

# 8. Validate

```http
POST /api/v1/catalog/{id}/validate
```

검사: Helm · Terraform · YAML · README · Metadata

```json
{ "valid": true, "score": 98 }
```

---

# 9. AI Recommendation

```http
POST /api/v1/catalog/recommend
```

```json
{ "description": "쇼핑몰 백엔드 API" }
```

**기존 Catalog에서 최적 Template 추천** (신규 생성 X)

```json
{
  "runtime": "SPRING_BOOT",
  "database": "POSTGRES",
  "cache": "REDIS",
  "template": "spring-api"
}
```

---

# 10. Import / Export

Format: zip · yaml · json  
Import 시 Blueprint 자동 생성

---

# 11. Blueprint

Service 전체 설계 (JSON 내부 모델 권장):

```yaml
runtime: spring-boot
database: postgres
cache: redis
deployment: rolling
monitoring: prometheus
```

포함 개념: Runtime · Repository · Pipeline · Helm · Terraform · Deployment · Monitoring

필요 시 Helm Values · Terraform Variables · GitHub Actions 생성

---

# 12. Helm / Terraform 연결

Helm: Chart.yaml · values.yaml · deployment/service/ingress  
Terraform Module: VPC · IAM · EKS · ALB · RDS

---

# 13. Entity / DTO

```java
// Template
UUID id; String name; TemplateType type; Runtime runtime;
TemplateStatus status; String blueprint; Version version;
```

DTO: CreateTemplateRequest · UpdateTemplateRequest · TemplateResponse · TemplateSummary · RecommendationResponse

---

# 14. Error Codes

| Code | 설명 |
|------|------|
| TEMPLATE001 | Template 없음 |
| TEMPLATE002 | 이름 중복 |
| TEMPLATE003 | Publish 실패 |
| TEMPLATE004 | Validation 실패 |
| TEMPLATE005 | Version 없음 |
| TEMPLATE006 | Blueprint 오류 |

---

# 15. Events / Audit

Events: template.created/updated/published/deleted/restored/validated  
Audit: TEMPLATE_CREATED/UPDATED/PUBLISHED · VERSION_RESTORED · TEMPLATE_IMPORTED

---

# 16. Wizard 연결 흐름

```text
Developer → Service Wizard → Service Catalog → Blueprint
→ GitHub Repo → GitHub Actions → Terraform → Helm → ArgoCD → Kubernetes
```

---

# 17. Acceptance Criteria

- [ ] Catalog CRUD · Version · Publish
- [ ] Validation · Blueprint · Helm/TF 포함
- [ ] AI 추천 · Audit · Event

---

# 18. 구현 지침

```text
catalog/controller|facade|service|repository|entity|dto|mapper|blueprint|validator|ai|event|exception
```

- Template = 재사용 가능한 Platform Asset
- Create 시 Blueprint + Version 동시
- Blueprint = JSON 내부 모델
- Publish된 Template만 Wizard 선택
- Validation 점수 산출
- AI는 Catalog 내 추천만
