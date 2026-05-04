# Frontend Image Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** imstargg-frontend 의 이미지 사용을 단일 helper + primitive 컴포넌트로 일관화. 트로피·파워레벨·프레스티지·솔로랭크·아이템(가젯/스타파워/기어/하이퍼차지/버피) 시각 표현을 신규 primitive 로 분리하고 PlayerHero/PlayerBrawlerCard/PlayerBrawlers 를 그에 맞춰 재구성.

**Architecture:** 자산 ↔ 데이터 매핑은 단일 헬퍼 (`src/lib/itemPlaceholder.ts`) 에 모으고, 시각 표현은 6 개 신규/확장 primitive 로 분리. 사용처는 primitive 호출부만 갱신.

**Tech Stack:** Next.js App Router, TanStack Query, shadcn/ui, lucide-react, Vitest + Testing Library.

**Spec:** `docs/superpowers/specs/2026-05-04-frontend-image-improvements-design.md`

---

## File Structure

**신규 파일** (8)

- `imstargg-frontend/src/lib/itemPlaceholder.ts`
- `imstargg-frontend/src/components/primitives/PowerLevel.tsx`
- `imstargg-frontend/src/components/primitives/PrestigeBadge.tsx`
- `imstargg-frontend/src/components/primitives/PlayerPrestigeBadge.tsx`
- `imstargg-frontend/src/components/primitives/RankTierIcon.tsx`
- `imstargg-frontend/src/components/primitives/ItemSlot.tsx`
- `imstargg-frontend/src/domain/player/sortPlayerBrawlers.ts`
- `imstargg-frontend/tests/unit/...` 단위 테스트 (각 파일별)

**수정 파일** (10)

- `imstargg-frontend/src/api/types.ts`
- `imstargg-frontend/src/domain/brawler/types.ts`
- `imstargg-frontend/src/domain/brawler/mappers.ts`
- `imstargg-frontend/src/domain/battle/tierLabels.ts`
- `imstargg-frontend/src/components/primitives/Trophy.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx`
- `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`

**삭제 파일** (1)

- `imstargg-frontend/src/components/primitives/PowerPill.tsx`

---

## Task 1: 자산 매핑 헬퍼 (`itemPlaceholder.ts`)

**Files:**

- Create: `imstargg-frontend/src/lib/itemPlaceholder.ts`
- Create: `imstargg-frontend/tests/unit/lib/itemPlaceholder.test.ts`
- Modify: `imstargg-frontend/src/api/types.ts` (add `GearRarity` export)

- [ ] **Step 1: `GearRarity` 타입 추가 (api/types.ts)**

`imstargg-frontend/src/api/types.ts` 의 `BrawlerRarity` 정의 직후에 추가:

```ts
export type GearRarity = "SUPER_RARE" | "EPIC" | "MYTHIC";
```

- [ ] **Step 2: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/lib/itemPlaceholder.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { basePlaceholder, emptyPlaceholder } from "@/lib/itemPlaceholder";

describe("basePlaceholder", () => {
  it("gadget 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("gadget")).toBe("/assets/gadget_base_empty.webp");
  });

  it("starPower 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("starPower")).toBe("/assets/starpower_base01_empty.webp");
  });

  it("hyperCharge 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("hyperCharge")).toBe("/assets/hypercharge_base_empty.webp");
  });

  it("gear 는 rarity 에 따라 다른 자산을 반환한다", () => {
    expect(basePlaceholder("gear", "SUPER_RARE")).toBe("/assets/gear_base_empty.webp");
    expect(basePlaceholder("gear", "EPIC")).toBe("/assets/gear_epic_super_base_empty.webp");
    expect(basePlaceholder("gear", "MYTHIC")).toBe("/assets/gear_mythic_super_base_empty.webp");
  });

  it("gear rarity 가 없으면 일반 base 를 반환한다", () => {
    expect(basePlaceholder("gear")).toBe("/assets/gear_base_empty.webp");
  });

  it("buffy 는 자산이 없으므로 null 을 반환한다", () => {
    expect(basePlaceholder("buffy")).toBeNull();
  });
});

describe("emptyPlaceholder", () => {
  it("gadget/gear/starPower 의 empty placeholder 를 반환한다", () => {
    expect(emptyPlaceholder("gadget")).toBe("/assets/gadget_empty.webp");
    expect(emptyPlaceholder("gear")).toBe("/assets/gear_empty.webp");
    expect(emptyPlaceholder("starPower")).toBe("/assets/starpower_empty.webp");
  });

  it("hyperCharge 와 buffy 는 empty 자산이 없으므로 null 을 반환한다", () => {
    expect(emptyPlaceholder("hyperCharge")).toBeNull();
    expect(emptyPlaceholder("buffy")).toBeNull();
  });
});
```

- [ ] **Step 3: 테스트가 실패하는지 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/unit/lib/itemPlaceholder.test.ts`
Expected: FAIL — `Cannot find module '@/lib/itemPlaceholder'`

- [ ] **Step 4: helper 구현**

Create `imstargg-frontend/src/lib/itemPlaceholder.ts`:

```ts
import type { GearRarity } from "@/api/types";

export type ItemKind = "gadget" | "gear" | "starPower" | "hyperCharge" | "buffy";

export function basePlaceholder(kind: ItemKind, gearRarity?: GearRarity): string | null {
  switch (kind) {
    case "gadget":
      return "/assets/gadget_base_empty.webp";
    case "starPower":
      return "/assets/starpower_base01_empty.webp";
    case "hyperCharge":
      return "/assets/hypercharge_base_empty.webp";
    case "gear":
      if (gearRarity === "MYTHIC") return "/assets/gear_mythic_super_base_empty.webp";
      if (gearRarity === "EPIC") return "/assets/gear_epic_super_base_empty.webp";
      return "/assets/gear_base_empty.webp";
    case "buffy":
      return null;
  }
}

export function emptyPlaceholder(kind: ItemKind): string | null {
  switch (kind) {
    case "gadget":
      return "/assets/gadget_empty.webp";
    case "gear":
      return "/assets/gear_empty.webp";
    case "starPower":
      return "/assets/starpower_empty.webp";
    case "hyperCharge":
    case "buffy":
      return null;
  }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `pnpm test:run tests/unit/lib/itemPlaceholder.test.ts`
Expected: PASS (8 tests)

- [ ] **Step 6: typecheck + lint**

Run: `pnpm typecheck && pnpm lint`
Expected: 0 errors

- [ ] **Step 7: 커밋**

```bash
git add imstargg-frontend/src/lib/itemPlaceholder.ts imstargg-frontend/tests/unit/lib/itemPlaceholder.test.ts imstargg-frontend/src/api/types.ts
git commit -m "feat(frontend): itemPlaceholder helper + GearRarity 타입"
```

---

## Task 2: SoloRankTier enum 동기화 + tierLabels 재구성

**Files:**

- Modify: `imstargg-frontend/src/api/types.ts`
- Modify: `imstargg-frontend/src/domain/battle/tierLabels.ts`
- Create: `imstargg-frontend/tests/unit/domain/battle/tierLabels.test.ts`

- [ ] **Step 1: `SoloRankTier` 동기화**

`imstargg-frontend/src/api/types.ts` 의 기존 `SoloRankTier` (~line 325) 를 다음과 같이 교체:

```ts
export type SoloRankTier =
  | "BRONZE_1" | "BRONZE_2" | "BRONZE_3"
  | "SILVER_1" | "SILVER_2" | "SILVER_3"
  | "GOLD_1" | "GOLD_2" | "GOLD_3"
  | "DIAMOND_1" | "DIAMOND_2" | "DIAMOND_3"
  | "MYTHIC_1" | "MYTHIC_2" | "MYTHIC_3"
  | "LEGENDARY_1" | "LEGENDARY_2" | "LEGENDARY_3"
  | "MASTER_1" | "MASTER_2" | "MASTER_3"
  | "PRO";
```

- [ ] **Step 2: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/domain/battle/tierLabels.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { tierColor, tierStickerImage, tierSegmentNumber } from "@/domain/battle/tierLabels";

describe("tierStickerImage", () => {
  it("base tier 별로 단일 자산을 반환한다 (1/2/3 segment 무관)", () => {
    expect(tierStickerImage("BRONZE_1")).toBe("/assets/icon_rank_sticker_bronze.webp");
    expect(tierStickerImage("BRONZE_3")).toBe("/assets/icon_rank_sticker_bronze.webp");
    expect(tierStickerImage("SILVER_2")).toBe("/assets/icon_rank_sticker_silver.webp");
    expect(tierStickerImage("GOLD_2")).toBe("/assets/icon_rank_sticker_gold.webp");
    expect(tierStickerImage("DIAMOND_2")).toBe("/assets/icon_rank_sticker_diamond.webp");
    expect(tierStickerImage("MYTHIC_2")).toBe("/assets/icon_rank_sticker_mythic.webp");
    expect(tierStickerImage("LEGENDARY_2")).toBe("/assets/icon_rank_sticker_legendary.webp");
    expect(tierStickerImage("MASTER_2")).toBe("/assets/icon_rank_sticker_masters.webp");
    expect(tierStickerImage("PRO")).toBe("/assets/icon_rank_sticker_pro.webp");
  });
});

describe("tierSegmentNumber", () => {
  it("BRONZE_2 는 '2' 를 반환한다", () => {
    expect(tierSegmentNumber("BRONZE_2")).toBe("2");
  });

  it("MASTER_3 는 '3' 을 반환한다", () => {
    expect(tierSegmentNumber("MASTER_3")).toBe("3");
  });

  it("PRO 는 단계가 없으므로 null 을 반환한다", () => {
    expect(tierSegmentNumber("PRO")).toBeNull();
  });
});

describe("tierColor", () => {
  it("기존 색 매핑을 유지한다", () => {
    expect(tierColor("BRONZE_1")).toBe("#C77545");
    expect(tierColor("MYTHIC_3")).toBe("#B83BFF");
    expect(tierColor("LEGENDARY_2")).toBe("#FF3A3A");
  });

  it("MASTER 와 PRO 는 같은 옐로우 톤을 사용한다", () => {
    expect(tierColor("MASTER_1")).toBe("#FFD447");
    expect(tierColor("PRO")).toBe("#FFD447");
  });
});
```

- [ ] **Step 3: tierLabels.ts 재구성**

`imstargg-frontend/src/domain/battle/tierLabels.ts` 전체를 다음으로 교체:

```ts
import type { SoloRankTier } from "@/api/types";

const TIER_BASE_COLOR: Record<string, string> = {
  BRONZE: "#C77545",
  SILVER: "#9FAFC5",
  GOLD: "#F2B93B",
  DIAMOND: "#58D7E8",
  MYTHIC: "#B83BFF",
  LEGENDARY: "#FF3A3A",
  MASTER: "#FFD447",
  PRO: "#FFD447",
};

const TIER_BASE_STICKER: Record<string, string> = {
  BRONZE: "/assets/icon_rank_sticker_bronze.webp",
  SILVER: "/assets/icon_rank_sticker_silver.webp",
  GOLD: "/assets/icon_rank_sticker_gold.webp",
  DIAMOND: "/assets/icon_rank_sticker_diamond.webp",
  MYTHIC: "/assets/icon_rank_sticker_mythic.webp",
  LEGENDARY: "/assets/icon_rank_sticker_legendary.webp",
  MASTER: "/assets/icon_rank_sticker_masters.webp",
  PRO: "/assets/icon_rank_sticker_pro.webp",
};

function tierBase(tier: SoloRankTier): string {
  if (tier === "PRO") return "PRO";
  return tier.split("_")[0];
}

export function tierColor(tier: SoloRankTier): string {
  return TIER_BASE_COLOR[tierBase(tier)];
}

export function tierStickerImage(tier: SoloRankTier): string {
  return TIER_BASE_STICKER[tierBase(tier)];
}

export function tierSegmentNumber(tier: SoloRankTier): string | null {
  if (tier === "PRO") return null;
  const seg = tier.split("_")[1];
  return seg ?? null;
}
```

기존 `tierLabel()` 은 RankTierIcon 도입 시 호출처 (`BattlePlayerRow`) 가 사라지므로 삭제. (Task 8 에서 `BattlePlayerRow` 변경 시 import 제거)

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm test:run tests/unit/domain/battle/tierLabels.test.ts`
Expected: PASS

- [ ] **Step 5: typecheck**

Run: `pnpm typecheck`
Expected: `BattlePlayerRow` 가 `tierLabel` 을 import 하던 줄에서 에러 발생 가능. 일단 통과시키기 위해 `BattlePlayerRow` 의 `tierLabel` 호출은 Task 8 에서 처리 — 임시로 `tierLabel` 을 export 유지 (호환 alias):

`tierLabels.ts` 끝에 임시 추가:

```ts
/** @deprecated Task 8 에서 RankTierIcon 으로 대체 */
export function tierLabel(tier: SoloRankTier): string {
  return tierSegmentNumber(tier) ?? "";
}
```

`pnpm typecheck` 다시 실행: 0 errors.

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/api/types.ts imstargg-frontend/src/domain/battle/tierLabels.ts imstargg-frontend/tests/unit/domain/battle/tierLabels.test.ts
git commit -m "feat(frontend): SoloRankTier 동기화 + tierLabels 재구성"
```

---

## Task 3: GearResponse rarity 추가 + brawler 도메인 보강

**Files:**

- Modify: `imstargg-frontend/src/api/types.ts`
- Modify: `imstargg-frontend/src/domain/brawler/types.ts`
- Modify: `imstargg-frontend/src/domain/brawler/mappers.ts`
- Modify: `imstargg-frontend/tests/unit/domain/brawler/mappers.test.ts`

- [ ] **Step 1: `GearResponse` 보강**

`imstargg-frontend/src/api/types.ts` line ~59 의 `GearResponse` 를 다음으로 교체:

```ts
export interface GearResponse { id: number; name: MessagesResponse; rarity: GearRarity; imageUrl: string | null; }
```

- [ ] **Step 2: `BrawlerItem` 도메인 타입 보강**

`imstargg-frontend/src/domain/brawler/types.ts` 를 다음으로 교체:

```ts
import type { BrawlerRarity, BrawlerRole, BuffyResponse, GearRarity } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
  rarity: GearRarity | null;
}

// (이후 인터페이스는 그대로 유지)
```

- [ ] **Step 3: `toBrawlerItem` mapper 분기**

`imstargg-frontend/src/domain/brawler/mappers.ts` 의 `toBrawlerItem` 을 다음으로 교체:

```ts
export function toBrawlerItem(dto: ItemDto): BrawlerItem {
  return {
    id: dto.id,
    name: pickName(dto.name),
    imageUrl: dto.imageUrl,
    rarity: "rarity" in dto ? dto.rarity : null,
  };
}
```

- [ ] **Step 4: 기존 mapper 테스트 보강**

`imstargg-frontend/tests/unit/domain/brawler/mappers.test.ts` 의 gear toBrawlerItem 픽스처에 `rarity: "EPIC"` 등 추가하고, 다음 테스트 추가:

```ts
it("gear DTO 의 rarity 를 패스스루한다", () => {
  const dto = { id: 1, name: { ko: "기어" }, rarity: "EPIC" as const, imageUrl: null };
  expect(toBrawlerItem(dto).rarity).toBe("EPIC");
});

it("gear 외 아이템은 rarity 가 null 이다", () => {
  const dto = { id: 1, name: { ko: "가젯" }, imageUrl: null };
  expect(toBrawlerItem(dto).rarity).toBeNull();
});
```

- [ ] **Step 5: typecheck + 테스트**

Run: `pnpm typecheck && pnpm test:run tests/unit/domain/brawler/mappers.test.ts`
Expected: PASS. 만약 다른 컴포넌트에서 `BrawlerItem` 의 새 `rarity` 필드 누락으로 type error 발생하면 해당 사용처 수정 (테스트 fixture 등).

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "feat(frontend): GearResponse 에 rarity 필드 + BrawlerItem 매핑 보강"
```

---

## Task 4: Trophy primitive 확장

**Files:**

- Modify: `imstargg-frontend/src/components/primitives/Trophy.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/Trophy.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/components/primitives/Trophy.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy 컴포넌트", () => {
  it("1000 미만은 기본 trophy 아이콘을 사용한다", () => {
    render(<Trophy value={999} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/icon_trophy.png");
  });

  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다", () => {
    render(<Trophy value={1000} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });

  it("3000 이상은 prestige_trophy 를 사용한다", () => {
    render(<Trophy value={3000} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/icon_prestige_trophy.webp");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm test:run tests/unit/components/primitives/Trophy.test.tsx`
Expected: FAIL (현재 항상 `icon_trophy.png` 만 반환)

- [ ] **Step 3: Trophy 확장**

`imstargg-frontend/src/components/primitives/Trophy.tsx` 전체를 다음으로 교체:

```tsx
import { formatNumber } from "@/lib/format";

interface Props {
  value: number;
  size?: number;
}

function trophyIcon(value: number): string {
  if (value >= 3000) return "/assets/icon_prestige_trophy.webp";
  if (value >= 1000) return "/assets/icon_prestige_trophy_2.webp";
  return "/assets/icon_trophy.png";
}

export default function Trophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src={trophyIcon(value)} alt="" style={{ width: size, height: size }} />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
```

- [ ] **Step 4: 테스트 통과 + typecheck**

Run: `pnpm test:run tests/unit/components/primitives/Trophy.test.tsx && pnpm typecheck`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/src/components/primitives/Trophy.tsx imstargg-frontend/tests/unit/components/primitives/Trophy.test.tsx
git commit -m "feat(frontend): Trophy 컴포넌트 임계값별 아이콘 자동 전환"
```

---

## Task 5: PowerLevel primitive 신규

**Files:**

- Create: `imstargg-frontend/src/components/primitives/PowerLevel.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/PowerLevel.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/components/primitives/PowerLevel.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PowerLevel from "@/components/primitives/PowerLevel";

describe("PowerLevel", () => {
  it("base SVG 위에 숫자를 표시한다", () => {
    render(<PowerLevel level={11} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/power_level_base_empty.svg");
    expect(screen.getByLabelText("파워 레벨 11")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 컴포넌트 구현**

Create `imstargg-frontend/src/components/primitives/PowerLevel.tsx`:

```tsx
interface Props {
  level: number;
  size?: number;
}

export default function PowerLevel({ level, size = 22 }: Props) {
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src="/assets/power_level_base_empty.svg" alt="" width={size} height={size} />
      <span
        aria-label={`파워 레벨 ${level}`}
        style={{
          position: "absolute",
          inset: 0,
          display: "grid",
          placeItems: "center",
          color: "#FFFFFF",
          fontFamily: "var(--font-num)",
          fontWeight: 800,
          fontSize: size * 0.55,
          textShadow: "0 1px 2px rgba(0,0,0,0.4)",
        }}
      >
        {level}
      </span>
    </span>
  );
}
```

- [ ] **Step 3: 테스트 통과 + 커밋**

```bash
pnpm test:run tests/unit/components/primitives/PowerLevel.test.tsx
git add imstargg-frontend/src/components/primitives/PowerLevel.tsx imstargg-frontend/tests/unit/components/primitives/PowerLevel.test.tsx
git commit -m "feat(frontend): PowerLevel primitive (SVG + 흰색 숫자)"
```

---

## Task 6: PrestigeBadge + PlayerPrestigeBadge primitives

**Files:**

- Create: `imstargg-frontend/src/components/primitives/PrestigeBadge.tsx`
- Create: `imstargg-frontend/src/components/primitives/PlayerPrestigeBadge.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/PrestigeBadge.test.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/PlayerPrestigeBadge.test.tsx`

- [ ] **Step 1: PrestigeBadge 실패 테스트**

Create `imstargg-frontend/tests/unit/components/primitives/PrestigeBadge.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PrestigeBadge from "@/components/primitives/PrestigeBadge";

describe("PrestigeBadge", () => {
  it.each([
    [0, "/assets/icon_trophy_brawler_wood.webp", null],
    [249, "/assets/icon_trophy_brawler_wood.webp", null],
    [250, "/assets/icon_trophy_brawler_bronze.webp", null],
    [500, "/assets/icon_trophy_brawler_silver.webp", null],
    [750, "/assets/icon_trophy_brawler_gold.webp", null],
    [1000, "/assets/icon_trophy_brawler_prestige_1.webp", "1"],
    [2000, "/assets/icon_trophy_brawler_prestige_2.webp", "2"],
    [3000, "/assets/icon_trophy_brawler_prestige_3.webp", "3"],
  ])("trophies=%d 에서 올바른 아이콘과 오버레이를 렌더한다", (trophies, src, overlay) => {
    render(<PrestigeBadge trophies={trophies} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", src);
    if (overlay) {
      expect(screen.getByText(overlay)).toBeInTheDocument();
    }
  });
});
```

- [ ] **Step 2: PrestigeBadge 구현**

Create `imstargg-frontend/src/components/primitives/PrestigeBadge.tsx`:

```tsx
interface Props {
  trophies: number;
  size?: number;
}

const STEPS = [
  { min: 3000, src: "/assets/icon_trophy_brawler_prestige_3.webp", overlay: "3" },
  { min: 2000, src: "/assets/icon_trophy_brawler_prestige_2.webp", overlay: "2" },
  { min: 1000, src: "/assets/icon_trophy_brawler_prestige_1.webp", overlay: "1" },
  { min: 750, src: "/assets/icon_trophy_brawler_gold.webp", overlay: null as string | null },
  { min: 500, src: "/assets/icon_trophy_brawler_silver.webp", overlay: null },
  { min: 250, src: "/assets/icon_trophy_brawler_bronze.webp", overlay: null },
  { min: 0, src: "/assets/icon_trophy_brawler_wood.webp", overlay: null },
];

export default function PrestigeBadge({ trophies, size = 18 }: Props) {
  const step = STEPS.find((s) => trophies >= s.min)!;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src={step.src} alt="" width={size} height={size} />
      {step.overlay && (
        <span
          style={{
            position: "absolute",
            inset: 0,
            display: "grid",
            placeItems: "center",
            color: "#FFFFFF",
            fontFamily: "var(--font-num)",
            fontWeight: 800,
            fontSize: size * 0.55,
            textShadow: "0 1px 2px rgba(0,0,0,0.5)",
          }}
        >
          {step.overlay}
        </span>
      )}
    </span>
  );
}
```

- [ ] **Step 3: PlayerPrestigeBadge 테스트**

Create `imstargg-frontend/tests/unit/components/primitives/PlayerPrestigeBadge.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import PlayerPrestigeBadge from "@/components/primitives/PlayerPrestigeBadge";

describe("PlayerPrestigeBadge", () => {
  it("level=0 이면 아무것도 렌더하지 않는다", () => {
    const { container } = render(<PlayerPrestigeBadge level={0} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("level>=1 이면 prestige 아이콘 + 숫자를 렌더한다", () => {
    render(<PlayerPrestigeBadge level={5} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_total_prestige.webp",
    );
    expect(screen.getByLabelText("프레스티지 5")).toBeInTheDocument();
  });
});
```

- [ ] **Step 4: PlayerPrestigeBadge 구현**

Create `imstargg-frontend/src/components/primitives/PlayerPrestigeBadge.tsx`:

```tsx
interface Props {
  level: number;
  size?: number;
}

export default function PlayerPrestigeBadge({ level, size = 22 }: Props) {
  if (level < 1) return null;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src="/assets/icon_total_prestige.webp" alt="" width={size} height={size} />
      <span
        aria-label={`프레스티지 ${level}`}
        style={{
          position: "absolute",
          inset: 0,
          display: "grid",
          placeItems: "center",
          color: "#FFFFFF",
          fontFamily: "var(--font-num)",
          fontWeight: 800,
          fontSize: size * 0.5,
          textShadow: "0 1px 2px rgba(0,0,0,0.5)",
        }}
      >
        {level}
      </span>
    </span>
  );
}
```

- [ ] **Step 5: 테스트 통과 + 커밋**

```bash
pnpm test:run tests/unit/components/primitives/
git add imstargg-frontend/src/components/primitives/PrestigeBadge.tsx imstargg-frontend/src/components/primitives/PlayerPrestigeBadge.tsx imstargg-frontend/tests/unit/components/primitives/PrestigeBadge.test.tsx imstargg-frontend/tests/unit/components/primitives/PlayerPrestigeBadge.test.tsx
git commit -m "feat(frontend): PrestigeBadge + PlayerPrestigeBadge primitives"
```

---

## Task 7: RankTierIcon primitive

**Files:**

- Create: `imstargg-frontend/src/components/primitives/RankTierIcon.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/RankTierIcon.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/components/primitives/RankTierIcon.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import RankTierIcon from "@/components/primitives/RankTierIcon";

describe("RankTierIcon", () => {
  it("BRONZE_2 는 bronze 스티커 + '2' 를 렌더한다", () => {
    render(<RankTierIcon tier="BRONZE_2" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_rank_sticker_bronze.webp",
    );
    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("PRO 는 pro 스티커 + 숫자 없음", () => {
    render(<RankTierIcon tier="PRO" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/icon_rank_sticker_pro.webp",
    );
    expect(screen.queryByText(/\d/)).toBeNull();
  });
});
```

- [ ] **Step 2: 컴포넌트 구현**

Create `imstargg-frontend/src/components/primitives/RankTierIcon.tsx`:

```tsx
import type { SoloRankTier } from "@/api/types";
import { tierColor, tierStickerImage, tierSegmentNumber } from "@/domain/battle/tierLabels";

interface Props {
  tier: SoloRankTier;
  size?: number;
}

export default function RankTierIcon({ tier, size = 18 }: Props) {
  const segment = tierSegmentNumber(tier);
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src={tierStickerImage(tier)} alt="" width={size} height={size} />
      {segment && (
        <span
          style={{
            position: "absolute",
            inset: 0,
            display: "grid",
            placeItems: "center",
            color: tierColor(tier),
            fontFamily: "var(--font-num)",
            fontWeight: 800,
            fontSize: size * 0.45,
          }}
        >
          {segment}
        </span>
      )}
    </span>
  );
}
```

- [ ] **Step 3: 테스트 통과 + 커밋**

```bash
pnpm test:run tests/unit/components/primitives/RankTierIcon.test.tsx
git add imstargg-frontend/src/components/primitives/RankTierIcon.tsx imstargg-frontend/tests/unit/components/primitives/RankTierIcon.test.tsx
git commit -m "feat(frontend): RankTierIcon primitive"
```

---

## Task 8: ItemSlot primitive

**Files:**

- Create: `imstargg-frontend/src/components/primitives/ItemSlot.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/ItemSlot.test.tsx`

- [ ] **Step 1: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/components/primitives/ItemSlot.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import ItemSlot from "@/components/primitives/ItemSlot";

describe("ItemSlot", () => {
  it("owned + imageUrl 이 있으면 해당 이미지를 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl="https://cdn/x.webp" owned />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "https://cdn/x.webp");
  });

  it("owned + imageUrl null 이면 base placeholder 를 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl={null} owned />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/gadget_base_empty.webp",
    );
  });

  it("미보유 + empty 자산이 있으면 empty placeholder 를 렌더한다", () => {
    render(<ItemSlot kind="gadget" imageUrl={null} owned={false} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/gadget_empty.webp",
    );
  });

  it("hyperCharge 미보유는 base 를 흐리게 렌더한다", () => {
    render(<ItemSlot kind="hyperCharge" imageUrl={null} owned={false} />);
    const img = screen.getByRole("img", { hidden: true });
    expect(img).toHaveAttribute("src", "/assets/hypercharge_base_empty.webp");
    expect(img.style.opacity).toBe("0.3");
  });

  it("buffy 는 imageUrl 이 없으면 lucide 아이콘 fallback 을 렌더한다", () => {
    render(<ItemSlot kind="buffy" imageUrl={null} owned ariaLabel="버피 가젯" />);
    expect(screen.getByLabelText("버피 가젯")).toBeInTheDocument();
    expect(screen.queryByRole("img", { hidden: true })).toBeNull();
  });

  it("gear EPIC 의 base placeholder 는 epic_super 자산이다", () => {
    render(<ItemSlot kind="gear" imageUrl={null} owned gearRarity="EPIC" />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute(
      "src",
      "/assets/gear_epic_super_base_empty.webp",
    );
  });
});
```

- [ ] **Step 2: 컴포넌트 구현**

Create `imstargg-frontend/src/components/primitives/ItemSlot.tsx`:

```tsx
import { Zap } from "lucide-react";
import type { GearRarity } from "@/api/types";
import { basePlaceholder, emptyPlaceholder, type ItemKind } from "@/lib/itemPlaceholder";

interface Props {
  kind: ItemKind;
  imageUrl: string | null;
  owned: boolean;
  gearRarity?: GearRarity;
  size?: number;
  ariaLabel?: string;
}

export default function ItemSlot({
  kind,
  imageUrl,
  owned,
  gearRarity,
  size = 24,
  ariaLabel,
}: Props) {
  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        aria-label={ariaLabel}
        style={{
          width: size,
          height: size,
          display: "grid",
          placeItems: "center",
          opacity: owned ? 1 : 0.3,
          color: owned ? "#14161B" : "#8A90A0",
        }}
      >
        <Zap size={size * 0.7} strokeWidth={2.5} />
      </span>
    );
  }

  if (owned) {
    const src = imageUrl ?? basePlaceholder(kind, gearRarity);
    return <img src={src ?? ""} alt="" width={size} height={size} aria-label={ariaLabel} />;
  }

  const emptySrc = emptyPlaceholder(kind);
  if (emptySrc) {
    return <img src={emptySrc} alt="" width={size} height={size} aria-label={ariaLabel} />;
  }
  // hyperCharge: empty 자산 부재 → base 를 흐리게
  const baseSrc = imageUrl ?? basePlaceholder(kind, gearRarity);
  return (
    <img
      src={baseSrc ?? ""}
      alt=""
      width={size}
      height={size}
      style={{ opacity: 0.3, filter: "grayscale(1)" }}
      aria-label={ariaLabel}
    />
  );
}
```

- [ ] **Step 3: 테스트 통과 + 커밋**

```bash
pnpm test:run tests/unit/components/primitives/ItemSlot.test.tsx
git add imstargg-frontend/src/components/primitives/ItemSlot.tsx imstargg-frontend/tests/unit/components/primitives/ItemSlot.test.tsx
git commit -m "feat(frontend): ItemSlot primitive (자산 매트릭스 결정 트리)"
```

---

## Task 9: sortPlayerBrawlers + 단위 테스트

**Files:**

- Create: `imstargg-frontend/src/domain/player/sortPlayerBrawlers.ts`
- Create: `imstargg-frontend/tests/unit/domain/player/sortPlayerBrawlers.test.ts`

- [ ] **Step 1: 실패 테스트 작성**

Create `imstargg-frontend/tests/unit/domain/player/sortPlayerBrawlers.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { sortPlayerBrawlers } from "@/domain/player/sortPlayerBrawlers";
import type { PlayerBrawler } from "@/domain/player/types";

function pb(overrides: Partial<PlayerBrawler> & { id?: number }): PlayerBrawler {
  const { id = 0, ...rest } = overrides;
  return {
    brawler: rest.brawler ?? {
      id,
      name: `Brawler ${id}`,
      rarity: "RARE",
      role: "DAMAGE_DEALER",
      portraitImageUrl: null,
    },
    power: 1,
    rank: 1,
    trophies: 0,
    highestTrophies: 0,
    prestigeLevel: 0,
    gadgets: [],
    gears: [],
    starPowers: [],
    hyperCharge: null,
    buffies: null,
    ...rest,
  } as PlayerBrawler;
}

describe("sortPlayerBrawlers", () => {
  it("trophies desc 로 정렬한다", () => {
    const list = [pb({ id: 1, trophies: 100 }), pb({ id: 2, trophies: 500 })];
    const sorted = sortPlayerBrawlers(list, "trophies", "desc");
    expect(sorted.map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("trophies asc 로 정렬한다", () => {
    const list = [pb({ id: 1, trophies: 500 }), pb({ id: 2, trophies: 100 })];
    expect(sortPlayerBrawlers(list, "trophies", "asc").map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("희귀도 순으로 정렬한다", () => {
    const list = [
      pb({ id: 1, brawler: { id: 1, name: "a", rarity: "LEGENDARY", role: "TANK", portraitImageUrl: null } }),
      pb({ id: 2, brawler: { id: 2, name: "b", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
    ];
    expect(sortPlayerBrawlers(list, "rarity", "desc").map((p) => p.brawler!.id)).toEqual([1, 2]);
  });

  it("이름 asc 로 정렬한다", () => {
    const list = [
      pb({ id: 1, brawler: { id: 1, name: "쉘리", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
      pb({ id: 2, brawler: { id: 2, name: "콜트", rarity: "RARE", role: "TANK", portraitImageUrl: null } }),
    ];
    expect(sortPlayerBrawlers(list, "name", "asc").map((p) => p.brawler!.id)).toEqual([1, 2]);
  });

  it("최신순 (id desc) 로 정렬한다", () => {
    const list = [pb({ id: 1 }), pb({ id: 2 })];
    expect(sortPlayerBrawlers(list, "id", "desc").map((p) => p.brawler!.id)).toEqual([2, 1]);
  });

  it("brawler 가 null 인 항목도 안전하게 처리한다", () => {
    const list = [pb({ id: 1, trophies: 100 }), { ...pb({ id: 2 }), brawler: null }];
    expect(() => sortPlayerBrawlers(list, "name", "asc")).not.toThrow();
  });
});
```

- [ ] **Step 2: 정렬 함수 구현**

Create `imstargg-frontend/src/domain/player/sortPlayerBrawlers.ts`:

```ts
import type { BrawlerRarity } from "@/api/types";
import type { PlayerBrawler } from "./types";

export type PlayerBrawlerSortKey = "trophies" | "power" | "rarity" | "name" | "id";
export type SortDirection = "asc" | "desc";

const RARITY_ORDER: Record<BrawlerRarity, number> = {
  STARTING_BRAWLER: 0,
  RARE: 1,
  SUPER_RARE: 2,
  EPIC: 3,
  MYTHIC: 4,
  LEGENDARY: 5,
  ULTRA_LEGENDARY: 6,
};

function pickValue(pb: PlayerBrawler, key: PlayerBrawlerSortKey): number | string {
  switch (key) {
    case "trophies":
      return pb.trophies;
    case "power":
      return pb.power;
    case "rarity":
      return pb.brawler ? RARITY_ORDER[pb.brawler.rarity] : -1;
    case "name":
      return pb.brawler?.name ?? "";
    case "id":
      return pb.brawler?.id ?? 0;
  }
}

export function sortPlayerBrawlers(
  brawlers: PlayerBrawler[],
  key: PlayerBrawlerSortKey,
  direction: SortDirection,
): PlayerBrawler[] {
  const sign = direction === "asc" ? 1 : -1;
  return [...brawlers].sort((a, b) => {
    const av = pickValue(a, key);
    const bv = pickValue(b, key);
    if (typeof av === "string" && typeof bv === "string") {
      return sign * av.localeCompare(bv);
    }
    return sign * ((av as number) - (bv as number));
  });
}
```

- [ ] **Step 3: 테스트 통과 + 커밋**

```bash
pnpm test:run tests/unit/domain/player/sortPlayerBrawlers.test.ts
git add imstargg-frontend/src/domain/player/sortPlayerBrawlers.ts imstargg-frontend/tests/unit/domain/player/sortPlayerBrawlers.test.ts
git commit -m "feat(frontend): sortPlayerBrawlers 순수 함수 + 단위 테스트"
```

---

## Task 10: PlayerHero 리팩토링

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx`

- [ ] **Step 1: PlayerHero 변경**

`imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";
import Trophy from "@/components/primitives/Trophy";
import PlayerPrestigeBadge from "@/components/primitives/PlayerPrestigeBadge";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { routes } from "@/config/routes";

const STAT_NUM_CLASS =
  "font-num font-bold text-[#14161B] tabular-nums [font-variant-numeric:tabular-nums]";

export function PlayerHero({ player }: { player: Player }) {
  const showdownWins = player.victoriesSolo + player.victoriesDuo;
  return (
    <Card className="flex flex-col gap-5 p-6 sm:p-7">
      <header className="flex items-center gap-4 sm:gap-6">
        <PlayerAvatar name={player.name} size={80} />
        <div className="min-w-0 flex-1">
          <h1
            className="flex items-center gap-2 truncate text-2xl font-bold sm:text-3xl"
            style={player.nameColor ? { color: player.nameColor } : undefined}
          >
            <span className="truncate">{player.name}</span>
            <PlayerPrestigeBadge level={player.totalPrestigeLevel} size={24} />
          </h1>
          <p className="font-mono text-sm text-[#5C6271]">{player.tag}</p>
          <p className="mt-1 flex items-center gap-1.5 text-sm">
            {player.club ? (
              <>
                <img src="/assets/icon_club_league_masters.png" alt="" width={16} height={16} />
                <span className="text-[#5C6271]">클럽:</span>
                <Link
                  className="font-medium hover:underline"
                  href={routes.club(player.club.tag)}
                >
                  {player.club.name}
                </Link>
                <span className="text-[#8A90A0]">→</span>
              </>
            ) : (
              <span className="text-[#8A90A0]">소속 클럽 없음</span>
            )}
          </p>
        </div>
        <RenewalButton kind="player" tag={player.tag} />
      </header>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCell label="트로피">
          <Trophy value={player.trophies} size={20} />
        </StatCell>
        <StatCell label="최고 트로피">
          <Trophy value={player.highestTrophies} size={20} />
        </StatCell>
        <StatCell label="3v3 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.victories3vs3)}</span>
        </StatCell>
        <StatCell label="쇼다운 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(showdownWins)}</span>
        </StatCell>
      </dl>
    </Card>
  );
}

function StatCell({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1 rounded-md bg-[#F6F7FA] px-3 py-2">
      <dt className="text-xs font-medium text-[#5C6271]">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
```

- [ ] **Step 2: typecheck + 시각 검증 + 커밋**

```bash
pnpm typecheck && pnpm lint
git add imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx
git commit -m "refactor(frontend): PlayerHero — 6 cell → 4 cell + inline prestige + 쇼다운 승"
```

---

## Task 11: PlayerBrawlerCard 리팩토링

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`

- [ ] **Step 1: 카드 재구성**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import Trophy from "@/components/primitives/Trophy";
import PowerLevel from "@/components/primitives/PowerLevel";
import PrestigeBadge from "@/components/primitives/PrestigeBadge";
import { PlayerBrawlerOwnership } from "./PlayerBrawlerOwnership";

export function PlayerBrawlerCard({ pb }: { pb: PlayerBrawler }) {
  return (
    <Card className="flex flex-col gap-2 p-3 text-sm">
      <div className="flex flex-col items-center gap-1 text-center">
        <BrawlerAvatar
          name={pb.brawler?.name ?? "?"}
          imageUrl={pb.brawler?.portraitImageUrl ?? null}
          rarity={pb.brawler?.rarity ?? null}
          size={80}
          showFrame
        />
        <p className="font-medium">{pb.brawler?.name ?? "Unknown"}</p>
        <div className="flex items-center justify-center gap-1.5">
          <PowerLevel level={pb.power} size={20} />
          <PrestigeBadge trophies={pb.trophies} size={18} />
          <Trophy value={pb.trophies} size={14} />
        </div>
      </div>
      <hr className="border-muted" />
      <PlayerBrawlerOwnership pb={pb} />
    </Card>
  );
}
```

- [ ] **Step 2: typecheck + 커밋**

```bash
pnpm typecheck
git add imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx
git commit -m "refactor(frontend): PlayerBrawlerCard — Trophy/PowerLevel/PrestigeBadge 사용"
```

---

## Task 12: PlayerBrawlerOwnership 재구성

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`

- [ ] **Step 1: Ownership 재작성**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import ItemSlot from "@/components/primitives/ItemSlot";
import type { ItemKind } from "@/lib/itemPlaceholder";

function SlotGroup({
  items,
  kind,
  ariaPrefix,
}: {
  items: PlayerBrawlerOwnedItem[];
  kind: ItemKind;
  ariaPrefix: string;
}) {
  if (items.length === 0) return null;
  return (
    <div className="flex gap-1">
      {items.map((it) => (
        <ItemSlot
          key={it.item.id}
          kind={kind}
          imageUrl={it.item.imageUrl}
          owned={it.owned}
          gearRarity={kind === "gear" ? (it.item.rarity ?? undefined) : undefined}
          size={20}
          ariaLabel={`${ariaPrefix} ${it.item.name} ${it.owned ? "보유" : "미보유"}`}
        />
      ))}
    </div>
  );
}

function HyperChargeSlot({ hc }: { hc: PlayerBrawlerOwnedItem }) {
  return (
    <ItemSlot
      kind="hyperCharge"
      imageUrl={hc.item.imageUrl}
      owned={hc.owned}
      size={22}
      ariaLabel={`하이퍼차지 ${hc.owned ? "보유" : "미보유"}`}
    />
  );
}

function BuffyGroup({ buffies }: { buffies: PlayerBrawlerBuffies }) {
  return (
    <div className="flex gap-1">
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.gadget.item.imageUrl}
        owned={buffies.gadget.owned}
        size={18}
        ariaLabel={`버피 가젯 ${buffies.gadget.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.starPower.item.imageUrl}
        owned={buffies.starPower.owned}
        size={18}
        ariaLabel={`버피 스타파워 ${buffies.starPower.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.hyperCharge.item.imageUrl}
        owned={buffies.hyperCharge.owned}
        size={18}
        ariaLabel={`버피 하이퍼차지 ${buffies.hyperCharge.owned ? "보유" : "미보유"}`}
      />
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
      <SlotGroup items={pb.gadgets} kind="gadget" ariaPrefix="가젯" />
      <SlotGroup items={pb.starPowers} kind="starPower" ariaPrefix="스타파워" />
      <SlotGroup items={pb.gears} kind="gear" ariaPrefix="기어" />
      {pb.buffies && <BuffyGroup buffies={pb.buffies} />}
      {pb.hyperCharge && <HyperChargeSlot hc={pb.hyperCharge} />}
    </div>
  );
}
```

> 참고: `PlayerBrawlerBuffies` 의 각 필드 (`gadget`, `starPower`, `hyperCharge`) 가 `PlayerBrawlerOwnedItem` 형식이라 `it.item.imageUrl` 접근이 가능. 기존 도메인 모델 (`pb.buffies.gadget.owned` 만 사용하던 구조) 와 호환되는지 typecheck 로 확인 — 만약 `BuffyResponse` 가 `imageUrl` 을 가지면 그대로 OK, 다르면 도메인 mapper 보강이 필요. (이 단계에서 typecheck 결과 보고 분기)

- [ ] **Step 2: typecheck — 도메인 모양 확인**

Run: `pnpm typecheck`

만약 type error 가 있으면 `src/domain/player/types.ts` 의 `PlayerBrawlerBuffies` 타입을 점검하고 mapper 가 buffy item shape 을 올바르게 채우는지 확인. 필요한 경우 `PlayerBrawlerBuffies.gadget` 등의 타입을 `PlayerBrawlerOwnedItem<BuffyResponse>` 로 통일.

- [ ] **Step 3: lint + 커밋**

```bash
pnpm lint
git add imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx
git commit -m "refactor(frontend): PlayerBrawlerOwnership — ItemSlot 슬롯 그리드 + 라벨 제거"
```

---

## Task 13: PlayerBrawlers 정렬 컨트롤

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx`

- [ ] **Step 1: 정렬 select + 토글 추가**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp } from "lucide-react";
import type { PlayerBrawler } from "@/domain/player/types";
import {
  sortPlayerBrawlers,
  type PlayerBrawlerSortKey,
  type SortDirection,
} from "@/domain/player/sortPlayerBrawlers";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PlayerBrawlerCard } from "./PlayerBrawlerCard";

const SORT_OPTIONS: ReadonlyArray<{ value: PlayerBrawlerSortKey; label: string }> = [
  { value: "trophies", label: "트로피" },
  { value: "power", label: "파워레벨" },
  { value: "rarity", label: "희귀도" },
  { value: "name", label: "이름" },
  { value: "id", label: "최신순" },
];

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  const [key, setKey] = useState<PlayerBrawlerSortKey>("trophies");
  const [dir, setDir] = useState<SortDirection>("desc");

  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }

  const sorted = sortPlayerBrawlers(brawlers, key, dir);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <Select value={key} onValueChange={(v) => setKey(v as PlayerBrawlerSortKey)}>
          <SelectTrigger className="w-32" aria-label="정렬 기준">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant="outline"
          size="icon"
          aria-label={dir === "asc" ? "오름차순" : "내림차순"}
          onClick={() => setDir((d) => (d === "asc" ? "desc" : "asc"))}
        >
          {dir === "asc" ? <ArrowUp /> : <ArrowDown />}
        </Button>
      </div>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
        {sorted.map((pb) => (
          <li key={`${pb.brawler?.id ?? "x"}-${pb.brawler?.name ?? "x"}`}>
            <PlayerBrawlerCard pb={pb} />
          </li>
        ))}
      </ul>
    </div>
  );
}
```

- [ ] **Step 2: shadcn Select / Button 존재 확인**

Run: `ls imstargg-frontend/src/components/ui/ | grep -E "select|button"`
Expected: `select.tsx`, `button.tsx` 둘 다 존재

만약 `select.tsx` 가 없다면: `cd imstargg-frontend && pnpm dlx shadcn@latest add select`

- [ ] **Step 3: typecheck + 커밋**

```bash
pnpm typecheck && pnpm lint
git add imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx
git commit -m "feat(frontend): PlayerBrawlers 정렬 select + 방향 토글"
```

---

## Task 14: BattlePlayerRow — RankTierIcon + PowerLevel 적용

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`

- [ ] **Step 1: BattlePlayerRow 수정**

`imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx` 전체를 다음으로 교체:

```tsx
import Link from "next/link";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import PowerLevel from "@/components/primitives/PowerLevel";
import RankTierIcon from "@/components/primitives/RankTierIcon";
import Trophy from "@/components/primitives/Trophy";
import { isSameTag, toUrlPath } from "@/lib/tag";
import type { BattlePlayer } from "@/domain/battle/types";

interface Props {
  player: BattlePlayer;
  ownerTag: string;
  starPlayerTag: string | null;
}

export function BattlePlayerRow({ player, ownerTag, starPlayerTag }: Props) {
  const isOwner = isSameTag(player.tag, ownerTag);
  const isStar = starPlayerTag != null && isSameTag(player.tag, starPlayerTag);
  const playerBrawler = player.brawler.brawler;

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
      <BrawlerAvatar
        name={playerBrawler?.name ?? "?"}
        imageUrl={playerBrawler?.portraitImageUrl ?? null}
        rarity={playerBrawler?.rarity}
        size={32}
      />
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
        <PowerLevel level={player.brawler.power} size={18} />
        {player.brawler.trophies != null && <Trophy value={player.brawler.trophies} size={12} />}
        {player.soloRankTier != null && <RankTierIcon tier={player.soloRankTier} size={16} />}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: typecheck + 커밋**

```bash
pnpm typecheck && pnpm lint
git add imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx
git commit -m "refactor(frontend): BattlePlayerRow — RankTierIcon + PowerLevel 적용"
```

---

## Task 15: PowerPill 삭제 + tierLabel deprecated 제거 + 최종 검증

**Files:**

- Delete: `imstargg-frontend/src/components/primitives/PowerPill.tsx`
- Modify: `imstargg-frontend/src/domain/battle/tierLabels.ts` (deprecated `tierLabel` 제거)

- [ ] **Step 1: PowerPill 사용처 0 확인**

Run: `grep -rln "PowerPill" imstargg-frontend/`
Expected: 출력 없음 (Task 11, 14 에서 모두 PowerLevel 로 교체됐어야 함)

만약 사용처가 남아 있으면 그곳을 PowerLevel 로 교체 후 진행.

- [ ] **Step 2: PowerPill 삭제**

Run: `rm imstargg-frontend/src/components/primitives/PowerPill.tsx`

- [ ] **Step 3: deprecated `tierLabel` 제거**

`imstargg-frontend/src/domain/battle/tierLabels.ts` 끝의 deprecated alias 제거:

```ts
/** @deprecated Task 8 에서 RankTierIcon 으로 대체 */
export function tierLabel(tier: SoloRankTier): string {
  return tierSegmentNumber(tier) ?? "";
}
```

→ 삭제. 그리고 `grep -rln "tierLabel" imstargg-frontend/` 로 잔존 호출처 0 확인.

- [ ] **Step 4: 전체 검증**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

Expected: typecheck 0 errors, lint 0 errors, all tests pass.

- [ ] **Step 5: 시각 검증 (수동)**

Dev 서버 시작:

```bash
pnpm dev
```

브라우저에서 다음 페이지 확인:

- `/players/88J8Q9RPV` — PlayerHero (4 cell + 이름 옆 prestige), PlayerBrawlers (정렬 select + 토글), PlayerBrawlerCard (Trophy/PowerLevel/PrestigeBadge), Battle (RankTierIcon)
- `/clubs/2U8CVLQQ9` — Trophy 자동 전파 확인 (1000+ 멤버가 있으면 prestige_trophy_2 가 보여야 함)

- [ ] **Step 6: 마무리 커밋**

```bash
git add -A
git commit -m "chore(frontend): PowerPill 삭제 + tierLabel deprecated alias 제거"
```

---

## Out of Scope

- 자산 추가/디자인 — 이미 사용자가 `public/assets/` 에 추가 완료 (commit `52b9137`)
- 백엔드 변경 — `GearResponse.rarity` / `SoloRankTier` 는 모두 갖춰져 있음
- 정렬 상태 sessionStorage 보존 — 단순화 우선; 추후 필요 시 별도 작업
- DESIGN.md 의 Rank Tier 색 매핑 보정 — 별도 디자인 검토 필요

---

## 위험 / 호환성

- **`SoloRankTier` enum 변경**: 백엔드는 이미 `MASTER_*` / `PRO` 를 보내고 있음 — 본 변경으로 자동 해소
- **모바일 레이아웃**: PlayerBrawlerCard 의 PowerLevel + PrestigeBadge + Trophy 한 줄이 좁은 폭에서 깨질 가능성 — `gap-1.5` + 작은 size 로 대응. Task 15 시각 검증 단계에서 확인
- **PlayerBrawlerOwnership buffy shape**: 도메인 `PlayerBrawlerBuffies` 가 `imageUrl` 을 노출하지 않으면 mapper 보강이 추가로 필요 — Task 12 typecheck 단계에서 확정
