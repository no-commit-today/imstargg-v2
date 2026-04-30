package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.enums.WeekRange
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WeeklyEventBattleStatsAggregationTaskTest {
    @Test
    fun `주간 이벤트별 전투를 페이지 단위로 읽고 부가 저장 콜백을 함께 실행한다`() {
        // given
        val em = mockk<EntityManager>()
        val calls = mutableListOf<String>()
        val sut = WeeklyEventBattleStatsAggregationTask()
        val weekRange =
            WeekRange(
                start = LocalDate.of(2026, 4, 27),
                end = LocalDate.of(2026, 4, 30),
            )
        val aggregators = mutableListOf<StreamingAggregator>()
        every { em.persist(any()) } answers {
            calls.add("em.persist:${firstArg<Int>()}")
            Unit
        }
        every { em.flush() } just Runs
        every { em.clear() } answers {
            calls.add("em.clear")
            Unit
        }

        // when
        sut.aggregate(
            em = em,
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
            "em.clear",
            "read:10:2",
            "delete:10",
            "em.persist:1",
            "additional:1",
            "em.persist:2",
            "additional:2",
            "em.clear",
            "read:20:0",
            "em.clear",
            "read:20:3",
            "delete:20",
            "em.persist:3",
            "additional:3",
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
