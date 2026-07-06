# AdSense 재심사 대응 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 빈 화면 광고 게재를 코드 수준에서 차단(자동 광고→수동 유닛)하고, 홈·이벤트 페이지를 데이터 기반 콘텐츠로 보강하며, 랭킹 플레이어를 core-api 경유로 사전 갱신하는 배치를 추가한다.

**Architecture:** 프론트는 AdSense 스크립트를 루트 레이아웃에서 제거하고 `AdUnit` 수동 유닛 컴포넌트(슬롯 ID 없으면 미렌더)만 콘텐츠 화면에 배치한다. 홈은 기존 core-api(랭킹·통계·이벤트)를 RSC에서 재사용해 3개 데이터 섹션을 얹는다. core-batch는 랭킹 스냅샷에서 태그를 모아 메모리 dedupe → 24h 필터 → core-api `POST /v1/players/{tag}/renewal`을 1초 간격 순차 호출한다.

**Tech Stack:** Next.js App Router + RSC + Vitest/Testing Library/Playwright (frontend) · Spring Boot 4 + Kotlin + Spring Batch + RestClient + MockK/MockRestServiceServer (core-batch)

**Spec:** `docs/superpowers/specs/2026-07-06-adsense-compliance-design.md`

---

## 사전 조건

- 작업 디렉토리: 이 worktree (`claude/unruffled-jennings-a25cc9` 브랜치, `origin/develop` 기반). 스펙 커밋 2개가 이미 있다.
- 검증 명령어:
  - frontend (`imstargg-frontend/`): `pnpm typecheck && pnpm lint && pnpm test:run`
  - backend (`imstargg-backend/`): `./gradlew ktlintFormat :infrastructure:db-core:test :core:core-batch:test`
- 커밋은 Conventional Commits (`feat(frontend):`, `feat(core-batch):`, `test(...)` 등).

## File Structure

**Part A — 광고 체계 전환 (frontend)**

| 파일 | 작업 | 책임 |
|---|---|---|
| `imstargg-frontend/src/components/ads/slots.ts` | Create | 수동 광고 슬롯 ID 상수 (빈 문자열 = 미발급) |
| `imstargg-frontend/src/components/ads/AdUnit.tsx` | Create | 수동 광고 유닛 (슬롯 없으면 null, 스크립트 로더 내장) |
| `imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx` | Modify | 컴포넌트 삭제, 상수·`shouldLoadGoogleAdsense`만 유지 |
| `imstargg-frontend/app/layout.tsx` | Modify | 전역 AdSense 스크립트 제거 |
| `imstargg-frontend/app/players/page.tsx` 외 6곳 | Modify | 콘텐츠 하단 AdUnit 배치 |
| `imstargg-frontend/tests/unit/components/ads/AdUnit.test.tsx` | Create | AdUnit 렌더 조건 테스트 |
| `imstargg-frontend/tests/e2e/ads.spec.ts` | Create | 빈 화면 광고 부재 회귀 테스트 |

**Part B — 콘텐츠 보강 (frontend)**

| 파일 | 작업 | 책임 |
|---|---|---|
| `imstargg-frontend/app/_components/home/HomeRankingSection.tsx` | Create | 글로벌 트로피 랭킹 TOP 10 (presentational) |
| `imstargg-frontend/app/_components/home/HomeMetaSection.tsx` | Create | 브롤러 메타 TOP 10 (presentational) |
| `imstargg-frontend/app/_components/home/HomeEventsSection.tsx` | Create | 진행 중 이벤트 미리보기 (presentational) |
| `imstargg-frontend/app/page.tsx` | Modify | RSC 데이터 페칭 + 섹션 조립 + AdUnit |
| `imstargg-frontend/app/_components/HomeScreen.tsx` | Modify | 히어로가 뷰포트를 다 차지하지 않도록 축소 |
| `imstargg-frontend/src/domain/event/types.ts` | Modify | `EventStatsSummary` 타입 추가 |
| `imstargg-frontend/app/events/page.tsx` | Modify | 이벤트별 통계 요약 페칭 |
| `imstargg-frontend/app/events/_components/{EventsListPage,RotationSection,RotationCard}.tsx` | Modify | summary prop 전달·표시 |
| `imstargg-frontend/tests/components/Home*.test.tsx`, `RotationCard.test.tsx` | Create | 섹션·카드 테스트 |

**Part C — 사전 갱신 배치 (backend)**

| 파일 | 작업 | 책임 |
|---|---|---|
| `imstargg-backend/core/core-batch/.../support/coreapi/PlayerRenewalResult.kt` | Create | 갱신 요청 결과 enum |
| `imstargg-backend/core/core-batch/.../support/coreapi/CoreApiClient.kt` | Create | core-api 호출 클라이언트 (RestClient) |
| `imstargg-backend/core/core-batch/.../job/renewal/task/PlayerRankingRenewalTask.kt` | Create | 태그 수집→dedupe→필터→간격 호출 |
| `imstargg-backend/core/core-batch/.../job/renewal/PlayerRankingRenewalJobConfig.kt` | Create | `playerRankingRenewalJob` 정의 |
| `imstargg-backend/infrastructure/db-core/.../renewal/repository/RenewalJpaRepository.kt` | Modify | `findAllByTypeAndBrawlStarsTagIn` 추가 |
| `imstargg-backend/core/core-batch/src/main/resources/application.yml` | Modify | `app.core-api.url`, 간격 프로퍼티 |
| `imstargg-backend/core/core-batch/src/test/resources/application.yml` | Modify | 테스트용 프로퍼티 (간격 0) |
| 테스트: `CoreApiClientTest.kt`, `PlayerRankingRenewalTaskTest.kt` | Create | 클라이언트·태스크 테스트 |

전체 경로 접두사: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/`, 테스트는 `src/test/kotlin/com/imstargg/core/batch/`.

---

### Task 1: AdUnit 컴포넌트 + 슬롯 상수 (TDD)

**Files:**
- Create: `imstargg-frontend/src/components/ads/slots.ts`
- Create: `imstargg-frontend/src/components/ads/AdUnit.tsx`
- Test: `imstargg-frontend/tests/unit/components/ads/AdUnit.test.tsx`

- [ ] **Step 1: 슬롯 상수 파일 작성**

```ts
// imstargg-frontend/src/components/ads/slots.ts
/**
 * AdSense 수동 광고 유닛 슬롯 ID.
 *
 * 빈 문자열이면 해당 위치의 AdUnit 은 아무것도 렌더링하지 않는다 —
 * AdSense 콘솔에서 슬롯 생성 후 값을 채워 배포한다 (운영자 액션).
 */
export const AD_SLOTS = {
  home: "",
  playerRanking: "",
  clubRanking: "",
  brawlerMeta: "",
  brawlerDetail: "",
  events: "",
  eventDetail: "",
  playerDetail: "",
  clubDetail: "",
} as const;
```

- [ ] **Step 2: 실패하는 테스트 작성**

```tsx
// imstargg-frontend/tests/unit/components/ads/AdUnit.test.tsx
import { describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { AdUnit } from "@/components/ads/AdUnit";

describe("AdUnit", () => {
  it("slot 이 비어 있으면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<AdUnit slot="" enabled={true} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("enabled=false 면 slot 이 있어도 렌더링하지 않는다", () => {
    const { container } = render(<AdUnit slot="1234567890" enabled={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("enabled + slot 이면 ins.adsbygoogle 를 슬롯·클라이언트 속성과 함께 렌더링한다", () => {
    const { container } = render(<AdUnit slot="1234567890" enabled={true} />);
    const ins = container.querySelector("ins.adsbygoogle");
    expect(ins).not.toBeNull();
    expect(ins).toHaveAttribute("data-ad-client", "ca-pub-5084387143997125");
    expect(ins).toHaveAttribute("data-ad-slot", "1234567890");
    expect(ins).toHaveAttribute("data-ad-format", "auto");
    expect(ins).toHaveAttribute("data-full-width-responsive", "true");
  });
});
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/unit/components/ads/AdUnit.test.tsx`
Expected: FAIL — `Cannot find module '@/components/ads/AdUnit'`

- [ ] **Step 4: AdUnit 구현**

```tsx
// imstargg-frontend/src/components/ads/AdUnit.tsx
"use client";

import { useEffect, useRef } from "react";
import Script from "next/script";
import {
  GOOGLE_ADSENSE_CLIENT,
  GOOGLE_ADSENSE_SCRIPT_SRC,
  shouldLoadGoogleAdsense,
} from "./GoogleAdsenseScript";

declare global {
  interface Window {
    adsbygoogle?: object[];
  }
}

type Props = {
  slot: string;
  className?: string;
  /** 테스트 주입용. 기본값은 production 에서만 true. */
  enabled?: boolean;
};

/**
 * AdSense 수동 광고 유닛. 광고는 이 컴포넌트를 통해서만 게재된다 —
 * 콘텐츠가 없는 화면(미갱신 플레이어/클럽, 404, 에러, 빈 검색, 법적 페이지)에는
 * 절대 배치하지 않는다 (AdSense "게시자 콘텐츠 없는 화면" 정책).
 * slot 이 빈 문자열이면 아무것도 렌더링하지 않으므로 콘솔 슬롯 발급 전에도 배포 가능.
 */
export function AdUnit({
  slot,
  className,
  enabled = shouldLoadGoogleAdsense(process.env.NODE_ENV),
}: Props) {
  const active = enabled && slot !== "";
  const pushed = useRef(false);

  useEffect(() => {
    if (!active || pushed.current) return;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
      pushed.current = true;
    } catch {
      // 광고 스크립트 로드 실패는 광고 미표시로 끝난다 — 페이지 동작에 영향 없음
    }
  }, [active]);

  if (!active) return null;
  return (
    <div className={className}>
      <Script
        id="google-adsense"
        async
        strategy="afterInteractive"
        src={GOOGLE_ADSENSE_SCRIPT_SRC}
        crossOrigin="anonymous"
      />
      <ins
        className="adsbygoogle"
        style={{ display: "block" }}
        data-ad-client={GOOGLE_ADSENSE_CLIENT}
        data-ad-slot={slot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </div>
  );
}
```

`next/script`는 같은 `id`("google-adsense")를 dedupe 하므로 한 페이지에 AdUnit 이 여러 개여도 스크립트는 1회만 로드된다.

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/unit/components/ads/AdUnit.test.tsx`
Expected: PASS (3 tests)

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/components/ads/slots.ts imstargg-frontend/src/components/ads/AdUnit.tsx imstargg-frontend/tests/unit/components/ads/AdUnit.test.tsx
git commit -m "feat(frontend): AdSense 수동 광고 유닛 AdUnit 컴포넌트 추가"
```

---

### Task 2: 전역 AdSense 스크립트 제거

**Files:**
- Modify: `imstargg-frontend/app/layout.tsx:10,30`
- Modify: `imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx`
- Modify: `imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx`

- [ ] **Step 1: layout.tsx 에서 스크립트 제거**

`app/layout.tsx`에서 두 줄 삭제:
- L10: `import { GoogleAdsenseScript } from "@/components/ads/GoogleAdsenseScript";`
- L30: `<GoogleAdsenseScript />`

- [ ] **Step 2: GoogleAdsenseScript 컴포넌트 삭제 (상수는 유지)**

`src/components/ads/GoogleAdsenseScript.tsx`를 아래 내용으로 교체 (`Script` import 와 컴포넌트 함수 제거):

```tsx
// imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx
/**
 * AdSense 설정 상수. 광고 게재는 AdUnit(수동 유닛)을 통해서만 한다 —
 * 전역 스크립트 로드(자동 광고)는 빈 화면 광고 게재로 이어져 정책 위반이었음.
 */
export const GOOGLE_ADSENSE_CLIENT = "ca-pub-5084387143997125";
export const GOOGLE_ADSENSE_SCRIPT_SRC = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${GOOGLE_ADSENSE_CLIENT}`;

export function shouldLoadGoogleAdsense(nodeEnv: string | undefined): boolean {
  return nodeEnv === "production";
}
```

- [ ] **Step 3: 기존 테스트 정리**

`tests/unit/components/ads/GoogleAdsenseScript.test.tsx`를 열어 `GoogleAdsenseScript` 컴포넌트를 직접 렌더/호출하는 테스트 블록과 해당 import 를 삭제한다. `GOOGLE_ADSENSE_SCRIPT_SRC` 상수 검증과 `shouldLoadGoogleAdsense` 케이스 테스트는 그대로 남긴다.

- [ ] **Step 4: 전체 검증**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm test:run`
Expected: PASS — `GoogleAdsenseScript` 참조가 남아 있으면 typecheck 이 잡아준다

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/app/layout.tsx imstargg-frontend/src/components/ads/GoogleAdsenseScript.tsx imstargg-frontend/tests/unit/components/ads/GoogleAdsenseScript.test.tsx
git commit -m "feat(frontend): 전역 AdSense 자동 광고 스크립트 제거"
```

---

### Task 3: 콘텐츠 페이지에 AdUnit 배치

**Files (7곳 Modify):**
- `imstargg-frontend/app/players/page.tsx`
- `imstargg-frontend/app/clubs/page.tsx`
- `imstargg-frontend/app/brawlers/page.tsx`
- `imstargg-frontend/app/brawlers/[id]/page.tsx`
- `imstargg-frontend/app/events/[id]/page.tsx`
- `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx`
- `imstargg-frontend/app/clubs/[tag]/_components/ClubContent.tsx`

홈은 Task 6, `/events` 목록은 Task 7 에서 콘텐츠 보강과 함께 배치한다. NotFound 뷰·404·에러·검색·법적 페이지에는 **배치하지 않는다**.

- [ ] **Step 1: 각 파일에 import 추가 후 `</main>` 닫는 태그 직전에 유닛 삽입**

공통 import (각 파일 상단):

```tsx
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";
```

삽입 위치와 슬롯 (모두 최상위 `<main>` 의 마지막 자식):

| 파일 | 삽입 코드 |
|---|---|
| `app/players/page.tsx` (L55 `</section>` 다음) | `<AdUnit slot={AD_SLOTS.playerRanking} />` |
| `app/clubs/page.tsx` (L55 `</section>` 다음) | `<AdUnit slot={AD_SLOTS.clubRanking} />` |
| `app/brawlers/page.tsx` (L57 `<BrawlerStatTable ... />` 다음) | `<AdUnit slot={AD_SLOTS.brawlerMeta} className="mt-5" />` |
| `app/brawlers/[id]/page.tsx` (L79 `<BrawlerStatsSection ... />` 다음) | `<AdUnit slot={AD_SLOTS.brawlerDetail} className="mt-6" />` |
| `app/events/[id]/page.tsx` (L67 `<EventStatsSection ... />` 다음) | `<AdUnit slot={AD_SLOTS.eventDetail} className="mt-6" />` |
| `PlayerContent.tsx` (L74 `</Tabs>` 다음) | `<AdUnit slot={AD_SLOTS.playerDetail} />` |
| `ClubContent.tsx` (멤버 그리드 `</div>` 다음, `</main>` 직전) | `<AdUnit slot={AD_SLOTS.clubDetail} />` |

`PlayerContent`/`ClubContent`는 데이터가 있을 때만 렌더링되는 컴포넌트이므로(NotFound 뷰는 별도 컴포넌트) "데이터 있을 때만 광고" 조건이 구조적으로 보장된다.

- [ ] **Step 2: 검증**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint && pnpm test:run`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add -A imstargg-frontend/app
git commit -m "feat(frontend): 콘텐츠 페이지 하단에 수동 광고 유닛 배치"
```

---

### Task 4: E2E — 빈 화면 광고 부재 회귀 테스트

**Files:**
- Create: `imstargg-frontend/tests/e2e/ads.spec.ts`

- [ ] **Step 1: E2E 스펙 작성**

```ts
// imstargg-frontend/tests/e2e/ads.spec.ts
import { test, expect } from "@playwright/test";

// 유효한 태그 문자셋(0289PYLQGRJCUV)이지만 실존하지 않는 태그 →
// core-api 404 → PlayerNotFoundView (콘텐츠 없는 화면)
const UNKNOWN_TAG_ENCODED = "%23RRRRRRRRR";

test("미갱신 플레이어 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto(`/players/${UNKNOWN_TAG_ENCODED}`);
  await expect(page.getByText("플레이어를 찾을 수 없어요")).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});

test("404 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto("/this-page-does-not-exist");
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});

test("빈 검색 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto("/players/search");
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});
```

한계: E2E 는 dev 서버(NODE_ENV=development)로 돌아 광고가 어디에도 안 뜨는 환경이다. 이 테스트는 "빈 화면에 AdUnit 을 실수로 배치"하는 회귀를 잡는 문서화 테스트이고, 광고 게재 자체의 최종 확인은 배포 후 라이브 검증(하단 "배포 후 절차")에서 한다.

- [ ] **Step 2: 실행 확인**

Run: `cd imstargg-frontend && pnpm test:e2e tests/e2e/ads.spec.ts`
Expected: PASS (3 tests) — prod core-api 를 호출하므로 네트워크 필요

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/tests/e2e/ads.spec.ts
git commit -m "test(frontend): 빈 화면 광고 부재 E2E 회귀 테스트 추가"
```

---

### Task 5: 홈 섹션 컴포넌트 3개 (TDD)

**Files:**
- Create: `imstargg-frontend/app/_components/home/HomeRankingSection.tsx`
- Create: `imstargg-frontend/app/_components/home/HomeMetaSection.tsx`
- Create: `imstargg-frontend/app/_components/home/HomeEventsSection.tsx`
- Test: `imstargg-frontend/tests/components/HomeRankingSection.test.tsx`
- Test: `imstargg-frontend/tests/components/HomeMetaSection.test.tsx`
- Test: `imstargg-frontend/tests/components/HomeEventsSection.test.tsx`

- [ ] **Step 1: 실패하는 테스트 3개 작성**

```tsx
// imstargg-frontend/tests/components/HomeRankingSection.test.tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeRankingSection } from "@app/_components/home/HomeRankingSection";
import type { PlayerRanking } from "@/domain/ranking/types";

function ranking(rank: number, tag: string, name: string): PlayerRanking {
  return {
    country: "global",
    rank,
    player: { tag, name, nameColor: null, iconId: 28000000, clubName: "" },
    trophies: 100000 - rank,
    rankChange: null,
  };
}

describe("HomeRankingSection", () => {
  it("상위 10명만 렌더링하고 플레이어 페이지로 링크한다", () => {
    const rankings = Array.from({ length: 15 }, (_, i) =>
      ranking(i + 1, `#TAG${i}`, `플레이어${i}`),
    );
    render(<HomeRankingSection rankings={rankings} />);
    expect(screen.getAllByRole("listitem")).toHaveLength(10);
    expect(screen.getByRole("link", { name: /플레이어0/ })).toHaveAttribute(
      "href",
      "/players/%23TAG0",
    );
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/players",
    );
  });

  it("빈 배열이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<HomeRankingSection rankings={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
```

```tsx
// imstargg-frontend/tests/components/HomeMetaSection.test.tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeMetaSection } from "@app/_components/home/HomeMetaSection";
import type { BrawlerResultStat } from "@/domain/statistics/types";

function stat(id: number, name: string, winRate: number): BrawlerResultStat {
  return {
    brawler: {
      id,
      name,
      rarity: "STARTING_BRAWLER",
      role: "DAMAGE_DEALER",
      portraitImageUrl: null,
    },
    battleCount: 1000,
    pickRate: 0.05,
    winRate,
    starPlayerRate: 0.1,
    tierScore: winRate,
    tier: "A",
  } as BrawlerResultStat;
}

describe("HomeMetaSection", () => {
  it("승률 내림차순 상위 10개만 렌더링하고 브롤러 페이지로 링크한다", () => {
    const stats = Array.from({ length: 15 }, (_, i) =>
      stat(16000000 + i, `브롤러${i}`, 0.7 - i * 0.01),
    );
    render(<HomeMetaSection stats={stats} />);
    expect(screen.getAllByRole("listitem")).toHaveLength(10);
    expect(screen.getByRole("link", { name: /브롤러0/ })).toHaveAttribute(
      "href",
      "/brawlers/16000000",
    );
    expect(screen.getByText("70.0%")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/brawlers",
    );
  });

  it("brawler 가 null 인 항목은 제외한다", () => {
    const withNull = [{ ...stat(1, "x", 0.5), brawler: null }];
    const { container } = render(<HomeMetaSection stats={withNull} />);
    expect(container).toBeEmptyDOMElement();
  });
});
```

주의: `rarity`/`role`/`tier` 리터럴이 타입과 안 맞으면 `src/domain/brawler/types.ts`·`src/domain/statistics/tier.ts`의 실제 유니온 값으로 보정한다 (`as BrawlerResultStat` 캐스팅은 픽스처 편의용).

```tsx
// imstargg-frontend/tests/components/HomeEventsSection.test.tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HomeEventsSection } from "@app/_components/home/HomeEventsSection";
import type { RotationEvent } from "@/domain/event/types";

function rotation(id: number, mapName: string): RotationEvent {
  return {
    event: { id, mode: null, mapName },
    startTime: "2026-07-01T00:00:00Z",
    endTime: "2099-01-01T00:00:00Z",
  };
}

describe("HomeEventsSection", () => {
  it("최대 6개 이벤트 카드를 렌더링하고 이벤트 페이지로 링크한다", () => {
    const events = Array.from({ length: 8 }, (_, i) =>
      rotation(15000000 + i, `맵${i}`),
    );
    render(<HomeEventsSection rotation={events} />);
    expect(screen.getByText("맵0")).toBeInTheDocument();
    expect(screen.queryByText("맵7")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /맵0/ })).toHaveAttribute(
      "href",
      "/events/15000000",
    );
    expect(screen.getByRole("link", { name: "더 보기" })).toHaveAttribute(
      "href",
      "/events",
    );
  });

  it("빈 배열이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<HomeEventsSection rotation={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/components/HomeRankingSection.test.tsx tests/components/HomeMetaSection.test.tsx tests/components/HomeEventsSection.test.tsx`
Expected: FAIL — 모듈 없음

- [ ] **Step 3: 컴포넌트 3개 구현**

```tsx
// imstargg-frontend/app/_components/home/HomeRankingSection.tsx
import Link from "next/link";
import type { PlayerRanking } from "@/domain/ranking/types";
import { toUrlPath } from "@/lib/tag";
import { formatNumber } from "@/lib/format";

type Props = { rankings: PlayerRanking[] };

export function HomeRankingSection({ rankings }: Props) {
  const top = rankings.slice(0, 10);
  if (top.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-ranking-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-ranking-title" className="section-title">
            글로벌 트로피 랭킹
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            지금 가장 높은 트로피를 보유한 플레이어
          </p>
        </div>
        <Link href="/players" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <ol className="data-table divide-y divide-[#ECEEF3]">
        {top.map((r) => (
          <li key={r.player.tag}>
            <Link
              href={`/players/${toUrlPath(r.player.tag)}`}
              className="grid grid-cols-[44px_minmax(0,1fr)_110px] items-center gap-2 px-4 py-2.5"
            >
              <span className="font-num font-bold tabular-nums">{r.rank}</span>
              <span className="truncate font-semibold">{r.player.name}</span>
              <span className="text-right font-num font-bold tabular-nums">
                {formatNumber(r.trophies)}
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}
```

```tsx
// imstargg-frontend/app/_components/home/HomeMetaSection.tsx
import Link from "next/link";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";

type Props = { stats: BrawlerResultStat[] };

export function HomeMetaSection({ stats }: Props) {
  const top = stats
    .filter((s) => s.brawler !== null)
    .sort((a, b) => b.winRate - a.winRate)
    .slice(0, 10);
  if (top.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-meta-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-meta-title" className="section-title">
            브롤러 메타 TOP 10
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            전체 트로피 구간 승률 상위 브롤러
          </p>
        </div>
        <Link href="/brawlers" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <ol className="data-table divide-y divide-[#ECEEF3]">
        {top.map((s, i) => (
          <li key={s.brawler!.id}>
            <Link
              href={`/brawlers/${s.brawler!.id}`}
              className="grid grid-cols-[44px_minmax(0,1fr)_88px_88px] items-center gap-2 px-4 py-2.5"
            >
              <span className="font-num font-bold tabular-nums">{i + 1}</span>
              <span className="truncate font-semibold">{s.brawler!.name}</span>
              <span className="text-right">
                <PercentCell value={s.winRate} />
              </span>
              <span className="text-right text-[#5C6271]">
                <PercentCell value={s.pickRate} />
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}
```

```tsx
// imstargg-frontend/app/_components/home/HomeEventsSection.tsx
import Link from "next/link";
import type { RotationEvent } from "@/domain/event/types";
import { RotationCard } from "@app/events/_components/RotationCard";

type Props = { rotation: RotationEvent[] };

export function HomeEventsSection({ rotation }: Props) {
  const current = rotation.slice(0, 6);
  if (current.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-events-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-events-title" className="section-title">
            진행 중인 이벤트
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">현재 로테이션 맵</p>
        </div>
        <Link href="/events" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <div className="grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-3">
        {current.map((r) => (
          <RotationCard key={r.event.id} rotation={r} />
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/components/HomeRankingSection.test.tsx tests/components/HomeMetaSection.test.tsx tests/components/HomeEventsSection.test.tsx`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/app/_components/home imstargg-frontend/tests/components/Home*.test.tsx
git commit -m "feat(frontend): 홈 랭킹·메타·이벤트 미리보기 섹션 컴포넌트 추가"
```

---

### Task 6: 홈 페이지 조립 (데이터 페칭 + 히어로 축소 + AdUnit)

**Files:**
- Modify: `imstargg-frontend/app/page.tsx` (전체 교체)
- Modify: `imstargg-frontend/app/_components/HomeScreen.tsx:9-18`

- [ ] **Step 1: page.tsx 를 RSC 데이터 페칭 + 섹션 조립로 교체**

```tsx
// imstargg-frontend/app/page.tsx
import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import { env } from "@/config/env";
import { rankingApi } from "@/api/endpoints/ranking";
import { statisticsApi } from "@/api/endpoints/statistics";
import { eventApi } from "@/api/endpoints/event";
import { CACHE_SECONDS } from "@/config/cache";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import { toRotationEvent } from "@/domain/event/mappers";
import { parseDateRange, parseTierRange } from "@/domain/statistics/enums";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";
import HomeScreen from "./_components/HomeScreen";
import { HomeRankingSection } from "./_components/home/HomeRankingSection";
import { HomeMetaSection } from "./_components/home/HomeMetaSection";
import { HomeEventsSection } from "./_components/home/HomeEventsSection";

export const metadata: Metadata = buildMetadata({
  title: "ImStarGG — 브롤스타즈 전적 · 통계 · 메타 검색",
  description:
    "브롤스타즈 전적 검색, 브롤러 메타 티어, 글로벌·국가별 트로피 랭킹, 이벤트 로테이션 통계까지. 한국어 브롤스타즈 통계 사이트.",
  path: "/",
});

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  potentialAction: {
    "@type": "SearchAction",
    target: {
      "@type": "EntryPoint",
      // routes.player encodes "#" → "%23"; SearchAction expects user to type the
      // tag body without "#", so we prepend the encoded "#" in the template.
      urlTemplate: `${env.NEXT_PUBLIC_SITE_URL}/players/%23{search_term_string}`,
    },
    "query-input": "required name=search_term_string",
  },
};

// 홈은 섹션 하나가 실패해도 나머지로 렌더링한다 — 검색(핵심 기능)이 죽으면 안 된다.
export default async function Page() {
  const [rankingDto, statsDto, rotationDto] = await Promise.all([
    rankingApi
      .players("global", { next: { revalidate: CACHE_SECONDS.SHORT } })
      .catch(() => null),
    statisticsApi
      .brawlerResult(
        { tierRange: parseTierRange(undefined), dateRange: parseDateRange(undefined) },
        { next: { revalidate: CACHE_SECONDS.SHORT } },
      )
      .catch(() => null),
    eventApi
      .rotation({ next: { revalidate: CACHE_SECONDS.SHORT } })
      .catch(() => null),
  ]);

  const rankings = rankingDto?.content.map(toPlayerRanking) ?? [];
  const stats = statsDto ? toBrawlerResultStatistics(statsDto).items : [];
  const rotation = rotationDto?.content.map(toRotationEvent) ?? [];

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <HomeScreen />
      <div className="mx-auto w-full max-w-7xl space-y-8 px-4 pb-10">
        <HomeEventsSection rotation={rotation} />
        <div className="grid gap-8 lg:grid-cols-2">
          <HomeRankingSection rankings={rankings} />
          <HomeMetaSection stats={stats} />
        </div>
        <section className="space-y-2">
          <h2 className="section-title">ImStarGG 소개</h2>
          <p className="ml-[14px] max-w-3xl text-sm leading-6 text-[#5C6271]">
            ImStarGG는 브롤스타즈 전투 데이터를 수집·집계해 플레이어 전적, 브롤러
            메타 티어, 맵·모드별 통계와 글로벌/국가별 랭킹을 제공하는 한국어 통계
            서비스입니다. 플레이어 태그로 전적을 검색하고, 갱신 버튼으로 최신 전투
            기록을 불러올 수 있어요.
          </p>
        </section>
        <AdUnit slot={AD_SLOTS.home} />
      </div>
    </>
  );
}
```

- [ ] **Step 2: HomeScreen 히어로 축소**

`app/_components/HomeScreen.tsx` 최상위 div 의 style 에서 `minHeight` 줄을 삭제하고 `paddingTop` 을 줄인다:

```tsx
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "flex-start",
        paddingTop: 48,
        paddingBottom: 24,
        position: "relative",
        overflow: "hidden",
      }}
```

- [ ] **Step 3: 검증 (기존 홈 E2E 포함)**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint && pnpm test:run`
Expected: PASS
Run: `pnpm test:e2e tests/e2e/home.spec.ts`
Expected: PASS — 검색 플로우 회귀 없음

- [ ] **Step 4: 커밋**

```bash
git add imstargg-frontend/app/page.tsx imstargg-frontend/app/_components/HomeScreen.tsx
git commit -m "feat(frontend): 홈에 랭킹·메타·이벤트 데이터 섹션과 소개 추가"
```

---

### Task 7: 이벤트 목록 통계 요약

**Files:**
- Modify: `imstargg-frontend/src/domain/event/types.ts` (타입 추가)
- Modify: `imstargg-frontend/app/events/_components/RotationCard.tsx`
- Modify: `imstargg-frontend/app/events/_components/RotationSection.tsx`
- Modify: `imstargg-frontend/app/events/_components/EventsListPage.tsx`
- Modify: `imstargg-frontend/app/events/page.tsx`
- Test: `imstargg-frontend/tests/components/RotationCard.test.tsx`

- [ ] **Step 1: 도메인 타입 추가** (`src/domain/event/types.ts` 끝에)

```ts
export interface EventBrawlerSummary {
  brawlerId: number;
  name: string;
  winRate: number;
}

export interface EventStatsSummary {
  totalBattleCount: number;
  topBrawlers: EventBrawlerSummary[];
}
```

- [ ] **Step 2: 실패하는 RotationCard 테스트 작성**

```tsx
// imstargg-frontend/tests/components/RotationCard.test.tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { RotationCard } from "@app/events/_components/RotationCard";
import type { EventStatsSummary, RotationEvent } from "@/domain/event/types";

const rotation: RotationEvent = {
  event: { id: 15000026, mode: null, mapName: "골드암 협곡" },
  startTime: "2026-07-01T00:00:00Z",
  endTime: "2099-01-01T00:00:00Z",
};

describe("RotationCard", () => {
  it("summary 가 있으면 판수와 승률 1위 브롤러를 노출한다", () => {
    const summary: EventStatsSummary = {
      totalBattleCount: 12345,
      topBrawlers: [{ brawlerId: 16000000, name: "쉘리", winRate: 0.61 }],
    };
    render(<RotationCard rotation={rotation} summary={summary} />);
    expect(screen.getByText("12,345")).toBeInTheDocument();
    expect(screen.getByText(/쉘리/)).toBeInTheDocument();
    expect(screen.getByText("61.0%")).toBeInTheDocument();
  });

  it("summary 가 없으면 통계 줄을 렌더링하지 않는다", () => {
    render(<RotationCard rotation={rotation} />);
    expect(screen.queryByText(/판 집계/)).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 3: 실패 확인**

Run: `cd imstargg-frontend && pnpm test:run tests/components/RotationCard.test.tsx`
Expected: FAIL — `summary` prop 없음 (TS 에러 또는 통계 줄 미노출)

- [ ] **Step 4: RotationCard 에 summary 표시 추가**

`RotationCard.tsx` 수정 — 시그니처와 footer 아래 블록:

```tsx
import { formatNumber } from "@/lib/format";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";
import type { EventStatsSummary, RotationEvent } from "@/domain/event/types";

export function RotationCard({
  rotation,
  summary,
}: {
  rotation: RotationEvent;
  summary?: EventStatsSummary;
}) {
```

기존 `event-card-footer` div 바로 다음(닫는 `</Link>` 직전)에 추가:

```tsx
      {summary && (
        <div className="px-3 pb-3 text-xs text-[#5C6271]">
          <span className="font-num font-bold tabular-nums">
            {formatNumber(summary.totalBattleCount)}
          </span>
          판 집계
          {summary.topBrawlers[0] && (
            <>
              {" · 승률 1위 "}
              {summary.topBrawlers[0].name}{" "}
              <PercentCell value={summary.topBrawlers[0].winRate} />
            </>
          )}
        </div>
      )}
```

- [ ] **Step 5: prop 전달 경로 연결**

`RotationSection.tsx`:

```tsx
import type { EventStatsSummary, RotationEvent } from "@/domain/event/types";

export function RotationSection({
  rotation,
  summaries,
}: {
  rotation: RotationEvent[];
  summaries?: Record<number, EventStatsSummary>;
}) {
```

카드 렌더링을 `<RotationCard key={r.event.id} rotation={r} summary={summaries?.[r.event.id]} />` 로 변경.

`EventsListPage.tsx`: `Props` 에 `summaries?: Record<number, EventStatsSummary>;` 추가, `<RotationSection rotation={rotation} summaries={summaries} />` 로 전달 (import 에 `EventStatsSummary` 추가).

- [ ] **Step 6: events/page.tsx 에서 요약 페칭**

```tsx
// imstargg-frontend/app/events/page.tsx
import type { Metadata } from "next";
import { eventApi } from "@/api/endpoints/event";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import { toBattleEvent, toRotationEvent } from "@/domain/event/mappers";
import type { EventStatsSummary } from "@/domain/event/types";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import { parseDateRange, parseTierRange } from "@/domain/statistics/enums";
import { buildMetadata } from "@/lib/seo";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";
import EventsListPage from "./_components/EventsListPage";

export const metadata: Metadata = buildMetadata({
  title: "이벤트 로테이션 | ImStarGG",
  description:
    "현재 진행 중인 이벤트 로테이션과 맵별 전투 통계, 경쟁전 맵 정보를 확인하세요.",
  path: "/events",
});

export default async function Page() {
  const [rotationDto, soloRankDto] = await Promise.all([
    eventApi.rotation({ cache: "no-store" }),
    eventApi.soloRank({ next: { revalidate: CACHE_SECONDS.SHORT } }),
  ]);
  const initialRotation = rotationDto.content.map(toRotationEvent);
  const initialSoloRank = soloRankDto.content.map(toBattleEvent);

  const summaries: Record<number, EventStatsSummary> = {};
  await Promise.all(
    initialRotation.map(async (r) => {
      const dto = await statisticsApi
        .brawlerResult(
          {
            tierRange: parseTierRange(undefined),
            dateRange: parseDateRange(undefined),
            eventId: r.event.id,
          },
          { next: { revalidate: CACHE_SECONDS.SHORT } },
        )
        .catch(() => null);
      if (!dto || dto.totalBattleCount === 0) return;
      const stats = toBrawlerResultStatistics(dto);
      summaries[r.event.id] = {
        totalBattleCount: stats.totalBattleCount,
        topBrawlers: stats.items
          .filter((s) => s.brawler !== null)
          .sort((a, b) => b.winRate - a.winRate)
          .slice(0, 3)
          .map((s) => ({
            brawlerId: s.brawler!.id,
            name: s.brawler!.name,
            winRate: s.winRate,
          })),
      };
    }),
  );

  return (
    <>
      <EventsListPage
        initialRotation={initialRotation}
        initialSoloRank={initialSoloRank}
        summaries={summaries}
      />
      <div className="mx-auto w-full max-w-7xl px-4 pb-10">
        <AdUnit slot={AD_SLOTS.events} />
      </div>
    </>
  );
}
```

- [ ] **Step 7: 테스트·검증**

Run: `cd imstargg-frontend && pnpm test:run tests/components/RotationCard.test.tsx`
Expected: PASS
Run: `pnpm typecheck && pnpm lint && pnpm test:run && pnpm test:e2e tests/e2e/event.spec.ts`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add -A imstargg-frontend/app/events imstargg-frontend/src/domain/event/types.ts imstargg-frontend/tests/components/RotationCard.test.tsx
git commit -m "feat(frontend): 이벤트 목록에 맵별 전투 통계 요약 표시"
```

---

### Task 8: CoreApiClient — core-api 갱신 호출 클라이언트 (TDD)

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/coreapi/PlayerRenewalResult.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/coreapi/CoreApiClient.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/resources/application.yml`
- Modify: `imstargg-backend/core/core-batch/src/test/resources/application.yml`
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/coreapi/CoreApiClientTest.kt`

- [ ] **Step 1: 결과 enum 작성**

```kotlin
// PlayerRenewalResult.kt
package com.imstargg.core.batch.support.coreapi

enum class PlayerRenewalResult {
    /** 갱신 요청 접수됨 (2xx) */
    ACCEPTED,

    /** 이미 갱신 진행 중 — 스킵 (409) */
    ALREADY_IN_PROGRESS,

    /** 삭제된 플레이어 — 스킵 (410) */
    PLAYER_DELETED,

    /** 브롤스타즈 API 점검 중 — 남은 요청 중단 (503) */
    API_UNDER_MAINTENANCE,

    /** 그 외 오류 — 로그 후 다음 태그 진행 */
    FAILED,
}
```

- [ ] **Step 2: 실패하는 테스트 작성** (스프링 컨텍스트 불필요 — 단위 테스트, 태그 없음)

```kotlin
// CoreApiClientTest.kt
package com.imstargg.core.batch.support.coreapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class CoreApiClientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val sut = CoreApiClient(restClientBuilder = builder, coreApiUrl = "")

    @Test
    fun `2xx 응답이면 ACCEPTED 를 반환하고 태그를 URL 인코딩해 호출한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.ACCEPTED)
        server.verify()
    }

    @Test
    fun `409 응답이면 ALREADY_IN_PROGRESS 를 반환한다`() {
        // given
        server.expect(requestTo("/v1/players/%23AAA/renewal")).andRespond(withStatus(HttpStatus.CONFLICT))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.ALREADY_IN_PROGRESS)
    }

    @Test
    fun `410 응답이면 PLAYER_DELETED 를 반환한다`() {
        // given
        server.expect(requestTo("/v1/players/%23AAA/renewal")).andRespond(withStatus(HttpStatus.GONE))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.PLAYER_DELETED)
    }

    @Test
    fun `503 응답이면 API_UNDER_MAINTENANCE 를 반환한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.API_UNDER_MAINTENANCE)
    }

    @Test
    fun `그 외 오류 응답이면 FAILED 를 반환한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.FAILED)
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-batch:compileTestKotlin`
Expected: FAIL — `CoreApiClient` 미존재

- [ ] **Step 4: 클라이언트 구현**

```kotlin
// CoreApiClient.kt
package com.imstargg.core.batch.support.coreapi

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * core-batch → core-api 내부 호출 클라이언트.
 *
 * 갱신 상태 머신(쿨다운·삭제·점검 검사)과 SQS 발행은 core-api 가 단일 진입점으로
 * 수행한다 — batch 가 renewal 파이프라인(renewal row·SQS)을 직접 조작하지 않기 위한 경계.
 */
@Component
class CoreApiClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${app.core-api.url}") coreApiUrl: String,
) {
    private val restClient = restClientBuilder.baseUrl(coreApiUrl).build()

    fun renewPlayer(tag: String): PlayerRenewalResult =
        try {
            restClient
                .post()
                .uri("/v1/players/{tag}/renewal", tag)
                .retrieve()
                .toBodilessEntity()
            PlayerRenewalResult.ACCEPTED
        } catch (ex: HttpStatusCodeException) {
            when (ex.statusCode.value()) {
                409 -> PlayerRenewalResult.ALREADY_IN_PROGRESS
                410 -> PlayerRenewalResult.PLAYER_DELETED
                503 -> PlayerRenewalResult.API_UNDER_MAINTENANCE
                else -> {
                    log.warn("플레이어 갱신 요청 실패 - tag: {}, status: {}", tag, ex.statusCode)
                    PlayerRenewalResult.FAILED
                }
            }
        } catch (ex: RestClientException) {
            log.warn("플레이어 갱신 요청 실패 - tag: {}", tag, ex)
            PlayerRenewalResult.FAILED
        }

    companion object {
        private val log = LoggerFactory.getLogger(CoreApiClient::class.java)
    }
}
```

`uri("/v1/players/{tag}/renewal", tag)` 의 path variable 은 자동으로 인코딩되어 `#AAA` → `%23AAA` 가 된다 (테스트가 검증).

- [ ] **Step 5: 프로퍼티 추가**

`core/core-batch/src/main/resources/application.yml` 의 기존 `app:` 블록(`alert:` 이 있는 곳)에 추가:

```yaml
app:
  alert:
    type: NO_OPS
  core-api:
    url: ${CORE_API_URL:http://localhost:8080}
```

`core/core-batch/src/test/resources/application.yml` 끝에 추가:

```yaml
app:
  core-api:
    url: http://localhost:0
  player-ranking-renewal:
    request-interval-millis: 0
```

(`player-ranking-renewal` 값은 Task 9 의 컨텍스트 테스트가 사용)

- [ ] **Step 6: 테스트 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.support.coreapi.CoreApiClientTest"`
Expected: PASS (5 tests)
참고: `MockRestServiceServer`(spring-test)가 클래스패스에 없다는 컴파일 오류가 나면 `core/core-batch/build.gradle.kts` 의 dependencies 에 `testImplementation("org.springframework.boot:spring-boot-starter-test")` 를 추가한다 (루트 컨벤션 플러그인이 이미 제공하면 불필요).

- [ ] **Step 7: 커밋**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/coreapi imstargg-backend/core/core-batch/src/main/resources/application.yml imstargg-backend/core/core-batch/src/test/resources/application.yml imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/coreapi
git commit -m "feat(core-batch): core-api 플레이어 갱신 호출 클라이언트 추가"
```

---

### Task 9: PlayerRankingRenewalTask (TDD, 컨텍스트 테스트)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/renewal/repository/RenewalJpaRepository.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/renewal/task/PlayerRankingRenewalTask.kt`
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/renewal/task/PlayerRankingRenewalTaskTest.kt`

- [ ] **Step 1: RenewalJpaRepository 에 벌크 조회 메서드 추가**

```kotlin
interface RenewalJpaRepository : JpaRepository<RenewalEntity, Long> {
    fun findByBrawlStarsTagAndType(
        brawlStarsTag: String,
        type: RenewalType,
    ): RenewalEntity?

    fun findAllByTypeAndBrawlStarsTagIn(
        type: RenewalType,
        brawlStarsTags: Collection<String>,
    ): List<RenewalEntity>
}
```

- [ ] **Step 2: 실패하는 컨텍스트 테스트 작성**

테스트 베이스는 `CoreBatchContextTest` (이미 `@Tag("context")` 포함 — 서브클래스에서 중복 선언 금지). 외부 시스템(CoreApiClient)은 MockK, 내부 데이터 계층은 실 DB.

```kotlin
// PlayerRankingRenewalTaskTest.kt
package com.imstargg.core.batch.job.renewal.task

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.support.coreapi.CoreApiClient
import com.imstargg.core.batch.support.coreapi.PlayerRenewalResult
import com.imstargg.core.enums.Country
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset

class PlayerRankingRenewalTaskTest
    @Autowired
    constructor(
        private val sut: PlayerRankingRenewalTask,
        private val playerRankingJpaRepository: PlayerRankingJpaRepository,
        private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
        private val renewalJpaRepository: RenewalJpaRepository,
        private val clock: Clock,
        private val cleanUp: CleanUp,
    ) : CoreBatchContextTest() {
        @MockkBean
        private lateinit var coreApiClient: CoreApiClient

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        private fun saveRanking(
            country: Country,
            vararg tags: String,
            collectedAtOffset: Duration = Duration.ZERO,
        ): PlayerRankingEntity {
            val ranking =
                playerRankingJpaRepository.save(
                    PlayerRankingEntity(country = country, collectedAt = clock.instant().plus(collectedAtOffset)),
                )
            playerRankingItemJpaRepository.saveAll(
                tags.mapIndexed { index, tag ->
                    PlayerRankingItemEntity(
                        playerRanking = ranking,
                        rank = index + 1,
                        player =
                            RankingEntityPlayer(
                                brawlStarsTag = tag,
                                name = "name$index",
                                nameColor = null,
                                iconBrawlStarsId = 28000000,
                                clubName = "",
                            ),
                        trophies = 10000,
                    )
                },
            )
            return ranking
        }

        private fun saveCompletedRenewal(
            tag: String,
            completedAgo: Duration,
        ) {
            val entity = RenewalEntity(brawlStarsTag = tag, type = RenewalType.PLAYER, requestedAt = clock.instant())
            entity.toPending()
            entity.toExecuting(clock)
            entity.toCompleted(Clock.fixed(clock.instant().minus(completedAgo), ZoneOffset.UTC))
            renewalJpaRepository.save(entity)
        }

        @Test
        fun `여러 국가 랭킹의 태그를 중복 제거해 한 번씩만 갱신 요청한다`() {
            // given
            saveRanking(Country.GLOBAL, "#AAA", "#BBB")
            saveRanking(Country.KOREA, "#BBB", "#CCC")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 1) { coreApiClient.renewPlayer("#AAA") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#BBB") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#CCC") }
        }

        @Test
        fun `24시간 이내 갱신 완료된 태그는 요청하지 않는다`() {
            // given
            saveRanking(Country.GLOBAL, "#FRESH", "#STALE", "#NEVER")
            saveCompletedRenewal("#FRESH", completedAgo = Duration.ofHours(1))
            saveCompletedRenewal("#STALE", completedAgo = Duration.ofHours(25))
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 0) { coreApiClient.renewPlayer("#FRESH") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#STALE") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#NEVER") }
        }

        @Test
        fun `점검 응답을 받으면 남은 요청을 중단한다`() {
            // given
            saveRanking(Country.GLOBAL, "#AAA", "#BBB", "#CCC")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.API_UNDER_MAINTENANCE

            // when
            sut.renewAll()

            // then
            verify(exactly = 1) { coreApiClient.renewPlayer(any()) }
        }

        @Test
        fun `국가별 최신 스냅샷만 사용한다`() {
            // given
            saveRanking(Country.GLOBAL, "#OLD", collectedAtOffset = Duration.ofHours(-1))
            saveRanking(Country.GLOBAL, "#NEW")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 0) { coreApiClient.renewPlayer("#OLD") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#NEW") }
        }
    }
```

참고: `Country` enum 은 6개국(GLOBAL, KOREA, CHINA, JAPAN, GERMANY, UNITED_STATES)이다 — 랭킹 태그 규모는 최대 6 × 200 = ~1,200건(중복 제거 전)으로, 1초 간격 최악 ~20분이면 완료된다. `RankingEntityPlayer` 생성자 시그니처가 다르면 `PlayerRankingCollectTask.kt:36-42` 사용례를 그대로 따른다.

필터 구현 노트: 스펙의 "players 레코드가 없거나 renewal 완료가 24h보다 오래된 태그" 조건은 "24h 이내 COMPLETED renewal 이 있는 태그만 제외"로 구현한다 — COMPLETED renewal 존재 ⇔ 플레이어 데이터 존재이므로 동치이고, players 테이블 벌크 조회 없이 renewal 테이블 하나로 판정된다. 경계 케이스(renewal 없이 players 만 존재)는 갱신 대상에 포함되며 무해한 새로고침이다.

- [ ] **Step 3: 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-batch:compileTestKotlin`
Expected: FAIL — `PlayerRankingRenewalTask` 미존재

- [ ] **Step 4: 태스크 구현**

```kotlin
// PlayerRankingRenewalTask.kt
package com.imstargg.core.batch.job.renewal.task

import com.imstargg.core.batch.support.coreapi.CoreApiClient
import com.imstargg.core.batch.support.coreapi.PlayerRenewalResult
import com.imstargg.core.enums.Country
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * 랭킹에 노출되는 플레이어를 사전 갱신한다 — 랭킹 페이지에서 링크를 타고 들어간
 * 플레이어 페이지가 빈 화면("갱신 필요")이 되지 않도록 (AdSense 저가치 콘텐츠 대응).
 *
 * batch 는 renewal 파이프라인을 직접 조작하지 않고 core-api 를 호출한다.
 * 24시간 이내 COMPLETED 인 태그만 배치에서 제외하고, 나머지 세부 판정
 * (쿨다운·삭제·중복)은 core-api 상태 머신에 위임한다.
 */
@Component
class PlayerRankingRenewalTask(
    private val clock: Clock,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val renewalJpaRepository: RenewalJpaRepository,
    private val coreApiClient: CoreApiClient,
    @Value("\${app.player-ranking-renewal.request-interval-millis:1000}")
    private val requestIntervalMillis: Long,
) {
    fun renewAll() {
        val tags = collectRankedTags()
        val targets = filterFreshlyRenewed(tags)
        log.info("[{}] 랭킹 태그 {}건 중 갱신 대상 {}건", STEP_NAME, tags.size, targets.size)

        var accepted = 0
        var skipped = 0
        var failed = 0
        for ((index, tag) in targets.withIndex()) {
            when (coreApiClient.renewPlayer(tag)) {
                PlayerRenewalResult.ACCEPTED -> accepted++
                PlayerRenewalResult.ALREADY_IN_PROGRESS, PlayerRenewalResult.PLAYER_DELETED -> skipped++
                PlayerRenewalResult.API_UNDER_MAINTENANCE -> {
                    log.warn("[{}] 브롤스타즈 API 점검 중 - 남은 {}건 중단", STEP_NAME, targets.size - index)
                    break
                }
                PlayerRenewalResult.FAILED -> failed++
            }
            if (index < targets.lastIndex && requestIntervalMillis > 0) {
                Thread.sleep(requestIntervalMillis)
            }
        }
        log.info("[{}] 완료 - 접수: {}건, 스킵: {}건, 실패: {}건", STEP_NAME, accepted, skipped, failed)
    }

    private fun collectRankedTags(): Set<String> =
        Country.entries
            .mapNotNull { playerRankingJpaRepository.findTopByCountryOrderByCollectedAtDesc(it) }
            .flatMap { playerRankingItemJpaRepository.findAllByPlayerRanking(it) }
            .mapTo(LinkedHashSet()) { it.player.brawlStarsTag }

    private fun filterFreshlyRenewed(tags: Set<String>): List<String> {
        val threshold = clock.instant().minus(FRESH_DURATION)
        val fresh =
            tags
                .chunked(CHUNK_SIZE)
                .flatMap { chunk ->
                    renewalJpaRepository.findAllByTypeAndBrawlStarsTagIn(
                        type = RenewalType.PLAYER,
                        brawlStarsTags = chunk,
                    )
                }.filter { it.status == RenewalStatus.COMPLETED && it.completedAt?.isAfter(threshold) == true }
                .mapTo(HashSet()) { it.brawlStarsTag }
        return tags.filterNot { it in fresh }
    }

    companion object {
        private const val STEP_NAME = "playerRankingRenewalStep"
        private const val CHUNK_SIZE = 1000
        private val FRESH_DURATION: Duration = Duration.ofHours(24)
        private val log = LoggerFactory.getLogger(PlayerRankingRenewalTask::class.java)
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.renewal.task.PlayerRankingRenewalTaskTest"`
Expected: PASS (4 tests) — 테스트 간격은 test yml 의 `request-interval-millis: 0` 적용

- [ ] **Step 6: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/renewal/repository/RenewalJpaRepository.kt imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/renewal imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/renewal
git commit -m "feat(core-batch): 랭킹 플레이어 사전 갱신 태스크 추가"
```

---

### Task 10: PlayerRankingRenewalJobConfig

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/renewal/PlayerRankingRenewalJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/resources/application.yml`

- [ ] **Step 1: Job 설정 작성**

```kotlin
// PlayerRankingRenewalJobConfig.kt
package com.imstargg.core.batch.job.renewal

import com.imstargg.core.batch.job.renewal.task.PlayerRankingRenewalTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 랭킹 플레이어 사전 갱신 잡. 일 1회, rankingCollectJob 이후 실행하도록 스케줄한다
 * (예: --job.name=playerRankingRenewalJob --date=2026-07-06).
 *
 * `date` 파라미터가 JobInstance 정체성을 부여한다 — 같은 날 재시도는 같은 인스턴스
 * 재시작으로 인식된다. 스텝에 트랜잭션 매니저를 넘기지 않는 이유는 RankingCollectJobConfig 와
 * 동일: 태스크가 태그당 1초 간격의 느린 HTTP 호출을 수천 번 수행하므로 스텝 전체를
 * 한 트랜잭션으로 묶으면 커넥션이 장시간 점유된다.
 */
@Configuration
class PlayerRankingRenewalJobConfig(
    private val jobRepository: JobRepository,
    private val playerRankingRenewalTask: PlayerRankingRenewalTask,
) {
    companion object {
        const val JOB_NAME = "playerRankingRenewalJob"
        const val DATE_PARAMETER_NAME = "date"
        private const val STEP_NAME = "playerRankingRenewalStep"
    }

    @Bean(JOB_NAME)
    fun playerRankingRenewalJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DATE_PARAMETER_NAME),
                    arrayOf(),
                ),
            ).start(playerRankingRenewalStep())
            .build()

    @Bean(STEP_NAME)
    fun playerRankingRenewalStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet { _, _ ->
                playerRankingRenewalTask.renewAll()
                RepeatStatus.FINISHED
            }.build()
}
```

import 경로(`org.springframework.batch.core.job.Job` 등)는 `RankingCollectJobConfig.kt` 와 동일한 Spring Boot 4 배치 패키지를 따른다. `.tasklet { }` 한 개 인자 형태가 컴파일되지 않으면 `RankingCollectJobConfig` 의 `.tasklet(...)` 사용례와 동일하게 맞춘다.

- [ ] **Step 2: 간격 프로퍼티 기본값을 main yml 에 명시**

`core/core-batch/src/main/resources/application.yml` 의 `app:` 블록에 추가 (Task 8 에서 만든 `core-api:` 아래):

```yaml
  player-ranking-renewal:
    request-interval-millis: ${PLAYER_RANKING_RENEWAL_REQUEST_INTERVAL_MILLIS:1000}
```

- [ ] **Step 3: 컨텍스트 로드 검증**

Run: `cd imstargg-backend && ./gradlew :core:core-batch:test --tests "com.imstargg.CoreBatchApplicationTest"`
Expected: PASS — 새 Job 빈 포함 컨텍스트 정상 로드

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/renewal/PlayerRankingRenewalJobConfig.kt imstargg-backend/core/core-batch/src/main/resources/application.yml
git commit -m "feat(core-batch): playerRankingRenewalJob 배치 잡 추가"
```

---

### Task 11: 백엔드 전체 검증

- [ ] **Step 1: 린트 + 전체 테스트**

Run: `cd imstargg-backend && ./gradlew ktlintFormat && ./gradlew ktlintCheck :infrastructure:db-core:test :core:core-batch:test`
Expected: PASS. ktlintFormat 이 파일을 고쳤으면 diff 확인 후 이전 커밋에 맞는 스타일 수정 커밋 추가:

```bash
git add -A imstargg-backend && git commit -m "chore(core-batch): ktlint 포맷 적용"
```

(변경 없으면 커밋 생략)

---

### Task 12: 프론트엔드 전체 검증

- [ ] **Step 1: 타입·린트·단위 테스트**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint && pnpm test:run`
Expected: PASS

- [ ] **Step 2: E2E 전체**

Run: `cd imstargg-frontend && pnpm test:e2e`
Expected: PASS (prod core-api 네트워크 필요. 실패 시 실패 스펙이 이번 변경과 무관한지 확인하고 결과를 기록)

---

### Task 13: 버전 bump + PR 준비

- [ ] **Step 1: release-management 스킬로 bump**

`/release bump` 스킬을 호출한다 — frontend(feat: minor)와 core-batch(feat: minor)가 자동 감지·SNAPSHOT bump 된다. core-api 는 코드 변경이 없으므로 대상에서 제외됨을 확인한다 (db-core 변경은 core-batch 에 포함되는 인프라 모듈 — 스킬 판정을 따른다).

- [ ] **Step 2: PR 생성 (develop 대상)**

```bash
git push origin HEAD:feature/adsense-compliance
gh pr create --base develop --head feature/adsense-compliance \
  --title "feat: AdSense 재심사 대응 — 수동 광고 전환·홈 콘텐츠 보강·랭킹 사전 갱신" \
  --body "docs/superpowers/specs/2026-07-06-adsense-compliance-design.md 구현.

- 자동 광고 → AdUnit 수동 유닛 (빈 화면 광고 제거)
- 홈 랭킹·메타·이벤트 데이터 섹션 + 이벤트 목록 통계 요약
- core-batch playerRankingRenewalJob: 랭킹 태그 dedupe → 24h 필터 → core-api 1초 간격 순차 갱신 요청

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

---

## 배포 후 절차 (운영자 액션 — 코드 밖)

PR 병합 → main 배포 후:

1. **배치 스케줄 등록**: 운영 스케줄러(K8s CronJob)에 `--job.name=playerRankingRenewalJob --date=<당일>` 일 1회, `rankingCollectJob` 이후 시간대로 등록. 배포 환경변수 `CORE_API_URL`(core-api 내부 주소) 설정 필수.
2. **AdSense 콘솔**: 자동 광고 **OFF** 확인 → 광고 유닛(디스플레이) 9개 생성 → 슬롯 ID 를 `src/components/ads/slots.ts` 의 `AD_SLOTS` 에 채워 재배포.
3. **라이브 검증** (browse/브라우저):
   - `https://imstargg.com/players/%23RRRRRRRRR` (미갱신): `ins.adsbygoogle` 0개, adsbygoogle 스크립트 미로드
   - 404·`/players/search`·`/terms`: 동일하게 광고 요소 0개
   - 홈: 랭킹·메타·이벤트 섹션 + 소개 렌더링 확인
   - `/players` 랭킹 상위 10명 링크 클릭 → 전원 데이터 표시 (배치 1회 실행 후)
   - 콘텐츠 페이지: `ins` 요소에 슬롯 ID 반영 (승인 전 광고 미표시는 정상)
4. **AdSense 재심사 요청**.
