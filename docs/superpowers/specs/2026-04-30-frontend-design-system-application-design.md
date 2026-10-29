# Design System Application — Frontend Design Spec

**작성일**: 2026-04-30
**상태**: Draft → 승인 대기

## 목적

`DESIGN.md` 가 정의한 도메인 시각 시스템(트로피·클래스·희귀도·랭크 아이콘) 이 실제 페이지에 거의 적용되어 있지 않다. emoji·plain 텍스트·정보 중복으로 op.gg 같은 데이터 통계 사이트의 정체성이 흐리다. 한 사이클로 도메인 시각 primitive 를 일반화하고 핵심 진입 페이지(MobileTabBar, Player, Brawler 상세, Brawler 메타)에 적용한다.

## 현재 상태

### 렌더링 결함 (chrome-devtools 오디트 결과)

- `MobileTabBar.tsx:12-16` — 탭 5개가 emoji (🏠 ⭐ 🗓️ 🏆 🛡️). DESIGN.md 의 클래스/아이콘 시스템과 충돌.
- `PlayerContent.tsx:44` — `defaultValue="stats"`. 사용자 요구는 "배틀" 이 기본.
- `PlayerSidebar.tsx` ↔ `PlayerStats.tsx` — 트로피·최고 트로피가 양쪽 모두에 표시되어 정보 중복.
- `PlayerBrawlers.tsx:31` — `🏆 {trophies}` emoji 사용. `Trophy` primitive 미사용.
- `BrawlerHeader` (브롤러 상세 헤더) — `대미지 딜러` 등 라벨이 텍스트만. `ClassIcon` 미사용.
- `ItemOwnershipSection.tsx:102` — 버피 영문 enum (`GADGET`, `HYPER_CHARGE`, `STAR_POWER`) 그대로 노출.
- 브롤러 상세 portrait 이미지가 LCP 인데 `priority` 미설정 — 콘솔 경고 1건.

### Primitive 시스템 분기

- 구파 (`Trophy.tsx`, `ClassIcon.tsx`, `BrawlerPortrait.tsx`): `lib/data.ts` mock 에 의존.
  - `Trophy({ value, size })` — `fmtNum` 만 mock 에 의존. 실 사용처: `BattlePlayerRow.tsx`.
  - `ClassIcon({ cls: ClassKey, size })` — mock 의 `ClassKey` (DAMAGE / TANK / …) 와 `CLS` 매핑 의존. 실 사용처: 없음.
  - `BrawlerPortrait({ id: string })` — mock `BRAWLERS` 배열을 id 로 lookup → 그라디언트 + 이니셜. 실제 이미지 표시 안 함. 실 사용처: `BattleRowHeader.tsx`, `BattlePlayerRow.tsx`.
- 신파 (`BrawlerAvatar.tsx`): 도메인 모델 직접 받음 (`name`, `imageUrl`, `rarity: BrawlerRarity`). `next/image` + CDN. 실 사용처: 없음(도입만 되고 미적용).
- 도메인 enum (`api/types.ts`): `BrawlerRole` (DAMAGE_DEALER / TANK / MARKSMAN / ASSASSIN / SUPPORT / CONTROLLER / ARTILLERY) — mock 의 `ClassKey` 와 명칭 충돌.
- 도메인 라벨 (`domain/brawler/labels.ts`): `RARITY_LABEL_KO`, `ROLE_LABEL_KO` 이미 정의.

### 컨테이너 폭 산발

- Player: `max-w-4xl` (≈ 56rem)
- Brawler 메타: `max-w-6xl` (≈ 72rem)
- DESIGN.md 표준: 1280px (`max-w-7xl`)

## Goals

1. 도메인 시각 primitive 가 도메인 모델만으로 동작하고 mock(`lib/data.ts`) 의존이 모두 제거된다. 실제 페이지에서 일관되게 사용된다.
2. MobileTabBar 가 5개 모두 정적 자산 이미지 또는 동질 스타일의 line icon 으로 렌더된다. emoji 부재.
3. Player 페이지가 단일 Hero 카드(아바타 + 이름/태그/갱신/클럽 + 6-칸 stats grid) + Tabs(배틀 default · 브롤러) 로 통합되고 정보 중복이 제거된다. 통계 탭 삭제. 기본 탭 = 배틀.
4. Brawler 상세에 클래스 아이콘이 들어가고, 버피 영문 enum 이 한글로 매핑되며, LCP eager 경고가 사라진다.
5. Brawler 메타 테이블의 브롤러 행에 클래스 아이콘이 함께 표시된다.
6. 영향 받는 페이지의 컨테이너 폭이 1280px (`max-w-7xl`) 로 통일된다.
7. `pnpm typecheck` / `pnpm lint` / `pnpm test:run` 통과. 페이지별 콘솔 경고 0 (LCP 포함).

## Non-goals

- Ranking / Club / Event 페이지 시각 적용 — 같은 primitive 적용으로 후속 사이클에서 처리.
- TanStack Query Devtools 위치 조정 (dev-only).
- DESIGN.md 자체 수정.
- 브롤러 메타 테이블의 컬럼 구조 변경 (클래스 아이콘만 행 안에 추가).
- 새 자산 디자인.

## 설계

### 1. 도메인 시각 primitive 일반화

#### `Trophy.tsx`
- `import { fmtNum } from "@/lib/data"` → `import { formatNumber } from "@/lib/format"`. mock 의존 제거.
- API 그대로(`{ value, size }`). 정적 `/assets/trophy.png` raw `<img>` 유지 (AGENTS.md 정합).

#### `ClassIcon.tsx`
- `cls: ClassKey` (mock) → `role: BrawlerRole` (도메인) 로 시그니처 변경.
- 매핑 데이터를 `domain/brawler/roleMeta.ts` 신설로 이동:
  ```ts
  export const ROLE_META: Record<BrawlerRole, { color: string; icon: string }> = {
    DAMAGE_DEALER: { color: "var(--class-damage)",     icon: "class_damage.svg" },
    TANK:          { color: "var(--class-tank)",       icon: "class_tank.svg" },
    MARKSMAN:      { color: "var(--class-marksman)",   icon: "class_marksmen.svg" },
    ASSASSIN:      { color: "var(--class-assassin)",   icon: "class_assassin.svg" },
    SUPPORT:       { color: "var(--class-support)",    icon: "class_support.svg" },
    CONTROLLER:    { color: "var(--class-controller)", icon: "class_controller.svg" },
    ARTILLERY:     { color: "var(--class-artillery)",  icon: "class_artillery.svg" },
  };
  ```
- `ClassIcon` 은 위 매핑을 받아 18px 원형 + svg 글리프 렌더 (현재 구조 유지). alt 는 `ROLE_LABEL_KO[role]`.

#### `BrawlerPortrait.tsx` 제거 + `BrawlerAvatar.tsx` 로 통합
- `BrawlerPortrait` (mock id 의존) 사용처 2건(`BattleRowHeader.tsx`, `BattlePlayerRow.tsx`) 을 `BrawlerAvatar` 로 교체.
- `BattlePlayer` 도메인의 `brawler.brawler` 가 `BrawlerItem | null` (`id`, `name`, `imageUrl`) — `BrawlerAvatar({ name, imageUrl, size })` 형태로 매핑. brawler 가 null 이면 placeholder.
- 통합 후 `BrawlerPortrait.tsx` 삭제.

#### `lib/data.ts` 정리
- 사용처 잔존 확인: `Trophy.tsx` (제거), `BrawlerPortrait.tsx` (제거), `ClassIcon.tsx` (제거), `TopNav.tsx` (`fmtMinAgo` 사용 — `lib/format.ts` 또는 `lib/relativeTime.ts` 로 이동).
- 모든 사용처 정리 후 `lib/data.ts` 자체를 삭제 가능하면 삭제. 추가 mock 의존이 남으면 spec 안에서 "후속 정리" 로 표기 후 진행.

### 2. MobileTabBar 자산 매핑

`MobileTabBar.tsx` 의 emoji 를 다음으로 교체:

| 탭 | 자산 / 아이콘 | 비고 |
|---|---|---|
| 홈 | `/assets/star.webp` | BS 별, 브랜드 정체성 |
| 브롤러 | `/assets/star_wings.webp` | 날개 별 (`star.webp` 와 시각 차별화) |
| 이벤트 | `lucide-react` `Calendar` | 자산 부재. line icon 1개로 시각 통일감 유지 |
| 랭킹 | `/assets/trophy.png` | |
| 클럽 | `/assets/club_league.png` | |

- 정적 `/assets/*` 자산은 raw `<img width={20} height={20} alt="">` (AGENTS.md 정합).
- 액티브 시 옐로우 텍스트는 그대로. 액티브 아이콘은 그대로(투명도 변화 없음).
- Calendar 아이콘은 `Calendar` (`lucide-react`) `size={20}`, `strokeWidth={2.25}`.

### 3. Player 페이지 — Hero 카드 통합

#### 컴포넌트 구조 변경

- `PlayerStats.tsx` — **삭제**.
- `PlayerSidebar.tsx` → `PlayerHero.tsx` 로 재명명 + 아래 구조로 재작성.
- `PlayerContent.tsx`:
  - `import { PlayerSidebar }` → `import { PlayerHero }`.
  - `import { PlayerStats }` 라인 삭제.
  - `Tabs defaultValue="stats"` → `defaultValue="battles"`.
  - `<TabsTrigger value="stats">` 와 `<TabsContent value="stats">` 삭제.
  - 컨테이너 `max-w-4xl` → `max-w-7xl`.

#### `PlayerHero.tsx` 레이아웃

```
┌─────────────────────────────────────────────────────────────┐
│ [BrawlerAvatar 80px ?]  이름 (t-h2)         [⟳ 갱신]         │
│                          #태그 (t-mono)                       │
│                          🛡️ 클럽: SJCE →                      │
├─────────────────────────────────────────────────────────────┤
│ 트로피  | 최고 트로피 | 프레스티지 | EXP Lv | 3v3 승 | 솔로 승 │
│ <Trophy> | <Trophy>   | num       | num   | num   | num     │
└─────────────────────────────────────────────────────────────┘
```

- 카드 컨테이너는 DESIGN.md 의 hero 블록(28×24 패딩, `--shadow-card-lg`).
- 좌측 아바타 영역: 플레이어 도메인에 portrait 이 없으므로 첫 글자 이니셜 + 그라디언트(브랜드 옐로우 → 오렌지). `BrawlerAvatar` 의 fallback 분기를 그대로 활용해도 되지만, 브롤러가 아니므로 `src/components/primitives/PlayerAvatar.tsx` 신규 컴포넌트(이니셜 전용) 로 분리. 80px.
- 이름 색은 `nameColor` 가 도메인에 없으면 토큰 기본(`--fg`).
- 트로피 두 칸은 `<Trophy value={...} size={20}>` (스타일 일관). 나머지 4칸은 `formatNumber()` + `var(--font-num)` tabular-nums.
- 클럽 라인은 `club_league.png` 16px + 이름 + `→`. 클럽 미가입 시 "소속 클럽 없음" (`fg-subtle`).
- 데스크톱: 6-col grid. 모바일(<640px): 3×2 grid.
- `RenewalButton` 위치는 우상단 (현재와 동일).

#### Tabs

- `TabsList`: 두 탭(`battles`, `brawlers`).
- `defaultValue="battles"` — 사용자 핵심 요구.
- 탭 자체는 shadcn `<Tabs>` 유지(인터랙티브 영역 가이드 정합). 단, 시각 토큰 미적용으로 작아 보이는 문제는 `app/styles/components.css` (또는 globals.css) 에서 `[role="tablist"]` 보강이 아닌, `TabsList`/`TabsTrigger` 의 className 에 DESIGN.md `.tabs` 패턴 일부(언더라인·14px·600) 적용.

### 4. Brawler 상세

- 헤더의 `대미지 딜러` 등 텍스트 라벨 → `<ClassIcon role={brawler.role} size={18} />` + 텍스트.
- 헤더 portrait 이미지에 `priority` prop 추가 (`Image priority unoptimized`). LCP 경고 해소.
- 희귀도: 헤더 avatar 에 `BrawlerAvatar showFrame` 또는 동일 색 매핑으로 frame 추가.
- 버피 enum 한글화 — `domain/brawler/buffyTypeLabel.ts` 신설:
  ```ts
  export const BUFFY_TYPE_LABEL_KO: Record<BuffyType, string> = {
    GADGET: "가젯",
    HYPER_CHARGE: "하이퍼차지",
    STAR_POWER: "스타파워",
  };
  ```
  `ItemOwnershipSection.tsx:102` 의 `name={b.buffyType}` 을 `name={BUFFY_TYPE_LABEL_KO[b.buffyType] ?? b.buffyType}` 로 교체.

### 5. Brawler 메타 테이블

- `app/brawlers/_components/BrawlerStatTable.tsx` (또는 행 컴포넌트) 의 "브롤러" 셀에서 이름 옆에 `<ClassIcon role={brawler.role} size={16} />` 추가.
- 컬럼 구조 변경 없음. 한 줄 인라인.

### 6. 컨테이너 폭

- 영향 페이지 모두 `max-w-7xl` (1280px) 로 통일:
  - `app/players/[tag]/_components/PlayerContent.tsx:42`
  - `app/brawlers/page.tsx:48`
  - `app/brawlers/[id]/page.tsx` (확인 필요. 다르면 변경)

## 테스팅 전략

### 단위 테스트

- `Trophy` — `formatNumber` 결과 텍스트 노출, alt 빈 문자열 (기존 테스트가 있다면 유지).
- `ClassIcon` — 모든 `BrawlerRole` 에 대해 alt 가 `ROLE_LABEL_KO[role]` 와 일치, src 가 `ROLE_META[role].icon` 매핑 정합.
- `BUFFY_TYPE_LABEL_KO` — 모든 enum 키에 한글 라벨 존재.

### 통합 / 컴포넌트 테스트

- `PlayerHero` — 도메인 mock(Player) 입력 시 트로피·최고트로피·프레스티지·EXP·3v3승·솔로승 6값이 모두 노출. 클럽 있/없 두 분기.
- `PlayerContent` 통합 — `defaultValue="battles"` 가 적용되어 초기 렌더에서 `BattleList` 가 보이고, "통계" 탭이 DOM 에 없는지 검증.
- `MobileTabBar` — 5 탭 모두 자산/아이콘 렌더, emoji 부재 (텍스트 문자열에 emoji 시그니처 없음 검증).

### 시각/런타임 검증

- `pnpm dev` 후 chrome-devtools 로 4 페이지 + 모바일 viewport(390px) 콘솔 0 warning 확인.
- 브롤러 상세 LCP eager 경고가 사라졌는지 (콘솔).

### 정적

- `pnpm typecheck`, `pnpm lint` 통과.
- `pnpm test:run` 통과.

## 위험과 대응

| 위험 | 대응 |
|---|---|
| `lib/data.ts` 잔존 사용처 누락 | `grep -rn "from.*lib/data"` 로 전수 검사. 한 commit 안에 모두 정리하지 못하면 spec 의 1번 항목을 "사용처 0 개 도달까지" 로 두고 marker 화. |
| `BrawlerPortrait` → `BrawlerAvatar` 통합 시 BattlePlayer 도메인의 brawler 정보(name, imageUrl) 가 누락된 경우 | 도메인 매퍼 확인. `imageUrl` 은 `BrawlerItem` 에 있음. fallback 은 이니셜 (현재와 동일 결과). 회귀 없음. |
| `defaultValue="battles"` 변경으로 첫 렌더 네트워크 비용 증가 | `BattleList` 는 이미 RSC initialBattles 대신 클라이언트 패칭. 측정 후 필요 시 RSC 보강은 후속 사이클. |
| 브롤러 메타 행에 ClassIcon 추가 시 모바일 폭에서 셀 깨짐 | 행 컴포넌트의 grid-template-columns 확인 후 폭 보전. 깨지면 모바일에서 ClassIcon 16→14px 다운. |
| 통계 탭 제거로 외부 링크가 `?tab=stats` 로 들어오는 경우 | 현재 URL 쿼리 동기화 안 함(shadcn Tabs 만 씀). 영향 없음. |

## 작업 산출물 (예상 7 commit)

이 spec 승인 후 별도 plan 문서에서 다음 7 task 로 분해:

1. **primitive 일반화 1** — `Trophy` mock 의존 제거, `lib/format` 으로 정합.
2. **primitive 일반화 2** — `ClassIcon` 시그니처 `BrawlerRole` 로 변경 + `roleMeta.ts` 신설.
3. **primitive 일반화 3** — `BrawlerPortrait` → `BrawlerAvatar` 통합, 사용처 2건 교체, `BrawlerPortrait` 삭제.
4. **`lib/data.ts` 정리** — 잔존 사용처(예: `TopNav` `fmtMinAgo`) 이동, 가능한 경우 파일 삭제.
5. **MobileTabBar 자산 교체** — 5 탭 매핑 적용.
6. **Player Hero 카드 통합** — `PlayerHero.tsx` 작성, `PlayerStats.tsx` 삭제, `PlayerContent.tsx` 정리, 기본 탭 = 배틀, 통계 탭 제거, 컨테이너 폭 1280.
7. **Brawler 상세 + 메타** — ClassIcon 적용, 버피 한글 매핑, LCP eager, 메타 테이블 ClassIcon 추가, 컨테이너 폭 1280.
