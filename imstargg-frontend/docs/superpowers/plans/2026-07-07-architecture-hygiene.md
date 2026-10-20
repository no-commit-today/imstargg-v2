# 아키텍처 정합성·코드 위생 (Spec B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 레이어 규칙 위반(검색 컴포넌트·인라인 queryKey)을 수습하고, 중복(에러/NotFound/검색/갱신)을 통합하며, 죽은 코드를 제거하고, 재발을 린트/문서로 막는다. 사용자 화면 동작은 보존한다.

**Architecture:** 기존 레이어(app → hooks → api, domain 매퍼)와 DESIGN.md 를 그대로 따른다. 순수 로직/훅은 단위 테스트(MSW 네트워크 경계), 시각 변경은 playwright 로 데스크탑(1280)·모바일(390) 확인. 스펙: `docs/superpowers/specs/2026-07-07-architecture-hygiene-design.md`. Spec A(성능) 완료 후 진행 — 코드베이스는 현재 타입 클린, 66 파일 / 311 테스트 통과 상태.

**Tech Stack:** Next.js 15.5 App Router, React 19, TanStack Query v5, MSW, Vitest + Testing Library, knip, ESLint(flat config).

---

## 실행 환경 (모든 task 공통)

- 기준 디렉토리: **저장소 루트**. 프론트 명령은 `pnpm --dir imstargg-frontend <script>`.
- **`test:e2e` 절대 실행 금지** — 실서비스 API 에 갱신(POST) 트리거.
- 빌드 검증 필요 시(이하 "**프로덕션 빌드**"): `cd imstargg-frontend && NEXT_PUBLIC_API_BASE_URL=https://api.imstargg.com NEXT_PUBLIC_SITE_URL=https://imstargg.com pnpm build; cd ..` (실 API GET — 안전, 수 분 소요).
- 시각 검증 dev 서버: `NEXT_PUBLIC_API_BASE_URL=/proxy-api PROXY_API_TARGET=https://api.imstargg.com NEXT_PUBLIC_SITE_URL=http://localhost:3000 pnpm --dir imstargg-frontend dev` + playwright MCP 스크린샷.
- 각 task 는 `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run` 통과를 커밋 전 확인한다(별도 명시 없어도).

## 현재 상태 근거 (탐색 완료)

- **검색 결과 페이지**(`PlayerSearchResults`/`ClubSearchResults`): `"use client"` 에서 `playerApi.search`/`clubApi.search` 직접 호출, `useState`+수동 `loadMore()`, `PlayerResponse`/`ClubResponse` DTO 직접 렌더 — TanStack Query·domain 매퍼 미경유(A1). 두 파일은 타입/API/부가 1컬럼만 다른 복붙.
- **라이브 검색**(`SearchableInput`): `fetchLiveResults` prop 주입식으로 이미 잘 추상화됨. 내부에 수동 `setTimeout` 디바운스 + `AbortController`(A3). `PlayerSearchInput`/`ClubSearchInput` 는 `fetchPlayers`/`fetchClubs`(kind·api 만 다름) + `renderPlayerResult`/`renderClubResult`(바이트 동일) 를 넘기는 근접 중복 래퍼.
- **RenewalButton/RenewalStatus**: `usePlayerRenewal`/`useClubRenewal`(각각 `useRenewal("player"/"club", tag)` 한 줄 래퍼)로 분기하느라 Player/Club 서브컴포넌트로 쪼개짐(A4). `kind` 는 안정적 prop.
- **useRenewal**(`src/hooks/useRenewal.ts:55-60`): mutation 에 `onError` 없음(Q4).
- **NotFoundView**: `PlayerNotFoundView`/`ClubNotFoundView` 가 `usePlayerRenewal`/`useClubRenewal` + `router.refresh()` 로직만 kind 별로 미러 중복.
- **not-found.tsx**(`players/[tag]`, `clubs/[tag]`): 페이지가 `notFound()` 대신 인라인 NotFoundView 반환 → **도달 불가**(Q5). (내용은 `PlayerNotFound` 정적 컴포넌트, `PlayerNotFoundView` 와 별개.)
- **error.tsx** 4벌(players/clubs/brawlers/events [.../]): 함수명만 다른 24줄 동일(Q2). 루트 `app/error.tsx`·`global-error.tsx` 없음.
- **BrawlerRankingTab**(`app/brawlers/[id]/_components/BrawlerRankingTab.tsx:20`): `queryKey: ["brawler-ranking", ...]` 인라인, `useQuery`+매퍼를 컴포넌트에서 직접. `queryKeys.ranking.brawlers(brawlerId, country)` 이미 존재(A2).
- **usePlayerStatistics**: 2개 훅이 DTO 반환, `PlayerStatistics.tsx` 가 `toPlayerBrawlerStat`/`toPlayerBrawlerRoleStat`+sort 를 컴포넌트에서 수행(Q10). seasonId/enabled 계산이 두 훅에 복붙. `PlayerStatistics` 에 `isError` 분기 없음(Q4).
- **HomeScreen**(`app/_components/HomeScreen.tsx`): `"use client"` 이나 훅·이벤트 없는 순수 프레젠테이션(자식 `PlayerSearchInput` 만 클라이언트 필요)(A5).
- **format.ts**: `formatNumber`(사용됨), `formatRelativeTime`(now:Date — **미사용**, `relativeTime.ts` 쪽이 실사용), `formatMinutesAgo`(미사용)(Q7).
- **URL 빌드 3중화**: `routes.player/club`(이미 `%23` 형태) vs `toUrlPath` 직접 vs `tag.replace(/^#/, "")`(비-%23). `lib/tag.ts` 에 `stripHash`/`toUrlPath`/`normalizeTag` 존재(Q8).
- **캐시 매직넘버**: `sitemap.ts`(86400/3600), 훅 `staleTime: 60_000`(usePlayer/usePlayerStatistics/useEventSearch 등) — `CACHE_SECONDS`/`QUERY_STALE_TIME` 우회(Q9).
- **knip(실측)**: 미사용 파일 6(ResultPill, useBrawlers, shadcn 4[dropdown-menu/pagination/separator/skeleton]). 미사용 export 중 **진짜 죽은 것**: `ServerError`(errors.ts), `formatMinutesAgo`, `SITE_INFO`(seo.ts), `FAVORITES_LIMIT`. 나머지 shadcn 배럴(Card*/Dialog*/Select*/Sheet*/Chart*/*Variants)·sort.ts 상수·DTO 타입 21건은 **의도된 라이브러리 표면/백엔드 계약 문서 → 삭제 아님, knip 무시**. devDep: `prettier` 진짜 미사용(제거), `eslint-config-next`·`pretendard` 는 **오탐**(전자는 FlatCompat, 후자는 폰트 서브셋 Python 스크립트가 빌드 시 사용 — 삭제 절대 금지, knip 무시).

## 목표 — 성공 기준 (기계 검증)

| # | 기준 | 검증 |
|---|---|---|
| 1 | `"use client"` 파일에서 `@/api/endpoints` 직접 import 0건(`src/hooks/**` 제외) | grep |
| 2 | `useQuery`/`useInfiniteQuery` 인라인 queryKey 배열 리터럴 0건 + ESLint 룰로 차단 | grep + lint |
| 3 | knip 미사용 파일 0 + `knip.json` 예외(shadcn 배럴·DTO 타입·pretendard·eslint-config-next) 명시 | `pnpm dlx knip` |
| 4 | `responsive.css` 죽은 셀렉터 제거 + DESIGN.md·AGENTS.md 현행화 | 육안 + grep |
| 5 | error.tsx 4벌·NotFoundView 2벌 → 공통 1벌, 루트 `error.tsx`/`global-error.tsx` 존재, 사용자 노출 문구에 내부 API 경로 0건 | grep |
| 6 | typecheck / lint / test:run 통과 + 주요 페이지 시각 회귀 없음(1280/390) | 명령 + playwright |

## 비범위 (Non-goals)

- zod 런타임 응답 검증, 관측성, JSON-LD, 다크모드, i18n → 이번 라운드 제외.
- shadcn 배럴 export·DTO 타입 개별 삭제 → 하지 않음(knip 무시로 처리).
- `pretendard`/`eslint-config-next` devDep 제거 → **금지**(오탐).
- endpoint import 제한의 ESLint 자동화 → `_components` 에 RSC 혼재로 오탐, 문서 규칙 + queryKey 룰만 자동화.

## File Structure

**생성**
- `src/lib/searchHistory` 는 그대로. 신규: `src/hooks/queries/useSearch.ts`(usePlayerSearch/useClubSearch), `src/hooks/queries/useBrawlerRanking.ts`, `src/hooks/useDebouncedValue.ts`
- `src/components/shared/NotFoundView.tsx`(공통), `src/components/shared/RouteError.tsx`(공통)
- `app/error.tsx`, `app/global-error.tsx`
- `app/players/search/_components/SearchResults.tsx`(kind-config 공통) — 또는 `src/components/search/SearchResults.tsx`
- `knip.json`
- 테스트: `tests/unit/hooks/useDebouncedValue.test.ts`, `tests/components/SearchResults.test.tsx`, `tests/components/NotFoundView.test.tsx`

**수정**: `SearchableInput.tsx`, `PlayerSearchInput.tsx`/`ClubSearchInput.tsx`, `BrawlerRankingTab.tsx`, `usePlayerStatistics.ts` + `PlayerStatistics.tsx`, `HomeScreen.tsx`, `useRenewal.ts`, `RenewalButton.tsx`/`RenewalStatus.tsx`, `BattleList.tsx`, `format.ts`, `sitemap.ts`, 각종 훅 staleTime, URL 사용처(~21곳), `eslint.config.mjs`, `app/styles/responsive.css`, `DESIGN.md`, `AGENTS.md`, `package.json`

**삭제**: 6개 미사용 파일, `app/players/[tag]/not-found.tsx`, `app/clubs/[tag]/not-found.tsx`, `format.ts` 의 `formatRelativeTime`/`formatMinutesAgo`, `PlayerNotFoundView.tsx`/`ClubNotFoundView.tsx`(공통으로 대체), 개별 error.tsx 본문(re-export 로 축소)

---

## Task 1: 죽은 파일·export 제거 + knip.json + prettier 제거

**Files:** 삭제 6개 + `src/api/errors.ts`, `src/lib/seo.ts`, `src/lib/favorites.ts`, `src/lib/format.ts`, `package.json`, 생성 `knip.json`

- [ ] **Step 1: 미사용 파일 6개 삭제**
```bash
cd imstargg-frontend
git rm src/components/primitives/ResultPill.tsx src/hooks/queries/useBrawlers.ts \
  src/components/ui/dropdown-menu.tsx src/components/ui/pagination.tsx \
  src/components/ui/separator.tsx src/components/ui/skeleton.tsx
cd ..
```
확인: `grep -rn "ResultPill\|useBrawlers\|dropdown-menu\|ui/pagination\|ui/separator\|ui/skeleton" imstargg-frontend/app imstargg-frontend/src` → 0건(있으면 실사용이므로 삭제 취소하고 보고).

- [ ] **Step 2: 진짜 죽은 export 제거**
- `src/api/errors.ts`: `ServerError` 클래스 export 제거(파일 내 생성만 되고 `instanceof` 체크 없음 — grep 으로 재확인 `grep -rn "ServerError" imstargg-frontend/src` 후 정의 외 참조 0이면 제거).
- `src/lib/seo.ts`: `SITE_INFO` export 제거(grep 확인).
- `src/lib/favorites.ts`: `FAVORITES_LIMIT` export 제거(내부에서 쓰면 export 만 제거하고 로컬 const 유지).
- `src/lib/format.ts`: `formatMinutesAgo` 제거(Task 2 에서 `formatRelativeTime` 도 제거하니 함께 해도 됨).

- [ ] **Step 3: prettier devDependency 제거**
```bash
pnpm --dir imstargg-frontend remove prettier
```

- [ ] **Step 4: `knip.json` 생성** (shadcn 배럴·DTO 계약 타입·오탐 devDep 무시)

`imstargg-frontend/knip.json`:
```json
{
  "$schema": "https://unpkg.com/knip@5/schema.json",
  "ignore": ["src/components/ui/**"],
  "ignoreDependencies": ["pretendard", "eslint-config-next"],
  "ignoreExportsUsedInFile": true
}
```
주석 대신 커밋 메시지에 근거를 남긴다: `src/components/ui/**` 는 shadcn 배럴(생성물), `pretendard` 는 `scripts/subset_fonts.py` 가 빌드 시 사용, `eslint-config-next` 는 FlatCompat 경유.

- [ ] **Step 5: knip 재실행 — 미사용 파일 0 확인**
```bash
pnpm --dir imstargg-frontend dlx knip --no-progress 2>&1 | sed -n '1,40p'
```
Expected: "Unused files" 섹션이 사라지거나 0. 남은 "Unused exports" 는 DTO 타입/sort 상수 등 의도된 것만(knip 이 계약 타입까지 무시하게 하려면 `ignore` 에 `src/api/types.ts` 를 추가할지 판단 — 단, 이는 실제 미사용 타입도 가려지므로 신중히. 기본은 두고 리뷰 판단).

- [ ] **Step 6: typecheck + test + 커밋**
```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
```
Expected: 0 에러, 전부 통과(삭제한 export 를 참조하는 죽은 테스트가 있으면 함께 삭제).
```bash
git add -A imstargg-frontend
git commit -m "chore(frontend): 미사용 파일·export 제거 + knip 설정 + prettier 제거"
```

---

## Task 2: `formatRelativeTime` 중복 제거

**Files:** `src/lib/format.ts`, 관련 테스트

- [ ] **Step 1: 실사용 확인**
```bash
grep -rn "from \"@/lib/format\"\|from \"@/lib/relativeTime\"" imstargg-frontend/app imstargg-frontend/src imstargg-frontend/tests
```
Expected: 프로덕션 코드의 `formatRelativeTime` import 는 전부 `@/lib/relativeTime` 경유(`RenewalStatus.tsx`, `BattleTime.tsx`). `@/lib/format` 의 `formatRelativeTime` 은 자기 테스트만 참조.

- [ ] **Step 2: `format.ts` 에서 `formatRelativeTime` 제거** (`formatNumber` 는 유지 — player/club page.tsx 가 사용). Task 1 에서 `formatMinutesAgo` 를 이미 지웠다면 `format.ts` 에는 `formatNumber` 만 남는다.

- [ ] **Step 3: 죽은 테스트 정리** — `tests/unit/lib/format.test.ts` 에서 `formatRelativeTime`/`formatMinutesAgo` 검증 블록 삭제(파일이 그것뿐이면 파일째 `git rm`). `formatNumber` 테스트가 있으면 유지.

- [ ] **Step 4: 검증 + 커밋**
```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
git add -A imstargg-frontend && git commit -m "refactor(frontend): 중복 formatRelativeTime 제거 — relativeTime 로 단일화"
```

---

## Task 3: responsive.css 죽은 셀렉터 제거 + 문서 현행화

**Files:** `app/styles/responsive.css`, `DESIGN.md`, `AGENTS.md`(있다면 프론트)

- [ ] **Step 1: 매칭 대상 부재 확인** — `responsive.css` 의 `[style*="grid-template-columns..."]` 계열 셀렉터 각각에 대해, 현재 TSX 에 대응 인라인 스타일이 없음을 grep 으로 확인한다:
```bash
grep -rn "gridTemplateColumns\|grid-template-columns" imstargg-frontend/app imstargg-frontend/src --include=*.tsx | grep "style"
```
남아있는 인라인 grid 사용처(battle 3곳 등)만 셀렉터를 유지 대상으로 표시.

- [ ] **Step 2: 셀렉터 그룹 단위 삭제** — 매칭 대상이 사라진 `[style*=...]` 규칙을 그룹별로 제거. 특히 범용 규칙(`div[style*="grid-template-columns: repeat(4, 1fr)"]` 등 인라인 재도입 시 오동작하는 "함정")을 우선 제거. 유지: 실제 매칭되는 `.topnav-*`, `.card-head`/`.card-body`, battle 관련.
- **삭제 단위마다 시각 검증**: dev 서버 + playwright 로 해당 페이지(랭킹/브롤러 메타/클럽 멤버/히어로)를 1280·390 스크린샷, 삭제 전후 비교. 회귀 없으면 다음 그룹.

- [ ] **Step 3: DESIGN.md 현행화** — "표 레이아웃은 inline-style grid-template-columns 로 정의 후 `responsive.css` 의 `[style*=...]` 로 모바일 변환" 서술(Layout 절)을 Tailwind 반응형 클래스(`grid-cols-[...] sm:grid-cols-[...]`) 기준으로 교체. 삭제된 `ResultPill` 언급 정리.

- [ ] **Step 4: 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): responsive.css 죽은 셀렉터 제거 + DESIGN 현행화"
```

---

## Task 4: 공통 `NotFoundView` + 도달 불가 not-found.tsx 삭제

**Files:** 생성 `src/components/shared/NotFoundView.tsx` + `tests/components/NotFoundView.test.tsx`; 삭제 `PlayerNotFoundView.tsx`/`ClubNotFoundView.tsx`, `app/players/[tag]/not-found.tsx`, `app/clubs/[tag]/not-found.tsx`; 수정 page.tsx ×2 import

- [ ] **Step 1: 실패 테스트** `tests/components/NotFoundView.test.tsx` — player kind 렌더 시 안내 문구 + RenewalButton(role=button "갱신") 노출, "홈으로" 링크 존재. (MSW 로 renewal GET 스텁, `renderWithProviders`.)

- [ ] **Step 2: 공통 컴포넌트** `src/components/shared/NotFoundView.tsx`:
```tsx
"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";

const LABEL: Record<RenewalKind, { title: string; noun: string }> = {
  player: { title: "플레이어를 찾을 수 없어요", noun: "플레이어" },
  club: { title: "클럽을 찾을 수 없어요", noun: "클럽" },
};

export function NotFoundView({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const router = useRouter();
  const { progressing } = useRenewal(kind, tag);
  const prev = useRef(progressing);

  useEffect(() => {
    if (prev.current && !progressing) router.refresh();
    prev.current = progressing;
  }, [progressing, router]);

  const { title, noun } = LABEL[kind];
  return (
    <main className="mx-auto flex w-full max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <h1 className="text-2xl font-semibold">{title}</h1>
      <p className="text-sm text-muted-foreground">
        <span className="num">{tag}</span> {noun} 정보가 아직 없어요. 갱신 요청을 보내면
        브롤스타즈에서 데이터를 가져와요.
      </p>
      <div className="flex items-center gap-2">
        <RenewalButton kind={kind} tag={tag} />
        <Button asChild variant="outline"><Link href="/">홈으로</Link></Button>
      </div>
      {progressing && (
        <p className="text-xs text-muted-foreground">
          갱신 중… 완료되면 자동으로 {noun} 페이지로 이동해요.
        </p>
      )}
    </main>
  );
}
```
(주의: `RenewalButton`/`RenewalStatus` 가 Task 6 에서 `useRenewal(kind)` 직접 사용으로 바뀌지만, 이 컴포넌트는 kind prop 만 넘기므로 Task 순서 무관.)

- [ ] **Step 3: 사용처 교체 + 삭제**
- `app/players/[tag]/page.tsx`: `import { PlayerNotFoundView }` → `import { NotFoundView } from "@/components/shared/NotFoundView"`, `<PlayerNotFoundView tag=.../>` → `<NotFoundView kind="player" tag=.../>`.
- `app/clubs/[tag]/page.tsx`: 동일하게 `kind="club"`.
```bash
git rm "imstargg-frontend/app/players/[tag]/_components/PlayerNotFoundView.tsx" \
       "imstargg-frontend/app/clubs/[tag]/_components/ClubNotFoundView.tsx" \
       "imstargg-frontend/app/players/[tag]/not-found.tsx" \
       "imstargg-frontend/app/clubs/[tag]/not-found.tsx"
```

- [ ] **Step 4: AGENTS.md 한 줄 추가** — 인라인 NotFoundView 반환이 의도된 설계(미동기 플레이어 갱신 유도, HTTP 200 + noindex)라 `notFound()`/`not-found.tsx` 를 쓰지 않는다는 주석.

- [ ] **Step 5: 검증 + 커밋** (typecheck/test + `grep -rn "PlayerNotFoundView\|ClubNotFoundView" imstargg-frontend` → 0)
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): NotFoundView 공통화 + 도달 불가 not-found.tsx 제거"
```

---

## Task 5: 공통 `RouteError` + 루트 error/global-error

**Files:** 생성 `src/components/shared/RouteError.tsx`, `app/error.tsx`, `app/global-error.tsx`; 수정 4개 세그먼트 error.tsx(re-export 로 축소)

- [ ] **Step 1: 공통 컴포넌트** `src/components/shared/RouteError.tsx`:
```tsx
"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export function RouteError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">문제가 발생했어요</h1>
      <p className="text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
      <Button onClick={reset}>다시 시도</Button>
    </main>
  );
}
```

- [ ] **Step 2: 4개 세그먼트 error.tsx 를 re-export 로 축소** — 각 `app/{players/[tag],clubs/[tag],brawlers/[id],events/[id]}/error.tsx` 전체를:
```tsx
"use client";
export { RouteError as default } from "@/components/shared/RouteError";
```
(Next 는 `error.tsx` 의 default export 가 `{error, reset}` 를 받는 클라이언트 컴포넌트이길 요구 — re-export 로 충족.)

- [ ] **Step 3: 루트 `app/error.tsx`** — 동일하게 `export { RouteError as default } ...` (현재 `/brawlers` 목록 등 catch 없는 페이지의 5xx 를 잡는다).

- [ ] **Step 4: 루트 `app/global-error.tsx`** — global-error 는 자체 `<html><body>` 를 렌더해야 한다:
```tsx
"use client";

import { useEffect } from "react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <html lang="ko">
      <body style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 16, padding: "96px 16px", fontFamily: "system-ui, sans-serif" }}>
        <h1 style={{ fontSize: 24, fontWeight: 600 }}>문제가 발생했어요</h1>
        <p style={{ color: "#5C6271" }}>잠시 후 다시 시도해 주세요.</p>
        <button onClick={reset} style={{ padding: "8px 16px", borderRadius: 8, border: "1px solid #D8DCE5" }}>다시 시도</button>
      </body>
    </html>
  );
}
```

- [ ] **Step 5: 검증 + 커밋** (typecheck/test; **프로덕션 빌드**로 error 경계가 빌드 통과하는지 확인 — Task 5·6·7·9 를 묶어 한 번만 빌드해도 됨)
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): RouteError 공통화 + 루트 error/global-error 추가"
```

---

## Task 6: 갱신 컴포넌트 통합 + 에러 피드백 + 화면 에러 표준화

**Files:** `src/hooks/useRenewal.ts`, `RenewalButton.tsx`, `RenewalStatus.tsx`, `app/players/[tag]/_components/battle/BattleList.tsx`, `PlayerStatistics.tsx`

- [ ] **Step 1: `useRenewal` 에 실패 상태 노출** — `useMutation` 은 `isError` 를 자동 추적하므로 명시적 `onError` 는 불필요하다. 기존 mutation(`useRenewal.ts:55-60`)은 그대로 두고, 반환 객체(`return {...}`, 62-69행)에 `triggerFailed: trigger.isError` 한 줄만 추가한다. (실패를 사용자에게 알리는 것은 Step 3 의 RenewalStatus 캡션이 담당 — DESIGN.md "갱신 플로우는 토스트 금지" 준수.)

- [ ] **Step 2: RenewalButton 서브컴포넌트 제거** `src/components/shared/RenewalButton.tsx` 전체:
```tsx
"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";

const ARIA: Record<RenewalKind, string> = {
  player: "플레이어 정보 갱신",
  club: "클럽 정보 갱신",
};

export function RenewalButton({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const { progressing, canRenew, trigger, triggering } = useRenewal(kind, tag);
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button type="button" variant="default" size="sm" disabled={disabled} onClick={trigger} aria-label={ARIA[kind]}>
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}
```
(호출부는 이미 `<RenewalButton kind=... tag=... />` 형태라 무변경 — Props 가 유니온에서 객체로 바뀌어도 동일 호출.)

- [ ] **Step 3: RenewalStatus 서브컴포넌트 제거 + 실패 캡션** `src/components/shared/RenewalStatus.tsx` 전체:
```tsx
"use client";

import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";
import { formatRelativeTime } from "@/lib/relativeTime";

export function RenewalStatus({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const { renewedAt, progressing, triggerFailed } = useRenewal(kind, tag);
  const text = (() => {
    if (triggerFailed) return "갱신 실패 — 잠시 후 다시 시도해 주세요";
    if (progressing) return "최근 업데이트: 진행 중…";
    if (!renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewedAt)}`;
  })();
  return <span className="text-sm text-[#5C6271]">{text}</span>;
}
```
(DESIGN.md "갱신 플로우는 토스트 금지" 준수 — 캡션으로만 표시.)

- [ ] **Step 4: BattleList 내부 경로 노출 제거** `BattleList.tsx:33` 의 `{(error as Error)?.message ?? ""}` 를 삭제하거나 고정 문구로. TanStack v5 `error` 는 이미 `Error | null` 이니 캐스팅도 제거. 예: 해당 `<p>`(내부 메시지) 줄 자체를 제거하고 상단 "배틀 기록을 불러오지 못했어요." 문구만 유지.

- [ ] **Step 5: PlayerStatistics isError 분기** — `PlayerStatistics.tsx` 에서 `brawlerQuery.isError || roleQuery.isError` 일 때 "통계를 불러오지 못했어요" 안내를 렌더(빈 데이터 "이 시즌 통계 없음" 과 구분). `brawlerItems`/`roleItems` 계산은 유지하되, 반환 JSX 상단에 에러 분기 추가.

- [ ] **Step 6: 검증 + 커밋** — 기존 renewal 통합 테스트(`tests/integration/player|club`)가 통과해야 한다(시그니처 보존). 실패 캡션은 컴포넌트 테스트로 추가(선택).
```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
git add -A imstargg-frontend && git commit -m "refactor(frontend): 갱신 컴포넌트 통합 + 실패 피드백 + 화면 에러 표준화"
```

---

## Task 7: 검색 결과 페이지 레이어 통일 (useInfiniteQuery + 매퍼)

**Files:** 생성 `src/hooks/queries/useSearch.ts`, 공통 `SearchResults` 컴포넌트, `tests/components/SearchResults.test.tsx`; 수정 `queryKeys.ts`, `players/search/page.tsx`·`clubs/search/page.tsx`(결과 컴포넌트 교체); 삭제 `PlayerSearchResults.tsx`/`ClubSearchResults.tsx`

- [ ] **Step 1: queryKeys 추가** — `queryKeys.player.search(query)`, `queryKeys.club.search(query)` (기존 `queryKeys.ts` 관례대로).

- [ ] **Step 2: 검색 훅** `src/hooks/queries/useSearch.ts` — `usePlayerSearch(query)`/`useClubSearch(query)` (`useInfiniteQuery`, offset 페이지네이션, `toPlayer`/`toClub` 매퍼 경유, `getNextPageParam` 은 `hasNext` 기반). 페이지 크기 20 유지.

- [ ] **Step 3: 공통 결과 컴포넌트** — kind 설정(제목·부가 컬럼 렌더·상세 href·addRecent kind) 주입식 `SearchResults`. player=클럽명, club=멤버수 컬럼만 렌더 함수로 분기. "더보기" 는 `fetchNextPage`/`hasNextPage`/`isFetchingNextPage` 로(수동 offset·race 제거).

- [ ] **Step 4: 테스트** `tests/components/SearchResults.test.tsx` — MSW 로 검색 API 스텁, 초기 결과 렌더 + "더보기" 클릭 시 다음 페이지 추가 + 빈 결과 문구. DTO `name` 은 평문 string(PlayerResponse/ClubResponse 의 name 타입 확인 — MessagesResponse 가 아니라 string 이면 그대로).

- [ ] **Step 5: 페이지 배선 교체 + 삭제** — `players/search/page.tsx`·`clubs/search/page.tsx` 가 `SearchResults kind=...` 를 렌더하도록. RSC 는 첫 페이지를 `initialData` 로 넘긴다(기존 `initial` prop 흐름 유지). `git rm` 두 결과 컴포넌트.

- [ ] **Step 6: 검증 + 커밋** — grep 성공 기준 1(`"use client"` 에서 endpoint 직접 import 0). 
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): 검색 결과를 useInfiniteQuery+매퍼 레이어로 통일"
```

---

## Task 8: 라이브 검색 디바운스 훅화 + 입력 래퍼 중복 제거

**Files:** 생성 `src/hooks/useDebouncedValue.ts` + 테스트; 수정 `SearchableInput.tsx`, `PlayerSearchInput.tsx`/`ClubSearchInput.tsx`

- [ ] **Step 1: `useDebouncedValue`** (순수 로직, 단위 테스트) — `useDebouncedValue(value, ms)` 반환 디바운스 값.

- [ ] **Step 2: SearchableInput 데이터 공급을 훅으로** — 내부 `setTimeout`+`AbortController`(57, 80-94행) 제거. `fetchLiveResults` prop 대신 `useQuery`(호출부가 kind 별 queryFn 주입) + `useDebouncedValue` 로 라이브 결과 조회. 취소·중복 제거는 TanStack 담당. `useEventSearch` 패턴과 수렴.

- [ ] **Step 3: 입력 래퍼 dedupe** — `renderPlayerResult`/`renderClubResult` 가 바이트 동일하니 공통 `renderSearchResult` 로. `PlayerSearchInput`/`ClubSearchInput` 는 kind·label·href·live 훅만 다른 얇은 설정으로 축소.

- [ ] **Step 4: 검증(기존 `tests/components/PlayerSearchInput.test.tsx` 통과) + 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): 라이브 검색 디바운스를 훅으로 + 입력 래퍼 중복 제거"
```

---

## Task 9: BrawlerRankingTab 인라인 queryKey 제거

**Files:** 생성 `src/hooks/queries/useBrawlerRanking.ts`; 수정 `BrawlerRankingTab.tsx`

- [ ] **Step 1: 훅** `useBrawlerRanking(brawlerId, country)` — `queryKeys.ranking.brawlers(brawlerId, country)` + `rankingApi.brawlers` + `toBrawlerRanking` 매퍼. (기존 인라인 `cache: "no-store"` 는 `staleTime: QUERY_STALE_TIME.SHORT` 로 대체.)

- [ ] **Step 2: BrawlerRankingTab 교체** — 컴포넌트 내 `useQuery`+매퍼 제거, `const { data, isLoading } = useBrawlerRanking(brawlerId, country)`.

- [ ] **Step 3: 검증 + 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): BrawlerRankingTab 을 useBrawlerRanking 훅+queryKeys 로 정합"
```

---

## Task 10: usePlayerStatistics select 매핑 + HomeScreen 서버화

**Files:** `usePlayerStatistics.ts`, `PlayerStatistics.tsx`, `HomeScreen.tsx`

- [ ] **Step 1: seasonId 계산 공용화** — `usePlayerStatistics.ts` 의 두 훅에 복붙된 seasonId/enabled 계산을 파일 내 `resolveSeasonId(battleType, brawlPassSeasonNumber, soloRankSeasonKey)` 헬퍼로 추출.

- [ ] **Step 2: select 로 매핑 이관** — `usePlayerBrawlerStatistics` 에 `select: (dto) => dto.items.map(toPlayerBrawlerStat).sort((a,b)=>b.winRate-a.winRate)`, `usePlayerBrawlerRoleStatistics` 에 `select: (dto) => dto.items.map(toPlayerBrawlerRoleStat)`. `PlayerStatistics.tsx` 의 `useMemo` 매핑/정렬 제거, `brawlerQuery.data ?? []` 를 직접 사용.

- [ ] **Step 3: HomeScreen 서버화** — `app/_components/HomeScreen.tsx` 최상단 `"use client"` 제거(순수 프레젠테이션; 자식 `PlayerSearchInput` 이 이미 클라이언트). `next/image`·인라인 스타일은 서버 호환.

- [ ] **Step 4: 검증(홈·통계 탭 시각 확인) + 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): 통계 매핑을 훅 select 로 이관 + HomeScreen 서버 컴포넌트화"
```

---

## Task 11: URL 빌드 통일 (routes.player/club)

**Files:** 다수 사용처(~21곳), 필요 시 `src/config/routes.ts`

- [ ] **Step 1: 사용처 목록화**
```bash
grep -rn "tag.replace(/^#/" imstargg-frontend/app imstargg-frontend/src
grep -rn "/players/\${\|/clubs/\${\|toUrlPath(" imstargg-frontend/app imstargg-frontend/src
```
- [ ] **Step 2: `routes.player(tag)`/`routes.club(tag)` 로 수렴** — `tag.replace(/^#/, "")`(비-%23)·인라인 `toUrlPath` 조립을 전부 `routes.player`/`routes.club`(=`%23` 인코딩, canonical 과 일치)으로 교체. `SearchableInput`·`SearchResults`·`RenewalCard` 등. history/공유 URL 이 canonical 과 일치하게 됨.
- [ ] **Step 3: 단위 테스트** — `routes.player`/`routes.club` 출력 고정(예: `#88J8Q9RPV` → `/players/%2388J8Q9RPV`).
- [ ] **Step 4: 시각 검증(주요 링크 이동 확인) + 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): 상세 URL 빌드를 routes 헬퍼(%23)로 통일"
```

---

## Task 12: 캐시 매직넘버 상수화

**Files:** `app/sitemap.ts`, `usePlayer.ts`·`useClub.ts`·`usePlayerBrawlers.ts`·`useEventSearch.ts`·`usePlayerStatistics.ts` 등 `staleTime` 하드코딩 훅

- [ ] **Step 1: sitemap** — `86400`→`CACHE_SECONDS.SEO_REFERENCE`, `3600`→`CACHE_SECONDS.BRAWLER_REFERENCE`(의미 일치 확인 후).
- [ ] **Step 2: 훅 staleTime** — `staleTime: 60_000` → `QUERY_STALE_TIME.SHORT` (Spec A 가 만진 훅은 이미 처리됨 — 남은 것만).
- [ ] **Step 3: 검증 + 커밋**
```bash
git add -A imstargg-frontend && git commit -m "refactor(frontend): 캐시 매직넘버를 CACHE_SECONDS/QUERY_STALE_TIME 상수로 통일"
```

---

## Task 13: 규칙 강제 (AGENTS.md + ESLint 인라인 queryKey 금지)

**Files:** `imstargg-frontend/AGENTS.md`, `eslint.config.mjs`

- [ ] **Step 1: AGENTS.md 레이어 규칙 분리 명시** — "RSC(page.tsx 등 서버 컴포넌트)는 endpoint 객체 직접 호출 허용 / `"use client"` 컴포넌트는 반드시 `src/hooks/` 훅 경유" + "mapper 호출 위치는 훅 내부가 기본" 가이드 추가.

- [ ] **Step 2: ESLint `no-restricted-syntax` 룰** — `eslint.config.mjs` 의 `defineConfig([...])` 에 규칙 객체 추가:
```js
  {
    files: ["**/*.ts", "**/*.tsx"],
    rules: {
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "CallExpression[callee.name=/^use(Query|InfiniteQuery)$/] Property[key.name='queryKey'] > ArrayExpression",
          message:
            "queryKey 는 queryKeys.* 로만 생성하세요 (src/hooks/queries/queryKeys.ts). 인라인 배열 금지.",
        },
      ],
    },
  },
```
(셀렉터가 실제 AST 와 맞는지 `pnpm lint` 로 확인 — 인라인 queryKey 가 이미 0건이어야 통과. Task 9 이후 인라인은 없어야 함. 셀렉터가 오탐/미탐이면 `useQuery({queryKey: [...]})` 만 잡도록 조정.)

- [ ] **Step 3: 검증** — `pnpm --dir imstargg-frontend lint` 통과(인라인 queryKey 0건이라 새 룰이 아무것도 안 잡아야 정상). 일부러 인라인 배열을 임시로 넣어 룰이 잡는지 1회 확인 후 되돌린다.

- [ ] **Step 4: 커밋**
```bash
git add -A imstargg-frontend && git commit -m "chore(frontend): 레이어 규칙 문서 분리 + 인라인 queryKey 금지 ESLint 룰"
```

---

## Task 14: 최종 검증 (성공 기준 전체)

- [ ] **Step 1: 기계 검증**
```bash
# 기준 1: "use client" 에서 endpoint 직접 import 0 (hooks 제외)
grep -rln '"use client"' imstargg-frontend/app imstargg-frontend/src | xargs grep -l "@/api/endpoints" | grep -v "/hooks/" || echo "OK: 0"
# 기준 2: 인라인 queryKey 0
grep -rn "queryKey: \[" imstargg-frontend/app imstargg-frontend/src | grep -v queryKeys || echo "OK: 0"
# 기준 3: knip 미사용 파일 0
pnpm --dir imstargg-frontend dlx knip --no-progress 2>&1 | sed -n '1,20p'
# 기준 5: 내부 API 경로 노출 0 (BattleList 등)
grep -rn "error as Error)?.message\|error.message" imstargg-frontend/app || echo "OK"
```
- [ ] **Step 2: typecheck / lint / test:run 전부 통과.**
- [ ] **Step 3: 프로덕션 빌드** — 라우트 테이블에 회귀 없음(브롤러/이벤트 여전히 ● ISR, 검색·랭킹 ƒ). error/global-error 빌드 통과.
- [ ] **Step 4: 시각 검증(playwright 1280/390)** — 홈(HomeScreen 서버화), 검색 결과(더보기), 플레이어/클럽 상세(NotFoundView·RouteError·갱신 실패 캡션 경로), 랭킹(responsive.css 정리 후 회귀 없음), 브롤러/이벤트 상세.
- [ ] **Step 5: 성공 기준표 대조 후 보고.**

---

## 리스크와 완화

- **responsive.css 시각 회귀** → 그룹 단위 삭제 + 각 단위 스크린샷 전후 비교, 매칭 부재를 grep 으로 선확인.
- **검색 재작성 동작 변화**(디바운스 지연·최소 글자수·최근검색 저장 시점) → 기존 동작을 테스트로 먼저 고정 후 내부 교체.
- **error.tsx re-export** → Next 가 `error.tsx` default 를 클라이언트 컴포넌트로 요구하므로 `"use client"` 지시어를 각 re-export 파일 최상단에 유지.
- **knip 오탐**(pretendard/eslint-config-next) → 삭제 금지, `knip.json` ignore 로만 처리.
- **URL 형태 변경**(비-%23 → %23) → canonical 과 일치시키는 방향이라 SEO 개선. 단위 테스트로 출력 고정.

## Spec A 와의 접점

- Spec A 가 신설한 통계 훅·queryKeys 는 이미 본 스펙 규칙 준수. Task 13 의 ESLint 룰은 Spec A 산출물에도 적용되어 인라인 queryKey 0 을 유지.
- Task 1 의 knip.json 은 Spec A 가 추가한 `pretendard`(폰트 스크립트용)를 반드시 무시해야 한다.
