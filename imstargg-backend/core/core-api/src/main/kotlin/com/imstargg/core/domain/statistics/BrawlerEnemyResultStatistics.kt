package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    /** 해당 매치업이 등장한 슬롯 수 (= 전투 기록 수). 전체 표본은 wrapper 의 `totalBattleCount` 참조. */
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

/**
 * 결과형 Enemy 통계.
 *
 * `totalBattleCount` 의 의미: 조회 범위의 **게임 수** (2026-05-19 정정).
 * 픽률 = enemy 매치업 슬롯 수 / 게임 수.
 *
 * item 단위는 `battleCount`(매치업별 전투 수)로 구분한다 — 전역 게임 수가 item 에 복사되어
 * 모든 행의 전투수가 동일 표시되던 버그 정정 (2026-06-06).
 */
data class BrawlerEnemyResultStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerEnemyResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerEnemyResultCount>,
        ): BrawlerEnemyResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerEnemyResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        enemyBrawlerId = count.enemyBrawlerId,
                        battleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerEnemyResultStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items = items,
            )
        }
    }
}
