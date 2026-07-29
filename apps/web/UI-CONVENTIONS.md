# Nimbus Web UI Conventions

**기준:** 2026-07-29 — 사용자 확정. 이후 UI 작업 시 **이 형식을 기본으로 유지**한다.

---

## 1. 언어 구조 (Copy)

| 영역 | 언어 | 예 |
|------|------|-----|
| **사이드바** 메뉴 · 브랜드 · Platform Portal | **English** | Dashboard, Projects, Create Service, Settings |
| **메인 제목** (`h1` / PageHeader `title`) | **English** | Dashboard, Services, Catalog, Audit |
| **eyebrow** (작은 상단 라벨) | English 허용 | Overview, Deploy, Workspace, Golden Path |
| **설명 · 헬프 · 폼 라벨 · Empty · 버튼 문구 · 토스트/에러** | **한국어** | 서비스 생성, 환경 추가, 승격, 프로젝트가 없습니다 |
| **도메인 코드 / enum / 기술 식별자** | English 유지 | READY, DEV, STAGE, LOG_LEVEL, AES |

### 원칙

- 크롬(내비·페이지 타이틀)은 제품감 있게 영어.
- 사람이 읽는 상세 설명·조작 카피는 한국어.
- 억지로 모든 걸 한 언어로 맞추지 않는다.

---

## 2. 시각 / 구조 (이미 확정된 톤)

| 항목 | 규칙 |
|------|------|
| 테마 | 다크 기본 (`globals.css` 토큰) |
| 아이콘 | **선형(stroke) SVG only** — `components/icons.tsx` |
| 공통 UI | `Page` / `PageHeader` / `Card` / `StatusBadge` / `nimbus-*` 유틸 |
| 사이드바 | sticky · 섹션 구분(main / Operations / Workspace) · 활성 인디케이터 · 이니셜 아바타 |
| 카드 | `nimbus-card` 그라데이션 보더/그림자 |
| 버튼 | primary = 그라데이션 파랑, ghost = 보더 |

아이콘이 제목과 자연스럽지 않으면 **넣지 않는다** (억지 매칭 금지).

---

## 3. 하지 말 것

- 사이드바/메인 타이틀을 한국어로 되돌리기
- filled 아이콘 · 이모지 아이콘 남발
- 페이지마다 다른 카드/버튼 스타일 새로 만들기 (기존 토큰 재사용)

---

## 4. 상태 UI (Loading / Empty / Error)

전 화면에서 **동일 컴포넌트**를 쓴다. 페이지마다 다른 빨간 박스·“로딩 중…” 문자열을 새로 만들지 않는다.

| 상태 | 컴포넌트 | 카피 (한국어 body) |
|------|----------|-------------------|
| 로딩 (페이지/섹션) | `LoadingBlock` | 불러오는 중… / 대시보드를 불러오는 중… |
| 로딩 (인라인) | `LoadingInline` | 불러오는 중… |
| 에러 | `ErrorBanner` + 선택 `onRetry` | API `error.message` 우선, 없으면 한국어 fallback |
| 성공 토스트성 | `SuccessBanner` | 짧은 확인 문구 |
| 데이터 없음 | `EmptyState` | 제목 + 설명 + (권한 있을 때만) action |
| VIEWER 등 읽기 전용 | `ReadOnlyBanner` | Promote · Secret · 재시도 불가 안내 |

### 권한 (RBAC UI)

- `me.canMutate` / `permissions.canMutate` / dashboard `canMutate` 로 버튼 노출.
- VIEWER: Promote · Secret 추가/보기 · Env 생성 · Project 생성 · Wizard 실행/재시도 버튼 **숨김 또는 비활성**.
- API도 `WorkspacePermissionService.requireMutator` 로 동일 차단 (UI만 숨기지 않음).

---

## 5. 관련 파일

- `src/app/globals.css` — 토큰 · `nimbus-*`
- `src/components/AppShell.tsx` — 내비 영어
- `src/components/ui.tsx` — PageHeader · LoadingBlock · ErrorBanner · EmptyState
- `src/components/icons.tsx` — linear SVG
- `src/lib/api.ts` — `fetchDashboardOverview` · `fetchPermissions` · `canMutate`
