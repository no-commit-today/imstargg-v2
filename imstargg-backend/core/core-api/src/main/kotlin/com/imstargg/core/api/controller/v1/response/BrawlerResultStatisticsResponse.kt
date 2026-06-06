package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerResultStatistics

data class BrawlerResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: Int,
    val tier: String,
) {
    /** 구버전 프론트 호환용 (= [battleCount]). 프론트의 battleCount 전환 배포 후 제거 예정. */
    val totalBattleCount: Long
        get() = battleCount
}

data class BrawlerResultStatisticsResponse(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItemResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerResultStatistics,
            brawlers: Brawlers,
        ) = BrawlerResultStatisticsResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                statistics.items.map { item ->
                    BrawlerResultStatisticsItemResponse(
                        brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                        battleCount = item.battleCount,
                        pickRate = item.pickRate,
                        winRate = item.winRate,
                        starPlayerRate = item.starPlayerRate,
                        tierScore = item.tierScore.value,
                        tier = item.tierScore.tier().name,
                    )
                },
        )
    }
}
