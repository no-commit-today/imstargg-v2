package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleStatsKey
import com.imstargg.core.batch.domain.statistics.ResultCountAccumulator
import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 같은 팀 브롤러 2인 조합의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 티어 구간 × (브롤러1 < 브롤러2). 조합은 순서가 없으므로 팀의 모든 `i < j` 쌍을 id 오름차순 키 1개로 센다(방향 중복 없음).
 * 이벤트는 키에 없다.
 *
 * ## 티어 규칙
 * 두 멤버 trophies 의 최솟값으로 [StatsTierRange.resolveAll] 한다(조합 전원이 그 티어 이상이어야 그 티어에 집계).
 *
 * ## 결과 방향
 * 아군 팀은 전투 결과 그대로, 적 팀은 [BattleResult.opposite] 로 집계한다.
 *
 * ## 봇 전투 제외
 * 봇 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다.
 *
 * 옛 집계기 `BrawlerPairBattleResultStatsAggregator` 와 규칙이 같다. 2단계에서 옛 것을 지운다.
 * 그때까지 규칙을 바꾸면 두 곳을 함께 고친다(`PatchStatsAggregatorParityTest` 가 어긋남을 잡는다).
 */
class BrawlerPairBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerPairBattleStatsKey>()

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
        if (team.size < MIN_PAIR_TEAM_SIZE) return

        for (i in team.indices) {
            for (j in i + 1 until team.size) {
                val first = team[i].brawler
                val second = team[j].brawler
                require(first.brawlStarsId != second.brawlStarsId) {
                    "같은 팀에 중복된 브롤러가 있습니다. brawlStarsId=${first.brawlStarsId}"
                }

                val tierRanges = StatsTierRange.resolveAll(battleType, minOf(first.trophies, second.trophies))
                for (tierRange in tierRanges) {
                    val key =
                        BrawlerPairBattleStatsKey(
                            brawler1BrawlStarsId = minOf(first.brawlStarsId, second.brawlStarsId),
                            brawler2BrawlStarsId = maxOf(first.brawlStarsId, second.brawlStarsId),
                            tierRange = tierRange,
                        )
                    resultCounts.add(key, battleResult)
                }
            }
        }
    }

    override fun toCounts(): List<PatchStatsCount> =
        resultCounts.map { key, resultCount ->
            PatchStatsCount(
                keys = listOf(key.tierRange.name, key.brawler1BrawlStarsId, key.brawler2BrawlStarsId),
                counts = listOf(resultCount.victoryCount, resultCount.defeatCount, resultCount.drawCount, resultCount.totalCount),
            )
        }

    companion object {
        private const val MIN_PAIR_TEAM_SIZE = 2
    }
}
