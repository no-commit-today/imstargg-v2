package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerTierScoreTest {
    @Nested
    inner class Tier {
        @Test
        fun `점수가 55 이상이면 S 티어이다`() {
            assertThat(BrawlerTierScore(55).tier()).isEqualTo(BrawlerTier.S)
            assertThat(BrawlerTierScore(60).tier()).isEqualTo(BrawlerTier.S)
        }

        @Test
        fun `점수가 52 이상 55 미만이면 A 티어이다`() {
            assertThat(BrawlerTierScore(52).tier()).isEqualTo(BrawlerTier.A)
            assertThat(BrawlerTierScore(54).tier()).isEqualTo(BrawlerTier.A)
        }

        @Test
        fun `점수가 48 이상 52 미만이면 B 티어이다`() {
            assertThat(BrawlerTierScore(48).tier()).isEqualTo(BrawlerTier.B)
            assertThat(BrawlerTierScore(51).tier()).isEqualTo(BrawlerTier.B)
        }

        @Test
        fun `점수가 45 이상 48 미만이면 C 티어이다`() {
            assertThat(BrawlerTierScore(45).tier()).isEqualTo(BrawlerTier.C)
            assertThat(BrawlerTierScore(47).tier()).isEqualTo(BrawlerTier.C)
        }

        @Test
        fun `점수가 45 미만이면 D 티어이다`() {
            assertThat(BrawlerTierScore(44).tier()).isEqualTo(BrawlerTier.D)
            assertThat(BrawlerTierScore(0).tier()).isEqualTo(BrawlerTier.D)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `빈 counts면 빈 맵을 반환한다`() {
            // given
            val counts = emptyList<BrawlerResultCount>()

            // when
            val result = BrawlerTierScore.create(counts)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `평균적인 승률과 픽률을 가진 브롤러는 B 티어이다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                        starPlayerCount = 100,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(2),
                        resultCount = ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                        starPlayerCount = 100,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            assertThat(result[BrawlStarsId(1)]!!.tier()).isEqualTo(BrawlerTier.B)
        }

        @Test
        fun `높은 승률과 높은 픽률을 가진 브롤러는 높은 점수를 받는다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 5500, defeatCount = 4500, drawCount = 0),
                        starPlayerCount = 1000,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(2),
                        resultCount = ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                        starPlayerCount = 100,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            val strongBrawler = result[BrawlStarsId(1)]!!
            val averageBrawler = result[BrawlStarsId(2)]!!
            assertThat(strongBrawler.value).isGreaterThan(averageBrawler.value)
        }

        @Test
        fun `높은 승률이라도 매우 낮은 픽률이면 점수가 억제된다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 55, defeatCount = 45, drawCount = 0),
                        starPlayerCount = 10,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(2),
                        resultCount = ResultCount(victoryCount = 5500, defeatCount = 4500, drawCount = 0),
                        starPlayerCount = 1000,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(3),
                        resultCount = ResultCount(victoryCount = 5000, defeatCount = 5000, drawCount = 0),
                        starPlayerCount = 500,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            val lowPickHighWinBrawler = result[BrawlStarsId(1)]!!
            val highPickHighWinBrawler = result[BrawlStarsId(2)]!!
            assertThat(lowPickHighWinBrawler.value).isLessThan(highPickHighWinBrawler.value)
        }

        @Test
        fun `낮은 승률과 높은 픽률을 가진 브롤러는 낮은 점수를 받는다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 4000, defeatCount = 6000, drawCount = 0),
                        starPlayerCount = 500,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(2),
                        resultCount = ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                        starPlayerCount = 100,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            val weakBrawler = result[BrawlStarsId(1)]!!
            assertThat(weakBrawler.value).isLessThan(50)
        }

        @Test
        fun `점수는 항상 0 이상이다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 100, defeatCount = 9900, drawCount = 0),
                        starPlayerCount = 0,
                    ),
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(2),
                        resultCount = ResultCount(victoryCount = 5000, defeatCount = 5000, drawCount = 0),
                        starPlayerCount = 500,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            result.values.forEach { tierScore ->
                assertThat(tierScore.value).isGreaterThanOrEqualTo(0)
            }
        }

        @Test
        fun `단일 브롤러는 자신이 평균이므로 점수가 50이다`() {
            // given
            val counts =
                listOf(
                    BrawlerResultCount(
                        brawlerId = BrawlStarsId(1),
                        resultCount = ResultCount(victoryCount = 5500, defeatCount = 4500, drawCount = 0),
                        starPlayerCount = 500,
                    ),
                )
            // when
            val result = BrawlerTierScore.create(counts)

            // then
            assertThat(result[BrawlStarsId(1)]!!.value).isEqualTo(50)
        }
    }
}
