package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
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
        val messageCodes = gears.map { it.nameMessageCode() }
        val imageCodes = gears.map { it.imageCode() }

        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }
        val imagesByCode = imageJpaRepository.findAllByCodeIn(imageCodes).associateBy { it.code }

        return gears.map { gear ->
            GearResponse(
                gear = gear,
                names = toNameMap(messagesByCode[gear.nameMessageCode()]),
                image = imagesByCode[gear.imageCode()],
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
        code: MessageCode,
        langMap: Map<Language, String>,
    ) {
        messageJpaRepository.deleteAllByCode(code)
        langMap.filter { it.value.isNotBlank() }.forEach { (lang, content) ->
            messageJpaRepository.save(MessageEntity(code, lang, content))
        }
    }

    @Transactional
    fun updateImage(
        code: BrawlStarsImageCode,
        inputStream: InputStream,
        contentType: String,
    ) {
        val key = code.value.replace(":", "/")
        imageStorageService.upload(key, inputStream, contentType)
        imageJpaRepository.deleteByCode(code)
        imageJpaRepository.save(BrawlStarsImageEntity(code, key))
    }
}
