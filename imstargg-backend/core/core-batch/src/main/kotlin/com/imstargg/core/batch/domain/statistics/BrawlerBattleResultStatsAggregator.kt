package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 개별 브롤러의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 브롤러 × 이벤트 × 티어 구간 × 날짜
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)**: 배틀 결과를 그대로 집계
 * - **적군 팀(enemyTeams)**: 배틀 결과의 반대(opposite)를 집계.
 *   예: 아군이 승리하면 적군 각 브롤러에는 패배로 집계
 * - **스타 플레이어**: 해당 배틀의 스타 플레이어와 태그가 일치하면 카운트 증가
 * - **티어 구간**: 각 플레이어 본인의 트로피 기준으로 결정
 */
class BrawlerBattleResultStatsAggregator : BattleStatsAggregator<StatsResultBattleEntity, BrawlerBattleResultStatsEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerBattleStatsKey>()
    private val starPlayerCounts = StarPlayerCountAccumulator<BrawlerBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeams()) {
            doAggregate(battle, team, battleType, battleResult)
        }
        for (team in battle.enemyTeams()) {
            doAggregate(battle, team, battleType, battleResult.opposite())
        }
    }

    private fun doAggregate(
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
                resultCounts.add(key, result)
                if (player.brawlStarsTag == battle.starPlayerBrawlStarsTag) {
                    starPlayerCounts.add(key)
                }
            }
        }
    }

    override fun forEachEntity(consumer: (BrawlerBattleResultStatsEntity) -> Unit) {
        resultCounts.forEach { key, resultCount ->
            consumer(
                BrawlerBattleResultStatsEntity(
                    eventBrawlStarsId = key.eventBrawlStarsId,
                    tierRange = key.tierRange,
                    battleDate = key.battleDate,
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    resultCount = resultCount,
                    starPlayerCount = starPlayerCounts[key],
                ),
            )
        }
    }

    override fun toEntities(): List<BrawlerBattleResultStatsEntity> =
        buildList {
            forEachEntity { add(it) }
        }
}
