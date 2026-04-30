package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailyBattleStatsAggregationTaskTest {
    @Test
    fun `일별 전투를 페이지 단위로 읽고 스트리밍 집계 엔티티를 저장한다`() {
        // given
        val em = mockk<EntityManager>()
        val calls = mutableListOf<String>()
        val sut = DailyBattleStatsAggregationTask()
        val date = LocalDate.of(2026, 4, 30)
        val aggregator = StreamingAggregator()
        every { em.persist(any()) } answers {
            calls.add("em.persist:${firstArg<Int>()}")
            Unit
        }
        every { em.flush() } answers {
            calls.add("em.flush")
            Unit
        }
        every { em.clear() } answers {
            calls.add("em.clear")
            Unit
        }

        // when
        sut.aggregate(
            em = em,
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
            "em.clear",
            "read:2",
            "em.clear",
            "read:3",
            "delete:2026-04-30",
            "em.persist:1",
            "em.persist:2",
            "em.persist:3",
            "em.flush",
            "em.clear",
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
