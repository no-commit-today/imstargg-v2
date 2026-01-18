package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.ranking.PlayerRanking
import com.imstargg.core.enums.Country

data class PlayerRankingResponse(
    val country: Country,
    val rank: Int,
    val player: RankingPlayerResponse,
    val trophies: Int,
    val rankChange: Int?,
) {
    companion object {
        fun of(ranking: PlayerRanking) =
            PlayerRankingResponse(
                country = ranking.country,
                rank = ranking.rank,
                player = RankingPlayerResponse.of(ranking.player),
                trophies = ranking.trophies,
                rankChange = ranking.rankChange,
            )
    }
}
