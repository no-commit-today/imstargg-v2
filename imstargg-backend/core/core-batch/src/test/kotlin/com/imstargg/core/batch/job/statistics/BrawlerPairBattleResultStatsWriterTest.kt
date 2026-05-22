package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerPairBattleResultStatsWriterTest {
    private val em = mockk<EntityManager>(relaxed = true)
    private val sut = BrawlerPairBattleResultStatsWriter(em)

    @Test
    fun `chunk 단위로 persist 한 뒤 flush 와 clear 를 호출한다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val eventBrawlStarsId = 10L
        val aggregator = mockk<BrawlerPairBattleResultStatsAggregator>()
        val persistedEntities = mutableListOf<BrawlerPairBattleResultStatsEntity>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            val flush = secondArg<(List<BrawlerPairBattleResultStatsEntity>) -> Unit>()
            // 1500개 -> 1000 + 500 두 청크
            flush(List(BattleStatsConstants.BATCH_SIZE) { mockk<BrawlerPairBattleResultStatsEntity>() })
            flush(List(500) { mockk<BrawlerPairBattleResultStatsEntity>() })
        }
        every { em.persist(any()) } answers {
            persistedEntities.add(firstArg<BrawlerPairBattleResultStatsEntity>())
        }

        // when
        val totalEntities =
            sut.write(
                weekRange = weekRange,
                eventBrawlStarsId = eventBrawlStarsId,
                aggregator = aggregator,
            )

        // then
        assertThat(totalEntities).isEqualTo(1500L)
        assertThat(persistedEntities).hasSize(1500)
        verify(exactly = 1) { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) }
        verify(exactly = 2) { em.flush() }
        verify(exactly = 2) { em.clear() }
    }

    @Test
    fun `엔티티가 한 chunk 미만이면 한 번만 flush 하고 clear 한다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val eventBrawlStarsId = 10L
        val aggregator = mockk<BrawlerPairBattleResultStatsAggregator>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            val flush = secondArg<(List<BrawlerPairBattleResultStatsEntity>) -> Unit>()
            flush(List(10) { mockk<BrawlerPairBattleResultStatsEntity>() })
        }

        // when
        val totalEntities =
            sut.write(
                weekRange = weekRange,
                eventBrawlStarsId = eventBrawlStarsId,
                aggregator = aggregator,
            )

        // then
        assertThat(totalEntities).isEqualTo(10L)
        verify(exactly = 10) { em.persist(any()) }
        verify(exactly = 1) { em.flush() }
        verify(exactly = 1) { em.clear() }
    }

    @Test
    fun `집계 엔티티가 없으면 persist 와 flush 를 호출하지 않는다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val eventBrawlStarsId = 10L
        val aggregator = mockk<BrawlerPairBattleResultStatsAggregator>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            // flush 콜백을 호출하지 않음
        }

        // when
        val totalEntities =
            sut.write(
                weekRange = weekRange,
                eventBrawlStarsId = eventBrawlStarsId,
                aggregator = aggregator,
            )

        // then
        assertThat(totalEntities).isZero()
        verify(exactly = 0) { em.persist(any()) }
        verify(exactly = 0) { em.flush() }
        verify(exactly = 0) { em.clear() }
    }
}
