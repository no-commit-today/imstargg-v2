package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

/**
 * 개별 브롤러의 순위(rank) 분포 통계를 집계한다.
 *
 * ## 집계 키
 * 티어 구간 × 이벤트 × 브롤러. 값은 1위부터 10위까지의 횟수와 합계다.
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)만** 집계 (순위형 배틀에는 적군 개념이 없음)
 * - 각 배틀의 순위(1위~N위)를 분포로 누적 (예: 1위 5회, 2위 3회, ...). 순위가 1~10 이 아니면 실패한다
 * - **티어 구간**: 각 플레이어 본인의 트로피 기준으로 결정
 * - **봇 로비**: 봇 로비 판정에 걸린 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다
 * - **모드**: 솔로·듀오·트리오 쇼다운 유형의 팀 구성인 전투만 집계한다. 모드를 거르지 않던 이전 수집기가 쓴 원본에는
 *   20인 솔로 쇼다운(75)·10팀 듀오 쇼다운(86)이 섞여 있다. 모드 75 는 순위가 1~20 이라 순위 분포(최대 10위)에 담을 수 없고,
 *   순위 10 을 넘는 전투만 빼면 상위 10등만 남아 그 이벤트의 순위 분포가 틀린 값이 되므로 모드 단위로 건너뛴다.
 *
 * 옛 집계기 `BrawlerBattleRankStatsAggregator` 와 규칙이 같다. 2단계에서 옛 것을 지운다.
 * 그때까지 규칙을 바꾸면 두 곳을 함께 고친다(`PatchStatsAggregatorParityTest` 가 어긋남을 잡는다).
 */
class BrawlerBattleRankPatchStatsAggregator : PatchStatsAggregator<StatsRankBattleEntity> {
    private val rankCounts = mutableMapOf<BrawlerEventBattleStatsKey, LongArray>()

    override fun aggregate(battle: StatsRankBattleEntity) {
        if (StatsEventModeType.find(BattleEventModeResultType.RANK, battle.teams.map { it.size }) == null) return
        if (StatsBotBattlePolicy.isBotLobby(battle)) return
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeams()) {
            for (player in team) {
                val tierRanges = StatsTierRange.resolveAll(battleType, player.brawler.trophies)
                for (tierRange in tierRanges) {
                    val key =
                        BrawlerEventBattleStatsKey(
                            tierRange = tierRange,
                            eventBrawlStarsId = battle.eventBrawlStarsId,
                            brawlerBrawlStarsId = player.brawler.brawlStarsId,
                        )
                    require(battle.battleRank in 1..MAX_RANK) { "rank(${battle.battleRank})는 1 이상 $MAX_RANK 이하여야 합니다." }
                    rankCounts.getOrPut(key) { LongArray(MAX_RANK) }[battle.battleRank - 1]++
                }
            }
        }
    }

    override fun toCounts(): List<PatchStatsCount> =
        rankCounts.map { (key, ranks) ->
            PatchStatsCount(
                keys = listOf(key.tierRange.name, key.eventBrawlStarsId, key.brawlerBrawlStarsId),
                counts = ranks.toList() + ranks.sum(),
            )
        }

    companion object {
        private const val MAX_RANK = 10
    }
}
