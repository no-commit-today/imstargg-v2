# 이슈 #49 프론트엔드 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 이슈 #49의 UI 개선(항목 1~8)과 갱신 hook 추상화(항목 11)를 구현해 모바일 가독성·일관성을 높인다.

**Architecture:** 기존 레이어(app/ → hooks/ → api/, domain 매퍼)와 디자인 시스템(DESIGN.md)을 그대로 따른다. 로직 변경(갱신 hook 통합, 절대시간 포맷)은 순수 함수/훅으로, 시각 변경(CSS·JSX 레이아웃)은 실행 중인 앱을 playwright로 데스크탑(1280)·모바일(390) 확인해 검증한다.

**Tech Stack:** Next.js App Router, React 19, TanStack Query v5, Radix UI(shadcn), Tailwind v4, lucide-react, Vitest.

---

## 사전 조건 (실행 환경)

- core-api 가 `http://localhost:8080` 에서 실행 중이어야 한다(검증 시 실제 데이터 필요).
- dev 서버는 다음으로 띄운다(이미 떠 있으면 재사용):
  ```bash
  NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 NEXT_PUBLIC_SITE_URL=http://localhost:3000 NEXT_PUBLIC_APP_VERSION=dev \
    pnpm --dir imstargg-frontend dev
  ```
- 모든 명령의 기준 디렉토리: 저장소 루트. 프론트 명령은 `pnpm --dir imstargg-frontend <script>` 형태로 실행한다.
- 시각 검증 공통 절차(아래 task들에서 "시각 검증"이라 하면 이 절차):
  1. playwright로 해당 URL 접속
  2. `browser_resize` 1280×800(데스크탑) / 390×844(모바일)
  3. `browser_take_screenshot` 후 이미지 확인 — 잘림/2줄깨짐/레이아웃 깨짐 없는지 육안 확인
- 테스트 원칙(프론트 AGENTS.md): 단위 테스트는 **순수 함수만**(`lib/`, `domain/*/mappers`). 컴포넌트/CSS/레이아웃은 단위 테스트하지 않고 **시각 검증**으로 대체한다.

## File Structure

**생성**
- `src/hooks/useRenewal.ts` — `useRenewal(kind, tag)` 제네릭 갱신 훅 (player/club 공통)
- `app/players/[tag]/_components/battle/BattleTime.tsx` — 상대시간 + 실제시간 툴팁(PC hover / 모바일 탭)
- `src/components/ranking/CountrySelect.tsx` — 국가 선택 shadcn Select
- `src/lib/relativeTime.test.ts` — `formatAbsoluteTime` 단위 테스트

**수정**
- `src/hooks/usePlayerRenewal.ts`, `src/hooks/useClubRenewal.ts` — `useRenewal` 위임 래퍼로 축소
- `src/lib/relativeTime.ts` — `formatAbsoluteTime` 추가
- `src/components/shared/SearchHistoryDropdown.tsx` — 트로피 이모지 → `icon_trophy.png`
- `src/components/layout/TopNav.tsx` — 피드백 버튼 가로 배치, 모바일 드로어 검색·피드백 제거
- `app/styles/app.css` — `.topnav-feedback`, 이벤트 카드 축소·검색창 폭·LIVE 제거 관련
- `app/events/_components/modeStyles.ts` — 그라디언트 → 단색 + 텍스트 대비 헬퍼
- `app/events/_components/RotationCard.tsx`, `SoloRankCard.tsx` — 단색 배경 적용, LIVE 뱃지 제거
- `app/events/_components/EventSearch.tsx` — PC 검색창 폭 제한
- `app/players/[tag]/_components/PlayerHero.tsx` — 아바타 축소, 클럽 줄 2줄깨짐 수정
- `app/players/[tag]/_components/battle/BattleRowHeader.tsx` — 시간 컬럼을 `BattleTime`으로 교체
- `app/styles/battle.css` — 모바일 배틀 행 그리드 재조정(결과 뱃지 잘림 수정 + 시간 노출)
- `app/players/page.tsx` (또는 `CountryFlagPills` 사용처) — `CountryFlagPills` → `CountrySelect` 교체

---

## Task 1: 갱신 hook 추상화 (항목 11)

**Files:**
- Create: `src/hooks/useRenewal.ts`
- Modify: `src/hooks/usePlayerRenewal.ts`, `src/hooks/useClubRenewal.ts`

`usePlayerRenewal`/`useClubRenewal`은 queryKey·API·invalidate 대상 3가지만 다르고 나머지가 동일하다. 공통 훅으로 통합하고 기존 두 훅은 위임 래퍼로 남겨 호출부 시그니처를 보존한다.

- [ ] **Step 1: `useRenewal` 제네릭 훅 생성**

`src/hooks/useRenewal.ts`:
```tsx
"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { renewalApi } from "@/api/endpoints/renewal";
import { NotFoundError } from "@/api/errors";
import { emptyRenewal } from "@/domain/renewal/empty";
import { queryKeys } from "@/hooks/queries/queryKeys";

export type RenewalKind = "player" | "club";

const config = {
  player: {
    renewalKey: queryKeys.player.renewal,
    allKey: queryKeys.player.all,
    get: renewalApi.getPlayer,
    renew: renewalApi.renewPlayer,
  },
  club: {
    renewalKey: queryKeys.club.renewal,
    allKey: queryKeys.club.all,
    get: renewalApi.getClub,
    renew: renewalApi.renewClub,
  },
} as const;

export function useRenewal(kind: RenewalKind, tag: string) {
  const qc = useQueryClient();
  const c = config[kind];

  const renewalQuery = useQuery({
    queryKey: c.renewalKey(tag),
    queryFn: async ({ signal }) => {
      try {
        return await c.get(tag, { signal });
      } catch (e) {
        if (e instanceof NotFoundError) return emptyRenewal(tag);
        throw e;
      }
    },
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(renewalQuery.data?.progressing ?? false);
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: c.allKey });
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc, c.allKey]);

  const trigger = useMutation({
    mutationFn: () => c.renew(tag),
    onSuccess: (data) => {
      qc.setQueryData(c.renewalKey(tag), data);
    },
  });

  return {
    renewal: renewalQuery.data,
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    renewedAt: renewalQuery.data?.renewedAt ?? null,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
```

- [ ] **Step 2: `usePlayerRenewal`을 래퍼로 축소**

`src/hooks/usePlayerRenewal.ts` 전체를 교체:
```tsx
"use client";

import { useRenewal } from "@/hooks/useRenewal";

export function usePlayerRenewal(tag: string) {
  return useRenewal("player", tag);
}
```

- [ ] **Step 3: `useClubRenewal`을 래퍼로 축소**

`src/hooks/useClubRenewal.ts` 전체를 교체:
```tsx
"use client";

import { useRenewal } from "@/hooks/useRenewal";

export function useClubRenewal(tag: string) {
  return useRenewal("club", tag);
}
```

- [ ] **Step 4: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 5: 갱신 동작 회귀 확인 (시각)**

플레이어 페이지(`http://localhost:3000/players/88J8Q9RPV`)에서 "갱신" 버튼이 기존과 동일하게 보이고 클릭 시 비활성/회전 상태로 전환되는지 시각 확인. (실제 갱신 요청이 prod에 영향 없는 로컬 core-api 기준)

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/hooks/useRenewal.ts imstargg-frontend/src/hooks/usePlayerRenewal.ts imstargg-frontend/src/hooks/useClubRenewal.ts
git commit -m "refactor(frontend): 갱신 로직 useRenewal 제네릭 훅으로 통합 (#49)"
```

---

## Task 2: 검색 기록 트로피 아이콘 교체 (항목 5)

**Files:**
- Modify: `src/components/shared/SearchHistoryDropdown.tsx`

최근/즐겨찾기 항목의 트로피 표시가 이모지 `🏆`다. `/assets/icon_trophy.png` 이미지로 바꾼다.

- [ ] **Step 1: import 추가**

`src/components/shared/SearchHistoryDropdown.tsx` 상단 import에 추가:
```tsx
import Image from "next/image";
```

- [ ] **Step 2: 트로피 이모지를 이미지로 교체**

현재(라인 173-183 부근):
```tsx
                  <span
                    style={{
                      marginLeft: "auto",
                      fontFamily: "var(--font-num)",
                      fontWeight: 700,
                      color: "#14161B",
                      fontVariantNumeric: "tabular-nums",
                    }}
                  >
                    {e.trophies.toLocaleString()} 🏆
                  </span>
```
교체:
```tsx
                  <span
                    style={{
                      marginLeft: "auto",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4,
                      fontFamily: "var(--font-num)",
                      fontWeight: 700,
                      color: "#14161B",
                      fontVariantNumeric: "tabular-nums",
                    }}
                  >
                    {e.trophies.toLocaleString()}
                    <Image
                      src="/assets/icon_trophy.png"
                      alt=""
                      width={14}
                      height={14}
                      unoptimized
                    />
                  </span>
```

- [ ] **Step 3: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 4: 시각 검증**

`http://localhost:3000` 검색창에 테스트 태그(예: `88J8Q9RPV`)를 한 번 검색해 최근 검색 기록을 만든 뒤, 검색창을 다시 포커스해 드롭다운을 열어 트로피가 `icon_trophy.png`로 표시되고 숫자 정렬이 유지되는지 확인.

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/src/components/shared/SearchHistoryDropdown.tsx
git commit -m "feat(frontend): 검색 기록 트로피를 icon_trophy.png 로 변경 (#49)"
```

---

## Task 3: 헤더 — 피드백 버튼 가로 배치 + 모바일 드로어 정리 (항목 1, 6)

**Files:**
- Modify: `src/components/layout/TopNav.tsx`, `app/styles/app.css`

데스크탑 피드백 버튼은 아이콘↑/텍스트↓ 세로로 보인다(`.topnav-link`에 flex 정렬이 없어 svg/span이 의도대로 정렬되지 않음). 가로 배치 전용 클래스를 부여한다. 모바일 드로어의 검색창(항목 1)과 피드백 항목(항목 6)을 제거한다.

- [ ] **Step 1: 피드백 버튼에 전용 클래스 부여**

`src/components/layout/TopNav.tsx` 라인 74-82 의 피드백 버튼 `className`을 `topnav-link` → `topnav-link topnav-feedback`로 변경:
```tsx
        <button
          type="button"
          onClick={() => setFeedbackOpen(true)}
          className="topnav-link topnav-feedback"
          aria-label="의견 보내기"
        >
          <MessageSquare className="h-4 w-4" aria-hidden="true" />
          <span className="hidden sm:inline">피드백</span>
        </button>
```

- [ ] **Step 2: 가로 정렬 CSS 추가**

`app/styles/app.css` 의 `.topnav-link.active { ... }`(라인 66) 다음 줄에 추가:
```css
.topnav-feedback { display: inline-flex; align-items: center; gap: 6px; }
```

- [ ] **Step 3: 모바일 드로어 검색창 제거**

`src/components/layout/TopNav.tsx` 라인 118-120 블록 삭제:
```tsx
            <div className="topnav-search-drawer">
              <PlayerSearchInput />
            </div>
```

- [ ] **Step 4: 모바일 드로어 피드백 항목 제거**

같은 파일 라인 133-142 의 드로어 내 피드백 버튼 삭제:
```tsx
              <button
                type="button"
                aria-label="의견 보내기"
                onClick={() => {
                  setDrawerOpen(false);
                  setFeedbackOpen(true);
                }}
              >
                피드백
              </button>
```

- [ ] **Step 5: 미사용 import 정리**

드로어에서 `PlayerSearchInput` 사용처가 데스크탑(라인 84-86) 외에 없으면 import 유지. 검색 드로어만 제거했으므로 데스크탑 `topnav-desktop-search`는 그대로 → `PlayerSearchInput` import 유지(삭제하지 말 것). `SheetDescription` 문구는 "주요 페이지" 의미로 유지.

- [ ] **Step 6: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 7: 시각 검증**

- 데스크탑(1280) `http://localhost:3000`: 헤더 피드백 버튼이 아이콘 + "피드백"이 한 줄(가로)로 표시.
- 모바일(390) 헤더: 피드백 아이콘 버튼 유지 확인. 햄버거 열기 → 드로어에 검색창 없음 + 피드백 항목 없음(브롤러/이벤트/플레이어/클럽 링크만).

- [ ] **Step 8: 커밋**

```bash
git add imstargg-frontend/src/components/layout/TopNav.tsx imstargg-frontend/app/styles/app.css
git commit -m "feat(frontend): 피드백 버튼 가로 배치 및 모바일 드로어 검색/피드백 제거 (#49)"
```

---

## Task 4: 랭킹 나라 선택 셀렉트화 (항목 7)

**Files:**
- Create: `src/components/ranking/CountrySelect.tsx`
- Modify: `CountryFlagPills` 사용처(랭킹 페이지)

국기 pill 버튼 그룹을 shadcn `Select`로 교체한다. URL 쿼리(`?country=`) 연동을 유지한다.

- [ ] **Step 1: `CountrySelect` 컴포넌트 생성**

`src/components/ranking/CountrySelect.tsx`:
```tsx
"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { COUNTRY_CODES, COUNTRY_META, type Country } from "@/domain/ranking/country";

type Props = {
  current: Country;
};

export function CountrySelect({ current }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  function handleChange(code: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (code === "global") {
      params.delete("country");
    } else {
      params.set("country", code);
    }
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <Select value={current} onValueChange={handleChange}>
      <SelectTrigger aria-label="국가 선택" className="w-[140px]">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {COUNTRY_CODES.map((code) => (
          <SelectItem key={code} value={code}>
            <span aria-hidden="true">{COUNTRY_META[code].flag}</span>
            <span>{COUNTRY_META[code].label}</span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
```

- [ ] **Step 2: 사용처에서 `CountryFlagPills` → `CountrySelect` 교체**

`CountryFlagPills`를 import/사용하는 파일을 찾는다:
Run: `grep -rn "CountryFlagPills" imstargg-frontend/app imstargg-frontend/src`
해당 파일에서 import 와 `<CountryFlagPills current={...} />` 를 `import { CountrySelect } from "@/components/ranking/CountrySelect";` 와 `<CountrySelect current={...} />` 로 교체(`current` prop 값은 그대로 전달).

- [ ] **Step 3: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 4: 시각 + 동작 검증**

`http://localhost:3000/players`(데스크탑·모바일): 나라 선택이 셀렉트로 표시되고, 항목 선택 시 랭킹이 해당 국가로 갱신되며 URL `?country=` 가 동기화되는지 확인. 새로고침 시 선택 유지 확인.

- [ ] **Step 5: 미사용 컴포넌트 처리**

`CountryFlagPills`의 다른 사용처가 없으면(Step 2 grep 결과로 확인) 파일과 관련 CSS(`app/styles/app.css` `.country-pill*` 블록)는 후속 정리 대상으로 남기되, 이번 커밋에서는 삭제하지 않고 미사용으로 둔다(회귀 위험 최소화). lint가 미사용 에러를 내면 그때 제거.

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/src/components/ranking/CountrySelect.tsx imstargg-frontend/app
git commit -m "feat(frontend): 랭킹 나라 선택을 셀렉트로 변경 (#49)"
```

---

## Task 5: 이벤트 모드 색상 단색화 (항목 4)

**Files:**
- Modify: `app/events/_components/modeStyles.ts`, `RotationCard.tsx`, `SoloRankCard.tsx`

그라디언트를 이슈 지정 단색으로 교체하고, 배경 명도에 따라 텍스트 색을 자동 대비한다.

- [ ] **Step 1: `modeStyles.ts`를 단색 + 대비 헬퍼로 교체**

`app/events/_components/modeStyles.ts` 전체 교체:
```ts
export interface ModeStyle {
  key: string;
  /** 단색 배경 (hex) */
  bg: string;
  /** 배경 대비 텍스트/아이콘 색 */
  fg: string;
}

/** hex 배경의 상대 명도로 흰색/짙은색 텍스트를 고른다. */
function pickFg(hex: string): string {
  const m = hex.replace("#", "");
  const r = parseInt(m.slice(0, 2), 16);
  const g = parseInt(m.slice(2, 4), 16);
  const b = parseInt(m.slice(4, 6), 16);
  // 상대 휘도 (sRGB 근사)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.6 ? "#14161B" : "#FFFFFF";
}

function style(key: string, bg: string): ModeStyle {
  return { key, bg, fg: pickFg(bg) };
}

const DEFAULT_STYLE: ModeStyle = style("default", "#2A2E38");

// 이슈 #49 지정 색상(단색). 미지정 모드는 기존 톤의 대표색을 단색화.
const MAP: Record<string, ModeStyle> = {
  젬그랩: style("gemGrab", "#8F43EA"),        // 143,67,234
  브롤볼: style("brawlBall", "#909FDB"),       // 144,159,219
  하이스트: style("heist", "#C763CD"),          // 199,99,205
  녹아웃: style("knockout", "#E8893A"),         // 232,137,58
  솔로쇼다운: style("soloShowdown", "#95D449"), // 149,212,73
  듀오쇼다운: style("duoShowdown", "#95D449"),  // 149,212,73
  바스켓브롤: style("basketBrawl", "#62C1F4"),  // 98,193,244
  브롤하키: style("brawlHockey", "#D2D2FB"),    // 210,210,251
  // --- 이슈 미지정: 기존 대표색 단색화 ---
  핫존: style("hotZone", "#FF8A1F"),
  현상금사냥: style("bounty", "#FFC60A"),
  듀얼: style("duels", "#FF5DAA"),
  전멸전: style("wipeout", "#FF3A3A"),
  발리브롤: style("volleyBrawl", "#3DE0FF"),
  공성전: style("siege", "#5C6271"),
  트로피쟁탈전: style("trophyThieves", "#FFC60A"),
  보스전: style("bossFight", "#C9141A"),
  빅게임: style("bigGame", "#B83BFF"),
  로보럼블: style("roboRumble", "#58D7E8"),
};

function normalize(name: string): string {
  return name.replace(/\s+/g, "").trim();
}

export function getModeStyle(name: string | null | undefined): ModeStyle {
  if (!name) return DEFAULT_STYLE;
  return MAP[normalize(name)] ?? DEFAULT_STYLE;
}
```

> 비고: "브롤하키"는 현재 로테이션에 없을 수 있으나 백엔드가 모드명 "브롤 하키"로 내려주면 매핑된다(normalize가 공백 제거). 솔로/듀오 쇼다운 모두 이슈의 쇼다운 색을 쓴다.

- [ ] **Step 2: `RotationCard.tsx`에서 단색 배경 + 텍스트 대비 적용**

`app/events/_components/RotationCard.tsx` 라인 17-20 의 head div를 교체:
```tsx
      <div
        className="event-card-head"
        style={{ background: modeStyle.bg, color: modeStyle.fg }}
      >
```
그리고 모드 라벨이 텍스트 색을 상속하도록, 라인 36 `<span className="event-card-mode-label">{modeName}</span>` 는 그대로 두되(상속), CSS에서 라벨 색 고정이 있으면 Step 4에서 제거한다.

- [ ] **Step 3: `SoloRankCard.tsx`에서 단색 배경 적용**

`app/events/_components/SoloRankCard.tsx` 라인 15-18 의 icon-wrap div 교체:
```tsx
      <div
        className="solorank-card-icon-wrap"
        style={{ background: modeStyle.bg, color: modeStyle.fg }}
      >
```

- [ ] **Step 4: 카드 헤더 텍스트 색 고정 제거(있으면)**

Run: `grep -n "event-card-mode-label\|event-card-head" imstargg-frontend/app/styles/app.css`
`.event-card-mode-label` 또는 `.event-card-head` 에 `color: ...` 가 하드코딩돼 있으면, 인라인 `color`(modeStyle.fg)가 이기도록 해당 `color` 선언을 제거한다(배경 명도별 대비를 위해).

- [ ] **Step 5: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 6: 시각 검증**

`http://localhost:3000/events`(데스크탑·모바일): 각 모드 카드 헤더가 지정 단색으로 표시되고, 밝은 배경(쇼다운·바스킷브롤·브롤하키)에서 텍스트/아이콘이 짙은 색으로 읽히는지 확인.

- [ ] **Step 7: 커밋**

```bash
git add imstargg-frontend/app/events/_components/modeStyles.ts imstargg-frontend/app/events/_components/RotationCard.tsx imstargg-frontend/app/events/_components/SoloRankCard.tsx imstargg-frontend/app/styles/app.css
git commit -m "feat(frontend): 이벤트 모드 색상을 이슈 지정 단색으로 변경 (#49)"
```

---

## Task 6: 이벤트 카드 축소 + LIVE 뱃지 제거 + PC 검색창 폭 (항목 2, 3c)

**Files:**
- Modify: `app/events/_components/RotationCard.tsx`, `EventSearch.tsx`, `app/styles/app.css`

- [ ] **Step 1: LIVE 뱃지 제거 (JSX)**

`app/events/_components/RotationCard.tsx` 라인 38-41 의 LIVE span 삭제:
```tsx
        <span className="event-card-live">
          <span className="event-card-live-dot" aria-hidden="true" />
          LIVE
        </span>
```

- [ ] **Step 2: LIVE 관련 CSS 제거**

`app/styles/app.css` 의 `.event-card-live`(라인 722~), `.event-card-live-dot`(라인 738~) 및 `@media` 안 `.event-card-live-dot` 애니메이션 참조(라인 859 부근)를 제거한다.

- [ ] **Step 3: PC 검색창 폭 제한**

`app/events/_components/EventSearch.tsx` 라인 28 의 검색 래퍼에 PC 폭 제한 클래스를 추가:
```tsx
      <div className="relative w-full sm:max-w-sm">
```
(모바일은 전체 폭, ≥640px에서 최대 24rem로 제한)

- [ ] **Step 4: 이벤트 카드 패딩/높이 축소**

`app/styles/app.css` 이벤트 카드 블록(655~785)에서 컴팩트화. 현재 값 확인 후 다음 방향으로 줄인다(정확 수치는 Step 6 시각 검증으로 조정):
- `.event-card-head` 의 `min-height`/`padding` 축소(예: padding 14px→10px)
- `.event-card-mode-icon` 크기 축소(56→44 등) 및 `RotationCard.tsx`의 `<Image width/height>`도 동일 값으로 맞춤
- `.event-card-body`/`.event-card-footer` 의 `padding`·`min-height` 축소
- `.event-card-mapname` 폰트 축소(20→17 등)

PC 그리드 열 수는 카드 폭 결정에 영향. `RotationSection`의 `xl:grid-cols-3`을 `xl:grid-cols-4` 후보로 시각 검증하며 결정(가독성 유지되면 4열 채택, 아니면 3열 유지 + 카드만 축소).

- [ ] **Step 5: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 6: 시각 검증**

`http://localhost:3000/events`(데스크탑·모바일): LIVE 뱃지 없음 · 카드가 더 컴팩트해 한 화면에 더 많은 항목 노출 · PC 맵 검색창이 과도하게 넓지 않음 · 모드 아이콘/맵 이름 잘림 없음.

- [ ] **Step 7: 커밋**

```bash
git add imstargg-frontend/app/events imstargg-frontend/app/styles/app.css
git commit -m "feat(frontend): 이벤트 카드 축소·LIVE 뱃지 제거·PC 맵 검색창 폭 제한 (#49)"
```

---

## Task 7: 배틀 시간 절대시간 포맷 함수 (항목 8 — 로직)

**Files:**
- Modify: `src/lib/relativeTime.ts`
- Create: `src/lib/relativeTime.test.ts`

툴팁에 보여줄 실제(절대) 시간 포맷 순수 함수를 TDD로 추가한다.

- [ ] **Step 1: 실패하는 테스트 작성**

`src/lib/relativeTime.test.ts`:
```ts
import { describe, it, expect } from "vitest";
import { formatAbsoluteTime } from "./relativeTime";

describe("formatAbsoluteTime", () => {
  it("ISO 문자열을 YYYY.MM.DD HH:mm 으로 포맷한다", () => {
    // 2026-06-03T05:09:00Z → 로컬 타임존에 따라 달라지므로 로컬 기준 검증
    const iso = "2026-06-03T14:09:00";
    expect(formatAbsoluteTime(iso)).toBe("2026.06.03 14:09");
  });

  it("한 자리 월/일/시/분을 0으로 패딩한다", () => {
    const iso = "2026-01-05T03:07:00";
    expect(formatAbsoluteTime(iso)).toBe("2026.01.05 03:07");
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm --dir imstargg-frontend test:run src/lib/relativeTime.test.ts`
Expected: FAIL — `formatAbsoluteTime` is not a function / not exported

- [ ] **Step 3: 최소 구현 추가**

`src/lib/relativeTime.ts` 끝에 추가:
```ts
export function formatAbsoluteTime(iso: string): string {
  const d = new Date(iso);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd} ${hh}:${mi}`;
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm --dir imstargg-frontend test:run src/lib/relativeTime.test.ts`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/src/lib/relativeTime.ts imstargg-frontend/src/lib/relativeTime.test.ts
git commit -m "feat(frontend): 절대시간 포맷 formatAbsoluteTime 추가 (#49)"
```

---

## Task 8: 배틀 시간 툴팁 — PC hover / 모바일 탭 (항목 8 — UI)

**Files:**
- Create: `app/players/[tag]/_components/battle/BattleTime.tsx`
- Modify: `app/players/[tag]/_components/battle/BattleRowHeader.tsx`, `app/styles/battle.css`

상대시간을 클릭/호버 시 실제시간 툴팁으로 보여준다. PC는 hover, 모바일은 탭으로 토글(Radix Tooltip을 controlled로 사용).

- [ ] **Step 1: `BattleTime` 컴포넌트 생성**

`app/players/[tag]/_components/battle/BattleTime.tsx`:
```tsx
"use client";

import { useState } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { formatRelativeTime, formatAbsoluteTime } from "@/lib/relativeTime";

export function BattleTime({ iso }: { iso: string }) {
  const [open, setOpen] = useState(false);
  return (
    <TooltipProvider delayDuration={0}>
      <Tooltip open={open} onOpenChange={setOpen}>
        <TooltipTrigger asChild>
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setOpen((o) => !o);
            }}
            className="battle-row-relative-time"
            style={{
              fontSize: 12,
              color: "#5C6271",
              whiteSpace: "nowrap",
              background: "none",
              border: "none",
              padding: 0,
              cursor: "pointer",
            }}
          >
            {formatRelativeTime(iso)}
          </button>
        </TooltipTrigger>
        <TooltipContent>{formatAbsoluteTime(iso)}</TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
```

> 동작: PC는 hover 시 Radix가 `onOpenChange(true)`로 열고, 모바일은 탭(onClick)으로 토글. `asChild`로 버튼을 트리거로 사용해 키보드 포커스도 동작. 배틀 행이 `Link`/확장 토글 안에 있을 수 있으므로 `preventDefault`/`stopPropagation`로 행 네비게이션과 분리.

- [ ] **Step 2: `BattleRowHeader`에서 시간 표시를 `BattleTime`으로 교체**

`app/players/[tag]/_components/battle/BattleRowHeader.tsx`:
- 라인 7 import 교체:
```tsx
import { BattleTime } from "./BattleTime";
```
(기존 `import { formatRelativeTime } from "@/lib/relativeTime";` 제거)
- 라인 128-130 의 시간 div 교체:
```tsx
      <div className="battle-row-time-cell">
        <BattleTime iso={battle.battleTime} />
      </div>
```

- [ ] **Step 3: 모바일 배틀 행 그리드 재조정 (결과 잘림 수정 + 시간 노출)**

`app/styles/battle.css` 의 모바일 미디어쿼리(`@media (max-width: 640px)`)를 다음으로 교체(결과 뱃지가 잘리지 않고 시간이 노출되도록):
```css
@media (max-width: 640px) {
  .battle-row-header {
    grid-template-columns: 3px 24px minmax(0, 1fr) auto auto !important;
    gap: 6px !important;
    padding: 8px !important;
  }
  /* 모바일에서는 소유 브롤러 아바타와 확장 화살표를 숨겨 공간 확보 */
  .battle-row-header .battle-row-owner-brawler {
    display: none !important;
  }
  /* 시간은 다시 노출 (탭 시 실제시간 툴팁) */
  .battle-row-header .battle-row-time-cell {
    display: block;
  }
}
```
> 현재 inline 그리드는 7컬럼(`4px 28px 1fr auto auto auto 24px`). 모바일에서 브롤러 컬럼(`.battle-row-owner-brawler`)을 숨기면 실제 렌더 컬럼 수가 줄어드는 게 아니라 빈 셀이 되므로, 그리드를 5컬럼으로 재정의하고 화살표 셀은 시각 검증에서 넘치면 별도 클래스로 숨긴다. 정확한 컬럼/숨김은 Step 5 시각 검증으로 확정.

- [ ] **Step 4: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 5: 시각 검증**

`http://localhost:3000/players/88J8Q9RPV`:
- 데스크탑(1280): 배틀 행 시간에 마우스 hover 시 실제시간 툴팁 표시.
- 모바일(390): 배틀 행에 시간이 다시 보이고, **결과 뱃지(WIN/LOSS)가 화면 안에 완전히 보이며 잘리지 않음**. 시간을 탭하면 실제시간 툴팁 표시.

- [ ] **Step 6: 커밋**

```bash
git add imstargg-frontend/app/players/[tag]/_components/battle/BattleTime.tsx imstargg-frontend/app/players/[tag]/_components/battle/BattleRowHeader.tsx imstargg-frontend/app/styles/battle.css
git commit -m "feat(frontend): 배틀 시간 실제시간 툴팁(PC hover/모바일 탭) 및 모바일 결과 잘림 수정 (#49)"
```

---

## Task 9: 플레이어 히어로 카드 모바일 정리 (항목 3a)

**Files:**
- Modify: `app/players/[tag]/_components/PlayerHero.tsx`

모바일에서 이름 과잉 잘림, "클럽:" 2줄 깨짐을 해소하고 아바타를 축소한다.

- [ ] **Step 1: 아바타 축소 (모바일 64 / 데스크탑 80)**

`PlayerAvatar`는 inline style로 width/height를 강제하므로 size prop으로 제어한다. `PlayerHero.tsx` 라인 24 를 반응형 두 인스턴스로 교체:
```tsx
        <PlayerAvatar name={player.name} size={64} className="sm:hidden" />
        <PlayerAvatar name={player.name} size={80} className="hidden sm:flex" />
```
> `PlayerAvatar` 루트가 `flex`를 쓰므로 데스크탑 표시는 `sm:flex`로 한다(`sm:block`이 아니라).

- [ ] **Step 2: "클럽:" 줄 2줄 깨짐 수정**

`PlayerHero.tsx` 라인 34-56 의 클럽 표시 `<p>`를 한 줄 유지 + 클럽명만 truncate로 변경. 라인 34 를:
```tsx
          <p className="mt-1 flex min-w-0 flex-nowrap items-center gap-1.5 text-sm">
```
로 바꾸고, 클럽명 `<Link>`(라인 45-50)에 `truncate min-w-0` 추가:
```tsx
                <Link
                  className="min-w-0 truncate font-medium hover:underline"
                  href={routes.club(player.club.tag)}
                >
                  {player.club.name}
                </Link>
```

- [ ] **Step 3: 타입/린트 확인**

Run: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint`
Expected: 에러 없음

- [ ] **Step 4: 시각 검증**

`http://localhost:3000/players/88J8Q9RPV`:
- 모바일(390): "클럽:" 라벨이 한 줄로 표시(2줄 깨짐 없음), 클럽명이 길면 `…`로 truncate. 아바타가 64px로 줄어 이름 공간이 늘고 과잉 잘림이 완화됨.
- 데스크탑(1280): 아바타 80px 유지, 레이아웃 회귀 없음.

- [ ] **Step 5: 커밋**

```bash
git add imstargg-frontend/app/players/[tag]/_components/PlayerHero.tsx
git commit -m "feat(frontend): 플레이어 히어로 카드 모바일 아바타 축소·클럽 줄 정리 (#49)"
```

---

## 최종 회귀 확인

- [ ] 전체 타입/린트/단위 테스트: `pnpm --dir imstargg-frontend typecheck && pnpm --dir imstargg-frontend lint && pnpm --dir imstargg-frontend test:run`
- [ ] playwright로 홈/이벤트/플레이어 상세/브롤러/랭킹을 데스크탑(1280)·모바일(390)에서 순회하며 잘림·2줄깨짐·레이아웃 깨짐이 없는지 최종 확인
- [ ] 검증용 스크린샷(모노레포 루트 `*.png`)과 `.playwright-mcp/` 산출물은 커밋에 포함하지 않음

---

## Self-Review (작성자 점검 결과)

- **Spec 커버리지:** 항목 1(Task3) · 2(Task6) · 3a(Task9) · 3c(Task6) · 4(Task5) · 5(Task2) · 6(Task3) · 7(Task4) · 8(Task7+8) · 11(Task1) 모두 태스크 존재. 3b는 spec에서 종료, 9·10은 범위 외.
- **Placeholder:** 픽셀 수치 일부는 "시각 검증으로 조정"으로 의도적으로 열어둠(레이아웃 특성상 실측 필요). 그 외 모호 지시 없음.
- **타입 일관성:** `ModeStyle`이 Task5에서 `{key,bg,fg}`로 바뀌고 RotationCard/SoloRankCard가 `modeStyle.bg/.fg`를 사용(Task5 내 일관). `formatAbsoluteTime`(Task7) → `BattleTime`(Task8)에서 사용. `useRenewal`(Task1) 반환 형태가 기존 훅과 동일해 호출부 호환.
