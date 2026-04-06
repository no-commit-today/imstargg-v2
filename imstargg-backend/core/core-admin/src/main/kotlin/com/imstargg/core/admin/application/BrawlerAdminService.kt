package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.HyperChargeResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.HyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
@Transactional(readOnly = true)
class BrawlerAdminService(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
) {
    fun findAll(): List<BrawlerResponse> {
        val brawlers = brawlerJpaRepository.findAll()
        val brawlerIds = brawlers.map { it.brawlStarsId }
        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, brawlerIds)
                .groupBy { it.brawlStarsId }
        val imagesByKey = imageJpaRepository.findAll().associateBy { it.type to it.brawlStarsId }

        return brawlers.map { brawler ->
            BrawlerResponse(
                brawler = brawler,
                names = toNameMap(messagesByBrawlStarsId[brawler.brawlStarsId]),
                image = imagesByKey[BrawlStarsImageType.BRAWLER_PORTRAIT to brawler.brawlStarsId],
            )
        }
    }

    fun findDetail(brawlStarsId: Long): BrawlerResponse? {
        val brawler = brawlerJpaRepository.findAll().find { it.brawlStarsId == brawlStarsId } ?: return null

        val starPowers = starPowerJpaRepository.findAll().filter { it.brawlerBrawlStarsId == brawlStarsId }
        val gadgets = gadgetJpaRepository.findAll().filter { it.brawlerBrawlStarsId == brawlStarsId }
        val hyperCharges = hyperChargeJpaRepository.findAll().filter { it.brawlerBrawlStarsId == brawlStarsId }
        val brawlerGears = brawlerGearJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
        val allGears = gearJpaRepository.findAll()

        val brawlerNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, listOf(brawlStarsId))
                .groupBy { it.brawlStarsId }
        val starPowerNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, starPowers.map { it.brawlStarsId })
                .groupBy { it.brawlStarsId }
        val gadgetNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GADGET_NAME, gadgets.map { it.brawlStarsId })
                .groupBy { it.brawlStarsId }
        val hyperChargeNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.HYPERCHARGE_NAME, hyperCharges.map { it.brawlStarsId })
                .groupBy { it.brawlStarsId }
        val imagesByKey = imageJpaRepository.findAll().associateBy { it.type to it.brawlStarsId }

        return BrawlerResponse(
            brawler = brawler,
            names = toNameMap(brawlerNameMessages[brawlStarsId]),
            image = imagesByKey[BrawlStarsImageType.BRAWLER_PORTRAIT to brawlStarsId],
            starPowers =
                starPowers.map { sp ->
                    StarPowerResponse(
                        starPower = sp,
                        names = toNameMap(starPowerNameMessages[sp.brawlStarsId]),
                        image = imagesByKey[BrawlStarsImageType.STARPOWER to sp.brawlStarsId],
                    )
                },
            gadgets =
                gadgets.map { gd ->
                    GadgetResponse(
                        gadget = gd,
                        names = toNameMap(gadgetNameMessages[gd.brawlStarsId]),
                        image = imagesByKey[BrawlStarsImageType.GADGET to gd.brawlStarsId],
                    )
                },
            hyperCharges =
                hyperCharges.map { hc ->
                    HyperChargeResponse(
                        hyperCharge = hc,
                        names = toNameMap(hyperChargeNameMessages[hc.brawlStarsId]),
                        image = imagesByKey[BrawlStarsImageType.HYPERCHARGE to hc.brawlStarsId],
                    )
                },
            linkedGearIds = brawlerGears.map { it.gearBrawlStarsId }.toSet(),
            allGears = allGears.map { GearResponse(gear = it, names = emptyMap(), image = null) },
            buffyImages =
                mapOf(
                    BrawlStarsImageType.BUFFY_GADGET to imagesByKey[BrawlStarsImageType.BUFFY_GADGET to brawlStarsId],
                    BrawlStarsImageType.BUFFY_STAR_POWER to imagesByKey[BrawlStarsImageType.BUFFY_STAR_POWER to brawlStarsId],
                    BrawlStarsImageType.BUFFY_HYPER_CHARGE to imagesByKey[BrawlStarsImageType.BUFFY_HYPER_CHARGE to brawlStarsId],
                ),
        )
    }

    @Transactional
    fun createBrawler(
        brawlStarsId: Long,
        rarity: BrawlerRarity,
        role: BrawlerRole,
    ) {
        brawlerJpaRepository.save(BrawlerEntity(brawlStarsId, rarity, role))
    }

    @Transactional
    fun createStarPower(
        brawlStarsId: Long,
        brawlerBrawlStarsId: Long,
    ) {
        starPowerJpaRepository.save(StarPowerEntity(brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun createGadget(
        brawlStarsId: Long,
        brawlerBrawlStarsId: Long,
    ) {
        gadgetJpaRepository.save(GadgetEntity(brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun createHyperCharge(
        brawlStarsId: Long,
        brawlerBrawlStarsId: Long,
    ) {
        hyperChargeJpaRepository.save(HyperChargeEntity(brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun updateMessages(
        type: MessageType,
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        messageJpaRepository.deleteAllByTypeAndBrawlStarsId(type, brawlStarsId)
        langMap.filter { it.value.isNotBlank() }.forEach { (lang, content) ->
            messageJpaRepository.save(MessageEntity(type, brawlStarsId, lang, content))
        }
    }

    @Transactional
    fun updateImage(
        type: BrawlStarsImageType,
        brawlStarsId: Long,
        inputStream: InputStream,
        contentType: String,
    ) {
        val storedName = BrawlStarsImageEntity.storedName(type, brawlStarsId)
        imageStorageService.upload(storedName, inputStream, contentType)
        imageJpaRepository.deleteByTypeAndBrawlStarsId(type, brawlStarsId)
        imageJpaRepository.save(BrawlStarsImageEntity(type, brawlStarsId, storedName))
    }

    @Transactional
    fun updateBrawlerGears(
        brawlerBrawlStarsId: Long,
        gearBrawlStarsIds: Set<Long>,
    ) {
        brawlerGearJpaRepository.deleteAllByBrawlerBrawlStarsId(brawlerBrawlStarsId)
        gearBrawlStarsIds.forEach { gearId ->
            brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId, gearId))
        }
    }
}
