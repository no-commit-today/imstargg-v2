package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerBuffies
import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerItem
import com.imstargg.core.domain.brawlstars.player.PlayerClub
import com.imstargg.core.enums.BuffyType
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.infrastructure.jpa.toPageable
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PlayerRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGadgetJpaRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerGearJpaRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerStarPowerJpaRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerHyperChargeJpaRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun isDeleted(tag: BrawlStarsTag): Boolean {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return false
        return entity.status == PlayerStatus.DELETED
    }

    fun find(tag: BrawlStarsTag): Player? {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        return toPlayer(entity)
    }

    fun searchByTag(
        tagPrefix: BrawlStarsTag,
        offsetLimit: OffsetLimit,
    ): Slice<Player> {
        val slice =
            playerJpaRepository.findAllByBrawlStarsTagStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
                brawlStarsTag = tagPrefix.value,
                pageable = offsetLimit.toPageable(),
            )
        return Slice.from(slice.map { toPlayer(it) })
    }

    fun searchByName(
        namePrefix: String,
        offsetLimit: OffsetLimit,
    ): Slice<Player> {
        val slice =
            playerJpaRepository.findAllByNameStartingWithOrderByTrophiesDescBrawlStarsTagAsc(
                name = namePrefix,
                pageable = offsetLimit.toPageable(),
            )
        return Slice.from(slice.map { toPlayer(it) })
    }

    fun findAllPlayerBrawler(tag: BrawlStarsTag): List<PlayerBrawler> {
        val playerEntity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return emptyList()
        val pbEntities = playerBrawlerJpaRepository.findAllByPlayer(playerEntity)
        if (pbEntities.isEmpty()) return emptyList()

        val brawlers = brawlerRepositoryWithCache.findBrawlers()
        val itemsByBrawlerId = brawlerRepositoryWithCache.findAllBrawlerItems()

        val gadgetsByPbId =
            playerBrawlerGadgetJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.gadgetBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val gearsByPbId =
            playerBrawlerGearJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.gearBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val starPowersByPbId =
            playerBrawlerStarPowerJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.starPowerBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val hyperChargesByPbId =
            playerBrawlerHyperChargeJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.hyperChargeBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val buffiesByPbId =
            playerBrawlerBuffyJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.buffyType })
                .mapValues { (_, types) -> types.toSet() }

        return pbEntities.map { entity ->
            val brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId)
            toPlayerBrawler(
                entity = entity,
                brawler = brawlers.find(brawlerId),
                items = itemsByBrawlerId[brawlerId],
                ownedGadgetIds = gadgetsByPbId[entity.id!!] ?: emptySet(),
                ownedGearIds = gearsByPbId[entity.id!!] ?: emptySet(),
                ownedStarPowerIds = starPowersByPbId[entity.id!!] ?: emptySet(),
                ownedHyperChargeIds = hyperChargesByPbId[entity.id!!] ?: emptySet(),
                ownedBuffyTypes = buffiesByPbId[entity.id!!] ?: emptySet(),
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
        items: BrawlerItems?,
        ownedGadgetIds: Set<Long>,
        ownedGearIds: Set<Long>,
        ownedStarPowerIds: Set<Long>,
        ownedHyperChargeIds: Set<Long>,
        ownedBuffyTypes: Set<BuffyType>,
    ): PlayerBrawler {
        val safeItems = if (brawler != null && items != null) items else null

        return PlayerBrawler(
            brawler = brawler,
            power = entity.power,
            rank = entity.rank,
            trophies = entity.trophies,
            highestTrophies = entity.highestTrophies,
            prestigeLevel = entity.prestigeLevel,
            gadgets =
                safeItems?.gadgets?.map { PlayerBrawlerItem(it, it.id.value in ownedGadgetIds) }
                    ?: emptyList(),
            gears =
                safeItems?.gears?.map { PlayerBrawlerItem(it, it.id.value in ownedGearIds) }
                    ?: emptyList(),
            starPowers =
                safeItems?.starPowers?.map { PlayerBrawlerItem(it, it.id.value in ownedStarPowerIds) }
                    ?: emptyList(),
            hyperCharge =
                safeItems?.hyperCharge?.let { PlayerBrawlerItem(it, it.id.value in ownedHyperChargeIds) },
            buffies =
                safeItems?.buffies?.let {
                    PlayerBrawlerBuffies(
                        gadget = PlayerBrawlerItem(it.gadget, BuffyType.GADGET in ownedBuffyTypes),
                        starPower = PlayerBrawlerItem(it.starPower, BuffyType.STAR_POWER in ownedBuffyTypes),
                        hyperCharge = PlayerBrawlerItem(it.hyperCharge, BuffyType.HYPER_CHARGE in ownedBuffyTypes),
                    )
                },
        )
    }
}
