# API-03-04 Project Metadata

**Nimbus Platform · Version 2.0**  
**Metadata = Software Catalog 핵심 데이터** (배포 영향 없음 · Portal 전용)

---

> **이어서 보면 좋은 문서:** [PRD-MASTER](../prd/PRD-MASTER.md) · [Design Evolution Map](../architecture/03-Canonical-Decisions.md) · 연관 PRD (`docs/prd/`)


# 1. Domain Overview

실제 서비스 메타 예: Owner · Team · Domain · Language · Runtime · Repository · Namespace · SLA · Criticality · Tags

```text
Project → Metadata
  ├── Label · Tag · Annotation
  ├── Owner · Team · Favorite
  ├── Dashboard · AI Metadata · Custom Property
```

Category: `SYSTEM` · `BUSINESS` · `CUSTOM` · `AI`

사용처: Search · Filter · AI 추천 · Dashboard · Software Catalog

---

# 2. API List (14)

| API | Method |
|-----|--------|
| Add/Remove/List Label | POST/DELETE/GET |
| Add/Remove/List Tag | POST/DELETE/GET |
| Change Owner | PATCH |
| Change Team | PATCH |
| Favorite / Unfavorite | POST/DELETE |
| Update Annotation | PATCH |
| Dashboard Settings | PATCH |
| Search Metadata | GET |
| AI Recommend Metadata | POST |

---

# 3. Label

```http
POST   /api/v1/projects/{projectId}/labels
DELETE /api/v1/projects/{projectId}/labels/{labelId}
GET    /api/v1/projects/{projectId}/labels
```

```json
{ "name": "backend" }
```

- name 1~30자 · 중복 불가
- Workspace 내 Label 재사용
- Project 최대 30개 Label
- 제거 = 연결 해제 (Soft Delete 미사용)

Event: `label.created` · Audit: `LABEL_CREATED`

---

# 4. Tag

Label보다 자유로운 문자열 · Project당 최대 50개

```http
POST   /api/v1/projects/{id}/tags
DELETE /api/v1/projects/{id}/tags/{tag}
```

```json
{ "tag": "msa" }
```

---

# 5. Owner / Team

```http
PATCH /api/v1/projects/{id}/owner
PATCH /api/v1/projects/{id}/team
```

```json
{ "ownerId": "uuid" }
{ "teamId": "uuid" }
```

- Owner: Workspace Member만
- Team: 동일 Workspace 필수
- Events: `owner.changed` · `team.changed`

---

# 6. Favorite

```http
POST   /api/v1/projects/{id}/favorite
DELETE /api/v1/projects/{id}/favorite
```

```json
{ "favorite": true }
```

---

# 7. Annotation

Platform 내부 연동 키 예:

- `backstage.io/kubernetes-id`
- `argocd.application`
- `grafana.dashboard`
- `prometheus.job`

```http
PATCH /api/v1/projects/{id}/annotations
```

```json
{
  "annotations": {
    "argocd.application": "payment",
    "grafana.dashboard": "dashboard-12"
  }
}
```

Key: `^[a-zA-Z0-9./_-]+$`  
예약 Prefix 시스템 관리: `argocd.` · `grafana.` · `backstage.io/`

---

# 8. Dashboard Settings

```http
PATCH /api/v1/projects/{id}/dashboard
```

```json
{
  "favoriteMetrics": ["cpu", "memory", "deployment"]
}
```

**사용자별(User Preference)** 저장

---

# 9. Metadata Search

```http
GET /api/v1/projects/search?label=kubernetes&owner=me
```

Filter: Owner · Label · Runtime · Status · Favorite · Team · Tag

```json
[{ "project": "Shopping", "score": 98 }]
```

---

# 10. AI Metadata Recommend

```http
POST /api/v1/projects/{id}/metadata/recommend
```

```text
Repository → README → Framework → LLM → labels/tags 추천
```

```json
{
  "labels": ["spring", "backend"],
  "tags": ["msa", "payment"]
}
```

사용자 수정 가능 · System Metadata와 분리 저장

---

# 11. Metadata Schema

| 종류 | 예시 |
|------|------|
| System | Framework · Language · Runtime · Repository · Cluster · Namespace |
| Business | Owner · Team · Business Domain · Description |
| AI | Risk · Complexity · Score · Recommendation |

---

# 12. Entity / DTO

```java
// ProjectMetadata
UUID id; Project project; User owner; Team team;
Boolean favorite; Map<String,String> annotations;

// Label: UUID id; String name
// Tag: UUID id; String tag
```

DTO: AddLabelRequest · AddTagRequest · OwnerRequest · AnnotationRequest · MetadataResponse

---

# 13. Error Codes

| Code | 설명 |
|------|------|
| META001 | Label 중복 |
| META002 | Tag 중복 |
| META003 | Owner 없음 |
| META004 | Team 없음 |
| META005 | Annotation 오류 |
| META006 | Favorite 중복 |

---

# 14. Events / Audit

Events: label.created/deleted · tag.created · owner.changed · team.changed · favorite.changed · annotation.updated  
(+ 검색 인덱스용 `metadata.updated` 확장 가능)

Audit: LABEL_CREATED · TAG_CREATED · OWNER_CHANGED · TEAM_CHANGED · FAVORITE_CHANGED · ANNOTATION_UPDATED

---

# 15. Acceptance Criteria

- [ ] Label/Tag/Owner/Team/Favorite/Annotation
- [ ] Dashboard 설정 · Metadata 검색 · AI 추천
- [ ] Audit · Event

---

# 16. 구현 지침

```text
metadata/controller|facade|service|repository|entity|dto|mapper|validator|ai|event|exception
```

- Label = Workspace 공용 · Tag = Project 자유값
- Annotation Map + 예약 Prefix
- Dashboard = User Preference
- AI 메타 ≠ System 메타
- 검색 인덱스 갱신 이벤트 대비
