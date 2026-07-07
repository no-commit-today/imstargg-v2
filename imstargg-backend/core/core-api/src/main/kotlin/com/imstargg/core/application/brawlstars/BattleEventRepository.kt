package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CloudFrontProperties
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class BattleEventRepository(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val brawlStarsImageJpaRepository: BrawlStarsImageJpaRepository,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAll(): List<BattleEvent> {
        val eventEntities = battleEventJpaRepository.findAll().toList()
        if (eventEntities.isEmpty()) return emptyList()

        val eventIds = eventEntities.map { it.brawlStarsId }
        val modeIds = eventEntities.mapNotNull { it.mode }.map { it.brawlStarsId }

        val mapNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, eventIds)
                .groupBy { it.brawlStarsId }
        val modeNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_MODE_NAME, modeIds)
                .groupBy { it.brawlStarsId }
        val modeIconImages =
            brawlStarsImageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(BrawlStarsImageType.BATTLE_EVENT_MODE_ICON, modeIds)
                .associateBy { it.brawlStarsId }

        return eventEntities.map { eventEntity ->
            toBattleEvent(eventEntity, mapNameMessages, modeNameMessages, modeIconImages)
        }
    }

    fun find(id: BrawlStarsId): BattleEvent? {
        val eventEntity = battleEventJpaRepository.findByBrawlStarsId(id.value) ?: return null

        val mapNameMessages =
            if (eventEntity.mapName != null) {
                messageJpaRepository.findAllByTypeAndBrawlStarsId(
                    MessageType.BATTLE_EVENT_NAME,
                    eventEntity.brawlStarsId,
                )
            } else {
                emptyList()
            }

        val mode =
            eventEntity.mode?.let { modeEntity ->
                val modeNameMessages =
                    messageJpaRepository.findAllByTypeAndBrawlStarsId(
                        MessageType.BATTLE_EVENT_MODE_NAME,
                        modeEntity.brawlStarsId,
                    )
                val modeIconImage =
                    brawlStarsImageJpaRepository.findByTypeAndBrawlStarsId(
                        BrawlStarsImageType.BATTLE_EVENT_MODE_ICON,
                        modeEntity.brawlStarsId,
                    )
                BattleEventMode(
                    id = BrawlStarsId(modeEntity.brawlStarsId),
                    name = toMessages(modeNameMessages),
                    iconImageUrl = modeIconImage?.toImageUrl(),
                    resultType = modeEntity.resultType,
                    playerCounts = modeEntity.playerCounts,
                    color = modeEntity.color,
                )
            }

        return BattleEvent(
            id = BrawlStarsId(eventEntity.brawlStarsId),
            mode = mode,
            mapName = toMessages(mapNameMessages),
        )
    }

    private fun toBattleEvent(
        eventEntity: BattleEventEntity,
        mapNameMessages: Map<Long, List<MessageEntity>>,
        modeNameMessages: Map<Long, List<MessageEntity>>,
        modeIconImages: Map<Long, BrawlStarsImageEntity>,
    ): BattleEvent {
        val mode =
            eventEntity.mode?.let { modeEntity ->
                BattleEventMode(
                    id = BrawlStarsId(modeEntity.brawlStarsId),
                    name = toMessages(modeNameMessages[modeEntity.brawlStarsId]),
                    iconImageUrl = modeIconImages[modeEntity.brawlStarsId]?.toImageUrl(),
                    resultType = modeEntity.resultType,
                    playerCounts = modeEntity.playerCounts,
                    color = modeEntity.color,
                )
            }
        return BattleEvent(
            id = BrawlStarsId(eventEntity.brawlStarsId),
            mode = mode,
            mapName =
                if (eventEntity.mapName != null) {
                    toMessages(mapNameMessages[eventEntity.brawlStarsId])
                } else {
                    toMessages(null)
                },
        )
    }

    private fun toMessages(entities: List<MessageEntity>?): Messages {
        if (entities.isNullOrEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }

    private fun BrawlStarsImageEntity.toImageUrl(): String = "${cloudFrontProperties.baseUrl}/$storedName"
}
