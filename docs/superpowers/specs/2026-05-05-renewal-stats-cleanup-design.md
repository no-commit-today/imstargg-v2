# Renewal · Player Statistics Tab · Stats Cleanup — Design

**Date**: 2026-05-05
**Topics**: 6
**Scope**: 단일 spec / 단일 plan, 독립 task group 으로 분할 가능

## Summary

| § | 토픽 | 영역 | 의존 |
|---|------|------|------|
| 1 | Renewal `renewedAt` 노출 + toast 제거 | Frontend | — |
| 2 | Club 트로피 분포 버킷 재정의 + 레이아웃 순서 | Frontend | — |
| 3 | Player 상세 통계 탭 추가 | Frontend | §4 |
| 4 | 시즌 목록 API (BrawlPass / SoloRank) | Backend | — |
| 5 | 스타플레이어/MVP/스타률 → 별 아이콘 통일 | Frontend | — |
| 6 | `stats_rank_battle.star_player_brawlstars_tag` 제거 | Backend + DB | — |

---

## §1 Renewal 개선

### 목표

- 갱신 트리거 시 toast 알림 제거 (조용한 갱신)
- 헤더에 `renewedAt` 기반 상대 시간 캡션 노출 ("최근 업데이트: 2시간 전")
- 클럽 갱신 버튼은 이미 `ClubHeader.tsx` 에 존재 — 추가 작업 없음 (hook 동작은 player 와 이미 동일)

### Hook 변경

`src/hooks/usePlayerRenewal.ts`, `src/hooks/useClubRenewal.ts`:

- 제거:
  - `import { toast } from "sonner"`
  - `toast.success("갱신 완료")` (useEffect 안)
  - `toast.info("갱신 요청됨")` (mutation onSuccess)
  - `toast.error("갱신 요청 실패")` (mutation onError)
- 반환에 `renewedAt: string | null` 추가 (이미 `RenewalResponse` 에 있음 — pass-through)

### 신규 컴포넌트 `src/components/shared/RenewalStatus.tsx`

```tsx
type Props =
  | { kind: "player"; tag: string }
  | { kind: "club"; tag: string };

export function RenewalStatus(props: Props) {
  const { renewal, progressing } = useRenewal(props);
  const text = (() => {
    if (progressing) return "최근 업데이트: 진행 중…";
    if (!renewal?.renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewal.renewedAt)}`;
  })();
  return <span className="text-sm text-[#5C6271]">{text}</span>;
}
```

(props.kind 별 hook 호출은 small wrapper 또는 두 컴포넌트로 분기 — 구현 세부는 plan 단계.)

### 배치

`PlayerHero.tsx:50` 와 `ClubHeader.tsx:60` 에서:

```tsx
<div className="flex flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-3">
  <RenewalStatus kind="player" tag={tag} />
  <RenewalButton kind="player" tag={tag} />
</div>
```

`RenewalButton` 시그니처는 그대로. NotFoundView 등 status 만 필요한 곳은 `<RenewalStatus/>` 만 사용.

### 비용 분석

`RenewalButton` 와 `RenewalStatus` 모두 같은 hook 호출 → TanStack Query 가 query key 로 dedupe → 추가 네트워크 요청 0.

---

## §2 Club 트로피 분포

### 버킷 재정의

`src/domain/club/members.ts:27` 의 `BUCKETS`:

```ts
const BUCKETS: ReadonlyArray<Omit<TrophyBucket, "count">> = [
  { label: "200K+",     min: 200000, max: null },
  { label: "180–200K",  min: 180000, max: 200000 },
  { label: "160–180K",  min: 160000, max: 180000 },
  { label: "140–160K",  min: 140000, max: 160000 },
  { label: "120–140K",  min: 120000, max: 140000 },
  { label: "100–120K",  min: 100000, max: 120000 },
  { label: "80–100K",   min: 80000,  max: 100000 },
  { label: "60–80K",    min: 60000,  max: 80000  },
  { label: "40–60K",    min: 40000,  max: 60000  },
  { label: "20–40K",    min: 20000,  max: 40000  },
  { label: "<20K",      min: 0,      max: 20000  },
];
```

(11개. 표시 순서는 큰 값 → 작은 값, 기존 동작과 일치.)

### Layout 순서 변경

`ClubContent.tsx:37`:

- DOM 순서를 `Distribution → Members` 로 바꾸기 (모바일 stack 시 Distribution 이 위에 노출됨)
- 데스크탑 grid 우측이 320px 컬럼이고 Members 가 좌측 1fr 이어야 하므로 `lg:order-*` 로 시각 순서 보정:

```tsx
<div className="grid gap-4 lg:grid-cols-[1fr_320px]">
  <ClubTrophyDistribution members={club.members} className="lg:order-2" />
  <ClubMembers members={sorted} className="lg:order-1" />
</div>
```

`ClubTrophyDistribution`, `ClubMembers` 가 `className?: string` 를 받아 root 에 합치도록 작은 확장.

---

## §3 Player 상세 통계 탭

### Tab 추가

`PlayerContent.tsx:43`:

```tsx
<TabsList ...>
  <TabsTrigger value="battles">배틀</TabsTrigger>
  <TabsTrigger value="brawlers">브롤러</TabsTrigger>
  <TabsTrigger value="statistics">통계</TabsTrigger>
</TabsList>
...
<TabsContent value="statistics">
  <PlayerStatistics tag={tag} />
</TabsContent>
```

### 컴포넌트 트리

```text
PlayerStatistics (tag)
├── PlayerStatisticsControls
│   ├── Segment [랭크전 | 경쟁전]      ← battleType
│   └── <Select>                       ← season (battleType 별 시즌 리스트)
├── PlayerBrawlerRoleSummary           ← role 7개 카드 strip
└── PlayerBrawlerStatsList             ← 브롤러별 표
```

### State

- `battleType: "RANKED" | "SOLO_RANKED"`, default `"RANKED"`
- `brawlPassSeasonNumber: number | null` (RANKED 일 때 사용)
- `soloRankSeasonKey: string | null` (SOLO_RANKED 일 때)
- 각 시즌 default = 가장 최신 시즌 (시즌 리스트 fetch 후 결정)
- URL persist 안 함 (기존 player tabs 와 동일 client-only state)

### API Hooks (`src/hooks/queries/usePlayerStatistics.ts`)

```ts
usePlayerBrawlerStatistics(tag, battleType, season)
  // RANKED → GET /v1/statistics/players/{tag}/brawler/ranked?seasonNumber={season.number}
  // SOLO_RANKED → GET /v1/statistics/players/{tag}/brawler/solo-ranked?seasonKey={season.key}

usePlayerBrawlerRoleStatistics(tag, battleType, season)
  // 같은 분기로 brawler-role/ranked / brawler-role/solo-ranked
```

두 query 는 병렬 fetch. 시즌 또는 battleType 변경 시 자동 refetch.

### 도메인 (`src/domain/statistics/player.ts` 신규)

```ts
export interface PlayerBrawlerStat {
  brawler: Brawler | null;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}
export interface PlayerBrawlerRoleStat {
  role: BrawlerRole;
  totalBattleCount: number;
  victoryCount: number;
  defeatCount: number;
  drawCount: number;
  winRate: number;
}
```

매퍼는 backend response (`PlayerBrawlerStatisticsResponse`, `PlayerBrawlerRoleStatisticsResponse`) → 위 타입으로 1:1 변환.

### UI 디테일

- **Role summary strip**: `BrawlerRole` 7개 카드 (damage / tank / marksman / assassin / support / controller / artillery), 각 카드 = ClassIcon + role 이름 + winRate% + battleCount. winRate 색상은 클래스 컬러 기반.
- **Brawler list**: 표 (`grid-cols-[40px_1fr_72px_72px_72px]`) — `BrawlerAvatar` + name + battleCount + winRate(%) + V-D-Dr 분포. 정렬 기본 winRate desc, totalBattleCount 0 인 행은 dimmed.

### 빈 상태

- `items.length === 0` → "이 시즌 통계 데이터가 없습니다."

---

## §4 시즌 목록 API

### Backend — Enum 메서드

`SoloRankSeason.kt`:

```kotlin
fun all(clock: Clock): List<SoloRankSeason> {
    val end = current(clock)
    val list = mutableListOf<SoloRankSeason>()
    var s = createFirst()
    while (s.key != end.key) {
        list += s
        s = s.next()
    }
    list += end
    return list
}
```

`BrawlPassSeason.kt`: 동일 패턴, 비교는 `s.number != end.number`.

`createFirst` 와 `next` 가 이미 클래스에 정의되어 있어 `all` 만 추가하면 됨.

`SoloRankSeason` 의 `private val number/month` 가시성 변경: `val number: Int`, `val month: Int` (public) — `BrawlPassSeason.number` 와 일관 + 응답 DTO 변환에 필요.

### Controller — `SeasonController.kt` 신규

```kotlin
@RestController
class SeasonController(private val clock: Clock) {
    @GetMapping("/v1/seasons/brawl-pass")
    fun brawlPass(): ResponseEntity<ListResponse<BrawlPassSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(BrawlPassSeason.all(clock).map(BrawlPassSeasonResponse::of)),
        )

    @GetMapping("/v1/seasons/solo-rank")
    fun soloRank(): ResponseEntity<ListResponse<SoloRankSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(SoloRankSeason.all(clock).map(SoloRankSeasonResponse::of)),
        )
}
```

(`BrawlPassSeason.all` 은 companion object 함수. `Clock` 은 기존 controller 패턴과 동일하게 주입.)

### Response DTOs

```kotlin
data class BrawlPassSeasonResponse(
    val number: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(s: BrawlPassSeason) = BrawlPassSeasonResponse(s.number, s.startAt, s.endAt)
    }
}

data class SoloRankSeasonResponse(
    val key: String,
    val number: Int,
    val month: Int,
    val startAt: OffsetDateTime,
    val endAt: OffsetDateTime,
) {
    companion object {
        fun of(s: SoloRankSeason) = SoloRankSeasonResponse(
            key = s.key,
            number = s.number,
            month = s.month,
            startAt = s.startAt,
            endAt = s.endAt,
        )
    }
}
```

> **SoloRankSeason 노출 변경**: 현재 `private val number: Int, private val month: Int` 인 두 필드를 `val number: Int, val month: Int` (public) 로 가시성 변경. `BrawlPassSeason.number` 가 이미 public `val` 이라 일관됨. `key` 는 그대로.

### Frontend

`src/api/endpoints/season.ts` 신규:

```ts
export const seasonApi = {
  brawlPass: () => apiFetch<ListResponse<BrawlPassSeasonResponse>>("/v1/seasons/brawl-pass"),
  soloRank: () => apiFetch<ListResponse<SoloRankSeasonResponse>>("/v1/seasons/solo-rank"),
};
```

`src/hooks/queries/useSeasons.ts` 신규: `useBrawlPassSeasons()`, `useSoloRankSeasons()` — `staleTime: Infinity`.

### 캐싱 (백엔드)

시즌 추가는 드물고 (`endAt` 도래 시점) 계산 비용도 미미 (~50회 next() 호출). 캐싱 불필요. 필요 시 `@Cacheable` 1시간 TTL 추가는 plan 외.

---

## §5 별 아이콘 통일

### 신규 primitive `src/components/primitives/StarIcon.tsx`

```tsx
import { Star } from "lucide-react";

interface Props {
  size?: number;
  label?: string;
  className?: string;
}

export default function StarIcon({ size = 14, label = "스타플레이어", className }: Props) {
  return (
    <Star
      size={size}
      fill="#FFC60A"
      stroke="#FFC60A"
      aria-label={label}
      className={className}
    />
  );
}
```

(`Brawl Stars Star No Shadow.webp` 도 후보였으나 헤더 셀의 작은 사이즈에서는 SVG 가 가독성 우수. 배틀 행의 Unicode `★` 도 함께 통일.)

### 교체 대상

| 파일 | 위치 | 변경 |
|------|------|------|
| `src/components/primitives/StarIcon.tsx` | new | 신규 |
| `app/_components/statistics/SortableHeader.tsx` | label prop 시그니처 | `string` → `React.ReactNode` |
| `app/brawlers/_components/BrawlerStatTable.tsx:67` | `label="스타률"` | `label={<StarIcon/>}` + `title="스타플레이어 비율"` |
| `app/events/[id]/_components/EventResultStatsTable.tsx:45` | `label="MVP"` | `label={<StarIcon/>}` + 동일 title |
| `app/brawlers/[id]/_components/DailyTrendChart.tsx:11,37` | `label: "MVP"` | tooltip 인라인은 `<StarIcon/>` (Recharts 검증 후 안 되면 chart 는 "스타플레이어" 짧은 라벨로 fallback) |
| `app/players/[tag]/_components/battle/BattlePlayerRow.tsx:54` | Unicode `★` | `<StarIcon size={12}/>` |
| `app/events/[id]/_components/EventStatsSection.tsx` | "MVP" 텍스트 사용처 | `<StarIcon/>` |

### a11y

- 헤더 셀의 별 아이콘은 `<th>` 안에서 `aria-label="스타플레이어 비율"` (StarIcon 의 `label` prop 으로 전달)
- 정렬 가능 헤더는 `SortableHeader` 가 `title` 속성으로 hover 툴팁 제공

---

## §6 `stats_rank_battle.star_player_brawlstars_tag` 제거

### 배경

- RANK 배틀 (쇼다운 등 순위형) 에는 star player 개념이 없음
- `BrawlerBattleRankStatsAggregator`, `BrawlerPairBattleRankStatsAggregator` 등 RANK 측 aggregator 가 `starPlayer` 를 사용하지 않음 (grep 검증 완료)
- `BrawlerBattleResultStatsAggregator` (RESULT) 만 `starPlayerBrawlStarsTag` 를 사용 → RESULT entity 로 옮기는 것이 도메인적으로 정확

### Backend

#### `BaseStatsBattleEntity.kt`

생성자 파라미터 `starPlayerBrawlStarsTag: String?` 제거 + `@Column(name = "star_player_brawlstars_tag")` 제거.

#### `StatsResultBattleEntity.kt`

자체 컬럼으로 흡수:

```kotlin
class StatsResultBattleEntity(
    battleKey: String,
    battleAt: OffsetDateTime,
    eventBrawlStarsId: Long,
    eventModeBrawlStarsId: Long,
    eventMap: String,
    mode: String,
    type: String,
    playerBrawlStarsTag: String,
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "result", length = 25, updatable = false, nullable = false)
    val result: String,
) : BaseStatsBattleEntity(
        battleKey = battleKey,
        battleAt = battleAt,
        eventBrawlStarsId = eventBrawlStarsId,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = eventMap,
        mode = mode,
        type = type,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
    ) { ... }
```

#### `StatsRankBattleEntity.kt`

생성자 인자에서 `starPlayerBrawlStarsTag` 제거 (super 호출도 정리).

#### 호출부

`StatsBattleAggregateApplier.kt` 에서 `StatsRankBattleEntity` 생성 시 인자 1개 빠짐. 컴파일러가 자연 발견.

### 마이그레이션

`infrastructure/db-core/src/main/resources/db/migration/V260505__drop_stats_rank_star_player.sql`:

```sql
alter table stats_rank_battle drop column if exists star_player_brawlstars_tag;
```

### Schema snapshot

`infrastructure/db-core/src/main/resources/schema.sql:287` 의 `star_player_brawlstars_tag varchar(45) null,` 라인 삭제.

### Test fixture

`infrastructure/db-core/src/testFixtures/.../BattleEntityFixture.kt` 의 rank-side fixture 호출에서 인자 제거. `StatsBattleAggregateApplierTest` 등 영향 받는 테스트의 `StatsRankBattleEntity` 생성 부분 갱신.

(grep 검증된 영향 파일 목록은 plan 단계에서 정확히 열거.)

---

## 검증 매트릭스

| § | typecheck | lint | unit test | 추가 검증 |
|---|----------|------|-----------|----------|
| 1 | ✓ | ✓ | hook test 갱신 (toast assertion 제거) | 수동 — Player/Club hero 화면 |
| 2 | ✓ | ✓ | `trophyDistribution` 단위 테스트 갱신 | 수동 — Club 페이지 narrow viewport |
| 3 | ✓ | ✓ | hook + 통계 컴포넌트 unit | 수동 — 통계 탭 진입, 시즌/타입 토글 |
| 4 | ✓ | ✓ | `SoloRankSeason.all`, `BrawlPassSeason.all` unit | API 수동 호출 |
| 5 | ✓ | ✓ | — | 수동 — 헤더 셀 hover tooltip |
| 6 | ✓ | ✓ | aggregator/applier 테스트 통과 | DB migration 적용 후 schema verify |

## Plan 단계 task group 분할 (예상)

1. **Backend cleanup §6** — DB migration 포함, 독립
2. **Backend §4** — Season API, 독립
3. **Frontend §1 §2 §5** — 병렬, 서로 독립
4. **Frontend §3** — §4 PR 머지 후 시작 (Season API 의존)

각 group 은 PR 단위로 분리 가능. plan 작성 시 task 별 구체적 file/line 명시.

## 위험 / 호환성

- §6: 동일 PR 에 코드 + DDL drop 동시 배포. 운영 DB 에 컬럼이 남아있는 상태에서 새 코드가 INSERT 시 컬럼 미사용 → 안전 (drop 이후 INSERT 도 안전). 그러나 dev/prod 배포 순서 — DB migration 먼저 → app 배포 순서 권장 (운영 절차 상 standard).
- §1: 갱신 toast 제거 후 사용자가 "갱신 됐는지" 인지 못 할 수 있음 → `RenewalStatus` 의 "진행 중…" 표시가 시각적 피드백 역할. 충분.
- §3: 시즌 select 가 비어있는 채로 첫 렌더링 → loading 상태 처리 필요 (skeleton 또는 disabled select).

## Out of scope

- 시즌 API 결과 backend 캐싱 (Caffeine/Redis)
- StatsResultBattleEntity 의 `result` enum 화 (별개 정리)
- RenewalStatus 의 polling interval 로 자동 새로고침 (현재는 hook 이 이미 polling 중)
