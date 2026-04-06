package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntityClub
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlayerUpdateApplier(
    private val playerRepository: PlayerJpaRepository,
    private val playerBrawlerRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGearRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerStarPowerRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerGadgetRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerHyperChargeRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyRepository: PlayerBrawlerBuffyJpaRepository,
) {
    @Transactional
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
            totalPrestigeLevel = response.totalPrestigeLevel,
            club = response.club?.let { PlayerEntityClub(it.tag, it.name) },
        )
        playerRepository.save(player)
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
                    existing.update(
                        response.power,
                        response.rank,
                        response.trophies,
                        response.highestTrophies,
                        response.prestigeLevel,
                    )
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
                            response.prestigeLevel,
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
        val existingHyperCharges =
            playerBrawlerHyperChargeRepository
                .findAllByPlayerBrawlerIn(brawlers)
                .groupBy({ it.playerBrawler.id!! }, { it.hyperChargeBrawlStarsId })
        val existingBuffies =
            playerBrawlerBuffyRepository
                .findAllByPlayerBrawlerIn(brawlers)
                .groupBy({ it.playerBrawler.id!! }, { it.buffyType })

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

            val existingHyperChargeIds = existingHyperCharges[brawlerId]?.toSet() ?: emptySet()
            response.hyperCharges.filter { it.id !in existingHyperChargeIds }.forEach {
                playerBrawlerHyperChargeRepository.save(PlayerBrawlerHyperChargeEntity(brawler, it.id))
            }

            val activeBuffies =
                buildSet {
                    if (response.buffies.gadget) add(BuffyType.GADGET)
                    if (response.buffies.starPower) add(BuffyType.STAR_POWER)
                    if (response.buffies.hyperCharge) add(BuffyType.HYPER_CHARGE)
                }
            val existingBuffyTypes = existingBuffies[brawlerId]?.toSet() ?: emptySet()
            if (activeBuffies != existingBuffyTypes) {
                playerBrawlerBuffyRepository.deleteAllByPlayerBrawler(brawler)
                activeBuffies.forEach { type ->
                    playerBrawlerBuffyRepository.save(PlayerBrawlerBuffyEntity(brawler, type))
                }
            }
        }
    }
}
