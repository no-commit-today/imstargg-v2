# Asset 이미지 매핑 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** CDN 동적 URL 을 보여주는 raw `<img>` 4 곳을 기존 `next/image` + `unoptimized` 패턴으로 통일하고 ESLint 동적 케이스 경고를 제거한다.

**Architecture:** 기존에 7 개 파일에서 정착된 `<Image src=... unoptimized width height alt />` 패턴을 그대로 따른다. fallback 분기는 변경하지 않는다. `next.config.ts` 변경 없음 (`unoptimized=true` 라 `remotePatterns` 불필요).

**Tech Stack:** Next.js 16 App Router, `next/image`, React 19 RSC/Client mix, Vitest + Testing Library, Playwright E2E.

**Spec:** `docs/superpowers/specs/2026-04-29-frontend-asset-image-mapping-design.md`

---

## File Structure

| 파일 | 책임 | 작업 |
|---|---|---|
| `imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx` | 브롤러명/이미지/희귀도/사이즈 받아 이미지 또는 fallback 그라디언트 렌더 | raw `<img>` → `next/image` |
| `imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx` | 모드 아이콘 URL 또는 fallback 모드 첫 글자 원형 렌더 | raw `<img>` → `next/image` (eslint-disable 제거) |
| `imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx` | 브롤러 buffy 이미지 그리드 | raw `<img>` → `next/image` |
| `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx` | gadget/gear/starpower/hypercharge 카드 | raw `<img>` → `next/image` |
| `imstargg-frontend/tests/components/BrawlerAvatar.test.tsx` | (신규) BrawlerAvatar 분기 렌더 단위 테스트 | 신규 |

각 파일은 작은 단위로, 한 task 당 한 파일을 변경하고 commit.

---

### Task 1: BrawlerAvatar 단위 테스트 추가

**목표:** 마이그레이션 전 회귀 가드를 먼저 작성. `imageUrl` 분기에 대한 보호망 확보.

**Files:**
- Create: `imstargg-frontend/tests/components/BrawlerAvatar.test.tsx`

- [ ] **Step 1: 테스트 작성 (실패할 것이 아님 — 현재 동작에 대한 가드)**

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";

describe("BrawlerAvatar", () => {
  it("imageUrl 이 있으면 alt 가 name 인 이미지를 렌더한다", () => {
    render(
      <BrawlerAvatar
        name="쉘리"
        imageUrl="https://cdn.imstargg.com/brawler/portrait/16000000.webp"
      />,
    );
    const img = screen.getByAltText("쉘리");
    expect(img).toBeInTheDocument();
    expect(img.tagName).toBe("IMG");
    expect(img.getAttribute("src")).toContain("16000000.webp");
  });

  it("imageUrl 이 null 이면 이미지 대신 이니셜을 렌더한다", () => {
    render(<BrawlerAvatar name="쉘리" imageUrl={null} />);
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(screen.getByText("쉘")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실행 → 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/BrawlerAvatar.test.tsx
```

Expected: 2 passing.

> 참고: `next/image` 마이그레이션 전이므로 raw `<img>` 가 렌더된다. 테스트는 `tagName === "IMG"` 만 검사하고 alt 와 src 를 확인하므로 `next/image` 마이그레이션 후에도 동일하게 통과한다.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/tests/components/BrawlerAvatar.test.tsx
git commit -m "$(cat <<'EOF'
test(frontend): BrawlerAvatar 분기 렌더 회귀 테스트 추가

imageUrl 유무에 따른 이미지 vs 이니셜 fallback 분기를 가드하기 위한 단위 테스트.
next/image 마이그레이션 전 baseline.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: BrawlerAvatar — next/image 마이그레이션

**Files:**
- Modify: `imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx:47`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx` 파일에서 다음 변경:

상단 import 추가:

```tsx
import Image from "next/image";
```

`imageUrl` 분기 안쪽:

```tsx
// before
<img
  src={imageUrl}
  alt={name}
  width={size}
  height={size}
  className="h-full w-full object-cover"
/>

// after
<Image
  src={imageUrl}
  alt={name}
  width={size}
  height={size}
  unoptimized
  className="h-full w-full object-cover"
/>
```

- [ ] **Step 2: 단위 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/components/BrawlerAvatar.test.tsx
```

Expected: 2 passing. (`next/image` 도 최종 렌더는 `<img>` 이므로 `tagName === "IMG"` 통과)

- [ ] **Step 3: typecheck/lint 확인**

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint 2>&1 | grep "BrawlerAvatar.tsx" || echo "BrawlerAvatar 경고 없음"
```

Expected: typecheck 통과, lint 에 BrawlerAvatar.tsx 의 `no-img-element` 경고가 없다.

- [ ] **Step 4: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/components/primitives/BrawlerAvatar.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BrawlerAvatar 의 raw img 를 next/image 로 마이그레이션

unoptimized 모드 + 명시적 width/height 로 CDN webp 를 그대로 서빙. 기존 7 개 파일에
정착된 패턴과 통일하고 no-img-element ESLint 경고를 제거. fallback 분기는 변경 없음.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: BattleModeIcon — next/image 마이그레이션

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx:13-19`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx` 파일에서 다음 변경:

상단 import 추가:

```tsx
import Image from "next/image";
```

`iconUrl` 분기 안쪽:

```tsx
// before
if (iconUrl) {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={iconUrl}
      alt={modeLabel(mode)}
      style={{ width: size, height: size, objectFit: "contain" }}
    />
  );
}

// after
if (iconUrl) {
  return (
    <Image
      src={iconUrl}
      alt={modeLabel(mode)}
      width={size}
      height={size}
      unoptimized
      style={{ objectFit: "contain" }}
    />
  );
}
```

`width`/`height` 가 `style` 에서 prop 으로 이동하고 `eslint-disable` 주석이 제거된다.

- [ ] **Step 2: typecheck 확인**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: lint 확인**

```bash
cd imstargg-frontend
pnpm lint 2>&1 | grep "BattleModeIcon.tsx" || echo "BattleModeIcon 경고 없음"
```

Expected: BattleModeIcon.tsx 의 `no-img-element` 경고 없음.

- [ ] **Step 4: 기존 단위 테스트 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run
```

Expected: 전체 통과 (변경 직전 baseline 과 동일 케이스 수).

- [ ] **Step 5: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleModeIcon.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BattleModeIcon 의 raw img 를 next/image 로 마이그레이션

unoptimized 모드 + 명시적 width/height 로 ESLint disable 주석 제거. fallback 회색 원
분기는 변경 없음.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: BrawlerBuffies — next/image 마이그레이션

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx:22`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx` 파일에서 다음 변경:

상단 import 추가 (`"use client"` 가 있다면 그대로 두고 import 추가):

```tsx
import Image from "next/image";
```

`b.imageUrl` 사용 라인 (line 22):

```tsx
// before
<img src={b.imageUrl} alt={label} width={20} height={20} className="h-5 w-5" />

// after
<Image src={b.imageUrl} alt={label} width={20} height={20} unoptimized className="h-5 w-5" />
```

- [ ] **Step 2: typecheck 확인**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: lint 확인**

```bash
cd imstargg-frontend
pnpm lint 2>&1 | grep "BrawlerBuffies.tsx" || echo "BrawlerBuffies 경고 없음"
```

Expected: BrawlerBuffies.tsx 의 `no-img-element` 경고 없음.

- [ ] **Step 4: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/[id]/_components/BrawlerBuffies.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BrawlerBuffies 의 raw img 를 next/image 로 마이그레이션

unoptimized 모드로 CDN buffy webp 를 그대로 서빙. 동일 패턴으로 통일.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: BrawlerItemCard — next/image 마이그레이션

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx:10`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx` 파일에서 다음 변경:

상단 import 추가 (`"use client"` 다음 줄):

```tsx
import Image from "next/image";
```

`item.imageUrl` 분기 안쪽 (line 10–16):

```tsx
// before
<img
  src={item.imageUrl}
  alt={item.name}
  width={48}
  height={48}
  className="h-full w-full object-contain"
/>

// after
<Image
  src={item.imageUrl}
  alt={item.name}
  width={48}
  height={48}
  unoptimized
  className="h-full w-full object-contain"
/>
```

- [ ] **Step 2: typecheck 확인**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: lint 확인**

```bash
cd imstargg-frontend
pnpm lint 2>&1 | grep "BrawlerItemCard.tsx" || echo "BrawlerItemCard 경고 없음"
```

Expected: BrawlerItemCard.tsx 의 `no-img-element` 경고 없음.

- [ ] **Step 4: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/brawlers/[id]/_components/BrawlerItemCard.tsx
git commit -m "$(cat <<'EOF'
refactor(frontend): BrawlerItemCard 의 raw img 를 next/image 로 마이그레이션

unoptimized 모드로 gadget/gear/starpower/hypercharge 카드 이미지를 통일된 패턴으로 마이그레이션.
fallback 이니셜 분기는 변경 없음.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: 통합 검증 (lint, typecheck, unit, prod E2E)

**목표:** 4 개 파일 마이그레이션 후 전체 정적 검사와 prod E2E 가 모두 통과하는지 확인.

- [ ] **Step 1: lint 통과 (동적 CDN 경고만 사라짐)**

```bash
cd imstargg-frontend
pnpm lint 2>&1 | tee /tmp/asset-lint.log | tail -20
grep "no-img-element" /tmp/asset-lint.log | wc -l
```

Expected: `no-img-element` 잔여 건수가 정확히 6 (정적 자산 6 곳: TopNav, ClassIcon, Trophy, ClubHeader, ClubMembers, HomeScreen). 동적 CDN 4 곳은 사라졌다.

> 만약 잔여가 6 이 아니면, 본 spec 에서 마이그레이션 누락 또는 정적 자산 카운트 변경. 위치를 점검하고 누락이라면 해당 task 로 돌아간다.

- [ ] **Step 2: typecheck 통과**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 에러 0.

- [ ] **Step 3: 단위 테스트 통과**

```bash
cd imstargg-frontend
pnpm test:run
```

Expected: 새 BrawlerAvatar 테스트 포함 전체 통과.

- [ ] **Step 4: prod E2E (statistics + player-battles) 통과**

```bash
cd imstargg-frontend
pnpm test:e2e tests/e2e/statistics.spec.ts tests/e2e/player-battles.spec.ts
```

Expected: 두 spec 모두 그린. (BrawlerBuffies, BrawlerItemCard 가 `/brawlers/[id]` 에서 노출되고 BattleModeIcon 이 `/players/[tag]` 의 배틀 탭에서 노출되어 회귀 커버.)

- [ ] **Step 5: 추가 commit 없음 — 검증만**

별도 커밋은 만들지 않는다. 검증이 모두 그린이면 task 종료.

---

## 검증 체크리스트 (요약)

- [ ] `pnpm lint` 의 `no-img-element` 경고 잔여 건수 = 6 (정적 자산)
- [ ] `pnpm typecheck` 통과
- [ ] `pnpm test:run` 통과 (BrawlerAvatar 테스트 신규 포함)
- [ ] `pnpm test:e2e tests/e2e/statistics.spec.ts tests/e2e/player-battles.spec.ts` 통과
- [ ] git log 에 6 개 commit (test 1, refactor 4, 검증은 무커밋)

## 위험과 대응

- `next/image` 가 `unoptimized` 라도 정적 분석에서 `src` 가 `null` 가능성 경고 → 항상 분기 안쪽이라 컴파일 에러 없음. 기존 7 개 파일에서 검증 완료.
- `BattleModeIcon` 의 `objectFit: contain` 이 `Image` wrapper 와 충돌 → `next/image` 는 `style` prop 을 그대로 통과. 동일 패턴 검증 완료.
- E2E 가 prod 의존이므로 일시적 실패 가능성 → 한 번 재시도. 그래도 실패하면 prod 상태 점검.

## Out-of-scope (이번 plan 제외)

- 정적 `/assets/*.png|webp` 의 next/image 마이그레이션 (TopNav 로고, ClassIcon, Trophy, ClubHeader/Members, HomeScreen)
- `iconId` / `badgeId` 신규 매핑
- `BrawlerPortrait` 와 `BrawlerAvatar` 통합
