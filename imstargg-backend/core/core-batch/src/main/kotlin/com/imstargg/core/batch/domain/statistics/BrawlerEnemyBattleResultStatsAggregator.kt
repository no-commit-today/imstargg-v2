package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import java.time.LocalDate

/**
 * 브롤러 vs 적 브롤러 매치업의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 내 브롤러 × 적 브롤러 × 이벤트 × 티어 구간
 */
class BrawlerEnemyBattleResultStatsAggregator(
    private val startBattleDate: LocalDate,
    private val endBattleDate: LocalDate,
) : BattleStatsAggregator<StatsResultBattleEntity, BrawlerEnemyBattleResultStatsEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerEnemyBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeams()) {
            for (enemyTeam in battle.enemyTeams()) {
                doAggregate(team, enemyTeam, battle, battleType, battleResult)
                doAggregate(enemyTeam, team, battle, battleType, battleResult.opposite())
            }
        }
    }

    private fun doAggregate(
        playerTeam: List<StatsBattleEntityTeamPlayer>,
        enemyTeam: List<StatsBattleEntityTeamPlayer>,
        battle: StatsResultBattleEntity,
        battleType: BattleType,
        battleResult: BattleResult,
    ) {
        for (player in playerTeam) {
            val tierRanges = StatsTierRange.resolveAll(battleType, player.brawler.trophies)
            for (enemy in enemyTeam) {
                for (tierRange in tierRanges) {
                    val key =
                        BrawlerEnemyBattleStatsKey(
                            brawlerBrawlStarsId = player.brawler.brawlStarsId,
                            enemyBrawlerBrawlStarsId = enemy.brawler.brawlStarsId,
                            eventBrawlStarsId = battle.eventBrawlStarsId,
                            tierRange = tierRange,
                        )
                    resultCounts.add(key, battleResult)
                }
            }
        }
    }

    override fun forEachEntity(consumer: (BrawlerEnemyBattleResultStatsEntity) -> Unit) {
        resultCounts.forEach { key, resultCount ->
            consumer(
                BrawlerEnemyBattleResultStatsEntity(
                    eventBrawlStarsId = key.eventBrawlStarsId,
                    tierRange = key.tierRange,
                    startBattleDate = startBattleDate,
                    endBattleDate = endBattleDate,
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    enemyBrawlerBrawlStarsId = key.enemyBrawlerBrawlStarsId,
                    resultCount = resultCount,
                ),
            )
        }
    }

    override fun toEntities(): List<BrawlerEnemyBattleResultStatsEntity> =
        buildList {
            forEachEntity { add(it) }
        }
}
