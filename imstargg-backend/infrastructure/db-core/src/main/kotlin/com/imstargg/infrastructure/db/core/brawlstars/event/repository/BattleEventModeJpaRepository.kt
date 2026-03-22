package com.imstargg.infrastructure.db.core.brawlstars.event

import org.springframework.data.jpa.repository.JpaRepository

interface BattleEventModeJpaRepository : JpaRepository<BattleEventModeEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BattleEventModeEntity?
}
