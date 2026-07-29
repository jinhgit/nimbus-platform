# API-02 Workspace Domain

**Nimbus Platform · Version 2.0**

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

Workspace = 최상위 협업 공간.  
Project · Team · Member 의 Owner.

책임: Project · Team · Member · Role · Invite · Audit

## 구조 (MVP)

```text
Workspace → Team → Member → Project
```

(Organization 은 MVP 생략 · 장기: Organization → Workspace → …)

---

# 2. Domain Model

```text
Workspace
├── Team → Member
├── Project
├── Invitation
└── Audit
```

---

# 3. Role Model

```text
OWNER · ADMIN · PLATFORM_ENGINEER · DEVELOPER · VIEWER
```

| 기능 | Owner | Admin | Platform | Developer | Viewer |
|------|:-----:|:-----:|:--------:|:---------:|:------:|
| Workspace 수정 | ✅ | ❌ | ❌ | ❌ | ❌ |
| Member 초대 | ✅ | ✅ | ✅ | ❌ | ❌ |
| Member 삭제 | ✅ | ✅ | ❌ | ❌ | ❌ |
| Role 변경 | ✅ | ✅ | ❌ | ❌ | ❌ |
| Project 생성 | ✅ | ✅ | ✅ | ✅ | ❌ |
| Deploy | ✅ | ✅ | ✅ | ✅ | ❌ |
| Monitoring | ✅ | ✅ | ✅ | ✅ | ✅ |

---

# 4. API List (16)

| API | Method |
|-----|--------|
| Create Workspace | POST |
| Get Workspace | GET |
| Update Workspace | PATCH |
| Delete Workspace | DELETE |
| List Workspaces | GET |
| Switch Workspace | PATCH |
| Invite Member | POST |
| Accept Invitation | POST |
| List Members | GET |
| Update Member Role | PATCH |
| Remove Member | DELETE |
| Leave Workspace | POST |
| List Teams | GET |
| Create Team | POST |
| Update Team | PATCH |
| Delete Team | DELETE |

---

# 5. Create Workspace

```http
POST /api/v1/workspaces
```

권한: Authenticated User

```json
{
  "name": "Nimbus Team",
  "slug": "nimbus-team",
  "description": "Platform Engineering Team"
}
```

| 필드 | 규칙 |
|------|------|
| name | 3~50자 |
| slug | 영문, 숫자, `-` |
| description | 최대 300자 |

Business:

- slug 중복 불가
- 생성자 OWNER
- Default Team 생성
- Audit

Transaction:

```text
Workspace → Default Team → Owner 등록 → Audit
```

```json
{ "success": true, "data": { "workspaceId": "uuid" } }
```

Event: `workspace.created` · Audit: `CREATE_WORKSPACE`

---

# 6. Get Workspace

```http
GET /api/v1/workspaces/{workspaceId}
```

권한: Workspace Member

```json
{
  "id": "uuid",
  "name": "Nimbus",
  "slug": "nimbus",
  "owner": "Nasuyu",
  "memberCount": 12,
  "projectCount": 31
}
```

---

# 7. List Workspaces

```http
GET /api/v1/workspaces
```

정렬: 최근 접속순

---

# 8. Update Workspace

```http
PATCH /api/v1/workspaces/{id}
```

권한: **OWNER**  
수정: 이름 · 설명 · Logo · Color · Visibility  
Audit: `UPDATE_WORKSPACE`

---

# 9. Delete Workspace

```http
DELETE /api/v1/workspaces/{id}
```

Soft Delete · Project 존재 시 삭제 불가 · 204

---

# 10. Switch Workspace

```http
PATCH /api/v1/workspaces/current
```

```json
{ "workspaceId": "uuid" }
```

```text
Workspace 확인 → JWT 재발급 → Redis Session 변경 → Audit
```

---

# 11. Invite Member

```http
POST /api/v1/workspaces/{id}/members/invite
```

```json
{
  "githubUsername": "octocat",
  "role": "DEVELOPER"
}
```

- GitHub 계정 존재 확인
- 이미 멤버면 실패
- Invite Token 생성 · **만료 7일**

```json
{ "inviteToken": "xxxxx" }
```

Event: `member.invited` · Audit: `INVITE_MEMBER`

---

# 12. Accept Invitation

```http
POST /api/v1/invitations/{token}/accept
```

```text
Token 확인 → 만료 확인 → Member 생성 → Audit
```

```json
{ "workspace": "Platform Team" }
```

---

# 13. List Members

```http
GET /api/v1/workspaces/{id}/members
```

필터: Role · Status · Search

---

# 14. Update Member Role

```http
PATCH /api/v1/workspaces/{id}/members/{memberId}
```

```json
{ "role": "PLATFORM_ENGINEER" }
```

Owner 역할 변경 불가 · Audit: `ROLE_CHANGED`

---

# 15. Remove Member

```http
DELETE /api/v1/workspaces/{id}/members/{memberId}
```

Owner 삭제 불가 · 마지막 Owner 삭제 불가  
Event: `member.removed`

---

# 16. Leave Workspace

```http
POST /api/v1/workspaces/{id}/leave
```

Owner Leave 불가 → Ownership 이전 필수

---

# 17–19. Team

```http
POST   /api/v1/workspaces/{id}/teams
PATCH  /api/v1/teams/{teamId}
DELETE /api/v1/teams/{teamId}
```

Team 삭제: Member 존재 시 불가

---

# 20. DTO

```java
public record CreateWorkspaceRequest(
    @NotBlank @Size(min = 3, max = 50) String name,
    @Pattern(regexp = "^[a-z0-9-]+$") String slug,
    @Size(max = 300) String description
) {}

public record InviteMemberRequest(
    @NotBlank String githubUsername,
    Role role
) {}
```

---

# 21. Error Codes

| Code | 의미 |
|------|------|
| WORKSPACE001 | Workspace 없음 |
| WORKSPACE002 | Slug 중복 |
| WORKSPACE003 | Permission 없음 |
| WORKSPACE004 | 마지막 Owner |
| WORKSPACE005 | Project 존재 |
| MEMBER001 | 이미 멤버 |
| MEMBER002 | 초대 만료 |
| MEMBER003 | GitHub 사용자 없음 |
| TEAM001 | Team 없음 |

---

# 22. Events

```text
workspace.created / updated / deleted
member.invited / joined / removed
team.created / deleted
```

---

# 23. Audit

```text
WORKSPACE_CREATE · WORKSPACE_UPDATE
INVITE_MEMBER · MEMBER_JOIN · ROLE_CHANGED · TEAM_CREATE
```

---

# 24. Package

```text
workspace/controller|facade|service|dto|entity|repository|mapper|event|validator
```

---

# 25. Acceptance Criteria

- [ ] Workspace 생성 · Default Team · Owner 자동
- [ ] Member 초대 (GitHub Username) · Token 만료
- [ ] Team CRUD
- [ ] Workspace 전환
- [ ] Audit · Event

---

# 26. 구현 지침

- Workspace = Aggregate Root
- Team/Member는 Workspace 종속 · 독립 생성 금지
- Owner 최소 1명
- 권한 검사 Facade 진입 시
- Invite: Redis(7일) 또는 DB 만료 · 사용 후 폐기
- `ApiResponse<T>` + record DTO
