# Frontend Design System Application Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 도메인 시각 primitive 를 mock 의존에서 떼어내 일반화하고, MobileTabBar / Player / Brawler 상세 / Brawler 메타 4 진입 페이지에 일관 적용한다.

**Architecture:** `Trophy`, `ClassIcon`, `BrawlerAvatar` 가 도메인 모델만 받게 시그니처를 정리하고 (`BrawlerPortrait` 는 `BrawlerAvatar` 로 흡수, `lib/data.ts` 는 사용처 정리 후 가능하면 삭제). MobileTabBar 의 emoji 를 정적 자산으로 교체. Player 페이지는 단일 Hero 카드 + 탭(배틀 default · 브롤러) 으로 정보 중복을 제거. Brawler 상세 헤더에 ClassIcon 추가, 버피 enum 한글 매핑, LCP eager. Brawler 메타 행에 ClassIcon 인라인. 컨테이너 폭 `max-w-7xl` 통일.

**Tech Stack:** Next.js 16.2.1 App Router · React 19 · TanStack Query · Tailwind 4 · Vitest · Testing Library · `lucide-react` · `next/image`(unoptimized).

**Spec:** `docs/superpowers/specs/2026-04-30-frontend-design-system-application-design.md`

---

## File Structure

### Create
- `imstargg-frontend/src/domain/brawler/roleMeta.ts` — `BrawlerRole → { color, icon }` 매핑
- `imstargg-frontend/src/domain/brawler/buffyTypeLabel.ts` — `BuffyResponse.buffyType → 한글` 매핑 함수
- `imstargg-frontend/src/components/primitives/PlayerAvatar.tsx` — 플레이어 이니셜 아바타
- `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx` — 통합 hero 카드
- `imstargg-frontend/tests/components/Trophy.test.tsx`
- `imstargg-frontend/tests/components/ClassIcon.test.tsx`
- `imstargg-frontend/tests/components/PlayerAvatar.test.tsx`
- `imstargg-frontend/tests/components/MobileTabBar.test.tsx`
- `imstargg-frontend/tests/components/PlayerHero.test.tsx`
- `imstargg-frontend/tests/lib/buffyTypeLabel.test.ts`

### Modify
- `imstargg-frontend/src/components/primitives/Trophy.tsx` — `lib/data` → `lib/format`
- `imstargg-frontend/src/components/primitives/ClassIcon.tsx` — `cls: ClassKey` → `role: BrawlerRole`
- `imstargg-frontend/src/lib/format.ts` — `fmtMinAgo` 추가
- `imstargg-frontend/src/components/layout/TopNav.tsx` — `lib/data` → `lib/format`
- `imstargg-frontend/src/components/layout/MobileTabBar.tsx` — emoji → 자산
- `imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx` — `BrawlerPortrait` → `BrawlerAvatar`
- `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx` — `BrawlerPortrait` → `BrawlerAvatar`
- `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx` — Hero 통합, 통계 탭 제거, 기본 탭 = 배틀, 컨테이너 폭
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx` — ClassIcon 추가
- `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx` — 버피 한글 매핑
- `imstargg-frontend/app/brawlers/[id]/page.tsx` — portrait LCP eager (확인 후 적용)
- `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx` — ClassIcon 인라인
- `imstargg-frontend/app/brawlers/page.tsx` — 컨테이너 폭

### Delete
- `imstargg-frontend/src/components/primitives/BrawlerPortrait.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerSidebar.tsx` (PlayerHero 로 대체)
- `imstargg-frontend/app/players/[tag]/_components/PlayerStats.tsx`
- `imstargg-frontend/src/lib/data.ts` (사용처가 모두 정리되면 마지막 task 에서 삭제)

---

## Task 1: Trophy primitive — mock 의존 제거

**Files:**
- Modify: `imstargg-frontend/src/components/primitives/Trophy.tsx`
- Test: `imstargg-frontend/tests/components/Trophy.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

```tsx
// imstargg-frontend/tests/components/Trophy.test.tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import Trophy from "@/components/primitives/Trophy";

describe("Trophy", () => {
  it("값을 ko-KR 천단위 구분으로 포맷한다", () => {
    render(<Trophy value={34679} />);
    expect(screen.getByText("34,679")).toBeInTheDocument();
  });

  it("trophy.png 이미지를 렌더한다", () => {
    const { container } = render(<Trophy value={1000} size={20} />);
    const img = container.querySelector("img");
    expect(img?.getAttribute("src")).toBe("/assets/trophy.png");
    expect(img?.getAttribute("alt")).toBe("");
  });
});
```

- [ ] **Step 2: Run test to verify it fails (mock 의존)**

```bash
cd imstargg-frontend
pnpm test:run tests/components/Trophy.test.tsx
```

Expected: FAIL — 첫 번째 테스트는 통과할 수 있으나 mock import 가 안전하게 동작하지 않으면 실패. 어쨌든 명시 검증을 추가한 것이므로 새 테스트 파일이 만들어진 것을 확인.

- [ ] **Step 3: 구현 변경 — `lib/format` 으로 교체**

`imstargg-frontend/src/components/primitives/Trophy.tsx` 전체를 다음으로:

```tsx
import { formatNumber } from "@/lib/format";

interface Props {
  value: number;
  size?: number;
}

export default function Trophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src="/assets/trophy.png" alt="" style={{ width: size, height: size }} />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
```

- [ ] **Step 4: 테스트 + typecheck 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/Trophy.test.tsx
pnpm typecheck
```

Expected: PASS / 0 error.

- [ ] **Step 5: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/primitives/Trophy.tsx imstargg-frontend/tests/components/Trophy.test.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): Trophy primitive 의 lib/data 의존 제거

formatNumber 로 교체하고 사용자 관점 단위 테스트 추가.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: ClassIcon — `BrawlerRole` 시그니처 + roleMeta

**Files:**
- Create: `imstargg-frontend/src/domain/brawler/roleMeta.ts`
- Modify: `imstargg-frontend/src/components/primitives/ClassIcon.tsx`
- Test: `imstargg-frontend/tests/components/ClassIcon.test.tsx` (create)

- [ ] **Step 1: roleMeta.ts 작성**

`imstargg-frontend/src/domain/brawler/roleMeta.ts`:

```ts
import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  color: string;
  icon: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { color: "var(--class-damage)",     icon: "class_damage.svg" },
  TANK:          { color: "var(--class-tank)",       icon: "class_tank.svg" },
  MARKSMAN:      { color: "var(--class-marksman)",   icon: "class_marksmen.svg" },
  ASSASSIN:      { color: "var(--class-assassin)",   icon: "class_assassin.svg" },
  SUPPORT:       { color: "var(--class-support)",    icon: "class_support.svg" },
  CONTROLLER:    { color: "var(--class-controller)", icon: "class_controller.svg" },
  ARTILLERY:     { color: "var(--class-artillery)",  icon: "class_artillery.svg" },
};
```

- [ ] **Step 2: Write the failing test**

`imstargg-frontend/tests/components/ClassIcon.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import ClassIcon from "@/components/primitives/ClassIcon";

describe("ClassIcon", () => {
  it("DAMAGE_DEALER 일 때 class_damage.svg 이미지를 렌더한다", () => {
    render(<ClassIcon role="DAMAGE_DEALER" />);
    const img = screen.getByRole("img", { name: "대미지 딜러" });
    expect(img.getAttribute("src")).toBe("/assets/class_damage.svg");
  });

  it("TANK 일 때 class_tank.svg 이미지를 렌더한다", () => {
    render(<ClassIcon role="TANK" />);
    const img = screen.getByRole("img", { name: "탱커" });
    expect(img.getAttribute("src")).toBe("/assets/class_tank.svg");
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd imstargg-frontend
pnpm test:run tests/components/ClassIcon.test.tsx
```

Expected: FAIL — 시그니처가 `cls: ClassKey` 라 `role` prop 미정의.

- [ ] **Step 4: ClassIcon 구현 교체**

`imstargg-frontend/src/components/primitives/ClassIcon.tsx` 전체:

```tsx
import type { CSSProperties } from "react";
import type { BrawlerRole } from "@/api/types";
import { ROLE_META } from "@/domain/brawler/roleMeta";
import { ROLE_LABEL_KO } from "@/domain/brawler/labels";

interface Props {
  role: BrawlerRole;
  size?: number;
}

export default function ClassIcon({ role, size = 18 }: Props) {
  const meta = ROLE_META[role];
  const style: CSSProperties & Record<string, string | number> = {
    "--cls-color": meta.color,
    width: size,
    height: size,
  };
  return (
    <span className="class-icon" style={style}>
      <img
        src={`/assets/${meta.icon}`}
        alt={ROLE_LABEL_KO[role]}
        style={{ width: size * 0.65, height: size * 0.65 }}
      />
    </span>
  );
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/ClassIcon.test.tsx
pnpm typecheck
```

Expected: PASS / 0 error. ClassIcon 사용처 0건이므로 타입 회귀 없음.

- [ ] **Step 6: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/primitives/ClassIcon.tsx \
        imstargg-frontend/src/domain/brawler/roleMeta.ts \
        imstargg-frontend/tests/components/ClassIcon.test.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): ClassIcon 시그니처를 BrawlerRole 도메인 enum 으로

mock ClassKey 의존 제거. roleMeta.ts 매핑 신설 + 단위 테스트 추가.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: `BrawlerPortrait` → `BrawlerAvatar` 통합

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx`
- Delete: `imstargg-frontend/src/components/primitives/BrawlerPortrait.tsx`

- [ ] **Step 1: BattleRowHeader 의 BrawlerPortrait 교체**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx` 의 import 와 사용처 수정:

```tsx
// 라인 1: import 교체
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
```

라인 50-52 근처의 `ownerBrawlerId` 계산 블록을 다음으로 교체:

```tsx
const ownerBrawler = owner?.brawler.brawler ?? null;
```

라인 91 근처의 사용 라인을 다음으로 교체:

```tsx
<BrawlerAvatar
  name={ownerBrawler?.name ?? "?"}
  imageUrl={ownerBrawler?.portraitImageUrl ?? null}
  size={28}
/>
```

- [ ] **Step 2: BattlePlayerRow 의 BrawlerPortrait 교체**

`imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx` 의 import 와 사용처 수정:

```tsx
// 라인 2: import 교체
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
```

라인 18-20 의 `brawlerId` 블록을 다음으로 교체:

```tsx
const playerBrawler = player.brawler.brawler;
```

라인 34 의 사용 라인을 다음으로 교체:

```tsx
<BrawlerAvatar
  name={playerBrawler?.name ?? "?"}
  imageUrl={playerBrawler?.portraitImageUrl ?? null}
  size={32}
/>
```

- [ ] **Step 3: BrawlerPortrait.tsx 삭제**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
rm imstargg-frontend/src/components/primitives/BrawlerPortrait.tsx
```

- [ ] **Step 4: typecheck + lint + 기존 배틀 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

Expected: 모두 통과. `BrawlerPortrait` 잔존 import 없음 (`grep -rn "BrawlerPortrait" imstargg-frontend/{app,src,tests}` 가 0건).

- [ ] **Step 5: dev 서버에서 배틀 행 렌더 확인**

`pnpm dev` 가 이미 떠 있으면 skip. 그렇지 않으면:

```bash
cd imstargg-frontend
pnpm dev > /tmp/dev.log 2>&1 &
sleep 8
```

브라우저(또는 chrome-devtools) 로 `http://localhost:3000/players/88J8Q9RPV` → "배틀" 탭 → 배틀 행에 브롤러 portrait 이미지가 보이는지(이전엔 그라디언트 + 이니셜만이었음) 육안 확인.

- [ ] **Step 6: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx \
        imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx \
        imstargg-frontend/src/components/primitives/BrawlerPortrait.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BrawlerPortrait 를 BrawlerAvatar 로 통합

배틀 헤더/플레이어 행이 도메인 brawler 의 portraitImageUrl 을 사용해
실제 CDN 이미지를 노출. 구파 BrawlerPortrait 와 mock id lookup 제거.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: `lib/data.ts` 정리 — `fmtMinAgo` 이전, mock 파일 삭제

**Files:**
- Modify: `imstargg-frontend/src/lib/format.ts`
- Modify: `imstargg-frontend/src/components/layout/TopNav.tsx`
- Delete: `imstargg-frontend/src/lib/data.ts` (잔존 사용처 0 확인 후)

- [ ] **Step 1: format.ts 에 fmtMinAgo 추가**

`imstargg-frontend/src/lib/format.ts` 끝에 추가:

```ts
export function formatMinutesAgo(min: number): string {
  if (min < 1) return "방금 전";
  if (min < 60) return `${min}분 전`;
  const hours = Math.floor(min / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}
```

- [ ] **Step 2: TopNav 의 import 와 사용처 교체**

`imstargg-frontend/src/components/layout/TopNav.tsx` 라인 4 를 다음으로 교체:

```tsx
import { formatMinutesAgo } from "@/lib/format";
```

라인 266 의 `fmtMinAgo(...)` 를 `formatMinutesAgo(...)` 로 교체 (첫 인자 동일):

```tsx
{now ? formatMinutesAgo(Math.max(0, Math.floor((now - r.ts) / 60000))) : ""}
```

- [ ] **Step 3: lib/data 잔존 사용처 검사**

```bash
cd imstargg-frontend
grep -rn "from.*lib/data\|from.*@/lib/data" app src tests 2>&1
```

Expected: 0 건.

- [ ] **Step 4: lib/data.ts 삭제**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
rm imstargg-frontend/src/lib/data.ts
```

- [ ] **Step 5: typecheck + lint + test 통과 확인**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

Expected: 0 error.

- [ ] **Step 6: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/format.ts \
        imstargg-frontend/src/components/layout/TopNav.tsx \
        imstargg-frontend/src/lib/data.ts
git commit -m "$(cat <<'EOF'
chore(frontend): lib/data mock 파일 제거

남은 사용처(TopNav 의 fmtMinAgo)는 formatMinutesAgo 로 옮겨
lib/format 에 통합. mock 의존 0 도달.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: MobileTabBar 자산 교체

**Files:**
- Modify: `imstargg-frontend/src/components/layout/MobileTabBar.tsx`
- Test: `imstargg-frontend/tests/components/MobileTabBar.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

`imstargg-frontend/tests/components/MobileTabBar.test.tsx`:

```tsx
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import MobileTabBar from "@/components/layout/MobileTabBar";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/",
}));

describe("MobileTabBar", () => {
  it("emoji 가 아닌 자산 또는 line icon 으로 5 탭을 렌더한다", () => {
    const { container } = render(<MobileTabBar />);
    const text = container.textContent ?? "";
    for (const emoji of ["🏠", "⭐", "🗓️", "🏆", "🛡️"]) {
      expect(text.includes(emoji)).toBe(false);
    }
    for (const label of ["홈", "브롤러", "이벤트", "랭킹", "클럽"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });

  it("랭킹 탭에 trophy.png 자산을 사용한다", () => {
    const { container } = render(<MobileTabBar />);
    const imgs = Array.from(container.querySelectorAll("img"));
    expect(imgs.some((i) => i.getAttribute("src") === "/assets/trophy.png")).toBe(true);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd imstargg-frontend
pnpm test:run tests/components/MobileTabBar.test.tsx
```

Expected: FAIL — 현재 emoji 가 들어 있어 첫 케이스 실패.

- [ ] **Step 3: MobileTabBar 구현 교체**

`imstargg-frontend/src/components/layout/MobileTabBar.tsx` 전체:

```tsx
"use client";
import { useRouter, usePathname } from "next/navigation";
import { Calendar } from "lucide-react";
import type { ReactNode } from "react";

interface Tab {
  route: string;
  label: string;
  icon: ReactNode;
  match?: (r: string) => boolean;
}

const ASSET_ICON = (src: string, alt: string) => (
  <img src={src} alt={alt} width={20} height={20} />
);

const TABS: Tab[] = [
  {
    route: "/",
    label: "홈",
    icon: ASSET_ICON("/assets/star.webp", ""),
    match: (r) => r === "/" || r.startsWith("/players/"),
  },
  { route: "/brawlers", label: "브롤러", icon: ASSET_ICON("/assets/star_wings.webp", "") },
  { route: "/events", label: "이벤트", icon: <Calendar size={20} strokeWidth={2.25} aria-hidden /> },
  { route: "/ranking", label: "랭킹", icon: ASSET_ICON("/assets/trophy.png", "") },
  { route: "/clubs", label: "클럽", icon: ASSET_ICON("/assets/club_league.png", "") },
];

export default function MobileTabBar() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const isActive = (prefix: string) => pathname === prefix || pathname.startsWith(prefix + "/");
  return (
    <div className="mobile-tabbar">
      {TABS.map((t) => {
        const active = t.match ? t.match(pathname) : isActive(t.route);
        return (
          <a
            key={t.route}
            className={active ? "active" : ""}
            onClick={() => router.push(t.route)}
            style={{ cursor: "pointer" }}
          >
            <span className="tab-icon">{t.icon}</span>
            <span>{t.label}</span>
          </a>
        );
      })}
    </div>
  );
}
```

- [ ] **Step 4: 테스트 + 시각 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/MobileTabBar.test.tsx
```

Expected: PASS.

브라우저(390px viewport)에서 `http://localhost:3000/` 를 열고 하단 탭바에 emoji 가 사라지고 자산이 보이는지 육안 확인.

- [ ] **Step 5: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/layout/MobileTabBar.tsx \
        imstargg-frontend/tests/components/MobileTabBar.test.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): MobileTabBar 의 emoji 를 정적 자산 + line icon 으로 교체

홈/브롤러/랭킹/클럽 4 탭은 /assets 자산, 이벤트는 lucide Calendar.
DESIGN.md 의 브랜드 아이콘 정합.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Player Hero 카드 통합 + 통계 탭 제거 + 컨테이너 폭

**Files:**
- Create: `imstargg-frontend/src/components/primitives/PlayerAvatar.tsx`
- Create: `imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx`
- Delete: `imstargg-frontend/app/players/[tag]/_components/PlayerSidebar.tsx`
- Delete: `imstargg-frontend/app/players/[tag]/_components/PlayerStats.tsx`
- Test: `imstargg-frontend/tests/components/PlayerAvatar.test.tsx` (create)
- Test: `imstargg-frontend/tests/components/PlayerHero.test.tsx` (create)

- [ ] **Step 1: PlayerAvatar 테스트 + 구현 (TDD 한 라운드)**

`imstargg-frontend/tests/components/PlayerAvatar.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";

describe("PlayerAvatar", () => {
  it("이름의 첫 글자를 이니셜로 노출한다", () => {
    render(<PlayerAvatar name="惡鬼滅殺" />);
    expect(screen.getByText("惡")).toBeInTheDocument();
  });
});
```

`imstargg-frontend/src/components/primitives/PlayerAvatar.tsx`:

```tsx
import type { CSSProperties } from "react";

interface Props {
  name: string;
  size?: number;
  className?: string;
}

export function PlayerAvatar({ name, size = 80, className = "" }: Props) {
  const initial = (name ?? "?").slice(0, 1) || "?";
  const fontSize = Math.max(16, size * 0.4);
  const style: CSSProperties = { width: size, height: size, fontSize };
  return (
    <div
      className={`flex items-center justify-center rounded-md bg-gradient-to-br from-[#FFC60A] to-[#FF8A1F] font-display font-extrabold text-white ${className}`}
      style={style}
      title={name}
    >
      {initial}
    </div>
  );
}
```

- [ ] **Step 2: PlayerAvatar 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/PlayerAvatar.test.tsx
```

Expected: PASS.

- [ ] **Step 3: PlayerHero 테스트 작성**

`imstargg-frontend/tests/components/PlayerHero.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PlayerHero } from "@/app/players/[tag]/_components/PlayerHero";
import type { Player } from "@/domain/player/types";

const player: Player = {
  tag: "#88J8Q9RPV",
  name: "惡鬼滅殺",
  nameColor: null,
  iconId: 28000010,
  trophies: 34679,
  highestTrophies: 34679,
  expLevel: 150,
  expPoints: 116278,
  victories3vs3: 7286,
  victoriesSolo: 321,
  victoriesDuo: 184,
  totalPrestigeLevel: 2,
  club: { tag: "#SJCE", name: "SJCE" },
};

function wrap(ui: React.ReactNode) {
  const qc = new QueryClient();
  return <QueryClientProvider client={qc}>{ui}</QueryClientProvider>;
}

describe("PlayerHero", () => {
  it("이름·태그·6 stats·클럽을 모두 노출한다", () => {
    render(wrap(<PlayerHero player={player} />));
    expect(screen.getByRole("heading", { name: "惡鬼滅殺" })).toBeInTheDocument();
    expect(screen.getByText("#88J8Q9RPV")).toBeInTheDocument();
    expect(screen.getAllByText("34,679").length).toBeGreaterThanOrEqual(2); // trophies + highest
    expect(screen.getByText("7,286")).toBeInTheDocument(); // 3v3
    expect(screen.getByText("321")).toBeInTheDocument(); // solo
    expect(screen.getByText(/SJCE/)).toBeInTheDocument();
  });

  it("club null 이면 '소속 클럽 없음' 을 노출한다", () => {
    const noClub: Player = { ...player, club: null };
    render(wrap(<PlayerHero player={noClub} />));
    expect(screen.getByText("소속 클럽 없음")).toBeInTheDocument();
  });
});
```

- [ ] **Step 4: Run test to verify it fails**

```bash
cd imstargg-frontend
pnpm test:run tests/components/PlayerHero.test.tsx
```

Expected: FAIL — `PlayerHero` 가 아직 없음.

- [ ] **Step 5: PlayerHero 구현**

`imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx`:

```tsx
"use client";

import Link from "next/link";
import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";
import Trophy from "@/components/primitives/Trophy";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { routes } from "@/config/routes";

const STAT_NUM_CLASS =
  "font-num font-bold text-[#14161B] tabular-nums [font-variant-numeric:tabular-nums]";

export function PlayerHero({ player }: { player: Player }) {
  return (
    <Card className="flex flex-col gap-5 p-6 sm:p-7">
      <header className="flex items-center gap-4 sm:gap-6">
        <PlayerAvatar name={player.name} size={80} />
        <div className="min-w-0 flex-1">
          <h1
            className="truncate text-2xl font-bold sm:text-3xl"
            style={player.nameColor ? { color: player.nameColor } : undefined}
          >
            {player.name}
          </h1>
          <p className="font-mono text-sm text-[#5C6271]">{player.tag}</p>
          <p className="mt-1 flex items-center gap-1.5 text-sm">
            {player.club ? (
              <>
                <img src="/assets/club_league.png" alt="" width={16} height={16} />
                <span className="text-[#5C6271]">클럽:</span>
                <Link className="font-medium hover:underline" href={routes.club(player.club.tag)}>
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
      <dl className="grid grid-cols-3 gap-3 sm:grid-cols-6">
        <StatCell label="트로피">
          <Trophy value={player.trophies} size={20} />
        </StatCell>
        <StatCell label="최고 트로피">
          <Trophy value={player.highestTrophies} size={20} />
        </StatCell>
        <StatCell label="프레스티지">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.totalPrestigeLevel)}</span>
        </StatCell>
        <StatCell label="EXP Lv">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.expLevel)}</span>
        </StatCell>
        <StatCell label="3v3 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.victories3vs3)}</span>
        </StatCell>
        <StatCell label="솔로 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.victoriesSolo)}</span>
        </StatCell>
      </dl>
    </Card>
  );
}

function StatCell({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1 rounded-md bg-[#F6F7FA] px-3 py-2">
      <dt className="text-xs font-medium text-[#5C6271]">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
```

- [ ] **Step 6: PlayerHero 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/PlayerHero.test.tsx
```

Expected: PASS (양 테스트).

- [ ] **Step 7: PlayerContent 정리**

`imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePlayer } from "@/hooks/queries/usePlayer";
import { usePlayerBrawlers } from "@/hooks/queries/usePlayerBrawlers";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import type { RenewalResponse } from "@/api/types";
import { PlayerHero } from "./PlayerHero";
import { PlayerBrawlers } from "./PlayerBrawlers";
import { PlayerBattles } from "./PlayerBattles";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type Props = {
  tag: string;
  initialPlayer: Player;
  initialBrawlers: PlayerBrawler[];
  initialRenewal: RenewalResponse | null;
};

export function PlayerContent({
  tag,
  initialPlayer,
  initialBrawlers,
  initialRenewal,
}: Props) {
  const qc = useQueryClient();

  useEffect(() => {
    if (initialRenewal) {
      qc.setQueryData(queryKeys.player.renewal(tag), initialRenewal);
    }
  }, [tag, initialRenewal, qc]);

  const { data: player } = usePlayer(tag, { initialData: initialPlayer });
  const { data: brawlers } = usePlayerBrawlers(tag, { initialData: initialBrawlers });

  if (!player) return null;
  return (
    <main className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6">
      <PlayerHero player={player} />
      <Tabs defaultValue="battles" className="w-full">
        <TabsList>
          <TabsTrigger value="battles">배틀</TabsTrigger>
          <TabsTrigger value="brawlers">브롤러</TabsTrigger>
        </TabsList>
        <TabsContent value="battles">
          <PlayerBattles tag={tag} />
        </TabsContent>
        <TabsContent value="brawlers">
          <PlayerBrawlers brawlers={brawlers ?? []} />
        </TabsContent>
      </Tabs>
    </main>
  );
}
```

- [ ] **Step 8: 구파 컴포넌트 삭제**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
rm imstargg-frontend/app/players/[tag]/_components/PlayerSidebar.tsx
rm imstargg-frontend/app/players/[tag]/_components/PlayerStats.tsx
```

- [ ] **Step 9: typecheck + 전체 테스트 + dev 시각 확인**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

Expected: 통과.

브라우저로 `http://localhost:3000/players/88J8Q9RPV` → Hero 카드 + 6 stats + 배틀 탭(기본) 확인. 통계 탭이 없는지, 배틀 행이 즉시 보이는지.

- [ ] **Step 10: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/primitives/PlayerAvatar.tsx \
        imstargg-frontend/tests/components/PlayerAvatar.test.tsx \
        imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx \
        imstargg-frontend/tests/components/PlayerHero.test.tsx \
        imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx \
        imstargg-frontend/app/players/[tag]/_components/PlayerSidebar.tsx \
        imstargg-frontend/app/players/[tag]/_components/PlayerStats.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Player 페이지 Hero 카드 통합 + 기본 탭 = 배틀

Sidebar/Stats 카드를 Hero 한 장(아바타 + 이름/태그/클럽 + 6 stats)
으로 통합하고 정보 중복 제거. 통계 탭 삭제, 배틀 탭이 기본.
컨테이너 폭 max-w-7xl 로 확대.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Brawler 상세 + 메타 — ClassIcon, 버피 한글, LCP eager, 컨테이너 폭

**Files:**
- Create: `imstargg-frontend/src/domain/brawler/buffyTypeLabel.ts`
- Test: `imstargg-frontend/tests/lib/buffyTypeLabel.test.ts` (create)
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx`
- Modify: `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx`
- Modify: `imstargg-frontend/app/brawlers/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx` (확인 후)

- [ ] **Step 1: buffyTypeLabel 테스트 + 구현 (TDD)**

`imstargg-frontend/tests/lib/buffyTypeLabel.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";

describe("buffyTypeLabel", () => {
  it("알려진 enum 을 한글로 매핑한다", () => {
    expect(buffyTypeLabel("GADGET")).toBe("가젯");
    expect(buffyTypeLabel("HYPER_CHARGE")).toBe("하이퍼차지");
    expect(buffyTypeLabel("STAR_POWER")).toBe("스타파워");
    expect(buffyTypeLabel("MYTHIC_GEAR")).toBe("신화 기어");
  });

  it("알려지지 않은 값은 원문을 반환한다", () => {
    expect(buffyTypeLabel("UNKNOWN_X")).toBe("UNKNOWN_X");
  });
});
```

`imstargg-frontend/src/domain/brawler/buffyTypeLabel.ts`:

```ts
const KNOWN: Record<string, string> = {
  GADGET: "가젯",
  HYPER_CHARGE: "하이퍼차지",
  STAR_POWER: "스타파워",
  MYTHIC_GEAR: "신화 기어",
};

export function buffyTypeLabel(buffyType: string): string {
  return KNOWN[buffyType] ?? buffyType;
}
```

- [ ] **Step 2: buffyTypeLabel 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/lib/buffyTypeLabel.test.ts
```

Expected: PASS.

- [ ] **Step 3: ItemOwnershipSection 의 enum → 한글 매핑 적용**

`imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx` 의 import 영역에 추가:

```tsx
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";
```

라인 102 의 `<ItemRow key={b.buffyType} name={b.buffyType} rate={b.ownershipRate} />` 를 다음으로 교체:

```tsx
<ItemRow key={b.buffyType} name={buffyTypeLabel(b.buffyType)} rate={b.ownershipRate} />
```

- [ ] **Step 4: BrawlerHero 에 ClassIcon 추가**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx` 의 import 영역에 추가:

```tsx
import ClassIcon from "@/components/primitives/ClassIcon";
```

`<Badge variant="secondary">{ROLE_LABEL_KO[brawler.role]}</Badge>` 을 다음으로 교체:

```tsx
<Badge variant="secondary" className="inline-flex items-center gap-1.5">
  <ClassIcon role={brawler.role} size={16} />
  {ROLE_LABEL_KO[brawler.role]}
</Badge>
```

- [ ] **Step 5: BrawlerStatRow 의 클래스 인디케이터 추가**

`imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx` 의 import 영역에 추가:

```tsx
import ClassIcon from "@/components/primitives/ClassIcon";
```

라인 24-36 의 "이름 + portrait" 영역을 다음으로 교체 (`portraitImageUrl` 분기 그대로 + ClassIcon 추가):

```tsx
<div className="flex items-center gap-2 overflow-hidden">
  {brawler.portraitImageUrl && (
    <Image
      src={brawler.portraitImageUrl}
      alt={brawler.name}
      width={32}
      height={32}
      unoptimized
      className="h-8 w-8 rounded-md"
    />
  )}
  <ClassIcon role={brawler.role} size={16} />
  <span className="truncate text-sm font-bold">{brawler.name}</span>
</div>
```

- [ ] **Step 6: 컨테이너 폭 정합 (Brawler 메타)**

`imstargg-frontend/app/brawlers/page.tsx` 라인 48 을:

```tsx
<main className="mx-auto w-full max-w-7xl space-y-5 px-4 py-6">
```

(기존 `max-w-6xl` 을 `max-w-7xl` 로 교체)

- [ ] **Step 7: Brawler 상세 LCP eager 확인 + 적용**

```bash
grep -n "BrawlerAvatar\|portraitImageUrl\|priority" imstargg-frontend/app/brawlers/\[id\]/_components/BrawlerHero.tsx
```

`BrawlerAvatar` 가 hero 에 portrait 이미지를 노출하므로, 그 컴포넌트가 `next/image` 에 `priority` 를 받게 한다.

`imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx` 의 Props 에 `priority?: boolean` 추가:

```tsx
interface Props {
  name: string;
  imageUrl: string | null;
  rarity?: BrawlerRarity;
  size?: number;
  className?: string;
  showFrame?: boolean;
  priority?: boolean;
}
```

`Image` 사용 부에 `priority={priority}` 전달:

```tsx
<Image
  src={imageUrl}
  alt={name}
  width={size}
  height={size}
  unoptimized
  priority={priority}
  className="h-full w-full object-cover"
/>
```

`BrawlerHero.tsx` 의 BrawlerAvatar 사용 라인에 `priority` 추가:

```tsx
<BrawlerAvatar
  name={brawler.name}
  imageUrl={brawler.portraitImageUrl}
  rarity={brawler.rarity}
  size={140}
  showFrame
  priority
/>
```

- [ ] **Step 8: typecheck + lint + 전체 테스트**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

Expected: 통과.

- [ ] **Step 9: dev 시각 확인 — 4 페이지 콘솔 0 warning**

브라우저(또는 chrome-devtools)로 다음 4 URL 을 확인하고 각각 콘솔에 error/warn 0 인지 확인:

- `http://localhost:3000/` — 홈
- `http://localhost:3000/brawlers` — Brawler 메타 (행에 클래스 아이콘 보이는지)
- `http://localhost:3000/brawlers/16000000` — Brawler 상세 (LCP 경고 사라짐, ClassIcon, 버피 한글)
- `http://localhost:3000/players/88J8Q9RPV` — Player (Hero, 배틀 default)

- [ ] **Step 10: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/domain/brawler/buffyTypeLabel.ts \
        imstargg-frontend/tests/lib/buffyTypeLabel.test.ts \
        imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx \
        imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx \
        imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx \
        imstargg-frontend/app/brawlers/page.tsx \
        imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Brawler 상세/메타 시각 강화 + LCP eager

- Brawler 상세 헤더에 ClassIcon, 버피 enum 한글 매핑
- Brawler 메타 행 이름 옆에 ClassIcon 인라인
- portrait 이미지에 priority 전달로 LCP 경고 해소
- 컨테이너 폭 max-w-7xl 통일

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review Notes

- **Spec coverage**:
  - Goal 1 (primitive 일반화) → Task 1, 2, 3, 4 ✅
  - Goal 2 (MobileTabBar 자산) → Task 5 ✅
  - Goal 3 (Player Hero + 기본 탭 = 배틀 + 통계 탭 제거) → Task 6 ✅
  - Goal 4 (Brawler 상세 ClassIcon + 버피 한글 + LCP) → Task 7 ✅
  - Goal 5 (Brawler 메타 ClassIcon) → Task 7 ✅
  - Goal 6 (컨테이너 폭 1280) → Task 6, 7 ✅
  - Goal 7 (typecheck/lint/test 통과 + 콘솔 경고 0) → 각 Task 마지막 step 에서 검증.
- **Type consistency**:
  - `BrawlerRole` 도메인 enum 만 사용. `ClassKey` mock 은 Task 2 에서 제거 → 잔존 0.
  - `BrawlerAvatar` props 가 Task 3 (배틀) → Task 7 (priority 추가) 에 걸쳐 일관 (`name`, `imageUrl`, `size` 추가, `priority` 옵셔널).
  - Player 도메인 필드명 (`expLevel`, `totalPrestigeLevel`, `victories3vs3`, `victoriesSolo`) 은 `src/domain/player/types.ts` 와 일치.
  - 라벨 매핑 (`ROLE_LABEL_KO`, `RARITY_LABEL_KO`) 는 기존 파일 그대로 import.
- **Placeholder scan**: 모든 step 이 구체 코드/명령. "TBD" 없음.
