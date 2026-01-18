package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.ranking.BrawlerRanking
import com.imstargg.core.enums.Country

data class BrawlerRankingResponse(
    val country: Country,
    val brawlerId: Long,
    val rank: Int,
    val player: RankingPlayerResponse,
    val trophies: Int,
    val rankChange: Int?,
) {
    companion object {
        fun of(ranking: BrawlerRanking) =
            BrawlerRankingResponse(
                country = ranking.country,
                brawlerId = ranking.brawlerId.value,
                rank = ranking.rank,
                player = RankingPlayerResponse.of(ranking.player),
                trophies = ranking.trophies,
                rankChange = ranking.rankChange,
            )
    }
}
