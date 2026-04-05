package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 브롤러 vs 적 브롤러 매치업의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 내 브롤러 × 적 브롤러 × 이벤트 × 티어 구간 × 날짜
 *
 * ## 집계 규칙
 * - **아군 × 적군 전수 조합**: 아군 팀의 각 플레이어�� 적군 팀의 각 플레이���를 1:1로 매칭하여 집계.
 *   예: 아군 [A, B, C] vs 적군 [X, Y, Z] → A-X, A-Y, A-Z, B-X, B-Y, B-Z, C-X, C-Y, C-Z 총 9쌍
 * - **양측 시점 집계**: 아군 시점(원래 결과)과 적군 시점(반대 결과) 모두 집계
 * - **티어 구간**: 각 시점에서 기준 브롤러(내 브롤러) 본인의 트로피 기준으로 결정
 */
class BrawlerEnemyBattleResultStatsAggregator : BattleStatsAggregator<StatsResultBattleEntity, BrawlerEnemyBattleResultStatsEntity> {
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
                            battleDate = battle.battleDate,
                        )
                    resultCounts.add(key, battleResult)
                }
            }
        }
    }

    override fun toEntities(): List<BrawlerEnemyBattleResultStatsEntity> =
        resultCounts.map { key, resultCount ->
            BrawlerEnemyBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                periodType = StatsPeriodType.DAILY,
                tierRange = key.tierRange,
                startBattleDate = key.battleDate,
                endBattleDate = key.battleDate,
                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                enemyBrawlerBrawlStarsId = key.enemyBrawlerBrawlStarsId,
                resultCount = resultCount,
            )
        }
}
