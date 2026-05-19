package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
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

class DailyBattleStatsAggregationTaskTest {
    @Test
    fun `일별 전투를 모두 읽은 뒤 날짜 단위 짧은 transaction 으로 집계를 교체한다`() {
        // given
        val entityManagerFactory = mockk<EntityManagerFactory>()
        val readEm = mockk<EntityManager>()
        val writeEm = mockk<EntityManager>()
        val txManager = mockk<PlatformTransactionManager>()
        val txStatus = mockk<TransactionStatus>(relaxed = true)
        val calls = mutableListOf<String>()
        val transactionRunner = JpaBatchTransactionRunner(txManager, writeEm)
        val sut = DailyBattleStatsAggregationTask(entityManagerFactory, transactionRunner)
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
