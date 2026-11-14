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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

            // then
            val strongBrawler = result[BrawlStarsId(1)]!!
            val averageBrawler = result[BrawlStarsId(2)]!!
            assertThat(strongBrawler.value).isGreaterThan(averageBrawler.value)
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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

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
            val result = TierScore.create(counts.associate { it.brawlerId to it.resultCount })

            // then
            assertThat(result[BrawlStarsId(1)]!!.value).isEqualTo(50)
        }

        @Test
        fun `점수는 수축 승률만으로 정해진다`() {
            // avg = 1160/2100 = 0.55238. few: adj = (100·0.6 + 300·avg)/400 = 0.56429 → 50 + 0.01190·200 = 52.38 → 52
            // many: adj = (1000·0.6 + 300·avg)/1300 = 0.58901 → 57.33 → 57.
            val counts =
                mapOf(
                    "many" to ResultCount(victoryCount = 600, defeatCount = 400, drawCount = 0),
                    "few" to ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                    "avg" to ResultCount(victoryCount = 500, defeatCount = 500, drawCount = 0),
                )

            val result = TierScore.create(counts)

            assertThat(result["few"]).isEqualTo(TierScore(52))
            assertThat(result["many"]).isEqualTo(TierScore(57))
        }

        @Test
        fun `n 이 0 인 키는 50 이다`() {
            val result = TierScore.create(mapOf("x" to ResultCount(0, 0, 0)))

            assertThat(result["x"]).isEqualTo(TierScore(50))
        }

        @Test
        fun `baselineWinRate 를 주면 그 값으로 수축한다`() {
            // a: adj = (60 + 300·0.5)/400 = 0.525 → 50 + 0.025·200 = 55. b: (40 + 150)/400 = 0.475 → 45
            val counts =
                mapOf(
                    "a" to ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                    "b" to ResultCount(victoryCount = 40, defeatCount = 60, drawCount = 0),
                )

            val result = TierScore.create(counts, baselineWinRate = 0.5)

            assertThat(result["a"]).isEqualTo(TierScore(55))
            assertThat(result["b"]).isEqualTo(TierScore(45))
        }

        @Test
        fun `baselineWinRate 가 없으면 입력 평균으로 수축한다`() {
            // 입력 평균 = 140/200 = 0.7.
            // 기준 없음 a: adj = (80 + 300·0.7)/400 = 0.725 → 50 + 0.025·200 = 55
            // 기준 0.5 a: adj = (80 + 300·0.5)/400 = 0.575 → 50 + 0.075·200 = 65
            val counts =
                mapOf(
                    "a" to ResultCount(victoryCount = 80, defeatCount = 20, drawCount = 0),
                    "b" to ResultCount(victoryCount = 60, defeatCount = 40, drawCount = 0),
                )

            val withoutBaseline = TierScore.create(counts)
            val withBaseline = TierScore.create(counts, baselineWinRate = 0.5)

            assertThat(withoutBaseline["a"]).isEqualTo(TierScore(55))
            assertThat(withBaseline["a"]).isEqualTo(TierScore(65))
            assertThat(withoutBaseline["a"]).isNotEqualTo(withBaseline["a"])
        }
    }
}
