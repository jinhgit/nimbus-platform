# Nimbus Platform — Docs

설계가 쌓인 공간이다.  
코드는 `apps/`, 설계·명세는 `docs/`.

문서끼리 **어느 하나를 지우거나 폐기하는 구조가 아니다.**  
버전을 쌓고, API 상세를 얹고, 요약 맵을 옆에 둔 형태다.

---

## 어디부터 읽나 (취향대로)

| 목적 | 문서 |
|------|------|
| 전체 한 장 요약 | [prd/PRD-MASTER.md](prd/PRD-MASTER.md) |
| **최종 시연 시나리오 (10~15분)** | [demo/DEMO-SCENARIO.md](demo/DEMO-SCENARIO.md) |
| **PRD vs 구현 매트릭스** | [status/PRD-vs-Implementation.md](status/PRD-vs-Implementation.md) |
| 설계가 쌓인 레이어 맵 | [architecture/03-Canonical-Decisions.md](architecture/03-Canonical-Decisions.md) *(Evolution Map)* |
| 용어 | [architecture/04-Glossary.md](architecture/04-Glossary.md) |
| 처음부터 제품 이야기 | [prd/PRD-v1.1-…](prd/PRD-v1.1-Project-Overview.md) |
| 구현 스펙 | [api/](api/) |
| 목록 전체 | [INDEX.md](INDEX.md) |
| **완전 무료 제약** | [architecture/05-Free-Only-Constraints.md](architecture/05-Free-Only-Constraints.md) |
| 로컬 실행 | [../README.md](../README.md) |

---

## 폴더

```text
docs/
├── README.md · INDEX.md
├── demo/           # 시연 시나리오 · 포트폴리오 가이드
├── prd/            # 제품 요구 (v1.1 ~ Master) — 전부 유효
├── api/            # Engineering Spec — PRD 위에 얹힌 구현 단위
└── architecture/   # 시스템 그림, 도메인 맵, monorepo, 용어, evolution
```

---

## 문서가 쌓인 방식

```text
PRD v1.1  Vision / Epic / IA
   + v1.2  기능 스펙
   + v1.3  GitOps 인프라
   + v1.4  AI Native
   + v1.5A DB
   + v1.5C Frontend
   + v1.5D Roadmap
   + API-01~05  구현 가능 수준 명세
   + Master / Evolution Map / Glossary  찾기 쉬운 인덱스 레이어
```

같은 주제가 여러 문서에 있으면 **삭제된 게 아니라 깊이가 다른 버전**이다.  
예: Wizard 는 v1.2 스케치 + v1.5C UX + API-04 오케스트레이션이 함께 있다.

---

## 구현 상태

| 단계 | 상태 |
|------|------|
| 설계 문서 축적 | ✅ |
| Monorepo 뼈대 | ✅ |
| 시연 MVP (Wizard·GitHub·Obs) | ✅~🔶 |
| 실운영 P0 (Audit · prod 프로필) | ✅ |
| 상세 매트릭스 | [status/PRD-vs-Implementation.md](status/PRD-vs-Implementation.md) |
| 루트 README (포트폴리오) | [../README.md](../README.md) |
