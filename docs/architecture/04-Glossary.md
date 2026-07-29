# Glossary

| 용어 | 의미 |
|------|------|
| **Nimbus** | 이 플랫폼 제품 |
| **IDP** | Internal Developer Platform |
| **DX** | Developer Experience |
| **Workspace** | 팀/조직 단위 최상위 협업 공간 |
| **Project** | 비즈니스 컨텍스트. 여러 Service를 묶음 |
| **Service** | 배포 가능한 애플리케이션 단위 |
| **Environment** | Service의 런타임/인프라 컨텍스트 (DEV/STAGE/PROD) |
| **Deployment** | 특정 Environment에 올라간 배포 이력/상태 |
| **Service Catalog** | Golden Path 템플릿·Blueprint 저장소 |
| **Blueprint** | Service 전체 설계(런타임, DB, Helm, TF, pipeline 등)의 논리 모델 |
| **Golden Path** | 조직이 권장하는 표준 경로/스택 |
| **Service Wizard** | Catalog→AI→Provision 을 잇는 생성 워크플로 UI/엔진 |
| **Provision Job** | Wizard 실행의 비동기 Saga 작업 |
| **GitOps** | Git을 배포 소스로 두고 ArgoCD 등이 맞추는 방식 |
| **ArgoCD Application** | Git 경로와 클러스터를 연결하는 GitOps 앱 정의 |
| **SCM Provider** | GitHub 등을 추상화한 소스 저장소 인터페이스 |
| **Context Builder** | AI에 넣기 전 플랫폼 메타를 JSON 컨텍스트로 모으는 층 |
| **Guardrail** | LLM 응답을 스키마·비즈니스 규칙으로 검증하는 층 |
| **IRSA** | AWS IAM Roles for Service Accounts |
| **HPA** | Horizontal Pod Autoscaler |
| **Soft Delete** | 물리 삭제 대신 `deleted_at` 로 숨김 |
| **TVP** | Thin Viable Platform — 얇게 시작해 확장하는 플랫폼 접근 |
| **Free-only** | 과금 클라우드·유료 API·결제 모델 없이 로컬/OSS/무료 티어만 사용 |
| **Ollama** | 로컬 LLM 런타임 — Nimbus AI 기본 Provider |
