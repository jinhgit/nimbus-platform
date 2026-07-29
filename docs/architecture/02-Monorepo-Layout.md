# Monorepo Layout

Phase 0 에서 잡힌 실제 코드 배치.  
설계 문서 진입: [docs/README.md](../README.md)

```text
.
├── apps/
│   ├── api/                 # Spring Boot 4 / Java 21
│   │   └── src/main/java/io/nimbus/platform/
│   │         common/        # ApiResponse, exception, config, BaseTimeEntity
│   │         health/
│   │         auth/ workspace/ project/ catalog/
│   │         wizard/ ai/ provision/ github/
│   └── web/                 # Next.js 15
│         src/app/           # App Router (/, /dashboard)
│         src/lib/api.ts
├── docs/
│   ├── README.md · INDEX.md
│   ├── prd/                 # Master + v1.x
│   ├── api/                 # Engineering specs
│   └── architecture/        # 구조 · 정본 · 용어
├── docker-compose.yml
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
2. `workspace` / `project` / `service` entity  
3. web shell (sidebar)
