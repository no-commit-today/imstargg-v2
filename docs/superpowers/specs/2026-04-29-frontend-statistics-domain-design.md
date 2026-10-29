# Frontend Statistics Domain Design

**Date**: 2026-04-29
**Status**: Approved
**Scope**: imstargg-frontend `/brawlers`, `/brawlers/[id]`, `/events/[id]` 페이지의 통계 영역 (별도 라우트 추가 없음)

## Goals

- 백엔드 statistics API 7종을 기존 페이지의 placeholder 영역에 채워 넣기
- Brawler/Event 도메인 spec 의 명시적 placeholder(`* — Statistics 도메인`) 구현
- Events/Ranking 도메인과 일관된 패턴(RSC + ISR, URL 쿼리 파라미터 필터, 매퍼 레이어)

## Non-Goals

- 새로운 top-level 라우트(`/statistics`, `/meta` 등) 추가
- `BattleEventResponse` 에 `battleType` 필드 추가하는 백엔드 변경 (솔로랭크 자동 분기 위해)
- `pairBrawlerId` 선택 필터 UI(특정 두 브롤러 픽 분석)
- 자동 refetch / 폴링 (통계는 정적 성격, ISR 600s 충분)
- 클라이언트 사이드 정렬/필터 — 모두 URL 쿼리 + RSC 재요청
- 클라이언트 사이드 차트 인터랙션(브러시, 줌)

## Backend Endpoints (확정)

| Method | Path | Required Query | Optional Query | 사용 페이지 |
|---|---|---|---|---|
| GET | `/v1/statistics/brawler/result` | `tierRange`, `dateRange` | `eventId` | `/brawlers`, `/events/[id]` (RESULT) |
| GET | `/v1/statistics/brawler/rank` | `eventId`, `tierRange`, `dateRange` | — | `/events/[id]` (RANK) |
| GET | `/v1/statistics/brawler/pair/result` | `brawlerId`, `tierRange`, `dateRange` | `eventId`, `pairBrawlerId`, `sortBy`, `sortOrder` | `/brawlers/[id]`, `/events/[id]` (RESULT) |
| GET | `/v1/statistics/brawler/pair/rank` | `eventId`, `brawlerId`, `tierRange`, `dateRange` | `pairBrawlerId`, `sortBy`, `sortOrder` | `/events/[id]` (RANK) |
| GET | `/v1/statistics/brawler/enemy/result` | `brawlerId`, `tierRange`, `dateRange` | `eventId`, `sortBy`, `sortOrder` | `/brawlers/[id]`, `/events/[id]` (RESULT) |
| GET | `/v1/statistics/brawler/result/daily` | `brawlerId`, `tierRange`, `dateRange` | — | `/brawlers/[id]` |
| GET | `/v1/statistics/brawler/item/ownership` | `brawlerId`, `trophyRange` | — | `/brawlers/[id]` |

## Decisions

| 결정 | 선택 | 이유 |
|---|---|---|
| 통합 위치 | 기존 페이지 placeholder 채우기 (별도 라우트 X) | Brawler/Event spec 의 명시적 placeholder, 일관된 SEO |
| 이벤트 모드 분기 | `mode.resultType === "RANK"` 자동 분기 | 결정적, 사용자 토글 불필요 |
| 필터 상태 | URL 쿼리 파라미터 (`?tier=&date=&trophy=&brawlerId=&pairSort=&pairOrder=&enemySort=&enemyOrder=&sort=&order=`) | RSC 친화, 공유 가능, Ranking 패턴 일치 |
| 데이터 패칭 | RSC + `revalidate: 600` | 정적 성격, Events/Ranking 패턴 일치 |
| 기본 tierRange | `TROPHY_1000_PLUS` (모든 페이지) | 솔로랭크 자동 식별 정보 부재 → 수동 전환 |
| 기본 dateRange | `SINCE_LAST_WEEK` (모든 페이지) | 사용자 지정 |
| 기본 trophyRange (보유율) | `TROPHY_1000_PLUS` | 의미있는 표본 크기 |
| 기본 정렬 | RESULT: `tierScore DESC`, RANK: `pickRate DESC`, PAIR/ENEMY: `PICK_RATE DESC` | 메타 분석 기본값 |
| 솔로랭크 자동 default | 미지원 (사용자 수동 전환) | 백엔드 변경 회피, YAGNI |
| pairBrawlerId 필터 | 미지원 | YAGNI |
| 차트 라이브러리 | shadcn `Chart` (recharts 래퍼) | 디자인 토큰 통합, 표준 인터페이스 |
| 브롤러 목록 형식 | 표 형식 + 통계 컬럼 (그리드 폐기) | op.gg 메타 표 시그니처 |
| 이벤트 상세 브롤러 선택 | 행 클릭 → `?brawlerId=` → 하단 패널 RSC | 페이지 전환 없음, 공유 가능 URL |

## Backend Tier Enum

```kotlin
// imstargg-backend BrawlerTier.kt
enum class BrawlerTier(val value: Int) { S(1), A(2), B(3), C(4), D(5) }
```

5단계: `S | A | B | C | D` (F 등급 없음).

## File Structure

```
src/api/types.ts                              # 7개 Response 타입 + enum 문자열 union 추가
src/api/endpoints/statistics.ts               # 7개 메서드

src/domain/statistics/
├── enums.ts                                  # Tier/Date/TrophyRange + LABELS + parsers
├── sort.ts                                   # ResultStatsSortBy/RankStatsSortBy/SortOrder + parsers
├── tier.ts                                   # StatTier(S~D) + 컬러 + parseTier
├── types.ts                                  # 7개 도메인 모델
└── mappers.ts                                # 7개 매퍼 (Brawler 도메인 toBrawler 재사용)

src/components/ui/chart.tsx                   # shadcn Chart (recharts) 추가

app/_components/statistics/                   # 도메인 공용
├── TierBadge.tsx                             # S/A/B/C/D 뱃지
├── StatRangeSelector.tsx                     # tier + date Select (URL 갱신)
├── TrophyRangeSelector.tsx                   # 보유율 전용 4단계
├── SortableHeader.tsx                        # 컬럼 헤더 클릭 정렬
└── StatNumberCell.tsx                        # 0~1 → "%" 포맷 헬퍼

app/brawlers/
├── page.tsx                                  # 표 + 메타 통계 (rewrite)
└── _components/
    ├── BrawlerStatTable.tsx
    ├── BrawlerStatRow.tsx
    └── BrawlerListHeader.tsx                 # 검색 + StatRangeSelector

app/brawlers/[id]/
├── page.tsx                                  # 통계 섹션 추가
└── _components/
    ├── BrawlerStatsSection.tsx               # 컨테이너 (StatRangeSelector + 자식)
    ├── DailyTrendChart.tsx
    ├── PairStatsTable.tsx
    ├── EnemyStatsTable.tsx
    └── ItemOwnershipSection.tsx              # TrophyRangeSelector + 5 카테고리 카드

app/events/[id]/
├── page.tsx                                  # 통계 영역 채우기 (mode.resultType 분기)
└── _components/
    ├── EventStatsSection.tsx                 # 분기 컨테이너 (StatRangeSelector)
    ├── EventResultStatsTable.tsx             # RESULT 메인 표
    ├── EventRankStatsTable.tsx               # RANK 메인 표
    └── BrawlerSelectedPanel.tsx              # ?brawlerId= 시 하단 페어/적군 패널

tests/unit/domain/statistics/
├── enums.test.ts
├── sort.test.ts
├── tier.test.ts
└── mappers.test.ts

tests/unit/components/statistics/
├── TierBadge.test.tsx
├── StatRangeSelector.test.tsx
└── SortableHeader.test.tsx

tests/e2e/statistics.spec.ts                  # 6개 시나리오
```

**파일당 1개 컴포넌트/클래스 원칙** (AGENTS.md). 도메인 enum 라벨은 `enums.ts` 한 파일에서 모음 — 강하게 연결된 그룹.

## Domain Model

```ts
// src/domain/statistics/enums.ts
export const STATS_TIER_RANGES = [
  "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
  "SOLO_RANK_MYTHIC_PLUS", "SOLO_RANK_LEGENDARY_PLUS", "SOLO_RANK_MASTER_PLUS",
] as const;
export type StatsTierRange = typeof STATS_TIER_RANGES[number];

export const STATS_DATE_RANGES = [
  "THIS_WEEK", "SINCE_LAST_WEEK", "LAST_WEEK",
  "SINCE_TWO_WEEKS_AGO", "TWO_WEEKS_AGO",
  "THIS_MONTH", "SINCE_LAST_MONTH", "LAST_MONTH",
] as const;
export type StatsDateRange = typeof STATS_DATE_RANGES[number];

export const BRAWLER_TROPHY_RANGES = [
  "TROPHY_500_PLUS", "TROPHY_1000_PLUS", "TROPHY_2000_PLUS", "TROPHY_3000_PLUS",
] as const;
export type BrawlerTrophyRange = typeof BRAWLER_TROPHY_RANGES[number];

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

export function parseTierRange(value: unknown): StatsTierRange;       // fallback "TROPHY_1000_PLUS"
export function parseDateRange(value: unknown): StatsDateRange;       // fallback "SINCE_LAST_WEEK"
export function parseTrophyRange(value: unknown): BrawlerTrophyRange; // fallback "TROPHY_1000_PLUS"
```

```ts
// src/domain/statistics/sort.ts
export type ResultStatsSortBy = "PICK_RATE" | "WIN_RATE";
export type RankStatsSortBy = "PICK_RATE" | "AVERAGE_RANK";
export type SortOrder = "ASC" | "DESC";

export function parseResultSortBy(value: unknown): ResultStatsSortBy; // fallback "PICK_RATE"
export function parseRankSortBy(value: unknown): RankStatsSortBy;     // fallback "PICK_RATE"
export function parseSortOrder(value: unknown): SortOrder;            // fallback "DESC"
```

```ts
// src/domain/statistics/tier.ts
export type StatTier = "S" | "A" | "B" | "C" | "D";

export const TIER_COLOR: Record<StatTier, string> = {
  S: "#FFC60A",  // brand yellow
  A: "#FF3A3A",  // red
  B: "#FF8A1F",  // orange
  C: "#2BD576",  // green
  D: "#5C6271",  // muted
};

export function parseTier(raw: unknown): StatTier;  // 알 수 없으면 "D"
```

```ts
// src/domain/statistics/types.ts
import type { Brawler } from "@/domain/brawler/types";

export type BrawlerResultStat = {
  brawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;        // 0~1
  winRate: number;         // 0~1
  starPlayerRate: number;  // 0~1
  tierScore: number;
  tier: StatTier;
};

export type BrawlerResultStatistics = {
  totalBattleCount: number;
  items: BrawlerResultStat[];
};

export type BrawlerRankStat = {
  brawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;     // 1.00 ~ N.00
};

export type PairResultStat = {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
};

export type PairRankStat = {
  brawler: Brawler | null;
  pairBrawler: Brawler | null;
  totalBattleCount: number;
  averageRank: number;
};

export type EnemyResultStat = {
  brawler: Brawler | null;
  enemyBrawler: Brawler | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
};

export type DailyResultStat = {
  date: string;            // ISO yyyy-MM-dd
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
};

export type ItemOwnershipItem = {
  itemId: number;
  ownershipRate: number;   // 0~1
  ownerCount: number;
  totalCount: number;
};

export type BuffyOwnershipItem = {
  buffyType: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
};

export type ItemOwnership = {
  gears: ItemOwnershipItem[];
  gadgets: ItemOwnershipItem[];
  starPowers: ItemOwnershipItem[];
  hyperCharges: ItemOwnershipItem[];
  buffies: BuffyOwnershipItem[];
};
```

## API Layer

```ts
// src/api/endpoints/statistics.ts
type ListResponse<T> = { content: T[] };

export const statisticsApi = {
  brawlerResult: (
    p: { tierRange: StatsTierRange; dateRange: StatsDateRange; eventId?: number },
    opts?: FetchOpts,
  ) => apiFetch<BrawlerResultStatisticsResponse>(buildUrl("/v1/statistics/brawler/result", p), opts),

  brawlerRank: (
    p: { eventId: number; tierRange: StatsTierRange; dateRange: StatsDateRange },
    opts?: FetchOpts,
  ) => apiFetch<ListResponse<BrawlerRankStatisticsResponse>>(buildUrl("/v1/statistics/brawler/rank", p), opts),

  pairResult: (
    p: { brawlerId: number; tierRange; dateRange; eventId?: number; sortBy?: ResultStatsSortBy; sortOrder?: SortOrder },
    opts?: FetchOpts,
  ) => apiFetch<ListResponse<BrawlerPairResultStatisticsItemResponse>>(buildUrl("/v1/statistics/brawler/pair/result", p), opts),

  pairRank: (
    p: { eventId: number; brawlerId: number; tierRange; dateRange; sortBy?: RankStatsSortBy; sortOrder?: SortOrder },
    opts?: FetchOpts,
  ) => apiFetch<ListResponse<BrawlerPairRankStatisticsResponse>>(buildUrl("/v1/statistics/brawler/pair/rank", p), opts),

  enemyResult: (
    p: { brawlerId: number; tierRange; dateRange; eventId?: number; sortBy?; sortOrder? },
    opts?: FetchOpts,
  ) => apiFetch<ListResponse<BrawlerEnemyResultStatisticsItemResponse>>(buildUrl("/v1/statistics/brawler/enemy/result", p), opts),

  daily: (
    p: { brawlerId: number; tierRange; dateRange },
    opts?: FetchOpts,
  ) => apiFetch<ListResponse<BrawlerResultDailyStatisticsResponse>>(buildUrl("/v1/statistics/brawler/result/daily", p), opts),

  itemOwnership: (
    p: { brawlerId: number; trophyRange: BrawlerTrophyRange },
    opts?: FetchOpts,
  ) => apiFetch<BrawlerItemOwnershipStatisticsResponse>(buildUrl("/v1/statistics/brawler/item/ownership", p), opts),
};
```

`buildUrl` 은 `null/undefined` 파라미터 자동 제거.

## Pages

### `/brawlers`

- RSC: 두 fetch 병렬 (brawler list + result statistics).
- 통계 없는 브롤러도 표 행으로 노출(0/0/0, tier=D).
- 헤더: 검색 input(클라이언트 substring 필터, 기존 유지) + `StatRangeSelector` (tier + date).
- 표 컬럼: `# | 브롤러 | tier | winRate | pickRate | starPlayerRate | 전투수`.
- 정렬: 기본 `tierScore DESC`. 컬럼 헤더 클릭 → URL `?sort=&order=` 갱신.
- 모바일: pickRate/starPlayerRate/전투수 컬럼 숨김.
- 빈 응답(전체 0건): "해당 조건에 데이터가 없습니다" 카드.

### `/brawlers/[id]`

- 기존 헤더/개요 + 하단 통계 섹션 추가.
- RSC: 5개 fetch 병렬 (brawler detail + daily + pair + enemy + ownership).
- 섹션 상단 `StatRangeSelector` (daily/pair/enemy 모두에 영향) — `trophy` 는 별도 셀렉터로 보유율에만 영향.
- Sub-section:
  1. `DailyTrendChart` — recharts LineChart, X=date, Y=winRate(0~100%) + starPlayerRate 점선
  2. `PairStatsTable` — `# | 페어 | 픽률 | 승률 | 전투수`, URL `?pairSort=&pairOrder=`
  3. `EnemyStatsTable` — `# | 적군 | 픽률 | 승률 | 전투수`, URL `?enemySort=&enemyOrder=`
  4. `ItemOwnershipSection` — `TrophyRangeSelector` + 5 카테고리 카드(빈 카테고리는 카드 자체 숨김)
- 빈 데이터: 각 sub-section 별 "데이터 없음" 캡션.

### `/events/[id]`

- 기존 헤더 + `EventStatsSection` 추가 (placeholder 채움).
- `event.mode.resultType` 분기:
  - `"RESULT"` → `EventResultStatsTable` (winRate, pickRate, starPlayerRate, tier, tierScore DESC 기본)
  - `"RANK"` → `EventRankStatsTable` (averageRank, pickRate, pickRate DESC 기본)
- 행 클릭 → `?brawlerId=<id>` URL 갱신(soft navigation, scroll: false). 같은 행 재클릭 → 파라미터 제거.
- 활성 행 시각: 옐로우 좌측 4px 바 + 행 배경 옐로우 틴트(`#FFE082`).
- `?brawlerId=` 있을 때 `BrawlerSelectedPanel` 하단 표시:
  - RESULT: 페어 표 + 적군 표 (desktop 좌우 2열, mobile 스택)
  - RANK: 페어 표만, 우측 "적군 통계는 결과형 모드 전용입니다"
- 패널 상단 닫기(X) 버튼 → `?brawlerId=` 제거.
- 정렬: 컬럼 헤더 클릭. 페어 `?pairSort=&pairOrder=`, 적군 `?enemySort=&enemyOrder=`, 메인 표 `?sort=&order=`.

## Common Components

### `StatRangeSelector`
- props: `tier`, `date`, `pathname` (선택적 — 자식이 직접 router.replace)
- 클라이언트 컴포넌트 (`'use client'`).
- `useRouter`/`useSearchParams`/`usePathname` 으로 기존 쿼리 보존하면서 `tier`/`date` 갱신.
- 기본값과 같으면 URL 에서 해당 파라미터 제거(깨끗한 URL).
- shadcn `Select`.

### `TrophyRangeSelector`
- 동일 패턴. 4단계.

### `SortableHeader`
- props: `column`, `currentSort`, `currentOrder`, `paramPrefix?` (예: `"pair"` → `pairSort`/`pairOrder`)
- 클릭: 같은 컬럼이면 order 토글, 다른 컬럼이면 해당 컬럼 + DESC.
- 시각: 활성 컬럼 ▲/▼.

### `TierBadge`
- props: `tier: StatTier`
- 28×28 원형, `TIER_COLOR[tier]` 배경 + 잉크 글리프.
- DESIGN.md 스티커 그림자 (`0 3px 0 rgba(0,0,0,0.35)`).

### `StatNumberCell`
- 0~1 값 → `(value*100).toFixed(1) + "%"`, `var(--font-num)` tabular-nums.
- `averageRank` 는 `value.toFixed(2)`.

## Caching

- ISR `revalidate: 600` (10분) — 모든 statistics fetch.
- 브롤러 목록은 `revalidate: 86400` 유지 (변하지 않음, 별도 fetch).
- 이벤트 단건은 `revalidate: 3600` 유지 (기존 spec).

## SEO

- 모든 페이지의 `generateMetadata` 는 기존 그대로 (통계는 본문 보강).
- sitemap 변경 없음.
- `?tier=&date=` 등 필터 변형은 noindex 처리하지 않음 — 메타 표 본질.

## Error Handling

- API 실패 → RSC throw → 가장 가까운 `error.tsx` (Foundation 패턴).
- 잘못된 쿼리 파라미터 → parser 안전 fallback (URL 보존).
- 잘못된 `brawlerId` (이벤트 표 클릭 시): 빈 배열 → 패널에 "데이터가 부족합니다".
- 잘못된 `eventId`: 기존 `notFound()` 처리.

## Tests

### 단위 (Vitest)
- `domain/statistics/enums.test.ts` — 3 parser (정상/대소문자/잘못된 값/null fallback)
- `domain/statistics/sort.test.ts` — sortBy/order parser
- `domain/statistics/tier.test.ts` — parseTier (S~D + 알 수 없는 값 → D)
- `domain/statistics/mappers.test.ts` — 7개 매퍼 (DTO → 도메인, brawler null 케이스)

### 컴포넌트 (Vitest + Testing Library)
- `TierBadge.test.tsx` — 5개 tier 컬러/라벨
- `StatRangeSelector.test.tsx` — 옵션 표시, 기본값일 때 URL 파라미터 제거
- `SortableHeader.test.tsx` — 클릭 토글, paramPrefix 적용

### E2E (Playwright, prod API)
- `/brawlers` 메타 표 진입 → 1위 브롤러 표시 → tier 변경 → 데이터 갱신
- `/brawlers/[id]` 진입 → 일별 차트 + 페어/적군 표 + 보유율 모두 표시
- `/brawlers/[id]?date=LAST_WEEK` 진입 → 데이터 다름 검증
- `/events/[id]` (RESULT) → 결과 표 → 행 클릭 → `?brawlerId=` URL → 페어/적군 패널 표시
- `/events/[id]` (RANK 쇼다운) → 평균 순위 표 → 행 클릭 → 페어 표 + "적군 통계 없음" 메시지
- 페어 표 헤더 클릭 → URL `?pairSort=WIN_RATE&pairOrder=desc` → 정렬 변경

## 위험 요소

- **`recharts` 의존성 추가**: ~50KB. 번들 영향 모니터.
- **이벤트 표 행 클릭 → 패널 RSC 추가 fetch**: soft-navigation 으로 새 RSC 응답. UX 지연 가능 → Suspense fallback 으로 처리.
- **솔로랭크 이벤트 + TROPHY_* 기본값**: 데이터 0건 응답 가능 → "데이터 없음" 메시지로 사용자에게 명확히 전달, SOLO_RANK_* 옵션을 셀렉터에 항상 노출하여 수동 전환 유도.
- **통계 0건 브롤러 표 행 노출**: 백엔드 응답에 없는 브롤러를 클라가 0% 행으로 보충 — 이름 정렬 시 위치, 통계 정렬 시 하단 묶음.
- **차트 로딩 시 누적 레이아웃 시프트**: 차트 영역 높이 고정(예: 240px)으로 CLS 회피.

## 완료 기준

- 7개 statistics 엔드포인트 → 각 페이지 노출
- 3개 페이지(`/brawlers`, `/brawlers/[id]`, `/events/[id]`) 통계 표시
- tier/date/trophy/sort/brawlerId 필터 URL 동작
- RANK/RESULT 자동 분기 동작
- 단위/컴포넌트 테스트 통과
- E2E 6개 시나리오 통과
- typecheck/lint 통과
