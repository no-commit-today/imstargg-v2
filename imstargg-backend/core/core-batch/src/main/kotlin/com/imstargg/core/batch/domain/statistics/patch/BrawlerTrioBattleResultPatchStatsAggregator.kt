package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.BrawlerTrioBattleStatsKey
import com.imstargg.core.batch.domain.statistics.ResultCountAccumulator
import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 같은 팀 브롤러 3인 조합(3v3 한 팀 전체)의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 티어 구간 × (브롤러1 < 브롤러2 < 브롤러3). 조합은 순서가 없으므로 팀의 세 브롤러를 id 오름차순 키 1개로 센다.
 * 이벤트는 키에 없다.
 *
 * ## 팀 인원
 * 팀 인원이 정확히 3명인 팀만 센다. 3명이 아닌 팀은 조용히 건너뛴다.
 *
 * ## 티어 규칙
 * 세 멤버 trophies 의 최솟값으로 [StatsTierRange.resolveAll] 한다(조합 전원이 그 티어 이상이어야 그 티어에 집계).
 *
 * ## 결과 방향
 * 아군 팀은 전투 결과 그대로, 적 팀은 [BattleResult.opposite] 로 집계한다.
 *
 * ## 봇 전투 제외
 * 봇 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다.
 *
 * 옛 집계기 `BrawlerTrioBattleResultStatsAggregator` 와 규칙이 같다. 2단계에서 옛 것을 지운다.
 * 그때까지 규칙을 바꾸면 두 곳을 함께 고친다(`PatchStatsAggregatorParityTest` 가 어긋남을 잡는다).
 */
class BrawlerTrioBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerTrioBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        if (StatsBotBattlePolicy.isBotBattle(battle)) return
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeams()) {
            doAggregate(team, battleType, battleResult)
        }
        for (team in battle.enemyTeams()) {
            doAggregate(team, battleType, battleResult.opposite())
        }
    }

    private fun doAggregate(
        team: List<StatsBattleEntityTeamPlayer>,
        battleType: BattleType,
        battleResult: BattleResult,
    ) {
        if (team.size != TRIO_TEAM_SIZE) return

        val brawlers = team.map { it.brawler }
        val sortedIds = brawlers.map { it.brawlStarsId }.sorted()
        require(sortedIds.distinct().size == TRIO_TEAM_SIZE) {
            "같은 팀에 중복된 브롤러가 있습니다. brawlStarsIds=$sortedIds"
        }

        val tierRanges = StatsTierRange.resolveAll(battleType, brawlers.minOf { it.trophies })
        for (tierRange in tierRanges) {
            val key =
                BrawlerTrioBattleStatsKey(
                    brawler1BrawlStarsId = sortedIds[0],
                    brawler2BrawlStarsId = sortedIds[1],
                    brawler3BrawlStarsId = sortedIds[2],
                    tierRange = tierRange,
                )
            resultCounts.add(key, battleResult)
        }
    }

    override fun toCounts(): List<PatchStatsCount> =
        resultCounts.map { key, resultCount ->
            PatchStatsCount(
                keys = listOf(key.tierRange.name, key.brawler1BrawlStarsId, key.brawler2BrawlStarsId, key.brawler3BrawlStarsId),
                counts = listOf(resultCount.victoryCount, resultCount.defeatCount, resultCount.drawCount, resultCount.totalCount),
            )
        }

    companion object {
        private const val TRIO_TEAM_SIZE = 3
    }
}
