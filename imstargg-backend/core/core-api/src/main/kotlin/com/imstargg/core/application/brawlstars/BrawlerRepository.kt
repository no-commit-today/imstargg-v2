package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.Gadget
import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.domain.brawlstars.brawler.StarPower
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
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
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val brawlStarsImageJpaRepository: BrawlStarsImageJpaRepository,
) {
    fun findAll(): List<Brawler> {
        val brawlerEntities = brawlerJpaRepository.findAll().toList()
        val gadgetEntities = gadgetJpaRepository.findAll().toList()
        val starPowerEntities = starPowerJpaRepository.findAll().toList()
        val gearEntities = gearJpaRepository.findAll().toList()
        val brawlerGearEntities = brawlerGearJpaRepository.findAll().toList()

        val messageCodes =
            brawlerEntities.map { it.nameMessageCode() } +
                gadgetEntities.map { it.nameMessageCode() } +
                starPowerEntities.map { it.nameMessageCode() } +
                gearEntities.map { it.nameMessageCode() }
        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }

        val imagesByCode = brawlStarsImageJpaRepository.findAll().associateBy { it.code }

        val gearsByBrawlerId = brawlerGearEntities.groupBy { it.brawlerBrawlStarsId }

        val gadgetsByBrawlerId = gadgetEntities.groupBy { it.brawlerBrawlStarsId }
        val starPowersByBrawlerId = starPowerEntities.groupBy { it.brawlerBrawlStarsId }

        val gearsById = gearEntities.associateBy { it.brawlStarsId }

        return brawlerEntities.map { brawlerEntity ->
            val brawlerId = brawlerEntity.brawlStarsId

            Brawler(
                id = BrawlStarsId(brawlerId),
                name = toMessages(messagesByCode[brawlerEntity.nameMessageCode()]),
                rarity = brawlerEntity.rarity,
                role = brawlerEntity.role,
                gadgets = (gadgetsByBrawlerId[brawlerId] ?: emptyList()).map { toGadget(it, messagesByCode, imagesByCode) },
                gears =
                    (gearsByBrawlerId[brawlerId] ?: emptyList())
                        .mapNotNull { gearsById[it.gearBrawlStarsId] }
                        .map { toGear(it, messagesByCode, imagesByCode) },
                starPowers = (starPowersByBrawlerId[brawlerId] ?: emptyList()).map { toStarPower(it, messagesByCode, imagesByCode) },
                portraitImagePath = imagesByCode[brawlerEntity.portraitImageCode()]?.storedName,
            )
        }
    }

    private fun toMessages(entities: List<MessageEntity>?): Messages {
        if (entities.isNullOrEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }

    private fun toGadget(
        entity: GadgetEntity,
        messages: Map<MessageCode, List<MessageEntity>>,
        images: Map<BrawlStarsImageCode, BrawlStarsImageEntity>,
    ): Gadget =
        Gadget(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(messages[entity.nameMessageCode()]),
            imagePath = images[entity.imageCode()]?.storedName,
        )

    private fun toGear(
        entity: GearEntity,
        messages: Map<MessageCode, List<MessageEntity>>,
        images: Map<BrawlStarsImageCode, BrawlStarsImageEntity>,
    ): Gear =
        Gear(
            id = BrawlStarsId(entity.brawlStarsId),
            rarity = entity.rarity,
            name = toMessages(messages[entity.nameMessageCode()]),
            imagePath = images[entity.imageCode()]?.storedName,
        )

    private fun toStarPower(
        entity: StarPowerEntity,
        messages: Map<MessageCode, List<MessageEntity>>,
        images: Map<BrawlStarsImageCode, BrawlStarsImageEntity>,
    ): StarPower =
        StarPower(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(messages[entity.nameMessageCode()]),
            imagePath = images[entity.imageCode()]?.storedName,
        )
}
