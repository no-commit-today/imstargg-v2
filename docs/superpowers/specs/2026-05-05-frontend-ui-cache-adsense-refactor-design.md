# Frontend UI, Cache, AdSense, Brawler Items Refactor - Design

**Date**: 2026-05-05
**Status**: Approved
**Scope**: `imstargg-frontend` targeted refactor for mobile navigation, cache policy, AdSense bootstrap, brawler detail items, and adjacent cleanup.

## Summary

This work improves the public frontend without changing backend APIs. The implementation should stay targeted: fix the requested UI/data freshness issues, add the AdSense site snippet safely, and refactor only code touched by those changes.

| Topic | Area | Decision |
|---|---|---|
| Mobile navigation | `TopNav`, layout shell | Mobile header becomes logo + hamburger. Search and menu links move into a Sheet drawer. |
| Bottom navigation | `MobileTabBar` | Remove from mobile layout to avoid duplicated navigation. |
| Data freshness | RSC fetch options, query stale times | Do not lengthen frontend caches for data that must reflect backend refreshes. Prefer `no-store` or short revalidate windows. |
| AdSense | Root layout script | Load AdSense script only in production, via a small component and env validated client id. |
| Brawler detail items | `/brawlers/[id]` | Merge item UI and ownership rates so rates appear on each item card. |
| Refactor | Local boundaries | Split long navigation/item code, centralize cache constants, avoid broad style rewrites. |

## Goals

- Mobile header no longer wraps or pushes links downward.
- Mobile top bar is compact: logo + hamburger only.
- Player search remains available through the hamburger drawer.
- Ranking and live event data should not be held stale by long frontend ISR.
- Brawler list/detail static data remains cached only where freshness is low risk.
- AdSense snippet is installed once, managed in a dedicated component, and not loaded in local/test runs.
- Brawler detail item cards show ownership rates inline with each item, including a clear fallback when no rate exists.
- Keep changes easy to review and test.

## Non-Goals

- Backend cache changes.
- New ad slots or layout ad placements.
- Full design-system rewrite.
- Full search-history architecture rewrite across home, top nav, and club search.
- On-demand revalidation endpoints for frontend caches.
- Changing API response shapes.

## Current Findings

Mobile screenshots show the top nav links are hidden at 390px and 320px, and the bottom tab bar is active. The larger problem is visual density: at 320px the home hero logo overflows horizontally, and the top logo/search row feels cramped. Moving all mobile navigation and search into a drawer makes the top header stable.

`TopNav.tsx` is currently large and owns custom search/recent-search logic. There is also a separate shared `PlayerSearchInput` stack for the home page. This spec should reduce `TopNav` complexity without forcing a complete search rewrite.

Frontend cache values are mixed across pages and hooks. Some existing values are long for static reference data, while ranking, events, and statistics need fresher behavior. Because backend caching already protects heavy APIs, the frontend should bias toward freshness rather than adding long ISR windows.

## Design Decisions

### 1. Mobile Navigation

Desktop keeps the current pattern:

```text
logo | brawlers | events | ranking | clubs | spacer | player search
```

Mobile becomes:

```text
logo | hamburger
```

The hamburger opens a right-side `Sheet` from the existing shadcn/Radix primitives. The drawer contains:

1. Player search input.
2. Navigation links: 브롤러, 이벤트, 랭킹, 클럽.
3. Optional app/version footer if it fits naturally, but not required.

Behavior:

- Opening the drawer traps focus through Radix Sheet.
- Selecting a menu item closes the drawer and navigates.
- Submitting player search closes the drawer and navigates to `/players/{tag}`.
- The desktop search remains visible only at desktop/tablet widths.
- `MobileTabBar` is removed from `app/layout.tsx`. Delete the component and tests if no imports remain after the refactor.

Implementation boundary:

- Split `TopNav` into small local pieces if useful:
  - `TopNavLinks`
  - `TopNavPlayerSearch`
  - `MobileNavDrawer`
- Avoid changing home page search except for shared helper reuse if trivial.

### 2. Cache Policy

Important correction: frontend cache duration must not equal backend refresh cadence. With time-based revalidation, a page can serve stale data and refresh later. For data that updates on a known 30 minute job, a 30 minute frontend cache can show outdated data after the backend has already refreshed.

Principles:

- Backend caches handle heavy API cost.
- Frontend caches should mainly reduce accidental duplicate requests, not hide backend refreshes.
- High-freshness pages use `cache: "no-store"` or very short `next.revalidate`.
- Static reference data may use longer cache windows, but not so long that new content is invisible for a day on primary pages.
- Cache values should be named constants for readability, but route segment `export const revalidate` must remain statically analyzable literals where used.

Recommended policy:

| Data | Frontend policy | Reason |
|---|---:|---|
| Ranking pages | `cache: "no-store"` | Backend refreshes every ~30m. Show fresh backend state immediately. |
| Event rotation list | `cache: "no-store"` | "현재 진행 중" must not linger. Existing client refetch by end time remains. |
| Event detail basic data | `cache: "no-store"` | Event context changes often enough that 1h is too stale. |
| Event detail statistics | `revalidate: 60` | Heavy work is backend-cached; frontend should stay fresh. |
| Brawler list primary page | `revalidate: 3600` | Reference data is stable, but new brawlers should appear same day. |
| Brawler detail basic data | `revalidate: 3600` | Same as brawler list. |
| Brawler statistics and ownership | `revalidate: 60` | Backend cache already protects heavy queries; frontend freshness wins. |
| Sitemap and static params helpers | `revalidate: 86400` acceptable | SEO helper paths can tolerate delayed discovery. |
| Player/club detail | keep `60s` | Existing renewal flow depends on short freshness. |
| Renewal status | `revalidate: 0` or `no-store` | Must reflect progress. |

Add `src/config/cache.ts`:

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

Use these constants in `fetch` options and TanStack Query hooks where TypeScript code can use expressions. For `export const revalidate`, use literal numbers and add comments if needed.

### 3. AdSense Script

Add a dedicated component:

```text
src/components/ads/GoogleAdsenseScript.tsx
```

Behavior:

- Uses `next/script`.
- Reads `env.NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT`.
- Renders only when `process.env.NODE_ENV === "production"`.
- Does not render in local dev, Vitest, or Playwright.
- Uses `strategy="afterInteractive"` and `crossOrigin="anonymous"`.

Environment:

- Add `NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT` to `src/config/env.ts`.
- Add it to `.env.example`.
- Client id value: `ca-pub-5084387143997125`.

Root layout:

```tsx
<GoogleAdsenseScript />
```

Place it inside `<body>` before or after the app shell. There is no `_document.tsx` in App Router.

### 4. Brawler Detail Items and Ownership

Current state:

- `BrawlerItems` shows item cards near the top.
- `ItemOwnershipSection` shows rates at the bottom.
- This separates the item identity from the ownership data and forces users to scroll.

New model:

```ts
type DisplayItemKind = "gadget" | "starPower" | "gear" | "hyperCharge" | "buffy";

type DisplayItem = {
  key: string;
  kind: DisplayItemKind;
  name: string;
  imageUrl: string | null;
  gearRarity?: GearRarity;
  ownership?: {
    rate: number;
    ownerCount: number;
    totalCount: number;
  };
};
```

Ownership matching:

- gadgets, star powers, gears, hyper charge: match by `itemId`.
- buffies: match by `buffyType`.
- If no ownership record exists, show a neutral "데이터 없음" badge.

Layout:

1. First row: 가젯 + 스타파워.
2. Second row: 기어.
3. Third row: 하이퍼차지 + 버피.

Each category section has a compact header and item cards. Each card contains:

- Item icon.
- Item name.
- Category label.
- Ownership percentage when available.
- Optional small count caption such as `12 / 20`.
- A small horizontal bar for percentage where it improves scanability.

The `TrophyRangeSelector` moves to the top of the item section so users understand which ownership cohort they are viewing.

Remove or replace the standalone bottom `ItemOwnershipSection` after rates are integrated. Keep the empty-state copy: "보유율 데이터가 없습니다." when the endpoint returns no usable data at all.

### 5. Refactor Boundary

Allowed refactors:

- Split `TopNav.tsx` if it materially improves readability.
- Replace emoji/icon text in nav controls with lucide icons where available.
- Remove `MobileTabBar` code and its test when no longer used.
- Extract brawler detail item display helpers into the brawler detail folder or `src/domain/statistics` if pure.
- Centralize cache constants.
- Replace fragile inline styles only in touched areas.

Avoid:

- Rewriting all responsive CSS.
- Changing visual design outside the affected header, drawer, and brawler item section.
- Changing backend DTOs.
- Migrating all search components into one architecture in this pass.
- Adding ad placements.

### 6. Testing and Verification

Automated checks:

- `pnpm typecheck`
- `pnpm test:run`
- Add/update component tests for mobile drawer behavior.
- Add/update tests replacing `MobileTabBar` expectations if it is removed.
- Add pure tests for item ownership matching if helper logic is extracted.

Manual browser verification:

- Desktop home, brawlers, events, ranking.
- Mobile 390px and 320px:
  - Header does not wrap.
  - Hamburger opens drawer.
  - Drawer search navigates.
  - Menu links navigate and close drawer.
  - No bottom tab bar remains.
- Brawler detail mobile and desktop:
  - item order matches requested rows.
  - ownership percentages appear on cards.
  - missing ownership shows "데이터 없음".
- Events:
  - current rotation does not remain stale after navigation/reload.
- Ranking:
  - page fetches fresh backend state rather than relying on long ISR.
- Console: no new errors or warnings.

## Risks

| Risk | Mitigation |
|---|---|
| Removing bottom tab bar reduces one-tap mobile navigation | Hamburger drawer keeps all primary links. Mobile header stays stable. |
| `no-store` increases frontend request count | Backend cache already protects heavy APIs. Revisit only if production metrics show pressure. |
| Drawer search duplicates home search behavior | Keep shared helper use small. Do not force a full search architecture rewrite. |
| AdSense accidentally loads in tests | Production-only component branch, plus test assertion if cheap. |
| Item ownership matching misses buffy type variants | Add pure unit tests for `itemId` and `buffyType` matching. |

## Implementation Order

1. Cache constants and freshness policy updates.
2. Mobile navigation refactor and bottom tab removal.
3. AdSense script component and env wiring.
4. Brawler item ownership integration.
5. Focused tests and browser verification.

This order reduces stale data first, then changes the shell UI, then adds the small global script, then changes the detailed brawler item surface.

## References

- Next.js Caching and Revalidating (Previous Model): https://nextjs.org/docs/app/guides/caching-without-cache-components
- Google AdSense code overview: https://support.google.com/adsense/answer/9274634
- Google AdSense invalid traffic definition: https://support.google.com/adsense/answer/16737
