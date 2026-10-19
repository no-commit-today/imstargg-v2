package com.imstargg.core.admin.application

import com.imstargg.core.admin.config.CloudFrontProperties
import com.imstargg.core.admin.request.BrawlerCreateRequest
import com.imstargg.core.admin.request.BrawlerSubItemCreateRequest
import com.imstargg.core.admin.request.BuffyCreateRequest
import com.imstargg.core.admin.response.BrawlerDetailResponse
import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.BuffyResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.HyperChargeResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.enums.BuffyType
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class BrawlerService(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val buffyJpaRepository: BuffyJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
    private val cloudFrontProperties: CloudFrontProperties,
    private val clock: Clock,
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

        val starPowers = starPowerJpaRepository.findAllByBrawlerBrawlStarsIdAndDeletedAtIsNull(brawlStarsId)
        val gadgets = gadgetJpaRepository.findAllByBrawlerBrawlStarsIdAndDeletedAtIsNull(brawlStarsId)
        val hyperCharges = hyperChargeJpaRepository.findAllByBrawlerBrawlStarsIdAndDeletedAtIsNull(brawlStarsId)
        val brawlerGears = brawlerGearJpaRepository.findAllByBrawlerBrawlStarsIdAndDeletedAtIsNull(brawlStarsId)
        val buffies = buffyJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
        val allGears = gearJpaRepository.findAllByDeletedAtIsNull()

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
            buffies =
                buffies.map { buffy ->
                    BuffyResponse(
                        type = buffy.type,
                        imageUrl = imagesByKey[buffy.type.toImageType() to brawlStarsId]?.toImageUrl(),
                    )
                },
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
        doUpdateMessages(MessageType.BRAWLER_NAME, request.brawlStarsId, request.names)
        request.gearBrawlStarsIds.forEach { gearId ->
            brawlerGearJpaRepository.save(BrawlerGearEntity(request.brawlStarsId, gearId))
        }
    }

    @Transactional
    fun createStarPower(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        starPowerJpaRepository.save(StarPowerEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.STARPOWER_NAME, request.brawlStarsId, request.names)
    }

    @Transactional
    fun createGadget(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        gadgetJpaRepository.save(GadgetEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.GADGET_NAME, request.brawlStarsId, request.names)
    }

    @Transactional
    fun createHyperCharge(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        hyperChargeJpaRepository.save(HyperChargeEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.HYPERCHARGE_NAME, request.brawlStarsId, request.names)
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
    fun createBuffy(
        brawlerBrawlStarsId: Long,
        request: BuffyCreateRequest,
    ) {
        require(buffyJpaRepository.findByBrawlerBrawlStarsIdAndType(brawlerBrawlStarsId, request.type) == null) {
            "이미 등록된 버피입니다: brawlerBrawlStarsId=$brawlerBrawlStarsId, type=${request.type}"
        }
        buffyJpaRepository.save(BuffyEntity(brawlerBrawlStarsId, request.type))
    }

    @Transactional
    fun updateBuffyImage(
        brawlerBrawlStarsId: Long,
        buffyType: BuffyType,
        update: ImageUpdate,
    ) {
        requireNotNull(buffyJpaRepository.findByBrawlerBrawlStarsIdAndType(brawlerBrawlStarsId, buffyType)) {
            "등록되지 않은 버피입니다: brawlerBrawlStarsId=$brawlerBrawlStarsId, type=$buffyType"
        }
        doUpdateImage(buffyType.toImageType(), brawlerBrawlStarsId, update)
    }

    @Transactional
    fun deleteBuffy(
        brawlerBrawlStarsId: Long,
        buffyType: BuffyType,
    ) {
        val imageType = buffyType.toImageType()
        val existingImage = imageJpaRepository.findByTypeAndBrawlStarsId(imageType, brawlerBrawlStarsId)
        buffyJpaRepository.deleteByBrawlerBrawlStarsIdAndType(brawlerBrawlStarsId, buffyType)
        imageJpaRepository.deleteByTypeAndBrawlStarsId(imageType, brawlerBrawlStarsId)
        if (existingImage != null) {
            imageStorageService.deleteAfterCommit(existingImage.storedName)
        }
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

    /**
     * 연결 해제된 기어는 행을 지우지 않고 soft-delete 한다 — 브롤스타즈 API 가 게임에서 제거된 기어를
     * 계속 응답하므로, 행을 지우면 `brawlerSyncJob` 이 매번 "기어 추가"로 감지한다.
     * (브롤러, 기어) 유니크 인덱스 때문에 다시 연결할 때는 새 행 대신 기존 행을 복구한다.
     */
    @Transactional
    fun updateBrawlerGears(
        brawlerBrawlStarsId: Long,
        gearBrawlStarsIds: Set<Long>,
    ) {
        val existingByGearId =
            brawlerGearJpaRepository
                .findAllByBrawlerBrawlStarsId(brawlerBrawlStarsId)
                .associateBy { it.gearBrawlStarsId }

        existingByGearId.values
            .filter { it.gearBrawlStarsId !in gearBrawlStarsIds && !it.isDeleted }
            .forEach { it.delete(clock) }
        gearBrawlStarsIds.forEach { gearId ->
            val existing = existingByGearId[gearId]
            if (existing == null) {
                brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId, gearId))
            } else if (existing.isDeleted) {
                existing.restore()
            }
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

    private fun BuffyType.toImageType(): BrawlStarsImageType =
        when (this) {
            BuffyType.GADGET -> BrawlStarsImageType.BUFFY_GADGET
            BuffyType.STAR_POWER -> BrawlStarsImageType.BUFFY_STAR_POWER
            BuffyType.HYPER_CHARGE -> BrawlStarsImageType.BUFFY_HYPER_CHARGE
        }
}
