# core-batch 데이터 정확성 번들 설계서

- 작성일: 2026-04-23
- 대상 모듈: `imstargg-backend/core/core-batch`, `imstargg-backend/core/core-worker`, `imstargg-backend/infrastructure/db-core`
- 선행 문서: core-batch 코드 리뷰 최종 보고서 (2026-04-23)

## 1. 목적

core-batch 코드 리뷰 결과 Critical·High 로 분류된 **데이터 정확성** 이슈를 한 묶음으로 해결한다. 팀 통계 메모리 스트리밍 등의 구조적 재설계(스펙 B) 이전에, **집계 수치 자체가 틀려 있을 수 있는 문제를 먼저 바로잡고 멱등성/원자성을 확보**하는 것이 이 스펙의 목표다.

이 스펙의 핵심 결정은 **Rank 전투의 `battle_key` 재정의**다. aggregator 측에 보조 dedup 장치를 추가하는 대신, 데이터 모델을 바꿔 "같은 battle 은 DB 에 1 row" 로 만든다. 서비스 미운영 시기라 마이그레이션 비용이 없는 지금이 아니면 하기 어려운 구조적 수정이다.

## 2. 범위

다음 이슈를 포함한다.

| #   | 심각도      | 이슈                                                                                          |
| --- | ----------- | --------------------------------------------------------------------------------------------- |
| 1-A | 🔴 Critical | `stats_rank_battle` 기반 rank 통계의 팀 크기 편향 중복 집계                                   |
| 1-B | 🔴 Critical | 수동 `EntityManager` 트랜잭션과 Spring Batch step 트랜잭션 분리로 인한 부분 실패 시 깨진 상태 |
| 2-B | 🟠 High     | `deleteExisting` → `persist` 사이 수십 초 공백에 외부 API가 빈 통계 관측                      |
| 2-C | 🟠 High     | 집계 테이블에 자연키 unique 제약 없음                                                         |
| 3-A | 🟡 Medium   | 통계/파티션 잡의 `RunIdIncrementer` 누락으로 동일 `date` 재실행 불가                          |
| 부수 | —          | `BattleKeyBuilder` 를 `db-core` 로 이전해 `BattleKeyEmbedded` 로 재구성                       |
| 부수 | —          | `imstargg-backend/docs/stats-battle-data-strategy.md` 문서 확장                                |

다음은 **포함하지 않는다** (별도 스펙).

- 팀 통계 메모리 스트리밍, chunk-oriented step 전환 → **스펙 B**
- 브롤스타즈 API 실패 삼킴, `orderBy` 누락, 알림 누락 → **스펙 C**
- `BrawlerItemOwnershipStatsJob`, `BrawlerSyncJob`, 파티션 매니지 개선

## 3. 대상 파일 맵

### 3.1 변경 대상

| 경로                                                                                                     | 변경 성격                                    |
| -------------------------------------------------------------------------------------------------------- | -------------------------------------------- |
| `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbedded.kt` | 신규: `@Embeddable` 값 객체 (1-A)            |
| `core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilder.kt`                   | 삭제 (1-A)                                   |
| `core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleKeyBuilderTest.kt`               | `db-core` 하위로 이동 + 확장 (1-A)           |
| `core/core-worker/src/main/kotlin/com/imstargg/core/worker/domain/BattleBuilder.kt`                      | battle_key 생성 로직 위임 (1-A)              |
| `core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplier.kt`   | rank dedup 키 단순화 (1-A)                   |
| `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/StatsRankBattleEntity.kt` | unique index 재정의 (1-A)                     |
| `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/BattleKeyPlayerTagProjection.kt` | 삭제 (1-A)                                   |
| `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustom.kt` | 시그니처 변경 (1-A)                          |
| `infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/repository/StatsRankBattleJpaRepositoryCustomImpl.kt` | 구현 변경 (1-A)                              |
| `infrastructure/db-core/src/main/resources/schema.sql`                                                   | unique index 재정의 + 통계 자연키 unique (1-A, 2-C) |
| `infrastructure/db-core/src/main/resources/db/migration/V260423a__stats_rank_battle_unique_key.sql`      | 신규 (1-A)                                   |
| `infrastructure/db-core/src/main/resources/db/migration/V260423b__stats_natural_key_unique.sql`          | 신규 (2-C)                                   |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt`      | 트랜잭션 구조 변경 (1-B, 2-B)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt` | 트랜잭션 구조 변경 (1-B, 2-B)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt` | 트랜잭션 구조 변경 (1-B, 2-B)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleRankStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerTeamBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt` | `RunIdIncrementer` 추가 (3-A)                |
| `core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/PartitionManageJobConfig.kt`                | `RunIdIncrementer` 추가 (3-A)                |
| `imstargg-backend/docs/stats-battle-data-strategy.md`                                                    | BattleKey 정의·rank 구조 섹션 추가           |

### 3.2 테스트 추가/확장 대상

| 경로                                                                                                        | 내용                                    |
| ----------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/statistics/battle/BattleKeyEmbeddedTest.kt` | 신규: 기존 `BattleKeyBuilderTest` 이관 + rank/result 생성 분기 검증 |
| `core/core-worker/src/test/kotlin/com/imstargg/core/worker/domain/BattleBuilderTest.kt`                     | rank/result 별 battle_key 생성 동작 확장 |
| `core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/StatsBattleAggregateApplierTest.kt`  | 같은 팀 다른 플레이어 수집 시 1건 merge 검증 |
| `core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/*StatsJobConfigContextTest.kt`      | 신규 또는 확장: 트랜잭션 원자성 통합 테스트 |

## 4. 이슈별 설계

### 4.1 (1-A) Rank 전투 battle_key 재정의 + unique 축소

#### 문제 요약

현재 `stats_rank_battle` 는 한 rank 전투가 **같은 팀 플레이어 수만큼 row** 로 저장된다 (트리오 랭크: 1 battle → 3 rows). aggregator 는 각 row 마다 `playerTeams()`(본인 팀) 을 전부 순회해 집계하기 때문에 **같은 팀이 3회 중복 집계**된다. Unique key 가 `(battle_date, battle_key, player_brawlstars_tag)` 라서 DB 레벨에서는 이 중복이 정상 row 로 수용된다.

#### 수정 방침

**데이터 모델 수준에서 해결한다.** 서비스 미운영 시기라 마이그레이션 비용이 낮은 지금 구조적으로 수정한다.

1. **rank 전투의 `battle_key` 를 "자기 팀 플레이어 태그" 기반으로 생성** 한다. (result 는 현행 유지 — 전체 플레이어 태그 기반)
2. **`stats_rank_battle` 의 unique index 를 `(battle_date, battle_key)`** 로 축소. `player_brawlstars_tag` 는 unique key 에서 제외.
3. 결과적으로 "같은 팀이 만들어내는 raw response N 개" 는 같은 `battle_key` 를 가지고 1 row 로 merge 된다.
4. aggregator (`BrawlerBattleRankStatsAggregator`, `BrawlerTeamBattleRankStatsAggregator`) 는 **로직 수정 없이 자동으로 1회만 집계**된다.

#### 용인 정책 (명시)

- **같은 battle 이 서로 다른 팀의 관측으로 2 건 이상 저장될 수 있음**. 예: 트리오 랭크에서 "우리 팀 A/B/C" 가 수집되고 "상대 팀 D/E/F" 도 수집되면 서로 다른 `battle_key` 로 2 rows.
- 현재 수집 대상 플레이어(`추적 대상`) 분포상 같은 배틀의 여러 팀이 모두 추적 대상일 확률은 낮다. 운영 관점에서 수용 가능.
- 이 정책은 `docs/stats-battle-data-strategy.md` 에 명시한다. 향후 모니터링에서 중복이 크다면 별도 스펙으로 다룬다.

#### `playerBrawlStarsTag` 의 의미 재정의

- 변경 후 row 당 `playerBrawlStarsTag` 는 "같은 팀 후보 중 '먼저 적재된' 플레이어 1명" 을 가리킨다 (현재 dedup 로직의 자연스러운 귀결: 첫 save 가 살아남고 unique 충돌하는 후속 save 는 dedup 으로 걸러짐).
- `playerTeams()` / `enemyTeams()` 의 동작은 "그 대표 플레이어의 팀 / 상대 팀" 으로 해석되며, rank 집계 로직과 일치한다. 추가 수정 불필요.

#### `BattleKeyEmbedded` 도입

- `core-worker/domain/BattleKeyBuilder.kt` 를 `infrastructure/db-core` 로 이전하면서 값 객체 `BattleKeyEmbedded` 로 재구성한다. `BaseStatsBattleEntity.battleKey: String` 타입은 **유지** (컬럼 타입/이름/의미 보존). `BattleKeyEmbedded` 는 "값 계산용 팩토리" 역할.

```kotlin
// infrastructure/db-core/statistics/battle/BattleKeyEmbedded.kt
package com.imstargg.infrastructure.db.core.statistics.battle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.OffsetDateTime

@JvmInline
value class BattleKeyEmbedded(val value: String) {
    companion object {
        fun forResult(
            battleTime: OffsetDateTime,
            playerTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTags)

        fun forRank(
            battleTime: OffsetDateTime,
            playerTeamTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTeamTags)

        private fun hash(battleTime: OffsetDateTime, tags: Collection<String>): BattleKeyEmbedded {
            val joined = tags.sorted().joinToString("").toByteArray(Charsets.UTF_8)
            val buf = ByteBuffer.allocate(8 + joined.size)
            buf.putLong(battleTime.toEpochSecond())
            buf.put(joined)
            val digest = MessageDigest.getInstance("SHA-256").digest(buf.array())
            return BattleKeyEmbedded(digest.toHex())
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }
    }
}
```

> `@Embeddable` 가 아닌 `@JvmInline value class` 로 선언하는 이유: 컬럼 타입/이름을 바꾸지 않고 "값 계산 규칙" 만 응집시키는 최소 변경. `BaseStatsBattleEntity.battleKey: String` 타입 유지. JPA 는 원시 String 으로 다룬다.

- `forResult` 와 `forRank` 를 분리해 "rank 는 자기 팀만, result 는 전체" 의 도메인 규칙을 타입 시스템에 반영.

#### `BattleBuilder` 변경

```kotlin
// core-worker/domain/BattleBuilder.kt
private fun createBattleKey(teams: List<List<BattleResultPlayerResponse>>): String {
    val isRank = battleResponse.battle.rank != null
    return if (isRank) {
        val playerTeam = teams.firstOrNull { team -> team.any { it.tag == brawlStarsTag } }
            ?: throw IllegalArgumentException("수집 대상 플레이어가 팀에 포함되지 않았습니다. tag=$brawlStarsTag")
        BattleKeyEmbedded.forRank(
            battleTime = battleResponse.battleTime,
            playerTeamTags = playerTeam.map { it.tag },
        ).value
    } else {
        BattleKeyEmbedded.forResult(
            battleTime = battleResponse.battleTime,
            playerTags = teams.flatten().map { it.tag },
        ).value
    }
}
```

> rank 판단 기준은 `battle.rank != null` — 수집 시점에 이미 확인 가능. `battle.type` 에 의존하지 않아 솔로 쇼다운, 듀오 쇼다운, 트리오 랭크 모두 동일하게 동작.

#### `StatsBattleAggregateApplier` 변경

- `BattleKeyPlayerTagProjection` 및 `findAllBattleKeyAndPlayerTagByStatsRankBattles` 제거.
- `StatsRankBattleJpaRepositoryCustom` 에 `findAllBattleKeysByStatsRankBattles` 를 추가 (result 와 동일 시그니처).
- dedup 키: `battle.battleKey` 단일.
- **같은 `apply()` 호출 안에서 중복 방지** — 한 팀의 여러 플레이어가 모두 수집 대상이고 하나의 `apply()` 호출에서 함께 들어올 경우, 세 건이 같은 `battle_key` 로 만들어진다. DB 대조는 저장 전 상태이므로 세 건 모두 `newBattles` 로 통과하고 `saveAll` 에서 unique 제약 위반. in-memory `distinctBy { it.battleKey }` 를 DB 대조 전에 적용해 이 경로를 막는다.
- result 전투에도 동일 원칙 적용 — 기존 unique 가 `(battle_date, battle_key)` 여서 원래도 잠재 문제였으나 실제 빈도가 낮았을 뿐. 일관성을 위해 함께 수정.

```kotlin
val statsRankBattles =
    aggregatableBattles
        .filter { canCreateRankBattle(it) }
        .map { toStatsRankBattle(it) }
        .distinctBy { it.battleKey }    // in-memory dedup (same-apply duplicates)

val statsResultBattles =
    aggregatableBattles
        .filter { canCreateResultBattle(it) }
        .map { toStatsResultBattle(it) }
        .distinctBy { it.battleKey }    // in-memory dedup (same-apply duplicates)

val existingResultBattleKeys =
    statsResultBattleRepository.findAllBattleKeysByStatsResultBattles(statsResultBattles)
val existingRankBattleKeys =
    statsRankBattleRepository.findAllBattleKeysByStatsRankBattles(statsRankBattles)

val newRankBattles = statsRankBattles.filter { it.battleKey !in existingRankBattleKeys }
val newResultBattles = statsResultBattles.filter { it.battleKey !in existingResultBattleKeys }
```

> `distinctBy` 는 최초 항목을 보존한다. 어떤 수집 주체의 row 가 살아남을지는 `battles` 입력 순서에 의존하며, 이는 `playerBrawlStarsTag` 대표 정책("팀 내 대표 1 명, 순서는 구현상 자연스러운 순") 과 일관된다.

#### Unique index 변경 (`StatsRankBattleEntity`, schema.sql, migration)

`StatsRankBattleEntity.kt`:

```kotlin
@Table(
    name = "stats_rank_battle",
    indexes = [
        Index(
            name = "uk_stats_rank_battle__battle_date_battle_key",
            columnList = "battle_date, battle_key",
            unique = true,
        ),
        Index(
            name = "ix_stats_rank_battle__battle_date_event",
            columnList = "battle_date, event_brawlstars_id",
        ),
    ],
)
```

`schema.sql` 에서 기존 `uk_stats_rank_battle__battle_date_battle_key_player` 를 `uk_stats_rank_battle__battle_date_battle_key` 로 교체.

`V260423a__stats_rank_battle_unique_key.sql`:

```sql
-- rank 전투 unique key 축소: (battle_date, battle_key, player_brawlstars_tag)
-- → (battle_date, battle_key)
-- 근거: battle_key 를 자기 팀 플레이어 태그 기반으로 생성하여 팀 단위 1 row.
-- 서비스 미운영 시기라 기존 데이터는 버려도 되며, 변경 후 중복 데이터가 없음을 전제.

drop index if exists uk_stats_rank_battle__battle_date_battle_key_player;

create unique index if not exists uk_stats_rank_battle__battle_date_battle_key
    on stats_rank_battle (battle_date, battle_key);
```

> 파티셔닝 테이블이라 index 는 파티션별로 생성되어 있을 수 있음. 플랜 단계에서 `pg_partman` / 수동 파티션 여부에 따라 `drop index` 범위 확인.

#### aggregator 는 수정 없음

- `BrawlerBattleRankStatsAggregator`, `BrawlerTeamBattleRankStatsAggregator` 는 기존 로직 유지. 데이터 모델이 바뀌면서 "row 1개 = battle 1개" 가 되어 자연스럽게 1회만 집계된다.
- 스펙 A 이전 초안에서 논의했던 "자기 플레이어만 집계(옵션 2)" / "battleKey HashSet dedup(옵션 1)" 은 **모두 불필요**.

#### 검증

- `BattleKeyEmbeddedTest`: 기존 `BattleKeyBuilderTest` 시나리오 이관 + `forRank` 와 `forResult` 가 서로 다른 키를 생성하는지 검증.
- `BattleBuilderTest`: `rank != null` 인 응답은 `forRank`, 아니면 `forResult` 를 쓰는지 검증.
- `StatsBattleAggregateApplierTest`: 같은 rank 배틀에서 같은 팀 소속 서로 다른 수집 주체로 두 번 들어온 raw 를 apply 하면 저장된 `stats_rank_battle` 은 1 건.
- 배치 통합 테스트(`*StatsJobConfigContextTest`): rank 집계가 중복 없이 1 battle 당 1 회만 집계됨을 검증.

---

### 4.2 (1-B, 2-B) 트랜잭션 원자화

#### 문제 요약

세 tasklet(`DailyBattleStatsTasklet`, `PerEventDailyBattleStatsTasklet`, `PerEventWeeklyBattleStatsTasklet`)은 Spring Batch 가 `txManager` 로 트랜잭션을 열어주지만, 내부에서 `entityManagerFactory.createEntityManager()` 로 별도 application-managed EM 을 만들고 수동 `em.transaction.begin()/commit()` 으로 DML 을 커밋한다. 결과:

1. `deleteExisting` 커밋 이후 `persist` chunk 가 N 번째에서 실패하면 롤백되지 않음 → **빈/부분 통계 상태 잔존**.
2. `deleteExisting` 와 첫 `persist` 커밋 사이에 수십 초~수 분 간격 발생 → **외부 조회 API 가 빈 통계 관측**.
3. 수동 EM 과 Spring EM 이 동시에 커넥션을 점유 → HikariCP 풀 고갈 가능.

#### 수정 방침

tasklet 을 "chunk-oriented step" 으로 전환하는 것은 스펙 B 에서 수행하는 큰 리팩토링이다. 본 스펙에서는 **현 tasklet 구조를 유지하되, "이벤트 1개(혹은 날짜 1개)의 delete + persist 전체" 를 하나의 Spring-managed 트랜잭션으로 묶는다**.

#### 트랜잭션 경계

- `DailyBattleStatsTasklet`: `deleteExisting(date)` + 전체 `persist` 을 **하나의 트랜잭션**. (범위: 날짜 1건)
- `PerEventDailyBattleStatsTasklet`: `deleteExisting(date, eventId)` + 해당 이벤트의 전체 `persist` 을 **이벤트별 트랜잭션**. (범위: 날짜 1개 × 이벤트 1개)
- `PerEventWeeklyBattleStatsTasklet`: `deleteExisting(weekRange, eventId)` + 해당 (주간 범위, 이벤트) 의 전체 `persist` 을 **(주간 범위, 이벤트) 조합별 트랜잭션**. (범위: 주간 범위 1개 × 이벤트 1개)

각 트랜잭션 내에서 대용량 데이터의 1차 캐시 누적을 막기 위해 기존 `em.clear()` 호출은 유지한다.

#### 읽기/쓰기 EM 분리

- **읽기 EM**: 트랜잭션 밖에서 배틀을 페이지 단위로 읽는 용도. application-managed `EntityManager` (`entityManagerFactory.createEntityManager()`) 를 사용하되 수동 트랜잭션은 열지 않는다. 읽기 중 `em.clear()` 로 1차 캐시 누적을 막는 기존 패턴 유지.
- **쓰기 EM**: 실제 DML 을 수행하는 용도. **반드시 Spring-managed** 이어야 한다. 쓰기 전용 협력자 빈에 `@PersistenceContext` 로 EM 을 주입받고 `@Transactional` 메서드 안에서 사용한다. `entityManagerFactory.createEntityManager()` 를 트랜잭션 내부에서 직접 호출하면 application-managed 로 남아 Spring 트랜잭션에 묶이지 않으므로 **금지**.

#### 코드 스케치

쓰기 전용 협력자 빈을 도입해 `@Transactional` 로 원자화한다.

```kotlin
@Component
class StatsReplaceWriter(
    @PersistenceContext private val em: EntityManager,
) {
    @Transactional
    fun <T : Any> replace(
        deleteExisting: (JPAQueryFactory) -> Unit,
        entities: List<T>,
        persistAdditional: (EntityManager, T) -> Unit = { _, _ -> },
    ) {
        val writeQueryFactory = JPAQueryFactory(em)
        deleteExisting(writeQueryFactory)
        entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
            batch.forEach {
                em.persist(it)
                persistAdditional(em, it)
            }
            em.flush()
            em.clear()
        }
    }
}
```

tasklet 은 읽기 EM 만 직접 열고, 쓰기는 writer 빈에 위임한다.

```kotlin
class PerEventWeeklyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val entityManagerFactory: EntityManagerFactory, // 읽기 전용
    private val writer: StatsReplaceWriter,                 // 쓰기 전용 (Spring-managed tx)
    // ... 기존 함수형 의존성
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val weekRanges = weekRangesSupplier()
        entityManagerFactory.createEntityManager().use { readEm ->
            val readQueryFactory = JPAQueryFactory(readEm)
            for (weekRange in weekRanges) {
                val eventIds = readDistinctEventIds(readQueryFactory, weekRange)
                for (eventId in eventIds) {
                    val aggregator = aggregatorFactory(weekRange)
                    readBattlesIntoAggregator(readEm, readQueryFactory, weekRange, eventId, aggregator)
                    val entities = aggregator.toEntities()
                    writer.replace(
                        deleteExisting = { qf -> deleteExisting(qf, weekRange, eventId) },
                        entities = entities,
                        persistAdditional = persistAdditional,
                    )
                }
            }
        }
        return RepeatStatus.FINISHED
    }
}
```

#### 실패 시 동작

- 트랜잭션 내에서 예외 발생 시 delete + persist 모두 롤백 → 해당 이벤트 통계는 **이전 상태 그대로 유지**.
- step 전체가 실패로 Spring Batch 에 보고되고, 재실행 시 `RunIdIncrementer`(3-A) 덕분에 같은 `date` 로 재시도 가능.

#### 한계 (의도된 미해결)

- 단일 이벤트의 집계 결과 List 가 매우 크면 한 트랜잭션이 길어진다.
- 현재 관측치 기준(수천~수만 추정, 리뷰 보고서 섹션 2-A)은 감당 가능.
- 본 스펙에서는 이 tradeoff 를 수용하고, 스펙 B 의 스트리밍 + upsert 전환으로 구조적으로 해결한다.

---

### 4.3 (2-C) 자연키 unique 인덱스 추가

#### 문제 요약

현재 통계 테이블은 application-level "매번 delete + insert" 방어만 존재하며 DB 레벨 unique 제약이 없다. 운영자가 SQL 로 수동 재적재하거나 잡을 두 번 돌리면 DB 가 막아주지 못한다.

#### 추가 인덱스

| 테이블                               | 자연키                                                                                                                                         |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `brawler_battle_result_stats`        | `(event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id)`                                                                        |
| `brawler_battle_rank_stats`          | `(event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id)`                                                                        |
| `brawler_team_battle_result_stats`   | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key)`                                                              |
| `brawler_team_battle_rank_stats`     | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key)`                                                              |
| `brawler_enemy_battle_result_stats`  | `(event_brawlstars_id, tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id)`                    |

- `team_key` 는 `BrawlerTeamEmbedded.teamKey` 로 이미 정의되어 있는 `varchar(100)` 컬럼 (정렬된 브롤러 ID 문자열). 별도 컬럼 신설 없이 바로 unique 인덱스에 사용 가능.

#### 마이그레이션 파일

`infrastructure/db-core/src/main/resources/db/migration/V260423b__stats_natural_key_unique.sql`

```sql
create unique index if not exists uk_brawler_battle_result_stats__1
  on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_battle_rank_stats__1
  on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_team_battle_result_stats__1
  on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_team_battle_rank_stats__1
  on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_enemy_battle_result_stats__1
  on brawler_enemy_battle_result_stats (
      event_brawlstars_id, tier_range, start_battle_date, end_battle_date,
      brawler_brawlstars_id, enemy_brawler_brawlstars_id
  );
```

- 프로젝트 컨벤션에 따라 `schema.sql` 에도 동일 `create unique index if not exists` 문을 추가한다.
- 서비스 미운영 시기라 기존 중복 데이터 진단 단계는 생략 가능. 개발 DB 는 truncate 후 새 로직으로 재적재.

---

### 4.4 (3-A) `RunIdIncrementer` 적용

#### 대상 잡

- `brawlerBattleResultStatsJob`
- `brawlerBattleRankStatsJob`
- `brawlerTeamBattleResultStatsJob`
- `brawlerTeamBattleRankStatsJob`
- `brawlerEnemyBattleResultStatsJob`
- `brawlerItemOwnershipStatsJob`
- `partitionManageJob`

#### 수정 내용

각 `JobBuilder(...)` 에 `.incrementer(RunIdIncrementer())` 추가. 기존 `DateJobParameter` 는 identifying 파라미터로 유지하여 "같은 date, 다른 run.id" 로 재실행 가능.

```kotlin
JobBuilder(JOB_NAME, jobRepository)
    .incrementer(RunIdIncrementer())
    .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
    .start(step)
    .build()
```

#### 운영 영향

- 재실행성이 확보되어 동일 `date` 재집계가 간단해진다.
- 스케줄러/진입점이 명시적으로 잡을 트리거하는 환경임을 플랜 단계에서 재확인한다.

---

### 4.5 문서화 — `stats-battle-data-strategy.md` 확장

`imstargg-backend/docs/stats-battle-data-strategy.md` 에 다음 섹션을 추가한다.

- **BattleKey 정의**
  - SHA-256 hash(battle epoch seconds + sorted player tags)
  - Result 전투: 전체 플레이어 태그 기반 (모든 팀 전체)
  - Rank 전투: **수집 주체 플레이어의 자기 팀 태그만** 기반
  - `BattleKeyEmbedded.forResult` / `forRank` 팩토리
- **Rank 전투의 row 정책**
  - 같은 팀에서 여러 플레이어가 수집 대상이어도 같은 `battle_key` 로 중복 dedup → **팀 단위 1 row**.
  - 서로 다른 팀의 관측은 서로 다른 `battle_key` 를 가지므로 별도 row 로 저장됨 (용인 정책).
  - `playerBrawlStarsTag` 는 "그 팀의 대표 1명"(첫 적재 승리)을 의미.
- **집계 시 함의**
  - `BrawlerBattleRankStatsAggregator`, `BrawlerTeamBattleRankStatsAggregator` 는 row 당 `playerTeams()` 를 순회해도 중복 없이 1 battle → 1 회 집계.
- **Unique index 변경 이력**
  - `uk_stats_rank_battle__battle_date_battle_key_player` → `uk_stats_rank_battle__battle_date_battle_key` (서비스 미운영 시기 구조 수정).

---

## 5. 컴포넌트와 경계

- **`BattleKeyEmbedded`** (`db-core`): 값 객체. JPA 엔티티 필드 타입은 `String` 유지, 팩토리 만 `BattleKeyEmbedded` 경유. rank/result 분기 규칙 보유.
- **`BattleBuilder`** (`core-worker`): 수집 시 `battleKey` 생성. rank 여부는 `battle.rank != null` 로 판단.
- **`StatsBattleAggregateApplier`** (`core-worker`): dedup 키 통일 — rank/result 모두 `battleKey` 만.
- **Aggregator** (`core-batch/domain/*Aggregator.kt`): **수정 없음**. 데이터 모델 변경으로 자동 해결.
- **Tasklet** (`core-batch/job/statistics/*Tasklet.kt`): 수동 트랜잭션 제거, 쓰기 협력자 빈 `StatsReplaceWriter` 에 위임.
- **JobConfig** (`core-batch/job/statistics/*JobConfig.kt`): `RunIdIncrementer` + writer 빈 주입.
- **Schema** (`db-core/.../schema.sql`, `V260423a__*.sql`, `V260423b__*.sql`): unique index 교체 + 자연키 unique 추가.

## 6. 실행 순서와 롤아웃

서비스 미운영 시기라 운영 재집계 단계가 제거된다.

| 순서 | 단계                                                                                                |
| ---- | --------------------------------------------------------------------------------------------------- |
| 1    | `BattleKeyEmbedded` 신설 + 테스트 이관                                                              |
| 2    | `BattleBuilder` / `StatsBattleAggregateApplier` / repository 변경                                   |
| 3    | `StatsRankBattleEntity` unique index 변경 + `schema.sql` 업데이트 + `V260423a__*.sql` 마이그레이션 |
| 4    | `BattleKeyPlayerTagProjection` 및 관련 메서드 제거                                                  |
| 5    | tasklet 3종 + `StatsReplaceWriter` 도입 (1-B, 2-B)                                                  |
| 6    | 7 개 잡에 `RunIdIncrementer` 적용 (3-A)                                                             |
| 7    | 통계 자연키 unique 인덱스 추가 (`V260423b__*.sql`, 2-C)                                             |
| 8    | `stats-battle-data-strategy.md` 섹션 추가 (4.5)                                                     |
| 9    | 개발 DB truncate 후 통계 파이프라인 end-to-end 검증                                                 |

## 7. 위험과 완화

| 위험                                                                              | 완화                                                                                                                                  |
| --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| rank 전투의 "진영 간 중복 관측" (같은 battle 을 상대 팀이 각각 수집)              | 용인 정책으로 문서화. 추적 대상 분포상 드묾. 필요 시 별도 스펙에서 "전체 팀 태그 unique" 로 강화 가능.                                 |
| 같은 팀의 여러 수집 주체가 한 `apply()` 호출로 함께 들어와 unique 충돌             | `StatsBattleAggregateApplier` 에서 DB 대조 전 in-memory `distinctBy { battleKey }` 로 차단 (4.1 참조). rank/result 모두 적용.             |
| `BattleKeyBuilder` 이전에 따른 import 깨짐                                         | 기존 파일 삭제 후 `db-core` 로 이관. 컴파일러가 누락 지점을 바로 보고한다. 테스트도 함께 이관.                                         |
| `BattleKeyPlayerTagProjection` 제거로 기존 repository 메서드 소비자 깨짐          | 사용처 확인됨: `StatsBattleAggregateApplier` 만. 동시 수정.                                                                            |
| 파티셔닝된 `stats_rank_battle` 의 인덱스 drop 범위                                 | 플랜 단계에서 파티션 구성 재확인 후 마이그레이션 형태 확정 (부모 테이블 기준 drop + 각 파티션 자동 반영 여부).                         |
| 단일 이벤트 트랜잭션이 길어짐                                                      | 현 데이터 규모에서 감당 가능. 측정 후 임계 초과 시 스펙 B 의 chunk-oriented step 전환으로 해결.                                        |
| `RunIdIncrementer` 적용이 외부 스케줄러 동작에 미치는 영향                         | 진입점에서 job 을 트리거할 때 `run.id` 를 자동 증가시키는 것이 기본 동작. 기존 스크립트에서 `run.id` 를 명시하고 있는지 플랜에서 확인. |

## 8. 검증 계획

### 8.1 단위 테스트

- `BattleKeyEmbeddedTest`
  - 같은 `(battleTime, playerTags)` 입력은 같은 key 반환.
  - 정렬 순서 무관 (입력 순서 다르고 내용 같으면 같은 key).
  - `forRank` 와 `forResult` 는 `playerTags` 가 같더라도 의미 분리 — 서로 다른 entry point 로 불림을 검증 (동일 입력이면 같은 hash 이지만 호출 경로가 분기되어 있음을 테스트에 명시).
- `BattleBuilderTest`
  - `battle.rank != null` 인 응답 → rank 팀 태그만으로 key 생성.
  - `battle.rank == null` 인 응답 → 전체 팀 태그로 key 생성.
  - 수집 대상 플레이어가 팀에 없는 비정상 응답 → 예외.

### 8.2 Context / Integration 테스트

- `StatsBattleAggregateApplierTest`
  - 같은 rank 배틀이 같은 팀의 서로 다른 수집 주체로 두 번 들어왔을 때 저장 결과는 1 row.
  - 같은 `apply()` 호출 안에서 같은 팀 A·B·C 세 수집분이 함께 들어와도 unique 충돌 없이 1 row 저장 (in-memory `distinctBy` 검증).
  - result 전투도 동일 시나리오 검증.
- tasklet 의 트랜잭션 원자성: 강제 예외 주입 시 delete 가 롤백되어 이전 통계가 유지.

### 8.3 수동 검증 (개발 DB)

- truncate 후 새 로직으로 수집 → `stats_rank_battle` 에 트리오 랭크 한 battle 당 row 수가 "관측 진영 수" 를 넘지 않는지 확인.
- 집계 후 `brawler_team_battle_rank_stats.total_count` 가 수정 전 값 대비 감소했는지 비교 (개발 DB 에서).

## 9. 비목표

- 팀 통계 aggregator 의 메모리 사용량 절감 (스펙 B).
- tasklet → chunk-oriented step 전환 (스펙 B).
- `orderBy` 누락 reader, API 실패 삼킴, 알림 재시도 (스펙 C).
- `BrawlerItemOwnershipStatsJob`, `BrawlerSyncJob` 성능 개선 (별도 스펙).

## 10. 오픈 이슈 (플랜 단계에서 확정)

- `StatsReplaceWriter` 를 제네릭 단일 빈으로 둘지, 통계 종류별 별도 빈으로 둘지. 본 스펙 기본 추천은 제네릭 단일 빈.
- Spring Batch job 의 외부 트리거(스케줄러) 가 `run.id` 를 명시적으로 넘기고 있는지 확인 — 고정값이면 `RunIdIncrementer` 효과가 상쇄되므로 트리거 쪽도 같이 조정.
- `stats_rank_battle` 이 파티셔닝 테이블인지와 unique index 재생성 시 파티션 레벨 DDL 필요 여부.
- `BattleKeyEmbedded` 를 `@JvmInline value class` 로 두는 방식이 프로젝트 코드 스타일과 맞는지 — 단순 `object BattleKey { fun forResult(...)/forRank(...) }` 팩토리 object 도 대안.
