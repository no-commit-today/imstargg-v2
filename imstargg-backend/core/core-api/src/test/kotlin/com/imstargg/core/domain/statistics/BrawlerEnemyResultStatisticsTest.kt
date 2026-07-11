package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerEnemyResultStatisticsTest {
    @Test
    fun `create 는 매치업 슬롯 수를 battleCount 로, 게임 수를 픽률 분모로 사용한다`() {
        // given - 매치업 슬롯 40 (승30 패10), 전체 100게임
        val counts =
            listOf(
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(10L),
                    enemyBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(totalBattleCount = 100L, totalElementCount = 1L, counts = counts)

        // then
        val item = sut.items.single()
        assertThat(item.battleCount).isEqualTo(40L)
        assertThat(item.pickRate).isEqualTo(0.4)
        assertThat(item.winRate).isEqualTo(0.75)
    }

    @Test
    fun `게임 수 0 이면 pickRate 0`() {
        // given
        val counts =
            listOf(
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(10L),
                    enemyBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(0, 0, 0),
                ),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(totalBattleCount = 0L, totalElementCount = 0L, counts = counts)

        // then
        assertThat(sut.items.single().pickRate).isEqualTo(0.0)
    }
}
