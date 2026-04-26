package com.imstargg.infrastructure.db.core.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RankCountsEmbeddedTest {
    @Test
    fun `fromMap 은 항상 10칸 rank count 를 만든다`() {
        val result = RankCountsEmbedded.fromMap(mapOf(5 to 2L))

        assertThat(result.counts).hasSize(RankCountsEmbedded.MAX_RANK)
        assertThat(result.countOf(5)).isEqualTo(2)
        assertThat(result.totalCount).isEqualTo(2)
    }

    @Test
    fun `다른 rank 범위에서 만들어진 count 도 merge 할 수 있다`() {
        val lowRankCount = RankCountsEmbedded.fromMap(mapOf(2 to 1L))
        val highRankCount = RankCountsEmbedded.fromMap(mapOf(5 to 2L))

        val result = lowRankCount + highRankCount

        assertThat(result.counts).hasSize(RankCountsEmbedded.MAX_RANK)
        assertThat(result.countOf(2)).isEqualTo(1)
        assertThat(result.countOf(5)).isEqualTo(2)
        assertThat(result.totalCount).isEqualTo(3)
        assertThat(result.weightedRankSum()).isEqualTo(12)
    }

    @Test
    fun `짧은 count 와 10칸 count 도 0 padding 으로 merge 한다`() {
        val shortCount = RankCountsEmbedded(counts = listOf(1L, 1L), totalCount = 2)
        val fullCount = RankCountsEmbedded.fromMap(mapOf(5 to 1L))

        val result = shortCount + fullCount

        assertThat(result.counts).hasSize(RankCountsEmbedded.MAX_RANK)
        assertThat(result.countOf(1)).isEqualTo(1)
        assertThat(result.countOf(2)).isEqualTo(1)
        assertThat(result.countOf(5)).isEqualTo(1)
        assertThat(result.totalCount).isEqualTo(3)
    }
}
