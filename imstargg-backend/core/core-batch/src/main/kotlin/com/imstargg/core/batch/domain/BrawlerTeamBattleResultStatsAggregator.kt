package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 브롤러 팀 조합의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 브롤러 조합(정렬된 ID 목록) × 이벤트 × 티어 구간 × 날짜
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)과 적군 팀(enemyTeams) 모두** 집계 (적군은 반대 결과)
 * - **팀 크기 제한**: 2명 미만인 팀은 건너뜀
 * - **조합 생성**: 팀원 중 2명 조합과 3명 조합을 각각 생성하여 별도 집계.
 *   예: 3인 팀 [A, B, C] → 2명 조합 [A,B], [A,C], [B,C] + 3명 조합 [A,B,C]
 * - **브롤러 ID 정렬**: 조합 내 브롤러 ID를 오름차순 정렬하여 순서 무관하게 동일 조합으로 취급
 * - **티어 구간**: 조합 내 최소 트로피를 기준으로 결정
 */
class BrawlerTeamBattleResultStatsAggregator : BattleStatsAggregator<StatsResultBattleEntity, BrawlerTeamBattleResultStatsEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerTeamBattleStatsKey>()

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
        if (team.size < 2) return
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
                            battleDate = battle.battleDate,
                        )
                    resultCounts.add(key, battleResult)
                }
            }
        }
    }

    override fun toEntities(): List<BrawlerTeamBattleResultStatsEntity> =
        resultCounts.map { key, resultCount ->
            BrawlerTeamBattleResultStatsEntity(
                eventBrawlStarsId = key.eventBrawlStarsId,
                periodType = StatsPeriodType.DAILY,
                tierRange = key.tierRange,
                battleDate = key.battleDate,
                teamSize = StatsTeamSize.fromValue(key.brawlerBrawlStarsIds.size),
                resultCount = resultCount,
                initialBrawlerBrawlStarsIds = key.brawlerBrawlStarsIds,
            )
        }
}
