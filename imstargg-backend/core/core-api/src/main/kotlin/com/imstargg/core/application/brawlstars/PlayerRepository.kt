package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.domain.brawlstars.player.PlayerClub
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PlayerRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun find(tag: BrawlStarsTag): Player? {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        return toPlayer(entity)
    }

    fun findAllPlayerBrawler(tag: BrawlStarsTag): List<PlayerBrawler> {
        val playerEntity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return emptyList()
        val brawlerEntities = playerBrawlerJpaRepository.findAllByPlayer(playerEntity)
        val brawlers = brawlerRepositoryWithCache.findBrawlers()

        val buffiesByBrawlerId =
            playerBrawlerBuffyJpaRepository
                .findAllByPlayerBrawlerIn(brawlerEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.buffyType })

        return brawlerEntities.map { entity ->
            toPlayerBrawler(
                entity,
                brawlers.find(BrawlStarsId(entity.brawlerBrawlStarsId)),
                buffiesByBrawlerId[entity.id!!]?.toSet() ?: emptySet(),
            )
        }
    }

    private fun toPlayer(entity: PlayerEntity): Player =
        Player(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            nameColor = entity.nameColor,
            iconId = BrawlStarsId(entity.iconBrawlStarsId),
            trophies = entity.trophies,
            highestTrophies = entity.highestTrophies,
            expLevel = entity.expLevel,
            expPoints = entity.expPoints,
            qualifiedFromChampionshipChallenge = entity.qualifiedFromChampionshipChallenge,
            victories3vs3 = entity.victories3vs3,
            victoriesSolo = entity.victoriesSolo,
            victoriesDuo = entity.victoriesDuo,
            bestRoboRumbleTime = entity.bestRoboRumbleTime,
            bestTimeAsBigBrawler = entity.bestTimeAsBigBrawler,
            totalPrestigeLevel = entity.totalPrestigeLevel,
            club = entity.club?.let { PlayerClub(BrawlStarsTag(it.brawlStarsTag), it.name) },
        )

    private fun toPlayerBrawler(
        entity: PlayerBrawlerEntity,
        brawler: Brawler?,
        buffies: Set<BuffyType>,
    ): PlayerBrawler =
        PlayerBrawler(
            brawler = brawler,
            power = entity.power,
            rank = entity.rank,
            trophies = entity.trophies,
            highestTrophies = entity.highestTrophies,
            prestigeLevel = entity.prestigeLevel,
            buffies = buffies,
        )
}
