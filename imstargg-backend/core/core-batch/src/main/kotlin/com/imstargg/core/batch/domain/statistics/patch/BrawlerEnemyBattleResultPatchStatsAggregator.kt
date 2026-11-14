package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.BrawlerEnemyBattleStatsKey
import com.imstargg.core.batch.domain.statistics.ResultCountAccumulator
import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 브롤러 vs 적 브롤러 매치업의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 티어 구간 × 내 브롤러 × 적 브롤러. 방향이 있다 — (X, Y) 와 (Y, X) 는 별개 줄이다.
 * 티어는 기준 플레이어(내 브롤러)의 trophies 로 정한다. 이벤트는 키에 없다.
 *
 * ## 봇 전투 제외
 * 봇 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다.
 *
 * 옛 집계기 `BrawlerEnemyBattleResultStatsAggregator` 와 규칙이 같다. 2단계에서 옛 것을 지운다.
 * 그때까지 규칙을 바꾸면 두 곳을 함께 고친다(`PatchStatsAggregatorParityTest` 가 어긋남을 잡는다).
 */
class BrawlerEnemyBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerEnemyBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        if (StatsBotBattlePolicy.isBotBattle(battle)) return
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return
        val battleResult = EnumCodeType.findByCode<BattleResult>(battle.result) ?: return

        for (team in battle.playerTeams()) {
            for (enemyTeam in battle.enemyTeams()) {
                doAggregate(team, enemyTeam, battleType, battleResult)
                doAggregate(enemyTeam, team, battleType, battleResult.opposite())
            }
        }
    }

    private fun doAggregate(
        playerTeam: List<StatsBattleEntityTeamPlayer>,
        enemyTeam: List<StatsBattleEntityTeamPlayer>,
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
                keys = listOf(key.tierRange.name, key.brawlerBrawlStarsId, key.enemyBrawlerBrawlStarsId),
                counts = listOf(resultCount.victoryCount, resultCount.defeatCount, resultCount.drawCount, resultCount.totalCount),
            )
        }
}
