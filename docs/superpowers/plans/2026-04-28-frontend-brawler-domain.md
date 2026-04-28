# Frontend Brawler Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace `app/brawlers/{page,[id]/page}.tsx` mocks with real API integration (list/detail/filter/search using static brawler data).

**Architecture:** RSC fetches `/v1/brawlers` for list and `/v1/brawlers/{id}` for detail with `revalidate: 86400`. Static brawler IDs fed to `generateStaticParams` for SSG. Client-side filters (role/rarity/search) over loaded list. New `BrawlerAvatar` component for real data; legacy `BrawlerPortrait` kept for other mock pages.

**Tech Stack:** Next.js 16 App Router, React 19, TanStack Query v5, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-04-28-frontend-brawler-domain-design.md`

**Working directory:** `/Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend`

**Branch:** create `feat/frontend-brawler-domain` off `main` before starting Task 1.

```bash
git checkout main && git pull --ff-only
git checkout -b feat/frontend-brawler-domain
```

---

## File Map

**Create:**
- `src/domain/i18n.ts`
- `tests/unit/domain/i18n.test.ts`
- `src/api/endpoints/brawler.ts`
- `src/domain/brawler/types.ts`
- `src/domain/brawler/mappers.ts`
- `src/domain/brawler/filters.ts`
- `tests/unit/domain/brawler/mappers.test.ts`
- `tests/unit/domain/brawler/filters.test.ts`
- `src/hooks/queries/useBrawlers.ts`
- `src/hooks/queries/useBrawler.ts`
- `src/components/primitives/BrawlerAvatar.tsx`
- `app/brawlers/_components/BrawlerCard.tsx`
- `app/brawlers/_components/BrawlerFilters.tsx`
- `app/brawlers/[id]/error.tsx`
- `app/brawlers/[id]/not-found.tsx`
- `app/brawlers/[id]/_components/BrawlerContent.tsx`
- `app/brawlers/[id]/_components/BrawlerHero.tsx`
- `app/brawlers/[id]/_components/BrawlerItems.tsx`
- `app/brawlers/[id]/_components/BrawlerItemCard.tsx`
- `app/brawlers/[id]/_components/BrawlerBuffies.tsx`
- `tests/e2e/brawler.spec.ts`

**Modify:**
- `src/domain/player/mappers.ts` (use shared pickName)
- `src/hooks/queries/queryKeys.ts` (add brawler keys)
- `app/brawlers/page.tsx` (rewrite as RSC)
- `app/brawlers/_components/BrawlersListPage.tsx` (rewrite)
- `app/brawlers/[id]/page.tsx` (rewrite as RSC + generateStaticParams)
- `app/sitemap.ts` (add dynamic brawler entries)

**Delete:**
- `app/brawlers/[id]/_components/BrawlerDetailPage.tsx`

---

## Task 1: Shared i18n.pickName (TDD)

**Files:**
- Create: `tests/unit/domain/i18n.test.ts`
- Create: `src/domain/i18n.ts`

- [ ] **Step 1: Failing tests**

```typescript
import { describe, it, expect } from "vitest";
import { pickName } from "@/domain/i18n";

describe("pickName", () => {
  it("prefers ko", () => {
    expect(pickName({ ko: "셸리", en: "Shelly" })).toBe("셸리");
  });
  it("falls back to en when ko absent", () => {
    expect(pickName({ en: "Shelly" })).toBe("Shelly");
  });
  it("falls back to first available value", () => {
    expect(pickName({ ja: "シェリー" })).toBe("シェリー");
  });
  it("returns unknown for null/undefined", () => {
    expect(pickName(undefined)).toBe("(unknown)");
    expect(pickName(null)).toBe("(unknown)");
  });
  it("returns unknown for empty object", () => {
    expect(pickName({})).toBe("(unknown)");
  });
});
```

- [ ] **Step 2: Run, verify FAIL**

`pnpm test:run tests/unit/domain/i18n.test.ts` — fails (module missing).

- [ ] **Step 3: Implement**

```typescript
// src/domain/i18n.ts
import type { MessagesResponse } from "@/api/types";

export function pickName(messages: MessagesResponse | undefined | null): string {
  if (!messages) return "(unknown)";
  return messages.ko ?? messages.en ?? Object.values(messages)[0] ?? "(unknown)";
}
```

- [ ] **Step 4: Run, verify PASS**

`pnpm test:run tests/unit/domain/i18n.test.ts` — 5 PASS.

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/i18n.test.ts src/domain/i18n.ts
git commit -m "feat(frontend): extract shared pickName i18n helper (TDD)"
```

---

## Task 2: Use shared pickName in player mappers

**Files:**
- Modify: `src/domain/player/mappers.ts`

- [ ] **Step 1: Replace local pickName**

Open `src/domain/player/mappers.ts`. Remove the local `pickName` function. Add `import { pickName } from "@/domain/i18n";` at the top. Keep the rest unchanged.

The file should now look like:

```typescript
import type {
  PlayerResponse,
  PlayerBrawlerResponse,
} from "@/api/types";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import { pickName } from "@/domain/i18n";

export function toPlayer(dto: PlayerResponse): Player {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    expLevel: dto.expLevel,
    expPoints: dto.expPoints,
    victories3vs3: dto.victories3vs3,
    victoriesSolo: dto.victoriesSolo,
    victoriesDuo: dto.victoriesDuo,
    totalPrestigeLevel: dto.totalPrestigeLevel,
    club: dto.club ? { tag: dto.club.tag, name: dto.club.name } : null,
  };
}

export function toPlayerBrawler(dto: PlayerBrawlerResponse): PlayerBrawler {
  const b = dto.brawler;
  return {
    id: b?.id ?? null,
    name: pickName(b?.name),
    rarity: b?.rarity ?? null,
    role: b?.role ?? null,
    power: dto.power,
    rank: dto.rank,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    prestigeLevel: dto.prestigeLevel,
    portraitImageUrl: b?.portraitImageUrl ?? null,
    buffies: dto.buffies,
    raw: b ?? null,
  };
}
```

> Note: `MessagesResponse` import was removed (now consumed inside `i18n.ts`).

- [ ] **Step 2: Verify**

```bash
pnpm typecheck
pnpm test:run
```

Both PASS.

- [ ] **Step 3: Commit**

```bash
git add src/domain/player/mappers.ts
git commit -m "refactor(frontend): use shared pickName in player mappers"
```

---

## Task 3: brawlerApi endpoint

**Files:**
- Create: `src/api/endpoints/brawler.ts`

- [ ] **Step 1: Write the file**

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerResponse>(`/v1/brawlers/${id}`, opts),
};
```

- [ ] **Step 2: Typecheck**

`pnpm typecheck` — PASS.

- [ ] **Step 3: Commit**

```bash
git add src/api/endpoints/brawler.ts
git commit -m "feat(frontend): add brawlerApi endpoint"
```

---

## Task 4: Brawler domain types

**Files:**
- Create: `src/domain/brawler/types.ts`

- [ ] **Step 1: Write**

```typescript
import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
}

export interface Brawler {
  id: number;
  name: string;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
  gadgets: BrawlerItem[];
  gears: BrawlerItem[];
  starPowers: BrawlerItem[];
  hyperCharges: BrawlerItem[];
  buffies: BuffyResponse[];
}
```

- [ ] **Step 2: Typecheck**

`pnpm typecheck` — PASS.

- [ ] **Step 3: Commit**

```bash
git add src/domain/brawler/types.ts
git commit -m "feat(frontend): add Brawler domain types"
```

---

## Task 5: Brawler mappers (TDD)

**Files:**
- Create: `tests/unit/domain/brawler/mappers.test.ts`
- Create: `src/domain/brawler/mappers.ts`

- [ ] **Step 1: Failing tests**

```typescript
import { describe, it, expect } from "vitest";
import { toBrawler, toBrawlerItem } from "@/domain/brawler/mappers";
import type { BrawlerResponse, GadgetResponse } from "@/api/types";

const dtoGadget = (overrides: Partial<GadgetResponse> = {}): GadgetResponse => ({
  id: 23000001,
  name: { ko: "브롤스타즈터틀!", en: "Brawl Box Turtle" },
  imageUrl: "https://cdn/gadget.png",
  ...overrides,
});

const dtoBrawler = (overrides: Partial<BrawlerResponse> = {}): BrawlerResponse => ({
  id: 16000000,
  name: { ko: "셸리", en: "Shelly" },
  rarity: "COMMON",
  role: "DAMAGE_DEALER",
  gadgets: [dtoGadget()],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  portraitImageUrl: "https://cdn/shelly.png",
  buffies: [],
  ...overrides,
});

describe("toBrawlerItem", () => {
  it("flattens MessagesResponse to ko-first name", () => {
    const item = toBrawlerItem(dtoGadget());
    expect(item.name).toBe("브롤스타즈터틀!");
    expect(item.id).toBe(23000001);
    expect(item.imageUrl).toBe("https://cdn/gadget.png");
  });

  it("preserves null imageUrl", () => {
    expect(toBrawlerItem(dtoGadget({ imageUrl: null })).imageUrl).toBeNull();
  });
});

describe("toBrawler", () => {
  it("flattens name to ko", () => {
    expect(toBrawler(dtoBrawler()).name).toBe("셸리");
  });

  it("falls back to en when ko absent", () => {
    expect(toBrawler(dtoBrawler({ name: { en: "Shelly" } })).name).toBe("Shelly");
  });

  it("maps all item arrays through toBrawlerItem", () => {
    const b = toBrawler(
      dtoBrawler({
        gadgets: [dtoGadget({ id: 1 }), dtoGadget({ id: 2 })],
        starPowers: [dtoGadget({ id: 11 })],
        gears: [dtoGadget({ id: 21 })],
        hyperCharges: [dtoGadget({ id: 31 })],
      }),
    );
    expect(b.gadgets.map((x) => x.id)).toEqual([1, 2]);
    expect(b.starPowers.map((x) => x.id)).toEqual([11]);
    expect(b.gears.map((x) => x.id)).toEqual([21]);
    expect(b.hyperCharges.map((x) => x.id)).toEqual([31]);
  });

  it("preserves portraitImageUrl null", () => {
    expect(toBrawler(dtoBrawler({ portraitImageUrl: null })).portraitImageUrl).toBeNull();
  });
});
```

- [ ] **Step 2: Run, verify FAIL**

`pnpm test:run tests/unit/domain/brawler/mappers.test.ts`

- [ ] **Step 3: Implement**

```typescript
// src/domain/brawler/mappers.ts
import type {
  BrawlerResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type { Brawler, BrawlerItem } from "@/domain/brawler/types";
import { pickName } from "@/domain/i18n";

type ItemDto =
  | GadgetResponse
  | GearResponse
  | StarPowerResponse
  | HyperChargeResponse;

export function toBrawlerItem(dto: ItemDto): BrawlerItem {
  return {
    id: dto.id,
    name: pickName(dto.name),
    imageUrl: dto.imageUrl,
  };
}

export function toBrawler(dto: BrawlerResponse): Brawler {
  return {
    id: dto.id,
    name: pickName(dto.name),
    rarity: dto.rarity,
    role: dto.role,
    portraitImageUrl: dto.portraitImageUrl,
    gadgets: dto.gadgets.map(toBrawlerItem),
    gears: dto.gears.map(toBrawlerItem),
    starPowers: dto.starPowers.map(toBrawlerItem),
    hyperCharges: dto.hyperCharges.map(toBrawlerItem),
    buffies: dto.buffies,
  };
}
```

- [ ] **Step 4: Run, verify PASS**

`pnpm test:run tests/unit/domain/brawler/mappers.test.ts` — 6 PASS.

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/brawler/mappers.test.ts src/domain/brawler/mappers.ts
git commit -m "feat(frontend): add Brawler mappers (TDD)"
```

---

## Task 6: Filters & sort (TDD)

**Files:**
- Create: `tests/unit/domain/brawler/filters.test.ts`
- Create: `src/domain/brawler/filters.ts`

- [ ] **Step 1: Failing tests**

```typescript
import { describe, it, expect } from "vitest";
import { filterBrawlers, sortByName } from "@/domain/brawler/filters";
import type { Brawler } from "@/domain/brawler/types";

const b = (overrides: Partial<Brawler> = {}): Brawler => ({
  id: 1,
  name: "Shelly",
  rarity: "COMMON",
  role: "DAMAGE_DEALER",
  portraitImageUrl: null,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  buffies: [],
  ...overrides,
});

describe("filterBrawlers", () => {
  const list: Brawler[] = [
    b({ id: 1, name: "셸리", role: "DAMAGE_DEALER", rarity: "COMMON" }),
    b({ id: 2, name: "Shelly", role: "DAMAGE_DEALER", rarity: "COMMON" }),
    b({ id: 3, name: "콜트", role: "MARKSMAN", rarity: "COMMON" }),
    b({ id: 4, name: "리언", role: "ASSASSIN", rarity: "LEGENDARY" }),
  ];

  it("returns all when no opts", () => {
    expect(filterBrawlers(list, {})).toHaveLength(4);
  });

  it("filters by role", () => {
    const r = filterBrawlers(list, { role: "MARKSMAN" });
    expect(r.map((x) => x.id)).toEqual([3]);
  });

  it("filters by rarity", () => {
    const r = filterBrawlers(list, { rarity: "LEGENDARY" });
    expect(r.map((x) => x.id)).toEqual([4]);
  });

  it("substring search matches Korean", () => {
    const r = filterBrawlers(list, { search: "셸" });
    expect(r.map((x) => x.id)).toEqual([1]);
  });

  it("substring search matches English (case-insensitive)", () => {
    const r = filterBrawlers(list, { search: "shel" });
    expect(r.map((x) => x.id)).toEqual([2]);
  });

  it("combines role + search", () => {
    const r = filterBrawlers(list, { role: "DAMAGE_DEALER", search: "Shelly" });
    expect(r.map((x) => x.id)).toEqual([2]);
  });

  it("trims and ignores empty search", () => {
    expect(filterBrawlers(list, { search: "   " })).toHaveLength(4);
  });
});

describe("sortByName", () => {
  it("sorts Korean names by Korean collation", () => {
    const list: Brawler[] = [
      b({ id: 1, name: "콜트" }),
      b({ id: 2, name: "셸리" }),
      b({ id: 3, name: "리언" }),
    ];
    expect(sortByName(list).map((x) => x.name)).toEqual(["리언", "셸리", "콜트"]);
  });

  it("does not mutate input", () => {
    const list: Brawler[] = [b({ id: 1, name: "콜트" }), b({ id: 2, name: "셸리" })];
    const before = list.map((x) => x.id);
    sortByName(list);
    expect(list.map((x) => x.id)).toEqual(before);
  });
});
```

- [ ] **Step 2: Run, verify FAIL**

`pnpm test:run tests/unit/domain/brawler/filters.test.ts`

- [ ] **Step 3: Implement**

```typescript
// src/domain/brawler/filters.ts
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerRole, BrawlerRarity } from "@/api/types";

export interface BrawlerFilterOptions {
  role?: BrawlerRole;
  rarity?: BrawlerRarity;
  search?: string;
}

export function filterBrawlers(
  list: Brawler[],
  opts: BrawlerFilterOptions,
): Brawler[] {
  const search = opts.search?.trim().toLocaleLowerCase("ko") ?? "";
  return list.filter((b) => {
    if (opts.role && b.role !== opts.role) return false;
    if (opts.rarity && b.rarity !== opts.rarity) return false;
    if (search && !b.name.toLocaleLowerCase("ko").includes(search)) return false;
    return true;
  });
}

export function sortByName(list: Brawler[]): Brawler[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name, "ko"));
}
```

- [ ] **Step 4: Run, verify PASS**

`pnpm test:run tests/unit/domain/brawler/filters.test.ts` — 9 PASS.

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/brawler/filters.test.ts src/domain/brawler/filters.ts
git commit -m "feat(frontend): add Brawler filter and sort (TDD)"
```

---

## Task 7: Query keys + hooks

**Files:**
- Modify: `src/hooks/queries/queryKeys.ts`
- Create: `src/hooks/queries/useBrawlers.ts`
- Create: `src/hooks/queries/useBrawler.ts`

- [ ] **Step 1: Extend queryKeys**

Replace `src/hooks/queries/queryKeys.ts` with:

```typescript
import { normalizeTag } from "@/lib/tag";

export const queryKeys = {
  player: {
    all: ["player"] as const,
    detail: (tag: string) =>
      [...queryKeys.player.all, "detail", normalizeTag(tag)] as const,
    brawlers: (tag: string) =>
      [...queryKeys.player.all, "brawlers", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.player.all, "renewal", normalizeTag(tag)] as const,
  },
  club: {
    all: ["club"] as const,
    detail: (tag: string) =>
      [...queryKeys.club.all, "detail", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.club.all, "renewal", normalizeTag(tag)] as const,
  },
  brawler: {
    all: ["brawler"] as const,
    list: () => [...queryKeys.brawler.all, "list"] as const,
    detail: (id: number | string) =>
      [...queryKeys.brawler.all, "detail", String(id)] as const,
  },
} as const;
```

- [ ] **Step 2: useBrawlers**

```typescript
// src/hooks/queries/useBrawlers.ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawlers(options?: { initialData?: Brawler[] }) {
  return useQuery({
    queryKey: queryKeys.brawler.list(),
    queryFn: ({ signal }) =>
      brawlerApi.list({ signal }).then((r) => r.content.map(toBrawler)),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

- [ ] **Step 3: useBrawler**

```typescript
// src/hooks/queries/useBrawler.ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawler(
  id: number | string,
  options?: { initialData?: Brawler },
) {
  return useQuery({
    queryKey: queryKeys.brawler.detail(id),
    queryFn: ({ signal }) => brawlerApi.get(id, { signal }).then(toBrawler),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

- [ ] **Step 4: Verify**

`pnpm typecheck` — PASS.

- [ ] **Step 5: Commit**

```bash
git add src/hooks/queries/
git commit -m "feat(frontend): add brawler query keys and hooks"
```

---

## Task 8: BrawlerAvatar primitive (real-data)

**Files:**
- Create: `src/components/primitives/BrawlerAvatar.tsx`

- [ ] **Step 1: Write the component**

A self-contained avatar that renders an `<img>` if `imageUrl` is provided, otherwise a colored gradient + first letter.

```typescript
import type { CSSProperties } from "react";
import type { BrawlerRarity } from "@/api/types";

const RARITY_BORDER: Record<BrawlerRarity, string> = {
  COMMON: "#B4B9C7",
  RARE: "#2BD576",
  SUPER_RARE: "#3DE0FF",
  EPIC: "#9146FF",
  MYTHIC: "#FF3A3A",
  LEGENDARY: "#FFC60A",
  ULTRA_LEGENDARY: "#FF8A1F",
};

interface Props {
  name: string;
  imageUrl: string | null;
  rarity?: BrawlerRarity;
  size?: number;
  className?: string;
  showFrame?: boolean;
}

export function BrawlerAvatar({
  name,
  imageUrl,
  rarity,
  size = 48,
  className = "",
  showFrame = false,
}: Props) {
  const frameColor = rarity ? RARITY_BORDER[rarity] : "#B4B9C7";
  const style: CSSProperties = {
    width: size,
    height: size,
    boxShadow: showFrame
      ? `inset 0 0 0 2px ${frameColor}, inset 0 0 0 3px rgba(0,0,0,0.2)`
      : undefined,
  };

  if (imageUrl) {
    return (
      <div
        className={`relative overflow-hidden rounded-md ${className}`}
        style={style}
        title={name}
      >
        <img
          src={imageUrl}
          alt={name}
          width={size}
          height={size}
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  const initial = name.slice(0, 1);
  const fontSize = Math.max(10, size * 0.4);
  return (
    <div
      className={`flex items-center justify-center rounded-md bg-gradient-to-br from-[#FFC60A] to-[#FF8A1F] font-display font-extrabold text-white ${className}`}
      style={{ ...style, fontSize }}
      title={name}
    >
      {initial}
    </div>
  );
}
```

- [ ] **Step 2: Typecheck**

`pnpm typecheck` — PASS.

- [ ] **Step 3: Commit**

```bash
git add src/components/primitives/BrawlerAvatar.tsx
git commit -m "feat(frontend): add BrawlerAvatar primitive (real-data, image+fallback)"
```

---

## Task 9: List page — RSC fetch + rewrite

**Files:**
- Modify: `app/brawlers/page.tsx`
- Modify (rewrite): `app/brawlers/_components/BrawlersListPage.tsx`
- Create: `app/brawlers/_components/BrawlerCard.tsx`
- Create: `app/brawlers/_components/BrawlerFilters.tsx`

- [ ] **Step 1: Rewrite `app/brawlers/page.tsx`**

```typescript
import type { Metadata } from "next";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { sortByName } from "@/domain/brawler/filters";
import { buildMetadata } from "@/lib/seo";
import BrawlersListPage from "./_components/BrawlersListPage";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 도감 | ImStarGG",
  description:
    "브롤스타즈 브롤러의 정보, 가젯, 스타파워, 기어, 하이퍼차지를 확인하세요.",
  path: "/brawlers",
});

export const revalidate = 86400;

export default async function Page() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  const brawlers = sortByName(list.content.map(toBrawler));
  return <BrawlersListPage initialBrawlers={brawlers} />;
}
```

- [ ] **Step 2: Create `BrawlerCard`**

```typescript
// app/brawlers/_components/BrawlerCard.tsx
"use client";

import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

const ROLE_LABEL: Record<Brawler["role"], string> = {
  DAMAGE_DEALER: "어태커",
  TANK: "탱커",
  MARKSMAN: "마크스맨",
  ASSASSIN: "어쌔신",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "아틸러리",
};

const RARITY_LABEL: Record<Brawler["rarity"], string> = {
  COMMON: "기본",
  RARE: "레어",
  SUPER_RARE: "슈퍼 레어",
  EPIC: "에픽",
  MYTHIC: "미식",
  LEGENDARY: "레전더리",
  ULTRA_LEGENDARY: "울트라 레전더리",
};

export function BrawlerCard({ brawler }: { brawler: Brawler }) {
  return (
    <Link href={`/brawlers/${brawler.id}`} className="block">
      <Card className="flex flex-col items-center gap-2 p-3 transition hover:border-foreground">
        <BrawlerAvatar
          name={brawler.name}
          imageUrl={brawler.portraitImageUrl}
          rarity={brawler.rarity}
          size={88}
          showFrame
        />
        <div className="w-full truncate text-center text-sm font-bold">
          {brawler.name}
        </div>
        <div className="flex flex-wrap items-center justify-center gap-1">
          <Badge variant="secondary" className="text-[10px]">
            {ROLE_LABEL[brawler.role]}
          </Badge>
          <Badge variant="outline" className="text-[10px]">
            {RARITY_LABEL[brawler.rarity]}
          </Badge>
        </div>
      </Card>
    </Link>
  );
}
```

- [ ] **Step 3: Create `BrawlerFilters`**

```typescript
// app/brawlers/_components/BrawlerFilters.tsx
"use client";

import type { BrawlerRarity, BrawlerRole } from "@/api/types";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search } from "lucide-react";

const ROLES: ReadonlyArray<{ value: BrawlerRole | "ALL"; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "DAMAGE_DEALER", label: "어태커" },
  { value: "TANK", label: "탱커" },
  { value: "MARKSMAN", label: "마크스맨" },
  { value: "ASSASSIN", label: "어쌔신" },
  { value: "SUPPORT", label: "서포터" },
  { value: "CONTROLLER", label: "컨트롤러" },
  { value: "ARTILLERY", label: "아틸러리" },
];

const RARITIES: ReadonlyArray<{ value: BrawlerRarity | "ALL"; label: string }> = [
  { value: "ALL", label: "전체 희귀도" },
  { value: "COMMON", label: "기본" },
  { value: "RARE", label: "레어" },
  { value: "SUPER_RARE", label: "슈퍼 레어" },
  { value: "EPIC", label: "에픽" },
  { value: "MYTHIC", label: "미식" },
  { value: "LEGENDARY", label: "레전더리" },
  { value: "ULTRA_LEGENDARY", label: "울트라 레전더리" },
];

interface Props {
  role: BrawlerRole | "ALL";
  rarity: BrawlerRarity | "ALL";
  search: string;
  onRoleChange: (v: BrawlerRole | "ALL") => void;
  onRarityChange: (v: BrawlerRarity | "ALL") => void;
  onSearchChange: (v: string) => void;
}

export function BrawlerFilters({
  role,
  rarity,
  search,
  onRoleChange,
  onRarityChange,
  onSearchChange,
}: Props) {
  return (
    <div className="space-y-3">
      <div className="-mx-1 flex gap-1 overflow-x-auto px-1 pb-1">
        {ROLES.map((r) => {
          const active = role === r.value;
          return (
            <button
              key={r.value}
              type="button"
              onClick={() => onRoleChange(r.value)}
              className={
                active
                  ? "shrink-0 rounded border border-foreground bg-foreground px-3 py-1.5 text-xs font-bold text-background"
                  : "shrink-0 rounded border border-border bg-background px-3 py-1.5 text-xs font-bold text-foreground hover:border-foreground"
              }
            >
              {r.label}
            </button>
          );
        })}
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <Select value={rarity} onValueChange={(v) => onRarityChange(v as BrawlerRarity | "ALL")}>
          <SelectTrigger className="w-44" aria-label="희귀도 필터">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {RARITIES.map((r) => (
              <SelectItem key={r.value} value={r.value}>
                {r.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="relative flex-1 min-w-[180px]">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="브롤러 이름 검색"
            className="pl-9"
            aria-label="브롤러 이름 검색"
          />
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Rewrite `BrawlersListPage`**

Replace `app/brawlers/_components/BrawlersListPage.tsx` ENTIRELY:

```typescript
"use client";

import { useMemo, useState } from "react";
import type { BrawlerRarity, BrawlerRole } from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";
import { useBrawlers } from "@/hooks/queries/useBrawlers";
import { filterBrawlers, sortByName } from "@/domain/brawler/filters";
import { BrawlerCard } from "./BrawlerCard";
import { BrawlerFilters } from "./BrawlerFilters";

export default function BrawlersListPage({
  initialBrawlers,
}: {
  initialBrawlers: Brawler[];
}) {
  const { data: brawlers = initialBrawlers } = useBrawlers({ initialData: initialBrawlers });
  const [role, setRole] = useState<BrawlerRole | "ALL">("ALL");
  const [rarity, setRarity] = useState<BrawlerRarity | "ALL">("ALL");
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const opts = {
      role: role === "ALL" ? undefined : role,
      rarity: rarity === "ALL" ? undefined : rarity,
      search: search.trim() || undefined,
    };
    return sortByName(filterBrawlers(brawlers, opts));
  }, [brawlers, role, rarity, search]);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6 space-y-5">
      <header>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          BRAWLERS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 도감</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {brawlers.length}명의 브롤러 · 가젯, 스타파워, 기어, 하이퍼차지 정보
        </p>
      </header>

      <BrawlerFilters
        role={role}
        rarity={rarity}
        search={search}
        onRoleChange={setRole}
        onRarityChange={setRarity}
        onSearchChange={setSearch}
      />

      {filtered.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">
          조건에 맞는 브롤러가 없어요.
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {filtered.map((b) => (
            <BrawlerCard key={b.id} brawler={b} />
          ))}
        </div>
      )}
    </main>
  );
}
```

- [ ] **Step 5: Verify**

`pnpm typecheck` — PASS.
`pnpm test:run` — all PASS.

- [ ] **Step 6: Commit**

```bash
git add app/brawlers/page.tsx app/brawlers/_components/
git commit -m "feat(frontend): real brawlers list page with filters and search"
```

---

## Task 10: Detail page RSC + error/not-found

**Files:**
- Modify: `app/brawlers/[id]/page.tsx`
- Create: `app/brawlers/[id]/error.tsx`
- Create: `app/brawlers/[id]/not-found.tsx`

- [ ] **Step 1: error.tsx**

```typescript
"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function BrawlerError({
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

- [ ] **Step 2: not-found.tsx**

```typescript
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function BrawlerNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">브롤러를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">존재하지 않는 ID일 수 있어요.</p>
      <Button asChild variant="outline">
        <Link href="/brawlers">브롤러 도감</Link>
      </Button>
    </main>
  );
}
```

- [ ] **Step 3: Rewrite `app/brawlers/[id]/page.tsx`**

```typescript
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";

type Params = { params: Promise<{ id: string }> };

export const revalidate = 86400;

export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

const ROLE_LABEL_META: Record<string, string> = {
  DAMAGE_DEALER: "어태커",
  TANK: "탱커",
  MARKSMAN: "마크스맨",
  ASSASSIN: "어쌔신",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "아틸러리",
};

const RARITY_LABEL_META: Record<string, string> = {
  COMMON: "기본",
  RARE: "레어",
  SUPER_RARE: "슈퍼 레어",
  EPIC: "에픽",
  MYTHIC: "미식",
  LEGENDARY: "레전더리",
  ULTRA_LEGENDARY: "울트라 레전더리",
};

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
    const brawler = toBrawler(dto);
    const role = ROLE_LABEL_META[brawler.role] ?? brawler.role;
    const rarity = RARITY_LABEL_META[brawler.rarity] ?? brawler.rarity;
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

export default async function BrawlerPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return <BrawlerContent initialBrawler={toBrawler(dto)} />;
}
```

- [ ] **Step 4: Note**

`BrawlerContent` does not yet exist; typecheck will fail. Skip and commit; Task 11 adds it.

- [ ] **Step 5: Commit**

```bash
git add app/brawlers/[id]/page.tsx app/brawlers/[id]/error.tsx app/brawlers/[id]/not-found.tsx
git commit -m "feat(frontend): brawler detail RSC page + error/not-found"
```

---

## Task 11: Detail components

**Files:**
- Create: `app/brawlers/[id]/_components/BrawlerContent.tsx`
- Create: `app/brawlers/[id]/_components/BrawlerHero.tsx`
- Create: `app/brawlers/[id]/_components/BrawlerItems.tsx`
- Create: `app/brawlers/[id]/_components/BrawlerItemCard.tsx`
- Create: `app/brawlers/[id]/_components/BrawlerBuffies.tsx`

- [ ] **Step 1: BrawlerContent**

```typescript
"use client";

import type { Brawler } from "@/domain/brawler/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerBuffies } from "./BrawlerBuffies";

export function BrawlerContent({ initialBrawler }: { initialBrawler: Brawler }) {
  const { data: brawler = initialBrawler } = useBrawler(initialBrawler.id, {
    initialData: initialBrawler,
  });
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6 space-y-5">
      <BrawlerHero brawler={brawler} />
      <BrawlerItems brawler={brawler} />
      {brawler.buffies.length > 0 && <BrawlerBuffies buffies={brawler.buffies} />}
    </main>
  );
}
```

- [ ] **Step 2: BrawlerHero**

```typescript
"use client";

import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Badge } from "@/components/ui/badge";

const ROLE_LABEL: Record<Brawler["role"], string> = {
  DAMAGE_DEALER: "어태커",
  TANK: "탱커",
  MARKSMAN: "마크스맨",
  ASSASSIN: "어쌔신",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "아틸러리",
};

const RARITY_LABEL: Record<Brawler["rarity"], string> = {
  COMMON: "기본",
  RARE: "레어",
  SUPER_RARE: "슈퍼 레어",
  EPIC: "에픽",
  MYTHIC: "미식",
  LEGENDARY: "레전더리",
  ULTRA_LEGENDARY: "울트라 레전더리",
};

export function BrawlerHero({ brawler }: { brawler: Brawler }) {
  return (
    <section className="flex flex-col items-center gap-4 rounded-lg border bg-card p-6 sm:flex-row sm:gap-6">
      <BrawlerAvatar
        name={brawler.name}
        imageUrl={brawler.portraitImageUrl}
        rarity={brawler.rarity}
        size={140}
        showFrame
      />
      <div className="flex-1 text-center sm:text-left">
        <h1 className="font-display text-3xl tracking-[0.02em]">{brawler.name}</h1>
        <div className="mt-2 flex flex-wrap justify-center gap-2 sm:justify-start">
          <Badge variant="secondary">{ROLE_LABEL[brawler.role]}</Badge>
          <Badge variant="outline">{RARITY_LABEL[brawler.rarity]}</Badge>
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Step 3: BrawlerItemCard**

```typescript
"use client";

import type { BrawlerItem } from "@/domain/brawler/types";

export function BrawlerItemCard({ item }: { item: BrawlerItem }) {
  return (
    <div className="flex items-center gap-3 rounded border bg-card p-2.5">
      <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded bg-muted">
        {item.imageUrl ? (
          <img
            src={item.imageUrl}
            alt={item.name}
            width={48}
            height={48}
            className="h-full w-full object-contain"
          />
        ) : (
          <span className="font-display text-lg font-extrabold text-muted-foreground">
            {item.name.slice(0, 1)}
          </span>
        )}
      </div>
      <div className="min-w-0 text-sm font-bold truncate">{item.name}</div>
    </div>
  );
}
```

- [ ] **Step 4: BrawlerItems**

```typescript
"use client";

import type { Brawler, BrawlerItem } from "@/domain/brawler/types";
import { BrawlerItemCard } from "./BrawlerItemCard";

function Section({ title, items }: { title: string; items: BrawlerItem[] }) {
  if (items.length === 0) return null;
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">
        {title} <span className="text-muted-foreground">({items.length})</span>
      </h2>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {items.map((it) => (
          <BrawlerItemCard key={it.id} item={it} />
        ))}
      </div>
    </section>
  );
}

export function BrawlerItems({ brawler }: { brawler: Brawler }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Section title="가젯" items={brawler.gadgets} />
      <Section title="스타파워" items={brawler.starPowers} />
      <Section title="기어" items={brawler.gears} />
      <Section title="하이퍼차지" items={brawler.hyperCharges} />
    </div>
  );
}
```

- [ ] **Step 5: BrawlerBuffies**

```typescript
"use client";

import type { BuffyResponse } from "@/api/types";

const TYPE_LABEL: Record<string, string> = {
  MYTHIC_GEAR: "미식 기어",
};

export function BrawlerBuffies({ buffies }: { buffies: BuffyResponse[] }) {
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">버피 ({buffies.length})</h2>
      <div className="flex flex-wrap gap-2">
        {buffies.map((b, i) => {
          const label = TYPE_LABEL[b.type] ?? b.type;
          return (
            <div
              key={`${b.type}-${i}`}
              className="flex items-center gap-2 rounded border bg-background px-2.5 py-1.5"
            >
              {b.imageUrl && (
                <img src={b.imageUrl} alt={label} width={20} height={20} className="h-5 w-5" />
              )}
              <span className="text-xs font-bold">{label}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
```

- [ ] **Step 6: Typecheck**

`pnpm typecheck` — PASS.

- [ ] **Step 7: Commit**

```bash
git add app/brawlers/[id]/_components/
git commit -m "feat(frontend): brawler detail components (Hero/Items/ItemCard/Buffies)"
```

---

## Task 12: Sitemap dynamic brawler entries

**Files:**
- Modify: `app/sitemap.ts`

- [ ] **Step 1: Replace contents**

```typescript
import type { MetadataRoute } from "next";
import { env } from "@/config/env";
import { brawlerApi } from "@/api/endpoints/brawler";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = env.NEXT_PUBLIC_SITE_URL;
  const now = new Date();

  let brawlerEntries: MetadataRoute.Sitemap = [];
  try {
    const list = await brawlerApi.list({ next: { revalidate: 86400 } });
    brawlerEntries = list.content.map((b) => ({
      url: `${base}/brawlers/${b.id}`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.5,
    }));
  } catch {
    brawlerEntries = [];
  }

  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
    ...brawlerEntries,
  ];
}
```

- [ ] **Step 2: Verify**

`pnpm typecheck` — PASS.

- [ ] **Step 3: Commit**

```bash
git add app/sitemap.ts
git commit -m "feat(frontend): include brawler detail URLs in sitemap"
```

---

## Task 13: Delete legacy mock detail page

**Files:**
- Delete: `app/brawlers/[id]/_components/BrawlerDetailPage.tsx`

- [ ] **Step 1: Confirm no references**

`pnpm exec rg -n "BrawlerDetailPage"` — only the file itself.

- [ ] **Step 2: Delete + verify build**

```bash
git rm 'app/brawlers/[id]/_components/BrawlerDetailPage.tsx'
pnpm typecheck && pnpm build
```

Both PASS.

- [ ] **Step 3: Commit**

```bash
git commit -m "chore(frontend): remove legacy BrawlerDetailPage mock"
```

---

## Task 14: E2E spec

**Files:**
- Create: `tests/e2e/brawler.spec.ts`

- [ ] **Step 1: Write spec**

```typescript
import { test, expect } from "@playwright/test";

test("브롤러 목록 페이지가 카드를 렌더한다", async ({ page }) => {
  await page.goto("/brawlers");
  const cards = page.locator('a[href^="/brawlers/"]');
  await expect(cards.first()).toBeVisible({ timeout: 15_000 });
  expect(await cards.count()).toBeGreaterThanOrEqual(50);
});

test("이름 검색이 결과를 좁힌다", async ({ page }) => {
  await page.goto("/brawlers");
  const input = page.getByLabel("브롤러 이름 검색");
  await input.waitFor({ state: "visible", timeout: 15_000 });
  await input.fill("셸리");
  await expect(page.locator('a[href^="/brawlers/"]')).toHaveCount(1, { timeout: 5_000 });
});

test("카드 클릭 시 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/brawlers");
  const firstCard = page.locator('a[href^="/brawlers/"]').first();
  await firstCard.waitFor({ state: "visible", timeout: 15_000 });
  await firstCard.click();
  await page.waitForURL(/\/brawlers\/\d+/, { timeout: 15_000 });
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
});

test("상세 페이지 SEO: title에 브롤러 이름 + canonical 1개", async ({ page }) => {
  await page.goto("/brawlers/16000000"); // Shelly
  await expect(page).toHaveTitle(/ImStarGG/, { timeout: 15_000 });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});
```

- [ ] **Step 2: Note**

E2E hits prod API. If the Playwright runner is not configured for the local environment, run typecheck only and skip execution:

```bash
pnpm typecheck
```

Otherwise:

```bash
pnpm test:e2e tests/e2e/brawler.spec.ts
```

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/brawler.spec.ts
git commit -m "test(frontend): e2e brawler list/search/detail/SEO"
```

---

## Task 15: Final verification

- [ ] **Step 1: All checks**

```bash
pnpm typecheck
pnpm lint
pnpm test:run
pnpm build
```

Expected: all PASS. Note: `pnpm build` will hit prod API for `/v1/brawlers` (used by `generateStaticParams` and `sitemap`). If the build environment cannot reach prod, this task may need to be done by the user.

- [ ] **Step 2: Manual smoke (recommended)**

```bash
pnpm dev
```

Visit `/brawlers` → cards render. Search "셸리" → narrows. Click a card → detail page with name/role/rarity/items.

- [ ] **Step 3: No additional commit needed unless cleanup required.**
