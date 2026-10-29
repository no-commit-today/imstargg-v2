package com.imstargg.core.admin.application

import com.imstargg.core.admin.config.CloudFrontProperties
import com.imstargg.core.admin.request.GearCreateRequest
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GearService(
    private val gearJpaRepository: GearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAll(): List<GearResponse> {
        val gears = gearJpaRepository.findAllByDeletedAtIsNull()
        val gearIds = gears.map { it.brawlStarsId }

        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, gearIds)
                .groupBy { it.brawlStarsId }
        val imagesByBrawlStarsId =
            imageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(BrawlStarsImageType.GEAR, gearIds)
                .associateBy { it.brawlStarsId }

        return gears.map { gear ->
            GearResponse(
                brawlStarsId = gear.brawlStarsId,
                names = toNameMap(messagesByBrawlStarsId[gear.brawlStarsId]),
                rarity = gear.rarity,
                imageUrl = imagesByBrawlStarsId[gear.brawlStarsId]?.let { "${cloudFrontProperties.baseUrl}/${it.storedName}" },
            )
        }
    }

    @Transactional
    fun create(request: GearCreateRequest) {
        gearJpaRepository.save(GearEntity(request.brawlStarsId, request.rarity))
        doUpdateMessages(MessageType.GEAR_NAME, request.brawlStarsId, request.names)
    }

    @Transactional
    fun updateGearMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.GEAR_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateGearImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.GEAR, brawlStarsId, update)
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
