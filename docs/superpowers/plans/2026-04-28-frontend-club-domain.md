# Frontend Club Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `app/clubs/{page,[tag]/page}.tsx` mock with real API integration (search, detail, members, renewal) using the Foundation pattern.

**Architecture:** RSC page fetches `GET /v1/clubs/{tag}` + `GET /v1/clubs/{tag}/renewal` with `revalidate: 60`, hydrates a client `ClubContent` via TanStack Query `initialData`. `useClubRenewal` mirrors `usePlayerRenewal` (1s polling, sonner toasts, NotFoundError → emptyRenewal). Member sort and trophy distribution are pure functions in `src/domain/club/members.ts`.

**Tech Stack:** Next.js 16 App Router, React 19, TanStack Query v5, sonner, Vitest + MSW + Testing Library, Playwright.

**Spec:** `docs/superpowers/specs/2026-04-28-frontend-club-domain-design.md`

**Working directory:** `/Users/chodyoungrae/Projects/imstargg-v2/imstargg-frontend`

**Branch:** create `feat/frontend-club-domain` off `main` before starting Task 1.

```bash
git checkout main && git pull --ff-only
git checkout -b feat/frontend-club-domain
```

---

## File Map

**Create:**
- `src/api/endpoints/club.ts`
- `src/domain/club/types.ts`
- `src/domain/club/mappers.ts`
- `src/domain/club/members.ts`
- `src/hooks/queries/useClub.ts`
- `src/hooks/useClubRenewal.ts`
- `app/clubs/[tag]/error.tsx`
- `app/clubs/[tag]/not-found.tsx`
- `app/clubs/[tag]/_components/ClubContent.tsx`
- `app/clubs/[tag]/_components/ClubHeader.tsx`
- `app/clubs/[tag]/_components/ClubMembers.tsx`
- `app/clubs/[tag]/_components/ClubTrophyDistribution.tsx`
- `app/clubs/_components/ClubSearchInput.tsx`
- `tests/unit/domain/club/mappers.test.ts`
- `tests/unit/domain/club/members.test.ts`
- `tests/integration/club/clubRenewal.test.tsx`
- `tests/e2e/club.spec.ts`

**Modify:**
- `src/api/types.ts` (add Club* types)
- `src/hooks/queries/queryKeys.ts` (add club keys)
- `src/components/shared/RenewalButton.tsx` (wire club kind to `useClubRenewal`)
- `app/clubs/page.tsx` (use rewritten ClubSearchPage)
- `app/clubs/_components/ClubSearchPage.tsx` (rewrite — drop mock recents, use ClubSearchInput)
- `app/clubs/[tag]/page.tsx` (rewrite as RSC + generateMetadata)

**Delete:**
- `app/clubs/[tag]/_components/ClubDetailPage.tsx`

---

## Task 1: Add Club DTO types

**Files:**
- Modify: `src/api/types.ts`

- [ ] **Step 1: Append Club types**

Open `src/api/types.ts` and append at the end (after the Renewal block):

```typescript
// === Club ===
export type ClubType = "OPEN" | "INVITE_ONLY" | "CLOSED" | "UNKNOWN";

export type ClubMemberRole =
  | "PRESIDENT"
  | "VICE_PRESIDENT"
  | "SENIOR"
  | "MEMBER"
  | "UNKNOWN";

export interface ClubMemberResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole | null;
  trophies: number;
  iconId: number;
}

export interface ClubResponse {
  tag: string;
  name: string;
  description: string;
  type: ClubType | null;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMemberResponse[];
}
```

- [ ] **Step 2: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/api/types.ts
git commit -m "feat(frontend): add Club* DTO types"
```

---

## Task 2: Add `clubApi` endpoint

**Files:**
- Create: `src/api/endpoints/club.ts`

- [ ] **Step 1: Write the file**

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { ClubResponse } from "@/api/types";

export const clubApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<ClubResponse>(`/v1/clubs/${toUrlPath(tag)}`, opts),
};
```

- [ ] **Step 2: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/api/endpoints/club.ts
git commit -m "feat(frontend): add clubApi.get endpoint"
```

> Note: `renewalApi.getClub` / `renewClub` already exist in `src/api/endpoints/renewal.ts`; no change needed there.

---

## Task 3: Add Club domain types

**Files:**
- Create: `src/domain/club/types.ts`

- [ ] **Step 1: Write the file**

Domain types collapse `null` role/type into `"UNKNOWN"` so consumers don't branch on null.

```typescript
import type { ClubType, ClubMemberRole } from "@/api/types";

export type ClubTypeDomain = ClubType; // alias kept for clarity at usage sites
export type ClubMemberRoleDomain = ClubMemberRole;

export interface ClubMember {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole;
  trophies: number;
  iconId: number;
}

export interface Club {
  tag: string;
  name: string;
  description: string;
  type: ClubType;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMember[];
}
```

- [ ] **Step 2: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/domain/club/types.ts
git commit -m "feat(frontend): add Club domain types"
```

---

## Task 4: Club mappers (TDD)

**Files:**
- Create: `tests/unit/domain/club/mappers.test.ts`
- Create: `src/domain/club/mappers.ts`

- [ ] **Step 1: Write failing tests**

```typescript
import { describe, it, expect } from "vitest";
import { toClub, toClubMember } from "@/domain/club/mappers";
import type { ClubMemberResponse, ClubResponse } from "@/api/types";

const dtoMember = (overrides: Partial<ClubMemberResponse> = {}): ClubMemberResponse => ({
  tag: "#M1",
  name: "Alice",
  nameColor: "0xfff",
  role: "MEMBER",
  trophies: 10000,
  iconId: 28000000,
  ...overrides,
});

const dtoClub = (overrides: Partial<ClubResponse> = {}): ClubResponse => ({
  tag: "2U8CVLQQ9",
  name: "팀나인",
  description: "환영합니다",
  type: "INVITE_ONLY",
  badgeId: 8000001,
  requiredTrophies: 30000,
  trophies: 1284820,
  members: [dtoMember()],
  ...overrides,
});

describe("toClubMember", () => {
  it("passes through values when role is present", () => {
    const m = toClubMember(dtoMember({ role: "PRESIDENT" }));
    expect(m.role).toBe("PRESIDENT");
    expect(m.tag).toBe("#M1");
    expect(m.name).toBe("Alice");
  });

  it("collapses null role to UNKNOWN", () => {
    const m = toClubMember(dtoMember({ role: null }));
    expect(m.role).toBe("UNKNOWN");
  });

  it("preserves nullable nameColor", () => {
    const m = toClubMember(dtoMember({ nameColor: null }));
    expect(m.nameColor).toBeNull();
  });
});

describe("toClub", () => {
  it("normalizes tag to leading-#", () => {
    expect(toClub(dtoClub({ tag: "2U8CVLQQ9" })).tag).toBe("#2U8CVLQQ9");
    expect(toClub(dtoClub({ tag: "#2U8CVLQQ9" })).tag).toBe("#2U8CVLQQ9");
  });

  it("collapses null type to UNKNOWN", () => {
    expect(toClub(dtoClub({ type: null })).type).toBe("UNKNOWN");
  });

  it("maps every member", () => {
    const club = toClub(
      dtoClub({
        members: [
          dtoMember({ tag: "#A", role: "PRESIDENT" }),
          dtoMember({ tag: "#B", role: null }),
        ],
      }),
    );
    expect(club.members.map((m) => m.role)).toEqual(["PRESIDENT", "UNKNOWN"]);
  });
});
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `pnpm test:run tests/unit/domain/club/mappers.test.ts`
Expected: FAIL — `toClub`/`toClubMember` not defined.

- [ ] **Step 3: Implement mappers**

Create `src/domain/club/mappers.ts`:

```typescript
import type { ClubResponse, ClubMemberResponse } from "@/api/types";
import type { Club, ClubMember } from "@/domain/club/types";
import { normalizeTag } from "@/lib/tag";

export function toClubMember(dto: ClubMemberResponse): ClubMember {
  return {
    tag: normalizeTag(dto.tag),
    name: dto.name,
    nameColor: dto.nameColor,
    role: dto.role ?? "UNKNOWN",
    trophies: dto.trophies,
    iconId: dto.iconId,
  };
}

export function toClub(dto: ClubResponse): Club {
  return {
    tag: normalizeTag(dto.tag),
    name: dto.name,
    description: dto.description,
    type: dto.type ?? "UNKNOWN",
    badgeId: dto.badgeId,
    requiredTrophies: dto.requiredTrophies,
    trophies: dto.trophies,
    members: dto.members.map(toClubMember),
  };
}
```

- [ ] **Step 4: Run tests, verify they pass**

Run: `pnpm test:run tests/unit/domain/club/mappers.test.ts`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/club/mappers.test.ts src/domain/club/mappers.ts
git commit -m "feat(frontend): add Club domain mappers (TDD)"
```

---

## Task 5: Member sort + trophy distribution (TDD)

**Files:**
- Create: `tests/unit/domain/club/members.test.ts`
- Create: `src/domain/club/members.ts`

- [ ] **Step 1: Write failing tests**

```typescript
import { describe, it, expect } from "vitest";
import { sortMembers, trophyDistribution } from "@/domain/club/members";
import type { ClubMember } from "@/domain/club/types";

const m = (overrides: Partial<ClubMember> = {}): ClubMember => ({
  tag: "#M",
  name: "n",
  nameColor: null,
  role: "MEMBER",
  trophies: 10000,
  iconId: 0,
  ...overrides,
});

describe("sortMembers", () => {
  it("orders by role rank PRESIDENT > VICE_PRESIDENT > SENIOR > MEMBER > UNKNOWN", () => {
    const input = [
      m({ tag: "#1", role: "MEMBER" }),
      m({ tag: "#2", role: "PRESIDENT" }),
      m({ tag: "#3", role: "UNKNOWN" }),
      m({ tag: "#4", role: "SENIOR" }),
      m({ tag: "#5", role: "VICE_PRESIDENT" }),
    ];
    expect(sortMembers(input).map((x) => x.tag)).toEqual([
      "#2", "#5", "#4", "#1", "#3",
    ]);
  });

  it("breaks ties by trophies descending", () => {
    const input = [
      m({ tag: "#a", role: "MEMBER", trophies: 100 }),
      m({ tag: "#b", role: "MEMBER", trophies: 300 }),
      m({ tag: "#c", role: "MEMBER", trophies: 200 }),
    ];
    expect(sortMembers(input).map((x) => x.tag)).toEqual(["#b", "#c", "#a"]);
  });

  it("does not mutate the input array", () => {
    const input = [m({ tag: "#a", role: "MEMBER" }), m({ tag: "#b", role: "PRESIDENT" })];
    const original = input.map((x) => x.tag);
    sortMembers(input);
    expect(input.map((x) => x.tag)).toEqual(original);
  });
});

describe("trophyDistribution", () => {
  it("buckets are 90k+, 80–90k, 70–80k, 60–70k, <60k in that order", () => {
    const result = trophyDistribution([]);
    expect(result.map((b) => b.label)).toEqual([
      "90k+", "80–90k", "70–80k", "60–70k", "<60k",
    ]);
    expect(result.every((b) => b.count === 0)).toBe(true);
  });

  it("counts boundary values: 60000→60–70k, 80000→80–90k, 90000→90k+", () => {
    const members = [
      m({ trophies: 59999 }),
      m({ trophies: 60000 }),
      m({ trophies: 79999 }),
      m({ trophies: 80000 }),
      m({ trophies: 89999 }),
      m({ trophies: 90000 }),
    ];
    const counts = Object.fromEntries(
      trophyDistribution(members).map((b) => [b.label, b.count]),
    );
    expect(counts["<60k"]).toBe(1);
    expect(counts["60–70k"]).toBe(1);
    expect(counts["70–80k"]).toBe(1);
    expect(counts["80–90k"]).toBe(2);
    expect(counts["90k+"]).toBe(1);
  });
});
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `pnpm test:run tests/unit/domain/club/members.test.ts`
Expected: FAIL — functions not defined.

- [ ] **Step 3: Implement**

Create `src/domain/club/members.ts`:

```typescript
import type { ClubMember } from "@/domain/club/types";
import type { ClubMemberRole } from "@/api/types";

const ROLE_RANK: Record<ClubMemberRole, number> = {
  PRESIDENT: 0,
  VICE_PRESIDENT: 1,
  SENIOR: 2,
  MEMBER: 3,
  UNKNOWN: 4,
};

export function sortMembers(members: ClubMember[]): ClubMember[] {
  return [...members].sort((a, b) => {
    const roleDiff = ROLE_RANK[a.role] - ROLE_RANK[b.role];
    if (roleDiff !== 0) return roleDiff;
    return b.trophies - a.trophies;
  });
}

export interface TrophyBucket {
  label: string;
  min: number;
  max: number | null; // exclusive upper bound; null = open
  count: number;
}

const BUCKETS: ReadonlyArray<Omit<TrophyBucket, "count">> = [
  { label: "90k+", min: 90000, max: null },
  { label: "80–90k", min: 80000, max: 90000 },
  { label: "70–80k", min: 70000, max: 80000 },
  { label: "60–70k", min: 60000, max: 70000 },
  { label: "<60k", min: 0, max: 60000 },
];

export function trophyDistribution(members: ClubMember[]): TrophyBucket[] {
  return BUCKETS.map((b) => ({
    ...b,
    count: members.filter(
      (m) => m.trophies >= b.min && (b.max === null || m.trophies < b.max),
    ).length,
  }));
}
```

- [ ] **Step 4: Run tests, verify they pass**

Run: `pnpm test:run tests/unit/domain/club/members.test.ts`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add tests/unit/domain/club/members.test.ts src/domain/club/members.ts
git commit -m "feat(frontend): add member sort and trophy distribution utils (TDD)"
```

---

## Task 6: Add club query keys + useClub

**Files:**
- Modify: `src/hooks/queries/queryKeys.ts`
- Create: `src/hooks/queries/useClub.ts`

- [ ] **Step 1: Extend queryKeys**

Replace the contents of `src/hooks/queries/queryKeys.ts` with:

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
} as const;
```

- [ ] **Step 2: Create `useClub`**

Create `src/hooks/queries/useClub.ts`:

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { clubApi } from "@/api/endpoints/club";
import { toClub } from "@/domain/club/mappers";
import type { Club } from "@/domain/club/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useClub(tag: string, options?: { initialData?: Club }) {
  return useQuery({
    queryKey: queryKeys.club.detail(tag),
    queryFn: ({ signal }) => clubApi.get(tag, { signal }).then(toClub),
    initialData: options?.initialData,
    staleTime: 60_000,
  });
}
```

- [ ] **Step 3: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/hooks/queries/queryKeys.ts src/hooks/queries/useClub.ts
git commit -m "feat(frontend): add club query keys and useClub hook"
```

---

## Task 7: useClubRenewal hook

**Files:**
- Create: `src/hooks/useClubRenewal.ts`

- [ ] **Step 1: Write the file (mirror `usePlayerRenewal`)**

```typescript
"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useClubRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.club.renewal(tag),
    queryFn: async ({ signal }) => {
      try {
        return await renewalApi.getClub(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.club.all });
      toast.success("갱신 완료");
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewClub(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.club.renewal(tag), data);
      toast.info("갱신 요청됨");
    },
    onError: () => toast.error("갱신 요청 실패"),
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
```

- [ ] **Step 2: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/hooks/useClubRenewal.ts
git commit -m "feat(frontend): add useClubRenewal hook (1s polling, sonner toasts)"
```

---

## Task 8: Wire RenewalButton club kind to `useClubRenewal`

**Files:**
- Modify: `src/components/shared/RenewalButton.tsx`

- [ ] **Step 1: Replace placeholder with real hook**

Replace the entire file with:

```typescript
"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";
import { useClubRenewal } from "@/hooks/useClubRenewal";

type Props =
  | { kind: "player"; tag: string }
  | { kind: "club"; tag: string };

export function RenewalButton(props: Props) {
  if (props.kind === "player") return <PlayerRenewalButton tag={props.tag} />;
  return <ClubRenewalButton tag={props.tag} />;
}

function PlayerRenewalButton({ tag }: { tag: string }) {
  const { progressing, canRenew, trigger, triggering } = usePlayerRenewal(tag);
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button
      type="button"
      variant="default"
      size="sm"
      disabled={disabled}
      onClick={trigger}
      aria-label="플레이어 정보 갱신"
    >
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}

function ClubRenewalButton({ tag }: { tag: string }) {
  const { progressing, canRenew, trigger, triggering } = useClubRenewal(tag);
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button
      type="button"
      variant="default"
      size="sm"
      disabled={disabled}
      onClick={trigger}
      aria-label="클럽 정보 갱신"
    >
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}
```

- [ ] **Step 2: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/components/shared/RenewalButton.tsx
git commit -m "feat(frontend): wire club RenewalButton to useClubRenewal"
```

---

## Task 9: Club detail RSC page + error/not-found

**Files:**
- Create: `app/clubs/[tag]/error.tsx`
- Create: `app/clubs/[tag]/not-found.tsx`
- Modify (rewrite): `app/clubs/[tag]/page.tsx`

- [ ] **Step 1: Create error.tsx**

```typescript
"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function ClubError({
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

- [ ] **Step 2: Create not-found.tsx**

```typescript
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function ClubNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">클럽을 찾을 수 없어요</h1>
      <p className="text-muted-foreground">태그가 정확한지 다시 확인해 주세요.</p>
      <Button asChild variant="outline">
        <Link href="/clubs">클럽 검색</Link>
      </Button>
    </main>
  );
}
```

- [ ] **Step 3: Rewrite `app/clubs/[tag]/page.tsx`**

Replace the entire file:

```typescript
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { clubApi } from "@/api/endpoints/club";
import { renewalApi } from "@/api/endpoints/renewal";
import { toClub } from "@/domain/club/mappers";
import { emptyRenewal } from "@/domain/renewal/empty";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { normalizeTag, toUrlPath } from "@/lib/tag";
import { formatNumber } from "@/lib/format";
import { ClubContent } from "./_components/ClubContent";

type Params = { params: Promise<{ tag: string }> };

async function loadClub(tag: string) {
  return clubApi.get(tag, { next: { revalidate: 60 } });
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { tag } = await params;
  try {
    const dto = await loadClub(tag);
    const display = `${dto.name} (${dto.tag})`;
    return buildMetadata({
      title: `${display} | ImStarGG`,
      description: `${display} — 트로피 ${formatNumber(dto.trophies)}, 멤버 ${dto.members.length}/30. 브롤스타즈 클럽 통계.`,
      path: `/clubs/${toUrlPath(tag)}`,
    });
  } catch {
    return buildMetadata({
      title: "클럽을 찾을 수 없음 | ImStarGG",
      description: "요청하신 클럽을 찾을 수 없습니다.",
      path: `/clubs/${toUrlPath(tag)}`,
      noindex: true,
    });
  }
}

export default async function ClubPage({ params }: Params) {
  const { tag } = await params;
  let dto;
  try {
    dto = await loadClub(tag);
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const renewal = await renewalApi
    .getClub(tag, { next: { revalidate: 0 } })
    .catch((e: unknown) => (e instanceof NotFoundError ? emptyRenewal(tag) : null));

  return (
    <ClubContent
      tag={normalizeTag(tag)}
      initialClub={toClub(dto)}
      initialRenewal={renewal}
    />
  );
}
```

- [ ] **Step 4: Note — ClubContent does not exist yet**

`pnpm typecheck` will fail until Task 10. Skip typecheck and commit; the next task adds `ClubContent`.

- [ ] **Step 5: Commit**

```bash
git add app/clubs/[tag]/page.tsx app/clubs/[tag]/error.tsx app/clubs/[tag]/not-found.tsx
git commit -m "feat(frontend): club detail RSC page + error/not-found"
```

---

## Task 10: ClubContent + ClubHeader + ClubMembers + ClubTrophyDistribution

**Files:**
- Create: `app/clubs/[tag]/_components/ClubContent.tsx`
- Create: `app/clubs/[tag]/_components/ClubHeader.tsx`
- Create: `app/clubs/[tag]/_components/ClubMembers.tsx`
- Create: `app/clubs/[tag]/_components/ClubTrophyDistribution.tsx`

- [ ] **Step 1: ClubContent**

```typescript
"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useClub } from "@/hooks/queries/useClub";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Club } from "@/domain/club/types";
import type { RenewalResponse } from "@/api/types";
import { sortMembers } from "@/domain/club/members";
import { ClubHeader } from "./ClubHeader";
import { ClubMembers } from "./ClubMembers";
import { ClubTrophyDistribution } from "./ClubTrophyDistribution";

type Props = {
  tag: string;
  initialClub: Club;
  initialRenewal: RenewalResponse | null;
};

export function ClubContent({ tag, initialClub, initialRenewal }: Props) {
  const qc = useQueryClient();

  useEffect(() => {
    if (initialRenewal) {
      qc.setQueryData(queryKeys.club.renewal(tag), initialRenewal);
    }
  }, [tag, initialRenewal, qc]);

  const { data: club } = useClub(tag, { initialData: initialClub });
  if (!club) return null;

  const sorted = sortMembers(club.members);

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-6 space-y-6">
      <ClubHeader club={club} tag={tag} />
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <ClubMembers members={sorted} />
        <ClubTrophyDistribution members={club.members} />
      </div>
    </main>
  );
}
```

- [ ] **Step 2: ClubHeader**

```typescript
"use client";

import type { Club } from "@/domain/club/types";
import type { ClubType } from "@/api/types";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { formatNumber } from "@/lib/format";

const TYPE_LABEL: Record<ClubType, string> = {
  OPEN: "가입 가능",
  INVITE_ONLY: "가입 신청 필요",
  CLOSED: "비공개",
  UNKNOWN: "-",
};

export function ClubHeader({ club, tag }: { club: Club; tag: string }) {
  const memberCount = club.members.length;
  const avgTrophies = memberCount
    ? Math.floor(club.members.reduce((s, m) => s + m.trophies, 0) / memberCount)
    : 0;

  return (
    <section className="card overflow-hidden">
      <div className="grid grid-cols-1 lg:grid-cols-[180px_1fr_240px] bg-[linear-gradient(135deg,#2A2E38,#1A1D24)] text-white">
        <div className="flex flex-col items-center justify-center gap-2 p-7 border-r border-white/5">
          <div className="flex h-28 w-28 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#FFC60A,#FF8A1F)] shadow-[0_6px_20px_rgba(255,198,10,0.4)]">
            <img src="/assets/club_league.png" alt="" className="h-16 w-16" />
          </div>
          <div className="font-mono text-xs text-white/70">{club.tag}</div>
        </div>

        <div className="p-6">
          <div className="mb-1 flex items-center gap-2">
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-white/60">
              CLUB
            </span>
            <span className="rounded bg-[rgba(255,198,10,0.2)] px-2 py-0.5 text-[11px] font-bold text-[var(--color-brand-yellow)]">
              {TYPE_LABEL[club.type]}
            </span>
          </div>
          <h1 className="m-0 text-3xl font-display tracking-[0.02em]">{club.name}</h1>
          <p className="mt-3 max-w-xl text-[13px] leading-relaxed text-white/85">
            {club.description}
          </p>

          <div className="mt-4 flex flex-wrap gap-6">
            <Stat label="총 트로피" value={formatNumber(club.trophies)} icon="trophy.png" />
            <Stat label="멤버" value={`${memberCount}/30`} />
            <Stat label="필요 트로피" value={formatNumber(club.requiredTrophies)} />
            <Stat label="평균" value={formatNumber(avgTrophies)} />
          </div>
        </div>

        <div className="flex flex-col justify-center gap-3 border-t border-white/5 p-6 lg:border-l lg:border-t-0">
          <div className="rounded bg-white/5 p-3 text-[11px] leading-relaxed text-white/70">
            <strong className="mb-1 block text-white">🏛 클럽 타입</strong>
            <span className="font-bold text-[var(--color-brand-yellow)]">
              {TYPE_LABEL[club.type]}
            </span>
          </div>
          <RenewalButton kind="club" tag={tag} />
        </div>
      </div>
    </section>
  );
}

function Stat({ label, value, icon }: { label: string; value: string; icon?: string }) {
  return (
    <div>
      <div className="mb-0.5 text-[10px] font-bold uppercase tracking-[0.06em] text-white/60">
        {label}
      </div>
      <div className="num flex items-center gap-1 text-lg font-extrabold text-white">
        {icon && <img src={`/assets/${icon}`} alt="" className="h-4 w-4" />}
        {value}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: ClubMembers**

```typescript
"use client";

import { useRouter } from "next/navigation";
import type { ClubMember } from "@/domain/club/types";
import type { ClubMemberRole } from "@/api/types";
import { Button } from "@/components/ui/button";
import { stripHash } from "@/lib/tag";
import { formatNumber } from "@/lib/format";

const ROLE_INFO: Record<ClubMemberRole, { label: string; color: string } | null> = {
  PRESIDENT: { label: "회장", color: "#FFC60A" },
  VICE_PRESIDENT: { label: "부회장", color: "#B83BFF" },
  SENIOR: { label: "시니어", color: "#2E7CF6" },
  MEMBER: { label: "멤버", color: "#8A90A0" },
  UNKNOWN: null,
};

export function ClubMembers({ members }: { members: ClubMember[] }) {
  const router = useRouter();
  const goPlayer = (tag: string) => router.push(`/players/${stripHash(tag)}`);

  return (
    <section className="card">
      <header className="card-head">
        <div className="card-title">멤버 ({members.length})</div>
      </header>
      <div role="table" aria-label="클럽 멤버 목록">
        <div
          role="row"
          className="grid grid-cols-[40px_1fr_100px_120px_64px] gap-2 border-b bg-muted/30 px-4 py-2 text-[11px] font-bold uppercase tracking-[0.06em] text-muted-foreground"
        >
          <span role="columnheader">#</span>
          <span role="columnheader">이름</span>
          <span role="columnheader">역할</span>
          <span role="columnheader" className="text-right">트로피</span>
          <span role="columnheader" />
        </div>
        {members.map((m, i) => {
          const role = ROLE_INFO[m.role];
          return (
            <div
              key={m.tag}
              role="row"
              onClick={() => goPlayer(m.tag)}
              className="grid cursor-pointer grid-cols-[40px_1fr_100px_120px_64px] items-center gap-2 border-b px-4 py-2 hover:bg-muted/40"
            >
              <span className="num text-sm text-muted-foreground">{i + 1}</span>
              <div className="flex items-center gap-2.5 min-w-0">
                <div
                  aria-hidden
                  className="flex h-7 w-7 items-center justify-center rounded-full font-display text-xs font-extrabold text-white"
                  style={{ background: `hsl(${(i * 47) % 360} 60% 55%)` }}
                >
                  {m.name.slice(0, 1)}
                </div>
                <div className="min-w-0">
                  <div className="truncate text-sm font-bold">{m.name}</div>
                  <div className="num truncate text-[10px] text-muted-foreground">{m.tag}</div>
                </div>
              </div>
              {role ? (
                <span
                  className="inline-flex w-fit items-center gap-1 rounded px-2 py-0.5 text-[11px] font-bold"
                  style={{ background: role.color + "22", color: role.color }}
                >
                  <span
                    aria-hidden
                    className="h-1.5 w-1.5 rounded-full"
                    style={{ background: role.color }}
                  />
                  {role.label}
                </span>
              ) : (
                <span />
              )}
              <span className="num text-right text-sm font-extrabold">
                <img
                  src="/assets/trophy.png"
                  alt=""
                  className="mr-1 inline-block h-3 w-3 align-middle"
                />
                {formatNumber(m.trophies)}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  goPlayer(m.tag);
                }}
              >
                전적
              </Button>
            </div>
          );
        })}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: ClubTrophyDistribution**

```typescript
"use client";

import type { ClubMember } from "@/domain/club/types";
import { trophyDistribution } from "@/domain/club/members";

export function ClubTrophyDistribution({ members }: { members: ClubMember[] }) {
  const buckets = trophyDistribution(members);
  const total = members.length || 1;

  return (
    <section className="card">
      <header className="card-head">
        <div className="card-title">트로피 분포</div>
      </header>
      <div className="flex flex-col gap-1.5 p-3">
        {buckets.map((b, i) => {
          const pct = (b.count / total) * 100;
          return (
            <div key={b.label} className="flex items-center gap-2">
              <span className="w-12 text-[11px] text-muted-foreground">{b.label}</span>
              <div className="h-3 flex-1 overflow-hidden rounded-sm bg-muted">
                <div
                  className="h-full"
                  style={{
                    width: `${pct}%`,
                    background: i === 0 ? "var(--color-brand-yellow)" : "var(--muted-foreground)",
                  }}
                />
              </div>
              <span className="num w-7 text-right text-[11px]">{b.count}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
```

- [ ] **Step 5: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/clubs/[tag]/_components/
git commit -m "feat(frontend): club detail components (Content/Header/Members/Distribution)"
```

---

## Task 11: ClubSearchInput + rewrite ClubSearchPage

**Files:**
- Create: `app/clubs/_components/ClubSearchInput.tsx`
- Modify (rewrite): `app/clubs/_components/ClubSearchPage.tsx`
- Modify: `app/clubs/page.tsx`

- [ ] **Step 1: ClubSearchInput**

```typescript
"use client";

import { useState, type SubmitEvent } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { isValidTag } from "@/lib/tag";
import { routes } from "@/config/routes";
import { toast } from "sonner";

export function ClubSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);

  function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!isValidTag(trimmed)) {
      toast.error("올바른 클럽 태그를 입력해 주세요. 예: #2U8CVLQQ9");
      return;
    }
    router.push(routes.club(trimmed));
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-md items-center gap-2">
      <Input
        type="text"
        inputMode="text"
        autoCapitalize="characters"
        spellCheck={false}
        placeholder="#CLUB_TAG"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        aria-label="클럽 태그 입력"
      />
      <Button type="submit" size="default" aria-label="클럽 검색">
        <Search className="size-4" />
      </Button>
    </form>
  );
}
```

- [ ] **Step 2: Rewrite ClubSearchPage**

Replace the entire `app/clubs/_components/ClubSearchPage.tsx`:

```typescript
import { ClubSearchInput } from "./ClubSearchInput";

export default function ClubSearchPage() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-14">
      <div className="mb-6">
        <div className="mb-2 text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          CLUBS
        </div>
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          클럽 태그로 멤버 · 트로피 정보를 조회하세요.
        </p>
      </div>
      <ClubSearchInput />
    </main>
  );
}
```

- [ ] **Step 3: Update `app/clubs/page.tsx`**

Replace contents:

```typescript
import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import ClubSearchPage from "./_components/ClubSearchPage";

export const metadata: Metadata = buildMetadata({
  title: "클럽 검색 | ImStarGG",
  description: "브롤스타즈 클럽을 태그로 검색하고 멤버·트로피 정보를 확인하세요.",
  path: "/clubs",
});

export default function Page() {
  return <ClubSearchPage />;
}
```

- [ ] **Step 4: Typecheck**

Run: `pnpm typecheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/clubs/page.tsx app/clubs/_components/
git commit -m "feat(frontend): club search page with ClubSearchInput, drop mock recents"
```

---

## Task 12: Delete legacy ClubDetailPage

**Files:**
- Delete: `app/clubs/[tag]/_components/ClubDetailPage.tsx`

- [ ] **Step 1: Confirm no references remain**

Run: `pnpm exec rg -n "ClubDetailPage"`
Expected: only the file itself is matched.

- [ ] **Step 2: Delete**

```bash
git rm app/clubs/[tag]/_components/ClubDetailPage.tsx
```

- [ ] **Step 3: Typecheck + build**

Run: `pnpm typecheck && pnpm build`
Expected: both PASS.

- [ ] **Step 4: Commit**

```bash
git commit -m "chore(frontend): remove legacy ClubDetailPage mock"
```

---

## Task 13: Integration test for club renewal (MSW)

**Files:**
- Create: `tests/integration/club/clubRenewal.test.tsx`

- [ ] **Step 1: Write the test (mirror playerRenewal.test.tsx)**

```typescript
import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../setup/msw";
import { renderWithProviders } from "../../setup/renderWithProviders";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RenewalButton } from "@/components/shared/RenewalButton";

const TAG = "#2U8CVLQQ9";
const ENC = "%232U8CVLQQ9";

describe("RenewalButton (club) — 폴링 + 완료", () => {
  beforeEach(() => {
    let progressing = false;
    server.use(
      http.get(`*/v1/clubs/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: progressing ? "EXECUTING" : "COMPLETED",
          progressing,
          canRenew: !progressing,
          requestedAt: "2026-04-28T00:00:00Z",
          executedAt: null,
          completedAt: progressing ? null : "2026-04-28T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-28T00:00:01Z",
        }),
      ),
      http.post(`*/v1/clubs/${ENC}/renewal`, () => {
        progressing = true;
        setTimeout(() => {
          progressing = false;
        }, 1500);
        return HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-28T00:00:02Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        });
      }),
    );
  });

  it("갱신 클릭 → '갱신 요청됨' → 폴링 후 '갱신 완료' 토스트", async () => {
    renderWithProviders(<RenewalButton kind="club" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    expect(await screen.findByText(/갱신 요청됨/)).toBeInTheDocument();
    expect(
      await screen.findByText(/갱신 완료/, {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  });
});

describe("RenewalButton (club) — 첫 갱신 (renewal record 없음)", () => {
  beforeEach(() => {
    let progressing = false;
    let recordExists = false;
    server.use(
      http.get(`*/v1/clubs/${ENC}/renewal`, () => {
        if (!recordExists) return new HttpResponse(null, { status: 404 });
        return HttpResponse.json({
          tag: TAG,
          status: progressing ? "EXECUTING" : "COMPLETED",
          progressing,
          canRenew: !progressing,
          requestedAt: "2026-04-28T00:00:00Z",
          executedAt: null,
          completedAt: progressing ? null : "2026-04-28T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-28T00:00:01Z",
        });
      }),
      http.post(`*/v1/clubs/${ENC}/renewal`, () => {
        recordExists = true;
        progressing = true;
        setTimeout(() => {
          progressing = false;
        }, 1500);
        return HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-28T00:00:02Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        });
      }),
    );
  });

  it("기록이 없어도 버튼이 활성화되고 첫 갱신을 트리거할 수 있다", async () => {
    renderWithProviders(<RenewalButton kind="club" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    expect(await screen.findByText(/갱신 요청됨/)).toBeInTheDocument();
    expect(
      await screen.findByText(/갱신 완료/, {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test**

Run: `pnpm test:run tests/integration/club/clubRenewal.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 3: Commit**

```bash
git add tests/integration/club/clubRenewal.test.tsx
git commit -m "test(frontend): club renewal polling + first-renewal integration tests"
```

---

## Task 14: E2E Playwright spec

**Files:**
- Create: `tests/e2e/club.spec.ts`

- [ ] **Step 1: Write the test**

```typescript
import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("클럽 페이지가 정상 데이터로 렌더된다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  await expect(
    page.getByText(TEST_TAGS.club, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/멤버/).first()).toBeVisible();
});

test("멤버 행 클릭 시 플레이어 페이지로 이동한다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  const firstMemberRow = page.getByRole("row").nth(1); // [0] is header
  await firstMemberRow.waitFor({ state: "visible", timeout: 15_000 });
  await firstMemberRow.click();
  await page.waitForURL(/\/players\//, { timeout: 15_000 });
  expect(page.url()).toContain("/players/");
});

test("SEO 메타데이터: title에 클럽 태그가 포함되고 canonical 태그가 존재한다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  await expect(page).toHaveTitle(new RegExp(TEST_TAGS.club.replace("#", "#?")), {
    timeout: 15_000,
  });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});

test.skip("갱신 버튼 클릭 시 토스트 → 폴링 → 완료 토스트 (CORS 배포 후 활성화)", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  const renewBtn = page.getByRole("button", { name: "클럽 정보 갱신" });
  await renewBtn.waitFor({ state: "visible", timeout: 15_000 });
  await expect(renewBtn).toBeEnabled({ timeout: 15_000 });
  await renewBtn.click();
  await expect(page.getByText(/갱신 요청됨/)).toBeVisible({ timeout: 5_000 });
  await expect(page.getByText(/갱신 완료/)).toBeVisible({ timeout: 60_000 });
});
```

- [ ] **Step 2: Run E2E**

Run: `pnpm test:e2e tests/e2e/club.spec.ts`
Expected: 3 tests PASS, 1 skipped.

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/club.spec.ts
git commit -m "test(frontend): e2e club page render, member nav, SEO"
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

Expected: all PASS.

- [ ] **Step 2: Manual smoke (optional but recommended)**

Run: `pnpm dev`
Visit `http://localhost:3000/clubs`, search `2U8CVLQQ9`, verify the detail page renders, click a member row, verify navigation.

- [ ] **Step 3: No additional commit needed**

If everything passes the previous commits stand. If lint or typecheck flags issues, fix them inline and commit:

```bash
git add -A
git commit -m "fix(frontend): final lint/type cleanup"
```
