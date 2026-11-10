package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrawlerResultStatisticsTest {
    @Nested
    inner class Create {
        @Test
        fun `단일 3v3 이벤트의 픽률은 게임 수 분모로 계산된다`() {
            // given: 3v3 이벤트 100게임 → 600 슬롯, 브롤러 A 가 300 슬롯
            val eventId = BrawlStarsId(1L)
            val brawlerA = BrawlStarsId(10L)
            val brawlerB = BrawlStarsId(11L)
            val rows =
                listOf(
                    BrawlerEventResultCount(eventId, brawlerA, ResultCount(150, 150, 0), 30),
                    BrawlerEventResultCount(eventId, brawlerB, ResultCount(150, 150, 0), 30),
                    BrawlerEventResultCount(eventId, BrawlStarsId(12L), ResultCount(150, 150, 0), 60),
                )

            // when
            val sut = BrawlerResultStatistics.create(rows)

            // then: totalBattleCount = 900/6 = 150, 브롤러 A 의 battleCount 는 자신의 슬롯 수
            assertThat(sut.totalBattleCount).isEqualTo(150)
            val itemA = sut.items.first { it.brawlerId == brawlerA }
            assertThat(itemA.pickRate).isEqualTo(2.0)
            assertThat(itemA.battleCount).isEqualTo(300)
        }

        @Test
        fun `여러 이벤트의 슬롯 합을 6 으로 나눈 값이 게임 수다`() {
            // given: 이벤트 A 600 슬롯 + 이벤트 B 300 슬롯
            val eventA = BrawlStarsId(1L)
            val eventB = BrawlStarsId(2L)
            val brawler = BrawlStarsId(10L)
            val rows =
                listOf(
                    BrawlerEventResultCount(eventA, brawler, ResultCount(300, 300, 0), 0),
                    BrawlerEventResultCount(eventB, brawler, ResultCount(150, 150, 0), 0),
                )

            // when
            val sut = BrawlerResultStatistics.create(rows)

            // then: totalBattleCount = (600 + 300) / 6 = 150
            assertThat(sut.totalBattleCount).isEqualTo(150)
        }

        @Test
        fun `이벤트별로 내림하지 않고 슬롯 합을 합산한 뒤 한 번만 내린다`() {
            // given: 이벤트 A 4 슬롯 + 이벤트 B 5 슬롯 (이벤트별로 내리면 각각 0 게임)
            val brawler = BrawlStarsId(10L)
            val rows =
                listOf(
                    BrawlerEventResultCount(BrawlStarsId(1L), brawler, ResultCount(2, 2, 0), 0),
                    BrawlerEventResultCount(BrawlStarsId(2L), brawler, ResultCount(3, 1, 1), 0),
                )

            // when
            val sut = BrawlerResultStatistics.create(rows)

            // then: totalBattleCount = (4 + 5) / 6 = 1
            assertThat(sut.totalBattleCount).isEqualTo(1)
        }

        @Test
        fun `등급 있는 항목이 먼저 오고 그 안에서 점수와 전투 수 내림차순이다`() {
            // given: a 57/A, b 46/B(1만 판), c 46/B(9500판), d 65/없음(100판), e 44/없음(200판). 입력 순서는 섞는다
            val event = BrawlStarsId(1L)
            val (a, b, c, d, e) = (10L..14L).map { BrawlStarsId(it) }
            val rows =
                listOf(
                    BrawlerEventResultCount(event, c, ResultCount(4750, 4750, 0), 0),
                    BrawlerEventResultCount(event, e, ResultCount(100, 100, 0), 0),
                    BrawlerEventResultCount(event, a, ResultCount(5600, 4400, 0), 0),
                    BrawlerEventResultCount(event, d, ResultCount(90, 10, 0), 0),
                    BrawlerEventResultCount(event, b, ResultCount(5000, 5000, 0), 0),
                )

            // when
            val sut = BrawlerResultStatistics.create(rows)

            // then
            assertThat(sut.items.map { it.brawlerId }).containsExactly(a, b, c, d, e)
            assertThat(sut.items.map { it.tierScore }).containsExactly(
                BrawlerTierScore(value = 57, grade = TierGrade.A),
                BrawlerTierScore(value = 46, grade = TierGrade.B),
                BrawlerTierScore(value = 46, grade = TierGrade.B),
                BrawlerTierScore(value = 65, grade = null),
                BrawlerTierScore(value = 44, grade = null),
            )
        }

        @Test
        fun `빈 입력은 totalBattleCount 0`() {
            // given / when
            val sut = BrawlerResultStatistics.create(emptyList())

            // then
            assertThat(sut.totalBattleCount).isEqualTo(0)
            assertThat(sut.items).isEmpty()
        }
    }
}
