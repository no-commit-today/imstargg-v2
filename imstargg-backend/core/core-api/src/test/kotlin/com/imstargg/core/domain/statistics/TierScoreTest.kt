package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TierScoreTest {
    @Nested
    inner class Grade {
        @Test
        fun `점수가 55 이상이면 S 티어이다`() {
            assertThat(TierScore(55).grade()).isEqualTo(TierGrade.S)
            assertThat(TierScore(60).grade()).isEqualTo(TierGrade.S)
        }

        @Test
        fun `점수가 52 이상 55 미만이면 A 티어이다`() {
            assertThat(TierScore(52).grade()).isEqualTo(TierGrade.A)
            assertThat(TierScore(54).grade()).isEqualTo(TierGrade.A)
        }

        @Test
        fun `점수가 48 이상 52 미만이면 B 티어이다`() {
            assertThat(TierScore(48).grade()).isEqualTo(TierGrade.B)
            assertThat(TierScore(51).grade()).isEqualTo(TierGrade.B)
        }

        @Test
        fun `점수가 45 이상 48 미만이면 C 티어이다`() {
            assertThat(TierScore(45).grade()).isEqualTo(TierGrade.C)
            assertThat(TierScore(47).grade()).isEqualTo(TierGrade.C)
        }

        @Test
        fun `점수가 45 미만이면 D 티어이다`() {
            assertThat(TierScore(44).grade()).isEqualTo(TierGrade.D)
            assertThat(TierScore(0).grade()).isEqualTo(TierGrade.D)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `빈 counts면 빈 맵을 반환한다`() {
            // given
            val counts = emptyList<BrawlerResultCount>()

            // when
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

            // then
            assertThat(result[BrawlStarsId(1)]!!.grade()).isEqualTo(TierGrade.B)
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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount }, pickRateWeight = true)

            // then
            assertThat(result[BrawlStarsId(1)]!!.value).isEqualTo(50)
        }

        @Test
        fun `가중을 끄면 점수는 수축 승률만으로 정해진다`() {
            // avg = 1160/2100 = 0.55238. few: adj = (100·0.6 + 300·avg)/400 = 0.56429 → 50 + 0.01190·200 = 52.38 → 52
            // many: adj = (1000·0.6 + 300·avg)/1300 = 0.58901 → 57.33 → 57. 가중 on 이면 few 의 픽률 가중 0.506 으로 51.
            val counts =
                mapOf(
                    "many" to ResultCount(victoryCount = 600, defeatCount = 400, drawCount = 0),
                    "few" to ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                    "avg" to ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                )

            val off = TierScore.create(counts, pickRateWeight = false)
            val on = TierScore.create(counts, pickRateWeight = true)

            assertThat(off["few"]).isEqualTo(TierScore(52))
            assertThat(off["many"]).isEqualTo(TierScore(57))
            assertThat(on["few"]!!.value).isLessThan(off["few"]!!.value)
        }

        @Test
        fun `가중을 꺼도 n 이 0 인 키는 50 이다`() {
            val result = TierScore.create(mapOf("x" to ResultCount(0, 0, 0)), pickRateWeight = false)

            assertThat(result["x"]).isEqualTo(TierScore(50))
        }
    }
}
