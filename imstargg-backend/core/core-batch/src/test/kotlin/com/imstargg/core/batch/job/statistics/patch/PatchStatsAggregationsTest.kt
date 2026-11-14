package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.StatsBotBattleMonitor
import com.imstargg.core.batch.domain.statistics.StatsBotLobbyMonitor
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PatchStatsAggregationsTest {
    private val sut =
        PatchStatsAggregations(
            mockk<StatsBattleReader<StatsResultBattleEntity>>(),
            mockk<StatsBattleReader<StatsRankBattleEntity>>(),
        )

    @Test
    fun `1인 승률은 전투 봇 감시를 단다`() {
        assertThat(sut.brawlerBattleResult.createBotMonitor?.invoke()).isInstanceOf(StatsBotBattleMonitor::class.java)
    }

    @Test
    fun `1인 순위는 로비 봇 감시를 단다`() {
        assertThat(sut.brawlerBattleRank.createBotMonitor?.invoke()).isInstanceOf(StatsBotLobbyMonitor::class.java)
    }

    @Test
    fun `상대, 2인, 3인은 봇 감시를 달지 않는다`() {
        val monitors =
            listOf(
                sut.brawlerEnemyBattleResult.createBotMonitor,
                sut.brawlerPairBattleResult.createBotMonitor,
                sut.brawlerTrioBattleResult.createBotMonitor,
            )
        assertThat(monitors).hasSize(3).containsOnlyNulls()
    }
}
