# Nimbus Platform

AI Native Internal Developer Platform (IDP).

개발자가 Kubernetes / Helm / Terraform을 직접 다루지 않아도 서비스를 만들고 배포할 수 있는 Platform Engineering Portal을 목표로 한다.  
설계 문서와 monorepo 뼈대가 같이 들어 있다.

> Backstage 클론이 아니라, Backstage + Port + Humanitec + GitHub + ArgoCD + AI Ops 를 한 플랫폼으로 묶는 방향.

---

## 현재 상태

**Phase 0 — Foundation (뼈대)**

- monorepo 구조 (`apps/api`, `apps/web`, `docs`)
- Spring Boot API 스켈레톤 + health endpoint
- Next.js 웹 스켈레톤 (다크 테마 홈 / dashboard placeholder)
- Docker Compose (PostgreSQL 16, Redis 7)
- PRD / API Engineering Spec 문서

아직 구현 전: OAuth, Wizard, Provision, GitHub Adapter 등 (docs 참고)

---

## 구조

```text
.
├── apps/
│   ├── api/          # Spring Boot (Java 21)
│   └── web/          # Next.js 15 (TypeScript)
├── docs/
│   ├── prd/          # PRD v1.1 ~ v1.5D
│   ├── api/          # API-01 ~ API-05 명세
│   └── architecture/
├── docker-compose.yml
├── Makefile
└── .env.example
```

### API 패키지 (도메인 뼈대)

```text
io.nimbus.platform
  common/     # ApiResponse, exception, config
  health/
  auth/       # API-01
  workspace/  # API-02
  project/    # API-03
  catalog/    # API-03-05
  wizard/     # API-04-01
  ai/         # API-04-02
  provision/  # API-04-03
  github/     # API-05-01
```

---

## 빠른 시작

### 1. 인프라

```bash
docker compose up -d
# postgres :5432  /  redis :6379
```

### 2. API

```bash
cd apps/api
./gradlew bootRun --args='--spring.profiles.active=local'
# http://localhost:8080/api/v1/health
```

### 3. Web

```bash
cd apps/web
cp .env.example .env.local   # 필요 시
npm install --cache ./.npm-cache   # 로컬 캐시 이슈 있을 때
npm run dev
# http://localhost:3000
```

또는 루트에서:

```bash
make up
make api    # 별도 터미널
make web    # 별도 터미널
```

---

## 스택

| Layer | 기술 |
|-------|------|
| Web | Next.js 15, TypeScript, Tailwind |
| API | Java 21, Spring Boot 4, Security, JPA, Redis |
| DB | PostgreSQL 16 |
| Cache | Redis 7 |
| Docs | PRD + Engineering API Spec (`docs/`) |

Spring Boot는 start.spring.io 기준으로 **4.0.x** 를 사용한다. (초기 PRD의 3.5 대신 현재 호환 버전)

---

## 문서

전체 인덱스: [docs/INDEX.md](docs/INDEX.md)

| 영역 | 경로 |
|------|------|
| PRD | [docs/prd/](docs/prd/) |
| API Spec | [docs/api/](docs/api/) |
| Architecture | [docs/architecture/](docs/architecture/) |

---

## 로드맵 (요약)

1. Auth (GitHub OAuth + JWT)
2. Workspace / Project
3. Service Wizard + Provision Job
4. GitHub Integration
5. AI Recommendation
6. Observability 연동

상세: [PRD v1.5D Engineering Roadmap](docs/prd/PRD-v1.5D-Engineering-Roadmap.md)

---

## 원칙

- Developer First / Zero YAML / GitOps First
- 긴 작업은 Job + Saga (동기 provision 금지)
- AI는 채팅이 아니라 Decision Engine
- 큰 기능 단위마다 문서 갱신 + commit/push

---

## License

Private personal project. All rights reserved unless otherwise noted.
