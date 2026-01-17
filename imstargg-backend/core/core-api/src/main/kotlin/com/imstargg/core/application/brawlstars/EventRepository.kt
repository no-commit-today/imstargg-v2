package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.Event
import com.imstargg.core.enums.BattleEventMode
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventJpaRepository
import org.springframework.stereotype.Repository

@Repository
class EventRepository(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
) {
    fun find(id: BrawlStarsId): Event? {
        val eventEntity = battleEventJpaRepository.findByBrawlStarsId(id.value) ?: return null

        val mapNameMessages =
            eventEntity.mapNameMessageCode()?.let { code ->
                messageJpaRepository.findAllByCode(code)
            } ?: emptyList()

        return Event(
            id = BrawlStarsId(eventEntity.brawlStarsId),
            mode = EnumCodeType.findByCode<BattleEventMode>(eventEntity.mode),
            mapName = toMessages(mapNameMessages),
        )
    }

    private fun toMessages(entities: List<MessageEntity>): Messages {
        if (entities.isEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }
}
