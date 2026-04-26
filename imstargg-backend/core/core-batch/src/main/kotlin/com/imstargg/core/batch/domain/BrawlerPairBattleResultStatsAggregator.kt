package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import java.time.LocalDate

/**
 * 기준 브롤러 관점의 방향성 pair 승/패/무 결과 통계를 집계한다.
 */
class BrawlerPairBattleResultStatsAggregator(
    private val startBattleDate: LocalDate,
    private val endBattleDate: LocalDate,
) : BattleStatsAggregator<StatsResultBattleEntity, BrawlerPairBattleResultStatsEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerPairBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeams()) {
            doAggregate(team, battle, battleType, battleResult)
        }
        for (team in battle.enemyTeams()) {
            doAggregate(team, battle, battleType, battleResult.opposite())
        }
    }

    private fun doAggregate(
        team: List<StatsBattleEntityTeamPlayer>,
        battle: StatsResultBattleEntity,
        battleType: BattleType,
        battleResult: BattleResult,
    ) {
        if (team.size < MIN_PAIR_TEAM_SIZE) return

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
                    resultCounts.add(key, battleResult)
                }
            }
        }
    }

    override fun toEntities(): List<BrawlerPairBattleResultStatsEntity> =
        resultCounts.map { key, resultCount ->
            BrawlerPairBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                tierRange = key.tierRange,
                startBattleDate = startBattleDate,
                endBattleDate = endBattleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                pairBrawlerBrawlStarsId = key.pairBrawlerBrawlStarsId,
                resultCount = resultCount,
            )
        }

    companion object {
        private const val MIN_PAIR_TEAM_SIZE = 2
    }
}
