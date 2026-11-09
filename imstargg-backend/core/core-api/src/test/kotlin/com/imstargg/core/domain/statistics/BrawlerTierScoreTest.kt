package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerTierScoreTest {
    private val average = count(5000, 5000)

    /** 입력 순서대로 브롤러 ID 0, 1, 2 … 를 붙여 점수를 낸다. */
    private fun create(vararg counts: ResultCount): List<BrawlerTierScore> {
        val scores =
            BrawlerTierScore.create(
                counts.withIndex().associate { (index, count) -> BrawlStarsId(index.toLong()) to count },
            )
        return counts.indices.map { scores.getValue(BrawlStarsId(it.toLong())) }
    }

    private fun count(
        victory: Long,
        defeat: Long,
        draw: Long = 0,
    ) = ResultCount(victoryCount = victory, defeatCount = defeat, drawCount = draw)

    private fun score(
        value: Int,
        grade: TierGrade?,
    ) = BrawlerTierScore(value = value, grade = grade)

    private val b50 = score(50, TierGrade.B)

    @Nested
    inner class Create {
        @Test
        fun `모두 같으면 50점 B 다`() {
            assertThat(create(count(500, 500), count(500, 500), count(500, 500))).containsExactly(b50, b50, b50)
        }

        @Test
        fun `인기만 높으면 54점이고 B 에 머문다`() {
            val small = count(500, 500)
            assertThat(create(count(50000, 50000), small, small, small, small))
                .containsExactly(score(54, TierGrade.B), b50, b50, b50, b50)
        }

        @Test
        fun `인기만 낮으면 46점이고 B 에 머문다`() {
            assertThat(create(count(150, 150), average, average, average, average))
                .containsExactly(score(46, TierGrade.B), b50, b50, b50, b50)
        }

        @Test
        fun `픽률이 같으면 승률 차이만으로 S 부터 D 까지 갈린다`() {
            assertThat(create(count(5600, 4400), count(5300, 4700), average, count(4700, 5300), count(4400, 5600)))
                .containsExactly(
                    score(62, TierGrade.S),
                    score(56, TierGrade.A),
                    b50,
                    score(44, TierGrade.C),
                    score(38, TierGrade.D),
                )
        }

        @Test
        fun `픽률이 중앙값의 2배면 가산이 더해진다`() {
            // 승률 55% 의 수축 승률은 54.93% 라 가산 없이는 60점이다. 가산 +2.2 로 62점이 된다.
            assertThat(create(count(11000, 9000), average, average, average, count(4000, 6000)))
                .containsExactly(score(62, TierGrade.S), b50, b50, b50, score(31, TierGrade.D))
        }

        @Test
        fun `전투 수가 300 미만이면 점수는 있고 등급은 없다`() {
            assertThat(create(count(180, 119), count(180, 120), average, average, average))
                .containsExactly(score(56, null), score(56, TierGrade.A), b50, b50, b50)
        }

        @Test
        fun `표본이 적으면 승률이 높아도 등급이 없다`() {
            assertThat(create(count(90, 10), average, average, average, average))
                .containsExactly(score(66, null), b50, b50, b50, b50)
        }

        @Test
        fun `브롤러 수가 짝수면 중앙값은 가운데 두 값의 평균이다`() {
            assertThat(create(count(1000, 1000), count(2000, 2000), count(3000, 3000), count(6000, 6000)))
                .containsExactly(
                    score(47, TierGrade.B),
                    score(49, TierGrade.B),
                    score(51, TierGrade.B),
                    score(53, TierGrade.B),
                )
        }

        @Test
        fun `무승부는 전투 수에 들어간다`() {
            assertThat(create(count(400, 400, 200), count(500, 500), count(500, 500))).containsExactly(b50, b50, b50)
        }

        @Test
        fun `전투 수가 0 인 항목의 가산은 -4 다`() {
            assertThat(create(count(0, 0), count(500, 500), count(500, 500)))
                .containsExactly(score(46, null), b50, b50)
        }

        @Test
        fun `중앙값은 전투 수가 300 이상인 브롤러만으로 낸다`() {
            // 표본이 적은 셋까지 넣은 중앙값(100)으로 가산을 내면 앞의 둘이 모두 54점이 된다
            val short = count(50, 50)
            assertThat(create(count(500, 500), count(1500, 1500), short, short, short))
                .containsExactly(
                    score(48, TierGrade.B),
                    score(51, TierGrade.B),
                    score(46, null),
                    score(46, null),
                    score(46, null),
                )
        }

        @Test
        fun `전투 수가 300 이면 중앙값에 들어가고 299 면 빠진다`() {
            assertThat(create(count(150, 150), count(450, 450)))
                .containsExactly(score(48, TierGrade.B), score(51, TierGrade.B))
            assertThat(create(count(150, 149), count(450, 450)))
                .containsExactly(score(47, null), b50)
        }

        @Test
        fun `전투 수가 300 이상인 브롤러가 없으면 가산은 0 이다`() {
            // 전투 수가 0 인 항목도 -4 가 아니라 0 이다
            assertThat(create(count(100, 100), count(100, 100), count(25, 25), count(0, 0)))
                .containsExactly(score(50, null), score(50, null), score(50, null), score(50, null))
        }

        @Test
        fun `브롤러가 하나면 50점 B 다`() {
            assertThat(create(count(5500, 4500))).containsExactly(b50)
        }

        @Test
        fun `빈 입력은 빈 결과다`() {
            assertThat(BrawlerTierScore.create(emptyMap())).isEmpty()
        }

        @Test
        fun `무승부뿐이면 모두 50점이다`() {
            assertThat(create(count(0, 0, 500), count(0, 0, 500))).containsExactly(b50, b50)
        }

        @Test
        fun `점수는 0 아래로 내려가지 않는다`() {
            val s69 = score(69, TierGrade.S)
            assertThat(create(count(100, 9900), average, average, average, average))
                .containsExactly(score(0, TierGrade.D), s69, s69, s69, s69)
        }
    }

    @Nested
    inner class GradeOf {
        @Test
        fun `점수 경계는 60 55 45 40 이다`() {
            val grades =
                listOf(60, 59, 55, 54, 45, 44, 40, 39, 0)
                    .map { BrawlerTierScore.gradeOf(value = it, battleCount = 300) }
            assertThat(grades).containsExactly(
                TierGrade.S,
                TierGrade.A,
                TierGrade.A,
                TierGrade.B,
                TierGrade.B,
                TierGrade.C,
                TierGrade.C,
                TierGrade.D,
                TierGrade.D,
            )
        }

        @Test
        fun `전투 수가 300 미만이면 등급이 없다`() {
            assertThat(BrawlerTierScore.gradeOf(value = 70, battleCount = 299)).isNull()
            assertThat(BrawlerTierScore.gradeOf(value = 70, battleCount = 300)).isEqualTo(TierGrade.S)
        }
    }
}
