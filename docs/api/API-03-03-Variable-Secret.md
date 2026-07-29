# API-03-03 Variable & Secret

**Nimbus Platform · Version 2.0**  
**Configuration = Platform Configuration Resource**

---

> **이어서 보면 좋은 문서:** [PRD-MASTER](../prd/PRD-MASTER.md) · [Design Evolution Map](../architecture/03-Canonical-Decisions.md) · 연관 PRD (`docs/prd/`)


# 1. Domain Overview

```text
Variable → ConfigMap
Secret   → K8s Secret → External Secret → GitHub Secret → Runtime Injection
```

| 종류 | Kubernetes | GitHub | 암호화 |
|------|------------|--------|--------|
| Variable | ConfigMap | Variable | X |
| Secret | Secret | Secret | O |

**Variable 예:** `SPRING_PROFILES_ACTIVE` · `SERVER_PORT` · `LOG_LEVEL`  
**Secret 예:** `DB_PASSWORD` · `JWT_SECRET` · `AWS_ACCESS_KEY` · `OPENAI_API_KEY`

---

# 2. Domain Model

```text
Service → Environment → Configuration
  ├── Variable · Secret
  ├── ConfigMap · Kubernetes Secret
```

## Configuration Source

`MANUAL` · `GITHUB` · `VAULT` · `AWS_SECRET_MANAGER` · `ENV_FILE`  
**MVP:** MANUAL · GITHUB

---

# 3. API List (14)

| API | Method |
|-----|--------|
| Create/Update/Delete/List Variable | POST/PATCH/DELETE/GET |
| Create/Update/Delete/List Secret | POST/PATCH/DELETE/GET |
| Mask Secret | GET |
| Rotate Secret | POST |
| Import/Export Variables | POST/GET |
| Sync GitHub Secret | POST |
| Configuration History | GET |

---

# 4. Variable APIs

### Create

```http
POST /api/v1/environments/{environmentId}/variables
```

권한: `CONFIG_WRITE`

```json
{ "key": "SPRING_PROFILES_ACTIVE", "value": "prod" }
```

- key: `^[A-Z0-9_]+$`
- value: 최대 500자
- ConfigMap 생성 예약 · Event `variable.created`

### Update

```http
PATCH /api/v1/variables/{id}
```

Value만 수정 · Key 불가  
ConfigMap Version 증가 · **Deployment Required = true**

### Delete

Soft Delete · ConfigMap 재생성

### List

```http
GET /api/v1/environments/{id}/variables
```

Search: key · prefix

---

# 5. Secret APIs

### Create

```http
POST /api/v1/environments/{id}/secrets
```

```json
{ "key": "DB_PASSWORD", "value": "mypassword" }
```

**평문 DB 저장 금지** → AES-256 암호화 → K8s Secret Job

### Update

Version 증가 · Rotation 가능

### Delete

Soft Delete · Deployment 존재 시 경고

### List

```json
[{ "key": "DB_PASSWORD", "value": "********", "updatedAt": "..." }]
```

**Secret 값 절대 반환 금지**

### Get (Mask)

```http
GET /api/v1/secrets/{id}
```

권한: OWNER · ADMIN 만 · 마스킹 응답

### Rotate

```http
POST /api/v1/secrets/{id}/rotate
```

```text
새 Secret → Version↑ → Old Archive → Deployment Required
```

```json
{ "status": "ROTATED" }
```

---

# 6. Import / Export

```http
POST /api/v1/environments/{id}/variables/import
GET  /api/v1/environments/{id}/variables/export
```

Format: `.env` · yaml · json

---

# 7. GitHub Secret Sync

```http
POST /api/v1/environments/{id}/github-sync
```

```text
Nimbus Secret → GitHub Secret → Repository Secret
```

비동기 Job

---

# 8. Configuration History

```http
GET /api/v1/environments/{id}/config/history
```

```json
[{ "version": 3, "action": "UPDATE", "user": "Nasuyu", "createdAt": "..." }]
```

---

# 9. Entity

```java
// Variable
UUID id; String key; String value; Environment environment; Version version;

// Secret
UUID id; String key; String encryptedValue; SecretVersion version; Boolean active;
```

DTO: CreateVariableRequest · UpdateVariableRequest · CreateSecretRequest · VariableResponse · SecretSummaryResponse

---

# 10. Error Codes

| Code | 설명 |
|------|------|
| CONFIG001 | Variable 없음 |
| CONFIG002 | Secret 없음 |
| CONFIG003 | 중복 Key |
| CONFIG004 | 암호화 실패 |
| CONFIG005 | GitHub Sync 실패 |
| CONFIG006 | Rotation 실패 |
| CONFIG007 | Import 실패 |

---

# 11. Events / Audit

Events: variable.created/updated/deleted · secret.created/updated/rotated · github.secret.synced  
Audit: VARIABLE_* · SECRET_* · CONFIG_IMPORTED · GITHUB_SYNC

---

# 12. Sequences

### Variable

```text
Validation → 저장 → ConfigMap Job → Audit
```

### Secret

```text
Encrypt → DB → Secret Job → Audit
```

### GitHub Sync

```text
Nimbus → GitHub API → Repo Secret → Audit
```

---

# 13. Acceptance Criteria

- [ ] Variable/Secret CRUD
- [ ] AES-256 · Masking
- [ ] ConfigMap/K8s Secret 생성 준비
- [ ] GitHub Sync · Import/Export · Version · Rotation
- [ ] Audit · Event

---

# 14. 구현 지침

```text
configuration/controller|facade|service|repository|entity|dto|mapper|crypto|validator|event|exception
```

- Variable ≠ Secret (별도 Aggregate)
- 평문 저장/응답 금지
- `CryptoService` + `SecretProvider` 인터페이스

```text
SecretProvider
  ├── LocalCryptoProvider (MVP)
  ├── AWSKmsProvider (v2)
  ├── HashiCorpVaultProvider (v2)
  └── ExternalSecretsProvider (v3)
```

- Variable 변경 → ConfigMap 재생성
- Secret 변경 → K8s Secret 재생성 + Deploy Required
- GitHub Sync 비동기 · Version History 롤백 가능

### Runtime Injection

```text
Secret → Kubernetes Secret → Pod Environment Variable
```
