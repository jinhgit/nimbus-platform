# API-03-01 Project Core

**Nimbus Platform · Version 2.0**

---

# 1. 목적 & 설계 변경

Platform Engineering에서 핵심은 Project가 아니라 **Service** 이다.

```text
Workspace → Project → Service → Environment → Deployment
```

예:

```text
Project: Shopping Mall
  ├── Auth Service
  ├── Payment Service
  ├── Search Service
  └── Notification Service
```

**Project** = 여러 Service를 담는 Business Context Container  
**Project** = Deployment 대상이 아니라 Deployment를 관리하는 **Root Aggregate**

---

# 2. Domain Model

Aggregate Root: **Project**  
Child 연관: Service · Environment · Label · Variable · Secret

## Status

```text
CREATING → READY → ARCHIVED → (RESTORED → READY) → DELETING
FAILED
```

---

# 3. API List (10)

| API | Method |
|-----|--------|
| Create Project | POST |
| Update Project | PATCH |
| Delete Project | DELETE |
| Get Project | GET |
| List Projects | GET |
| Archive Project | POST |
| Restore Project | POST |
| Clone Project | POST |
| Favorite Project | POST |
| Unfavorite | DELETE |

---

# 4. Create Project

```http
POST /api/v1/projects
```

권한: `PROJECT_CREATE`

```json
{
  "name": "Shopping Mall",
  "description": "Platform Project",
  "workspaceId": "uuid",
  "visibility": "PRIVATE",
  "teamId": "uuid"
}
```

| 항목 | 조건 |
|------|------|
| name | 3~50 |
| workspaceId | 필수 |
| visibility | ENUM |
| teamId | 필수 |

Visibility: `PRIVATE` · `INTERNAL` · (v2 `PUBLIC`)

생성 시 자동:

```text
Project → Default Service Folder → Default Environment (DEV) → Audit → Event
```

> GitHub Repository / K8s 리소스는 **생성하지 않음**. Service Wizard(API-04)에서 수행.

```json
{
  "success": true,
  "data": { "projectId": "uuid", "status": "READY" }
}
```

Event: `project.created` · Audit: `PROJECT_CREATED`

---

# 5. Get Project

```http
GET /api/v1/projects/{id}
```

집계: Project + Services + Environment + Pipeline + Deploy Count

```json
{
  "id": "uuid",
  "name": "Shopping",
  "status": "READY",
  "workspace": "Platform",
  "team": "Backend",
  "serviceCount": 4,
  "deployCount": 83
}
```

권한: Workspace Member

---

# 6. List Projects

```http
GET /api/v1/projects
```

Filter: Workspace · Status · Owner · Favorite · Search (`?q=`)  
Sort: Created · Updated · Name  
Pagination: Page · Size

---

# 7. Update Project

```http
PATCH /api/v1/projects/{id}
```

수정 가능: 이름 · 설명 · Team · Visibility  
수정 불가: Workspace · Project UUID  
이름 중복 불가 · Audit: `PROJECT_UPDATED`

---

# 8. Delete Project

```http
DELETE /api/v1/projects/{id}
```

Hard Delete 금지 → Soft Delete (`deleted_at`)  
Service 존재 / Environment 존재 시 삭제 불가

---

# 9. Archive / Restore

```http
POST /api/v1/projects/{id}/archive
POST /api/v1/projects/{id}/restore
```

Archive 시: Pipeline 중지 · Deploy 금지 · Status `ARCHIVED`  
Restore → `READY`  
Events: `project.archived` · `project.restored`

---

# 10. Clone

```http
POST /api/v1/projects/{id}/clone
```

```json
{ "name": "Shopping-v2" }
```

복사: Project · Service · Environment · Variable  
**Secret 복사 금지**  
Event: `project.cloned`

---

# 11. Favorite

```http
POST   /api/v1/projects/{id}/favorite
DELETE /api/v1/projects/{id}/favorite
```

---

# 12. Entity / DTO

```java
// Project fields
UUID id
String name
String description
Visibility visibility
ProjectStatus status
Workspace workspace
Team team
LocalDateTime archivedAt

public record CreateProjectRequest(
    @NotBlank @Size(min = 3, max = 50) String name,
    UUID workspaceId,
    UUID teamId,
    String description
) {}

public record ProjectResponse(
    UUID id,
    String name,
    ProjectStatus status,
    long serviceCount,
    long deploymentCount
) {}
```

---

# 13. Error Codes

| Code | 설명 |
|------|------|
| PROJECT001 | 중복 이름 |
| PROJECT002 | Workspace 없음 |
| PROJECT003 | 권한 없음 |
| PROJECT004 | Archive 상태 |
| PROJECT005 | 삭제 불가 |
| PROJECT006 | Service 존재 |
| PROJECT007 | Environment 존재 |
| PROJECT008 | Team 없음 |

---

# 14. Events / Audit

Events: created · updated · archived · restored · deleted · cloned · favorited  
Audit: PROJECT_CREATED · UPDATED · DELETED · CLONED · ARCHIVED

---

# 15. Sequence (Create)

```text
Controller → Facade → Workspace Validation → Project 생성
→ Default Environment → Event → Audit → Response
```

---

# 16. Test Cases

- 생성: 정상 · 이름 중복 · 권한 없음
- 삭제: Service/Env 존재 · Soft Delete
- Archive: 정상 · 이미 Archive
- Clone: Secret 제외 · Variable 복사

---

# 17. Acceptance Criteria

- [ ] Project CRUD · Soft Delete
- [ ] Archive/Restore · Clone · Favorite
- [ ] Pagination · Search
- [ ] Event · Audit

---

# 18. 구현 지침

```text
project/controller|facade|service|repository|entity|dto|event|mapper|validator|exception
```

- Project = Aggregate Root
- 직접 Deployment 생성 금지 (Service Domain)
- Soft Delete · Clone 시 Secret 금지
- Event + Audit 동시
- DEV Environment 자동 · Repo/K8s는 Wizard
