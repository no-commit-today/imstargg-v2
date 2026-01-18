package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.ranking.BrawlerRanking
import com.imstargg.core.domain.brawlstars.ranking.ClubRanking
import com.imstargg.core.domain.brawlstars.ranking.PlayerRanking
import com.imstargg.core.domain.brawlstars.ranking.RankingPlayer
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import org.springframework.stereotype.Repository

@Repository
class RankingRepository(
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
) {
    fun findPlayerRankings(country: Country): List<PlayerRanking> =
        playerRankingJpaRepository
            .findAllByCountry(country)
            .map { toPlayerRanking(it) }

    fun findBrawlerRankings(
        country: Country,
        brawlerId: BrawlStarsId,
    ): List<BrawlerRanking> =
        brawlerRankingJpaRepository
            .findAllByCountryAndBrawlerBrawlStarsId(country, brawlerId.value)
            .map { toBrawlerRanking(it) }

    fun findClubRankings(country: Country): List<ClubRanking> =
        clubRankingJpaRepository
            .findAllByCountry(country)
            .map { toClubRanking(it) }

    private fun toPlayerRanking(entity: PlayerRankingEntity) =
        PlayerRanking(
            country = entity.country,
            rank = entity.rank,
            player = toRankingPlayer(entity.player),
            trophies = entity.trophies,
            rankChange = entity.rankChange,
        )

    private fun toBrawlerRanking(entity: BrawlerRankingEntity) =
        BrawlerRanking(
            country = entity.country,
            brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId),
            rank = entity.rank,
            player = toRankingPlayer(entity.player),
            trophies = entity.trophies,
            rankChange = entity.rankChange,
        )

    private fun toClubRanking(entity: ClubRankingEntity) =
        ClubRanking(
            country = entity.country,
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            badgeId = BrawlStarsId(entity.badgeBrawlStarsId),
            trophies = entity.trophies,
            rank = entity.rank,
            memberCount = entity.memberCount,
        )

    private fun toRankingPlayer(entity: RankingEntityPlayer) =
        RankingPlayer(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            nameColor = entity.nameColor,
            iconId = BrawlStarsId(entity.iconBrawlStarsId),
            clubName = entity.clubName,
        )
}
