# 성능 개선 (Spec A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 브롤러/이벤트 상세의 ISR 복구, 플레이어/클럽 페이지 워터폴 제거, recharts 지연 로딩, 목록 캐싱 통일, next/font 전환으로 TTFB·FCP·번들을 구조적으로 개선한다.

**Architecture:** 페이지(RSC)는 기본 필터 데이터만 페치해 정적 생성을 유지하고, 필터 상호작용은 클라이언트 패널(useState + history.replaceState + TanStack Query)로 이동한다. 레이어 규칙(app → hooks → api, domain mapper) 준수. 스펙: `docs/superpowers/specs/2026-07-07-performance-design.md`.

**Tech Stack:** Next.js 15.5 App Router(RSC/ISR), React 19, TanStack Query v5, MSW(테스트), fonttools(폰트 서브셋, uv 실행), Vitest + Testing Library.

---

## 실행 환경 (모든 task 공통)

- 기준 디렉토리: **저장소 루트**. 프론트 명령은 `pnpm --dir imstargg-frontend <script>` 형태.
- 의존성은 설치돼 있다 (`pnpm --dir imstargg-frontend install --frozen-lockfile` 완료 상태).
- **`pnpm --dir imstargg-frontend test:e2e` 절대 실행 금지** — 실서비스 API 에 갱신(POST)을 트리거한다.
- 빌드 검증 명령 (여러 task 에서 반복 사용, 이하 "**프로덕션 빌드**"라 지칭):
  ```bash
  cd imstargg-frontend && NEXT_PUBLIC_API_BASE_URL=https://api.imstargg.com NEXT_PUBLIC_SITE_URL=https://imstargg.com pnpm build; cd ..
  ```
  빌드는 실 API 를 GET 호출한다(읽기 전용이라 안전). 출력의 라우트 테이블을 확인한다.
- 시각 검증용 dev 서버 (클라이언트 fetch 가 CORS 를 우회하도록 기존 `/proxy-api` rewrite 사용):
  ```bash
  NEXT_PUBLIC_API_BASE_URL=/proxy-api PROXY_API_TARGET=https://api.imstargg.com NEXT_PUBLIC_SITE_URL=http://localhost:3000 pnpm --dir imstargg-frontend dev
  ```
  playwright MCP 로 1280×800(데스크탑) / 390×844(모바일) 스크린샷을 찍어 육안 확인한다.

## File Structure

**생성**
- `src/lib/shallowSearch.ts` — URL 쿼리 병합(순수) + `history.replaceState` 적용 헬퍼
- `src/hooks/queries/useBrawlerStats.ts` — `StatsFilter` 타입 + 브롤러 통계 3종 클라이언트 훅
- `src/hooks/queries/useItemOwnership.ts` — 아이템 소유율 클라이언트 훅
- `src/hooks/queries/useEventStats.ts` — 이벤트 통계(결과형/순위형/선택 브롤러) 클라이언트 훅
- `app/brawlers/[id]/_components/BrawlerStatsPanel.tsx` — 브롤러 통계 클라이언트 패널 (recharts dynamic 포함)
- `app/events/[id]/_components/EventStatsPanel.tsx` — 이벤트 통계 클라이언트 패널
- `app/events/[id]/_components/EventSelectedBrawlerPanel.tsx` — 선택 브롤러 분석 (클라이언트, 기존 RSC 대체)
- `src/components/shared/PageSkeleton.tsx` + `app/{players/[tag],clubs/[tag],brawlers/[id],events/[id]}/loading.tsx`
- `scripts/subset_fonts.py` + `app/fonts.ts` + `app/fonts/KoBrawlGothic-subset.woff2` + `app/fonts/PretendardVariable-subset.woff2`
- 테스트: `tests/unit/lib/shallowSearch.test.ts`, `tests/components/BrawlerStatsPanel.test.tsx`, `tests/components/EventStatsPanel.test.tsx`, `tests/components/BattleListInitialData.test.tsx`

**수정**
- `app/_components/statistics/StatRangeSelector.tsx` — View/래퍼 분리
- `app/_components/statistics/TrophyRangeSelector.tsx` — controlled 전환
- `app/events/[id]/_components/{EventResultStatsTable,EventRankStatsTable}.tsx` — `onToggleBrawler` prop 전환
- `app/brawlers/[id]/page.tsx`, `app/brawlers/[id]/_components/{BrawlerContent,BrawlerItems}.tsx` — 정적화 + 클라이언트 필터
- `app/events/[id]/page.tsx`, `app/events/[id]/_components/EventStatsSection.tsx` — 정적화(no-store 제거)
- `app/players/[tag]/page.tsx`, `app/clubs/[tag]/page.tsx` — `Promise.allSettled` 병렬화
- `src/hooks/queries/usePlayerBattles.ts`, `app/players/[tag]/_components/{PlayerContent,PlayerBattles}.tsx`, `.../battle/BattleList.tsx` — 배틀 첫 페이지 프리페치
- `app/events/page.tsx`, `app/players/page.tsx`, `app/clubs/page.tsx` — no-store → revalidate
- `app/brawlers/page.tsx` — dead `export const revalidate` 제거
- `app/globals.css`, `app/layout.tsx`, `package.json` — 폰트 전환
- 테스트 갱신: `tests/components/EventStatsTables.test.tsx`, `tests/unit/components/statistics/StatRangeSelector.test.tsx`

**삭제/이동**
- 삭제: `app/brawlers/[id]/_components/BrawlerStatsSection.tsx`(페이지로 흡수), `app/events/[id]/_components/BrawlerSelectedPanel.tsx`(클라이언트 대체), `public/fonts/lilitaone-regular-webfont__2_.ttf`
- 이동: `public/fonts/KoBrawl_Gothic40.otf` → `scripts/fonts-src/KoBrawl_Gothic40.otf` (재생성용 보존)

---

### Task 1: shallowSearch 유틸

**Files:**
- Create: `imstargg-frontend/src/lib/shallowSearch.ts`
- Test: `imstargg-frontend/tests/unit/lib/shallowSearch.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/unit/lib/shallowSearch.test.ts`:
```ts
import { describe, expect, it } from "vitest";
import { mergeSearchParams } from "@/lib/shallowSearch";

describe("mergeSearchParams", () => {
  it("값을 설정하면 ?key=value 문자열을 만든다", () => {
    expect(mergeSearchParams("", { tier: "TROPHY_2000_PLUS" })).toBe(
      "?tier=TROPHY_2000_PLUS",
    );
  });

  it("null 이면 해당 키를 제거한다", () => {
    expect(mergeSearchParams("?tier=TROPHY_2000_PLUS&date=LAST_WEEK", { tier: null })).toBe(
      "?date=LAST_WEEK",
    );
  });

  it("모든 키가 제거되면 빈 문자열을 반환한다 (물음표 없음)", () => {
    expect(mergeSearchParams("?tier=TROPHY_2000_PLUS", { tier: null })).toBe("");
  });

  it("관련 없는 기존 키는 보존한다", () => {
    expect(mergeSearchParams("?country=KR", { tier: "TROPHY_3000_PLUS" })).toBe(
      "?country=KR&tier=TROPHY_3000_PLUS",
    );
  });
});
```

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/unit/lib/shallowSearch.test.ts
```
Expected: FAIL — `Cannot find module '@/lib/shallowSearch'` 류의 모듈 없음 에러

- [ ] **Step 3: 구현**

`imstargg-frontend/src/lib/shallowSearch.ts`:
```ts
export function mergeSearchParams(
  current: string,
  updates: Record<string, string | null>,
): string {
  const sp = new URLSearchParams(current);
  for (const [key, value] of Object.entries(updates)) {
    if (value === null) sp.delete(key);
    else sp.set(key, value);
  }
  const qs = sp.toString();
  return qs ? `?${qs}` : "";
}

/**
 * 서버 왕복 없이 현재 URL 의 쿼리스트링만 갱신한다 (Next shallow routing).
 * 정적(ISR) 상세 페이지의 필터 상태 영속화 전용 — RSC 재렌더를 유발하지 않는다.
 */
export function applySearchShallow(updates: Record<string, string | null>): void {
  const qs = mergeSearchParams(window.location.search, updates);
  window.history.replaceState(null, "", `${window.location.pathname}${qs}`);
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/unit/lib/shallowSearch.test.ts
```
Expected: PASS (4 tests)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/src/lib/shallowSearch.ts imstargg-frontend/tests/unit/lib/shallowSearch.test.ts
git commit -m "feat(frontend): shallow URL 쿼리 갱신 유틸 추가"
```

---

### Task 2: 통계 클라이언트 훅 3파일

**Files:**
- Create: `imstargg-frontend/src/hooks/queries/useBrawlerStats.ts`
- Create: `imstargg-frontend/src/hooks/queries/useItemOwnership.ts`
- Create: `imstargg-frontend/src/hooks/queries/useEventStats.ts`

쿼리 키는 기존 `queryKeys.statistics.*` 를 사용한다(신설 금지 — `src/hooks/queries/queryKeys.ts:57-73` 에 이미 정의됨). 훅 자체의 동작 테스트는 Task 5·7 의 패널 컴포넌트 테스트(MSW)가 커버한다.

- [ ] **Step 1: useBrawlerStats 작성**

`imstargg-frontend/src/hooks/queries/useBrawlerStats.ts`:
```ts
"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  type StatsDateRange,
  type StatsTierRange,
} from "@/domain/statistics/enums";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type {
  DailyResultStat,
  EnemyResultStat,
  PairResultStat,
} from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export interface StatsFilter {
  tier: StatsTierRange;
  date: StatsDateRange;
}

export const DEFAULT_STATS_FILTER: StatsFilter = {
  tier: DEFAULT_TIER_RANGE,
  date: DEFAULT_DATE_RANGE,
};

export function isDefaultStatsFilter(filter: StatsFilter): boolean {
  return filter.tier === DEFAULT_TIER_RANGE && filter.date === DEFAULT_DATE_RANGE;
}

export interface BrawlerStatsInitial {
  daily: DailyResultStat[];
  pair: PairResultStat[];
  enemy: EnemyResultStat[];
}

export function useBrawlerStats(
  brawlerId: number,
  filter: StatsFilter,
  initial?: BrawlerStatsInitial,
) {
  const params = { brawlerId, tierRange: filter.tier, dateRange: filter.date };
  const useInitial = isDefaultStatsFilter(filter) ? initial : undefined;
  const common = {
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  } as const;

  const daily = useQuery({
    queryKey: queryKeys.statistics.daily(params),
    queryFn: ({ signal }) =>
      statisticsApi.daily(params, { signal }).then((r) => r.content.map(toDailyResultStat)),
    initialData: useInitial?.daily,
    ...common,
  });
  const pair = useQuery({
    queryKey: queryKeys.statistics.pairResult(params),
    queryFn: ({ signal }) =>
      statisticsApi.pairResult(params, { signal }).then((r) => r.content.map(toPairResultStat)),
    initialData: useInitial?.pair,
    ...common,
  });
  const enemy = useQuery({
    queryKey: queryKeys.statistics.enemyResult(params),
    queryFn: ({ signal }) =>
      statisticsApi
        .enemyResult(params, { signal })
        .then((r) => r.content.map(toEnemyResultStat)),
    initialData: useInitial?.enemy,
    ...common,
  });

  return { daily, pair, enemy };
}
```

- [ ] **Step 2: useItemOwnership 작성**

`imstargg-frontend/src/hooks/queries/useItemOwnership.ts`:
```ts
"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  DEFAULT_TROPHY_RANGE,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import { toItemOwnership } from "@/domain/statistics/mappers";
import type { ItemOwnership } from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useItemOwnership(
  brawlerId: number,
  trophy: BrawlerTrophyRange,
  initial?: ItemOwnership,
) {
  const params = { brawlerId, trophyRange: trophy };
  return useQuery({
    queryKey: queryKeys.statistics.itemOwnership(params),
    queryFn: ({ signal }) =>
      statisticsApi.itemOwnership(params, { signal }).then(toItemOwnership),
    initialData: trophy === DEFAULT_TROPHY_RANGE ? initial : undefined,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}
```

- [ ] **Step 3: useEventStats 작성**

`imstargg-frontend/src/hooks/queries/useEventStats.ts`:
```ts
"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/api/endpoints/statistics";
import type { BattleEventModeResultType } from "@/api/types";
import { QUERY_STALE_TIME } from "@/config/cache";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
  toEnemyResultStat,
  toPairRankStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type {
  BrawlerRankStat,
  BrawlerResultStatistics,
} from "@/domain/statistics/types";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { isDefaultStatsFilter, type StatsFilter } from "./useBrawlerStats";

export function useEventResultStats(
  eventId: number,
  filter: StatsFilter,
  initial?: BrawlerResultStatistics,
  enabled = true,
) {
  const params = { eventId, tierRange: filter.tier, dateRange: filter.date };
  return useQuery({
    queryKey: queryKeys.statistics.brawlerResult(params),
    queryFn: ({ signal }) =>
      statisticsApi.brawlerResult(params, { signal }).then(toBrawlerResultStatistics),
    initialData: isDefaultStatsFilter(filter) ? initial : undefined,
    enabled,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}

export function useEventRankStats(
  eventId: number,
  filter: StatsFilter,
  initial?: BrawlerRankStat[],
  enabled = true,
) {
  const params = { eventId, tierRange: filter.tier, dateRange: filter.date };
  return useQuery({
    queryKey: queryKeys.statistics.brawlerRank(params),
    queryFn: ({ signal }) =>
      statisticsApi
        .brawlerRank(params, { signal })
        .then((r) => r.content.map(toBrawlerRankStat)),
    initialData: isDefaultStatsFilter(filter) ? initial : undefined,
    enabled,
    staleTime: QUERY_STALE_TIME.SHORT,
    placeholderData: keepPreviousData,
  });
}

export function useEventSelectedStats(
  eventId: number,
  brawlerId: number | null,
  resultType: BattleEventModeResultType,
  filter: StatsFilter,
) {
  const selected = brawlerId !== null;
  const base = { eventId, tierRange: filter.tier, dateRange: filter.date };
  const common = { staleTime: QUERY_STALE_TIME.SHORT } as const;

  // enabled 가드로 brawlerId=null 인 동안 queryFn 이 실행되지 않는다.
  const pairResult = useQuery({
    queryKey: queryKeys.statistics.pairResult({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .pairResult({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toPairResultStat)),
    enabled: selected && resultType === "RESULT",
    ...common,
  });
  const enemyResult = useQuery({
    queryKey: queryKeys.statistics.enemyResult({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .enemyResult({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toEnemyResultStat)),
    enabled: selected && resultType === "RESULT",
    ...common,
  });
  const pairRank = useQuery({
    queryKey: queryKeys.statistics.pairRank({ ...base, brawlerId }),
    queryFn: ({ signal }) =>
      statisticsApi
        .pairRank({ ...base, brawlerId: brawlerId! }, { signal })
        .then((r) => r.content.map(toPairRankStat)),
    enabled: selected && resultType === "RANK",
    ...common,
  });

  return { pairResult, enemyResult, pairRank };
}
```

- [ ] **Step 4: 타입 확인 후 커밋**

```bash
pnpm --dir imstargg-frontend typecheck
```
Expected: 에러 0

```bash
git add imstargg-frontend/src/hooks/queries/useBrawlerStats.ts imstargg-frontend/src/hooks/queries/useItemOwnership.ts imstargg-frontend/src/hooks/queries/useEventStats.ts
git commit -m "feat(frontend): 통계 클라이언트 훅 추가 (기존 queryKeys.statistics 사용)"
```

---

### Task 3: 셀렉터 분리 (StatRangeSelectorView / controlled TrophyRangeSelector)

**Files:**
- Modify: `imstargg-frontend/app/_components/statistics/StatRangeSelector.tsx`
- Modify: `imstargg-frontend/app/_components/statistics/TrophyRangeSelector.tsx`
- Test: `imstargg-frontend/tests/unit/components/statistics/StatRangeSelector.test.tsx`

배경: `/brawlers` 목록 페이지는 서버가 searchParams 로 통계를 렌더하므로 router.replace 내비게이션이 필요하고, 정적 상세 페이지는 내비게이션 훅(useSearchParams)이 프리렌더 CSR bailout 을 유발하므로 훅 없는 View 가 필요하다.

- [ ] **Step 1: 실패하는 테스트 추가**

`imstargg-frontend/tests/unit/components/statistics/StatRangeSelector.test.tsx` 전체를 다음으로 교체:
```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeAll, describe, expect, it, vi } from "vitest";
import {
  StatRangeSelector,
  StatRangeSelectorView,
} from "@app/_components/statistics/StatRangeSelector";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/brawlers",
  useSearchParams: () => new URLSearchParams(""),
}));

// Radix Select 가 jsdom 에 없는 Pointer Capture API 를 요구한다
beforeAll(() => {
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

describe("StatRangeSelector (목록 페이지 래퍼)", () => {
  it("현재 tier/date 라벨을 표시한다", () => {
    render(<StatRangeSelector tier="TROPHY_2000_PLUS" date="LAST_WEEK" />);
    expect(screen.getByText("트로피 2,000+")).toBeInTheDocument();
    expect(screen.getByText("지난 주")).toBeInTheDocument();
  });
});

describe("StatRangeSelectorView (controlled)", () => {
  it("티어 선택 시 onSelect 로 새 필터를 전달한다", async () => {
    const onSelect = vi.fn();
    render(
      <StatRangeSelectorView
        tier="TROPHY_1000_PLUS"
        date="SINCE_LAST_WEEK"
        onSelect={onSelect}
      />,
    );
    await userEvent.click(screen.getByRole("combobox", { name: "티어 범위 선택" }));
    await userEvent.click(await screen.findByRole("option", { name: /트로피 3,000\+/ }));
    expect(onSelect).toHaveBeenCalledWith({
      tier: "TROPHY_3000_PLUS",
      date: "SINCE_LAST_WEEK",
    });
  });
});
```

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/unit/components/statistics/StatRangeSelector.test.tsx
```
Expected: FAIL — `StatRangeSelectorView` export 없음

- [ ] **Step 3: StatRangeSelector 분리 구현**

`imstargg-frontend/app/_components/statistics/StatRangeSelector.tsx` 전체 교체:
```tsx
"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  TIER_RANGE_LABELS,
  TIER_RANGE_ICONS,
  DATE_RANGE_LABELS,
  DEFAULT_TIER_RANGE,
  DEFAULT_DATE_RANGE,
  type StatsTierRange,
  type StatsDateRange,
} from "@/domain/statistics/enums";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

function TierIcon({ tier }: { tier: StatsTierRange }) {
  return (
    <Image
      src={TIER_RANGE_ICONS[tier]}
      alt=""
      width={16}
      height={16}
      unoptimized
      className="h-4 w-4 shrink-0 object-contain"
    />
  );
}

type ViewProps = {
  tier: StatsTierRange;
  date: StatsDateRange;
  onSelect: (next: { tier: StatsTierRange; date: StatsDateRange }) => void;
};

/**
 * 프레젠테이션 전용 셀렉터 — 내비게이션 훅이 없어 정적(ISR) 페이지의
 * 클라이언트 패널에서 프리렌더 bailout 없이 사용할 수 있다.
 */
export function StatRangeSelectorView({ tier, date, onSelect }: ViewProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select
        value={tier}
        onValueChange={(v) => onSelect({ tier: v as StatsTierRange, date })}
      >
        <SelectTrigger aria-label="티어 범위 선택" className="w-40 text-sm">
          <SelectValue>
            <span className="inline-flex items-center gap-1.5">
              <TierIcon tier={tier} />
              {TIER_RANGE_LABELS[tier]}
            </span>
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_TIER_RANGES.map((t) => (
            <SelectItem key={t} value={t}>
              <span className="inline-flex items-center gap-1.5">
                <TierIcon tier={t} />
                {TIER_RANGE_LABELS[t]}
              </span>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select
        value={date}
        onValueChange={(v) => onSelect({ tier, date: v as StatsDateRange })}
      >
        <SelectTrigger aria-label="기간 선택" className="w-32 text-sm">
          <SelectValue>{DATE_RANGE_LABELS[date]}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_DATE_RANGES.map((d) => (
            <SelectItem key={d} value={d}>
              {DATE_RANGE_LABELS[d]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

type Props = {
  tier: StatsTierRange;
  date: StatsDateRange;
};

/** 동적(서버 렌더) 목록 페이지용 — 선택 시 URL 내비게이션으로 서버 재렌더. */
export function StatRangeSelector({ tier, date }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function handleSelect(next: { tier: StatsTierRange; date: StatsDateRange }) {
    const sp = new URLSearchParams(params);
    if (next.tier === DEFAULT_TIER_RANGE) sp.delete("tier");
    else sp.set("tier", next.tier);
    if (next.date === DEFAULT_DATE_RANGE) sp.delete("date");
    else sp.set("date", next.date);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return <StatRangeSelectorView tier={tier} date={date} onSelect={handleSelect} />;
}
```

- [ ] **Step 4: TrophyRangeSelector 를 controlled 로 전환**

`imstargg-frontend/app/_components/statistics/TrophyRangeSelector.tsx` 전체 교체 (사용처가 브롤러 상세 한 곳뿐이라 내비게이션 경로 삭제):
```tsx
"use client";

import Image from "next/image";
import {
  BRAWLER_TROPHY_RANGES,
  TROPHY_RANGE_LABELS,
  TROPHY_RANGE_ICON,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type Props = {
  trophy: BrawlerTrophyRange;
  onSelect: (next: BrawlerTrophyRange) => void;
};

export function TrophyRangeSelector({ trophy, onSelect }: Props) {
  return (
    <Select value={trophy} onValueChange={(v) => onSelect(v as BrawlerTrophyRange)}>
      <SelectTrigger aria-label="트로피 구간 선택" className="w-28 text-sm">
        <SelectValue>
          <span className="inline-flex items-center gap-1.5">
            <Image
              src={TROPHY_RANGE_ICON}
              alt=""
              width={16}
              height={16}
              unoptimized
              className="h-4 w-4 shrink-0 object-contain"
            />
            {TROPHY_RANGE_LABELS[trophy]}
          </span>
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {BRAWLER_TROPHY_RANGES.map((t) => (
          <SelectItem key={t} value={t}>
            <span className="inline-flex items-center gap-1.5">
              <Image
                src={TROPHY_RANGE_ICON}
                alt=""
                width={16}
                height={16}
                unoptimized
                className="h-4 w-4 shrink-0 object-contain"
              />
              {TROPHY_RANGE_LABELS[t]}
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

주의: 이 시점에 `BrawlerItems.tsx` 가 `<TrophyRangeSelector trophy={trophy} />` 로 호출하고 있어 typecheck 이 깨진다 — Task 5 에서 함께 고치므로 이 task 의 typecheck 은 `BrawlerItems` 에러 1건만 허용, 테스트만 통과시킨다.

- [ ] **Step 5: 테스트 통과 확인 후 커밋**

```bash
pnpm --dir imstargg-frontend test:run tests/unit/components/statistics/StatRangeSelector.test.tsx
```
Expected: PASS (2 tests)

```bash
git add imstargg-frontend/app/_components/statistics/ imstargg-frontend/tests/unit/components/statistics/StatRangeSelector.test.tsx
git commit -m "refactor(frontend): 통계 셀렉터를 View/내비게이션 래퍼로 분리"
```

---

### Task 4: 이벤트 테이블 onToggleBrawler prop 전환

**Files:**
- Modify: `imstargg-frontend/app/events/[id]/_components/EventResultStatsTable.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventRankStatsTable.tsx`
- Test: `imstargg-frontend/tests/components/EventStatsTables.test.tsx`

두 테이블에서 `useRouter`/`usePathname`/`useSearchParams` 를 제거하고 선택 토글을 부모 콜백으로 위임한다 (정적 페이지에서 useSearchParams 는 CSR bailout 원인).

- [ ] **Step 1: 기존 테스트를 새 API 로 갱신 (실패 상태로)**

`imstargg-frontend/tests/components/EventStatsTables.test.tsx` 에서:
1. 파일 상단의 `const nav = vi.hoisted(...)` 블록과 `vi.mock("next/navigation", ...)` 블록을 삭제한다.
2. 모든 `<EventResultStatsTable ... />` / `<EventRankStatsTable ... />` 렌더에 `onToggleBrawler={vi.fn()}` prop 을 추가한다.
3. 파일 끝에 토글 테스트를 추가한다:
```tsx
  it("결과형 행 클릭 시 onToggleBrawler 로 브롤러 id 를 전달한다", async () => {
    const onToggle = vi.fn();
    render(
      <EventResultStatsTable
        items={[
          {
            brawler,
            battleCount: 275,
            pickRate: 0.03,
            winRate: 0.713,
            starPlayerRate: 0.276,
            tierScore: 100,
            tier: "S",
          },
        ]}
        selectedBrawlerId={null}
        onToggleBrawler={onToggle}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: /데미안/ }));
    expect(onToggle).toHaveBeenCalledWith(16000104);
  });
```
파일 상단 import 에 `import userEvent from "@testing-library/user-event";` 를 추가한다.

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/components/EventStatsTables.test.tsx
```
Expected: FAIL — onToggleBrawler prop 미존재(타입) 또는 next/navigation 모킹 제거로 인한 에러

- [ ] **Step 3: 테이블 2개 수정**

`EventResultStatsTable.tsx`:
1. import 에서 `usePathname, useRouter, useSearchParams` 줄(4행)을 삭제.
2. Props 를 다음으로 교체:
```tsx
type Props = {
  items: BrawlerResultStat[];
  selectedBrawlerId: number | null;
  onToggleBrawler: (id: number) => void;
};
```
3. 함수 시그니처를 `export function EventResultStatsTable({ items, selectedBrawlerId, onToggleBrawler }: Props)` 로 바꾸고, 본문의 `const router = ...`/`const pathname = ...`/`const params = ...` 3줄과 `function toggleBrawler(id: number) {...}` 블록을 삭제.
4. 행 버튼의 `onClick={() => id > 0 && toggleBrawler(id)}` 를 `onClick={() => id > 0 && onToggleBrawler(id)}` 로 교체.

`EventRankStatsTable.tsx` 도 동일한 4단계 수정 (동일 구조, `items: BrawlerRankStat[]`).

- [ ] **Step 4: 통과 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/components/EventStatsTables.test.tsx
```
Expected: PASS. 주의: 이 시점에 `EventStatsSection.tsx` 가 `onToggleBrawler` 없이 테이블을 렌더해 typecheck 이 깨진다 — Task 7·8 에서 해소된다.

- [ ] **Step 5: 커밋**

```bash
git add "imstargg-frontend/app/events/[id]/_components/EventResultStatsTable.tsx" "imstargg-frontend/app/events/[id]/_components/EventRankStatsTable.tsx" imstargg-frontend/tests/components/EventStatsTables.test.tsx
git commit -m "refactor(frontend): 이벤트 통계 테이블의 브롤러 선택을 controlled prop 으로 전환"
```

---

### Task 5: BrawlerStatsPanel (클라이언트 통계 패널 + recharts dynamic)

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsPanel.tsx`
- Test: `imstargg-frontend/tests/components/BrawlerStatsPanel.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/components/BrawlerStatsPanel.test.tsx`:
```tsx
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { BrawlerStatsPanel } from "@app/brawlers/[id]/_components/BrawlerStatsPanel";

beforeAll(() => {
  // recharts(ChartContainer)가 jsdom 에서 요구하는 ResizeObserver 스텁
  vi.stubGlobal(
    "ResizeObserver",
    class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  );
  // Radix Select 가 jsdom 에 없는 Pointer Capture API 를 요구한다
  Object.assign(window.HTMLElement.prototype, {
    hasPointerCapture: () => false,
    setPointerCapture: () => {},
    releasePointerCapture: () => {},
    scrollIntoView: () => {},
  });
});

beforeEach(() => {
  // 테스트 간 URL(쿼리스트링) 오염 방지 — 패널이 mount 시 URL 을 읽는다
  window.history.replaceState(null, "", "/brawlers/16000000");
});

const initial = {
  daily: [],
  pair: [
    {
      brawler: { id: 1, name: "쉘리", rarity: "COMMON", role: "DAMAGE_DEALER", portraitImageUrl: null },
      battleCount: 100,
      winRate: 0.55,
    },
  ],
  enemy: [],
};

describe("BrawlerStatsPanel", () => {
  it("서버 initial 데이터를 즉시 렌더한다 (추가 fetch 없음)", async () => {
    renderWithProviders(<BrawlerStatsPanel brawlerId={16000000} initial={initial} />);
    expect(await screen.findByText("쉘리")).toBeInTheDocument();
  });

  it("티어 필터 변경 시 해당 필터로 통계를 다시 가져오고 URL 을 갱신한다", async () => {
    let requestedTier: string | null = null;
    server.use(
      http.get("*/v1/statistics/brawler/result/daily", () => HttpResponse.json({ content: [] })),
      http.get("*/v1/statistics/brawler/pair/result", ({ request }) => {
        requestedTier = new URL(request.url).searchParams.get("tierRange");
        return HttpResponse.json({
          content: [
            {
              brawler: { id: 2, name: "콜트", rarity: "COMMON", role: "DAMAGE_DEALER", portraitImageUrl: null },
              battleCount: 42,
              winRate: 0.61,
            },
          ],
        });
      }),
      http.get("*/v1/statistics/brawler/enemy/result", () => HttpResponse.json({ content: [] })),
    );

    renderWithProviders(<BrawlerStatsPanel brawlerId={16000000} initial={initial} />);

    await userEvent.click(screen.getByRole("combobox", { name: "티어 범위 선택" }));
    await userEvent.click(await screen.findByRole("option", { name: /트로피 3,000\+/ }));

    expect(await screen.findByText("콜트")).toBeInTheDocument();
    await waitFor(() => expect(requestedTier).toBe("TROPHY_3000_PLUS"));
    expect(window.location.search).toContain("tier=TROPHY_3000_PLUS");
  });
});
```

주의: pair 응답 DTO 의 필드는 `src/api/types.ts` 의 `BrawlerPairResultStatisticsItemResponse` 정의를 따라야 한다. 위 코드의 필드(`brawler`, `battleCount`, `winRate`)가 실제 타입과 다르면 **실제 타입에 맞춰 테스트 fixture 를 수정**하라 (`toPairResultStat` 매퍼 소스가 정답).

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/components/BrawlerStatsPanel.test.tsx
```
Expected: FAIL — 모듈 없음

- [ ] **Step 3: 구현**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsPanel.tsx`:
```tsx
"use client";

import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { applySearchShallow } from "@/lib/shallowSearch";
import {
  DEFAULT_STATS_FILTER,
  useBrawlerStats,
  type BrawlerStatsInitial,
  type StatsFilter,
} from "@/hooks/queries/useBrawlerStats";
import { StatRangeSelectorView } from "@app/_components/statistics/StatRangeSelector";
import { PairStatsTable } from "./PairStatsTable";
import { EnemyStatsTable } from "./EnemyStatsTable";

const DailyTrendChart = dynamic(
  () => import("./DailyTrendChart").then((m) => m.DailyTrendChart),
  {
    ssr: false,
    loading: () => (
      <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">
        차트 불러오는 중…
      </div>
    ),
  },
);

type Props = {
  brawlerId: number;
  initial: BrawlerStatsInitial;
};

export function BrawlerStatsPanel({ brawlerId, initial }: Props) {
  // useSearchParams 대신 useState + popstate 동기화 — 정적 프리렌더에서
  // CSR bailout 없이 기본값 통계가 HTML 에 포함되게 한다.
  const [filter, setFilter] = useState<StatsFilter>(DEFAULT_STATS_FILTER);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setFilter({
        tier: parseTierRange(sp.get("tier") ?? undefined),
        date: parseDateRange(sp.get("date") ?? undefined),
      });
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const { daily, pair, enemy } = useBrawlerStats(brawlerId, filter, initial);

  function handleFilterChange(next: StatsFilter) {
    setFilter(next);
    applySearchShallow({
      tier: next.tier === DEFAULT_TIER_RANGE ? null : next.tier,
      date: next.date === DEFAULT_DATE_RANGE ? null : next.date,
    });
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="section-title">통계</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            {TIER_RANGE_LABELS[filter.tier]} 기준
          </p>
        </div>
        <StatRangeSelectorView
          tier={filter.tier}
          date={filter.date}
          onSelect={handleFilterChange}
        />
      </div>
      <DailyTrendChart items={daily.data ?? []} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.data ?? []} />
        <EnemyStatsTable items={enemy.data ?? []} />
      </div>
    </section>
  );
}
```

- [ ] **Step 4: 통과 확인 후 커밋**

```bash
pnpm --dir imstargg-frontend test:run tests/components/BrawlerStatsPanel.test.tsx
```
Expected: PASS (2 tests)

```bash
git add "imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsPanel.tsx" imstargg-frontend/tests/components/BrawlerStatsPanel.test.tsx
git commit -m "feat(frontend): 브롤러 통계 클라이언트 패널 추가 (shallow 필터 + recharts lazy)"
```

---

### Task 6: brawlers/[id] 정적화 (searchParams 제거 + 아이템 탭 클라이언트화)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx`
- Delete: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`

- [ ] **Step 1: page.tsx 교체**

`imstargg-frontend/app/brawlers/[id]/page.tsx` — import 블록과 페이지 함수를 다음으로 교체 (`generateStaticParams`/`generateMetadata`/`export const revalidate = 60` 은 그대로 유지):
```tsx
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import { toBrawlerWithItems } from "@/domain/brawler/mappers";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  DEFAULT_TROPHY_RANGE,
} from "@/domain/statistics/enums";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toItemOwnership,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";
import { BrawlerStatsPanel } from "./_components/BrawlerStatsPanel";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

type Params = { params: Promise<{ id: string }> };
```
페이지 함수:
```tsx
export default async function BrawlerPage({ params }: Params) {
  const { id } = await params;
  let detail;
  try {
    const dto = await brawlerApi.get(id, {
      next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE },
    });
    detail = toBrawlerWithItems(dto);
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }

  const statsParams = {
    brawlerId: detail.brawler.id,
    tierRange: DEFAULT_TIER_RANGE,
    dateRange: DEFAULT_DATE_RANGE,
  };
  const opts = { next: { revalidate: CACHE_SECONDS.SHORT } };
  const [ownershipDto, dailyDto, pairDto, enemyDto] = await Promise.all([
    statisticsApi.itemOwnership(
      { brawlerId: detail.brawler.id, trophyRange: DEFAULT_TROPHY_RANGE },
      opts,
    ),
    statisticsApi.daily(statsParams, opts),
    statisticsApi.pairResult(statsParams, opts),
    statisticsApi.enemyResult(statsParams, opts),
  ]);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 pb-10">
      <BrawlerContent
        initialDetail={detail}
        initialOwnership={toItemOwnership(ownershipDto)}
      />
      <BrawlerStatsPanel
        brawlerId={detail.brawler.id}
        initial={{
          daily: dailyDto.content.map(toDailyResultStat),
          pair: pairDto.content.map(toPairResultStat),
          enemy: enemyDto.content.map(toEnemyResultStat),
        }}
      />
      <AdUnit slot={AD_SLOTS.brawlerDetail} className="mt-6" />
    </main>
  );
}
```
그리고 `BrawlerStatsSection.tsx` 파일을 삭제한다:
```bash
git rm "imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx"
```

- [ ] **Step 2: BrawlerContent 수정**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx` 에서:
1. `import type { BrawlerTrophyRange } from "@/domain/statistics/enums";` 삭제.
2. Props 를 교체:
```tsx
type Props = {
  initialDetail: BrawlerWithItems;
  initialOwnership: ItemOwnership;
};
```
3. 시그니처를 `export function BrawlerContent({ initialDetail, initialOwnership }: Props)` 로.
4. `<BrawlerItems items={detail.items} ownership={ownership} trophy={trophy} />` 를 다음으로 교체:
```tsx
<BrawlerItems
  items={detail.items}
  brawlerId={detail.brawler.id}
  initialOwnership={initialOwnership}
/>
```

- [ ] **Step 3: BrawlerItems 를 필터 소유 컴포넌트로 수정**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx` 에서 import·Props·본문 함수를 수정:
```tsx
"use client";

import { useEffect, useState } from "react";
import type { BrawlerItems as BrawlerItemsModel } from "@/domain/brawler/types";
import {
  buildBrawlerItemGroups,
  type BrawlerItemSection,
} from "@/domain/brawler/itemOwnership";
import {
  DEFAULT_TROPHY_RANGE,
  parseTrophyRange,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { useItemOwnership } from "@/hooks/queries/useItemOwnership";
import { applySearchShallow } from "@/lib/shallowSearch";
import { cn } from "@/lib/cn";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";
import { BrawlerItemCard } from "./BrawlerItemCard";

type Props = {
  items: BrawlerItemsModel;
  brawlerId: number;
  initialOwnership: ItemOwnership;
};
```
(`hasOwnershipRows`/`ItemSection` 은 그대로 두고) 본문 함수를 교체:
```tsx
export function BrawlerItems({ items, brawlerId, initialOwnership }: Props) {
  const [trophy, setTrophy] = useState<BrawlerTrophyRange>(DEFAULT_TROPHY_RANGE);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setTrophy(parseTrophyRange(sp.get("trophy") ?? undefined));
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const query = useItemOwnership(brawlerId, trophy, initialOwnership);
  const ownership = query.data ?? initialOwnership;

  function handleTrophyChange(next: BrawlerTrophyRange) {
    setTrophy(next);
    applySearchShallow({ trophy: next === DEFAULT_TROPHY_RANGE ? null : next });
  }

  const groups = buildBrawlerItemGroups(items, ownership);
  const isOwnershipEmpty = !hasOwnershipRows(ownership);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <h2 className="text-lg font-bold">아이템</h2>
        <TrophyRangeSelector trophy={trophy} onSelect={handleTrophyChange} />
      </div>
      {isOwnershipEmpty && (
        <div className="rounded-lg border bg-card px-4 py-3 text-sm text-muted-foreground">
          보유율 데이터가 없습니다. 아이템은 데이터 없음 상태로 표시됩니다.
        </div>
      )}
      <div className="flex flex-col gap-5">
        {groups.map((group) => (
          <div
            key={group.key}
            className={cn(
              "grid grid-cols-1 gap-4",
              group.key !== "gears" && "lg:grid-cols-2",
            )}
          >
            {group.sections.map((section) => (
              <ItemSection key={section.key} section={section} />
            ))}
          </div>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: 검증 — typecheck + 전체 테스트 + 프로덕션 빌드**

```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
```
Expected: typecheck 에러 0 (Task 3 에서 예고한 BrawlerItems 에러 해소), 테스트 전부 통과

**프로덕션 빌드** 실행 후 라우트 테이블에서 확인:
- `/brawlers/[id]` 가 `●` (SSG/ISR) + Revalidate `1m` 로 표기되고 `(+106 more paths)` 류의 경로 수가 보일 것
- 만약 빌드가 `useSearchParams() should be wrapped in a suspense boundary` 에러로 실패하면 원인 컴포넌트를 확인하라 — `BrawlerRankingTab`(비활성 탭이라 프리렌더에서 렌더되지 않아 정상적으로는 발생하지 않음)이 원인이면 `BrawlerContent.tsx` 의 `<TabsContent value="ranking">` 내부를 `<Suspense fallback={null}>` 로 감싼다 (`import { Suspense } from "react"` 추가)

추가 확인 (산출물에 HTML 이 실제로 존재하는지):
```bash
python3 -c "import json; m = json.load(open('imstargg-frontend/.next/prerender-manifest.json')); print(sum(1 for k in m['routes'] if k.startswith('/brawlers/')))"
```
Expected: 100 내외 (0 이 아니어야 함)

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-frontend/app imstargg-frontend/src
git commit -m "feat(frontend): 브롤러 상세 ISR 복구 — 통계/아이템 필터를 클라이언트로 이동"
```

---

### Task 7: EventStatsPanel + EventSelectedBrawlerPanel

**Files:**
- Create: `imstargg-frontend/app/events/[id]/_components/EventStatsPanel.tsx`
- Create: `imstargg-frontend/app/events/[id]/_components/EventSelectedBrawlerPanel.tsx`
- Test: `imstargg-frontend/tests/components/EventStatsPanel.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/components/EventStatsPanel.test.tsx`:
```tsx
import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { EventStatsPanel } from "@app/events/[id]/_components/EventStatsPanel";

const brawlerDto = {
  id: 16000104,
  name: "데미안",
  rarity: "EPIC",
  role: "TANK",
  portraitImageUrl: null,
};

const initialResult = {
  totalBattleCount: 1000,
  items: [
    {
      brawler: { id: 16000104, name: "데미안", rarity: "EPIC", role: "TANK", portraitImageUrl: null },
      battleCount: 275,
      pickRate: 0.03,
      winRate: 0.713,
      starPlayerRate: 0.276,
      tierScore: 100,
      tier: "S",
    },
  ],
};

describe("EventStatsPanel (결과형)", () => {
  it("초기 통계를 렌더한다", async () => {
    renderWithProviders(
      <EventStatsPanel
        eventId={15001021}
        resultType="RESULT"
        initialResult={initialResult}
        initialRank={null}
      />,
    );
    expect(await screen.findByText("데미안")).toBeInTheDocument();
  });

  it("브롤러 행 클릭 → 선택 패널이 열리고 URL 에 brawlerId 가 실린다", async () => {
    server.use(
      http.get("*/v1/statistics/brawler/pair/result", () =>
        HttpResponse.json({
          content: [{ brawler: brawlerDto, battleCount: 10, winRate: 0.5 }],
        }),
      ),
      http.get("*/v1/statistics/brawler/enemy/result", () =>
        HttpResponse.json({ content: [] }),
      ),
    );

    renderWithProviders(
      <EventStatsPanel
        eventId={15001021}
        resultType="RESULT"
        initialResult={initialResult}
        initialRank={null}
      />,
    );

    await userEvent.click(await screen.findByRole("button", { name: /데미안/ }));
    expect(await screen.findByText("선택한 브롤러 분석")).toBeInTheDocument();
    expect(window.location.search).toContain("brawlerId=16000104");

    await userEvent.click(screen.getByRole("button", { name: /닫기/ }));
    expect(screen.queryByText("선택한 브롤러 분석")).not.toBeInTheDocument();
    expect(window.location.search).not.toContain("brawlerId");
  });
});
```

주의: `initialResult`/pair 응답 fixture 의 필드가 `BrawlerResultStatistics`(`src/domain/statistics/types.ts`)·`BrawlerPairResultStatisticsItemResponse`(`src/api/types.ts`) 실제 정의와 다르면 실제 타입에 맞춰 수정하라. 또한 테스트 간 URL 오염 방지를 위해 이전 테스트가 남긴 쿼리스트링이 문제되면 각 it 시작에서 `window.history.replaceState(null, "", "/events/15001021")` 로 초기화한다.

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/components/EventStatsPanel.test.tsx
```
Expected: FAIL — 모듈 없음

- [ ] **Step 3: EventSelectedBrawlerPanel 구현**

`imstargg-frontend/app/events/[id]/_components/EventSelectedBrawlerPanel.tsx`:
```tsx
"use client";

import type { BattleEventModeResultType } from "@/api/types";
import { useEventSelectedStats } from "@/hooks/queries/useEventStats";
import type { StatsFilter } from "@/hooks/queries/useBrawlerStats";
import { PairStatsTable } from "@app/brawlers/[id]/_components/PairStatsTable";
import { EnemyStatsTable } from "@app/brawlers/[id]/_components/EnemyStatsTable";
import { PairRankTable } from "./PairRankTable";

type Props = {
  eventId: number;
  brawlerId: number;
  resultType: BattleEventModeResultType;
  filter: StatsFilter;
  onClose: () => void;
};

function LoadingBox() {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
      불러오는 중…
    </div>
  );
}

export function EventSelectedBrawlerPanel({
  eventId,
  brawlerId,
  resultType,
  filter,
  onClose,
}: Props) {
  const { pairResult, enemyResult, pairRank } = useEventSelectedStats(
    eventId,
    brawlerId,
    resultType,
    filter,
  );

  const isRank = resultType === "RANK";
  const loading = isRank
    ? pairRank.isPending
    : pairResult.isPending || enemyResult.isPending;

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
        <button
          type="button"
          onClick={onClose}
          className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]"
        >
          닫기 ✕
        </button>
      </div>
      {loading ? (
        <LoadingBox />
      ) : (
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {isRank ? (
            <>
              <PairRankTable items={pairRank.data ?? []} />
              <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
                카운터는 결과형 모드 전용입니다.
              </div>
            </>
          ) : (
            <>
              <PairStatsTable items={pairResult.data ?? []} />
              <EnemyStatsTable items={enemyResult.data ?? []} />
            </>
          )}
        </div>
      )}
    </section>
  );
}
```

- [ ] **Step 4: EventStatsPanel 구현**

`imstargg-frontend/app/events/[id]/_components/EventStatsPanel.tsx`:
```tsx
"use client";

import { useEffect, useState } from "react";
import type { BattleEventModeResultType } from "@/api/types";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import type {
  BrawlerRankStat,
  BrawlerResultStatistics,
} from "@/domain/statistics/types";
import {
  useEventRankStats,
  useEventResultStats,
} from "@/hooks/queries/useEventStats";
import {
  DEFAULT_STATS_FILTER,
  type StatsFilter,
} from "@/hooks/queries/useBrawlerStats";
import { applySearchShallow } from "@/lib/shallowSearch";
import { StatRangeSelectorView } from "@app/_components/statistics/StatRangeSelector";
import { EventResultStatsTable } from "./EventResultStatsTable";
import { EventRankStatsTable } from "./EventRankStatsTable";
import { EventSelectedBrawlerPanel } from "./EventSelectedBrawlerPanel";

type Props = {
  eventId: number;
  resultType: BattleEventModeResultType;
  initialResult: BrawlerResultStatistics | null;
  initialRank: BrawlerRankStat[] | null;
};

function parseBrawlerId(raw: string | null): number | null {
  return raw && /^\d+$/.test(raw) ? Number(raw) : null;
}

export function EventStatsPanel({
  eventId,
  resultType,
  initialResult,
  initialRank,
}: Props) {
  const [filter, setFilter] = useState<StatsFilter>(DEFAULT_STATS_FILTER);
  const [selectedBrawlerId, setSelectedBrawlerId] = useState<number | null>(null);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setFilter({
        tier: parseTierRange(sp.get("tier") ?? undefined),
        date: parseDateRange(sp.get("date") ?? undefined),
      });
      setSelectedBrawlerId(parseBrawlerId(sp.get("brawlerId")));
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const isRank = resultType === "RANK";
  const resultStats = useEventResultStats(
    eventId,
    filter,
    initialResult ?? undefined,
    !isRank,
  );
  const rankStats = useEventRankStats(
    eventId,
    filter,
    initialRank ?? undefined,
    isRank,
  );

  function handleFilterChange(next: StatsFilter) {
    setFilter(next);
    applySearchShallow({
      tier: next.tier === DEFAULT_TIER_RANGE ? null : next.tier,
      date: next.date === DEFAULT_DATE_RANGE ? null : next.date,
    });
  }

  function handleToggleBrawler(id: number) {
    const next = selectedBrawlerId === id ? null : id;
    setSelectedBrawlerId(next);
    applySearchShallow({ brawlerId: next === null ? null : String(next) });
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-bold">
            {isRank ? "이벤트 통계 (순위형)" : "이벤트 통계"}
          </h2>
          <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[filter.tier]} 기준</p>
        </div>
        <StatRangeSelectorView
          tier={filter.tier}
          date={filter.date}
          onSelect={handleFilterChange}
        />
      </div>
      {isRank ? (
        <EventRankStatsTable
          items={rankStats.data ?? []}
          selectedBrawlerId={selectedBrawlerId}
          onToggleBrawler={handleToggleBrawler}
        />
      ) : (
        <EventResultStatsTable
          items={resultStats.data?.items ?? []}
          selectedBrawlerId={selectedBrawlerId}
          onToggleBrawler={handleToggleBrawler}
        />
      )}
      {selectedBrawlerId !== null && (
        <EventSelectedBrawlerPanel
          eventId={eventId}
          brawlerId={selectedBrawlerId}
          resultType={resultType}
          filter={filter}
          onClose={() => handleToggleBrawler(selectedBrawlerId)}
        />
      )}
    </section>
  );
}
```

- [ ] **Step 5: 통과 확인 후 커밋**

```bash
pnpm --dir imstargg-frontend test:run tests/components/EventStatsPanel.test.tsx
```
Expected: PASS (2 tests)

```bash
git add "imstargg-frontend/app/events/[id]/_components/EventStatsPanel.tsx" "imstargg-frontend/app/events/[id]/_components/EventSelectedBrawlerPanel.tsx" imstargg-frontend/tests/components/EventStatsPanel.test.tsx
git commit -m "feat(frontend): 이벤트 통계 클라이언트 패널 추가 (선택 브롤러 분석 포함)"
```

---

### Task 8: events/[id] 정적화

**Files:**
- Modify: `imstargg-frontend/app/events/[id]/page.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`
- Delete: `imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx`

- [ ] **Step 1: EventStatsSection 을 기본값 페치 + 패널 위임으로 교체**

`imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx` 전체 교체:
```tsx
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
} from "@/domain/statistics/enums";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
} from "@/domain/statistics/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { EventStatsPanel } from "./EventStatsPanel";

export async function EventStatsSection({ event }: { event: BattleEvent }) {
  const resultType = event.mode?.resultType ?? "RESULT";
  const base = {
    eventId: event.id,
    tierRange: DEFAULT_TIER_RANGE,
    dateRange: DEFAULT_DATE_RANGE,
  };
  const opts = { next: { revalidate: CACHE_SECONDS.SHORT } };

  if (resultType === "RANK") {
    const main = await statisticsApi.brawlerRank(base, opts);
    return (
      <EventStatsPanel
        eventId={event.id}
        resultType="RANK"
        initialResult={null}
        initialRank={main.content.map(toBrawlerRankStat)}
      />
    );
  }

  const main = await statisticsApi.brawlerResult(base, opts);
  return (
    <EventStatsPanel
      eventId={event.id}
      resultType="RESULT"
      initialResult={toBrawlerResultStatistics(main)}
      initialRank={null}
    />
  );
}
```
기존 RSC 선택 패널 삭제:
```bash
git rm "imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx"
```

- [ ] **Step 2: page.tsx 정적화**

`imstargg-frontend/app/events/[id]/page.tsx` 에서:
1. `generateMetadata` 안의 `eventApi.get(id, { cache: "no-store" })` → `eventApi.get(id, { next: { revalidate: CACHE_SECONDS.SHORT } })`.
2. 페이지 함수를 다음으로 교체 (searchParams 제거):
```tsx
export const revalidate = 60;

export default async function EventPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await eventApi.get(id, { next: { revalidate: CACHE_SECONDS.SHORT } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const event = toBattleEvent(dto);
  return (
    <main className="mx-auto w-full max-w-7xl px-4 pb-10">
      <EventContent initialEvent={event} />
      <EventStatsSection event={event} />
      <AdUnit slot={AD_SLOTS.eventDetail} className="mt-6" />
    </main>
  );
}
```
(`export const revalidate = 60;` 는 import 블록 아래, `generateStaticParams` 위에 배치한다.)

- [ ] **Step 3: 검증 — typecheck + 전체 테스트 + 프로덕션 빌드**

```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
```
Expected: 에러 0, 전부 통과 (Task 4 에서 예고한 EventStatsSection typecheck 에러 해소)

**프로덕션 빌드** 후 라우트 테이블 확인:
- `/events/[id]` 가 `●` + Revalidate `1m`

- [ ] **Step 4: 커밋**

```bash
git add -A imstargg-frontend/app
git commit -m "feat(frontend): 이벤트 상세 ISR 복구 — no-store 제거·필터 클라이언트화"
```

---

### Task 9: players/clubs 병렬화 + 배틀 첫 페이지 프리페치

**Files:**
- Modify: `imstargg-frontend/src/hooks/queries/usePlayerBattles.ts`
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattleList.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBattles.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/page.tsx`
- Modify: `imstargg-frontend/app/clubs/[tag]/page.tsx`
- Test: `imstargg-frontend/tests/components/BattleListInitialData.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/components/BattleListInitialData.test.tsx`:
```tsx
import { describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { BattleList } from "@app/players/[tag]/_components/battle/BattleList";
import type { SliceResponse, BattleResponse } from "@/api/types";

// 최소 필드의 배틀 DTO — src/api/types.ts 의 BattleResponse 와 다르면 실제 타입에 맞춰 수정
const battle = {
  battleTime: "2026-07-01T12:00:00Z",
  event: null,
  type: "RANKED",
  result: "VICTORY",
  duration: 120,
  rank: null,
  trophyChange: 8,
  starPlayer: null,
  teams: [],
} as unknown as BattleResponse;

const initialPage: SliceResponse<BattleResponse> = {
  content: [battle],
  hasNext: false,
};

describe("BattleList — RSC 프리페치 initialData", () => {
  it("initialBattles 가 있으면 네트워크 요청 없이 즉시 렌더한다", async () => {
    // MSW 에 배틀 핸들러를 등록하지 않는다 — 요청이 발생하면
    // onUnhandledRequest 정책에 따라 실패하거나 에러 UI 가 뜨므로 검출된다.
    renderWithProviders(
      <BattleList
        tag="#88J8Q9RPV"
        initialBattles={initialPage}
        battlesFetchedAt={Date.now()}
      />,
    );
    expect(await screen.findByText(/더 이상 기록이 없어요/)).toBeInTheDocument();
    expect(screen.queryByText(/불러오지 못했어요/)).not.toBeInTheDocument();
  });
});
```
주의: `battle` fixture 는 `BattleRow` 가 렌더에 필요로 하는 필드를 충족해야 한다. 렌더가 필드 부족으로 깨지면 `src/domain/battle/mappers.ts` 의 `toBattle` 입력(=`BattleResponse`)을 열어 실제 필수 필드로 fixture 를 보강하라.

- [ ] **Step 2: 실패 확인**

```bash
pnpm --dir imstargg-frontend test:run tests/components/BattleListInitialData.test.tsx
```
Expected: FAIL — `initialBattles` prop 미존재

- [ ] **Step 3: usePlayerBattles 에 initialData 지원 추가**

`imstargg-frontend/src/hooks/queries/usePlayerBattles.ts` 전체 교체:
```ts
import { useInfiniteQuery } from "@tanstack/react-query";
import { battleApi } from "@/api/endpoints/battle";
import type { BattleResponse, SliceResponse } from "@/api/types";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBattle } from "@/domain/battle/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export const BATTLES_PAGE_SIZE = 25;

export interface PlayerBattlesOptions {
  /** RSC 가 프리페치한 첫 페이지 (원본 DTO — select 이전 형태) */
  initialPage?: SliceResponse<BattleResponse>;
  /** 서버 페치 시각(ms) — 신선도 계산용 */
  fetchedAt?: number;
}

export function usePlayerBattles(tag: string, options?: PlayerBattlesOptions) {
  return useInfiniteQuery({
    queryKey: queryKeys.player.battles(tag),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) =>
      battleApi.getPlayerBattles(
        tag,
        { offset: pageParam as number, limit: BATTLES_PAGE_SIZE },
        { signal },
      ),
    getNextPageParam: (lastPage, _pages, lastPageParam) =>
      lastPage.hasNext ? (lastPageParam as number) + BATTLES_PAGE_SIZE : undefined,
    select: (data) => ({
      pages: data.pages.map((p) => p.content.map(toBattle)),
      pageParams: data.pageParams,
    }),
    initialData: options?.initialPage
      ? { pages: [options.initialPage], pageParams: [0] }
      : undefined,
    initialDataUpdatedAt: options?.fetchedAt,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
```

- [ ] **Step 4: BattleList → PlayerBattles → PlayerContent 프롭 스레딩**

`BattleList.tsx` — Props 와 훅 호출만 수정:
```tsx
import type { BattleResponse, SliceResponse } from "@/api/types";
import { usePlayerBattles, type PlayerBattlesOptions } from "@/hooks/queries/usePlayerBattles";

interface Props {
  tag: string;
  initialBattles?: SliceResponse<BattleResponse>;
  battlesFetchedAt?: number;
}

export function BattleList({ tag, initialBattles, battlesFetchedAt }: Props) {
  const {
    data, isLoading, isError, error,
    fetchNextPage, hasNextPage, isFetchingNextPage, refetch,
  } = usePlayerBattles(tag, { initialPage: initialBattles, fetchedAt: battlesFetchedAt });
  ...나머지 본문 무변경...
```
(import 정리 시 `PlayerBattlesOptions` 를 실제로 쓰지 않으면 import 하지 않는다.)

`PlayerBattles.tsx` 전체 교체:
```tsx
"use client";

import type { BattleResponse, SliceResponse } from "@/api/types";
import { BattleList } from "./battle/BattleList";

type Props = {
  tag: string;
  initialBattles?: SliceResponse<BattleResponse>;
  battlesFetchedAt?: number;
};

export function PlayerBattles({ tag, initialBattles, battlesFetchedAt }: Props) {
  return (
    <BattleList
      tag={tag}
      initialBattles={initialBattles}
      battlesFetchedAt={battlesFetchedAt}
    />
  );
}
```

`PlayerContent.tsx` — Props 에 두 필드 추가 + `PlayerBattles` 호출부 교체:
```tsx
type Props = {
  tag: string;
  initialPlayer: Player;
  initialBrawlers: PlayerBrawler[];
  initialRenewal: RenewalResponse | null;
  initialBattles?: SliceResponse<BattleResponse>;
  battlesFetchedAt?: number;
};
```
(import 에 `SliceResponse, BattleResponse` 추가 — 기존 `RenewalResponse` import 와 같은 줄에 합친다.)
```tsx
<TabsContent value="battles">
  <PlayerBattles
    tag={tag}
    initialBattles={initialBattles}
    battlesFetchedAt={battlesFetchedAt}
  />
</TabsContent>
```
함수 시그니처의 구조 분해에도 `initialBattles, battlesFetchedAt` 를 추가한다.

- [ ] **Step 5: players/[tag]/page.tsx 병렬화**

페이지 함수와 `loadPlayer` 를 교체 (`generateMetadata` 무변경), import 에 `battleApi`·`BATTLES_PAGE_SIZE`·`CACHE_SECONDS` 추가:
```tsx
import { battleApi } from "@/api/endpoints/battle";
import { BATTLES_PAGE_SIZE } from "@/hooks/queries/usePlayerBattles";
import { CACHE_SECONDS } from "@/config/cache";
```
```tsx
async function loadPlayer(tag: string) {
  return playerApi.get(tag, { next: { revalidate: CACHE_SECONDS.SHORT } });
}

export default async function PlayerPage({ params }: Params) {
  const { tag } = await params;

  // 4개 요청을 동시에 시작한다. allSettled 이므로 개별 실패가
  // unhandled rejection 이 되지 않고, 아래에서 의미론을 명시적으로 복원한다.
  const [playerResult, brawlersResult, renewalResult, battlesResult] =
    await Promise.allSettled([
      loadPlayer(tag),
      playerApi.getBrawlers(tag, { next: { revalidate: CACHE_SECONDS.SHORT } }),
      renewalApi.getPlayer(tag, { next: { revalidate: 0 } }),
      battleApi.getPlayerBattles(
        tag,
        { offset: 0, limit: BATTLES_PAGE_SIZE },
        { next: { revalidate: CACHE_SECONDS.SHORT } },
      ),
    ]);

  // player 404 → 갱신 유도 화면 (기존 동작)
  if (playerResult.status === "rejected") {
    if (playerResult.reason instanceof NotFoundError) {
      return <PlayerNotFoundView tag={normalizeTag(tag)} />;
    }
    throw playerResult.reason;
  }
  // player 정상인데 brawlers 실패 → 기존처럼 error.tsx 로
  if (brawlersResult.status === "rejected") throw brawlersResult.reason;

  const renewal =
    renewalResult.status === "fulfilled"
      ? renewalResult.value
      : renewalResult.reason instanceof NotFoundError
        ? emptyRenewal(tag)
        : null;

  // 배틀 프리페치 실패는 페이지 실패로 승격하지 않는다 — 클라이언트 훅이 자체 페치
  const initialBattles =
    battlesResult.status === "fulfilled" ? battlesResult.value : undefined;

  return (
    <PlayerContent
      tag={normalizeTag(tag)}
      initialPlayer={toPlayer(playerResult.value)}
      initialBrawlers={brawlersResult.value.content.map(toPlayerBrawler)}
      initialRenewal={renewal}
      initialBattles={initialBattles}
      battlesFetchedAt={initialBattles ? Date.now() : undefined}
    />
  );
}
```

- [ ] **Step 6: clubs/[tag]/page.tsx 병렬화**

`loadClub` 의 `revalidate: 60` → `CACHE_SECONDS.SHORT` (import 추가), 페이지 함수 교체:
```tsx
export default async function ClubPage({ params }: Params) {
  const { tag } = await params;
  const [clubResult, renewalResult] = await Promise.allSettled([
    loadClub(tag),
    renewalApi.getClub(tag, { next: { revalidate: 0 } }),
  ]);

  if (clubResult.status === "rejected") {
    if (clubResult.reason instanceof NotFoundError) {
      return <ClubNotFoundView tag={normalizeTag(tag)} />;
    }
    throw clubResult.reason;
  }

  const renewal =
    renewalResult.status === "fulfilled"
      ? renewalResult.value
      : renewalResult.reason instanceof NotFoundError
        ? emptyRenewal(tag)
        : null;

  return (
    <ClubContent
      tag={normalizeTag(tag)}
      initialClub={toClub(clubResult.value)}
      initialRenewal={renewal}
    />
  );
}
```

- [ ] **Step 7: 검증 후 커밋**

```bash
pnpm --dir imstargg-frontend test:run tests/components/BattleListInitialData.test.tsx && pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend test:run
```
Expected: 전부 통과 (기존 renewal 통합 테스트 포함 — 의미론 보존 확인)

```bash
git add -A imstargg-frontend/app imstargg-frontend/src imstargg-frontend/tests
git commit -m "feat(frontend): 플레이어/클럽 페이지 병렬 페칭 + 배틀 첫 페이지 RSC 프리페치"
```

---

### Task 10: loading.tsx 4개 + PageSkeleton

**Files:**
- Create: `imstargg-frontend/src/components/shared/PageSkeleton.tsx`
- Create: `imstargg-frontend/app/players/[tag]/loading.tsx`, `imstargg-frontend/app/clubs/[tag]/loading.tsx`, `imstargg-frontend/app/brawlers/[id]/loading.tsx`, `imstargg-frontend/app/events/[id]/loading.tsx`

- [ ] **Step 1: PageSkeleton 작성**

`imstargg-frontend/src/components/shared/PageSkeleton.tsx`:
```tsx
export function PageSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <main className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6">
      <div className="card" style={{ padding: 24 }}>
        <div className="skel" style={{ height: 96, borderRadius: 12 }} />
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="card" style={{ height: 64, padding: 12 }}>
            <div className="skel" style={{ width: "100%", height: "100%", borderRadius: 6 }} />
          </div>
        ))}
      </div>
    </main>
  );
}
```

- [ ] **Step 2: loading.tsx 4개 작성 (모두 동일 내용)**

`imstargg-frontend/app/players/[tag]/loading.tsx` (나머지 3개 경로도 동일 파일 내용):
```tsx
import { PageSkeleton } from "@/components/shared/PageSkeleton";

export default function Loading() {
  return <PageSkeleton />;
}
```

- [ ] **Step 3: 검증 후 커밋**

```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint
```
Expected: 에러 0

```bash
git add imstargg-frontend/src/components/shared/PageSkeleton.tsx "imstargg-frontend/app/players/[tag]/loading.tsx" "imstargg-frontend/app/clubs/[tag]/loading.tsx" "imstargg-frontend/app/brawlers/[id]/loading.tsx" "imstargg-frontend/app/events/[id]/loading.tsx"
git commit -m "feat(frontend): 상세 4개 세그먼트에 loading.tsx 스켈레톤 추가"
```

---

### Task 11: 목록 페이지 캐싱 정책 통일

**Files:**
- Modify: `imstargg-frontend/app/events/page.tsx:23`
- Modify: `imstargg-frontend/app/players/page.tsx:34`
- Modify: `imstargg-frontend/app/clubs/page.tsx:34`
- Modify: `imstargg-frontend/app/brawlers/page.tsx:25-26`

- [ ] **Step 1: no-store → revalidate 교체**

- `app/events/page.tsx`: `eventApi.rotation({ cache: "no-store" })` → `eventApi.rotation({ next: { revalidate: CACHE_SECONDS.SHORT } })`
- `app/players/page.tsx`: `rankingApi.players(country, { cache: "no-store" })` → `rankingApi.players(country, { next: { revalidate: CACHE_SECONDS.SHORT } })` (파일 상단에 `import { CACHE_SECONDS } from "@/config/cache";` 추가)
- `app/clubs/page.tsx`: `rankingApi.clubs(country, { cache: "no-store" })` → 동일 패턴 (import 추가)
- 검색 페이지(`players/search`, `clubs/search`)는 손대지 않는다.

- [ ] **Step 2: brawlers 목록의 dead route config 제거**

`app/brawlers/page.tsx` 에서 아래 두 줄을 삭제 (searchParams 를 읽는 동적 라우트라 무효):
```tsx
// Primary brawler list can cache reference data, but statistics use a shorter fetch cache.
export const revalidate = 60;
```

- [ ] **Step 3: 프로덕션 빌드로 확인 후 커밋**

**프로덕션 빌드** 후 라우트 테이블 확인:
- `/events` 가 `○` 또는 `●`(Revalidate 1m) 로 정적 전환
- `/players` `/clubs` `/brawlers` 는 ƒ 유지 (searchParams 라우트 — 의도됨)

```bash
git add imstargg-frontend/app/events/page.tsx imstargg-frontend/app/players/page.tsx imstargg-frontend/app/clubs/page.tsx imstargg-frontend/app/brawlers/page.tsx
git commit -m "refactor(frontend): 목록 페이지 no-store 를 revalidate 캐시로 통일"
```

---

### Task 12: 폰트 서브셋 스크립트 + 산출물 생성

**Files:**
- Create: `imstargg-frontend/scripts/subset_fonts.py`
- Create: `imstargg-frontend/app/fonts/KoBrawlGothic-subset.woff2` (생성물)
- Create: `imstargg-frontend/app/fonts/PretendardVariable-subset.woff2` (생성물)
- Move: `imstargg-frontend/public/fonts/KoBrawl_Gothic40.otf` → `imstargg-frontend/scripts/fonts-src/`
- Modify: `imstargg-frontend/package.json` (pretendard devDependency)

- [ ] **Step 1: pretendard 패키지 추가 + 원본 이동**

```bash
pnpm --dir imstargg-frontend add -D pretendard
mkdir -p imstargg-frontend/scripts/fonts-src
git mv imstargg-frontend/public/fonts/KoBrawl_Gothic40.otf imstargg-frontend/scripts/fonts-src/KoBrawl_Gothic40.otf
ls imstargg-frontend/node_modules/pretendard/dist/web/variable/woff2/
```
Expected: `PretendardVariable.woff2` 가 목록에 보임. (경로가 다르면 `find imstargg-frontend/node_modules/pretendard -name "PretendardVariable.woff2"` 로 실제 경로를 찾아 Step 2 스크립트의 경로를 맞춘다.)

- [ ] **Step 2: 서브셋 스크립트 작성**

`imstargg-frontend/scripts/subset_fonts.py`:
```python
"""KoBrawl·Pretendard 를 KS X 1001 한글 + Latin 서브셋 WOFF2 로 변환한다.

실행 (imstargg-frontend 디렉토리에서):
    uv run --with fonttools --with brotli python scripts/subset_fonts.py

산출물은 app/fonts/ 에 커밋한다. 원본: scripts/fonts-src/(KoBrawl),
node_modules/pretendard/(Pretendard — devDependency).
"""
from pathlib import Path

from fontTools.subset import Options, Subsetter, load_font, save_font

ROOT = Path(__file__).resolve().parent.parent


def ksx1001_hangul() -> set[int]:
    """KS X 1001 완성형 한글 음절 2,350자 — EUC-KR 인코딩 가능 여부로 판별."""
    result: set[int] = set()
    for cp in range(0xAC00, 0xD7A4):
        try:
            chr(cp).encode("euc_kr")
            result.add(cp)
        except UnicodeEncodeError:
            pass
    return result


def target_unicodes() -> list[int]:
    cps = ksx1001_hangul()
    cps.update(range(0x0020, 0x007F))  # Basic Latin
    cps.update(range(0x3131, 0x3164))  # 한글 호환 자모
    cps.update(
        [0x00B7, 0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x00D7, 0x2192]
    )  # 자주 쓰는 문장부호
    return sorted(cps)


def subset(src: Path, dst: Path) -> None:
    options = Options()
    options.flavor = "woff2"
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    font = load_font(str(src), options)
    subsetter = Subsetter(options)
    subsetter.populate(unicodes=target_unicodes())
    subsetter.subset(font)
    save_font(font, str(dst), options)
    print(f"{dst.name}: {dst.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    hangul_count = len(ksx1001_hangul())
    assert hangul_count == 2350, f"KS X 1001 음절 수가 2,350 이 아님: {hangul_count}"
    out = ROOT / "app" / "fonts"
    out.mkdir(parents=True, exist_ok=True)
    subset(
        ROOT / "scripts" / "fonts-src" / "KoBrawl_Gothic40.otf",
        out / "KoBrawlGothic-subset.woff2",
    )
    subset(
        ROOT
        / "node_modules"
        / "pretendard"
        / "dist"
        / "web"
        / "variable"
        / "woff2"
        / "PretendardVariable.woff2",
        out / "PretendardVariable-subset.woff2",
    )
```

- [ ] **Step 3: 실행 및 크기 확인**

```bash
cd imstargg-frontend && uv run --with fonttools --with brotli python scripts/subset_fonts.py; cd ..
ls -la imstargg-frontend/app/fonts/
```
Expected: 두 woff2 생성. `KoBrawlGothic-subset.woff2` **≤ 250KB** (스펙 성공 기준 3). Pretendard 는 수백 KB 수준이면 정상 (크기를 기록해둔다). 250KB 를 초과하면 `options.layout_features = []` 로 재시도하고 그래도 초과하면 결과를 보고하고 진행 여부를 확인한다.

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/scripts/subset_fonts.py imstargg-frontend/scripts/fonts-src/ imstargg-frontend/app/fonts/ imstargg-frontend/package.json imstargg-frontend/pnpm-lock.yaml
git commit -m "feat(frontend): KoBrawl·Pretendard KS X 1001 서브셋 WOFF2 생성 파이프라인"
```

---

### Task 13: next/font 전환 (fonts.ts + layout + globals.css)

**Files:**
- Create: `imstargg-frontend/app/fonts.ts`
- Modify: `imstargg-frontend/app/layout.tsx`
- Modify: `imstargg-frontend/app/globals.css`
- Delete: `imstargg-frontend/public/fonts/lilitaone-regular-webfont__2_.ttf`

- [ ] **Step 1: app/fonts.ts 작성**

```ts
import { Bricolage_Grotesque, JetBrains_Mono, Lilita_One } from "next/font/google";
import localFont from "next/font/local";

export const bricolage = Bricolage_Grotesque({
  subsets: ["latin"],
  axes: ["opsz"],
  variable: "--font-bricolage",
  display: "swap",
});

export const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "700"],
  variable: "--font-jetbrains",
  display: "swap",
});

export const lilitaOne = Lilita_One({
  subsets: ["latin"],
  weight: "400",
  variable: "--font-lilita",
  display: "swap",
});

export const koBrawl = localFont({
  src: "./fonts/KoBrawlGothic-subset.woff2",
  weight: "400",
  variable: "--font-kobrawl",
  display: "swap",
});

export const pretendard = localFont({
  src: "./fonts/PretendardVariable-subset.woff2",
  weight: "45 920",
  variable: "--font-pretendard",
  display: "swap",
});
```

- [ ] **Step 2: layout.tsx 에 변수 주입**

`app/layout.tsx` 에 import 추가:
```tsx
import { bricolage, jetbrainsMono, koBrawl, lilitaOne, pretendard } from "./fonts";
```
`<html lang="ko">` 를 다음으로 교체:
```tsx
<html
  lang="ko"
  className={`${koBrawl.variable} ${lilitaOne.variable} ${pretendard.variable} ${bricolage.variable} ${jetbrainsMono.variable}`}
>
```

- [ ] **Step 3: globals.css 정리**

`app/globals.css` 에서:
1. 1~3행의 원격 `@import url('https://fonts.googleapis.com/...')` ×2 와 `@import url('https://cdn.jsdelivr.net/...pretendardvariable.css')` 를 삭제 (`@import "tailwindcss";` 가 첫 줄이 되게).
2. 9~22행의 로컬 `@font-face` 블록 2개(KoBrawl, Lilita One)를 삭제.
3. `@theme` 블록(약 58~62행)과 `:root` 쪽 중복 정의(약 179~183행) **두 곳 모두**의 폰트 토큰을 다음으로 교체:
```css
  --font-display: var(--font-kobrawl), var(--font-lilita), var(--font-pretendard), system-ui, sans-serif;
  --font-title: var(--font-kobrawl), var(--font-lilita), var(--font-pretendard), system-ui, sans-serif;
  --font-body: var(--font-pretendard), -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  --font-num: var(--font-bricolage), var(--font-pretendard), system-ui, sans-serif;
  --font-mono: var(--font-jetbrains), ui-monospace, monospace;
```
4. Lilita TTF 삭제:
```bash
git rm imstargg-frontend/public/fonts/lilitaone-regular-webfont__2_.ttf
```
(`public/fonts/` 가 비면 디렉토리도 사라진다 — 정상.)

- [ ] **Step 4: 검증 — 빌드 산출물에서 외부 폰트 0건 + preload 존재**

**프로덕션 빌드** 후:
```bash
grep -rl "fonts.googleapis.com\|cdn.jsdelivr.net" imstargg-frontend/.next/static/css/ ; echo "exit=$?"
grep -c 'as="font"' imstargg-frontend/.next/server/app/index.html
```
Expected: 첫 grep 은 매치 없음(exit=1), 둘째 grep 은 1 이상 (폰트 preload 링크 존재). 시각 검증용 dev 서버로 홈·플레이어·브롤러 페이지를 열어 KoBrawl 타이틀/Pretendard 본문/Bricolage 숫자가 적용됐는지 스크린샷으로 확인한다.

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-frontend/app imstargg-frontend/public
git commit -m "feat(frontend): 폰트를 next/font 셀프호스팅으로 전환 — 외부 CSS 체인 제거"
```

---

### Task 14: 최종 검증 (스펙 성공 기준 전체)

**Files:** (수정 없음 — 검증 전용)

- [ ] **Step 1: 전체 테스트·린트·타입**

```bash
pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint && pnpm --dir imstargg-frontend test:run
```
Expected: 전부 통과 (기준 6)

- [ ] **Step 2: 프로덕션 빌드 기준 확인**

**프로덕션 빌드** 후 라우트 테이블에서:
- [ ] `/brawlers/[id]` `●` + Revalidate 1m + First Load JS **≤ 200KB** (기준 1·2)
- [ ] `/events/[id]` `●` + Revalidate 1m (기준 1)
- [ ] `/events` 정적(○/●) (WS④)
- [ ] prerender-manifest 의 `/brawlers/*` 경로 수 ≥ 100 (Task 6 Step 4 의 python 명령 재사용, 기준 1)
- [ ] 외부 폰트 도메인 grep 0건 (Task 13 Step 4 재실행, 기준 3)
- [ ] `ls -la imstargg-frontend/app/fonts/` 에서 KoBrawl ≤ 250KB (기준 3)

- [ ] **Step 3: 필터 shallow 동작 확인 (기준 4)**

시각 검증용 dev 서버 기동 후 playwright MCP 로:
1. `/brawlers/16000000` 접속 → 네트워크 기록 시작 → 티어 필터 변경
2. 문서/RSC 요청(`?_rsc=` 포함 요청)이 **0건**, `/proxy-api/v1/statistics/*` XHR 만 발생하는지 확인
3. `?tier=TROPHY_3000_PLUS` 딥링크 직접 접속 → 기본값 표가 잠깐 보인 뒤 필터 값으로 교체되는지 확인 (의도된 트레이드오프)
4. 이벤트 상세에서 브롤러 행 클릭 → 선택 패널 열림 + URL `brawlerId` 반영 확인

- [ ] **Step 4: 시각 회귀 확인**

playwright MCP 로 1280×800 / 390×844 스크린샷: 홈, `/brawlers`(목록 필터가 여전히 서버 내비게이션으로 동작하는지 포함), 브롤러 상세(아이템 트로피 필터·차트 lazy 로딩), 이벤트 상세, 플레이어 상세(스켈레톤 → 배틀 목록 즉시 표시), 클럽 상세. 레이아웃 깨짐·폰트 미적용·희귀 음절 닉네임 폴백(플레이어 이름에 특수문자 포함 사례)을 육안 확인.

- [ ] **Step 5: 스펙 성공 기준표 대조 후 종료 보고**

`docs/superpowers/specs/2026-07-07-performance-design.md` 의 기준 1~6 을 하나씩 체크하고, 측정값(First Load JS, 폰트 크기, 정적 경로 수)을 기록해 보고한다.
