# API-05-01 GitHub Integration

**Nimbus Platform · Version 2.0**  
**GitHub = Platform Resource Provider / Source of Truth** (단순 API 호출 레이어 아님)

---

> **정본 우선:** 도메인 계층·Wizard 7step·GitOps·스택은 [`docs/architecture/03-Canonical-Decisions.md`](../architecture/03-Canonical-Decisions.md) 기준.


# 1. Domain Overview

책임:

- GitHub OAuth / App 인증
- Repository 생성 · Template 적용
- Branch Protection · Actions Workflow
- Secret 관리 · PR · Release · Webhook
- GitHub API Health

**비즈니스 계층은 GitHub REST를 직접 호출하지 않음** → Adapter / `GitProvider`

---

# 2. Architecture

```text
Developer → Nimbus API → GitHub Integration → GitHub Adapter
  → Repository / Actions / Webhook → GitHub REST API
```

---

# 3. Authentication

| 버전 | 방식 |
|------|------|
| MVP | GitHub OAuth App |
| 권장 | **GitHub App** (권한·Repo·Secret·Webhook 관리에 유리) |
| v2 | GitHub App 정식 |

---

# 4. Domain Model

```text
GitHubConnection
  ├── Repository · Workflow · Secret
  ├── Webhook · BranchProtection · Release
```

## Repository Lifecycle

```text
Requested → Created → Template Applied → Workflow Generated
→ Secrets Injected → Branch Protected → Ready
```

---

# 5. API List (16)

| API | Method |
|-----|--------|
| Connect / Get / Disconnect GitHub | POST/GET/DELETE |
| Create / Get / Delete Repository | POST/GET/DELETE |
| Apply Repository Template | POST |
| Generate / Update Workflow | POST/PATCH |
| Create Branch Protection | POST |
| Create Secret / Sync Secrets | POST/POST |
| Create Pull Request | POST |
| Create Release | POST |
| Register Webhook | POST |
| GitHub Health | GET |

---

# 6. Connect GitHub

```http
POST /api/v1/github/connect
```

```json
{ "installationId": "123456" }
```

```text
OAuth/App 인증 → Access Token 저장 → Connection 생성 → Audit
```

```json
{ "connectionId": "uuid", "status": "CONNECTED" }
```

---

# 7. Create Repository

```http
POST /api/v1/github/repositories
```

```json
{
  "workspace": "nimbus",
  "repository": "payment-service",
  "visibility": "PRIVATE",
  "template": "spring-api"
}
```

Validation: 이름 중복 불가 · Connection 존재 · Template 존재

```text
GitHub API → Repo 생성 → README → Default Branch → Response
```

```json
{
  "repositoryId": "uuid",
  "url": "https://github.com/nimbus/payment-service"
}
```

---

# 8. Get / Delete Repository

```http
GET    /api/v1/github/repositories/{repositoryId}
DELETE /api/v1/github/repositories/{repositoryId}
```

Delete 기본: **Archive** · Hard Delete v2

---

# 9. Apply Template

```http
POST /api/v1/github/repositories/{repositoryId}/template
```

자동: README · .gitignore · LICENSE · 폴더 구조 · GitHub Actions

### 자동 구조

```text
payment-service/
├── src/
├── helm/
├── terraform/
├── docs/
├── README.md
└── .github/
```

---

# 10. Generate Workflow

```http
POST /api/v1/github/repositories/{repositoryId}/workflow
```

생성:

```text
.github/workflows/ci.yml
.github/workflows/cd.yml
```

---

# 11. Branch Protection

```http
POST /api/v1/github/repositories/{repositoryId}/branch-protection
```

정책: Require PR · Status Check · Review · Signed Commit (v2)

---

# 12. Secret / Sync

```http
POST /api/v1/github/repositories/{repositoryId}/secrets
POST /api/v1/github/repositories/{repositoryId}/sync
```

```json
{ "key": "DOCKER_USERNAME", "value": "********" }
```

- 암호화 후 GitHub 전송
- 응답에 값 반환 금지
- Sync: Nimbus Secret → GitHub Repository Secret

---

# 13. Pull Request / Release

```http
POST /api/v1/github/pull-requests
POST /api/v1/github/releases
```

PR: base · head · title  
Release: tag · name

---

# 14. Webhook

```http
POST /api/v1/github/webhooks
```

수신 이벤트: push · pull_request · workflow_run · release · repository · deployment  
Endpoint: `POST /webhook/github`  
**HMAC Signature 검증 필수**

---

# 15. Health

```http
GET /api/v1/github/health
```

```json
{
  "status": "UP",
  "rateLimit": 4821,
  "provider": "GitHub"
}
```

---

# 16. Adapter / SCM Abstraction

```java
public interface GitHubAdapter {
    Repository createRepository(...);
    void createWorkflow(...);
    void createSecret(...);
    void createWebhook(...);
}

public interface GitProvider {
    Repository createRepository(CreateRepositoryCommand command);
    void applyTemplate(UUID repositoryId);
    void createWorkflow(UUID repositoryId);
    void createSecret(UUID repositoryId, SecretCommand command);
    void registerWebhook(UUID repositoryId);
}
```

```text
SCM Provider
  ├── GitHub Adapter (MVP)
  ├── GitLab Adapter (v2)
  ├── Bitbucket Adapter (v2)
  └── Azure DevOps Adapter (v3)
```

Wizard / Provisioning 은 **GitHub를 직접 알지 않음** · SCM 인터페이스만 의존

---

# 17. Entity / DTO

```java
// GitHubRepository
UUID id; String repository; String organization;
String defaultBranch; RepositoryStatus status;

// GitHubConnection
UUID id; String installationId; String account; Boolean active;
```

DTO: ConnectGitHubRequest · CreateRepositoryRequest · RepositoryResponse · WorkflowResponse · WebhookResponse

---

# 18. Error Codes

| Code | 설명 |
|------|------|
| GITHUB001 | 연결 없음 |
| GITHUB002 | Repository 중복 |
| GITHUB003 | Workflow 생성 실패 |
| GITHUB004 | Secret 생성 실패 |
| GITHUB005 | Rate Limit 초과 |
| GITHUB006 | Webhook 등록 실패 |
| GITHUB007 | Branch Protection 실패 |

---

# 19. Retry / Rate Limit

| 기능 | Retry |
|------|------:|
| Repository 생성 | 3 |
| Workflow 생성 | 2 |
| Secret 생성 | 3 |
| Webhook 등록 | 2 |

Backoff: Exponential  
429 → Retry-After 확인 → 대기 → 재시도

---

# 20. Events / Audit

Events: github.connected · repository.created/archived · workflow.generated · secret.synced · pull-request.created · release.created · webhook.registered  

Audit: GITHUB_CONNECTED · REPOSITORY_CREATED · WORKFLOW_GENERATED · SECRET_SYNCED · WEBHOOK_REGISTERED

---

# 21. Sequences

### Repository

```text
Wizard → GitHub Service → Adapter → GitHub API → Event → Audit
```

### Secret Sync

```text
Nimbus Secret → Encrypt → Adapter → GitHub Secret
```

---

# 22. Test Cases

- Connection · Installation 없음
- Repo 생성/중복/Org 없음
- Workflow YAML 오류
- Secret 암호화
- Webhook Signature 실패
- Rate Limit 429 → Retry

---

# 23. Acceptance Criteria

- [ ] Connect · Repo · Template · Workflow · Branch Protection
- [ ] Secret Sync · PR · Release · Webhook
- [ ] Retry · Rate Limit · Adapter · Event · Audit

---

# 24. 구현 지침

```text
github/controller|facade|service|adapter|webhook|workflow|dto|entity|repository|validator|event|exception
```

- GitHub API는 `GitProvider` 경유만
- GitHub App 기본 설계
- Template/Workflow/Protection = Provision Step
- Webhook HMAC 검증
- Rate Limit + Exponential Backoff
- GitLab/Bitbucket 확장 가능 Provider 유지
