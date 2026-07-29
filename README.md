# Nimbus Platform

AI Native Internal Developer Platform (IDP).

Kubernetes / Helm / Terraform을 몰라도 서비스를 만들고 배포할 수 있는  
Platform Engineering Portal을 목표로 한다.

설계 문서와 monorepo 뼈대가 같은 저장소에 있다.

> Backstage 클론이 아니다.  
> Catalog + Blueprint + GitOps + AI Decision Engine 을 한 플랫폼으로 묶는 방향.

---

## 문서 먼저

| 문서 | 용도 |
|------|------|
| [docs/README.md](docs/README.md) | 문서 허브 |
| [docs/prd/PRD-MASTER.md](docs/prd/PRD-MASTER.md) | 제품 한 장 요약 |
| [docs/architecture/03-Canonical-Decisions.md](docs/architecture/03-Canonical-Decisions.md) | **확정 설계 결정** |
| [docs/INDEX.md](docs/INDEX.md) | 전체 목록 |

초안 PRD 사이에 용어·단계 수가 달랐던 부분은 Canonical 기준으로 맞춰 두었다.

---

## 현재 상태

**Phase 0 — Foundation**

- monorepo: `apps/api`, `apps/web`, `docs`
- Spring Boot API skeleton + `/api/v1/health`
- Next.js dark landing + dashboard placeholder
- Docker Compose: PostgreSQL 16, Redis 7
- PRD / API Spec / Architecture 정리본

**아직:** OAuth, Wizard, Provision, GitHub Adapter 등 도메인 구현

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
docker compose up -d

cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'
# http://localhost:8080/api/v1/health

cd apps/web && npm run dev
# http://localhost:3000
```

`make up` / `make api` / `make web` 도 가능.

---

## 스택

| Layer | 기술 |
|-------|------|
| Web | Next.js 15, TypeScript, Tailwind |
| API | Java 21, Spring Boot 4.0.x, Security, JPA |
| DB | PostgreSQL 16 |
| Cache | Redis 7 |

---

## 도메인 (정본)

```text
Workspace → Project → Service → Environment → Deployment
```

Wizard 7 step · Provision = Saga Job · Deploy path = GitOps (ArgoCD)

---

## 로드맵 요약

Auth → Workspace/Project/Service → Wizard + Provision → GitHub → AI → Observability → v1.0  

상세: [PRD v1.5D](docs/prd/PRD-v1.5D-Engineering-Roadmap.md)

---

## License

Private personal project unless otherwise noted.
