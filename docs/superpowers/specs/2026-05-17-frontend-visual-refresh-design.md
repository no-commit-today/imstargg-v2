# imstargg-frontend Visual Refresh 설계 (Phase 1: brawlers)

- 작성일: 2026-05-17
- 1차 적용 범위: `/brawlers`, `/brawlers/[id]`
- 후속 범위(별도 plan): `/clubs`, `/players`, `/events`, `/ranking` — 본 spec 의 표/카드 유틸을 그대로 재사용

## 1. 배경 (Context)

`docs/superpowers/specs/2026-04-30-frontend-design-system-application-design.md` 와 `imstargg-frontend/DESIGN.md` 에서 정의한 디자인 시스템은 잘 정리되어 있으나, 실제 페이지의 표·카드 컴포넌트에 부분 적용되어 다음 문제가 관찰된다.

브라우저 실측(라이브 dev 서버, 1440×900)에서 확인된 사실:

| 항목 | 현재 값 | DESIGN.md 규정 | 결과 |
|---|---|---|---|
| 표 `border-radius` | 16px (Tailwind v4 `rounded-lg`) | 12px (`--radius-md`) | 모서리가 과하게 부드러워 윤곽이 흐림 |
| 표 `border-color` | `#D8DCE5` (neutral-200) | `#D8DCE5` | 페이지 bg `#F6F7FA` 와 명도차 ~4% — 가장자리가 녹아 보임 |
| 표 `box-shadow` | `none` | 시그니처 sticker shadow `0 3px 0 0 rgba(0,0,0,0.35)` | 카드가 페이지 위에 떠있지 않음 |
| 표 헤더 | `bg-[#F6F7FA]` | 액션·헤더는 명확히 대비 | 페이지 bg 와 같은 색 → 헤더가 헤더로 안 읽힘 |
| Hero 카드 | 140px 아바타 + 평면 카드 | 28×24 패딩, 큰 임팩트, sticker-lg shadow | 캐릭터 굿즈 사이트의 "주인공" 카드답지 않음 |
| 행 hover | `bg-[#F6F7FA]` 만 | `--ease-pop` 오버슈트 + lift | 게임 사이트의 통통 튀는 모션 부재 |
| `result-bar` 패턴 | 미적용 | 좌측 4px 컬러 바 — 시그니처 | 메타 표에 시그니처가 없음 |
| Class color 활용 | TierBadge 만 | 클래스/희귀도 컬러 카테고리 매핑 | hero·행에서 캐릭터 컬러를 살리지 않음 |

요약: **"DESIGN.md 의 시그니처(sticker shadow, result-bar, ease-pop, 캐릭터 컬러 매핑)가 표/카드 일부에만 적용되어 있다."** 사용자가 호소한 "흐릿함"·"역동성 부족"은 시그니처 미적용의 복합 결과다.

본 작업은 **DESIGN.md 토큰을 정합화**(A) 하면서, 게임 사이트다운 임팩트를 위해 **Hero 격상 + 표 시그니처 강화 + 모션 도입**(B) 을 추가한다(이른바 B안). 다크 풀 리뉴얼(C안)은 정체성을 폐기하므로 채택하지 않는다.

## 2. 비목표 (Non-Goals)

- 다크 모드 전환
- 폰트 패밀리·디자인 토큰의 신규 추가(기존 토큰 활용만)
- 신규 페이지·기능 추가
- 클럽/플레이어/이벤트/랭킹 페이지의 동시 적용 — 후속 plan 으로 분리. 본 spec 의 유틸이 동일하게 재사용되도록 설계.
- 백엔드 API·도메인 모델 변경
- 라이트하우스 외 추가 성능 최적화

## 3. 변경 포인트 개요

### 3.1 표 가장자리 — 흐릿함 제거
모든 데이터 표에 공통 적용할 신규 유틸을 `app/styles/app.css` 에 추가한다.

```css
.data-table {
  background: #FFFFFF;
  border: 1px solid var(--color-neutral-300);   /* #B4B9C7 — 한 단계 진하게 */
  border-radius: var(--radius-md);              /* 12px */
  box-shadow:
    0 1px 0 0 rgba(20,22,27,0.06),
    0 4px 12px rgba(20,22,27,0.05);
  overflow: hidden;
}

.data-table-head {
  display: grid;                                 /* 자식 grid-template-columns 유지 */
  background: var(--color-neutral-900);          /* #14161B */
  color: #FFFFFF;
  border-bottom: 2px solid var(--color-brand-yellow);
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.data-table-head [data-sortable]:hover { color: var(--color-brand-yellow); }

.data-row {
  display: grid;
  position: relative;
  border-bottom: 1px solid var(--color-neutral-100);
  padding: 8px 12px 8px 16px;                    /* 좌측 4px 여유는 row-rail 자리 */
  transition: background 160ms var(--ease-pop), transform 160ms var(--ease-pop);
}
.data-row:last-child { border-bottom: 0; }
.data-row:hover {
  background: #FFFBEB;                           /* 옐로우 틴트 8% */
  transform: translateX(2px);
}
.data-row::before {                              /* result-bar */
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--row-rail, var(--color-neutral-200));
}
.data-row[data-rail='win']  { --row-rail: var(--color-accent-green); }
.data-row[data-rail='loss'] { --row-rail: var(--color-accent-red); }
.data-row[data-rail='neutral'] { --row-rail: var(--color-neutral-300); }
.data-row[data-rail='gold']  { --row-rail: var(--tier-gold); }
.data-row[data-rail='silver']{ --row-rail: var(--tier-silver); }
.data-row[data-rail='bronze']{ --row-rail: var(--tier-bronze); }

.win-gauge {
  display: inline-flex; align-items: center; gap: 6px;
}
.win-gauge-track {
  width: 28px; height: 4px;
  border-radius: 999px;
  background: var(--color-neutral-100);
  overflow: hidden;
}
.win-gauge-fill { height: 100%; background: var(--color-accent-green); }
.win-gauge-fill[data-tone='loss'] { background: var(--color-accent-red); }
.win-gauge-fill[data-tone='neutral'] { background: var(--color-neutral-400); }
```

DESIGN.md 에 이미 정의된 토큰만 사용한다. 새 토큰 도입 없음.

### 3.2 모션 — DESIGN.md 의 `--ease-pop` 실제 활성화

- 모든 행·버튼·티어배지 hover/포커스 이즈를 `var(--ease-pop)` 로 통일
- TierBadge hover: `transform: rotate(-4deg) scale(1.08)` (180ms ease-pop) — 캐릭터 스티커가 살짝 들리는 느낌
- Sort 화살표: 클릭/토글 시 180° flip (200ms)
- 표 초기 mount: stagger fade-in (12개 행까지, row 당 30ms 지연) — `data-row` 에 `animation: row-in 240ms var(--ease-pop) both;` + nth-child 지연. 12개 초과는 즉시 표시(가독성/성능 균형).

```css
@keyframes row-in {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}
```

### 3.3 BrawlerHero — 캐릭터 굿즈 카드로 격상

레이아웃:
```
[히어로 카드] height auto(데스크탑 ~280px), border-radius 18px,
              border: 2px solid var(--color-neutral-900),
              box-shadow: var(--shadow-sticker-lg)
├ 배경 layer 1: linear-gradient(135deg, classColor.c1, classColor.c2) — 좌측 100%
├ 배경 layer 2 (오른쪽 40%): 흰색 패널 (decorative diagonal cut 으로 분할)
├ 좌측 panel:
│   ├ 아바타 220px (rarity-frame 4px inset + outline 3px #14161B + shadow-sticker)
│   └ 이름 (KoBrawl Gothic 60px display) 우측/하단 정렬
├ 우측 panel:
│   ├ ROLE pill (class color bg + ClassIcon)
│   ├ RARITY pill (rarity color bg + sticker shadow)
│   └ (후속) 핵심 스탯 chip line — 본 spec 에서는 위치만 확보, 데이터 연결은 향후
└ 모바일: 세로 스택, 아바타 160px, 이름 36px
```

`BrawlerHero.tsx` 의 현 구현(`flex flex-col items-center gap-4 rounded-lg border bg-card p-6`)을 위 구조로 교체. shadcn `<Badge>` 대신 DESIGN.md 의 `.pill` 시스템 사용(`.pill.dark`, `.pill yellow`, rarity 컬러 인라인 스타일).

클래스 컬러 매핑은 기존 `--class-*` 토큰을 그대로 사용. 그라디언트의 두 색은 `(c1, c2) = (classColor, classColor 명도 -15%)` 로 단순화하거나, 또는 `brawler.role` 에 따라 사전 정의한 페어를 쓴다. 본 spec 에서는 후자 — DESIGN.md 의 BrawlerPortrait 가 이미 `--bp-c1/c2` 페어를 정의해두었으므로 그 페어를 hero 에도 재활용한다.

### 3.4 표 구조 변경

#### `BrawlerStatTable`
- 외곽: `.data-table` 적용 (3.1)
- 헤더 스트립: `.data-table-head` (다크 잉크 + 옐로우 2px 하단)
- 각 행: `.data-row` + `data-rail` 속성. 매핑:
  - 1~3위(브롤러): `gold`/`silver`/`bronze` (rank 기반)
  - 그 외: tier 기반 — S→`win`, A/B→`neutral`, C/D→`loss`
- 브롤러 아바타: 32px → 36px (모바일 32px 유지). `BrawlerListHeader` 의 검색/필터는 `.data-table` 외곽 안 상단에 통합 — 별도 카드 중첩 제거.

#### `PairStatsTable`, `EnemyStatsTable`
- 외곽: `.data-table`
- 헤더 스트립: `.data-table-head` + 좌측에 아이콘(`🤝 조합` / `⚔️ 카운터` — 영문 emoji 가 아니라 ClassIcon 비슷한 SVG 글리프) + micro caption (`최소 픽률 N%`)
- 각 행 `data-rail`:
  - **조합**: 승률 ≥55% → `win`, 50~55% → `neutral`, <50% → `loss`
  - **카운터**: 승률 ≥55% → `loss`(나에게 위협), ≤45% → `win`(좋음 — 즉, "이 카운터 브롤러를 만나면 내가 이긴다"는 뜻에서 win), 그 외 `neutral`
- 승률 셀: `.win-gauge` 동반 (PercentCell 에 `gauge?: boolean` props 추가)
- 모바일에서는 gauge 숨김(공간 부족) — `hidden sm:inline-flex`

### 3.5 글자 / 컴포넌트 사이징

| 위치 | 현재 | 변경 |
|---|---|---|
| `BrawlerHero h1` | text-3xl (30px) | KoBrawl Gothic 60px / 모바일 36px / tracking -0.02em |
| 표 헤더 | text-[10px] / sm:text-[11px] | 12px / 700 / tracking 0.06em / 영문 라벨 uppercase (한글 라벨은 유지) |
| 표 행 본문 | text-[11px] / sm:text-sm(14px) | 13px(모바일) / 14px(데스크탑) — 가독성 ↑ |
| `BrawlerStatsSection h2` ("통계") | text-lg (18px) | 24px + 좌측 3px 옐로우 액센트 바 (`.section-title`) |
| 카드 타이틀 (조합/카운터) | text-sm font-bold | 14px 그대로, 다크 헤더 안 흰 텍스트로 |
| 표 내 브롤러 아바타 | 28~32px | 32~36px (모바일 ~ 데스크탑) |
| TierBadge | h-7 w-7 (28px) | 그대로(28px) — 단 sticker shadow + hover 모션 추가 |

### 3.6 컴포넌트 위치 조정

- **`BrawlerStatsSection`**: `통계` h2 + `tier 기준` caption + `StatRangeSelector` 를 한 줄로 정렬, 시각 그룹화. 헤더 영역에 좌측 옐로우 액센트.
- **`BrawlerListHeader`** (검색/필터): 표 외부 별도 카드 → 표 카드 내부 상단으로 흡수. 카드 중첩 깊이 1단계 감소.
- **조합/카운터 표**: `lg:grid-cols-2` 유지 (정보 밀도). 각 표 헤더에 아이콘 + caption.
- **`DailyTrendChart`**: 통계 섹션 안에서 카드 padding 통일(`p-4`), 차트 자체는 본 spec 변경 대상 아님.

### 3.7 적용 파일

신규/수정 대상:

- `imstargg-frontend/app/styles/app.css` — `.data-table`, `.data-table-head`, `.data-row`, `.win-gauge`, `.section-title`, row-in keyframe 추가
- `imstargg-frontend/app/brawlers/_components/BrawlerStatTable.tsx` — 외곽/헤더/행 유틸 교체, rank 기반 rail
- `imstargg-frontend/app/brawlers/_components/BrawlerStatRow.tsx` — rail 속성, 아바타 사이즈, 텍스트 사이즈
- `imstargg-frontend/app/brawlers/_components/BrawlerListHeader.tsx` — 표 카드 내부로 흡수되는 레이아웃 변형 옵션
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx` — 굿즈 카드로 격상
- `imstargg-frontend/app/brawlers/[id]/_components/PairStatsTable.tsx` — 외곽/헤더/행/rail/gauge
- `imstargg-frontend/app/brawlers/[id]/_components/EnemyStatsTable.tsx` — 같은 처리 + rail 의미 반전
- `imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx` — 섹션 헤더 정렬 + 옐로우 액센트
- `imstargg-frontend/app/_components/statistics/StatNumberCell.tsx` — `PercentCell` 에 `gauge?: boolean` props 추가, 게이지 렌더링
- `imstargg-frontend/app/_components/statistics/TierBadge.tsx` — sticker shadow 는 이미 있음. hover transition 만 보강(컴포넌트 내부에서 `transition` 추가).

### 3.8 후속 plan 으로 미루는 작업

후속 plan 에서 동일 `.data-table` / `.data-row` 를 다음에 적용 가능하도록 본 spec 의 CSS 유틸은 의도적으로 "domain-agnostic" 으로 둔다.

- `/clubs/[tag]` 내 멤버 표 등
- `/players/[tag]` 내 브롤러 표 / 매치 히스토리 행
- `/events/*` 내 브롤러·메타 표
- `/ranking/*` 표

각 페이지의 도메인-특화 헤더(예: 클럽 hero, 플레이어 hero)는 본 spec 의 BrawlerHero 격상 패턴을 참고해 각자의 plan 에서 처리.

## 4. 접근성 (Accessibility)

- result-bar 는 색만이 아니라 `aria-label`("승률 우위" 등)로도 전달 — `.data-row` 의 `data-rail` 변환과 함께 `aria-describedby` 또는 시각 텍스트 보강.
- 옐로우 틴트 호버 배경(`#FFFBEB`) 위의 다크 텍스트(`#14161B`) — 명도 대비 충분(>14:1).
- 다크 헤더 위 화이트 텍스트(`#FFFFFF` on `#14161B`) — 충분(>15:1).
- `prefers-reduced-motion: reduce` 시 stagger fade-in 과 hover translateX 비활성. CSS 에서 `@media (prefers-reduced-motion)` 가드.

## 5. 테스트 / 검증

- **수동 시각 검증 (필수)**: 1차 검증 페이지 `/brawlers`, `/brawlers/16000000(쉘리)`, `/brawlers/16000001(콜트)` — 데스크탑 1440 / 모바일 375 두 폭에서 스크린샷 비교 및 hover/sort 인터랙션 확인.
- **Lighthouse 라이트 체크**: 1차 페이지 2개 — Performance/Accessibility 점수가 작업 전 대비 -5 이상 떨어지지 않는지 확인 (절대값 목표는 두지 않음).
- **단위 테스트**: 신규 prop 인 `PercentCell { gauge }` 가 켜졌을 때 렌더 보장. `gauge` 의 채움 폭이 `value` 와 일치하는지 단순 스냅샷.
- **빌드/타입체크/Lint**: `pnpm typecheck && pnpm lint && pnpm test:run` — 기존 통과 기준 유지.
- **E2E (선택)**: 본 작업은 시각적 변경이라 prod API 호출이 필요한 Playwright E2E 신규 작성은 하지 않는다. 기존 E2E가 새 DOM 구조에서 셀렉터 깨지지 않는지만 확인.

## 6. 롤백 전략

순수 시각 변경이므로 PR 단위 revert 로 즉시 롤백 가능. 백엔드/도메인 영향 없음. `app.css` 의 신규 유틸은 다른 페이지에서 아직 사용하지 않으므로 revert 시 부수 효과 없음.

## 7. 열린 결정 사항

- **Hero 우측 패널의 핵심 스탯 chip 데이터 소스**: 현재 `Brawler` 도메인 타입은 base stat(피해량/체력)을 포함하지 않음. 본 spec 에서는 chip line 의 "위치만 확보"하고 데이터 연결은 후속 plan 또는 별도 spec(브롤러 base stat API 신규)에서 처리.
- **`BrawlerListHeader` 통합 위치**: 표 카드 내부 상단 vs 표 카드 외부 sticky bar. 본 spec 은 전자(내부 통합)로 결정. 모바일에서 차지하는 공간을 고려한 결정.

## 8. 시각적 검증 절차 (구현 plan 에서 참조)

1. `pnpm dev` 로 dev 서버 기동, 백엔드 로컬 prod 프로파일 사용 (사용자 환경에서 이미 가동 중).
2. Playwright MCP 로 `/brawlers`, `/brawlers/16000000`, `/brawlers/16000001` 의 1440·375 스크린샷 캡처 — 작업 전/후 비교용.
3. hover 인터랙션은 `mcp__playwright__browser_hover` 로 한 행씩 확인.
4. `prefers-reduced-motion` 분기는 Chrome DevTools emulate 로 검증.
