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
- E2E는 dev 서버의 `/proxy-api` Route Handler(`app/proxy-api/[...path]/route.ts`, `PROXY_API_TARGET` 은 playwright.config.ts 에서 설정)를 통해 prod core-api(`https://api.imstargg.com`) 호출. rewrite 가 아닌 Route Handler 인 이유는 브라우저 POST 의 `Origin` 헤더를 업스트림에 전달하지 않기 위함(prod CORS 는 imstargg.com 만 허용). 테스트 태그: `tests/e2e/fixtures/tags.ts` (플레이어 `#88J8Q9RPV`, 클럽 `#2U8CVLQQ9`).

## UI

- 일반 프리미티브: `src/components/ui/` (shadcn/ui).
- 도메인 시각요소: `src/components/primitives/` (Trophy, BrawlerPortrait 등).
- 토스트: `import { toast } from "sonner"`.

## 이미지

CDN(`cdn.imstargg.com`) 동적 자산은 `next/image` + `unoptimized` (이미 webp 라 Vercel 변환 불필요, `next.config.ts` `remotePatterns` 도 불필요). 정적 `/assets/*` 만 raw `<img>` 잔존 — 새 동적 자산은 `<Image unoptimized width height alt>` 패턴.

## 메타데이터 / OG image

`src/lib/seo.ts` 의 `buildMetadata({ dynamicOg: true })` 는 `openGraph.images` / `twitter.images` 키를 객체에서 omit. 동적 OG 카드를 적용하는 페이지는 반드시 이 옵션 + 같은 디렉토리에 `opengraph-image.tsx` / `twitter-image.tsx`. 공통 빌더는 `src/lib/og/`.

## 환경변수

`NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SITE_URL`. 검증은 `src/config/env.ts`.

## 설계/구현 문서

`docs/superpowers/specs/` (설계) 와 `docs/superpowers/plans/` (구현 계획) 에 도메인별로 보관. 신규 도메인 작업은 spec → plan → implementation 사이클을 따른다.

## Design

@DESIGN.md
