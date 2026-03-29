package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

class BrawlerTeamBattleResultStatsAggregator {
    private val stats = mutableMapOf<BrawlersBattleStatsKey, ResultAccumulator>()

    fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeam()) {
            if (team.size < 2) continue
            val maxSize = minOf(team.size, 3)
            for (size in 2..maxSize) {
                for (combination in PlayerCombinator.combine(team, size)) {
                    val brawlerIds = combination.map { it.brawler.brawlStarsId }.sorted()
                    val minTrophies = combination.minOf { it.brawler.trophies }
                    for (tierRange in StatsTierRange.resolveAll(battleType, minTrophies)) {
                        val key =
                            BrawlersBattleStatsKey(
                                brawlerBrawlStarsIds = brawlerIds,
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

    fun toEntities(): List<BrawlerTeamBattleResultStatsEntity> =
        stats.map { (key, acc) ->
            BrawlerTeamBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                periodType = StatsPeriodType.DAILY,
                tierRange = key.tierRange,
                startBattleDate = key.battleDate,
                endBattleDate = key.battleDate,
                teamSize = key.brawlerBrawlStarsIds.size,
                resultCount =
                    ResultCountEmbedded.create(
                        victoryCount = acc.victoryCount,
                        defeatCount = acc.defeatCount,
                        drawCount = acc.drawCount,
                    ),
                initialBrawlerBrawlStarsIds = key.brawlerBrawlStarsIds,
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
