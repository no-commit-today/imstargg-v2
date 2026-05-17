# Frontend Visual Refresh (Brawlers) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `/brawlers` 와 `/brawlers/[id]` 의 흐릿한 표 가장자리·정체된 hero·시그니처 모션 부재를 해결해, DESIGN.md 토큰을 정합화하고 게임 사이트다운 임팩트를 부여한다.

**Architecture:** `imstargg-frontend/app/styles/app.css` 에 도메인-agnostic 한 `.data-table` / `.data-row` / `.win-gauge` / `.section-title` 유틸을 추가하고, `/brawlers` 리스트 표 · 브롤러 상세 hero · 조합 / 카운터 표를 이 유틸로 재구성. 모든 인터랙션은 DESIGN.md 가 이미 정의한 `--ease-pop` 토큰을 활용하고, `prefers-reduced-motion` 으로 가드한다.

**Tech Stack:** Next.js 15 (App Router) + React 19 + TailwindCSS v4 + 기존 디자인 토큰 (`globals.css`, `app.css`) + Vitest + React Testing Library + Playwright (MCP).

**Spec:** [docs/superpowers/specs/2026-05-17-frontend-visual-refresh-design.md](../specs/2026-05-17-frontend-visual-refresh-design.md)

**File map (단위 책임)**:

- 생성: 없음 (기존 파일에만 추가)
- 수정 (CSS 유틸 추가):
  - `imstargg-frontend/app/styles/app.css` — 신규 유틸 + 모션 keyframe
- 수정 (도메인 메타 보강):
  - `imstargg-frontend/src/domain/brawler/roleMeta.ts` — 클래스 컬러 페어 추가
- 수정 (컴포넌트):
  - `imstargg-frontend/app/_components/statistics/StatNumberCell.tsx`
  - `imstargg-frontend/app/_components/statistics/TierBadge.tsx`
  - `imstargg-frontend/app/brawlers/_components/BrawlerListHeader.tsx`
  - `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx`
  - `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx`
  - `imstargg-frontend/app/brawlers/[id]/_components/PairStatsTable.tsx`
  - `imstargg-frontend/app/brawlers/[id]/_components/EnemyStatsTable.tsx`
  - `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`
  - `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx`
- 신규 테스트:
  - `imstargg-frontend/tests/components/PercentCell.test.tsx` — 신규 prop 검증

각 Task 는 단일 책임, 단일 커밋. 모든 명령은 작업 디렉토리 `imstargg-frontend/` 기준.

**Pre-flight (한 번)**:

- [ ] **dev 서버 기동** — `pnpm dev` (이미 떠 있으면 skip). 로컬 backend 가 prod 프로파일로 떠 있어야 데이터가 보임.
- [ ] **현 상태 스크린샷** (작업 전 비교용) — Playwright MCP 로 `/brawlers`, `/brawlers/16000000` 을 1440 / 375 폭에서 캡처. 파일명: `before-brawlers-{viewport}.png`, `before-shelly-{viewport}.png`.

---

### Task 1: CSS 유틸 추가 (data-table / data-row / win-gauge / section-title / row-in keyframe)

**Files:**
- Modify: `imstargg-frontend/app/styles/app.css` (끝부분에 신규 섹션 추가)

- [ ] **Step 1: `app/styles/app.css` 끝에 다음 블록을 추가**

```css
/* ===== Data table (refresh 2026-05-17) ===== */
.data-table {
  background: #fff;
  border: 1px solid var(--color-neutral-300);
  border-radius: var(--radius-md);
  box-shadow:
    0 1px 0 0 rgba(20, 22, 27, 0.06),
    0 4px 12px rgba(20, 22, 27, 0.05);
  overflow: hidden;
}
.data-table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-neutral-200);
  background: #fff;
}
.data-table-head {
  background: var(--color-neutral-900);
  color: #fff;
  border-bottom: 2px solid var(--color-brand-yellow);
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.data-table-head [data-sortable]:hover {
  color: var(--color-brand-yellow);
}
.data-row {
  position: relative;
  border-bottom: 1px solid var(--color-neutral-100);
  padding: 8px 12px 8px 16px;
  transition:
    background var(--dur-base) var(--ease-pop),
    transform var(--dur-base) var(--ease-pop);
}
.data-row:last-child { border-bottom: 0; }
.data-row:hover {
  background: #fffbe6;
  transform: translateX(2px);
}
.data-row::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--row-rail, var(--color-neutral-200));
}
.data-row[data-rail="win"]     { --row-rail: var(--color-accent-green); }
.data-row[data-rail="loss"]    { --row-rail: var(--color-accent-red); }
.data-row[data-rail="neutral"] { --row-rail: var(--color-neutral-300); }
.data-row[data-rail="gold"]    { --row-rail: var(--tier-gold); }
.data-row[data-rail="silver"]  { --row-rail: var(--tier-silver); }
.data-row[data-rail="bronze"]  { --row-rail: var(--tier-bronze); }

/* ===== Win gauge ===== */
.win-gauge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.win-gauge-track {
  width: 28px;
  height: 4px;
  border-radius: 999px;
  background: var(--color-neutral-100);
  overflow: hidden;
}
.win-gauge-fill {
  height: 100%;
  background: var(--color-accent-green);
  transition: width var(--dur-slow) var(--ease-out);
}
.win-gauge-fill[data-tone="loss"]    { background: var(--color-accent-red); }
.win-gauge-fill[data-tone="neutral"] { background: var(--color-neutral-400); }

/* ===== Section title ===== */
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-body);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.01em;
}
.section-title::before {
  content: "";
  width: 4px;
  align-self: stretch;
  border-radius: 2px;
  background: var(--color-brand-yellow);
}

/* ===== Row stagger fade-in (only first 12 rows) ===== */
@keyframes row-in {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}
.data-row { animation: row-in var(--dur-slow) var(--ease-pop) both; }
.data-row:nth-child(n+13) { animation: none; }
.data-row:nth-child(1)  { animation-delay: 0ms; }
.data-row:nth-child(2)  { animation-delay: 30ms; }
.data-row:nth-child(3)  { animation-delay: 60ms; }
.data-row:nth-child(4)  { animation-delay: 90ms; }
.data-row:nth-child(5)  { animation-delay: 120ms; }
.data-row:nth-child(6)  { animation-delay: 150ms; }
.data-row:nth-child(7)  { animation-delay: 180ms; }
.data-row:nth-child(8)  { animation-delay: 210ms; }
.data-row:nth-child(9)  { animation-delay: 240ms; }
.data-row:nth-child(10) { animation-delay: 270ms; }
.data-row:nth-child(11) { animation-delay: 300ms; }
.data-row:nth-child(12) { animation-delay: 330ms; }

@media (prefers-reduced-motion: reduce) {
  .data-row {
    animation: none !important;
    transition: none !important;
  }
  .data-row:hover { transform: none; }
  .win-gauge-fill { transition: none; }
}
```

- [ ] **Step 2: 빌드 깨지지 않는지 확인**

Run: `pnpm typecheck`
Expected: Exit code 0, no errors.

- [ ] **Step 3: dev 서버에서 시각 확인 (수동)** — 새 유틸 자체는 아직 어디서도 안 쓰임. CSS 만 추가됐으므로 기존 페이지에 변화 없음을 확인.

브라우저로 `http://localhost:3000/brawlers` 열어 기존 디자인이 그대로인지(즉, 신규 클래스가 적용되지 않아 변화 없음) 확인.

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/app/styles/app.css
git commit -m "$(cat <<'EOF'
feat(frontend): add data-table/data-row/win-gauge/section-title utilities

DESIGN.md sticker-shadow + result-bar 시그니처를 표/카드에 적용하기 위한
재사용 유틸. row-in stagger keyframe 과 prefers-reduced-motion 가드 포함.
실제 컴포넌트 적용은 후속 커밋.
EOF
)"
```

---

### Task 2: ROLE_META 에 클래스 컬러 페어 추가 (Hero 그라디언트용)

**Files:**
- Modify: `imstargg-frontend/src/domain/brawler/roleMeta.ts`

- [ ] **Step 1: `roleMeta.ts` 를 다음으로 교체**

```typescript
import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  icon: string;
  /** Hero 그라디언트용 시작 컬러 — DESIGN.md class color 매핑. */
  colorStart: string;
  /** Hero 그라디언트용 끝 컬러 — colorStart 의 명도 약 -15%. */
  colorEnd: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { icon: "icon_class_damage.svg",     colorStart: "#FF3A3A", colorEnd: "#B91515" },
  TANK:          { icon: "icon_class_tank.svg",        colorStart: "#9146FF", colorEnd: "#5C1FB7" },
  MARKSMAN:      { icon: "icon_class_marksmen.svg",    colorStart: "#FF8A1F", colorEnd: "#B85700" },
  ASSASSIN:      { icon: "icon_class_assassin.svg",    colorStart: "#FF5DAA", colorEnd: "#B82B70" },
  SUPPORT:       { icon: "icon_class_support.svg",     colorStart: "#2BD576", colorEnd: "#15914A" },
  CONTROLLER:    { icon: "icon_class_controller.svg",  colorStart: "#3DE0FF", colorEnd: "#0A91B0" },
  ARTILLERY:     { icon: "icon_class_artillery.svg",   colorStart: "#FFC60A", colorEnd: "#B58800" },
};
```

- [ ] **Step 2: 기존 `ClassIcon` 가 여전히 동작하는지 typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 3: 기존 ClassIcon 테스트 통과 확인**

Run: `pnpm vitest run tests/components/ClassIcon.test.tsx`
Expected: 2 passed.

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/src/domain/brawler/roleMeta.ts
git commit -m "$(cat <<'EOF'
feat(frontend): extend ROLE_META with class color pair

BrawlerHero 그라디언트 배경에 사용. 컬러는 DESIGN.md 의 --class-* 토큰
매핑을 따른다.
EOF
)"
```

---

### Task 3: PercentCell 에 `gauge` prop 추가 (TDD)

**Files:**
- Modify: `imstargg-frontend/app/_components/statistics/StatNumberCell.tsx`
- Test: `imstargg-frontend/tests/components/PercentCell.test.tsx` (신규)

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/components/PercentCell.test.tsx` 생성:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";

describe("PercentCell", () => {
  it("값을 퍼센트로 렌더한다 (gauge 미지정 시 게이지 없음)", () => {
    const { container } = render(<PercentCell value={0.523} />);
    expect(screen.getByText("52.3%")).toBeInTheDocument();
    expect(container.querySelector(".win-gauge")).toBeNull();
  });

  it("gauge 활성화 시 win-gauge 트랙과 채움이 렌더된다", () => {
    const { container } = render(<PercentCell value={0.6} gauge />);
    const fill = container.querySelector(".win-gauge-fill") as HTMLElement | null;
    expect(fill).not.toBeNull();
    expect(fill?.style.width).toBe("60%");
  });

  it("gauge tone='loss' 일 때 data-tone 속성을 부여한다", () => {
    const { container } = render(<PercentCell value={0.45} gauge gaugeTone="loss" />);
    const fill = container.querySelector(".win-gauge-fill") as HTMLElement;
    expect(fill.getAttribute("data-tone")).toBe("loss");
    expect(fill.style.width).toBe("45%");
  });

  it("100% 초과·음수도 0~100% 로 clamp 한다", () => {
    const { container: c1 } = render(<PercentCell value={1.5} gauge />);
    expect((c1.querySelector(".win-gauge-fill") as HTMLElement).style.width).toBe("100%");
    const { container: c2 } = render(<PercentCell value={-0.2} gauge />);
    expect((c2.querySelector(".win-gauge-fill") as HTMLElement).style.width).toBe("0%");
  });
});
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `pnpm vitest run tests/components/PercentCell.test.tsx`
Expected: 4 failed — gauge·gaugeTone 같은 props 가 없거나 게이지 DOM 이 없어서.

- [ ] **Step 3: `StatNumberCell.tsx` 를 다음으로 교체**

```tsx
type RateProps = {
  value: number;
  fractionDigits?: number;
  /** true 면 숫자 옆에 미니 게이지 바를 함께 렌더한다. */
  gauge?: boolean;
  /** 게이지 색상 톤. 기본은 win(green). */
  gaugeTone?: "win" | "loss" | "neutral";
};

function clamp01(v: number): number {
  if (Number.isNaN(v)) return 0;
  if (v < 0) return 0;
  if (v > 1) return 1;
  return v;
}

export function PercentCell({ value, fractionDigits = 1, gauge, gaugeTone = "win" }: RateProps) {
  const text = (
    <span className="font-num font-bold tabular-nums">
      {(value * 100).toFixed(fractionDigits)}%
    </span>
  );
  if (!gauge) return text;
  const pct = clamp01(value) * 100;
  return (
    <span className="win-gauge">
      {text}
      <span className="win-gauge-track" aria-hidden>
        <span
          className="win-gauge-fill"
          data-tone={gaugeTone === "win" ? undefined : gaugeTone}
          style={{ width: `${pct}%` }}
        />
      </span>
    </span>
  );
}

export function AverageRankCell({ value }: { value: number }) {
  return (
    <span className="font-num font-bold tabular-nums">{value.toFixed(2)}</span>
  );
}

export function CountCell({ value }: { value: number }) {
  return (
    <span className="font-num tabular-nums text-[#5C6271]">
      {value.toLocaleString("ko-KR")}
    </span>
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm vitest run tests/components/PercentCell.test.tsx`
Expected: 4 passed.

- [ ] **Step 5: 기존 사용처 회귀 없는지 typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 6: Commit**

```bash
git add imstargg-frontend/app/_components/statistics/StatNumberCell.tsx imstargg-frontend/tests/components/PercentCell.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): add gauge option to PercentCell

조합/카운터 표의 승률 셀이 시각적 게이지 바를 동반할 수 있도록 옵션 props
(gauge, gaugeTone) 추가. 0~100% 로 clamp. 기존 호출은 gauge=false 디폴트로
동작 변화 없음.
EOF
)"
```

---

### Task 4: TierBadge hover 모션 추가

**Files:**
- Modify: `imstargg-frontend/app/_components/statistics/TierBadge.tsx`

- [ ] **Step 1: `TierBadge.tsx` 를 다음으로 교체**

```tsx
import { TIER_COLOR, type StatTier } from "@/domain/statistics/tier";

export function TierBadge({ tier }: { tier: StatTier }) {
  const color = TIER_COLOR[tier];
  return (
    <span
      aria-label={`${tier} 티어`}
      className="tier-badge inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-extrabold text-[#14161B] tabular-nums"
      style={{
        backgroundColor: color,
        boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)",
      }}
    >
      {tier}
    </span>
  );
}
```

- [ ] **Step 2: `app/styles/app.css` 의 Tier 스타일 섹션 끝에 hover 규칙 추가** (Task 1 블록 이후)

```css
.tier-badge {
  transition: transform var(--dur-base) var(--ease-pop);
  transform-origin: center;
}
.tier-badge:hover { transform: rotate(-4deg) scale(1.08); }
@media (prefers-reduced-motion: reduce) {
  .tier-badge { transition: none; }
  .tier-badge:hover { transform: none; }
}
```

- [ ] **Step 3: typecheck + test 회귀 없는지 확인**

Run: `pnpm typecheck && pnpm test:run -- tests/components/`
Expected: typecheck pass, 기존 component 테스트 모두 pass.

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/app/_components/statistics/TierBadge.tsx imstargg-frontend/app/styles/app.css
git commit -m "$(cat <<'EOF'
feat(frontend): TierBadge hover 시 살짝 회전·확대

게임 스티커가 들리는 느낌. reduced-motion 시 미적용.
EOF
)"
```

---

### Task 5: BrawlerListHeader 를 표 toolbar 로 합치기 (시그니처 변경)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerListHeader.tsx`

> 후속 Task 6 에서 `BrawlerStatTable` 이 `BrawlerListHeader` 를 표 카드 내부 toolbar 로 흡수한다. 본 Task 는 헤더 컴포넌트가 표 카드 안에서도 자연스럽게 렌더되도록 단순 layout 변형을 만든다.

- [ ] **Step 1: `BrawlerListHeader.tsx` 를 다음으로 교체**

```tsx
"use client";

import { Input } from "@/components/ui/input";

type Props = {
  search: string;
  onSearchChange: (s: string) => void;
  totalBattleCount: number;
};

export function BrawlerListHeader({ search, onSearchChange, totalBattleCount }: Props) {
  return (
    <div className="data-table-toolbar">
      <div className="min-w-0">
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#5C6271]">
          BRAWLERS
        </div>
        <p className="mt-1 text-xs text-[#5C6271]">
          전체 전투 표본:{" "}
          <span className="font-num tabular-nums text-[#14161B]">
            {totalBattleCount.toLocaleString("ko-KR")}
          </span>
        </p>
      </div>
      <Input
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder="브롤러 이름 검색"
        className="w-full max-w-[220px]"
      />
    </div>
  );
}
```

> 페이지 타이틀 "브롤러 메타" 는 페이지 레벨로 이동(Task 6 의 page.tsx 작업과 함께). 본 헤더는 표 toolbar 역할만 한다.

- [ ] **Step 2: typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/brawlers/_components/BrawlerListHeader.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BrawlerListHeader 를 표 toolbar 형태로 단순화

페이지 타이틀(h1)은 페이지로 분리, 본 컴포넌트는 표 카드 내부 검색·표본
toolbar 역할만 한다.
EOF
)"
```

---

### Task 6: BrawlerStatTable + BrawlerStatRow refactor (data-table 적용)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx`
- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx`
- Modify: `imstargg-frontend/app/brawlers/page.tsx`

- [ ] **Step 1: `BrawlerStatRow.tsx` 를 다음으로 교체**

```tsx
import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import ClassIcon from "@/components/primitives/ClassIcon";
import { TierBadge } from "@app/_components/statistics/TierBadge";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";
import type { StatTier } from "@/domain/statistics/tier";

type Props = {
  rank: number;
  brawler: Brawler;
  stat: BrawlerResultStat | null;
};

function railFor(rank: number, tier: StatTier | undefined): string {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  switch (tier) {
    case "S": return "win";
    case "A":
    case "B": return "neutral";
    case "C":
    case "D":
    default:  return "loss";
  }
}

export function BrawlerStatRow({ rank, brawler, stat }: Props) {
  const rail = railFor(rank, stat?.tier);
  return (
    <Link
      href={`/brawlers/${brawler.id}`}
      data-rail={rail}
      className="data-row grid grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px] sm:gap-2 sm:text-sm"
    >
      <span className="font-bold text-[#5C6271] tabular-nums">{rank}</span>
      <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
        <BrawlerAvatar
          name={brawler.name}
          imageUrl={brawler.portraitImageUrl}
          rarity={brawler.rarity}
          size={36}
          className="shrink-0"
        />
        <span className="shrink-0">
          <ClassIcon role={brawler.role} size={14} />
        </span>
        <span className="truncate font-bold">{brawler.name}</span>
      </div>
      <div>{stat ? <TierBadge tier={stat.tier} /> : <TierBadge tier="D" />}</div>
      <PercentCell value={stat?.winRate ?? 0} />
      <PercentCell value={stat?.pickRate ?? 0} />
      <PercentCell value={stat?.starPlayerRate ?? 0} />
      <CountCell value={stat?.totalBattleCount ?? 0} />
    </Link>
  );
}
```

- [ ] **Step 2: `BrawlerStatTable.tsx` 를 다음으로 교체**

```tsx
"use client";

import { useMemo, useState } from "react";
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import type { SortOrder } from "@/domain/statistics/sort";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import { BrawlerStatRow } from "./BrawlerStatRow";
import { BrawlerListHeader } from "./BrawlerListHeader";
import StarIcon from "@/components/primitives/StarIcon";

type Row = { brawler: Brawler; stat: BrawlerResultStat | null };

type Props = {
  rows: Row[];
  sort: string;
  order: SortOrder;
  totalBattleCount: number;
};

function compareRows(a: Row, b: Row, sort: string, order: SortOrder): number {
  const sign = order === "DESC" ? -1 : 1;
  switch (sort) {
    case "name":
      return a.brawler.name.localeCompare(b.brawler.name, "ko") * sign;
    case "winRate":
      return ((a.stat?.winRate ?? 0) - (b.stat?.winRate ?? 0)) * sign;
    case "pickRate":
      return ((a.stat?.pickRate ?? 0) - (b.stat?.pickRate ?? 0)) * sign;
    case "starPlayerRate":
      return ((a.stat?.starPlayerRate ?? 0) - (b.stat?.starPlayerRate ?? 0)) * sign;
    case "totalBattleCount":
      return ((a.stat?.totalBattleCount ?? 0) - (b.stat?.totalBattleCount ?? 0)) * sign;
    case "tier":
    default:
      return ((a.stat?.tierScore ?? 0) - (b.stat?.tierScore ?? 0)) * sign;
  }
}

export function BrawlerStatTable({ rows, sort, order, totalBattleCount }: Props) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => r.brawler.name.toLowerCase().includes(q));
  }, [rows, search]);

  const sorted = useMemo(
    () => [...filtered].sort((a, b) => compareRows(a, b, sort, order)),
    [filtered, sort, order],
  );

  return (
    <div className="data-table">
      <BrawlerListHeader
        search={search}
        onSearchChange={setSearch}
        totalBattleCount={totalBattleCount}
      />
      <div className="data-table-head grid grid-cols-[26px_minmax(0,1fr)_36px_42px_42px_42px_52px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_56px_72px_72px_72px_96px] sm:gap-2">
        <span>순위</span>
        <SortableHeader column="name" label="브롤러" currentSort={sort} currentOrder={order} />
        <SortableHeader column="tier" label="티어" currentSort={sort} currentOrder={order} />
        <SortableHeader column="winRate" label="승률" currentSort={sort} currentOrder={order} />
        <SortableHeader column="pickRate" label="픽률" currentSort={sort} currentOrder={order} />
        <SortableHeader column="starPlayerRate" label={<StarIcon />} title="스타플레이어 비율" currentSort={sort} currentOrder={order} />
        <SortableHeader column="totalBattleCount" label="전투수" currentSort={sort} currentOrder={order} />
      </div>
      {sorted.length === 0 ? (
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          해당 조건에 데이터가 없습니다.
        </div>
      ) : (
        sorted.map((r, i) => (
          <BrawlerStatRow key={r.brawler.id} rank={i + 1} brawler={r.brawler} stat={r.stat} />
        ))
      )}
    </div>
  );
}
```

- [ ] **Step 3: `app/brawlers/page.tsx` 의 상단 영역에 페이지 타이틀(h1) 을 추가** — Task 5 에서 헤더에서 빠진 페이지 타이틀을 복원.

`app/brawlers/page.tsx` 의 `return (...)` 블록을 다음으로 교체:

```tsx
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 메타</h1>
          <p className="mt-1 text-sm text-[#5C6271]">
            <span className="font-bold text-[#14161B]">{TIER_RANGE_LABELS[tierRange]}</span> 메타
          </p>
        </div>
        <StatRangeSelector tier={tierRange} date={dateRange} />
      </header>
      <BrawlerStatTable
        rows={rows}
        sort={sort}
        order={order}
        totalBattleCount={stats.totalBattleCount}
      />
    </main>
  );
```

- [ ] **Step 4: typecheck + 빌드 핫리로드 확인**

Run: `pnpm typecheck`
Expected: Exit code 0.

브라우저에서 `http://localhost:3000/brawlers` 새로고침. 표 가장자리가 또렷해졌고(border-color 진해짐 + sticker-soft shadow), 다크 헤더 띠 + 옐로우 보더가 보이며, 1·2·3 위 행에 좌측 골드/실버/브론즈 컬러 바가 있는지 확인. row hover 시 옐로우 틴트 + 살짝 우측 이동 확인.

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx imstargg-frontend/app/brawlers/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): brawlers 표를 data-table 유틸로 재구성

다크 헤더 띠 + 옐로우 액센트, 행 좌측 4px result-bar (rank 1~3 골드/실버
/브론즈, 그 외 tier 매핑), hover 시 옐로우 틴트 + translateX, sticker-soft
box-shadow. 페이지 타이틀(h1)을 page.tsx 로 이동.
EOF
)"
```

---

### Task 7: PairStatsTable refactor (조합 표)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/PairStatsTable.tsx`

- [ ] **Step 1: `PairStatsTable.tsx` 를 다음으로 교체**

```tsx
import Link from "next/link";
import type { PairResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: PairResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

function pairRail(winRate: number): "win" | "neutral" | "loss" {
  if (winRate >= 0.55) return "win";
  if (winRate >= 0.5)  return "neutral";
  return "loss";
}

function pairTone(winRate: number): "win" | "neutral" | "loss" {
  return pairRail(winRate);
}

export function PairStatsTable({ items, sort, order }: Props) {
  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <span aria-hidden>🤝</span> 조합
        </h3>
        <span className="text-[11px] text-[#5C6271]">함께 픽됐을 때의 승률</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>조합 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="pair" />
        <span>전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.pairBrawler ? (
            <Link
              key={`${it.pairBrawler.id}-${i}`}
              href={`/brawlers/${it.pairBrawler.id}`}
              data-rail={pairRail(it.winRate)}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.pairBrawler.name}
                  imageUrl={it.pairBrawler.portraitImageUrl}
                  rarity={it.pairBrawler.rarity}
                  size={32}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.pairBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} gauge gaugeTone={pairTone(it.winRate)} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/brawlers/[id]/_components/PairStatsTable.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): PairStatsTable 을 data-table 유틸로 재구성

조합 표에 다크 헤더 + 행 좌측 result-bar (승률 ≥55% win, 50~55% neutral,
<50% loss) + 승률 셀 미니 게이지. toolbar 에 🤝 아이콘 + caption.
EOF
)"
```

---

### Task 8: EnemyStatsTable refactor (카운터 표)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/EnemyStatsTable.tsx`

- [ ] **Step 1: `EnemyStatsTable.tsx` 를 다음으로 교체**

```tsx
import Link from "next/link";
import type { EnemyResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: EnemyResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

/**
 * 카운터 표는 행의 의미가 반전된다.
 * - 적의 승률 ≥55%: 나에게 "위협(loss)" — 빨간 rail
 * - 적의 승률 ≤45%: 나에게 "좋음(win)"  — 초록 rail
 * - 그 외: neutral
 */
function enemyRail(enemyWinRate: number): "win" | "neutral" | "loss" {
  if (enemyWinRate >= 0.55) return "loss";
  if (enemyWinRate <= 0.45) return "win";
  return "neutral";
}

function enemyTone(enemyWinRate: number): "win" | "neutral" | "loss" {
  return enemyRail(enemyWinRate);
}

export function EnemyStatsTable({ items, sort, order }: Props) {
  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <span aria-hidden>⚔️</span> 카운터
        </h3>
        <span className="text-[11px] text-[#5C6271]">적 픽일 때의 적 승률</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>카운터 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <span>전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.enemyBrawler ? (
            <Link
              key={`${it.enemyBrawler.id}-${i}`}
              href={`/brawlers/${it.enemyBrawler.id}`}
              data-rail={enemyRail(it.winRate)}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.enemyBrawler.name}
                  imageUrl={it.enemyBrawler.portraitImageUrl}
                  rarity={it.enemyBrawler.rarity}
                  size={32}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.enemyBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} gauge gaugeTone={enemyTone(it.winRate)} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/brawlers/[id]/_components/EnemyStatsTable.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): EnemyStatsTable 을 data-table 유틸로 재구성

카운터 표 시그니처: 적 승률 ≥55% → loss(빨강, 위협), ≤45% → win(초록,
좋음), 그 외 neutral. 게이지 톤도 동일 매핑. ⚔️ 헤더 아이콘 + caption.
EOF
)"
```

---

### Task 9: BrawlerStatsSection — section-title 적용 + 카드 padding 통일

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx`

- [ ] **Step 1: `BrawlerStatsSection.tsx` 의 `return` 블록을 다음으로 교체**

```tsx
  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="section-title">통계</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            {TIER_RANGE_LABELS[tier]} 기준
          </p>
        </div>
        <StatRangeSelector tier={tier} date={date} />
      </div>
      <DailyTrendChart items={daily.content.map(toDailyResultStat)} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.content.map(toPairResultStat)} sort={pairSort} order={pairOrder} />
        <EnemyStatsTable items={enemy.content.map(toEnemyResultStat)} sort={enemySort} order={enemyOrder} />
      </div>
    </section>
  );
```

- [ ] **Step 2: typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): 통계 섹션 타이틀에 section-title (옐로우 액센트) 적용
EOF
)"
```

---

### Task 10: BrawlerHero 격상 (캐릭터 굿즈 카드)

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx`

- [ ] **Step 1: `BrawlerHero.tsx` 를 다음으로 교체**

```tsx
"use client";

import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import ClassIcon from "@/components/primitives/ClassIcon";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import { ROLE_META } from "@/domain/brawler/roleMeta";

const RARITY_BG: Record<string, string> = {
  STARTING_BRAWLER: "#B4B9C7",
  RARE: "#2BD576",
  SUPER_RARE: "#3DE0FF",
  EPIC: "#9146FF",
  MYTHIC: "#FF3A3A",
  LEGENDARY: "#FFC60A",
  ULTRA_LEGENDARY: "#FF8A1F",
};

export function BrawlerHero({ brawler }: { brawler: Brawler }) {
  const meta = ROLE_META[brawler.role];
  const rarityColor = RARITY_BG[brawler.rarity] ?? "#B4B9C7";

  return (
    <section
      className="relative overflow-hidden rounded-[18px] border-2 border-[#14161B] bg-white"
      style={{ boxShadow: "var(--shadow-sticker-lg)" }}
    >
      <div
        className="absolute inset-0"
        style={{
          background: `linear-gradient(135deg, ${meta.colorStart} 0%, ${meta.colorEnd} 100%)`,
          clipPath: "polygon(0 0, 62% 0, 52% 100%, 0 100%)",
        }}
        aria-hidden
      />
      <div className="relative flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:gap-6 sm:p-7">
        <div className="flex justify-center sm:justify-start">
          <BrawlerAvatar
            name={brawler.name}
            imageUrl={brawler.portraitImageUrl}
            rarity={brawler.rarity}
            size={180}
            showFrame
            priority
            className="border-[3px] border-[#14161B] shadow-[0_4px_0_0_rgba(0,0,0,0.4)]"
          />
        </div>
        <div className="flex-1 text-center sm:text-left">
          <h1 className="font-display text-4xl leading-tight tracking-[-0.01em] text-white drop-shadow-[0_2px_0_rgba(0,0,0,0.45)] sm:text-6xl">
            {brawler.name}
          </h1>
          <div className="mt-3 flex flex-wrap justify-center gap-2 sm:justify-start">
            <span
              className="pill dark inline-flex items-center gap-1.5"
              style={{ boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)" }}
            >
              <ClassIcon role={brawler.role} size={14} />
              {ROLE_LABEL_KO[brawler.role]}
            </span>
            <span
              className="pill inline-flex items-center"
              style={{
                background: rarityColor,
                color: "#14161B",
                boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)",
              }}
            >
              {RARITY_LABEL_KO[brawler.rarity]}
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Step 2: 기존 BrawlerHero 의 shadcn `<Badge>` import 가 다른 곳에서 안 쓰이는지 확인**

Run: `grep -RIn "from \"@/components/ui/badge\"" imstargg-frontend/app imstargg-frontend/src 2>/dev/null`
Expected: BrawlerHero 외 다른 사용처 0개 또는 있음 — 있으면 그대로 두고, 없으면 import 만 삭제됨. (본 Task 의 새 코드는 이미 Badge import 제거 상태.)

- [ ] **Step 3: typecheck + 기존 test 회귀 확인**

Run: `pnpm typecheck && pnpm test:run -- tests/components/`
Expected: typecheck pass, component test 회귀 없음.

- [ ] **Step 4: 시각 확인 (수동)**

브라우저에서 `http://localhost:3000/brawlers/16000000` (쉘리) 와 `/16000003` (벌), `/16000007` (콜트) 처럼 클래스가 다른 3개의 브롤러를 차례로 열어 클래스별 그라디언트가 다른지 확인. 모바일 폭(devtools) 에서 세로 스택 + 아바타 축소 확인.

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): BrawlerHero 를 캐릭터 굿즈 카드로 격상

클래스 컬러 사선 그라디언트 + 두꺼운 잉크 아웃라인 + sticker-lg shadow.
아바타 180px + 디스플레이 폰트 60px 이름. 모바일에서 자동 축소.
EOF
)"
```

---

### Task 11: 전수 회귀 검증

- [ ] **Step 1: typecheck**

Run: `pnpm typecheck`
Expected: Exit code 0.

- [ ] **Step 2: Lint**

Run: `pnpm lint`
Expected: Exit code 0, no errors.

- [ ] **Step 3: 전체 단위 테스트**

Run: `pnpm test:run`
Expected: 모든 테스트 통과.

- [ ] **Step 4: 프로덕션 빌드**

Run: `pnpm build`
Expected: Exit code 0, no errors. (시간이 걸리니 인내심을 갖자.)

- [ ] **Step 5: 시각 비교 — 작업 후 스크린샷**

Playwright MCP 로 `/brawlers`, `/brawlers/16000000` 의 1440 / 375 폭 스크린샷. 파일명: `after-brawlers-{viewport}.png`, `after-shelly-{viewport}.png`. Pre-flight 의 `before-*.png` 와 나란히 비교해 다음 체크리스트 확인:

  - [ ] 표 가장자리가 또렷 (border 진해짐 + sticker-soft shadow)
  - [ ] 표 헤더가 다크 잉크 띠 + 옐로우 보더
  - [ ] 행 좌측 4px 컬러 바 (브롤러 표: 1~3위 골드/실버/브론즈, 그 외 tier 매핑)
  - [ ] 행 hover 시 옐로우 틴트 + 살짝 우측 이동
  - [ ] BrawlerHero: 클래스 그라디언트 + 두꺼운 검정 외곽 + 큰 이름
  - [ ] 조합 / 카운터 표: 승률 옆 미니 게이지, 좌측 rail
  - [ ] 모바일 폭: 헤더 폰트·아바타 축소, 카드 비율 유지
  - [ ] 모션 시연: TierBadge hover 시 회전·확대, 표 첫 진입 시 stagger fade-in

- [ ] **Step 6: prefers-reduced-motion 검증**

Chrome DevTools → Rendering → "Emulate CSS media feature: prefers-reduced-motion" 를 `reduce` 로. `/brawlers` 새로고침 → stagger 사라짐, hover translateX 사라짐, TierBadge hover 변형 사라짐. (그러나 색 변화·border 등 정적 시각 효과는 유지.)

- [ ] **Step 7: 작업 전 / 후 한 줄 변화 메모로 마무리 commit (선택)**

만약 추가 수정 없이 통과했다면 별도 commit 불필요. Lint/typecheck/test/build 결과를 PR description 에 정리.

---

## Self-Review (작성자가 직접 점검)

**Spec coverage**:
- §3.1 표 가장자리 → Task 1 (CSS) + Task 6/7/8 (적용) ✓
- §3.2 모션 (--ease-pop, TierBadge hover, sort flip, stagger, reduced-motion) → Task 1 + Task 4 + Task 6 ✓ (sort flip 은 SortableHeader 컴포넌트 내부 처리이며 별도 변경 없음 — spec 도 "선택" 으로 분류, 본 plan 의 범위에서 제외; 필요 시 후속 plan)
- §3.3 BrawlerHero 격상 → Task 10 ✓
- §3.4 표 구조 (BrawlerStat / Pair / Enemy) → Task 6 + Task 7 + Task 8 ✓
- §3.5 글자 / 컴포넌트 사이징 → Task 6 (Row 텍스트 13/14px, 아바타 36px), Task 7/8 (Row, 아바타 32px), Task 9 (section-title 22px), Task 10 (Hero 4xl/6xl) ✓
- §3.6 컴포넌트 위치 → Task 5 (List header → toolbar), Task 6 (page.tsx 타이틀 분리), Task 9 (section-title) ✓
- §3.7 적용 파일 → 모든 파일이 Task 로 커버됨 ✓
- §3.8 후속 plan 으로 미루는 작업 → 본 plan 의 비범위로 명시 ✓
- §4 접근성 (reduced-motion) → Task 1 + Task 4 + Task 11 Step 6 ✓
- §5 테스트 → Task 3 (PercentCell 단위 테스트) + Task 11 (typecheck/lint/test/build + 수동 시각) ✓
- §6 롤백 → 각 Task 별 단일 커밋 = revert 안전 ✓
- §7 열린 결정 → Hero base-stat chip 은 본 plan 의 범위 밖, BrawlerListHeader 통합 위치는 표 카드 내부로 결정 (Task 5, 6) ✓

**Placeholder scan**: TBD/TODO/"implement later" 없음. 모든 code block 은 완성 코드. SortableHeader 의 sort flip 애니메이션만 의도적으로 본 plan 범위 밖으로 명시(spec §3.2 의 "선택").

**Type consistency**: `PercentCell` 의 신규 props `gauge`, `gaugeTone` 은 Task 3 에서 정의된 그대로 Task 7 / 8 에서 호출됨. `ROLE_META` 의 `colorStart` / `colorEnd` 는 Task 2 에서 정의, Task 10 에서 사용. `data-rail` 값(`win` / `loss` / `neutral` / `gold` / `silver` / `bronze`) 은 Task 1 의 CSS 와 Task 6 / 7 / 8 의 JSX 에서 일치.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-05-17-frontend-visual-refresh-brawlers.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
