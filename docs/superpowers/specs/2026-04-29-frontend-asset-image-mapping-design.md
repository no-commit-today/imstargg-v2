# Asset 이미지 매핑 — Frontend Design Spec

**작성일**: 2026-04-29
**상태**: Draft → 승인 대기

## 목적

백엔드 CDN(`cdn.imstargg.com`) 의 절대 URL 을 보여주는 raw `<img>` 4 곳을 기존에 정착된 `next/image + unoptimized` 패턴으로 통일한다. ESLint `@next/next/no-img-element` 동적 케이스 경고를 제거하고, `next/image` 의 lazy loading 과 명시적 `width`/`height` 로 CLS 를 방지한다.

## 현재 상태

### 백엔드 DTO 의 자산 URL 제공

`https://core-api.imstargg.com/v1/brawlers/16000000` 응답 예시:

- `portraitImageUrl: "https://cdn.imstargg.com/brawler/portrait/16000000.webp"`
- `gadgets[].imageUrl: "https://cdn.imstargg.com/gadget/23000255.webp"`
- `gears[].imageUrl: "https://cdn.imstargg.com/gear/62000000.webp"`
- `starPowers[].imageUrl: "https://cdn.imstargg.com/starpower/...webp"`
- `hyperCharges[].imageUrl: "https://cdn.imstargg.com/hypercharge/...webp"`
- `buffies[].imageUrl: "https://cdn.imstargg.com/brawler/buffy_*/16000000.webp"`
- 이벤트 모드 아이콘: `cdn.imstargg.com/event/mode/icon/{n}.webp`

### 이미 마이그레이션된 7 개 파일 (참조 패턴)

```tsx
<Image
  src={brawler.portraitImageUrl}
  alt={brawler.name}
  width={32}
  height={32}
  unoptimized
  className="..."
/>
```

- `app/players/[tag]/_components/PlayerBrawlers.tsx`
- `app/brawlers/_components/BrawlerStatRow.tsx`
- `app/brawlers/[id]/_components/PairStatsTable.tsx`
- `app/brawlers/[id]/_components/EnemyStatsTable.tsx`
- `app/events/[id]/_components/EventResultStatsTable.tsx`
- `app/events/[id]/_components/EventRankStatsTable.tsx`
- `app/events/[id]/_components/PairRankTable.tsx`

이 패턴이 본 spec 의 표준이다.

### 마이그레이션 대상 (raw `<img>` 잔여 — CDN URL)

| # | 파일 | 라인 | URL 출처 |
|---|---|---|---|
| 1 | `src/components/primitives/BrawlerAvatar.tsx` | 47 | `imageUrl` prop |
| 2 | `app/players/[tag]/_components/battle/BattleModeIcon.tsx` | 14 | `iconUrl` prop |
| 3 | `app/brawlers/[id]/_components/BrawlerBuffies.tsx` | 22 | `b.imageUrl` |
| 4 | `app/brawlers/[id]/_components/BrawlerItemCard.tsx` | 10 | `item.imageUrl` |

## Goals (성공 기준)

1. 위 4 개 파일이 `next/image` + `unoptimized` + `width`/`height` + 의미 있는 `alt` 패턴으로 통일된다.
2. `pnpm lint` 시 동적 CDN URL 사용처의 `no-img-element` 경고가 사라진다 (정적 자산 케이스만 잔존).
3. `imageUrl`/`iconUrl` 이 `null` 인 경우 기존 fallback (BrawlerAvatar 의 그라디언트+이니셜, BattleModeIcon 의 회색 원, BrawlerBuffies 의 분기 없음, BrawlerItemCard 의 회색 박스+이니셜) 동작이 변경되지 않는다.
4. 기존 prod E2E (`statistics.spec.ts`, `player-battles.spec.ts`) 가 그대로 통과한다.
5. `pnpm test:run`, `pnpm typecheck` 가 통과한다.

## Non-goals

- 정적 자산(`/assets/*.png|webp`) 의 `next/image` 마이그레이션 (TopNav, ClassIcon, Trophy, ClubHeader, ClubMembers, HomeScreen).
- 사용자 `iconId` / 클럽 `badgeId` 의 신규 이미지 매핑 (백엔드 URL 미제공).
- `BrawlerPortrait` / `BrawlerAvatar` 통합 (책임이 분리되어 있어 둘 다 유지).
- `next.config.ts` 의 `images.remotePatterns` 등록 (`unoptimized=true` 라 불필요).
- Vercel 이미지 최적화 도입 (CDN 이 이미 webp 제공, 비용 회피).

## 설계

### 적용 패턴

```tsx
import Image from "next/image";

{url ? (
  <Image
    src={url}
    alt={meaningfulName}
    width={size}
    height={size}
    unoptimized
    className="..."
  />
) : (
  <FallbackElement />
)}
```

- **`unoptimized=true`**: CDN 이 이미 webp 제공. Vercel 변환을 거치지 않으며 `remotePatterns` 등록도 불필요. DESIGN.md 의 "`<Image unoptimized>` 로 별도 처리" 정책과 일치.
- **`width`/`height` 픽셀 명시**: 기존 컴포넌트가 받던 `size` prop 또는 고정 값을 그대로 매핑. CLS 방지.
- **`alt`**: BrawlerAvatar 는 `name`, BattleModeIcon 은 `modeLabel(mode)`, BrawlerBuffies/Card 는 `name`/`label`. 장식용이 아닌 의미 자산.
- **fallback**: 기존 분기 그대로 유지. `imageUrl` 이 `null` 인 경우의 시각 결과는 변경하지 않는다.
- **`className`**: 기존 클래스 그대로 유지 (`object-cover`, `object-contain`, 라운드, 사이즈 클래스 등).

### 파일별 변경 요약

#### 1. `BrawlerAvatar.tsx`

```tsx
// before
<img src={imageUrl} alt={name} width={size} height={size}
     className="h-full w-full object-cover" />

// after
<Image src={imageUrl} alt={name} width={size} height={size}
       unoptimized className="h-full w-full object-cover" />
```

`imageUrl` 이 `null` 일 때의 그라디언트+이니셜 fallback 은 그대로.

#### 2. `BattleModeIcon.tsx`

```tsx
// before
// eslint-disable-next-line @next/next/no-img-element
<img src={iconUrl} alt={modeLabel(mode)}
     style={{ width: size, height: size, objectFit: "contain" }} />

// after
<Image src={iconUrl} alt={modeLabel(mode)} width={size} height={size}
       unoptimized style={{ objectFit: "contain" }} />
```

`eslint-disable` 주석 제거. `iconUrl` 이 `null` 일 때의 회색 원 fallback 은 그대로.

#### 3. `BrawlerBuffies.tsx`

```tsx
// before
<img src={b.imageUrl} alt={label} width={20} height={20} className="h-5 w-5" />

// after
<Image src={b.imageUrl} alt={label} width={20} height={20}
       unoptimized className="h-5 w-5" />
```

#### 4. `BrawlerItemCard.tsx`

```tsx
// before
<img src={item.imageUrl} alt={item.name} width={48} height={48}
     className="h-full w-full object-contain" />

// after
<Image src={item.imageUrl} alt={item.name} width={48} height={48}
       unoptimized className="h-full w-full object-contain" />
```

`item.imageUrl` 이 `null` 일 때의 이니셜 fallback 은 그대로.

### 의존성 그래프

```
[Backend DTO] --absolute URL--> [domain mappers]
                                       |
                                       v
                          [Component prop: imageUrl]
                                       |
                          +------------+------------+
                          |                         |
                       null?                      url
                          |                         |
                          v                         v
                   [Fallback element]        [next/image unoptimized]
```

- 마이그레이션은 화살표의 마지막 단계만 변경한다. domain mapper, prop 시그니처, fallback 분기 모두 그대로.

## 테스팅 전략

### 단위 테스트 (Vitest + Testing Library)

`BrawlerAvatar` 의 분기 렌더 테스트를 새로 추가한다 (기존에 없음 — 가벼운 회귀 가드).

- `imageUrl` 제공 시 `<img>` 가 렌더되고 `alt` 가 `name` 이다.
- `imageUrl` 이 `null` 일 때 이미지가 없고 이니셜이 노출된다.

`BrawlerBuffies`, `BrawlerItemCard`, `BattleModeIcon` 은 도메인 의존성이 작아 별도 단위 테스트 없이 E2E + lint 로 검증한다.

### E2E (Playwright)

기존 prod E2E 가 자동으로 커버한다.

- `statistics.spec.ts` — `/brawlers/[id]` 의 PairStatsTable 등 기존 `<Image>` 케이스를 사용. 변경된 4 개 컴포넌트 중 `BrawlerBuffies`, `BrawlerItemCard` 가 `/brawlers/[id]` 페이지에서 노출되므로 동일 spec 으로 회귀 확인.
- `player-battles.spec.ts` — `BattleModeIcon` 노출.

추가 E2E 케이스는 만들지 않는다.

### 정적 검증

- `pnpm lint` — 동적 CDN 케이스의 `no-img-element` 경고 4 건이 사라져야 한다 (정적 자산 6 건만 잔존).
- `pnpm typecheck` — 통과.
- `pnpm test:run` — 새 단위 테스트 포함 통과.

## 위험과 대응

| 위험 | 대응 |
|---|---|
| `next/image` 가 `unoptimized` 라도 prod 에서 `cdn.imstargg.com` 호출이 차단됨 | 이미 7 개 파일에서 동일 패턴이 prod 에 떠 있음. 호환성 검증 완료. |
| `objectFit: contain` 등 inline style 이 `next/image` 의 wrapper 와 충돌 | `Image` 의 `style` prop 은 그대로 통과. 7 개 기존 파일과 동일 처리. |
| fallback 분기가 변경되어 `imageUrl=null` 시 깨짐 | spec 의 핵심 invariant. 분기 로직은 절대 변경하지 않는다. 단위 테스트로 가드. |
| ESLint 가 `<Image>` 도 다른 규칙으로 경고 | 현재 7 개 기존 파일에 경고 없음 — 동일 사용법이면 안전. |

## 작업 산출물

이 spec 승인 후 별도 plan 문서(`docs/superpowers/plans/2026-04-29-frontend-asset-image-mapping.md`) 에서 TDD 단위로 작업을 분해한다.
