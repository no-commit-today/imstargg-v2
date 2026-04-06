package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.response.BattleEventResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
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
        val eventIds = events.filter { it.mapName != null }.map { it.brawlStarsId }
        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, eventIds)
                .groupBy { it.brawlStarsId }

        return events.map { event ->
            BattleEventResponse(
                event = event,
                mapNames = if (event.mapName != null) toNameMap(messagesByBrawlStarsId[event.brawlStarsId]) else emptyMap(),
            )
        }
    }

    fun findAllModes(): List<BattleEventModeResponse> {
        val modes = battleEventModeJpaRepository.findAll()
        val modeIds = modes.map { it.brawlStarsId }

        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_MODE_NAME, modeIds)
                .groupBy { it.brawlStarsId }
        val imagesByBrawlStarsId =
            imageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(BrawlStarsImageType.BATTLE_EVENT_MODE_ICON, modeIds)
                .associateBy { it.brawlStarsId }

        return modes.map { mode ->
            BattleEventModeResponse(
                mode = mode,
                names = toNameMap(messagesByBrawlStarsId[mode.brawlStarsId]),
                icon = imagesByBrawlStarsId[mode.brawlStarsId],
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
