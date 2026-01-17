package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerGadgetEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerGadgetJpaRepository
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerGearEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerGearJpaRepository
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerJpaRepository
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerStarPowerEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntityClub
import org.springframework.stereotype.Component

@Component
class PlayerUpdateApplier(
    private val playerBrawlerRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGearRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerStarPowerRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerGadgetRepository: PlayerBrawlerGadgetJpaRepository,
) {
    fun apply(
        player: PlayerEntity,
        response: PlayerResponse,
    ) {
        updatePlayerInfo(player, response)
        updateBrawlers(player, response.brawlers)
    }

    private fun updatePlayerInfo(
        player: PlayerEntity,
        response: PlayerResponse,
    ) {
        player.update(
            name = response.name,
            nameColor = response.nameColor,
            iconBrawlStarsId = response.icon.id,
            trophies = response.trophies,
            highestTrophies = response.highestTrophies,
            expLevel = response.expLevel,
            expPoints = response.expPoints,
            qualifiedFromChampionshipChallenge = response.isQualifiedFromChampionshipChallenge,
            victories3vs3 = response.victories3vs3,
            victoriesSolo = response.victoriesSolo,
            victoriesDuo = response.victoriesDuo,
            bestRoboRumbleTime = response.bestRoboRumbleTime,
            bestTimeAsBigBrawler = response.bestTimeAsBigBrawler,
            club = response.club?.let { PlayerEntityClub(it.tag, it.name) },
        )
    }

    private fun updateBrawlers(
        player: PlayerEntity,
        brawlerResponses: List<BrawlerStatResponse>,
    ) {
        val existingBrawlers =
            playerBrawlerRepository
                .findAllByPlayer(player)
                .associateBy { it.brawlerBrawlStarsId }

        val updatedBrawlers =
            brawlerResponses.map { response ->
                val existing = existingBrawlers[response.id]
                if (existing != null) {
                    existing.update(response.power, response.rank, response.trophies, response.highestTrophies)
                    existing
                } else {
                    playerBrawlerRepository.save(
                        PlayerBrawlerEntity(
                            player,
                            response.id,
                            response.power,
                            response.rank,
                            response.trophies,
                            response.highestTrophies,
                        ),
                    )
                }
            }

        syncBrawlerAccessories(updatedBrawlers, brawlerResponses)
    }

    private fun syncBrawlerAccessories(
        brawlers: List<PlayerBrawlerEntity>,
        responses: List<BrawlerStatResponse>,
    ) {
        if (brawlers.isEmpty()) return

        val existingGears =
            playerBrawlerGearRepository
                .findAllByPlayerBrawlerIn(brawlers)
                .groupBy({ it.playerBrawler.id!! }, { it.gearBrawlStarsId })
        val existingStarPowers =
            playerBrawlerStarPowerRepository
                .findAllByPlayerBrawlerIn(brawlers)
                .groupBy({ it.playerBrawler.id!! }, { it.starPowerBrawlStarsId })
        val existingGadgets =
            playerBrawlerGadgetRepository
                .findAllByPlayerBrawlerIn(brawlers)
                .groupBy({ it.playerBrawler.id!! }, { it.gadgetBrawlStarsId })

        val brawlerMap = brawlers.associateBy { it.brawlerBrawlStarsId }

        for (response in responses) {
            val brawler = brawlerMap[response.id] ?: continue
            val brawlerId = brawler.id!!

            val existingGearIds = existingGears[brawlerId]?.toSet() ?: emptySet()
            response.gears.filter { it.id !in existingGearIds }.forEach {
                playerBrawlerGearRepository.save(PlayerBrawlerGearEntity(brawler, it.id))
            }

            val existingStarPowerIds = existingStarPowers[brawlerId]?.toSet() ?: emptySet()
            response.starPowers.filter { it.id !in existingStarPowerIds }.forEach {
                playerBrawlerStarPowerRepository.save(PlayerBrawlerStarPowerEntity(brawler, it.id))
            }

            val existingGadgetIds = existingGadgets[brawlerId]?.toSet() ?: emptySet()
            response.gadgets.filter { it.id !in existingGadgetIds }.forEach {
                playerBrawlerGadgetRepository.save(PlayerBrawlerGadgetEntity(brawler, it.id))
            }
        }
    }
}
