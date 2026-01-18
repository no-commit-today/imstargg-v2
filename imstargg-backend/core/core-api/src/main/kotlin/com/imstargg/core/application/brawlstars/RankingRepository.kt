package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.ranking.BrawlerRanking
import com.imstargg.core.domain.brawlstars.ranking.ClubRanking
import com.imstargg.core.domain.brawlstars.ranking.PlayerRanking
import com.imstargg.core.domain.brawlstars.ranking.RankingPlayer
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import org.springframework.stereotype.Repository

@Repository
class RankingRepository(
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
    private val clubRankingItemJpaRepository: ClubRankingItemJpaRepository,
) {
    fun findPlayerRankings(country: Country): List<PlayerRanking> {
        val latestRanking =
            playerRankingJpaRepository
                .findTopByCountryOrderByCollectedAtDesc(country)
                ?: return emptyList()

        val latestItems =
            playerRankingItemJpaRepository
                .findByPlayerRankingId(latestRanking.id!!)

        val previousRanking =
            playerRankingJpaRepository
                .findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
                    country,
                    latestRanking.collectedAt,
                )

        val previousRankMap =
            previousRanking?.id?.let { previousId ->
                playerRankingItemJpaRepository
                    .findByPlayerRankingId(previousId)
                    .associate { it.player.brawlStarsTag to it.rank }
            } ?: emptyMap()

        return latestItems.map { item ->
            toPlayerRanking(latestRanking, item, previousRankMap)
        }
    }

    fun findBrawlerRankings(
        country: Country,
        brawlerId: BrawlStarsId,
    ): List<BrawlerRanking> {
        val latestRanking =
            brawlerRankingJpaRepository
                .findTopByCountryAndBrawlerBrawlStarsIdOrderByCollectedAtDesc(
                    country,
                    brawlerId.value,
                ) ?: return emptyList()

        val latestItems =
            brawlerRankingItemJpaRepository
                .findByBrawlerRankingId(latestRanking.id!!)

        val previousRanking =
            brawlerRankingJpaRepository
                .findTopByCountryAndBrawlerBrawlStarsIdAndCollectedAtLessThanOrderByCollectedAtDesc(
                    country,
                    brawlerId.value,
                    latestRanking.collectedAt,
                )

        val previousRankMap =
            previousRanking?.id?.let { previousId ->
                brawlerRankingItemJpaRepository
                    .findByBrawlerRankingId(previousId)
                    .associate { it.player.brawlStarsTag to it.rank }
            } ?: emptyMap()

        return latestItems.map { item ->
            toBrawlerRanking(latestRanking, item, previousRankMap)
        }
    }

    fun findClubRankings(country: Country): List<ClubRanking> {
        val latestRanking =
            clubRankingJpaRepository
                .findTopByCountryOrderByCollectedAtDesc(country)
                ?: return emptyList()

        val latestItems =
            clubRankingItemJpaRepository
                .findByClubRankingId(latestRanking.id!!)

        val previousRanking =
            clubRankingJpaRepository
                .findTopByCountryAndCollectedAtLessThanOrderByCollectedAtDesc(
                    country,
                    latestRanking.collectedAt,
                )

        val previousRankMap =
            previousRanking?.id?.let { previousId ->
                clubRankingItemJpaRepository
                    .findByClubRankingId(previousId)
                    .associate { it.brawlStarsTag to it.rank }
            } ?: emptyMap()

        return latestItems.map { item ->
            toClubRanking(latestRanking, item, previousRankMap)
        }
    }

    private fun calculateRankChange(
        currentRank: Int,
        previousRankMap: Map<String, Int>,
        key: String,
    ): Int? {
        val previousRank = previousRankMap[key] ?: return null
        return previousRank - currentRank
    }

    private fun toPlayerRanking(
        entity: PlayerRankingEntity,
        item: PlayerRankingItemEntity,
        previousRankMap: Map<String, Int>,
    ) = PlayerRanking(
        country = entity.country,
        rank = item.rank,
        player = toRankingPlayer(item.player),
        trophies = item.trophies,
        rankChange = calculateRankChange(item.rank, previousRankMap, item.player.brawlStarsTag),
    )

    private fun toBrawlerRanking(
        entity: BrawlerRankingEntity,
        item: BrawlerRankingItemEntity,
        previousRankMap: Map<String, Int>,
    ) = BrawlerRanking(
        country = entity.country,
        brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId),
        rank = item.rank,
        player = toRankingPlayer(item.player),
        trophies = item.trophies,
        rankChange = calculateRankChange(item.rank, previousRankMap, item.player.brawlStarsTag),
    )

    private fun toClubRanking(
        entity: ClubRankingEntity,
        item: ClubRankingItemEntity,
        previousRankMap: Map<String, Int>,
    ) = ClubRanking(
        country = entity.country,
        tag = BrawlStarsTag(item.brawlStarsTag),
        name = item.name,
        badgeId = BrawlStarsId(item.badgeBrawlStarsId),
        trophies = item.trophies,
        rank = item.rank,
        memberCount = item.memberCount,
        rankChange = calculateRankChange(item.rank, previousRankMap, item.brawlStarsTag),
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
