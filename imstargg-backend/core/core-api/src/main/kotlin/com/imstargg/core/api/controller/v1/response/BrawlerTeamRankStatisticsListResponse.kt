package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.api.controller.PageResponse
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerTeamRankPageStatistics

data class BrawlerTeamRankStatisticsListResponse(
    val totalBattleCount: Long,
    val items: PageResponse<BrawlerTeamRankStatisticsResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerTeamRankPageStatistics,
            brawlers: Brawlers,
        ) = BrawlerTeamRankStatisticsListResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                PageResponse.from(
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
