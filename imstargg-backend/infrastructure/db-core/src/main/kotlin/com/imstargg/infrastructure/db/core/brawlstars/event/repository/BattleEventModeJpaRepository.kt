package com.imstargg.infrastructure.db.core.brawlstars.event.repository

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventModeJpaRepository : JpaRepository<BattleEventModeEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BattleEventModeEntity?
}
