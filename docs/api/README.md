# Nimbus API Specs

## OpenAPI 스냅샷 (고정)

| 항목 | 경로 |
|------|------|
| Repo | [openapi.yaml](./openapi.yaml) |
| 런타임 | `GET /v3/api-docs` 또는 `GET /api/v1/openapi.yaml` |
| 메타 | `GET /api/v1/openapi.json` |

핵심 엔드포인트만 담은 **OpenAPI 3.0** 스냅샷이다.  
전체 시나리오·AC는 아래 Engineering Spec을 본다.

## Engineering Specs

| Spec | 주제 |
|------|------|
| [API-01](./API-01-Authentication.md) | Auth |
| [API-02](./API-02-Workspace.md) | Workspace |
| [API-03-01](./API-03-01-Project-Core.md) | Project |
| [API-03-02](./API-03-02-Environment.md) | Environment |
| [API-03-03](./API-03-03-Variable-Secret.md) | Variable/Secret |
| [API-03-05](./API-03-05-Service-Catalog.md) | Catalog |
| [API-04-01](./API-04-01-Service-Wizard-Core.md) | Wizard |
| [API-04-02](./API-04-02-AI-Recommendation.md) | AI |
| [API-04-03](./API-04-03-Provisioning-Orchestration.md) | Provision |
| [API-05-01](./API-05-01-GitHub-Integration.md) | GitHub |

## 스모크 테스트

```bash
cd apps/api && ./gradlew test --tests "io.nimbus.platform.PlatformApiSmokeTest"
```
