package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class BrawlerRankStatisticsTest {
    @Test
    fun `create 는 같은 브롤러의 count 를 병합해 item 을 만든다`() {
        // given
        val counts =
            listOf(
                BrawlerRankCount(BrawlStarsId(10L), RankCount(mapOf(1 to 2L))),
                BrawlerRankCount(BrawlStarsId(10L), RankCount(mapOf(3 to 1L))),
                BrawlerRankCount(BrawlStarsId(20L), RankCount(mapOf(2 to 2L))),
            )

        // when
        val sut = BrawlerRankStatistics.create(counts)

        // then
        assertThat(sut.items).hasSize(2)
        val item10 = sut.items.first { it.brawlerId == BrawlStarsId(10L) }
        assertThat(item10.totalBattleCount).isEqualTo(3L)
        assertThat(item10.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
        val item20 = sut.items.first { it.brawlerId == BrawlStarsId(20L) }
        assertThat(item20.averageRank).isEqualTo(2.0)
    }

    @Test
    fun `빈 입력은 빈 items`() {
        // when
        val sut = BrawlerRankStatistics.create(emptyList())

        // then
        assertThat(sut.items).isEmpty()
    }
}
