# API-01 Authentication Service

**Nimbus Platform · Version 2.0**  
**목표:** AI가 Spring Boot 코드를 생성할 수 있는 Engineering Spec

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

Authentication Domain 책임:

- GitHub OAuth 로그인
- JWT 발급
- Refresh Token Rotation
- Workspace 선택
- RBAC
- Session 관리
- Audit Logging

스택: **Spring Security + OAuth2 + JWT + Redis Session**

---

# 2. Architecture

```text
Client (Next.js)
      │
      ▼
GitHub OAuth
      │
      ▼
Spring Security OAuth2
      │
      ▼
Authentication Facade
      │
 ┌────┴──────────┐
UserService   TokenService
 │               │
Redis        PostgreSQL
 │
AuditService
```

---

# 3. API List

| API | Method | Auth |
|-----|--------|------|
| GitHub Login | GET | ❌ |
| OAuth Callback | GET | ❌ |
| Refresh Token | POST | Cookie |
| Logout | POST | ✅ |
| Current User | GET | ✅ |
| Switch Workspace | PATCH | ✅ |
| Validate Token | POST | ✅ |
| Revoke Session | DELETE | ✅ |
| User Permissions | GET | ✅ |

**총 9개 API**

---

# 4. OAuth Login

```http
GET /api/v1/auth/github
```

- OAuth State 생성 → Redis 저장 → Redirect GitHub
- Redis: `oauth:state:{uuid}` TTL **5분**
- Response: `302` → `https://github.com/login/oauth/authorize`

---

# 5. OAuth Callback

```http
GET /api/v1/auth/github/callback?code=xxxx&state=xxxx
```

### Validation

- state 존재 · 만료 확인 · code 재사용 금지

### Process

```text
GitHub Token → User API → DB 조회 → 신규면 회원가입
→ JWT 생성 → Refresh 생성 → Audit
```

### Success

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "name": "Nasuyu",
      "role": "DEVELOPER",
      "workspaceId": "workspace_uuid"
    }
  }
}
```

### Failure

```json
{
  "success": false,
  "error": {
    "code": "AUTH001",
    "message": "GitHub OAuth Failed"
  }
}
```

---

# 6. Refresh Token

```http
POST /api/v1/auth/refresh
```

Cookie: `refresh_token=...`

```text
Cookie 검증 → Redis 확인 → Rotation → 새 JWT → 새 Refresh
```

```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "expiresIn": 3600
  }
}
```

---

# 7. Logout

```http
POST /api/v1/auth/logout
```

```text
Refresh 삭제 → Redis Session 삭제 → Audit → Cookie 삭제
```

```json
{ "success": true }
```

---

# 8. Current User

```http
GET /api/v1/auth/me
```

```json
{
  "id": "uuid",
  "name": "Nasuyu",
  "email": "...",
  "avatarUrl": "...",
  "role": "DEVELOPER",
  "workspace": {
    "id": "...",
    "name": "Platform Team"
  }
}
```

---

# 9. Switch Workspace

```http
PATCH /api/v1/auth/workspace
```

```json
{ "workspaceId": "uuid" }
```

- Workspace 멤버 확인
- JWT 재발급

---

# 10. Validate Token

```http
POST /api/v1/auth/validate
```

Frontend 최초 진입 · Gateway 인증용

```json
{ "valid": true, "expiresIn": 2800 }
```

---

# 11. Revoke Session

```http
DELETE /api/v1/auth/session/{sessionId}
```

다른 기기 로그아웃

---

# 12. User Permissions

```http
GET /api/v1/auth/permissions
```

```json
{
  "permissions": [
    "PROJECT_CREATE",
    "PROJECT_DELETE",
    "DEPLOY",
    "AI_REVIEW"
  ]
}
```

---

# 13. DTO

```java
public record LoginResponse(
    UUID userId,
    String accessToken,
    long expiresIn,
    UserSummary user
) {}

public record WorkspaceRequest(
    UUID workspaceId
) {}
```

---

# 14. Validation

- Workspace: `@NotNull UUID workspaceId`
- JWT: `Authorization: Bearer xxxx` 필수

---

# 15. Error Codes

| Code | 설명 |
|------|------|
| AUTH001 | OAuth 실패 |
| AUTH002 | JWT 만료 |
| AUTH003 | Refresh 만료 |
| AUTH004 | Workspace 접근 불가 |
| AUTH005 | User Disabled |
| AUTH006 | Invalid State |
| AUTH007 | Session Expired |
| AUTH008 | Invalid Signature |

---

# 16. Events

`user.logged_in` · `user.logged_out` · `workspace.changed` · `token.refreshed`

---

# 17. Audit

`LOGIN` · `LOGOUT` · `REFRESH` · `WORKSPACE_CHANGE`

---

# 18. Sequence

```text
GitHub Login → OAuth → Callback → User 조회 → JWT → Redis → Audit → Response
```

---

# 19. Spring Package

```text
auth/
  controller/ service/ facade/ dto/ entity/ repository/
  security/ oauth/ jwt/ filter/ handler/
```

---

# 20. Security Filter Chain

```text
Request → JWT Filter → Authentication → Authorization → Controller
```

---

# 21. Redis Keys

```text
session:{user}
refresh:{user}
oauth:{state}
login:{ip}
```

---

# 22. Transaction

- 읽기: `readOnly=true`
- 쓰기: `@Transactional`

---

# 23. Test Cases

| 영역 | 케이스 |
|------|--------|
| OAuth | 정상 · state 불일치 · code 재사용 · GitHub API 실패 |
| JWT | 만료 · 위조 · Signature 오류 |
| Refresh | 정상 · 만료 · Rotation |
| Logout | 정상 · 이미 삭제됨 |

---

# 24. Acceptance Criteria

- [ ] GitHub OAuth 정상
- [ ] JWT 발급
- [ ] Refresh Rotation
- [ ] Redis Session
- [ ] Workspace 변경
- [ ] RBAC
- [ ] Audit
- [ ] Swagger 자동 생성
- [ ] Global Exception Handler

---

# 25. Cursor 구현 지침

### 기술

Java 21 · Spring Boot 3.5 · Security 6 · OAuth2 Client · JWT **RS256** · Redis · PostgreSQL · JPA · Lombok · MapStruct · SpringDoc

### 아키텍처

```text
Controller → Facade → Service → Repository
```

### 원칙

- Controller에 비즈니스 로직 금지
- Entity 직접 반환 금지 · DTO(record)만
- 모든 API `ApiResponse<T>` 래퍼
- 인증 이벤트 Audit 필수
- OAuth/JWT/Redis 예외는 GlobalExceptionHandler
