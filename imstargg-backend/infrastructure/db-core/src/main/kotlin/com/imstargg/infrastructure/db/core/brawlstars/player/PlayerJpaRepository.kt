package com.imstargg.infrastructure.db.core.brawlstars.player

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface PlayerJpaRepository : JpaRepository<PlayerEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): PlayerEntity?
}
