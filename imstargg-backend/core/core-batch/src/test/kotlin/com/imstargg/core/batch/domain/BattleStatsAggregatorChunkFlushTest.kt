package com.imstargg.core.batch.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BattleStatsAggregatorChunkFlushTest {
    @Test
    fun `엔티티를 chunk size 만큼 flush 하고 남은 엔티티도 flush 한다`() {
        // given
        val sut = TestAggregator(listOf(1, 2, 3, 4, 5))
        val flushedChunks = mutableListOf<List<Int>>()

        // when
        sut.flushEntityChunks(chunkSize = 2) { chunk ->
            flushedChunks.add(chunk)
        }

        // then
        assertThat(flushedChunks)
            .containsExactly(
                listOf(1, 2),
                listOf(3, 4),
                listOf(5),
            )
    }

    private class TestAggregator(
        private val entities: List<Int>,
    ) : BattleStatsAggregator<Unit, Int> {
        override fun aggregate(battle: Unit) = Unit

        override fun forEachEntity(consumer: (Int) -> Unit) {
            entities.forEach(consumer)
        }

        override fun toEntities(): List<Int> = entities
    }
}
