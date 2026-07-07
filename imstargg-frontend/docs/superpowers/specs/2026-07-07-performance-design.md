# 성능 개선 설계 (Spec A)

2026-07-07 프론트엔드 종합 리뷰에서 확인된 성능 이슈를 해소한다. 별도 진행되는 Spec B(아키텍처 정합성·코드 위생)와 한 쌍이며, 이 문서는 성능 축만 다룬다.

## 배경 — 확인된 문제

리뷰 근거: 병렬 리뷰 에이전트 4개 + `pnpm build` 산출물(.next prerender-manifest, 청크 gzip 실측) 대조 + 원본 코드 재검증.

| ID | 문제 | 근거 위치 |
|---|---|---|
| P1 | `generateStaticParams` 로 빌드 시 브롤러 106개+이벤트 경로를 생성하지만, 페이지가 `searchParams` 를 읽어(통계 필터) 동적 렌더로 강등 — 산출물 전부 폐기, SEO 핵심 100+ URL 이 매 요청 SSR. `export const revalidate = 60` 은 dead config | `app/brawlers/[id]/page.tsx:60`, `app/events/[id]/page.tsx:57,60` |
| P2 | 원격 CSS `@import` 3중 체인(Google Fonts ×2, jsdelivr Pretendard)이 빌드 CSS 에 잔존 + KoBrawl 950KB 비서브셋 OTF + preconnect/preload 0개 → FCP 지연, 전면 FOUT/리플로우 | `app/globals.css:1-22`, `public/fonts/KoBrawl_Gothic40.otf` |
| P3 | 플레이어 페이지가 독립 API 3개(player→brawlers→renewal)를 순차 await — TTFB 가 왕복 3회의 합. 클럽 페이지 동일 패턴. 앱 전체 `loading.tsx` 0개 | `app/players/[tag]/page.tsx:44-54`, `app/clubs/[tag]/page.tsx:44-53` |
| P4 | recharts 정적 import 로 `/brawlers/[id]` First Load JS 297KB (recharts 청크 gzip 107KB) | `app/brawlers/[id]/_components/DailyTrendChart.tsx:3` |
| P5 | 목록 페이지들의 `cache: "no-store"` — 홈은 같은 데이터를 `revalidate: 60` 으로 캐시(정책 불일치). `/events` 는 no-store 만 제거하면 ISR 가능 | `app/events/page.tsx:23`, `app/players/page.tsx:34`, `app/clubs/page.tsx:34` |
| P6 | 플레이어 기본 탭(배틀 목록)이 하이드레이션 완료 후에야 클라이언트 fetch 시작 — 핵심 콘텐츠가 가장 늦게 등장 | `app/players/[tag]/_components/battle/BattleList.tsx:14-24` |

필터 변경 메커니즘: `StatRangeSelector` 가 `router.replace(pathname?tier=&date=)` 로 페이지 전체 RSC 재렌더를 유발한다(`app/_components/statistics/StatRangeSelector.tsx:47-57`). 이것이 P1 의 직접 원인이다.

## 목표 — 성공 기준

| # | 기준 | 현재 → 목표 |
|---|---|---|
| 1 | `pnpm build` 라우트 테이블에서 `/brawlers/[id]`·`/events/[id]` 가 ISR(revalidate 60)로 생성되고 `.next/prerender-manifest.json` 에 해당 HTML 경로가 100개 이상 존재 | 동적 ƒ·HTML 0개 → ISR ● |
| 2 | `/brawlers/[id]` First Load JS | 297KB → ≤200KB |
| 3 | 생성 HTML/CSS 에 fonts.googleapis.com·cdn.jsdelivr.net 참조 0건, KoBrawl 전송량 ≤250KB(WOFF2 서브셋) | 950KB OTF → ≤250KB |
| 4 | 통계 필터 변경 시 서버 RSC 왕복 0회 — 통계 API XHR 만 발생 | 매번 페이지 재렌더 → 클라이언트 쿼리 |
| 5 | `players/[tag]` 초기 데이터 페칭이 병렬(최대 왕복 1회 상당) + 상세 4개 세그먼트에 `loading.tsx` 존재 | 순차 3왕복·loading 없음 |
| 6 | `pnpm typecheck` / `pnpm lint` / `pnpm test:run` 통과 유지 | 현재 전부 통과 |

## 비범위 (Non-goals)

- 레이어 규칙 위반 수습, 중복/죽은 코드 제거, 에러 UX 표준화 → **Spec B**
- zod 런타임 응답 검증, 관측성(Sentry/Web Vitals), JSON-LD, 다크모드, i18n → 이번 라운드 제외 (별도 갭 목록 참조)
- 목록 페이지(`/brawlers` 등)의 완전 정적화 — 필터 구조가 달라 이번 범위에서는 fetch 캐시 정합까지만

## 설계

### 워크스트림 ① ISR 복구 (P1)

**원리**: 페이지(RSC)에서 `searchParams` 소비를 제거하면 기존 `generateStaticParams` + `export const revalidate = 60` 이 실효된다. 필터는 클라이언트 상태로 이동한다.

**brawlers/[id]**

- `page.tsx`: `searchParams` prop 과 `parseTrophyRange` 등 모든 searchParams 소비를 제거. 브롤러 상세 + **기본 필터값 통계**만 서버 페치.
- `BrawlerStatsSection`(RSC 유지): 기본 필터(`DEFAULT_TIER_RANGE`/`DEFAULT_DATE_RANGE`) 통계 3종(daily/pair/enemy)을 기존처럼 `Promise.all` + `revalidate: CACHE_SECONDS.SHORT` 로 페치 → 화면 모델로 매핑해 새 클라이언트 컴포넌트에 전달.
- 신규 `BrawlerStatsPanel`("use client"):
  - 필터 상태를 `useSearchParams()` 로 읽는다(사용처를 `<Suspense>` 로 감싸 SSG 셸과 호환).
  - 필터 변경은 `router.replace` 대신 **`window.history.replaceState`** (Next 지원 shallow routing) — 서버 왕복 없이 URL 동기화 유지.
  - 필터가 기본값이면 서버에서 받은 `initialData` 를 그대로 사용, 기본값이 아니면 신규 훅으로 클라이언트 페치.
- `StatRangeSelector`: 컴포넌트 내부의 `router.replace` 를 `window.history.replaceState` 로 교체한다 — 브롤러·이벤트 상세 두 사용처 모두 동일 동작이 필요하므로 내부 단일 수정으로 처리.
- `sp.trophy` 를 소비하던 영역(랭킹 탭 등)도 동일하게 클라이언트로 이동.

**events/[id]**

- `eventApi.get(id, { cache: "no-store" })` → `next: { revalidate: CACHE_SECONDS.SHORT }`.
- `EventStatsSection` 을 동일 패턴으로 분할(RSC 기본값 페치 → 클라이언트 패널). 브롤러 선택 토글(`toggleBrawler` — 통계 테이블의 URL 갱신)도 shallow 방식으로 전환.

**신규 데이터 레이어** (Spec B 의 레이어 규칙을 선제 준수)

- `queryKeys` 에 statistics 도메인 키를 추가한다: `queryKeys.statistics.brawler(brawlerId, tier, date)` / `queryKeys.statistics.event(eventId, filters)` (plan 단계에서 기존 키와 충돌이 확인될 때만 이름 조정).
- 신규 훅 `useBrawlerStats(brawlerId, { tier, date }, initial?)` / `useEventStats(eventId, filters, initial?)` — `src/hooks/queries/` 에 배치, `statisticsApi` 경유, 반환은 domain mapper 를 통과한 화면 모델.

**부수 정리**: 여전히 동적으로 남는 목록 페이지의 route-level `revalidate`(`app/brawlers/page.tsx:26`)는 오독 방지를 위해 제거(fetch-level 캐시는 유지). 상세 페이지의 것은 실효되므로 유지.

**트레이드오프(의도된 동작)**: 필터 딥링크(`?tier=…`) 직접 방문 시 정적 셸의 기본값 통계가 잠깐 보인 뒤 필터 결과로 교체된다. canonical 은 기본 URL(`src/lib/seo.ts`)이므로 SEO 영향 없음.

### 워크스트림 ② 서버 워터폴 제거 + 로딩 UI (P3 + P6)

**병렬화** — `players/[tag]/page.tsx`:

- player·brawlers·renewal·**배틀 첫 페이지** 4개 요청을 동시에 시작하고 `Promise.allSettled` 로 수합.
- 에러 의미론을 표로 고정한다 (회귀 방지):

| 결과 | 처리 (현행과 동일) |
|---|---|
| player 가 `NotFoundError` 로 rejected | `PlayerNotFoundView` 반환 (나머지 결과 폐기) |
| player 가 기타 사유로 rejected | 그대로 throw → 세그먼트 `error.tsx` |
| player 정상, brawlers rejected | 그 reason 을 throw → `error.tsx` (현행 동일) |
| player 정상, renewal rejected | `NotFoundError` → `emptyRenewal(tag)`, 기타 → `null` (현행 catch 로직 이관) |
| player 정상, battles rejected | `initialBattles` 를 `undefined` 로 — 클라이언트 훅이 기존처럼 자체 페치 (성능 최적화 실패가 페이지 실패가 되지 않도록) |

- `clubs/[tag]/page.tsx` 도 동일 패턴 적용 (club + renewal 등 해당 세그먼트의 독립 요청 병렬화).

**배틀 첫 페이지 프리페치** (P6):

- RSC 에서 `battleApi` 첫 페이지(현행 페이지 크기 그대로)를 페치해 `PlayerContent` 에 전달 → `usePlayerBattles`(useInfiniteQuery)의 `initialData`(`{ pages: [first], pageParams: [초기 오프셋] }`)로 주입. `initialDataUpdatedAt` 을 서버 페치 시각으로 설정해 기존 신선도 의미론 유지. 갱신(renewal) 완료 시 invalidate 흐름은 변경 없음.

**loading.tsx 신설**: `players/[tag]`, `clubs/[tag]`, `brawlers/[id]`, `events/[id]` 4개 세그먼트. DESIGN.md 의 `.skel` 시머 스타일 기반 — 히어로 블록 + 카드 행 2~3개 수준의 얕은 스켈레톤 (기존 `BattleSkeleton` 참조).

### 워크스트림 ③ recharts 지연 로딩 (P4)

- `DailyTrendChart` 를 클라이언트 통계 패널(워크스트림 ① 산출물) 안에서 `next/dynamic(() => import(...), { ssr: false, loading: 스켈레톤 })` 으로 분리.
- 차트는 뷰포트 하단 통계 카드 1장이므로 SSR 미포함 허용. gzip 기준 약 90KB 가 First Load 에서 제외될 것으로 예상 (성공 기준 2 로 실측 확인).

### 워크스트림 ④ 목록 페이지 캐싱 정책 통일 (P5)

- `app/events/page.tsx`(rotation), `app/players/page.tsx`·`app/clubs/page.tsx`(랭킹): `cache: "no-store"` → `next: { revalidate: CACHE_SECONDS.SHORT }` — 홈(`app/page.tsx`)과 동일 정책.
- `/events` 는 searchParams 를 읽지 않으므로 이 변경만으로 ISR 전환 예상 (빌드 테이블로 확인).
- 검색 페이지(`players/search`, `clubs/search`)의 `no-store` 는 임의 질의이므로 **유지**.
- 이 워크스트림이 만지는 파일 안의 하드코딩 캐시 값(`revalidate: 60` 등)은 `CACHE_SECONDS` 상수로 교체 (전면 수렴은 Spec B).

### 워크스트림 ⑤ 폰트 파이프라인 전환 (P2)

| 폰트 | 현재 | 전환 |
|---|---|---|
| Bricolage Grotesque | Google Fonts `@import` | `next/font/google` |
| JetBrains Mono | Google Fonts `@import` | `next/font/google` |
| Lilita One | 로컬 TTF `@font-face` | `next/font/google` (동일 서체, 관리 단순화) |
| Pretendard Variable | jsdelivr `@import` | npm `pretendard` 패키지의 Variable WOFF2 + `next/font/local` |
| KoBrawl Gothic | 950KB OTF `@font-face` | **서브셋 WOFF2** + `next/font/local` |

- KoBrawl 서브셋: fonttools `pyftsubset`(uv 환경)으로 **KS X 1001 완성형 한글 2,350자 + Basic Latin + 숫자·문장부호** 를 WOFF2 로 추출해 커밋. 생성 커맨드는 재현 가능하도록 리포에 기록(스크립트 또는 문서). 원본 OTF·Lilita TTF 는 삭제.
- 서브셋 밖 글리프(희귀 한글 음절이 든 플레이어 닉네임 등)는 폰트 스택 폴백(Pretendard)으로 렌더 — 디스플레이 서체와 결이 다르지만 깨지지 않는다. 의도된 트레이드오프.
- `app/globals.css`: 원격 `@import` 3줄 + 로컬 `@font-face` 2블록 제거. 디자인 토큰(`--font-*` 계열)이 `layout.tsx` 에서 주입되는 next/font CSS 변수를 참조하도록 연결 — 토큰 이름은 유지해 사용처 변경 0.
- 효과: 외부 도메인 2곳이 크리티컬 패스에서 제거, preload·fallback 메트릭(size-adjust) 자동화로 FOUT/CLS 완화.

## 검증 계획

1. **빌드 검증**: `pnpm build` 후 라우트 테이블 + `.next/prerender-manifest.json` 으로 성공 기준 1·2 확인. 생성 HTML `<head>` 에서 기준 3(외부 폰트 0건, preload 존재) 확인.
2. **동작 검증**: dev 서버에서 브롤러/이벤트 상세의 필터 변경 시 네트워크 패널로 기준 4 확인(RSC 페이로드 요청 없음). 딥링크 진입, 기본값 복귀, 뒤로가기 동작 확인.
3. **테스트**: `pnpm typecheck` / `pnpm lint` / `pnpm test:run`. 신규 유닛 테스트 — 필터 파서·shallow URL 동기화 유틸(순수 함수), 신규 훅(MSW 네트워크 경계 모킹, CLAUDE.md 테스트 원칙 준수).
4. **시각 검증**: playwright MCP 로 데스크탑 1280×800 / 모바일 390×844 스크린샷 — 브롤러 상세(차트 lazy 포함), 플레이어 상세(스켈레톤→콘텐츠), 폰트 적용 페이지 전반. issue #49 플랜의 시각 검증 절차 재사용.
5. **E2E 스위트는 실행하지 않는다** — prod API 에 실제 갱신(POST)을 트리거하기 때문. 배포 후 기존 운영 절차로 확인.

## 리스크와 완화

- **allSettled 전환의 에러 의미론 회귀** → 위 표를 구현·테스트의 명세로 사용.
- **KoBrawl 서브셋 글리프 누락** → 시각 검증 체크리스트에 "희귀 음절 닉네임 폴백" 항목 포함.
- **responsive/기존 CSS 와의 간섭** → 이 스펙은 CSS 구조를 건드리지 않음(폰트 토큰 연결만). CSS 정리는 Spec B.
- **통계 신선도** → 서버 기본값 페치가 기존과 동일한 `revalidate: 60` 이므로 체감 변화 없음. 클라이언트 훅 staleTime 은 기존 훅 관례(`QUERY_STALE_TIME.SHORT`)를 따른다.
