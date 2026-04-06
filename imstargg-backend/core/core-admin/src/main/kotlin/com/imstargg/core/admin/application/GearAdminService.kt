package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.GearRarity
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
import java.io.InputStream

@Service
@Transactional(readOnly = true)
class GearAdminService(
    private val gearJpaRepository: GearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
) {
    fun findAll(): List<GearResponse> {
        val gears = gearJpaRepository.findAll()
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
                gear = gear,
                names = toNameMap(messagesByBrawlStarsId[gear.brawlStarsId]),
                image = imagesByBrawlStarsId[gear.brawlStarsId],
            )
        }
    }

    @Transactional
    fun create(
        brawlStarsId: Long,
        rarity: GearRarity,
    ) {
        gearJpaRepository.save(GearEntity(brawlStarsId, rarity))
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
}
