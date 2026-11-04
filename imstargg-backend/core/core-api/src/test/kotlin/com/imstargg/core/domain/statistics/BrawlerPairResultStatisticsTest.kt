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
    fun `등급은 X 자기 평균이 아니라 전역 기준 승률 50% 로 수축해 정한다`() {
        // given - X(10) 의 조합 평균은 96/200 = 0.48 이다. 이 평균으로 수축하면 (10,20) 54·(10,30) 46 이지만 기준은 0.5 다.
        // (55,45) 는 50 + 0.0125·200 = 52.5 로 반올림 경계에 걸려(부동소수로 52.4999…) 피했다.
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 56, defeatCount = 44, drawCount = 0),
                ),
                BrawlerPairResultCount(
                    brawler1Id = BrawlStarsId(10L),
                    brawler2Id = BrawlStarsId(30L),
                    resultCount = ResultCount(victoryCount = 40, defeatCount = 60, drawCount = 0),
                ),
            )

        // when
        val sut =
            BrawlerPairResultStatistics.create(
                brawlerId = BrawlStarsId(10L),
                totalTeamCount = 1_000L,
                counts = counts,
            )

        // then - (10,20): adj = (56 + 150)/400 = 0.515 → 53(A). (10,30): (40 + 150)/400 = 0.475 → 45(C)
        val byPair = sut.items.associateBy { it.pairBrawlerId }
        assertThat(byPair.getValue(BrawlStarsId(20L)).tierScore).isEqualTo(TierScore(53))
        assertThat(byPair.getValue(BrawlStarsId(20L)).tierScore.grade()).isEqualTo(TierGrade.A)
        assertThat(byPair.getValue(BrawlStarsId(30L)).tierScore).isEqualTo(TierScore(45))
        assertThat(byPair.getValue(BrawlStarsId(30L)).tierScore.grade()).isEqualTo(TierGrade.C)
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
