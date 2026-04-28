# Frontend Event Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `app/events/*` mock with real backend integration: rotation list with live countdown, solo-rank maps, map-name search, and SSG detail header.

**Architecture:** RSC fetches `/v1/events/rotation` + `/v1/events/solo-rank` in parallel for the list page and hands `initialData` to TanStack Query for client-side hydration. Detail uses `generateStaticParams` over the union of rotation + solo-rank IDs with `revalidate: 3600`. Search calls `/v1/event/search?name=` (map-name only) via debounced query. Domain types flatten `MessagesResponse` through the existing `pickName` helper.

**Tech Stack:** Next.js 16 App Router, React 19, TanStack Query v5, shadcn/ui (Card/Input/Search), Vitest, Playwright. Existing patterns from brawler/club domains.

**Spec:** `docs/superpowers/specs/2026-04-28-frontend-event-domain-design.md`

**Conventions:**
- Working directory: `imstargg-frontend/` (run all `pnpm` commands from here unless otherwise stated)
- File paths in this plan are relative to `imstargg-frontend/`
- Stage only files explicitly touched by each task. **Never use `git add -A` or `git add .`**
- Commit message style: `<type>(frontend): <imperative summary>` matching club/brawler history

---

## File Structure

**Created:**
- `src/api/endpoints/event.ts`
- `src/domain/event/types.ts`
- `src/domain/event/mappers.ts`
- `src/lib/countdown.ts`
- `src/hooks/queries/useRotationEvents.ts`
- `src/hooks/queries/useSoloRankEvents.ts`
- `src/hooks/queries/useEvent.ts`
- `src/hooks/queries/useEventSearch.ts`
- `app/events/_components/EventsListPage.tsx` (replaces mock `EventsPage.tsx`)
- `app/events/_components/RotationSection.tsx`
- `app/events/_components/RotationCard.tsx`
- `app/events/_components/SoloRankSection.tsx`
- `app/events/_components/SoloRankCard.tsx`
- `app/events/_components/EventCountdown.tsx`
- `app/events/_components/EventSearch.tsx`
- `app/events/_components/EventSearchResults.tsx`
- `app/events/[id]/error.tsx`
- `app/events/[id]/not-found.tsx`
- `app/events/[id]/_components/EventContent.tsx`
- `app/events/[id]/_components/EventHero.tsx`
- `tests/unit/lib/countdown.test.ts`
- `tests/unit/domain/event/mappers.test.ts`
- `tests/e2e/event.spec.ts`

**Modified:**
- `src/api/types.ts` — add `BattleEventResultType`, `BattleEventModeResponse`, `BattleEventResponse`, `RotationBattleEventResponse`
- `src/hooks/queries/queryKeys.ts` — add `event` namespace
- `app/events/page.tsx` — replace mock import with RSC fetch
- `app/events/[id]/page.tsx` — replace mock import with RSC fetch + `generateStaticParams` + `generateMetadata`
- `app/sitemap.ts` — add dynamic `/events/{id}` entries
- `src/lib/data.ts` — remove mock event symbols (`ROTATION`, `SOLO_RANKED_MAPS`, `RotationItem`, `RankedMap`, `ResolvedEvent`, `getEvent`, `EventBrawlerStat`, `genEventBrawlerStats`, `EventInfo` if only event-scoped)

**Deleted:**
- `app/events/_components/EventsPage.tsx`
- `app/events/[id]/_components/EventDetailPage.tsx`

---

## Task 1: Add backend DTO types

**Files:**
- Modify: `src/api/types.ts` (append new types after `BuffyResponse` block, before any unrelated section)

- [ ] **Step 1: Add DTOs**

Append to `src/api/types.ts`:

```typescript
// === BattleEvent ===
export type BattleEventResultType = "RESULT" | "RANK";

export interface BattleEventModeResponse {
  id: number;
  name: MessagesResponse;
  iconImageUrl: string | null;
  resultType: BattleEventResultType;
}

export interface BattleEventResponse {
  id: number;
  mode: BattleEventModeResponse | null;
  mapName: MessagesResponse;
}

export interface RotationBattleEventResponse {
  event: BattleEventResponse;
  startTime: string;  // OffsetDateTime serialized as ISO 8601
  endTime: string;
}
```

- [ ] **Step 2: Verify typecheck**

Run from `imstargg-frontend/`: `pnpm typecheck`
Expected: PASS (no consumers yet)

- [ ] **Step 3: Commit**

```bash
git add src/api/types.ts
git commit -m "feat(frontend): add BattleEvent DTO types"
```

---

## Task 2: Add `eventApi` endpoint

**Files:**
- Create: `src/api/endpoints/event.ts`

- [ ] **Step 1: Create endpoint**

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BattleEventResponse,
  ListResponse,
  RotationBattleEventResponse,
} from "@/api/types";

export const eventApi = {
  rotation: (opts?: FetchOpts) =>
    apiFetch<ListResponse<RotationBattleEventResponse>>(`/v1/events/rotation`, opts),

  soloRank: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(`/v1/events/solo-rank`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BattleEventResponse>(`/v1/events/${id}`, opts),

  search: (name: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(
      `/v1/event/search?name=${encodeURIComponent(name)}`,
      opts,
    ),
};
```

- [ ] **Step 2: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/api/endpoints/event.ts
git commit -m "feat(frontend): add eventApi endpoint"
```

---

## Task 3: Add domain types

**Files:**
- Create: `src/domain/event/types.ts`

- [ ] **Step 1: Create types**

```typescript
import type { BattleEventResultType } from "@/api/types";

export interface BattleEventMode {
  id: number;
  name: string;
  iconImageUrl: string | null;
  resultType: BattleEventResultType;
}

export interface BattleEvent {
  id: number;
  mode: BattleEventMode | null;
  mapName: string;
}

export interface RotationEvent {
  event: BattleEvent;
  startTime: string;
  endTime: string;
}
```

- [ ] **Step 2: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/domain/event/types.ts
git commit -m "feat(frontend): add Event domain types"
```

---

## Task 4: Domain mappers (TDD)

**Files:**
- Create: `tests/unit/domain/event/mappers.test.ts`
- Create: `src/domain/event/mappers.ts`

- [ ] **Step 1: Write failing tests**

Create `tests/unit/domain/event/mappers.test.ts`:

```typescript
import { describe, it, expect } from "vitest";
import {
  toBattleEvent,
  toBattleEventMode,
  toRotationEvent,
} from "@/domain/event/mappers";
import type {
  BattleEventModeResponse,
  BattleEventResponse,
  RotationBattleEventResponse,
} from "@/api/types";

const dtoMode = (overrides: Partial<BattleEventModeResponse> = {}): BattleEventModeResponse => ({
  id: 14000000,
  name: { ko: "젬 그랩", en: "Gem Grab" },
  iconImageUrl: "https://cdn/gem.png",
  resultType: "RESULT",
  ...overrides,
});

const dtoEvent = (overrides: Partial<BattleEventResponse> = {}): BattleEventResponse => ({
  id: 15000001,
  mode: dtoMode(),
  mapName: { ko: "광산 분쟁", en: "Hard Rock Mine" },
  ...overrides,
});

describe("toBattleEventMode", () => {
  it("flattens MessagesResponse name to ko-first", () => {
    const result = toBattleEventMode(dtoMode());
    expect(result).toEqual({
      id: 14000000,
      name: "젬 그랩",
      iconImageUrl: "https://cdn/gem.png",
      resultType: "RESULT",
    });
  });

  it("preserves null iconImageUrl", () => {
    const result = toBattleEventMode(dtoMode({ iconImageUrl: null }));
    expect(result.iconImageUrl).toBeNull();
  });

  it("falls back to en when ko missing", () => {
    const result = toBattleEventMode(dtoMode({ name: { en: "Gem Grab" } }));
    expect(result.name).toBe("Gem Grab");
  });
});

describe("toBattleEvent", () => {
  it("flattens mapName and maps mode", () => {
    const result = toBattleEvent(dtoEvent());
    expect(result.id).toBe(15000001);
    expect(result.mapName).toBe("광산 분쟁");
    expect(result.mode?.name).toBe("젬 그랩");
  });

  it("preserves null mode", () => {
    const result = toBattleEvent(dtoEvent({ mode: null }));
    expect(result.mode).toBeNull();
  });

  it("returns (unknown) when mapName messages object is empty", () => {
    const result = toBattleEvent(dtoEvent({ mapName: {} }));
    expect(result.mapName).toBe("(unknown)");
  });
});

describe("toRotationEvent", () => {
  it("preserves ISO startTime/endTime and maps inner event", () => {
    const dto: RotationBattleEventResponse = {
      event: dtoEvent(),
      startTime: "2026-04-28T00:00:00Z",
      endTime: "2026-04-29T00:00:00Z",
    };
    const result = toRotationEvent(dto);
    expect(result.startTime).toBe("2026-04-28T00:00:00Z");
    expect(result.endTime).toBe("2026-04-29T00:00:00Z");
    expect(result.event.mapName).toBe("광산 분쟁");
  });
});
```

- [ ] **Step 2: Run tests, expect failure**

Run: `pnpm test:run tests/unit/domain/event/mappers.test.ts`
Expected: FAIL (module `@/domain/event/mappers` not found)

- [ ] **Step 3: Implement mappers**

Create `src/domain/event/mappers.ts`:

```typescript
import type {
  BattleEventModeResponse,
  BattleEventResponse,
  RotationBattleEventResponse,
} from "@/api/types";
import type {
  BattleEvent,
  BattleEventMode,
  RotationEvent,
} from "@/domain/event/types";
import { pickName } from "@/domain/i18n";

export function toBattleEventMode(dto: BattleEventModeResponse): BattleEventMode {
  return {
    id: dto.id,
    name: pickName(dto.name),
    iconImageUrl: dto.iconImageUrl,
    resultType: dto.resultType,
  };
}

export function toBattleEvent(dto: BattleEventResponse): BattleEvent {
  return {
    id: dto.id,
    mode: dto.mode ? toBattleEventMode(dto.mode) : null,
    mapName: pickName(dto.mapName),
  };
}

export function toRotationEvent(dto: RotationBattleEventResponse): RotationEvent {
  return {
    event: toBattleEvent(dto.event),
    startTime: dto.startTime,
    endTime: dto.endTime,
  };
}
```

- [ ] **Step 4: Run tests, expect pass**

Run: `pnpm test:run tests/unit/domain/event/mappers.test.ts`
Expected: PASS (8 tests)

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/event/mappers.test.ts src/domain/event/mappers.ts
git commit -m "feat(frontend): add Event domain mappers (TDD)"
```

---

## Task 5: Countdown utility (TDD)

**Files:**
- Create: `tests/unit/lib/countdown.test.ts`
- Create: `src/lib/countdown.ts`

- [ ] **Step 1: Write failing tests**

Create `tests/unit/lib/countdown.test.ts`:

```typescript
import { describe, it, expect } from "vitest";
import { computeCountdown, formatCountdown } from "@/lib/countdown";

describe("computeCountdown", () => {
  it("computes hours/minutes/seconds for 2h13m45s remaining", () => {
    const now = new Date("2026-04-28T00:00:00Z").getTime();
    const end = "2026-04-28T02:13:45Z";
    const cd = computeCountdown(end, now);
    expect(cd.hours).toBe(2);
    expect(cd.minutes).toBe(13);
    expect(cd.seconds).toBe(45);
    expect(cd.ended).toBe(false);
    expect(cd.totalMs).toBe(2 * 3600_000 + 13 * 60_000 + 45_000);
  });

  it("flags ended when totalMs is zero or negative", () => {
    const now = new Date("2026-04-28T02:00:00Z").getTime();
    const end = "2026-04-28T01:00:00Z";
    const cd = computeCountdown(end, now);
    expect(cd.ended).toBe(true);
    expect(cd.hours).toBe(0);
    expect(cd.minutes).toBe(0);
    expect(cd.seconds).toBe(0);
  });

  it("clamps negative remaining to zero parts", () => {
    const cd = computeCountdown("2026-04-28T01:00:00Z", new Date("2026-04-29T01:00:00Z").getTime());
    expect(cd.hours).toBe(0);
    expect(cd.minutes).toBe(0);
    expect(cd.seconds).toBe(0);
    expect(cd.totalMs).toBeLessThan(0);
    expect(cd.ended).toBe(true);
  });
});

describe("formatCountdown", () => {
  it("pads to HH:MM:SS", () => {
    expect(
      formatCountdown({ hours: 2, minutes: 3, seconds: 5, totalMs: 1, ended: false }),
    ).toBe("02:03:05");
  });

  it("returns 종료됨 when ended", () => {
    expect(
      formatCountdown({ hours: 0, minutes: 0, seconds: 0, totalMs: -1, ended: true }),
    ).toBe("종료됨");
  });
});
```

- [ ] **Step 2: Run tests, expect failure**

Run: `pnpm test:run tests/unit/lib/countdown.test.ts`
Expected: FAIL (module `@/lib/countdown` not found)

- [ ] **Step 3: Implement countdown**

Create `src/lib/countdown.ts`:

```typescript
export interface Countdown {
  hours: number;
  minutes: number;
  seconds: number;
  totalMs: number;
  ended: boolean;
}

export function computeCountdown(endIso: string, now: number = Date.now()): Countdown {
  const end = new Date(endIso).getTime();
  const totalMs = end - now;
  const ended = totalMs <= 0;
  const safe = Math.max(totalMs, 0);
  const totalSeconds = Math.floor(safe / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return { hours, minutes, seconds, totalMs, ended };
}

export function formatCountdown(c: Countdown): string {
  if (c.ended) return "종료됨";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(c.hours)}:${pad(c.minutes)}:${pad(c.seconds)}`;
}
```

- [ ] **Step 4: Run tests, expect pass**

Run: `pnpm test:run tests/unit/lib/countdown.test.ts`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add tests/unit/lib/countdown.test.ts src/lib/countdown.ts
git commit -m "feat(frontend): add countdown utility (TDD)"
```

---

## Task 6: Query keys + hooks

**Files:**
- Modify: `src/hooks/queries/queryKeys.ts`
- Create: `src/hooks/queries/useRotationEvents.ts`
- Create: `src/hooks/queries/useSoloRankEvents.ts`
- Create: `src/hooks/queries/useEvent.ts`
- Create: `src/hooks/queries/useEventSearch.ts`

- [ ] **Step 1: Extend queryKeys**

Add new namespace inside `queryKeys` object in `src/hooks/queries/queryKeys.ts` (after `brawler`):

```typescript
  event: {
    all: ["event"] as const,
    rotation: () => [...queryKeys.event.all, "rotation"] as const,
    soloRank: () => [...queryKeys.event.all, "solo-rank"] as const,
    detail: (id: number | string) =>
      [...queryKeys.event.all, "detail", String(id)] as const,
    search: (name: string) =>
      [...queryKeys.event.all, "search", name] as const,
  },
```

- [ ] **Step 2: Create rotation hook**

Create `src/hooks/queries/useRotationEvents.ts`:

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toRotationEvent } from "@/domain/event/mappers";
import type { RotationEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useRotationEvents(options?: { initialData?: RotationEvent[] }) {
  return useQuery({
    queryKey: queryKeys.event.rotation(),
    queryFn: ({ signal }) =>
      eventApi.rotation({ signal }).then((r) => r.content.map(toRotationEvent)),
    initialData: options?.initialData,
    staleTime: 5 * 60_000,
  });
}
```

- [ ] **Step 3: Create solo-rank hook**

Create `src/hooks/queries/useSoloRankEvents.ts`:

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useSoloRankEvents(options?: { initialData?: BattleEvent[] }) {
  return useQuery({
    queryKey: queryKeys.event.soloRank(),
    queryFn: ({ signal }) =>
      eventApi.soloRank({ signal }).then((r) => r.content.map(toBattleEvent)),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

- [ ] **Step 4: Create detail hook**

Create `src/hooks/queries/useEvent.ts`:

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useEvent(id: number | string, options?: { initialData?: BattleEvent }) {
  return useQuery({
    queryKey: queryKeys.event.detail(id),
    queryFn: ({ signal }) => eventApi.get(id, { signal }).then(toBattleEvent),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

- [ ] **Step 5: Create search hook**

Create `src/hooks/queries/useEventSearch.ts`:

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useEventSearch(name: string) {
  const trimmed = name.trim();
  return useQuery({
    queryKey: queryKeys.event.search(trimmed),
    queryFn: ({ signal }) =>
      eventApi.search(trimmed, { signal }).then((r) => r.content.map(toBattleEvent)),
    enabled: trimmed.length > 0,
    staleTime: 60_000,
  });
}
```

- [ ] **Step 6: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/hooks/queries/queryKeys.ts src/hooks/queries/useRotationEvents.ts src/hooks/queries/useSoloRankEvents.ts src/hooks/queries/useEvent.ts src/hooks/queries/useEventSearch.ts
git commit -m "feat(frontend): add event query keys and hooks"
```

---

## Task 7: `EventCountdown` component

**Files:**
- Create: `app/events/_components/EventCountdown.tsx`

- [ ] **Step 1: Implement**

```tsx
"use client";

import { useEffect, useState } from "react";
import { computeCountdown, formatCountdown } from "@/lib/countdown";

export function EventCountdown({ endTime }: { endTime: string }) {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    setNow(Date.now());
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  if (now === null) {
    return <span className="font-num text-base font-bold tabular-nums">--:--:--</span>;
  }

  const cd = computeCountdown(endTime, now);
  const urgent = !cd.ended && cd.totalMs < 60_000;

  return (
    <span
      className={`font-num text-base font-bold tabular-nums ${urgent ? "text-danger" : ""}`}
    >
      {formatCountdown(cd)}
    </span>
  );
}
```

- [ ] **Step 2: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/events/_components/EventCountdown.tsx
git commit -m "feat(frontend): add EventCountdown client tick component"
```

---

## Task 8: Rotation card + section

**Files:**
- Create: `app/events/_components/RotationCard.tsx`
- Create: `app/events/_components/RotationSection.tsx`

- [ ] **Step 1: Implement RotationCard**

```tsx
"use client";

import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { RotationEvent } from "@/domain/event/types";
import { EventCountdown } from "./EventCountdown";

export function RotationCard({ rotation }: { rotation: RotationEvent }) {
  const { event, endTime } = rotation;
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);

  return (
    <Link href={`/events/${event.id}`} className="block">
      <Card className="flex h-full flex-col overflow-hidden transition hover:shadow-md">
        <div className="flex items-center gap-3 p-4">
          {event.mode?.iconImageUrl ? (
            <Image
              src={event.mode.iconImageUrl}
              alt={modeName}
              width={48}
              height={48}
              className="rounded-md"
              unoptimized
            />
          ) : (
            <div
              aria-hidden
              className="flex h-12 w-12 items-center justify-center rounded-md bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-2xl font-bold text-white"
            >
              {initial}
            </div>
          )}
          <div className="min-w-0 flex-1">
            <div className="text-[11px] font-bold uppercase tracking-[0.08em] text-muted-foreground">
              {modeName}
            </div>
            <div className="truncate text-base font-bold">{event.mapName}</div>
          </div>
        </div>
        <div className="flex items-center justify-between border-t bg-muted/40 px-4 py-2">
          <span className="text-[10px] font-bold uppercase tracking-[0.06em] text-muted-foreground">
            남은 시간
          </span>
          <EventCountdown endTime={endTime} />
        </div>
      </Card>
    </Link>
  );
}
```

- [ ] **Step 2: Implement RotationSection**

```tsx
import type { RotationEvent } from "@/domain/event/types";
import { RotationCard } from "./RotationCard";

export function RotationSection({ rotation }: { rotation: RotationEvent[] }) {
  if (rotation.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        진행 중인 이벤트가 없어요.
      </p>
    );
  }
  return (
    <section className="space-y-3">
      <header className="flex items-baseline gap-3">
        <span className="rounded bg-danger px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.1em] text-white">
          LIVE
        </span>
        <h2 className="font-display text-xl font-bold tracking-[0.02em]">
          현재 진행 중
        </h2>
        <span className="text-xs text-muted-foreground">{rotation.length}개</span>
      </header>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {rotation.map((r) => (
          <RotationCard key={r.event.id} rotation={r} />
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 3: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/events/_components/RotationCard.tsx app/events/_components/RotationSection.tsx
git commit -m "feat(frontend): rotation card + section with countdown"
```

---

## Task 9: Solo-rank card + section

**Files:**
- Create: `app/events/_components/SoloRankCard.tsx`
- Create: `app/events/_components/SoloRankSection.tsx`

- [ ] **Step 1: Implement SoloRankCard**

```tsx
"use client";

import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { BattleEvent } from "@/domain/event/types";

export function SoloRankCard({ event }: { event: BattleEvent }) {
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);

  return (
    <Link href={`/events/${event.id}`} className="block">
      <Card className="flex items-center gap-3 p-3 transition hover:shadow-md">
        {event.mode?.iconImageUrl ? (
          <Image
            src={event.mode.iconImageUrl}
            alt={modeName}
            width={40}
            height={40}
            className="rounded-md"
            unoptimized
          />
        ) : (
          <div
            aria-hidden
            className="flex h-10 w-10 items-center justify-center rounded-md bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-lg font-bold text-white"
          >
            {initial}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <div className="text-[10px] font-bold uppercase tracking-[0.08em] text-muted-foreground">
            {modeName}
          </div>
          <div className="truncate text-sm font-bold">{event.mapName}</div>
        </div>
      </Card>
    </Link>
  );
}
```

- [ ] **Step 2: Implement SoloRankSection**

```tsx
import type { BattleEvent } from "@/domain/event/types";
import { SoloRankCard } from "./SoloRankCard";

export function SoloRankSection({ events }: { events: BattleEvent[] }) {
  if (events.length === 0) return null;
  return (
    <section className="space-y-3">
      <header className="flex items-baseline gap-3">
        <span className="rounded bg-amber-500 px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.1em] text-white">
          RANKED
        </span>
        <h2 className="font-display text-xl font-bold tracking-[0.02em]">경쟁전 맵</h2>
        <span className="text-xs text-muted-foreground">{events.length}개</span>
      </header>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {events.map((e) => (
          <SoloRankCard key={e.id} event={e} />
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 3: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/events/_components/SoloRankCard.tsx app/events/_components/SoloRankSection.tsx
git commit -m "feat(frontend): solo-rank card + section"
```

---

## Task 10: Search input + results

**Files:**
- Create: `app/events/_components/EventSearchResults.tsx`
- Create: `app/events/_components/EventSearch.tsx`

- [ ] **Step 1: Implement EventSearchResults**

```tsx
"use client";

import type { BattleEvent } from "@/domain/event/types";
import { SoloRankCard } from "./SoloRankCard";

interface Props {
  results: BattleEvent[] | undefined;
  loading: boolean;
}

export function EventSearchResults({ results, loading }: Props) {
  if (loading && !results) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">검색 중…</p>
    );
  }
  if (!results || results.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        결과 없음
      </p>
    );
  }
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
      {results.map((e) => (
        <SoloRankCard key={e.id} event={e} />
      ))}
    </div>
  );
}
```

- [ ] **Step 2: Implement EventSearch**

```tsx
"use client";

import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useEventSearch } from "@/hooks/queries/useEventSearch";
import { EventSearchResults } from "./EventSearchResults";

interface Props {
  children: React.ReactNode;  // default content shown when search is empty
}

export function EventSearch({ children }: Props) {
  const [text, setText] = useState("");
  const [debounced, setDebounced] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setDebounced(text), 300);
    return () => clearTimeout(t);
  }, [text]);

  const trimmed = debounced.trim();
  const { data, isPending } = useEventSearch(debounced);
  const active = trimmed.length > 0;

  return (
    <div className="space-y-4">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="맵 이름 검색"
          className="pl-9"
          aria-label="맵 이름 검색"
        />
      </div>
      {active ? <EventSearchResults results={data} loading={isPending} /> : children}
    </div>
  );
}
```

- [ ] **Step 3: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/events/_components/EventSearchResults.tsx app/events/_components/EventSearch.tsx
git commit -m "feat(frontend): event search input with debounced backend search"
```

---

## Task 11: List page (RSC) + EventsListPage

**Files:**
- Create: `app/events/_components/EventsListPage.tsx`
- Modify: `app/events/page.tsx`

- [ ] **Step 1: Implement EventsListPage**

Create `app/events/_components/EventsListPage.tsx`:

```tsx
"use client";

import type { BattleEvent, RotationEvent } from "@/domain/event/types";
import { useRotationEvents } from "@/hooks/queries/useRotationEvents";
import { useSoloRankEvents } from "@/hooks/queries/useSoloRankEvents";
import { EventSearch } from "./EventSearch";
import { RotationSection } from "./RotationSection";
import { SoloRankSection } from "./SoloRankSection";

interface Props {
  initialRotation: RotationEvent[];
  initialSoloRank: BattleEvent[];
}

export default function EventsListPage({ initialRotation, initialSoloRank }: Props) {
  const { data: rotation = initialRotation } = useRotationEvents({
    initialData: initialRotation,
  });
  const { data: soloRank = initialSoloRank } = useSoloRankEvents({
    initialData: initialSoloRank,
  });

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6 space-y-6">
      <header>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          EVENTS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">이벤트 로테이션</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          현재 진행 중인 모드와 경쟁전 맵을 확인하세요.
        </p>
      </header>

      <EventSearch>
        <div className="space-y-6">
          <RotationSection rotation={rotation} />
          <SoloRankSection events={soloRank} />
        </div>
      </EventSearch>
    </main>
  );
}
```

- [ ] **Step 2: Replace `app/events/page.tsx`**

```tsx
import type { Metadata } from "next";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent, toRotationEvent } from "@/domain/event/mappers";
import { buildMetadata } from "@/lib/seo";
import EventsListPage from "./_components/EventsListPage";

export const metadata: Metadata = buildMetadata({
  title: "이벤트 로테이션 | ImStarGG",
  description:
    "현재 진행 중인 이벤트 로테이션과 경쟁전 맵 정보를 확인하세요.",
  path: "/events",
});

export const revalidate = 600;

export default async function Page() {
  const [rotationDto, soloRankDto] = await Promise.all([
    eventApi.rotation({ next: { revalidate: 600 } }),
    eventApi.soloRank({ next: { revalidate: 3600 } }),
  ]);
  const initialRotation = rotationDto.content.map(toRotationEvent);
  const initialSoloRank = soloRankDto.content.map(toBattleEvent);
  return (
    <EventsListPage
      initialRotation={initialRotation}
      initialSoloRank={initialSoloRank}
    />
  );
}
```

- [ ] **Step 3: Verify typecheck + build**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/events/_components/EventsListPage.tsx app/events/page.tsx
git commit -m "feat(frontend): real events list RSC + EventsListPage"
```

---

## Task 12: Detail Hero + Content

**Files:**
- Create: `app/events/[id]/_components/EventHero.tsx`
- Create: `app/events/[id]/_components/EventContent.tsx`

- [ ] **Step 1: Implement EventHero**

```tsx
import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import type { BattleEvent } from "@/domain/event/types";

const RESULT_TYPE_LABEL: Record<"RESULT" | "RANK", string> = {
  RESULT: "결과형 (승/패)",
  RANK: "순위형",
};

export function EventHero({ event }: { event: BattleEvent }) {
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);
  return (
    <section className="flex flex-col items-start gap-4 sm:flex-row sm:items-center">
      {event.mode?.iconImageUrl ? (
        <Image
          src={event.mode.iconImageUrl}
          alt={modeName}
          width={96}
          height={96}
          className="rounded-lg"
          unoptimized
        />
      ) : (
        <div
          aria-hidden
          className="flex h-24 w-24 items-center justify-center rounded-lg bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-4xl font-bold text-white"
        >
          {initial}
        </div>
      )}
      <div className="min-w-0 flex-1">
        <div className="text-xs font-bold uppercase tracking-[0.1em] text-muted-foreground">
          {modeName}
        </div>
        <h1 className="mt-1 font-display text-3xl font-bold tracking-[0.02em]">
          {event.mapName}
        </h1>
        {event.mode && (
          <div className="mt-2 flex flex-wrap gap-2">
            <Badge variant="outline">{RESULT_TYPE_LABEL[event.mode.resultType]}</Badge>
          </div>
        )}
      </div>
    </section>
  );
}
```

- [ ] **Step 2: Implement EventContent**

```tsx
"use client";

import type { BattleEvent } from "@/domain/event/types";
import { useEvent } from "@/hooks/queries/useEvent";
import { EventHero } from "./EventHero";

export function EventContent({ initialEvent }: { initialEvent: BattleEvent }) {
  const { data: event = initialEvent } = useEvent(initialEvent.id, {
    initialData: initialEvent,
  });
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6 space-y-5">
      <EventHero event={event} />
    </main>
  );
}
```

- [ ] **Step 3: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/events/[id]/_components/EventHero.tsx app/events/[id]/_components/EventContent.tsx
git commit -m "feat(frontend): event detail hero + content (header only)"
```

---

## Task 13: Detail page (RSC) + error/not-found

**Files:**
- Modify: `app/events/[id]/page.tsx`
- Create: `app/events/[id]/error.tsx`
- Create: `app/events/[id]/not-found.tsx`

- [ ] **Step 1: Replace `app/events/[id]/page.tsx`**

```tsx
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { EventContent } from "./_components/EventContent";

type Params = { params: Promise<{ id: string }> };

export const revalidate = 3600;

export async function generateStaticParams() {
  const [rotation, soloRank] = await Promise.all([
    eventApi.rotation({ next: { revalidate: 3600 } }),
    eventApi.soloRank({ next: { revalidate: 3600 } }),
  ]);
  const ids = new Set<string>();
  for (const r of rotation.content) ids.add(String(r.event.id));
  for (const e of soloRank.content) ids.add(String(e.id));
  return [...ids].map((id) => ({ id }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await eventApi.get(id, { next: { revalidate: 3600 } });
    const event = toBattleEvent(dto);
    const modeName = event.mode?.name ?? "이벤트";
    const resultLabel = event.mode?.resultType === "RANK" ? "순위형" : "결과형";
    return buildMetadata({
      title: `${event.mapName} (${modeName}) | ImStarGG`,
      description: `모드: ${modeName} · 결과 타입: ${resultLabel}`,
      path: `/events/${id}`,
      image: event.mode?.iconImageUrl ?? undefined,
    });
  } catch {
    return buildMetadata({
      title: "이벤트를 찾을 수 없음 | ImStarGG",
      description: "요청하신 이벤트를 찾을 수 없습니다.",
      path: `/events/${id}`,
      noindex: true,
    });
  }
}

export default async function EventPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await eventApi.get(id, { next: { revalidate: 3600 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return <EventContent initialEvent={toBattleEvent(dto)} />;
}
```

- [ ] **Step 2: Create error.tsx**

`app/events/[id]/error.tsx`:

```tsx
"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function EventError({
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

- [ ] **Step 3: Create not-found.tsx**

`app/events/[id]/not-found.tsx`:

```tsx
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function EventNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">이벤트를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">존재하지 않는 ID일 수 있어요.</p>
      <Button asChild variant="outline">
        <Link href="/events">이벤트 목록</Link>
      </Button>
    </main>
  );
}
```

- [ ] **Step 4: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/events/[id]/page.tsx app/events/[id]/error.tsx app/events/[id]/not-found.tsx
git commit -m "feat(frontend): event detail RSC + error/not-found"
```

---

## Task 14: Sitemap

**Files:**
- Modify: `app/sitemap.ts`

- [ ] **Step 1: Add event entries**

Replace contents of `app/sitemap.ts` with:

```tsx
import type { MetadataRoute } from "next";
import { env } from "@/config/env";
import { brawlerApi } from "@/api/endpoints/brawler";
import { eventApi } from "@/api/endpoints/event";

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

  let eventEntries: MetadataRoute.Sitemap = [];
  try {
    const [rotation, soloRank] = await Promise.all([
      eventApi.rotation({ next: { revalidate: 3600 } }),
      eventApi.soloRank({ next: { revalidate: 3600 } }),
    ]);
    const ids = new Set<number>();
    for (const r of rotation.content) ids.add(r.event.id);
    for (const e of soloRank.content) ids.add(e.id);
    eventEntries = [...ids].map((id) => ({
      url: `${base}/events/${id}`,
      lastModified: now,
      changeFrequency: "daily",
      priority: 0.5,
    }));
  } catch {
    eventEntries = [];
  }

  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
    ...brawlerEntries,
    ...eventEntries,
  ];
}
```

- [ ] **Step 2: Verify typecheck**

Run: `pnpm typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/sitemap.ts
git commit -m "feat(frontend): include event detail URLs in sitemap"
```

---

## Task 15: Remove event mocks

**Files:**
- Delete: `app/events/_components/EventsPage.tsx`
- Delete: `app/events/[id]/_components/EventDetailPage.tsx`
- Modify: `src/lib/data.ts`

- [ ] **Step 1: Delete mock components**

```bash
rm app/events/_components/EventsPage.tsx
rm app/events/[id]/_components/EventDetailPage.tsx
```

- [ ] **Step 2: Identify mock symbols to remove from `src/lib/data.ts`**

Open `src/lib/data.ts` and remove these exports (and their helpers if no other consumer remains):
- `RotationItem` interface and `ROTATION` constant
- `RankedMap` interface and `SOLO_RANKED_MAPS` constant
- `ResolvedEvent` interface and `getEvent` function
- `EventBrawlerStat` interface and `genEventBrawlerStats` function

Leave any non-event symbols (e.g., `BRAWLERS`, `BRAWLER_META`, `EVENTS`, `EventInfo`, `PairStat`) untouched if they are referenced elsewhere. Verify with grep before removing each symbol:

```bash
pnpm exec rg --no-heading -n 'EventInfo|EVENTS\b' app src
```

If `EventInfo` and `EVENTS` are only referenced by the deleted symbols and the deleted mock components, remove them too. Otherwise keep them.

- [ ] **Step 3: Verify nothing references removed symbols**

```bash
pnpm exec rg --no-heading -n 'ROTATION\b|SOLO_RANKED_MAPS|getEvent\b|genEventBrawlerStats|EventBrawlerStat|RotationItem|RankedMap|ResolvedEvent' app src tests
```

Expected: no matches.

- [ ] **Step 4: Run typecheck and tests**

Run: `pnpm typecheck && pnpm lint && pnpm test:run`
Expected: PASS

- [ ] **Step 5: Commit**

Stage only the files actually touched (deletions + `src/lib/data.ts`):

```bash
git add -u app/events/_components/EventsPage.tsx app/events/[id]/_components/EventDetailPage.tsx src/lib/data.ts
git commit -m "chore(frontend): remove legacy event mock data and components"
```

---

## Task 16: E2E spec

**Files:**
- Create: `tests/e2e/event.spec.ts`

- [ ] **Step 1: Write E2E test**

```typescript
import { test, expect } from "@playwright/test";

test("이벤트 목록은 카드를 렌더한다", async ({ page }) => {
  await page.goto("/events");
  const cards = page.locator('a[href^="/events/"]');
  await expect(cards.first()).toBeVisible({ timeout: 15_000 });
  expect(await cards.count()).toBeGreaterThanOrEqual(1);
});

test("rotation 카드는 카운트다운을 표시한다", async ({ page }) => {
  await page.goto("/events");
  const card = page.locator('a[href^="/events/"]').first();
  await card.waitFor({ state: "visible", timeout: 15_000 });
  // HH:MM:SS 형태 또는 종료됨
  await expect(card).toContainText(/\d{2}:\d{2}:\d{2}|종료됨/, { timeout: 5_000 });
});

test("맵 이름 검색은 결과를 표시한다", async ({ page }) => {
  await page.goto("/events");
  const cardSelector = 'a[href^="/events/"]';
  await page.locator(cardSelector).first().waitFor({ state: "visible", timeout: 15_000 });

  // 첫 카드의 맵 이름을 추출해 그 안의 한 토큰으로 검색한다
  const firstCardText = (await page.locator(cardSelector).first().innerText()).trim();
  // 한글/영문 토큰 추출 (공백 분리, 길이 ≥2)
  const token = firstCardText
    .split(/\s+/)
    .map((s) => s.replace(/[^\p{L}\p{N}]/gu, ""))
    .find((s) => s.length >= 2);
  test.skip(!token, "맵 이름에서 검색 토큰을 찾지 못했어요");

  const input = page.getByLabel("맵 이름 검색");
  await input.fill(token!);
  await expect(page.locator(cardSelector).first()).toBeVisible({ timeout: 5_000 });
});

test("검색을 비우면 메인 섹션으로 돌아간다", async ({ page }) => {
  await page.goto("/events");
  const input = page.getByLabel("맵 이름 검색");
  await input.fill("zzz_no_match_zzz");
  await expect(page.getByText("결과 없음")).toBeVisible({ timeout: 5_000 });
  await input.fill("");
  await expect(page.getByRole("heading", { name: "현재 진행 중" })).toBeVisible({
    timeout: 5_000,
  });
});

test("카드 클릭 시 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/events");
  const firstCard = page.locator('a[href^="/events/"]').first();
  await firstCard.waitFor({ state: "visible", timeout: 15_000 });
  await firstCard.click();
  await page.waitForURL(/\/events\/\d+/, { timeout: 15_000 });
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
});

test("상세 페이지 SEO: title + canonical 1개", async ({ page }) => {
  // 목록에서 첫 이벤트 ID 추출
  await page.goto("/events");
  const firstCard = page.locator('a[href^="/events/"]').first();
  await firstCard.waitFor({ state: "visible", timeout: 15_000 });
  const href = await firstCard.getAttribute("href");
  test.skip(!href, "이벤트 카드 href를 찾지 못했어요");

  await page.goto(href!);
  await expect(page).toHaveTitle(/ImStarGG/, { timeout: 15_000 });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});
```

- [ ] **Step 2: Commit (do not run E2E here — runs in CI/manual against prod)**

```bash
git add tests/e2e/event.spec.ts
git commit -m "test(frontend): e2e event list/search/detail/SEO"
```

---

## Task 17: Final verification

- [ ] **Step 1: Full check from `imstargg-frontend/`**

```bash
pnpm typecheck && pnpm lint && pnpm test:run && pnpm build
```

Expected: all PASS. The `build` step exercises `generateStaticParams` for `/events/[id]` and the dynamic sitemap, hitting prod API the same way the brawler domain already does.

- [ ] **Step 2: Manual smoke (optional but recommended)**

```bash
pnpm dev
```

In a browser:
- `/events` shows rotation grid with 1s-ticking countdowns and solo-rank list
- Type a partial map name → results appear; clear → main sections return
- Click a card → detail header (mode icon, mode name, map name, result type badge) renders
- View page source: `<link rel="canonical">` and `<title>` correct on both list and detail

- [ ] **Step 3: No commit needed if everything passed**

If a final fix was required, commit it as `fix(frontend): <what>`.

---

## Notes / Risks

- **Build-time backend dependency:** `generateStaticParams` and `sitemap.ts` call `/v1/events/rotation` + `/v1/events/solo-rank` at build time. CI build environment must reach prod API (same condition as brawler domain).
- **Hydration safety:** `EventCountdown` returns `--:--:--` until `useEffect` populates `now`, avoiding SSR/CSR mismatch.
- **Search scope:** Backend `searchByMapName` only matches `MessageType.BATTLE_EVENT_NAME` (map name), case-insensitive substring. Mode names will not match — placeholder/aria-label communicate this.
- **`git add -A` is forbidden** (prior club domain incident leaked unrelated backend dirty files into a frontend commit). Always stage explicit paths.
- **`EventInfo` / `EVENTS` cleanup:** Task 15 instructs to verify no other consumer exists before removing. Keep them if home/ranking pages still reference them; this plan does not preemptively delete them.
