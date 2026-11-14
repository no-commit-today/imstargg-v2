package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.ResultCountAccumulator
import com.imstargg.core.batch.domain.statistics.StarPlayerCountAccumulator
import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 개별 브롤러의 승/패/무 결과 통계를 집계한다.
 *
 * ## 집계 키
 * 티어 구간 × 이벤트 × 브롤러. 값은 승·패·무·합계·스타 플레이어 횟수다.
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)**: 배틀 결과를 그대로 집계
 * - **적군 팀(enemyTeams)**: 배틀 결과의 반대(opposite)를 집계.
 *   예: 아군이 승리하면 적군 각 브롤러에는 패배로 집계
 * - **스타 플레이어**: 해당 배틀의 스타 플레이어와 태그가 일치하면 카운트 증가
 * - **티어 구간**: 각 플레이어 본인의 트로피 기준으로 결정
 * - **봇 전투**: 봇 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다
 *
 * 옛 집계기 `BrawlerBattleResultStatsAggregator` 와 규칙이 같다. 2단계에서 옛 것을 지운다.
 * 그때까지 규칙을 바꾸면 두 곳을 함께 고친다(`PatchStatsAggregatorParityTest` 가 어긋남을 잡는다).
 */
class BrawlerBattleResultPatchStatsAggregator : PatchStatsAggregator<StatsResultBattleEntity> {
    private val resultCounts = ResultCountAccumulator<BrawlerEventBattleStatsKey>()
    private val starPlayerCounts = StarPlayerCountAccumulator<BrawlerEventBattleStatsKey>()

    override fun aggregate(battle: StatsResultBattleEntity) {
        if (StatsBotBattlePolicy.isBotBattle(battle)) return
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
                    BrawlerEventBattleStatsKey(
                        tierRange = tierRange,
                        eventBrawlStarsId = battle.eventBrawlStarsId,
                        brawlerBrawlStarsId = player.brawler.brawlStarsId,
                    )
                resultCounts.add(key, result)
                if (player.brawlStarsTag == battle.starPlayerBrawlStarsTag) {
                    starPlayerCounts.add(key)
                }
            }
        }
    }

    override fun toCounts(): List<PatchStatsCount> =
        resultCounts.map { key, resultCount ->
            PatchStatsCount(
                keys = listOf(key.tierRange.name, key.eventBrawlStarsId, key.brawlerBrawlStarsId),
                counts =
                    listOf(
                        resultCount.victoryCount,
                        resultCount.defeatCount,
                        resultCount.drawCount,
                        resultCount.totalCount,
                        starPlayerCounts[key],
                    ),
            )
        }
}
