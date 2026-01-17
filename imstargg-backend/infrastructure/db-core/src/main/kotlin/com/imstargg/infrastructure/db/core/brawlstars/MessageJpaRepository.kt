package com.imstargg.infrastructure.db.core.brawlstars

import org.springframework.data.jpa.repository.JpaRepository

interface MessageJpaRepository : JpaRepository<MessageEntity, Long> {
    fun findAllByCode(code: MessageCode): List<MessageEntity>

    fun findAllByCodeIn(codes: Collection<MessageCode>): List<MessageEntity>
}
