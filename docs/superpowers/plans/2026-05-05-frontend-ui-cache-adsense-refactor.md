# Frontend UI, Cache, AdSense, Brawler Items Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `imstargg-frontend` so mobile navigation uses a hamburger drawer, high-freshness pages avoid stale frontend caches, AdSense is loaded safely in production, and brawler detail item cards show ownership rates inline.

**Architecture:** Keep the existing Next.js App Router + RSC + TanStack Query shape. Centralize cache durations in `src/config/cache.ts`, split `TopNav` into focused local components, install one production-only AdSense script component in the root layout, and move brawler item ownership matching into a pure domain helper consumed by the brawler detail UI.

**Tech Stack:** Next.js 15 App Router, React 19, TypeScript, TanStack Query, shadcn/Radix Sheet, Vitest, Testing Library, Playwright/browser visual verification.

---

## File Structure

Create:

- `imstargg-frontend/src/config/cache.ts` - named frontend cache durations in seconds and query stale times in ms.
- `imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx` - production-only AdSense script wrapper and small pure predicate.
- `imstargg-frontend/src/domain/brawler/itemOwnership.ts` - pure display model builder for brawler detail item cards and ownership rates.
- `imstargg-frontend/tests/unit/config/cache.test.ts` - verifies cache constants match the approved policy.
- `imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx` - verifies the AdSense loading predicate.
- `imstargg-frontend/tests/unit/domain/brawler/itemOwnership.test.ts` - verifies item ownership matching by `itemId` and `buffyType`.
- `imstargg-frontend/tests/components/TopNav.test.tsx` - replaces the removed `MobileTabBar` test with mobile drawer behavior coverage.

Modify:

- `.gitignore` - add `.superpowers/` so brainstorming artifacts stay local.
- `imstargg-frontend/app/layout.tsx` - remove `MobileTabBar`, add `GoogleAdsenseScript`.
- `imstargg-frontend/app/styles/responsive.css` - remove bottom tab bar styles and compact mobile top nav rules.
- `imstargg-frontend/src/config/env.ts` - validate `NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT`.
- `imstargg-frontend/.env.example` - document `NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT`.
- `imstargg-frontend/vitest.config.ts` - provide a test value for the new public env.
- `imstargg-frontend/src/components/layout/TopNav.tsx` - split logic into small in-file or sibling components and add mobile Sheet drawer.
- `imstargg-frontend/src/hooks/queries/useBrawler.ts` - use cache stale time constant.
- `imstargg-frontend/src/hooks/queries/useBrawlers.ts` - use cache stale time constant.
- `imstargg-frontend/src/hooks/queries/useEvent.ts` - shorten event stale time.
- `imstargg-frontend/src/hooks/queries/useSoloRankEvents.ts` - shorten stale time.
- `imstargg-frontend/src/hooks/queries/useRotationEvents.ts` - keep end-time refetch, use short stale constant.
- `imstargg-frontend/app/brawlers/page.tsx` - use `CACHE_SECONDS.BRAWLER_REFERENCE` for reference data and short cache for stats.
- `imstargg-frontend/app/brawlers/[id]/page.tsx` - fetch item ownership at page level and pass it to item UI.
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx` - accept ownership/trophy props and render integrated items.
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx` - replace separate category cards with ownership-aware rows.
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx` - show icon, name, category, ownership badge/bar.
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx` - stop fetching/rendering item ownership.
- `imstargg-frontend/app/events/page.tsx` - use `cache: "no-store"` for rotation.
- `imstargg-frontend/app/events/[id]/page.tsx` - use `cache: "no-store"` for event detail.
- `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx` - use short revalidate for stats.
- `imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx` - use short revalidate for selected brawler stats.
- `imstargg-frontend/app/ranking/players/page.tsx` - use `cache: "no-store"`.
- `imstargg-frontend/app/ranking/clubs/page.tsx` - use `cache: "no-store"`.
- `imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx` - use `cache: "no-store"` for ranking fetch, keep brawler reference cached.

Delete if unused after imports are removed:

- `imstargg-frontend/src/components/layout/MobileTabBar.tsx`
- `imstargg-frontend/tests/components/MobileTabBar.test.tsx`
- `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx`
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx`

---

## Task 1: Cache Constants and Freshness Policy

**Files:**

- Create: `imstargg-frontend/src/config/cache.ts`
- Create: `imstargg-frontend/tests/unit/config/cache.test.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useBrawler.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useBrawlers.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useEvent.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useSoloRankEvents.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useRotationEvents.ts`
- Modify: `imstargg-frontend/app/brawlers/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`
- Modify: `imstargg-frontend/app/events/page.tsx`
- Modify: `imstargg-frontend/app/events/[id]/page.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`
- Modify: `imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx`
- Modify: `imstargg-frontend/app/ranking/players/page.tsx`
- Modify: `imstargg-frontend/app/ranking/clubs/page.tsx`
- Modify: `imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx`

- [ ] **Step 1: Write the cache constants test**

Create `imstargg-frontend/tests/unit/config/cache.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { CACHE_SECONDS, QUERY_STALE_TIME } from "@/config/cache";

describe("frontend cache policy", () => {
  it("keeps high-freshness cache windows short", () => {
    expect(CACHE_SECONDS.SHORT).toBe(60);
    expect(QUERY_STALE_TIME.SHORT).toBe(60_000);
  });

  it("keeps brawler reference data under one hour on primary pages", () => {
    expect(CACHE_SECONDS.BRAWLER_REFERENCE).toBe(3600);
    expect(QUERY_STALE_TIME.BRAWLER_REFERENCE).toBe(60 * 60_000);
  });

  it("allows long cache only for SEO helper paths", () => {
    expect(CACHE_SECONDS.SEO_REFERENCE).toBe(86400);
  });
});
```

- [ ] **Step 2: Run the cache constants test and verify it fails**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/config/cache.test.ts
```

Expected: FAIL because `@/config/cache` does not exist.

- [ ] **Step 3: Add cache constants**

Create `imstargg-frontend/src/config/cache.ts`:

```ts
export const CACHE_SECONDS = {
  SHORT: 60,
  BRAWLER_REFERENCE: 3600,
  SEO_REFERENCE: 86400,
} as const;

export const QUERY_STALE_TIME = {
  SHORT: 60_000,
  BRAWLER_REFERENCE: 60 * 60_000,
} as const;
```

- [ ] **Step 4: Run the cache constants test and verify it passes**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/config/cache.test.ts
```

Expected: PASS.

- [ ] **Step 5: Update TanStack Query stale times**

In `imstargg-frontend/src/hooks/queries/useBrawler.ts`, add:

```ts
import { QUERY_STALE_TIME } from "@/config/cache";
```

Replace:

```ts
staleTime: 60 * 60_000,
```

with:

```ts
staleTime: QUERY_STALE_TIME.BRAWLER_REFERENCE,
```

In `imstargg-frontend/src/hooks/queries/useBrawlers.ts`, add the same import and replace:

```ts
staleTime: 60 * 60_000,
```

with:

```ts
staleTime: QUERY_STALE_TIME.BRAWLER_REFERENCE,
```

In `imstargg-frontend/src/hooks/queries/useEvent.ts`, add the same import and replace:

```ts
staleTime: 60 * 60_000,
```

with:

```ts
staleTime: QUERY_STALE_TIME.SHORT,
```

In `imstargg-frontend/src/hooks/queries/useSoloRankEvents.ts`, add the same import and replace:

```ts
staleTime: 60 * 60_000,
```

with:

```ts
staleTime: QUERY_STALE_TIME.SHORT,
```

In `imstargg-frontend/src/hooks/queries/useRotationEvents.ts`, add the import and replace:

```ts
staleTime: 30_000,
```

with:

```ts
staleTime: QUERY_STALE_TIME.SHORT,
```

- [ ] **Step 6: Update brawler page fetch policies**

In `imstargg-frontend/app/brawlers/page.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace:

```ts
export const revalidate = 600;
```

with:

```ts
// Primary brawler list can cache reference data, but statistics use a shorter fetch cache.
export const revalidate = 60;
```

Replace:

```ts
brawlerApi.list({ next: { revalidate: 86400 } }),
```

with:

```ts
brawlerApi.list({ next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE } }),
```

Replace statistics fetch options:

```ts
{ next: { revalidate: 600 } },
```

with:

```ts
{ next: { revalidate: CACHE_SECONDS.SHORT } },
```

In `imstargg-frontend/app/brawlers/[id]/page.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace:

```ts
export const revalidate = 600;
```

with:

```ts
export const revalidate = CACHE_SECONDS.SHORT;
```

Replace brawler reference fetches:

```ts
brawlerApi.list({ next: { revalidate: 86400 } });
brawlerApi.get(id, { next: { revalidate: 86400 } });
```

with:

```ts
brawlerApi.list({ next: { revalidate: CACHE_SECONDS.SEO_REFERENCE } });
brawlerApi.get(id, { next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE } });
```

Use `CACHE_SECONDS.SEO_REFERENCE` only in `generateStaticParams`.

- [ ] **Step 7: Update brawler stats fetch policies**

In `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace each statistics fetch option:

```ts
{ next: { revalidate: 600 } },
```

with:

```ts
{ next: { revalidate: CACHE_SECONDS.SHORT } },
```

In this task, leave `itemOwnership` in place; it moves in Task 4.

- [ ] **Step 8: Update event fetch policies**

In `imstargg-frontend/app/events/page.tsx`, replace:

```ts
export const revalidate = 600;
```

with:

```ts
export const dynamic = "force-dynamic";
```

Replace:

```ts
eventApi.rotation({ next: { revalidate: 600 } }),
```

with:

```ts
eventApi.rotation({ cache: "no-store" }),
```

Replace:

```ts
eventApi.soloRank({ next: { revalidate: 3600 } }),
```

with:

```ts
eventApi.soloRank({ next: { revalidate: CACHE_SECONDS.SHORT } }),
```

and add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

In `imstargg-frontend/app/events/[id]/page.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace:

```ts
export const revalidate = 3600;
```

with:

```ts
export const dynamic = "force-dynamic";
```

Keep `generateStaticParams` using `CACHE_SECONDS.SEO_REFERENCE` for rotation/solo rank discovery:

```ts
eventApi.rotation({ next: { revalidate: CACHE_SECONDS.SEO_REFERENCE } }),
eventApi.soloRank({ next: { revalidate: CACHE_SECONDS.SEO_REFERENCE } }),
```

Replace event detail fetches in `generateMetadata` and page render:

```ts
eventApi.get(id, { next: { revalidate: 3600 } });
```

with:

```ts
eventApi.get(id, { cache: "no-store" });
```

- [ ] **Step 9: Update event stats fetch policies**

In `imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace each:

```ts
{ next: { revalidate: 600 } },
```

with:

```ts
{ next: { revalidate: CACHE_SECONDS.SHORT } },
```

In `imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx`, add the same import and replace every `revalidate: 600` with `revalidate: CACHE_SECONDS.SHORT`.

- [ ] **Step 10: Update ranking pages to no-store**

In `imstargg-frontend/app/ranking/players/page.tsx`, replace:

```ts
export const revalidate = 600;
```

with:

```ts
export const dynamic = "force-dynamic";
```

Replace:

```ts
const dto = await rankingApi.players(country, {
  next: { revalidate: 600 },
});
```

with:

```ts
const dto = await rankingApi.players(country, { cache: "no-store" });
```

In `imstargg-frontend/app/ranking/clubs/page.tsx`, make the same `dynamic = "force-dynamic"` change and replace:

```ts
const dto = await rankingApi.clubs(country, { next: { revalidate: 600 } });
```

with:

```ts
const dto = await rankingApi.clubs(country, { cache: "no-store" });
```

In `imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx`, add:

```ts
import { CACHE_SECONDS } from "@/config/cache";
```

Replace:

```ts
export const revalidate = 600;
```

with:

```ts
export const dynamic = "force-dynamic";
```

Replace brawler reference fetch:

```ts
next: { revalidate: 86400 },
```

with:

```ts
next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE },
```

Replace ranking fetch:

```ts
const dto = await rankingApi.brawlers(brawler.id, country, {
  next: { revalidate: 600 },
});
```

with:

```ts
const dto = await rankingApi.brawlers(brawler.id, country, { cache: "no-store" });
```

- [ ] **Step 11: Run cache tests and typecheck**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/config/cache.test.ts
pnpm typecheck
```

Expected: both PASS.

- [ ] **Step 12: Commit cache policy changes**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/config/cache.ts \
  imstargg-frontend/tests/unit/config/cache.test.ts \
  imstargg-frontend/src/hooks/queries/useBrawler.ts \
  imstargg-frontend/src/hooks/queries/useBrawlers.ts \
  imstargg-frontend/src/hooks/queries/useEvent.ts \
  imstargg-frontend/src/hooks/queries/useSoloRankEvents.ts \
  imstargg-frontend/src/hooks/queries/useRotationEvents.ts \
  imstargg-frontend/app/brawlers/page.tsx \
  'imstargg-frontend/app/brawlers/[id]/page.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx' \
  imstargg-frontend/app/events/page.tsx \
  'imstargg-frontend/app/events/[id]/page.tsx' \
  'imstargg-frontend/app/events/[id]/_components/EventStatsSection.tsx' \
  'imstargg-frontend/app/events/[id]/_components/BrawlerSelectedPanel.tsx' \
  imstargg-frontend/app/ranking/players/page.tsx \
  imstargg-frontend/app/ranking/clubs/page.tsx \
  'imstargg-frontend/app/ranking/brawlers/[brawlerId]/page.tsx'
git commit -m "fix(frontend): align frontend cache freshness policy"
```

Expected: commit succeeds.

---

## Task 2: Mobile Hamburger Navigation

**Files:**

- Create: `imstargg-frontend/tests/components/TopNav.test.tsx`
- Modify: `.gitignore`
- Modify: `imstargg-frontend/app/layout.tsx`
- Modify: `imstargg-frontend/app/styles/responsive.css`
- Modify: `imstargg-frontend/src/components/layout/TopNav.tsx`
- Delete: `imstargg-frontend/src/components/layout/MobileTabBar.tsx`
- Delete: `imstargg-frontend/tests/components/MobileTabBar.test.tsx`

- [ ] **Step 1: Write the TopNav drawer behavior test**

Create `imstargg-frontend/tests/components/TopNav.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TopNav from "@/components/layout/TopNav";

const push = vi.fn();
let pathname = "/";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  usePathname: () => pathname,
}));

describe("TopNav", () => {
  beforeEach(() => {
    push.mockReset();
    pathname = "/";
    window.localStorage.clear();
  });

  it("opens mobile menu drawer with search and primary links", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));

    expect(screen.getByRole("dialog", { name: "메뉴" })).toBeInTheDocument();
    expect(screen.getByPlaceholderText("#PLAYERTAG")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "브롤러 페이지로 이동" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "이벤트 페이지로 이동" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "랭킹 페이지로 이동" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "클럽 페이지로 이동" })).toBeInTheDocument();
  });

  it("submits drawer search and closes the drawer", async () => {
    const user = userEvent.setup();
    render(<TopNav />);

    await user.click(screen.getByRole("button", { name: "모바일 메뉴 열기" }));
    await user.type(screen.getByPlaceholderText("#PLAYERTAG"), "#88J8Q9RPV");
    await user.click(screen.getByRole("button", { name: "플레이어 검색" }));

    expect(push).toHaveBeenCalledWith("/players/88J8Q9RPV");
  });
});
```

- [ ] **Step 2: Run the TopNav test and verify it fails**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/components/TopNav.test.tsx
```

Expected: FAIL because the mobile menu button and drawer do not exist.

- [ ] **Step 3: Add `.superpowers/` to `.gitignore`**

Append this line to `/Users/chodyoungrae/Projects/imstargg-v2/.gitignore`:

```gitignore
.superpowers/
```

- [ ] **Step 4: Remove MobileTabBar from layout**

In `imstargg-frontend/app/layout.tsx`, remove:

```tsx
import MobileTabBar from "@/components/layout/MobileTabBar";
```

Remove:

```tsx
<MobileTabBar />
```

Keep:

```tsx
<TopNav />
{children}
<Toaster richColors position="top-center" />
```

- [ ] **Step 5: Replace TopNav with drawer-capable structure**

Rewrite `imstargg-frontend/src/components/layout/TopNav.tsx` so it keeps existing recent-search behavior while adding mobile drawer state. Use this structure as the implementation target:

```tsx
"use client";

import { Menu, Search, X } from "lucide-react";
import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import { useRouter, usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { formatMinutesAgo } from "@/lib/format";

const RECENT_KEY = "imstargg.recentSearches";
const RECENT_MAX = 8;

interface RecentSearch {
  tag: string;
  ts: number;
}

const NAV_ITEMS = [
  { label: "브롤러", href: "/brawlers" },
  { label: "이벤트", href: "/events" },
  { label: "랭킹", href: "/ranking" },
  { label: "클럽", href: "/clubs" },
] as const;

function loadRecents(): RecentSearch[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(RECENT_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}

function saveRecents(list: RecentSearch[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(list));
  } catch {}
}

function pushRecent(tag: string) {
  const cleaned = tag.trim().replace(/^#/, "").toUpperCase();
  if (!cleaned) return;
  const list = loadRecents().filter((r) => r.tag !== cleaned);
  list.unshift({ tag: cleaned, ts: Date.now() });
  saveRecents(list.slice(0, RECENT_MAX));
}

function removeRecent(tag: string) {
  saveRecents(loadRecents().filter((r) => r.tag !== tag));
}

function clearRecents() {
  saveRecents([]);
}

const recentsListeners = new Set<() => void>();
let recentsVersion = 0;

function notifyRecents() {
  recentsVersion++;
  recentsListeners.forEach((fn) => fn());
}

function subscribeRecentsStore(fn: () => void) {
  recentsListeners.add(fn);
  return () => {
    recentsListeners.delete(fn);
  };
}

function getRecentsSnapshot(): number {
  return recentsVersion;
}

function getRecentsServerSnapshot(): number {
  return 0;
}

const syncedPathnames = new Set<string>();

export default function TopNav() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  useSyncExternalStore(subscribeRecentsStore, getRecentsSnapshot, getRecentsServerSnapshot);
  const recents: RecentSearch[] = typeof window === "undefined" ? [] : loadRecents();
  const [now] = useState<number>(() => (typeof window === "undefined" ? 0 : Date.now()));

  if (
    pathname.startsWith("/players/") &&
    !syncedPathnames.has(pathname) &&
    typeof window !== "undefined"
  ) {
    syncedPathnames.add(pathname);
    const tag = decodeURIComponent(pathname.slice("/players/".length));
    pushRecent(tag);
    notifyRecents();
  }

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  const goToTag = useCallback(
    (tag: string) => {
      const cleaned = tag.trim().replace(/^#/, "");
      if (!cleaned) return;
      pushRecent(cleaned);
      notifyRecents();
      router.push(`/players/${encodeURIComponent(cleaned)}`);
      setSearch("");
      setOpen(false);
      setDrawerOpen(false);
    },
    [router],
  );

  const submit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    goToTag(search);
  };

  const navigate = (href: string) => {
    router.push(href);
    setDrawerOpen(false);
  };

  const isActive = (prefix: string) => pathname === prefix || pathname.startsWith(prefix + "/");
  const q = search.trim().replace(/^#/, "").toUpperCase();
  const filteredRecents = q ? recents.filter((r) => r.tag.includes(q)) : recents;

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <button
          type="button"
          className="topnav-logo"
          onClick={() => router.push("/")}
          title="ImStarGG"
        >
          <img src="/assets/logo.webp" alt="ImStarGG" />
        </button>

        <div className="topnav-links">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.href}
              type="button"
              className={`topnav-link ${isActive(item.href) ? "active" : ""}`}
              onClick={() => router.push(item.href)}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="topnav-spacer" />

        <TopNavSearch
          ref={wrapRef}
          search={search}
          setSearch={setSearch}
          open={open}
          setOpen={setOpen}
          submit={submit}
          filteredRecents={filteredRecents}
          goToTag={goToTag}
          now={now}
        />

        <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
          <SheetTrigger asChild>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="topnav-menu-button"
              aria-label="모바일 메뉴 열기"
            >
              <Menu className="size-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="right" className="w-[86vw] max-w-sm p-0" aria-label="메뉴">
            <SheetHeader className="border-b border-[#D8DCE5] p-4">
              <SheetTitle>메뉴</SheetTitle>
              <SheetDescription>검색과 주요 페이지 이동</SheetDescription>
            </SheetHeader>
            <div className="flex flex-col gap-4 p-4">
              <TopNavSearch
                search={search}
                setSearch={setSearch}
                open={open}
                setOpen={setOpen}
                submit={submit}
                filteredRecents={filteredRecents}
                goToTag={goToTag}
                now={now}
                mobile
              />
              <div className="grid gap-2">
                {NAV_ITEMS.map((item) => (
                  <Button
                    key={item.href}
                    type="button"
                    variant={isActive(item.href) ? "default" : "outline"}
                    className="justify-start"
                    aria-label={`${item.label} 페이지로 이동`}
                    onClick={() => navigate(item.href)}
                  >
                    {item.label}
                  </Button>
                ))}
              </div>
            </div>
            <SheetClose asChild>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute right-3 top-3"
                aria-label="메뉴 닫기"
              >
                <X className="size-4" />
              </Button>
            </SheetClose>
          </SheetContent>
        </Sheet>
      </div>
    </nav>
  );
}
```

In the same file, preserve the existing recent-search dropdown rendering in a `TopNavSearch` helper. The helper must be a `forwardRef` component because desktop outside-click handling attaches `wrapRef` to it:

```tsx
type TopNavSearchProps = {
  search: string;
  setSearch: (value: string) => void;
  open: boolean;
  setOpen: (value: boolean) => void;
  submit: (event: React.FormEvent<HTMLFormElement>) => void;
  filteredRecents: RecentSearch[];
  goToTag: (tag: string) => void;
  now: number;
  mobile?: boolean;
};

const TopNavSearch = forwardRef<HTMLDivElement, TopNavSearchProps>(function TopNavSearch(
  {
    search,
    setSearch,
    open,
    setOpen,
    submit,
    filteredRecents,
    goToTag,
    now,
    mobile = false,
  },
  ref,
) {
  const q = search.trim().replace(/^#/, "").toUpperCase();

  return (
    <div
      ref={ref}
      className={
        mobile
          ? "topnav-search-wrap topnav-search-wrap-drawer"
          : "topnav-search-wrap topnav-desktop-search"
      }
    >
      <form
        className={mobile ? "topnav-search topnav-search-drawer" : "topnav-search"}
        onSubmit={submit}
      >
        <Search className="size-4 text-[#8A90A0]" aria-hidden />
        <input
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          placeholder="#PLAYERTAG"
        />
        <button type="submit" aria-label="플레이어 검색" className="sr-only">
          검색
        </button>
      </form>

      {open && filteredRecents.length > 0 && (
        <div
          style={{
            position: "absolute",
            top: "calc(100% + 6px)",
            right: 0,
            width: mobile ? "100%" : 280,
            background: "#fff",
            border: "1px solid var(--color-neutral-200)",
            borderRadius: 10,
            boxShadow: "0 8px 28px rgba(20,24,32,0.12), 0 2px 6px rgba(20,24,32,0.06)",
            overflow: "hidden",
            zIndex: 100,
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              padding: "8px 12px",
              fontSize: 11,
              fontWeight: 700,
              color: "var(--color-neutral-500)",
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              borderBottom: "1px solid var(--color-neutral-100)",
              background: "var(--color-neutral-50)",
            }}
          >
            <span>최근 검색</span>
            <button
              type="button"
              onClick={() => {
                clearRecents();
                notifyRecents();
              }}
              style={{
                background: "transparent",
                border: "none",
                padding: 0,
                color: "var(--color-neutral-500)",
                fontSize: 11,
                fontWeight: 600,
                cursor: "pointer",
                letterSpacing: 0,
                textTransform: "none",
              }}
              title="전체 삭제"
            >
              전체 삭제
            </button>
          </div>
          {filteredRecents.map((r) => (
            <div
              key={r.tag}
              onClick={() => goToTag(r.tag)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "10px 12px",
                cursor: "pointer",
                borderBottom: "1px solid var(--color-neutral-100)",
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = "var(--color-neutral-50)")}
              onMouseLeave={(e) => (e.currentTarget.style.background = "#fff")}
            >
              <span style={{ color: "#8A90A0", fontSize: 14 }}>최근</span>
              <span
                style={{
                  flex: 1,
                  fontFamily: "var(--font-num)",
                  fontWeight: 700,
                  fontSize: 13,
                  color: "var(--color-neutral-800)",
                  letterSpacing: "0.02em",
                }}
              >
                #{r.tag}
              </span>
              <span style={{ fontSize: 11, color: "var(--color-neutral-400)" }}>
                {now ? formatMinutesAgo(Math.max(0, Math.floor((now - r.ts) / 60000))) : ""}
              </span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  removeRecent(r.tag);
                  notifyRecents();
                }}
                style={{
                  background: "transparent",
                  border: "none",
                  padding: 4,
                  color: "var(--color-neutral-400)",
                  cursor: "pointer",
                  lineHeight: 1,
                  borderRadius: 4,
                }}
                title="삭제"
                aria-label={`${r.tag} 최근 검색 삭제`}
              >
                <X className="size-3.5" aria-hidden />
              </button>
            </div>
          ))}
        </div>
      )}

      {open && filteredRecents.length === 0 && (
        <div
          style={{
            position: "absolute",
            top: "calc(100% + 6px)",
            right: 0,
            width: mobile ? "100%" : 280,
            background: "#fff",
            border: "1px solid var(--color-neutral-200)",
            borderRadius: 10,
            boxShadow: "0 8px 28px rgba(20,24,32,0.12)",
            padding: "18px 14px",
            fontSize: 12,
            color: "var(--color-neutral-500)",
            textAlign: "center",
            zIndex: 100,
          }}
        >
          {q ? (
            <>일치하는 최근 검색이 없어요.</>
          ) : (
            <>
              아직 검색한 플레이어가 없어요.
              <br />
              상단에 #태그를 입력해 검색해보세요.
            </>
          )}
        </div>
      )}
    </div>
  );
});
```

Preserve the current recent list item behavior: click recent tag calls `goToTag(r.tag)`, delete button calls `removeRecent(r.tag); notifyRecents();`, clear button calls `clearRecents(); notifyRecents();`.

- [ ] **Step 6: Update responsive CSS**

In `imstargg-frontend/app/styles/responsive.css`, remove mobile bottom tabbar styles:

```css
.mobile-tabbar { display: none; }
body { padding-bottom: 64px; }
body::after { ... }
.mobile-tabbar { ... }
.mobile-tabbar a { ... }
.mobile-tabbar a.active { ... }
.mobile-tabbar a:hover { ... }
.mobile-tabbar .tab-icon { ... }
body:has(.mobile-tabbar)::after { display: none; }
```

Add:

```css
.topnav-menu-button {
  display: none;
  color: #fff;
}

@media (max-width: 639px) {
  .topnav-inner {
    height: 52px;
    padding: 0 12px;
    gap: 10px;
  }

  .topnav-logo img {
    height: 24px !important;
  }

  .topnav-links,
  .topnav-spacer,
  .topnav-desktop-search {
    display: none !important;
  }

  .topnav-menu-button {
    display: inline-flex;
    margin-left: auto;
  }

  body {
    padding-bottom: 0;
  }

  .topnav-search-drawer {
    width: 100%;
    background: #F6F7FA;
    border-color: #D8DCE5;
  }

  .topnav-search-drawer input {
    color: #14161B;
  }
}
```

In `imstargg-frontend/app/styles/app.css`, keep `.topnav-search` desktop styles. Add this near `.topnav-logo`:

```css
.topnav-logo {
  border: 0;
  background: transparent;
  padding: 0;
}

.topnav-search-wrap {
  position: relative;
}
```

- [ ] **Step 7: Delete MobileTabBar files**

Delete these files with `apply_patch` delete hunks or another explicit file deletion mechanism available to the implementer. Do not delete unrelated files:

```text
imstargg-frontend/src/components/layout/MobileTabBar.tsx
imstargg-frontend/tests/components/MobileTabBar.test.tsx
```

- [ ] **Step 8: Run TopNav test**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/components/TopNav.test.tsx
```

Expected: PASS.

- [ ] **Step 9: Run component test suite around navigation**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/components
pnpm typecheck
```

Expected: PASS. If typecheck reports stale `MobileTabBar` imports, remove those imports and rerun.

- [ ] **Step 10: Commit mobile navigation changes**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add .gitignore \
  imstargg-frontend/app/layout.tsx \
  imstargg-frontend/app/styles/app.css \
  imstargg-frontend/app/styles/responsive.css \
  imstargg-frontend/src/components/layout/TopNav.tsx \
  imstargg-frontend/src/components/layout/MobileTabBar.tsx \
  imstargg-frontend/tests/components/MobileTabBar.test.tsx \
  imstargg-frontend/tests/components/TopNav.test.tsx
git commit -m "refactor(frontend): replace mobile tab bar with hamburger drawer"
```

Expected: commit succeeds. If deleted files are already removed, `git add` stages the deletion.

---

## Task 3: Production-Only AdSense Script

**Files:**

- Create: `imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx`
- Create: `imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx`
- Modify: `imstargg-frontend/app/layout.tsx`
- Modify: `imstargg-frontend/src/config/env.ts`
- Modify: `imstargg-frontend/.env.example`
- Modify: `imstargg-frontend/vitest.config.ts`

- [ ] **Step 1: Write the AdSense predicate test**

Create `imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { shouldLoadGoogleAdsense } from "@/components/ads/GoogleAdsenseScript";

describe("shouldLoadGoogleAdsense", () => {
  it("loads only in production with a client id", () => {
    expect(shouldLoadGoogleAdsense("production", "ca-pub-5084387143997125")).toBe(true);
  });

  it("does not load in development", () => {
    expect(shouldLoadGoogleAdsense("development", "ca-pub-5084387143997125")).toBe(false);
  });

  it("does not load in test", () => {
    expect(shouldLoadGoogleAdsense("test", "ca-pub-5084387143997125")).toBe(false);
  });

  it("does not load without a client id", () => {
    expect(shouldLoadGoogleAdsense("production", "")).toBe(false);
  });
});
```

- [ ] **Step 2: Run the AdSense test and verify it fails**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/components/ads/GoogleAdsenseScript.test.tsx
```

Expected: FAIL because the component module does not exist.

- [ ] **Step 3: Add env validation**

In `imstargg-frontend/src/config/env.ts`, add this schema field:

```ts
NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT: z.string().default("ca-pub-5084387143997125"),
```

Add it to the parsed object:

```ts
NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT: process.env.NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT,
```

In `imstargg-frontend/.env.example`, add:

```env
NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT=ca-pub-5084387143997125
```

In `imstargg-frontend/vitest.config.ts`, add to `test.env`:

```ts
NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT: "ca-pub-5084387143997125",
```

- [ ] **Step 4: Create the AdSense component**

Create `imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx`:

```tsx
import Script from "next/script";
import { env } from "@/config/env";

export function shouldLoadGoogleAdsense(
  nodeEnv: string | undefined,
  client: string | undefined,
): boolean {
  return nodeEnv === "production" && Boolean(client?.trim());
}

export function GoogleAdsenseScript() {
  const client = env.NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT;
  if (!shouldLoadGoogleAdsense(process.env.NODE_ENV, client)) return null;

  return (
    <Script
      id="google-adsense"
      async
      strategy="afterInteractive"
      src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${client}`}
      crossOrigin="anonymous"
    />
  );
}
```

- [ ] **Step 5: Add the component to root layout**

In `imstargg-frontend/app/layout.tsx`, add:

```tsx
import { GoogleAdsenseScript } from "@/components/ads/GoogleAdsenseScript";
```

Inside `<body>`, before `<QueryProvider>`:

```tsx
<GoogleAdsenseScript />
```

- [ ] **Step 6: Run AdSense tests and typecheck**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/components/ads/GoogleAdsenseScript.test.tsx
pnpm typecheck
```

Expected: PASS.

- [ ] **Step 7: Commit AdSense changes**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx \
  imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx \
  imstargg-frontend/app/layout.tsx \
  imstargg-frontend/src/config/env.ts \
  imstargg-frontend/.env.example \
  imstargg-frontend/vitest.config.ts
git commit -m "feat(frontend): add production AdSense bootstrap script"
```

Expected: commit succeeds.

---

## Task 4: Brawler Detail Item Ownership Integration

**Files:**

- Create: `imstargg-frontend/src/domain/brawler/itemOwnership.ts`
- Create: `imstargg-frontend/tests/unit/domain/brawler/itemOwnership.test.ts`
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`
- Delete: `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx`
- Delete: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx`

- [ ] **Step 1: Write item ownership helper tests**

Create `imstargg-frontend/tests/unit/domain/brawler/itemOwnership.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import type { BrawlerItems } from "@/domain/brawler/types";
import type { ItemOwnership } from "@/domain/statistics/types";
import { buildBrawlerItemGroups } from "@/domain/brawler/itemOwnership";

const items: BrawlerItems = {
  gadgets: [
    { id: 1, name: "빨리 감기", imageUrl: "/gadget.webp", rarity: null },
    { id: 2, name: "집중 사격", imageUrl: "/gadget2.webp", rarity: null },
  ],
  starPowers: [{ id: 11, name: "쉘 쇼크", imageUrl: "/sp.webp", rarity: null }],
  gears: [{ id: 21, name: "속도", imageUrl: "/gear.webp", rarity: "SUPER_RARE" }],
  hyperCharge: { id: 31, name: "더블 배럴", imageUrl: "/hc.webp", rarity: null },
  buffies: {
    gadget: { type: "GADGET", imageUrl: "/buffy-gadget.webp" },
    starPower: { type: "STAR_POWER", imageUrl: "/buffy-sp.webp" },
    hyperCharge: { type: "HYPER_CHARGE", imageUrl: "/buffy-hc.webp" },
  },
};

const ownership: ItemOwnership = {
  gadgets: [{ itemId: 1, ownershipRate: 0.75, ownerCount: 75, totalCount: 100 }],
  starPowers: [{ itemId: 11, ownershipRate: 1, ownerCount: 100, totalCount: 100 }],
  gears: [{ itemId: 21, ownershipRate: 0.5, ownerCount: 50, totalCount: 100 }],
  hyperCharges: [{ itemId: 31, ownershipRate: 0.25, ownerCount: 25, totalCount: 100 }],
  buffies: [{ buffyType: "GADGET", ownershipRate: 0.4, ownerCount: 40, totalCount: 100 }],
};

describe("buildBrawlerItemGroups", () => {
  it("keeps the requested row order", () => {
    const groups = buildBrawlerItemGroups(items, ownership);
    expect(groups.map((g) => g.key)).toEqual([
      "gadgets-starPowers",
      "gears",
      "hyperCharge-buffies",
    ]);
  });

  it("matches normal items by itemId", () => {
    const groups = buildBrawlerItemGroups(items, ownership);
    const gadget = groups[0].sections[0].items[0];
    expect(gadget.name).toBe("빨리 감기");
    expect(gadget.ownership?.rate).toBe(0.75);
    expect(gadget.ownership?.ownerCount).toBe(75);
  });

  it("shows undefined ownership when an item has no ownership record", () => {
    const groups = buildBrawlerItemGroups(items, ownership);
    const secondGadget = groups[0].sections[0].items[1];
    expect(secondGadget.name).toBe("집중 사격");
    expect(secondGadget.ownership).toBeUndefined();
  });

  it("matches buffies by buffyType", () => {
    const groups = buildBrawlerItemGroups(items, ownership);
    const buffy = groups[2].sections[1].items[0];
    expect(buffy.kind).toBe("buffy");
    expect(buffy.ownership?.rate).toBe(0.4);
  });
});
```

- [ ] **Step 2: Run item ownership tests and verify they fail**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/domain/brawler/itemOwnership.test.ts
```

Expected: FAIL because `@/domain/brawler/itemOwnership` does not exist.

- [ ] **Step 3: Add item ownership display helper**

Create `imstargg-frontend/src/domain/brawler/itemOwnership.ts`:

```ts
import type { BuffyResponse, GearRarity } from "@/api/types";
import type { BrawlerItem, BrawlerItems } from "@/domain/brawler/types";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";
import type {
  BuffyOwnershipItem,
  ItemOwnership,
  ItemOwnershipItem,
} from "@/domain/statistics/types";

export type BrawlerDisplayItemKind =
  | "gadget"
  | "starPower"
  | "gear"
  | "hyperCharge"
  | "buffy";

export type BrawlerDisplayItem = {
  key: string;
  kind: BrawlerDisplayItemKind;
  name: string;
  imageUrl: string | null;
  gearRarity?: GearRarity;
  ownership?: {
    rate: number;
    ownerCount: number;
    totalCount: number;
  };
};

export type BrawlerItemSection = {
  key: string;
  title: string;
  items: BrawlerDisplayItem[];
};

export type BrawlerItemGroup = {
  key: "gadgets-starPowers" | "gears" | "hyperCharge-buffies";
  sections: BrawlerItemSection[];
};

function ownershipByItemId(rows: ItemOwnershipItem[]): Map<number, ItemOwnershipItem> {
  return new Map(rows.map((row) => [row.itemId, row]));
}

function mapOwnership(row: ItemOwnershipItem | BuffyOwnershipItem | undefined) {
  if (!row) return undefined;
  return {
    rate: row.ownershipRate,
    ownerCount: row.ownerCount,
    totalCount: row.totalCount,
  };
}

function mapItem(
  kind: Exclude<BrawlerDisplayItemKind, "buffy">,
  item: BrawlerItem,
  ownership: ItemOwnershipItem | undefined,
): BrawlerDisplayItem {
  return {
    key: `${kind}:${item.id}`,
    kind,
    name: item.name,
    imageUrl: item.imageUrl,
    gearRarity: kind === "gear" ? (item.rarity ?? undefined) : undefined,
    ownership: mapOwnership(ownership),
  };
}

function mapBuffy(
  buffy: BuffyResponse,
  ownership: BuffyOwnershipItem | undefined,
): BrawlerDisplayItem {
  return {
    key: `buffy:${buffy.type}`,
    kind: "buffy",
    name: buffyTypeLabel(buffy.type),
    imageUrl: buffy.imageUrl,
    ownership: mapOwnership(ownership),
  };
}

export function buildBrawlerItemGroups(
  items: BrawlerItems,
  ownership: ItemOwnership,
): BrawlerItemGroup[] {
  const gadgetOwnership = ownershipByItemId(ownership.gadgets);
  const starPowerOwnership = ownershipByItemId(ownership.starPowers);
  const gearOwnership = ownershipByItemId(ownership.gears);
  const hyperChargeOwnership = ownershipByItemId(ownership.hyperCharges);
  const buffyOwnership = new Map(ownership.buffies.map((row) => [row.buffyType, row]));

  const firstRow: BrawlerItemGroup = {
    key: "gadgets-starPowers",
    sections: [
      {
        key: "gadgets",
        title: "가젯",
        items: items.gadgets.map((item) =>
          mapItem("gadget", item, gadgetOwnership.get(item.id)),
        ),
      },
      {
        key: "starPowers",
        title: "스타파워",
        items: items.starPowers.map((item) =>
          mapItem("starPower", item, starPowerOwnership.get(item.id)),
        ),
      },
    ],
  };

  const secondRow: BrawlerItemGroup = {
    key: "gears",
    sections: [
      {
        key: "gears",
        title: "기어",
        items: items.gears.map((item) => mapItem("gear", item, gearOwnership.get(item.id))),
      },
    ],
  };

  const thirdRow: BrawlerItemGroup = {
    key: "hyperCharge-buffies",
    sections: [
      {
        key: "hyperCharge",
        title: "하이퍼차지",
        items: items.hyperCharge
          ? [
              mapItem(
                "hyperCharge",
                items.hyperCharge,
                hyperChargeOwnership.get(items.hyperCharge.id),
              ),
            ]
          : [],
      },
      {
        key: "buffies",
        title: "버피",
        items: items.buffies
          ? [
              mapBuffy(items.buffies.gadget, buffyOwnership.get(items.buffies.gadget.type)),
              mapBuffy(
                items.buffies.starPower,
                buffyOwnership.get(items.buffies.starPower.type),
              ),
              mapBuffy(
                items.buffies.hyperCharge,
                buffyOwnership.get(items.buffies.hyperCharge.type),
              ),
            ]
          : [],
      },
    ],
  };

  return [firstRow, secondRow, thirdRow].map((group) => ({
    ...group,
    sections: group.sections.filter((section) => section.items.length > 0),
  }));
}
```

- [ ] **Step 4: Run item ownership tests and verify they pass**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/domain/brawler/itemOwnership.test.ts
```

Expected: PASS.

- [ ] **Step 5: Move ownership fetch to brawler page**

In `imstargg-frontend/app/brawlers/[id]/page.tsx`, add:

```ts
import { statisticsApi } from "@/api/endpoints/statistics";
import { parseTrophyRange } from "@/domain/statistics/enums";
import { toItemOwnership } from "@/domain/statistics/mappers";
import { CACHE_SECONDS } from "@/config/cache";
```

If `CACHE_SECONDS` was already imported in Task 1, reuse that import instead of adding a duplicate.

After `const sp = await searchParams;`, add:

```ts
const trophy = parseTrophyRange(sp.trophy);
```

After `detail = toBrawlerWithItems(dto);`, fetch ownership:

```ts
const ownershipDto = await statisticsApi.itemOwnership(
  { brawlerId: detail.brawler.id, trophyRange: trophy },
  { next: { revalidate: CACHE_SECONDS.SHORT } },
);
const ownership = toItemOwnership(ownershipDto);
```

Replace render:

```tsx
<BrawlerContent initialDetail={detail} />
<BrawlerStatsSection brawler={detail.brawler} items={detail.items} searchParams={sp} />
```

with:

```tsx
<BrawlerContent initialDetail={detail} ownership={ownership} trophy={trophy} />
<BrawlerStatsSection brawler={detail.brawler} searchParams={sp} />
```

- [ ] **Step 6: Update BrawlerContent props and remove separate buffies render**

In `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`, replace imports:

```ts
import type { BrawlerWithItems } from "@/domain/brawler/types";
import { BrawlerBuffies } from "./BrawlerBuffies";
```

with:

```ts
import type { BrawlerWithItems } from "@/domain/brawler/types";
import type { ItemOwnership } from "@/domain/statistics/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
```

Replace component signature:

```tsx
export function BrawlerContent({ initialDetail }: { initialDetail: BrawlerWithItems }) {
```

with:

```tsx
type Props = {
  initialDetail: BrawlerWithItems;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

export function BrawlerContent({ initialDetail, ownership, trophy }: Props) {
```

Replace:

```tsx
<BrawlerItems items={detail.items} />
{detail.items.buffies && <BrawlerBuffies buffies={detail.items.buffies} />}
```

with:

```tsx
<BrawlerItems items={detail.items} ownership={ownership} trophy={trophy} />
```

- [ ] **Step 7: Update BrawlerItems to render grouped ownership cards**

Replace `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx` with:

```tsx
"use client";

import type { BrawlerItems as BrawlerItemsModel } from "@/domain/brawler/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { buildBrawlerItemGroups } from "@/domain/brawler/itemOwnership";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";
import { BrawlerItemCard } from "./BrawlerItemCard";

type Props = {
  items: BrawlerItemsModel;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

export function BrawlerItems({ items, ownership, trophy }: Props) {
  const groups = buildBrawlerItemGroups(items, ownership).filter((group) =>
    group.sections.some((section) => section.items.length > 0),
  );
  const hasOwnership =
    ownership.gears.length > 0 ||
    ownership.gadgets.length > 0 ||
    ownership.starPowers.length > 0 ||
    ownership.hyperCharges.length > 0 ||
    ownership.buffies.length > 0;

  return (
    <section className="space-y-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-sm font-bold">아이템</h2>
          <p className="text-xs text-[#5C6271]">
            아이템별 보유율을 함께 확인하세요.
          </p>
        </div>
        <TrophyRangeSelector trophy={trophy} />
      </div>

      {!hasOwnership && (
        <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-3 text-sm text-[#5C6271]">
          보유율 데이터가 없습니다. 아이템은 데이터 없음 상태로 표시됩니다.
        </div>
      )}

      <div className="space-y-3">
        {groups.map((group) => (
          <div
            key={group.key}
            className={
              group.sections.length > 1
                ? "grid grid-cols-1 gap-3 lg:grid-cols-2"
                : "grid grid-cols-1 gap-3"
            }
          >
            {group.sections.map((section) => (
              <section key={section.key} className="rounded-lg border bg-card p-4">
                <h3 className="mb-3 text-sm font-bold">
                  {section.title}{" "}
                  <span className="text-muted-foreground">({section.items.length})</span>
                </h3>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {section.items.map((item) => (
                    <BrawlerItemCard key={item.key} item={item} />
                  ))}
                </div>
              </section>
            ))}
          </div>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 8: Update BrawlerItemCard for ownership display**

Replace `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx` with:

```tsx
"use client";

import ItemSlot from "@/components/primitives/ItemSlot";
import type { BrawlerDisplayItem } from "@/domain/brawler/itemOwnership";
import type { ItemKind } from "@/lib/itemPlaceholder";

const KIND_LABEL: Record<BrawlerDisplayItem["kind"], string> = {
  gadget: "가젯",
  starPower: "스타파워",
  gear: "기어",
  hyperCharge: "하이퍼차지",
  buffy: "버피",
};

const SLOT_KIND: Record<BrawlerDisplayItem["kind"], ItemKind> = {
  gadget: "gadget",
  starPower: "starPower",
  gear: "gear",
  hyperCharge: "hyperCharge",
  buffy: "buffy",
};

function formatRate(rate: number): string {
  return `${Math.round(rate * 100)}%`;
}

export function BrawlerItemCard({ item }: { item: BrawlerDisplayItem }) {
  const pct = item.ownership ? Math.round(item.ownership.rate * 100) : null;

  return (
    <div className="flex min-w-0 items-center gap-3 rounded border bg-card p-2.5">
      <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded bg-muted">
        <ItemSlot
          kind={SLOT_KIND[item.kind]}
          imageUrl={item.imageUrl}
          owned
          gearRarity={item.gearRarity}
          size={42}
          name={item.name}
          ariaLabel={`${KIND_LABEL[item.kind]} ${item.name}`}
        />
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 items-center gap-2">
          <span className="truncate text-sm font-bold">{item.name}</span>
          <span className="shrink-0 rounded bg-[#ECEEF3] px-1.5 py-0.5 text-[10px] font-bold text-[#5C6271]">
            {KIND_LABEL[item.kind]}
          </span>
        </div>

        {item.ownership ? (
          <div className="mt-1.5 flex items-center gap-2">
            <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-[#ECEEF3]">
              <div className="h-full bg-[#FFC60A]" style={{ width: `${pct}%` }} />
            </div>
            <span className="w-10 text-right text-xs font-bold tabular-nums">
              {formatRate(item.ownership.rate)}
            </span>
          </div>
        ) : (
          <div className="mt-1.5 text-xs font-semibold text-[#8A90A0]">데이터 없음</div>
        )}

        {item.ownership && (
          <div className="mt-0.5 text-[11px] text-[#8A90A0]">
            {item.ownership.ownerCount.toLocaleString()} /{" "}
            {item.ownership.totalCount.toLocaleString()}
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 9: Remove ownership from BrawlerStatsSection**

In `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`, remove imports:

```ts
parseTrophyRange,
toItemOwnership,
type BrawlerItems,
import { ItemOwnershipSection } from "./ItemOwnershipSection";
```

Change props:

```ts
type Props = {
  brawler: Brawler;
  items: BrawlerItems;
  searchParams: Record<string, string | string[] | undefined>;
};
```

to:

```ts
type Props = {
  brawler: Brawler;
  searchParams: Record<string, string | string[] | undefined>;
};
```

Remove:

```ts
const trophy = parseTrophyRange(searchParams.trophy);
```

Change `Promise.all` from four fetches to three fetches by removing `statisticsApi.itemOwnership(...)`.

Remove this render block:

```tsx
<ItemOwnershipSection
  items={items}
  ownership={toItemOwnership(ownership)}
  trophy={trophy}
/>
```

- [ ] **Step 10: Delete old standalone item ownership components**

Delete these files with `apply_patch` delete hunks or another explicit file deletion mechanism available to the implementer:

```text
imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx
imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx
```

- [ ] **Step 11: Run brawler ownership tests and typecheck**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/domain/brawler/itemOwnership.test.ts
pnpm typecheck
```

Expected: PASS. If `BrawlerBuffies` or `ItemOwnershipSection` imports remain, remove them and rerun.

- [ ] **Step 12: Commit brawler item integration**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/brawler/itemOwnership.ts \
  imstargg-frontend/tests/unit/domain/brawler/itemOwnership.test.ts \
  'imstargg-frontend/app/brawlers/[id]/page.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx' \
  'imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx'
git commit -m "feat(frontend): show brawler item ownership inline"
```

Expected: commit succeeds.

---

## Task 5: Final Verification

**Files:**

- No source files created unless a verification failure requires a fix.

- [ ] **Step 1: Run focused unit/component tests**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run tests/unit/config/cache.test.ts \
  tests/unit/components/ads/GoogleAdsenseScript.test.tsx \
  tests/unit/domain/brawler/itemOwnership.test.ts \
  tests/components/TopNav.test.tsx
```

Expected: PASS.

- [ ] **Step 2: Run full frontend test suite**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm test:run
```

Expected: PASS.

- [ ] **Step 3: Run typecheck and lint**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm typecheck
pnpm lint
```

Expected: PASS.

- [ ] **Step 4: Start local frontend server**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend
pnpm dev
```

Expected: Next.js starts and prints a local URL. If port 3000 is busy, use the next printed port.

- [ ] **Step 5: Browser verify desktop pages**

Open the local URL in the in-app browser and verify:

```text
/              desktop header: logo, links, search visible
/brawlers      desktop header stable, brawler table renders
/events        rotation cards render
/ranking       ranking list renders
```

Expected: no console error/warn from app code.

- [ ] **Step 6: Browser verify mobile pages**

Use 390px and 320px viewport screenshots for:

```text
/              top header is logo + hamburger only, no bottom tab bar
/brawlers      top header does not wrap
/events        top header does not wrap
/ranking       top header does not wrap
```

Expected:

- Hamburger opens drawer.
- Drawer contains player search and four menu buttons.
- Drawer search navigates to `/players/{tag}`.
- Drawer menu links navigate and close drawer.

- [ ] **Step 7: Browser verify brawler detail item ownership**

Use an existing brawler detail URL such as:

```text
/brawlers/16000000
```

Expected:

- First row contains 가젯 and 스타파워 sections.
- Second row contains 기어 section.
- Third row contains 하이퍼차지 and 버피 sections when present.
- Each item card shows either a percentage bar or "데이터 없음".
- The old bottom "아이템 보유율" section is gone.

- [ ] **Step 8: Inspect final git status**

Run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git status --short
```

Expected:

- Only user-owned unrelated files may remain, such as existing `.serena` files.
- `.superpowers/` should no longer appear as untracked because `.gitignore` covers it.

- [ ] **Step 9: Commit verification fixes if any**

If verification required source changes, run:

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add <changed-files>
git commit -m "fix(frontend): address UI cache refactor verification"
```

Expected: commit succeeds. Skip this step if no fixes were needed.
