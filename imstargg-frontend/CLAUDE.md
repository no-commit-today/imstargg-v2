<!-- markdownlint-disable-next-line MD041 -->
@AGENTS.md

# imstargg-frontend

브롤스타즈 통계 프론트엔드. Next.js App Router + RSC + TanStack Query.

## 명령어

```bash
pnpm dev              # 개발 서버
pnpm build            # 프로덕션 빌드
pnpm typecheck        # tsc --noEmit
pnpm lint
pnpm test:run         # Vitest
pnpm test:e2e         # Playwright (prod API 호출)
```

## 아키텍처 레이어 (단방향 의존)

```text
app/  ──▶  src/hooks/  ──▶  src/api/   ──▶  외부 HTTP
            │
            ▼
        src/domain/  (DTO → 화면 모델 매퍼)
```

- 컴포넌트/RSC는 `src/api/`를 직접 부르지 않고 훅 또는 endpoint 객체 (`playerApi` 등)를 통해서만 호출.
- TanStack Query 키는 `src/hooks/queries/queryKeys.ts`에서만 생성.
- 백엔드 응답 DTO 타입은 `src/api/types.ts`. 변경 시 `src/domain/*/mappers.ts`도 함께 수정.

## 데이터 패칭

- **RSC-first**: 페이지 초기 데이터는 서버 컴포넌트에서 `apiFetch` 호출 + `next.revalidate`.
- **클라이언트 인터랙션**: `useQuery` `initialData`로 RSC 데이터 이어받음. 갱신/폴링 등 동적 영역은 TanStack Query.

## 갱신(Renewal)

`POST /v1/{players|clubs}/{tag}/renewal` 트리거 → `progressing=true` 동안 1초 폴링 → `false`로 전환 시 자동 invalidate. 훅: `usePlayerRenewal`.

## 테스트 원칙

- **네트워크 경계만 모킹**(MSW). 우리 코드(`src/api/*`, `src/hooks/*`, 컴포넌트)는 모킹 금지.
- **사용자 관점**(Testing Library의 role/text 쿼리). 구현 상세 검증 금지.
- 단위 테스트는 순수 함수만(`lib/`, `domain/*/mappers`).
- E2E는 prod API 직접 호출. 테스트 태그: 플레이어 `#88J8Q9RPV`, 클럽 `#2U8CVLQQ9`.

## UI

- 일반 프리미티브: `src/components/ui/` (shadcn/ui).
- 도메인 시각요소: `src/components/primitives/` (Trophy, BrawlerPortrait 등).
- 토스트: `import { toast } from "sonner"`.

## 환경변수

`NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SITE_URL`. 검증은 `src/config/env.ts`.

## 설계/구현 문서

- `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md`
- `docs/superpowers/plans/2026-04-27-frontend-foundation.md`

## Design

@DESIGN.md