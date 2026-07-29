# Free-Only Constraints

**이 프로젝트는 완전 무료로 운영·구현한다.**  
과금 클라우드 리소스, 유료 API, 결제 연동, 상용 SaaS 유료 플랜을 쓰지 않는다.

추가된 제약 레이어 (기존 PRD의 “EKS / 클라우드 비용” 이야기는 **로컬·OSS 대체 경로**로 구현한다).

---

## 1. 원칙

| 원칙 | 설명 |
|------|------|
| $0 기본 | 개인이 카드 등록·크레딧 충전 없이 재현 가능해야 한다 |
| OSS 우선 | 실행 환경은 로컬 Docker / 오픈소스 |
| 유료 API 금지 | OpenAI·Claude·유료 Gemini 등 **결제 필요 모델 미사용** |
| 클라우드 청구 금지 | AWS/GCP/Azure 유료 리소스(EKS, RDS, ALB, Route53 등) 미사용 |
| 구조는 확장 가능 | 인터페이스는 남기되, **기본 구현체는 free path** |

“나중에 돈 내면 쓰는 옵션”을 문서에 적을 수는 있지만, **기본 개발·데모·README 경로는 전부 free**.

---

## 2. 영역별 Free Path

| 영역 | 쓰지 않음 | Free 구현 |
|------|-----------|-----------|
| DB | RDS, 유료 managed DB | Docker **PostgreSQL 16** |
| Cache | ElastiCache 등 | Docker **Redis 7** |
| K8s | EKS / GKE / AKS 과금 | **k3d** 또는 **kind** (로컬) |
| Git | 유료 전용 기능 의존 | **GitHub Free** (OAuth App / 개인·공개 협업 범위) |
| CI | 유료 러너 | **GitHub Actions free tier** 안에서, 또는 로컬 생성만 |
| Registry | 유료 ECR 필수 | 로컬 이미지 / kind 로드 / **GHCR free** 범위 주의 |
| GitOps | 상용 GitOps SaaS | **ArgoCD** OSS (로컬 클러스터) |
| Helm / TF | Terraform Cloud 유료 | 로컬 **Helm CLI**, **Terraform OSS** (파일 생성 + 선택적 local apply) |
| AI | 유료 LLM API | **Ollama** 로컬 모델 (MVP 기본 AIProvider) |
| 시크릿 | 유료 Vault Cloud | 로컬 암호화 + K8s Secret (필요 시 로컬 Vault OSS는 선택) |
| 관측 | Grafana Cloud 유료 | 로컬 **Prometheus + Grafana + Loki** (compose/helm) |
| 도메인/TLS | Route53, 유료 인증서 | `localhost` / nip.io 류 / 클러스터 내부 DNS |
| 알림 | 유료 Slack 플랜 의존 | 로그·UI 알림 우선 (Webhook은 free 워크스페이스만) |

---

## 3. AI (중요)

- MVP `AIProvider` 구현체: **Ollama** (`http://localhost:11434`)
- 모델 예: 로컬에서 pull 가능한 소형 모델 (환경에 맞게)
- 네트워크 유료 API 키 필수 경로는 기본값에서 제외
- Guardrail / Context Builder / Decision Engine **구조는 그대로** — provider만 local

Cost Advisor의 “클라우드 청구액”은 free 모드에서:

- 실제 $ 과금 추정 대신 **리소스 점수 / 대략적 부하 등급** (이미 PRD MVP에 있던 방향)

---

## 4. Infra / Provision (중요)

API-04-03 의 MVP free 범위와 맞춤:

| Step | Free 동작 |
|------|-----------|
| GitHub | Free 계정 API로 repo/workflow **생성** |
| Helm | chart/values **파일 생성** (+ 선택: 로컬 cluster helm upgrade) |
| Terraform | `.tf` / `.tfvars` **생성** (AWS apply 기본 비활성) |
| ArgoCD | Application **manifest 생성** (+ 선택: 로컬 ArgoCD 적용) |
| Deploy | **k3d/kind** 대상 |
| Rollback | Job Step 보상 + 로컬 리소스 정리 시뮬 |

AWS EKS, Route53, ACM, ALB, RDS 실프로비저닝은 **이 프로젝트 범위 밖** (구조 문서에만 “확장 여지”로 남길 수 있음).

---

## 5. GitHub Free 사용 시 주의

- OAuth App / PAT 는 free 계정으로 가능
- API **Rate Limit** 은 free 한도 안에서 Queue + Retry (기존 설계 유지)
- Actions 분 한도 초과 방지: 데모 워크플로는 가볍게, 또는 “파일만 생성하고 Actions 실행은 옵션”
- 조직(Org) 유료 기능 의존 금지

---

## 6. 제품 안의 “Cost / FinOps”

- **과금 모델(결제·구독·크레딧 판매) 없음** — Nimbus 자체도 과금 제품이 아님
- FinOps 대시보드의 클라우드 청구 연동은 free 모드에서 비활성
- UI에 비용 위젯이 있다면 “예상 리소스 점수” 수준 또는 placeholder

---

## 7. 로컬 권장 구성 (전부 free)

```text
Docker Compose
  ├── PostgreSQL
  ├── Redis
  └── (optional) Ollama는 호스트 설치도 가능

k3d or kind
  ├── sample apps
  ├── (optional) ArgoCD
  └── (optional) kube-prometheus-stack / Loki

Nimbus
  ├── apps/api
  └── apps/web
```

---

## 8. 구현 체크리스트

- [ ] README 기본 경로에 유료 계정 생성 단계 없음
- [ ] `.env.example` 에 유료 API 키 필수 항목 없음 (Ollama URL 등)
- [ ] Provision 기본 profile = `local` / `free`
- [ ] Terraform AWS apply 코드가 있어도 **default off**
- [ ] AI 기본 bean = OllamaProvider
- [ ] 데모 스크립트가 인터넷 유료 엔드포인트 없이 동작 (GitHub free API 제외)

---

## 9. 기존 문서와의 관계

| 기존 문서 | Free 모드에서의 읽기 |
|-----------|----------------------|
| PRD v1.3 EKS/ALB 등 | 아키텍처 학습용 + 확장 여지. **실행은 k3d/kind** |
| API-04-02 Gemini/Groq 등 | 어댑터 확장 여지. **기본은 Ollama** |
| Cost ₩ 표기 | 개념 예시. 구현은 점수/등급 또는 생략 |
| Multi-cloud | 이후 이야기. free MVP 비포함 |
