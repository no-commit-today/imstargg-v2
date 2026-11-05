package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TrioResultRankingTest {
    @Test
    fun `counts 가 비면 items 가 비고 예외가 없다`() {
        // when
        val sut = TrioResultRanking.create(totalTeamCount = 0L, counts = emptyList())

        // then
        assertThat(sut.items).isEmpty()
    }

    @Test
    fun `brawlerIds 는 brawler1, brawler2, brawler3 순이다`() {
        // given - 조합 100판 (승60 패40), 전체 400팀
        val counts =
            listOf(
                BrawlerTrioResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    brawler3Id = BrawlStarsId(30L),
                    resultCount = ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                ),
            )

        // when
        val sut = TrioResultRanking.create(totalTeamCount = 400L, counts = counts)

        // then
        val item = sut.items.single()
        assertThat(item.brawlerIds).containsExactly(BrawlStarsId(10L), BrawlStarsId(20L), BrawlStarsId(30L))
        assertThat(item.battleCount).isEqualTo(100L)
        assertThat(item.pickRate).isEqualTo(0.25)
        assertThat(item.winRate).isEqualTo(0.6)
    }
}
