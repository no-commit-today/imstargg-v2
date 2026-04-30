package com.imstargg.core.batch.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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

    @Test
    fun `엔티티가 없으면 flush 하지 않는다`() {
        // given
        val sut = TestAggregator(emptyList())
        val flushedChunks = mutableListOf<List<Int>>()

        // when
        sut.flushEntityChunks(chunkSize = 2) { chunk ->
            flushedChunks.add(chunk)
        }

        // then
        assertThat(flushedChunks).isEmpty()
    }

    @Test
    fun `마지막 chunk 가 가득 차면 빈 chunk 를 추가로 flush 하지 않는다`() {
        // given
        val sut = TestAggregator(listOf(1, 2, 3, 4))
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
            )
    }

    @Test
    fun `chunk size 는 0보다 커야 한다`() {
        // given
        val sut = TestAggregator(listOf(1))

        // when & then
        assertThatIllegalArgumentException()
            .isThrownBy { sut.flushEntityChunks(chunkSize = 0) { } }
            .withMessage("chunkSize must be greater than 0")
    }

    @Test
    fun `forEachEntity 를 override 하지 않으면 toEntities 결과를 사용한다`() {
        // given
        val sut = DefaultForEachAggregator(listOf(1, 2, 3))
        val flushedChunks = mutableListOf<List<Int>>()

        // when
        sut.flushEntityChunks(chunkSize = 2) { chunk ->
            flushedChunks.add(chunk)
        }

        // then
        assertThat(flushedChunks)
            .containsExactly(
                listOf(1, 2),
                listOf(3),
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

    private class DefaultForEachAggregator(
        private val entities: List<Int>,
    ) : BattleStatsAggregator<Unit, Int> {
        override fun aggregate(battle: Unit) = Unit

        override fun toEntities(): List<Int> = entities
    }
}
