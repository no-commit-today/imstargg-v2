package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsEventMode
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerBattleRankStatsAggregatorTest {
    @Test
    fun `수집자 팀만 본인 트로피의 티어로 순위를 집계한다`() {
        val entities =
            aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610), battleRank = 2))

        assertThat(entities.map { it.brawlerBrawlStarsId to it.tierRange }).containsExactlyInAnyOrder(
            1L to StatsTierRange.TROPHY_1000_PLUS,
            2L to StatsTierRange.TROPHY_1000_PLUS,
            2L to StatsTierRange.TROPHY_2000_PLUS,
        )
        assertThat(entities).allSatisfy {
            assertThat(it.rankCounts.countOf(2)).isEqualTo(1L)
            assertThat(it.rankCounts.totalCount).isEqualTo(1L)
        }
    }

    @Test
    fun `봇 로비는 건너뛰고 임계값 아래 로비는 집계한다`() {
        assertThat(aggregate(duo(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)))).isEmpty()
        assertThat(aggregate(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)))).isNotEmpty()
    }

    private fun aggregate(battle: StatsRankBattleEntity) = BrawlerBattleRankStatsAggregator().also { it.aggregate(battle) }.toEntities()

    private fun duo(
        vararg others: List<Int>,
        battleRank: Int = 1,
    ) = showdown(StatsEventMode.DUO_SHOWDOWN, listOf(listOf(1500, 2100)) + others, battleRank = battleRank)
}
