# Frontend Ranking Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `/ranking` domain — player/brawler/club trophy rankings with country filter — following Events/Brawlers domain patterns.

**Architecture:** RSC + ISR (`revalidate: 600`) for data fetching. Country selection via `?country=xx` query param with shadcn Select-based navigation. Single-class-per-file. TDD for pure logic (mappers, parseCountry, RankBadge).

**Tech Stack:** Next.js 16 App Router, React 19, TypeScript, TanStack Query (queryKeys only — no useQuery), Vitest, Testing Library, Playwright (E2E), shadcn/ui (Select, Card, Badge), Tailwind.

## Conventions

- **Working dir**: All `pnpm` commands run from `imstargg-frontend/`.
- **Commit messages**: Korean. Trailer: `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. Use HEREDOC.
- **Staging**: Never `git add -A`. Stage explicit files only.
- **Asset images**: `iconId`/`badgeId` rendering is out of scope (asset mapping is a separate infrastructure task). Display tag/name only.
- **File-per-class**: One exported component/type group per file, matching project convention.

---

## File Structure

### Created
```
imstargg-frontend/
├── src/
│   ├── api/endpoints/ranking.ts
│   ├── domain/ranking/
│   │   ├── country.ts
│   │   ├── types.ts
│   │   └── mappers.ts
│   └── domain/ranking/__tests__/
│       ├── country.test.ts
│       └── mappers.test.ts
├── app/ranking/
│   ├── page.tsx
│   ├── players/page.tsx
│   ├── clubs/page.tsx
│   ├── brawlers/page.tsx
│   ├── brawlers/[brawlerId]/page.tsx
│   └── _components/
│       ├── RankBadge.tsx
│       ├── CountrySelector.tsx
│       ├── PlayerRankingList.tsx
│       ├── ClubRankingList.tsx
│       └── BrawlerRankingList.tsx
└── tests/
    ├── unit/ranking/RankBadge.test.tsx
    └── e2e/ranking.spec.ts
```

### Modified
```
src/api/types.ts                        # 4 ranking DTO types 추가
src/hooks/queries/queryKeys.ts          # ranking 키 추가
app/sitemap.ts                          # ranking sub-routes 추가
```

---

## Task 1: Country 타입과 parseCountry

**Files:**
- Create: `imstargg-frontend/src/domain/ranking/country.ts`
- Create: `imstargg-frontend/src/domain/ranking/__tests__/country.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/src/domain/ranking/__tests__/country.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { COUNTRY_CODES, COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";

describe("COUNTRY_CODES", () => {
  it("6개 country code를 가진다", () => {
    expect(COUNTRY_CODES).toEqual(["global", "kr", "cn", "jp", "de", "us"]);
  });
});

describe("COUNTRY_LABELS", () => {
  it("각 country code에 한글 라벨이 매핑된다", () => {
    expect(COUNTRY_LABELS.global).toBe("글로벌");
    expect(COUNTRY_LABELS.kr).toBe("한국");
    expect(COUNTRY_LABELS.cn).toBe("중국");
    expect(COUNTRY_LABELS.jp).toBe("일본");
    expect(COUNTRY_LABELS.de).toBe("독일");
    expect(COUNTRY_LABELS.us).toBe("미국");
  });
});

describe("parseCountry", () => {
  it("유효한 code는 그대로 반환한다", () => {
    expect(parseCountry("kr")).toBe("kr");
    expect(parseCountry("global")).toBe("global");
  });

  it("대문자/공백을 정규화한다", () => {
    expect(parseCountry(" KR ")).toBe("kr");
    expect(parseCountry("US")).toBe("us");
  });

  it("undefined/null/빈 문자열은 global로 fallback", () => {
    expect(parseCountry(undefined)).toBe("global");
    expect(parseCountry(null)).toBe("global");
    expect(parseCountry("")).toBe("global");
  });

  it("알 수 없는 code는 global로 fallback", () => {
    expect(parseCountry("fr")).toBe("global");
    expect(parseCountry("xyz")).toBe("global");
  });

  it("문자열이 아닌 입력은 global로 fallback", () => {
    expect(parseCountry(123)).toBe("global");
    expect(parseCountry(["kr"])).toBe("global");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
pnpm test:run src/domain/ranking/__tests__/country.test.ts
```

Expected: FAIL — `Cannot find module '@/domain/ranking/country'`

- [ ] **Step 3: 구현**

`imstargg-frontend/src/domain/ranking/country.ts`:

```ts
export const COUNTRY_CODES = ["global", "kr", "cn", "jp", "de", "us"] as const;
export type Country = (typeof COUNTRY_CODES)[number];

export const COUNTRY_LABELS: Record<Country, string> = {
  global: "글로벌",
  kr: "한국",
  cn: "중국",
  jp: "일본",
  de: "독일",
  us: "미국",
};

export function parseCountry(value: unknown): Country {
  if (typeof value !== "string") return "global";
  const normalized = value.trim().toLowerCase();
  return (COUNTRY_CODES as readonly string[]).includes(normalized)
    ? (normalized as Country)
    : "global";
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run src/domain/ranking/__tests__/country.test.ts
```

Expected: PASS (5 tests)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/src/domain/ranking/country.ts imstargg-frontend/src/domain/ranking/__tests__/country.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): ranking Country 타입과 parseCountry 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Ranking DTO 타입과 queryKeys

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts`
- Modify: `imstargg-frontend/src/hooks/queries/queryKeys.ts`

- [ ] **Step 1: API DTO 타입 추가**

`imstargg-frontend/src/api/types.ts` 파일 끝에 추가:

```ts
// === Ranking ===
export interface RankingPlayerResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;
}

export interface PlayerRankingResponse {
  country: string;
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
}

export interface BrawlerRankingResponse {
  country: string;
  brawlerId: number;
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
}

export interface ClubRankingResponse {
  country: string;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
}
```

- [ ] **Step 2: queryKeys에 ranking 추가**

`imstargg-frontend/src/hooks/queries/queryKeys.ts` 의 `queryKeys` 객체 안 `event` 다음에 추가:

```ts
  ranking: {
    all: ["ranking"] as const,
    players: (country: string) =>
      [...queryKeys.ranking.all, "players", country] as const,
    brawlers: (brawlerId: number | string, country: string) =>
      [...queryKeys.ranking.all, "brawlers", String(brawlerId), country] as const,
    clubs: (country: string) =>
      [...queryKeys.ranking.all, "clubs", country] as const,
  },
```

- [ ] **Step 3: typecheck**

```bash
pnpm typecheck
```

Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/src/api/types.ts imstargg-frontend/src/hooks/queries/queryKeys.ts
git commit -m "$(cat <<'EOF'
feat(frontend): ranking DTO 타입과 queryKeys 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: 도메인 타입과 매퍼

**Files:**
- Create: `imstargg-frontend/src/domain/ranking/types.ts`
- Create: `imstargg-frontend/src/domain/ranking/mappers.ts`
- Create: `imstargg-frontend/src/domain/ranking/__tests__/mappers.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/src/domain/ranking/__tests__/mappers.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import {
  toBrawlerRanking,
  toClubRanking,
  toPlayerRanking,
  toRankingPlayer,
} from "@/domain/ranking/mappers";

const playerDto = {
  tag: "#ABC",
  name: "Player",
  nameColor: "0xffffffff",
  iconId: 28000000,
  clubName: "MyClub",
};

describe("toRankingPlayer", () => {
  it("DTO 필드를 그대로 매핑한다", () => {
    expect(toRankingPlayer(playerDto)).toEqual({
      tag: "#ABC",
      name: "Player",
      nameColor: "0xffffffff",
      iconId: 28000000,
      clubName: "MyClub",
    });
  });

  it("nameColor null 허용", () => {
    expect(toRankingPlayer({ ...playerDto, nameColor: null }).nameColor).toBeNull();
  });
});

describe("toPlayerRanking", () => {
  it("country를 정규화하고 필드를 매핑한다", () => {
    const result = toPlayerRanking({
      country: "kr",
      rank: 1,
      player: playerDto,
      trophies: 50000,
      rankChange: 2,
    });
    expect(result).toEqual({
      country: "kr",
      rank: 1,
      player: {
        tag: "#ABC",
        name: "Player",
        nameColor: "0xffffffff",
        iconId: 28000000,
        clubName: "MyClub",
      },
      trophies: 50000,
      rankChange: 2,
    });
  });

  it("알 수 없는 country는 global로 fallback", () => {
    const result = toPlayerRanking({
      country: "fr",
      rank: 1,
      player: playerDto,
      trophies: 50000,
      rankChange: null,
    });
    expect(result.country).toBe("global");
    expect(result.rankChange).toBeNull();
  });
});

describe("toBrawlerRanking", () => {
  it("brawlerId를 포함해 매핑한다", () => {
    const result = toBrawlerRanking({
      country: "global",
      brawlerId: 16000000,
      rank: 1,
      player: playerDto,
      trophies: 1500,
      rankChange: -1,
    });
    expect(result.brawlerId).toBe(16000000);
    expect(result.rankChange).toBe(-1);
  });
});

describe("toClubRanking", () => {
  it("클럽 정보를 매핑한다", () => {
    const result = toClubRanking({
      country: "kr",
      tag: "#XYZ",
      name: "ClubName",
      badgeId: 8000000,
      trophies: 1000000,
      rank: 1,
      memberCount: 30,
      rankChange: 0,
    });
    expect(result).toEqual({
      country: "kr",
      tag: "#XYZ",
      name: "ClubName",
      badgeId: 8000000,
      trophies: 1000000,
      rank: 1,
      memberCount: 30,
      rankChange: 0,
    });
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
pnpm test:run src/domain/ranking/__tests__/mappers.test.ts
```

Expected: FAIL — module not found

- [ ] **Step 3: 도메인 타입 작성**

`imstargg-frontend/src/domain/ranking/types.ts`:

```ts
import type { Country } from "@/domain/ranking/country";

export interface RankingPlayer {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;
}

export interface PlayerRanking {
  country: Country;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
}

export interface BrawlerRanking {
  country: Country;
  brawlerId: number;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
}

export interface ClubRanking {
  country: Country;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
}
```

- [ ] **Step 4: 매퍼 작성**

`imstargg-frontend/src/domain/ranking/mappers.ts`:

```ts
import type {
  BrawlerRankingResponse,
  ClubRankingResponse,
  PlayerRankingResponse,
  RankingPlayerResponse,
} from "@/api/types";
import { parseCountry } from "@/domain/ranking/country";
import type {
  BrawlerRanking,
  ClubRanking,
  PlayerRanking,
  RankingPlayer,
} from "@/domain/ranking/types";

export function toRankingPlayer(dto: RankingPlayerResponse): RankingPlayer {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    clubName: dto.clubName,
  };
}

export function toPlayerRanking(dto: PlayerRankingResponse): PlayerRanking {
  return {
    country: parseCountry(dto.country),
    rank: dto.rank,
    player: toRankingPlayer(dto.player),
    trophies: dto.trophies,
    rankChange: dto.rankChange,
  };
}

export function toBrawlerRanking(dto: BrawlerRankingResponse): BrawlerRanking {
  return {
    country: parseCountry(dto.country),
    brawlerId: dto.brawlerId,
    rank: dto.rank,
    player: toRankingPlayer(dto.player),
    trophies: dto.trophies,
    rankChange: dto.rankChange,
  };
}

export function toClubRanking(dto: ClubRankingResponse): ClubRanking {
  return {
    country: parseCountry(dto.country),
    tag: dto.tag,
    name: dto.name,
    badgeId: dto.badgeId,
    trophies: dto.trophies,
    rank: dto.rank,
    memberCount: dto.memberCount,
    rankChange: dto.rankChange,
  };
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
pnpm test:run src/domain/ranking/__tests__/mappers.test.ts
pnpm typecheck
```

Expected: PASS (5 tests), typecheck OK

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/domain/ranking/types.ts imstargg-frontend/src/domain/ranking/mappers.ts imstargg-frontend/src/domain/ranking/__tests__/mappers.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): ranking 도메인 타입과 매퍼 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: rankingApi 엔드포인트

**Files:**
- Create: `imstargg-frontend/src/api/endpoints/ranking.ts`

- [ ] **Step 1: 엔드포인트 작성**

`imstargg-frontend/src/api/endpoints/ranking.ts`:

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BrawlerRankingResponse,
  ClubRankingResponse,
  ListResponse,
  PlayerRankingResponse,
} from "@/api/types";

export const rankingApi = {
  players: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerRankingResponse>>(
      `/v1/ranking/players?country=${encodeURIComponent(country)}`,
      opts,
    ),

  brawlers: (brawlerId: number, country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerRankingResponse>>(
      `/v1/ranking/brawlers/${brawlerId}?country=${encodeURIComponent(country)}`,
      opts,
    ),

  clubs: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<ClubRankingResponse>>(
      `/v1/ranking/clubs?country=${encodeURIComponent(country)}`,
      opts,
    ),
};
```

- [ ] **Step 2: typecheck**

```bash
pnpm typecheck
```

Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/src/api/endpoints/ranking.ts
git commit -m "$(cat <<'EOF'
feat(frontend): rankingApi 엔드포인트 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: RankBadge 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/ranking/_components/RankBadge.tsx`
- Create: `imstargg-frontend/tests/unit/ranking/RankBadge.test.tsx`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/unit/ranking/RankBadge.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RankBadge } from "@/../app/ranking/_components/RankBadge";

describe("RankBadge", () => {
  it("rank를 #N 형식으로 표시한다", () => {
    render(<RankBadge rank={1} rankChange={null} />);
    expect(screen.getByText("#1")).toBeInTheDocument();
  });

  it("rankChange가 양수면 ▲N으로 표시한다", () => {
    render(<RankBadge rank={5} rankChange={3} />);
    expect(screen.getByText("▲3")).toBeInTheDocument();
  });

  it("rankChange가 음수면 ▼N으로 표시한다 (절댓값)", () => {
    render(<RankBadge rank={10} rankChange={-2} />);
    expect(screen.getByText("▼2")).toBeInTheDocument();
  });

  it("rankChange가 0이면 ─로 표시한다", () => {
    render(<RankBadge rank={7} rankChange={0} />);
    expect(screen.getByText("─")).toBeInTheDocument();
  });

  it("rankChange가 null이면 변동 표시가 없다", () => {
    render(<RankBadge rank={2} rankChange={null} />);
    expect(screen.queryByText("▲")).toBeNull();
    expect(screen.queryByText("▼")).toBeNull();
    expect(screen.queryByText("─")).toBeNull();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
pnpm test:run tests/unit/ranking/RankBadge.test.tsx
```

Expected: FAIL — module not found

- [ ] **Step 3: 컴포넌트 작성**

`imstargg-frontend/app/ranking/_components/RankBadge.tsx`:

```tsx
type Props = {
  rank: number;
  rankChange: number | null;
};

export function RankBadge({ rank, rankChange }: Props) {
  return (
    <div className="flex items-center gap-1">
      <span className="text-sm font-bold tabular-nums">#{rank}</span>
      {rankChange !== null && <RankChangeIndicator change={rankChange} />}
    </div>
  );
}

function RankChangeIndicator({ change }: { change: number }) {
  if (change > 0) {
    return (
      <span className="text-xs tabular-nums text-emerald-600">▲{change}</span>
    );
  }
  if (change < 0) {
    return (
      <span className="text-xs tabular-nums text-red-600">▼{Math.abs(change)}</span>
    );
  }
  return <span className="text-xs text-muted-foreground">─</span>;
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/ranking/RankBadge.test.tsx
```

Expected: PASS (5 tests)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/app/ranking/_components/RankBadge.tsx imstargg-frontend/tests/unit/ranking/RankBadge.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): RankBadge 컴포넌트 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: CountrySelector 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/ranking/_components/CountrySelector.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/app/ranking/_components/CountrySelector.tsx`:

```tsx
"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  COUNTRY_CODES,
  COUNTRY_LABELS,
  type Country,
} from "@/domain/ranking/country";

type Props = {
  current: Country;
};

export function CountrySelector({ current }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const onChange = (next: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (next === "global") {
      params.delete("country");
    } else {
      params.set("country", next);
    }
    const qs = params.toString();
    router.push(qs ? `${pathname}?${qs}` : pathname);
  };

  return (
    <Select value={current} onValueChange={onChange}>
      <SelectTrigger className="w-32" aria-label="국가 선택">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {COUNTRY_CODES.map((code) => (
          <SelectItem key={code} value={code}>
            {COUNTRY_LABELS[code]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

- [ ] **Step 2: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/app/ranking/_components/CountrySelector.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): CountrySelector 컴포넌트 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: /ranking 인덱스 페이지

**Files:**
- Create: `imstargg-frontend/app/ranking/page.tsx`

- [ ] **Step 1: 페이지 작성**

`imstargg-frontend/app/ranking/page.tsx`:

```tsx
import type { Metadata } from "next";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "랭킹 | ImStarGG",
  description: "브롤스타즈 플레이어, 브롤러, 클럽 트로피 랭킹을 확인하세요.",
  path: "/ranking",
});

const ITEMS = [
  {
    href: "/ranking/players",
    title: "플레이어 랭킹",
    description: "국가별 플레이어 트로피 순위",
  },
  {
    href: "/ranking/brawlers",
    title: "브롤러 랭킹",
    description: "각 브롤러별 국가 트로피 순위",
  },
  {
    href: "/ranking/clubs",
    title: "클럽 랭킹",
    description: "국가별 클럽 트로피 순위",
  },
];

export default function Page() {
  return (
    <main className="container mx-auto max-w-3xl space-y-6 py-8">
      <h1 className="text-2xl font-bold">랭킹</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {ITEMS.map((item) => (
          <Link key={item.href} href={item.href} className="block">
            <Card className="h-full p-5 transition hover:border-foreground">
              <h2 className="text-lg font-bold">{item.title}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {item.description}
              </p>
            </Card>
          </Link>
        ))}
      </div>
    </main>
  );
}
```

- [ ] **Step 2: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/app/ranking/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): /ranking 인덱스 페이지 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: PlayerRankingList + /ranking/players

**Files:**
- Create: `imstargg-frontend/app/ranking/_components/PlayerRankingList.tsx`
- Create: `imstargg-frontend/app/ranking/players/page.tsx`

- [ ] **Step 1: PlayerRankingList 작성**

`imstargg-frontend/app/ranking/_components/PlayerRankingList.tsx`:

```tsx
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { PlayerRanking } from "@/domain/ranking/types";
import { RankBadge } from "@/app/ranking/_components/RankBadge";

type Props = {
  rankings: PlayerRanking[];
};

export function PlayerRankingList({ rankings }: Props) {
  if (rankings.length === 0) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        랭킹 데이터가 없습니다
      </Card>
    );
  }
  return (
    <ul className="space-y-2">
      {rankings.map((r) => (
        <li key={r.player.tag}>
          <Link href={`/players/${toUrlPath(r.player.tag)}`} className="block">
            <Card className="flex items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div
                  className="truncate font-bold"
                  style={
                    r.player.nameColor
                      ? { color: hexFromColorCode(r.player.nameColor) }
                      : undefined
                  }
                >
                  {r.player.name}
                </div>
                {r.player.clubName && (
                  <div className="truncate text-xs text-muted-foreground">
                    {r.player.clubName}
                  </div>
                )}
              </div>
              <div className="shrink-0 text-right tabular-nums">
                <div className="text-sm font-bold">
                  {r.trophies.toLocaleString("ko-KR")}
                </div>
                <div className="text-[10px] text-muted-foreground">트로피</div>
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  );
}

function hexFromColorCode(code: string): string {
  // 백엔드 nameColor: "0xffXXXXXX" 형식 — 앞 4자(0x + alpha) 제거 후 # prefix
  const m = /^0x[0-9a-fA-F]{2}([0-9a-fA-F]{6})$/.exec(code.trim());
  return m ? `#${m[1]}` : "inherit";
}
```

- [ ] **Step 2: 페이지 작성**

`imstargg-frontend/app/ranking/players/page.tsx`:

```tsx
import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { PlayerRankingList } from "@/app/ranking/_components/PlayerRankingList";

export const revalidate = 600;

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 플레이어 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 플레이어 트로피 랭킹.`,
    path: "/ranking/players",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.players(country, {
    next: { revalidate: 600 },
  });
  const rankings = dto.content.map(toPlayerRanking);

  return (
    <main className="container mx-auto max-w-3xl space-y-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          {COUNTRY_LABELS[country]} 플레이어 랭킹
        </h1>
        <CountrySelector current={country} />
      </div>
      <PlayerRankingList rankings={rankings} />
    </main>
  );
}
```

- [ ] **Step 3: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/app/ranking/_components/PlayerRankingList.tsx imstargg-frontend/app/ranking/players/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): /ranking/players 페이지 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 9: ClubRankingList + /ranking/clubs

**Files:**
- Create: `imstargg-frontend/app/ranking/_components/ClubRankingList.tsx`
- Create: `imstargg-frontend/app/ranking/clubs/page.tsx`

- [ ] **Step 1: ClubRankingList 작성**

`imstargg-frontend/app/ranking/_components/ClubRankingList.tsx`:

```tsx
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { ClubRanking } from "@/domain/ranking/types";
import { RankBadge } from "@/app/ranking/_components/RankBadge";

type Props = {
  rankings: ClubRanking[];
};

export function ClubRankingList({ rankings }: Props) {
  if (rankings.length === 0) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        랭킹 데이터가 없습니다
      </Card>
    );
  }
  return (
    <ul className="space-y-2">
      {rankings.map((r) => (
        <li key={r.tag}>
          <Link href={`/clubs/${toUrlPath(r.tag)}`} className="block">
            <Card className="flex items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate font-bold">{r.name}</div>
                <div className="text-xs text-muted-foreground">
                  멤버 {r.memberCount}명
                </div>
              </div>
              <div className="shrink-0 text-right tabular-nums">
                <div className="text-sm font-bold">
                  {r.trophies.toLocaleString("ko-KR")}
                </div>
                <div className="text-[10px] text-muted-foreground">트로피</div>
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  );
}
```

- [ ] **Step 2: 페이지 작성**

`imstargg-frontend/app/ranking/clubs/page.tsx`:

```tsx
import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toClubRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { ClubRankingList } from "@/app/ranking/_components/ClubRankingList";

export const revalidate = 600;

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 클럽 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 클럽 트로피 랭킹.`,
    path: "/ranking/clubs",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.clubs(country, { next: { revalidate: 600 } });
  const rankings = dto.content.map(toClubRanking);

  return (
    <main className="container mx-auto max-w-3xl space-y-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          {COUNTRY_LABELS[country]} 클럽 랭킹
        </h1>
        <CountrySelector current={country} />
      </div>
      <ClubRankingList rankings={rankings} />
    </main>
  );
}
```

- [ ] **Step 3: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/app/ranking/_components/ClubRankingList.tsx imstargg-frontend/app/ranking/clubs/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): /ranking/clubs 페이지 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: /ranking/brawlers 그리드 페이지

**Files:**
- Create: `imstargg-frontend/app/ranking/brawlers/page.tsx`

기존 `app/brawlers/_components/BrawlerCard.tsx` 는 `/brawlers/{id}` 로 링크되므로, 랭킹용은 자체 카드를 사용한다.

- [ ] **Step 1: 페이지 작성**

`imstargg-frontend/app/ranking/brawlers/page.tsx`:

```tsx
import type { Metadata } from "next";
import Link from "next/link";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { sortByName } from "@/domain/brawler/filters";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Card } from "@/components/ui/card";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 랭킹 | ImStarGG",
  description: "브롤러를 선택하여 국가별 랭킹을 확인하세요.",
  path: "/ranking/brawlers",
});

export const revalidate = 86400;

export default async function Page() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  const brawlers = sortByName(list.content.map(toBrawler));

  return (
    <main className="container mx-auto max-w-5xl space-y-4 py-8">
      <h1 className="text-2xl font-bold">브롤러 랭킹</h1>
      <p className="text-sm text-muted-foreground">
        랭킹을 확인할 브롤러를 선택하세요.
      </p>
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {brawlers.map((b) => (
          <Link
            key={b.id}
            href={`/ranking/brawlers/${b.id}`}
            className="block"
          >
            <Card className="flex flex-col items-center gap-2 p-3 transition hover:border-foreground">
              <BrawlerAvatar
                name={b.name}
                imageUrl={b.portraitImageUrl}
                rarity={b.rarity}
                size={72}
                showFrame
              />
              <div className="w-full truncate text-center text-xs font-bold">
                {b.name}
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </main>
  );
}
```

- [ ] **Step 2: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/app/ranking/brawlers/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): /ranking/brawlers 그리드 페이지 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: BrawlerRankingList + /ranking/brawlers/[brawlerId]

**Files:**
- Create: `imstargg-frontend/app/ranking/_components/BrawlerRankingList.tsx`
- Create: `imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx`

- [ ] **Step 1: BrawlerRankingList 작성**

`imstargg-frontend/app/ranking/_components/BrawlerRankingList.tsx`:

```tsx
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { BrawlerRanking } from "@/domain/ranking/types";
import { RankBadge } from "@/app/ranking/_components/RankBadge";

type Props = {
  rankings: BrawlerRanking[];
};

export function BrawlerRankingList({ rankings }: Props) {
  if (rankings.length === 0) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        랭킹 데이터가 없습니다
      </Card>
    );
  }
  return (
    <ul className="space-y-2">
      {rankings.map((r) => (
        <li key={r.player.tag}>
          <Link href={`/players/${toUrlPath(r.player.tag)}`} className="block">
            <Card className="flex items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate font-bold">{r.player.name}</div>
                {r.player.clubName && (
                  <div className="truncate text-xs text-muted-foreground">
                    {r.player.clubName}
                  </div>
                )}
              </div>
              <div className="shrink-0 text-right tabular-nums">
                <div className="text-sm font-bold">
                  {r.trophies.toLocaleString("ko-KR")}
                </div>
                <div className="text-[10px] text-muted-foreground">트로피</div>
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  );
}
```

- [ ] **Step 2: 페이지 작성**

`imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx`:

```tsx
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { rankingApi } from "@/api/endpoints/ranking";
import { toBrawler } from "@/domain/brawler/mappers";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toBrawlerRanking } from "@/domain/ranking/mappers";
import { ApiError } from "@/api/errors";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { BrawlerRankingList } from "@/app/ranking/_components/BrawlerRankingList";

export const revalidate = 600;

type Params = Promise<{ brawlerId: string }>;
type SearchParams = Promise<{ country?: string }>;

async function loadBrawler(brawlerId: string) {
  const idNum = Number(brawlerId);
  if (!Number.isFinite(idNum) || idNum <= 0) notFound();
  try {
    const dto = await brawlerApi.get(idNum, {
      next: { revalidate: 86400 },
    });
    return toBrawler(dto);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}

export async function generateMetadata({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: SearchParams;
}): Promise<Metadata> {
  const { brawlerId } = await params;
  const country = parseCountry((await searchParams).country);
  const brawler = await loadBrawler(brawlerId);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${brawler.name} ${label} 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${brawler.name}의 ${label} 트로피 랭킹.`,
    path: `/ranking/brawlers/${brawler.id}`,
  });
}

export default async function Page({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: SearchParams;
}) {
  const { brawlerId } = await params;
  const country = parseCountry((await searchParams).country);
  const brawler = await loadBrawler(brawlerId);

  const dto = await rankingApi.brawlers(brawler.id, country, {
    next: { revalidate: 600 },
  });
  const rankings = dto.content.map(toBrawlerRanking);

  return (
    <main className="container mx-auto max-w-3xl space-y-4 py-8">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <BrawlerAvatar
            name={brawler.name}
            imageUrl={brawler.portraitImageUrl}
            rarity={brawler.rarity}
            size={56}
            showFrame
          />
          <h1 className="text-2xl font-bold">
            {brawler.name} {COUNTRY_LABELS[country]} 랭킹
          </h1>
        </div>
        <CountrySelector current={country} />
      </div>
      <BrawlerRankingList rankings={rankings} />
    </main>
  );
}
```

- [ ] **Step 3: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/app/ranking/_components/BrawlerRankingList.tsx imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): /ranking/brawlers/[brawlerId] 페이지 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 12: sitemap 업데이트

**Files:**
- Modify: `imstargg-frontend/app/sitemap.ts`

기존 sitemap에 `/ranking`이 이미 포함되어 있다. ranking 하위 경로(`/players`, `/clubs`, `/brawlers`)를 추가한다. country 변형은 query param이므로 미포함.

- [ ] **Step 1: sitemap 수정**

`imstargg-frontend/app/sitemap.ts` 의 마지막 return 배열에 추가:

기존:
```ts
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
```

다음으로 교체:
```ts
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/ranking/players`, lastModified: now, changeFrequency: "hourly", priority: 0.7 },
    { url: `${base}/ranking/clubs`, lastModified: now, changeFrequency: "hourly", priority: 0.7 },
    { url: `${base}/ranking/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.6 },
```

- [ ] **Step 2: typecheck**

```bash
pnpm typecheck
```

Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/app/sitemap.ts
git commit -m "$(cat <<'EOF'
feat(frontend): sitemap에 ranking 하위 경로 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 13: E2E 테스트

**Files:**
- Create: `imstargg-frontend/tests/e2e/ranking.spec.ts`

prod API 직접 호출. 랭킹 데이터는 생성 시점에 비어 있을 수 있으므로 페이지 골격만 검증하고, 행이 있을 경우 추가 검증한다.

- [ ] **Step 1: E2E 테스트 작성**

`imstargg-frontend/tests/e2e/ranking.spec.ts`:

```ts
import { expect, test } from "@playwright/test";

test.describe("Ranking domain", () => {
  test("플레이어 랭킹 글로벌 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/ranking/players");
    await expect(
      page.getByRole("heading", { name: /글로벌 플레이어 랭킹/ }),
    ).toBeVisible();
    await expect(page.getByLabel("국가 선택")).toBeVisible();
  });

  test("CountrySelector로 국가를 전환하면 URL이 바뀐다", async ({ page }) => {
    await page.goto("/ranking/players");
    await page.getByLabel("국가 선택").click();
    await page.getByRole("option", { name: "한국" }).click();
    await page.waitForURL(/country=kr/);
    await expect(
      page.getByRole("heading", { name: /한국 플레이어 랭킹/ }),
    ).toBeVisible();
  });

  test("클럽 랭킹 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/ranking/clubs?country=kr");
    await expect(
      page.getByRole("heading", { name: /한국 클럽 랭킹/ }),
    ).toBeVisible();
  });

  test("브롤러 그리드에서 브롤러를 선택하면 랭킹 페이지로 이동한다", async ({
    page,
  }) => {
    await page.goto("/ranking/brawlers");
    await expect(
      page.getByRole("heading", { name: "브롤러 랭킹" }),
    ).toBeVisible();
    const firstLink = page
      .locator('a[href^="/ranking/brawlers/"]')
      .first();
    await firstLink.click();
    await page.waitForURL(/\/ranking\/brawlers\/\d+/);
    await expect(page.getByLabel("국가 선택")).toBeVisible();
  });

  test("/ranking 인덱스 페이지가 3개 카드를 표시한다", async ({ page }) => {
    await page.goto("/ranking");
    await expect(
      page.getByRole("heading", { name: "플레이어 랭킹" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "브롤러 랭킹" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "클럽 랭킹" }),
    ).toBeVisible();
  });
});
```

- [ ] **Step 2: typecheck/lint**

```bash
pnpm typecheck && pnpm lint
```

Expected: PASS

- [ ] **Step 3: E2E 실행 (선택, prod API 의존)**

```bash
pnpm test:e2e tests/e2e/ranking.spec.ts
```

Expected: PASS — prod API에 랭킹 데이터가 있으면 통과. 데이터 부재 시 일부 셀렉터가 fail할 수 있음 → 그 경우 빈 데이터 대응이 spec 통과 기준은 아니므로 정상.

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/tests/e2e/ranking.spec.ts
git commit -m "$(cat <<'EOF'
test(frontend): ranking 도메인 E2E 시나리오 추가

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## 완료 검증

모든 task 완료 후:

- [ ] `pnpm typecheck` PASS
- [ ] `pnpm lint` PASS
- [ ] `pnpm test:run` 전체 PASS (country/mappers/RankBadge)
- [ ] `pnpm dev` 후 다음 라우트 수동 검증:
  - `/ranking` → 3개 카드 표시
  - `/ranking/players` → 글로벌 랭킹, CountrySelector 동작
  - `/ranking/players?country=kr` → 한국 랭킹
  - `/ranking/clubs?country=jp` → 일본 클럽 랭킹
  - `/ranking/brawlers` → 브롤러 그리드
  - `/ranking/brawlers/{id}?country=us` → 브롤러별 미국 랭킹
- [ ] sitemap에 ranking 하위 경로 노출 확인 (`curl localhost:3000/sitemap.xml | grep ranking`)
