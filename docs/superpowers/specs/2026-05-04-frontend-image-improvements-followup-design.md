# Frontend Image Improvements — Follow-up Design Spec

**Goal**: 이미지 1차 개선 (`2026-05-04-frontend-image-improvements-design.md`) 이후 발견된 4 가지 보완 — 트로피 아이콘 의미 분리, 아이템 슬롯 미보유 처리 통일, 플레이어 브롤러 아이템 3-row 레이아웃, 아이템 툴팁 + 브롤러 카드 클릭 네비.

**Architecture**: 기존 primitive 구조 유지. `Trophy` 단순화 + `BrawlerTrophy` 신규 분리 / `ItemSlot` 결정 트리 단순화 / `PlayerBrawlerOwnership` 명시적 3-row layout / `ItemSlot` 에 `name` prop 추가 (title 툴팁) / `PlayerBrawlerCard` 의 BrawlerAvatar 를 Link 로 감쌈.

---

## 1. Trophy / BrawlerTrophy 분리

### 1.1 결정

- **Player 트로피** (PlayerHero, ClubMembers, BattlePlayerRow 의 player 사이드, ranking 페이지 등): 항상 `icon_trophy.png`. 임계값 분기 없음.
- **Brawler 트로피** (PlayerBrawlerCard, BattlePlayerRow 의 `player.brawler.trophies`): 임계값 분기 유지 (`< 1000` → `icon_trophy.png`, `>= 1000` → `icon_prestige_trophy_2.webp`, `>= 3000` → `icon_prestige_trophy.webp`).

### 1.2 컴포넌트 분리

`imstargg-frontend/src/components/primitives/Trophy.tsx` 단순화 — 항상 `icon_trophy.png`:

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

신규 `imstargg-frontend/src/components/primitives/BrawlerTrophy.tsx`:

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

### 1.3 사용처 변경

- `PlayerHero.tsx`: `<Trophy>` 그대로 (단순화 후에도 호출부 동일)
- `app/clubs/[tag]/_components/ClubMembers.tsx`: `<Trophy>` 그대로
- `app/clubs/[tag]/_components/ClubHeader.tsx`: 그대로 (`Stat icon="icon_trophy.png"` 직접 참조)
- `PlayerBrawlerCard.tsx`: `<Trophy>` → `<BrawlerTrophy>`
- `BattlePlayerRow.tsx`: `<Trophy>` (player.brawler.trophies 인자) → `<BrawlerTrophy>`
- 그 외 ranking 페이지 등 `<Trophy>` 사용처는 player 트로피이므로 그대로.

검증: `grep -rn "<Trophy" imstargg-frontend/{src,app}` 으로 모든 사용처를 식별하고 player vs brawler 컨텍스트로 분류.

### 1.4 테스트

- `tests/unit/components/primitives/Trophy.test.tsx` 단순화: 항상 `icon_trophy.png` 만 검증 (1 테스트).
- 신규 `tests/unit/components/primitives/BrawlerTrophy.test.tsx`: 4 boundary 테스트 (999/1000/2999/3000) — 기존 Trophy 테스트와 동일한 임계값.

---

## 2. ItemSlot 미보유 처리 통일

### 2.1 결정

`gadget_empty.webp`, `gear_empty.webp`, `starpower_empty.webp` 자산을 더 이상 사용하지 않는다. 미보유 시 보유 시와 같은 src 를 `opacity: 0.3 + grayscale(1)` 로 처리 (hyperCharge 미보유 처리와 통일).

자산 자체는 `imstargg-frontend/public/assets/` 에 남겨두되 (deletion 은 별도 cleanup), 코드에서 참조하지 않는다.

### 2.2 ItemSlot 결정 트리 단순화

`imstargg-frontend/src/components/primitives/ItemSlot.tsx`:

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
  name?: string;          // 신규: 툴팁 / aria-label fallback
  ariaLabel?: string;
}

export default function ItemSlot({
  kind, imageUrl, owned, gearRarity, size = 24, name, ariaLabel,
}: Props) {
  const title = name || undefined;

  // buffy: 자산 없음 → lucide fallback
  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        title={title}
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

변경:
- `emptyPlaceholder()` import 와 호출 제거. 미보유 분기에서 emptyPlaceholder 검사 단계를 단축.
- 미보유 = src 동일 + opacity/grayscale.
- buffy + imageUrl 있을 때도 미보유 시 opacity/grayscale 자동 적용.
- `name` prop 추가 → `<img title={name}>` 으로 마우스 호버 툴팁.

### 2.3 itemPlaceholder helper 정리

`imstargg-frontend/src/lib/itemPlaceholder.ts` 의 `emptyPlaceholder()` 함수는 caller 가 0 이 됨. 본 task 에서 제거 + 단위 테스트 의 emptyPlaceholder describe 블록 삭제.

### 2.4 ItemSlot 테스트 업데이트

`tests/unit/components/primitives/ItemSlot.test.tsx`:

- 기존 "미보유 + empty 자산이 있으면 empty placeholder 를 렌더한다" 테스트 → "미보유는 base 를 흐리게 렌더한다 (gadget)" 로 변경. `expect(img).toHaveAttribute("src", "/assets/gadget_base_empty.webp")` + opacity 0.3 + filter grayscale(1) 검증.
- 기존 "hyperCharge 미보유" 테스트는 그대로.
- 신규 1 테스트: name prop 전달 시 `<img title={name}>` 가 렌더되는지 검증.

---

## 3. PlayerBrawlerOwnership 3-row 레이아웃

### 3.1 결정

명시적 3 row layout (flex-wrap 대신):

```text
Row 1: [가젯×N]  [스타파워×N]
Row 2: [기어×N]
Row 3: [HyperCharge]  [버피×3]
```

HyperCharge 가 3번째 줄 맨 왼쪽, 버피가 그 오른쪽.

### 3.2 구현

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`:

```tsx
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

- top wrapper: `flex flex-col gap-1.5` (수직 row 간격)
- 각 row: `flex items-center gap-3` (그룹 사이 간격)
- 빈 row 도 렌더 (시각적 일관성). 단, 모든 그룹이 비면 row 자체가 빈 div 가 되어 약간의 공간만 차지 — 받아들임.

### 3.3 SlotGroup 의 name 전달

`SlotGroup` 의 `<ItemSlot>` 호출에 `name={it.item.name}` 추가:

```tsx
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
```

`HyperChargeSlot` 도 동일하게 `name={hc.item.name}` 추가.

`BuffyGroup` 은 `BuffyResponse` 가 `name` 필드를 갖지 않음 (`type` 만 있음) 이므로 `name` 전달 안 함. 툴팁 없음 (사용자 요구 그대로 — "이름이 존재할 경우").

### 3.4 테스트

`tests/components/PlayerBrawlerOwnership.test.tsx` 의 슬롯 개수 검증은 그대로 유지. 새 row 구조 자체는 시각만 변경되고 a11y label 은 동일하므로 테스트 회귀 없음.

---

## 4. PlayerBrawlerCard — BrawlerAvatar 클릭 → 브롤러 상세

### 4.1 결정

브롤러가 있을 때 `BrawlerAvatar` 를 `<Link href={routes.brawler(id)}>` 로 wrap. `pb.brawler` 가 null 인 경우 wrap 하지 않음 (혹은 disabled).

### 4.2 구현

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`:

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

- `<Trophy>` → `<BrawlerTrophy>` (Section 1)
- BrawlerAvatar 가 `<Link>` 안. `pb.brawler` null 이면 wrap 안 함.
- aria-label: 스크린 리더용.

---

## 5. 검증

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

추가로 grep:

```bash
grep -rn "<Trophy " imstargg-frontend/{src,app}    # player trophy 사용처만 남아있는지
grep -rn "<BrawlerTrophy " imstargg-frontend/{src,app}  # brawler trophy 사용처
grep -rn "emptyPlaceholder" imstargg-frontend/{src,tests}  # 0 callers 확인
```

시각 검증 (사용자):
- `/players/88J8Q9RPV` — 트로피 아이콘이 player 영역에서 단일 / 브롤러 카드에서 임계값 분기 / 미보유 슬롯이 흐릿한 owned src / 3-row 레이아웃 / 아이템 호버 툴팁 / 브롤러 아이콘 클릭 → `/brawlers/{id}` 이동

---

## 6. Out of Scope

- `_empty.webp` 자산 파일 삭제는 본 spec 범위 외 (코드 미참조 후 추후 cleanup)
- BuffyResponse 에 name 필드 추가 (백엔드 변경 필요)
- a11y 보강 (RankTierIcon / PrestigeBadge ariaLabel prop) — 별도 작업
