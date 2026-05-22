# Statistics Repository 리팩터링 디자인

- 날짜: 2026-05-21
- 대상: `imstargg-backend/core/core-api` 의 statistics 도메인
- 영향 범위: 백엔드 내부 구조 (API 응답 / 프론트엔드 영향 없음)

## 1. 목표와 동기

현재 `BattleResultStatisticsService` 는 `dateRange` 정규화 외에 두 가지 도메인 책임을 떠안고 있다.

- `slotsPerGameByEvent(eventIds)` — event 별 `playerCounts` → Result 슬롯 환산. `BattleEventRepositoryWithCache` 를 Service 가 직접 알아야 함.
- `computeTotalBattleCount(...)` — EventTotalSlotSums × slotsPerGame 으로 게임 수 계산.

또한 캐시 단위가 count/raw 라서 외부에 캐시-목적 컨테이너 (`RawCounts<T>`, `BrawlerPairRankCounts`, `EventTotalSlotSum`) 들이 노출되어 있다.

### 목표

1. Service 를 `dateRange` 정규화 + Repository 위임만 하는 얇은 layer 로 축소
2. `GameSlot` 도메인 값 객체 도입 — `playerCounts` 를 감싸 슬롯 종류별 환산을 메서드 호출 대신 property 로 명시
3. `GameSlots` 도메인 객체 도입 — event 별 GameSlot 묶음. missing event 는 default 폴백을 한 진입점에서 책임
4. `GameSlotRepository` 도입 — `BattleEventRepositoryWithCache` 만 의존. `find(eventId)`, `findAllByEventIds(ids)` 만 책임
5. Statistics Repository 의 외부 시그니처를 `*Statistics` 도메인 객체 반환으로 변경하고, `@Cacheable` 단위도 Statistics 단위로 교체
6. `computeTotalGameCount(...)` 는 별도 Repository 가 아니라 `BattleResultStatisticsRepository` 의 private 메서드로 흡수하여 한 readOnly 트랜잭션 안에서 Pair/Enemy raw 조회와 묶음
7. 캐시-목적 외부 컨테이너 (`RawCounts<T>`, `BrawlerPairRankCounts`, `EventTotalSlotSum`) 제거

### 범위

- 적용 대상: `BattleResultStatisticsService/Repository(+WithCache)`, `BattleRankStatisticsService/Repository(+WithCache)`
- 변경 없음: `BrawlerItemOwnershipStatisticsService` (이미 단순 위임), `player/` 하위 통계 (별도 패키지)
- API 응답 형태 변경 없음 → Controller / QueryComposer / frontend / admin-frontend 영향 없음
- DB 스키마 / 마이그레이션 SQL 변경 없음

## 2. 새 도메인 객체

### `GameSlot`

위치: `com.imstargg.core.domain.statistics.GameSlot`

기존 `SlotsPerGame` (object + 3 함수) 를 대체한다. `playerCounts` 를 감싼 값 객체로, 슬롯 종류별 환산을 property 로 노출.

```kotlin
/**
 * 한 게임당 각 통계 엔티티에 누적되는 슬롯 수.
 *
 * 각 Aggregator 의 집계 로직과 일치해야 한다:
 *  - Result: 양 팀 모든 슬롯 카운트 → `Σ nᵢ`
 *  - Pair:   한 팀 내 방향성 페어 카운트 → `Σ nᵢ(nᵢ−1)`
 *  - Enemy:  모든 적팀 양방향 매치업 카운트 → `(Σ nᵢ)² − Σ nᵢ²`
 *
 * 환산: gameCount = totalSlotSum / GameSlot.<x>SlotsPerGame
 */
data class GameSlot(val playerCounts: List<Int>) {
    val resultSlotsPerGame: Int
        get() = playerCounts.sum()
    val pairSlotsPerGame: Int
        get() = playerCounts.sumOf { it * (it - 1) }
    val enemySlotsPerGame: Int
        get() {
            val total = playerCounts.sum()
            return total * total - playerCounts.sumOf { it * it }
        }

    companion object {
        fun default(): GameSlot = GameSlot(BattleEventMode.DEFAULT_PLAYER_COUNTS)
    }
}
```

기존 `SlotsPerGame.kt` 의 KDoc(각 Aggregator 와 일치해야 한다는 도메인 히스토리) 은 위 KDoc 으로 이전한 뒤 파일 삭제.

### `GameSlots`

위치: `com.imstargg.core.domain.statistics.GameSlots`

event 별 GameSlot 의 묶음. missing event 에 대한 default 폴백을 단일 진입점으로 책임진다. `operator fun get` 으로 호출부에서 indexer 표현을 사용.

```kotlin
class GameSlots(
    private val byEventId: Map<BrawlStarsId, GameSlot>,
) {
    operator fun get(eventId: BrawlStarsId): GameSlot =
        byEventId[eventId] ?: GameSlot.default()
}
```

## 3. 새 Repository — `GameSlotRepository`

위치: `com.imstargg.core.application.statistics.GameSlotRepository`

`BattleEventRepositoryWithCache` 만 의존. DB 직접 접근이 없으므로 `@Transactional` 불필요. `BattleEventRepositoryWithCache` 가 이미 로컬 캐시라 별도 `*WithCache` 도 만들지 않는다.

```kotlin
@Repository
class GameSlotRepository(
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
) {
    fun find(eventId: BrawlStarsId): GameSlot =
        battleEventRepositoryWithCache.find(eventId)?.mode?.playerCounts
            ?.let(::GameSlot)
            ?: GameSlot.default()

    fun findAllByEventIds(eventIds: List<BrawlStarsId>): GameSlots =
        GameSlots(eventIds.distinct().associateWith { find(it) })
}
```

## 4. `BattleResultStatisticsRepository` 재구성

`BrawlerBattleResultStatsJpaRepository` / `BrawlerEnemyBattleResultStatsJpaRepository` / `BrawlerPairBattleResultStatsJpaRepository` + `GameSlotRepository` 를 주입. `@Transactional(readOnly = true)` 안에서 Pair/Enemy 통계 조회와 `computeTotalGameCount` 가 같은 DB 스냅샷으로 묶이도록 한다.

### 외부 시그니처

```kotlin
fun findBrawlerResultStatistics(eventId, tierRange, startDate, endDate): BrawlerResultStatistics
fun findBrawlerPairResultStatistics(eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate): BrawlerPairResultStatistics
fun findBrawlerEnemyResultStatistics(eventId, brawlerId, tierRange, startDate, endDate): BrawlerEnemyResultStatistics
fun findBrawlerDailyResultStatistics(brawlerId, tierRange, startDate, endDate): BrawlerResultDailyStatistics
```

### 내부 흐름

```kotlin
fun findBrawlerResultStatistics(...): BrawlerResultStatistics {
    val rows = battleResultJpaRepository.findGroupedByEventAndBrawler(...).map { /* projection → BrawlerEventResultCount */ }
    val gameSlots = gameSlotRepository.findAllByEventIds(rows.map { it.eventId })
    return BrawlerResultStatistics.create(rows = rows, gameSlots = gameSlots)
}

fun findBrawlerPairResultStatistics(...): BrawlerPairResultStatistics {
    val counts = pairBattleResultJpaRepository.findAllByConditions(...).map { /* projection → BrawlerPairResultCount */ }
    val totalElementCount = pairBattleResultJpaRepository.countDistinctPairBrawlers(...)
    val totalGameCount = computeTotalGameCount(eventId, tierRange, startDate, endDate)
    return BrawlerPairResultStatistics.create(
        totalBattleCount = totalGameCount,
        totalElementCount = totalElementCount,
        counts = counts,
    )
}

fun findBrawlerEnemyResultStatistics(...): BrawlerEnemyResultStatistics { /* Pair 와 동일 패턴 */ }

fun findBrawlerDailyResultStatistics(...): BrawlerResultDailyStatistics {
    // 기존 로직 그대로 (분모 계산 불필요)
}

private fun computeTotalGameCount(
    eventId: BrawlStarsId?, tierRange: StatsTierRange,
    startDate: LocalDate, endDate: LocalDate,
): Long {
    val sums = battleResultJpaRepository.findEventTotalSlotSums(...)
    val gameSlots = gameSlotRepository.findAllByEventIds(sums.map { BrawlStarsId(it.eventBrawlStarsId) })
    return sums.sumOf {
        it.totalSlotCount / gameSlots[BrawlStarsId(it.eventBrawlStarsId)].resultSlotsPerGame
    }
}
```

기존 public `findEventTotalSlotSums(...)` 와 외부 컨테이너 `EventTotalSlotSum` 은 제거. JPA projection 을 `computeTotalGameCount` 내부에서 직접 소비.

## 5. `BattleResultStatisticsRepositoryWithCache`

`@Cacheable` 단위를 Statistics 객체로 교체. 캐시 키 버전은 `v1` 유지 (Redis 사용 버전이 아직 배포 전이므로 v2 bump 불필요), 키 식별자만 새 메서드명에 맞춰 정리.

```kotlin
@Repository
class BattleResultStatisticsRepositoryWithCache(
    private val battleResultStatisticsRepository: BattleResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerResultStatistics(...): BrawlerResultStatistics = battleResultStatisticsRepository.findBrawlerResultStatistics(...)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerPairResultStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
              " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerPairResultStatistics(...): BrawlerPairResultStatistics = ...

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerEnemyResultStats:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
              " + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerEnemyResultStatistics(...): BrawlerEnemyResultStatistics = ...

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerDailyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerDailyResultStatistics(...): BrawlerResultDailyStatistics = ...
}
```

기존 `'v1:eventSlotSums:*'` 키는 제거 (외부 노출 사라짐). `'v1:enemyResultRaw:*'`, `'v1:pairResultRaw:*'`, `'v1:brawlerEventResult:*'`, `'v1:brawlerDailyResult:*'` 키는 새 키 식별자로 교체.

## 6. `BattleRankStatisticsRepository(+WithCache)` 동일 패턴

### 외부 시그니처

```kotlin
fun findBrawlerRankStatistics(eventId, tierRange, startDate, endDate): BrawlerRankStatistics
fun findBrawlerPairRankStatistics(eventId, brawlerId, pairBrawlerId, tierRange, startDate, endDate): BrawlerPairRankStatistics
```

내부에서 기존 projection → item 매핑 (`averageRank` 계산 포함) 을 그대로 수행. Pair 분모는 기존 `pairBattleRankJpaRepository.countTotalBattleCount(...)` 를 그대로 사용 (Result 의 게임 수 계산과 무관한 별개 로직).

### 캐시 키

```
'v1:brawlerRankStats:'     + eventId + ':' + tierRange + ':' + startDate + ':' + endDate
'v1:brawlerPairRankStats:' + eventId + ':' + brawlerId + ':' + pairBrawlerId + ':' + tierRange + ':' + startDate + ':' + endDate
```

기존 `'v1:battleEventBrawlerRank:*'`, `'v1:pairRank:*'` 는 새 키 식별자로 교체.

## 7. Statistics 도메인 객체 `create(...)` 변경

영향있는 곳은 한 곳: `BrawlerResultStatistics.create`.

```kotlin
// Before
fun create(
    rows: List<BrawlerEventResultCount>,
    slotsPerGameByEvent: Map<BrawlStarsId, Int>,
): BrawlerResultStatistics

// After
fun create(
    rows: List<BrawlerEventResultCount>,
    gameSlots: GameSlots,
): BrawlerResultStatistics
```

내부에서 `gameSlots[eventId].resultSlotsPerGame` 으로 접근. `error("playerCounts missing for event=$eventId")` 분기는 GameSlots 가 default 를 보장하므로 제거.

다른 도메인 객체 (`BrawlerPairResultStatistics.create`, `BrawlerEnemyResultStatistics.create`, `BrawlerResultDailyStatistics.create`, `BrawlerRankStatistics`, `BrawlerPairRankStatistics`) 의 시그니처는 변경 없음.

## 8. Service 의 최종 모양

`BattleResultStatisticsService`:

```kotlin
@Service
class BattleResultStatisticsService(
    private val clock: Clock,
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?, tierRange: StatsTierRange, dateRange: StatsDateRange,
    ): BrawlerResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerResultStatistics(eventId, tierRange, startDate, endDate)
    }
    // Pair / Enemy / Daily 도 동일 — dateRange 정규화 + 위임만
}
```

`BattleEventRepositoryWithCache` 의존 제거. `slotsPerGameByEvent` / `computeTotalBattleCount` private 메서드 제거.

`BattleRankStatisticsService` 도 같은 형태 (clock + RepositoryWithCache 만 주입).

## 9. 의존 그래프 (확정)

```
BattleResultStatisticsService
   └─ BattleResultStatisticsRepositoryWithCache   (@Cacheable: Statistics 단위, v1 키)
        └─ BattleResultStatisticsRepository       (@Transactional readOnly)
             ├─ BrawlerBattleResultStatsJpaRepository
             ├─ BrawlerEnemyBattleResultStatsJpaRepository
             ├─ BrawlerPairBattleResultStatsJpaRepository
             └─ GameSlotRepository
                  └─ BattleEventRepositoryWithCache

BattleRankStatisticsService
   └─ BattleRankStatisticsRepositoryWithCache
        └─ BattleRankStatisticsRepository
             ├─ BrawlerBattleRankStatsJpaRepository
             └─ BrawlerPairBattleRankStatsJpaRepository
```

## 10. 제거되는 파일

- `application/statistics/RawCounts.kt`
- `application/statistics/BrawlerPairRankCounts.kt`
- `application/statistics/EventTotalSlotSum.kt`
- `domain/statistics/SlotsPerGame.kt` (→ `GameSlot.kt` 로 대체)
- 관련 테스트 (`SlotsPerGameTest.kt`) 는 `GameSlotTest.kt` 로 교체

## 11. 테스트 계획

| 파일 | 변경 |
|---|---|
| `SlotsPerGameTest.kt` | → `GameSlotTest.kt` 로 교체. 기존 케이스/검증값 동일 (`GameSlot(listOf(3,3)).resultSlotsPerGame == 6` 등) |
| `GameSlotsTest.kt` | 신규 — existing eventId 와 missing eventId 두 케이스 |
| `GameSlotRepositoryTest.kt` | 신규 — `CoreApiContextTest` + 실 DB. BattleEvent 있/없 두 케이스 |
| `BattleResultStatisticsRepositoryTest.kt` | 신규 또는 확장 — `CoreApiContextTest` + 실 DB. 4개 메서드 통합 검증 |
| `BattleRankStatisticsRepositoryTest.kt` | 신규 또는 확장 — 동일 패턴 |
| `BattleResultStatisticsServiceTest.kt` | 슬림화 — Service 가 위임뿐이라 dateRange 정규화 검증만. 기존 통계 계산 검증은 Repository 테스트로 이동 |
| `BattleRankStatisticsServiceTest.kt` | 동일하게 슬림화 |
| `BrawlerResultStatistics` 단위 테스트 | 입력 `Map<...,Int>` → `GameSlots` 로 교체 (`GameSlots(mapOf(...))` 직접 생성) |

## 12. 작업 순서

컴파일이 끊기지 않도록 다음 순서로 진행:

1. `domain/statistics/GameSlot.kt`, `GameSlots.kt` 추가 — `SlotsPerGame.kt` 는 일단 유지
2. `application/statistics/GameSlotRepository.kt` 추가
3. `BrawlerResultStatistics.create` 시그니처를 `gameSlots: GameSlots` 로 변경 + 호출자 (Service) 함께 갱신 — `error("playerCounts missing")` 분기 제거
4. `BattleResultStatisticsRepository` 재구성: Statistics 반환 4개 메서드, `gameSlotRepository` 주입, `computeTotalGameCount` private 메서드, public `findEventTotalSlotSums` 제거
5. `BattleResultStatisticsRepositoryWithCache` 재구성: `@Cacheable` 키/시그니처 교체
6. `BattleResultStatisticsService` 슬림화 (`BattleEventRepositoryWithCache` 의존/슬롯 계산 제거)
7. `BattleRankStatisticsRepository(+WithCache, +Service)` 동일 패턴 적용
8. 제거: `RawCounts.kt`, `BrawlerPairRankCounts.kt`, `EventTotalSlotSum.kt`, `SlotsPerGame.kt` (+ `SlotsPerGameTest.kt`)
9. 테스트 갱신 (위 11. 표 참조)
10. `./gradlew ktlintFormat build` 통과 확인

## 13. 영향 없음 확인

- API 응답 / Controller / QueryComposer / frontend / admin-frontend: 영향 없음 (Service 반환 타입 동일)
- DB 스키마 / 마이그레이션 SQL: 변경 없음
- `BrawlerItemOwnershipStatisticsService`: 변경 없음
- `player/` 하위 통계: 변경 없음 (별도 패키지)
