# Nimbus Platform

AI Native Internal Developer Platform (IDP).

Kubernetes / Helm / Terraform을 몰라도 서비스를 만들고 배포할 수 있는  
Platform Engineering Portal을 목표로 한다.

설계 문서와 monorepo 뼈대가 같은 저장소에 있다.

> Backstage 클론이 아니다.  
> Catalog + Blueprint + GitOps + AI Decision Engine 을 한 플랫폼으로 묶는 방향.

### 완전 무료 (Free-only)

- **과금 클라우드 / 유료 LLM API / 결제·구독 모델 없음**
- 로컬: Docker (Postgres, Redis), k3d·kind, Ollama, ArgoCD OSS
- GitHub Free + Actions free 한도 안에서 데모
- 상세: [docs/architecture/05-Free-Only-Constraints.md](docs/architecture/05-Free-Only-Constraints.md)

---

## 문서

설계는 **쌓아 올린 것**이다. 버전 PRD + API Spec 을 유지하고, 위에 요약/맵을 얹었다.

| 문서 | 용도 |
|------|------|
| [docs/README.md](docs/README.md) | 문서 허브 |
| [docs/prd/](docs/prd/) | PRD v1.1~v1.5 + Master 요약 |
| [docs/api/](docs/api/) | 구현용 API Spec |
| [docs/architecture/03-Canonical-Decisions.md](docs/architecture/03-Canonical-Decisions.md) | 설계 누적 맵 (Evolution) |
| [docs/INDEX.md](docs/INDEX.md) | 전체 목록 |

---

## 현재 상태

**Phase 1 — Foundation (Auth / Workspace / Project)**

- monorepo: `apps/api`, `apps/web`, `docs`
- **Dev Login** (free-only 로컬) + GitHub OAuth 훅 (Client ID 설정 시)
- JWT Access Token + Refresh Token
- Workspace / Team / Member API
- Project CRUD (Archive/Restore)
- Next.js: Login · App Shell · Dashboard · Projects · Workspaces
- 로컬 API: H2 파일 DB (`--spring.profiles.active=local`, Docker 불필요)
- Docker Compose: PostgreSQL 16, Redis 7 (선택)

**다음:** Service Catalog · Service Wizard · Provision Saga · GitHub Adapter

---

## 구조

```text
.
├── apps/
│   ├── api/          # Java 21, Spring Boot 4
│   └── web/          # Next.js 15
├── docs/
│   ├── prd/
│   ├── api/
│   └── architecture/
├── docker-compose.yml
├── Makefile
└── .env.example
```

도메인 패키지 자리:

```text
auth · workspace · project · catalog · wizard · ai · provision · github
```

---

## 빠른 시작

```bash
# API (H2 local profile — Docker 없이 기동)
cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'
# http://localhost:8080/api/v1/health

# Web
cd apps/web && npm install && npm run dev
# http://localhost:3000/login  → Dev Login → Dashboard

# (선택) Postgres/Redis
docker compose up -d
```

`make api` / `make web` / `make test-api` 도 가능.

### Dev Login

로컬 기본 활성. 프론트 `/login` 또는:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"name":"Dev","email":"dev@nimbus.local"}'
```

---

## 스택

| Layer | 기술 |
|-------|------|
| Web | Next.js 15, TypeScript, Tailwind |
| API | Java 21, Spring Boot 4.0.x, Security, JPA |
| DB | PostgreSQL 16 |
| Cache | Redis 7 |

---

## 도메인 (누적)

```text
Workspace → Project → Service → Environment → Deployment
```

Wizard = 입력 + AI + Preview + Provision Job  
Deploy path = GitOps (ArgoCD) 보강 · 상세는 docs/

---

## 로드맵 요약

Auth → Workspace/Project/Service → Wizard + Provision → GitHub → AI → Observability → v1.0  

상세: [PRD v1.5D](docs/prd/PRD-v1.5D-Engineering-Roadmap.md)

---

## License

Private personal project unless otherwise noted.
