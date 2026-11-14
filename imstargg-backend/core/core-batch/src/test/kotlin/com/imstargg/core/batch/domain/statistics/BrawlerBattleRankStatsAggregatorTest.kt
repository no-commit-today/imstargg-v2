package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsEventModeType
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

    @Test
    fun `20인 솔로 쇼다운(모드 75)의 순위 19 전투는 건너뛰고 같은 날짜의 솔로 쇼다운 전투는 집계한다`() {
        val sut = BrawlerBattleRankStatsAggregator()

        sut.aggregate(statsRankBattle(teams = (101..120).map { listOf(statsPlayer(it)) }, battleRank = 19, eventModeBrawlStarsId = 75))
        sut.aggregate(showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + List(9) { listOf(2100) }))

        assertThat(sut.toEntities().map { it.brawlerBrawlStarsId }).containsOnly(1L)
    }

    @Test
    fun `10팀 듀오 쇼다운(모드 86) 전투는 집계하지 않는다`() {
        val tenDuos = (0 until 10).map { t -> listOf(statsPlayer(2 * t + 1), statsPlayer(2 * t + 2)) }

        assertThat(aggregate(statsRankBattle(teams = tenDuos, battleRank = 3, eventModeBrawlStarsId = 86))).isEmpty()
    }

    @Test
    fun `쇼다운 구성이면 모드 ID 와 관계없이 집계한다`() {
        val teams = showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + List(9) { listOf(2100) }).teams

        assertThat(aggregate(statsRankBattle(teams = teams, eventModeBrawlStarsId = 999))).isNotEmpty()
    }

    private fun aggregate(battle: StatsRankBattleEntity) = BrawlerBattleRankStatsAggregator().also { it.aggregate(battle) }.toEntities()

    private fun duo(
        vararg others: List<Int>,
        battleRank: Int = 1,
    ) = showdown(StatsEventModeType.DUO_SHOWDOWN, listOf(listOf(1500, 2100)) + others, battleRank = battleRank)
}
