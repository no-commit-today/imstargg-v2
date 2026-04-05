package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

/**
 * 개별 브롤러의 순위(rank) 분포 통계를 집계한다.
 *
 * ## 집계 키
 * 브롤러 × 이벤트 × 티어 구간 × 날짜
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)만** 집계 (순위형 배틀에는 적군 개념이 없음)
 * - 각 배틀의 순위(1위~N위)를 분포로 누적 (예: 1위 5회, 2위 3회, ...)
 * - **티어 구간**: 각 플레이어 본인의 트로피 기준으로 결정
 */
class BrawlerBattleRankStatsAggregator : BattleStatsAggregator<StatsRankBattleEntity, BrawlerBattleRankStatsEntity> {
    private val rankCounts = RankCountAccumulator<BrawlerBattleStatsKey>()

    override fun aggregate(battle: StatsRankBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeams()) {
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
                    rankCounts.add(key, battle.battleRank)
                }
            }
        }
    }

    override fun toEntities(): List<BrawlerBattleRankStatsEntity> =
        rankCounts.map { key, rankCountsEmbedded ->
            BrawlerBattleRankStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                battleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                rankCounts = rankCountsEmbedded,
            )
        }
}
