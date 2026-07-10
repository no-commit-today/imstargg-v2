package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class BrawlerPairRankStatisticsTest {
    @Test
    fun `create 는 weightedRankSum 을 totalCount 로 나눠 averageRank 를 계산한다`() {
        // given - 1위 2회 + 3위 1회 → weightedRankSum=5, totalCount=3
        val counts =
            listOf(
                BrawlerPairRankCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    totalCount = 3L,
                    weightedRankSum = 5L,
                ),
            )

        // when
        val sut = BrawlerPairRankStatistics.create(totalBattleCount = 3L, totalElementCount = 1L, counts = counts)

        // then
        assertThat(sut.totalBattleCount).isEqualTo(3L)
        assertThat(sut.totalElementCount).isEqualTo(1L)
        val item = sut.items.single()
        assertThat(item.totalBattleCount).isEqualTo(3L)
        assertThat(item.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
    }

    @Test
    fun `totalCount 0 이면 averageRank 0`() {
        // given
        val counts =
            listOf(
                BrawlerPairRankCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    totalCount = 0L,
                    weightedRankSum = 0L,
                ),
            )

        // when
        val sut = BrawlerPairRankStatistics.create(totalBattleCount = 0L, totalElementCount = 0L, counts = counts)

        // then
        assertThat(sut.items.single().averageRank).isEqualTo(0.0)
    }
}
