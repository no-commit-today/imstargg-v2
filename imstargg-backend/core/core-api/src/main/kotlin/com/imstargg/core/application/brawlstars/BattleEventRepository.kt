package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
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
) {
    fun find(id: BrawlStarsId): BattleEvent? {
        val eventEntity = battleEventJpaRepository.findByBrawlStarsId(id.value) ?: return null

        val mapNameMessages =
            eventEntity.mapNameMessageCode()?.let { code ->
                messageJpaRepository.findAllByCode(code)
            } ?: emptyList()

        val mode =
            eventEntity.mode?.let { modeEntity ->
                val modeNameMessages = messageJpaRepository.findAllByCode(modeEntity.nameMessageCode())
                val modeIconImage = brawlStarsImageJpaRepository.findByCode(modeEntity.iconImageCode())
                BattleEventMode(
                    id = BrawlStarsId(modeEntity.brawlStarsId),
                    name = toMessages(modeNameMessages),
                    iconImagePath = modeIconImage?.storedName,
                )
            }

        return BattleEvent(
            id = BrawlStarsId(eventEntity.brawlStarsId),
            mode = mode,
            mapName = toMessages(mapNameMessages),
        )
    }

    private fun toMessages(entities: List<MessageEntity>): Messages {
        if (entities.isEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }
}
