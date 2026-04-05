package com.imstargg.infrastructure.db.core.brawlstars.repository

import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.data.jpa.repository.JpaRepository

interface MessageJpaRepository : JpaRepository<MessageEntity, Long> {
    fun findAllByTypeAndBrawlStarsId(
        type: MessageType,
        brawlStarsId: Long,
    ): List<MessageEntity>

    fun findAllByTypeAndBrawlStarsIdIn(
        type: MessageType,
        brawlStarsIds: Collection<Long>,
    ): List<MessageEntity>

    fun findAllByTypeAndContentContainingIgnoreCase(
        type: MessageType,
        content: String,
    ): List<MessageEntity>

    fun deleteAllByTypeAndBrawlStarsId(
        type: MessageType,
        brawlStarsId: Long,
    )
}
