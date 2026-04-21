package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import java.time.LocalDate

/**
 * 브롤러 팀 조합의 순위(rank) 분포 통계를 집계한다.
 *
 * ## 집계 키
 * 브롤러 조합(정렬된 ID 목록) × 이벤트 × 티어 구간
 */
class BrawlerTeamBattleRankStatsAggregator(
    private val startBattleDate: LocalDate,
    private val endBattleDate: LocalDate,
) : BattleStatsAggregator<StatsRankBattleEntity, BrawlerTeamBattleRankStatsEntity> {
    private val rankCounts = RankCountAccumulator<BrawlerTeamBattleStatsKey>()

    override fun aggregate(battle: StatsRankBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeams()) {
            if (team.size < 2) continue
            val maxSize = minOf(team.size, 3)
            for (size in 2..maxSize) {
                for (combination in PlayerCombinator.combine(team, size)) {
                    val brawlerIds = combination.map { it.brawler.brawlStarsId }
                    val minTrophies = combination.minOf { it.brawler.trophies }
                    for (tierRange in StatsTierRange.resolveAll(battleType, minTrophies)) {
                        val key =
                            BrawlerTeamBattleStatsKey.create(
                                brawlerBrawlStarsIds = brawlerIds,
                                eventBrawlStarsId = battle.eventBrawlStarsId,
                                tierRange = tierRange,
                            )
                        rankCounts.add(key, battle.battleRank)
                    }
                }
            }
        }
    }

    override fun toEntities(): List<BrawlerTeamBattleRankStatsEntity> =
        rankCounts.map { key, rankCountsEmbedded ->
            BrawlerTeamBattleRankStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                startBattleDate = startBattleDate,
                endBattleDate = endBattleDate,
                rankCounts = rankCountsEmbedded,
                brawlerTeam = BrawlerTeamEmbedded.of(key.brawlerBrawlStarsIds),
            )
        }
}
