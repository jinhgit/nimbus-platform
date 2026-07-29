# Nimbus Platform — Docs

설계·구현 문서 모음이다.  
코드는 `apps/`, 설계는 여기(`docs/`)를 기준으로 본다.

---

## 어디부터 읽나

| 목적 | 문서 |
|------|------|
| 한 번에 전체 그림 | [prd/PRD-MASTER.md](prd/PRD-MASTER.md) |
| 확정된 설계 결정 (모순 정리) | [architecture/03-Canonical-Decisions.md](architecture/03-Canonical-Decisions.md) |
| 용어 | [architecture/04-Glossary.md](architecture/04-Glossary.md) |
| 문서 목록 | [INDEX.md](INDEX.md) |
| 로컬 실행 | 루트 [README.md](../README.md) |

권장 순서:

1. Canonical Decisions  
2. PRD Master  
3. 관심 도메인 API Spec  
4. 구현

---

## 폴더

```text
docs/
├── README.md          ← 지금 파일
├── INDEX.md           ← 전체 목록
├── prd/               ← 제품 요구사항 (v1.1 ~ Master)
├── api/               ← Engineering API Spec (구현 단위)
└── architecture/      ← 시스템·도메인·모노레포·용어
```

---

## 문서 역할 구분

| 종류 | 쓰는 때 |
|------|---------|
| **PRD** | Why / What / 범위 / UX·NFR |
| **API Spec** | How — endpoint, DTO, event, AC (코드 생성용) |
| **Architecture** | 구조 결정, 도메인 맵, monorepo 현실 |

초기 초안(v1.1~)과 이후 확장(API-03~) 사이에 용어·단계 수가 달라진 부분이 있다.  
**충돌하면 Canonical Decisions 를 따른다.**

---

## 현재 구현 상태 (문서 기준)

| 단계 | 상태 |
|------|------|
| 설계 문서 | 있음 |
| Monorepo 뼈대 | 있음 (`apps/api`, `apps/web`, compose) |
| Auth ~ Provision 구현 | 아직 (다음 스프린트) |
