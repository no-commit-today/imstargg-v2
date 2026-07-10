package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CloudFrontProperties
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.brawler.Buffies
import com.imstargg.core.domain.brawlstars.brawler.Buffy
import com.imstargg.core.domain.brawlstars.brawler.Gadget
import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.domain.brawlstars.brawler.HyperCharge
import com.imstargg.core.domain.brawlstars.brawler.StarPower
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.HyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class BrawlerRepository(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val buffyJpaRepository: BuffyJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val brawlStarsImageJpaRepository: BrawlStarsImageJpaRepository,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAll(): List<Brawler> {
        val brawlerEntities = brawlerJpaRepository.findAll().toList()
        val brawlerIds = brawlerEntities.map { it.brawlStarsId }
        val brawlerNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, brawlerIds)
                .groupBy { it.brawlStarsId }
        val portraits =
            brawlStarsImageJpaRepository
                .findAllByTypeIn(listOf(BrawlStarsImageType.BRAWLER_PORTRAIT))
                .associateBy { it.brawlStarsId }

        return brawlerEntities.map { entity ->
            Brawler(
                id = BrawlStarsId(entity.brawlStarsId),
                name = toMessages(brawlerNames[entity.brawlStarsId]),
                rarity = entity.rarity,
                role = entity.role,
                portraitImageUrl = portraits[entity.brawlStarsId]?.toImageUrl(),
            )
        }
    }

    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> {
        val brawlerEntities = brawlerJpaRepository.findAll().toList()
        val gadgetEntities = gadgetJpaRepository.findAll().toList()
        val starPowerEntities = starPowerJpaRepository.findAll().toList()
        val gearEntities = gearJpaRepository.findAll().toList()
        val hyperChargeEntities = hyperChargeJpaRepository.findAll().toList()
        val brawlerGearEntities = brawlerGearJpaRepository.findAll().toList()

        val brawlerIds = brawlerEntities.map { it.brawlStarsId }
        val gadgetIds = gadgetEntities.map { it.brawlStarsId }
        val starPowerIds = starPowerEntities.map { it.brawlStarsId }
        val gearIds = gearEntities.map { it.brawlStarsId }
        val hyperChargeIds = hyperChargeEntities.map { it.brawlStarsId }

        val gadgetNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GADGET_NAME, gadgetIds)
                .groupBy { it.brawlStarsId }
        val starPowerNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, starPowerIds)
                .groupBy { it.brawlStarsId }
        val gearNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, gearIds)
                .groupBy { it.brawlStarsId }
        val hyperChargeNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.HYPERCHARGE_NAME, hyperChargeIds)
                .groupBy { it.brawlStarsId }

        val imagesByKey =
            brawlStarsImageJpaRepository
                .findAllByTypeIn(ITEM_IMAGE_TYPES)
                .associateBy { it.type to it.brawlStarsId }

        val gadgetsByBrawlerId = gadgetEntities.groupBy { it.brawlerBrawlStarsId }
        val starPowersByBrawlerId = starPowerEntities.groupBy { it.brawlerBrawlStarsId }
        val hyperChargesByBrawlerId = hyperChargeEntities.groupBy { it.brawlerBrawlStarsId }
        val gearsByBrawlerId = brawlerGearEntities.groupBy { it.brawlerBrawlStarsId }
        val gearsById = gearEntities.associateBy { it.brawlStarsId }
        val buffiesByBrawlerId =
            buffyJpaRepository
                .findAllByBrawlerBrawlStarsIdIn(brawlerIds)
                .groupBy { it.brawlerBrawlStarsId }

        return brawlerEntities.associate { brawlerEntity ->
            val brawlerId = brawlerEntity.brawlStarsId

            val buffiesByType =
                (buffiesByBrawlerId[brawlerId] ?: emptyList()).associate { buffyEntity ->
                    buffyEntity.type to
                        Buffy(
                            type = buffyEntity.type,
                            imageUrl = imagesByKey[buffyEntity.type.toImageType() to brawlerId]?.toImageUrl(),
                        )
                }
            val buffies =
                buffiesByType[BuffyType.GADGET]?.let { gadget ->
                    val starPower = buffiesByType[BuffyType.STAR_POWER] ?: return@let null
                    val hyperCharge = buffiesByType[BuffyType.HYPER_CHARGE] ?: return@let null
                    Buffies(gadget = gadget, starPower = starPower, hyperCharge = hyperCharge)
                }

            BrawlStarsId(brawlerId) to
                BrawlerItems(
                    gadgets =
                        (gadgetsByBrawlerId[brawlerId] ?: emptyList()).map { toGadget(it, gadgetNames, imagesByKey) },
                    gears =
                        (gearsByBrawlerId[brawlerId] ?: emptyList())
                            .mapNotNull { gearsById[it.gearBrawlStarsId] }
                            .map { toGear(it, gearNames, imagesByKey) },
                    starPowers =
                        (starPowersByBrawlerId[brawlerId] ?: emptyList()).map { toStarPower(it, starPowerNames, imagesByKey) },
                    hyperCharge =
                        (hyperChargesByBrawlerId[brawlerId] ?: emptyList())
                            .firstOrNull()
                            ?.let { toHyperCharge(it, hyperChargeNames, imagesByKey) },
                    buffies = buffies,
                )
        }
    }

    private fun BuffyType.toImageType(): BrawlStarsImageType =
        when (this) {
            BuffyType.GADGET -> BrawlStarsImageType.BUFFY_GADGET
            BuffyType.STAR_POWER -> BrawlStarsImageType.BUFFY_STAR_POWER
            BuffyType.HYPER_CHARGE -> BrawlStarsImageType.BUFFY_HYPER_CHARGE
        }

    private fun toMessages(entities: List<MessageEntity>?): Messages {
        if (entities.isNullOrEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }

    private fun toGadget(
        entity: GadgetEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): Gadget =
        Gadget(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.GADGET to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toGear(
        entity: GearEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): Gear =
        Gear(
            id = BrawlStarsId(entity.brawlStarsId),
            rarity = entity.rarity,
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.GEAR to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toStarPower(
        entity: StarPowerEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): StarPower =
        StarPower(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.STARPOWER to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toHyperCharge(
        entity: HyperChargeEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): HyperCharge =
        HyperCharge(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.HYPERCHARGE to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun BrawlStarsImageEntity.toImageUrl(): String = "${cloudFrontProperties.baseUrl}/$storedName"

    companion object {
        private val ITEM_IMAGE_TYPES =
            listOf(
                BrawlStarsImageType.GADGET,
                BrawlStarsImageType.GEAR,
                BrawlStarsImageType.STARPOWER,
                BrawlStarsImageType.HYPERCHARGE,
                BrawlStarsImageType.BUFFY_GADGET,
                BrawlStarsImageType.BUFFY_STAR_POWER,
                BrawlStarsImageType.BUFFY_HYPER_CHARGE,
            )
    }
}
