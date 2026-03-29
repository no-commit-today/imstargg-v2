package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

class BrawlerTeamBattleRankStatsAggregator {
    private val stats = mutableMapOf<BrawlersBattleStatsKey, MutableMap<Int, Long>>()

    fun aggregate(battle: StatsRankBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

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
                        stats
                            .getOrPut(key) { mutableMapOf() }
                            .merge(battle.battleRank, 1L) { old, new -> old + new }
                    }
                }
            }
        }
    }

    fun toEntities(): List<BrawlerTeamBattleRankStatsEntity> =
        stats.map { (key, rankCounts) ->
            BrawlerTeamBattleRankStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                periodType = StatsPeriodType.DAILY,
                tierRange = key.tierRange,
                startBattleDate = key.battleDate,
                endBattleDate = key.battleDate,
                teamSize = key.brawlerBrawlStarsIds.size,
                rankCounts = RankCountsEmbedded.fromMap(rankCounts),
                initialBrawlerBrawlStarsIds = key.brawlerBrawlStarsIds,
            )
        }
}
