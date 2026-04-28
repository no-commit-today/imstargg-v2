# imstargg-frontend 이벤트 도메인 설계

작성일: 2026-04-28
대상: `imstargg-frontend`
범위: 이벤트 목록(로테이션 + 경쟁전 맵) / 상세 / 검색
선행: `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md` (Foundation), `docs/superpowers/specs/2026-04-28-frontend-brawler-domain-design.md` (Brawler)

> 본 spec은 Foundation 및 Brawler 도메인 패턴을 미러링합니다. 통계(브롤러 tier/winRate/pickRate, 페어/엔미, 메타 등)는 별도 Statistics 도메인 spec에 분리합니다.

---

## 1. 목표와 비목표

### 목표

- `app/events/page.tsx` (목록), `app/events/[id]/page.tsx` (상세)의 mock을 백엔드 API와 연동된 실제 구현으로 교체
- 현재 로테이션(rotation) 카드 그리드 + 카운트다운 표시
- 경쟁전(solo rank) 맵 목록 표시
- 이벤트 검색 (`/v1/event/search?name=`)
- 상세 페이지: 모드/맵 이름/아이콘/결과 타입 (헤더 영역만)
- `app/sitemap.ts`에 모든 이벤트 상세 경로 포함

### 비목표 (별도 spec으로 미룸)

- **이벤트별 통계**: 브롤러 tier/winRate/pickRate/starRate, 페어/엔미, 일별 추이, 메타 페이지 — Statistics 도메인
- **이 이벤트의 추천 브롤러 / 자주 쓰는 플레이어** — Statistics 도메인
- **상세 페이지 통계 placeholder**: 헤더만 두고 통계 영역은 비워둠 (Statistics spec에서 교체)
- **i18n**: 영어 UI 토글 (한국어 단일, `pickName` 폴백만)

---

## 2. 핵심 결정사항

| 결정 | 선택 | 이유 |
|---|---|---|
| 백엔드 엔드포인트 | `/v1/events/rotation`, `/v1/events/solo-rank`, `/v1/events/{id}`, `/v1/event/search?name=` | 기존 `BattleEventController` 그대로 |
| 검색 방식 | **백엔드 검색** (`/v1/event/search?name=`) | 약 100개 이벤트가 시즌별로 누적되며 화면에 보이는 ~30개를 넘어서야 의미 있음. 백엔드 검색은 모든 이벤트(rotation/solo-rank/과거)에서 매칭 가능 |
| 검색 디바운스 | 300ms | 입력당 fetch 폭주 방지 |
| 카운트다운 | client-side 1s `setInterval`, `endTime - now` | rotation 카드의 시그니처 표시 |
| 카운트다운 정확도 | 분 단위까지만 표시(예: `02:13:45`), 1초 미만은 무시 | 시각적 노이즈 최소화 |
| 상세 페이지 캐싱 | **SSG**: `generateStaticParams` (rotation + solo-rank ID 합집합) + `revalidate: 3600` | 신규 이벤트는 ISR 1h로 반영 |
| 상세 통계 영역 | **완전히 비워둠** (placeholder 카드 없음) | Statistics spec이 추가될 때 자연스럽게 채워질 자리. UX는 헤더만 단독 노출 |
| 404 갱신 트리거 | **없음** | 이벤트는 사용자 생성 데이터 아님 |
| `MessagesResponse` 처리 | `pickName` (Brawler 도메인에서 추출된 `src/domain/i18n.ts`) 재사용 | DRY |
| 모드 아이콘 | `mode.iconImageUrl` 우선, 없으면 모드명 첫 글자 그라디언트 박스 | DESIGN.md 패턴 |
| 정렬 | rotation은 `startTime` 오름차순(시작이 빠른 순), solo-rank는 백엔드 응답 순 그대로 | 백엔드 ordering 신뢰 |

---

## 3. 파일 구조

```text
app/events/
  page.tsx                              (RSC, rotation + solo-rank 동시 fetch, generateMetadata)
  _components/
    EventsListPage.tsx                  재작성 — RSC가 데이터를 props로 전달
    EventSearch.tsx                     (신규: 검색 input + 결과 드롭다운/리스트)
    RotationSection.tsx                 (신규: rotation grid)
    RotationCard.tsx                    (신규: 카운트다운 포함 카드)
    EventCountdown.tsx                  (신규: client 1s tick)
    SoloRankSection.tsx                 (신규: solo-rank 맵 리스트)
    SoloRankCard.tsx                    (신규)
  [id]/
    page.tsx                            (RSC, generateStaticParams, generateMetadata)
    error.tsx                           (신규)
    not-found.tsx                       (신규 — 갱신 트리거 없음)
    _components/
      EventContent.tsx                  (initialData hydrate)
      EventHero.tsx                     (포트레이트 = 모드 아이콘 + 맵 이름 + 결과 타입 칩)

src/api/endpoints/event.ts              (신규: eventApi.rotation/soloRank/get/search)
src/api/types.ts                        (필요 시 BattleEventResponse / RotationBattleEventResponse / BattleEventModeResponse / BattleEventResultType DTO 타입 추가 — 이미 존재하면 그대로)
src/domain/event/
  types.ts                              BattleEventMode, BattleEvent, RotationEvent
  mappers.ts                            toBattleEventMode, toBattleEvent, toRotationEvent

src/hooks/queries/queryKeys.ts          (수정: event.{all,rotation,soloRank,detail(id),search(name)})
src/hooks/queries/useRotationEvents.ts
src/hooks/queries/useSoloRankEvents.ts
src/hooks/queries/useEvent.ts
src/hooks/queries/useEventSearch.ts

app/sitemap.ts                          (수정: /events + /events/{id} 동적 추가)

tests/unit/domain/event/
  mappers.test.ts                       (신규)
tests/unit/lib/
  countdown.test.ts                     (신규: 카운트다운 포맷 순수 함수)
tests/e2e/event.spec.ts                 (신규)
```

기존 mock 컴포넌트(`EventsPage.tsx`, `EventDetailPage.tsx`, `lib/data.ts`의 `ROTATION`/`SOLO_RANKED_MAPS`/`getEvent` 등 이벤트 mock 부분)는 신규 컴포넌트군으로 대체 후 삭제.

---

## 4. API / 도메인 레이어

### 4.1 endpoint (`src/api/endpoints/event.ts`)

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import type {
  BattleEventResponse,
  RotationBattleEventResponse,
  ListResponse,
} from "@/api/types";

export const eventApi = {
  rotation: (opts?: FetchOpts) =>
    apiFetch<ListResponse<RotationBattleEventResponse>>(`/v1/events/rotation`, opts),

  soloRank: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(`/v1/events/solo-rank`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BattleEventResponse>(`/v1/events/${id}`, opts),

  search: (name: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<BattleEventResponse>>(
      `/v1/event/search?name=${encodeURIComponent(name)}`,
      opts,
    ),
};
```

### 4.2 도메인 타입 (`src/domain/event/types.ts`)

```typescript
import type { BattleEventResultType } from "@/api/types";

export interface BattleEventMode {
  id: number;
  name: string;
  iconImageUrl: string | null;
  resultType: BattleEventResultType;
}

export interface BattleEvent {
  id: number;
  mode: BattleEventMode | null;
  mapName: string;
}

export interface RotationEvent {
  event: BattleEvent;
  startTime: string;  // ISO 8601 (OffsetDateTime → string)
  endTime: string;
}
```

### 4.3 매퍼 (`src/domain/event/mappers.ts`)

```typescript
import type {
  BattleEventResponse,
  BattleEventModeResponse,
  RotationBattleEventResponse,
} from "@/api/types";
import type { BattleEvent, BattleEventMode, RotationEvent } from "@/domain/event/types";
import { pickName } from "@/domain/i18n";

export function toBattleEventMode(dto: BattleEventModeResponse): BattleEventMode {
  return {
    id: dto.id,
    name: pickName(dto.name),
    iconImageUrl: dto.iconImageUrl,
    resultType: dto.resultType,
  };
}

export function toBattleEvent(dto: BattleEventResponse): BattleEvent {
  return {
    id: dto.id,
    mode: dto.mode ? toBattleEventMode(dto.mode) : null,
    mapName: pickName(dto.mapName),
  };
}

export function toRotationEvent(dto: RotationBattleEventResponse): RotationEvent {
  return {
    event: toBattleEvent(dto.event),
    startTime: dto.startTime,
    endTime: dto.endTime,
  };
}
```

### 4.4 카운트다운 유틸 (`src/lib/countdown.ts`)

```typescript
export interface Countdown {
  hours: number;
  minutes: number;
  seconds: number;
  totalMs: number;  // 음수면 종료
  ended: boolean;
}

export function computeCountdown(endIso: string, now: number = Date.now()): Countdown {
  const end = new Date(endIso).getTime();
  const totalMs = end - now;
  const ended = totalMs <= 0;
  const safe = Math.max(totalMs, 0);
  const totalSeconds = Math.floor(safe / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return { hours, minutes, seconds, totalMs, ended };
}

export function formatCountdown(c: Countdown): string {
  if (c.ended) return "종료됨";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(c.hours)}:${pad(c.minutes)}:${pad(c.seconds)}`;
}
```

순수 함수 → TDD 대상.

---

## 5. 훅 레이어

### 5.1 query keys (수정 후)

```typescript
event: {
  all: ["event"] as const,
  rotation: () => [...queryKeys.event.all, "rotation"] as const,
  soloRank: () => [...queryKeys.event.all, "solo-rank"] as const,
  detail: (id: number | string) =>
    [...queryKeys.event.all, "detail", String(id)] as const,
  search: (name: string) =>
    [...queryKeys.event.all, "search", name] as const,
},
```

### 5.2 훅

```typescript
// useRotationEvents.ts
export function useRotationEvents(options?: { initialData?: RotationEvent[] }) {
  return useQuery({
    queryKey: queryKeys.event.rotation(),
    queryFn: ({ signal }) =>
      eventApi.rotation({ signal }).then((r) => r.content.map(toRotationEvent)),
    initialData: options?.initialData,
    staleTime: 5 * 60_000,  // 5m (rotation은 분 단위로 변하지 않으나 카운트다운 계산은 client에서)
  });
}

// useSoloRankEvents.ts — 동일 패턴, staleTime 1h

// useEvent.ts
export function useEvent(id: number | string, options?: { initialData?: BattleEvent }) {
  return useQuery({
    queryKey: queryKeys.event.detail(id),
    queryFn: ({ signal }) => eventApi.get(id, { signal }).then(toBattleEvent),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}

// useEventSearch.ts — debounced name으로 호출. enabled: name.trim().length > 0
export function useEventSearch(name: string) {
  const trimmed = name.trim();
  return useQuery({
    queryKey: queryKeys.event.search(trimmed),
    queryFn: ({ signal }) =>
      eventApi.search(trimmed, { signal }).then((r) => r.content.map(toBattleEvent)),
    enabled: trimmed.length > 0,
    staleTime: 60_000,
  });
}
```

디바운스는 `EventSearch.tsx` 컴포넌트에서 `useEffect` + `setTimeout(300ms)`로 입력값 → debouncedValue 갱신 후 훅에 전달.

---

## 6. 페이지 / 컴포넌트

### 6.1 `app/events/page.tsx`

```text
RSC:
  const [rotationDto, soloRankDto] = await Promise.all([
    eventApi.rotation({ next: { revalidate: 600 } }),
    eventApi.soloRank({ next: { revalidate: 3600 } }),
  ]);
  rotation = rotationDto.content.map(toRotationEvent);
  soloRank = soloRankDto.content.map(toBattleEvent);
  generateMetadata: title "이벤트 로테이션 | ImStarGG", canonical /events
  → <EventsListPage initialRotation={rotation} initialSoloRank={soloRank} />
```

### 6.2 `EventsListPage` (재작성)

`use client`. 받은 props로 `useRotationEvents` / `useSoloRankEvents` initialData hydrate.

UI 상단: `<EventSearch />` (검색 결과가 있을 땐 검색 결과 리스트로 본문 교체, 비어있으면 기본 두 섹션 노출)

본문 (검색 비어있을 때):
- `<RotationSection rotation={rotation} />`
- `<SoloRankSection events={soloRank} />`

본문 (검색 활성):
- `<EventSearchResults results={searchResults} loading={isPending} />` — 단일 리스트, 카드 클릭 → `/events/{id}`

### 6.3 `EventSearch`

- shadcn `Input` + `Search` 아이콘
- 300ms 디바운스
- 입력 비면 메인 섹션 복귀
- 결과 0건일 때 "결과 없음" 빈 상태

### 6.4 `RotationCard`

- shadcn `Card`
- 모드 아이콘(있으면 `<Image>`, 없으면 첫 글자 그라디언트 박스)
- 모드 이름 (h4 두께)
- 맵 이름 (h5)
- `<EventCountdown endTime={r.endTime} />` — 1s tick, "남은 시간 02:13:45". `totalMs < 60_000` 시 빨강
- 카드 클릭 → `router.push("/events/" + r.event.id)`

### 6.5 `EventCountdown`

```tsx
"use client";
const [now, setNow] = useState(() => Date.now());
useEffect(() => {
  const t = setInterval(() => setNow(Date.now()), 1000);
  return () => clearInterval(t);
}, []);
const cd = computeCountdown(endTime, now);
return <span className={cd.totalMs < 60_000 ? "text-danger" : ""}>{formatCountdown(cd)}</span>;
```

### 6.6 `SoloRankCard`

rotation과 동일한 시각 톤이지만 카운트다운 없음 (시즌 종료까지 유지). 모드 아이콘 + 모드명 + 맵명만.

### 6.7 `app/events/[id]/page.tsx`

```typescript
export async function generateStaticParams() {
  const [rotation, soloRank] = await Promise.all([
    eventApi.rotation({ next: { revalidate: 3600 } }),
    eventApi.soloRank({ next: { revalidate: 3600 } }),
  ]);
  const ids = new Set<string>();
  rotation.content.forEach((r) => ids.add(String(r.event.id)));
  soloRank.content.forEach((e) => ids.add(String(e.id)));
  return [...ids].map((id) => ({ id }));
}

export const revalidate = 3600;

export async function generateMetadata({ params }: Params): Promise<Metadata> { ... }

export default async function EventPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await eventApi.get(id, { next: { revalidate: 3600 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return <EventContent initialEvent={toBattleEvent(dto)} />;
}
```

### 6.8 상세 컴포넌트들

- `EventContent`: `useEvent(id, { initialData })`
- `EventHero`: 큰 모드 아이콘 (96px), 모드명 (h2 두께), 맵 이름 (h1), 결과 타입 칩 (`BATTLE` / `RANK`)
- 통계 영역: **추가하지 않음**. 헤더 단독 노출

### 6.9 not-found.tsx

```tsx
"이벤트를 찾을 수 없어요" + 홈/이벤트 목록으로 돌아가기 버튼.
```

갱신 트리거 없음.

---

## 7. SEO

- `/events`: title `이벤트 로테이션 | ImStarGG`, description `현재 진행 중인 이벤트 로테이션과 경쟁전 맵 정보를 확인하세요.`
- `/events/{id}`: title `{맵 이름}{ ({모드})}? | ImStarGG`, description `모드: {모드명}, 결과 타입: {타입}`
- `app/sitemap.ts`: `/events` 정적 + 빌드 시 rotation+soloRank 합집합 ID로 `/events/{id}` 추가

---

## 8. 테스트 전략

### 8.1 단위

- `tests/unit/lib/countdown.test.ts`: `computeCountdown` (남은 시간 계산, 종료 케이스, 음수), `formatCountdown` (자릿수 패딩, 종료 라벨)
- `tests/unit/domain/event/mappers.test.ts`: `toBattleEvent` (mode null 케이스, mapName 평탄화), `toBattleEventMode`, `toRotationEvent` (ISO 보존)

### 8.2 E2E (Playwright, prod API)

`tests/e2e/event.spec.ts`:
1. `/events` 진입 → rotation 카드 ≥1, solo-rank 카드 ≥1
2. 검색: `젬 그랩` 입력 → 결과 ≥1 (변경 가능. 안정적인 모드명 사용)
3. 검색 클리어 → 메인 섹션 복귀
4. rotation 카드 클릭 → `/events/{id}` 진입, 맵 이름 표시
5. SEO: `<title>`에 맵 이름 포함, canonical 1개

통합 테스트(MSW)는 비즈니스 로직이 거의 없어 생략 (단위 테스트로 충분).

---

## 9. 구현 순서

1. `eventApi` endpoint 추가
2. `src/api/types.ts`에 `BattleEventResponse` / `RotationBattleEventResponse` / `BattleEventModeResponse` / `BattleEventResultType` 등 누락된 DTO 타입 정의 (이미 있으면 skip)
3. 도메인 타입 + 매퍼 (TDD)
4. 카운트다운 유틸 (TDD)
5. queryKeys 확장 + 4개 훅
6. 목록 RSC + `EventsListPage` 재작성 + Rotation/SoloRank/Search 섹션 + 카운트다운 컴포넌트
7. 상세 RSC + `generateStaticParams` + Hero + error/not-found
8. `app/sitemap.ts`에 동적 이벤트 경로 추가
9. mock 정리 (`app/events/_components/EventsPage.tsx`, `app/events/[id]/_components/EventDetailPage.tsx`, `lib/data.ts`의 이벤트 mock)
10. E2E spec
11. 최종 typecheck/lint/build/test

각 단계 끝에 frequent commit. **`git add -A` 금지** — 작업한 파일만 stage.

---

## 10. 의존 / 위험

- **백엔드 의존**: 변경 없음. `BattleEventController`의 4개 엔드포인트 그대로 사용
- **빌드 시 백엔드 호출**: `generateStaticParams` + `sitemap.ts` 모두 빌드 타임에 prod API 호출. Brawler와 동일 조건이라 추가 위험 없음
- **신규 이벤트 추가**: ISR `revalidate: 3600`이면 1h 후 자동 반영. 즉시 반영 필요 시 빌드/배포 트리거 (별도 spec)
- **카운트다운 SSR/CSR mismatch**: 서버 렌더링 시점과 클라 hydrate 시점의 시간이 다름 → 클라에서만 시간 표시 (`useEffect`로 mount 후 첫 렌더에 시간 표시), 서버 렌더는 빈 placeholder
- **검색 API 인코딩**: `encodeURIComponent`로 한글 처리. 백엔드는 query param `name` (이미 동작 검증됨)

---

## 11. 완료 기준

- `pnpm typecheck` / `pnpm lint` / `pnpm test:run` / `pnpm build` 모두 통과
- `/events` 진입 → rotation + solo-rank 카드 표시, 카운트다운 1초 단위로 갱신
- 검색 input → 디바운스 후 백엔드 검색 결과 표시
- `/events/{id}` 진입 → 모드 아이콘/이름·맵 이름·결과 타입 칩 표시 (통계 영역 없음)
- `app/sitemap.xml`에 `/events/{id}` 다수 항목 포함
- E2E event spec 통과
- 기존 mock 컴포넌트/데이터 삭제, `pnpm exec rg "ROTATION\|SOLO_RANKED_MAPS\|getEvent\b"`는 매치 0
