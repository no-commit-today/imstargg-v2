# 아키텍처 정합성·코드 위생 설계 (Spec B)

2026-07-07 프론트엔드 종합 리뷰에서 확인된 레이어 규칙 위반·중복·죽은 코드를 해소하고 재발을 구조적으로 방지한다. Spec A(성능, `2026-07-07-performance-design.md`)와 한 쌍이며 A 이후 순차 진행한다.

## 배경 — 확인된 문제

| ID | 문제 | 근거 위치 |
|---|---|---|
| A1 | 검색 결과 컴포넌트가 레이어 전체 우회 — `"use client"` 에서 endpoint 직접 호출, TanStack Query 없이 `useState` 수동 페칭, DTO 를 mapper 없이 렌더. "더보기" 연타 race 여지 | `app/players/search/_components/PlayerSearchResults.tsx:1-34`, `app/clubs/search/_components/ClubSearchResults.tsx:1-34`, `app/clubs/_components/ClubSearchInput.tsx:6-18` |
| A2 | 인라인 queryKey 생성 — `queryKeys.ranking.brawlers` 가 이미 존재하는데 `["brawler-ranking", ...]` 직접 생성. invalidate 에 잡히지 않는 유령 캐시 | `app/brawlers/[id]/_components/BrawlerRankingTab.tsx:20` |
| A3 | 라이브 검색 로직 2벌 — `SearchableInput` 의 수동 `setTimeout` 디바운스 + `AbortController` vs `useEventSearch` 의 TanStack 패턴 | `src/components/shared/SearchableInput.tsx:88-93` vs `src/hooks/queries/useEventSearch.ts` |
| A4/Q2 | player/club 페어 복붙 6쌍 — 검색 결과(96줄 중 1컬럼 diff), `renderResult` 바이트 단위 동일, NotFoundView 2벌, error.tsx 4벌(1줄 diff), RenewalButton/Status 서브컴포넌트 분기, 이벤트 통계 테이블 셸 | 각 파일 쌍 (리뷰 보고서 참조) |
| A5 | 정적 마크업뿐인 `HomeScreen` 이 `"use client"` | `app/_components/HomeScreen.tsx:1` |
| Q1 | `responsive.css` 의 `[style*="grid-template-columns..."]` 셀렉터 71개 중 대부분이 매칭 대상 소실(Tailwind 이관 완료) — 죽은 CSS 약 200줄 + 인라인 grid 재도입 시 오동작하는 "살아있는 함정" 규칙. DESIGN.md 는 아직 이 폐기 패턴을 지시 | `app/styles/responsive.css:29-317`, `DESIGN.md` Layout 절 |
| Q4 | 에러 처리 3원화 — 갱신 트리거 실패 무반응(`useRenewal` mutation onError 없음), 쿼리 오류가 "데이터 없음"으로 표시(`PlayerStatistics` isError 미분기), 내부 API 경로가 담긴 `error.message` 노출(`BattleList`), 루트 error.tsx/global-error.tsx 부재 | `src/hooks/useRenewal.ts:55-60`, `app/players/[tag]/_components/statistics/PlayerStatistics.tsx:50-60`, `.../battle/BattleList.tsx:33` |
| Q5 | 도달 불가 `not-found.tsx` 2개 — 페이지가 `notFound()` 대신 인라인 NotFoundView 를 반환(갱신 유도 의도)하므로 렌더 경로 없음 | `app/players/[tag]/not-found.tsx`, `app/clubs/[tag]/not-found.tsx` |
| Q6 | 미사용 파일 6개(ResultPill, useBrawlers, shadcn 4개)·미사용 export 35건·타입 21건·유령 devDependency(prettier — 설정/스크립트 없음). knip + grep 이중 확인 완료 | 리뷰 보고서 참조 |
| Q7 | `formatRelativeTime` 동명이함수 2벌 — 프로덕션은 `relativeTime.ts` 만 사용, `format.ts` 쪽은 자기 테스트만 참조(죽은 코드가 살아있는 것처럼 보임) | `src/lib/format.ts:7-22` vs `src/lib/relativeTime.ts:1-17` |
| Q8 | 상세 페이지 href 빌드 3중화 — `routes.ts` 헬퍼(3파일) vs `toUrlPath` 직접 조립(14곳, `%23` 형태) vs `tag.replace(/^#/,"")`(7곳) — 같은 페이지가 두 URL 로 링크되어 캐시 이원화 | `src/config/routes.ts`, 각 사용처 |
| Q9 | 캐시 상수 우회 매직 넘버 — `cache.ts` 에 상수가 있는데 `sitemap.ts`(86400/3600), 훅들(`staleTime: 60_000`)이 하드코딩 | `app/sitemap.ts:12,26-27`, `src/hooks/queries/usePlayer.ts:14` 등 |
| Q10 | `usePlayerStatistics` 만 DTO 를 그대로 반환해 mapper 호출이 컴포넌트로 누출 — 다른 훅들은 훅 내부에서 매핑 | `src/hooks/queries/usePlayerStatistics.ts:27-35` |
| 문서 | CLAUDE.md 규칙 문면("훅 또는 endpoint 객체 경유")이 클라이언트 컴포넌트의 endpoint 직접 호출을 막지 못함. queryKeys 규칙에 강제 수단 없음 | `CLAUDE.md` 아키텍처 절 |

## 목표 — 성공 기준 (전부 기계 검증 가능)

| # | 기준 |
|---|---|
| 1 | `"use client"` 파일에서 `@/api/endpoints` 직접 import 0건 (`src/hooks/**` 제외, grep 검증) |
| 2 | 인라인 queryKey 배열 0건 + ESLint 룰(`no-restricted-syntax`)로 재발 차단 |
| 3 | knip 기준 미사용 파일·export 0 (knip 설정에 명시한 예외만 허용) |
| 4 | `responsive.css` 죽은 셀렉터 제거(약 200줄 감소) + DESIGN.md·CLAUDE.md 서술 현행화 |
| 5 | error.tsx 4벌·NotFoundView 2벌 → 공통 1벌씩, 루트 `app/error.tsx`·`app/global-error.tsx` 존재, 사용자 노출 문구에 내부 API 경로 0건 |
| 6 | `pnpm typecheck` / `pnpm lint` / `pnpm test:run` 통과 + 주요 페이지 시각 회귀 없음 (1280×800 / 390×844) |

## 비범위 (Non-goals)

- zod 런타임 응답 검증 도입 — 관측성 없는 현 단계에서 이득 제한적, C 라운드에서 재검토
- 테스트 커버리지 캠페인 — 이 스펙의 변경에 딸린 테스트만 추가
- endpoint import 제한의 ESLint 자동화 — `_components` 에 RSC 가 섞여 있어 vanilla 룰로는 오탐. 문서 규칙으로 명시하고 자동화는 후속 과제 (워크스트림 ⑤ 참조)
- 성능 관련 변경 전반 — Spec A

## 설계

### 워크스트림 ① 검색 데이터 페칭 레이어 통일 (A1·A3)

- 신규 훅 `usePlayerSearch(query)` / `useClubSearch(query)` — `useInfiniteQuery` 기반, `queryKeys.player.search(query)`·`queryKeys.club.search(query)` 신설, domain mapper(`toPlayer`/`toClub`) 경유. 페이지 크기는 현행(20) 유지. "더보기" 동시성은 TanStack 의 fetchNextPage 큐 관리로 해소.
- `PlayerSearchResults`/`ClubSearchResults` → **kind config 주입 방식의 단일 컴포넌트**로 통합. 부가 컬럼(플레이어=클럽명, 클럽=멤버 수)만 렌더 함수로 주입하고, 그 이상의 추상화는 하지 않는다.
- `SearchableInput` 은 표시 전용으로 유지. 내부의 수동 디바운스·`AbortController` 를 제거하고 데이터 공급을 호출부 훅으로 이전: 공용 `useDebouncedValue` 훅 신설 + `useQuery`(취소·캐시·중복 제거는 TanStack 담당). `useEventSearch` 와 같은 패턴으로 수렴. `ClubSearchInput` 의 즉석 `fetchClubs` 도 이 경로로 흡수.

### 워크스트림 ② 쿼리·경계 정합 (A2·Q10·A5)

- `BrawlerRankingTab` → 신규 훅 `useBrawlerRanking(brawlerId, country)` (`src/hooks/queries/`) + 기존 `queryKeys.ranking.brawlers` 사용. 컴포넌트 내 `useQuery`·mapper 호출 제거.
- `usePlayerStatistics` — `select` 로 mapper(`toPlayerBrawlerStat`/`toPlayerBrawlerRoleStat`)를 훅 안으로 내려 반환형을 화면 모델로 통일. brawler/role 두 훅에 복붙된 seasonId 계산·enabled 로직을 공용 함수로 추출.
- `HomeScreen` 의 `"use client"` 제거 — 상호작용이 필요한 `PlayerSearchInput` 만 클라이언트 자식으로 남긴다.

### 워크스트림 ③ 에러 UX 표준화 + 중복 제거 (Q4·A4·Q5)

- 공통 `RouteError` 클라이언트 컴포넌트 신설(문구·재시도 버튼·스타일 단일화) → 기존 4벌 `error.tsx` 는 한 줄 re-export 로 축소. 루트 `app/error.tsx` + `app/global-error.tsx` 신설 — 현재 `/brawlers` 목록 등 catch 없는 페이지의 5xx 가 Next 기본 화면으로 떨어지는 문제 해소.
- `PlayerNotFoundView`/`ClubNotFoundView` → kind config 기반 공통 `NotFoundView` 1벌 (문구·링크 href·갱신 훅 kind 만 주입).
- 도달 불가한 `players/[tag]/not-found.tsx`·`clubs/[tag]/not-found.tsx` 삭제. 인라인 NotFoundView 반환이 의도된 설계(미동기 플레이어에게 갱신 유도, HTTP 200 + noindex)임을 CLAUDE.md 에 한 줄 명시.
- `useRenewal` mutation 에 `onError` 추가 → 실패 상태를 **RenewalStatus 캡션**으로 표시 (DESIGN.md "갱신 플로우는 토스트를 쓰지 않는다" 정책 준수). 표시 문구는 "갱신 실패 — 잠시 후 다시 시도" 계열의 고정 문구.
- `BattleList` 의 `(error as Error)?.message` 렌더 → 고정 문구로 교체 (TanStack v5 의 `error` 는 이미 `Error | null` 이므로 캐스팅도 제거). 내부 API 경로 노출 해소.
- `PlayerStatistics` 에 `isError` 분기 추가 — 장애와 "이 시즌 통계 없음"을 구분해 표시.
- `RenewalButton`/`RenewalStatus` 의 Player/Club 서브컴포넌트 분기 제거 — `useRenewal(kind, tag)` 가 이미 파라미터화되어 있으므로 kind prop 하나로 통합.

### 워크스트림 ④ 죽은 코드 제거 (Q1·Q6·Q7)

- `responsive.css`: 매칭 대상이 사라진 `[style*=...]` 계열 셀렉터를 전수 제거 — 범용 매칭이라 오동작 위험이 있는 함정 규칙(`repeat(4, 1fr)` 등) 우선. 유지 대상은 실제 매칭이 확인되는 `.topnav-*`·`.card-*`·battle 관련뿐. 삭제는 페이지 단위로 나눠 각각 시각 검증.
- DESIGN.md 현행화: "표 레이아웃은 inline-style grid + `[style*=...]` 변환" 서술을 Tailwind 반응형 클래스(`grid-cols-[...] sm:grid-cols-[...]`) 기준으로 교체. 삭제되는 `ResultPill` 서술 정리.
- 미사용 파일 6개 삭제: `src/components/primitives/ResultPill.tsx`, `src/hooks/queries/useBrawlers.ts`, `src/components/ui/{dropdown-menu,pagination,separator,skeleton}.tsx` (shadcn — 필요 시 CLI 재추가 가능. 단 Spec A 의 loading.tsx 가 skeleton 프리미티브를 쓰게 되면 해당 파일은 유지).
- `src/lib/format.ts` 의 `formatRelativeTime`·`formatMinutesAgo` + 대응 테스트 삭제 — `relativeTime.ts` 로 단일화. `format.ts` 에 남는 유효 함수는 유지.
- 미사용 export 35건·타입 21건 정리. `knip.json` 설정 파일 추가(shadcn 배럴 등 예외 명시)로 이후에도 `pnpm dlx knip` 재실행 가능 상태 유지.
- `prettier` devDependency 를 제거한다 — 설정 파일도 스크립트도 없는 유령 의존성. 포맷터 도입 여부는 별도 결정 사항으로 이 스펙에서 다루지 않는다.

### 워크스트림 ⑤ 컨벤션 수렴 + 규칙 강제 (Q8·Q9·문서)

- URL 빌드 통일: `routes.player(tag)`/`routes.club(tag)` 헬퍼로 전 사용처(약 21곳 — `toUrlPath` 직접 조립 14곳 + 정규식 스트립 7곳) 수렴. URL 형태는 canonical(`src/lib/seo.ts`)과 동일한 `%23` 인코딩으로 통일 — 같은 페이지가 두 URL 로 갈라지는 RSC/브라우저 캐시 이원화 해소. `lib/tag.ts` 의 `stripHash` 등 기존 유틸 활용.
- 잔여 캐시 매직 넘버 → 상수화: `sitemap.ts` 의 `86400`/`3600` → `CACHE_SECONDS.*`, 훅들의 `staleTime: 60_000` → `QUERY_STALE_TIME.SHORT`. (Spec A 가 만진 파일은 A 에서 이미 처리.)
- CLAUDE.md 레이어 규칙 문면 분리: "RSC(`page.tsx` 등 서버 컴포넌트)는 endpoint 객체 직접 호출 허용 / `"use client"` 컴포넌트는 반드시 `src/hooks/` 훅 경유" + "mapper 호출 위치는 훅 내부가 기본(RSC 는 페이지에서 호출 가능)" 가이드 명시.
- ESLint 자동 강제 (현실적 범위): `no-restricted-syntax` 로 `useQuery`/`useInfiniteQuery` 호출 인자의 인라인 queryKey 배열 리터럴을 금지하는 룰을 `eslint.config.mjs` 에 추가. endpoint import 제한은 위 문서 규칙으로 두고, 커스텀 룰("use client" 디렉티브 감지 필요)은 후속 과제로 남긴다.

## 검증 계획

1. **기계 검증**: 성공 기준 1(grep)·2(lint 룰 + grep)·3(knip)·5(파일 존재/grep)를 스크립트성 확인.
2. **테스트**: `pnpm typecheck` / `pnpm lint` / `pnpm test:run`. 신규 — 검색 훅(MSW 네트워크 경계 모킹), `useDebouncedValue`(순수 로직), 검색 결과 컴포넌트 상호작용(Testing Library, 더보기·빈 결과), RenewalStatus 실패 캡션. **리팩터링 대상의 상호작용 테스트를 먼저 작성한 뒤 재작성**(동작 보존 확인).
3. **시각 검증**: playwright MCP 로 홈/검색 결과/플레이어/브롤러/이벤트 상세, 1280×800·390×844 — 특히 responsive.css 삭제 단위마다 전후 비교.
4. **E2E 스위트 미실행** 원칙 동일 (prod 갱신 트리거 방지).

## 리스크와 완화

- **responsive.css 제거의 시각 회귀** → 셀렉터 그룹 단위로 나눠 삭제하고 각 단위마다 스크린샷 전후 비교. grep 으로 매칭 대상 부재를 삭제 전에 재확인.
- **검색 재작성의 동작 변화** → 기존 동작(디바운스 지연, 최소 글자 수, 최근 검색 저장 시점)을 테스트로 먼저 고정한 뒤 내부 교체.
- **"미사용" 판정 오류** → knip + grep 이중 확인이 끝난 목록만 삭제, 삭제 후 typecheck·프로덕션 빌드로 확증. Spec A 가 skeleton 등 일부 파일을 사용하게 되면 해당 항목은 삭제 목록에서 제외 (A 선행 후 재확인).
- **URL 형태 통일의 링크 회귀** → 단위 테스트로 `routes.*` 헬퍼 출력 고정 + 시각 검증에서 주요 링크 이동 확인.

## Spec A 와의 접점

- A 가 신설하는 통계 훅·`queryKeys.statistics.*` 는 본 스펙의 레이어 규칙을 선제 준수하도록 설계됨.
- A 의 `loading.tsx` 가 shadcn `skeleton.tsx` 를 사용하게 되면 워크스트림 ④ 의 삭제 목록에서 제외.
- 검색 컴포넌트 재작성(B)은 A 의 변경 파일과 겹치지 않아 순차 진행에 충돌 없음.
