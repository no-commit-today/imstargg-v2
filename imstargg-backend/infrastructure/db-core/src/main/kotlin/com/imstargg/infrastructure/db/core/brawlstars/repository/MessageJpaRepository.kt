package com.imstargg.infrastructure.db.core.brawlstars.repository

import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MessageJpaRepository : JpaRepository<MessageEntity, Long> {
    fun findAllByCode(code: MessageCode): List<MessageEntity>

    fun findAllByCodeIn(codes: Collection<MessageCode>): List<MessageEntity>

    fun deleteAllByCode(code: MessageCode)
}
