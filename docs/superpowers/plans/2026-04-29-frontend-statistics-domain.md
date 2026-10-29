# Frontend Statistics Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement statistics domain — fill placeholder areas in `/brawlers`, `/brawlers/[id]`, `/events/[id]` with 7 backend statistics endpoints. No new top-level routes.

**Architecture:** RSC + ISR (`revalidate: 600`) for all statistics fetches. URL query params for all filters (`tier`, `date`, `trophy`, `brawlerId`, `sort`, `order`, `pairSort`, `pairOrder`, `enemySort`, `enemyOrder`). Single-class-per-file. TDD for pure logic (parsers, mappers, tier mapping). `mode.resultType` drives RESULT/RANK branching on event detail.

**Tech Stack:** Next.js 16 App Router, React 19, TypeScript, Vitest, Testing Library, Playwright (E2E), shadcn/ui (Select, Card, Chart), recharts, Tailwind.

## Conventions

- **Working dir**: All `pnpm` commands run from `imstargg-frontend/`.
- **Commit messages**: Korean. Trailer: `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. Use HEREDOC.
- **Staging**: Never `git add -A`. Stage explicit files only.
- **File-per-class**: One exported component/type group per file (AGENTS.md). Domain enum bundles (e.g., 3 closely related enums + their labels + parsers) live in one `enums.ts` file as a cohesive unit.
- **Test placement**: Vitest config matches `tests/**/*.test.{ts,tsx}` only. Place all unit/component tests under `tests/unit/...`. Do NOT use `src/__tests__/` (Vitest will not discover them).
- **Import alias**: `@` → `./src`, `@app` → `./app`. Use `@/...` for `src/`, `@app/...` for `app/`.
- **`Brawler` reuse**: Statistics responses include nested `BrawlerResponse`. Reuse `@/domain/brawler/mappers#toBrawler` — do NOT introduce a `BrawlerLite` type.
- **Number formatting**: All rate values are `0..1` from backend. UI shows `(value * 100).toFixed(1) + "%"`. `averageRank` shows `value.toFixed(2)`.

---

## File Structure

### Created
```
imstargg-frontend/
├── src/
│   ├── api/endpoints/statistics.ts
│   ├── components/ui/chart.tsx                  # shadcn install
│   └── domain/statistics/
│       ├── enums.ts
│       ├── sort.ts
│       ├── tier.ts
│       ├── types.ts
│       └── mappers.ts
├── app/
│   ├── _components/statistics/
│   │   ├── TierBadge.tsx
│   │   ├── StatRangeSelector.tsx
│   │   ├── TrophyRangeSelector.tsx
│   │   ├── SortableHeader.tsx
│   │   └── StatNumberCell.tsx
│   ├── brawlers/_components/
│   │   ├── BrawlerStatTable.tsx
│   │   ├── BrawlerStatRow.tsx
│   │   └── BrawlerListHeader.tsx
│   ├── brawlers/[id]/_components/
│   │   ├── BrawlerStatsSection.tsx
│   │   ├── DailyTrendChart.tsx
│   │   ├── PairStatsTable.tsx
│   │   ├── EnemyStatsTable.tsx
│   │   └── ItemOwnershipSection.tsx
│   └── events/[id]/_components/
│       ├── EventStatsSection.tsx
│       ├── EventResultStatsTable.tsx
│       ├── EventRankStatsTable.tsx
│       └── BrawlerSelectedPanel.tsx
└── tests/
    ├── unit/domain/statistics/
    │   ├── enums.test.ts
    │   ├── sort.test.ts
    │   ├── tier.test.ts
    │   └── mappers.test.ts
    ├── unit/components/statistics/
    │   ├── TierBadge.test.tsx
    │   ├── StatRangeSelector.test.tsx
    │   └── SortableHeader.test.tsx
    └── e2e/statistics.spec.ts
```

### Modified
```
src/api/types.ts                                 # 7 statistics DTOs + sortBy enums
src/hooks/queries/queryKeys.ts                   # statistics keys (for future useQuery)
package.json                                     # recharts dependency
app/brawlers/page.tsx                            # rewrite as table-style
app/brawlers/[id]/page.tsx                       # mount BrawlerStatsSection
app/brawlers/[id]/_components/BrawlerContent.tsx # render statistics section
app/events/[id]/page.tsx                         # mount EventStatsSection
app/events/[id]/_components/EventContent.tsx     # render statistics section
```

The `BrawlersListPage` component in `app/brawlers/_components/` is **deleted** in Task 13 (rewrite to table form).

---

## Task 1: recharts + shadcn chart 설치

**Files:**
- Modify: `imstargg-frontend/package.json`
- Create: `imstargg-frontend/src/components/ui/chart.tsx`

- [ ] **Step 1: recharts 설치**

```bash
cd imstargg-frontend && pnpm add recharts
```

Expected: `recharts` 가 `dependencies` 에 추가됨.

- [ ] **Step 2: shadcn chart 컴포넌트 추가**

```bash
cd imstargg-frontend && pnpm dlx shadcn@latest add chart
```

Expected: `src/components/ui/chart.tsx` 생성, `recharts` 와 디자인 토큰(`--chart-1` 등)이 globals.css 에 추가될 수 있음. CLI 가 globals.css 변경 제안 시 그대로 수용.

- [ ] **Step 3: typecheck 통과 확인**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 4: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/package.json imstargg-frontend/pnpm-lock.yaml imstargg-frontend/src/components/ui/chart.tsx imstargg-frontend/app/globals.css 2>/dev/null || true
git commit -m "$(cat <<'EOF'
chore(frontend): recharts + shadcn chart 컴포넌트 추가

statistics 도메인의 일별 트렌드 차트용 의존성 추가.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Statistics DTO 타입 + queryKeys

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts`
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`

- [ ] **Step 1: DTO 타입 추가** — `src/api/types.ts` 끝에 추가

```ts
// === Statistics ===
export type StatsTierRangeDto =
  | "TROPHY_1000_PLUS" | "TROPHY_2000_PLUS" | "TROPHY_3000_PLUS"
  | "SOLO_RANK_MYTHIC_PLUS" | "SOLO_RANK_LEGENDARY_PLUS" | "SOLO_RANK_MASTER_PLUS";

export type StatsDateRangeDto =
  | "THIS_WEEK" | "SINCE_LAST_WEEK" | "LAST_WEEK"
  | "SINCE_TWO_WEEKS_AGO" | "TWO_WEEKS_AGO"
  | "THIS_MONTH" | "SINCE_LAST_MONTH" | "LAST_MONTH";

export type BrawlerTrophyRangeDto =
  | "TROPHY_500_PLUS" | "TROPHY_1000_PLUS" | "TROPHY_2000_PLUS" | "TROPHY_3000_PLUS";

export type ResultStatsSortByDto = "PICK_RATE" | "WIN_RATE";
export type RankStatsSortByDto = "PICK_RATE" | "AVERAGE_RANK";
export type SortOrderDto = "ASC" | "DESC";

export interface BrawlerResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
  starPlayerRate: number;
  tierScore: number;
  tier: string; // "S" | "A" | "B" | "C" | "D"
}

export interface BrawlerResultStatisticsResponse {
  totalBattleCount: number;
  items: BrawlerResultStatisticsItemResponse[];
}

export interface BrawlerRankStatisticsResponse {
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface BrawlerPairResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  pairBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface BrawlerPairRankStatisticsResponse {
  brawler: BrawlerResponse | null;
  pairBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface BrawlerEnemyResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  enemyBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface BrawlerResultDailyStatisticsResponse {
  date: string; // ISO yyyy-MM-dd
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
}

export interface BrawlerItemOwnershipStatisticsItemResponse {
  itemId: number;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BrawlerBuffyOwnershipStatisticsItemResponse {
  buffyType: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BrawlerItemOwnershipStatisticsResponse {
  gears: BrawlerItemOwnershipStatisticsItemResponse[];
  gadgets: BrawlerItemOwnershipStatisticsItemResponse[];
  starPowers: BrawlerItemOwnershipStatisticsItemResponse[];
  hyperCharges: BrawlerItemOwnershipStatisticsItemResponse[];
  buffies: BrawlerBuffyOwnershipStatisticsItemResponse[];
}
```

- [ ] **Step 2: queryKeys 추가** — `src/hooks/queries/queryKeys.ts` 의 `queryKeys` 객체 안에 마지막 항목으로 추가

```ts
  statistics: {
    all: ["statistics"] as const,
    brawlerResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "brawler-result", params] as const,
    brawlerRank: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "brawler-rank", params] as const,
    pairResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "pair-result", params] as const,
    pairRank: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "pair-rank", params] as const,
    enemyResult: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "enemy-result", params] as const,
    daily: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "daily", params] as const,
    itemOwnership: (params: Record<string, unknown>) =>
      [...queryKeys.statistics.all, "item-ownership", params] as const,
  },
```

- [ ] **Step 3: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 4: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/api/types.ts imstargg-frontend/src/hooks/queries/queryKeys.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics DTO 타입과 queryKeys 추가

7개 statistics 엔드포인트 응답 타입과 향후 useQuery 용 queryKeys 정의.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: enums (TierRange/DateRange/TrophyRange + parsers)

**Files:**
- Create: `imstargg-frontend/src/domain/statistics/enums.ts`
- Create: `imstargg-frontend/tests/unit/domain/statistics/enums.test.ts`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/domain/statistics/enums.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  BRAWLER_TROPHY_RANGES,
  TIER_RANGE_LABELS,
  DATE_RANGE_LABELS,
  TROPHY_RANGE_LABELS,
  parseTierRange,
  parseDateRange,
  parseTrophyRange,
} from "@/domain/statistics/enums";

describe("STATS_TIER_RANGES", () => {
  it("6개 tier range 를 가진다", () => {
    expect(STATS_TIER_RANGES).toEqual([
      "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
      "SOLO_RANK_MYTHIC_PLUS", "SOLO_RANK_LEGENDARY_PLUS", "SOLO_RANK_MASTER_PLUS",
    ]);
  });
});

describe("STATS_DATE_RANGES", () => {
  it("8개 date range 를 가진다", () => {
    expect(STATS_DATE_RANGES).toHaveLength(8);
    expect(STATS_DATE_RANGES).toContain("SINCE_LAST_WEEK");
  });
});

describe("BRAWLER_TROPHY_RANGES", () => {
  it("4개 trophy range 를 가진다", () => {
    expect(BRAWLER_TROPHY_RANGES).toEqual([
      "TROPHY_500_PLUS", "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
    ]);
  });
});

describe("LABELS", () => {
  it("모든 tier range 에 한글 라벨이 있다", () => {
    for (const r of STATS_TIER_RANGES) {
      expect(TIER_RANGE_LABELS[r]).toBeTruthy();
    }
  });
  it("모든 date range 에 한글 라벨이 있다", () => {
    for (const r of STATS_DATE_RANGES) {
      expect(DATE_RANGE_LABELS[r]).toBeTruthy();
    }
  });
  it("모든 trophy range 에 한글 라벨이 있다", () => {
    for (const r of BRAWLER_TROPHY_RANGES) {
      expect(TROPHY_RANGE_LABELS[r]).toBeTruthy();
    }
  });
});

describe("parseTierRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseTierRange("TROPHY_2000_PLUS")).toBe("TROPHY_2000_PLUS");
  });
  it("대소문자가 섞여도 정규화한다", () => {
    expect(parseTierRange("solo_rank_mythic_plus")).toBe("SOLO_RANK_MYTHIC_PLUS");
  });
  it("알 수 없는 값은 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTierRange("FOO")).toBe("TROPHY_1000_PLUS");
  });
  it("undefined 는 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTierRange(undefined)).toBe("TROPHY_1000_PLUS");
  });
});

describe("parseDateRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseDateRange("LAST_WEEK")).toBe("LAST_WEEK");
  });
  it("알 수 없는 값은 SINCE_LAST_WEEK 로 fallback", () => {
    expect(parseDateRange("FOO")).toBe("SINCE_LAST_WEEK");
  });
  it("undefined 는 SINCE_LAST_WEEK 로 fallback", () => {
    expect(parseDateRange(undefined)).toBe("SINCE_LAST_WEEK");
  });
});

describe("parseTrophyRange", () => {
  it("정상 값을 그대로 반환한다", () => {
    expect(parseTrophyRange("TROPHY_500_PLUS")).toBe("TROPHY_500_PLUS");
  });
  it("알 수 없는 값은 TROPHY_1000_PLUS 로 fallback", () => {
    expect(parseTrophyRange("FOO")).toBe("TROPHY_1000_PLUS");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/enums.test.ts
```

Expected: FAIL — `Cannot find module '@/domain/statistics/enums'`.

- [ ] **Step 3: enums 구현**

`src/domain/statistics/enums.ts`:

```ts
export const STATS_TIER_RANGES = [
  "TROPHY_1000_PLUS",
  "TROPHY_2000_PLUS",
  "TROPHY_3000_PLUS",
  "SOLO_RANK_MYTHIC_PLUS",
  "SOLO_RANK_LEGENDARY_PLUS",
  "SOLO_RANK_MASTER_PLUS",
] as const;
export type StatsTierRange = (typeof STATS_TIER_RANGES)[number];

export const STATS_DATE_RANGES = [
  "THIS_WEEK",
  "SINCE_LAST_WEEK",
  "LAST_WEEK",
  "SINCE_TWO_WEEKS_AGO",
  "TWO_WEEKS_AGO",
  "THIS_MONTH",
  "SINCE_LAST_MONTH",
  "LAST_MONTH",
] as const;
export type StatsDateRange = (typeof STATS_DATE_RANGES)[number];

export const BRAWLER_TROPHY_RANGES = [
  "TROPHY_500_PLUS",
  "TROPHY_1000_PLUS",
  "TROPHY_2000_PLUS",
  "TROPHY_3000_PLUS",
] as const;
export type BrawlerTrophyRange = (typeof BRAWLER_TROPHY_RANGES)[number];

export const TIER_RANGE_LABELS: Record<StatsTierRange, string> = {
  TROPHY_1000_PLUS: "트로피 1,000+",
  TROPHY_2000_PLUS: "트로피 2,000+",
  TROPHY_3000_PLUS: "트로피 3,000+",
  SOLO_RANK_MYTHIC_PLUS: "미식+",
  SOLO_RANK_LEGENDARY_PLUS: "레전드+",
  SOLO_RANK_MASTER_PLUS: "마스터+",
};

export const DATE_RANGE_LABELS: Record<StatsDateRange, string> = {
  THIS_WEEK: "이번 주",
  SINCE_LAST_WEEK: "지난 주부터",
  LAST_WEEK: "지난 주",
  SINCE_TWO_WEEKS_AGO: "2주 전부터",
  TWO_WEEKS_AGO: "2주 전",
  THIS_MONTH: "이번 달",
  SINCE_LAST_MONTH: "지난 달부터",
  LAST_MONTH: "지난 달",
};

export const TROPHY_RANGE_LABELS: Record<BrawlerTrophyRange, string> = {
  TROPHY_500_PLUS: "500+",
  TROPHY_1000_PLUS: "1,000+",
  TROPHY_2000_PLUS: "2,000+",
  TROPHY_3000_PLUS: "3,000+",
};

export const DEFAULT_TIER_RANGE: StatsTierRange = "TROPHY_1000_PLUS";
export const DEFAULT_DATE_RANGE: StatsDateRange = "SINCE_LAST_WEEK";
export const DEFAULT_TROPHY_RANGE: BrawlerTrophyRange = "TROPHY_1000_PLUS";

function parseEnum<T extends string>(
  value: unknown,
  values: readonly T[],
  fallback: T,
): T {
  if (typeof value !== "string") return fallback;
  const normalized = value.trim().toUpperCase();
  return (values as readonly string[]).includes(normalized) ? (normalized as T) : fallback;
}

export function parseTierRange(value: unknown): StatsTierRange {
  return parseEnum(value, STATS_TIER_RANGES, DEFAULT_TIER_RANGE);
}

export function parseDateRange(value: unknown): StatsDateRange {
  return parseEnum(value, STATS_DATE_RANGES, DEFAULT_DATE_RANGE);
}

export function parseTrophyRange(value: unknown): BrawlerTrophyRange {
  return parseEnum(value, BRAWLER_TROPHY_RANGES, DEFAULT_TROPHY_RANGE);
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/enums.test.ts
```

Expected: PASS — 모든 테스트 통과.

- [ ] **Step 5: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/statistics/enums.ts imstargg-frontend/tests/unit/domain/statistics/enums.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics enum 타입과 parser 추가

TierRange/DateRange/TrophyRange + 한글 라벨 + 안전 fallback parser.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: sort enums (SortBy/Order + parsers)

**Files:**
- Create: `imstargg-frontend/src/domain/statistics/sort.ts`
- Create: `imstargg-frontend/tests/unit/domain/statistics/sort.test.ts`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/domain/statistics/sort.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import {
  parseResultSortBy,
  parseRankSortBy,
  parseSortOrder,
} from "@/domain/statistics/sort";

describe("parseResultSortBy", () => {
  it("PICK_RATE 를 그대로 반환", () => {
    expect(parseResultSortBy("PICK_RATE")).toBe("PICK_RATE");
  });
  it("WIN_RATE 를 그대로 반환", () => {
    expect(parseResultSortBy("WIN_RATE")).toBe("WIN_RATE");
  });
  it("소문자도 정규화", () => {
    expect(parseResultSortBy("win_rate")).toBe("WIN_RATE");
  });
  it("알 수 없는 값은 PICK_RATE 로 fallback", () => {
    expect(parseResultSortBy("FOO")).toBe("PICK_RATE");
  });
  it("AVERAGE_RANK 는 결과형에 없으므로 PICK_RATE 로 fallback", () => {
    expect(parseResultSortBy("AVERAGE_RANK")).toBe("PICK_RATE");
  });
});

describe("parseRankSortBy", () => {
  it("AVERAGE_RANK 를 그대로 반환", () => {
    expect(parseRankSortBy("AVERAGE_RANK")).toBe("AVERAGE_RANK");
  });
  it("PICK_RATE 를 그대로 반환", () => {
    expect(parseRankSortBy("PICK_RATE")).toBe("PICK_RATE");
  });
  it("알 수 없는 값은 PICK_RATE 로 fallback", () => {
    expect(parseRankSortBy("FOO")).toBe("PICK_RATE");
  });
});

describe("parseSortOrder", () => {
  it("ASC 를 그대로 반환", () => {
    expect(parseSortOrder("ASC")).toBe("ASC");
  });
  it("desc 도 DESC 로 정규화", () => {
    expect(parseSortOrder("desc")).toBe("DESC");
  });
  it("알 수 없는 값은 DESC 로 fallback", () => {
    expect(parseSortOrder("FOO")).toBe("DESC");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/sort.test.ts
```

Expected: FAIL — `Cannot find module '@/domain/statistics/sort'`.

- [ ] **Step 3: 구현**

`src/domain/statistics/sort.ts`:

```ts
export const RESULT_SORT_BYS = ["PICK_RATE", "WIN_RATE"] as const;
export type ResultStatsSortBy = (typeof RESULT_SORT_BYS)[number];

export const RANK_SORT_BYS = ["PICK_RATE", "AVERAGE_RANK"] as const;
export type RankStatsSortBy = (typeof RANK_SORT_BYS)[number];

export const SORT_ORDERS = ["ASC", "DESC"] as const;
export type SortOrder = (typeof SORT_ORDERS)[number];

export const RESULT_SORT_LABELS: Record<ResultStatsSortBy, string> = {
  PICK_RATE: "픽률",
  WIN_RATE: "승률",
};

export const RANK_SORT_LABELS: Record<RankStatsSortBy, string> = {
  PICK_RATE: "픽률",
  AVERAGE_RANK: "평균 순위",
};

function parseEnum<T extends string>(
  value: unknown,
  values: readonly T[],
  fallback: T,
): T {
  if (typeof value !== "string") return fallback;
  const normalized = value.trim().toUpperCase();
  return (values as readonly string[]).includes(normalized) ? (normalized as T) : fallback;
}

export function parseResultSortBy(value: unknown): ResultStatsSortBy {
  return parseEnum(value, RESULT_SORT_BYS, "PICK_RATE");
}

export function parseRankSortBy(value: unknown): RankStatsSortBy {
  return parseEnum(value, RANK_SORT_BYS, "PICK_RATE");
}

export function parseSortOrder(value: unknown): SortOrder {
  return parseEnum(value, SORT_ORDERS, "DESC");
}

export function toggleSortOrder(order: SortOrder): SortOrder {
  return order === "ASC" ? "DESC" : "ASC";
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/sort.test.ts
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/statistics/sort.ts imstargg-frontend/tests/unit/domain/statistics/sort.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics sort enum 과 parser 추가

ResultStatsSortBy, RankStatsSortBy, SortOrder + 한글 라벨 + parser.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: tier (StatTier + 컬러 + parseTier)

**Files:**
- Create: `imstargg-frontend/src/domain/statistics/tier.ts`
- Create: `imstargg-frontend/tests/unit/domain/statistics/tier.test.ts`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/domain/statistics/tier.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { STAT_TIERS, TIER_COLOR, parseTier } from "@/domain/statistics/tier";

describe("STAT_TIERS", () => {
  it("S/A/B/C/D 5단계", () => {
    expect(STAT_TIERS).toEqual(["S", "A", "B", "C", "D"]);
  });
});

describe("TIER_COLOR", () => {
  it("S 는 브랜드 옐로우", () => {
    expect(TIER_COLOR.S).toBe("#FFC60A");
  });
  it("모든 tier 에 컬러가 있다", () => {
    for (const t of STAT_TIERS) {
      expect(TIER_COLOR[t]).toMatch(/^#[0-9A-F]{6}$/i);
    }
  });
});

describe("parseTier", () => {
  it("S/A/B/C/D 를 그대로 반환", () => {
    expect(parseTier("S")).toBe("S");
    expect(parseTier("A")).toBe("A");
    expect(parseTier("D")).toBe("D");
  });
  it("소문자도 정규화", () => {
    expect(parseTier("b")).toBe("B");
  });
  it("알 수 없는 값은 D 로 fallback", () => {
    expect(parseTier("F")).toBe("D");
    expect(parseTier("foo")).toBe("D");
    expect(parseTier(undefined)).toBe("D");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/tier.test.ts
```

Expected: FAIL.

- [ ] **Step 3: 구현**

`src/domain/statistics/tier.ts`:

```ts
export const STAT_TIERS = ["S", "A", "B", "C", "D"] as const;
export type StatTier = (typeof STAT_TIERS)[number];

export const TIER_COLOR: Record<StatTier, string> = {
  S: "#FFC60A",
  A: "#FF3A3A",
  B: "#FF8A1F",
  C: "#2BD576",
  D: "#5C6271",
};

export function parseTier(value: unknown): StatTier {
  if (typeof value !== "string") return "D";
  const normalized = value.trim().toUpperCase();
  return (STAT_TIERS as readonly string[]).includes(normalized) ? (normalized as StatTier) : "D";
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/tier.test.ts
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/statistics/tier.ts imstargg-frontend/tests/unit/domain/statistics/tier.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics StatTier 와 컬러 매핑 추가

S/A/B/C/D 5단계 + DESIGN.md 컬러 매핑 + parseTier 안전 fallback.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: 도메인 타입 (types.ts)

**Files:**
- Create: `imstargg-frontend/src/domain/statistics/types.ts`

- [ ] **Step 1: 타입 정의**

`src/domain/statistics/types.ts`:

```ts
import type { Brawler } from "@/domain/brawler/types";
import type { StatTier } from "@/domain/statistics/tier";

export interface BrawlerResultStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
  starPlayerRate: number;
  tierScore: number;
  tier: StatTier;
}

export interface BrawlerResultStatistics {
  totalBattleCount: number;
  items: BrawlerResultStat[];
}

export interface BrawlerRankStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface PairResultStat {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface PairRankStat {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface EnemyResultStat {
  brawler: Brawler | null;
  enemyBrawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface DailyResultStat {
  date: string;
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
}

export interface ItemOwnershipItem {
  itemId: number;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BuffyOwnershipItem {
  buffyType: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface ItemOwnership {
  gears: ItemOwnershipItem[];
  gadgets: ItemOwnershipItem[];
  starPowers: ItemOwnershipItem[];
  hyperCharges: ItemOwnershipItem[];
  buffies: BuffyOwnershipItem[];
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/statistics/types.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics 도메인 타입 추가

7개 통계 응답에 대응하는 도메인 모델. 기존 Brawler 도메인 재사용.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 매퍼 (mappers.ts)

**Files:**
- Create: `imstargg-frontend/src/domain/statistics/mappers.ts`
- Create: `imstargg-frontend/tests/unit/domain/statistics/mappers.test.ts`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/domain/statistics/mappers.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import {
  toBrawlerResultStatistics,
  toBrawlerRankStat,
  toPairResultStat,
  toPairRankStat,
  toEnemyResultStat,
  toDailyResultStat,
  toItemOwnership,
} from "@/domain/statistics/mappers";

const fakeBrawler = (id: number) => ({
  id,
  name: { ko: `브롤러${id}` },
  rarity: "RARE" as const,
  role: "DAMAGE_DEALER" as const,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  portraitImageUrl: null,
  buffies: [],
});

describe("toBrawlerResultStatistics", () => {
  it("DTO 를 도메인으로 매핑한다", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 1000,
      items: [
        {
          brawler: fakeBrawler(16000000),
          totalBattleCount: 100,
          pickRate: 0.1,
          winRate: 0.55,
          starPlayerRate: 0.2,
          tierScore: 80,
          tier: "S",
        },
      ],
    });
    expect(result.totalBattleCount).toBe(1000);
    expect(result.items).toHaveLength(1);
    expect(result.items[0].brawler?.id).toBe(16000000);
    expect(result.items[0].tier).toBe("S");
  });

  it("brawler 가 null 이어도 처리한다", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 0,
      items: [
        {
          brawler: null,
          totalBattleCount: 5,
          pickRate: 0.01,
          winRate: 0.5,
          starPlayerRate: 0,
          tierScore: 0,
          tier: "D",
        },
      ],
    });
    expect(result.items[0].brawler).toBeNull();
  });

  it("알 수 없는 tier 는 D 로 fallback", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 0,
      items: [
        {
          brawler: null,
          totalBattleCount: 1,
          pickRate: 0,
          winRate: 0,
          starPlayerRate: 0,
          tierScore: 0,
          tier: "Z",
        },
      ],
    });
    expect(result.items[0].tier).toBe("D");
  });
});

describe("toBrawlerRankStat", () => {
  it("매핑한다", () => {
    const result = toBrawlerRankStat({
      brawler: fakeBrawler(16000001),
      totalBattleCount: 50,
      averageRank: 4.32,
    });
    expect(result.brawler?.id).toBe(16000001);
    expect(result.averageRank).toBe(4.32);
  });
});

describe("toPairResultStat", () => {
  it("매핑한다", () => {
    const result = toPairResultStat({
      brawler: fakeBrawler(1),
      pairBrawler: fakeBrawler(2),
      totalBattleCount: 100,
      pickRate: 0.05,
      winRate: 0.6,
    });
    expect(result.brawler?.id).toBe(1);
    expect(result.pairBrawler?.id).toBe(2);
  });
});

describe("toPairRankStat", () => {
  it("매핑한다", () => {
    const result = toPairRankStat({
      brawler: fakeBrawler(1),
      pairBrawler: fakeBrawler(2),
      totalBattleCount: 50,
      averageRank: 3.0,
    });
    expect(result.averageRank).toBe(3.0);
  });
});

describe("toEnemyResultStat", () => {
  it("매핑한다", () => {
    const result = toEnemyResultStat({
      brawler: fakeBrawler(1),
      enemyBrawler: fakeBrawler(3),
      totalBattleCount: 200,
      pickRate: 0.08,
      winRate: 0.4,
    });
    expect(result.enemyBrawler?.id).toBe(3);
  });
});

describe("toDailyResultStat", () => {
  it("매핑한다", () => {
    const result = toDailyResultStat({
      date: "2026-04-29",
      brawler: null,
      totalBattleCount: 100,
      winRate: 0.55,
      starPlayerRate: 0.2,
    });
    expect(result.date).toBe("2026-04-29");
    expect(result.winRate).toBe(0.55);
  });
});

describe("toItemOwnership", () => {
  it("5개 카테고리를 그대로 매핑", () => {
    const result = toItemOwnership({
      gears: [{ itemId: 1, ownershipRate: 0.5, ownerCount: 100, totalCount: 200 }],
      gadgets: [],
      starPowers: [],
      hyperCharges: [],
      buffies: [{ buffyType: "MYTHIC_GEAR", ownershipRate: 0.1, ownerCount: 10, totalCount: 100 }],
    });
    expect(result.gears).toHaveLength(1);
    expect(result.buffies).toHaveLength(1);
    expect(result.buffies[0].buffyType).toBe("MYTHIC_GEAR");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/mappers.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: 구현**

`src/domain/statistics/mappers.ts`:

```ts
import type {
  BrawlerEnemyResultStatisticsItemResponse,
  BrawlerItemOwnershipStatisticsResponse,
  BrawlerPairRankStatisticsResponse,
  BrawlerPairResultStatisticsItemResponse,
  BrawlerRankStatisticsResponse,
  BrawlerResultDailyStatisticsResponse,
  BrawlerResultStatisticsItemResponse,
  BrawlerResultStatisticsResponse,
} from "@/api/types";
import { toBrawler } from "@/domain/brawler/mappers";
import type {
  BrawlerRankStat,
  BrawlerResultStat,
  BrawlerResultStatistics,
  DailyResultStat,
  EnemyResultStat,
  ItemOwnership,
  PairRankStat,
  PairResultStat,
} from "@/domain/statistics/types";
import { parseTier } from "@/domain/statistics/tier";

function toBrawlerResultStat(dto: BrawlerResultStatisticsItemResponse): BrawlerResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
    starPlayerRate: dto.starPlayerRate,
    tierScore: dto.tierScore,
    tier: parseTier(dto.tier),
  };
}

export function toBrawlerResultStatistics(
  dto: BrawlerResultStatisticsResponse,
): BrawlerResultStatistics {
  return {
    totalBattleCount: dto.totalBattleCount,
    items: dto.items.map(toBrawlerResultStat),
  };
}

export function toBrawlerRankStat(dto: BrawlerRankStatisticsResponse): BrawlerRankStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    totalBattleCount: dto.totalBattleCount,
    averageRank: dto.averageRank,
  };
}

export function toPairResultStat(
  dto: BrawlerPairResultStatisticsItemResponse,
): PairResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    pairBrawler: dto.pairBrawler ? toBrawler(dto.pairBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
  };
}

export function toPairRankStat(
  dto: BrawlerPairRankStatisticsResponse,
): PairRankStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    pairBrawler: dto.pairBrawler ? toBrawler(dto.pairBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    averageRank: dto.averageRank,
  };
}

export function toEnemyResultStat(
  dto: BrawlerEnemyResultStatisticsItemResponse,
): EnemyResultStat {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    enemyBrawler: dto.enemyBrawler ? toBrawler(dto.enemyBrawler) : null,
    totalBattleCount: dto.totalBattleCount,
    pickRate: dto.pickRate,
    winRate: dto.winRate,
  };
}

export function toDailyResultStat(
  dto: BrawlerResultDailyStatisticsResponse,
): DailyResultStat {
  return {
    date: dto.date,
    totalBattleCount: dto.totalBattleCount,
    winRate: dto.winRate,
    starPlayerRate: dto.starPlayerRate,
  };
}

export function toItemOwnership(
  dto: BrawlerItemOwnershipStatisticsResponse,
): ItemOwnership {
  return {
    gears: dto.gears,
    gadgets: dto.gadgets,
    starPowers: dto.starPowers,
    hyperCharges: dto.hyperCharges,
    buffies: dto.buffies,
  };
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/domain/statistics/mappers.test.ts
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/statistics/mappers.ts imstargg-frontend/tests/unit/domain/statistics/mappers.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statistics 도메인 매퍼 추가

7개 응답 → 도메인 모델 변환. brawler 는 null/존재 둘 다 처리, tier 는 안전 parser.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: statisticsApi 엔드포인트

**Files:**
- Create: `imstargg-frontend/src/api/endpoints/statistics.ts`

- [ ] **Step 1: 구현**

`src/api/endpoints/statistics.ts`:

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlerEnemyResultStatisticsItemResponse,
  BrawlerItemOwnershipStatisticsResponse,
  BrawlerPairRankStatisticsResponse,
  BrawlerPairResultStatisticsItemResponse,
  BrawlerRankStatisticsResponse,
  BrawlerResultDailyStatisticsResponse,
  BrawlerResultStatisticsResponse,
  BrawlerTrophyRangeDto,
  ListResponse,
  RankStatsSortByDto,
  ResultStatsSortByDto,
  SortOrderDto,
  StatsDateRangeDto,
  StatsTierRangeDto,
} from "@/api/types";

function buildQuery(params: Record<string, string | number | undefined | null>): string {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null,
  ) as [string, string | number][];
  if (entries.length === 0) return "";
  const sp = new URLSearchParams();
  for (const [k, v] of entries) sp.set(k, String(v));
  return `?${sp.toString()}`;
}

export interface BrawlerResultParams {
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
}

export interface BrawlerRankParams {
  eventId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
}

export interface PairResultParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
  sortBy?: ResultStatsSortByDto;
  sortOrder?: SortOrderDto;
}

export interface PairRankParams {
  eventId: number;
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  sortBy?: RankStatsSortByDto;
  sortOrder?: SortOrderDto;
}

export interface EnemyResultParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
  eventId?: number;
  sortBy?: ResultStatsSortByDto;
  sortOrder?: SortOrderDto;
}

export interface DailyParams {
  brawlerId: number;
  tierRange: StatsTierRangeDto;
  dateRange: StatsDateRangeDto;
}

export interface ItemOwnershipParams {
  brawlerId: number;
  trophyRange: BrawlerTrophyRangeDto;
}

export const statisticsApi = {
  brawlerResult: (p: BrawlerResultParams, opts?: FetchOpts) =>
    apiFetch<BrawlerResultStatisticsResponse>(
      `/v1/statistics/brawler/result${buildQuery(p)}`,
      opts,
    ),

  brawlerRank: (p: BrawlerRankParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerRankStatisticsResponse>>(
      `/v1/statistics/brawler/rank${buildQuery(p)}`,
      opts,
    ),

  pairResult: (p: PairResultParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerPairResultStatisticsItemResponse>>(
      `/v1/statistics/brawler/pair/result${buildQuery(p)}`,
      opts,
    ),

  pairRank: (p: PairRankParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerPairRankStatisticsResponse>>(
      `/v1/statistics/brawler/pair/rank${buildQuery(p)}`,
      opts,
    ),

  enemyResult: (p: EnemyResultParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerEnemyResultStatisticsItemResponse>>(
      `/v1/statistics/brawler/enemy/result${buildQuery(p)}`,
      opts,
    ),

  daily: (p: DailyParams, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResultDailyStatisticsResponse>>(
      `/v1/statistics/brawler/result/daily${buildQuery(p)}`,
      opts,
    ),

  itemOwnership: (p: ItemOwnershipParams, opts?: FetchOpts) =>
    apiFetch<BrawlerItemOwnershipStatisticsResponse>(
      `/v1/statistics/brawler/item/ownership${buildQuery(p)}`,
      opts,
    ),
};
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/api/endpoints/statistics.ts
git commit -m "$(cat <<'EOF'
feat(frontend): statisticsApi 엔드포인트 추가

7개 statistics 엔드포인트 클라이언트. URL 빌더가 undefined 파라미터 자동 제거.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: TierBadge 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/_components/statistics/TierBadge.tsx`
- Create: `imstargg-frontend/tests/unit/components/statistics/TierBadge.test.tsx`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/components/statistics/TierBadge.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TierBadge } from "@app/_components/statistics/TierBadge";

describe("TierBadge", () => {
  it("S 티어를 렌더한다", () => {
    render(<TierBadge tier="S" />);
    expect(screen.getByText("S")).toBeInTheDocument();
  });

  it("D 티어를 렌더한다", () => {
    render(<TierBadge tier="D" />);
    expect(screen.getByText("D")).toBeInTheDocument();
  });

  it("aria-label 에 tier 가 포함된다", () => {
    render(<TierBadge tier="A" />);
    expect(screen.getByLabelText(/A 티어/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/TierBadge.test.tsx
```

Expected: FAIL.

- [ ] **Step 3: 구현**

`app/_components/statistics/TierBadge.tsx`:

```tsx
import { TIER_COLOR, type StatTier } from "@/domain/statistics/tier";

export function TierBadge({ tier }: { tier: StatTier }) {
  const color = TIER_COLOR[tier];
  return (
    <span
      aria-label={`${tier} 티어`}
      className="inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-extrabold text-[#14161B] tabular-nums"
      style={{
        backgroundColor: color,
        boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)",
      }}
    >
      {tier}
    </span>
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/TierBadge.test.tsx
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/_components/statistics/TierBadge.tsx imstargg-frontend/tests/unit/components/statistics/TierBadge.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): TierBadge 컴포넌트 추가

S/A/B/C/D 5단계 + DESIGN.md 스티커 그림자 + aria-label 접근성.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: StatRangeSelector + TrophyRangeSelector

**Files:**
- Create: `imstargg-frontend/app/_components/statistics/StatRangeSelector.tsx`
- Create: `imstargg-frontend/app/_components/statistics/TrophyRangeSelector.tsx`
- Create: `imstargg-frontend/tests/unit/components/statistics/StatRangeSelector.test.tsx`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/components/statistics/StatRangeSelector.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/brawlers",
  useSearchParams: () => new URLSearchParams(""),
}));

describe("StatRangeSelector", () => {
  it("현재 tier/date 라벨을 표시한다", () => {
    render(<StatRangeSelector tier="TROPHY_2000_PLUS" date="LAST_WEEK" />);
    expect(screen.getByText("트로피 2,000+")).toBeInTheDocument();
    expect(screen.getByText("지난 주")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/StatRangeSelector.test.tsx
```

Expected: FAIL.

- [ ] **Step 3: StatRangeSelector 구현**

`app/_components/statistics/StatRangeSelector.tsx`:

```tsx
"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  TIER_RANGE_LABELS,
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

type Props = {
  tier: StatsTierRange;
  date: StatsDateRange;
};

export function StatRangeSelector({ tier, date }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function navigate(next: { tier?: StatsTierRange; date?: StatsDateRange }) {
    const sp = new URLSearchParams(params);
    const newTier = next.tier ?? tier;
    const newDate = next.date ?? date;
    if (newTier === DEFAULT_TIER_RANGE) sp.delete("tier");
    else sp.set("tier", newTier);
    if (newDate === DEFAULT_DATE_RANGE) sp.delete("date");
    else sp.set("date", newDate);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select value={tier} onValueChange={(v) => navigate({ tier: v as StatsTierRange })}>
        <SelectTrigger className="w-40 text-sm">
          <SelectValue>{TIER_RANGE_LABELS[tier]}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_TIER_RANGES.map((t) => (
            <SelectItem key={t} value={t}>
              {TIER_RANGE_LABELS[t]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={date} onValueChange={(v) => navigate({ date: v as StatsDateRange })}>
        <SelectTrigger className="w-32 text-sm">
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
```

- [ ] **Step 4: TrophyRangeSelector 구현**

`app/_components/statistics/TrophyRangeSelector.tsx`:

```tsx
"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  BRAWLER_TROPHY_RANGES,
  TROPHY_RANGE_LABELS,
  DEFAULT_TROPHY_RANGE,
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
  paramName?: string; // default "trophy"
};

export function TrophyRangeSelector({ trophy, paramName = "trophy" }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function navigate(next: BrawlerTrophyRange) {
    const sp = new URLSearchParams(params);
    if (next === DEFAULT_TROPHY_RANGE) sp.delete(paramName);
    else sp.set(paramName, next);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <Select value={trophy} onValueChange={(v) => navigate(v as BrawlerTrophyRange)}>
      <SelectTrigger className="w-28 text-sm">
        <SelectValue>{TROPHY_RANGE_LABELS[trophy]}</SelectValue>
      </SelectTrigger>
      <SelectContent>
        {BRAWLER_TROPHY_RANGES.map((t) => (
          <SelectItem key={t} value={t}>
            {TROPHY_RANGE_LABELS[t]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

- [ ] **Step 5: 테스트 통과 확인 + typecheck**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/StatRangeSelector.test.tsx && pnpm typecheck
```

Expected: PASS / 통과.

- [ ] **Step 6: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/_components/statistics/StatRangeSelector.tsx imstargg-frontend/app/_components/statistics/TrophyRangeSelector.tsx imstargg-frontend/tests/unit/components/statistics/StatRangeSelector.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): StatRangeSelector + TrophyRangeSelector 추가

URL 쿼리 파라미터 기반 tier/date/trophy 셀렉터. 기본값과 같으면 URL 정리.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: SortableHeader + StatNumberCell

**Files:**
- Create: `imstargg-frontend/app/_components/statistics/SortableHeader.tsx`
- Create: `imstargg-frontend/app/_components/statistics/StatNumberCell.tsx`
- Create: `imstargg-frontend/tests/unit/components/statistics/SortableHeader.test.tsx`

- [ ] **Step 1: 실패하는 테스트**

`tests/unit/components/statistics/SortableHeader.test.tsx`:

```tsx
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/brawlers/1",
  useSearchParams: () => new URLSearchParams(""),
}));

describe("SortableHeader", () => {
  it("활성 컬럼은 ▼ 표시", () => {
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    expect(screen.getByRole("button")).toHaveTextContent("▼");
  });

  it("비활성 컬럼은 화살표 없음", () => {
    render(
      <SortableHeader
        column="pickRate"
        label="픽률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    expect(screen.getByRole("button")).not.toHaveTextContent("▼");
    expect(screen.getByRole("button")).not.toHaveTextContent("▲");
  });

  it("활성 컬럼 클릭 시 order 토글", () => {
    replace.mockClear();
    render(
      <SortableHeader
        column="winRate"
        label="승률"
        currentSort="winRate"
        currentOrder="DESC"
      />,
    );
    fireEvent.click(screen.getByRole("button"));
    expect(replace).toHaveBeenCalledWith(expect.stringContaining("order=ASC"), expect.any(Object));
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/SortableHeader.test.tsx
```

Expected: FAIL.

- [ ] **Step 3: SortableHeader 구현**

`app/_components/statistics/SortableHeader.tsx`:

```tsx
"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { type SortOrder, toggleSortOrder } from "@/domain/statistics/sort";

type Props = {
  column: string;
  label: string;
  currentSort: string;
  currentOrder: SortOrder;
  /** "" | "pair" | "enemy" — sort/order 파라미터 prefix. 비어 있으면 sort/order 직접 사용. */
  paramPrefix?: string;
};

function paramName(prefix: string | undefined, base: "sort" | "order"): string {
  if (!prefix) return base;
  return `${prefix}${base.charAt(0).toUpperCase()}${base.slice(1)}`;
}

export function SortableHeader({
  column,
  label,
  currentSort,
  currentOrder,
  paramPrefix,
}: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const isActive = currentSort === column;

  function onClick() {
    const sp = new URLSearchParams(params);
    const sortKey = paramName(paramPrefix, "sort");
    const orderKey = paramName(paramPrefix, "order");
    const nextOrder: SortOrder = isActive ? toggleSortOrder(currentOrder) : "DESC";
    sp.set(sortKey, column);
    sp.set(orderKey, nextOrder);
    router.replace(`${pathname}?${sp.toString()}`, { scroll: false });
  }

  const arrow = !isActive ? "" : currentOrder === "DESC" ? "▼" : "▲";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1 text-xs font-bold uppercase tracking-wider ${
        isActive ? "text-[#14161B]" : "text-[#5C6271] hover:text-[#14161B]"
      }`}
    >
      {label}
      {arrow && <span aria-hidden>{arrow}</span>}
    </button>
  );
}
```

- [ ] **Step 4: StatNumberCell 구현**

`app/_components/statistics/StatNumberCell.tsx`:

```tsx
type RateProps = { value: number; fractionDigits?: number };

export function PercentCell({ value, fractionDigits = 1 }: RateProps) {
  return (
    <span className="font-num font-bold tabular-nums">
      {(value * 100).toFixed(fractionDigits)}%
    </span>
  );
}

export function AverageRankCell({ value }: { value: number }) {
  return (
    <span className="font-num font-bold tabular-nums">{value.toFixed(2)}</span>
  );
}

export function CountCell({ value }: { value: number }) {
  return (
    <span className="font-num tabular-nums text-[#5C6271]">
      {value.toLocaleString("ko-KR")}
    </span>
  );
}
```

- [ ] **Step 5: 테스트 + typecheck**

```bash
cd imstargg-frontend && pnpm test:run tests/unit/components/statistics/SortableHeader.test.tsx && pnpm typecheck
```

Expected: PASS / 통과.

- [ ] **Step 6: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/_components/statistics/SortableHeader.tsx imstargg-frontend/app/_components/statistics/StatNumberCell.tsx imstargg-frontend/tests/unit/components/statistics/SortableHeader.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): SortableHeader + StatNumberCell 추가

URL 쿼리 기반 정렬 토글 헤더와 0~1 → 퍼센트/평균순위/카운트 셀 헬퍼.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: /brawlers 메타 표 (rewrite)

**Files:**
- Create: `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx`
- Create: `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx`
- Create: `imstargg-frontend/app/brawlers/_components/BrawlerListHeader.tsx`
- Modify: `imstargg-frontend/app/brawlers/page.tsx` (rewrite)
- Delete: `imstargg-frontend/app/brawlers/_components/BrawlersListPage.tsx` (replaced)
- Delete: `imstargg-frontend/app/brawlers/_components/BrawlerCard.tsx` (no longer used by `/brawlers`; keep if still used elsewhere — check first)
- Delete: `imstargg-frontend/app/brawlers/_components/BrawlerFilters.tsx` (filter UI replaced by `BrawlerListHeader`)

- [ ] **Step 1: 기존 컴포넌트 사용처 확인**

```bash
cd imstargg-frontend && grep -rn "BrawlerCard\|BrawlersListPage\|BrawlerFilters" app src --include="*.ts" --include="*.tsx" | grep -v "app/brawlers/_components"
```

Expected: 다른 페이지에서 import 가 있으면 그 페이지도 함께 갱신해야 함. 없으면 안전하게 삭제 가능.

- [ ] **Step 2: BrawlerStatRow 구현**

`app/brawlers/_components/BrawlerStatRow.tsx`:

```tsx
import Image from "next/image";
import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  rank: number;
  brawler: Brawler;
  stat: BrawlerResultStat | null;
};

export function BrawlerStatRow({ rank, brawler, stat }: Props) {
  return (
    <Link
      href={`/brawlers/${brawler.id}`}
      className="grid grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px]"
    >
      <span className="text-sm font-bold text-[#5C6271] tabular-nums">{rank}</span>
      <div className="flex items-center gap-2 overflow-hidden">
        {brawler.portraitImageUrl && (
          <Image
            src={brawler.portraitImageUrl}
            alt={brawler.name}
            width={32}
            height={32}
            unoptimized
            className="h-8 w-8 rounded-md"
          />
        )}
        <span className="truncate text-sm font-bold">{brawler.name}</span>
      </div>
      <div>{stat ? <TierBadge tier={stat.tier} /> : <TierBadge tier="D" />}</div>
      <PercentCell value={stat?.winRate ?? 0} />
      <span className="hidden sm:inline">
        <PercentCell value={stat?.pickRate ?? 0} />
      </span>
      <span className="hidden sm:inline">
        <PercentCell value={stat?.starPlayerRate ?? 0} />
      </span>
      <span className="hidden sm:inline">
        <CountCell value={stat?.totalBattleCount ?? 0} />
      </span>
    </Link>
  );
}
```

- [ ] **Step 3: BrawlerStatTable 구현**

`app/brawlers/_components/BrawlerStatTable.tsx`:

```tsx
"use client";

import { useMemo, useState } from "react";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import { BrawlerStatRow } from "./BrawlerStatRow";
import { BrawlerListHeader } from "./BrawlerListHeader";

type Row = { brawler: Brawler; stat: BrawlerResultStat | null };

type Props = {
  rows: Row[];
  sort: string;
  order: SortOrder;
  totalBattleCount: number;
};

function compareRows(a: Row, b: Row, sort: string, order: SortOrder): number {
  const sign = order === "DESC" ? -1 : 1;
  switch (sort) {
    case "name":
      return a.brawler.name.localeCompare(b.brawler.name, "ko") * sign;
    case "winRate":
      return ((a.stat?.winRate ?? 0) - (b.stat?.winRate ?? 0)) * sign;
    case "pickRate":
      return ((a.stat?.pickRate ?? 0) - (b.stat?.pickRate ?? 0)) * sign;
    case "starPlayerRate":
      return ((a.stat?.starPlayerRate ?? 0) - (b.stat?.starPlayerRate ?? 0)) * sign;
    case "totalBattleCount":
      return ((a.stat?.totalBattleCount ?? 0) - (b.stat?.totalBattleCount ?? 0)) * sign;
    case "tier":
    default:
      return ((a.stat?.tierScore ?? 0) - (b.stat?.tierScore ?? 0)) * sign;
  }
}

export function BrawlerStatTable({ rows, sort, order, totalBattleCount }: Props) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => r.brawler.name.toLowerCase().includes(q));
  }, [rows, search]);

  const sorted = useMemo(
    () => [...filtered].sort((a, b) => compareRows(a, b, sort, order)),
    [filtered, sort, order],
  );

  return (
    <div className="space-y-3">
      <BrawlerListHeader
        search={search}
        onSearchChange={setSearch}
        totalBattleCount={totalBattleCount}
      />
      <div className="rounded-lg border border-[#D8DCE5] bg-white">
        <div className="grid grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px]">
          <span>#</span>
          <SortableHeader column="name" label="브롤러" currentSort={sort} currentOrder={order} />
          <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} />
          <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} />
          <span className="hidden sm:inline">
            <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} />
          </span>
          <span className="hidden sm:inline">
            <SortableHeader column="starPlayerRate" label="MVP" currentSort={sort} currentOrder={order} />
          </span>
          <span className="hidden sm:inline">
            <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
          </span>
        </div>
        {sorted.length === 0 ? (
          <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
            해당 조건에 데이터가 없습니다.
          </div>
        ) : (
          sorted.map((r, i) => (
            <BrawlerStatRow key={r.brawler.id} rank={i + 1} brawler={r.brawler} stat={r.stat} />
          ))
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 4: BrawlerListHeader 구현**

`app/brawlers/_components/BrawlerListHeader.tsx`:

```tsx
"use client";

import { Input } from "@/components/ui/input";

type Props = {
  search: string;
  onSearchChange: (s: string) => void;
  totalBattleCount: number;
};

export function BrawlerListHeader({ search, onSearchChange, totalBattleCount }: Props) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#5C6271]">
          BRAWLERS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 메타</h1>
        <p className="mt-1 text-xs text-[#5C6271]">
          전체 전투 표본: <span className="font-num tabular-nums">{totalBattleCount.toLocaleString("ko-KR")}</span>
        </p>
      </div>
      <Input
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder="브롤러 이름 검색"
        className="sm:w-56"
      />
    </div>
  );
}
```

- [ ] **Step 5: page.tsx rewrite**

`app/brawlers/page.tsx`:

```tsx
import type { Metadata } from "next";
import { brawlerApi } from "@/api/endpoints/brawler";
import { statisticsApi } from "@/api/endpoints/statistics";
import { toBrawler } from "@/domain/brawler/mappers";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import {
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseSortOrder } from "@/domain/statistics/sort";
import { sortByName } from "@/domain/brawler/filters";
import { buildMetadata } from "@/lib/seo";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { BrawlerStatTable } from "./_components/BrawlerStatTable";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 메타 | ImStarGG",
  description: "브롤스타즈 브롤러 메타 — 티어, 승률, 픽률을 한눈에.",
  path: "/brawlers",
});

export const revalidate = 600;

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function Page({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const tierRange = parseTierRange(sp.tier);
  const dateRange = parseDateRange(sp.date);
  const sort = typeof sp.sort === "string" ? sp.sort : "tier";
  const order = parseSortOrder(sp.order);

  const [brawlersList, statsDto] = await Promise.all([
    brawlerApi.list({ next: { revalidate: 86400 } }),
    statisticsApi.brawlerResult(
      { tierRange, dateRange },
      { next: { revalidate: 600 } },
    ),
  ]);

  const stats = toBrawlerResultStatistics(statsDto);
  const brawlers = sortByName(brawlersList.content.map(toBrawler));
  const statById = new Map(stats.items.filter((s) => s.brawler).map((s) => [s.brawler!.id, s]));
  const rows = brawlers.map((b) => ({ brawler: b, stat: statById.get(b.id) ?? null }));

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6 space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="text-sm text-[#5C6271]">
          <span className="font-bold text-[#14161B]">{TIER_RANGE_LABELS[tierRange]}</span> 메타
        </div>
        <StatRangeSelector tier={tierRange} date={dateRange} />
      </div>
      <BrawlerStatTable
        rows={rows}
        sort={sort}
        order={order}
        totalBattleCount={stats.totalBattleCount}
      />
    </main>
  );
}
```

- [ ] **Step 6: 사용 안 하는 컴포넌트 삭제 (Step 1 결과 기반)**

기존 `BrawlersListPage.tsx`, `BrawlerCard.tsx`, `BrawlerFilters.tsx` 가 다른 곳에서 import 안 되면:

```bash
cd imstargg-frontend
git rm app/brawlers/_components/BrawlersListPage.tsx app/brawlers/_components/BrawlerCard.tsx app/brawlers/_components/BrawlerFilters.tsx
```

다른 곳에서 사용 중이면 import 를 새 컴포넌트로 갱신.

- [ ] **Step 7: dev 서버에서 시각 확인**

```bash
cd imstargg-frontend && pnpm dev
```

브라우저로 `http://localhost:3000/brawlers` 진입 → 표 렌더, tier 필터 변경 → URL 갱신, 컬럼 클릭 → 정렬 토글.

- [ ] **Step 8: typecheck + lint + 단위 테스트**

```bash
cd imstargg-frontend && pnpm typecheck && pnpm lint && pnpm test:run
```

Expected: 모두 통과.

- [ ] **Step 9: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers
git commit -m "$(cat <<'EOF'
feat(frontend): /brawlers 페이지를 메타 표 형식으로 전환

티어/승률/픽률/MVP/전투수 컬럼 + 컬럼 정렬 + 검색 + StatRangeSelector.
기존 그리드 컴포넌트(BrawlersListPage/BrawlerCard/BrawlerFilters) 제거.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: /brawlers/[id] DailyTrendChart

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/DailyTrendChart.tsx`

- [ ] **Step 1: 구현**

`app/brawlers/[id]/_components/DailyTrendChart.tsx`:

```tsx
"use client";

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { DailyResultStat } from "@/domain/statistics/types";

type Props = { items: DailyResultStat[] };

export function DailyTrendChart({ items }: Props) {
  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">
        일별 통계 데이터가 없습니다.
      </div>
    );
  }
  const data = items.map((d) => ({
    date: d.date.slice(5),
    winRate: Number((d.winRate * 100).toFixed(1)),
    starPlayerRate: Number((d.starPlayerRate * 100).toFixed(1)),
    totalBattleCount: d.totalBattleCount,
  }));
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white p-4">
      <h3 className="mb-2 text-sm font-bold">일별 트렌드</h3>
      <div style={{ height: 240 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid stroke="#ECEEF3" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#5C6271" fontSize={11} />
            <YAxis stroke="#5C6271" fontSize={11} unit="%" domain={[0, 100]} />
            <Tooltip
              formatter={(v: number, name: string) => [`${v}%`, name === "winRate" ? "승률" : "MVP"]}
            />
            <Line type="monotone" dataKey="winRate" stroke="#FFC60A" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="starPlayerRate" stroke="#9146FF" strokeWidth={2} strokeDasharray="4 4" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/\[id\]/_components/DailyTrendChart.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): DailyTrendChart 컴포넌트 추가

브롤러 일별 승률/MVP 라인 차트. recharts 라인 차트 + 빈 데이터 처리.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: PairStatsTable + EnemyStatsTable

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/PairStatsTable.tsx`
- Create: `imstargg-frontend/app/brawlers/[id]/_components/EnemyStatsTable.tsx`

- [ ] **Step 1: PairStatsTable 구현**

`app/brawlers/[id]/_components/PairStatsTable.tsx`:

```tsx
import Image from "next/image";
import Link from "next/link";
import type { PairResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

export function PairStatsTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">페어 통계</h3>
      </div>
      <div className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]">
        <span>#</span>
        <span>페어 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <span className="hidden sm:inline">전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]"
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.pairBrawler.portraitImageUrl && (
                  <Image
                    src={it.pairBrawler.portraitImageUrl}
                    alt={it.pairBrawler.name}
                    width={28}
                    height={28}
                    unoptimized
                    className="h-7 w-7 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.pairBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} />
              <span className="hidden sm:inline">
                <CountCell value={it.totalBattleCount} />
              </span>
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
```

- [ ] **Step 2: EnemyStatsTable 구현**

`app/brawlers/[id]/_components/EnemyStatsTable.tsx`:

```tsx
import Image from "next/image";
import Link from "next/link";
import type { EnemyResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: EnemyResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

export function EnemyStatsTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">적군 통계</h3>
      </div>
      <div className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]">
        <span>#</span>
        <span>적군 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <span className="hidden sm:inline">전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.enemyBrawler ? (
            <Link
              key={`${it.enemyBrawler.id}-${i}`}
              href={`/brawlers/${it.enemyBrawler.id}`}
              className="grid grid-cols-[40px_minmax(0,1fr)_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_72px_72px]"
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.enemyBrawler.portraitImageUrl && (
                  <Image
                    src={it.enemyBrawler.portraitImageUrl}
                    alt={it.enemyBrawler.name}
                    width={28}
                    height={28}
                    unoptimized
                    className="h-7 w-7 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.enemyBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} />
              <span className="hidden sm:inline">
                <CountCell value={it.totalBattleCount} />
              </span>
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/\[id\]/_components/PairStatsTable.tsx imstargg-frontend/app/brawlers/\[id\]/_components/EnemyStatsTable.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): PairStatsTable + EnemyStatsTable 컴포넌트 추가

브롤러 상세의 페어/적군 결과형 통계 표. 컬럼 헤더 정렬 + 모바일 압축.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: ItemOwnershipSection

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx`

- [ ] **Step 1: 구현**

`app/brawlers/[id]/_components/ItemOwnershipSection.tsx`:

```tsx
import type { Brawler } from "@/domain/brawler/types";
import type { ItemOwnership, ItemOwnershipItem } from "@/domain/statistics/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";

type Props = {
  brawler: Brawler;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

function findItemName(brawler: Brawler, key: keyof Brawler, itemId: number): string {
  const list = brawler[key] as Array<{ id: number; name: string }>;
  return list.find((x) => x.id === itemId)?.name ?? `#${itemId}`;
}

function ItemRow({
  name,
  rate,
}: {
  name: string;
  rate: number;
}) {
  const pct = Math.round(rate * 100);
  return (
    <div className="flex items-center gap-2 py-1.5">
      <span className="flex-1 truncate text-sm">{name}</span>
      <div className="h-1.5 w-24 overflow-hidden rounded-full bg-[#ECEEF3] sm:w-32">
        <div className="h-full bg-[#FFC60A]" style={{ width: `${pct}%` }} />
      </div>
      <span className="w-12 text-right text-xs font-bold tabular-nums">{pct}%</span>
    </div>
  );
}

function CategoryCard({
  title,
  items,
  resolveName,
}: {
  title: string;
  items: ItemOwnershipItem[];
  resolveName: (itemId: number) => string;
}) {
  if (items.length === 0) return null;
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white p-3">
      <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5C6271]">
        {title}
      </h4>
      {items.map((it) => (
        <ItemRow key={it.itemId} name={resolveName(it.itemId)} rate={it.ownershipRate} />
      ))}
    </div>
  );
}

export function ItemOwnershipSection({ brawler, ownership, trophy }: Props) {
  const isAllEmpty =
    ownership.gears.length === 0 &&
    ownership.gadgets.length === 0 &&
    ownership.starPowers.length === 0 &&
    ownership.hyperCharges.length === 0 &&
    ownership.buffies.length === 0;

  return (
    <section className="space-y-3">
      <div className="flex items-end justify-between">
        <h3 className="text-sm font-bold">아이템 보유율</h3>
        <TrophyRangeSelector trophy={trophy} />
      </div>
      {isAllEmpty ? (
        <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
          보유율 데이터가 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <CategoryCard
            title="기어"
            items={ownership.gears}
            resolveName={(id) => findItemName(brawler, "gears", id)}
          />
          <CategoryCard
            title="가젯"
            items={ownership.gadgets}
            resolveName={(id) => findItemName(brawler, "gadgets", id)}
          />
          <CategoryCard
            title="스타파워"
            items={ownership.starPowers}
            resolveName={(id) => findItemName(brawler, "starPowers", id)}
          />
          <CategoryCard
            title="하이퍼차지"
            items={ownership.hyperCharges}
            resolveName={(id) => findItemName(brawler, "hyperCharges", id)}
          />
          {ownership.buffies.length > 0 && (
            <div className="rounded-lg border border-[#D8DCE5] bg-white p-3">
              <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5C6271]">버피</h4>
              {ownership.buffies.map((b) => (
                <ItemRow key={b.buffyType} name={b.buffyType} rate={b.ownershipRate} />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/\[id\]/_components/ItemOwnershipSection.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): ItemOwnershipSection 컴포넌트 추가

기어/가젯/스타파워/하이퍼차지/버피 5개 카테고리 보유율 카드.
TrophyRangeSelector 로 4단계 전환.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 16: BrawlerStatsSection 컴포즈 + /brawlers/[id] 통합

**Files:**
- Create: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`

- [ ] **Step 1: BrawlerStatsSection 구현 (컨테이너 RSC)**

`app/brawlers/[id]/_components/BrawlerStatsSection.tsx`:

```tsx
import { statisticsApi } from "@/api/endpoints/statistics";
import {
  parseDateRange,
  parseTierRange,
  parseTrophyRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseResultSortBy, parseSortOrder } from "@/domain/statistics/sort";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toItemOwnership,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { DailyTrendChart } from "./DailyTrendChart";
import { PairStatsTable } from "./PairStatsTable";
import { EnemyStatsTable } from "./EnemyStatsTable";
import { ItemOwnershipSection } from "./ItemOwnershipSection";

type Props = {
  brawler: Brawler;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function BrawlerStatsSection({ brawler, searchParams }: Props) {
  const tier = parseTierRange(searchParams.tier);
  const date = parseDateRange(searchParams.date);
  const trophy = parseTrophyRange(searchParams.trophy);
  const pairSort = parseResultSortBy(searchParams.pairSort);
  const pairOrder = parseSortOrder(searchParams.pairOrder);
  const enemySort = parseResultSortBy(searchParams.enemySort);
  const enemyOrder = parseSortOrder(searchParams.enemyOrder);

  const [daily, pair, enemy, ownership] = await Promise.all([
    statisticsApi.daily(
      { brawlerId: brawler.id, tierRange: tier, dateRange: date },
      { next: { revalidate: 600 } },
    ),
    statisticsApi.pairResult(
      {
        brawlerId: brawler.id,
        tierRange: tier,
        dateRange: date,
        sortBy: pairSort,
        sortOrder: pairOrder,
      },
      { next: { revalidate: 600 } },
    ),
    statisticsApi.enemyResult(
      {
        brawlerId: brawler.id,
        tierRange: tier,
        dateRange: date,
        sortBy: enemySort,
        sortOrder: enemyOrder,
      },
      { next: { revalidate: 600 } },
    ),
    statisticsApi.itemOwnership(
      { brawlerId: brawler.id, trophyRange: trophy },
      { next: { revalidate: 600 } },
    ),
  ]);

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-bold">통계</h2>
          <p className="text-xs text-[#5C6271]">
            {TIER_RANGE_LABELS[tier]} 기준
          </p>
        </div>
        <StatRangeSelector tier={tier} date={date} />
      </div>
      <DailyTrendChart items={daily.content.map(toDailyResultStat)} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.content.map(toPairResultStat)} sort={pairSort} order={pairOrder} />
        <EnemyStatsTable items={enemy.content.map(toEnemyResultStat)} sort={enemySort} order={enemyOrder} />
      </div>
      <ItemOwnershipSection
        brawler={brawler}
        ownership={toItemOwnership(ownership)}
        trophy={trophy}
      />
    </section>
  );
}
```

- [ ] **Step 2: page.tsx 에 통계 섹션 통합**

기존 `app/brawlers/[id]/page.tsx` 의 default export 를 다음으로 교체 (`searchParams` 받기 + revalidate 600 으로 변경):

```tsx
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";
import { BrawlerStatsSection } from "./_components/BrawlerStatsSection";

type Params = { params: Promise<{ id: string }> };
type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export const revalidate = 600;

export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
    const brawler = toBrawler(dto);
    const role = ROLE_LABEL_KO[brawler.role];
    const rarity = RARITY_LABEL_KO[brawler.rarity];
    return buildMetadata({
      title: `${brawler.name} | ImStarGG`,
      description: `${brawler.name} — ${role} · ${rarity} · 가젯 ${brawler.gadgets.length} · 스타파워 ${brawler.starPowers.length} · 기어 ${brawler.gears.length} · 하이퍼차지 ${brawler.hyperCharges.length}.`,
      path: `/brawlers/${id}`,
      image: brawler.portraitImageUrl ?? undefined,
    });
  } catch {
    return buildMetadata({
      title: "브롤러를 찾을 수 없음 | ImStarGG",
      description: "요청하신 브롤러를 찾을 수 없습니다.",
      path: `/brawlers/${id}`,
      noindex: true,
    });
  }
}

export default async function BrawlerPage({
  params,
  searchParams,
}: {
  params: Params["params"];
  searchParams: SearchParams;
}) {
  const { id } = await params;
  const sp = await searchParams;
  let dto;
  try {
    dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const brawler = toBrawler(dto);
  return (
    <>
      <BrawlerContent initialBrawler={brawler} />
      <main className="mx-auto w-full max-w-6xl px-4 pb-10">
        <BrawlerStatsSection brawler={brawler} searchParams={sp} />
      </main>
    </>
  );
}
```

(기존 `BrawlerContent` 가 자체 `<main>` 을 가지면 위 두 번째 `<main>` 은 그대로 별도 섹션으로 유지 — 본문 길이 늘리는 자연스러운 흐름. `BrawlerContent` 내부에서 stats placeholder 를 가지고 있다면 그 placeholder 만 제거.)

- [ ] **Step 3: BrawlerContent 의 stats placeholder 제거 (있는 경우)**

```bash
cd imstargg-frontend && grep -n "통계\|placeholder\|stats" app/brawlers/\[id\]/_components/BrawlerContent.tsx
```

placeholder 마크업이 있으면 제거.

- [ ] **Step 4: dev 서버 시각 확인**

```bash
cd imstargg-frontend && pnpm dev
```

`http://localhost:3000/brawlers/16000000` (또는 임의 ID) 진입 → 차트, 페어/적군 표, 보유율 모두 렌더, tier/date 변경 → 데이터 갱신.

- [ ] **Step 5: typecheck + lint**

```bash
cd imstargg-frontend && pnpm typecheck && pnpm lint
```

Expected: 통과.

- [ ] **Step 6: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/\[id\]
git commit -m "$(cat <<'EOF'
feat(frontend): /brawlers/[id] 통계 섹션 추가

일별 차트 + 페어/적군 표 + 보유율을 RSC 로 병렬 fetch.
StatRangeSelector + TrophyRangeSelector 로 URL 쿼리 기반 필터.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 17: EventResultStatsTable + EventRankStatsTable

**Files:**
- Create: `imstargg-frontend/app/events/[id]/_components/EventResultStatsTable.tsx`
- Create: `imstargg-frontend/app/events/[id]/_components/EventRankStatsTable.tsx`

- [ ] **Step 1: EventResultStatsTable 구현**

`app/events/[id]/_components/EventResultStatsTable.tsx`:

```tsx
"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: BrawlerResultStat[];
  sort: string;
  order: SortOrder;
  selectedBrawlerId: number | null;
};

export function EventResultStatsTable({ items, sort, order, selectedBrawlerId }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function toggleBrawler(id: number) {
    const sp = new URLSearchParams(params);
    if (selectedBrawlerId === id) sp.delete("brawlerId");
    else sp.set("brawlerId", String(id));
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="grid grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px]">
        <span>#</span>
        <span>브롤러</span>
        <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} />
        <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} />
        <span className="hidden sm:inline">
          <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} />
        </span>
        <span className="hidden sm:inline">
          <SortableHeader column="starPlayerRate" label="MVP" currentSort={sort} currentOrder={order} />
        </span>
        <span className="hidden sm:inline">
          <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
        </span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        items.map((it, i) => {
          const id = it.brawler?.id ?? -1;
          const isActive = selectedBrawlerId === id;
          return (
            <button
              key={`${id}-${i}`}
              type="button"
              onClick={() => id > 0 && toggleBrawler(id)}
              className={`grid w-full grid-cols-[40px_minmax(0,1fr)_56px_72px_72px_72px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-left transition-colors hover:bg-[#F6F7FA] sm:grid-cols-[40px_minmax(0,1fr)_56px_72px] ${
                isActive ? "bg-[#FFE082]/40 border-l-4 border-l-[#FFC60A]" : ""
              }`}
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.brawler?.portraitImageUrl && (
                  <Image
                    src={it.brawler.portraitImageUrl}
                    alt={it.brawler.name}
                    width={32}
                    height={32}
                    unoptimized
                    className="h-8 w-8 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.brawler?.name ?? "-"}</span>
              </div>
              <TierBadge tier={it.tier} />
              <PercentCell value={it.winRate} />
              <span className="hidden sm:inline">
                <PercentCell value={it.pickRate} />
              </span>
              <span className="hidden sm:inline">
                <PercentCell value={it.starPlayerRate} />
              </span>
              <span className="hidden sm:inline">
                <CountCell value={it.totalBattleCount} />
              </span>
            </button>
          );
        })
      )}
    </div>
  );
}
```

- [ ] **Step 2: EventRankStatsTable 구현**

`app/events/[id]/_components/EventRankStatsTable.tsx`:

```tsx
"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { BrawlerRankStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: BrawlerRankStat[];
  sort: string;
  order: SortOrder;
  selectedBrawlerId: number | null;
};

export function EventRankStatsTable({ items, sort, order, selectedBrawlerId }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function toggleBrawler(id: number) {
    const sp = new URLSearchParams(params);
    if (selectedBrawlerId === id) sp.delete("brawlerId");
    else sp.set("brawlerId", String(id));
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>브롤러</span>
        <SortableHeader column="averageRank" label="평균순위" currentSort={sort} currentOrder={order} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 통계 데이터가 없습니다.
        </div>
      ) : (
        items.map((it, i) => {
          const id = it.brawler?.id ?? -1;
          const isActive = selectedBrawlerId === id;
          return (
            <button
              key={`${id}-${i}`}
              type="button"
              onClick={() => id > 0 && toggleBrawler(id)}
              className={`grid w-full grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-left hover:bg-[#F6F7FA] ${
                isActive ? "bg-[#FFE082]/40 border-l-4 border-l-[#FFC60A]" : ""
              }`}
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.brawler?.portraitImageUrl && (
                  <Image
                    src={it.brawler.portraitImageUrl}
                    alt={it.brawler.name}
                    width={32}
                    height={32}
                    unoptimized
                    className="h-8 w-8 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.brawler?.name ?? "-"}</span>
              </div>
              <AverageRankCell value={it.averageRank} />
              <CountCell value={it.totalBattleCount} />
            </button>
          );
        })
      )}
    </div>
  );
}
```

- [ ] **Step 3: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```

Expected: 통과.

- [ ] **Step 4: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/events/\[id\]/_components/EventResultStatsTable.tsx imstargg-frontend/app/events/\[id\]/_components/EventRankStatsTable.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): EventResultStatsTable + EventRankStatsTable 추가

이벤트 상세의 결과형/순위형 메인 표. 행 클릭으로 ?brawlerId= URL 토글, 활성 행 옐로우 강조.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 18: BrawlerSelectedPanel + EventStatsSection (분기 컴포즈)

**Files:**
- Create: `imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx`
- Create: `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`
- Modify: `imstargg-frontend/app/events/[id]/page.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventContent.tsx` (placeholder 제거 — 있는 경우)

- [ ] **Step 1: BrawlerSelectedPanel 구현 (RSC, RESULT 와 RANK 모두 처리)**

`app/events/[id]/_components/BrawlerSelectedPanel.tsx`:

```tsx
import Link from "next/link";
import { statisticsApi } from "@/api/endpoints/statistics";
import {
  toEnemyResultStat,
  toPairRankStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import { parseResultSortBy, parseRankSortBy, parseSortOrder } from "@/domain/statistics/sort";
import type { StatsTierRange, StatsDateRange } from "@/domain/statistics/enums";
import type { BattleEventResultType } from "@/api/types";
import { PairStatsTable } from "@app/brawlers/[id]/_components/PairStatsTable";
import { EnemyStatsTable } from "@app/brawlers/[id]/_components/EnemyStatsTable";
import { PairRankTable } from "./PairRankTable";

type Props = {
  eventId: number;
  brawlerId: number;
  resultType: BattleEventResultType;
  tier: StatsTierRange;
  date: StatsDateRange;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function BrawlerSelectedPanel({
  eventId,
  brawlerId,
  resultType,
  tier,
  date,
  searchParams,
}: Props) {
  const pairOrder = parseSortOrder(searchParams.pairOrder);
  const enemyOrder = parseSortOrder(searchParams.enemyOrder);

  const closeHref = (() => {
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(searchParams)) {
      if (k === "brawlerId") continue;
      if (typeof v === "string") sp.set(k, v);
    }
    const qs = sp.toString();
    return qs ? `?${qs}` : "?";
  })();

  if (resultType === "RANK") {
    const pairSort = parseRankSortBy(searchParams.pairSort);
    const pair = await statisticsApi.pairRank(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: pairSort, sortOrder: pairOrder },
      { next: { revalidate: 600 } },
    );
    return (
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
          <Link href={closeHref} scroll={false} className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]">
            닫기 ✕
          </Link>
        </div>
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          <PairRankTable items={pair.content.map(toPairRankStat)} sort={pairSort} order={pairOrder} />
          <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
            적군 통계는 결과형 모드 전용입니다.
          </div>
        </div>
      </section>
    );
  }

  const pairSort = parseResultSortBy(searchParams.pairSort);
  const enemySort = parseResultSortBy(searchParams.enemySort);
  const [pair, enemy] = await Promise.all([
    statisticsApi.pairResult(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: pairSort, sortOrder: pairOrder },
      { next: { revalidate: 600 } },
    ),
    statisticsApi.enemyResult(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: enemySort, sortOrder: enemyOrder },
      { next: { revalidate: 600 } },
    ),
  ]);

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
        <Link href={closeHref} scroll={false} className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]">
          닫기 ✕
        </Link>
      </div>
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.content.map(toPairResultStat)} sort={pairSort} order={pairOrder} />
        <EnemyStatsTable items={enemy.content.map(toEnemyResultStat)} sort={enemySort} order={enemyOrder} />
      </div>
    </section>
  );
}
```

- [ ] **Step 2: PairRankTable 구현 (RANK 모드 페어 표 — RESULT 와 컬럼이 다름)**

`app/events/[id]/_components/PairRankTable.tsx`:

```tsx
import Image from "next/image";
import Link from "next/link";
import type { PairRankStat } from "@/domain/statistics/types";
import type { SortOrder, RankStatsSortBy } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  AverageRankCell,
  CountCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairRankStat[];
  sort: RankStatsSortBy;
  order: SortOrder;
};

export function PairRankTable({ items, sort, order }: Props) {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2">
        <h3 className="text-sm font-bold">페어 통계</h3>
      </div>
      <div className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#D8DCE5] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>페어 브롤러</span>
        <SortableHeader column="AVERAGE_RANK" label="평균순위" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              className="grid grid-cols-[40px_minmax(0,1fr)_88px_96px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 hover:bg-[#F6F7FA]"
            >
              <span className="text-sm font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex items-center gap-2 overflow-hidden">
                {it.pairBrawler.portraitImageUrl && (
                  <Image
                    src={it.pairBrawler.portraitImageUrl}
                    alt={it.pairBrawler.name}
                    width={28}
                    height={28}
                    unoptimized
                    className="h-7 w-7 rounded-md"
                  />
                )}
                <span className="truncate text-sm font-bold">{it.pairBrawler.name}</span>
              </div>
              <AverageRankCell value={it.averageRank} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
```

- [ ] **Step 3: EventStatsSection 구현 (분기 컨테이너)**

`app/events/[id]/_components/EventStatsSection.tsx`:

```tsx
import { Suspense } from "react";
import { statisticsApi } from "@/api/endpoints/statistics";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
} from "@/domain/statistics/mappers";
import {
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseSortOrder } from "@/domain/statistics/sort";
import type { BattleEvent } from "@/domain/event/types";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { EventResultStatsTable } from "./EventResultStatsTable";
import { EventRankStatsTable } from "./EventRankStatsTable";
import { BrawlerSelectedPanel } from "./BrawlerSelectedPanel";

type Props = {
  event: BattleEvent;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function EventStatsSection({ event, searchParams }: Props) {
  const tier = parseTierRange(searchParams.tier);
  const date = parseDateRange(searchParams.date);
  const sort = typeof searchParams.sort === "string" ? searchParams.sort : "tier";
  const order = parseSortOrder(searchParams.order);
  const brawlerIdRaw = searchParams.brawlerId;
  const selectedBrawlerId =
    typeof brawlerIdRaw === "string" && /^\d+$/.test(brawlerIdRaw) ? Number(brawlerIdRaw) : null;

  const resultType = event.mode?.resultType ?? "RESULT";

  if (resultType === "RANK") {
    const main = await statisticsApi.brawlerRank(
      { eventId: event.id, tierRange: tier, dateRange: date },
      { next: { revalidate: 600 } },
    );
    const items = main.content.map(toBrawlerRankStat);
    return (
      <section className="space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-lg font-bold">이벤트 통계 (순위형)</h2>
            <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[tier]} 기준</p>
          </div>
          <StatRangeSelector tier={tier} date={date} />
        </div>
        <EventRankStatsTable
          items={items}
          sort={sort}
          order={order}
          selectedBrawlerId={selectedBrawlerId}
        />
        {selectedBrawlerId && (
          <Suspense fallback={<PanelFallback />}>
            <BrawlerSelectedPanel
              eventId={event.id}
              brawlerId={selectedBrawlerId}
              resultType="RANK"
              tier={tier}
              date={date}
              searchParams={searchParams}
            />
          </Suspense>
        )}
      </section>
    );
  }

  const main = await statisticsApi.brawlerResult(
    { eventId: event.id, tierRange: tier, dateRange: date },
    { next: { revalidate: 600 } },
  );
  const stats = toBrawlerResultStatistics(main);
  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-bold">이벤트 통계</h2>
          <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[tier]} 기준</p>
        </div>
        <StatRangeSelector tier={tier} date={date} />
      </div>
      <EventResultStatsTable
        items={sortResultItems(stats.items, sort, order)}
        sort={sort}
        order={order}
        selectedBrawlerId={selectedBrawlerId}
      />
      {selectedBrawlerId && (
        <Suspense fallback={<PanelFallback />}>
          <BrawlerSelectedPanel
            eventId={event.id}
            brawlerId={selectedBrawlerId}
            resultType="RESULT"
            tier={tier}
            date={date}
            searchParams={searchParams}
          />
        </Suspense>
      )}
    </section>
  );
}

function sortResultItems<
  T extends { tierScore: number; winRate: number; pickRate: number; starPlayerRate: number; totalBattleCount: number },
>(items: T[], sort: string, order: import("@/domain/statistics/sort").SortOrder): T[] {
  const sign = order === "DESC" ? -1 : 1;
  const cmp = (a: T, b: T) => {
    switch (sort) {
      case "winRate": return (a.winRate - b.winRate) * sign;
      case "pickRate": return (a.pickRate - b.pickRate) * sign;
      case "starPlayerRate": return (a.starPlayerRate - b.starPlayerRate) * sign;
      case "totalBattleCount": return (a.totalBattleCount - b.totalBattleCount) * sign;
      case "tier":
      default: return (a.tierScore - b.tierScore) * sign;
    }
  };
  return [...items].sort(cmp);
}

function PanelFallback() {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
      불러오는 중…
    </div>
  );
}
```

- [ ] **Step 4: page.tsx 통합 (`searchParams` 받기 + 통계 섹션 렌더)**

`app/events/[id]/page.tsx` default export 를 다음으로 교체:

```tsx
export default async function EventPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { id } = await params;
  const sp = await searchParams;
  let dto;
  try {
    dto = await eventApi.get(id, { next: { revalidate: 3600 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const event = toBattleEvent(dto);
  return (
    <>
      <EventContent initialEvent={event} />
      <main className="mx-auto w-full max-w-6xl px-4 pb-10">
        <EventStatsSection event={event} searchParams={sp} />
      </main>
    </>
  );
}
```

`EventStatsSection` import 도 추가: `import { EventStatsSection } from "./_components/EventStatsSection";`

- [ ] **Step 5: EventContent placeholder 제거 (있는 경우)**

```bash
cd imstargg-frontend && grep -n "통계\|placeholder\|Statistics" app/events/\[id\]/_components/EventContent.tsx
```

placeholder 마크업이 있으면 제거.

- [ ] **Step 6: dev 시각 확인**

```bash
cd imstargg-frontend && pnpm dev
```

- `http://localhost:3000/events/<RESULT 모드 ID>` 진입 → 결과 표 + 행 클릭 → URL `?brawlerId=` + 하단 페어/적군 패널.
- `http://localhost:3000/events/<RANK 모드 ID>` (쇼다운 등) → 평균순위 표 + 행 클릭 → 페어 표 + 적군 안내.

- [ ] **Step 7: typecheck + lint**

```bash
cd imstargg-frontend && pnpm typecheck && pnpm lint
```

Expected: 통과.

- [ ] **Step 8: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/events/\[id\]
git commit -m "$(cat <<'EOF'
feat(frontend): /events/[id] 통계 섹션과 BrawlerSelectedPanel 추가

mode.resultType 분기로 결과/순위 표 자동 노출. 행 클릭 → ?brawlerId= 토글 →
하단 패널이 RSC 로 페어/적군 fetch (RANK 는 페어만, 적군 안내).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 19: E2E 테스트

**Files:**
- Create: `imstargg-frontend/tests/e2e/statistics.spec.ts`

- [ ] **Step 1: E2E 테스트 작성**

`tests/e2e/statistics.spec.ts`:

```ts
import { expect, test } from "@playwright/test";

test("/brawlers 메타 표가 렌더된다", async ({ page }) => {
  await page.goto("/brawlers");
  await expect(page.getByRole("heading", { name: "브롤러 메타" })).toBeVisible({
    timeout: 15_000,
  });
  // 첫 번째 데이터 행이 등장
  const firstRow = page.locator('a[href^="/brawlers/"]').first();
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
});

test("/brawlers tier 변경 시 URL 갱신", async ({ page }) => {
  await page.goto("/brawlers");
  await page.getByRole("combobox").first().click();
  await page.getByRole("option", { name: "트로피 2,000+" }).click();
  await expect(page).toHaveURL(/tier=TROPHY_2000_PLUS/);
});

test("/brawlers/[id] 상세 통계가 모두 렌더된다", async ({ page }) => {
  await page.goto("/brawlers");
  const firstLink = page.locator('a[href^="/brawlers/"]').first();
  await firstLink.click();
  await expect(page.getByText("일별 트렌드")).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText("페어 통계")).toBeVisible();
  await expect(page.getByText("적군 통계")).toBeVisible();
  await expect(page.getByText("아이템 보유율")).toBeVisible();
});

test("/events/[id] RESULT 모드: 행 클릭 시 brawlerId URL", async ({ page }) => {
  // event id 는 rotation 첫번째로 가져온다
  await page.goto("/events");
  const eventLink = page.locator('a[href^="/events/"]').first();
  const href = await eventLink.getAttribute("href");
  if (!href) test.skip();
  await page.goto(href!);
  await expect(page.getByRole("heading", { name: /이벤트 통계/ })).toBeVisible({
    timeout: 15_000,
  });
  const firstStatRow = page.locator('button:has(img[alt])').first();
  await firstStatRow.click();
  await expect(page).toHaveURL(/brawlerId=\d+/);
  await expect(page.getByText("선택한 브롤러 분석")).toBeVisible();
});

test("페어 표 헤더 클릭 시 정렬 URL 변경", async ({ page }) => {
  await page.goto("/brawlers");
  const firstLink = page.locator('a[href^="/brawlers/"]').first();
  await firstLink.click();
  await expect(page.getByText("페어 통계")).toBeVisible({ timeout: 15_000 });
  await page.getByRole("button", { name: /^승률/ }).first().click();
  await expect(page).toHaveURL(/pairSort=WIN_RATE/);
});

test("/brawlers/[id] date 변경 시 URL 갱신", async ({ page }) => {
  await page.goto("/brawlers/16000000");
  // 두 번째 셀렉터(date)
  const selectors = page.getByRole("combobox");
  await selectors.nth(1).click();
  await page.getByRole("option", { name: "지난 주" }).click();
  await expect(page).toHaveURL(/date=LAST_WEEK/);
});
```

- [ ] **Step 2: dev 서버 띄운 채로 E2E 실행**

```bash
cd imstargg-frontend && pnpm test:e2e tests/e2e/statistics.spec.ts
```

Expected: 6/6 통과. 하나라도 실패하면 dev 콘솔 + spec 메시지 확인 후 수정.

- [ ] **Step 3: 커밋**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/tests/e2e/statistics.spec.ts
git commit -m "$(cat <<'EOF'
test(frontend): statistics 도메인 E2E 시나리오 추가

브롤러 메타 표/상세, 이벤트 메인 표/패널 토글, 정렬, 필터 변경 6개 시나리오.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 20: 최종 검증

- [ ] **Step 1: 전체 단위/컴포넌트 테스트**

```bash
cd imstargg-frontend && pnpm test:run
```

Expected: 모두 통과.

- [ ] **Step 2: typecheck + lint 전체**

```bash
cd imstargg-frontend && pnpm typecheck && pnpm lint
```

Expected: 통과.

- [ ] **Step 3: 프로덕션 빌드**

```bash
cd imstargg-frontend && pnpm build
```

Expected: 빌드 성공. statistics 페이지가 모두 정적/동적 라우트로 등록됨.

- [ ] **Step 4: 메모리 업데이트 (선택, 권장)**

이 plan 의 결과를 메모리에 한 줄 기록(예: 완료 도메인 트래커가 있다면).

---

## 완료 검증

모두 통과해야 함:

1. 7개 statistics 엔드포인트가 3개 페이지에 매핑됨:
   - `/brawlers` ← `/v1/statistics/brawler/result`
   - `/brawlers/[id]` ← `/result/daily`, `/pair/result`, `/enemy/result`, `/item/ownership`
   - `/events/[id]` (RESULT) ← `/result?eventId`, `/pair/result`, `/enemy/result`
   - `/events/[id]` (RANK) ← `/rank?eventId`, `/pair/rank`
2. URL 쿼리 필터 동작: `tier`, `date`, `trophy`, `brawlerId`, `sort`, `order`, `pairSort`, `pairOrder`, `enemySort`, `enemyOrder`
3. `mode.resultType` 분기 동작 (`RESULT` ↔ `RANK`)
4. 단위 테스트(enums/sort/tier/mappers) + 컴포넌트 테스트(TierBadge/StatRangeSelector/SortableHeader) 모두 통과
5. E2E 시나리오 6개 통과
6. 메타 표는 op.gg 스타일(컬럼 정렬, tier 뱃지, 검색)
7. 일별 차트(recharts)가 240px 고정 높이로 렌더
8. 보유율 5개 카테고리(기어/가젯/스타파워/하이퍼/버피)가 각자 카드로 노출
9. 이벤트 표 행 클릭 → URL `?brawlerId=` → 하단 패널 노출, 닫기 → 제거
10. typecheck/lint/build 통과
