package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

class BrawlerBattleResultStatsAggregator {
    private val stats = mutableMapOf<BrawlerBattleStatsKey, Accumulator>()

    fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeam()) {
            aggregateTeam(battle, team, battleType, battleResult)
        }
        for (team in battle.enemyTeams()) {
            aggregateTeam(battle, team, battleType, battleResult.opposite())
        }
    }

    private fun aggregateTeam(
        battle: StatsResultBattleEntity,
        team: List<StatsBattleEntityTeamPlayer>,
        battleType: BattleType,
        result: BattleResult,
    ) {
        for (player in team) {
            val tierRanges = StatsTierRange.resolveAll(battleType, player.brawler.trophies)
            for (tierRange in tierRanges) {
                val key =
                    BrawlerBattleStatsKey(
                        brawlerBrawlStarsId = player.brawler.brawlStarsId,
                        eventBrawlStarsId = battle.eventBrawlStarsId,
                        tierRange = tierRange,
                        battleDate = battle.battleDate,
                    )
                val acc = stats.getOrPut(key) { Accumulator() }
                acc.add(result, player.brawlStarsTag == battle.starPlayerBrawlStarsTag)
            }
        }
    }

    fun toEntities(): List<BrawlerBattleResultStatsEntity> =
        stats.map { (key, acc) ->
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                battleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                resultCount =
                    ResultCountEmbedded(
                        victoryCount = acc.victoryCount,
                        defeatCount = acc.defeatCount,
                        drawCount = acc.drawCount,
                    ),
                starPlayerCount = acc.starPlayerCount,
            )
        }

    private class Accumulator {
        var victoryCount: Long = 0
        var defeatCount: Long = 0
        var drawCount: Long = 0
        var starPlayerCount: Long = 0

        fun add(
            result: BattleResult,
            isStarPlayer: Boolean,
        ) {
            when (result) {
                BattleResult.VICTORY -> victoryCount++
                BattleResult.DEFEAT -> defeatCount++
                BattleResult.DRAW -> drawCount++
            }
            if (isStarPlayer) starPlayerCount++
        }
    }
}
