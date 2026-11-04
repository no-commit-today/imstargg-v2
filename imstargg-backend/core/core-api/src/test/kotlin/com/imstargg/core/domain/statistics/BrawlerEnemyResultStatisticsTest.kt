package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerEnemyResultStatisticsTest {
    private val brawlerId = BrawlStarsId(10L)

    private fun count(
        enemyBrawlerId: Long,
        victoryCount: Long,
        defeatCount: Long,
        drawCount: Long = 0L,
    ) = BrawlerEnemyResultCount(
        brawlerId = brawlerId,
        enemyBrawlerId = BrawlStarsId(enemyBrawlerId),
        resultCount = ResultCount(victoryCount = victoryCount, defeatCount = defeatCount, drawCount = drawCount),
    )

    @Test
    fun `픽률 분모는 X 의 게임 수(Σ total ÷ 3)`() {
        // given - 상대 5명 각 총 30 → Σ 150 → X 게임 50
        val counts = listOf(20L, 30L, 40L, 50L, 60L).map { count(it, victoryCount = 15, defeatCount = 15) }

        // when
        val sut = BrawlerEnemyResultStatistics.create(counts)

        // then
        assertThat(sut.items).hasSize(5)
        assertThat(sut.items).allSatisfy { item ->
            assertThat(item.battleCount).isEqualTo(30L)
            assertThat(item.pickRate).isEqualTo(0.6)
        }
    }

    @Test
    fun `winRate 오름차순`() {
        // given
        val counts =
            listOf(
                count(20L, victoryCount = 20, defeatCount = 10),
                count(30L, victoryCount = 10, defeatCount = 20),
                count(40L, victoryCount = 15, defeatCount = 15),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(counts)

        // then
        assertThat(sut.items.map { it.enemyBrawlerId })
            .containsExactly(BrawlStarsId(30L), BrawlStarsId(40L), BrawlStarsId(20L))
        assertThat(sut.items.first().winRate).isEqualTo(1.0 / 3)
    }

    @Test
    fun `30 미만 제외`() {
        // given - 30판 3명 + 29판 1명 + 1판 1명 → Σ 120 → X 게임 40. 빠진 행도 분모에는 들어간다.
        val counts =
            listOf(
                count(20L, victoryCount = 15, defeatCount = 15),
                count(30L, victoryCount = 15, defeatCount = 15),
                count(40L, victoryCount = 15, defeatCount = 15),
                count(50L, victoryCount = 15, defeatCount = 14),
                count(60L, victoryCount = 1, defeatCount = 0),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(counts)

        // then
        assertThat(sut.items.map { it.enemyBrawlerId })
            .containsExactlyInAnyOrder(BrawlStarsId(20L), BrawlStarsId(30L), BrawlStarsId(40L))
        assertThat(sut.items).allSatisfy { item -> assertThat(item.pickRate).isEqualTo(0.75) }
    }
}
