# Frontend Image Improvements Follow-up — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 4 가지 보완 — Trophy/BrawlerTrophy 분리, ItemSlot 미보유 통일, 3-row 레이아웃, 아이템 툴팁 + 브롤러 카드 클릭 네비.

**Spec:** `docs/superpowers/specs/2026-05-04-frontend-image-improvements-followup-design.md`

---

## Task 1: Trophy 단순화 + BrawlerTrophy primitive 신규

**Files:**

- Modify: `imstargg-frontend/src/components/primitives/Trophy.tsx`
- Create: `imstargg-frontend/src/components/primitives/BrawlerTrophy.tsx`
- Modify: `imstargg-frontend/tests/unit/components/primitives/Trophy.test.tsx`
- Create: `imstargg-frontend/tests/unit/components/primitives/BrawlerTrophy.test.tsx`

- [ ] **Step 1: BrawlerTrophy 테스트**

Create `imstargg-frontend/tests/unit/components/primitives/BrawlerTrophy.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";

describe("BrawlerTrophy 컴포넌트", () => {
  it("1000 미만은 기본 trophy 아이콘을 사용한다", () => {
    render(<BrawlerTrophy value={999} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_trophy.png");
  });
  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다 (하단)", () => {
    render(<BrawlerTrophy value={1000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });
  it("1000 이상 3000 미만은 prestige_trophy_2 를 사용한다 (상단)", () => {
    render(<BrawlerTrophy value={2999} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy_2.webp");
  });
  it("3000 이상은 prestige_trophy 를 사용한다", () => {
    render(<BrawlerTrophy value={3000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_prestige_trophy.webp");
  });
});
```

- [ ] **Step 2: BrawlerTrophy 구현**

Create `imstargg-frontend/src/components/primitives/BrawlerTrophy.tsx`:

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

export default function BrawlerTrophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src={trophyIcon(value)} alt="" role="img" width={size} height={size} />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
```

- [ ] **Step 3: Trophy 단순화**

`imstargg-frontend/src/components/primitives/Trophy.tsx` 전체를 다음으로 교체:

```tsx
import { formatNumber } from "@/lib/format";

interface Props {
  value: number;
  size?: number;
}

export default function Trophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src="/assets/icon_trophy.png" alt="" role="img" width={size} height={size} />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
```

- [ ] **Step 4: Trophy 테스트 단순화**

`imstargg-frontend/tests/unit/components/primitives/Trophy.test.tsx` 를 다음으로 교체:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy 컴포넌트", () => {
  it("항상 기본 trophy 아이콘을 사용한다 (player 트로피)", () => {
    render(<Trophy value={5000} />);
    expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("src", "/assets/icon_trophy.png");
  });
});
```

- [ ] **Step 5: PlayerBrawlerCard 와 BattlePlayerRow 의 Trophy → BrawlerTrophy 교체**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`:

```tsx
// import 변경
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";  // Trophy 제거
// 사용처
<BrawlerTrophy value={pb.trophies} size={14} />
```

`imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`:

```tsx
// import 변경
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";
// 사용처 (player.brawler.trophies 인자 — 브롤러 트로피)
<BrawlerTrophy value={player.brawler.trophies} size={12} />
```

- [ ] **Step 6: 사용처 검증 + 테스트 + 커밋**

```bash
cd imstargg-frontend
grep -rn "<Trophy " src app  # player 트로피 사용처만 남는지 — PlayerHero / ClubMembers
grep -rn "<BrawlerTrophy " src app  # PlayerBrawlerCard, BattlePlayerRow 두 곳
pnpm typecheck && pnpm lint && pnpm test:run 2>&1 | tail -20
```

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add -A
git -c commit.gpgsign=false commit -m "feat(frontend): Trophy/BrawlerTrophy 분리 — player vs brawler 트로피 의미 분리"
```

---

## Task 2: ItemSlot 미보유 처리 통일 + name 툴팁

**Files:**

- Modify: `imstargg-frontend/src/components/primitives/ItemSlot.tsx`
- Modify: `imstargg-frontend/src/lib/itemPlaceholder.ts` (emptyPlaceholder 제거)
- Modify: `imstargg-frontend/tests/unit/components/primitives/ItemSlot.test.tsx`
- Modify: `imstargg-frontend/tests/unit/lib/itemPlaceholder.test.ts`

- [ ] **Step 1: ItemSlot 테스트 업데이트**

`imstargg-frontend/tests/unit/components/primitives/ItemSlot.test.tsx` 의 변경:

- "미보유 + empty 자산이 있으면 empty placeholder 를 렌더한다" → "미보유는 base 를 흐리게 렌더한다 (gadget)" 로 교체:

```tsx
it("gadget 미보유는 base 를 opacity + grayscale 로 렌더한다", () => {
  render(<ItemSlot kind="gadget" imageUrl={null} owned={false} />);
  const img = screen.getByRole("img", { hidden: true });
  expect(img).toHaveAttribute("src", "/assets/gadget_base_empty.webp");
  expect(img.style.opacity).toBe("0.3");
  expect(img.style.filter).toBe("grayscale(1)");
});
```

- 기존 "hyperCharge 미보유" 테스트는 그대로 유지 (이미 같은 패턴 검증).
- 신규 테스트 추가:

```tsx
it("name prop 이 있으면 title 속성을 렌더한다", () => {
  render(<ItemSlot kind="gadget" imageUrl="https://cdn/x.webp" owned name="가젯이름" />);
  expect(screen.getByRole("img", { hidden: true })).toHaveAttribute("title", "가젯이름");
});

it("buffy 미보유는 lucide 아이콘 + opacity 0.3 으로 렌더한다", () => {
  render(<ItemSlot kind="buffy" imageUrl={null} owned={false} ariaLabel="버피 가젯 미보유" />);
  expect(screen.getByLabelText("버피 가젯 미보유")).toHaveStyle({ opacity: "0.3" });
});
```

- [ ] **Step 2: ItemSlot 단순화 + name prop 추가**

`imstargg-frontend/src/components/primitives/ItemSlot.tsx` 전체 교체:

```tsx
import { Zap } from "lucide-react";
import type { GearRarity } from "@/api/types";
import { basePlaceholder, type ItemKind } from "@/lib/itemPlaceholder";

interface Props {
  kind: ItemKind;
  imageUrl: string | null;
  owned: boolean;
  gearRarity?: GearRarity;
  size?: number;
  name?: string;
  ariaLabel?: string;
}

export default function ItemSlot({
  kind,
  imageUrl,
  owned,
  gearRarity,
  size = 24,
  name,
  ariaLabel,
}: Props) {
  const title = name || undefined;

  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        title={title}
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

  const src = imageUrl ?? basePlaceholder(kind, gearRarity);
  if (!src) return null;

  return (
    <img
      src={src}
      alt=""
      role="img"
      width={size}
      height={size}
      title={title}
      aria-label={ariaLabel}
      style={owned ? undefined : { opacity: 0.3, filter: "grayscale(1)" }}
    />
  );
}
```

- [ ] **Step 3: emptyPlaceholder 제거**

`imstargg-frontend/src/lib/itemPlaceholder.ts` 의 `emptyPlaceholder` 함수 제거:

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
```

`imstargg-frontend/tests/unit/lib/itemPlaceholder.test.ts` 의 `describe("emptyPlaceholder", ...)` 블록 전체 삭제. import 도 정리.

- [ ] **Step 4: 검증 + 커밋**

```bash
cd imstargg-frontend
grep -rn "emptyPlaceholder" src tests app  # 0 callers
pnpm typecheck && pnpm lint && pnpm test:run 2>&1 | tail -20
```

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add -A
git -c commit.gpgsign=false commit -m "refactor(frontend): ItemSlot 미보유 = grayscale 통일 + name 툴팁"
```

---

## Task 3: PlayerBrawlerOwnership 3-row 레이아웃 + name 전달

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`

- [ ] **Step 1: 3-row 레이아웃 + name 전달**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx` 전체 교체:

```tsx
"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import ItemSlot from "@/components/primitives/ItemSlot";
import type { ItemKind } from "@/lib/itemPlaceholder";

const SIZE = {
  item: 20,   // gadget / starPower / gear
  buffy: 18,  // buffy (작은 보조 아이템)
  hc: 22,     // HyperCharge (단독 강조 슬롯)
} as const;

function SlotGroup({
  items,
  kind,
  label,
}: {
  items: PlayerBrawlerOwnedItem[];
  kind: ItemKind;
  label: string;
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
          size={SIZE.item}
          name={it.item.name}
          ariaLabel={`${label} ${it.item.name} ${it.owned ? "보유" : "미보유"}`}
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
      size={SIZE.hc}
      name={hc.item.name}
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
        size={SIZE.buffy}
        ariaLabel={`버피 가젯 ${buffies.gadget.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.starPower.item.imageUrl}
        owned={buffies.starPower.owned}
        size={SIZE.buffy}
        ariaLabel={`버피 스타파워 ${buffies.starPower.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.hyperCharge.item.imageUrl}
        owned={buffies.hyperCharge.owned}
        size={SIZE.buffy}
        ariaLabel={`버피 하이퍼차지 ${buffies.hyperCharge.owned ? "보유" : "미보유"}`}
      />
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center gap-3">
        <SlotGroup items={pb.gadgets} kind="gadget" label="가젯" />
        <SlotGroup items={pb.starPowers} kind="starPower" label="스타파워" />
      </div>
      <div className="flex items-center gap-3">
        <SlotGroup items={pb.gears} kind="gear" label="기어" />
      </div>
      <div className="flex items-center gap-3">
        {pb.hyperCharge && <HyperChargeSlot hc={pb.hyperCharge} />}
        {pb.buffies && <BuffyGroup buffies={pb.buffies} />}
      </div>
    </div>
  );
}
```

변경:
- top wrapper `flex flex-wrap items-center gap-x-3 gap-y-1.5` → `flex flex-col gap-1.5` + 3 row div
- Row 1: gadget + starPower
- Row 2: gear
- Row 3: HyperCharge (왼쪽) + Buffy (오른쪽)
- SlotGroup / HyperChargeSlot 의 ItemSlot 호출에 `name={it.item.name}` 추가 (BuffyGroup 은 name 없음)

- [ ] **Step 2: 검증 + 커밋**

```bash
cd imstargg-frontend
pnpm typecheck && pnpm lint && pnpm test:run tests/components/PlayerBrawlerOwnership.test.tsx
```

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add -A
git -c commit.gpgsign=false commit -m "refactor(frontend): PlayerBrawlerOwnership 3-row 레이아웃 + 아이템 name 툴팁"
```

---

## Task 4: PlayerBrawlerCard — BrawlerAvatar 클릭 → 브롤러 상세

**Files:**

- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`

- [ ] **Step 1: BrawlerAvatar 를 Link 로 wrap**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx` 전체 교체:

```tsx
"use client";

import Link from "next/link";
import type { PlayerBrawler } from "@/domain/player/types";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";
import PowerLevel from "@/components/primitives/PowerLevel";
import PrestigeBadge from "@/components/primitives/PrestigeBadge";
import { routes } from "@/config/routes";
import { PlayerBrawlerOwnership } from "./PlayerBrawlerOwnership";

export function PlayerBrawlerCard({ pb }: { pb: PlayerBrawler }) {
  const avatar = (
    <BrawlerAvatar
      name={pb.brawler?.name ?? "?"}
      imageUrl={pb.brawler?.portraitImageUrl ?? null}
      rarity={pb.brawler?.rarity ?? null}
      size={80}
      showFrame
    />
  );
  return (
    <Card className="flex flex-col gap-2 p-3 text-sm">
      <div className="flex flex-col items-center gap-1 text-center">
        {pb.brawler ? (
          <Link href={routes.brawler(pb.brawler.id)} aria-label={`${pb.brawler.name} 상세`}>
            {avatar}
          </Link>
        ) : (
          avatar
        )}
        <p className="font-medium">{pb.brawler?.name ?? "Unknown"}</p>
        <div className="flex items-center justify-center gap-1.5">
          <PowerLevel level={pb.power} size={20} />
          <PrestigeBadge trophies={pb.trophies} size={18} />
          <BrawlerTrophy value={pb.trophies} size={14} />
        </div>
      </div>
      <hr className="border-muted" />
      <PlayerBrawlerOwnership pb={pb} />
    </Card>
  );
}
```

(Task 1 에서 이미 `<Trophy>` → `<BrawlerTrophy>` 변경했으므로 이 task 의 핵심은 `<Link>` 추가만.)

- [ ] **Step 2: 검증 + 커밋**

```bash
cd imstargg-frontend
pnpm typecheck && pnpm lint
```

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx
git -c commit.gpgsign=false commit -m "feat(frontend): PlayerBrawlerCard — BrawlerAvatar 클릭 시 브롤러 상세 이동"
```

---

## Out of Scope

- `_empty.webp` 자산 파일 삭제 (코드 미참조 후 추후 cleanup)
- BuffyResponse 에 name 필드 추가 (백엔드 변경 필요)
- a11y 보강 (RankTierIcon / PrestigeBadge ariaLabel prop)

## 위험 / 호환성

- `<Trophy>` 호출처가 player 트로피 의미를 가져야 하는데 잘못된 곳에서 호출된 경우 잠재적 회귀. 검증: grep 로 모든 `<Trophy ` / `<BrawlerTrophy ` 호출처 분류.
- emptyPlaceholder caller 가 0 임을 grep 으로 확실히 확인 후 제거.
- ItemSlot 의 `name` prop 은 optional, 기존 호출부에 영향 없음.
