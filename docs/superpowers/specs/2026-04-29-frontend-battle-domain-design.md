# Frontend Battle 도메인 설계

> 플레이어 페이지 "배틀" 탭 구현. 결과형/순위형 분기, 컴팩트 행 + 펼침, 25개씩 무한 스크롤.

작성일: 2026-04-29

---

## 1. 목표 및 범위

### 목표
플레이어 상세 페이지의 "배틀" 탭(`PlayerBattles.tsx` placeholder)을 채워 최근 배틀 기록을 op.gg 스타일로 표시한다.

### 범위
- **포함**: `/players/[tag]` 의 "배틀" 탭 안 콘텐츠. 배틀 행 컴포넌트, 펼침 팀 카드, 무한 스크롤, 모드/맵/결과 시각화.
- **제외**: 별도 라우트 (`/battles/...` 등), 배틀 상세 페이지, 배틀 검색/필터 (YAGNI), 배틀 통계 (Statistics 도메인 소관), Home 페이지 (별도 spec 예정).

### 엔드포인트
- `GET /v1/players/{tag}/battles?offset=&limit=` → `SliceResponse<BattleResponse>`

---

## 2. 도메인 모델 매핑

### 백엔드 DTO (이미 존재)
```ts
// src/api/types.ts 에 추가
type BattleMode =
  | "KNOCKOUT" | "GEM_GRAB" | "HEIST" | "HOT_ZONE" | "SIEGE" | "BOUNTY"
  | "BRAWL_BALL" | "DUELS" | "SOLO_SHOWDOWN" | "DUO_SHOWDOWN" | "WIPEOUT"
  | "VOLLEY_BRAWL" | "TROPHY_THIEVES" | "BOSS_FIGHT" | "BIG_GAME" | "BASKET_BRAWL"
  | "ROBO_RUMBLE" | "PAYLOAD" | "TAKEDOWN" | "HUNTERS" | "BOT_DROP"
  | "HOLD_THE_TROPHY" | "UNKNOWN";

type BattleType =
  | "RANKED" | "SOLO_RANKED" | "FRIENDLY" | "CHALLENGE"
  | "TOURNAMENT" | "CHAMPIONSHIP_CHALLENGE";

type BattleResult = "VICTORY" | "DEFEAT" | "DRAW";

interface BattlePlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

interface BattlePlayerResponse {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawlerResponse;
}

interface BattleResponse {
  battleTime: string;            // OffsetDateTime ISO
  event: BattleEventResponse | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;       // seconds
  starPlayerTag: string | null;
  rank: number | null;           // 순위형 1..N
  trophyChange: number | null;
  teams: BattlePlayerResponse[][];
}
```

### 화면 모델 (`src/domain/battle/types.ts`)
```ts
export type BattleMode = ...same as DTO;
export type BattleType = ...same;
export type BattleResult = "VICTORY" | "DEFEAT" | "DRAW";

export interface BattlePlayerBrawler {
  brawler: Brawler | null;       // 도메인 Brawler 재사용
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

export interface BattlePlayer {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawler;
}

export interface BattleEvent {
  id: number;
  modeName: string | null;       // i18n 으로 추출된 한국어 모드명
  modeIconUrl: string | null;
  modeResultType: "RESULT" | "RANK" | null;
  mapName: string;               // i18n 으로 추출
}

export interface Battle {
  battleTime: string;            // ISO
  event: BattleEvent | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;
  starPlayerTag: string | null;
  rank: number | null;
  trophyChange: number | null;
  teams: BattlePlayer[][];
  /** 화면 분기용 파생값 — `rank != null ? "RANK" : "RESULT"` */
  resultType: "RESULT" | "RANK";
}
```

### 매퍼 (`src/domain/battle/mappers.ts`)
- `toBattle(dto: BattleResponse): Battle`
- `event.mapName / modeName` 은 `pickName(messages)` 으로 한국어 우선 추출 (Statistics 도메인의 헬퍼 재사용)
- `resultType` 파생: `dto.rank != null ? "RANK" : "RESULT"`

---

## 3. 데이터 패칭

### API 클라이언트 (`src/api/endpoints/battle.ts`)
```ts
export const battleApi = {
  getPlayerBattles(tag: string, params: { offset: number; limit: number }) {
    return apiFetch<SliceResponse<BattleResponse>>(
      `/v1/players/${encodeURIComponent(tag)}/battles${buildQuery(params)}`
    );
  },
};
```

### 훅 (`src/hooks/queries/usePlayerBattles.ts`)
```ts
const PAGE_SIZE = 25;

export function usePlayerBattles(tag: string) {
  return useInfiniteQuery({
    queryKey: queryKeys.player.battles(tag),
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      battleApi.getPlayerBattles(tag, { offset: pageParam, limit: PAGE_SIZE }),
    getNextPageParam: (lastPage, pages) =>
      lastPage.hasNext ? pages.length * PAGE_SIZE : undefined,
    select: (data) => ({
      pages: data.pages.map((p) => p.content.map(toBattle)),
      pageParams: data.pageParams,
    }),
  });
}
```

- `queryKeys.player.battles(tag)` 키 추가
- 페이지별 25개, `hasNext` 가 false 면 더 이상 fetch 안 함
- 클라이언트 컴포넌트, RSC initialData 없음 (탭 클릭 시 첫 fetch)

---

## 4. 컴포넌트 구조

```text
app/players/[tag]/_components/
├── PlayerBattles.tsx                  // 컨테이너 (탭 콘텐츠)
└── battle/
    ├── BattleList.tsx                 // 행 리스트 + Load More
    ├── BattleRow.tsx                  // 컴팩트 행 + 펼침 토글
    ├── BattleRowHeader.tsx            // 컴팩트 영역 (모드/맵/결과/브롤러/시간)
    ├── BattleExpandedTeams.tsx        // 펼친 팀 카드 그리드
    ├── BattleTeamCard.tsx             // 팀 카드 (결과형: 좌우, 순위형: N개)
    ├── BattlePlayerRow.tsx            // 팀 카드 안 한 명
    ├── BattleResultPill.tsx           // WIN/LOSS/DRAW 또는 #N 칩
    ├── BattleModeIcon.tsx             // 모드 아이콘 (URL 기반)
    └── BattleSkeleton.tsx             // 로딩 스켈레톤 행
```

각 컴포넌트는 단일 책임을 갖고 props 로만 통신한다.

---

## 5. UI 상세 설계

### 5.1 컴팩트 행 (`BattleRow` + `BattleRowHeader`)

**레이아웃** (DESIGN.md grid-template-columns 패턴):
```
[4px result-bar] [모드 아이콘 28px] [모드명/맵명] [본인 브롤러 + 파워] [결과 칩 + 트로피변동] [상대시간 + ▾]
```

- **결과 사이드바**: 4px 폭. `VICTORY=#2BD576`, `DEFEAT=#FF3A3A`, `DRAW=#8A90A0`. 순위형은 `rank<=halfN ? success-soft : danger-soft`
- **모드/맵**: 모드명 (Pretendard 14/700) 위, 맵명 (12/500/muted) 아래
- **본인 브롤러**: BrawlerPortrait 28px + 파워 PowerPill. 본인이 어떤 브롤러로 참가했는지 (= 본인 태그 매칭으로 추출)
- **결과 / 트로피변동**:
  - 결과형: `<ResultPill>WIN/LOSS/DRAW</ResultPill>` + `+12` (옐로우/그린) or `−6` (레드)
  - 순위형: `#1` ~ `#10` 칩 (1~3위 옐로우/금, 4~순위/2 까지 그린, 그 외 레드)
- **상대시간**: "방금 전 / N분 전 / N시간 전 / N일 전" — `src/lib/relativeTime.ts` (신규). 7일 초과시 "yyyy.MM.dd"
- **▾ 토글**: 행 우측 끝. 클릭/엔터/스페이스로 펼침. 행 전체도 클릭 가능 (펼친 영역 내부 링크는 stopPropagation)

**모바일 (<640px)**:
- 모드 아이콘 24px
- 본인 브롤러 컬럼 숨김 (펼치면 보임)
- 결과 칩 + 트로피변동 한 줄로

### 5.2 펼친 팀 영역 (`BattleExpandedTeams` + `BattleTeamCard`)

- **결과형 (`resultType=RESULT`)**: `grid-template-columns: 1fr 1fr` 좌우. 본인 팀 (옐로우 틴트 보더) + 상대 팀.
- **순위형 (`resultType=RANK`)**:
  - 솔로(10팀×1): `grid-template-columns: repeat(2, 1fr)` 5×2
  - 듀오(5팀×2): `repeat(5, 1fr)` 한 줄 (모바일 1열)
  - 트리오(4팀×3): `repeat(4, 1fr)` (모바일 2열)
- 각 팀 카드 헤더: 결과형은 "WIN/LOSS/DRAW", 순위형은 "#N"
- 본인 팀은 옅은 옐로우 보더 (`border: 2px solid rgba(255,198,10,0.4)`)

### 5.3 팀 안 플레이어 행 (`BattlePlayerRow`)

```
[BrawlerPortrait 32px] [이름 (본인은 굵게+옅은 옐로우 배경) + 태그 + ⭐] [PowerPill] [트로피] [SoloRankTier]
```

- **이름 클릭** → `Link href="/players/{encodeURIComponent(tag)}"`. 행 펼침 토글과 분리하기 위해 `e.stopPropagation()`.
- **별 플레이어**: `player.tag === starPlayerTag` 면 이름 옆 ⭐ (`#FFC60A`).
- **본인 강조**: `player.tag === pageOwnerTag` 면 이름 굵게 + 행 배경 `rgba(255,198,10,0.08)`.
- **트로피**: `<Trophy>{trophies}</Trophy>` (있을 때만).
- **SoloRankTier**: 있을 때만 작은 뱃지 (랭크 컬러 매핑).

### 5.4 빈 상태 / 로딩 / 에러

- **로딩 (첫 페이지)**: `BattleSkeleton` × 25 (skel rows 64px height).
- **로딩 (다음 페이지)**: 하단에 "더 보기" 버튼이 비활성 + spinner.
- **빈 상태**: 카드 안 "최근 배틀 기록이 없어요" + 부드러운 muted 텍스트.
- **에러**: sonner 토스트 + 카드 안 "불러오기에 실패했어요" + "다시 시도" 버튼 (`refetch()`).

### 5.5 더 보기 / 종료

- 마지막 페이지 (`hasNextPage===false`) 면 버튼 자리에 "더 이상 기록이 없어요" 캡션.
- 버튼은 `<Button variant="outline">더 보기</Button>` shadcn.

---

## 6. 모드/맵 표시 규칙

- **모드 아이콘**: `event.mode.iconImageUrl` 사용. null 이면 모드명 첫 글자 fallback (다크 그레이 원형).
- **모드명**: `pickName(event.mode.name)` 한국어 우선. event 가 null 이거나 mode 가 null 이면 BattleMode enum 라벨 (예: "KNOCKOUT" → "녹아웃") — `src/domain/battle/modeLabels.ts` 매핑 (한국어 라벨 22개).
- **맵명**: `pickName(event.mapName)` 한국어 우선. event 가 null 이면 "알 수 없는 맵".
- **타입 뱃지**: `BattleType` 이 RANKED 등이면 작은 칩으로 표시 (예: 모드명 옆 "랭크"). FRIENDLY 는 "친선", null/RANKED 는 생략 (가장 흔함).

---

## 7. 라이브러리 / 유틸

- **상대시간**: `src/lib/relativeTime.ts` 신규. `Intl.RelativeTimeFormat("ko")` 사용. 7일 초과는 절대 날짜.
- **외부 라이브러리 추가 없음**. recharts 등 불필요.
- **재사용**: BrawlerPortrait, Trophy, PowerPill, ResultPill (DESIGN.md 컴포넌트), pickName (Statistics 도메인 매퍼 헬퍼), `useInfiniteQuery` (TanStack Query 이미 설치).

---

## 8. 접근성 / 키보드

- 행은 `role="button"` + `tabIndex={0}` + Enter/Space 로 펼침/접힘.
- 팀 안 플레이어 링크는 `<Link>` 기본 a 태그, 키보드 포커스 가능.
- `aria-expanded` 행에 부여.
- 결과 색은 단독 의미 전달 금지 — `result-bar` + 결과 칩 텍스트 동시 노출 (색약 사용자 배려).

---

## 9. 테스트 전략

- **단위 테스트** (`src/domain/battle/mappers.test.ts`): `toBattle` — `rank!=null` → `resultType=RANK`, `rank===null` → `resultType=RESULT`. 빈 teams, null event 처리.
- **단위 테스트** (`src/lib/relativeTime.test.ts`): 시간 분기 (방금 전, N분, N시간, N일, 7일 초과는 yyyy.MM.dd).
- **컴포넌트 테스트** (Vitest + Testing Library): `BattleRow` 펼침 토글 동작, 결과형/순위형 분기 렌더, 본인 강조, 별 플레이어 ⭐ 노출.
- **E2E** (`tests/e2e/player-battles.spec.ts`): "배틀" 탭 클릭 → 행 25개 렌더 → 첫 행 펼침 → 팀 보임 → "더 보기" 클릭 → 추가 행 append. (테스트 태그 `#88J8Q9RPV`)

---

## 10. 마이그레이션 / 롤아웃

- 기존 `PlayerBattles.tsx` placeholder 를 신규 컨테이너로 교체. 다른 페이지/탭에 영향 없음.
- 백엔드 변경 없음 (기존 `/v1/players/{tag}/battles` 그대로).
- TanStack Query key 추가만 — 기존 키와 충돌 없음.

---

## 11. 비기능 요구사항

- **성능**: 첫 25개 paint < 1.5s (P75, 캐시 미스). 펼침 < 100ms.
- **번들 영향**: 신규 컴포넌트 ~15KB, 외부 라이브러리 추가 없음.
- **i18n**: 한국어 1차. 모드/맵명은 백엔드 messages 한국어 우선 추출, fallback 영문 키.

---

## 12. Open Questions / 후속 작업

- **별 플레이어 배지의 hover 툴팁**: "이 게임의 MVP" 툴팁? — 일단 ⭐만, 나중에 설명 추가.
- **트로피 변동 +N 색**: 양수=success(green), 음수=danger(red), 0=muted. 트로피 미변동(친선전)은 숨김.
- **익명 플레이어 (#)**: 백엔드가 익명 플레이어를 어떻게 표현하는지 확인 필요. 현재 가정은 정상 태그/이름.
- **봇 매치 (예: BIG_GAME)**: 모드별 팀 구성이 일반 룰과 다를 수 있음. teams[][] 그대로 그려도 무리 없을 거라 가정.
