package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import java.time.LocalDate

/**
 * 기준 브롤러 관점의 방향성 pair 순위(rank) 분포 통계를 집계한다.
 */
class BrawlerPairBattleRankStatsAggregator(
    private val startBattleDate: LocalDate,
    private val endBattleDate: LocalDate,
) : BattleStatsAggregator<StatsRankBattleEntity, BrawlerPairBattleRankStatsEntity> {
    private val rankCounts = RankCountAccumulator<BrawlerPairBattleStatsKey>()

    override fun aggregate(battle: StatsRankBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeams()) {
            if (team.size < MIN_PAIR_TEAM_SIZE) continue
            for (player in team) {
                val tierRanges = StatsTierRange.resolveAll(battleType, player.brawler.trophies)
                for (pairPlayer in team) {
                    if (player.brawler.brawlStarsId == pairPlayer.brawler.brawlStarsId) continue
                    for (tierRange in tierRanges) {
                        val key =
                            BrawlerPairBattleStatsKey(
                                brawlerBrawlStarsId = player.brawler.brawlStarsId,
                                pairBrawlerBrawlStarsId = pairPlayer.brawler.brawlStarsId,
                                eventBrawlStarsId = battle.eventBrawlStarsId,
                                tierRange = tierRange,
                            )
                        rankCounts.add(key, battle.battleRank)
                    }
                }
            }
        }
    }

    override fun forEachEntity(consumer: (BrawlerPairBattleRankStatsEntity) -> Unit) {
        rankCounts.forEach { key, rankCountsEmbedded ->
            consumer(
                BrawlerPairBattleRankStatsEntity(
                    eventBrawlStarsId = key.eventBrawlStarsId,
                    tierRange = key.tierRange,
                    startBattleDate = startBattleDate,
                    endBattleDate = endBattleDate,
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    pairBrawlerBrawlStarsId = key.pairBrawlerBrawlStarsId,
                    rankCounts = rankCountsEmbedded,
                ),
            )
        }
    }

    override fun toEntities(): List<BrawlerPairBattleRankStatsEntity> =
        buildList {
            forEachEntity { add(it) }
        }

    companion object {
        private const val MIN_PAIR_TEAM_SIZE = 2
    }
}
