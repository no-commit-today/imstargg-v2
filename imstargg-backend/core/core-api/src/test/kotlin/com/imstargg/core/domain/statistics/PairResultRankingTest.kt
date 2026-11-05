package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PairResultRankingTest {
    @Test
    fun `battleCount 100 미만은 빠진다`() {
        // given - 총 99판 조합과 총 100판 조합
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 50, defeatCount = 49, drawCount = 0),
                ),
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(30L),
                    brawler2Id = BrawlStarsId(40L),
                    resultCount = ResultCount(victoryCount = 50, defeatCount = 50, drawCount = 0),
                ),
            )

        // when
        val sut = PairResultRanking.create(totalTeamCount = 1_000L, counts = counts)

        // then
        assertThat(sut.items.map { it.brawlerIds })
            .containsExactly(listOf(BrawlStarsId(30L), BrawlStarsId(40L)))
    }

    @Test
    fun `tierScore 내림차순이고 상위 50 개만 남는다`() {
        // given - 각 총 100판, 승수 0, 2, …, 100 의 51개 조합. 평균 승률 0.5 라 점수는 25 + 승수/2 로 모두 다르다.
        // 입력은 승률 오름차순이라 가장 높은 승률이 맨 뒤다.
        val counts =
            (0..50).map { index ->
                val victoryCount = 2L * index
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(1L),
                    brawler2Id = BrawlStarsId(100L + index),
                    resultCount =
                        ResultCount(
                            victoryCount = victoryCount,
                            defeatCount = 100L - victoryCount,
                            drawCount = 0L,
                        ),
                )
            }
        val highest = counts.maxBy { it.resultCount.winRate }
        val lowest = counts.minBy { it.resultCount.winRate }

        // when
        val sut = PairResultRanking.create(totalTeamCount = 10_000L, counts = counts)

        // then
        assertThat(counts).hasSize(51)
        assertThat(sut.items).hasSize(50)
        assertThat(sut.items.first().brawlerIds).containsExactly(highest.brawler1Id, highest.brawler2Id)
        assertThat(sut.items.map { it.brawlerIds })
            .doesNotContain(listOf(lowest.brawler1Id, lowest.brawler2Id))
        assertThat(sut.items.map { it.tierScore.value }).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `brawlerIds 는 brawler1, brawler2 순이다`() {
        // given - 조합 100판 (승60 패40), 전체 200팀
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                ),
            )

        // when
        val sut = PairResultRanking.create(totalTeamCount = 200L, counts = counts)

        // then
        val item = sut.items.single()
        assertThat(item.brawlerIds).containsExactly(BrawlStarsId(10L), BrawlStarsId(20L))
        assertThat(item.battleCount).isEqualTo(100L)
        assertThat(item.pickRate).isEqualTo(0.5)
        assertThat(item.winRate).isEqualTo(0.6)
    }
}
