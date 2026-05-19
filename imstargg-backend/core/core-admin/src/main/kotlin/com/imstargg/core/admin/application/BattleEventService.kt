package com.imstargg.core.admin.application

import com.imstargg.core.admin.config.CloudFrontProperties
import com.imstargg.core.admin.request.BattleEventCreateRequest
import com.imstargg.core.admin.request.BattleEventModeCreateRequest
import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.response.BattleEventResponse
import com.imstargg.core.enums.BattleEventResultType
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

@Service
@Transactional(readOnly = true)
class BattleEventService(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAllEvents(): List<BattleEventResponse> {
        val events = battleEventJpaRepository.findAll().toList()
        val eventIds = events.filter { it.mapName != null }.map { it.brawlStarsId }
        val messagesByBrawlStarsId =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, eventIds)
                .groupBy { it.brawlStarsId }

        return events.map { event ->
            BattleEventResponse(
                brawlStarsId = event.brawlStarsId,
                modeBrawlStarsName = event.mode?.brawlStarsName,
                mapName = event.mapName,
                mapNames = if (event.mapName != null) toNameMap(messagesByBrawlStarsId[event.brawlStarsId]) else emptyMap(),
            )
        }
    }

    fun findAllModes(): List<BattleEventModeResponse> {
        val modes = battleEventModeJpaRepository.findAll().toList()
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
                brawlStarsId = mode.brawlStarsId,
                brawlStarsName = mode.brawlStarsName,
                names = toNameMap(messagesByBrawlStarsId[mode.brawlStarsId]),
                iconImageUrl = imagesByBrawlStarsId[mode.brawlStarsId]?.let { "${cloudFrontProperties.baseUrl}/${it.storedName}" },
                resultType = mode.resultType,
                playerCounts = mode.playerCounts,
            )
        }
    }

    @Transactional
    fun createMode(request: BattleEventModeCreateRequest) {
        battleEventModeJpaRepository.save(
            BattleEventModeEntity(
                brawlStarsId = request.brawlStarsId,
                brawlStarsName = request.brawlStarsName,
                resultType = request.resultType,
            ),
        )
    }

    @Transactional
    fun updateModeResultType(
        brawlStarsId: Long,
        resultType: BattleEventResultType,
    ) {
        val entity =
            battleEventModeJpaRepository.findByBrawlStarsId(brawlStarsId)
                ?: throw IllegalArgumentException("BattleEventMode not found: $brawlStarsId")
        entity.changeResultType(resultType)
    }

    @Transactional
    fun updateModePlayerCounts(
        brawlStarsId: Long,
        playerCounts: List<Int>,
    ) {
        val entity =
            battleEventModeJpaRepository.findByBrawlStarsId(brawlStarsId)
                ?: throw IllegalArgumentException("BattleEventMode not found: $brawlStarsId")
        entity.changePlayerCounts(playerCounts)
    }

    @Transactional
    fun createEvent(request: BattleEventCreateRequest) {
        val mode = request.modeId?.let { battleEventModeJpaRepository.findByBrawlStarsId(it) }
        battleEventJpaRepository.save(BattleEventEntity(request.brawlStarsId, mode, request.mapName))
    }

    @Transactional
    fun updateModeMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.BATTLE_EVENT_MODE_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateEventMessages(
        brawlStarsId: Long,
        langMap: Map<Language, String>,
    ) {
        doUpdateMessages(MessageType.BATTLE_EVENT_NAME, brawlStarsId, langMap)
    }

    @Transactional
    fun updateModeImage(
        brawlStarsId: Long,
        update: ImageUpdate,
    ) {
        doUpdateImage(BrawlStarsImageType.BATTLE_EVENT_MODE_ICON, brawlStarsId, update)
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
