package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

class BrawlerBattleRankStatsAggregator {
    private val stats = mutableMapOf<BrawlerBattleStatsKey, MutableMap<Int, Long>>()

    fun aggregate(battle: StatsRankBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeam()) {
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
                    val rankCounts = stats.getOrPut(key) { mutableMapOf() }
                    rankCounts.merge(battle.battleRank, 1L) { old, new -> old + new }
                }
            }
        }
    }

    fun toEntities(): List<BrawlerBattleRankStatsEntity> =
        stats.map { (key, rankCounts) ->
            BrawlerBattleRankStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                battleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                rankCounts = RankCountsEmbedded.fromMap(rankCounts),
            )
        }
}
