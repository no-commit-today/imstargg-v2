package com.imstargg.infrastructure.db.core.brawlstars

import org.springframework.data.jpa.repository.JpaRepository

interface MessageJpaRepository : JpaRepository<MessageEntity, Long>
