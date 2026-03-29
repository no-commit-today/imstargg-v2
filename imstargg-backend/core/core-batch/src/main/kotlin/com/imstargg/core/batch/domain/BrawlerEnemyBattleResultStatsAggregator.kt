package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

class BrawlerEnemyBattleResultStatsAggregator {
    private val stats = mutableMapOf<BrawlerEnemyBattleStatsKey, ResultAccumulator>()

    fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeam()) {
            for (player in team) {
                val tierRanges = StatsTierRange.resolveAll(battleType, player.brawler.trophies)
                for (enemyTeam in battle.enemyTeams()) {
                    for (enemy in enemyTeam) {
                        for (tierRange in tierRanges) {
                            val key =
                                BrawlerEnemyBattleStatsKey(
                                    brawlerBrawlStarsId = player.brawler.brawlStarsId,
                                    enemyBrawlerBrawlStarsId = enemy.brawler.brawlStarsId,
                                    eventBrawlStarsId = battle.eventBrawlStarsId,
                                    tierRange = tierRange,
                                    battleDate = battle.battleDate,
                                )
                            stats.getOrPut(key) { ResultAccumulator() }.add(battleResult)
                        }
                    }
                }
            }
        }
    }

    fun toEntities(): List<BrawlerEnemyBattleResultStatsEntity> =
        stats.map { (key, acc) ->
            BrawlerEnemyBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                periodType = StatsPeriodType.DAILY,
                tierRange = key.tierRange,
                startBattleDate = key.battleDate,
                endBattleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                enemyBrawlerBrawlStarsId = key.enemyBrawlerBrawlStarsId,
                resultCount =
                    ResultCountEmbedded(
                        victoryCount = acc.victoryCount,
                        defeatCount = acc.defeatCount,
                        drawCount = acc.drawCount,
                    ),
            )
        }

    private class ResultAccumulator {
        var victoryCount: Long = 0
        var defeatCount: Long = 0
        var drawCount: Long = 0

        fun add(result: BattleResult) {
            when (result) {
                BattleResult.VICTORY -> victoryCount++
                BattleResult.DEFEAT -> defeatCount++
                BattleResult.DRAW -> drawCount++
            }
        }
    }
}
