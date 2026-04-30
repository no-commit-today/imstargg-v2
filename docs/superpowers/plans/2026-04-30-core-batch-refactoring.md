# Core Batch Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `core-batch` jobs so JobConfig classes stay thin while statistics jobs get safer paging, simpler execution components, and managed transaction boundaries.

**Architecture:** Keep Spring Batch configuration classes as wiring-only classes. Move API collection, change detection, ownership counting, and statistics orchestration into focused components. Preserve the intentional `BrawlStarsApiTasklet` maintenance behavior, avoid inheritance-heavy tasklet abstractions, and introduce simple chunk flush APIs for aggregate writes.

**Tech Stack:** Kotlin, Spring Boot, Spring Batch, Spring Data JPA, QueryDSL, AssertJ, MockK, JUnit 5, Gradle.

---

## File Structure

Create or modify these focused units:

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTasklet.kt`  
  Keep the maintenance policy wrapper and document that `ServiceUnavailable` intentionally finishes the step.

- `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTaskletTest.kt`  
  Lock the policy with unit tests.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/*.kt`  
  Hold API collection and sync logic currently embedded in `*JobConfig`.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChangeDetector.kt`  
  Pure change detection for `BrawlerSyncTask`.

- `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChangeDetectorTest.kt`  
  Pure unit tests for item added/removed detection.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncItemWriter.kt`  
  Move the inner writer out of `BattleEventSyncJobConfig`.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/SoloRankSeasonCache.kt`  
  Move the inner cache out of `BattleEventSyncJobConfig`.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/StatsPlayerSyncFromRankingTask.kt`  
  Move ranking-based stats-player sync out of `StatsPlayerSyncJobConfig`.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/writer/StatsPlayerSyncJobItemWriter.kt`  
  Keep the current writer implementation under the dedicated writer package.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/*.kt`  
  Hold ownership reader, accumulator, writer, result model, and task.

- `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsAccumulatorTest.kt`  
  Pure tests for ownership counting.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/BattleStatsAggregator.kt`  
  Add simple chunk flush support.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/ResultCountAccumulator.kt`  
  Add `forEach` so result aggregators can flush without building a full list.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/RankCountAccumulator.kt`  
  Add `forEach` so rank aggregators can flush without building a full list.

- `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BattleStatsAggregatorChunkFlushTest.kt`  
  Verify chunk flush behavior.

- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/*.kt`  
  Hold daily and weekly statistics orchestration components.

---

### Task 1: Guardrails, Reader Ordering, and BrawlStars API Policy

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTasklet.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTaskletTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslPagingItemReader.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/StatsPlayerSyncJobConfig.kt`
- Delete: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt`

- [ ] **Step 1: Write the failing `BrawlStarsApiTasklet` policy test**

Create `BrawlStarsApiTaskletTest.kt`:

```kotlin
package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import feign.FeignException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.infrastructure.repeat.RepeatStatus

class BrawlStarsApiTaskletTest {
    private val contribution = mockk<StepContribution>(relaxed = true)
    private val chunkContext = mockk<ChunkContext>(relaxed = true)

    @Test
    fun `action 이 성공하면 FINISHED 를 반환한다`() {
        // given
        var executed = false
        val sut = BrawlStarsApiTasklet("testJob") {
            executed = true
        }

        // when
        val result = sut.execute(contribution, chunkContext)

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
        assertThat(executed).isTrue()
    }

    @Test
    fun `BrawlStars API 점검 예외는 삼키고 FINISHED 를 반환한다`() {
        // given
        val sut = BrawlStarsApiTasklet("testJob") {
            throw serviceUnavailable()
        }

        // when
        val result = sut.execute(contribution, chunkContext)

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
    }

    @Test
    fun `ServiceUnavailable 이 아닌 예외는 전파한다`() {
        // given
        val sut = BrawlStarsApiTasklet("testJob") {
            throw IllegalStateException("boom")
        }

        // when
        val ex =
            catchThrowableOfType(IllegalStateException::class.java) {
                sut.execute(contribution, chunkContext)
            }

        // then
        assertThat(ex).isNotNull()
        assertThat(ex.message).isEqualTo("boom")
    }

    private fun serviceUnavailable(): BrawlStarsApiServerException.ServiceUnavailable {
        val feignException =
            mockk<FeignException>(relaxed = true) {
                every { status() } returns 503
            }
        return BrawlStarsApiServerException.ServiceUnavailable("maintenance", feignException)
    }
}
```

- [ ] **Step 2: Run the guardrail test**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.BrawlStarsApiTaskletTest"
```

Expected: PASS. This is a characterization test for the existing intentional maintenance behavior before the refactor continues.

- [ ] **Step 3: Document the intentional maintenance policy**

Update `BrawlStarsApiTasklet.kt`:

```kotlin
/**
 * Wraps BrawlStars API batch actions with the service-maintenance policy.
 *
 * `ServiceUnavailable` from the BrawlStars API means the upstream API is under maintenance.
 * Jobs using this tasklet must not fail in that case: they log the maintenance state and
 * finish normally so scheduled batch orchestration can continue.
 */
class BrawlStarsApiTasklet(
    private val jobName: String,
    private val action: () -> Unit,
) : Tasklet {
```

Keep the existing `catch (ex: BrawlStarsApiServerException.ServiceUnavailable)` behavior unchanged.

- [ ] **Step 4: Add QueryDSL paging KDoc**

In `QuerydslPagingItemReader.kt`, extend the KDoc:

```kotlin
/**
 * QueryDSL-backed paging item reader.
 *
 * Callers must provide a stable `orderBy`, preferably by primary key ascending.
 * Offset paging without deterministic ordering can skip or duplicate rows between pages.
 *
 * https://github.com/jojoldu/spring-batch-querydsl
 */
open class QuerydslPagingItemReader<T : Any>(
```

- [ ] **Step 5: Add stable ordering to battle event sync readers**

In `BattleEventSyncJobConfig.kt`, update the rank reader:

```kotlin
return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
    queryFactory
        .selectFrom(rankBattle)
        .where(rankBattle.battleDate.eq(dateJobParameter().date))
        .orderBy(rankBattle.id.asc())
}
```

Update the result reader:

```kotlin
return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
    queryFactory
        .selectFrom(resultBattle)
        .where(resultBattle.battleDate.eq(dateJobParameter().date))
        .orderBy(resultBattle.id.asc())
}
```

- [ ] **Step 6: Add stable ordering to stats player sync readers**

In `StatsPlayerSyncJobConfig.kt`, update the rank reader:

```kotlin
return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
    queryFactory
        .selectFrom(rankBattle)
        .where(rankBattle.battleDate.eq(dateJobParameter().date))
        .orderBy(rankBattle.id.asc())
}
```

Update the result reader:

```kotlin
return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
    queryFactory
        .selectFrom(resultBattle)
        .where(resultBattle.battleDate.eq(dateJobParameter().date))
        .orderBy(resultBattle.id.asc())
}
```

- [ ] **Step 7: Delete the unused daily per-event tasklet**

Remove:

```text
imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt
```

Confirm no references remain:

```bash
rg "PerEventDailyBattleStatsTasklet" imstargg-backend/core/core-batch
```

Expected: no output.

- [ ] **Step 8: Run task tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.BrawlStarsApiTaskletTest"
./gradlew :core:core-batch:contextTest
```

Expected: both commands pass.

- [ ] **Step 9: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTasklet.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlStarsApiTaskletTest.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/querydsl/QuerydslPagingItemReader.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/StatsPlayerSyncJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventDailyBattleStatsTasklet.kt
git commit -m "refactor(batch): add batch guardrails"
```

---

### Task 2: Extract Simple BrawlStars API Collection Tasks

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/PlayerRankingCollectTask.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/ClubRankingCollectTask.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlerRankingCollectTask.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BattleEventRotationSyncTask.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BattleEventModeSyncTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/PlayerRankingCollectJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/ClubRankingCollectJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerRankingCollectJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventRotationSyncJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventModeSyncJobConfig.kt`

- [ ] **Step 1: Create `PlayerRankingCollectTask`**

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class PlayerRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
) {
    fun collect() {
        log.info("[{}] 시작", JOB_NAME)
        Country.entries.forEach { country ->
            val collectedAt = OffsetDateTime.now(clock)
            val response = brawlStarsClient.getPlayerRanking(country.code)
            val ranking = playerRankingJpaRepository.save(PlayerRankingEntity(country = country, collectedAt = collectedAt))
            val items =
                response.items.map { resp ->
                    PlayerRankingItemEntity(
                        playerRankingId = ranking.id!!,
                        rank = resp.rank,
                        player =
                            RankingEntityPlayer(
                                brawlStarsTag = resp.tag,
                                name = resp.name,
                                nameColor = resp.nameColor,
                                iconBrawlStarsId = resp.icon.id,
                                clubName = resp.club?.name ?: "",
                            ),
                        trophies = resp.trophies,
                    )
                }
            playerRankingItemJpaRepository.saveAll(items)
            deleteOutdated(country)
            log.debug("[{}] 국가 {} 수집 - {}건", JOB_NAME, country, items.size)
        }
        log.info("[{}] 완료", JOB_NAME)
    }

    private fun deleteOutdated(country: Country) {
        val outdated = playerRankingJpaRepository.findAllByCountryOrderByCollectedAtDesc(country).drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return
        val outdatedIds = outdated.mapNotNull { it.id }
        val outdatedItems = playerRankingItemJpaRepository.findAllByPlayerRankingIdIn(outdatedIds)
        playerRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        playerRankingJpaRepository.deleteAllInBatch(outdated)
    }

    companion object {
        private const val JOB_NAME = "playerRankingCollectJob"
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(PlayerRankingCollectTask::class.java)
    }
}
```

- [ ] **Step 2: Wire `PlayerRankingCollectJobConfig` to the task**

Remove `Clock`, `BrawlStarsClient`, `PlayerRankingJpaRepository`, and `PlayerRankingItemJpaRepository` constructor dependencies from `PlayerRankingCollectJobConfig`. Add `private val task: PlayerRankingCollectTask`.

Replace the tasklet body with:

```kotlin
.tasklet(
    BrawlStarsApiTasklet(JOB_NAME) {
        task.collect()
    },
    txManager,
).build()
```

- [ ] **Step 3: Create `ClubRankingCollectTask`**

Use this class and keep the existing retention behavior:

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class ClubRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
    private val clubRankingItemJpaRepository: ClubRankingItemJpaRepository,
) {
    fun collect() {
        log.info("[{}] 시작", JOB_NAME)
        Country.entries.forEach { country ->
            val collectedAt = OffsetDateTime.now(clock)
            val response = brawlStarsClient.getClubRanking(country.code)
            val ranking = clubRankingJpaRepository.save(ClubRankingEntity(country = country, collectedAt = collectedAt))
            val items =
                response.items.map { resp ->
                    ClubRankingItemEntity(
                        clubRankingId = ranking.id!!,
                        rank = resp.rank,
                        brawlStarsTag = resp.tag,
                        name = resp.name,
                        badgeBrawlStarsId = resp.badgeId.toLong(),
                        trophies = resp.trophies,
                        memberCount = resp.memberCount,
                    )
                }
            clubRankingItemJpaRepository.saveAll(items)
            deleteOutdated(country)
            log.debug("[{}] 국가 {} 수집 - {}건", JOB_NAME, country, items.size)
        }
        log.info("[{}] 완료", JOB_NAME)
    }

    private fun deleteOutdated(country: Country) {
        val outdated = clubRankingJpaRepository.findAllByCountryOrderByCollectedAtDesc(country).drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return
        val outdatedIds = outdated.mapNotNull { it.id }
        val outdatedItems = clubRankingItemJpaRepository.findAllByClubRankingIdIn(outdatedIds)
        clubRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        clubRankingJpaRepository.deleteAllInBatch(outdated)
    }

    companion object {
        private const val JOB_NAME = "clubRankingCollectJob"
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(ClubRankingCollectTask::class.java)
    }
}
```

- [ ] **Step 4: Wire `ClubRankingCollectJobConfig` to the task**

Remove collection dependencies from the config constructor and add `private val task: ClubRankingCollectTask`.

Use:

```kotlin
.tasklet(
    BrawlStarsApiTasklet(JOB_NAME) {
        task.collect()
    },
    txManager,
).build()
```

- [ ] **Step 5: Extract brawler ranking, event rotation, and event mode tasks**

Create these classes with one public method each:

```text
com.imstargg.core.batch.job.brawlstars.task.BrawlerRankingCollectTask.collect()
com.imstargg.core.batch.job.brawlstars.task.BattleEventRotationSyncTask.sync()
com.imstargg.core.batch.job.brawlstars.task.BattleEventModeSyncTask.sync()
```

`BrawlerRankingCollectTask` owns the existing `getBrawlers -> Country.entries -> getBrawlerRanking -> save ranking/items -> delete outdated snapshots` flow and keeps `SNAPSHOT_RETENTION_COUNT = 2`. `BattleEventRotationSyncTask` owns the existing `getEventRotation -> save rotation header -> save rotation items` flow. `BattleEventModeSyncTask` owns the existing `getGameModes -> compare known mode ids -> alert new modes` flow.

- [ ] **Step 6: Wire the remaining configs to tasks**

Use these tasklet bodies:

```kotlin
BrawlStarsApiTasklet(JOB_NAME) {
    task.collect()
}
```

for `BrawlerRankingCollectJobConfig`, and:

```kotlin
BrawlStarsApiTasklet(JOB_NAME) {
    task.sync()
}
```

for `BattleEventRotationSyncJobConfig` and `BattleEventModeSyncJobConfig`.

- [ ] **Step 7: Run context tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars
git commit -m "refactor(batch): extract api collection tasks"
```

---

### Task 3: Extract Brawler Sync Change Detection

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChange.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChangeDetector.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlerSyncTask.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChangeDetectorTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerSyncJobConfig.kt`

- [ ] **Step 1: Write the failing detector test**

Create `BrawlerChangeDetectorTest.kt` with a pure repository-free test using ID sets:

```kotlin
package com.imstargg.core.batch.job.brawlstars

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerChangeDetectorTest {
    @Test
    fun `새 아이템과 삭제 아이템을 감지한다`() {
        // given
        val sut = BrawlerChangeDetector()

        // when
        val result =
            sut.detectItemChanges(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerChangeItemType.GEAR,
                apiItemIds = setOf(1, 2),
                dbItemIds = setOf(2, 3),
                toJson = { id -> "item=$id" },
            )

        // then
        assertThat(result).hasSize(2)
        assertThat(result)
            .extracting("changeType")
            .containsExactlyInAnyOrder(BrawlerChangeType.ADDED, BrawlerChangeType.REMOVED)
        assertThat(result.map { it.itemJson }).containsExactlyInAnyOrder("item=1", "brawlStarsId=3")
    }
}
```

- [ ] **Step 2: Run the failing detector test**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.BrawlerChangeDetectorTest"
```

Expected: FAIL because `BrawlerChangeDetector` does not exist.

- [ ] **Step 3: Create shared change model**

Create `BrawlerChange.kt`:

```kotlin
package com.imstargg.core.batch.job.brawlstars

enum class BrawlerChangeType {
    ADDED,
    REMOVED,
}

enum class BrawlerChangeItemType {
    STAR_POWER,
    GADGET,
    HYPER_CHARGE,
    GEAR,
}

data class BrawlerChange(
    val brawlerId: Long,
    val brawlerName: String,
    val changeType: BrawlerChangeType,
    val itemType: BrawlerChangeItemType,
    val itemJson: String = "",
)
```

- [ ] **Step 4: Implement `BrawlerChangeDetector`**

```kotlin
package com.imstargg.core.batch.job.brawlstars

import org.springframework.stereotype.Component

@Component
class BrawlerChangeDetector {
    fun detectItemChanges(
        brawlerId: Long,
        brawlerName: String,
        itemType: BrawlerChangeItemType,
        apiItemIds: Set<Long>,
        dbItemIds: Set<Long>,
        toJson: (Long) -> String,
    ): List<BrawlerChange> {
        val addedChanges =
            (apiItemIds - dbItemIds).map { id ->
                BrawlerChange(
                    brawlerId = brawlerId,
                    brawlerName = brawlerName,
                    changeType = BrawlerChangeType.ADDED,
                    itemType = itemType,
                    itemJson = toJson(id),
                )
            }
        val removedChanges =
            (dbItemIds - apiItemIds).map { id ->
                BrawlerChange(
                    brawlerId = brawlerId,
                    brawlerName = brawlerName,
                    changeType = BrawlerChangeType.REMOVED,
                    itemType = itemType,
                    itemJson = "brawlStarsId=$id",
                )
            }
        return addedChanges + removedChanges
    }
}
```

- [ ] **Step 5: Create `BrawlerSyncTask`**

Create `BrawlerSyncTask.sync()` with the current `getBrawlers`, new/deleted brawler alerting, item change detection, and completion logging flow. Inject `BrawlerChangeDetector` and replace the old private `detectItemChanges` mutation with:

```kotlin
changes +=
    changeDetector.detectItemChanges(
        brawlerId = brawler.id,
        brawlerName = brawler.name,
        itemType = BrawlerChangeItemType.STAR_POWER,
        apiItemIds = brawler.starPowers.map { it.id }.toSet(),
        dbItemIds =
            starPowerJpaRepository
                .findAllByBrawlerBrawlStarsId(brawler.id)
                .map { it.brawlStarsId }
                .toSet(),
        toJson = { id -> jsonMapper.writeValueAsString(brawler.starPowers.first { it.id == id }) },
    )
```

Add equivalent detector calls for `GADGET`, `HYPER_CHARGE`, and `GEAR`; each call passes the matching API item id set, DB item id set, and JSON lookup from the matching response collection.

- [ ] **Step 6: Slim `BrawlerSyncJobConfig`**

Remove nested enums/data class/private detection methods from `BrawlerSyncJobConfig`. Keep only job/step beans and inject:

```kotlin
private val task: BrawlerSyncTask
```

Use:

```kotlin
.tasklet(
    BrawlStarsApiTasklet(JOB_NAME) {
        task.sync()
    },
    txManager,
).build()
```

- [ ] **Step 7: Run tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.BrawlerChangeDetectorTest"
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/BrawlerChangeDetectorTest.kt
git commit -m "refactor(batch): extract brawler sync logic"
```

---

### Task 4: Extract Battle Event Sync Writer

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/SoloRankSeasonCache.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncItemWriter.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncJobConfig.kt`

- [ ] **Step 1: Move `SoloRankSeasonCache`**

Create:

```kotlin
package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.enums.SoloRankSeason
import java.time.OffsetDateTime

class SoloRankSeasonCache {
    private val seasons: MutableList<SoloRankSeason> = mutableListOf()

    fun get(battleAt: OffsetDateTime): SoloRankSeason =
        seasons.find { !battleAt.isBefore(it.startAt) && !battleAt.isAfter(it.endAt) }
            ?: SoloRankSeason.of(battleAt).also { seasons.add(it) }
}
```

- [ ] **Step 2: Create `BattleEventSyncItemWriter`**

Create `BattleEventSyncItemWriter.kt` with the current writer behavior from `BattleEventSyncJobConfig`: detect unregistered events, detect mode/map mismatches, alert through the existing `AlertManager`, and create missing solo-rank season event rows. Use these constructor dependencies:

```kotlin
class BattleEventSyncItemWriter<T : BaseStatsBattleEntity>(
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) : ItemWriter<T> {
```

The class contains `write`, `detectEventChanges`, `matches`, `alertUnregisteredEvent`, `alertMismatch`, and `createSoloRankEvents`. Use this logger owner:

```kotlin
private val log = LoggerFactory.getLogger(BattleEventSyncItemWriter::class.java)
```

- [ ] **Step 3: Remove inner classes from config**

Delete the inner `SoloRankSeasonCache` and `BattleEventSyncItemWriter` classes from `BattleEventSyncJobConfig.kt`. Keep the two writer beans:

```kotlin
@Bean
@StepScope
fun battleEventSyncFromRankBattleWriter() =
    BattleEventSyncItemWriter<StatsRankBattleEntity>(
        alertManager = alertManager,
        battleEventJpaRepository = battleEventJpaRepository,
        battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
    )
```

and:

```kotlin
@Bean
@StepScope
fun battleEventSyncFromResultBattleWriter() =
    BattleEventSyncItemWriter<StatsResultBattleEntity>(
        alertManager = alertManager,
        battleEventJpaRepository = battleEventJpaRepository,
        battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
    )
```

- [ ] **Step 4: Run context tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/BattleEventSyncItemWriter.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/SoloRankSeasonCache.kt
git commit -m "refactor(batch): extract battle event sync writer"
```

---

### Task 5: Extract Stats Player Sync Components

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/StatsPlayerSyncFromRankingTask.kt`
- Move: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/StatsPlayerSyncJobItemWriter.kt` to `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/writer/StatsPlayerSyncJobItemWriter.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/StatsPlayerSyncJobConfig.kt`

- [ ] **Step 1: Create `StatsPlayerSyncFromRankingTask`**

```kotlin
package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StatsPlayerSyncFromRankingTask(
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val allTags = latestRankingTags()
        var totalNew = 0

        allTags.chunked(CHUNK_SIZE).forEach { batch ->
            val existingTags =
                statsPlayerJpaRepository
                    .findAllByBrawlStarsTagIn(batch)
                    .map { it.brawlStarsTag }
                    .toSet()
            val newPlayers =
                batch
                    .filter { it !in existingTags }
                    .map { StatsPlayerEntity(brawlStarsTag = it) }
            if (newPlayers.isNotEmpty()) {
                statsPlayerJpaRepository.saveAll(newPlayers)
                totalNew += newPlayers.size
            }
        }

        log.info("[{}] 완료 - 대상 태그: {}건, 신규: {}건", JOB_NAME, allTags.size, totalNew)
    }

    private fun latestRankingTags(): List<String> {
        val playerRankingIds =
            Country.entries.mapNotNull { country ->
                playerRankingJpaRepository.findTopByCountryOrderByCollectedAtDesc(country)?.id
            }
        val brawlerRankingIds =
            brawlerRankingJpaRepository
                .findAllLatestPerCountryAndBrawler()
                .mapNotNull { it.id }

        return buildList {
            if (playerRankingIds.isNotEmpty()) {
                addAll(
                    playerRankingItemJpaRepository
                        .findAllByPlayerRankingIdIn(playerRankingIds)
                        .map { it.player.brawlStarsTag },
                )
            }
            if (brawlerRankingIds.isNotEmpty()) {
                addAll(
                    brawlerRankingItemJpaRepository
                        .findAllByBrawlerRankingIdIn(brawlerRankingIds)
                        .map { it.player.brawlStarsTag },
                )
            }
        }.distinct()
    }

    companion object {
        private const val JOB_NAME = "statsPlayerSyncFromRankingJob"
        private const val CHUNK_SIZE = 1000
        private val log = LoggerFactory.getLogger(StatsPlayerSyncFromRankingTask::class.java)
    }
}
```

- [ ] **Step 2: Move writer package**

Move the writer file and update its package:

```kotlin
package com.imstargg.core.batch.job.statistics.writer
```

Keep the class name `StatsPlayerSyncJobItemWriter` and implementation unchanged.

- [ ] **Step 3: Slim the ranking step**

In `StatsPlayerSyncJobConfig`, remove ranking repository constructor dependencies except those no longer needed by the config. Add:

```kotlin
private val statsPlayerSyncFromRankingTask: StatsPlayerSyncFromRankingTask,
```

Replace the ranking tasklet body with:

```kotlin
.tasklet({ _, _ ->
    statsPlayerSyncFromRankingTask.sync()
    RepeatStatus.FINISHED
}, txManager)
.build()
```

Update the writer import to:

```kotlin
import com.imstargg.core.batch.job.statistics.writer.StatsPlayerSyncJobItemWriter
```

- [ ] **Step 4: Run tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics
git commit -m "refactor(batch): extract stats player sync task"
```

---

### Task 6: Add Ownership Accumulator with Pure Tests

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipSnapshot.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsResult.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsAccumulator.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsAccumulatorTest.kt`

- [ ] **Step 1: Write the failing accumulator test**

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.BuffyType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatsAccumulatorTest {
    @Test
    fun `브롤러 보유 총수와 아이템 보유수를 trophy range 별로 집계한다`() {
        // given
        val sut = BrawlerItemOwnershipStatsAccumulator()

        // when
        sut.add(
            PlayerBrawlerOwnershipSnapshot(
                playerBrawlerId = 1,
                brawlerBrawlStarsId = 16000000,
                trophies = 1000,
                gearBrawlStarsIds = listOf(62000000),
                gadgetBrawlStarsIds = listOf(23000000),
                starPowerBrawlStarsIds = listOf(23000100),
                hyperChargeBrawlStarsIds = listOf(23000200),
                buffyTypes = listOf(BuffyType.GADGET),
            ),
        )

        // then
        val result = sut.toResult()
        assertThat(result.gears).hasSize(1)
        assertThat(result.gears.first().ownerCount).isEqualTo(1)
        assertThat(result.gears.first().totalCount).isEqualTo(1)
        assertThat(result.gears.first().trophyRange).isEqualTo(BrawlerTrophyRange.TROPHY_1000_PLUS)
        assertThat(result.gadgets).hasSize(1)
        assertThat(result.starPowers).hasSize(1)
        assertThat(result.hyperCharges).hasSize(1)
        assertThat(result.buffies).hasSize(1)
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsAccumulatorTest"
```

Expected: FAIL because ownership classes do not exist.

- [ ] **Step 3: Create snapshot and result models**

Create `BrawlerItemOwnershipSnapshot.kt`:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BuffyType

data class PlayerBrawlerOwnershipSnapshot(
    val playerBrawlerId: Long,
    val brawlerBrawlStarsId: Long,
    val trophies: Int,
    val gearBrawlStarsIds: List<Long>,
    val gadgetBrawlStarsIds: List<Long>,
    val starPowerBrawlStarsIds: List<Long>,
    val hyperChargeBrawlStarsIds: List<Long>,
    val buffyTypes: List<BuffyType>,
)
```

Create `BrawlerItemOwnershipStatsResult.kt`:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsEntity

data class BrawlerItemOwnershipStatsResult(
    val gears: List<BrawlerGearOwnershipStatsEntity>,
    val gadgets: List<BrawlerGadgetOwnershipStatsEntity>,
    val starPowers: List<BrawlerStarPowerOwnershipStatsEntity>,
    val hyperCharges: List<BrawlerHyperChargeOwnershipStatsEntity>,
    val buffies: List<BrawlerBuffyOwnershipStatsEntity>,
) {
    val totalEntityCount: Int
        get() = gears.size + gadgets.size + starPowers.size + hyperCharges.size + buffies.size
}
```

- [ ] **Step 4: Implement accumulator**

Create `BrawlerItemOwnershipStatsAccumulator.kt` with the same counting rules as the current job:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsEntity

class BrawlerItemOwnershipStatsAccumulator {
    private val totalCounts = mutableMapOf<BrawlerTrophyKey, Long>()
    private val gearCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val gadgetCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val starPowerCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val hyperChargeCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val buffyCounts = mutableMapOf<BrawlerTrophyBuffyKey, Long>()

    fun add(snapshot: PlayerBrawlerOwnershipSnapshot) {
        val trophyRanges = BrawlerTrophyRange.resolveAll(snapshot.trophies)
        for (trophyRange in trophyRanges) {
            val totalKey = BrawlerTrophyKey(snapshot.brawlerBrawlStarsId, trophyRange)
            totalCounts.merge(totalKey, 1L, Long::plus)
            snapshot.gearBrawlStarsIds.forEach { gearId ->
                gearCounts.merge(BrawlerTrophyItemKey(snapshot.brawlerBrawlStarsId, trophyRange, gearId), 1L, Long::plus)
            }
            snapshot.gadgetBrawlStarsIds.forEach { gadgetId ->
                gadgetCounts.merge(BrawlerTrophyItemKey(snapshot.brawlerBrawlStarsId, trophyRange, gadgetId), 1L, Long::plus)
            }
            snapshot.starPowerBrawlStarsIds.forEach { starPowerId ->
                starPowerCounts.merge(BrawlerTrophyItemKey(snapshot.brawlerBrawlStarsId, trophyRange, starPowerId), 1L, Long::plus)
            }
            snapshot.hyperChargeBrawlStarsIds.forEach { hyperChargeId ->
                hyperChargeCounts.merge(BrawlerTrophyItemKey(snapshot.brawlerBrawlStarsId, trophyRange, hyperChargeId), 1L, Long::plus)
            }
            snapshot.buffyTypes.forEach { buffyType ->
                buffyCounts.merge(BrawlerTrophyBuffyKey(snapshot.brawlerBrawlStarsId, trophyRange, buffyType), 1L, Long::plus)
            }
        }
    }

    fun toResult(): BrawlerItemOwnershipStatsResult =
        BrawlerItemOwnershipStatsResult(
            gears =
                gearCounts.map { (key, ownerCount) ->
                    BrawlerGearOwnershipStatsEntity(
                        brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                        trophyRange = key.trophyRange,
                        gearBrawlStarsId = key.itemBrawlStarsId,
                        ownerCount = ownerCount,
                        totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                    )
                },
            gadgets =
                gadgetCounts.map { (key, ownerCount) ->
                    BrawlerGadgetOwnershipStatsEntity(
                        brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                        trophyRange = key.trophyRange,
                        gadgetBrawlStarsId = key.itemBrawlStarsId,
                        ownerCount = ownerCount,
                        totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                    )
                },
            starPowers =
                starPowerCounts.map { (key, ownerCount) ->
                    BrawlerStarPowerOwnershipStatsEntity(
                        brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                        trophyRange = key.trophyRange,
                        starPowerBrawlStarsId = key.itemBrawlStarsId,
                        ownerCount = ownerCount,
                        totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                    )
                },
            hyperCharges =
                hyperChargeCounts.map { (key, ownerCount) ->
                    BrawlerHyperChargeOwnershipStatsEntity(
                        brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                        trophyRange = key.trophyRange,
                        hyperChargeBrawlStarsId = key.itemBrawlStarsId,
                        ownerCount = ownerCount,
                        totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                    )
                },
            buffies =
                buffyCounts.map { (key, ownerCount) ->
                    BrawlerBuffyOwnershipStatsEntity(
                        brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                        trophyRange = key.trophyRange,
                        buffyType = key.buffyType,
                        ownerCount = ownerCount,
                        totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                    )
                },
        )

    private data class BrawlerTrophyKey(
        val brawlerBrawlStarsId: Long,
        val trophyRange: BrawlerTrophyRange,
    )

    private data class BrawlerTrophyBuffyKey(
        val brawlerBrawlStarsId: Long,
        val trophyRange: BrawlerTrophyRange,
        val buffyType: BuffyType,
    ) {
        fun toBrawlerTrophyKey() = BrawlerTrophyKey(brawlerBrawlStarsId, trophyRange)
    }

    private data class BrawlerTrophyItemKey(
        val brawlerBrawlStarsId: Long,
        val trophyRange: BrawlerTrophyRange,
        val itemBrawlStarsId: Long,
    ) {
        fun toBrawlerTrophyKey() = BrawlerTrophyKey(brawlerBrawlStarsId, trophyRange)
    }
}
```

- [ ] **Step 5: Run accumulator test**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsAccumulatorTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsAccumulatorTest.kt
git commit -m "refactor(batch): add ownership accumulator"
```

---

### Task 7: Replace Ownership Job Manual Transactions

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsReader.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriter.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt`

- [ ] **Step 1: Create reader**

Create `BrawlerItemOwnershipStatsReader.kt` with the page-read QueryDSL implementation below. It reads stats-player pages, fetches player brawler item rows for those tags, and returns snapshots:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerBuffyEntity.Companion.playerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerEntity.Companion.playerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGadgetEntity.Companion.playerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGearEntity.Companion.playerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerHyperChargeEntity.Companion.playerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerStarPowerEntity.Companion.playerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerEntity.Companion.playerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerItemOwnershipStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun readAll(consumer: (List<PlayerBrawlerOwnershipSnapshot>) -> Unit): Long {
        entityManagerFactory.createEntityManager().use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L
            while (true) {
                em.clear()
                val statsPlayers =
                    queryFactory
                        .selectFrom(statsPlayerEntity)
                        .where(statsPlayerEntity.id.gt(lastId))
                        .orderBy(statsPlayerEntity.id.asc())
                        .limit(PAGE_SIZE)
                        .fetch()
                if (statsPlayers.isEmpty()) break
                totalRead += statsPlayers.size
                lastId = statsPlayers.last().id!!
                val snapshots = readSnapshots(queryFactory, statsPlayers.map { it.brawlStarsTag })
                if (snapshots.isNotEmpty()) {
                    consumer(snapshots)
                }
            }
            return totalRead
        }
    }

    private fun readSnapshots(
        queryFactory: JPAQueryFactory,
        tags: List<String>,
    ): List<PlayerBrawlerOwnershipSnapshot> {
        val playerBrawlers =
            queryFactory
                .selectFrom(playerBrawlerEntity)
                .join(playerBrawlerEntity.player, playerEntity)
                .where(playerEntity.brawlStarsTag.`in`(tags))
                .fetch()
        if (playerBrawlers.isEmpty()) return emptyList()

        val playerBrawlerIds = playerBrawlers.mapNotNull { it.id }
        val gears = queryFactory.selectFrom(playerBrawlerGearEntity).where(playerBrawlerGearEntity.playerBrawler.id.`in`(playerBrawlerIds)).fetch()
        val gadgets = queryFactory.selectFrom(playerBrawlerGadgetEntity).where(playerBrawlerGadgetEntity.playerBrawler.id.`in`(playerBrawlerIds)).fetch()
        val starPowers = queryFactory.selectFrom(playerBrawlerStarPowerEntity).where(playerBrawlerStarPowerEntity.playerBrawler.id.`in`(playerBrawlerIds)).fetch()
        val hyperCharges = queryFactory.selectFrom(playerBrawlerHyperChargeEntity).where(playerBrawlerHyperChargeEntity.playerBrawler.id.`in`(playerBrawlerIds)).fetch()
        val buffies = queryFactory.selectFrom(playerBrawlerBuffyEntity).where(playerBrawlerBuffyEntity.playerBrawler.id.`in`(playerBrawlerIds)).fetch()

        return playerBrawlers.mapNotNull { playerBrawler ->
            val playerBrawlerId = playerBrawler.id ?: return@mapNotNull null
            PlayerBrawlerOwnershipSnapshot(
                playerBrawlerId = playerBrawlerId,
                brawlerBrawlStarsId = playerBrawler.brawlerBrawlStarsId,
                trophies = playerBrawler.trophies,
                gearBrawlStarsIds = gears.filter { it.playerBrawler.id == playerBrawlerId }.map { it.gearBrawlStarsId },
                gadgetBrawlStarsIds = gadgets.filter { it.playerBrawler.id == playerBrawlerId }.map { it.gadgetBrawlStarsId },
                starPowerBrawlStarsIds = starPowers.filter { it.playerBrawler.id == playerBrawlerId }.map { it.starPowerBrawlStarsId },
                hyperChargeBrawlStarsIds = hyperCharges.filter { it.playerBrawler.id == playerBrawlerId }.map { it.hyperChargeBrawlStarsId },
                buffyTypes = buffies.filter { it.playerBrawler.id == playerBrawlerId }.map { it.buffyType },
            )
        }
    }

    companion object {
        private const val PAGE_SIZE = 1000L
    }
}
```

- [ ] **Step 2: Create managed writer**

Create `BrawlerItemOwnershipStatsWriter.kt`:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class BrawlerItemOwnershipStatsWriter(
    txManager: PlatformTransactionManager,
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
) {
    private val txTemplate = TransactionTemplate(txManager)

    fun replaceAll(result: BrawlerItemOwnershipStatsResult) {
        txTemplate.executeWithoutResult {
            gearRepository.deleteAllInBatch()
            gadgetRepository.deleteAllInBatch()
            starPowerRepository.deleteAllInBatch()
            hyperChargeRepository.deleteAllInBatch()
            buffyRepository.deleteAllInBatch()

            result.gears.chunked(BATCH_SIZE).forEach { gearRepository.saveAll(it) }
            result.gadgets.chunked(BATCH_SIZE).forEach { gadgetRepository.saveAll(it) }
            result.starPowers.chunked(BATCH_SIZE).forEach { starPowerRepository.saveAll(it) }
            result.hyperCharges.chunked(BATCH_SIZE).forEach { hyperChargeRepository.saveAll(it) }
            result.buffies.chunked(BATCH_SIZE).forEach { buffyRepository.saveAll(it) }
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
```

- [ ] **Step 3: Create task**

Create `BrawlerItemOwnershipStatsTask.kt`:

```kotlin
package com.imstargg.core.batch.job.statistics.ownership

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerItemOwnershipStatsTask(
    private val reader: BrawlerItemOwnershipStatsReader,
    private val writer: BrawlerItemOwnershipStatsWriter,
) {
    fun rebuild() {
        val accumulator = BrawlerItemOwnershipStatsAccumulator()
        val totalRead =
            reader.readAll { snapshots ->
                snapshots.forEach { accumulator.add(it) }
                log.debug("[{}] 읽기 진행 - 누적 snapshot: {}건", JOB_NAME, snapshots.size)
            }
        val result = accumulator.toResult()
        writer.replaceAll(result)
        log.info("[{}] 완료 - 플레이어: {}건, 집계: {}건", JOB_NAME, totalRead, result.totalEntityCount)
    }

    companion object {
        private const val JOB_NAME = "brawlerItemOwnershipStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerItemOwnershipStatsTask::class.java)
    }
}
```

- [ ] **Step 4: Slim JobConfig**

Replace `BrawlerItemOwnershipStatsJobConfig` constructor with:

```kotlin
class BrawlerItemOwnershipStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerItemOwnershipStatsTask,
) {
```

Replace the step tasklet with:

```kotlin
.tasklet({ _, _ ->
    task.rebuild()
    RepeatStatus.FINISHED
}, txManager)
.build()
```

Remove QueryDSL imports and nested key data classes from JobConfig.

- [ ] **Step 5: Run tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsAccumulatorTest"
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership
git commit -m "refactor(batch): split ownership stats job"
```

---

### Task 8: Add Aggregator Chunk Flush API

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/BattleStatsAggregator.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/ResultCountAccumulator.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/RankCountAccumulator.kt`
- Modify: all five `*StatsAggregator.kt` files under `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BattleStatsAggregatorChunkFlushTest.kt`

- [ ] **Step 1: Write the failing chunk flush test**

```kotlin
package com.imstargg.core.batch.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BattleStatsAggregatorChunkFlushTest {
    @Test
    fun `chunk size 만큼 나눠 flush 한다`() {
        // given
        val sut = TestAggregator(listOf(1, 2, 3, 4, 5))
        val chunks = mutableListOf<List<Int>>()

        // when
        sut.flushEntityChunks(chunkSize = 2) { chunks.add(it) }

        // then
        assertThat(chunks).containsExactly(listOf(1, 2), listOf(3, 4), listOf(5))
    }

    private class TestAggregator(
        private val entities: List<Int>,
    ) : BattleStatsAggregator<Unit, Int> {
        override fun aggregate(battle: Unit) {
        }

        override fun forEachEntity(consumer: (Int) -> Unit) {
            entities.forEach(consumer)
        }

        override fun toEntities(): List<Int> = entities
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.BattleStatsAggregatorChunkFlushTest"
```

Expected: FAIL because `flushEntityChunks` and `forEachEntity` do not exist.

- [ ] **Step 3: Add aggregator default methods**

Update `BattleStatsAggregator.kt`:

```kotlin
interface BattleStatsAggregator<TBattle, TEntity> {
    fun aggregate(battle: TBattle)

    fun forEachEntity(consumer: (TEntity) -> Unit) {
        toEntities().forEach(consumer)
    }

    fun flushEntityChunks(
        chunkSize: Int,
        flush: (List<TEntity>) -> Unit,
    ) {
        require(chunkSize > 0) { "chunkSize must be positive" }
        val buffer = ArrayList<TEntity>(chunkSize)
        forEachEntity { entity ->
            buffer.add(entity)
            if (buffer.size == chunkSize) {
                flush(buffer.toList())
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) {
            flush(buffer.toList())
        }
    }

    fun toEntities(): List<TEntity>
}
```

- [ ] **Step 4: Add accumulator `forEach` methods**

In `ResultCountAccumulator.kt`, add:

```kotlin
fun forEach(consumer: (K, ResultCountEmbedded) -> Unit) {
    counts.forEach { (key, count) -> consumer(key, count.toEmbedded()) }
}
```

In `RankCountAccumulator.kt`, add:

```kotlin
fun forEach(consumer: (K, RankCountsEmbedded) -> Unit) {
    counts.forEach { (key, count) -> consumer(key, count.toEmbedded()) }
}
```

- [ ] **Step 5: Override `forEachEntity` in aggregators**

For each aggregator, convert `toEntities()` to delegate to `forEachEntity`.

Example for `BrawlerBattleResultStatsAggregator`:

```kotlin
override fun forEachEntity(consumer: (BrawlerBattleResultStatsEntity) -> Unit) {
    resultCounts.forEach { key, resultCount ->
        consumer(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                battleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                resultCount = resultCount,
                starPlayerCount = starPlayerCounts[key],
            ),
        )
    }
}

override fun toEntities(): List<BrawlerBattleResultStatsEntity> =
    buildList {
        forEachEntity { add(it) }
    }
```

Implement `forEachEntity` plus `toEntities` delegation in these files:

```text
BrawlerBattleRankStatsAggregator
BrawlerPairBattleResultStatsAggregator
BrawlerPairBattleRankStatsAggregator
BrawlerEnemyBattleResultStatsAggregator
```

- [ ] **Step 6: Run aggregator tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.*"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/BattleStatsAggregatorChunkFlushTest.kt
git commit -m "refactor(batch): add aggregator chunk flush"
```

---

### Task 9: Replace Generic Statistics Tasklet Orchestration

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt`
- Modify: all five statistics JobConfig files that currently instantiate those tasklets

- [ ] **Step 1: Create daily aggregation task component**

Create a composition helper that is not a `Tasklet`:

```kotlin
package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyBattleStatsAggregationTask {
    fun <TBattle, TEntity : Any> aggregate(
        em: EntityManager,
        jobName: String,
        date: LocalDate,
        readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregator: BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
    ) {
        val queryFactory = JPAQueryFactory(em)
        var lastId = 0L
        var totalRead = 0L
        while (true) {
            val battles = readPage(queryFactory, date, lastId)
            if (battles.isEmpty()) break
            totalRead += battles.size
            battles.forEach { aggregator.aggregate(it) }
            lastId = getId(battles.last())
            em.clear()
            log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
        }

        deleteExisting(queryFactory, date)
        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { entities ->
            entities.forEach {
                persist(em, it)
                totalEntities++
            }
            em.flush()
            em.clear()
        }

        log.info("[{}] 완료 - date: {}, 전투: {}건, 집계: {}건", jobName, date, totalRead, totalEntities)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyBattleStatsAggregationTask::class.java)
    }
}
```

- [ ] **Step 2: Create weekly event aggregation task component**

Create `WeeklyEventBattleStatsAggregationTask.kt` with the current weekly event loop structure: resolve event ids per week range, read battle pages per event, aggregate, delete the event's existing stats, and flush aggregate entities by chunk.

Use this method signature:

```kotlin
fun <TBattle, TEntity : Any> aggregate(
    em: EntityManager,
    jobName: String,
    weekRanges: List<WeekRange>,
    readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
    readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
    getId: (TBattle) -> Long,
    aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
    deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
    persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
)
```

Inside the event loop, replace the old entity list code with:

```kotlin
aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { entities ->
    entities.forEach {
        em.persist(it)
        persistAdditional(em, it)
        totalEntities++
    }
    em.flush()
    em.clear()
}
```

- [ ] **Step 3: Update daily JobConfig files**

Inject `DailyBattleStatsAggregationTask` into:

```text
BrawlerBattleResultStatsJobConfig
BrawlerBattleRankStatsJobConfig
```

Replace `DailyBattleStatsTasklet(...)` with a lambda tasklet:

```kotlin
.tasklet({ _, _ ->
    aggregationTask.aggregate(
        em = em,
        jobName = JOB_NAME,
        date = dateJobParameter().date!!,
        readPage = { queryFactory, date, lastId ->
            queryFactory
                .selectFrom(statsResultBattleEntity)
                .where(
                    statsResultBattleEntity.battleDate.eq(date),
                    statsResultBattleEntity.id.gt(lastId),
                ).orderBy(statsResultBattleEntity.id.asc())
                .limit(BattleStatsConstants.READ_PAGE_SIZE)
                .fetch()
                .toList()
        },
        getId = { it.id!! },
        aggregator = BrawlerBattleResultStatsAggregator(),
        deleteExisting = { queryFactory, date ->
            queryFactory
                .delete(brawlerBattleResultStatsEntity)
                .where(brawlerBattleResultStatsEntity.battleDate.eq(date))
                .execute()
        },
    )
    RepeatStatus.FINISHED
}, txManager)
.build()
```

For rank stats, use the same method call shown above but pass `statsRankBattleEntity`, `BrawlerBattleRankStatsAggregator()`, and the rank stats target table in the `deleteExisting` lambda.

- [ ] **Step 4: Update weekly JobConfig files**

Inject `WeeklyEventBattleStatsAggregationTask` into:

```text
BrawlerPairBattleResultStatsJobConfig
BrawlerPairBattleRankStatsJobConfig
BrawlerEnemyBattleResultStatsJobConfig
```

Replace `PerEventWeeklyBattleStatsTasklet(...)` with lambda tasklets that pass each job's existing `readDistinctEventIds`, `readPage`, `getId`, `aggregatorFactory`, and `deleteExisting` lambdas to `aggregationTask.aggregate(...)`.

- [ ] **Step 5: Delete old tasklet classes**

After all references are gone, delete:

```text
imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/DailyBattleStatsTasklet.kt
imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/PerEventWeeklyBattleStatsTasklet.kt
```

Confirm:

```bash
rg "DailyBattleStatsTasklet|PerEventWeeklyBattleStatsTasklet" imstargg-backend/core/core-batch
```

Expected: no output.

- [ ] **Step 6: Run statistics tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.*"
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain
git commit -m "refactor(batch): extract statistics aggregation tasks"
```

---

### Task 10: Final Verification and Cleanup

**Files:**
- Review: all files changed under `imstargg-backend/core/core-batch`
- Review: `docs/superpowers/specs/2026-04-30-core-batch-refactoring-design.md`

- [ ] **Step 1: Format Kotlin**

Run:

```bash
cd imstargg-backend
./gradlew ktlintFormat
```

Expected: SUCCESS.

- [ ] **Step 2: Run focused core-batch tests**

Run:

```bash
cd imstargg-backend
./gradlew :core:core-batch:test :core:core-batch:contextTest
```

Expected: SUCCESS.

- [ ] **Step 3: Run backend unit test suite**

Run:

```bash
cd imstargg-backend
./gradlew unitTest
```

Expected: SUCCESS.

- [ ] **Step 4: Confirm no forbidden patterns remain**

Run:

```bash
rg "createEntityManager\\(|\\.transaction\\.begin\\(|\\.transaction\\.commit\\(|DailyBattleStatsTasklet|PerEventWeeklyBattleStatsTasklet|PerEventDailyBattleStatsTasklet" imstargg-backend/core/core-batch/src/main/kotlin
```

Expected: no output for removed tasklets and no ownership-job manual transaction usage. `QuerydslPagingItemReader` may still call `createEntityManager` internally.

- [ ] **Step 5: Review JobConfig size and responsibility**

Run:

```bash
wc -l imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/**/*.kt
```

Expected: `BrawlerItemOwnershipStatsJobConfig.kt`, `BrawlerSyncJobConfig.kt`, and ranking/sync JobConfig files are substantially shorter than before and contain wiring rather than API loops or aggregation loops.

- [ ] **Step 6: Commit final formatting cleanup**

```bash
git status --short
git add imstargg-backend/core/core-batch
git commit -m "chore(batch): finalize core-batch refactoring"
```

If `git status --short` shows no changes after verification, skip this commit.
