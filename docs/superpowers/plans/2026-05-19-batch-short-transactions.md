# Core Batch Statistics Short Transactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove long DB transactions from `core-batch` statistics tasklet jobs while keeping Spring Batch job metadata and the existing aggregation model.

**Architecture:** Statistics steps remain tasklet steps, but they no longer receive the DB `PlatformTransactionManager`. Read/aggregate work runs outside a step-wide DB transaction, and each replace unit opens a short independent JPA transaction through a thin `JpaBatchTransactionRunner`. Replace logic stays inside the task/writer classes instead of being hidden behind a large lambda-based abstraction.

**Tech Stack:** Kotlin, Spring Boot 4.0.2, Spring Batch 6.0.2, Spring Transaction, JPA/Hibernate, QueryDSL, MockK, AssertJ, JUnit 5.

---

## Scope Check

This plan covers one subsystem: `imstargg-backend/core/core-batch` statistics transaction boundaries. It does not change API reads, schemas, DB aggregation SQL, or core-worker behavior.

All Gradle commands in this plan are run from `/Users/chodyoungrae/Projects/imstargg-v2/imstargg-backend`. Git and `rg` commands are run from `/Users/chodyoungrae/Projects/imstargg-v2`.

## File Structure

- Create `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunner.kt`
  - Thin helper that opens a `PROPAGATION_REQUIRES_NEW` transaction and provides a transaction-bound `EntityManager` plus `JPAQueryFactory`.
- Create `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunnerTest.kt`
  - Unit tests for commit, rollback, and transaction definition.
- Modify `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt`
  - Own read `EntityManager` lifecycle and run `date` replace inside `JpaBatchTransactionRunner`.
- Modify `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTaskTest.kt`
  - Verify read happens before replace and replace flushes chunks in a short transaction.
- Modify `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt`
  - Own read `EntityManager` lifecycle and run `(weekRange, eventId)` replace inside `JpaBatchTransactionRunner`.
- Modify `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTaskTest.kt`
  - Verify event-level replace transactions and existing `persistAdditional` behavior.
- Modify statistics job configs:
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt`
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt`
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt`
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt`
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt`
  - `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt`
  - Remove statistics step DB `txManager` usage and remove direct `EntityManager` injection from battle stats configs.
- Modify `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriter.kt`
  - Remove `@Transactional`, use `JpaBatchTransactionRunner`, and keep table replace flow inside the writer.
- Modify `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriterTest.kt`
  - Verify table-level transactions and chunk clear behavior.
- Use existing `CoreBatchApplicationTest`/`contextTest` plus a source guard command to verify statistics tasklet steps no longer pass the DB transaction manager.

### Task 1: Add Thin JPA Transaction Runner

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunner.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunnerTest.kt`

- [x] **Step 1: Write the failing runner test**

Create `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunnerTest.kt`:

```kotlin
package com.imstargg.core.batch.support.transaction

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

class JpaBatchTransactionRunnerTest {
    private val txManager = mockk<PlatformTransactionManager>()
    private val entityManager = mockk<EntityManager>()
    private val sut = JpaBatchTransactionRunner(txManager, entityManager)

    @Test
    fun `REQUIRES_NEW transaction 안에서 EntityManager 와 QueryFactory 를 제공한다`() {
        // given
        val status = mockk<TransactionStatus>(relaxed = true)
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } returns status
        every { txManager.commit(status) } just Runs

        // when
        val result =
            sut.run { em, queryFactory ->
                assertThat(em).isSameAs(entityManager)
                assertThat(queryFactory).isNotNull()
                "done"
            }

        // then
        assertThat(result).isEqualTo("done")
        verify(exactly = 1) { txManager.commit(status) }
    }

    @Test
    fun `action 이 실패하면 transaction 을 rollback 하고 예외를 전파한다`() {
        // given
        val status = mockk<TransactionStatus>(relaxed = true)
        val failure = IllegalStateException("replace failed")
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } returns status
        every { txManager.rollback(status) } just Runs

        // when, then
        assertThatThrownBy {
            sut.run { _, _ -> throw failure }
        }.isSameAs(failure)
        verify(exactly = 1) { txManager.rollback(status) }
    }
}
```

- [x] **Step 2: Run the runner test to verify it fails**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunnerTest"
```

Expected: FAIL because `JpaBatchTransactionRunner` does not exist.

- [x] **Step 3: Implement `JpaBatchTransactionRunner`**

Create `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunner.kt`:

```kotlin
package com.imstargg.core.batch.support.transaction

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Component
class JpaBatchTransactionRunner(
    txManager: PlatformTransactionManager,
    private val entityManager: EntityManager,
) {
    private val transactionTemplate =
        TransactionTemplate(txManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun <T : Any> run(action: (EntityManager, JPAQueryFactory) -> T): T =
        transactionTemplate.execute {
            action(entityManager, JPAQueryFactory(entityManager))
        } ?: error("JpaBatchTransactionRunner action returned null")
}
```

- [x] **Step 4: Run the runner test to verify it passes**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunnerTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunner.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/support/transaction/JpaBatchTransactionRunnerTest.kt
git commit -m "feat(batch): add JPA batch transaction runner"
```

### Task 2: Refactor Daily Battle Aggregation

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTaskTest.kt`

- [x] **Step 1: Replace the daily aggregation test with a transaction-boundary test**

Update `DailyBattleStatsAggregationTaskTest.kt` to this shape:

```kotlin
package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import java.time.LocalDate

class DailyBattleStatsAggregationTaskTest {
    @Test
    fun `일별 전투를 모두 읽은 뒤 날짜 단위 짧은 transaction 으로 집계를 교체한다`() {
        // given
        val readEm = mockk<EntityManager>()
        val writeEm = mockk<EntityManager>()
        val entityManagerFactory = mockk<EntityManagerFactory>()
        val txManager = mockk<PlatformTransactionManager>()
        val txStatus = mockk<TransactionStatus>(relaxed = true)
        val calls = mutableListOf<String>()
        val sut =
            DailyBattleStatsAggregationTask(
                entityManagerFactory = entityManagerFactory,
                transactionRunner = JpaBatchTransactionRunner(txManager, writeEm),
            )
        val date = LocalDate.of(2026, 4, 30)
        val aggregator = StreamingAggregator()

        every { entityManagerFactory.createEntityManager() } returns readEm
        every { readEm.clear() } answers {
            calls.add("readEm.clear")
            Unit
        }
        every { readEm.close() } answers {
            calls.add("readEm.close")
            Unit
        }
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } answers {
            calls.add("tx.begin")
            txStatus
        }
        every { txManager.commit(txStatus) } answers {
            calls.add("tx.commit")
            Unit
        }
        every { writeEm.persist(any()) } answers {
            calls.add("writeEm.persist:${firstArg<Int>()}")
            Unit
        }
        every { writeEm.flush() } answers {
            calls.add("writeEm.flush")
            Unit
        }
        every { writeEm.clear() } answers {
            calls.add("writeEm.clear")
            Unit
        }

        // when
        sut.aggregate(
            jobName = "testDailyJob",
            date = date,
            readPage = { _, _, lastId ->
                calls.add("read:$lastId")
                when (lastId) {
                    0L -> listOf(TestBattle(id = 1), TestBattle(id = 2))
                    2L -> listOf(TestBattle(id = 3))
                    else -> emptyList()
                }
            },
            getId = { it.id },
            aggregator = aggregator,
            deleteExisting = { _, deletedDate ->
                calls.add("delete:$deletedDate")
            },
        )

        // then
        assertThat(aggregator.aggregatedIds).containsExactly(1L, 2L, 3L)
        assertThat(calls).containsExactly(
            "read:0",
            "readEm.clear",
            "read:2",
            "readEm.clear",
            "read:3",
            "readEm.close",
            "tx.begin",
            "delete:2026-04-30",
            "writeEm.persist:1",
            "writeEm.persist:2",
            "writeEm.persist:3",
            "writeEm.flush",
            "writeEm.clear",
            "tx.commit",
        )
    }

    private data class TestBattle(
        val id: Long,
    )

    private class StreamingAggregator : BattleStatsAggregator<TestBattle, Int> {
        val aggregatedIds = mutableListOf<Long>()

        override fun aggregate(battle: TestBattle) {
            aggregatedIds.add(battle.id)
        }

        override fun forEachEntity(consumer: (Int) -> Unit) {
            aggregatedIds.forEach { consumer(it.toInt()) }
        }

        override fun toEntities(): List<Int> = error("toEntities must not be called")
    }
}
```

- [x] **Step 2: Run the daily test to verify it fails**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.task.DailyBattleStatsAggregationTaskTest"
```

Expected: FAIL because `DailyBattleStatsAggregationTask` still requires `em` as an aggregate argument and has no constructor dependencies.

- [x] **Step 3: Refactor `DailyBattleStatsAggregationTask`**

Replace `DailyBattleStatsAggregationTask.kt` with:

```kotlin
package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyBattleStatsAggregationTask(
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun <TBattle, TEntity : Any> aggregate(
        jobName: String,
        date: LocalDate,
        readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregator: BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
    ) {
        val totalRead = aggregateBattles(
            jobName = jobName,
            date = date,
            readPage = readPage,
            getId = getId,
            aggregator = aggregator,
        )

        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            deleteExisting(queryFactory, date)

            aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
                batch.forEach { persist(em, it) }
                totalEntities += batch.size
                em.flush()
                em.clear()
            }
        }

        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            jobName,
            date,
            totalRead,
            totalEntities,
        )
    }

    private fun <TBattle, TEntity : Any> aggregateBattles(
        jobName: String,
        date: LocalDate,
        readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregator: BattleStatsAggregator<TBattle, TEntity>,
    ): Long {
        var lastId = 0L
        var totalRead = 0L

        entityManagerFactory.createEntityManager().use { em ->
            val queryFactory = JPAQueryFactory(em)
            while (true) {
                val battles = readPage(queryFactory, date, lastId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                em.clear()
                log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
            }
        }

        return totalRead
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyBattleStatsAggregationTask::class.java)
    }
}
```

- [x] **Step 4: Run the daily test to verify it passes**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.task.DailyBattleStatsAggregationTaskTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTask.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/DailyBattleStatsAggregationTaskTest.kt
git commit -m "refactor(batch): shorten daily statistics transactions"
```

### Task 3: Refactor Weekly Event Battle Aggregation

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTaskTest.kt`

- [x] **Step 1: Replace the weekly aggregation test with an event-level transaction test**

Update `WeeklyEventBattleStatsAggregationTaskTest.kt` so the main test uses separate read/write entity managers and verifies transaction boundaries:

```kotlin
package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import java.time.LocalDate

class WeeklyEventBattleStatsAggregationTaskTest {
    @Test
    fun `주간 이벤트별 전투를 읽고 이벤트 단위 짧은 transaction 으로 집계를 교체한다`() {
        // given
        val readEm = mockk<EntityManager>()
        val writeEm = mockk<EntityManager>()
        val entityManagerFactory = mockk<EntityManagerFactory>()
        val txManager = mockk<PlatformTransactionManager>()
        val txStatus = mockk<TransactionStatus>(relaxed = true)
        val calls = mutableListOf<String>()
        val sut =
            WeeklyEventBattleStatsAggregationTask(
                entityManagerFactory = entityManagerFactory,
                transactionRunner = JpaBatchTransactionRunner(txManager, writeEm),
            )
        val weekRange =
            WeekRange(
                start = LocalDate.of(2026, 4, 27),
                end = LocalDate.of(2026, 4, 30),
            )
        val aggregators = mutableListOf<StreamingAggregator>()

        every { entityManagerFactory.createEntityManager() } returns readEm
        every { readEm.clear() } answers {
            calls.add("readEm.clear")
            Unit
        }
        every { readEm.close() } answers {
            calls.add("readEm.close")
            Unit
        }
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } answers {
            calls.add("tx.begin")
            txStatus
        }
        every { txManager.commit(txStatus) } answers {
            calls.add("tx.commit")
            Unit
        }
        every { writeEm.persist(any()) } answers {
            calls.add("writeEm.persist:${firstArg<Int>()}")
            Unit
        }
        every { writeEm.flush() } answers {
            calls.add("writeEm.flush")
            Unit
        }
        every { writeEm.clear() } answers {
            calls.add("writeEm.clear")
            Unit
        }

        // when
        sut.aggregate(
            jobName = "testWeeklyJob",
            weekRanges = listOf(weekRange),
            readDistinctEventIds = { _, requestedWeekRange ->
                calls.add("events:${requestedWeekRange.start}")
                listOf(10L, 20L)
            },
            readPage = { _, _, lastId, eventId ->
                calls.add("read:$eventId:$lastId")
                when (eventId to lastId) {
                    10L to 0L -> listOf(TestBattle(id = 1), TestBattle(id = 2))
                    20L to 0L -> listOf(TestBattle(id = 3))
                    else -> emptyList()
                }
            },
            getId = { it.id },
            aggregatorFactory = {
                StreamingAggregator().also { aggregators.add(it) }
            },
            deleteExisting = { _, _, eventId ->
                calls.add("delete:$eventId")
            },
            persistAdditional = { _, entity ->
                calls.add("additional:$entity")
            },
        )

        // then
        assertThat(aggregators.map { it.aggregatedIds.toList() })
            .containsExactly(
                listOf(1L, 2L),
                listOf(3L),
            )
        assertThat(calls).containsExactly(
            "events:2026-04-27",
            "read:10:0",
            "readEm.clear",
            "read:10:2",
            "tx.begin",
            "delete:10",
            "writeEm.persist:1",
            "additional:1",
            "writeEm.persist:2",
            "additional:2",
            "writeEm.flush",
            "writeEm.clear",
            "tx.commit",
            "read:20:0",
            "readEm.clear",
            "read:20:3",
            "tx.begin",
            "delete:20",
            "writeEm.persist:3",
            "additional:3",
            "writeEm.flush",
            "writeEm.clear",
            "tx.commit",
            "readEm.close",
        )
    }

    private data class TestBattle(
        val id: Long,
    )

    private class StreamingAggregator : BattleStatsAggregator<TestBattle, Int> {
        val aggregatedIds = mutableListOf<Long>()

        override fun aggregate(battle: TestBattle) {
            aggregatedIds.add(battle.id)
        }

        override fun forEachEntity(consumer: (Int) -> Unit) {
            aggregatedIds.forEach { consumer(it.toInt()) }
        }

        override fun toEntities(): List<Int> = error("toEntities must not be called")
    }
}
```

- [x] **Step 2: Run the weekly test to verify it fails**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.task.WeeklyEventBattleStatsAggregationTaskTest"
```

Expected: FAIL because `WeeklyEventBattleStatsAggregationTask` still requires `em` as an aggregate argument and has no constructor dependencies.

- [x] **Step 3: Refactor `WeeklyEventBattleStatsAggregationTask`**

Change the class constructor and aggregate method to own the read `EntityManager` and use `JpaBatchTransactionRunner` for each event replace:

```kotlin
@Component
class WeeklyEventBattleStatsAggregationTask(
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun <TBattle, TEntity : Any> aggregate(
        jobName: String,
        weekRanges: List<WeekRange>,
        readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
        readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
        persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
    ) {
        log.info("[{}] 주간 범위 {}개 처리 시작", jobName, weekRanges.size)

        entityManagerFactory.createEntityManager().use { readEm ->
            val queryFactory = JPAQueryFactory(readEm)
            for (weekRange in weekRanges) {
                processWeekRange(
                    readEm = readEm,
                    jobName = jobName,
                    queryFactory = queryFactory,
                    weekRange = weekRange,
                    readDistinctEventIds = readDistinctEventIds,
                    readPage = readPage,
                    getId = getId,
                    aggregatorFactory = aggregatorFactory,
                    deleteExisting = deleteExisting,
                    persist = persist,
                    persistAdditional = persistAdditional,
                )
            }
        }
    }
}
```

Inside `processWeekRange`, keep the read loop but replace the direct delete/flush block with:

```kotlin
transactionRunner.run { writeEm, writeQueryFactory ->
    deleteExisting(writeQueryFactory, weekRange, eventId)

    aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
        batch.forEach {
            persist(writeEm, it)
            persistAdditional(writeEm, it)
        }
        totalEntities += batch.size
        writeEm.flush()
        writeEm.clear()
    }
}
```

Use parameter name `readEm` instead of `em` in `processWeekRange`, and keep the final log unchanged.

- [x] **Step 4: Run the weekly test to verify it passes**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.task.WeeklyEventBattleStatsAggregationTaskTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTask.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/task/WeeklyEventBattleStatsAggregationTaskTest.kt
git commit -m "refactor(batch): shorten weekly statistics transactions"
```

### Task 4: Remove DB Transaction Manager From Statistics Tasklet Steps

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt`
- [x] **Step 1: Run the source guard to verify current long-transaction wiring is present**

Run:

```bash
rg -n "tasklet\\(\\{.*txManager|tasklet\\([^\\n]+txManager|private val txManager: PlatformTransactionManager|private val em: EntityManager|em = em" \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt
```

Expected: matches are printed for the current `txManager`, `EntityManager`, `em = em`, and `tasklet(..., txManager)` usages.

- [x] **Step 2: Update daily battle stats job configs**

In `BrawlerBattleResultStatsJobConfig.kt`:

- Remove imports `jakarta.persistence.EntityManager` and `org.springframework.transaction.PlatformTransactionManager`.
- Remove constructor fields `txManager` and `em`.
- Remove `em = em,` from `aggregationTask.aggregate`.
- Change `.tasklet({ ... }, txManager)` to `.tasklet { _, _ -> ... }`.

The step method should end like this:

```kotlin
@Bean(STEP_NAME)
@JobScope
fun brawlerBattleResultStatsStep(): Step =
    StepBuilder(STEP_NAME, jobRepository)
        .tasklet { _, _ ->
            aggregationTask.aggregate(
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
        }.build()
```

In `BrawlerBattleRankStatsJobConfig.kt`, apply the same constructor/import/tasklet changes. The aggregate call should use `statsRankBattleEntity` and `BrawlerBattleRankStatsAggregator()` with no `em` argument.

- [x] **Step 3: Update weekly battle stats job configs**

In each weekly config (`BrawlerPairBattleResultStatsJobConfig.kt`, `BrawlerPairBattleRankStatsJobConfig.kt`, `BrawlerEnemyBattleResultStatsJobConfig.kt`):

- Remove imports `jakarta.persistence.EntityManager` and `org.springframework.transaction.PlatformTransactionManager`.
- Remove constructor fields `txManager` and `em`.
- Remove `em = em,` from `aggregationTask.aggregate`.
- Change `.tasklet({ ... }, txManager)` to `.tasklet { _, _ -> ... }`.

For example, `BrawlerPairBattleResultStatsJobConfig.kt` step should start like:

```kotlin
@Bean(STEP_NAME)
@JobScope
fun brawlerPairBattleResultStatsStep(): Step =
    StepBuilder(STEP_NAME, jobRepository)
        .tasklet { _, _ ->
            aggregationTask.aggregate(
                jobName = JOB_NAME,
                weekRanges = StatsWeekRange.currentAndPrevious(dateJobParameter().date!!),
                readDistinctEventIds = { queryFactory, weekRange ->
                    queryFactory
                        .select(statsResultBattleEntity.eventBrawlStarsId)
                        .distinct()
                        .from(statsResultBattleEntity)
                        .where(statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                        .fetch()
                        .toList()
                },
                readPage = { queryFactory, weekRange, lastId, eventBrawlStarsId ->
                    queryFactory
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                            statsResultBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            statsResultBattleEntity.id.gt(lastId),
                        ).orderBy(statsResultBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()
                        .toList()
                },
                getId = { it.id!! },
                aggregatorFactory = { weekRange ->
                    BrawlerPairBattleResultStatsAggregator(
                        startBattleDate = weekRange.start,
                        endBattleDate = weekRange.end,
                    )
                },
                deleteExisting = { queryFactory, weekRange, eventBrawlStarsId ->
                    queryFactory
                        .delete(brawlerPairBattleResultStatsEntity)
                        .where(
                            brawlerPairBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                            brawlerPairBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                            brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                        ).execute()
                },
            )
            RepeatStatus.FINISHED
        }.build()
```

Keep each existing config's entity, aggregator, and delete target unchanged.

- [x] **Step 4: Update ownership stats job config**

In `BrawlerItemOwnershipStatsJobConfig.kt`:

- Remove import `org.springframework.transaction.PlatformTransactionManager`.
- Remove constructor field `txManager`.
- Change `.tasklet({ ... }, txManager)` to `.tasklet { _, _ -> ... }`.

The step method should be:

```kotlin
@Bean(STEP_NAME)
@JobScope
fun brawlerItemOwnershipStatsStep(): Step =
    StepBuilder(STEP_NAME, jobRepository)
        .tasklet { _, _ ->
            task.rebuild()
            RepeatStatus.FINISHED
        }.build()
```

- [x] **Step 5: Run the source guard and context load to verify the config change**

Run:

```bash
rg -n "tasklet\\(\\{.*txManager|tasklet\\([^\\n]+txManager|private val txManager: PlatformTransactionManager|private val em: EntityManager|em = em" \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt
```

Expected: no matches.

Then run:

```bash
./gradlew :core:core-batch:contextTest --tests "com.imstargg.CoreBatchApplicationTest"
```

Expected: PASS. This verifies the six job configs still wire into the Spring context after removing constructor dependencies and changing the tasklet builder overload.

- [x] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerPairBattleRankStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerEnemyBattleResultStatsJobConfig.kt \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/BrawlerItemOwnershipStatsJobConfig.kt
git commit -m "refactor(batch): use resourceless statistics tasklet steps"
```

### Task 5: Refactor Ownership Stats Writer

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriter.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriterTest.kt`

- [x] **Step 1: Update ownership writer test for table-level transactions**

Update the test setup to construct an actual `JpaBatchTransactionRunner` with mocked transaction manager and write entity manager:

```kotlin
private val txManager = mockk<PlatformTransactionManager>()
private val entityManager = mockk<EntityManager>()
private val sut =
    BrawlerItemOwnershipStatsWriter(
        gearRepository = gearRepository,
        gadgetRepository = gadgetRepository,
        starPowerRepository = starPowerRepository,
        hyperChargeRepository = hyperChargeRepository,
        buffyRepository = buffyRepository,
        transactionRunner = JpaBatchTransactionRunner(txManager, entityManager),
    )
```

In the test `given` block, add transaction stubbing:

```kotlin
every {
    txManager.getTransaction(
        match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
    )
} answers {
    mockk<TransactionStatus>(relaxed = true)
}
every { txManager.commit(any()) } just Runs
```

Keep the existing gear batch assertions, and add:

```kotlin
verify(exactly = 5) {
    txManager.getTransaction(
        match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
    )
}
verify(exactly = 5) { txManager.commit(any()) }
```

Add imports:

```kotlin
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
```

- [x] **Step 2: Run ownership writer test to verify it fails**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsWriterTest"
```

Expected: FAIL because `BrawlerItemOwnershipStatsWriter` does not accept `JpaBatchTransactionRunner`.

- [x] **Step 3: Refactor `BrawlerItemOwnershipStatsWriter`**

Remove `@Transactional` and the `EntityManager` constructor dependency. Inject `JpaBatchTransactionRunner` and replace each table in its own transaction:

```kotlin
@Component
class BrawlerItemOwnershipStatsWriter(
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(result: BrawlerItemOwnershipStatsResult) {
        replaceTable(
            deleteAll = { gearRepository.deleteAllInBatch() },
            entities = result.gears,
            saveAllAndFlush = { gearRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { gadgetRepository.deleteAllInBatch() },
            entities = result.gadgets,
            saveAllAndFlush = { gadgetRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { starPowerRepository.deleteAllInBatch() },
            entities = result.starPowers,
            saveAllAndFlush = { starPowerRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { hyperChargeRepository.deleteAllInBatch() },
            entities = result.hyperCharges,
            saveAllAndFlush = { hyperChargeRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { buffyRepository.deleteAllInBatch() },
            entities = result.buffies,
            saveAllAndFlush = { buffyRepository.saveAllAndFlush(it) },
        )
    }

    private fun <T : Any> replaceTable(
        deleteAll: () -> Unit,
        entities: List<T>,
        saveAllAndFlush: (List<T>) -> Unit,
    ) {
        transactionRunner.run { entityManager, _ ->
            deleteAll()
            entities.chunked(BATCH_SIZE).forEach { batch ->
                saveAllAndFlush(batch)
                entityManager.clear()
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
```

- [x] **Step 4: Run ownership writer test to verify it passes**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsWriterTest"
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriter.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/statistics/ownership/BrawlerItemOwnershipStatsWriterTest.kt
git commit -m "refactor(batch): replace ownership stats in short transactions"
```

### Task 6: Final Verification

**Files:**
- No source files expected beyond previous tasks.

- [x] **Step 1: Run focused statistics tests**

Run:

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunnerTest" \
  --tests "com.imstargg.core.batch.job.statistics.task.DailyBattleStatsAggregationTaskTest" \
  --tests "com.imstargg.core.batch.job.statistics.task.WeeklyEventBattleStatsAggregationTaskTest" \
  --tests "com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsWriterTest"
```

Expected: PASS.

- [x] **Step 2: Run core-batch context test**

Run:

```bash
./gradlew :core:core-batch:contextTest
```

Expected: PASS.

- [x] **Step 3: Run ktlint check for touched Kotlin files**

Run:

```bash
./gradlew :core:core-batch:ktlintCheck
```

Expected: PASS. If this task name is unavailable in the module, run repository-level `./gradlew ktlintCheck` instead.

- [x] **Step 4: Inspect remaining transaction-manager usage in statistics tasklet jobs**

Run:

```bash
rg -n "tasklet\\(\\{.*txManager|tasklet\\([^\\n]+txManager|private val txManager: PlatformTransactionManager|private val em: EntityManager" \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/statistics
```

Expected: no matches in the six statistics job configs covered by this plan. Matches in `StatsPlayerSyncJobConfig` are acceptable because that job uses chunk-oriented steps and is out of this plan's scope.

- [x] **Step 5: Commit any verification-only fixes**

If formatting or import cleanup changed files, commit them:

```bash
git add imstargg-backend/core/core-batch
git commit -m "test(batch): verify short statistics transactions"
```

If no files changed, do not create an empty commit.
