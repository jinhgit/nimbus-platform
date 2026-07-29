# Monorepo Layout

Phase 0 에서 잡힌 실제 코드 배치.

```text
Nimbus Platform/
├── apps/
│   ├── api/                 # Backend
│   │   └── src/main/java/io/nimbus/platform/
│   │         common/ api, config, domain, exception
│   │         health/
│   │         auth/ workspace/ project/ catalog/
│   │         wizard/ ai/ provision/ github/
│   └── web/                 # Frontend
│         src/app/           # App Router
│         src/lib/api.ts     # API client stub
├── docs/                    # PRD + API specs
├── docker-compose.yml       # postgres, redis
├── Makefile
└── .env.example
```

## 로컬 포트

| 서비스 | 포트 |
|--------|------|
| Web | 3000 |
| API | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |

## 다음 작업

1. `auth` — GitHub OAuth + JWT
2. `workspace` / `project` entity + migration
3. web layout (sidebar, dark shell)
