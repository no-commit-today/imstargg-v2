package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerTeamRankSliceStatistics

data class BrawlerTeamRankStatisticsListResponse(
    val totalBattleCount: Long,
    val items: SliceResponse<BrawlerTeamRankStatisticsResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerTeamRankSliceStatistics,
            brawlers: Brawlers,
        ) = BrawlerTeamRankStatisticsListResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                SliceResponse.from(
                    statistics.items.map { item ->
                        BrawlerTeamRankStatisticsResponse(
                            brawlers =
                                item.brawlerIds.map { id ->
                                    brawlers.find(id)?.let { BrawlerResponse.of(it) }
                                },
                            totalBattleCount = item.totalBattleCount,
                            averageRank = item.averageRank,
                        )
                    },
                ),
        )
    }
}
