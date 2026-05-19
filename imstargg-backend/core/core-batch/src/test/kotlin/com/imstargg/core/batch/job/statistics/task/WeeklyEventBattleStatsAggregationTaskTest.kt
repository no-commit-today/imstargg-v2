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
        val entityManagerFactory = mockk<EntityManagerFactory>()
        val readEm = mockk<EntityManager>()
        val writeEm = mockk<EntityManager>()
        val txManager = mockk<PlatformTransactionManager>()
        val txStatus = mockk<TransactionStatus>(relaxed = true)
        val calls = mutableListOf<String>()
        val transactionRunner = JpaBatchTransactionRunner(txManager, writeEm)
        val sut =
            WeeklyEventBattleStatsAggregationTask(
                entityManagerFactory = entityManagerFactory,
                transactionRunner = transactionRunner,
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
