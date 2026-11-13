package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

/**
 * 개별 브롤러의 순위(rank) 분포 통계를 집계한다.
 *
 * ## 집계 키
 * 브롤러 × 이벤트 × 티어 구간 × 날짜
 *
 * ## 집계 규칙
 * - **아군 팀(playerTeam)만** 집계 (순위형 배틀에는 적군 개념이 없음)
 * - 각 배틀의 순위(1위~N위)를 분포로 누적 (예: 1위 5회, 2위 3회, ...)
 * - **티어 구간**: 각 플레이어 본인의 트로피 기준으로 결정
 * - **봇 로비**: 봇 로비 판정에 걸린 전투([StatsBotBattlePolicy])는 전투 단위로 건너뛴다. 티어 구간을 정하기 전이라 어느 티어 행에도 들지 않는다
 * - **모드**: 솔로·듀오·트리오 쇼다운 유형의 팀 구성인 전투만 집계한다. 수집기도 같은 모드만 원본에
 *   넣지만, 모드를 거르지 않던 이전 수집기가 쓴 원본에는 2026-11-05 부터 20인 솔로 쇼다운(75)·10팀 듀오 쇼다운(86)이
 *   섞여 있다. 모드 75 는 순위가 1~20 이라 순위 분포(최대 10위)에 담을 수 없고, 한 판이라도 섞이면 그 날짜 전체가
 *   쓰기에서 실패한다. 순위 10 을 넘는 전투만 빼지 않는 이유: 모드 75 의 상위 10등만 남아 그 이벤트의 순위 분포가
 *   틀린 값이 된다.
 */
class BrawlerBattleRankStatsAggregator : BattleStatsAggregator<StatsRankBattleEntity, BrawlerBattleRankStatsEntity> {
    private val rankCounts = RankCountAccumulator<BrawlerBattleStatsKey>()

    override fun aggregate(battle: StatsRankBattleEntity) {
        if (StatsEventModeType.find(BattleEventModeResultType.RANK, battle.teams.map { it.size }) == null) return
        if (StatsBotBattlePolicy.isBotLobby(battle)) return
        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return

        for (team in battle.playerTeams()) {
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
                    rankCounts.add(key, battle.battleRank)
                }
            }
        }
    }

    override fun forEachEntity(consumer: (BrawlerBattleRankStatsEntity) -> Unit) {
        rankCounts.forEach { key, rankCountsEmbedded ->
            consumer(
                BrawlerBattleRankStatsEntity(
                    eventBrawlStarsId = key.eventBrawlStarsId,
                    tierRange = key.tierRange,
                    battleDate = key.battleDate,
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    rankCounts = rankCountsEmbedded,
                ),
            )
        }
    }

    override fun toEntities(): List<BrawlerBattleRankStatsEntity> =
        buildList {
            forEachEntity { add(it) }
        }
}
