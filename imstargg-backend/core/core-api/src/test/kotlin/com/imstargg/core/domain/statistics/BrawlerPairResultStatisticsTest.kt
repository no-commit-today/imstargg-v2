package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerPairResultStatisticsTest {
    @Test
    fun `X 가 brawler2 이면 pairBrawlerId 는 brawler1 이다`() {
        // given
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(5L),
                    brawler2Id = BrawlStarsId(10L),
                    resultCount = ResultCount(victoryCount = 20, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerPairResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        val item = sut.items.single()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(10L))
        assertThat(item.pairBrawlerId).isEqualTo(BrawlStarsId(5L))
    }

    @Test
    fun `픽률 분모는 팀 수다`() {
        // given - 조합 팀 40 (승30 패10), 전체 100팀
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerPairResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        val item = sut.items.single()
        assertThat(item.pairBrawlerId).isEqualTo(BrawlStarsId(20L))
        assertThat(item.battleCount).isEqualTo(40L)
        assertThat(item.pickRate).isEqualTo(0.4)
        assertThat(item.winRate).isEqualTo(0.75)
    }

    @Test
    fun `battleCount 30 미만은 빠진다`() {
        // given
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 20, defeatCount = 9, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerPairResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 100L,
                counts = counts,
            )

        // then
        assertThat(sut.items).isEmpty()
    }

    @Test
    fun `tierScore 내림차순이고 상위 200 개만 남는다`() {
        // given - 각 총 30판, 승률이 서로 다른 201개 조합. 입력은 승률 오름차순이라 가장 높은 승률이 맨 뒤다.
        val brawlerId = BrawlStarsId(1L)
        val winRateFractions =
            (1L..30L)
                .flatMap { decided -> (0L..decided).map { victory -> victory to decided } }
                .distinctBy { (victory, decided) -> victory.toDouble() / decided }
                .take(201)
                .sortedBy { (victory, decided) -> victory.toDouble() / decided }
        val counts =
            winRateFractions.mapIndexed { index, (victory, decided) ->
                BrawlerPairResultCount(
                    brawler1Id = brawlerId,
                    brawler2Id = BrawlStarsId(100L + index),
                    resultCount =
                        ResultCount(
                            victoryCount = victory,
                            defeatCount = decided - victory,
                            drawCount = 30L - decided,
                        ),
                )
            }
        val highest = counts.maxBy { it.resultCount.winRate }
        val lowest = counts.minBy { it.resultCount.winRate }

        // when
        val sut = BrawlerPairResultStatistics.create(brawlerId = brawlerId, totalTeamCount = 10_000L, counts = counts)

        // then
        assertThat(counts).hasSize(201)
        assertThat(counts.map { it.resultCount.winRate }).doesNotHaveDuplicates()
        assertThat(sut.items).hasSize(200)
        assertThat(sut.items.first().pairBrawlerId).isEqualTo(highest.brawler2Id)
        assertThat(sut.items.map { it.pairBrawlerId }).doesNotContain(lowest.brawler2Id)
        assertThat(sut.items.map { it.tierScore.value }).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `totalTeamCount 0 이면 pickRate 0`() {
        // given
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerPairResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 0L,
                counts = counts,
            )

        // then
        assertThat(sut.items.single().pickRate).isEqualTo(0.0)
    }
}
