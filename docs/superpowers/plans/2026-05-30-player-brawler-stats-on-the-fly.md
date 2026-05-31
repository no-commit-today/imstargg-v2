# 플레이어 브롤러 통계 온더플라이 계산 + Redis 캐시 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> 이 계획 문서는 실행 스킬이 `- [ ]` 체크박스를 파싱하므로 Markdown 으로 작성됨(전역 HTML 규칙의 예외 — 기계 실행용 산출물). 설계 문서는 HTML: `docs/superpowers/specs/2026-05-30-player-brawler-stats-on-the-fly-design.html`.

**Goal:** 플레이어 브롤러 통계를 사전집계 테이블(`player_brawler_stats`/`_item`) 대신 API 호출 시 battle 에서 SQL 집계로 계산하고 redis-core 의 플레이어별 Redis Hash 에 캐싱하며, 플레이어 갱신 시 워커가 캐시를 무효화하도록 전환한다.

**Architecture:** 읽기(core-api)는 `Service → RepositoryWithCache → (redis-core 캐시 스토어 cache-aside | PlayerBrawlerStatsRepository.compute → db-core QueryDSL GROUP BY)`. 쓰기(core-worker)는 `PlayerUpdater` 가 배틀 저장 후 커밋 시점(afterCommit)에 해당 플레이어 캐시를 `DEL`. 키·직렬화는 redis-core 에 단일 정의해 core-api/core-worker 가 공유한다.

**Tech Stack:** Kotlin, Spring Boot 4, Spring Data Redis(StringRedisTemplate), QueryDSL(openfeign), Jackson 3(`tools.jackson`), JUnit5 + AssertJ + MockK(springmockk), H2(test, PostgreSQL 모드).

---

## 실행자를 위한 환경 컨텍스트 (반드시 숙지)

- **테스트는 H2 인메모리**(`test-db` 프로파일, `ddl-auto: create`)로 돈다. 별도 Postgres/Redis 없이 `./gradlew test`(=context 포함, develop 태그 제외)가 CI에서 실행된다.
- **CI에는 Redis가 없다.** 따라서 캐시 스토어 코드는 Redis 장애를 삼켜야 하고(`runCatching`), 테스트는 실 Redis에 의존하지 않는다(직렬화 코덱 순수 테스트 + `StringRedisTemplate` MockK + `@MockkBean` 캐시 스토어).
- **Jackson 3**(`tools.jackson.*`)이 루트 `subprojects` 블록을 통해 **모든 모듈에 이미 제공**된다(`tools.jackson.module:jackson-module-kotlin` + `kotlin-reflect`). redis-core 에서 별도 Jackson 의존 추가 불필요.
- **메인 앱 패키지는 `com.imstargg`** (core-api/core-worker 모두). redis-core 의 `@Component`(`com.imstargg.infrastructure.redis.core.*`)는 컴포넌트 스캔된다.
- **afterCommit 패턴 레퍼런스**: `core-admin/.../ImageStorageServiceExtensions.kt` (`TransactionSynchronizationManager.registerSynchronization` + `runCatching`).
- **QueryDSL 컨벤션**: `JpaRepository` + `...Custom` 인터페이스 + `internal class ...CustomImpl(entityManager)` 프래그먼트. Q-Class는 companion 정적 import 직접 사용. (예: `BrawlerEnemyBattleResultStatsJpaRepositoryCustomImpl`)
- **테스트 컨벤션**: 베이스 클래스 상속, `sut` 변수명, 한글 백틱 메서드명, `// given/when/then`, AssertJ, 픽스처 `aXxx().withYyy().build()`. `@DataJpaTest`(DbCoreContextTest)는 트랜잭션 롤백이라 별도 cleanup 불필요. `@SpringBootTest`(CoreApiContextTest/CoreWorkerContextTest)는 `CleanUp.all()` 사용.
- **빌드/검증 명령**: `./gradlew ktlintFormat` 후 `./gradlew :모듈:test --tests "FQN"` 또는 전체 `./gradlew build`.
- **삭제 대상 클래스 참조처(확인됨)**: `PlayerBrawlerStatsAccumulator`(worker main/test + PlayerUpdater), `PlayerBrawlerStats(Item)Entity`/`...JpaRepository`(db-core + accumulator + core-api `PlayerBrawlerStatsRepository`). 그 외 참조 없음.

---

## 파일 구조 (생성/수정/삭제)

**생성**
- `infrastructure/db-core/.../brawlstars/battle/repository/PlayerBrawlerResultProjection.kt`
- `infrastructure/db-core/.../brawlstars/battle/repository/BattleJpaRepositoryCustom.kt`
- `infrastructure/db-core/.../brawlstars/battle/repository/BattleJpaRepositoryCustomImpl.kt`
- `infrastructure/db-core/src/test/.../brawlstars/battle/repository/BattleJpaRepositoryCustomImplTest.kt`
- `infrastructure/redis-core/.../statistics/player/PlayerBrawlerStatsCacheValue.kt`
- `infrastructure/redis-core/.../statistics/player/PlayerBrawlerStatsCacheCodec.kt`
- `infrastructure/redis-core/.../statistics/player/PlayerBrawlerStatsCacheStore.kt`
- `infrastructure/redis-core/src/test/.../statistics/player/PlayerBrawlerStatsCacheCodecTest.kt`
- `infrastructure/redis-core/src/test/.../statistics/player/PlayerBrawlerStatsCacheStoreTest.kt`
- `core/core-api/.../application/statistics/player/PlayerBrawlerStatsRepositoryWithCache.kt`
- `core/core-api/src/test/.../application/statistics/player/PlayerBrawlerStatsRepositoryTest.kt`
- `core/core-worker/src/test/.../worker/application/PlayerUpdaterCacheEvictionTest.kt`
- `infrastructure/db-core/src/main/resources/db/migration/V260530__drop_player_brawler_stats_tables.sql`

**수정**
- `infrastructure/db-core/.../brawlstars/battle/repository/BattleJpaRepository.kt` (Custom 상속)
- `core/core-api/.../application/statistics/player/PlayerBrawlerStatsRepository.kt` (compute 로 재작성)
- `core/core-api/.../application/statistics/player/PlayerBrawlerStatsService.kt` (시그니처 + 위임)
- `core/core-api/.../api/query/PlayerStatisticsQueryComposer.kt` (OffsetDateTime 전달)
- `core/core-api/src/test/.../application/statistics/player/PlayerBrawlerStatsServiceTest.kt` (재작성)
- `core/core-worker/.../worker/application/PlayerUpdater.kt` (accumulator 제거 + 무효화)
- `core/core-worker/build.gradle.kts` (redis-core 의존)
- `core/core-worker/src/main/resources/application.yml` (redis 프로파일)
- `infrastructure/redis-core/build.gradle.kts` (core-enum 의존)
- `infrastructure/db-core/src/main/resources/schema.sql` (두 테이블 정의 제거)

**삭제**
- `core/core-worker/.../worker/application/PlayerBrawlerStatsAccumulator.kt`
- `core/core-worker/src/test/.../worker/application/PlayerBrawlerStatsAccumulatorTest.kt`
- `infrastructure/db-core/.../statistics/player/PlayerBrawlerStatsEntity.kt`
- `infrastructure/db-core/.../statistics/player/PlayerBrawlerStatsItemEntity.kt`
- `infrastructure/db-core/.../statistics/player/PlayerBrawlerStatsJpaRepository.kt`
- `infrastructure/db-core/.../statistics/player/PlayerBrawlerStatsItemJpaRepository.kt`

> 경로 약어: `com/imstargg` = `.../kotlin/com/imstargg`. 각 Task 에 정확한 절대경로(프로젝트 루트 = `imstargg-backend/`)를 명시한다.

---

## Task 1: db-core 배틀 집계 쿼리 (QueryDSL GROUP BY)

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/PlayerBrawlerResultProjection.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepositoryCustom.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepositoryCustomImpl.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepository.kt`
- Test: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepositoryCustomImplTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepositoryCustomImplTest.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.aBattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.defaultBrawler
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BattleJpaRepositoryCustomImplTest(
    private val sut: BattleJpaRepository,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : DbCoreContextTest() {
    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00")
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00")

    @Test
    fun `본인 브롤러의 결과별 카운트를 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-11T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-12T12:00:00+09:00", "ranked", "defeat", 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            PlayerBrawlerResultProjection(16000000L, "victory", 2L),
            PlayerBrawlerResultProjection(16000000L, "defeat", 1L),
        )
    }

    @Test
    fun `본인 태그가 아닌 팀플레이어 브롤러는 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            sut.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00"))
                    .withType("ranked")
                    .withResult("victory")
                    .withBattleKey("kb")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity().withBattle(battle).withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000111L)).build(),
        )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity().withBattle(battle).withBrawlStarsTag("#OTHER")
                .withBrawler(defaultBrawler(brawlStarsId = 16000999L)).build(),
        )

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000111L, "victory", 1L))
    }

    @Test
    fun `시즌 시각 경계(startAt 이상 endAt 미만)만 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-03-31T23:59:59+09:00", "ranked", "victory", 16000000L) // startAt 이전 → 제외
        saveOwnBattle(player, "2026-04-01T00:00:00+09:00", "ranked", "victory", 16000000L) // startAt 포함
        saveOwnBattle(player, "2026-04-30T23:59:59+09:00", "ranked", "victory", 16000000L) // endAt 직전 포함
        saveOwnBattle(player, "2026-05-01T00:00:00+09:00", "ranked", "victory", 16000000L) // endAt → 제외

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 2L))
    }

    @Test
    fun `요청한 battleType 코드와 다른 배틀은 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-10T13:00:00+09:00", "friendly", "victory", 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    @Test
    fun `result 가 null 인 배틀은 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-10T13:00:00+09:00", "ranked", null, 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    @Test
    fun `다른 플레이어가 소유한 배틀은 제외한다`() {
        // given
        val me = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val other = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#OTHER").build())
        saveOwnBattle(me, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        val otherBattle =
            sut.save(
                aBattleEntity()
                    .withPlayer(other)
                    .withBattleAt(OffsetDateTime.parse("2026-04-11T12:00:00+09:00"))
                    .withType("ranked")
                    .withResult("victory")
                    .withBattleKey("ob")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity().withBattle(otherBattle).withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000000L)).build(),
        )

        // when
        val result = sut.aggregatePlayerBrawlerResults(me.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    private fun saveOwnBattle(
        player: PlayerEntity,
        at: String,
        type: String,
        result: String?,
        brawlerId: Long,
    ): BattleEntity {
        val battle =
            sut.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse(at))
                    .withType(type)
                    .withResult(result)
                    .withBattleKey("k-$at-$brawlerId")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(battle)
                .withBrawlStarsTag(player.brawlStarsTag)
                .withBrawler(defaultBrawler(brawlStarsId = brawlerId))
                .build(),
        )
        return battle
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :infrastructure:db-core:compileTestKotlin`
Expected: FAIL — `aggregatePlayerBrawlerResults` 및 `PlayerBrawlerResultProjection` 미해결.

- [ ] **Step 3: Projection + Custom 인터페이스 생성**

`PlayerBrawlerResultProjection.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

data class PlayerBrawlerResultProjection(
    val brawlerBrawlStarsId: Long,
    val result: String,
    val count: Long,
)
```

`BattleJpaRepositoryCustom.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import java.time.OffsetDateTime

interface BattleJpaRepositoryCustom {
    /**
     * 한 플레이어가 [startAt, endAt) 기간에 치른 [battleTypeCode] 배틀을, 본인이 사용한 브롤러와 결과별로 집계한다.
     *
     * 시즌 판정을 LocalDate 자정이 아닌 시각 경계로 거르는 이유: 과거 PlayerBrawlerStatsAccumulator 가
     * 배틀마다 BrawlPassSeason.of(battleAt) 로 시즌을 판정했으므로, 동일 결과를 내려면 시즌의 실제 시각
     * (OffsetDateTime startAt/endAt) 으로 필터해야 한다.
     *
     * 본인 브롤러는 battle_team_player.brawlstars_tag == playerTag 조인으로 격리하고,
     * 배틀 소유는 battle.player_id == playerId 로 한정한다.
     */
    fun aggregatePlayerBrawlerResults(
        playerId: Long,
        playerTag: String,
        battleTypeCode: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): List<PlayerBrawlerResultProjection>
}
```

- [ ] **Step 4: Custom 구현 생성**

`BattleJpaRepositoryCustomImpl.kt`:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.infrastructure.db.core.brawlstars.battle.QBattleEntity.Companion.battleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.QBattleTeamPlayerEntity.Companion.battleTeamPlayerEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.OffsetDateTime

internal class BattleJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BattleJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun aggregatePlayerBrawlerResults(
        playerId: Long,
        playerTag: String,
        battleTypeCode: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): List<PlayerBrawlerResultProjection> {
        val brawlerId = battleTeamPlayerEntity.brawler.brawlStarsId
        val result = battleEntity.result
        val count = battleTeamPlayerEntity.id.count()

        return queryFactory
            .select(brawlerId, result, count)
            .from(battleTeamPlayerEntity)
            .join(battleTeamPlayerEntity.battle, battleEntity)
            .where(
                battleEntity.player.id.eq(playerId),
                battleTeamPlayerEntity.brawlStarsTag.eq(playerTag),
                battleEntity.type.eq(battleTypeCode),
                battleEntity.battleAt.goe(startAt),
                battleEntity.battleAt.lt(endAt),
                battleEntity.result.isNotNull,
            ).groupBy(brawlerId, result)
            .fetch()
            .map { tuple ->
                PlayerBrawlerResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    result = tuple[result]!!,
                    count = tuple[count]!!,
                )
            }
    }
}
```

> 참고: `from(battleTeamPlayerEntity).join(battleTeamPlayerEntity.battle, battleEntity)` 는 매핑된 `@ManyToOne battle` 연관을 통한 표준 조인이다(미매핑 엔티티 ad-hoc 조인 미사용). Postgres 는 `ix_battle__playerid_battleat`(player_id, battle_at) 선택도가 높아 battle 을 드라이빙으로 잡고 `ix_battleteamplayer__battleid` 로 NL 조인하는 계획을 택한다.

- [ ] **Step 5: BattleJpaRepository 가 Custom 상속하도록 수정**

`BattleJpaRepository.kt` 를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface BattleJpaRepository :
    JpaRepository<BattleEntity, Long>,
    BattleJpaRepositoryCustom {
    fun findTopByPlayerIdOrderByBattleAtDesc(playerId: Long): BattleEntity?

    fun findSliceByPlayerIdOrderByBattleAtDesc(
        playerId: Long,
        pageable: Pageable,
    ): Slice<BattleEntity>
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :infrastructure:db-core:test --tests "com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepositoryCustomImplTest"`
Expected: PASS (6 tests).

- [ ] **Step 7: ktlint 후 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/ \
        infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/brawlstars/battle/repository/BattleJpaRepositoryCustomImplTest.kt
git commit -m "feat(db-core): add player brawler result aggregation query (#38)"
```

---

## Task 2: redis-core 캐시 값 DTO + 직렬화 코덱

**Files:**
- Create: `imstargg-backend/infrastructure/redis-core/src/main/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheValue.kt`
- Create: `imstargg-backend/infrastructure/redis-core/src/main/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheCodec.kt`
- Test: `imstargg-backend/infrastructure/redis-core/src/test/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheCodecTest.kt`

- [ ] **Step 1: 실패하는 코덱 테스트 작성**

`PlayerBrawlerStatsCacheCodecTest.kt`:

```kotlin
package com.imstargg.infrastructure.redis.core.statistics.player

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayerBrawlerStatsCacheCodecTest {
    private val sut = PlayerBrawlerStatsCacheCodec()

    @Test
    fun `인코딩 후 디코딩하면 원본과 같다`() {
        // given
        val value =
            PlayerBrawlerStatsCacheValue(
                endBattleDate = "2026-05-01",
                items =
                    listOf(
                        PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L),
                        PlayerBrawlerStatsCacheValue.Item(16000001L, 2L, 2L, 2L),
                    ),
            )

        // when
        val decoded = sut.decode(sut.encode(value))

        // then
        assertThat(decoded).isEqualTo(value)
    }

    @Test
    fun `빈 items 도 라운드트립된다`() {
        // given
        val value = PlayerBrawlerStatsCacheValue(endBattleDate = "2026-05-01", items = emptyList())

        // when
        val decoded = sut.decode(sut.encode(value))

        // then
        assertThat(decoded).isEqualTo(value)
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :infrastructure:redis-core:compileTestKotlin`
Expected: FAIL — `PlayerBrawlerStatsCacheValue`, `PlayerBrawlerStatsCacheCodec` 미해결.

- [ ] **Step 3: 캐시 값 DTO 생성**

`PlayerBrawlerStatsCacheValue.kt` (wrapper+Item 쌍은 한 파일 — 통계 예외 규약):

```kotlin
package com.imstargg.infrastructure.redis.core.statistics.player

/**
 * Redis Hash 에 저장되는 플레이어 브롤러 통계 캐시 값.
 * 키(playerTag)·필드(battleType/startBattleDate)는 캐시 키에 들어가므로 값에는 담지 않는다.
 * Jackson(Kotlin module)만으로 직렬화되도록 모든 필드를 primitive 로 둔다(날짜는 ISO-8601 문자열).
 */
data class PlayerBrawlerStatsCacheValue(
    val endBattleDate: String,
    val items: List<Item>,
) {
    data class Item(
        val brawlerId: Long,
        val victoryCount: Long,
        val defeatCount: Long,
        val drawCount: Long,
    )
}
```

- [ ] **Step 4: 코덱 생성**

`PlayerBrawlerStatsCacheCodec.kt`:

```kotlin
package com.imstargg.infrastructure.redis.core.statistics.player

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Component
class PlayerBrawlerStatsCacheCodec {
    private val mapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .build()

    fun encode(value: PlayerBrawlerStatsCacheValue): String = mapper.writeValueAsString(value)

    fun decode(json: String): PlayerBrawlerStatsCacheValue =
        mapper.readValue(json, PlayerBrawlerStatsCacheValue::class.java)
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :infrastructure:redis-core:test --tests "com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheCodecTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: ktlint 후 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add infrastructure/redis-core/src/main/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/ \
        infrastructure/redis-core/src/test/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheCodecTest.kt
git commit -m "feat(redis-core): add player brawler stats cache value + codec (#38)"
```

---

## Task 3: redis-core 캐시 스토어 (Hash find/put/evict)

**Files:**
- Modify: `imstargg-backend/infrastructure/redis-core/build.gradle.kts`
- Create: `imstargg-backend/infrastructure/redis-core/src/main/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheStore.kt`
- Test: `imstargg-backend/infrastructure/redis-core/src/test/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheStoreTest.kt`

- [ ] **Step 1: redis-core 가 core-enum 에 의존하도록 build.gradle 수정**

`infrastructure/redis-core/build.gradle.kts` 를 아래로 교체:

```kotlin
dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.apache.commons:commons-pool2")
    implementation(project(":core:core-enum"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
}
```

- [ ] **Step 2: 실패하는 스토어 테스트 작성 (StringRedisTemplate MockK)**

`PlayerBrawlerStatsCacheStoreTest.kt`:

```kotlin
package com.imstargg.infrastructure.redis.core.statistics.player

import com.imstargg.core.enums.BattleType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.LocalDate

class PlayerBrawlerStatsCacheStoreTest {
    private val redisTemplate = mockk<StringRedisTemplate>()
    private val hashOps = mockk<HashOperations<String, String, String>>()
    private val codec = PlayerBrawlerStatsCacheCodec()
    private val sut = PlayerBrawlerStatsCacheStore(redisTemplate, codec)

    private val date = LocalDate.of(2026, 5, 1)
    private val value =
        PlayerBrawlerStatsCacheValue(
            endBattleDate = "2026-05-31",
            items = listOf(PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L)),
        )

    @Test
    fun `find 는 키와 필드로 조회해 디코딩한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every {
            hashOps.get("playerBrawlerStats:v1:#TAG", "RANKED:2026-05-01")
        } returns codec.encode(value)

        // when
        val result = sut.find("#TAG", BattleType.RANKED, date)

        // then
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `find 결과가 없으면 null 을 반환한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.get(any(), any()) } returns null

        // when & then
        assertThat(sut.find("#TAG", BattleType.RANKED, date)).isNull()
    }

    @Test
    fun `find 중 예외가 나면 null 을 반환한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } throws RuntimeException("redis down")

        // when & then
        assertThat(sut.find("#TAG", BattleType.RANKED, date)).isNull()
    }

    @Test
    fun `put 은 직렬화해 저장하고 TTL 을 설정한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.put(any(), any(), any()) } returns Unit
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true

        // when
        sut.put("#TAG", BattleType.RANKED, date, value)

        // then
        verify { hashOps.put("playerBrawlerStats:v1:#TAG", "RANKED:2026-05-01", codec.encode(value)) }
        verify { redisTemplate.expire("playerBrawlerStats:v1:#TAG", Duration.ofHours(1)) }
    }

    @Test
    fun `evict 는 플레이어 키를 삭제한다`() {
        // given
        every { redisTemplate.delete("playerBrawlerStats:v1:#TAG") } returns true

        // when
        sut.evict("#TAG")

        // then
        verify { redisTemplate.delete("playerBrawlerStats:v1:#TAG") }
    }

    @Test
    fun `evict 중 예외가 나도 전파하지 않는다`() {
        // given
        every { redisTemplate.delete(any<String>()) } throws RuntimeException("redis down")

        // when & then (예외 미전파)
        sut.evict("#TAG")
    }
}
```

- [ ] **Step 3: 컴파일 실패 확인**

Run: `./gradlew :infrastructure:redis-core:compileTestKotlin`
Expected: FAIL — `PlayerBrawlerStatsCacheStore` 미해결.

- [ ] **Step 4: 캐시 스토어 구현**

`PlayerBrawlerStatsCacheStore.kt`:

```kotlin
package com.imstargg.infrastructure.redis.core.statistics.player

import com.imstargg.core.enums.BattleType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

/**
 * 플레이어 브롤러 통계 캐시. 플레이어별 Redis Hash 로 보관한다.
 * - key  : playerBrawlerStats:v1:{playerTag}
 * - field: {battleType.name}:{startBattleDate}
 * 플레이어 갱신 시 evict(=DEL) 한 번으로 그 플레이어의 모든 시즌·타입 캐시를 제거한다.
 * Redis 장애는 모두 삼켜 호출측(읽기 재계산, 쓰기 무효화)을 깨뜨리지 않는다.
 */
@Component
class PlayerBrawlerStatsCacheStore(
    private val redisTemplate: StringRedisTemplate,
    private val codec: PlayerBrawlerStatsCacheCodec,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun find(
        playerTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): PlayerBrawlerStatsCacheValue? =
        runCatching {
            redisTemplate
                .opsForHash<String, String>()
                .get(key(playerTag), field(battleType, startBattleDate))
                ?.let { codec.decode(it) }
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] find 실패 tag={}", playerTag, it) }
            .getOrNull()

    fun put(
        playerTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
        value: PlayerBrawlerStatsCacheValue,
    ) {
        runCatching {
            val key = key(playerTag)
            redisTemplate.opsForHash<String, String>().put(key, field(battleType, startBattleDate), codec.encode(value))
            redisTemplate.expire(key, TTL)
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] put 실패 tag={}", playerTag, it) }
    }

    fun evict(playerTag: String) {
        runCatching {
            redisTemplate.delete(key(playerTag))
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] evict 실패 tag={}", playerTag, it) }
    }

    private fun key(playerTag: String): String = "$KEY_PREFIX$playerTag"

    private fun field(
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): String = "${battleType.name}:$startBattleDate"

    companion object {
        private const val KEY_PREFIX = "playerBrawlerStats:v1:"
        private val TTL: Duration = Duration.ofHours(1)
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :infrastructure:redis-core:test --tests "com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStoreTest"`
Expected: PASS (6 tests).

- [ ] **Step 6: ktlint 후 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add infrastructure/redis-core/build.gradle.kts \
        infrastructure/redis-core/src/main/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheStore.kt \
        infrastructure/redis-core/src/test/kotlin/com/imstargg/infrastructure/redis/core/statistics/player/PlayerBrawlerStatsCacheStoreTest.kt
git commit -m "feat(redis-core): add player brawler stats cache store (#38)"
```

---

## Task 4: core-api 읽기 경로 전환 (compute + WithCache + Service + QueryComposer)

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/player/PlayerBrawlerStatsRepository.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/player/PlayerBrawlerStatsRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/player/PlayerBrawlerStatsService.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/query/PlayerStatisticsQueryComposer.kt`
- Test (new): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/player/PlayerBrawlerStatsRepositoryTest.kt`
- Test (rewrite): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/player/PlayerBrawlerStatsServiceTest.kt`

- [ ] **Step 1: compute 리포지토리 테스트 작성 (실DB, H2)**

`PlayerBrawlerStatsRepositoryTest.kt` (새 파일):

```kotlin
package com.imstargg.core.application.statistics.player

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.aBattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.defaultBrawler
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PlayerBrawlerStatsRepositoryTest(
    private val sut: PlayerBrawlerStatsRepository,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : CoreApiContextTest() {
    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00")
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00")

    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `플레이어가 없으면 입력값으로 채운 빈 stats 를 반환한다`() {
        // when
        val result = sut.compute(BrawlStarsTag("#NONE"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.playerTag).isEqualTo(BrawlStarsTag("#NONE"))
        assertThat(result.battleType).isEqualTo(BattleType.RANKED)
        assertThat(result.startBattleDate).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.endBattleDate).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `본인 브롤러의 승무패를 ResultCount 로 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            battleJpaRepository.save(
                aBattleEntity().withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00"))
                    .withType("ranked").withResult("victory").withBattleKey("k1").build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity().withBattle(battle).withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000000L)).build(),
        )

        // when
        val result = sut.compute(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(item.resultCount.victoryCount).isEqualTo(1L)
        assertThat(item.resultCount.defeatCount).isEqualTo(0L)
        assertThat(item.resultCount.drawCount).isEqualTo(0L)
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `./gradlew :core:core-api:compileTestKotlin`
Expected: FAIL — `PlayerBrawlerStatsRepository.compute` 미해결.

- [ ] **Step 3: PlayerBrawlerStatsRepository 를 compute 로 재작성**

`PlayerBrawlerStatsRepository.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Repository
@Transactional(readOnly = true)
class PlayerBrawlerStatsRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
) {
    fun compute(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats {
        val playerId = playerJpaRepository.findByBrawlStarsTag(playerTag.value)?.id
        val items =
            if (playerId == null) {
                emptyList()
            } else {
                aggregate(playerId, playerTag.value, battleType, startAt, endAt)
            }
        return PlayerBrawlerStats(
            playerTag = playerTag,
            battleType = battleType,
            startBattleDate = startAt.toLocalDate(),
            endBattleDate = endAt.toLocalDate(),
            items = items,
        )
    }

    private fun aggregate(
        playerId: Long,
        playerTag: String,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): List<PlayerBrawlerStatsItem> =
        battleJpaRepository
            .aggregatePlayerBrawlerResults(
                playerId = playerId,
                playerTag = playerTag,
                battleTypeCode = battleType.code,
                startAt = startAt,
                endAt = endAt,
            ).groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerId, rows) ->
                var victory = 0L
                var defeat = 0L
                var draw = 0L
                rows.forEach { row ->
                    when (EnumCodeType.findByCode<BattleResult>(row.result)) {
                        BattleResult.VICTORY -> victory += row.count
                        BattleResult.DEFEAT -> defeat += row.count
                        BattleResult.DRAW -> draw += row.count
                        null -> {}
                    }
                }
                PlayerBrawlerStatsItem(
                    brawlerId = BrawlStarsId(brawlerId),
                    resultCount = ResultCount(victory, defeat, draw),
                )
            }
}
```

- [ ] **Step 4: (체크포인트) 아직 모듈 컴파일 불가 — 다음 단계 계속**

`PlayerBrawlerStatsRepository.find()` 가 사라져 옛 `PlayerBrawlerStatsService`/`QueryComposer` 가 깨진다. 읽기 경로는 상호의존적이라 **한 커밋으로 전환**한다 — Step 5~7 까지 마친 뒤 Step 9 에서 한 번에 테스트한다. 여기서는 빌드/테스트를 실행하지 않는다.

- [ ] **Step 5: WithCache 생성 (cache-aside + 도메인 매핑)**

`PlayerBrawlerStatsRepositoryWithCache.kt` (새 파일):

```kotlin
package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheValue
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 모듈 간(core-api 읽기/쓰기, core-worker 무효화) 키 공유를 위해 Spring @Cacheable 대신
 * redis-core 의 명시적 캐시 스토어로 cache-aside 한다. (기존 *WithCache 패턴의 의도된 변형)
 */
@Repository
class PlayerBrawlerStatsRepositoryWithCache(
    private val playerBrawlerStatsRepository: PlayerBrawlerStatsRepository,
    private val cacheStore: PlayerBrawlerStatsCacheStore,
) {
    fun find(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats {
        val startBattleDate = startAt.toLocalDate()
        cacheStore.find(playerTag.value, battleType, startBattleDate)?.let { cached ->
            return cached.toDomain(playerTag, battleType, startBattleDate)
        }
        val computed = playerBrawlerStatsRepository.compute(playerTag, battleType, startAt, endAt)
        cacheStore.put(playerTag.value, battleType, startBattleDate, computed.toCacheValue())
        return computed
    }
}

private fun PlayerBrawlerStatsCacheValue.toDomain(
    playerTag: BrawlStarsTag,
    battleType: BattleType,
    startBattleDate: LocalDate,
): PlayerBrawlerStats =
    PlayerBrawlerStats(
        playerTag = playerTag,
        battleType = battleType,
        startBattleDate = startBattleDate,
        endBattleDate = LocalDate.parse(endBattleDate),
        items =
            items.map { item ->
                PlayerBrawlerStatsItem(
                    brawlerId = BrawlStarsId(item.brawlerId),
                    resultCount = ResultCount(item.victoryCount, item.defeatCount, item.drawCount),
                )
            },
    )

private fun PlayerBrawlerStats.toCacheValue(): PlayerBrawlerStatsCacheValue =
    PlayerBrawlerStatsCacheValue(
        endBattleDate = endBattleDate.toString(),
        items =
            items.map { item ->
                PlayerBrawlerStatsCacheValue.Item(
                    brawlerId = item.brawlerId.value,
                    victoryCount = item.resultCount.victoryCount,
                    defeatCount = item.resultCount.defeatCount,
                    drawCount = item.resultCount.drawCount,
                )
            },
    )
```

- [ ] **Step 6: Service 시그니처 변경 + WithCache 위임**

`PlayerBrawlerStatsService.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.enums.BattleType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PlayerBrawlerStatsService(
    private val playerBrawlerStatsRepositoryWithCache: PlayerBrawlerStatsRepositoryWithCache,
) {
    fun get(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats =
        playerBrawlerStatsRepositoryWithCache.find(
            playerTag = playerTag,
            battleType = battleType,
            startAt = startAt,
            endAt = endAt,
        )
}
```

- [ ] **Step 7: QueryComposer 가 OffsetDateTime(시즌 시각 경계)을 전달하도록 수정**

`PlayerStatisticsQueryComposer.kt` 전체 교체:

```kotlin
package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerRoleStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerRoleStatisticsResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerStatisticsResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.player.PlayerBrawlerStatsService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PlayerStatisticsQueryComposer(
    private val playerBrawlerStatsService: PlayerBrawlerStatsService,
    private val brawlerService: BrawlerService,
) {
    fun getRankedBrawlerStatistics(
        playerTag: BrawlStarsTag,
        season: BrawlPassSeason,
    ): PlayerBrawlerStatisticsResponse =
        buildBrawlerStatistics(
            playerTag = playerTag,
            battleType = BattleType.RANKED,
            startAt = season.startAt,
            endAt = season.endAt,
        )

    fun getSoloRankedBrawlerStatistics(
        playerTag: BrawlStarsTag,
        season: SoloRankSeason,
    ): PlayerBrawlerStatisticsResponse =
        buildBrawlerStatistics(
            playerTag = playerTag,
            battleType = BattleType.SOLO_RANKED,
            startAt = season.startAt,
            endAt = season.endAt,
        )

    fun getRankedBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        season: BrawlPassSeason,
    ): PlayerBrawlerRoleStatisticsResponse =
        buildBrawlerRoleStatistics(
            playerTag = playerTag,
            battleType = BattleType.RANKED,
            startAt = season.startAt,
            endAt = season.endAt,
        )

    fun getSoloRankedBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        season: SoloRankSeason,
    ): PlayerBrawlerRoleStatisticsResponse =
        buildBrawlerRoleStatistics(
            playerTag = playerTag,
            battleType = BattleType.SOLO_RANKED,
            startAt = season.startAt,
            endAt = season.endAt,
        )

    private fun buildBrawlerStatistics(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStatisticsResponse {
        val stats =
            playerBrawlerStatsService.get(
                playerTag = playerTag,
                battleType = battleType,
                startAt = startAt,
                endAt = endAt,
            )
        val items = stats.items
        val brawlers = Brawlers(brawlerService.getAll())
        val responseItems =
            items.map { item ->
                PlayerBrawlerStatisticsItemResponse(
                    brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                    totalBattleCount = item.resultCount.totalCount,
                    victoryCount = item.resultCount.victoryCount,
                    defeatCount = item.resultCount.defeatCount,
                    drawCount = item.resultCount.drawCount,
                    winRate = item.resultCount.winRate,
                )
            }
        return PlayerBrawlerStatisticsResponse(
            startBattleDate = stats.startBattleDate,
            endBattleDate = stats.endBattleDate,
            totalBattleCount = items.sumOf { it.resultCount.totalCount },
            items = responseItems,
        )
    }

    private fun buildBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerRoleStatisticsResponse {
        val stats =
            playerBrawlerStatsService.get(
                playerTag = playerTag,
                battleType = battleType,
                startAt = startAt,
                endAt = endAt,
            )
        val items = stats.items
        val roleByBrawlerId = brawlerService.getAll().associate { it.id to it.role }
        val countsByRole: Map<BrawlerRole, ResultCount> =
            items
                .mapNotNull { item -> roleByBrawlerId[item.brawlerId]?.let { it to item } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, sameRoleItems) -> sameRoleItems.sumResultCount() }
        val responseItems =
            BrawlerRole.entries.map { role ->
                val count = countsByRole[role] ?: ResultCount(0, 0, 0)
                PlayerBrawlerRoleStatisticsItemResponse(
                    role = role,
                    totalBattleCount = count.totalCount,
                    victoryCount = count.victoryCount,
                    defeatCount = count.defeatCount,
                    drawCount = count.drawCount,
                    winRate = count.winRate,
                )
            }
        return PlayerBrawlerRoleStatisticsResponse(
            startBattleDate = stats.startBattleDate,
            endBattleDate = stats.endBattleDate,
            totalBattleCount = items.sumOf { it.resultCount.totalCount },
            items = responseItems,
        )
    }

    private fun List<PlayerBrawlerStatsItem>.sumResultCount(): ResultCount =
        fold(ResultCount(0, 0, 0)) { acc, item -> acc.merge(item.resultCount) }
}
```

- [ ] **Step 8: Service 테스트 재작성 (cache-aside, @MockkBean 캐시 스토어)**

`PlayerBrawlerStatsServiceTest.kt` 전체 교체:

```kotlin
package com.imstargg.core.application.statistics.player

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.aBattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.defaultBrawler
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class PlayerBrawlerStatsServiceTest(
    private val sut: PlayerBrawlerStatsService,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : CoreApiContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var cacheStore: PlayerBrawlerStatsCacheStore

    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00")
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00")

    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `캐시 미스면 battle 에서 계산하고 캐시에 저장한다`() {
        // given (relaxed MockkBean: find 는 기본 null 반환 = 캐시 미스)
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            battleJpaRepository.save(
                aBattleEntity().withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00"))
                    .withType("ranked").withResult("victory").withBattleKey("k1").build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity().withBattle(battle).withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000000L)).build(),
        )

        // when
        val result = sut.get(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(result.items.first().resultCount.victoryCount).isEqualTo(1L)
        verify { cacheStore.put("#ME", BattleType.RANKED, startAt.toLocalDate(), any()) }
    }

    @Test
    fun `캐시 히트면 계산하지 않고 캐시 값을 반환한다`() {
        // given (DB 에는 배틀이 없지만 캐시가 값을 돌려줌 = 계산을 거치지 않음)
        every {
            cacheStore.find("#ME", BattleType.RANKED, startAt.toLocalDate())
        } returns
            PlayerBrawlerStatsCacheValue(
                endBattleDate = "2026-05-01",
                items = listOf(PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L)),
            )

        // when
        val result = sut.get(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(result.items.first().resultCount.victoryCount).isEqualTo(5L)
        assertThat(result.items.first().resultCount.defeatCount).isEqualTo(1L)
        verify(exactly = 0) { cacheStore.put(any(), any(), any(), any()) }
    }

    @Test
    fun `배틀이 없으면 빈 items 를 반환한다`() {
        // when
        val result = sut.get(BrawlStarsTag("#NONE"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).isEmpty()
        assertThat(result.startBattleDate).isEqualTo(startAt.toLocalDate())
        assertThat(result.endBattleDate).isEqualTo(endAt.toLocalDate())
    }
}
```

- [ ] **Step 9: core-api 전체 테스트 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.statistics.player.*"`
Expected: PASS (PlayerBrawlerStatsRepositoryTest 2 + PlayerBrawlerStatsServiceTest 3).

- [ ] **Step 10: ktlint 후 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/player/ \
        core/core-api/src/main/kotlin/com/imstargg/core/api/query/PlayerStatisticsQueryComposer.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/player/
git commit -m "feat(core-api): compute player brawler stats on-the-fly with redis cache (#38)"
```

---

## Task 5: core-worker 무효화 전환 (accumulator 제거 + afterCommit evict)

**Files:**
- Modify: `imstargg-backend/core/core-worker/build.gradle.kts`
- Modify: `imstargg-backend/core/core-worker/src/main/resources/application.yml`
- Modify: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerUpdater.kt`
- Delete: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerBrawlerStatsAccumulator.kt`
- Delete: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerBrawlerStatsAccumulatorTest.kt`
- Test (new): `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerUpdaterCacheEvictionTest.kt`

- [ ] **Step 1: core-worker 가 redis-core 에 의존하도록 build.gradle 수정**

`core/core-worker/build.gradle.kts` 의 `dependencies` 블록에 redis-core 한 줄 추가 (db-core 다음 줄):

```kotlin
    implementation(project(":infrastructure:db-core"))
    implementation(project(":infrastructure:redis-core"))
    implementation(project(":infrastructure:aws-sqs"))
```

- [ ] **Step 2: worker 가 redis 설정을 로드하도록 application.yml 수정**

`core/core-worker/src/main/resources/application.yml` 의 `profiles.group` 을 아래로 교체(각 그룹에 `redis` 추가):

```yaml
  profiles:
    active: local
    group:
      local:     [db, aws-sqs, redis]
      local-dev: [db, aws-sqs, redis]
      dev:       [db, aws-sqs, redis]
      prod:      [db, aws-sqs, redis]
```

- [ ] **Step 3: 무효화 테스트 작성 (@MockkBean 캐시 스토어)**

`PlayerUpdaterCacheEvictionTest.kt` (새 파일):

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PlayerUpdaterCacheEvictionTest(
    private val sut: PlayerUpdater,
    private val cleanUp: CleanUp,
    private val playerRepository: PlayerJpaRepository,
) : CoreWorkerContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var cacheStore: PlayerBrawlerStatsCacheStore

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `신규 배틀이 있으면 커밋 후 해당 플레이어 캐시를 무효화한다`() {
        // given
        val tag = "#EVICT1"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        // when
        sut.update(
            createPlayerResponse(tag),
            listOf(createBattle(tag, OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC))),
        )

        // then
        verify(exactly = 1) { cacheStore.evict(tag) }
    }

    @Test
    fun `신규 배틀이 없으면 캐시를 무효화하지 않는다`() {
        // given
        val tag = "#EVICT2"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        // when
        sut.update(createPlayerResponse(tag), emptyList())

        // then
        verify(exactly = 0) { cacheStore.evict(any()) }
    }

    private fun createPlayerResponse(tag: String) =
        PlayerResponse(
            tag = tag,
            name = "Test",
            nameColor = "0xff0000ff",
            icon = PlayerIconResponse(id = 1L),
            trophies = 1000,
            highestTrophies = 1200,
            expLevel = 50,
            expPoints = 5000,
            isQualifiedFromChampionshipChallenge = false,
            victories3vs3 = 10,
            victoriesSolo = 5,
            victoriesDuo = 3,
            bestRoboRumbleTime = 0,
            bestTimeAsBigBrawler = 0,
            totalPrestigeLevel = 0,
            club = PlayerClubResponse(tag = "#CLUBTEST", name = "Test Club"),
            brawlers = emptyList<BrawlerStatResponse>(),
        )

    private fun createBattle(
        playerBrawlStarsTag: String,
        battleAt: OffsetDateTime,
    ) = Battle(
        battleKey = "battle_key_${battleAt.toEpochSecond()}",
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 1L,
                modeBrawlStarsName = "knockout",
                modeBrawlStarsId = 1,
                map = "Test Map",
            ),
        mode = "knockout",
        type = "ranked",
        result = "victory",
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = null,
        trophyChange = 8,
        teams = emptyList(),
    )
}
```

- [ ] **Step 4: 테스트 실패 확인 (PlayerUpdater 미수정)**

Run: `./gradlew :core:core-worker:compileTestKotlin`
Expected: FAIL — `PlayerUpdater` 생성자에 아직 `playerBrawlerStatsCacheStore` 가 없음(현재는 accumulator). 다음 단계에서 교체.

- [ ] **Step 5: PlayerUpdater 를 accumulator 제거 + afterCommit evict 로 교체**

`PlayerUpdater.kt` 전체 교체:

```kotlin
package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.Player
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class PlayerUpdater(
    private val playerRepository: PlayerRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
    private val playerBrawlerStatsCacheStore: PlayerBrawlerStatsCacheStore,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PlayerUpdater::class.java)
    }

    @Transactional
    fun update(
        playerResponse: PlayerResponse,
        battles: List<Battle>,
    ): List<Battle> {
        val player = findOrCreate(playerResponse)
        player.update(playerResponse)
        playerRepository.save(player)
        val newBattles = filterNewBattles(player.entity, battles)
        log.debug("[플레이어 갱신] 신규 배틀 {}건 tag={}", newBattles.size, playerResponse.tag)
        updateBattles(player.entity, newBattles)
        if (newBattles.isNotEmpty()) {
            evictBrawlerStatsCacheAfterCommit(player.entity.brawlStarsTag)
        }
        return newBattles
    }

    @Transactional
    fun updatePlayer(response: PlayerResponse) {
        val player = findOrCreate(response)
        player.update(response)
        playerRepository.save(player)
    }

    /**
     * 신규 배틀 반영 후 그 플레이어의 브롤러 통계 캐시를 무효화한다.
     * 커밋 전에 DEL 하면 동시 읽기가 신규 배틀 미반영 상태로 재적재할 수 있어 afterCommit 으로 순서를 보장한다.
     */
    private fun evictBrawlerStatsCacheAfterCommit(playerTag: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        playerBrawlerStatsCacheStore.evict(playerTag)
                    }
                },
            )
        } else {
            playerBrawlerStatsCacheStore.evict(playerTag)
        }
    }

    private fun filterNewBattles(
        player: PlayerEntity,
        battles: List<Battle>,
    ): List<Battle> {
        val latestBattleTime =
            battleJpaRepository
                .findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
                ?.battleAt
                ?: return battles
        return battles.filter { it.battleAt.isAfter(latestBattleTime) }
    }

    private fun updateBattles(
        player: PlayerEntity,
        newBattles: List<Battle>,
    ) {
        val battleEntities =
            newBattles.map { battle ->
                BattleEntity(
                    battleKey = battle.battleKey,
                    battleAt = battle.battleAt,
                    event =
                        battle.event?.let {
                            BattleEntityEvent(
                                brawlStarsId = it.brawlStarsId,
                                modeBrawlStarsName = it.modeBrawlStarsName,
                                modeBrawlStarsId = it.modeBrawlStarsId,
                                map = it.map,
                            )
                        },
                    mode = battle.mode,
                    type = battle.type,
                    result = battle.result,
                    duration = battle.duration,
                    starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
                    player = player,
                    battleRank = battle.battleRank,
                    trophyChange = battle.trophyChange,
                )
            }
        battleJpaRepository.saveAll(battleEntities)

        val teamPlayerEntities =
            newBattles.zip(battleEntities).flatMap { (battle, battleEntity) ->
                battle.teams.flatMapIndexed { teamIdx, team ->
                    team.mapIndexed { playerIdx, tp ->
                        BattleTeamPlayerEntity(
                            battle = battleEntity,
                            teamIdx = teamIdx,
                            playerIdx = playerIdx,
                            brawlStarsTag = tp.brawlStarsTag,
                            name = tp.name,
                            brawler =
                                BattleTeamPlayerEntityBrawler(
                                    brawlStarsId = tp.brawler.brawlStarsId,
                                    name = tp.brawler.name,
                                    power = tp.brawler.power,
                                    trophies = tp.brawler.trophies,
                                    trophyChange = tp.brawler.trophyChange,
                                ),
                        )
                    }
                }
            }
        battleTeamPlayerJpaRepository.saveAll(teamPlayerEntities)
    }

    private fun findOrCreate(response: PlayerResponse): Player =
        playerRepository.findWithLockByBrawlStarsTag(response.tag)
            ?: Player.create(response)
}
```

- [ ] **Step 6: accumulator + accumulator 테스트 삭제**

```bash
cd imstargg-backend
git rm core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/PlayerBrawlerStatsAccumulator.kt \
       core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/PlayerBrawlerStatsAccumulatorTest.kt
```

- [ ] **Step 7: worker 테스트 통과 확인**

Run: `./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.PlayerUpdater*"`
Expected: PASS — `PlayerUpdaterCacheEvictionTest`(2) + 기존 `PlayerUpdaterUpdateBattlesTest`(3) + `PlayerUpdaterConcurrencyTest`(4) 모두 통과. (기존 두 테스트는 코드 변경 없이 통과: afterCommit evict 는 실 캐시 스토어로 가지만 CI에 Redis 가 없어 내부에서 삼켜짐.)

- [ ] **Step 8: ktlint 후 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-worker/
git commit -m "feat(core-worker): evict player brawler stats cache on update, drop accumulator (#38)"
```

---

## Task 6: db-core 통계 엔티티/리포지토리 삭제 + 마이그레이션 + 스키마

> 이 시점에 `PlayerBrawlerStats(Item)Entity`/`...JpaRepository` 를 참조하는 코드는 더 이상 없다(Task 4·5 에서 제거됨). 안전하게 삭제한다.

**Files:**
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsEntity.kt`
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsItemEntity.kt`
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsJpaRepository.kt`
- Delete: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsItemJpaRepository.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260530__drop_player_brawler_stats_tables.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`

- [ ] **Step 1: 엔티티·리포지토리 4개 삭제**

```bash
cd imstargg-backend
git rm infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsEntity.kt \
       infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsItemEntity.kt \
       infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsJpaRepository.kt \
       infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/player/PlayerBrawlerStatsItemJpaRepository.kt
```

- [ ] **Step 2: DROP 마이그레이션 생성**

`infrastructure/db-core/src/main/resources/db/migration/V260530__drop_player_brawler_stats_tables.sql`:

```sql
-- V260530__drop_player_brawler_stats_tables.sql
-- 플레이어 브롤러 통계 사전집계 테이블 제거 (#38)
-- API 호출 시 battle 에서 온더플라이 계산 + Redis 캐시로 대체. battle 영구 보존이라 손실 없음.
drop table if exists player_brawler_stats_item;
drop table if exists player_brawler_stats;
```

- [ ] **Step 3: schema.sql 에서 두 테이블 정의 제거**

`infrastructure/db-core/src/main/resources/schema.sql` 에서 아래 블록(주석 포함, `brawler_buffy_ownership_stats` 다음 ~ `feedback` 테이블 직전)을 통째로 삭제:

```sql
-- player_brawler_stats / player_brawler_stats_item tables
create table if not exists player_brawler_stats
(
    player_brawler_stats_id bigint generated always as identity primary key,
    player_brawlstars_tag   varchar(45) not null,
    battle_type             varchar(45) not null,
    start_battle_date       date        not null,
    end_battle_date         date        not null,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_stats
    on player_brawler_stats (player_brawlstars_tag, battle_type, start_battle_date);

create table if not exists player_brawler_stats_item
(
    player_brawler_stats_item_id bigint generated always as identity primary key,
    player_brawler_stats_id      bigint      not null,
    brawler_brawlstars_id        bigint      not null,
    victory_count                bigint      not null,
    defeat_count                 bigint      not null,
    draw_count                   bigint      not null,
    total_count                  bigint      not null,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_stats_item
    on player_brawler_stats_item (player_brawler_stats_id, brawler_brawlstars_id);

```

- [ ] **Step 4: db-core 컴파일 + 전체 테스트 확인**

Run: `./gradlew :infrastructure:db-core:test`
Expected: PASS (삭제된 엔티티 참조 없음, 신규 집계 쿼리 테스트 통과).

- [ ] **Step 5: 커밋**

```bash
cd imstargg-backend
git add infrastructure/db-core/src/main/resources/db/migration/V260530__drop_player_brawler_stats_tables.sql \
        infrastructure/db-core/src/main/resources/schema.sql
git commit -m "feat(db-core): drop player_brawler_stats tables (#38)"
```

---

## Task 7: 전체 빌드·정합성 최종 검증

**Files:** (없음 — 검증 전용)

- [ ] **Step 1: ktlint 전체 검사**

Run: `cd imstargg-backend && ./gradlew ktlintCheck`
Expected: PASS (위반 시 `./gradlew ktlintFormat` 후 변경 커밋).

- [ ] **Step 2: 전체 빌드 + 테스트**

Run: `cd imstargg-backend && ./gradlew build`
Expected: PASS — 전 모듈 컴파일 + 테스트 통과. 특히:
- `:infrastructure:db-core:test` (집계 쿼리)
- `:infrastructure:redis-core:test` (코덱·스토어)
- `:core:core-api:test` (compute·service)
- `:core:core-worker:test` (무효화·기존 PlayerUpdater)

- [ ] **Step 3: 삭제 대상 잔여 참조 0건 확인**

Run:
```bash
cd imstargg-backend && grep -rn "PlayerBrawlerStatsAccumulator\|PlayerBrawlerStatsEntity\|PlayerBrawlerStatsItemEntity\|PlayerBrawlerStatsJpaRepository\|PlayerBrawlerStatsItemJpaRepository" --include="*.kt" . || echo "참조 없음 (정상)"
```
Expected: "참조 없음 (정상)".

- [ ] **Step 4: 최종 상태 확인 (추가 변경 없으면 커밋 불필요)**

Run: `cd imstargg-backend && git status --short`
Expected: 변경 없음(클린). 잔여 변경 있으면 `chore: ...` 로 커밋.

---

## Self-Review (작성자 점검 결과)

**1. 스펙 커버리지** — 설계(HTML) 각 섹션 대응:
- §3.1 일괄 제거 → Task 5·6 (accumulator/엔티티/리포지토리/테이블 한 사이클에 제거).
- §3.2 SQL GROUP BY 집계 → Task 1.
- §3.3 redis-core Hash 스토어 → Task 2·3.
- §4 데이터 흐름(읽기 cache-aside / 쓰기 afterCommit) → Task 4·5.
- §5.A 집계 쿼리 → Task 1. §5.B 스토어/코덱 → Task 2·3. §5.C core-api 배선 → Task 4. §5.D 무효화 → Task 5. §5.E 제거 → Task 5·6.
- §6 정합성(시즌 시각 경계, result 매핑, cache-aside) → Task 1(경계·result)·Task 4(매핑)·Task 5(afterCommit).
- §7 테스트 → 각 Task 의 테스트 단계(집계 6, 코덱 2, 스토어 6, compute 2, service 3, 무효화 2).

**2. Placeholder 스캔** — "TBD/적절히/추후" 등 없음. 모든 코드 단계에 완전한 코드 포함.

**3. 타입 일관성** — 시그니처 일관 확인:
- `aggregatePlayerBrawlerResults(playerId: Long, playerTag: String, battleTypeCode: String, startAt, endAt: OffsetDateTime)` — Task 1 정의 ↔ Task 4 호출 일치.
- `PlayerBrawlerStatsCacheStore.find/put(playerTag: String, battleType: BattleType, startBattleDate: LocalDate, ...)` / `evict(playerTag: String)` — Task 3 정의 ↔ Task 4·5 호출 일치.
- `PlayerBrawlerStatsService.get(playerTag, battleType, startAt, endAt: OffsetDateTime)` — Task 4 정의 ↔ QueryComposer 호출 일치.
- `PlayerBrawlerStatsCacheValue(endBattleDate: String, items: List<Item(brawlerId/victory/defeat/drawCount: Long)>)` — Task 2 정의 ↔ Task 3·4 사용 일치.
- `ResultCount(Long,Long,Long)`, `BrawlStarsId(val value: Long)` — 실제 정의와 일치.

**4. 순서/그린 보장** — 각 Task 커밋이 컴파일·테스트 그린: Task1~3 가산적, Task4 가 읽기경로 한 커밋 전환, Task5 가 워커 전환+accumulator 삭제, Task6 가 미참조 엔티티 삭제. 삭제 시점에 잔여 참조 없음(Task7 Step3 로 확증).
