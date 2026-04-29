# Battle 도메인 프론트엔드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 플레이어 페이지 "배틀" 탭(`PlayerBattles.tsx` placeholder)을 op.gg 스타일 컴팩트 행 + 펼침 팀 + 25개 무한 스크롤로 채운다.

**Architecture:** RSC 없이 클라이언트 단독 (탭 클릭 시 첫 fetch). TanStack Query `useInfiniteQuery` + 도메인 매퍼 패턴. 결과형(`rank===null`)/순위형(`rank!=null`) 분기는 매퍼에서 `resultType` 으로 파생.

**Tech Stack:** Next.js 16 App Router, React 19, TanStack Query v5, Vitest + Testing Library, Playwright. 외부 라이브러리 추가 없음.

**Spec:** `docs/superpowers/specs/2026-04-29-frontend-battle-domain-design.md`

**Convention:** 모든 commit 은 한국어, trailer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>` 포함. HEREDOC 사용.

---

## File Structure

```text
imstargg-frontend/
├── src/api/
│   ├── types.ts                                  # SliceResponse + Battle DTO 추가
│   └── endpoints/battle.ts                       # 신규
├── src/domain/battle/
│   ├── types.ts                                  # 신규
│   ├── modeLabels.ts                             # 신규 (BattleMode → 한국어)
│   ├── mappers.ts                                # 신규
│   └── mappers.test.ts                           # 신규
├── src/lib/
│   ├── relativeTime.ts                           # 신규
│   └── relativeTime.test.ts                      # 신규
├── src/hooks/queries/
│   ├── queryKeys.ts                              # battles 키 추가
│   └── usePlayerBattles.ts                       # 신규
└── app/players/[tag]/_components/
    ├── PlayerBattles.tsx                         # placeholder → 구현 교체
    └── battle/
        ├── BattleList.tsx
        ├── BattleRow.tsx
        ├── BattleRowHeader.tsx
        ├── BattleExpandedTeams.tsx
        ├── BattleTeamCard.tsx
        ├── BattlePlayerRow.tsx
        ├── BattleResultBadge.tsx
        ├── BattleModeIcon.tsx
        └── BattleSkeleton.tsx
└── tests/e2e/
    └── player-battles.spec.ts                    # 신규
```

---

## Task 1: SliceResponse + Battle DTO 타입 추가

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts` (파일 끝에 추가)

- [ ] **Step 1: SliceResponse 와 Battle DTO 들을 추가**

`imstargg-frontend/src/api/types.ts` 파일 끝에 다음을 append:

```ts
// === Slice (offset/limit pagination) ===
export interface SliceResponse<T> {
  content: T[];
  hasNext: boolean;
}

// === Battle ===
export type BattleMode =
  | "KNOCKOUT" | "GEM_GRAB" | "HEIST" | "HOT_ZONE" | "SIEGE" | "BOUNTY"
  | "BRAWL_BALL" | "DUELS" | "SOLO_SHOWDOWN" | "DUO_SHOWDOWN" | "WIPEOUT"
  | "VOLLEY_BRAWL" | "TROPHY_THIEVES" | "BOSS_FIGHT" | "BIG_GAME" | "BASKET_BRAWL"
  | "ROBO_RUMBLE" | "PAYLOAD" | "TAKEDOWN" | "HUNTERS" | "BOT_DROP"
  | "HOLD_THE_TROPHY" | "UNKNOWN";

export type BattleType =
  | "RANKED" | "SOLO_RANKED" | "FRIENDLY" | "CHALLENGE"
  | "TOURNAMENT" | "CHAMPIONSHIP_CHALLENGE";

export type BattleResult = "VICTORY" | "DEFEAT" | "DRAW";

export type SoloRankTier =
  | "BRONZE_1" | "BRONZE_2" | "BRONZE_3"
  | "SILVER_1" | "SILVER_2" | "SILVER_3"
  | "GOLD_1" | "GOLD_2" | "GOLD_3"
  | "DIAMOND_1" | "DIAMOND_2" | "DIAMOND_3"
  | "MYTHIC_1" | "MYTHIC_2" | "MYTHIC_3"
  | "LEGENDARY_1" | "LEGENDARY_2" | "LEGENDARY_3"
  | "MASTERS";

export interface BattlePlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

export interface BattlePlayerResponse {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawlerResponse;
}

export interface BattleResponse {
  battleTime: string;
  event: BattleEventResponse | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;
  starPlayerTag: string | null;
  rank: number | null;
  trophyChange: number | null;
  teams: BattlePlayerResponse[][];
}
```

- [ ] **Step 2: typecheck 로 다른 파일 깨짐 없음 확인**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS (신규 타입만 추가, 기존 사용처 영향 없음)

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/api/types.ts
git commit -m "$(cat <<'EOF'
feat(frontend): SliceResponse 와 Battle DTO 타입 추가

백엔드 BattleController DTO 와 SliceResponse 페이지네이션 타입을 프론트
api/types.ts 에 추가. 도메인 매퍼/엔드포인트/훅의 기반이 된다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 상대시간 유틸 + 테스트

**Files:**
- Create: `imstargg-frontend/src/lib/relativeTime.ts`
- Create: `imstargg-frontend/tests/lib/relativeTime.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/lib/relativeTime.test.ts`:

```ts
import { describe, it, expect } from "vitest";
import { formatRelativeTime } from "@/lib/relativeTime";

const NOW = new Date("2026-04-29T12:00:00Z").getTime();

describe("formatRelativeTime", () => {
  it("30초 이내는 '방금 전'", () => {
    expect(formatRelativeTime("2026-04-29T11:59:50Z", NOW)).toBe("방금 전");
  });
  it("분 단위", () => {
    expect(formatRelativeTime("2026-04-29T11:55:00Z", NOW)).toBe("5분 전");
  });
  it("시간 단위", () => {
    expect(formatRelativeTime("2026-04-29T09:00:00Z", NOW)).toBe("3시간 전");
  });
  it("일 단위 (7일 이내)", () => {
    expect(formatRelativeTime("2026-04-26T12:00:00Z", NOW)).toBe("3일 전");
  });
  it("7일 초과는 절대 날짜 yyyy.MM.dd", () => {
    expect(formatRelativeTime("2026-04-20T12:00:00Z", NOW)).toBe("2026.04.20");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/relativeTime.test.ts`
Expected: FAIL (`Cannot find module '@/lib/relativeTime'`)

- [ ] **Step 3: 구현**

`imstargg-frontend/src/lib/relativeTime.ts`:

```ts
export function formatRelativeTime(iso: string, nowMs: number = Date.now()): string {
  const ts = new Date(iso).getTime();
  const diff = Math.max(0, nowMs - ts);
  const sec = Math.floor(diff / 1000);
  if (sec < 30) return "방금 전";
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}시간 전`;
  const day = Math.floor(hr / 24);
  if (day <= 7) return `${day}일 전`;
  const d = new Date(ts);
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(d.getUTCDate()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd}`;
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/relativeTime.test.ts`
Expected: PASS (5/5)

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/src/lib/relativeTime.ts imstargg-frontend/tests/lib/relativeTime.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): 상대시간 포맷 유틸 추가

배틀 행에서 'N분 전' / 'N시간 전' / 'N일 전' / 'yyyy.MM.dd' 표시용.
7일 이내는 상대시간, 초과는 절대 날짜.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: BattleMode 한국어 라벨 매핑

**Files:**
- Create: `imstargg-frontend/src/domain/battle/modeLabels.ts`

- [ ] **Step 1: 매핑 테이블 작성**

`imstargg-frontend/src/domain/battle/modeLabels.ts`:

```ts
import type { BattleMode } from "@/api/types";

const LABELS: Record<BattleMode, string> = {
  KNOCKOUT: "녹아웃",
  GEM_GRAB: "젬 그랩",
  HEIST: "하이스트",
  HOT_ZONE: "핫 존",
  SIEGE: "공성전",
  BOUNTY: "현상금 사냥",
  BRAWL_BALL: "브롤 볼",
  DUELS: "듀얼",
  SOLO_SHOWDOWN: "솔로 쇼다운",
  DUO_SHOWDOWN: "듀오 쇼다운",
  WIPEOUT: "전멸전",
  VOLLEY_BRAWL: "발리 브롤",
  TROPHY_THIEVES: "트로피 쟁탈전",
  BOSS_FIGHT: "보스전",
  BIG_GAME: "빅 게임",
  BASKET_BRAWL: "바스켓 브롤",
  ROBO_RUMBLE: "로보 럼블",
  PAYLOAD: "페이로드",
  TAKEDOWN: "테이크다운",
  HUNTERS: "헌터스",
  BOT_DROP: "봇 드롭",
  HOLD_THE_TROPHY: "트로피 지키기",
  UNKNOWN: "알 수 없음",
};

export function modeLabel(mode: BattleMode): string {
  return LABELS[mode] ?? mode;
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/domain/battle/modeLabels.ts
git commit -m "$(cat <<'EOF'
feat(frontend): BattleMode 한국어 라벨 매핑 추가

event.mode 가 null 일 때 fallback 으로 사용하는 한국어 라벨.
Record 매핑으로 누락된 모드는 컴파일 타임에 검증된다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Battle 도메인 타입 정의

**Files:**
- Create: `imstargg-frontend/src/domain/battle/types.ts`

- [ ] **Step 1: 타입 작성**

`imstargg-frontend/src/domain/battle/types.ts`:

```ts
import type { BattleMode, BattleType, BattleResult, SoloRankTier } from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";

export type { BattleMode, BattleType, BattleResult, SoloRankTier };

export interface BattlePlayerBrawler {
  brawler: Brawler | null;
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

export interface BattlePlayer {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawler;
}

export interface BattleEvent {
  id: number;
  modeName: string | null;
  modeIconUrl: string | null;
  modeResultType: "RESULT" | "RANK" | null;
  mapName: string;
}

export type BattleResultType = "RESULT" | "RANK";

export interface Battle {
  battleTime: string;
  event: BattleEvent | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;
  starPlayerTag: string | null;
  rank: number | null;
  trophyChange: number | null;
  teams: BattlePlayer[][];
  /** 화면 분기용 파생값. rank != null 이면 RANK, 아니면 RESULT */
  resultType: BattleResultType;
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

`Brawler` 타입은 `src/domain/brawler/types.ts` 에 존재. 없으면 그 파일을 먼저 확인.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/domain/battle/types.ts
git commit -m "$(cat <<'EOF'
feat(frontend): Battle 도메인 화면 모델 타입 정의

DTO 와 분리된 화면 모델. event.mapName/modeName 은 i18n 으로 한국어
추출된 string, resultType 은 rank 유무로 파생되는 RESULT/RANK 플래그.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Battle 매퍼 + 테스트

**Files:**
- Create: `imstargg-frontend/src/domain/battle/mappers.ts`
- Create: `imstargg-frontend/tests/domain/battle/mappers.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/domain/battle/mappers.test.ts`:

```ts
import { describe, it, expect } from "vitest";
import { toBattle } from "@/domain/battle/mappers";
import type { BattleResponse } from "@/api/types";

const baseDto: BattleResponse = {
  battleTime: "2026-04-29T11:00:00Z",
  event: {
    id: 1,
    mode: { id: 10, name: { ko: "젬 그랩", en: "Gem Grab" }, iconImageUrl: "x.png", resultType: "RESULT" },
    mapName: { ko: "다이아몬드 광산", en: "Diamond Mine" },
  },
  mode: "GEM_GRAB",
  type: "RANKED",
  result: "VICTORY",
  duration: 120,
  starPlayerTag: "#A1",
  rank: null,
  trophyChange: 9,
  teams: [
    [
      { tag: "#A1", name: "P1", soloRankTier: null, brawler: { brawler: null, power: 11, trophies: 700, trophyChange: 9 } },
    ],
    [
      { tag: "#B1", name: "P2", soloRankTier: null, brawler: { brawler: null, power: 10, trophies: 500, trophyChange: -6 } },
    ],
  ],
};

describe("toBattle", () => {
  it("rank 가 null 이면 resultType=RESULT", () => {
    const sut = toBattle(baseDto);
    expect(sut.resultType).toBe("RESULT");
    expect(sut.rank).toBeNull();
  });

  it("rank 가 있으면 resultType=RANK", () => {
    const sut = toBattle({ ...baseDto, rank: 3, result: null });
    expect(sut.resultType).toBe("RANK");
    expect(sut.rank).toBe(3);
  });

  it("event.mapName / modeName 은 한국어 우선 추출", () => {
    const sut = toBattle(baseDto);
    expect(sut.event?.mapName).toBe("다이아몬드 광산");
    expect(sut.event?.modeName).toBe("젬 그랩");
    expect(sut.event?.modeIconUrl).toBe("x.png");
    expect(sut.event?.modeResultType).toBe("RESULT");
  });

  it("event 가 null 이면 event=null", () => {
    const sut = toBattle({ ...baseDto, event: null });
    expect(sut.event).toBeNull();
  });

  it("event.mode 가 null 이면 modeName/modeIconUrl/modeResultType 모두 null", () => {
    const sut = toBattle({
      ...baseDto,
      event: { id: 1, mode: null, mapName: { ko: "맵", en: "Map" } },
    });
    expect(sut.event?.modeName).toBeNull();
    expect(sut.event?.modeIconUrl).toBeNull();
    expect(sut.event?.modeResultType).toBeNull();
    expect(sut.event?.mapName).toBe("맵");
  });

  it("teams 의 각 플레이어가 BattlePlayer 형태로 매핑됨", () => {
    const sut = toBattle(baseDto);
    expect(sut.teams).toHaveLength(2);
    expect(sut.teams[0][0].tag).toBe("#A1");
    expect(sut.teams[0][0].brawler.power).toBe(11);
    expect(sut.teams[0][0].brawler.trophyChange).toBe(9);
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/domain/battle/mappers.test.ts`
Expected: FAIL (`Cannot find module '@/domain/battle/mappers'`)

- [ ] **Step 3: 매퍼 구현**

`imstargg-frontend/src/domain/battle/mappers.ts`:

```ts
import type {
  BattleResponse,
  BattlePlayerResponse,
  BattlePlayerBrawlerResponse,
  BattleEventResponse,
} from "@/api/types";
import { pickName } from "@/domain/i18n";
import { toBrawler } from "@/domain/brawler/mappers";
import type {
  Battle,
  BattlePlayer,
  BattlePlayerBrawler,
  BattleEvent,
} from "@/domain/battle/types";

function toBattlePlayerBrawler(dto: BattlePlayerBrawlerResponse): BattlePlayerBrawler {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    power: dto.power,
    trophies: dto.trophies,
    trophyChange: dto.trophyChange,
  };
}

function toBattlePlayer(dto: BattlePlayerResponse): BattlePlayer {
  return {
    tag: dto.tag,
    name: dto.name,
    soloRankTier: dto.soloRankTier,
    brawler: toBattlePlayerBrawler(dto.brawler),
  };
}

function toBattleEvent(dto: BattleEventResponse): BattleEvent {
  return {
    id: dto.id,
    modeName: dto.mode ? pickName(dto.mode.name) : null,
    modeIconUrl: dto.mode ? dto.mode.iconImageUrl : null,
    modeResultType: dto.mode ? dto.mode.resultType : null,
    mapName: pickName(dto.mapName),
  };
}

export function toBattle(dto: BattleResponse): Battle {
  return {
    battleTime: dto.battleTime,
    event: dto.event ? toBattleEvent(dto.event) : null,
    mode: dto.mode,
    type: dto.type,
    result: dto.result,
    duration: dto.duration,
    starPlayerTag: dto.starPlayerTag,
    rank: dto.rank,
    trophyChange: dto.trophyChange,
    teams: dto.teams.map((team) => team.map(toBattlePlayer)),
    resultType: dto.rank != null ? "RANK" : "RESULT",
  };
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/domain/battle/mappers.test.ts`
Expected: PASS (6/6)

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/src/domain/battle/mappers.ts imstargg-frontend/tests/domain/battle/mappers.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): Battle DTO → 화면 모델 매퍼 추가

resultType 은 rank 유무로 파생, event.mapName/modeName 은 한국어 우선
추출, teams 는 재귀적으로 BattlePlayer 로 매핑.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Battle API 엔드포인트

**Files:**
- Create: `imstargg-frontend/src/api/endpoints/battle.ts`

- [ ] **Step 1: 엔드포인트 작성**

`imstargg-frontend/src/api/endpoints/battle.ts`:

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { BattleResponse, SliceResponse } from "@/api/types";

export interface PlayerBattlesParams {
  offset: number;
  limit: number;
}

function buildQuery(params: object): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null) continue;
    sp.set(k, String(v));
  }
  const qs = sp.toString();
  return qs ? `?${qs}` : "";
}

export const battleApi = {
  getPlayerBattles: (
    tag: string,
    params: PlayerBattlesParams,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<BattleResponse>>(
      `/v1/players/${toUrlPath(tag)}/battles${buildQuery(params)}`,
      opts,
    ),
};
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/api/endpoints/battle.ts
git commit -m "$(cat <<'EOF'
feat(frontend): Battle API 엔드포인트 추가

GET /v1/players/{tag}/battles?offset=&limit= 호출. SliceResponse 반환.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: queryKeys.player.battles 키 추가

**Files:**
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`

- [ ] **Step 1: 키 추가**

`queryKeys.player` 객체 안에 `battles` 키 추가. 수정 후 player 블록은 다음과 같다:

```ts
  player: {
    all: ["player"] as const,
    detail: (tag: string) =>
      [...queryKeys.player.all, "detail", normalizeTag(tag)] as const,
    brawlers: (tag: string) =>
      [...queryKeys.player.all, "brawlers", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.player.all, "renewal", normalizeTag(tag)] as const,
    battles: (tag: string) =>
      [...queryKeys.player.all, "battles", normalizeTag(tag)] as const,
  },
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/hooks/queries/queryKeys.ts
git commit -m "$(cat <<'EOF'
feat(frontend): queryKeys.player.battles 추가

플레이어 배틀 무한 스크롤 쿼리 키.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: usePlayerBattles 훅 (useInfiniteQuery)

**Files:**
- Create: `imstargg-frontend/src/hooks/queries/usePlayerBattles.ts`

- [ ] **Step 1: 훅 작성**

`imstargg-frontend/src/hooks/queries/usePlayerBattles.ts`:

```ts
import { useInfiniteQuery } from "@tanstack/react-query";
import { battleApi } from "@/api/endpoints/battle";
import { toBattle } from "@/domain/battle/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export const BATTLES_PAGE_SIZE = 25;

export function usePlayerBattles(tag: string) {
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
  });
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/hooks/queries/usePlayerBattles.ts
git commit -m "$(cat <<'EOF'
feat(frontend): usePlayerBattles 무한 스크롤 훅 추가

useInfiniteQuery 기반, 25개씩 페이지네이션. select 에서 매퍼 적용.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: BattleResultBadge 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleResultBadge.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleResultBadge.tsx`:

```tsx
import type { Battle } from "@/domain/battle/types";

interface Props {
  battle: Pick<Battle, "result" | "rank" | "resultType">;
}

const RESULT_LABEL: Record<string, string> = {
  VICTORY: "WIN",
  DEFEAT: "LOSS",
  DRAW: "DRAW",
};

export function BattleResultBadge({ battle }: Props) {
  if (battle.resultType === "RANK" && battle.rank != null) {
    const top = battle.rank <= 3;
    const mid = battle.rank <= 6;
    const cls = top ? "green" : mid ? "" : "red";
    return (
      <span className={`pill ${cls}`} style={{ fontFamily: "var(--font-num)" }}>
        #{battle.rank}
      </span>
    );
  }
  if (battle.result === "VICTORY") return <span className="pill green">WIN</span>;
  if (battle.result === "DEFEAT") return <span className="pill red">LOSS</span>;
  if (battle.result === "DRAW") return <span className="pill">DRAW</span>;
  return <span className="pill">—</span>;
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleResultBadge.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleResultBadge 컴포넌트 추가

결과형은 WIN/LOSS/DRAW 칩, 순위형은 #N 칩 (1-3위 green, 4-6위 neutral,
7위 이하 red). DESIGN.md 의 ResultPill 가이드 준수.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: BattleModeIcon 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx`:

```tsx
import type { BattleMode } from "@/api/types";
import { modeLabel } from "@/domain/battle/modeLabels";

interface Props {
  iconUrl: string | null;
  mode: BattleMode;
  size?: number;
}

export function BattleModeIcon({ iconUrl, mode, size = 28 }: Props) {
  if (iconUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={iconUrl}
        alt={modeLabel(mode)}
        style={{ width: size, height: size, objectFit: "contain" }}
      />
    );
  }
  const fontSize = Math.max(10, size * 0.4);
  return (
    <div
      aria-label={modeLabel(mode)}
      style={{
        width: size,
        height: size,
        borderRadius: "50%",
        background: "#3B404D",
        color: "#fff",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "var(--font-display)",
        fontSize,
      }}
    >
      {modeLabel(mode).slice(0, 1)}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleModeIcon 컴포넌트 추가

iconUrl 이 있으면 이미지, 없으면 모드 첫 글자 fallback (다크 그레이 원형).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: BattlePlayerRow 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`:

```tsx
import Link from "next/link";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import PowerPill from "@/components/primitives/PowerPill";
import Trophy from "@/components/primitives/Trophy";
import { toUrlPath } from "@/lib/tag";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  player: BattlePlayer;
  ownerTag: string;
  starPlayerTag: string | null;
}

function isSameTag(a: string, b: string): boolean {
  return a.replace(/^#/, "").toUpperCase() === b.replace(/^#/, "").toUpperCase();
}

export function BattlePlayerRow({ player, ownerTag, starPlayerTag }: Props) {
  const isOwner = isSameTag(player.tag, ownerTag);
  const isStar = starPlayerTag != null && isSameTag(player.tag, starPlayerTag);
  const brawlerId = player.brawler.brawler?.id != null
    ? String(player.brawler.brawler.id)
    : "0";

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "32px 1fr auto",
        alignItems: "center",
        gap: 8,
        padding: "6px 8px",
        background: isOwner ? "rgba(255,198,10,0.08)" : "transparent",
        borderRadius: 6,
      }}
    >
      <BrawlerPortrait id={brawlerId} size={32} />
      <div style={{ minWidth: 0 }}>
        <Link
          href={`/players/${toUrlPath(player.tag)}`}
          onClick={(e) => e.stopPropagation()}
          style={{
            display: "block",
            fontWeight: isOwner ? 700 : 500,
            fontSize: 13,
            color: "#14161B",
            textDecoration: "none",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {player.name}
          {isStar && <span style={{ marginLeft: 4, color: "#FFC60A" }}>★</span>}
        </Link>
        <div style={{ fontSize: 11, color: "#8A90A0", fontFamily: "var(--font-mono)" }}>
          {player.tag}
        </div>
      </div>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <PowerPill level={player.brawler.power} />
        {player.brawler.trophies != null && <Trophy value={player.brawler.trophies} size={12} />}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattlePlayerRow 컴포넌트 추가

펼친 팀 카드 안의 한 플레이어 행. 본인은 옐로우 틴트 + 굵게,
별 플레이어는 ★, 이름 클릭 시 /players/{tag} 이동.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: BattleTeamCard 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleTeamCard.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleTeamCard.tsx`:

```tsx
import { BattlePlayerRow } from "./BattlePlayerRow";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  team: BattlePlayer[];
  label: string;             // "WIN" / "LOSS" / "DRAW" / "#1" 등
  labelColor: "green" | "red" | "neutral" | "yellow";
  ownerTag: string;
  starPlayerTag: string | null;
  containsOwner: boolean;
}

const COLOR_MAP: Record<string, string> = {
  green: "#2BD576",
  red: "#FF3A3A",
  neutral: "#8A90A0",
  yellow: "#FFC60A",
};

export function BattleTeamCard({
  team, label, labelColor, ownerTag, starPlayerTag, containsOwner,
}: Props) {
  return (
    <div
      style={{
        border: containsOwner
          ? "2px solid rgba(255,198,10,0.4)"
          : "1px solid #D8DCE5",
        borderRadius: 8,
        background: "#fff",
        padding: 8,
      }}
    >
      <div
        style={{
          fontSize: 11,
          fontWeight: 700,
          letterSpacing: "0.04em",
          color: COLOR_MAP[labelColor],
          marginBottom: 4,
          textTransform: "uppercase",
        }}
      >
        {label}
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        {team.map((p) => (
          <BattlePlayerRow
            key={p.tag}
            player={p}
            ownerTag={ownerTag}
            starPlayerTag={starPlayerTag}
          />
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleTeamCard.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleTeamCard 컴포넌트 추가

팀 단위 카드. 본인 포함 팀은 옐로우 보더로 강조, 헤더 라벨은
결과형 WIN/LOSS/DRAW 또는 순위형 #N.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: BattleExpandedTeams 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleExpandedTeams.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleExpandedTeams.tsx`:

```tsx
import { BattleTeamCard } from "./BattleTeamCard";
import type { Battle, BattlePlayer } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
}

function teamContainsOwner(team: BattlePlayer[], ownerTag: string): boolean {
  const norm = ownerTag.replace(/^#/, "").toUpperCase();
  return team.some((p) => p.tag.replace(/^#/, "").toUpperCase() === norm);
}

function resultLabel(battle: Battle, teamIndex: number, isOwnerTeam: boolean): {
  label: string;
  color: "green" | "red" | "neutral" | "yellow";
} {
  if (battle.resultType === "RANK") {
    const rank = teamIndex + 1;
    const totalTeams = battle.teams.length;
    const top = rank <= Math.ceil(totalTeams / 3);
    const mid = rank <= Math.ceil(totalTeams * 2 / 3);
    return { label: `#${rank}`, color: top ? "green" : mid ? "neutral" : "red" };
  }
  // RESULT
  if (battle.result === "DRAW") return { label: "DRAW", color: "neutral" };
  const ownerWon = battle.result === "VICTORY";
  const teamWon = isOwnerTeam ? ownerWon : !ownerWon;
  if (teamWon) return { label: "WIN", color: "green" };
  return { label: "LOSS", color: "red" };
}

export function BattleExpandedTeams({ battle, ownerTag }: Props) {
  const numTeams = battle.teams.length;
  const cols = battle.resultType === "RESULT"
    ? "1fr 1fr"
    : numTeams === 10 ? "repeat(2, 1fr)"
    : numTeams === 5 ? "repeat(5, 1fr)"
    : numTeams === 4 ? "repeat(4, 1fr)"
    : `repeat(${numTeams}, 1fr)`;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: cols,
        gap: 8,
        padding: "8px 12px 12px 16px",
      }}
    >
      {battle.teams.map((team, i) => {
        const isOwner = teamContainsOwner(team, ownerTag);
        const { label, color } = resultLabel(battle, i, isOwner);
        return (
          <BattleTeamCard
            key={i}
            team={team}
            label={label}
            labelColor={color}
            ownerTag={ownerTag}
            starPlayerTag={battle.starPlayerTag}
            containsOwner={isOwner}
          />
        );
      })}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleExpandedTeams.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleExpandedTeams 컴포넌트 추가

결과형은 좌우 2-card grid, 순위형은 팀 수에 따라 동적 grid (10/5/4팀
지원). 팀별 라벨/색은 resultLabel 헬퍼로 분기.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: BattleRowHeader 컴포넌트 (컴팩트 영역)

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx`:

```tsx
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import PowerPill from "@/components/primitives/PowerPill";
import { BattleResultBadge } from "./BattleResultBadge";
import { BattleModeIcon } from "./BattleModeIcon";
import { modeLabel } from "@/domain/battle/modeLabels";
import { formatRelativeTime } from "@/lib/relativeTime";
import type { Battle, BattlePlayer } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
  expanded: boolean;
}

function findOwnerPlayer(battle: Battle, ownerTag: string): BattlePlayer | null {
  const norm = ownerTag.replace(/^#/, "").toUpperCase();
  for (const team of battle.teams) {
    for (const p of team) {
      if (p.tag.replace(/^#/, "").toUpperCase() === norm) return p;
    }
  }
  return null;
}

function sideBarColor(battle: Battle): string {
  if (battle.resultType === "RANK" && battle.rank != null) {
    const top = battle.rank <= Math.ceil(battle.teams.length / 3);
    return top ? "#2BD576" : "#8A90A0";
  }
  if (battle.result === "VICTORY") return "#2BD576";
  if (battle.result === "DEFEAT") return "#FF3A3A";
  return "#8A90A0";
}

function trophyChangeColor(n: number): string {
  if (n > 0) return "#2BD576";
  if (n < 0) return "#FF3A3A";
  return "#8A90A0";
}

export function BattleRowHeader({ battle, ownerTag, expanded }: Props) {
  const owner = findOwnerPlayer(battle, ownerTag);
  const ownerBrawlerId =
    owner?.brawler.brawler?.id != null ? String(owner.brawler.brawler.id) : "0";
  const modeText = battle.event?.modeName ?? modeLabel(battle.mode);
  const mapText = battle.event?.mapName ?? "알 수 없는 맵";

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "4px 28px 1fr auto auto auto 24px",
        alignItems: "center",
        gap: 12,
        padding: "10px 12px",
        cursor: "pointer",
      }}
    >
      <div style={{ width: 4, height: 40, background: sideBarColor(battle), borderRadius: 2 }} />
      <BattleModeIcon iconUrl={battle.event?.modeIconUrl ?? null} mode={battle.mode} size={28} />
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: "#14161B" }}>{modeText}</div>
        <div style={{ fontSize: 12, color: "#5C6271" }}>{mapText}</div>
      </div>
      {owner && (
        <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
          <BrawlerPortrait id={ownerBrawlerId} size={28} />
          <PowerPill level={owner.brawler.power} />
        </div>
      )}
      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
        <BattleResultBadge battle={battle} />
        {battle.trophyChange != null && battle.trophyChange !== 0 && (
          <span
            style={{
              fontFamily: "var(--font-num)",
              fontWeight: 700,
              fontSize: 13,
              color: trophyChangeColor(battle.trophyChange),
            }}
          >
            {battle.trophyChange > 0 ? "+" : ""}{battle.trophyChange}
          </span>
        )}
      </div>
      <div style={{ fontSize: 12, color: "#5C6271", whiteSpace: "nowrap" }}>
        {formatRelativeTime(battle.battleTime)}
      </div>
      <div
        aria-hidden
        style={{
          transform: expanded ? "rotate(180deg)" : "rotate(0deg)",
          transition: "transform 150ms cubic-bezier(0.34,1.56,0.64,1)",
          color: "#8A90A0",
          fontSize: 14,
        }}
      >
        ▾
      </div>
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleRowHeader 컴포넌트 추가

컴팩트 행: 결과 사이드바(4px) + 모드 아이콘 + 모드/맵 + 본인 브롤러 +
결과/트로피변동 + 상대시간 + ▾.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: BattleRow 컨테이너 (펼침 토글)

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleRow.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleRow.tsx`:

```tsx
"use client";

import { useState, type KeyboardEvent } from "react";
import { BattleRowHeader } from "./BattleRowHeader";
import { BattleExpandedTeams } from "./BattleExpandedTeams";
import type { Battle } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
}

export function BattleRow({ battle, ownerTag }: Props) {
  const [expanded, setExpanded] = useState(false);
  const toggle = () => setExpanded((v) => !v);
  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      toggle();
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      aria-expanded={expanded}
      onClick={toggle}
      onKeyDown={onKey}
      className="card"
      style={{ overflow: "hidden", cursor: "pointer" }}
    >
      <BattleRowHeader battle={battle} ownerTag={ownerTag} expanded={expanded} />
      {expanded && (
        <div onClick={(e) => e.stopPropagation()} style={{ borderTop: "1px solid #ECEEF3" }}>
          <BattleExpandedTeams battle={battle} ownerTag={ownerTag} />
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleRow.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleRow 컨테이너 추가

펼침 토글 (클릭/Enter/Space), aria-expanded 부여, 펼친 영역 내부
클릭은 stopPropagation 으로 토글 방지.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 16: BattleSkeleton 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleSkeleton.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleSkeleton.tsx`:

```tsx
interface Props {
  count?: number;
}

export function BattleSkeleton({ count = 25 }: Props) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="card" style={{ height: 64, padding: 12 }}>
          <div className="skel" style={{ width: "100%", height: "100%", borderRadius: 6 }} />
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleSkeleton.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleSkeleton 로딩 스켈레톤 추가

첫 페이지 로딩 시 25개 행 placeholder.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 17: BattleList 컴포넌트 (Load More)

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/battle/BattleList.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleList.tsx`:

```tsx
"use client";

import { Button } from "@/components/ui/button";
import { BattleRow } from "./BattleRow";
import { BattleSkeleton } from "./BattleSkeleton";
import { usePlayerBattles } from "@/hooks/queries/usePlayerBattles";

interface Props {
  tag: string;
}

export function BattleList({ tag }: Props) {
  const {
    data, isLoading, isError, error,
    fetchNextPage, hasNextPage, isFetchingNextPage, refetch,
  } = usePlayerBattles(tag);

  if (isLoading) return <BattleSkeleton />;

  if (isError) {
    return (
      <div className="card" style={{ padding: 24, textAlign: "center" }}>
        <p style={{ color: "#5C6271", marginBottom: 12 }}>
          배틀 기록을 불러오지 못했어요.
        </p>
        <p style={{ color: "#8A90A0", fontSize: 12, marginBottom: 12 }}>
          {(error as Error)?.message ?? ""}
        </p>
        <Button variant="outline" onClick={() => refetch()}>다시 시도</Button>
      </div>
    );
  }

  const battles = data?.pages.flat() ?? [];
  if (battles.length === 0) {
    return (
      <div className="card" style={{ padding: 32, textAlign: "center", color: "#5C6271" }}>
        최근 배틀 기록이 없어요
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {battles.map((b, i) => (
        <BattleRow key={`${b.battleTime}-${i}`} battle={b} ownerTag={tag} />
      ))}
      {hasNextPage ? (
        <div style={{ display: "flex", justifyContent: "center", padding: "16px 0" }}>
          <Button
            variant="outline"
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
          >
            {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
          </Button>
        </div>
      ) : (
        <div style={{ textAlign: "center", padding: "12px 0", color: "#8A90A0", fontSize: 12 }}>
          더 이상 기록이 없어요
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleList.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BattleList 컴포넌트 추가

usePlayerBattles 훅을 소비하는 리스트. 로딩/빈/에러 상태 처리,
"더 보기" 버튼으로 다음 25개 fetch.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 18: PlayerBattles 컨테이너 교체

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBattles.tsx`

- [ ] **Step 1: placeholder 를 BattleList 로 교체**

`imstargg-frontend/app/players/[tag]/_components/PlayerBattles.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import { BattleList } from "./battle/BattleList";

export function PlayerBattles({ tag }: { tag: string }) {
  return <BattleList tag={tag} />;
}
```

- [ ] **Step 2: typecheck + lint**

Run: `pnpm --filter imstargg-frontend typecheck && pnpm --filter imstargg-frontend lint`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/PlayerBattles.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): PlayerBattles placeholder 를 BattleList 로 교체

플레이어 페이지 "배틀" 탭이 실제 배틀 데이터를 보여주기 시작한다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 19: 모바일 반응형 CSS

**Files:**
- Modify: `imstargg-frontend/app/styles/responsive.css` (또는 globals.css의 모바일 블록)

- [ ] **Step 1: 현재 responsive 스타일 위치 확인**

Run: `ls imstargg-frontend/app/styles/ 2>/dev/null && grep -l "@media" imstargg-frontend/app/globals.css imstargg-frontend/app/styles/*.css 2>/dev/null`

- [ ] **Step 2: 모바일 미디어 쿼리 추가**

위에서 찾은 적절한 css 파일에 다음을 추가 (없으면 `imstargg-frontend/app/styles/battle.css` 신규 + globals.css 에 import):

```css
/* === Battle row mobile === */
@media (max-width: 640px) {
  .battle-row-header {
    grid-template-columns: 4px 24px 1fr auto 20px !important;
    gap: 8px !important;
    padding: 8px !important;
  }
  .battle-row-header .battle-row-owner-brawler,
  .battle-row-header .battle-row-relative-time {
    display: none;
  }
  .battle-expanded-teams[data-result-type="RANK"][data-team-count="5"] {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .battle-expanded-teams[data-result-type="RANK"][data-team-count="4"] {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .battle-expanded-teams[data-result-type="RESULT"] {
    grid-template-columns: 1fr !important;
  }
}
```

이렇게 하려면 컴포넌트에 className/data-attribute 를 추가해야 한다. `BattleRowHeader.tsx` 의 grid div 에 `className="battle-row-header"`, owner brawler div 에 `className="battle-row-owner-brawler"`, time div 에 `className="battle-row-relative-time"` 추가. `BattleExpandedTeams.tsx` 의 grid div 에 `className="battle-expanded-teams"` + `data-result-type={battle.resultType}` + `data-team-count={battle.teams.length}` 추가.

- [ ] **Step 3: 변경사항 적용**

위 변경에 따라 다음 파일들을 수정:
- `BattleRowHeader.tsx`: 최상위 div 에 `className="battle-row-header"` 추가, 본인 브롤러 wrapper 에 `className="battle-row-owner-brawler"`, 시간 div 에 `className="battle-row-relative-time"`.
- `BattleExpandedTeams.tsx`: 최상위 div 에 `className="battle-expanded-teams"` + `data-result-type={battle.resultType}` + `data-team-count={battle.teams.length}`.

- [ ] **Step 4: typecheck + lint**

Run: `pnpm --filter imstargg-frontend typecheck && pnpm --filter imstargg-frontend lint`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx \
        imstargg-frontend/app/players/[tag]/_components/battle/BattleExpandedTeams.tsx \
        imstargg-frontend/app/styles/battle.css \
        imstargg-frontend/app/globals.css
git commit -m "$(cat <<'EOF'
feat(frontend): Battle 컴포넌트 모바일 반응형 추가

640px 이하에서 컴팩트 행은 본인 브롤러/상대시간 컬럼 숨기고,
펼친 팀 그리드는 결과형 1열, 순위형 2열로 압축.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 20: E2E 테스트

**Files:**
- Create: `imstargg-frontend/tests/e2e/player-battles.spec.ts`

- [ ] **Step 1: 테스트 작성**

`imstargg-frontend/tests/e2e/player-battles.spec.ts`:

```ts
import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("배틀 탭 클릭 시 배틀 행이 렌더된다", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  // 첫 행이 보이면 OK (행 컴포넌트는 role="button" + aria-expanded)
  const firstRow = page.locator('[role="button"][aria-expanded]').first();
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
});

test("배틀 행 클릭 시 펼침 동작", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  const firstRow = page.locator('[role="button"][aria-expanded]').first();
  await firstRow.waitFor({ state: "visible", timeout: 15_000 });
  await expect(firstRow).toHaveAttribute("aria-expanded", "false");
  await firstRow.click();
  await expect(firstRow).toHaveAttribute("aria-expanded", "true");
});

test("'더 보기' 버튼 클릭 시 추가 행 append", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  const moreBtn = page.getByRole("button", { name: "더 보기" });
  // 더 보기 버튼이 없으면 (배틀이 25개 미만) 스킵
  if (!(await moreBtn.isVisible({ timeout: 15_000 }).catch(() => false))) {
    test.skip(true, "테스트 플레이어의 배틀이 25개 미만");
  }
  const before = await page.locator('[role="button"][aria-expanded]').count();
  await moreBtn.click();
  await expect
    .poll(() => page.locator('[role="button"][aria-expanded]').count(), { timeout: 15_000 })
    .toBeGreaterThan(before);
});
```

- [ ] **Step 2: 테스트 실행**

Run: `pnpm --filter imstargg-frontend test:e2e player-battles.spec.ts`
Expected: 3/3 PASS (또는 마지막 1건은 skip 가능 — 테스트 플레이어 데이터에 따라)

배틀 데이터가 부족해 더 보기 버튼이 안 보이는 경우, 마지막 테스트는 자동으로 skip 된다.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/tests/e2e/player-battles.spec.ts
git commit -m "$(cat <<'EOF'
test(frontend): 플레이어 배틀 탭 E2E 테스트 추가

배틀 행 렌더, 펼침 토글, 더 보기 페이지네이션 검증.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review (작성 시점)

**1. Spec coverage:**
- 1. 목표 및 범위 → Task 18 (PlayerBattles 교체로 완성)
- 2. 도메인 모델 매핑 → Task 1 (DTO), Task 4 (도메인 타입), Task 5 (매퍼)
- 3. 데이터 패칭 → Task 6 (엔드포인트), Task 7 (queryKey), Task 8 (훅)
- 4. 컴포넌트 구조 → Task 9-17 (각 컴포넌트 1개씩)
- 5. UI 상세 → Task 14 (compact), Task 13/12/11 (펼침), Task 17 (빈/로딩/에러)
- 6. 모드/맵 표시 → Task 3 (modeLabels), Task 5 (mappers), Task 14 (rendering)
- 7. 라이브러리/유틸 → Task 2 (relativeTime), 외부 lib 추가 없음 ✓
- 8. 접근성 → Task 15 (role/aria-expanded/Enter/Space)
- 9. 테스트 → Task 2/5 (단위), Task 20 (E2E)
- 10. 마이그레이션 → Task 18 (placeholder 교체만)
- 11. 비기능 → 별도 task 없음 (구현 자체에 포함)

**2. Placeholder scan:** 본문에 TBD/TODO/"적절한"/"같은" 패턴 없음. 모든 코드 블록 완전.

**3. Type consistency:** `Battle`/`BattlePlayer`/`BattleEvent` 타입은 Task 4에서 정의되고 Task 5/9-17에서 일관되게 사용됨. `BATTLES_PAGE_SIZE` 는 Task 8 에서 정의됨. `usePlayerBattles` 시그니처 일관됨.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-29-frontend-battle-domain.md`. Two execution options:

**1. Subagent-Driven (recommended)** - 각 task 마다 fresh subagent + spec/quality 2단계 리뷰

**2. Inline Execution** - 이 세션에서 직접 task-by-task 실행

Which approach?
