# Frontend Ranking Domain Design

**Date**: 2026-04-28
**Status**: Approved
**Scope**: imstargg-frontend `/ranking` 도메인 (플레이어/브롤러/클럽 트로피 랭킹)

## Goals

- 백엔드 ranking API 3종을 프론트엔드 페이지로 노출
- country 6개(GLOBAL/KOREA/CHINA/JAPAN/GERMANY/UNITED_STATES) 전환 지원
- Events/Brawlers 도메인과 일관된 패턴(RSC + ISR, 단방향 의존, 매퍼 레이어)

## Non-Goals

- 페이지네이션/무한스크롤 (백엔드가 고정 길이 리스트 반환)
- 클라이언트 사이드 정렬/필터/즐겨찾기 country
- 자동 refetch / 폴링 (랭킹은 정적 성격)
- 랭킹 변동 그래프, 시계열 분석

## Backend Endpoints (확정)

| Method | Path | Query | Response |
|---|---|---|---|
| GET | `/v1/ranking/players` | `country` (필수) | `ListResponse<PlayerRankingResponse>` |
| GET | `/v1/ranking/brawlers/{brawlerId}` | `country` (필수) | `ListResponse<BrawlerRankingResponse>` |
| GET | `/v1/ranking/clubs` | `country` (필수) | `ListResponse<ClubRankingResponse>` |

### Response 스키마 (DTO)

```ts
type PlayerRankingResponse = {
  country: string;              // 'global' | 'kr' | ...
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
};

type BrawlerRankingResponse = {
  country: string;
  brawlerId: number;
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
};

type ClubRankingResponse = {
  country: string;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
};

type RankingPlayerResponse = {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;             // 빈 문자열 가능
};
```

## Decisions

| 결정 | 선택 | 이유 |
|---|---|---|
| URL 구조 | 분리 라우트 + 브롤러 인덱스 | Events 패턴 일치, SEO 친화 |
| Country 전달 | query param `?country=xx` | 라우트 단순, 공유 가능, soft navigation |
| 데이터 패칭 | RSC + `revalidate: 600` | 정적 성격에 적합, 일관된 패턴 |
| Country UI | shadcn Select | 모바일 친화 |
| 클라이언트 hydration | 미사용 | 인터랙션 없음, 단순화 |

## File Structure

```
app/ranking/
├── page.tsx                          # 인덱스: 3가지 랭킹 진입 카드
├── players/page.tsx                  # 플레이어 랭킹 (?country=)
├── clubs/page.tsx                    # 클럽 랭킹 (?country=)
├── brawlers/
│   ├── page.tsx                      # 브롤러 그리드 (선택 화면)
│   └── [brawlerId]/page.tsx          # 특정 브롤러 랭킹 (?country=)
└── _components/
    ├── CountrySelector.tsx
    ├── RankBadge.tsx                 # #N + rankChange 표시
    ├── PlayerRankingList.tsx
    ├── ClubRankingList.tsx
    └── BrawlerRankingList.tsx

src/api/endpoints/ranking.ts
src/api/types.ts                      # 위 4개 Response 타입 추가
src/domain/ranking/
├── types.ts                          # 도메인 모델 + Country 타입
├── country.ts                        # COUNTRY_CODES, COUNTRY_LABELS, parseCountry
└── mappers.ts                        # toPlayerRanking, toBrawlerRanking, toClubRanking
src/hooks/queries/queryKeys.ts        # ranking 키 추가 (향후 useQuery 필요 시)
```

## API/도메인 레이어

### `src/api/endpoints/ranking.ts`

```ts
export const rankingApi = {
  players: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerRankingResponse>>(
      `/v1/ranking/players?country=${encodeURIComponent(country)}`,
      opts,
    ),
  brawlers: (brawlerId: number, country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerRankingResponse>>(
      `/v1/ranking/brawlers/${brawlerId}?country=${encodeURIComponent(country)}`,
      opts,
    ),
  clubs: (country: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<ClubRankingResponse>>(
      `/v1/ranking/clubs?country=${encodeURIComponent(country)}`,
      opts,
    ),
};
```

### `src/domain/ranking/country.ts`

```ts
export const COUNTRY_CODES = ['global', 'kr', 'cn', 'jp', 'de', 'us'] as const;
export type Country = typeof COUNTRY_CODES[number];

export const COUNTRY_LABELS: Record<Country, string> = {
  global: '글로벌',
  kr: '한국',
  cn: '중국',
  jp: '일본',
  de: '독일',
  us: '미국',
};

// 잘못된 값/누락 시 'global' fallback
export function parseCountry(value: unknown): Country { ... }
```

### `src/domain/ranking/types.ts`

```ts
export type RankingPlayer = {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;
};

export type PlayerRanking = {
  country: Country;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
};

export type BrawlerRanking = {
  country: Country;
  brawlerId: number;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
};

export type ClubRanking = {
  country: Country;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
};
```

### `src/domain/ranking/mappers.ts`

DTO → 도메인 모델로 변환. `country` 필드는 `parseCountry`로 검증.

## 페이지 동작

### `/ranking` (인덱스)

- 정적 페이지. 3개 카드(플레이어/브롤러/클럽)로 진입.
- 카드 클릭 → 각 랭킹 페이지(`?country=global` 기본).
- 브롤러 카드 → `/ranking/brawlers` 그리드.

### `/ranking/players?country=xx`

- RSC: `searchParams.country` → `parseCountry` → `rankingApi.players(country)` (revalidate: 600).
- 헤더: 제목 + `CountrySelector` (현재 country 표시, Link로 이동).
- 본문: `PlayerRankingList` — `RankBadge` + 플레이어 아이콘 + 이름(nameColor) + 클럽명 + 트로피.
- 행 클릭 → `/players/{tag}` 이동.
- 빈 배열 → "랭킹 데이터가 없습니다" 메시지.

### `/ranking/clubs?country=xx`

- 위와 동일 패턴.
- 행: `RankBadge` + 클럽 배지 + 클럽명 + 멤버수 + 트로피.
- 행 클릭 → `/clubs/{tag}` 이동.

### `/ranking/brawlers`

- 브롤러 그리드 (Brawlers 도메인의 `BrawlerGrid` 재사용 검토).
- 클릭 → `/ranking/brawlers/{brawlerId}?country=global`.

### `/ranking/brawlers/[brawlerId]?country=xx`

- 헤더: 브롤러 이름/포트레이트 + `CountrySelector`.
- 본문: 플레이어 랭킹과 유사 행 (브롤러는 헤더에 있으므로 행에 미표시).
- 잘못된 brawlerId → `notFound()`.

## CountrySelector 동작

- 현재 pathname 유지 + `?country={code}` 변경하여 `<Link>` 생성.
- shadcn `Select` 사용 — 옵션 6개 (`COUNTRY_LABELS` 표시).
- 클라이언트 컴포넌트 (`'use client'`) — `useRouter`/`usePathname` 사용 (App Router 16 API).

## RankBadge 동작

- 입력: `rank: number`, `rankChange: number | null`.
- 표시:
  - `rank` (예: `#1`, `#42`)
  - `rankChange > 0` → 🟢 `▲{N}`
  - `rankChange < 0` → 🔴 `▼{abs(N)}`
  - `rankChange === 0` → `─` (변동 없음)
  - `rankChange === null` → 빈칸 (신규/데이터 없음)

## SEO

- 각 ranking 페이지 `generateMetadata({ searchParams })`:
  - `/ranking/players` → "{country 라벨} 플레이어 랭킹 | imstargg"
  - `/ranking/clubs` → "{country 라벨} 클럽 랭킹 | imstargg"
  - `/ranking/brawlers/{brawlerId}` → "{브롤러명} {country 라벨} 랭킹 | imstargg"
- `/ranking`, `/ranking/brawlers`는 정적 메타.
- sitemap에 `/ranking`, `/ranking/players`, `/ranking/clubs`, `/ranking/brawlers` 추가 (country 변형은 미포함 — query param이므로).

## 에러 처리

- API 에러 → RSC throw → 가장 가까운 `error.tsx` (Foundation 패턴).
- 잘못된 country query → `parseCountry`가 `'global'`로 fallback (URL은 그대로, 데이터는 GLOBAL).
- 잘못된 brawlerId → `notFound()`.

## 테스트

### 단위 (Vitest)
- `domain/ranking/country.test.ts`: `parseCountry` 검증 (정상/대소문자/잘못된 값/null).
- `domain/ranking/mappers.test.ts`: 각 매퍼 (DTO → 도메인, country fallback).

### 컴포넌트 (Vitest + Testing Library)
- `RankBadge.test.tsx`: rankChange 4가지 케이스 표시.
- `CountrySelector.test.tsx`: 옵션 표시, Link href 검증.

### E2E (Playwright, prod API)
- `/ranking/players?country=global` 진입 → 1위 플레이어 표시 확인.
- CountrySelector로 `kr` 전환 → URL 변경 + 새 데이터 로드.
- `/ranking/brawlers` → 브롤러 클릭 → 랭킹 페이지 진입.
- `/ranking/clubs?country=kr` 행 클릭 → `/clubs/{tag}` 이동.

## 구현 순서

1. **API/도메인 레이어**: types, country, mappers, endpoints
2. **공통 컴포넌트**: RankBadge, CountrySelector
3. **인덱스 페이지**: `/ranking`
4. **플레이어 랭킹**: `/ranking/players`
5. **클럽 랭킹**: `/ranking/clubs`
6. **브롤러 그리드**: `/ranking/brawlers`
7. **브롤러 랭킹**: `/ranking/brawlers/[brawlerId]`
8. **sitemap, generateMetadata**
9. **E2E 테스트**

## 위험 요소

- **brawlerId 유효성**: 백엔드가 잘못된 brawlerId에 대해 빈 배열을 반환할 수 있음 — 빈 배열일 때 "데이터 없음" 표시로 대응.
- **clubName 빈 문자열**: 무클럽 플레이어 처리 — 행에서 conditional 렌더.
- **nameColor 색상**: 헥스 코드 형식 가정 — 잘못된 값일 경우 기본 색상.

## 완료 기준

- 5개 라우트(`/ranking`, `/players`, `/clubs`, `/brawlers`, `/brawlers/[id]`) 동작
- 6개 country 전환 동작
- 단위/컴포넌트 테스트 통과
- E2E 시나리오 통과
- sitemap 등록
- typecheck/lint 통과
