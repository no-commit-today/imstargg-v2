package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerBattleResultStatsWriterTest {
    private val em = mockk<EntityManager>(relaxed = true)
    private val sut = BrawlerBattleResultStatsWriter(em)

    @Test
    fun `chunk 단위로 persist 한 뒤 flush 와 clear 를 호출한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val aggregator = mockk<BrawlerBattleResultStatsAggregator>()
        val persistedEntities = mutableListOf<BrawlerBattleResultStatsEntity>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            val flush = secondArg<(List<BrawlerBattleResultStatsEntity>) -> Unit>()
            // 1500개 -> 1000 + 500 두 청크
            flush(List(BattleStatsConstants.BATCH_SIZE) { mockk<BrawlerBattleResultStatsEntity>() })
            flush(List(500) { mockk<BrawlerBattleResultStatsEntity>() })
        }
        every { em.persist(any()) } answers {
            persistedEntities.add(firstArg<BrawlerBattleResultStatsEntity>())
        }

        // when
        val totalEntities = sut.write(date, aggregator)

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
        val date = LocalDate.of(2026, 5, 1)
        val aggregator = mockk<BrawlerBattleResultStatsAggregator>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            val flush = secondArg<(List<BrawlerBattleResultStatsEntity>) -> Unit>()
            flush(List(10) { mockk<BrawlerBattleResultStatsEntity>() })
        }

        // when
        val totalEntities = sut.write(date, aggregator)

        // then
        assertThat(totalEntities).isEqualTo(10L)
        verify(exactly = 10) { em.persist(any()) }
        verify(exactly = 1) { em.flush() }
        verify(exactly = 1) { em.clear() }
    }

    @Test
    fun `집계 엔티티가 없으면 persist 와 flush 를 호출하지 않는다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val aggregator = mockk<BrawlerBattleResultStatsAggregator>()

        every { aggregator.flushEntityChunks(eq(BattleStatsConstants.BATCH_SIZE), any()) } answers {
            // flush 콜백을 호출하지 않음
        }

        // when
        val totalEntities = sut.write(date, aggregator)

        // then
        assertThat(totalEntities).isZero()
        verify(exactly = 0) { em.persist(any()) }
        verify(exactly = 0) { em.flush() }
        verify(exactly = 0) { em.clear() }
    }
}
