package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.ranking.ClubRanking
import com.imstargg.core.enums.Country

data class ClubRankingResponse(
    val country: Country,
    val tag: String,
    val name: String,
    val badgeId: Long,
    val trophies: Int,
    val rank: Int,
    val memberCount: Int,
) {
    companion object {
        fun of(ranking: ClubRanking) =
            ClubRankingResponse(
                country = ranking.country,
                tag = ranking.tag.value,
                name = ranking.name,
                badgeId = ranking.badgeId.value,
                trophies = ranking.trophies,
                rank = ranking.rank,
                memberCount = ranking.memberCount,
            )
    }
}
