# Frontend Image Improvements — Design Spec

**Goal**: imstargg-frontend 의 이미지 사용을 일관·풍부하게 개선한다. 트로피·파워레벨·프레스티지·솔로랭크·아이템(가젯/스타파워/기어/하이퍼차지/버피) 시각 표현을 신규 primitive 컴포넌트로 통일하고, 플레이어 상세를 그에 맞춰 재구성한다.

**Architecture**: 자산 ↔ 데이터 매핑은 단일 헬퍼 (`src/lib/itemPlaceholder.ts`) 에 모으고, 시각 표현은 6 개 신규/확장 primitive (`Trophy`, `PowerLevel`, `PrestigeBadge`, `PlayerPrestigeBadge`, `RankTierIcon`, `ItemSlot`) 로 분리한다. 사용처 (PlayerHero / PlayerBrawlerCard / PlayerBrawlerOwnership / BattlePlayerRow / ClubMembers / ClubHeader) 는 primitive 호출부만 갱신.

**Tech Stack**: Next.js App Router, TanStack Query, shadcn/ui, lucide-react. 정적 자산은 raw `<img>` 직접 사용 (DESIGN.md 컨벤션).

---

## 1. 자산 매핑 (확정)

`imstargg-frontend/public/assets/` 에 이미 추가된 자산을 사용한다.

### 1.1 아이템 placeholder 자산 매트릭스

- **Base placeholder** = catalog 에 등재됐으나 `imageUrl == null` 인 fallback
- **Empty placeholder** = 플레이어 미보유 슬롯

| Kind | Base placeholder | Empty placeholder |
| --- | --- | --- |
| gadget | `gadget_base_empty.webp` | `gadget_empty.webp` |
| gear (`SUPER_RARE`) | `gear_base_empty.webp` | `gear_empty.webp` |
| gear (`EPIC`) | `gear_epic_super_base_empty.webp` | `gear_empty.webp` |
| gear (`MYTHIC`) | `gear_mythic_super_base_empty.webp` | `gear_empty.webp` |
| starPower | `starpower_base01_empty.webp` | `starpower_empty.webp` |
| hyperCharge | `hypercharge_base_empty.webp` | (자산 부재 → base 를 `opacity: 0.3 + grayscale`) |
| buffy | (자산 부재 → lucide `Zap`) | (자산 부재 → lucide `Zap` + `opacity: 0.3`) |

명명 컨벤션:

- `*_empty.webp` = 플레이어 **미보유** 슬롯
- `*_base_empty.webp` = 카탈로그엔 등재됐으나 **`imageUrl == null`** 인 fallback

스타파워 자산명의 `01` 은 단순 명명 컨벤션이며 슬롯 인덱스 분기가 아니다 — 모든 스타파워에 동일 자산 사용.

### 1.2 트로피 임계값 매핑

플레이어 트로피 (사용자 단일 값):

| 임계값 | 아이콘 |
| --- | --- |
| `< 1000` | `icon_trophy.png` |
| `>= 1000` | `icon_prestige_trophy_2.webp` |
| `>= 3000` | `icon_prestige_trophy.webp` |

브롤러 트로피 (각 PlayerBrawler):

| 임계값 | 아이콘 | 오버레이 숫자 |
| --- | --- | --- |
| `< 250` | `icon_trophy_brawler_wood.webp` | — |
| `>= 250` | `icon_trophy_brawler_bronze.webp` | — |
| `>= 500` | `icon_trophy_brawler_silver.webp` | — |
| `>= 750` | `icon_trophy_brawler_gold.webp` | — |
| `>= 1000` | `icon_trophy_brawler_prestige_1.webp` | `1` (흰색 두꺼운 글자) |
| `>= 2000` | `icon_trophy_brawler_prestige_2.webp` | `2` |
| `>= 3000` | `icon_trophy_brawler_prestige_3.webp` | `3` |

### 1.3 솔로 랭크 스티커 매핑

base tier 별로 단일 자산 (1/2/3 segment 구분 없음):

| Tier base | 자산 |
| --- | --- |
| `BRONZE_*` | `icon_rank_sticker_bronze.webp` |
| `SILVER_*` | `icon_rank_sticker_silver.webp` |
| `GOLD_*` | `icon_rank_sticker_gold.webp` |
| `DIAMOND_*` | `icon_rank_sticker_diamond.webp` |
| `MYTHIC_*` | `icon_rank_sticker_mythic.webp` |
| `LEGENDARY_*` | `icon_rank_sticker_legendary.webp` |
| `MASTER_*` | `icon_rank_sticker_masters.webp` |
| `PRO` | `icon_rank_sticker_pro.webp` |

표기: `BRONZE_2` → 스티커 + `"2"` (티어 색). `MASTER_3` → 스티커 + `"3"`. `PRO` → 스티커 단독 (숫자 없음).

### 1.4 플레이어 프레스티지 (이름 옆 inline)

`totalPrestigeLevel >= 1` 일 때 `icon_total_prestige.webp` 위 흰색 두꺼운 숫자. `0` 이면 렌더하지 않음.

---

## 2. 신규/확장 Primitive 컴포넌트

### 2.1 `src/lib/itemPlaceholder.ts` (신규)

자산 매핑 단일 책임 헬퍼. 컴포넌트는 placeholder 이미지 경로 결정 로직을 직접 갖지 않고 이 모듈에 위임.

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
      return null;
    case "buffy":
      return null;
  }
}
```

단위 테스트: 각 kind × (rarity 조합) 의 반환 경로가 자산 매트릭스와 일치하는지.

### 2.2 `src/components/primitives/Trophy.tsx` (확장)

기존 단일 아이콘 → 값 임계값별 아이콘 자동 전환.

```tsx
interface Props { value: number; size?: number; }

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

단위 테스트: 임계값 경계 (`999`, `1000`, `2999`, `3000`) 에서 올바른 자산 경로 반환.

### 2.3 `src/components/primitives/PowerLevel.tsx` (신규)

기존 `PowerPill` 을 대체. `power_level_base_empty.svg` 위 흰색 두꺼운 숫자.

```tsx
interface Props { level: number; size?: number; }

export default function PowerLevel({ level, size = 22 }: Props) {
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src="/assets/power_level_base_empty.svg" alt="" width={size} height={size} />
      <span
        style={{
          position: "absolute", inset: 0,
          display: "grid", placeItems: "center",
          color: "#FFFFFF",
          fontFamily: "var(--font-num)",
          fontWeight: 800,
          fontSize: size * 0.55,
          textShadow: "0 1px 2px rgba(0,0,0,0.4)",
        }}
        aria-label={`파워 레벨 ${level}`}
      >
        {level}
      </span>
    </span>
  );
}
```

P11 별도 강조 없음 (Q4-A1 결정). `PowerPill.tsx` 는 본 작업 마지막 단계에서 삭제.

### 2.4 `src/components/primitives/PrestigeBadge.tsx` (신규)

브롤러 카드용. 트로피 값에 따른 prestige 트로피 아이콘 + 오버레이 숫자 (1000+ 부터).

```tsx
interface Props { trophies: number; size?: number; }

const STEPS = [
  { min: 3000, src: "/assets/icon_trophy_brawler_prestige_3.webp", overlay: "3" },
  { min: 2000, src: "/assets/icon_trophy_brawler_prestige_2.webp", overlay: "2" },
  { min: 1000, src: "/assets/icon_trophy_brawler_prestige_1.webp", overlay: "1" },
  { min:  750, src: "/assets/icon_trophy_brawler_gold.webp",       overlay: null },
  { min:  500, src: "/assets/icon_trophy_brawler_silver.webp",     overlay: null },
  { min:  250, src: "/assets/icon_trophy_brawler_bronze.webp",     overlay: null },
  { min:    0, src: "/assets/icon_trophy_brawler_wood.webp",       overlay: null },
];

export default function PrestigeBadge({ trophies, size = 18 }: Props) {
  const step = STEPS.find((s) => trophies >= s.min)!;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src={step.src} alt="" width={size} height={size} />
      {step.overlay && (
        <span
          style={{
            position: "absolute", inset: 0,
            display: "grid", placeItems: "center",
            color: "#FFFFFF", fontFamily: "var(--font-num)", fontWeight: 800,
            fontSize: size * 0.55, textShadow: "0 1px 2px rgba(0,0,0,0.5)",
          }}
        >{step.overlay}</span>
      )}
    </span>
  );
}
```

단위 테스트: 각 임계값 경계.

### 2.5 `src/components/primitives/PlayerPrestigeBadge.tsx` (신규)

플레이어 이름 옆 inline. `totalPrestigeLevel >= 1` 일 때만 렌더, 그 외엔 `null`.

```tsx
interface Props { level: number; size?: number; }

export default function PlayerPrestigeBadge({ level, size = 22 }: Props) {
  if (level < 1) return null;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src="/assets/icon_total_prestige.webp" alt="" width={size} height={size} />
      <span
        style={{
          position: "absolute", inset: 0,
          display: "grid", placeItems: "center",
          color: "#FFFFFF", fontFamily: "var(--font-num)", fontWeight: 800,
          fontSize: size * 0.5, textShadow: "0 1px 2px rgba(0,0,0,0.5)",
        }}
        aria-label={`프레스티지 ${level}`}
      >{level}</span>
    </span>
  );
}
```

### 2.6 `src/components/primitives/RankTierIcon.tsx` (신규)

솔로 랭크 스티커 + 흰색 → 티어 색 숫자.

```tsx
import type { SoloRankTier } from "@/api/types";
import { tierColor, tierStickerImage, tierSegmentNumber } from "@/domain/battle/tierLabels";

interface Props { tier: SoloRankTier; size?: number; }

export default function RankTierIcon({ tier, size = 18 }: Props) {
  const segment = tierSegmentNumber(tier);  // "1" | "2" | "3" | null
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src={tierStickerImage(tier)} alt="" width={size} height={size} />
      {segment && (
        <span
          style={{
            position: "absolute", inset: 0,
            display: "grid", placeItems: "center",
            color: tierColor(tier),
            fontFamily: "var(--font-num)", fontWeight: 800,
            fontSize: size * 0.45,
          }}
        >{segment}</span>
      )}
    </span>
  );
}
```

### 2.7 `src/components/primitives/ItemSlot.tsx` (신규)

아이템 한 칸. `kind`, `imageUrl | null`, `owned: boolean` 을 받아 자산 매트릭스에 따라 `<img>` 렌더. buffy 만 자산 부재 시 lucide `<Zap>` 으로 fallback.

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

export default function ItemSlot({ kind, imageUrl, owned, gearRarity, size = 24, ariaLabel }: Props) {
  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        aria-label={ariaLabel}
        style={{
          width: size, height: size,
          display: "grid", placeItems: "center",
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
    return <img src={src!} alt="" width={size} height={size} aria-label={ariaLabel} />;
  }

  // not owned
  const emptySrc = emptyPlaceholder(kind);
  if (emptySrc) {
    return <img src={emptySrc} alt="" width={size} height={size} aria-label={ariaLabel} />;
  }
  // hyperCharge: empty 자산 부재 → base 를 흐리게
  const baseSrc = imageUrl ?? basePlaceholder(kind, gearRarity);
  return (
    <img
      src={baseSrc!}
      alt=""
      width={size}
      height={size}
      style={{ opacity: 0.3, filter: "grayscale(1)" }}
      aria-label={ariaLabel}
    />
  );
}
```

---

## 3. 데이터/타입 변경

### 3.1 `src/api/types.ts`

- `GearRarity` 신규 export: `export type GearRarity = "SUPER_RARE" | "EPIC" | "MYTHIC";`
- `GearResponse` 보강:

```ts
export interface GearResponse { id: number; name: MessagesResponse; rarity: GearRarity; imageUrl: string | null; }
```

- `SoloRankTier` 동기화 — 기존 `MASTERS` 단일 → 백엔드와 일치:

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

### 3.2 `src/domain/brawler/types.ts` + `mappers.ts`

- `BrawlerItem` 에 `rarity: GearRarity | null` 추가 (gear 만 채움, 그 외 null)
- `toBrawlerItem` 이 dto 의 `rarity` 패스스루 (있을 때)
- mapper 단위 테스트 보강

### 3.3 `src/domain/battle/tierLabels.ts`

기존 `tierLabel`/`tierColor` → 의미 변화 + 신규 함수 추가.

기존 색 매핑은 유지하고 (`MYTHIC: #B83BFF`, `LEGENDARY: #FF3A3A`), 누락된 `MASTER_*` / `PRO` 만 추가한다. 색 매핑 자체의 디자인 보정 (DESIGN.md 의 Rank Tier 매핑과 일부 어긋남) 은 본 작업 범위 외 — 별도 디자인 검토 필요.

```ts
const TIER_BASE_COLOR: Record<string, string> = {
  BRONZE: "#C77545",
  SILVER: "#9FAFC5",
  GOLD: "#F2B93B",
  DIAMOND: "#58D7E8",
  MYTHIC: "#B83BFF",
  LEGENDARY: "#FF3A3A",
  MASTER: "#FFD447",   // 기존 MASTERS 의 색 유지
  PRO: "#FFD447",      // PRO 도 같은 색 (MASTERS 의 연장)
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

기존 `tierLabel()` 함수는 더 이상 사용처가 없으면 삭제, 사용처가 남아있으면 `tierSegmentNumber()` 결과로 교체. (현재 유일 사용처는 `BattlePlayerRow`)

단위 테스트: 모든 22 개 tier 값에 대해 색/스티커/segment 반환 검증.

### 3.4 `src/domain/player/sortPlayerBrawlers.ts` (신규)

정렬 함수 분리.

```ts
import type { PlayerBrawler } from "./types";
import type { BrawlerRarity } from "@/api/types";

export type PlayerBrawlerSortKey = "trophies" | "power" | "rarity" | "name" | "id";
export type SortDirection = "asc" | "desc";

const RARITY_ORDER: Record<BrawlerRarity, number> = {
  STARTING_BRAWLER: 0, RARE: 1, SUPER_RARE: 2, EPIC: 3,
  MYTHIC: 4, LEGENDARY: 5, ULTRA_LEGENDARY: 6,
};

export function sortPlayerBrawlers(
  brawlers: PlayerBrawler[],
  key: PlayerBrawlerSortKey,
  direction: SortDirection,
): PlayerBrawler[] {
  const sign = direction === "asc" ? 1 : -1;
  return [...brawlers].sort((a, b) => {
    const av = pick(a, key);
    const bv = pick(b, key);
    if (typeof av === "string" && typeof bv === "string") return sign * av.localeCompare(bv);
    return sign * ((av as number) - (bv as number));
  });
}

function pick(pb: PlayerBrawler, key: PlayerBrawlerSortKey): number | string {
  switch (key) {
    case "trophies": return pb.trophies;
    case "power":    return pb.power;
    case "rarity":   return pb.brawler ? RARITY_ORDER[pb.brawler.rarity] : -1;
    case "name":     return pb.brawler?.name ?? "";
    case "id":       return pb.brawler?.id ?? 0;
  }
}
```

단위 테스트: 각 키 × 방향, null brawler 안전성.

---

## 4. 사용처 변경

### 4.1 `app/players/[tag]/_components/PlayerHero.tsx`

#### 변경 요약

- 6 stat cell → **4 stat cell** (트로피 / 최고 트로피 / 3v3 승 / 쇼다운 승)
- `프레스티지` cell 제거 → 이름 옆 `<PlayerPrestigeBadge level={player.totalPrestigeLevel} />`
- `EXP Lv` cell 제거
- `솔로 승` cell 제거 → `쇼다운 승 = victoriesSolo + victoriesDuo` cell 추가
- 트로피 cell 의 `<Trophy>` 컴포넌트는 호출부 그대로 (내부 임계값 로직이 흡수)

```tsx
<header>
  <PlayerAvatar />
  <div>
    <h1 className="flex items-center gap-2 ...">
      {player.name}
      <PlayerPrestigeBadge level={player.totalPrestigeLevel} />
    </h1>
    {/* tag, club ... */}
  </div>
</header>
<dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
  <StatCell label="트로피"><Trophy value={player.trophies} size={20} /></StatCell>
  <StatCell label="최고 트로피"><Trophy value={player.highestTrophies} size={20} /></StatCell>
  <StatCell label="3v3 승">{formatNumber(player.victories3vs3)}</StatCell>
  <StatCell label="쇼다운 승">{formatNumber(player.victoriesSolo + player.victoriesDuo)}</StatCell>
</dl>
```

### 4.2 `app/players/[tag]/_components/PlayerBrawlerCard.tsx`

```tsx
<Card className="flex flex-col gap-2 p-3 text-sm">
  <div className="flex flex-col items-center gap-1 text-center">
    <BrawlerAvatar ... />
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
```

`🏆` 이모티콘 제거. `파워 N` 텍스트 제거.

### 4.3 `app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`

라벨 텍스트 ("가젯", "스타파워", "기어"), 보유/총 카운트 ("3/4") 모두 제거. 슬롯만 시각으로 표시.

```
[가젯×N] [스타파워×N] [기어×N]   |   [버피 가젯/SP/HC]   |   [HyperCharge]
```

`flex flex-wrap gap-2` 로 모바일 대응. 한 줄로 안 들어가면 자연스럽게 줄바꿈.

```tsx
export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
      <SlotGroup label="가젯" items={pb.gadgets} kind="gadget" />
      <SlotGroup label="스타파워" items={pb.starPowers} kind="starPower" />
      <SlotGroup label="기어" items={pb.gears} kind="gear" />
      {pb.buffies && <BuffyGroup buffies={pb.buffies} />}
      {pb.hyperCharge && <HyperChargeSlot hc={pb.hyperCharge} />}
    </div>
  );
}
```

`SlotGroup` 은 `items.map((it) => <ItemSlot kind={kind} imageUrl={it.item.imageUrl} owned={it.owned} gearRarity={it.item.rarity ?? undefined} size={20} />)`.

`BuffyGroup` 은 3 슬롯 (gadget/starPower/hyperCharge buffy) 을 같은 방식으로.

> 주의: `pb.hyperCharge` (브롤러 본체의 하이퍼차지) 와 `pb.buffies.hyperCharge` (하이퍼차지 버피) 는 도메인 상 **별개의 아이템**이다. UI 에서 둘 다 표시하되, 본체 HC 는 큰 단독 슬롯으로, 버피 HC 는 BuffyGroup 안의 작은 슬롯으로 배치하여 시각적으로 구분한다 (위 레이아웃 참고).

### 4.4 `app/players/[tag]/_components/PlayerBrawlers.tsx`

정렬 컨트롤 추가.

```tsx
"use client";
import { useState } from "react";

const SORT_OPTIONS: Array<{ value: PlayerBrawlerSortKey; label: string }> = [
  { value: "trophies", label: "트로피" },
  { value: "power",    label: "파워레벨" },
  { value: "rarity",   label: "희귀도" },
  { value: "name",     label: "이름" },
  { value: "id",       label: "최신순" },
];

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  const [key, setKey] = useState<PlayerBrawlerSortKey>("trophies");
  const [dir, setDir] = useState<SortDirection>("desc");
  const sorted = sortPlayerBrawlers(brawlers, key, dir);

  return (
    <>
      <div className="mb-3 flex items-center gap-2">
        <Select value={key} onValueChange={(v) => setKey(v as PlayerBrawlerSortKey)}>
          <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant="outline" size="icon"
          aria-label={dir === "asc" ? "오름차순" : "내림차순"}
          onClick={() => setDir((d) => (d === "asc" ? "desc" : "asc"))}
        >
          {dir === "asc" ? <ArrowUp /> : <ArrowDown />}
        </Button>
      </div>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
        {sorted.map((pb) => (
          <li key={`${pb.brawler?.id ?? "x"}`}>
            <PlayerBrawlerCard pb={pb} />
          </li>
        ))}
      </ul>
    </>
  );
}
```

기본값: trophies / desc. sessionStorage 보존 안함.

### 4.5 `app/players/[tag]/_components/battle/BattlePlayerRow.tsx`

- 인라인 랭크 칩 → `<RankTierIcon tier={...} size={16} />`
- `<PowerPill>` → `<PowerLevel level={...} size={18} />`
- `<Trophy>` 호출부는 그대로

### 4.6 그 외 사용처

- `app/clubs/[tag]/_components/ClubMembers.tsx` — `<Trophy>` 호출 그대로 (자동 전파)
- `app/clubs/[tag]/_components/ClubHeader.tsx` — `<Trophy>` 호출 그대로
- `src/components/primitives/PowerPill.tsx` — 본 작업 마지막에 삭제 (사용처 0 확인 후)

---

## 5. 검증

### 5.1 자동 검증

```bash
pnpm typecheck   # GearResponse / SoloRankTier 동기화로 인한 타입 회귀 검출
pnpm lint
pnpm test:run    # itemPlaceholder, sortPlayerBrawlers, tierLabels, mappers 단위 테스트
```

### 5.2 수동 검증 (dev 서버)

- `/players/88J8Q9RPV` — PlayerHero (4 cell + 이름 옆 prestige), PlayerBrawlers (정렬 select + 토글), PlayerBrawlerCard (Trophy/PowerLevel/PrestigeBadge), Battle (RankTierIcon)
- `/clubs/2U8CVLQQ9` — Trophy 자동 전파 확인

### 5.3 회귀 위험 영역

- 트로피가 1000 또는 3000 을 넘는 플레이어/멤버 모두 새 아이콘으로 보임 — 시각 검증 필수
- `SoloRankTier` 가 `MASTERS` 단일이었던 코드 어디에도 의존하지 않음을 typecheck 가 확정 (백엔드는 이미 `MASTER_*` / `PRO` 보냄 — 프론트가 outdated 여서 런타임 에러 가능성 있었음 — 이번에 수정)
- `PowerPill` 삭제 시 다른 사용처 누락이 없는지 grep 으로 확인

---

## 6. 위험 / 호환성

- **자산 누락**: 빌드 타임 검증 없음 — `src/lib/itemPlaceholder.ts` 단위 테스트로 경로 문자열 검증 + 수동 시각 검증으로 커버.
- **`SoloRankTier` enum 변경**: 백엔드는 이미 `MASTER_*` / `PRO` 를 보내고 있음. 프론트 outdated 가 진짜 런타임 에러를 일으키고 있었는지 (현재 빈 `tierColor[tier]` 가 `undefined` 반환) 확인 필요. 본 변경으로 자동 해소.
- **모바일 레이아웃**: PlayerBrawlerCard 의 PowerLevel + PrestigeBadge + Trophy 한 줄이 좁은 폭에서 깨질 가능성 — `gap-1` + 작은 size 로 대응. 시각 검증 필요.
- **버피 lucide fallback**: 자산이 추후 들어오면 `ItemSlot.tsx` 의 buffy 분기만 수정하면 됨 (격리 잘 되어 있음).

---

## 7. 영향 범위 요약

**신규 파일** (8)
- `src/lib/itemPlaceholder.ts`
- `src/components/primitives/PowerLevel.tsx`
- `src/components/primitives/PrestigeBadge.tsx`
- `src/components/primitives/PlayerPrestigeBadge.tsx`
- `src/components/primitives/RankTierIcon.tsx`
- `src/components/primitives/ItemSlot.tsx`
- `src/domain/player/sortPlayerBrawlers.ts`
- 단위 테스트들

**수정 파일** (11)
- `src/api/types.ts`
- `src/domain/brawler/types.ts`, `mappers.ts`
- `src/domain/battle/tierLabels.ts`
- `src/components/primitives/Trophy.tsx`
- `app/players/[tag]/_components/PlayerHero.tsx`
- `app/players/[tag]/_components/PlayerBrawlerCard.tsx`
- `app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`
- `app/players/[tag]/_components/PlayerBrawlers.tsx`
- `app/players/[tag]/_components/battle/BattlePlayerRow.tsx`

**삭제 파일** (1)
- `src/components/primitives/PowerPill.tsx`

---

## 8. Out of Scope

- 자산 추가/디자인 (이미 사용자가 `public/assets/` 에 추가 완료)
- 백엔드 변경 (백엔드 `GearResponse.rarity` / `SoloRankTier` 는 이미 모두 갖춰져 있음 — 본 작업은 프론트 동기화)
- 정렬 상태 sessionStorage 보존 (단순화 우선; 추후 필요 시 별도 작업)
