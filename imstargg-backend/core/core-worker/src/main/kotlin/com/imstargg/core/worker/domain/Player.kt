package com.imstargg.core.worker.domain

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

class Player(
    val entity: PlayerEntity,
    brawlers: List<PlayerBrawlerEntity> = emptyList(),
    gears: List<PlayerBrawlerGearEntity> = emptyList(),
    starPowers: List<PlayerBrawlerStarPowerEntity> = emptyList(),
    gadgets: List<PlayerBrawlerGadgetEntity> = emptyList(),
    hyperCharges: List<PlayerBrawlerHyperChargeEntity> = emptyList(),
    buffys: List<PlayerBrawlerBuffyEntity> = emptyList(),
) {
    private val _brawlers: MutableList<PlayerBrawlerEntity> = brawlers.toMutableList()
    private val _gears: MutableList<PlayerBrawlerGearEntity> = gears.toMutableList()
    private val _starPowers: MutableList<PlayerBrawlerStarPowerEntity> = starPowers.toMutableList()
    private val _gadgets: MutableList<PlayerBrawlerGadgetEntity> = gadgets.toMutableList()
    private val _hyperCharges: MutableList<PlayerBrawlerHyperChargeEntity> = hyperCharges.toMutableList()
    private val _buffys: MutableList<PlayerBrawlerBuffyEntity> = buffys.toMutableList()

    val brawlers: List<PlayerBrawlerEntity> get() = _brawlers
    val gears: List<PlayerBrawlerGearEntity> get() = _gears
    val starPowers: List<PlayerBrawlerStarPowerEntity> get() = _starPowers
    val gadgets: List<PlayerBrawlerGadgetEntity> get() = _gadgets
    val hyperCharges: List<PlayerBrawlerHyperChargeEntity> get() = _hyperCharges
    val buffys: List<PlayerBrawlerBuffyEntity> get() = _buffys

    fun update(response: PlayerResponse) {
        entity.update(
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

        val existingBrawlers = _brawlers.associateBy { it.brawlerBrawlStarsId }
        val gearIdsByBrawlerId = _gears.groupBy({ it.playerBrawler.id!! }, { it.gearBrawlStarsId })
        val starPowerIdsByBrawlerId = _starPowers.groupBy({ it.playerBrawler.id!! }, { it.starPowerBrawlStarsId })
        val gadgetIdsByBrawlerId = _gadgets.groupBy({ it.playerBrawler.id!! }, { it.gadgetBrawlStarsId })
        val hyperChargeIdsByBrawlerId = _hyperCharges.groupBy({ it.playerBrawler.id!! }, { it.hyperChargeBrawlStarsId })
        val buffyTypesByBrawlerId = _buffys.groupBy({ it.playerBrawler.id!! }, { it.buffyType })

        for (brawlerResponse in response.brawlers) {
            val existing = existingBrawlers[brawlerResponse.id]
            if (existing != null) {
                existing.update(
                    power = brawlerResponse.power,
                    rank = brawlerResponse.rank,
                    trophies = brawlerResponse.trophies,
                    highestTrophies = brawlerResponse.highestTrophies,
                    prestigeLevel = brawlerResponse.prestigeLevel,
                )
                val brawlerId = existing.id!!
                addNewAccessories(
                    brawler = existing,
                    brawlerResponse = brawlerResponse,
                    existingGearIds = gearIdsByBrawlerId[brawlerId]?.toSet() ?: emptySet(),
                    existingStarPowerIds = starPowerIdsByBrawlerId[brawlerId]?.toSet() ?: emptySet(),
                    existingGadgetIds = gadgetIdsByBrawlerId[brawlerId]?.toSet() ?: emptySet(),
                    existingHyperChargeIds = hyperChargeIdsByBrawlerId[brawlerId]?.toSet() ?: emptySet(),
                    existingBuffyTypes = buffyTypesByBrawlerId[brawlerId]?.toSet() ?: emptySet(),
                )
            } else {
                val newBrawler =
                    PlayerBrawlerEntity(
                        player = entity,
                        brawlerBrawlStarsId = brawlerResponse.id,
                        power = brawlerResponse.power,
                        rank = brawlerResponse.rank,
                        trophies = brawlerResponse.trophies,
                        highestTrophies = brawlerResponse.highestTrophies,
                        prestigeLevel = brawlerResponse.prestigeLevel,
                    )
                _brawlers.add(newBrawler)
                addNewAccessories(
                    brawler = newBrawler,
                    brawlerResponse = brawlerResponse,
                    existingGearIds = emptySet(),
                    existingStarPowerIds = emptySet(),
                    existingGadgetIds = emptySet(),
                    existingHyperChargeIds = emptySet(),
                    existingBuffyTypes = emptySet(),
                )
            }
        }
    }

    private fun addNewAccessories(
        brawler: PlayerBrawlerEntity,
        brawlerResponse: BrawlerStatResponse,
        existingGearIds: Set<Long>,
        existingStarPowerIds: Set<Long>,
        existingGadgetIds: Set<Long>,
        existingHyperChargeIds: Set<Long>,
        existingBuffyTypes: Set<BuffyType>,
    ) {
        brawlerResponse.gears
            .asSequence()
            .filter { it.id !in existingGearIds }
            .mapTo(_gears) { PlayerBrawlerGearEntity(brawler, it.id) }
        brawlerResponse.starPowers
            .asSequence()
            .filter { it.id !in existingStarPowerIds }
            .mapTo(_starPowers) { PlayerBrawlerStarPowerEntity(brawler, it.id) }
        brawlerResponse.gadgets
            .asSequence()
            .filter { it.id !in existingGadgetIds }
            .mapTo(_gadgets) { PlayerBrawlerGadgetEntity(brawler, it.id) }
        brawlerResponse.hyperCharges
            .asSequence()
            .filter { it.id !in existingHyperChargeIds }
            .mapTo(_hyperCharges) { PlayerBrawlerHyperChargeEntity(brawler, it.id) }
        val activeBuffies =
            buildSet {
                if (brawlerResponse.buffies.gadget) add(BuffyType.GADGET)
                if (brawlerResponse.buffies.starPower) add(BuffyType.STAR_POWER)
                if (brawlerResponse.buffies.hyperCharge) add(BuffyType.HYPER_CHARGE)
            }
        activeBuffies
            .asSequence()
            .filter { it !in existingBuffyTypes }
            .mapTo(_buffys) { PlayerBrawlerBuffyEntity(brawler, it) }
    }

    companion object {
        fun create(response: PlayerResponse): Player =
            Player(
                entity =
                    PlayerEntity(
                        brawlStarsTag = response.tag,
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
                    ),
            )
    }
}
