package com.imstargg.core.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class RankCountTest {
    @Test
    fun `merge 는 순위별 카운트를 합산한다`() {
        // given
        val sut = RankCount(mapOf(1 to 2L, 3 to 1L))

        // when
        val result = sut.merge(RankCount(mapOf(1 to 1L, 2 to 4L)))

        // then
        assertThat(result).isEqualTo(RankCount(mapOf(1 to 3L, 2 to 4L, 3 to 1L)))
    }

    @Test
    fun `averageRank 는 가중 평균이다`() {
        // given - 1위 2회, 3위 1회 → (1*2 + 3*1) / 3
        val sut = RankCount(mapOf(1 to 2L, 3 to 1L))

        // when // then
        assertThat(sut.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
    }

    @Test
    fun `빈 카운트의 averageRank 는 0`() {
        // when // then
        assertThat(RankCount(emptyMap()).averageRank).isEqualTo(0.0)
    }
}
