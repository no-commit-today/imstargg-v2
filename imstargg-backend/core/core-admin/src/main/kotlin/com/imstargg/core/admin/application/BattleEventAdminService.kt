package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.response.BattleEventResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
@Transactional(readOnly = true)
class BattleEventAdminService(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
) {
    fun findAllEvents(): List<BattleEventResponse> {
        val events = battleEventJpaRepository.findAll()
        val messageCodes = events.mapNotNull { it.mapNameMessageCode() }
        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }

        return events.map { event ->
            val mapMessageCode = event.mapNameMessageCode()
            BattleEventResponse(
                event = event,
                mapNames = if (mapMessageCode != null) toNameMap(messagesByCode[mapMessageCode]) else emptyMap(),
            )
        }
    }

    fun findAllModes(): List<BattleEventModeResponse> {
        val modes = battleEventModeJpaRepository.findAll()
        val messageCodes = modes.map { it.nameMessageCode() }
        val imageCodes = modes.map { it.iconImageCode() }

        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }
        val imagesByCode = imageJpaRepository.findAllByCodeIn(imageCodes).associateBy { it.code }

        return modes.map { mode ->
            BattleEventModeResponse(
                mode = mode,
                names = toNameMap(messagesByCode[mode.nameMessageCode()]),
                icon = imagesByCode[mode.iconImageCode()],
            )
        }
    }

    @Transactional
    fun createMode(
        brawlStarsId: Long,
        brawlStarsName: String,
    ) {
        battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId, brawlStarsName))
    }

    @Transactional
    fun createEvent(
        brawlStarsId: Long,
        modeId: Long?,
        mapName: String?,
    ) {
        val mode = modeId?.let { battleEventModeJpaRepository.findByBrawlStarsId(it) }
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId, mode, mapName))
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
