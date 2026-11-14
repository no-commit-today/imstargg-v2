package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerTrioResultStatisticsTest {
    @Test
    fun `X 가 brawler3 이면 pairBrawlerIds 는 brawler1, brawler2 순이다`() {
        // given
        val counts =
            listOf(
                BrawlerTrioResultCount(
                    brawler1Id = BrawlStarsId(5L),
                    brawler2Id = BrawlStarsId(7L),
                    brawler3Id = BrawlStarsId(10L),
                    resultCount = ResultCount(victoryCount = 20, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerTrioResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        val item = sut.items.single()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(10L))
        assertThat(item.pairBrawlerIds).containsExactly(BrawlStarsId(5L), BrawlStarsId(7L))
    }

    @Test
    fun `픽률 분모는 팀 수 그대로다`() {
        // given - 조합 팀 40 (승30 패10), 전체 100팀. 3인 조합 행 하나가 팀 하나라 ÷3 을 하지 않는다.
        val counts =
            listOf(
                BrawlerTrioResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    brawler3Id = BrawlStarsId(30L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerTrioResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        val item = sut.items.single()
        assertThat(item.pairBrawlerIds).containsExactly(BrawlStarsId(20L), BrawlStarsId(30L))
        assertThat(item.battleCount).isEqualTo(40L)
        assertThat(item.pickRate).isEqualTo(0.4)
        assertThat(item.winRate).isEqualTo(0.75)
    }

    @Test
    fun `battleCount 30 미만은 빠진다`() {
        // given
        val counts =
            listOf(
                BrawlerTrioResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    brawler3Id = BrawlStarsId(30L),
                    resultCount = ResultCount(victoryCount = 20, defeatCount = 9, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerTrioResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        assertThat(sut.items).isEmpty()
    }
}
