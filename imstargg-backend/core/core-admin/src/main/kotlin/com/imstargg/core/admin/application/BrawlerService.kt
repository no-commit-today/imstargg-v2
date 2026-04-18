package com.imstargg.core.admin.application

import com.imstargg.core.admin.config.CloudFrontProperties
import com.imstargg.core.admin.controller.request.BrawlerCreateRequest
import com.imstargg.core.admin.controller.request.BrawlerSubItemCreateRequest
import com.imstargg.core.admin.controller.response.BrawlerDetailResponse
import com.imstargg.core.admin.controller.response.BrawlerResponse
import com.imstargg.core.admin.controller.response.GadgetResponse
import com.imstargg.core.admin.controller.response.GearResponse
import com.imstargg.core.admin.controller.response.HyperChargeResponse
import com.imstargg.core.admin.controller.response.StarPowerResponse
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

@Service
@Transactional(readOnly = true)
class BrawlerService(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAll(): List<BrawlerResponse> {
        val brawlers = brawlerJpaRepository.findAll().toList()
        val brawlerIds = brawlers.map { it.brawlStarsId }
        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, brawlerIds)
                .groupBy { it.brawlStarsId }
        val imagesByBrawlStarsId =
            imageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(BrawlStarsImageType.BRAWLER_PORTRAIT, brawlerIds)
                .associateBy { it.brawlStarsId }

        return brawlers.map { brawler ->
            BrawlerResponse(
                brawlStarsId = brawler.brawlStarsId,
                names = toNameMap(messagesByBrawlStarsId[brawler.brawlStarsId]),
                rarity = brawler.rarity,
                role = brawler.role,
                imageUrl = imagesByBrawlStarsId[brawler.brawlStarsId]?.toImageUrl(),
            )
        }
    }

    private fun BrawlStarsImageEntity.toImageUrl(): String = "${cloudFrontProperties.baseUrl}/$storedName"

    fun findDetail(brawlStarsId: Long): BrawlerDetailResponse? {
        val brawler = brawlerJpaRepository.findByBrawlStarsId(brawlStarsId) ?: return null

        val starPowers = starPowerJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
        val gadgets = gadgetJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
        val hyperCharges = hyperChargeJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
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
        val gearNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, allGears.map { it.brawlStarsId })
                .groupBy { it.brawlStarsId }
        val relevantTypes =
            listOf(
                BrawlStarsImageType.BRAWLER_PORTRAIT,
                BrawlStarsImageType.STARPOWER,
                BrawlStarsImageType.GADGET,
                BrawlStarsImageType.HYPERCHARGE,
                BrawlStarsImageType.BUFFY_GADGET,
                BrawlStarsImageType.BUFFY_STAR_POWER,
                BrawlStarsImageType.BUFFY_HYPER_CHARGE,
            )
        val relevantIds =
            listOf(brawlStarsId) +
                starPowers.map { it.brawlStarsId } +
                gadgets.map { it.brawlStarsId } +
                hyperCharges.map { it.brawlStarsId }
        val imagesByKey =
            imageJpaRepository
                .findAllByTypeInAndBrawlStarsIdIn(relevantTypes, relevantIds)
                .associateBy { it.type to it.brawlStarsId }

        return BrawlerDetailResponse(
            brawlStarsId = brawler.brawlStarsId,
            names = toNameMap(brawlerNameMessages[brawlStarsId]),
            rarity = brawler.rarity,
            role = brawler.role,
            imageUrl = imagesByKey[BrawlStarsImageType.BRAWLER_PORTRAIT to brawlStarsId]?.toImageUrl(),
            buffyImages =
                mapOf(
                    BrawlStarsImageType.BUFFY_GADGET to
                        imagesByKey[BrawlStarsImageType.BUFFY_GADGET to brawlStarsId]?.toImageUrl(),
                    BrawlStarsImageType.BUFFY_STAR_POWER to
                        imagesByKey[BrawlStarsImageType.BUFFY_STAR_POWER to brawlStarsId]?.toImageUrl(),
                    BrawlStarsImageType.BUFFY_HYPER_CHARGE to
                        imagesByKey[BrawlStarsImageType.BUFFY_HYPER_CHARGE to brawlStarsId]?.toImageUrl(),
                ),
            starPowers =
                starPowers.map { sp ->
                    StarPowerResponse(
                        brawlStarsId = sp.brawlStarsId,
                        names = toNameMap(starPowerNameMessages[sp.brawlStarsId]),
                        imageUrl = imagesByKey[BrawlStarsImageType.STARPOWER to sp.brawlStarsId]?.toImageUrl(),
                    )
                },
            gadgets =
                gadgets.map { gd ->
                    GadgetResponse(
                        brawlStarsId = gd.brawlStarsId,
                        names = toNameMap(gadgetNameMessages[gd.brawlStarsId]),
                        imageUrl = imagesByKey[BrawlStarsImageType.GADGET to gd.brawlStarsId]?.toImageUrl(),
                    )
                },
            hyperCharges =
                hyperCharges.map { hc ->
                    HyperChargeResponse(
                        brawlStarsId = hc.brawlStarsId,
                        names = toNameMap(hyperChargeNameMessages[hc.brawlStarsId]),
                        imageUrl = imagesByKey[BrawlStarsImageType.HYPERCHARGE to hc.brawlStarsId]?.toImageUrl(),
                    )
                },
            linkedGearBrawlStarsIds = brawlerGears.map { it.gearBrawlStarsId }.toSet(),
            allGears =
                allGears.map { gear ->
                    GearResponse(
                        brawlStarsId = gear.brawlStarsId,
                        names = toNameMap(gearNameMessages[gear.brawlStarsId]),
                        rarity = gear.rarity,
                        imageUrl = null,
                    )
                },
        )
    }

    @Transactional
    fun createBrawler(request: BrawlerCreateRequest) {
        brawlerJpaRepository.save(BrawlerEntity(request.brawlStarsId, request.rarity, request.role))
    }

    @Transactional
    fun createStarPower(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        starPowerJpaRepository.save(StarPowerEntity(request.brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun createGadget(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        gadgetJpaRepository.save(GadgetEntity(request.brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun createHyperCharge(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        hyperChargeJpaRepository.save(HyperChargeEntity(request.brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun updateBrawlerMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.BRAWLER_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateStarPowerMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.STARPOWER_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateGadgetMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.GADGET_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateHyperChargeMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.HYPERCHARGE_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateBrawlerImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.BRAWLER_PORTRAIT, brawlStarsId, update)
    }

    @Transactional
    fun updateBuffyImage(
        buffyType: BrawlStarsImageType,
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(buffyType, brawlStarsId, update)
    }

    @Transactional
    fun updateStarPowerImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.STARPOWER, brawlStarsId, update)
    }

    @Transactional
    fun updateGadgetImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.GADGET, brawlStarsId, update)
    }

    @Transactional
    fun updateHyperChargeImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.HYPERCHARGE, brawlStarsId, update)
    }

    @Transactional
    fun updateBrawlerGears(
        brawlerBrawlStarsId: Long,
        gearBrawlStarsIds: Set<Long>,
    ) {
        brawlerGearJpaRepository.deleteAllByBrawlerBrawlStarsId(brawlerBrawlStarsId)
        brawlerGearJpaRepository.flush()
        gearBrawlStarsIds.forEach { gearId ->
            brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId, gearId))
        }
    }

    private fun doUpdateMessages(
        type: MessageType,
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        messageJpaRepository.deleteAllByTypeAndBrawlStarsId(type, brawlStarsId)
        messageJpaRepository.flush()
        langMap.filter { it.value.isNotBlank() }.forEach { (lang, content) ->
            messageJpaRepository.save(MessageEntity(type, brawlStarsId, lang, content))
        }
    }

    private fun doUpdateImage(
        type: BrawlStarsImageType,
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        val storedName = BrawlStarsImageEntity.storedName(type, brawlStarsId, update.ext)
        val existing = imageJpaRepository.findByTypeAndBrawlStarsId(type, brawlStarsId)
        imageStorageService.upload(storedName, update.inputStream, update.contentType)
        imageJpaRepository.deleteByTypeAndBrawlStarsId(type, brawlStarsId)
        imageJpaRepository.flush()
        imageJpaRepository.save(BrawlStarsImageEntity(type, brawlStarsId, storedName))
        if (existing != null && existing.storedName != storedName) {
            imageStorageService.deleteAfterCommit(existing.storedName)
        }
    }
}
