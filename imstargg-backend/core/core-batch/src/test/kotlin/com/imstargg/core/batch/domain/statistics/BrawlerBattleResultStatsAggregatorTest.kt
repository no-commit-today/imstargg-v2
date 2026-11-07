package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerBattleResultStatsAggregatorTest {
    @Test
    fun `수집자 팀은 결과 그대로, 상대 팀은 반대로 집계한다`() {
        val entities = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))))

        assertThat(entities.single { it.brawlerBrawlStarsId == 1L }.resultCount.victoryCount).isEqualTo(1L)
        assertThat(entities.single { it.brawlerBrawlStarsId == 2L }.resultCount.defeatCount).isEqualTo(1L)
    }

    @Test
    fun `티어는 플레이어 본인의 trophies 로 정한다`() {
        val entities = aggregate(statsResultBattle(listOf(listOf(statsPlayer(1, 2500)), listOf(statsPlayer(2, 1000)))))

        assertThat(entities.filter { it.brawlerBrawlStarsId == 1L }.map { it.tierRange })
            .containsExactlyInAnyOrder(StatsTierRange.TROPHY_1000_PLUS, StatsTierRange.TROPHY_2000_PLUS)
        assertThat(entities.filter { it.brawlerBrawlStarsId == 2L }.map { it.tierRange }).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
    }

    @Test
    fun `스타 플레이어의 행만 starPlayerCount 를 센다`() {
        val entities =
            aggregate(statsResultBattle(listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2))), starPlayerBrawlStarsTag = "#TAG2"))

        assertThat(entities.single { it.brawlerBrawlStarsId == 1L }.starPlayerCount).isZero()
        assertThat(entities.single { it.brawlerBrawlStarsId == 2L }.starPlayerCount).isEqualTo(1L)
    }

    @Test
    fun `유형이나 결과가 알 수 없는 코드면 건너뛴다`() {
        val teams = listOf(listOf(statsPlayer(1)), listOf(statsPlayer(2)))
        assertThat(aggregate(statsResultBattle(teams, type = "unknown"))).isEmpty()
        assertThat(aggregate(statsResultBattle(teams, result = "unknown"))).isEmpty()
    }

    private fun aggregate(battle: StatsResultBattleEntity) = BrawlerBattleResultStatsAggregator().also { it.aggregate(battle) }.toEntities()
}
