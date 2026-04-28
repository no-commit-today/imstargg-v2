# imstargg-frontend 브롤러 도메인 설계

작성일: 2026-04-28
대상: `imstargg-frontend`
범위: 브롤러 목록 / 상세 (정적 데이터 + 아이템) / 이름 검색
선행: `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md` (Foundation)

> 본 spec은 Foundation 패턴(레이어 분리, RSC + initialData, 표준 SEO, 단위 테스트는 순수 함수만)을 브롤러 도메인에 미러링합니다. 통계(tier/winRate/pickRate 등)는 별도 Statistics 도메인 spec 범위로 명시 분리합니다.

---

## 1. 목표와 비목표

### 목표

- `app/brawlers/page.tsx` (목록), `app/brawlers/[id]/page.tsx` (상세)의 mock을 백엔드 API와 연동된 실제 구현으로 교체
- 정적 브롤러 데이터(이름, 역할, 희귀도, 가젯, 스타파워, 기어, 하이퍼차지, 버피, 포트레이트 이미지) 표시
- 이름/역할/희귀도 클라이언트 사이드 필터 + 검색
- 상세 페이지는 `generateStaticParams`로 SSG
- `app/sitemap.ts`에 모든 브롤러 상세 경로 포함

### 비목표 (별도 spec으로 미룸)

- **통계**: tier(S/A/B/C/D), winRate, pickRate, starRate, 일별 추이, 페어/엔미 분석, 아이템 보유율 — Statistics 도메인
- **이 브롤러를 자주 쓰는 플레이어 / 클럽 / 이벤트 추천** — Statistics 도메인
- **i18n**: 영어 UI 토글 (현재 한국어 단일, 도메인 데이터의 `name.ko ?? name.en` 폴백만 처리)

---

## 2. 핵심 결정사항

| 결정 | 선택 | 이유 |
|---|---|---|
| 검색 방식 | **클라이언트 사이드 substring 필터** (백엔드 `/v1/brawler/search?name=` 미사용) | 약 100개 정적 리스트, 클라 필터가 단순/빠르고 추가 fetch 불필요 |
| 정렬 | 이름 가나다순 (`localeCompare("ko")`) | 통계 기반 정렬은 Statistics 도메인 |
| 상세 페이지 캐싱 | **SSG**: `generateStaticParams` + `revalidate: 86400` | 정적 데이터, 거의 변하지 않음 |
| 404 갱신 트리거 | **없음** | 브롤러는 사용자 생성 데이터 아님. 누락된 ID는 단순 not-found |
| `MessagesResponse` 처리 | 도메인 매퍼에서 `pickName` 함수로 `ko ?? en ?? first ?? "(unknown)"` | 컴포넌트가 매번 분기하지 않게 평탄화 |
| `pickName` 위치 | `src/domain/i18n.ts`로 추출, player 매퍼와 brawler 매퍼가 공용화 | DRY (현재 player에만 존재, 중복 회피) |
| 이미지 fallback | `portraitImageUrl === null` → 기존 `BrawlerPortrait` 그라디언트 + 첫 글자 | DESIGN.md에 정의된 패턴 |
| 아이템 카드 fallback | `imageUrl === null` → 첫 글자 아바타 | 같은 원칙 |

---

## 3. 파일 구조

```text
app/brawlers/
  page.tsx                              (RSC, fetches /v1/brawlers, generateMetadata)
  _components/
    BrawlersListPage.tsx                재작성 — RSC가 데이터 props로 전달
    BrawlerCard.tsx                     (신규)
    BrawlerFilters.tsx                  (신규: 역할 탭 + 희귀도 select + 검색 input)
  [id]/
    page.tsx                            (RSC, generateStaticParams, generateMetadata)
    error.tsx                           (신규)
    not-found.tsx                       (신규 — 검색 트리거 없음)
    _components/
      BrawlerContent.tsx                (initialData hydrate)
      BrawlerHero.tsx                   (포트레이트 + 이름 + 역할/희귀도 칩)
      BrawlerItems.tsx                  (4 섹션: 가젯/스타파워/기어/하이퍼차지)
      BrawlerItemCard.tsx               (단일 아이템 카드)
      BrawlerBuffies.tsx                (buffies[] 표시)

src/api/endpoints/brawler.ts            (신규: brawlerApi.list / get)
src/domain/i18n.ts                      (신규: pickName 추출)
src/domain/brawler/
  types.ts                              Brawler, BrawlerItem
  mappers.ts                            toBrawler, toBrawlerItem
  filters.ts                            filterBrawlers, sortByName

src/hooks/queries/queryKeys.ts          (수정: brawler.all / list / detail(id))
src/hooks/queries/useBrawlers.ts        (목록)
src/hooks/queries/useBrawler.ts         (상세)

src/domain/player/mappers.ts            (수정: 로컬 pickName 제거 → src/domain/i18n.ts 사용)

app/sitemap.ts                          (수정: /brawlers + /brawlers/{id} 동적 추가)

tests/unit/domain/
  i18n.test.ts                          (신규)
  brawler/
    mappers.test.ts                     (신규)
    filters.test.ts                     (신규)
tests/e2e/brawler.spec.ts               (신규)
```

기존 mock `BrawlersListPage.tsx`, `BrawlerDetailPage.tsx`는 신규 컴포넌트군으로 대체 후 삭제.

---

## 4. API / 도메인 레이어

### 4.1 endpoint (`src/api/endpoints/brawler.ts`)

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerResponse>(`/v1/brawlers/${id}`, opts),
};
```

### 4.2 도메인 타입 (`src/domain/brawler/types.ts`)

```typescript
import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
}

export interface Brawler {
  id: number;
  name: string;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
  gadgets: BrawlerItem[];
  gears: BrawlerItem[];
  starPowers: BrawlerItem[];
  hyperCharges: BrawlerItem[];
  buffies: BuffyResponse[];
}
```

### 4.3 i18n 유틸 (`src/domain/i18n.ts`)

```typescript
import type { MessagesResponse } from "@/api/types";

export function pickName(messages: MessagesResponse | undefined | null): string {
  if (!messages) return "(unknown)";
  return messages.ko ?? messages.en ?? Object.values(messages)[0] ?? "(unknown)";
}
```

기존 `src/domain/player/mappers.ts`의 로컬 `pickName`을 이 모듈로 교체. (Foundation 작업 시 player 매퍼에서 만들었지만 이번에 brawler에서도 같은 로직이 필요해 공용화.)

### 4.4 매퍼 (`src/domain/brawler/mappers.ts`)

```typescript
import type {
  BrawlerResponse, GadgetResponse, GearResponse,
  StarPowerResponse, HyperChargeResponse,
} from "@/api/types";
import type { Brawler, BrawlerItem } from "@/domain/brawler/types";
import { pickName } from "@/domain/i18n";

type ItemDto =
  | GadgetResponse | GearResponse | StarPowerResponse | HyperChargeResponse;

export function toBrawlerItem(dto: ItemDto): BrawlerItem {
  return { id: dto.id, name: pickName(dto.name), imageUrl: dto.imageUrl };
}

export function toBrawler(dto: BrawlerResponse): Brawler {
  return {
    id: dto.id,
    name: pickName(dto.name),
    rarity: dto.rarity,
    role: dto.role,
    portraitImageUrl: dto.portraitImageUrl,
    gadgets: dto.gadgets.map(toBrawlerItem),
    gears: dto.gears.map(toBrawlerItem),
    starPowers: dto.starPowers.map(toBrawlerItem),
    hyperCharges: dto.hyperCharges.map(toBrawlerItem),
    buffies: dto.buffies,
  };
}
```

### 4.5 필터 / 정렬 (`src/domain/brawler/filters.ts`)

순수 함수, TDD 대상:

```typescript
import type { Brawler } from "@/domain/brawler/types";
import type { BrawlerRole, BrawlerRarity } from "@/api/types";

export interface BrawlerFilterOptions {
  role?: BrawlerRole;
  rarity?: BrawlerRarity;
  search?: string;
}

export function filterBrawlers(
  list: Brawler[],
  opts: BrawlerFilterOptions,
): Brawler[];

export function sortByName(list: Brawler[]): Brawler[];
```

검색은 `name.toLocaleLowerCase("ko")`에 substring 매치(공백 trim).

---

## 5. 훅 레이어

### 5.1 query keys (수정 후)

```typescript
brawler: {
  all: ["brawler"] as const,
  list: () => [...queryKeys.brawler.all, "list"] as const,
  detail: (id: number | string) =>
    [...queryKeys.brawler.all, "detail", String(id)] as const,
},
```

### 5.2 훅

```typescript
// useBrawlers.ts
export function useBrawlers(options?: { initialData?: Brawler[] }) {
  return useQuery({
    queryKey: queryKeys.brawler.list(),
    queryFn: ({ signal }) =>
      brawlerApi.list({ signal }).then((r) => r.content.map(toBrawler)),
    initialData: options?.initialData,
    staleTime: 60 * 60_000, // 1h
  });
}

// useBrawler.ts
export function useBrawler(id: number | string, options?: { initialData?: Brawler }) {
  return useQuery({
    queryKey: queryKeys.brawler.detail(id),
    queryFn: ({ signal }) => brawlerApi.get(id, { signal }).then(toBrawler),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

---

## 6. 페이지 / 컴포넌트

### 6.1 `app/brawlers/page.tsx`

```text
RSC:
  list = await brawlerApi.list({ next: { revalidate: 3600 } })
  brawlers = list.content.map(toBrawler).then(sortByName)
  generateMetadata: title "브롤러 도감 | ImStarGG", canonical /brawlers
  → <BrawlersListPage initialBrawlers={brawlers} />
```

### 6.2 `BrawlersListPage` (재작성)

`use client`. 받은 `initialBrawlers`로 `useBrawlers({ initialData })`.

UI:
- 헤더: "브롤러" 제목 + 총 개수 표시
- `<BrawlerFilters />`: 역할 탭(전체 + 7 역할), 희귀도 select, 검색 input
- 결과: `<BrawlerCard />` 그리드 (`grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5`, gap-3)
- 검색 결과 0건 시 "결과 없음" 빈 상태

### 6.3 `BrawlerFilters`

- 역할 탭: 가로 스크롤 가능, 모바일 친화
- 희귀도 select: shadcn `Select`
- 검색: shadcn `Input` + 좌측 `Search` 아이콘
- 상태는 부모에서 관리 (controlled), props로 받아 dispatch

부모(`BrawlersListPage`)에서:
```ts
const [role, setRole] = useState<BrawlerRole | "ALL">("ALL");
const [rarity, setRarity] = useState<BrawlerRarity | "ALL">("ALL");
const [search, setSearch] = useState("");
const filtered = useMemo(() =>
  filterBrawlers(brawlers, {
    role: role === "ALL" ? undefined : role,
    rarity: rarity === "ALL" ? undefined : rarity,
    search: search.trim() || undefined,
  }),
  [brawlers, role, rarity, search],
);
```

### 6.4 `BrawlerCard`

- shadcn `Card`
- `<BrawlerPortrait id={b.id} name={b.name} imageUrl={b.portraitImageUrl} size={...} />` (기존 컴포넌트 재사용; portraitImageUrl 우선, 없으면 그라디언트)
- 이름 (truncate)
- 역할 칩 (`<ClassIcon />`)
- 희귀도 칩 (희귀도 컬러 매핑)
- 카드 클릭 → `router.push("/brawlers/" + b.id)`

### 6.5 `app/brawlers/[id]/page.tsx`

```typescript
export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

export const revalidate = 86400;

export async function generateMetadata({ params }: Params): Promise<Metadata> { ... }

export default async function BrawlerPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return <BrawlerContent initialBrawler={toBrawler(dto)} />;
}
```

### 6.6 상세 컴포넌트들

- `BrawlerContent`: `useBrawler(id, { initialData })`로 hydrate
- `BrawlerHero`: 포트레이트 큰 사이즈 (160px), 이름 h1, 역할 칩, 희귀도 칩
- `BrawlerItems`: 4 섹션을 카드로. 각 섹션 header에 카운트 ("가젯 (2)") + grid로 `BrawlerItemCard` 나열
- `BrawlerItemCard`: `imageUrl`이 있으면 `<img>` (`width=64 height=64`), 없으면 기존 첫 글자 아바타
- `BrawlerBuffies`: `buffies[]` 단순 가로 나열 (`type` 라벨 + imageUrl)

빈 섹션은 숨김(0개라면 카드 자체 미표시).

### 6.7 not-found.tsx (정적)

기존 player의 `not-found.tsx` 미러:
```tsx
"브롤러를 찾을 수 없어요" + 홈/브롤러 도감으로 돌아가기 버튼.
```

갱신 트리거 없음 (브롤러는 사용자 생성 데이터 아님).

---

## 7. SEO

- `/brawlers`: title `브롤러 도감 | ImStarGG`, description `약 N개 브롤러의 정보, 가젯, 스타파워, 기어, 하이퍼차지를 확인하세요.`
- `/brawlers/{id}`: title `{name} | ImStarGG`, description `{role} · {rarity} · 가젯 N · 스타파워 N · 기어 N · 하이퍼차지 N`
- canonical 항상 포함
- `app/sitemap.ts`: `/brawlers` 정적 + 빌드 시 `brawlerApi.list()` 호출하여 `/brawlers/{id}` 동적 추가

---

## 8. 테스트 전략

### 8.1 단위

- `tests/unit/domain/i18n.test.ts`: `pickName` (`ko` 우선, `en` 폴백, 빈 객체, undefined)
- `tests/unit/domain/brawler/mappers.test.ts`: `toBrawler`, `toBrawlerItem` (이름 평탄화, 아이템 매핑, null portraitImageUrl 보존)
- `tests/unit/domain/brawler/filters.test.ts`: `filterBrawlers` (역할/희귀도/검색 단독 + 조합), `sortByName` (가나다순)

### 8.2 E2E (Playwright, prod API)

`tests/e2e/brawler.spec.ts`:
1. `/brawlers` 진입 → 카드 다수 표시 (≥ 50)
2. 검색 입력 → 결과 좁혀짐 (예: "셸리" → 1개 카드)
3. 역할 탭 클릭 → 결과 좁혀짐
4. 카드 클릭 → `/brawlers/{id}` 이동, 이름 표시, 가젯/스타파워 등 표시
5. SEO: `<title>`에 브롤러 이름 포함, canonical 1개

통합 테스트(MSW)는 굳이 필요 없음 — 비즈니스 로직 거의 없고, 필터는 단위 테스트로 충분, 페치는 E2E로 검증.

---

## 9. 구현 순서

플랜에서 확정:

1. `src/domain/i18n.ts` 생성 (TDD), `player/mappers.ts`의 로컬 pickName 교체
2. `brawlerApi` endpoint
3. 도메인 타입 + 매퍼 (TDD)
4. 필터/정렬 (TDD)
5. queryKeys 확장 + `useBrawlers` / `useBrawler`
6. 목록 RSC + `BrawlersListPage` 재작성 + `BrawlerFilters` + `BrawlerCard`
7. 상세 RSC + `generateStaticParams` + error/not-found
8. 상세 컴포넌트군 (Content/Hero/Items/ItemCard/Buffies)
9. `app/sitemap.ts`에 동적 브롤러 경로 추가
10. mock 컴포넌트 삭제(`BrawlerDetailPage.tsx`, 기존 mock `BrawlersListPage.tsx`)
11. E2E spec
12. 최종 typecheck/lint/build/test

각 단계 끝에 frequent commit.

---

## 10. 의존 / 위험

- **백엔드 의존**: 변경 없음. CORS는 Foundation에서 이미 설정됨
- **빌드 시 백엔드 호출**: `generateStaticParams` + `sitemap.ts` 모두 빌드 타임에 `/v1/brawlers` 호출 — 백엔드가 빌드 시 가용해야 함. CI 빌드 환경에서 prod API에 도달 가능해야 함 (이미 player 도메인에서도 RSC가 prod API 호출 중이라 동일 조건)
- **새 브롤러 추가 시**: ISR `revalidate: 86400`이면 24h 후 자동 갱신. 즉시 반영이 필요하면 빌드/배포 트리거 필요 (별도 spec)

---

## 11. 완료 기준

- `pnpm typecheck` / `pnpm lint` / `pnpm test:run` / `pnpm build` 모두 통과
- `/brawlers` 진입 → 다수 카드 표시, 필터/검색 동작
- `/brawlers/{id}` 진입 → 이름·역할·희귀도·아이템 4섹션·버피 표시
- `app/sitemap.xml`에 `/brawlers/{id}` 다수 항목 포함
- E2E brawler spec 통과
- 기존 mock 컴포넌트 삭제, `pnpm exec rg "BRAWLER_META\|BrawlerDetailPage"`는 매치 0
