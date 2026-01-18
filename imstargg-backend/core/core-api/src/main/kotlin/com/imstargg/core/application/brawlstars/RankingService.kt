package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.ranking.BrawlerRanking
import com.imstargg.core.domain.brawlstars.ranking.ClubRanking
import com.imstargg.core.domain.brawlstars.ranking.PlayerRanking
import com.imstargg.core.enums.Country
import org.springframework.stereotype.Service

@Service
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    fun getPlayerRankings(country: Country): List<PlayerRanking> = rankingRepository.findPlayerRankings(country)

    fun getBrawlerRankings(
        country: Country,
        brawlerId: BrawlStarsId,
    ): List<BrawlerRanking> = rankingRepository.findBrawlerRankings(country, brawlerId)

    fun getClubRankings(country: Country): List<ClubRanking> = rankingRepository.findClubRankings(country)
}
