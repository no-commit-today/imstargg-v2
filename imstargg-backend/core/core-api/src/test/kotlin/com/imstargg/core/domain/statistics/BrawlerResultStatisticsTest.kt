package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
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
            val slotsPerGameByEvent = mapOf(eventId to 6)

            // when
            val sut = BrawlerResultStatistics.create(rows, slotsPerGameByEvent)

            // then: totalBattleCount = 900/6 = 150
            assertThat(sut.totalBattleCount).isEqualTo(150)
            // pickRate(A) = 300 / 150 = 2.0
            val itemA = sut.items.first { it.brawlerId == brawlerA }
            assertThat(itemA.pickRate).isEqualTo(2.0)
            assertThat(itemA.totalBattleCount).isEqualTo(150)
        }

        @Test
        fun `다중 이벤트 mix 시 게임수 분모는 이벤트별 환산 합`() {
            // given: 3v3 이벤트(slots=6) 60게임 = 360 슬롯, Solo SD(slots=10) 50게임 = 500 슬롯
            val event3v3 = BrawlStarsId(1L)
            val eventSolo = BrawlStarsId(2L)
            val brawler = BrawlStarsId(10L)
            val rows =
                listOf(
                    BrawlerEventResultCount(event3v3, brawler, ResultCount(60, 0, 0), 0),
                    BrawlerEventResultCount(event3v3, BrawlStarsId(99L), ResultCount(150, 150, 0), 0),
                    BrawlerEventResultCount(eventSolo, brawler, ResultCount(50, 0, 0), 0),
                    BrawlerEventResultCount(eventSolo, BrawlStarsId(99L), ResultCount(225, 225, 0), 0),
                )
            val slotsPerGameByEvent = mapOf(event3v3 to 6, eventSolo to 10)

            // when
            val sut = BrawlerResultStatistics.create(rows, slotsPerGameByEvent)

            // then: totalBattleCount = 360/6 + 500/10 = 60 + 50 = 110
            assertThat(sut.totalBattleCount).isEqualTo(110)
            // brawler 슬롯 합 = 60 + 50 = 110, pickRate = 110/110 = 1.0
            val item = sut.items.first { it.brawlerId == brawler }
            assertThat(item.pickRate).isEqualTo(1.0)
        }

        @Test
        fun `slotsPerGameByEvent에 누락된 이벤트가 있으면 예외`() {
            // given
            val eventId = BrawlStarsId(1L)
            val rows =
                listOf(
                    BrawlerEventResultCount(eventId, BrawlStarsId(10L), ResultCount(1, 0, 0), 0),
                )
            val slotsPerGameByEvent = emptyMap<BrawlStarsId, Int>()

            // when
            val ex =
                catchThrowableOfType(IllegalStateException::class.java) {
                    BrawlerResultStatistics.create(rows, slotsPerGameByEvent)
                }

            // then
            assertThat(ex).hasMessageContaining("playerCounts")
        }

        @Test
        fun `빈 입력은 totalBattleCount 0`() {
            // given / when
            val sut = BrawlerResultStatistics.create(emptyList(), emptyMap())

            // then
            assertThat(sut.totalBattleCount).isEqualTo(0)
            assertThat(sut.items).isEmpty()
        }
    }
}
