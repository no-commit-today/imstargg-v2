# 결과형 통계 픽률 정의 정정

## 배경

현재 `BrawlerResultStatistics.create()` 는 픽률을 다음과 같이 계산한다.

```kotlin
val totalBattleCount = counts.sumOf { it.resultCount.totalCount }   // 모든 브롤러 슬롯 합
val pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount)
// = brawler slot count / total slot count
```

이는 사실상 **슬롯 점유율**(전체 슬롯 중 해당 브롤러가 차지한 비율)이며, 모든 브롤러 픽률의 합이 1.0 이 된다. "픽률"이라는 용어가 일반적으로 의미하는 **"한 게임에서 이 브롤러를 볼 확률"** 과 다르다.

`BrawlerPairResultStatistics`, `BrawlerEnemyResultStatistics` 도 동일한 잘못된 분모를 사용한다.

## 목표

결과형 통계(Result / Pair / Enemy)의 픽률 정의를 다음으로 정정한다.

> **픽률 = 해당 브롤러가 등장한 슬롯 수 / 게임 수**

- 분자: 현재 집계 그대로 (한 게임 양팀 동시 픽이면 2 카운트)
- 분모: 게임 수
- 3v3 모드에서 모든 브롤러 픽률 합 ≈ 6.0

## 비목표

- **Rank 통계**(`BrawlerRankStatistics` 등) 는 이번 변경 대상이 아니다. 자세한 사유는 [§ Rank 통계 한계](#rank-통계-한계) 참조.
- 배치 집계 로직 변경 없음. 통계 엔티티 스키마 변경 없음.

## 게임수 산출 공식

게임수를 알려면 한 게임당 슬롯 수(이하 `slotsPerGame`)가 필요하다. 이는 모드의 **팀별 인원 구성** 으로부터 통계 종류별로 다음과 같이 도출된다.

`playerCounts = [n₁, n₂, …, n_k]` (각 팀 인원, 예: 3v3 → `[3,3]`, Solo Showdown → `[1]*10`, 비대칭 → `[1,5]`)

| 통계 | `slotsPerGame` | 예시 (3v3) |
|---|---|---|
| Brawler Result | `Σ nᵢ`                       | 6  |
| Pair Result    | `Σ nᵢ(nᵢ−1)` (방향성 페어)  | 12 |
| Enemy Result   | `(Σ nᵢ)² − Σ nᵢ²` (방향성 매치업) | 18 |

(공식 근거는 각 Aggregator 의 집계 로직과 일치 — `BrawlerBattleResultStatsAggregator`, `BrawlerPairBattleResultStatsAggregator`, `BrawlerEnemyBattleResultStatsAggregator`.)

각 통계 조회에서:

```text
eventSlotSum_e     = Σ totalCount over all brawlers within event e
eventBattleCount_e = eventSlotSum_e / slotsPerGame(playerCounts_e)
totalBattleCount   = Σ_e eventBattleCount_e        (eventId 단일이면 단일 항)
pickRate_b         = (Σ_e brawler_b's totalCount) / totalBattleCount
```

## 설계

### 1. `BattleEventMode` 에 `playerCounts` 추가

#### 1.1 도메인 (`core-api`)

[BattleEventMode.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/event/BattleEventMode.kt)

```kotlin
data class BattleEventMode(
    val id: BrawlStarsId,
    val name: Messages,
    val iconImageUrl: String?,
    val resultType: BattleEventResultType,
    val playerCounts: List<Int>,   // NEW. 각 팀의 인원 수 리스트. 예: [3,3], [1]*10, [1,5]
)
```

#### 1.2 엔티티 (`db-core`)

[BattleEventModeEntity.kt](imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/event/BattleEventModeEntity.kt)

```kotlin
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "player_counts", nullable = false, columnDefinition = "jsonb")
var playerCounts: List<Int> = playerCounts
    protected set

fun changePlayerCounts(newCounts: List<Int>) {
    require(newCounts.isNotEmpty()) { "playerCounts must not be empty" }
    require(newCounts.all { it >= 1 }) { "every team size must be >= 1" }
    this.playerCounts = newCounts
}
```

#### 1.3 컬럼 형식: `jsonb`

선택 근거:
- 타입 구조(정수 배열)가 형식 자체로 보존됨 — CSV 파싱·검증 코드 불필요
- Hibernate 6+ (Spring Boot 3/4) 의 `@JdbcTypeCode(SqlTypes.JSON)` 가 `List<Int>` ↔ `jsonb` 매핑을 Jackson 통해 native 지원 — 별도 `AttributeConverter` 작성 불필요
- 향후 다른 모드 메타데이터(예: 게임 모드 옵션, 비대칭 팀 규칙)를 같은 컬럼에 확장하기 용이
- admin UI/디버깅에서 `[3,3]`, `[1,1,1,1,1,1,1,1,1,1]` 가독성 충분
- Postgres `int[]` 은 JPA 매핑이 UserType 필요로 비용 대비 이점 없음

#### 1.4 직렬화

- Hibernate `@JdbcTypeCode(SqlTypes.JSON)` 가 Jackson `ObjectMapper` 로 직렬화/역직렬화
- 도메인 → 엔티티 변환부에서 `playerCounts: List<Int>` 그대로 전달
- 빈 리스트는 도메인 검증(`require(newCounts.isNotEmpty())`)으로 차단

### 2. DB 마이그레이션

[V260519__add_player_counts_to_battle_event_mode.sql](imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260519__add_player_counts_to_battle_event_mode.sql)

```sql
alter table battle_event_mode
  add column if not exists player_counts jsonb not null default '[3, 3]'::jsonb;
```

- 모든 기존 모드를 일괄 `[3, 3]` 으로 백필 (브롤스타즈에서 절대다수가 3v3)
- admin 이 비표준 모드(Showdown 류, 5v5, Duels 등)만 골라 후속 수정
- `schema.sql` 동일 반영

### 3. Admin API

`core-admin` 모듈:

- `PATCH /battle-event-modes/{id}/player-counts`
- Request: `{ "playerCounts": [3, 3] }`
- 검증: 비어있지 않음, 모든 원소 ≥ 1
- 응답: 갱신된 모드

캐시 무효화는 수행하지 않는다. 로컬 Caffeine TTL 만료까지 대기.

### 4. 통계 조회 로직 — 조회 시점 동적 환산

#### 4.1 통계 엔티티 무변경 (원칙)

`BrawlerBattleResultStatsEntity` 및 Pair/Enemy 엔티티에 컬럼을 추가하지 않는다. 재집계 회피.

#### 4.2 Repository — event 단위 보존

기존 `findBrawlerResultCounts()` 는 브롤러 단위로만 합산해 event 정보가 소실된다. event 단위로 보존하는 메서드로 교체.

[BattleResultStatisticsRepository.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt)

```kotlin
fun findBrawlerEventResultCounts(
    eventId: BrawlStarsId?,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
): List<BrawlerEventResultCount>
```

- JPA 쿼리는 `GROUP BY event_brawlstars_id, brawler_brawlstars_id` 로 변경
- Pair/Enemy 도 동일하게 event 보존 변형 추가

신규 도메인 record:

```kotlin
data class BrawlerEventResultCount(
    val eventId: BrawlStarsId,
    val brawlerId: BrawlStarsId,
    val resultCount: ResultCount,
    val starPlayerCount: Long,
)
// Pair/Enemy 도 유사하게 BrawlerPairEventResultCount, BrawlerEnemyEventResultCount
```

#### 4.3 `SlotsPerGame` 도메인 헬퍼

[SlotsPerGame.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/SlotsPerGame.kt) — 신규.

```kotlin
object SlotsPerGame {
    fun forResult(playerCounts: List<Int>): Int =
        playerCounts.sum()

    fun forPair(playerCounts: List<Int>): Int =
        playerCounts.sumOf { it * (it - 1) }

    fun forEnemy(playerCounts: List<Int>): Int {
        val total = playerCounts.sum()
        return total * total - playerCounts.sumOf { it * it }
    }
}
```

#### 4.4 Service 레이어 — mode 조회 합류

[BattleResultStatisticsService.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsService.kt)

```kotlin
fun getBrawlerResultStatistics(...): BrawlerResultStatistics {
    val rows = resultRepository.findBrawlerEventResultCounts(...)
    val eventIds = rows.map { it.eventId }.distinct()
    val slotsPerGameByEvent: Map<BrawlStarsId, Int> =
        battleEventService.findModesByEventIds(eventIds)
            .mapValues { (_, mode) -> SlotsPerGame.forResult(mode.playerCounts) }
    return BrawlerResultStatistics.create(rows, slotsPerGameByEvent)
}
```

- `battleEventService.findModesByEventIds(eventIds)` 는 신규 메서드 (event → mode lookup, 캐시 활용)
- Pair/Enemy Service 도 동일 패턴, `SlotsPerGame.forPair/forEnemy` 사용

#### 4.5 도메인 모델 변경

[BrawlerResultStatistics.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt)

```kotlin
data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,        // 의미 정정: 슬롯 합 → 전투(게임) 수
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: BrawlerTierScore,
)

data class BrawlerResultStatistics(
    val totalBattleCount: Long,        // 의미 정정: 슬롯 합 → 전투(게임) 수
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(
            rows: List<BrawlerEventResultCount>,
            slotsPerGameByEvent: Map<BrawlStarsId, Int>,
        ): BrawlerResultStatistics {
            val totalBattleCount: Long =
                rows.groupBy { it.eventId }
                    .map { (eventId, eventRows) ->
                        val eventSlotSum = eventRows.sumOf { it.resultCount.totalCount }
                        val slotsPerGame = slotsPerGameByEvent[eventId]
                            ?: error("playerCounts missing for event=$eventId")
                        eventSlotSum / slotsPerGame
                    }.sum()

            // 아이템의 totalBattleCount 는 "이 브롤러가 등장한 게임 수의 추정치"가 아니라
            // 응답상의 "통계 표본 크기"로서 분모와 동일한 totalBattleCount 를 노출한다.
            // (한 게임에 동일 브롤러가 다중 등장하는 케이스에서 '등장 게임 수' 정의가 모호해지므로)
            val items = rows.groupBy { it.brawlerId }.map { (brawlerId, brawlerRows) ->
                val brawlerSlotSum = brawlerRows.sumOf { it.resultCount.totalCount }
                val resultCountSum = brawlerRows.map { it.resultCount }.reduce(ResultCount::merge)
                val starPlayerCountSum = brawlerRows.sumOf { it.starPlayerCount }
                BrawlerResultStatisticsItem(
                    brawlerId = brawlerId,
                    totalBattleCount = totalBattleCount,
                    pickRate = StatsRate.pickRate(brawlerSlotSum, totalBattleCount),
                    winRate = resultCountSum.winRate,
                    starPlayerRate = StatsRate.starPlayerRate(starPlayerCountSum, brawlerSlotSum),
                    tierScore = tierScores[brawlerId] ?: BrawlerTierScore(50),
                )
            }
            return BrawlerResultStatistics(
                totalBattleCount = totalBattleCount,
                items = items.sortedByDescending { it.tierScore.value },
            )
        }
    }
}
```

> ⚠️ Item 의 `totalBattleCount` 의미는 본 디자인에서 **전체 분모(=조회 범위 게임 수)** 를 표시하는 것으로 정한다. "이 브롤러가 등장한 게임 수"를 별도로 보여주고 싶다면, 한 게임에 동일 브롤러가 다중 등장하는 케이스(쇼다운에서 양팀 동일 브롤러)에서 정의가 모호해지므로 별도 필드(`appearanceBattleCount` 등)로 추가하는 것을 권한다.

`StatsRate.pickRate` 시그니처는 유지하되 의미만 정정 — 함수 본체는 단순 나눗셈이라 변화 없음. KDoc 으로 "분모 = totalBattleCount" 명시.

#### 4.6 Pair / Enemy 통계

동일 패턴 적용:
- `BrawlerPairEventResultCount`, `BrawlerEnemyEventResultCount` 신규 도메인
- `BattleResultStatisticsRepository.findBrawlerPairEventResultCounts(...)`, `findBrawlerEnemyEventResultCounts(...)`
- `BrawlerPairResultStatistics.create(rows, slotsPerGameByEvent)` — 내부에서 `SlotsPerGame.forPair`
- `BrawlerEnemyResultStatistics.create(rows, slotsPerGameByEvent)` — 내부에서 `SlotsPerGame.forEnemy`
- 기존 `totalElementCount` 의미는 보존(별개 카운트 — 페어/매치업 row 수)

### 5. Response 필드

[BrawlerResultStatisticsResponse.kt](imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlerResultStatisticsResponse.kt) 외 Pair/Enemy 응답:

- 필드명 `totalBattleCount` 그대로 유지. **의미만** "슬롯 합" → "전투(게임) 수" 로 정정.
- 프론트엔드(`imstargg-frontend`) 타입 변경 불필요. 다만 라벨/툴팁/포맷이 "n 전투" 의미로 노출되고 있다면 의미 변화에 맞게 검토 필요 (값 자체의 단위가 달라지므로 — 예전엔 슬롯 합, 이제는 게임 수).

### 6. KDoc — 중요 히스토리 보존

#### 6.1 `BrawlerBattleResultStatsEntity`

```kotlin
/**
 * 결과형(승/패/무) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **양 팀 모든 슬롯** 이 카운트된다. 3v3 이면 한 게임당 6 슬롯이
 * 다수 row 에 분산 기록된다 (자기 팀 결과 + 적군 팀 반대 결과).
 *
 * ## 게임 수 도출
 * 이 엔티티는 슬롯 단위로 누적되지만, `BattleEventMode.playerCounts` 가 알려진다면
 * 게임 수를 정확히 도출할 수 있다:
 *
 *     eventSlotSum  = Σ totalCount over all brawlers in event
 *     totalBattleCount     = eventSlotSum / playerCounts.sum()
 *
 * 픽률(= 브롤러 슬롯 수 / 게임 수) 계산이 이 관계에 의존한다.
 * `BrawlerBattleRankStatsEntity` 와의 결정적 차이는 이 엔티티는 적군까지 모두 집계한다는 점.
 */
```

#### 6.2 `BrawlerBattleRankStatsEntity`

```kotlin
/**
 * 순위형(1위~N위) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **본인 팀 슬롯만** 카운트된다 (순위형 배틀에는 적군 개념이 없음).
 * 예: Solo Showdown 10팀 1게임은 통계 entity 상 단 1 슬롯만 기록된다.
 *
 * ## 게임 수 정확 계산 불가
 * Result 통계와 달리 한 게임의 **일부 슬롯만** 통계에 들어가므로,
 * `BattleEventMode.playerCounts` 만으로는 게임 수를 도출할 수 없다.
 * (한 게임에 본인 팀 인원이 항상 일정하다고 가정하더라도, 어느 팀의 데이터가
 * 수집되었는지를 알 수 없다.)
 *
 * 따라서 결과형 통계(`BrawlerResultStatistics` 등)에 도입된
 * "게임 수 분모 픽률" 정의를 이 엔티티에 그대로 옮길 수 없다.
 * Rank 통계용 픽률을 도입하려면 별도의 게임 수 집계 메커니즘이 필요하다.
 */
```

(Pair/Enemy 의 Result/Rank 엔티티도 동일 패턴으로 KDoc 추가)

#### 6.3 `BattleEventMode` 도메인 / `BattleEventModeEntity`

`playerCounts` 필드 KDoc:

```kotlin
/**
 * 각 팀의 인원 수 리스트. 결과형 통계의 픽률 계산에서 게임 수 도출에 사용된다.
 *
 * 예시:
 *  - 3v3: [3, 3]
 *  - Solo Showdown: [1]*10
 *  - 비대칭(가상): [1, 5]
 *  - 듀얼: [1, 1]
 *
 * Rank 모드(=`BattleEventResultType.RANK`) 에서도 의미는 동일하지만,
 * Rank 통계는 통계 entity 자체가 본인 팀만 집계하므로 게임 수 환산에 사용할 수 없다
 * (`BrawlerBattleRankStatsEntity` KDoc 참조).
 */
```

### 7. `imstargg-backend/AGENTS.md` 컨벤션 추가

기존 "Code Organization" 섹션에 한 줄 추가:

> **도메인 히스토리는 KDoc 으로 보존**: 엔티티/도메인 클래스가 갖는 비자명한 집계 규칙·정합성 제약·"왜 이렇게 안 하는지" 같은 결정은 클래스/메서드 KDoc 으로 남긴다. 향후 유사 변경을 시도하는 사람이 코드만 읽고도 의도를 알 수 있어야 한다. (예: `BrawlerBattleRankStatsEntity` 가 자기 팀만 집계하는 이유)

## Rank 통계 한계

위 § 6.2 KDoc 으로 영구히 명시한다. 요지:

- Result 통계 entity: 한 게임의 양 팀 전체가 들어감 → 슬롯 수 ÷ slotsPerGame = 정확한 게임 수
- Rank 통계 entity: 본인 팀만 들어감 → 게임 수 환산 불가
- 향후 "Rank 통계에도 픽률 도입" 요구가 오면, 별도의 게임 수 집계가 선행돼야 함

이번 변경은 `BrawlerRankStatistics` 의 도메인/엔티티/Aggregator/Service 그 무엇도 건드리지 않는다.

## 변경 범위 요약

| 모듈 | 파일 | 변경 |
|---|---|---|
| db-core | `BattleEventModeEntity.kt` | `playerCounts` jsonb 컬럼 추가, KDoc |
| db-core | `schema.sql`, `V260519__add_player_counts_to_battle_event_mode.sql` | 신규 jsonb 컬럼 |
| db-core | `BrawlerBattleResultStatsEntity.kt`, `BrawlerBattleRankStatsEntity.kt`, Pair/Enemy 엔티티 | KDoc 만 추가 |
| db-core | testFixture `BattleEventModeEntityFixture` | `withPlayerCounts(...)` |
| core-api | `BattleEventMode.kt` (도메인) | `playerCounts` 필드, KDoc |
| core-api | `SlotsPerGame.kt` | 신규 |
| core-api | `BattleResultStatisticsRepository.kt` | event 단위 보존 메서드 |
| core-api | `BattleResultStatisticsService.kt` | mode lookup 합류 |
| core-api | `BattleEventService` (또는 동등) | `findModesByEventIds(...)` |
| core-api | `BrawlerEventResultCount.kt`, `BrawlerPairEventResultCount.kt`, `BrawlerEnemyEventResultCount.kt` | 신규 |
| core-api | `BrawlerResultStatistics.kt`, `BrawlerPairResultStatistics.kt`, `BrawlerEnemyResultStatistics.kt` | `create()` 시그니처 변경, `totalBattleCount` 의미 정정 + KDoc |
| core-api | `StatsRate.kt` | KDoc 만 (분모 의미) |
| core-api | `BrawlerResultStatisticsResponse.kt`, Pair/Enemy 응답 | 필드명 유지, 의미 변경에 따른 KDoc/주석 |
| core-api | `StatisticsQueryComposer.kt` | 필요 시 호출부만 정리 |
| core-admin | `BattleEventModeController.kt`, request/response | `playerCounts` patch 엔드포인트 |
| imstargg-backend | `AGENTS.md` | KDoc 컨벤션 한 줄 |
| imstargg-frontend | 통계 페이지 라벨/표시 | 타입 무변경, 의미 변화에 따라 라벨 검토 |

배치(`core-batch`) 무변경. Rank 통계 무변경 (KDoc 추가 외).

## 테스트 전략

- `BattleEventModeEntity` jsonb round-trip 테스트 (`DbCoreContextTest`로 save/find 시 `List<Int>` 보존 확인, 빈 리스트 검증)
- `SlotsPerGame` 단위 테스트
  - 3v3 → result 6, pair 12, enemy 18
  - Solo SD `[1]*10` → result 10, pair 0, enemy 90
  - 비대칭 `[1,5]` → result 6, pair 20, enemy 10
- `BrawlerResultStatistics.create` 단위 테스트
  - 단일 이벤트 정상
  - 다중 이벤트 + 모드 혼합 시 픽률 정확
  - `slotsPerGameByEvent` 누락 시 에러
- Repository `findBrawlerEventResultCounts` `@DataJpaTest` 로 GROUP BY 동작 확인
- `BattleEventModeEntity` 마이그레이션 멱등성 확인
- `imstargg-frontend` 의 응답 파싱 갱신 후 타입 체크

## 미해결 / 결정 필요 항목

1. **Item 의 `totalBattleCount` 정의** — 본 디자인에서는 전체 분모(=조회 범위 게임 수)를 노출. 추후 "브롤러별 등장 게임 수" 가 필요하면 별도 필드(`appearanceBattleCount` 등)로 추가 가능.
2. **`battleEventService.findModesByEventIds(...)` 위치** — 기존 EventRepositoryWithCache 와 연결할지, BattleEventModeRepository 측에 둘지. 구현 단계에서 캐시 키와 함께 결정.
